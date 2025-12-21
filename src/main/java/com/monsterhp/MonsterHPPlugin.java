package com.monsterhp;


import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import static net.runelite.api.gameval.NpcID.*;
import static net.runelite.api.gameval.VarbitID.*;

@Slf4j
@PluginDescriptor(
        name = "Monster HP Percentage"
)
public class MonsterHPPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private MonsterHPConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MonsterHPOverlay monsterhpoverlay;

    @Inject
    private ClientThread clientThread;

    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, WanderingNPC> wanderingNPCs = new HashMap<>();

    private List<String> selectedNpcs = new ArrayList<>();

    private List<String> selectedNpcsWithRules = new ArrayList<>();

    private List<String> selectedNpcIDs = new ArrayList<>();

    private List<String> npcShowAllBlacklist = new ArrayList<>();

    private boolean npcShowAll = true;

    private HashMap<Integer, WorldPoint> npcLocations = new HashMap<>();

    @Provides
    MonsterHPConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(MonsterHPConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(monsterhpoverlay);
        selectedNpcs = getSelectedNpcNames(false);
        selectedNpcsWithRules = getSelectedNpcNames(true);
        selectedNpcIDs = getSelectedNpcIds();

        this.npcShowAll = config.npcShowAll();
        npcShowAllBlacklist = getShowAllBlacklistNames();

        clientThread.invokeLater(this::rebuildAllNpcs);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(monsterhpoverlay);
        wanderingNPCs.clear();
        npcLocations.clear();
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        final NPC npc = npcSpawned.getNpc();
        if (!isNpcInList(npc)) return;

        WanderingNPC wnpc = new WanderingNPC(npc);

        if (isNpcNumericDefined(npc))
            wnpc.setIsTypeNumeric(1);

        Integer thresholdOverride = getNpcThreshold(npc);
        if (thresholdOverride != null)
            wnpc.setHealthThreshold(thresholdOverride);

        wanderingNPCs.put(npc.getIndex(), wnpc);
        npcLocations.put(npc.getIndex(), npc.getWorldLocation());
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        final NPC npc = npcDespawned.getNpc();

        if (npc == null || !isNpcInList(npc)) return;

        wanderingNPCs.remove(npc.getIndex());
        npcLocations.remove(npc.getIndex());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN ||
            gameStateChanged.getGameState() == GameState.HOPPING) {
            wanderingNPCs.clear();
            npcLocations.clear();
        }
    }

    // onNpcChanged is mostly required for id listing to work when npc is changing id but name remain the same
    // Example: npcs like phantom muspah have multiple ids(transitions) but same static name
    // so this applies and removes the text accordingly on npc id change if in list
    @Subscribe
    public void onNpcChanged(NpcChanged e) {
        final NPC npc = e.getNpc();
        int id = npc.getId();
        int idx = npc.getIndex();

        // Duke Sucellus have no onNpcDespawned when dying but fires sometimes on instance leaving if npc is not dead but in 12167(DUKE_SUCELLUS_ASLEEP) state...
        // So we have to do this special little step
        if (id == DUKE_SUCELLUS_DEAD || id == DUKE_SUCELLUS_DEAD_QUEST) {
            wanderingNPCs.remove(idx);
            npcLocations.remove(idx);
        }

        // Actual method
        if (isNpcInList(npc)) {
            WanderingNPC wnpc = new WanderingNPC(npc);

            if (isNpcNumericDefined(npc))
                wnpc.setIsTypeNumeric(1);

            Integer thresholdOverride = getNpcThreshold(npc);
            if (thresholdOverride != null)
                wnpc.setHealthThreshold(thresholdOverride);

            wanderingNPCs.put(idx, wnpc);
            npcLocations.put(idx, npc.getWorldLocation());
        } else {
            wanderingNPCs.remove(idx);
            npcLocations.remove(idx);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.showOverlay()) {
            return;
        }

        HashMap<WorldPoint, Integer> locationCount = new HashMap<>();
        for (WorldPoint location : npcLocations.values()) {
            locationCount.merge(location, 1, Integer::sum);
        }

        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (!isNpcInList(npc)) {
                continue;
            }

            final WanderingNPC wnpc = wanderingNPCs.get(npc.getIndex());

            if (wnpc != null) {
                updateWnpcProperties(npc, wnpc, locationCount);
            }
        }
    }

    private void updateWnpcProperties(NPC npc, WanderingNPC wnpc, Map<WorldPoint, Integer> locationCount) {
        // Runelite api npc actor method to calculate npc hp ratio
        double monsterHp = ((double) npc.getHealthRatio() / (double) npc.getHealthScale() * 100);

        // Without a jagex health api we have to use duct tape fixes.
        // Normally we'd just use getHealthRatio and getHealthScale, but for some NPCS like TOA raid bosses we have to use varbits
        boolean isBoss = BossUtil.isNpcBoss(npc);
        if (isBoss) {
            final int curHp = client.getVarbitValue(HPBAR_HUD_HP);
            final int maxHp = client.getVarbitValue(HPBAR_HUD_BASEHP);
            if (maxHp > 0 && curHp >= 0) {
                double hpVarbitRatio = 100.0 * curHp / maxHp;
                if (hpVarbitRatio > 0) {
                    monsterHp = hpVarbitRatio;
                }
            } else {
                // If we can't get data just don't update.
                return;
            }
        }

        // Doom of Mokhaiotl - Burrow or shielded transition support
        boolean isDoomBoss = BossUtil.DOOM_BOSS_IDS.contains(npc.getId());

        // Update WanderingNPC properties
        if (!npc.isDead() && ((npc.getHealthRatio() / npc.getHealthScale() != 1) || isDoomBoss)) {
            wnpc.setHealthRatio(monsterHp);
            wnpc.setCurrentLocation(npc.getWorldLocation());
            wnpc.setDead(false);

            WorldPoint currentLocation = wnpc.getCurrentLocation();

            if (locationCount.containsKey(currentLocation)) {
                wnpc.setOffset(locationCount.get(currentLocation) - 1);
                locationCount.put(currentLocation, locationCount.get(currentLocation) - 1);
            }
        } else if (npc.isDead()) {
            wnpc.setHealthRatio(0);
            if (config.hideDeath()) {
                wnpc.setDead(true);
            }
        }

        npcLocations.put(wnpc.getNpcIndex(), wnpc.getCurrentLocation());
    }

    private boolean isNpcNameInShowAllBlacklist(String npcName) {
        // Check for exact match or wildcard match
        return npcName != null && (npcShowAllBlacklist.contains(npcName) ||
                npcShowAllBlacklist.stream().anyMatch(pattern -> WildcardMatcher.matches(pattern, npcName)));
    }

    private boolean isNpcNameInList(String npcName) {
        // Check for exact match or wildcard match
        return npcName != null && (selectedNpcs.contains(npcName) ||
                selectedNpcs.stream().anyMatch(pattern -> WildcardMatcher.matches(pattern, npcName)));
    }

    private boolean isNpcIdInList(int npcId) {
        return selectedNpcIDs.contains(String.valueOf(npcId));
    }

    private boolean isNpcInList(NPC npc) {
        if (isNpcIdBlacklisted(npc)) return false;

        String npcName = npc.getName();
        String npcNameLower = npcName == null ? null : npcName.toLowerCase();

        boolean isInList = (isNpcNameInList(npcNameLower) || isNpcIdInList(npc.getId()));

        if (!isInList) {
            return this.npcShowAll && !isNpcNameInShowAllBlacklist(npcNameLower);
        }

        return true;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (Objects.equals(configChanged.getGroup(), "MonsterHP") && (Objects.equals(configChanged.getKey(), "npcShowAll") || Objects.equals(configChanged.getKey(), "npcShowAllBlacklist") || Objects.equals(configChanged.getKey(), "npcToShowHp") || Objects.equals(configChanged.getKey(), "npcIdToShowHp"))) {
            selectedNpcs = getSelectedNpcNames(false);
            selectedNpcsWithRules = getSelectedNpcNames(true);
            selectedNpcIDs = getSelectedNpcIds();

            this.npcShowAll = config.npcShowAll();
            npcShowAllBlacklist = getShowAllBlacklistNames();

            clientThread.invokeLater(this::rebuildAllNpcs);
        }
    }

    List<String> getShowAllBlacklistNames() {
        String configNPCs = config.npcShowAllBlacklist().toLowerCase();
        return configNPCs.isEmpty() ? Collections.emptyList() : Text.fromCSV(configNPCs);
    }

    @VisibleForTesting
    List<String> getSelectedNpcNames(boolean includeRuleDisplay) {
        String configNPCs = config.npcToShowHp().toLowerCase();
        if (configNPCs.isEmpty()) {
            return Collections.emptyList();
        }

        // "Raw" contains the comma-separated RAW text, this includes npc rules like ":n"
        List<String> selectedNpcNamesRaw = Text.fromCSV(configNPCs);

        // If false, remove all rules from the string to create a list of only the NPC names
        if (!includeRuleDisplay) {
            List<String> strippedNpcNames = new ArrayList<>(selectedNpcNamesRaw);

            // Strips the rule suffixes from each name if present
            strippedNpcNames.replaceAll(npcName -> {
                if (!npcName.contains(":")) {
                    return npcName;
                }

                // Handle edge cases like "::" or ":::".. where split[0] would be empty or non-existent
                String[] parts = npcName.split(":");
                return parts.length > 0 && !parts[0].isEmpty() ? parts[0] : npcName;
            });

            return strippedNpcNames;
        }

        return selectedNpcNamesRaw;
    }

    @VisibleForTesting
    List<String> getSelectedNpcIds() {
        String configNPCIDs = config.npcIdToShowHp().toLowerCase();
        if (configNPCIDs.isEmpty()) {
            return Collections.emptyList();
        }

        return Text.fromCSV(configNPCIDs);
    }

    
    boolean isNpcNumericDefined(NPC npc) {
        String npcName = Objects.requireNonNull(npc.getName()).toLowerCase();
        for (String npcRaw : selectedNpcsWithRules) {
            NpcRule rule = getNpcRule(npcRaw);
            if (WildcardMatcher.matches(rule.namePattern, npcName)) {
                return rule.numeric;
            }
        }
        return false;
    }

    Integer getNpcThreshold(NPC npc) {
        String npcName = Objects.requireNonNull(npc.getName()).toLowerCase();
        for (String npcRaw : selectedNpcsWithRules) {
            NpcRule rule = getNpcRule(npcRaw);
            if (WildcardMatcher.matches(rule.namePattern, npcName)) {
                return rule.threshold;
            }
        }
        return null;
    }

    private void rebuildAllNpcs() {
        wanderingNPCs.clear();

        if (client.getGameState() != GameState.LOGGED_IN &&
            client.getGameState() != GameState.LOADING) {
            // NPCs are still in the client after logging out, ignore them
            return;
        }

        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (isNpcInList(npc)) {
                WanderingNPC wnpc = new WanderingNPC(npc);

                if (isNpcNumericDefined(npc))
                    wnpc.setIsTypeNumeric(1);

                Integer thresholdOverride = getNpcThreshold(npc);
                if (thresholdOverride != null)
                    wnpc.setHealthThreshold(thresholdOverride);

                wanderingNPCs.put(npc.getIndex(), wnpc);
                npcLocations.put(npc.getIndex(), npc.getWorldLocation());
            }
        }
    }

    // Not to be confused with show all blacklist, this is for specific npc ids
    public boolean isNpcIdBlacklisted(NPC npc) {
        String npcName = npc.getName();
        if (npcName != null) {
            int id = npc.getId();

            switch (npcName) {
                case "Duke Sucellus": // duke sucellus - allow only fight id to be tracked from duke
                    return id != DUKE_SUCELLUS_AWAKE && id != DUKE_SUCELLUS_ASLEEP;
                case "Vardorvis":
                    return id == VARDORVIS_BASE_POSTQUEST; // Vardorvis outside instance.
                case "Akkha":
                    return id == AKKHA_SPAWN; // Pre-enter room idle Akkha id 11789
                case "Muttadile":
                    // On name tagging this is duplicating dogodile junior's hp to submerged version
                    // this is due to name matching, I doubt anybody want submerged version
                    return id == RAIDS_DOGODILE_SUBMERGED;
                case "Yama":
                    // Blacklist everything but Yama the boss
                    return id != YAMA;
                default:
                    return false;
            }
        }

        return false;
    }

    public static NpcRule getNpcRule(String npcRaw) {
        String[] parts = npcRaw.split(":");

        // Name should always be first part index e.g., 'Guard' from: 'Guard:n:10'.
        String name = (parts.length > 0 && !parts[0].isEmpty()) ? parts[0] : "";
        boolean numeric = false;
        Integer threshold = null;

        for (String s : parts) {
            if (s.equalsIgnoreCase("n")) {
                numeric = true;
            } else {
                try {
                    threshold = Integer.parseInt(s);
                }
                catch (NumberFormatException ignored) {} // ignore invalid parts
            }
        }

        return new NpcRule(name, numeric, threshold);
    }
}
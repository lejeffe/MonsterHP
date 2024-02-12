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
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

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

    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, WanderingNPC> wanderingNPCs = new HashMap<>();

    private List<String> selectedNpcs = new ArrayList<>();

    private List<String> selectedNpcIDs = new ArrayList<>();

    private boolean npcShowAll = true;

    private HashMap<Integer, WorldPoint> npcLocations = new HashMap<>();

    @Provides
    MonsterHPConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(MonsterHPConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(monsterhpoverlay);
        selectedNpcs = getSelectedNpcNames();
        selectedNpcIDs = getSelectedNpcIds();
        this.npcShowAll = config.npcShowAll();
        rebuildAllNpcs();
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

        if (!isNpcInList(npc.getName(), npc.getId())) return;

        wanderingNPCs.putIfAbsent(npc.getIndex(), new WanderingNPC(npc));
        npcLocations.put(npc.getIndex(), npc.getWorldLocation());
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        final NPC npc = npcDespawned.getNpc();
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

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.showOverlay()) {
            return;
        }

        HashMap<WorldPoint, Integer> locationCount = new HashMap<>();
        for (WorldPoint location : npcLocations.values()) {
            locationCount.put(location, locationCount.getOrDefault(location, 0) + 1);
        }

        for (NPC npc : client.getNpcs()) {
            if (!isNpcInList(npc.getName(), npc.getId())) {
                continue;
            }

            final WanderingNPC wnpc = wanderingNPCs.get(npc.getIndex());

            if (wnpc == null) {
                continue;
            }

            updateWnpcProperties(npc, wnpc, locationCount);
        }
    }

    private void updateWnpcProperties(NPC npc, WanderingNPC wnpc, Map<WorldPoint, Integer> locationCount) {
        double monsterHp = ((double) npc.getHealthRatio() / (double) npc.getHealthScale() * 100);

        if (!npc.isDead() && npc.getHealthRatio() / npc.getHealthScale() != 1) {
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

    private boolean isNpcNameInList(String npcName) {
        return (npcName != null && selectedNpcs.contains(npcName.toLowerCase()));
    }

    private boolean isNpcIdInList(int npcId) {
        String npcIdString = String.valueOf(npcId);
        return selectedNpcIDs.contains(npcIdString);
    }

    private boolean isNpcInList(String npcName, int npcId) {
        boolean isInList = (isNpcNameInList(npcName) || isNpcIdInList(npcId));

        if (!isInList) {
            return this.npcShowAll;
        }

        return true;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (Objects.equals(configChanged.getGroup(), "MonsterHP") && (Objects.equals(configChanged.getKey(), "npcShowAll") || Objects.equals(configChanged.getKey(), "npcToShowHp") || Objects.equals(configChanged.getKey(), "npcIdToShowHp"))) {
            selectedNpcs = getSelectedNpcNames();
            selectedNpcIDs = getSelectedNpcIds();

            this.npcShowAll = config.npcShowAll();
            rebuildAllNpcs();
        }
    }

    @VisibleForTesting
    List<String> getSelectedNpcNames() {
        String configNPCs = config.npcToShowHp().toLowerCase();
        if (configNPCs.isEmpty()) {
            return Collections.emptyList();
        }

        return Text.fromCSV(configNPCs);
    }

    @VisibleForTesting
    List<String> getSelectedNpcIds() {
        String configNPCIDs = config.npcIdToShowHp().toLowerCase();
        if (configNPCIDs.isEmpty()) {
            return Collections.emptyList();
        }

        return Text.fromCSV(configNPCIDs);
    }

    private void rebuildAllNpcs() {
        wanderingNPCs.clear();

        if (client.getGameState() != GameState.LOGGED_IN &&
                client.getGameState() != GameState.LOADING) {
            // NPCs are still in the client after logging out, ignore them
            return;
        }

        for (NPC npc : client.getNpcs()) {
            if (isNpcInList(npc.getName(), npc.getId())) {
                wanderingNPCs.putIfAbsent(npc.getIndex(), new WanderingNPC(npc));
                npcLocations.put(npc.getIndex(), npc.getWorldLocation());
            }
        }
    }
}
package com.monsterhp;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;

import net.runelite.api.*;
import static net.runelite.api.gameval.NpcID.*;

@Getter
public class BossUtil {

    // Tombs of Amascut - String identification seems like the way to identify these bosses (with blacklist control)
    private static final ImmutableSet<String> TOA_BOSS_NAMES = ImmutableSet.of("Akkha", "Kephri", "Zebak", "Ba-Ba", "Tumeken's Warden", "Elidinis' Warden");

    // Chambers of Xeric - Some ids for cox that support varbits
    private static final ImmutableSet<Integer> COX_BOSS_IDS = ImmutableSet.of(
        // The Great Olm
        OLM_HEAD,
        // Tekton
        RAIDS_TEKTON_WAITING, RAIDS_TEKTON_WALKING_STANDARD, RAIDS_TEKTON_FIGHTING_STANDARD, RAIDS_TEKTON_WALKING_ENRAGED, RAIDS_TEKTON_FIGHTING_ENRAGED, RAIDS_TEKTON_HAMMERING,
        // Vespula
        RAIDS_VESPULA_FLYING, RAIDS_VESPULA_ENRAGED, RAIDS_VESPULA_WALKING, RAIDS_VESPULA_PORTAL,
        // Muttadile
        RAIDS_DOGODILE_SUBMERGED, RAIDS_DOGODILE_JUNIOR, RAIDS_DOGODILE,
        // Vasa
        RAIDS_VASANISTIRIO_WALKING, RAIDS_VASANISTIRIO_HEALING
    );

    // Desert Treasure 2
    private static final ImmutableSet<Integer> DT2_BOSS_IDS = ImmutableSet.of(
        LEVIATHAN,
        VARDORVIS, VARDORVIS_BASE_POSTQUEST,
        WHISPERER,
        DUKE_SUCELLUS_AWAKE, DUKE_SUCELLUS_ASLEEP
    );

    // Generic bosses - bosses that does not have a specific section
    private static final ImmutableSet<Integer> GEN_BOSS_IDS = ImmutableSet.of(
        YAMA
    );

    // Doom of Mokhaiotl
    public static final ImmutableSet<Integer> DOOM_BOSS_IDS = ImmutableSet.of(
        DOM_BOSS,
        DOM_BOSS_SHIELDED,
        DOM_BOSS_BURROWED
    );

    private static final ImmutableSet<Integer> ALL_BOSS_IDS = 
        ImmutableSet.<Integer>builder()
            .addAll(GEN_BOSS_IDS)
            .addAll(COX_BOSS_IDS)
            .addAll(DT2_BOSS_IDS)
            .addAll(DOOM_BOSS_IDS)
            .build();

    public static boolean isNpcBossFromTOA(NPC npc) {
        int id = npc.getId();
        String name = npc.getName();
        boolean isWardenP1 = (id == TOA_WARDEN_TUMEKEN_PHASE1 || id == TOA_WARDEN_ELIDINIS_PHASE1);

        return name != null && TOA_BOSS_NAMES.contains(name) && !isWardenP1;
    }

    public static boolean isNpcBoss(NPC npc) {
        if (ALL_BOSS_IDS.contains(npc.getId())) {
            return true;
        }
        // Only check TOA names if ID lookup fails
        return isNpcBossFromTOA(npc);
    }
}

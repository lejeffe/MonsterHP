package com.monsterhp;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;

import net.runelite.api.*;
import static net.runelite.api.NpcID.*;

import java.util.Set;

@Getter
public class BossUtil {

    // Tombs of Amascut (demi bosses for now)
    public static final Set<String> TOA_BOSS_NAMES = Set.of("Akkha", "Kephri", "Zebak", "Ba-Ba");

    // Chambers of Xeric - Some ids for cox is only ratio based, so we comment them out to preserve fuller list (percentage still works if tagged)
    private static final ImmutableSet<Integer> COX_BOSS_NAMES = ImmutableSet.of(
            TEKTON, TEKTON_7541, TEKTON_7542, TEKTON_ENRAGED, TEKTON_ENRAGED_7544, TEKTON_7545,
            VESPULA, VESPULA_7531, VESPULA_7532, ABYSSAL_PORTAL,
            VANGUARD, VANGUARD_7526, VANGUARD_7527, VANGUARD_7528, VANGUARD_7529,
            GREAT_OLM, GREAT_OLM_LEFT_CLAW, GREAT_OLM_RIGHT_CLAW, GREAT_OLM_RIGHT_CLAW_7553, GREAT_OLM_7554, GREAT_OLM_LEFT_CLAW_7555,
            //DEATHLY_RANGER, DEATHLY_MAGE,
            MUTTADILE, MUTTADILE_7562, MUTTADILE_7563,
            VASA_NISTIRIO, VASA_NISTIRIO_7567
            //GUARDIAN, GUARDIAN_7570, GUARDIAN_7571, GUARDIAN_7572,
            //LIZARDMAN_SHAMAN_7573, LIZARDMAN_SHAMAN_7574,
            //ICE_DEMON, ICE_DEMON_7585,
            //SKELETAL_MYSTIC, SKELETAL_MYSTIC_7605, SKELETAL_MYSTIC_7606
    );

    // Desert Treasure 2
    private static final ImmutableSet<Integer> DT2_BOSS_NAMES = ImmutableSet.of(
            THE_LEVIATHAN,
            VARDORVIS,
            THE_WHISPERER
            // DUKE, is already added hardcoded in MonsterHPOverlay -> renderTimer
    );

    public static boolean isNpcBossFromTOA(NPC npc) {
        String name = npc.getName();
        return name != null && TOA_BOSS_NAMES.contains(name);
    }

    public static boolean isNpcBossFromCOX(NPC npc) {
        return COX_BOSS_NAMES.contains(npc.getId());
    }

    public static boolean isNpcBossFromDT2(NPC npc) {return DT2_BOSS_NAMES.contains(npc.getId());}

    public static boolean isNpcBoss(NPC npc) {
        return isNpcBossFromCOX(npc) || isNpcBossFromTOA(npc) ||  isNpcBossFromDT2(npc);
    }
}

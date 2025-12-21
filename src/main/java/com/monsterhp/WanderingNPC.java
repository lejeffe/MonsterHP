package com.monsterhp;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

public class WanderingNPC {
    @Getter
    private final int npcIndex;

    @Getter
    private final String npcName;

    @Getter
    private final int id;
    @Getter
    @Setter
    private NPC npc;

    @Getter
    @Setter
    private WorldPoint currentLocation;

    @Getter
    @Setter
    private double currentHp;

    @Getter
    @Setter
    private double healthRatio;

    @Getter
    @Setter
    private double healthScale;

    @Getter
    @Setter
    private boolean isDead;

    @Getter
    @Setter
    private int offset;

    @Getter
    @Setter
    private int isTypeNumeric;

    @Getter
    @Setter
    private int healthThreshold;

    WanderingNPC(NPC npc) {
        this.npc = npc;
        this.id = npc.getId();
        this.npcName = npc.getName();
        this.npcIndex = npc.getIndex();
        this.currentLocation = npc.getWorldLocation();
        this.currentHp = 100;
        this.healthRatio = 100;
        this.healthScale = npc.getHealthScale();
        this.isDead = false;
        this.offset = 0;
        this.isTypeNumeric = 0;
        this.healthThreshold = 0;
    }
}
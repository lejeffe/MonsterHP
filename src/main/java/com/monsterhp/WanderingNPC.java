package com.monsterhp;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

public class WanderingNPC
{
	@Getter
	private final int npcIndex;

	@Getter
	private final String npcName;

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
	private double lastHp;


	WanderingNPC(NPC npc)
	{
		this.npc = npc;
		this.npcName = npc.getName();
		this.npcIndex = npc.getIndex();
		this.currentLocation = npc.getWorldLocation();
		this.currentHp = 100;
		this.lastHp = 3000;
	}
}
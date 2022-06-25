package com.monsterhp;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("MonsterHP")
public interface MonsterHPConfig extends Config
{
	@ConfigItem(
		position = 0,
		keyName = "showOverlay",
		name = "Show HP over chosen NPCs",
		description = "Configures whether or not to have the HP shown over the chosen NPCs"
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "npcToShowHp",
		name = "NPC Names",
		description = "Enter names of NPCs where you wish to use this plugin"
	)
	default String npcToShowHp()
	{
		return "";
	}

//	@ConfigItem(
//		position = 2,
//		keyName = "showOverlayId",
//		name = "Activate on NPC Id",
//		description = "Configures whether or not to have the HP shown over the given NPC Id's"
//	)
//	default boolean showOverlayId()
//	{
//		return true;
//	}
//
//	@ConfigItem(
//		position = 3,
//		keyName = "npcIdToShowHp",
//		name = "NPC Id's",
//		description = "Enter the NPC Id's where you wish to use this plugin"
//	)
//	default String npcIdToShowHp()
//	{
//		return "";
//	}

	@Range(
		max = 300
	)
	@ConfigItem(
		position = 4,
		keyName = "normalHPColor",
		name = "Default hp overlay color",
		description = "Choose the color to be used on the hp"
	)
	default Color normalHPColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 5,
		keyName = "useLowHP",
		name = "Use low HP threshold",
		description = "Configures whether or not you wish to use a 2nd color when the monster hp hits below the low hp threshold"
	)
	default boolean useLowHP()
	{
		return true;
	}

	@ConfigItem(
		position = 6,
		keyName = "lowHPThreshold",
		name = "Low HP threshold",
		description = "Used to set the low HP threshold"
	)
	default int lowHPThreshold()
	{
		return 25;
	}

	@ConfigItem(
		position = 7,
		keyName = "lowHPColor",
		name = "Overlay color Low HP",
		description = "Choose the color to be used when the hp of the npc is below the chosen hp threshold"
	)
	default Color lowHPColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		position = 8,
		keyName = "HPHeight",
		name = "Height above NPC",
		description = "Change the vertical offset of the HP above the npc"
	)
	default int HPHeight()
	{
		return 25;
	}
}
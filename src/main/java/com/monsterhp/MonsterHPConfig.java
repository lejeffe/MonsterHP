package com.monsterhp;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("MonsterHP")
public interface MonsterHPConfig extends Config
{
	enum FontStyle
	{
		BOLD("Bold"),
		ITALICS("Italics"),
		BOLD_ITALICS("Bold and italics"),
		DEFAULT("Default");

		String name;

		FontStyle(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}
	}
	@ConfigSection(
		name = "hp settings",
		description = "Settings relating to hp",
		position = 1
	)
	String hp_settings = "hp_settings";

	@ConfigSection(
		name = "font settings",
		description = "Settings relating to fonts",
		position = 2
	)
	String font_settings = "font_settings";
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
		description = "Enter names of NPCs where you wish to use this plugin",
		section = hp_settings
	)
	default String npcToShowHp()
	{
		return "";
	}
	@ConfigItem(
		position = 2,
		keyName = "npcShowAll",
		name = "Show All",
		description = "Show for all NPCs",
		section = hp_settings
	)
	default boolean npcShowAll()
	{
		return false;
	}
	@Range(
		max = 300
	)
	@ConfigItem(
		position = 3,
		keyName = "normalHPColor",
		name = "Default hp overlay color",
		description = "Choose the color to be used on the hp",
		section = hp_settings
	)
	default Color normalHPColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 4,
		keyName = "useLowHP",
		name = "Use low HP threshold",
		description = "Configures whether or not you wish to use a 2nd color when the monster hp hits below the low hp threshold",
		section = hp_settings
	)
	default boolean useLowHP()
	{
		return true;
	}

	@ConfigItem(
		position = 5,
		keyName = "lowHPThreshold",
		name = "Low HP threshold",
		description = "Used to set the low HP threshold",
		section = hp_settings
	)
	default int lowHPThreshold()
	{
		return 25;
	}

	@ConfigItem(
		position = 6,
		keyName = "lowHPColor",
		name = "Overlay color Low HP",
		description = "Choose the color to be used when the hp of the npc is below the chosen hp threshold",
		section = hp_settings
	)
	default Color lowHPColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		position = 7,
		keyName = "aboveHPBar",
		name = "Above HP bar",
		description = "Hp above the monsters hp bar, otherwise the Hp is show on the body of the NPC",
		section = hp_settings
	)
	default boolean aboveHPBar()
	{
		return true;
	}

	@ConfigItem(
		position = 8,
		keyName = "HPHeight",
		name = "Height of the HP",
		description = "Change the vertical offset of the HP above the npc body or the HP bar",
		section = hp_settings
	)
	default int HPHeight()
	{
		return 25;
	}

	@ConfigItem(
		position = 9,
		keyName = "hideDeath",
		name = "Hide hp on death",
		description = "Hides the hp when the npc dies. Works nicely with the entity hider: Hide Dead NPCs option",
		section = hp_settings
	)
	default boolean hideDeath()
	{
		return false;
	}

	@ConfigItem(
		position = 10,
		keyName = "stackHp",
		name = "Stack monster HP",
		description = "Stacks the HP numbers on top of each other if multiple npc's are on the same tile",
		section = hp_settings
	)
	default boolean stackHp()
	{
		return false;
	}

	@ConfigItem(
		position = 11,
		keyName = "fontName",
		name = "Font",
		description = "Name of the font to use for the hp shown. Leave blank to use RuneLite setting.",
		section = font_settings
	)
	default String fontName()
	{
		return "roboto";
	}

	@ConfigItem(
		position = 12,
		keyName = "fontStyle",
		name = "Font style",
		description = "Style of the font to use for the hp shown. Only works with custom font.",
		section = font_settings
	)
	default FontStyle fontStyle()
	{
		return FontStyle.DEFAULT;
	}

	@ConfigItem(
		position = 13,
		keyName = "fontSize",
		name = "Font size",
		description = "Size of the font to use for XP drops. Only works with custom font.",
		section = font_settings
	)
	default int fontSize()
	{
		return 15;
	}
}
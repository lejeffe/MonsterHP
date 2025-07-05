package com.monsterhp;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.runelite.client.config.*;

@ConfigGroup("MonsterHP")
public interface MonsterHPConfig extends Config {
    enum FontStyle {
        BOLD("Bold"),
        ITALICS("Italics"),
        BOLD_ITALICS("Bold and italics"),
        DEFAULT("Default");

        String name;

        FontStyle(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum Background
    {
        OFF("Off"),
        SHADOW("Shadow"),
        OUTLINE("Outline");

        @Getter
        private final String group;

        @Override
        public String toString()
        {
            return group;
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
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "npcToShowHp",
            name = "NPC Names",
            description = "Enter names of NPCs where you wish to use this plugin",
            section = hp_settings
    )
    default String npcToShowHp() {
        return "";
    }

    @ConfigItem(
            position = 2,
            keyName = "npcIdToShowHp",
            name = "NPC Ids",
            description = "Enter Id of NPCs where you wish to use this plugin (optional)",
            section = hp_settings
    )
    default String npcIdToShowHp() {
        return "";
    }

    @ConfigItem(
            position = 3,
            keyName = "npcShowAll",
            name = "Show All",
            description = "Show for all NPCs",
            section = hp_settings
    )
    default boolean npcShowAll() {
        return false;
    }

    @ConfigItem(
            position = 4,
            keyName = "npcShowAllBlacklist",
            name = "Show all blacklist",
            description = "blacklist npc names from show all option",
            section = hp_settings
    )
    default String npcShowAllBlacklist() {
        return "";
    }

    @ConfigItem(
            position = 5,
            keyName = "npcHideFull",
            name = "Hide hp when full",
            description = "Hides the hp when the npc has not been damaged. Works nicely with the Show All option",
            section = hp_settings
    )
    default boolean npcHideFull() {
        return false;
    }

    @Alpha
    @Range(
            max = 300
    )
    @ConfigItem(
            position = 6,
            keyName = "normalHPColor",
            name = "Default hp overlay color",
            description = "Choose the color to be used on the hp",
            section = hp_settings
    )
    default Color normalHPColor() {
        return Color.GREEN;
    }

    @ConfigItem(
            position = 7,
            keyName = "useLowHP",
            name = "Use low HP threshold",
            description = "Configures whether or not you wish to use a 2nd color when the monster hp hits below the low hp threshold",
            section = hp_settings
    )
    default boolean useLowHP() {
        return true;
    }

    @ConfigItem(
            position = 8,
            keyName = "lowHPThreshold",
            name = "Low HP threshold",
            description = "Used to set the low HP threshold",
            section = hp_settings
    )
    default int lowHPThreshold() {
        return 25;
    }
    @Alpha
    @ConfigItem(
            position = 9,
            keyName = "lowHPColor",
            name = "Overlay color Low HP",
            description = "Choose the color to be used when the hp of the npc is below the chosen hp threshold",
            section = hp_settings
    )
    default Color lowHPColor() {
        return Color.RED;
    }

    @ConfigItem(
            position = 10,
            keyName = "aboveHPBar",
            name = "Above HP bar",
            description = "Hp above the monsters hp bar, otherwise the Hp is show on the body of the NPC",
            section = hp_settings
    )
    default boolean aboveHPBar() {
        return true;
    }

    @ConfigItem(
            position = 11,
            keyName = "HPHeight",
            name = "Height of the HP",
            description = "Change the vertical offset of the HP above the npc body or the HP bar",
            section = hp_settings
    )
    default int HPHeight() {
        return 50;
    }

    @ConfigItem(
            position = 12,
            keyName = "hideDeath",
            name = "Hide hp on death",
            description = "Hides the hp when the npc dies. Works nicely with the entity hider: Hide Dead NPCs option",
            section = hp_settings
    )
    default boolean hideDeath() {
        return false;
    }

    @ConfigItem(
            position = 13,
            keyName = "stackHp",
            name = "Stack monster HP",
            description = "Stacks the HP numbers on top of each other if multiple npc's are on the same tile",
            section = hp_settings
    )
    default boolean stackHp() {
        return false;
    }

    @Range(
            min = 0,
            max = 2
    )
    @ConfigItem(
            position = 14,
            keyName = "decimalHp",
            name = "Amount of decimals",
            description = "Show 0-2 decimals of precision, e.g. 13.33 instead of 13.",
            section = hp_settings
    )
    default int decimalHp() {
        return 0;
    }

    @ConfigItem(
            position = 15,
            keyName = "customFont",
            name = "Enable custom fonts",
            description = "Enabling this setting makes it possible to use the custom font from the box below this",
            section = font_settings
    )
    default boolean customFont() {
        return true;
    }

    @ConfigItem(
            position = 16,
            keyName = "fontName",
            name = "Font",
            description = "Name of the font to use for the hp shown. Leave blank to use RuneLite setting.",
            section = font_settings
    )
    default String fontName() {
        return "roboto";
    }

    @ConfigItem(
            position = 17,
            keyName = "fontStyle",
            name = "Style",
            description = "Style of the font to use for the hp shown. Only works with custom font.",
            section = font_settings
    )
    default FontStyle fontStyle() {
        return FontStyle.DEFAULT;
    }

    @ConfigItem(
            position = 18,
            keyName = "fontSize",
            name = "Size",
            description = "Size of the font to use for hp text. Only works with custom font.",
            section = font_settings
    )
    default int fontSize() {
        return 15;
    }

    @ConfigItem(
            position = 19,
            keyName = "numericHealth",
            name = "Numeric All Health",
            description = "Tries to show the numeric health of all tagged monsters instead of percentage.",
            section = hp_settings
    )
    default boolean numericAllHealth() {
        return false;
    }

    @ConfigItem(
            position = 20,
            name = "Background",
            keyName = "fontBackground",
            description = "Background of the HP text",
            section = font_settings
    )
    default Background fontBackground()
    {
        return Background.SHADOW;
    }

    @Range(
            min = 1,
            max = 100
    )
    @ConfigItem(
            position = 21,
            keyName = "fontShadowSize",
            name = "Shadow size",
            description = "Offset of the shadow drawn, requires font backgrounds.",
            section = font_settings
    )
    default int fontShadowSize() {
        return 1;
    }

    @Range(
            min = 1,
            max = 100
    )
    @ConfigItem(
            position = 22,
            keyName = "fontOutlineSize",
            name = "Outline size",
            description = "Size of the outline drawn, requires font backgrounds.",
            section = font_settings
    )
    default int fontOutlineSize() {
        return 4;
    }
    @Alpha
    @ConfigItem(
            position = 23,
            keyName = "fontOutlineColor",
            name = "Outline color",
            description = "Choose the color for the text outline",
            section = font_settings
    )
    default Color fontOutlineColor() {
        return Color.BLACK;
    }

    @ConfigItem(
            position = 24,
            keyName = "gradientHP",
            name = "Gradient HP",
            description = "HP will be gradient from color preset A to B depending on the percentage. (Overwrites low HP threshold setting)",
            section = hp_settings
    )
    default boolean useGradientHP() {
        return true;
    }

    @Alpha
    @ConfigItem(
            position = 25,
            keyName = "gradientHPColorA",
            name = "Gradient color A",
            description = "Choose the color for gradient A",
            section = hp_settings
    )
    default Color gradientHPColorA() {
        return Color.GREEN;
    }

    @Alpha
    @ConfigItem(
            position = 26,
            keyName = "gradientHPColorB",
            name = "Gradient color B",
            description = "Choose the color for gradient B",
            section = hp_settings
    )
    default Color gradientHPColorB() {
        return Color.RED;
    }
}

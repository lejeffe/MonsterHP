package com.monsterhp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NpcID;
import net.runelite.api.Point;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
public class MonsterHPOverlay extends Overlay {
    private final MonsterHPPlugin plugin;
    private final MonsterHPConfig config;

    private NPCManager npcManager;
    protected String lastFont = "";
    protected int lastFontSize = 0;
    protected boolean useRunescapeFont = true;
    protected MonsterHPConfig.FontStyle lastFontStyle = MonsterHPConfig.FontStyle.DEFAULT;
    protected Font font = null;

    NumberFormat format = new DecimalFormat("#");
    NumberFormat oneDecimalFormat = new DecimalFormat("#.#");
    NumberFormat twoDecimalFormat = new DecimalFormat("#.##");

    @Inject
    MonsterHPOverlay(MonsterHPPlugin plugin, MonsterHPConfig config, NPCManager npcManager) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.plugin = plugin;
        this.config = config;
        this.npcManager = npcManager;
    }

    protected void handleFont(Graphics2D graphics) {
        if (font != null) {
            graphics.setFont(font);
            if (useRunescapeFont) {
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        updateFont();
        handleFont(graphics);
        if (config.showOverlay()) {
            plugin.getWanderingNPCs().forEach((id, npc) -> renderTimer(npc, graphics));
        }
        return null;
    }

    private String getCurrentHpString(WanderingNPC npc) {
        String currentHPString;
        if (config.numericHealth()) {
            currentHPString = String.valueOf((int) npc.getCurrentHp());
        } else {
            switch (config.decimalHp()) {
                case 1:
                    currentHPString = String.valueOf(oneDecimalFormat.format(npc.getHealthRatio()));
                    break;
                case 2:
                    currentHPString = String.valueOf(twoDecimalFormat.format(npc.getHealthRatio()));
                    break;
                default:
                    currentHPString = String.valueOf(format.format(npc.getHealthRatio()));
                    break;
            }
        }
        return currentHPString;
    }


    private void renderTimer(final WanderingNPC npc, final Graphics2D graphics) {
        if (npc.isDead()) {
            return;
        }
        
        // Use Numeric health
        if (config.numericHealth()) {

            Integer maxHealth = npcManager.getHealth(npc.getId());

            // Some npcs return null when using npcManager as they seem to not be added with all details to static.runelite.net?
            // i assume because health mimic mechanics like Duke Sucellus poison wakeup that has a 200 fake health before fight id
            // https://github.com/runelite/static.runelite.net/blob/gh-pages/npcs/npcs.json
            if (npc.getNpcName().equals("Duke Sucellus")) {
                if (npc.getId() == NpcID.DUKE_SUCELLUS_12191) { // Duke Sucellus - fight id
                    maxHealth = 440; // we assume its health is 440
                }
                if (npc.getId() == NpcID.DUKE_SUCELLUS_12167) { // Duke Sucellus - pre fight id
                    maxHealth = 200; // we assume its 'poison' health is 200
                }
            }

            if (maxHealth != null) {
                // Use the current health ratio and round it according to monsters max hp
                double numericHealth = (int) Math.floor((npc.getHealthRatio() / 100) * maxHealth);
                npc.setCurrentHp(numericHealth);
            }
        }

        // Coloring
        Color timerColor = config.normalHPColor();
        boolean isHealthBelowThreshold = npc.getHealthRatio() < config.lowHPThreshold();
        if (config.useLowHP() && isHealthBelowThreshold) {
            timerColor = config.lowHPColor();
        }

        String currentHPString = getCurrentHpString(npc);

        Point canvasPoint;
        if (config.aboveHPBar()) {
            canvasPoint = npc.getNpc().getCanvasTextLocation(graphics, currentHPString, npc.getNpc().getLogicalHeight() + config.HPHeight());
        } else {
            canvasPoint = npc.getNpc().getCanvasTextLocation(graphics, currentHPString, config.HPHeight());
        }

        if (canvasPoint == null) {
            return;
        }

        if (config.stackHp()) {
            int offSet = (int) (npc.getOffset() * config.fontSize() * 0.85);
            Point stackOffset = new Point(canvasPoint.getX(), canvasPoint.getY() + offSet);
            OverlayUtil.renderTextLocation(graphics, stackOffset, currentHPString, timerColor);
        } else {
            OverlayUtil.renderTextLocation(graphics, canvasPoint, currentHPString, timerColor);
        }
    }

    private void updateFont() {
        //only perform anything within this function if any settings related to the font have changed
        if (!lastFont.equals(config.fontName()) || lastFontSize != config.fontSize() || lastFontStyle != config.fontStyle()) {
            if (config.customFont()) {
                lastFont = config.fontName();
            }
            lastFontSize = config.fontSize();
            lastFontStyle = config.fontStyle();

            //use runescape font as default
            if (config.fontName().equals("") || config.customFont() == false) {
                if (config.fontSize() < 16) {
                    font = FontManager.getRunescapeSmallFont();
                } else if (config.fontStyle() == MonsterHPConfig.FontStyle.BOLD || config.fontStyle() == MonsterHPConfig.FontStyle.BOLD_ITALICS) {
                    font = FontManager.getRunescapeBoldFont();
                } else {
                    font = FontManager.getRunescapeFont();
                }

                if (config.fontSize() > 16) {
                    font = font.deriveFont((float) config.fontSize());
                }

                if (config.fontStyle() == MonsterHPConfig.FontStyle.BOLD) {
                    font = font.deriveFont(Font.BOLD);
                }
                if (config.fontStyle() == MonsterHPConfig.FontStyle.ITALICS) {
                    font = font.deriveFont(Font.ITALIC);
                }
                if (config.fontStyle() == MonsterHPConfig.FontStyle.BOLD_ITALICS) {
                    font = font.deriveFont(Font.ITALIC | Font.BOLD);
                }

                useRunescapeFont = true;
                return;
            }

            int style = Font.PLAIN;
            switch (config.fontStyle()) {
                case BOLD:
                    style = Font.BOLD;
                    break;
                case ITALICS:
                    style = Font.ITALIC;
                    break;
                case BOLD_ITALICS:
                    style = Font.BOLD | Font.ITALIC;
                    break;
            }

            font = new Font(config.fontName(), style, config.fontSize());
            useRunescapeFont = false;
        }
    }
}

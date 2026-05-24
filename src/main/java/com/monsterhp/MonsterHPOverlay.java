package com.monsterhp;

import java.awt.*;
import java.awt.font.TextLayout;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.inject.Inject;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import static net.runelite.api.gameval.NpcID.*;
import static net.runelite.api.gameval.VarbitID.*;

@Slf4j
public class MonsterHPOverlay extends Overlay {
    @Inject
    private Client client;

    private final MonsterHPPlugin plugin;
    private final MonsterHPConfig config;

    private NPCManager npcManager;
    protected String lastFont = "";
    protected int lastFontSize = 0;
    protected boolean useRunescapeFont = true;
    protected MonsterHPConfig.FontStyle lastFontStyle = MonsterHPConfig.FontStyle.DEFAULT;
    protected Font font = null;

    NumberFormat format = new DecimalFormat("#");
    NumberFormat oneDecimalFormat = new DecimalFormat("#.0");
    NumberFormat twoDecimalFormat = new DecimalFormat("#.00");

    @Inject
    MonsterHPOverlay(MonsterHPPlugin plugin, MonsterHPConfig config, NPCManager npcManager) {
        setPriority(0.75f);
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
            ArrayList<NPC> stackedNpcs = new ArrayList<>();
            plugin.getWanderingNPCs().forEach((id, npc) -> renderTimer(npc, graphics, stackedNpcs));
        }
        return null;
    }

    private String getCurrentHpString(WanderingNPC wnpc) {
        boolean showNumericHealth = config.numericAllHealth() || wnpc.getIsTypeNumeric() == 1;
        NPC npc = wnpc.getNpc();

        // Numeric
        if (showNumericHealth) {
            boolean usePercentage = !wnpc.isSupportsNumeric() && wnpc.getHealthRatio() < 100.0;

            if (!usePercentage) {
                if (BossUtil.isNpcBoss(npc)) {
                    // Defaults to percentage if numeric fails
                    final int curHp = client.getVarbitValue(HPBAR_HUD_HP);
                    return String.valueOf(curHp);
                }

                return String.valueOf((int) (wnpc.getCurrentHp()));
            }
        }

        // Percentage
        return getNpcDecimalHealthPercentage(wnpc.getHealthRatio());
    }

    // Returns health ratio formatted by user decimal health config value
    private String getNpcDecimalHealthPercentage(double healthRatio) {
        switch (config.decimalHp()) {
            case 1:  return String.valueOf(oneDecimalFormat.format(healthRatio));
            case 2:  return String.valueOf(twoDecimalFormat.format(healthRatio));
            default: return String.valueOf((healthRatio >= 1) ? format.format(healthRatio) : twoDecimalFormat.format(healthRatio));
        }
    }

    private void renderTimer(final WanderingNPC wnpc, final Graphics2D graphics, ArrayList<NPC> stackedNpcs) {
        if (wnpc == null || wnpc.isDead()) {
            return;
        }

        // Get NPC from WanderingNPC
        NPC npc = wnpc.getNpc();

        // NPC Health Ratio
        double wNpcHealthRatio = wnpc.getHealthRatio();

        // Skip npc with full hp if enabled
        if (config.npcHideFull() && wNpcHealthRatio == 100) return;

        // Get max health through NPC manager, returns null if not found
        Integer maxHealth = npcManager.getHealth(npc.getId());

        // Health fix for some bosses (that are not correctly set in npcManager)
        if (BossUtil.isNpcBoss(npc)) {
            maxHealth = client.getVarbitValue(HPBAR_HUD_BASEHP);

            // Special check for Chambers of xeric(cox) - check if both hands are dead (allows OLM_HEAD to be main varbit source)
            if (npc.getId() == OLM_HEAD && isCoxOlmHandsAlive()) {
                return;
            }

            if (maxHealth <= 0) return;
        }

        // If NPC Does not support numeric health, remove support
        if (maxHealth == null) {
            wnpc.setSupportsNumeric(false);
        }

        // Use Numeric health
        // TLDR: (isSupportsNumeric check required for later % symbol concatenation)
        boolean isUsingNumeric = (config.numericAllHealth() || wnpc.getIsTypeNumeric() == 1) && wnpc.isSupportsNumeric();
        if (isUsingNumeric && maxHealth != null) {
            // Use the current health ratio and round it according to monsters max hp
            double numericHealth = (int) Math.round((wNpcHealthRatio / 100) * maxHealth);
            wnpc.setCurrentHp(numericHealth);
        }

        // Coloring
        Color timerColor = config.normalHPColor();
        boolean isHealthBelowThreshold = wNpcHealthRatio < config.lowHPThreshold();
        if (config.useLowHP() && isHealthBelowThreshold) {
            timerColor = config.lowHPColor();
        }

        if (config.useGradientHP()) {
            if (maxHealth != null && maxHealth > 1) {
                int curNumericHealth = (int) Math.round((wNpcHealthRatio / 100) * maxHealth);
                timerColor = getGradientHpColor(curNumericHealth, maxHealth);
            } else { // Try percentage based gradient hp - happens if npcManager can't get numeric max health.
                int curNumericHealth = (int) Math.round(wNpcHealthRatio);
                timerColor = getGradientHpColor(curNumericHealth, 100);
            }
        }

        // Per NPC Threshold support
        int healthThreshold = wnpc.getHealthThreshold();
        if (healthThreshold > 0) {
            boolean belowThreshold = isUsingNumeric ?
                (int) Math.round(wnpc.getCurrentHp()) < healthThreshold : wNpcHealthRatio < healthThreshold;
            if (belowThreshold) {
                timerColor = config.lowHPColor();
            }
        }

        String currentHPString = getCurrentHpString(wnpc);
        if (config.appendPercentSymbol() && !isUsingNumeric) {
            currentHPString += "%";
        }

        if (config.stackHp()) {
            /*
             * Stack method created by github.com/MoreBuchus in his tzhaar-hp-tracker plugin
             * i Xines just modified this method to work with Monster HP plugin.
             * So credits goes to Buchus for the method.
             */

            int offset = 0;
            NPC firstStack = null;
            for (NPC sNpc : stackedNpcs) {
                if (sNpc.getWorldLocation().getX() == npc.getWorldLocation().getX() && sNpc.getWorldLocation().getY() == npc.getWorldLocation().getY()) {
                    if (firstStack == null) {
                        firstStack = npc;
                    }
                    offset += graphics.getFontMetrics().getHeight();
                }
            }

            int zOffset = config.HPHeight();
            if (config.aboveHPBar()) {
                zOffset += npc.getLogicalHeight();
            }

            stackedNpcs.add(npc);

            Point textLocation = offset > 0 ? firstStack.getCanvasTextLocation(graphics, currentHPString, zOffset) : npc.getCanvasTextLocation(graphics, currentHPString, zOffset);
            if (textLocation != null) {
                Point stackOffset = new Point(textLocation.getX(), textLocation.getY() - offset);
                handleText(graphics, stackOffset, currentHPString, timerColor);
            }
        } else {
            Point canvasPoint;
            if (config.aboveHPBar()) {
                canvasPoint = npc.getCanvasTextLocation(graphics, currentHPString, npc.getLogicalHeight() + config.HPHeight());
            } else {
                canvasPoint = npc.getCanvasTextLocation(graphics, currentHPString, config.HPHeight());
            }

            if (canvasPoint != null) {
                handleText(graphics, canvasPoint, currentHPString, timerColor);
            }
        }
    }

    // Returns true if either of olm hands are alive
    private boolean isCoxOlmHandsAlive() {
        for (WanderingNPC wnpc : plugin.getWanderingNPCs().values()) {
            NPC npc = wnpc.getNpc();
            if (npc == null) continue;

            int id = npc.getId();
            if ((id == OLM_HAND_RIGHT || id == OLM_HAND_LEFT) && !npc.isDead()) {
                return true;
            }
        }

        return false;
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
            if (config.fontName().equals("") || !config.customFont()) {
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

    private void handleText(Graphics2D graphics, Point textLoc, String text, Color color)
    {
        switch (config.fontBackground())
        {
            case OUTLINE:
            {
                // Create a new Graphics2D instance to avoid modifying the original one
                Graphics2D g2d = (Graphics2D) graphics.create();

                // Enable antialiasing for smoother text (just to be sure)
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Translate the graphics
                g2d.translate(textLoc.getX(), textLoc.getY());

                // Set outline color
                g2d.setColor(config.fontOutlineColor());

                // Text layout
                TextLayout tl = new TextLayout(text, graphics.getFont(), g2d.getFontRenderContext());

                // Get the outline shape
                Shape shape = tl.getOutline(null);

                // Set outline thickness and try to prevent artifacts on sharp angles
                g2d.setStroke(new BasicStroke((float) config.fontOutlineSize(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Draw the outline
                g2d.draw(shape);

                // Set fill color
                g2d.setColor(color);

                // Fill the shape
                g2d.fill(shape);

                // Dispose of the temporary Graphics2D instance
                g2d.dispose();

                break;
            }
            case SHADOW:
            {
                int offsetShadow = config.fontShadowSize();

                graphics.setColor(new Color(0,0,0, color.getAlpha()));
                graphics.drawString(text, textLoc.getX() + offsetShadow, textLoc.getY() + offsetShadow);
                graphics.setColor(color);
                graphics.drawString(text, textLoc.getX(), textLoc.getY());
                break;
            }
            case OFF:
                // Mini shadow
                graphics.setColor(new Color(0,0,0, color.getAlpha()));
                graphics.drawString(text, textLoc.getX() + 1, textLoc.getY() + 1);

                // Draw string (renderTextLocation does not support alpha coloring or is broken...)
                graphics.setColor(color);
                graphics.drawString(text, textLoc.getX(), textLoc.getY());
                break;
            default:
                break;
        }
    }

    private Color getGradientHpColor(int currentHealth, int maxHealth) {
        // Ensure currentHealth is between 0 and maxHealth
        currentHealth = Math.min(maxHealth, Math.max(0, currentHealth));

        // Calculate the health percentage
        double healthPercentage = (double) currentHealth / maxHealth;

        // Get config RGB values
        Color colorA = config.gradientHPColorA();
        Color colorB = config.gradientHPColorB();

        // Calculate the gradient depending on percentage and RGB values
        int red = (int) (colorB.getRed() + (colorA.getRed() - colorB.getRed()) * healthPercentage);
        int green = (int) (colorB.getGreen() + (colorA.getGreen() - colorB.getGreen()) * healthPercentage);
        int blue = (int) (colorB.getBlue() + (colorA.getBlue() - colorB.getBlue()) * healthPercentage);

        return new Color(red, green, blue);
    }
}
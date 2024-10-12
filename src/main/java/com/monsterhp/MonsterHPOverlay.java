package com.monsterhp;

import java.awt.*;
import java.awt.font.TextLayout;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.inject.Inject;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Point;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

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
            plugin.getWanderingNPCs().forEach((id, npc) -> {
                if (config.npcHideFull() && npc.getHealthRatio() == 100) return;
                renderTimer(npc, graphics);
            });
            ArrayList<NPC> stackedNpcs = new ArrayList<>();
            plugin.getWanderingNPCs().forEach((id, npc) -> renderTimer(npc, graphics, stackedNpcs));
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


    private void renderTimer(final WanderingNPC npc, final Graphics2D graphics, ArrayList<NPC> stackedNpcs) {
        if (npc.isDead()) {
            return;
        }

        // Skip npc with full hp if enabled
        if (config.npcHideFull() && npc.getHealthRatio() == 100) return;

        // Get max health through NPC manager, returns null if not found
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

        // Use Numeric health
        if (config.numericHealth()) {
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

        if (config.useGradientHP()) {
            if (maxHealth != null) {
                int curNumericHealth = (int) Math.floor((npc.getHealthRatio() / 100) * maxHealth);
                timerColor = getGradientHpColor(curNumericHealth, maxHealth);
            } else { // Try percentage based gradient hp - happens if npcManager can't get numeric health.
                int curNumericHealth = (int) Math.floor(npc.getHealthRatio());
                timerColor = getGradientHpColor(curNumericHealth, 100);
            }
        }

        String currentHPString = getCurrentHpString(npc);

        if (config.stackHp()) {
            /*
            * Stack method created by github.com/MoreBuchus in his tzhaar-hp-tracker plugin
            * i Xines just modified this method to work with Monster HP plugin.
            * So credits goes to Buchus for the method.
            */

            int offset = 0;
            NPC firstStack = null;
            for (NPC sNpc : stackedNpcs)
            {
                if (sNpc.getWorldLocation().getX() == npc.getNpc().getWorldLocation().getX() && sNpc.getWorldLocation().getY() == npc.getNpc().getWorldLocation().getY())
                {
                    if (firstStack == null)
                    {
                        firstStack = npc.getNpc();
                    }
                    offset += graphics.getFontMetrics().getHeight();
                }
            }

            int zOffset = config.HPHeight();
            if (config.aboveHPBar()) {
                zOffset += npc.getNpc().getLogicalHeight();
            }

            stackedNpcs.add(npc.getNpc());

            Point textLocation = offset > 0 ? firstStack.getCanvasTextLocation(graphics, currentHPString, zOffset) : npc.getNpc().getCanvasTextLocation(graphics, currentHPString, zOffset);
            if (textLocation != null) {
                Point stackOffset = new Point(textLocation.getX(), textLocation.getY() - offset);
                handleText(graphics, stackOffset, currentHPString, timerColor);
            }
        } else {
            Point canvasPoint;
            if (config.aboveHPBar()) {
                canvasPoint = npc.getNpc().getCanvasTextLocation(graphics, currentHPString, npc.getNpc().getLogicalHeight() + config.HPHeight());
            } else {
                canvasPoint = npc.getNpc().getCanvasTextLocation(graphics, currentHPString, config.HPHeight());
            }

            if (canvasPoint != null) {
                handleText(graphics, canvasPoint, currentHPString, timerColor);
            }
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

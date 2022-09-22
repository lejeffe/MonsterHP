package com.monsterhp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.inject.Inject;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class MonsterHPOverlay extends Overlay
{
	private final MonsterHPPlugin plugin;
	private final MonsterHPConfig config;
	private final NumberFormat format;

	protected String lastFont = "";
	protected int lastFontSize = 0;
	protected boolean useRunescapeFont = true;
	protected MonsterHPConfig.FontStyle lastFontStyle = MonsterHPConfig.FontStyle.DEFAULT;
	protected Font font = null;

	@Inject
	MonsterHPOverlay(MonsterHPPlugin plugin, MonsterHPConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.plugin = plugin;
		this.config = config;
		this.format = config.increasePrecision() ? new DecimalFormat("#.0") : new DecimalFormat("#");
	}

	protected void handleFont(Graphics2D graphics)
	{
		if(font != null)
		{
			graphics.setFont(font);
			if(useRunescapeFont)
			{
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			}
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		updateFont();
		handleFont(graphics);
		if (config.showOverlay())
		{
			plugin.getWanderingNPCs().forEach((id, npc) -> renderTimer(npc, graphics));
		}
		return null;
	}

	private void renderTimer(final WanderingNPC npc, final Graphics2D graphics)
	{
		if(!npc.isDead())
		{
			Color timerColor = config.normalHPColor();

			if (config.useLowHP() && npc.getCurrentHp() < config.lowHPThreshold())
			{
				timerColor = config.lowHPColor();
			}

			String currentHPString = String.valueOf(format.format(npc.getCurrentHp()));
			Point canvasPoint = new Point(0,0);
			if (config.aboveHPBar())
			{
				canvasPoint = npc.getNpc().getCanvasTextLocation(graphics, currentHPString, npc.getNpc().getLogicalHeight() + config.HPHeight());
			}
			else
			{
				canvasPoint = npc.getNpc().getCanvasTextLocation(graphics, currentHPString, config.HPHeight());
			}

			if (config.stackHp())
			{
				int offSet = (int) (npc.getOffset() * config.fontSize() * 0.85);
				Point stackOffset = new Point(canvasPoint.getX(), canvasPoint.getY() + offSet);
				OverlayUtil.renderTextLocation(graphics, stackOffset, currentHPString, timerColor);
			}
			else
			{
				OverlayUtil.renderTextLocation(graphics, canvasPoint, currentHPString, timerColor);
			}
		}
	}
	private void updateFont()
	{
		//only perform anything within this function if any settings related to the font have changed
		if(!lastFont.equals(config.fontName()) || lastFontSize != config.fontSize() || lastFontStyle != config.fontStyle())
		{
			if(config.customFont()){
				lastFont = config.fontName();
			}
			lastFontSize = config.fontSize();
			lastFontStyle = config.fontStyle();

			//use runescape font as default
			if (config.fontName().equals("") || config.customFont() == false)
			{
				if (config.fontSize() < 16)
				{
					font = FontManager.getRunescapeSmallFont();
				}
				else if (config.fontStyle() == MonsterHPConfig.FontStyle.BOLD || config.fontStyle() == MonsterHPConfig.FontStyle.BOLD_ITALICS)
				{
					font = FontManager.getRunescapeBoldFont();
				}
				else
				{
					font = FontManager.getRunescapeFont();
				}

				if (config.fontSize() > 16)
				{
					font = font.deriveFont((float)config.fontSize());
				}

				if (config.fontStyle() == MonsterHPConfig.FontStyle.BOLD)
				{
					font = font.deriveFont(Font.BOLD);
				}
				if (config.fontStyle() == MonsterHPConfig.FontStyle.ITALICS)
				{
					font = font.deriveFont(Font.ITALIC);
				}
				if (config.fontStyle() == MonsterHPConfig.FontStyle.BOLD_ITALICS)
				{
					font = font.deriveFont(Font.ITALIC | Font.BOLD);
				}

				useRunescapeFont = true;
				return;
			}

			int style = Font.PLAIN;
			switch (config.fontStyle())
			{
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

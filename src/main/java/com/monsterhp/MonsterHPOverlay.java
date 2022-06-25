package com.monsterhp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.inject.Inject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class MonsterHPOverlay extends Overlay
{
	private final MonsterHPPlugin plugin;
	private final MonsterHPConfig config;

	NumberFormat format = new DecimalFormat("#");

	@Inject
	MonsterHPOverlay(MonsterHPPlugin plugin, MonsterHPConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.showOverlay())
		{
			plugin.getWanderingNPCs().forEach((id, npc) -> renderTimer(npc, graphics));
		}
		return null;
	}

	private void renderTimer(final WanderingNPC npc, final Graphics2D graphics)
	{
		Color timerColor = config.normalHPColor();

		if (config.useLowHP() && npc.getCurrentHp()<config.lowHPThreshold())
		{
			timerColor = config.lowHPColor();
		}

		String currentHPString = String.valueOf(format.format(npc.getCurrentHp()));

		final Point canvasPoint = npc.getNpc().getCanvasTextLocation(graphics, currentHPString, npc.getNpc().getLogicalHeight() + config.HPHeight());

		OverlayUtil.renderTextLocation(graphics, canvasPoint, currentHPString, timerColor);
	}
}

package com.monsterhp;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Monster HP Percentage"
)
public class MonsterHPPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private MonsterHPConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MonsterHPOverlay monsterhpoverlay;

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, WanderingNPC> wanderingNPCs = new HashMap<>();

	private List<String> selectedNPCs = new ArrayList<>();

	private HashMap<Integer, WorldPoint> npcLocations = new HashMap<>();

	@Provides
	MonsterHPConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MonsterHPConfig.class);
	}
	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(monsterhpoverlay);
		selectedNPCs = getSelectedNPCs();
		rebuildAllNpcs();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(monsterhpoverlay);
		wanderingNPCs.clear();
		npcLocations.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		final NPC npc = npcSpawned.getNpc();
		final String npcName = npc.getName();
		final int npcId = npc.getId();

		if (npcName == null || !selectedNPCs.contains(npcName.toLowerCase()))
		{
			return;
		}


		wanderingNPCs.putIfAbsent(npc.getIndex(), new WanderingNPC(npc));
		npcLocations.put(npc.getIndex(), npc.getWorldLocation());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		final NPC npc = npcDespawned.getNpc();
		final String npcName = npc.getName();

		if (npcName == null || !selectedNPCs.contains(npcName.toLowerCase()))
		{
			return;
		}

		wanderingNPCs.remove(npc.getIndex());
		npcLocations.remove(npc.getIndex());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN ||
			gameStateChanged.getGameState() == GameState.HOPPING)
		{
			wanderingNPCs.clear();
			npcLocations.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		HashMap<WorldPoint, Integer> locationCount = new HashMap<>();
		for (WorldPoint location : npcLocations.values())
		{
			if (locationCount.containsKey(location))
			{
				locationCount.put(location, locationCount.get(location) + 1);
			}
			else
			{
				locationCount.put(location, 1);
			}
		}
		for (NPC npc : client.getNpcs())
		{
			final String npcName = npc.getName();

			if (npcName == null || !selectedNPCs.contains(npcName.toLowerCase()))
			{
				continue;
			}

			final WanderingNPC wnpc = wanderingNPCs.get(npc.getIndex());

			if (wnpc == null)
			{
				continue;
			}

			if(config.showOverlay())
			{
				double monsterHP = ((double) npc.getHealthRatio() / (double) npc.getHealthScale() * 100);
				if (!npc.isDead())
				{
					if((npc.getHealthRatio()/npc.getHealthScale() != 1))
					{
						wnpc.setCurrentHp(monsterHP);
						wnpc.setCurrentLocation(npc.getWorldLocation());
						wnpc.setDead(false);
						if (locationCount.containsKey(wnpc.getCurrentLocation()))
						{
							wnpc.setOffset(locationCount.get(wnpc.getCurrentLocation())-1);
							locationCount.put(wnpc.getCurrentLocation(), locationCount.get(wnpc.getCurrentLocation()) - 1);
						}
					}
				}
				else if(npc.isDead())
				{
					wnpc.setCurrentHp(0);
					if(config.hideDeath())
					{
						wnpc.setDead(true);
					}
				}

				npcLocations.put(wnpc.getNpcIndex(), wnpc.getCurrentLocation());
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		selectedNPCs = getSelectedNPCs();
		rebuildAllNpcs();
	}

	@VisibleForTesting
	List<String> getSelectedNPCs()
	{
		String configNPCs = config.npcToShowHp().toLowerCase();
		if (configNPCs.isEmpty())
		{
			return Collections.emptyList();
		}

		return Text.fromCSV(configNPCs);
	}

	private void rebuildAllNpcs()
	{
		wanderingNPCs.clear();

		if (client.getGameState() != GameState.LOGGED_IN &&
			client.getGameState() != GameState.LOADING)
		{
			// NPCs are still in the client after logging out, ignore them
			return;
		}

		for (NPC npc : client.getNpcs())
		{
			final String npcName = npc.getName();

			if (npcName == null || !selectedNPCs.contains(npcName.toLowerCase()))
			{
				continue;
			}

			wanderingNPCs.putIfAbsent(npc.getIndex(), new WanderingNPC(npc));
			npcLocations.put(npc.getIndex(), npc.getWorldLocation());
		}
	}
}

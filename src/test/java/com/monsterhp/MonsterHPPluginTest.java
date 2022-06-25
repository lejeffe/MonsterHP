package com.monsterhp;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MonsterHPPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MonsterHPPlugin.class);
		RuneLite.main(args);
	}
}
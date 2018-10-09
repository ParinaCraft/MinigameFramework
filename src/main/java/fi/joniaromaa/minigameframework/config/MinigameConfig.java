package fi.joniaromaa.minigameframework.config;

import java.util.Collection;
import java.util.Random;

import fi.joniaromaa.minigameframework.game.AbstractMinigame;

public abstract class MinigameConfig
{
	public abstract int getGameType();
	public abstract int getTeamSize();
	
	public abstract Collection<MinigameMapConfig> getMapConfigs();
	public MinigameMapConfig getRandomMapConfig()
	{
		MinigameMapConfig[] maps = this.getMapConfigs().toArray(new MinigameMapConfig[0]);
		if (maps.length > 0)
		{
			Random random = new Random();
			return maps[random.nextInt(maps.length)];
		}
		else
		{
			return null;
		}
	}
	
	//TODO: Do something to this
	public abstract Class<? extends AbstractMinigame<?, ?>> getMinigameClass();
}

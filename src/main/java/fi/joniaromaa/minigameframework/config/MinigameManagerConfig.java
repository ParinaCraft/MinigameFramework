package fi.joniaromaa.minigameframework.config;

import fi.joniaromaa.minigameframework.game.AbstractPreMinigame;

public abstract class MinigameManagerConfig
{
	public abstract int getConcurrentGameLimit();

	//TODO: Do something to this
	public abstract Class<? extends AbstractPreMinigame> getPreMinigameClass();
	
	public abstract MinigameConfig getMinigameConfig();
}

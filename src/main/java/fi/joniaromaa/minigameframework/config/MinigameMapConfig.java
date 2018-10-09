package fi.joniaromaa.minigameframework.config;

import java.util.Map;

public abstract class MinigameMapConfig
{
	public abstract int getPlayerLimit();
	public abstract Map<Integer, Integer> getPreGameStartTimes();
}

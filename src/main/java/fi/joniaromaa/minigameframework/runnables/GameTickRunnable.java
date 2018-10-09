package fi.joniaromaa.minigameframework.runnables;

import fi.joniaromaa.minigameframework.MinigamePlugin;

public class GameTickRunnable implements Runnable
{
	@Override
	public void run()
	{
		MinigamePlugin.getPlugin().getGameManager().onTick();
	}
}

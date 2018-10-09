package fi.joniaromaa.minigameframework.runnables;

import fi.joniaromaa.minigameframework.MinigamePlugin;

public class MonitorRunnable implements Runnable
{
	@Override
	public void run()
	{
		MinigamePlugin.getPlugin().getGameManager().onMonitor();
	}
}

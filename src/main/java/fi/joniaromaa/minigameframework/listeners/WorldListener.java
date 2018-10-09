package fi.joniaromaa.minigameframework.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import fi.joniaromaa.minigameframework.utils.MinigameWorldUtils;
import fi.joniaromaa.minigameframework.world.MinigameWorldData;
import fi.joniaromaa.minigameframework.world.WorldWeatherType;

public class WorldListener implements Listener
{
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChunkLoadEvent(ChunkLoadEvent event)
	{
		if (event.getWorld().hasMetadata("MinigamePlugin: DontLoadChunks"))
		{
			event.getChunk().unload(false, false);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChunkUnloadEvent(ChunkUnloadEvent event)
	{
		if (event.getWorld().hasMetadata("MinigamePlugin: DontLoadChunks"))
		{
			event.setCancelled(true);
			event.getChunk().unload(false, false);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onWeatherChangeEvent(WeatherChangeEvent event)
	{
		World world = event.getWorld();

		MinigameWorldData data = MinigameWorldUtils.getWorldData(world);
		if (data != null)
		{
			if (data.getWeatherType() != WorldWeatherType.DEFAULT)
			{
				event.setCancelled(true);
			}
		}
	}
}

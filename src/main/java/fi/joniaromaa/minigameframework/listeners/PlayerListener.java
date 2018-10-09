package fi.joniaromaa.minigameframework.listeners;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.game.AbstractPreMinigame;
import fi.joniaromaa.minigameframework.user.dataset.UserPreferedMinigameTeamDataStorage;
import fi.joniaromaa.parinacorelibrary.api.ParinaCore;
import fi.joniaromaa.parinacorelibrary.api.user.User;

public class PlayerListener implements Listener
{
	@EventHandler(priority = EventPriority.MONITOR)
	public void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event)
	{
		User user = ParinaCore.getApi().getUserManager().getUser(event.getUniqueId());
		if (user != null)
		{
			user.removeDataStorage(UserPreferedMinigameTeamDataStorage.class);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLoginEvent(PlayerLoginEvent event)
	{
		PlayerLoginEvent.Result result = event.getResult();
		if (result == PlayerLoginEvent.Result.ALLOWED)
		{
			Player player = event.getPlayer();
			
			MinigamePlugin.getPlugin().getGameManager().onPlayerLogin(player).ifPresent((m) ->
			{
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m);
			});
		}
	}

	//No PlayerQuitEvent is called
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLoginEventMonitor(PlayerLoginEvent event)
	{
		PlayerLoginEvent.Result result = event.getResult();
		if (result != PlayerLoginEvent.Result.ALLOWED)
		{
			Player player = event.getPlayer();
			
			MinigamePlugin.getPlugin().getGameManager().onPlayerQuit(player);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerSpawnLocationEvent(PlayerSpawnLocationEvent event)
	{
		Player player = event.getPlayer();
		
		Optional<Minigame> minigame = MinigamePlugin.getPlugin().getGameManager().getMinigame(player);
		if (minigame.isPresent())
		{
			minigame.get().onPlayerSpawn(player).ifPresent((l) ->
			{
				event.setSpawnLocation(l);
			});
		}
		else
		{
			player.kickPlayer("Unable to find the game");
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoinEvent(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		
		Optional<Minigame> minigame = MinigamePlugin.getPlugin().getGameManager().getMinigame(player);
		if (minigame.isPresent())
		{
			minigame.get().onPlayerJoin(player);
		}
		else
		{
			player.kickPlayer("Unable to find the game");
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuitEvent(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		
		MinigamePlugin.getPlugin().getGameManager().onPlayerQuit(player);
		
		User user = ParinaCore.getApi().getUserManager().getUser(player.getUniqueId());
		if (user != null)
		{
			user.removeDataStorage(UserPreferedMinigameTeamDataStorage.class);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerInteractEvent(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		MinigamePlugin.getPlugin().getGameManager().getMinigame(player).ifPresent((m) ->
		{
			if (m instanceof AbstractMinigame)
			{
				if (((AbstractMinigame<?, ?>)m).isSpectator(player))
				{
					event.setCancelled(true);
				}
			}
			else if (m instanceof AbstractPreMinigame)
			{
				event.setCancelled(true);
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();
		
		MinigamePlugin.getPlugin().getGameManager().getMinigame(player).ifPresent((m) ->
		{
			if (m instanceof AbstractMinigame)
			{
				if (((AbstractMinigame<?, ?>)m).isSpectator(player))
				{
					event.setCancelled(true);
				}
			}
			else if (m instanceof AbstractPreMinigame)
			{
				event.setCancelled(true);
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerDropItemEvent(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		
		MinigamePlugin.getPlugin().getGameManager().getMinigame(player).ifPresent((m) ->
		{
			if (m instanceof AbstractMinigame)
			{
				if (((AbstractMinigame<?, ?>)m).isSpectator(player))
				{
					event.setCancelled(true);
				}
			}
			else if (m instanceof AbstractPreMinigame)
			{
				event.setCancelled(true);
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerPickupItemEvent(PlayerPickupItemEvent event)
	{
		Player player = event.getPlayer();
		
		MinigamePlugin.getPlugin().getGameManager().getMinigame(player).ifPresent((m) ->
		{
			if (m instanceof AbstractMinigame)
			{
				if (((AbstractMinigame<?, ?>)m).isSpectator(player))
				{
					event.setCancelled(true);
				}
			}
			else if (m instanceof AbstractPreMinigame)
			{
				event.setCancelled(true);
			}
		});
	}
}

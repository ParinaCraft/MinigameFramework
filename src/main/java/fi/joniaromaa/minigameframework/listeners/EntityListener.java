package fi.joniaromaa.minigameframework.listeners;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.game.AbstractPreMinigame;
import fi.joniaromaa.minigameframework.player.AbstractMinigamePlayer;

public class EntityListener implements Listener
{
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFoodLevelChangeEvent(FoodLevelChangeEvent event)
	{
		Entity entity = event.getEntity();
		if (entity instanceof Player)
		{
			Player player = (Player)entity;
			MinigamePlugin.getPlugin().getGameManager().getMinigame(player).ifPresent((m) ->
			{
				if ((m instanceof AbstractPreMinigame) || (m instanceof AbstractMinigame && ((AbstractMinigame<?, ?>)m).isSpectator(player)))
				{
					event.setCancelled(true);
					
					((CraftHumanEntity)player).getHandle().getFoodData().eat(20, 20); //Set food level and saturation
				}
			});
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamageEvent(EntityDamageEvent event)
	{
		Entity entity = event.getEntity();
		if (entity instanceof Player)
		{
			Player player = (Player)entity;
			
			MinigamePlugin.getPlugin().getGameManager().getMinigame(player).ifPresent((m) ->
			{
				if (m instanceof AbstractMinigame)
				{
					AbstractMinigame<?, ?> minigame = (AbstractMinigame<?, ?>)m;
					if (minigame.isSpectator(player))
					{
						event.setCancelled(true);
					}
					else
					{
						AbstractMinigamePlayer<?> minigamePlayer = minigame.getMinigamePlayer(player);
						if (minigamePlayer != null)
						{
							minigamePlayer.onEntityDamageEvent(event);
							if (player.getHealth() - event.getFinalDamage() <= 0.0)
							{
								event.setCancelled(true); //Prevent killing the player
								
								minigamePlayer.onDied();
							}
						}
					}
				}
				else if (m instanceof AbstractPreMinigame)
				{
					event.setCancelled(true);
				}
			});
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event)
	{
		Entity damager = event.getDamager();
		if (damager instanceof Player)
		{
			Player damagerPlayer = (Player)damager;
			
			MinigamePlugin.getPlugin().getGameManager().getMinigame(damagerPlayer).ifPresent((m) ->
			{
				if (m instanceof AbstractMinigame)
				{
					if (((AbstractMinigame<?, ?>)m).isSpectator(damagerPlayer))
					{
						event.setCancelled(true);
					}
				}
			});
		}
	}
}

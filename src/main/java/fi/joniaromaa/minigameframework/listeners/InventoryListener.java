package fi.joniaromaa.minigameframework.listeners;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.game.AbstractPreMinigame;

public class InventoryListener implements Listener
{
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryClickEvent(InventoryClickEvent event)
	{
		HumanEntity human = event.getWhoClicked();
		if (human instanceof Player)
		{
			Player player = (Player)human;
			
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
}

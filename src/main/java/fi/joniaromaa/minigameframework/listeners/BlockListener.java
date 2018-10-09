package fi.joniaromaa.minigameframework.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.utils.BlockUtils;
import fi.joniaromaa.minigameframework.utils.MinigameWorldUtils;
import fi.joniaromaa.minigameframework.world.BlockBreakContractTypeType;
import fi.joniaromaa.minigameframework.world.MinigameWorldData;

public class BlockListener implements Listener
{
	private final MinigamePlugin plugin;
	
	public BlockListener(MinigamePlugin plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlaceEvent(BlockPlaceEvent event)
	{
		Block block = event.getBlock();
		
		MinigameWorldData data = MinigameWorldUtils.getWorldData(block.getWorld());
		if (data != null)
		{
			if (!data.isAllowBlockPlace())
			{
				event.setCancelled(true);
				return;
			}
			
			Player player = event.getPlayer();
			
			Minigame m = MinigamePlugin.getPlugin().getGameManager().getMinigame(player).orElse(null);
			if (m instanceof AbstractMinigame)
			{
				if (((AbstractMinigame<?, ?>)m).isSpectator(player))
				{
					event.setCancelled(true);
					return;
				}
			}
			
			if (data.getBlockBreakContractType() == BlockBreakContractTypeType.USER_PLACED)
			{
				block.setMetadata(MinigameWorldUtils.USER_PLACED_BLOCK_METADATA_KEY, new FixedMetadataValue(this.plugin, true));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreakEvent(BlockBreakEvent event)
	{
		Block block = event.getBlock();
		
		MinigameWorldData data = MinigameWorldUtils.getWorldData(block.getWorld());
		if (data != null)
		{
			Result result = BlockUtils.handleBlockBreakContract(event);
			if (result == Result.DENY)
			{
				event.setCancelled(true);
				return;
			}
			else if (result == Result.DEFAULT)
			{
				if (data.getBlockBreakContractType() == BlockBreakContractTypeType.USER_PLACED)
				{
					if (!block.hasMetadata(MinigameWorldUtils.USER_PLACED_BLOCK_METADATA_KEY))
					{
						event.setCancelled(true);
						return;
					}
				}
				else if (data.getBlockBreakContractType() == BlockBreakContractTypeType.NONE)
				{
					event.setCancelled(true);
					return;
				}
			}
			
			Player player = event.getPlayer();
			
			Minigame m = MinigamePlugin.getPlugin().getGameManager().getMinigame(player).orElse(null);
			if (m instanceof AbstractMinigame)
			{
				if (((AbstractMinigame<?, ?>)m).isSpectator(player))
				{
					event.setCancelled(true);
					return;
				}
			}
		}
	}
}

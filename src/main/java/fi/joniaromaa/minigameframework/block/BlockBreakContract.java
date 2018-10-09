package fi.joniaromaa.minigameframework.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;

@FunctionalInterface
public interface BlockBreakContract
{
	public Result onBlockBreak(Block block, Player player);
}

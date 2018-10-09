package fi.joniaromaa.minigameframework.utils;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import fi.joniaromaa.minigameframework.block.BlockBreakContract;

public class BlockUtils
{
	public static final String BLOCK_BREAK_CONTRACT_METDATA_KEY = "MinigamePlugin: BlockBreakContract";
	
	public static void addBlockBreakContract(Block block, BlockBreakContract contract, Plugin plugin)
	{
		block.setMetadata(BlockUtils.BLOCK_BREAK_CONTRACT_METDATA_KEY, new FixedMetadataValue(plugin, contract));
	}
	
	public static Result handleBlockBreakContract(BlockBreakEvent event)
	{
		Block block = event.getBlock();
		
		List<MetadataValue> meta = block.getMetadata(BlockUtils.BLOCK_BREAK_CONTRACT_METDATA_KEY);
		for(MetadataValue metaValue : meta)
		{
			Object value = metaValue.value();
			if (value instanceof BlockBreakContract)
			{
				Result result = ((BlockBreakContract)value).onBlockBreak(block, event.getPlayer());
				if (result != Result.DEFAULT)
				{
					return result;
				}
			}
		}
		
		return Result.DEFAULT;
	}
}

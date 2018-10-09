package fi.joniaromaa.minigameframework.utils;

import org.bukkit.World;
import org.bukkit.metadata.MetadataValue;

import fi.joniaromaa.minigameframework.world.MinigameWorldData;

public class MinigameWorldUtils
{
	public static final String METADATA_KEY = "MinigamePlugin: MinigameWorld";
	public static final String USER_PLACED_BLOCK_METADATA_KEY = "MinigamePlugin: UserPlacedBlock";
	
	public static MinigameWorldData getWorldData(World world)
	{
		if (world.hasMetadata(MinigameWorldUtils.METADATA_KEY))
		{
			for(MetadataValue metadata : world.getMetadata(MinigameWorldUtils.METADATA_KEY))
			{
				Object metadataValue = metadata.value();
				if (metadataValue instanceof MinigameWorldData)
				{
					return (MinigameWorldData)metadataValue;
				}
			}
		}
		
		return null;
	}
}

package fi.joniaromaa.minigameframework.builder;

import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;

import fi.joniaromaa.minigameframework.nms.NoChunkSaveProviderServer;
import fi.joniaromaa.minigameframework.utils.MinigameWorldUtils;
import fi.joniaromaa.minigameframework.world.BlockBreakContractTypeType;
import fi.joniaromaa.minigameframework.world.MinigameWorldData;
import fi.joniaromaa.minigameframework.world.WorldWeatherType;
import net.minecraft.server.v1_8_R3.WorldServer;

public class MinigameWorldBuilder
{
	//Basic world stuff
	private String worldName;
	private Environment environment;
	private WorldType worldType;
	
	//Basic extra stuff
	private boolean generateStructures;
	private String generatorSettings;
	
	//Basic settings
	private Difficulty difficulty;
	
	//Gamerule shortcuts
	private boolean doDaylightCycle;
	private boolean doTileDrops;
	private boolean doFireTick;
	
	//My own stuff
	private boolean saveChunks;
	private WorldWeatherType weatherType;
	
	private boolean allowBlockPlace;
	private BlockBreakContractTypeType blockBreakContractType;
	
	private MinigameWorldBuilder()
	{
		this.environment = Environment.NORMAL;
		this.worldType = WorldType.NORMAL;
		
		this.generateStructures = true;
		this.generatorSettings = null;
		
		this.difficulty = Difficulty.NORMAL;
		
		this.doDaylightCycle = true;
		this.doTileDrops = true;
		this.doFireTick = true;
		
		this.saveChunks = true;
		this.weatherType = WorldWeatherType.DEFAULT;
		
		this.allowBlockPlace = true;
		this.blockBreakContractType = BlockBreakContractTypeType.WORLD;
	}
	
	public MinigameWorldBuilder worldName(String worldName)
	{
		this.worldName = worldName;
		
		return this;
	}
	
	public MinigameWorldBuilder envivorment(Environment envivorment)
	{
		this.environment = envivorment;
		
		return this;
	}
	
	public MinigameWorldBuilder worldType(WorldType worldType)
	{
		this.worldType = worldType;
		
		return this;
	}
	
	public MinigameWorldBuilder generateStructures(boolean generateStructures)
	{
		this.generateStructures = generateStructures;
		
		return this;
	}
	
	public MinigameWorldBuilder generatorSettings(String generatorSettings)
	{
		this.generatorSettings = generatorSettings;
		
		return this;
	}
	
	public MinigameWorldBuilder doDaylightCycle(boolean doDaylightCycle)
	{
		this.doDaylightCycle = doDaylightCycle;
		
		return this;
	}
	
	public MinigameWorldBuilder doTileDrops(boolean doTileDrops)
	{
		this.doTileDrops = doTileDrops;
		
		return this;
	}
	
	public MinigameWorldBuilder doFireTick(boolean doFireTick)
	{
		this.doFireTick = doFireTick;
		
		return this;
	}
	
	public MinigameWorldBuilder saveChunks(boolean saveChunks)
	{
		this.saveChunks = saveChunks;
		
		return this;
	}
	
	public MinigameWorldBuilder setWeatherType(WorldWeatherType weatherType)
	{
		this.weatherType = weatherType;
		
		return this;
	}
	
	public MinigameWorldBuilder voidOnlyGenerator()
	{
		return this.voidOnlyGenerator(Environment.NORMAL);
	}
	
	public MinigameWorldBuilder voidOnlyGenerator(Environment envivorment)
	{
		this.envivorment(envivorment);
		this.generateStructures(false);
		this.worldType(WorldType.FLAT);
		this.generatorSettings("2;0;1");
		
		return this;
	}
	
	public MinigameWorldBuilder allowBlockPlace(boolean allowBlockPlace)
	{
		this.allowBlockPlace = allowBlockPlace;
		
		return this;
	}
	
	public MinigameWorldBuilder blockBreakContractType(BlockBreakContractTypeType blockBreakContractType)
	{
		this.blockBreakContractType = blockBreakContractType;
		
		return this;
	}
	
	public World build(Plugin plugin)
	{
		Preconditions.checkNotNull(this.worldName, "You need to assign world name");
		
		World world = WorldCreator.name(this.worldName)
				.environment(this.environment)
				.generateStructures(this.generateStructures)
				.type(this.worldType)
				.generatorSettings(this.generatorSettings)
				.createWorld();
		
		WorldServer nmsWorld = ((CraftWorld)world).getHandle();
		
		if (!this.saveChunks)
		{
			world.setAutoSave(false);
			
			try
			{
				//Warp the current provider around the no chunk save provider
				nmsWorld.chunkProviderServer = new NoChunkSaveProviderServer(nmsWorld.chunkProviderServer);
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				throw new RuntimeException("Failed to set no chunk save provider", e);
			}
		}
		
		MinigameWorldData minigameWorld = new MinigameWorldData(world);
		minigameWorld.setWeatherType(this.weatherType);
		minigameWorld.setAllowBlockPlace(this.allowBlockPlace);
		minigameWorld.setBlockBreakContractType(this.blockBreakContractType);
		
		world.setMetadata(MinigameWorldUtils.METADATA_KEY, new FixedMetadataValue(plugin, minigameWorld));
		world.setDifficulty(this.difficulty);
		world.setGameRuleValue("doDaylightCycle", Boolean.toString(this.doDaylightCycle));
		world.setGameRuleValue("doTileDrops", Boolean.toString(this.doTileDrops));
		world.setGameRuleValue("doFireTick", Boolean.toString(this.doFireTick));
		
		return world;
	}
	
	public static MinigameWorldBuilder builder()
	{
		return new MinigameWorldBuilder();
	}
}

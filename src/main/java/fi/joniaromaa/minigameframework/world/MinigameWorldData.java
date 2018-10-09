package fi.joniaromaa.minigameframework.world;

import org.bukkit.World;

import lombok.Getter;
import lombok.Setter;

public class MinigameWorldData
{
	@Getter private final World world;
	
	@Getter private WorldWeatherType weatherType;
	
	@Getter @Setter private boolean allowBlockPlace;
	@Getter @Setter private BlockBreakContractTypeType blockBreakContractType;
	
	public MinigameWorldData(World world)
	{
		this.world = world;
	}

	public void setWeatherType(WorldWeatherType weatherType)
	{
		if (weatherType == WorldWeatherType.CLEAR)
		{
			this.world.setStorm(false);
			this.world.setWeatherDuration(Integer.MAX_VALUE);
			this.world.setThundering(false);
			this.world.setThunderDuration(Integer.MAX_VALUE);
		}
		else if (weatherType == WorldWeatherType.STORM)
		{
			this.world.setStorm(true);
			this.world.setWeatherDuration(Integer.MAX_VALUE);
			this.world.setThundering(false);
			this.world.setThunderDuration(Integer.MAX_VALUE);
		}
		else if (weatherType == WorldWeatherType.THUNDER)
		{
			this.world.setStorm(true);
			this.world.setWeatherDuration(Integer.MAX_VALUE);
			this.world.setThundering(true);
			this.world.setThunderDuration(Integer.MAX_VALUE);
		}
		else
		{
			//Do randomization or something idk
		}
	}
}

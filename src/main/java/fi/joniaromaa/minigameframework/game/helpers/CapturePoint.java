package fi.joniaromaa.minigameframework.game.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Wool;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.event.game.TeamCapturePointCapEvent;
import fi.joniaromaa.minigameframework.event.game.TeamCapturePointNormalizedEvent;
import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.player.AbstractMinigamePlayer;
import fi.joniaromaa.minigameframework.team.AbstractMinigameTeam;
import fi.joniaromaa.minigameframework.utils.EntityUtils;
import fi.joniaromaa.parinacorelibrary.bukkit.utils.BlockUtils;
import fi.joniaromaa.parinacorelibrary.bukkit.utils.FireworkUtils;
import fi.joniaromaa.parinacorelibrary.bukkit.utils.ParticleUtils;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

public class CapturePoint
{
	private final AbstractMinigame<?, ?> minigame;
	private final Location location;
	private final int radius;
	@Getter private final int captureTime;
	
	private List<Block> woolBlocks;
	private int randomSeed;
	
	@Getter private AbstractMinigameTeam<?> teamHolding;
	@Getter private int holdingTime;
	
	@Getter private AbstractMinigameTeam<?> capturingTeam;
	@Getter private int capturingTime;
	
	private ArmorStand capturePointHolderArmorStand;
	
	public CapturePoint(AbstractMinigame<?, ?> minigame, Location location, int radius, int captureTime)
	{
		this.minigame = minigame;
		this.location = location;
		this.radius = radius;
		this.captureTime = captureTime;
		
		this.newRandomSeed();

		this.woolBlocks = this.getWoolBlocks();
		
		EntityUtils.addHologram(location.clone().add(0, 3, 0), ChatColor.AQUA + ChatColor.BOLD.toString() + "Capture" + ChatColor.GREEN + ChatColor.BOLD.toString() + " Point");
		
		this.capturePointHolderArmorStand = EntityUtils.addHologram(location.clone().add(0, 2.8, 0), ChatColor.WHITE + ChatColor.BOLD.toString() + "None");

		this.cap(DyeColor.WHITE);
	}
	
	private void newRandomSeed()
	{
		this.randomSeed = (int)(Math.random() * Integer.MAX_VALUE);
	}
	
	public void tick()
	{
		World world = this.location.getWorld();
		
		Map<AbstractMinigameTeam<?>, List<AbstractMinigamePlayer<?>>> capturing = new HashMap<>();
		
		Collection<Entity> nearbyEntities = world.getNearbyEntities(this.location, this.radius, this.radius, this.radius);
		for(Entity entity : nearbyEntities)
		{
			if (entity instanceof Player && !this.minigame.isSpectator(entity.getUniqueId()))
			{
				AbstractMinigamePlayer<?> player = this.minigame.getMinigamePlayer(entity.getUniqueId());
				AbstractMinigameTeam<?> playerTeam = player.getTeam();
				
				List<AbstractMinigamePlayer<?>> capturingPlayers = capturing.get(playerTeam);
				if (capturingPlayers == null)
				{
					capturing.put(playerTeam, capturingPlayers = new ArrayList<>());
				}
				
				capturingPlayers.add(player);
			}
		}
		
		if (this.teamHolding != null)
		{
			this.holdingTime++;
		}
		
		if (capturing.size() == 0 || (capturing.size() == 1 && capturing.containsKey(this.teamHolding))) //None capturing, decrease any "capture" time
		{
			if (this.capturingTime > 0)
			{
				if (--this.capturingTime == 0)
				{
					this.capturingTeam = null;
					
					this.newRandomSeed();
				}
				
				this.cap(this.capturingTeam == null ? DyeColor.WHITE : this.capturingTeam.getColor(), this.capturingTime / (float)this.captureTime, true);
			}
			else
			{
				this.capturingTeam = null;
			}
		}
		else
		{
			if (this.teamHolding != null && !capturing.containsKey(this.teamHolding)) //Nautralizing, no opposite teams, combine all
			{
				//Every other team counts here
				List<AbstractMinigamePlayer<?>> capturingPlayers = capturing.values().stream()
						.flatMap((l) -> l.stream())
						.collect(Collectors.toList());
				
				if (capturingPlayers.size() > 0)
				{
					this.capturingTime = Math.min(this.captureTime, this.capturingTime + capturingPlayers.size());
					if (this.capturingTime == this.captureTime)
					{
						TeamCapturePointNormalizedEvent event = new TeamCapturePointNormalizedEvent(this.minigame, this.teamHolding, capturingPlayers);
						MinigamePlugin.getPlugin().getServer().getPluginManager().callEvent(event);
						
						if (!event.isCancelled())
						{
							this.capturingTime = 0;
							this.holdingTime = 0;
							
							this.teamHolding = null;
							
							this.cap(DyeColor.WHITE, 1.0F, true);
							
							this.newRandomSeed();
						}
					}
					else
					{
						this.cap(DyeColor.WHITE, this.capturingTime / (float)this.captureTime, true);
					}
				}
			}
			else if (this.teamHolding == null) //Nautralized, someone can start capping
			{
				if (capturing.size() == 1) //Only one team can start capping at once
				{
					Entry<AbstractMinigameTeam<?>, List<AbstractMinigamePlayer<?>>> entry = capturing.entrySet().iterator().next();
					
					AbstractMinigameTeam<?> team = entry.getKey();
					if (this.capturingTeam == null) //Nobody capping, give to this team
					{
						this.capturingTeam = team;
					}
					
					List<AbstractMinigamePlayer<?>> capturingPlayers = entry.getValue();
					if (this.capturingTeam == team) //Same team, increase the time
					{
						this.capturingTime = Math.min(this.captureTime, this.capturingTime + capturingPlayers.size());
						if (this.capturingTime == this.captureTime)
						{
							TeamCapturePointCapEvent event = new TeamCapturePointCapEvent(this.minigame, team, capturingPlayers);
							MinigamePlugin.getPlugin().getServer().getPluginManager().callEvent(event);
							
							if (!event.isCancelled())
							{
								this.capturingTime = 0;
								this.holdingTime = 0;
								
								this.capturingTeam = null;
								this.teamHolding = event.getTeam();
	
								this.cap(team.getColor(), 1.0F, true);
								
								this.newRandomSeed();
							}
						}
					}
					else //Other time, decrease the time
					{
						this.capturingTime = Math.max(0, this.capturingTime - capturingPlayers.size());
						if (this.capturingTime == 0)
						{
							this.capturingTeam = team;
							
							this.newRandomSeed();
						}
					}
					
					this.cap(this.capturingTeam == null ? this.teamHolding == null ? DyeColor.WHITE : this.teamHolding.getColor() : this.capturingTeam.getColor(), this.capturingTime / (float)this.captureTime, true);
				}
			}
		}
	}
	
	private void cap(DyeColor color)
	{
		this.cap(color, 1.0F, false);
	}
	
	private void cap(DyeColor color, float progress, boolean effect)
	{
		if (this.woolBlocks == null)
		{
			this.woolBlocks = this.getWoolBlocks();
		}
		
		World world = this.location.getWorld();
		
		List<Block> wools = this.woolBlocks;
		if (progress != 1.0F)
		{
			wools = new ArrayList<>(wools);
			
			Collections.shuffle(wools, new Random(this.randomSeed));
			
			Wool teamWool_ = new Wool(this.teamHolding == null ? DyeColor.WHITE : this.teamHolding.getColor());
			
			int progressAmount = (int)Math.ceil(progress * wools.size());
			while (wools.size() > progressAmount)
			{
				Block block = wools.remove(wools.size() - 1); //Remove from the end so we can process forward
				
				if (effect && !block.getState().getData().equals(teamWool_))
				{
					block.getWorld().playSound(block.getLocation(), Sound.DIG_WOOL, 1F, 0);
				}
				
				BlockUtils.setBlockData(block, teamWool_);
			}
		}
		else
		{
			if (this.teamHolding == null)
			{
				this.capturePointHolderArmorStand.setCustomName(ChatColor.WHITE + ChatColor.BOLD.toString() + "None");
			}
			else
			{
				this.capturePointHolderArmorStand.setCustomName(ChatColor.valueOf(color.name()) + ChatColor.BOLD.toString() + this.teamHolding.getName());
			}
		}

		Wool teamWool = new Wool(color);
		for(Block block : wools)
		{
			if (effect && !block.getState().getData().equals(teamWool))
			{
				block.getWorld().playSound(block.getLocation(), Sound.DIG_WOOL, 1F, 0);
			}
			
    		BlockUtils.setBlockData(block, teamWool);
    		
    		ParticleUtils.tileBreakParticle(world, block.getLocation().add(0.5, 0.8, 0.5), teamWool);
		}
		
		this.location.clone().subtract(0, 1, 0).getBlock().setType(Material.BEACON);
		
		if (effect && progress == 1.0F)
		{
			FireworkUtils.fireworkExplodeEffect(this.location.clone().add(0, 2.9, 0), FireworkEffect.builder().with(Type.BURST).withColor(this.teamHolding == null ? Color.WHITE : this.teamHolding.getColor().getColor()).build());
		}
	}
	
	private List<Block> getWoolBlocks()
	{
		Location location = this.location.clone().subtract(0, 1, 0);
		
		List<Block> locations = new ArrayList<>((int)Math.pow(this.radius, 4));
		for (float x = -this.radius; x <= this.radius; x++)
		{
            for (float z = -this.radius; z <= this.radius; z++)
            {
            	if ((x == 0 && z == 0) || (x == this.radius && z == this.radius) || (x == -this.radius && z == -this.radius) || (x == this.radius && z == -this.radius) || (x == -this.radius && z == this.radius))
            	{
            		continue; //Skip the corners
            	}
            	
            	location.add(x, 0, z);
            	
            	locations.add(location.getBlock());
            	
            	location.subtract(x, 0, z);
            }
		}
		
		return locations;
	}
}

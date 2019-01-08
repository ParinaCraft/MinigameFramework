package fi.joniaromaa.minigameframework.game.helpers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Weather;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

import fi.joniaromaa.minigameframework.player.AbstractMinigamePlayer;
import lombok.Getter;

public class CombatTracker
{
	@Getter private final AbstractMinigamePlayer<?> player;
	
	private List<CombatTrackerEntry> combatEvents;
	
	public CombatTracker(AbstractMinigamePlayer<?> player)
	{
		this.player = player;
		
		this.combatEvents = new ArrayList<>();
	}
	
	public void onEntityDamageEvent(EntityDamageEvent event)
	{
		int ticksLived = this.player.getBukkitPlayer().getTicksLived();
		
		if (event instanceof EntityDamageByEntityEvent)
		{
			this.onEntityDamageByEntityEvent((EntityDamageByEntityEvent)event);
		}
		else
		{
			DamageCause cause = event.getCause();
			double finalDamage = event.getFinalDamage();
			
			this.combatEvents.add(new CombatTrackerEntry(cause, finalDamage, ticksLived, null));
		}
		
		//Do little bit housekeeping
		Iterator<CombatTrackerEntry> iterator = this.combatEvents.iterator();
		while (iterator.hasNext())
		{
			CombatTrackerEntry entry = iterator.next();
			if (ticksLived - entry.getTick() > 200)
			{
				iterator.remove();
			}
		}
	}
	
	private void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event)
	{
		DamageCause cause = event.getCause();
		double finalDamage = event.getFinalDamage();
		
		Entity damager = event.getDamager();
		
		//Change damger if needed
		if (damager instanceof Projectile)
		{
			Projectile projectile = (Projectile)damager;
			ProjectileSource shooter = projectile.getShooter();
			if (shooter instanceof Entity)
			{
				damager = (Entity)shooter;
			}
		}
		
		this.combatEvents.add(new CombatTrackerEntry(cause, finalDamage, this.player.getBukkitPlayer().getTicksLived(), damager));
	}
	
	public String buildDeathMessage()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(this.player.getTeam().getChatColor())
		.append(this.player.getUser().getDisplayName())
		.append(ChatColor.GRAY)
		.append(" ");
		
		if (this.combatEvents.size() > 0)
		{
			CombatTrackerEntry entry = this.combatEvents.get(this.combatEvents.size() - 1); //The latest one
			switch(entry.getCause())
			{
				case VOID:
				{
					stringBuilder.append("putosi maailmasta");
					break;
				}
				case FIRE:
				{
					stringBuilder.append("paloi mustaksi makakraksi");
					break;
				}
				case ENTITY_ATTACK:
				{
					stringBuilder.append("sai nenään");
					break;
				}
				case PROJECTILE:
				{
					stringBuilder.append("sniputettiin");
					break;
				}
				case SUFFOCATION:
				{
					stringBuilder.append("tukehtui cringeen");
					break;
				}
				case CONTACT:
				{
					stringBuilder.append("aliarvioi kontaktin voiman");
					break;
				}
				case FALL:
				{
					stringBuilder.append("jalat antoivat periksi");
					break;
				}
				case MELTING:
				{
					stringBuilder.append("suli vedeksi");
					break;
				}
				case FIRE_TICK:
				{
					stringBuilder.append("epäonnistui juoksemaan tulta pakoon");
					break;
				}
				case LAVA:
				{
					stringBuilder.append("tuli kosketuksiin laavan kanssa");
					break;
				}
				case DROWNING:
				{
					stringBuilder.append("ei saanut happea");
					break;
				}
				case BLOCK_EXPLOSION:
				case ENTITY_EXPLOSION:
				{
					stringBuilder.append("ymmärsi räjähdyksen voiman");
					break;
				}
				case LIGHTNING:
				{
					stringBuilder.append("kuoli tyylillä");
					break;
				}
				case SUICIDE:
				{
					stringBuilder.append("halusi parempaan paikkaan");
					break;
				}
				case STARVATION:
				{
					stringBuilder.append("huomasi, että ravinto on tärkeää");
					break;
				}
				case POISON:
				case MAGIC:
				case WITHER:
				{
					stringBuilder.append("sai traagisen kuoleman");
					break;
				}
				case FALLING_BLOCK:
				{
					stringBuilder.append("ei huomannut väistää");
					break;
				}
				case THORNS:
				{
					stringBuilder.append("silmä silmästä, hammas hampaasta");
					break;
				}
				case CUSTOM:
				{
					stringBuilder.append("sai kokea kolmannen osapuolen raivon");
					break;
				}
				default:
					stringBuilder.append("menehtyi");
			}
			
			Entity damager = this.getLastDamager();
			if (damager != null)
			{
				if (damager instanceof Player)
				{
					Player damagerPlayer = (Player)damager;
					
					stringBuilder.append(" pelaajan ");
					
					AbstractMinigamePlayer<?> minigamePlayerDamager = this.getPlayer().getGame().getMinigamePlayer(damagerPlayer);
					if (minigamePlayerDamager != null)
					{
						stringBuilder.append(minigamePlayerDamager.getTeam().getChatColor())
							.append(minigamePlayerDamager.getUser().getDisplayName())
							.append(ChatColor.GRAY);
					}
					else
					{
						stringBuilder.append(damagerPlayer.getName());
					}
					
					stringBuilder.append(" (")
						.append(ChatColor.RED)
						.append(new DecimalFormat("#0.00").format(damagerPlayer.getHealth()))
						.append(" ❤")
						.append(ChatColor.GRAY)
						.append(')')
						.append(" ansiosta!");
				}
				else
				{
					if (!(damager instanceof Weather) && !(damager instanceof FallingBlock))
					{
						stringBuilder.append(" mobin ")
						.append(damager.getType().name())
						.append(" ansiosta!");
					}
				}
			}
			else
			{
				stringBuilder.append("!");
			}
		}
		else
		{
			stringBuilder.append("menehtyi!");
		}

		return stringBuilder.toString();
	}
	
	public Entity getLastDamager()
	{
		for(int i = this.combatEvents.size() - 1; i >= 0; i--)
		{
			CombatTrackerEntry entry = this.combatEvents.get(i);
			
			Entity damager = entry.getDamager();
			if (damager != null)
			{
				if (damager instanceof Weather || damager instanceof FallingBlock)
				{
					continue;
				}
				
				return entry.getDamager();
			}
		}
		
		return null;
	}
	
	public void clear()
	{
		this.combatEvents.clear();
	}
}

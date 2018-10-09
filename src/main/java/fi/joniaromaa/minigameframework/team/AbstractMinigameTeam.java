package fi.joniaromaa.minigameframework.team;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.DyeColor;
import org.bukkit.entity.Player;

import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.player.AbstractMinigamePlayer;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

public abstract class AbstractMinigameTeam<T extends AbstractMinigamePlayer<?>>
{
	@Getter private final AbstractMinigame<?, ?> game;
	
	@Getter private final String name;
	@Getter private final DyeColor color;
	
	private LinkedHashMap<UUID, T> teamMembers;
	
	public AbstractMinigameTeam(AbstractMinigame<?, ?> game, String name, DyeColor color)
	{
		this.game = game;
		
		this.name = name;
		this.color = color;
		
		this.teamMembers = new LinkedHashMap<>();
	}
	
	public void addTeamMember(T player)
	{
		this.teamMembers.put(player.getUniqueId(), player);
	}
	
	public boolean isAlive()
	{
		return this.teamMembers.values().stream().anyMatch((p) -> p.isAlive());
	}
	
	public boolean isTeamMember(Player player)
	{
		return this.isTeamMember(player.getUniqueId());
	}
	
	public boolean isTeamMember(UUID uniqueId)
	{
		return this.teamMembers.containsKey(uniqueId);
	}
	
	public int getAlivePlayersCount()
	{
		return (int)this.teamMembers.values().stream().filter((p) -> p.isAlive()).count();
	}
	
	public Collection<T> getTeamMembers()
	{
		return Collections.unmodifiableCollection(this.teamMembers.values());
	}
	
	public Collection<T> getAliveTeamMembers()
	{
		return Collections.unmodifiableCollection(this.teamMembers.values().stream().filter((p) -> p.isAlive()).collect(Collectors.toList()));
	}
	
	public ChatColor getChatColor()
	{
		return ChatColor.valueOf(this.color.name());
	}
}

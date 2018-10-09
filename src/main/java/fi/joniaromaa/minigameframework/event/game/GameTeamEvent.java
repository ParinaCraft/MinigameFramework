package fi.joniaromaa.minigameframework.event.game;

import org.bukkit.event.Event;

import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.team.AbstractMinigameTeam;
import lombok.Getter;

public abstract class GameTeamEvent extends Event
{
	@Getter private AbstractMinigame<?, ?> minigame;
	@Getter private AbstractMinigameTeam<?> team;
	
	public GameTeamEvent(AbstractMinigame<?, ?> minigame, AbstractMinigameTeam<?> team)
	{
		this.minigame = minigame;
		this.team = team;
	}
}

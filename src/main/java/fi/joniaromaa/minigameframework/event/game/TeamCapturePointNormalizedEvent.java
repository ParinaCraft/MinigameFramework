package fi.joniaromaa.minigameframework.event.game;

import java.util.Collections;
import java.util.List;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.player.AbstractMinigamePlayer;
import fi.joniaromaa.minigameframework.team.AbstractMinigameTeam;

public class TeamCapturePointNormalizedEvent extends GameTeamEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;
    private List<AbstractMinigamePlayer<?>> players;
    
    public TeamCapturePointNormalizedEvent(AbstractMinigame<?, ?> minigame, AbstractMinigameTeam<?> team, List<AbstractMinigamePlayer<?>> players)
    {
    	super(minigame, team);
    	
    	this.players = players;
    }
    
    public List<AbstractMinigamePlayer<?>> getPlayers()
    {
    	return Collections.unmodifiableList(this.players);
    }

	@Override
	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}

	@Override
	public boolean isCancelled()
	{
		return this.cancelled;
	}
    
	@Override
	public HandlerList getHandlers()
	{
		return TeamCapturePointNormalizedEvent.handlers;
	}

    public static HandlerList getHandlerList()
    {
        return TeamCapturePointNormalizedEvent.handlers;
    }
}

package fi.joniaromaa.minigameframework.player;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.game.helpers.CombatTracker;
import fi.joniaromaa.minigameframework.team.AbstractMinigameTeam;
import fi.joniaromaa.parinacorelibrary.api.user.dataset.UserDataStorage;
import lombok.Getter;
import lombok.Setter;

public abstract class AbstractMinigamePlayer<T extends AbstractMinigameTeam<?>> extends BukkitUser
{
	@Getter private final AbstractMinigame<?, ?> game;
	@Setter @Getter private T team;
	
	@Getter private final UserDataStorage stats;
	
	@Getter private CombatTracker combatTracker;
	@Getter @Setter private boolean alive;
	
	public AbstractMinigamePlayer(AbstractMinigame<?, ?> game, Player bukkitPlayer, UserDataStorage stats)
	{
		super(bukkitPlayer);
		
		this.game = game;
		
		this.stats = stats;
		
		this.combatTracker = new CombatTracker(this);
		this.alive = true;
	}

	public void onEntityDamageEvent(EntityDamageEvent event)
	{
		this.combatTracker.onEntityDamageEvent(event);
	}
	
	public void onDied()
	{
		this.game.sendMessage(this.combatTracker.buildDeathMessage());
		
		this.combatTracker.clear();
	}
}

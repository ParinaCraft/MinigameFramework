package fi.joniaromaa.minigameframework.game.helpers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CombatTrackerEntry
{
	private final DamageCause cause;
	private final double damage;
	private final int tick;
	private final Entity damager;
}

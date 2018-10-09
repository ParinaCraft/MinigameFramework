package fi.joniaromaa.minigameframework.api.game;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface Minigame
{
	Optional<String> onPlayerLogin(Player player);
	Optional<Location> onPlayerSpawn(Player player);
	void onPlayerJoin(Player player);
	void onPlayerQuit(Player player);
	
	void onTick();
	
	void onCriticalException(Throwable e);
}

package fi.joniaromaa.minigameframework.player;

import java.util.Locale;
import java.util.UUID;

import org.bukkit.entity.Player;

import fi.joniaromaa.parinacorelibrary.api.ParinaCore;
import fi.joniaromaa.parinacorelibrary.api.user.User;
import lombok.Getter;

//TODO: Fix static abuse
public class BukkitUser
{
	@Getter private final Player bukkitPlayer;
	@Getter private final User user;
	
	public BukkitUser(Player bukkitPlayer)
	{
		this.bukkitPlayer = bukkitPlayer;
		this.user = ParinaCore.getApi().getUserManager().getUser(this.getBukkitPlayer().getUniqueId()).orElse(null);
	}
	
	public boolean isLoaded()
	{
		return this.user != null;
	}
	
	public UUID getUniqueId()
	{
		return this.user.getUniqueId();
	}

	public void sendTranslatableMessage(String area, String key, Object ...params)
	{
		this.getBukkitPlayer().sendMessage(ParinaCore.getApi().getLanguageManager().getTranslation(this.user.getLocale().orElse(Locale.forLanguageTag(this.bukkitPlayer.spigot().getLocale().replaceAll( "_", "-" ))), area, key, params));
	}
}

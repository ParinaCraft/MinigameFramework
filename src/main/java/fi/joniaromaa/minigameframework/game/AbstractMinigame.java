package fi.joniaromaa.minigameframework.game;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.github.paperspigot.Title;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.config.MinigameConfig;
import fi.joniaromaa.minigameframework.config.MinigameMapConfig;
import fi.joniaromaa.minigameframework.net.NetworkManager;
import fi.joniaromaa.minigameframework.player.AbstractMinigamePlayer;
import fi.joniaromaa.minigameframework.player.BukkitUser;
import fi.joniaromaa.minigameframework.player.MinigameSpectatorPlayer;
import fi.joniaromaa.minigameframework.team.AbstractMinigameTeam;
import fi.joniaromaa.minigameframework.user.dataset.UserPreferedMinigameTeamDataStorage;
import fi.joniaromaa.parinacorelibrary.api.ParinaCore;
import fi.joniaromaa.parinacorelibrary.api.user.User;
import fi.joniaromaa.parinacorelibrary.api.user.dataset.UserDataStorage;
import fi.joniaromaa.parinacorelibrary.bukkit.scoreboard.ScoreboardManager;
import fi.joniaromaa.parinacorelibrary.common.storage.modules.UserStorageModule;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityPlayer;

public abstract class AbstractMinigame<T extends AbstractMinigameTeam<S>, S extends AbstractMinigamePlayer<T>> implements Minigame
{
	@Getter private int gameId;
	@Getter private final MinigameConfig config;
	@Getter private final MinigameMapConfig mapConfig;
	@Getter private final World world;
	
	@Getter private final boolean privateGame;
	
	protected LinkedHashMap<String, T> teams;
	protected Map<UUID, S> players;
	protected Map<UUID, UserDataStorage> stats; //These are hold even if player dies or disconnects
	
	private Map<UUID, MinigameSpectatorPlayer> spectators;

	private NetworkManager networkManager;
	@Getter protected ScoreboardManager scoreboardManager;
	
	public AbstractMinigame(int gameId, MinigameConfig config, MinigameMapConfig mapConfig, World world, Collection<BukkitUser> users, boolean privateGame)
	{
		this.gameId = gameId;
		this.config = config;
		this.mapConfig = mapConfig;
		this.world = world;
		
		this.privateGame = privateGame;
		
		this.teams = new LinkedHashMap<>();
		this.players = new LinkedHashMap<>();
		this.stats = new LinkedHashMap<>();
		
		this.sortToTeams(users);
		
		this.spectators = new HashMap<UUID, MinigameSpectatorPlayer>();
		
		this.networkManager = new NetworkManager(this);
	}
	
	protected void sortToTeams(Collection<BukkitUser> users)
	{
		this.teams.clear();
		this.players.clear();
		this.stats.clear();
		
		List<BukkitUser> unsortedUsers = new ArrayList<>(users);
		Collections.shuffle(unsortedUsers);
		Collections.sort(unsortedUsers, (o1, o2) ->
		{
			int weight = Integer.compare(o2.getUser().getWeight(), o1.getUser().getWeight());
			if (weight == 0)
			{
				UserPreferedMinigameTeamDataStorage o1Prefered = o1.getUser().getDataStorage(UserPreferedMinigameTeamDataStorage.class);
				UserPreferedMinigameTeamDataStorage o2Prefered = o1.getUser().getDataStorage(UserPreferedMinigameTeamDataStorage.class);
				if (o1Prefered != null && o2Prefered == null)
				{
					return 1;
				}
				else if (o1Prefered == null && o2Prefered != null)
				{
					return -1;
				}
				else if (o1Prefered != null && o2Prefered != null)
				{
					return Long.compare(o1Prefered.getTime(), o2Prefered.getTime());
				}
			}
			
			return weight;
		});
		
		List<T> teams = this.buildTeams();
		Collections.shuffle(teams);
		
		int playersMaxPerTeam = (int)Math.ceil(users.size() / (float)teams.size());
		
		while (unsortedUsers.size() > 0)
		{
			S player = this.createPlayer(unsortedUsers.remove(0));
			T team = null;
			
			UserPreferedMinigameTeamDataStorage preferedTeam = player.getUser().getDataStorage(UserPreferedMinigameTeamDataStorage.class);
			if (preferedTeam != null && preferedTeam.getTeam() != null)
			{
				team = teams.stream()
						.filter((t) -> t.getName().equals(preferedTeam.getTeam()) && t.getTeamMembers().size() < playersMaxPerTeam)
						.findFirst()
						.orElse(null);
			}
			
			if (team == null)
			{
				team = teams.stream()
						.min((o1, o2) -> Integer.compare(o1.getTeamMembers().size(), o2.getTeamMembers().size()))
						.orElse(null);
			}
			
			this.teams.put(team.getName(), team);
			
			team.addTeamMember(player);
			
			player.setTeam(team);
			
			this.teams.put(team.getName(), team);
			this.players.put(player.getUniqueId(), player);
			this.stats.put(player.getUniqueId(), player.getStats());
		}
	}
	
	protected abstract List<T> buildTeams();
	protected abstract S createPlayer(BukkitUser user);
	
	public void start()
	{
		try
		{
			this.networkManager.start();
		}
		catch(Throwable e) //Lets ignore this, we also have reconnect mechanism so it should be gud
		{
			e.printStackTrace();
		}
	}
	
	public Optional<String> onPlayerLogin(Player player)
	{
		this.makeSpectator(player);
		
		return Optional.empty();
	}
	
	public Optional<Location> onPlayerSpawn(Player player)
	{
		return Optional.empty();
	}
	
	public void onPlayerJoin(Player player)
	{
		if (this.scoreboardManager != null)
		{
			this.scoreboardManager.addPlayer(player);
		}
		
		this.makeSpectator(player);
	}
	
	public void onTick()
	{
		if (this.scoreboardManager != null)
		{
			this.scoreboardManager.onTick();
		}
	}
	
	public void makeSpectator(Player player)
	{
		this.spectators.put(player.getUniqueId(), new MinigameSpectatorPlayer(player));
		
		if (player.isDead())
		{
			Location location = player.getLocation();
			player.spigot().respawn();
			player.teleport(location);
		}
		
		EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
		
		//PaperSpigot <3
		//player.spigot().setCollidesWithEntities(false);
		//((CraftPlayer)player).getHandle().k = false; //Block placement collision
		entityPlayer.invulnerableTicks = Integer.MAX_VALUE; //This is fine?
		
		if (entityPlayer.playerConnection != null) //There is no null check inside CraftBukkit
		{
			player.closeInventory();
		}
		
		player.setGameMode(GameMode.ADVENTURE);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setHealth(player.getMaxHealth());
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		player.setFireTicks(0);
		player.setFallDistance(0);
		for(PotionEffect effect : player.getActivePotionEffects())
		{
			player.removePotionEffect(effect.getType());
		}
		
		if (entityPlayer.playerConnection != null) //There is no null check inside CraftBukkit
		{
			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
		}
		else
		{
			MinigamePlugin.getPlugin().getServer().getScheduler().runTask(MinigamePlugin.getPlugin(), () -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false)));
		}
		
		for(S alivePlayer : this.players.values())
		{
			alivePlayer.getBukkitPlayer().hidePlayer(player); //Hide the player from all alive players
		}
		
		for(MinigameSpectatorPlayer spectator : this.spectators.values())
		{
			player.showPlayer(spectator.getBukkitPlayer()); //Show the spectator to the current player
		}
	}
	
	public void removeSpectator(Player player)
	{
		this.spectators.remove(player.getUniqueId());
		
		//PaperSpigot <3
		//player.spigot().setCollidesWithEntities(true);
		//((CraftPlayer)player).getHandle().k = true; //Block placement collision
		((CraftPlayer)player).getHandle().invulnerableTicks = 0; //This is fine?
		
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setHealth(player.getMaxHealth());
		player.setFireTicks(0);
		player.setFallDistance(0);
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		
		for(S alivePlayer : this.players.values())
		{
			alivePlayer.getBukkitPlayer().showPlayer(player); //Show the player to all alive players
		}
		
		for(MinigameSpectatorPlayer spectator : this.spectators.values())
		{
			player.hidePlayer(spectator.getBukkitPlayer()); //Hide the spectator from alive player
		}
	}
	
	public void onPlayerQuit(Player player)
	{
		this.spectators.remove(player.getUniqueId());
		
		if (this.scoreboardManager != null)
		{
			this.scoreboardManager.removePlayer(player);
		}
	}
	
	public void cleanup()
	{
		this.networkManager.stop();
		
		this.world.getPlayers().forEach((p) -> p.kickPlayer("Cleanup"));
		
		MinigamePlugin.getPlugin().getServer().unloadWorld(this.world, false);

		File worldFle = new File(this.world.getName());
		if (worldFle.isDirectory())
		{
			try
			{
				FileUtils.deleteDirectory(worldFle);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		if (!this.privateGame)
		{
			ParinaCore.getApi().getStorageManager().getStorageModule(UserStorageModule.class).updateUserDataStorageMultiple(this.stats);
		}
	}

	public void sendTranslatableMessage(String area, String key, Object... params)
	{
		this.world.getPlayers().forEach((p) ->
		{
			Locale locale = Locale.forLanguageTag(p.spigot().getLocale().replaceAll( "_", "-"));
			
			User user = ParinaCore.getApi().getUserManager().getUser(p.getUniqueId());
			if (user != null)
			{
				locale = user.getLocale().orElse(null);
			}
			
			p.sendMessage(ParinaCore.getApi().getLanguageManager().getTranslation(locale, area, key, params));
		});
	}
	
	public void sendTitle(Title title)
	{
		this.world.getPlayers().forEach((p) -> p.sendTitle(title));
	}
	
	public void sendMessage(String message)
	{
		this.world.getPlayers().forEach((p) -> p.sendMessage(message));
	}
	
	public void kick(String reason)
	{
		this.world.getPlayers().forEach((p) -> p.kickPlayer(reason));
	}
	
	public void sendPluginMessage(Plugin plugin, String channel, byte[] data)
	{
		this.world.sendPluginMessage(plugin, channel, data);
	}
	
	public S getMinigamePlayer(Player player)
	{
		return this.getMinigamePlayer(player.getUniqueId());
	}
	
	public S getMinigamePlayer(UUID uuid)
	{
		return this.players.get(uuid);
	}
	
	public Collection<S> getPlayers()
	{
		return Collections.unmodifiableCollection(this.players.values());
	}
	
	public Collection<UUID> getPlayersUniqueIds()
	{
		return Collections.unmodifiableCollection(this.players.keySet());
	}
	
	public Collection<S> getAlivePlayers()
	{
		return Collections.unmodifiableCollection(this.players.values().stream().filter((p) -> p.isAlive()).collect(Collectors.toList()));
	}
	
	public Collection<UUID> getAlivePlayerUniqueIds()
	{
		return Collections.unmodifiableCollection(this.players.values().stream().filter((p) -> p.isAlive()).map((p) -> p.getUniqueId()).collect(Collectors.toList()));
	}
	
	public Collection<T> getTeams()
	{
		return Collections.unmodifiableCollection(this.teams.values());
	}
	
	public Collection<T> getAliveTeams()
	{
		return Collections.unmodifiableCollection(this.teams.values().stream().filter((t) -> t.isAlive()).collect(Collectors.toList()));
	}
	
	public Collection<MinigameSpectatorPlayer> getSpectator()
	{
		return Collections.unmodifiableCollection(this.spectators.values());
	}
	
	public boolean isPlaying(Player player)
	{
		return this.isPlaying(player.getUniqueId());
	}
	
	public boolean isPlaying(UUID uuid)
	{
		return this.players.containsKey(uuid);
	}
	
	public boolean isSpectator(Player player)
	{
		return this.isSpectator(player.getUniqueId());
	}
	
	public boolean isSpectator(UUID uuid)
	{
		return this.spectators.containsKey(uuid);
	}
	
	public int getAlivePlayersCount()
	{
		return (int)this.players.values().stream().filter((p) -> p.isAlive()).count();
	}
	
	public int getAliveTeamsCount()
	{
		return (int)this.teams.values().stream().filter((t) -> t.isAlive()).count();
	}
	
	public int getSpectatorsCount()
	{
		return this.spectators.size();
	}
}

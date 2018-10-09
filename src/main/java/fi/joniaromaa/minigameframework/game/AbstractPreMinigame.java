package fi.joniaromaa.minigameframework.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.github.paperspigot.Title;

import com.google.common.collect.Sets;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.api.game.PreMinigameStatus;
import fi.joniaromaa.minigameframework.communication.tcp.outgoing.PlayerJoinOutgoingPacket;
import fi.joniaromaa.minigameframework.communication.tcp.outgoing.PlayerQuitOutgoingPacket;
import fi.joniaromaa.minigameframework.config.MinigameConfig;
import fi.joniaromaa.minigameframework.config.MinigameMapConfig;
import fi.joniaromaa.minigameframework.net.NetworkManager;
import fi.joniaromaa.minigameframework.player.BukkitUser;
import fi.joniaromaa.parinacorelibrary.bukkit.scoreboard.ScoreboardManager;
import fi.joniaromaa.parinacorelibrary.common.utils.TimeUtils;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

public abstract class AbstractPreMinigame implements Minigame
{
	@Getter private int gameId;
	@Getter private final MinigameConfig config;
	@Getter private final MinigameMapConfig mapConfig;
	
	@Getter private volatile PreMinigameStatus status;
	@Getter private int spotsLeft;

	private Map<UUID, BukkitUser> players;
	
	private List<Set<UUID>> tryIncludeInSameTeam;
	private ReentrantLock tryIncludeInsameTeamLock;
	
	private NetworkManager networkManager;
	protected ScoreboardManager scoreboardManager;
	
	private int actionTimeTicks;
	private int lastBroadcastTime; //SECONDS
	
	private Integer gameStartTimeTicks;
	private int gameStartTimeModifier; //TICKS
	
	public AbstractPreMinigame(int gameId, MinigameConfig config, MinigameMapConfig mapConfig)
	{
		this.gameId = gameId;
		this.config = config;
		this.mapConfig = mapConfig;
		
		this.spotsLeft = mapConfig.getPlayerLimit() * config.getTeamSize();
		
		this.players = new HashMap<>();
		
		this.tryIncludeInSameTeam = new ArrayList<>();
		this.tryIncludeInsameTeamLock = new ReentrantLock();
		
		this.networkManager = new NetworkManager(this);
		
		this.setStatus(PreMinigameStatus.WAITING_FOR_PLAYERS);
	}
	
	public void setup() throws Exception
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

	@Override
	public Optional<String> onPlayerLogin(Player player)
	{
		if (this.status != PreMinigameStatus.GAME_STARTED)
		{
			if (this.spotsLeft > 0)
			{
				BukkitUser user = new BukkitUser(player);
				if (user != null && user.isLoaded())
				{
					this.spotsLeft--;
					
					this.players.put(player.getUniqueId(), user);
					
					return Optional.empty();
				}
				else
				{
					return Optional.of("Unable to load populate player cache");
				}
			}
			else
			{
				return Optional.of("Game is full");
			}
		}
		else
		{
			return Optional.of("Game has started");
		}
	}

	public void onPlayerJoin(Player player)
	{
		this.networkManager.sendPacket(new PlayerJoinOutgoingPacket(player.getUniqueId()));
		
		BukkitUser user = this.getPlayer(player);
		if (user == null)
		{
			player.kickPlayer("Bukkit user is null on join");
			return;
		}
		
		this.sendTranslatableMessage("minigame", "game.player-joined", user.getUser().getColoredDisplayName(), this.getPlayersCount(), this.getPlayersLimit());
		
		Integer startTime = this.getCurrentPreGameStartTime();
		if (startTime != null)
		{
			this.calcGameStartTime(startTime);
			
			if (this.status != PreMinigameStatus.START_COUNTDOWN)
			{
				this.setStatus(PreMinigameStatus.START_COUNTDOWN);
				this.tryBroadcastCountdown(true);
			}
		}
		
		if (this.scoreboardManager != null)
		{
			this.scoreboardManager.addPlayer(player);
		}
	}

	private void calcGameStartTime(int startTime)
	{
		if (this.gameStartTimeTicks == null)
		{
			this.gameStartTimeTicks = startTime;
		}
		else if (this.gameStartTimeTicks != startTime)
		{
			int timeLeftInTicks = this.getTimeLeftToStartInTicks();
			
			if (this.gameStartTimeTicks > startTime)
			{
				int newStartTimeInTicks = startTime;
				if (timeLeftInTicks > newStartTimeInTicks)
				{
					this.gameStartTimeTicks = startTime;
					this.gameStartTimeModifier += this.actionTimeTicks;
					this.actionTimeTicks = 0;
				}
			}
			else
			{
				this.gameStartTimeTicks = startTime;
				this.actionTimeTicks += this.gameStartTimeModifier;
				this.gameStartTimeModifier = 0;
				
				this.sendTranslatableMessage("minigame", "game.countdown-time-added");
			}
		}
	}

	public void onTick()
	{
		switch (this.status)
		{
			case WAITING_FOR_PLAYERS:
			{
				Integer startTime = this.getCurrentPreGameStartTime();
				if (startTime != null)
				{
					this.calcGameStartTime(startTime);
					
					this.setStatus(PreMinigameStatus.START_COUNTDOWN);
					this.tryBroadcastCountdown(true);
				}
			}
			break;
			case START_COUNTDOWN:
			{
				Integer startTime = this.getCurrentPreGameStartTime();
				if (startTime != null)
				{
					this.calcGameStartTime(startTime);
					
					if (++this.actionTimeTicks >= this.gameStartTimeTicks)
					{
						this.networkManager.stop();
						
						this.setStatus(PreMinigameStatus.GAME_STARTED);

						this.resetTitle();
						this.sendTranslatableMessage("minigame", "game.starting");
					}
					else
					{
						this.tryBroadcastCountdown(false);
					}
				}
				else
				{
					this.setStatus(PreMinigameStatus.WAITING_FOR_PLAYERS);
				}
			}
			break;
			case GAME_STARTED:
			{
				if (MinigamePlugin.getPlugin().getGameManager().createGame(this, false) == null) //ERROR!
				{
					for(BukkitUser player : this.players.values())
					{
						player.getBukkitPlayer().kickPlayer("Critical error!");
					}
				}
			}
			break;
		}

		if (this.scoreboardManager != null)
		{
			this.scoreboardManager.onTick();
		}
	}
	
	public void onPlayerQuit(Player player)
	{
		if (this.status != PreMinigameStatus.GAME_STARTED)
		{
			BukkitUser minigamePlayer = this.players.remove(player.getUniqueId());
			if (minigamePlayer != null)
			{
				this.sendTranslatableMessage("minigame", "game.player-left", minigamePlayer.getUser().getColoredDisplayName(), this.getPlayersCount(), this.getPlayersLimit());
				
				this.spotsLeft++;
				
				this.networkManager.sendPacket(new PlayerQuitOutgoingPacket(player.getUniqueId()));
			}
			
			if (this.scoreboardManager != null)
			{
				this.scoreboardManager.removePlayer(player);
			}
		}
	}

	public void setStatus(PreMinigameStatus status)
	{
		this.setStatus(status, 0);
	}
	
	public void setStatus(PreMinigameStatus status, int countdown)
	{
		this.status = status;
		
		this.actionTimeTicks = countdown;
		this.lastBroadcastTime = -1;
		
		this.gameStartTimeTicks = null;
		this.gameStartTimeModifier = 0;
	}
	
	public void tryBroadcastCountdown(boolean forced)
	{
		int secs = this.getTimeLeftToStartInSecs();
		if (this.lastBroadcastTime != secs)
		{
			this.lastBroadcastTime = secs;

			if (forced || secs <= 5 || secs == 10)
			{
				this.broadcastCountdown(secs);
				
				for(BukkitUser player : this.getPlayers())
				{
					Player bukkitPlayer = player.getBukkitPlayer();
					bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.CLICK, Integer.MAX_VALUE, 1);
				}
			}
		}
	}
	
	public void broadcastCountdown(int secs)
	{
		ChatColor color = null;
		if (secs <= 3)
		{
			color = ChatColor.RED;
		}
		else if (secs <= 5)
		{
			color = ChatColor.YELLOW;
		}
		else
		{
			color = ChatColor.GREEN;
		}
		
		if (secs <= 60) //Only show seconds with title
		{
			this.sendTitle(Title.builder()
					.title(color.toString() + secs)
					.fadeIn(5)
					.stay(20)
					.fadeOut(5)
					.build());
		}
		
		this.sendTranslatableMessage("minigame", "game.countdown-starting", color + TimeUtils.getHumanReadableSimplePeriod(secs));
	}
	
	public void sendTranslatableMessage(String area, String key, Object... params)
	{
		for(BukkitUser player : this.players.values())
		{
			player.sendTranslatableMessage(area, key, params);
		}
	}
	
	public void sendTitle(Title title)
	{
		for(BukkitUser player : this.players.values())
		{
			player.getBukkitPlayer().sendTitle(title);
		}
	}
	
	public void resetTitle()
	{
		for(BukkitUser player : this.players.values())
		{
			player.getBukkitPlayer().resetTitle();
		}
	}
	
	public void cleanup()
	{
		this.spotsLeft = 0;
		
		this.networkManager.stop();
		
		if (this.scoreboardManager != null)
		{
			this.scoreboardManager.clear();
		}
	}
	
	public void tryIncludePlayersInSameTeam(UUID... uuids)
	{
		if (this.status != PreMinigameStatus.GAME_STARTED)
		{
			Set<UUID> uuids_ = Sets.newHashSet(uuids);
			
			this.tryIncludeInsameTeamLock.lock();
			try
			{
				for(Iterator<Set<UUID>> teamsIterator = this.tryIncludeInSameTeam.iterator(); teamsIterator.hasNext(); )
				{
					Set<UUID> teamMembers = teamsIterator.next();
					for(Iterator<UUID> teamMembersIterator = teamMembers.iterator(); teamMembersIterator.hasNext(); )
					{
						UUID teamMember = teamMembersIterator.next();
						if (uuids_.contains(teamMember))
						{
							teamMembersIterator.remove();
						}
					}
					
					if (teamMembers.size() <= 0)
					{
						teamsIterator.remove();
					}
				}
				
				if (uuids_.size() > 1)
				{
					this.tryIncludeInSameTeam.add(uuids_);
				}
			}
			finally
			{
				this.tryIncludeInsameTeamLock.unlock();
			}
		}
	}
	
	/*public List<AbstractMinigameTeam<?>> sortPlayersToTeams()
	{
		List<AbstractMinigameTeam<?>> teams = this.generateTeams();
		Set<UUID> playersToSort = Sets.newHashSet(this.players.keySet());

		this.tryIncludeInsameTeamLock.lock();
		try
		{
			for(Iterator<Set<UUID>> playersIterator = this.tryIncludeInSameTeam.iterator(); playersIterator.hasNext(); )
			{
				Set<UUID> players = playersIterator.next();
				for(Iterator<LinkedHashMap<UUID, S>> teamsIterator = teams.values().iterator(); teamsIterator.hasNext(); ) //Now look for team to fit them in
				{
					LinkedHashMap<UUID, S> team = teamsIterator.next();
					
					//If team don't have any players fit in as much players as you can and continue if there is reamining players
					//If the party is small enought to fit in even if there is players, continue too
					//Otherwise we need to try find free team
					if (team.size() <= 0 || team.size() + players.size() <= this.config.getTeamSize())
					{
						for(Iterator<UUID> uuidIterator = players.iterator(); uuidIterator.hasNext(); )
						{
							if (team.size() >= this.config.getTeamSize()) //This team is full, we can't fill it up any futher, find new team instead
							{
								break;
							}
							
							UUID uuid = uuidIterator.next();
							
							uuidIterator.remove();
							
							players.remove(uuid);
							
							T player = this.players.get(uuid);
							if (player != null) //Not in pregame, ignore
							{
								team.put(uuid, this.createMinigamePlayer(player));
							}
						}
					}
					
					if (players.size() <= 0) //We don't have anymore players to fit in, lets move on
					{
						break;
					}
				}
				
				if (players.size() <= 0)
				{
					playersIterator.remove();
				}
				else
				{
					System.out.println("Failed to sort party?!?!?");
				}
			}
		}
		finally
		{
			this.tryIncludeInsameTeamLock.unlock();
		}
		
		for(Iterator<UUID> uuidIterator = playersToSort.iterator(); uuidIterator.hasNext();) //Now we sort any remaining players
		{
			UUID uuid = uuidIterator.next();
			
			uuidIterator.remove();
			
			T player = this.players.get(uuid);
			if (player != null) //Not in pregame, ignore
			{
				for(LinkedHashMap<UUID, S> team : teams.values()) //Now look for team to fit them in
				{
					if (team.size() < this.config.getTeamSize())
					{
						team.put(uuid, this.createMinigamePlayer(player));
						
						break;
					}
				}
			}
		}
		
		if (playersToSort.size() > 0)
		{
			System.out.println("Failed to sort players?!?!?!");
		}
			
		return teams;
	}*/
	
	public boolean isPlaying(Player player)
	{
		return this.isPlaying(player.getUniqueId());
	}
	
	public boolean isPlaying(UUID uniqueId)
	{
		return this.players.containsKey(uniqueId);
	}
	
	public BukkitUser getPlayer(Player player)
	{
		return this.getPlayer(player.getUniqueId());
	}
	
	public BukkitUser getPlayer(UUID uuid)
	{
		return this.players.get(uuid);
	}
	
	public int getPlayersLimit()
	{
		return this.mapConfig.getPlayerLimit() * this.config.getTeamSize();
	}
	
	public int getPlayersCount()
	{
		return this.players.size();
	}

	public int getTeamsCount()
	{
		return (int)Math.ceil(this.getPlayersCount() / (double)this.config.getTeamSize());
	}
	
	public List<String> getTeamNames()
	{
		List<String> teamNames = new ArrayList<>();
		
		for(int i = 1; i <= this.getTeamsCount(); i++)
		{
			teamNames.add("Team #" + i);
		}
		
		return teamNames;
	}
	
	public Set<UUID> getPlayersUniqueIds()
	{
		return Collections.unmodifiableSet(this.players.keySet());
	}
	
	public Collection<BukkitUser> getPlayers()
	{
		return Collections.unmodifiableCollection(this.players.values());
	}
	
	public Integer getCurrentPreGameStartTime()
	{
		Integer startTime = null;
		for(Entry<Integer, Integer> startTime_ : this.mapConfig.getPreGameStartTimes().entrySet())
		{
			if (this.players.size() >= startTime_.getKey() * this.getConfig().getTeamSize())
			{
				startTime = startTime_.getValue();
			}
		}
		
		if (startTime == null)
		{
			return startTime;
		}
		
		return startTime * 20; //To ticks
	}
	
	public int getTimeLeftToStartInTicks()
	{
		if (this.gameStartTimeTicks == null)
		{
			return this.status == PreMinigameStatus.GAME_STARTED ? 0 : -1;
		}
		
		return this.gameStartTimeTicks - this.actionTimeTicks;
	}
	
	public int getTimeLeftToStartInSecs()
	{
		if (this.gameStartTimeTicks == null)
		{
			return this.status == PreMinigameStatus.GAME_STARTED ? 0 : -1;
		}
		
		return (int)Math.ceil((this.gameStartTimeTicks - this.actionTimeTicks) / 20D);
	}
	
	public abstract World getGameWorld();
}

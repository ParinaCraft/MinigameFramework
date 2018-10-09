package fi.joniaromaa.minigameframework.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.config.MinigameConfig;
import fi.joniaromaa.minigameframework.config.MinigameManagerConfig;
import fi.joniaromaa.minigameframework.config.MinigameMapConfig;

public class GameManager
{
	private final MinigameManagerConfig config;

	private Map<Integer, Minigame> games;
	
	private Cache<UUID, Integer> joiningPlayers;
	private Map<UUID, Integer> players;
	
	private int nextGameId;
	
	public GameManager(MinigameManagerConfig config)
	{
		this.config = config;

		this.games = new HashMap<>();
		
		this.joiningPlayers = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();
		this.players = new HashMap<>();
		
		this.nextGameId = 1;
	}
	
	public int getNextGameId()
	{
		return this.nextGameId++;
	}
	
	public AbstractPreMinigame createPreGame(Class<? extends AbstractPreMinigame> preMinigameClass, MinigameConfig config)
	{
		return this.createPreGame(preMinigameClass, config, config.getRandomMapConfig());
	}
	
	public AbstractPreMinigame createPreGame(Class<? extends AbstractPreMinigame> preMinigameClass, MinigameConfig config, MinigameMapConfig mapConfig)
	{
		int gameId = this.getNextGameId();
		
		try
		{
			AbstractPreMinigame preMinigame = preMinigameClass.getConstructor(int.class, MinigameConfig.class, MinigameMapConfig.class).newInstance(gameId, config, mapConfig);
			
			try
			{
				preMinigame.setup();
	
				this.games.put(preMinigame.getGameId(), preMinigame);
	
				return preMinigame;
			}
			catch(Throwable e)
			{
				preMinigame.onCriticalException(e);
				
				e.printStackTrace(); 
				
				this.games.remove(preMinigame.getGameId()); //Remove on error
			}
		}
		catch (Throwable e) //Constructor failed or something like that
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public AbstractMinigame<?, ?> createGame(AbstractPreMinigame preGame, boolean privateGame)
	{
		//Try clean up the pre game first and then try to create the actual game
		try
		{
			preGame.cleanup();
			
			try
			{
				AbstractMinigame<?, ?> minigame = preGame.getConfig().getMinigameClass().getConstructor(int.class, MinigameConfig.class, MinigameMapConfig.class, World.class, Collection.class, boolean.class).newInstance(preGame.getGameId(), preGame.getConfig(), preGame.getMapConfig(), preGame.getGameWorld(), preGame.getPlayers(), privateGame);
				
				try
				{
					minigame.start();
					
					this.games.put(minigame.getGameId(), minigame);
					
					return minigame;
				}
				catch (Throwable e)
				{
					minigame.onCriticalException(e);
					
					this.games.remove(minigame.getGameId());
					
					e.printStackTrace();
				}
			}
			catch (Throwable e) //Constructor failed or something like that
			{
				e.printStackTrace();
			}
		}
		catch (Throwable e)
		{
			preGame.onCriticalException(e);
			
			this.games.remove(preGame.getGameId());
			
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void deleteGame(AbstractMinigame<?, ?> game)
	{
		this.games.remove(game.getGameId());
		
		try
		{
			game.cleanup();
		}
		catch (Throwable e)
		{
			game.onCriticalException(e);
			
			e.printStackTrace();
		}
	}
	
	public void onMonitor()
	{
		this.joiningPlayers.cleanUp();
	}
	
	public void onTick()
	{
		while (this.games.size() < this.config.getConcurrentGameLimit()) //Create more games, one per tick to avoid bigger issues
		{
			this.createPreGame(this.config.getPreMinigameClass(), this.config.getMinigameConfig());
		}
		
		for(Minigame minigame : this.games.values().toArray(new Minigame[0])) //Make copy to avoid CME
		{
			try
			{
				minigame.onTick();
			}
			catch(Throwable e)
			{
				minigame.onCriticalException(e);
				
				e.printStackTrace();
			}
		}
	}
	
	public void addJoiningPlayers(int gameId, UUID... uuids)
	{
		for(UUID uuid : uuids)
		{
			this.joiningPlayers.put(uuid, gameId);
		}
	}
	
	public Optional<String> onPlayerLogin(Player player)
	{
		Integer gameId = this.joiningPlayers.getIfPresent(player.getUniqueId());
		if (gameId != null)
		{
			this.joiningPlayers.invalidate(player.getUniqueId());
			
			Minigame minigame = this.games.get(gameId);
			if (minigame != null)
			{
				Optional<String> response = minigame.onPlayerLogin(player);
				if (!response.isPresent())
				{
					this.players.put(player.getUniqueId(), gameId);
				}
				
				return response;
			}
			else
			{
				return Optional.of("Unable to find the requested game");
			}
		}
		else
		{
			return Optional.of("You may only join this server thru queue system");
		}
	}
	
	public void onPlayerQuit(Player player)
	{
		Integer gameId = this.players.remove(player.getUniqueId());
		if (gameId != null)
		{
			Minigame minigame = this.games.get(gameId);
			if (minigame != null)
			{
				minigame.onPlayerQuit(player);
			}
		}
	}
	
	public Optional<Minigame> getMinigame(Player player)
	{
		return this.getMinigame(player.getUniqueId());
	}
	
	public Optional<Minigame> getMinigame(UUID uniqueId)
	{
		Integer gameId = this.players.get(uniqueId);
		if (gameId != null)
		{
			return Optional.ofNullable(this.games.get(gameId));
		}
		else
		{
			return Optional.empty();
		}
	}

	public void requestSpectator(int gameId, UUID uniqueId)
	{
		this.joiningPlayers.put(uniqueId, gameId);
	}
}

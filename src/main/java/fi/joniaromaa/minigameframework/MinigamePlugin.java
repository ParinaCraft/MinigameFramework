package fi.joniaromaa.minigameframework;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.SpigotConfig;

import fi.joniaromaa.minigameframework.commands.MinigameCommandExecutor;
import fi.joniaromaa.minigameframework.config.MinigameManagerConfig;
import fi.joniaromaa.minigameframework.game.GameManager;
import fi.joniaromaa.minigameframework.listeners.BlockListener;
import fi.joniaromaa.minigameframework.listeners.EntityListener;
import fi.joniaromaa.minigameframework.listeners.InventoryListener;
import fi.joniaromaa.minigameframework.listeners.PlayerListener;
import fi.joniaromaa.minigameframework.listeners.WorldListener;
import fi.joniaromaa.minigameframework.nms.NoDataServerNBTManager;
import fi.joniaromaa.minigameframework.nms.NoDataWorldNBTStorage;
import fi.joniaromaa.minigameframework.runnables.GameTickRunnable;
import fi.joniaromaa.minigameframework.runnables.MonitorRunnable;
import fi.joniaromaa.parinacorelibrary.api.ParinaCore;
import fi.joniaromaa.parinacorelibrary.api.ParinaCoreApi;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.MinecraftServer;

public class MinigamePlugin extends JavaPlugin
{
	@Getter private static MinigamePlugin plugin;
	
	@Getter private ParinaCoreApi parinaCoreApi;
	
	@Getter private GameManager gameManager;
	
	public MinigamePlugin()
	{
		MinigamePlugin.plugin = this;
	}
	
	@Override
	public void onLoad()
	{
		MinecraftServer mcServer = ((CraftServer)this.getServer()).getServer();
		mcServer.getPropertyManager().setProperty("level-type", "FLAT");
		mcServer.getPropertyManager().setProperty("generator-settings", "2;0;1"); //Void only
		mcServer.getPropertyManager().setProperty("spawn-protection", "0");
		mcServer.getPropertyManager().savePropertiesFile();
		
		try
		{
			Field bukkitConfigField = CraftServer.class.getDeclaredField("configuration");
			bukkitConfigField.setAccessible(true);
			
			YamlConfiguration bukkitConfig = (YamlConfiguration)bukkitConfigField.get(this.getServer());
			bukkitConfig.set("ticks-per.autosave", 0);
			
			Method bukkitConfigSaveMethod = CraftServer.class.getDeclaredMethod("saveConfig");
			bukkitConfigSaveMethod.setAccessible(true);
			bukkitConfigSaveMethod.invoke(this.getServer());
		}
		catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
		{
			e.printStackTrace();
		}
		
		mcServer.autosavePeriod = 0;
		
		SpigotConfig.config.set("stats.disable-saving", true);
		
		try
		{
			Field spigotConfigFile = SpigotConfig.class.getDeclaredField("CONFIG_FILE");
			spigotConfigFile.setAccessible(true);
			SpigotConfig.config.save((File)spigotConfigFile.get(null));
		}
		catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | IOException e)
		{
			e.printStackTrace();
		}
		
		SpigotConfig.disableStatSaving = true;
		
		try
		{
			File defaultWorld = new File(mcServer.getPropertyManager().getString("level-name", "world"));
			if (defaultWorld.isDirectory())
			{
				FileUtils.deleteDirectory(defaultWorld);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onEnable()
	{
		Locale finishLocale = Locale.forLanguageTag("fi-FI");
		
		//Lets grap our API as soon as its avaible
		this.parinaCoreApi = ParinaCore.getApi();
		
		//We should really load these from file...
		this.parinaCoreApi.getLanguageManager().addTranslation(finishLocale, "minigame", "game.player-joined", ChatColor.AQUA + "PELI > " + ChatColor.RESET + "{0}" + ChatColor.RESET + " on liittynyt peliin! (" + ChatColor.GREEN + "{1}/{2}" + ChatColor.RESET + ")");
		this.parinaCoreApi.getLanguageManager().addTranslation(finishLocale, "minigame", "game.player-left", ChatColor.AQUA + "PELI > " + ChatColor.RESET + "{0}" + ChatColor.RESET + " on poistunut pelistä! (" + ChatColor.GREEN + "{1}/{2}" + ChatColor.RESET + ")");
		this.parinaCoreApi.getLanguageManager().addTranslation(finishLocale, "minigame", "game.countdown-starting", ChatColor.AQUA + "PELI > " + ChatColor.RESET + "Peli alkaa " + ChatColor.GREEN + "{0}");
		this.parinaCoreApi.getLanguageManager().addTranslation(finishLocale, "minigame", "game.countdown-cancelled", ChatColor.AQUA + "PELI > " + ChatColor.RESET + "Pelin aloitus on peruuttu (Ei tarpeeksi pelaajia)");
		this.parinaCoreApi.getLanguageManager().addTranslation(finishLocale, "minigame", "game.starting", ChatColor.AQUA + "PELI > " + ChatColor.RESET + "Peli alkaa..");
		this.parinaCoreApi.getLanguageManager().addTranslation(finishLocale, "minigame", "game.countdown-time-added", ChatColor.AQUA + "PELI > " + ChatColor.RESET + "Aloitusaikaa lisättiin, koska pelaajia poistui pelistä");
		
		World world = this.getServer().getWorld(((CraftServer)this.getServer()).getServer().getPropertyManager().getString("level-name", "world"));
		world.setSpawnLocation(0, 64, 0);
		world.setKeepSpawnInMemory(false);
		world.setAutoSave(false);
		world.setMetadata("MinigamePlugin: DontLoadChunks", new FixedMetadataValue(this, true));
		
		IDataManager dataManager = ((CraftWorld)world).getHandle().getDataManager();
		NoDataServerNBTManager noDataServerNBTManager = new NoDataServerNBTManager(dataManager.getDirectory().getParentFile(), dataManager.getDirectory().getName(), true);
		
		try
		{
			Field dataManagerField = net.minecraft.server.v1_8_R3.World.class.getDeclaredField("dataManager");
			dataManagerField.setAccessible(true);
			
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(dataManagerField, dataManagerField.getModifiers() & ~Modifier.FINAL);
			
			dataManagerField.set(((CraftWorld)world).getHandle(), noDataServerNBTManager);
		}
		catch(IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
		{
			e.printStackTrace();
		}
		
		MinecraftServer mcServer = ((CraftServer)this.getServer()).getServer();
		mcServer.getPlayerList().playerFileData = new NoDataWorldNBTStorage(new File("."), world.getName());
		
		for(Chunk chunk : world.getLoadedChunks())
		{
			chunk.unload(false, false);
		}
	}
	
	public void configure(MinigameManagerConfig config) throws InterruptedException
	{
		if (this.gameManager != null)
		{
			throw new IllegalStateException("Already configured");
		}
		
		this.gameManager = new GameManager(config);
		this.gameManager.onMonitor();
		
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new BlockListener(this), this);
		this.getServer().getPluginManager().registerEvents(new WorldListener(), this);
		this.getServer().getPluginManager().registerEvents(new EntityListener(), this);
		this.getServer().getPluginManager().registerEvents(new InventoryListener(), this);
		
		this.getServer().getPluginCommand("minigame").setExecutor(new MinigameCommandExecutor(this));
		
		this.getServer().getScheduler().runTaskTimer(this, new GameTickRunnable(), 1L, 1L);
		this.getServer().getScheduler().runTaskTimerAsynchronously(this, new MonitorRunnable(), 5L, 5L);
	}
}

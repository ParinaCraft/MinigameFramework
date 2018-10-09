package fi.joniaromaa.minigameframework.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.game.AbstractPreMinigame;

public class MinigameCommandExecutor implements CommandExecutor
{
	private final MinigamePlugin plugin;
	
	public MinigameCommandExecutor(MinigamePlugin plugin)
	{
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (sender instanceof Player)
		{
			Player player = (Player)sender;
			if (args.length > 0)
			{
				switch(args[0])
				{
					case "start":
					case "s":
					{
						this.plugin.getGameManager().getMinigame(player).ifPresent((m) ->
						{
							if (m instanceof AbstractPreMinigame)
							{
								this.plugin.getGameManager().createGame((AbstractPreMinigame)m, true);
							}
						});
					}
					break;
				}
			}
			else
			{
				sender.sendMessage("Here is some uselss help for you, idiot");
			}
		}
		else
		{
			sender.sendMessage("At the moment, only player");
		}
		
		return true;
	}
}

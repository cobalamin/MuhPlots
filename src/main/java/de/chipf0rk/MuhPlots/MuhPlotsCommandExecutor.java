package de.chipf0rk.MuhPlots;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.chipf0rk.MuhPlots.MessageSender.State;

public class MuhPlotsCommandExecutor implements CommandExecutor {
	private MuhPlots plugin;
	private MessageSender msg;
	
	MuhPlotsCommandExecutor(MuhPlots plugin) {
		this.plugin = plugin;
		this.msg = plugin.msg;
	}
	
	Player getPlayerByName(Player issuer, String name) {
		return issuer.getServer().getPlayer(name);
	}
	
	public boolean onCommand(CommandSender sender, Command c, String label, String[] args) {
		if (!(sender instanceof Player)) {
			msg.send(sender, State.FAILURE, "This command can't be used from the console.");
			return true;
		}

		Player player = (Player) sender;
		// get actual plot command
		switch(args[0].toLowerCase()) {
			
		}
		
		return false;
	}
}

package de.chipf0rk.MuhPlots;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageSender {
	enum State { SUCCESS, FAILURE, NOTICE }
	
	MuhPlots plugin;
	MessageSender(MuhPlots plugin) {
		this.plugin = plugin;
	}
	
	private String formatString(State state, String string) {
		switch(state) {
		case SUCCESS:
			return ChatColor.BLUE + "["
				+ ChatColor.DARK_AQUA + plugin.getName()
				+ ChatColor.BLUE + "] "
				+ ChatColor.WHITE + string;
		case FAILURE:
			return ChatColor.BLUE + "["
				+ ChatColor.DARK_AQUA + plugin.getName()
				+ ChatColor.BLUE + "] "
				+ ChatColor.RED + string;
		case NOTICE:
		default:
			return ChatColor.BLUE + "["
				+ ChatColor.WHITE + plugin.getName()
				+ ChatColor.BLUE + "] "
				+ ChatColor.WHITE + string;
		}
	}
	
	void send(CommandSender recipient, State state, String message) {
		String formattedMessage = formatString(state, message);
		recipient.sendMessage(formattedMessage);
	}
}

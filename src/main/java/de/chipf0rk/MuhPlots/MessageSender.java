package de.chipf0rk.MuhPlots;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageSender {
	enum State { SUCCESS, FAILURE, NOTICE }
	
	MuhPlots plugin;
	MessageSender(MuhPlots plugin) {
		this.plugin = plugin;
	}
	
	private String helpEntry(String cmd, String explanation) {
		return " " + ChatColor.BLUE + " > " + ChatColor.DARK_AQUA +
			"/mp " + cmd +
			ChatColor.BLUE + " > " + ChatColor.WHITE + explanation;
	}
	
	List<String> help = Arrays.asList(
		helpEntry("protect", "Protects your plot"),
		helpEntry("add", "Adds a friend to your plot"),
		helpEntry("rem", "Removes a friend from your plot"),
		helpEntry("info", "Info about the current plot"),
		helpEntry("list", "Lists all your plots"),
		helpEntry("tp", "Teleports you to a plot"),
		helpEntry("find", "Finds an empty plot")
	);	
	
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
	
	void sendHelpTo(CommandSender recipient) {
		recipient.sendMessage("");
		for(String line : help) {
			recipient.sendMessage(line);
		}
		recipient.sendMessage("");
	}
}

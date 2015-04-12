package de.chipf0rk.MuhPlots;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class MuhPlots extends JavaPlugin {
	private List<String> mpCommandNames = Arrays.asList("mp", "muhplot", "muhplots", "plot");

	PlotHelpers plotHelpers;
	PlotActions actions;
	MessageSender msg;
	Database db;

	// Logging
	void log(Level level, String msg) {
		String fullMsg =
				ChatColor.DARK_AQUA + "[" + getName() + "]" +
				ChatColor.RESET + msg;
		getLogger().log(level, fullMsg);
	}
	void info(String msg) { log(Level.INFO, msg); }
	void severe(String msg) { log(Level.SEVERE, msg); }

	@Override
	public void onEnable() {
		info(getName() + " has been enabled! :D");
		
		// === Init the database
		db = new SQLite(getLogger(), 
				"[" + getName() + "] ",
				this.getDataFolder().getAbsolutePath(),
				getName(),
				".sqlite");
		if(!db.open()) {
			severe("Could not open a connection to the database! Plugin initialisation failed.");
			return;
		}
		
		// === Instantiate helper instances
		this.msg = new MessageSender(this);
		this.actions = new PlotActions(this);
		
		// === Set the command executor
		// We do this last so that initialisation errors prevent any commands from being registered
		MuhPlotsCommandExecutor executor = new MuhPlotsCommandExecutor(this);
		for(String cmd : mpCommandNames) {
			this.getCommand(cmd).setExecutor(executor);
		}
	}

	@Override
	public void onDisable() {
		info(getName() + " has been disabled.");
	}
}

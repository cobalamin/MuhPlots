package de.chipf0rk.MuhPlots;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;

import de.chipf0rk.MuhPlots.exceptions.MuhInitException;

public final class MuhPlots extends JavaPlugin {
	// Helper instances
	MessageSender msg;
	PlotActions actions;
	PlotHelpers helpers;

	// DB
	Database db;
	
	// Plot schematic
	File plotFile;
	CuboidClipboard plotSchematic;
	
	// Information
	List<String> plotWorlds;

	// Logging
	void log(Level level, String msg) {
		String fullMsg =
				ChatColor.DARK_AQUA + "[" + getName() + "]" +
				ChatColor.RESET + msg;
		getLogger().log(level, fullMsg);
	}
	void info(String msg) { log(Level.INFO, msg); }
	void severe(String msg) { log(Level.SEVERE, msg); }
	void warn(String msg) { log(Level.WARNING, msg); }

	@Override
	public void onEnable() {
		// Init the database
		db = new SQLite(getLogger(), 
				"[" + getName() + "] ",
				this.getDataFolder().getAbsolutePath(),
				getName(),
				".sqlite");
		if(!db.open()) {
			severe("Could not open a connection to the database! Plugin initialisation failed.");
			return;
		}
		
		// Load list of plot worlds from the config
		this.plotWorlds = getConfig().getStringList("plotworlds");
		
		// Try loading the plot schematic file
		plotFile = new File(getDataFolder(), "plot.schematic");
		if(plotFile.exists()) {
			try {
				this.plotSchematic = SchematicFormat.MCEDIT.load(plotFile);
			} catch (DataException | IOException e) {
				this.plotSchematic = null;
				severe("An error occurred when loading the plot file!");
			}
		}
		else {
			warn("The plot file does not exist! Plot restoring commands will not work.");
		}
		
		// Create helper instances
		try {
			this.msg = new MessageSender(this);
			this.actions = new PlotActions(this);
			this.helpers = new PlotHelpers(this);
		} catch(MuhInitException e) {
			severe("Initialisation exception: " + e.getMessage());
			return;
		}
		
		// Set the command executor
		// We do this last so that initialisation errors prevent any commands from being registered
		MuhPlotsCommandExecutor executor = new MuhPlotsCommandExecutor(this);
		for(String cmd : getDescription().getCommands().keySet()) {
			this.getCommand(cmd).setExecutor(executor);
		}
		
		info(getFullName() + " has been enabled! :D");
	}

	@Override
	public void onDisable() {
		info(getFullName() + " has been disabled.");
	}
	
	public String getFullName() {
		return getName() + " v" + getDescription().getVersion();
	}
}

package de.chipf0rk.MuhPlots;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import de.chipf0rk.Helpers;
import de.chipf0rk.MuhPlots.exceptions.MuhInitException;

@SuppressWarnings("deprecation")
public final class MuhPlots extends JavaPlugin {
	// Helper instances
	MessageSender msg;
	PlotActions actions;
	
	// Plugin instances
	WorldGuardPlugin worldGuard;
	WorldEditPlugin worldEdit;

	// DB
	DatabaseManager dbm;
	public DatabaseManager getDatabaseManager() { return this.dbm; }
	
	// Plot schematic
	File plotFile;
	CuboidClipboard plotSchematic;

	// Logging
	void log(Level level, String msg) {
		getLogger().log(level, msg);
	}
	void info(String msg) { log(Level.INFO, msg); }
	void severe(String msg) { log(Level.SEVERE, msg); }
	void warn(String msg) { log(Level.WARNING, msg); }

	@Override
	public void onEnable() {
		// Save a copy of the default config.yml
		this.saveDefaultConfig();
		
		// Get plugin instances
		this.worldGuard = WGBukkit.getPlugin();
		try {
			this.worldEdit = WGBukkit.getPlugin().getWorldEdit();
		} catch (CommandException e1) {
			severe("WorldEdit is not available; can't initialise MuhPlots");
			return;
		}
		
		// Load plot configuration values
		ConfigurationSection worldsCfg = this.getConfig().getConfigurationSection("plotworlds");
		
		info(Helpers.join(worldsCfg.getKeys(false), ", "));

		for(String worldName : worldsCfg.getKeys(false)) {
			ConfigurationSection worldCfg = worldsCfg.getConfigurationSection(worldName);
			World w = getServer().getWorld(worldName);
			
			int maxPerPlayer = worldCfg.getInt("max_per_player");
			boolean unprotectedPlotsArePublic = worldCfg.getBoolean("unprotected_are_public");
			
			try {
				new PlotWorld(w, maxPerPlayer, unprotectedPlotsArePublic);
				info("Initialised plot world " + w.getName());
			} catch (MuhInitException e) {
				severe("Could not initialize plot world " + w.getName() + " - world will not be managed by MuhPlots!");
				severe(e.getMessage());
			}
		}
		
		// Try loading the plot schematic file
		plotFile = new File(getDataFolder(), "plot.schematic");
		if(plotFile.exists()) {
			try {
				this.plotSchematic = SchematicFormat.MCEDIT.load(plotFile);
			} catch (DataException | IOException e) {
				severe("An error occurred when loading the plot file!");
			}
		}
		else {
			warn("The plot file does not exist! Plot resetting commands will not work.");
		}
		
		// Create helper instances
		try {
			this.dbm = new DatabaseManager(this);
			this.msg = new MessageSender(this);
			this.actions = new PlotActions(this);
		} catch(MuhInitException e) {
			severe("Initialisation exception: " + e.getMessage());
			return;
		}
		
		// Create Bukkit event listeners
		new PlayerJoinLeaveListener(this);
		
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
		// Close DB connection
		this.dbm.close();
		
		info(getFullName() + " has been disabled.");
	}
	
	public String getFullName() {
		return getName() + " v" + getDescription().getVersion();
	}
}

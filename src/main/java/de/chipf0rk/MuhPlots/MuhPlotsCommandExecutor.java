package de.chipf0rk.MuhPlots;

import java.util.Arrays;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.chipf0rk.MuhPlots.MessageSender.State;

public class MuhPlotsCommandExecutor implements CommandExecutor {
	private MuhPlots plugin;
	private MessageSender msg;
	private PlotHelpers helpers;
	private PlotActions actions;
	
	// This is a list of commands that operate on the plot where a player is standing
	// We use this list to quickly determine outside of these commands
	private final List<String> cmdsOperatingOnCurrentPlot = Arrays.asList(
		"protect",
		"clear"
	);
	
	MuhPlotsCommandExecutor(MuhPlots plugin) {
		this.plugin = plugin;
		this.msg = plugin.msg;
		this.actions = plugin.actions;
		this.helpers = plugin.helpers;
	}
	
	// See discussion at https://github.com/Bukkit/Bukkit/commit/4bc86be459a7ce310f523ca03e2908c1d29956f4
	@SuppressWarnings("deprecation")
	Player getPlayerByName(Player issuer, String name) {
		return issuer.getServer().getPlayer(name);
	}
	
	public boolean onCommand(CommandSender sender, Command c, String label, String[] arguments) {
		if (!(sender instanceof Player)) {
			msg.send(sender, State.FAILURE, "This command can't be used from the console.");
			return true;
		}
		
		// === Help
		if(arguments.length == 0 || "help".equalsIgnoreCase(arguments[0])) {
			msg.sendHelpTo(sender);
			return true;
		}
		
		// === Commands
		Player player = (Player) sender;
		World world = player.getWorld();
		ProtectedRegion plot = helpers.getCurrentPlot(player);
		
		String cmd = arguments[0].toLowerCase();
		List<String> args = Arrays.asList(arguments).subList(1, arguments.length);
		
		// check if WorldGuard knows of a RegionManager for the world the player is in
		if(WGBukkit.getRegionManager(world) == null) {
			msg.send(player, State.FAILURE, "The world you're in doesn't seem to be managed by WorldGuard.");
			return true;
		}
		// then check if the world is a plot world, as set in the plugin configuration
		if(!plugin.plotWorlds.contains(world.getName())) {
			msg.send(player, State.FAILURE, "The world you're in is not a plot world.");
			return true;
		}
		
		// check if player has permission to use this command
		if(!checkPermission(player, cmd)) {
			msg.send(player, State.FAILURE, "Sorry, you don't have permission to use this command.");
			return true;
		}
		
		// check if the player is standing inside a plot region if the issued command requires one
		if(cmdsOperatingOnCurrentPlot.contains(cmd)) {
			if(plot == null) {
				msg.send(player, State.FAILURE, "There's no plot where you're standing.");
				return true;
			}
			if(!helpers.isPlot(plot)) {
				msg.send(player,State.FAILURE, "Sorry, the region you're in doesn't seem to be a plot region!");
				return true;
			}
		}
		
		// execute command
		switch(cmd) {
		case "protect":
			if(!helpers.canProtectPlot(player)) {
				msg.send(player, State.FAILURE, "Sorry, you can't protect any more plots.");
				return true;
			}
			if(plot.hasMembersOrOwners()) {
				msg.send(player, State.FAILURE, "Sorry, this plot is already protected by someone.");
				return true;
			}
			
			else {
				actions.protectPlot(plot, player);
				msg.send(player, State.SUCCESS, "You've successfully protected plot #" + helpers.getNumber(plot.getId()) + "!");
			}

			return true;

		case "clear":
			actions.clearPlot(plot, player);
			msg.send(player, State.SUCCESS, "You've cleared plot #" + helpers.getNumber(plot.getId()) + ".");
			return true;
			
		default:
			msg.send(player, State.FAILURE, "Unknown command. Use /mp help to get a list of all commands.");
			return true;
		}
	}
	
	private boolean checkPermission(Player player, String cmd) {
		Permissions perm = Permissions.getByName(cmd);
		if(perm != null) {
			return player.hasPermission(perm.toString());
		}
		else {
			plugin.warn("No matching permission for command " + cmd + " found; returning false");
			return false;
		}
	}
}

package de.chipf0rk.MuhPlots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.chipf0rk.MuhPlots.MessageSender.State;

public class MuhPlotsCommandExecutor implements CommandExecutor {
	private MuhPlots plugin;
	private MessageSender msg;
	private PlotHelpers helpers;
	private PlotActions actions;

	// This is a list of commands that operate on the plot where a player is standing
	// We use this list to DRY-style determine if the commands can be executed given the players' position
	private final List<String> cmdsOperatingOnCurrentPlot = Arrays.asList(
		"protect",
		"info",
		"clear", "reset", "delete"
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
		RegionManager regionManager = WGBukkit.getRegionManager(world);
		ProtectedRegion plot = helpers.getCurrentPlot(player);
		String plotId = "<unknown>";

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
				msg.send(player,State.FAILURE, "The region you're in doesn't seem to be a plot region.");
				return true;
			}

			plotId = "#" + helpers.getNumber(plot.getId());
		}

		// Execute the commands!
		// NOTE: We use {} around each case block. This is intentional, to isolate the scope of the block for each case.
		// If we didn't do this, duplicate variable names in two different cases could cause trouble.
		switch(cmd) {
		case "list": {
			List<String> playersPlots = new ArrayList<String>();
			for(ProtectedRegion region : regionManager.getRegions().values()) {
				if(region.isOwner((LocalPlayer) player)) {
					playersPlots.add("#" + helpers.getNumber(region.getId()));
				}
			}

			if(playersPlots.size() > 0) msg.send(player, State.NOTICE, "You own the following plots: " + String.join(", ", playersPlots));
			else msg.send(player, State.NOTICE, "You don't own any plots in this world yet.");
			
			return true;
		}

		case "protect": {
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
		}

		case "info": {
			Set<String> owners = plot.getOwners().getPlayers();
			Set<String> members = plot.getMembers().getPlayers();

			msg.send(player, State.NOTICE, "Plot ID: " + plotId);
			msg.send(player, State.NOTICE, owners.size() > 0 ?
					"Owners: " + String.join(", ", owners) :
					"This plot has no owner.");
			msg.send(player, State.NOTICE, members.size() > 0 ?
					"Members: " + String.join(", ", members) :
					"This plot has no members.");
			
			return true;
		}

		case "clear": {
			actions.clearPlot(plot, player);
			msg.send(player, State.SUCCESS, "You've successfully cleared plot " + plotId + "!");
			
			return true;
		}

		case "reset": {
			boolean couldReset = actions.resetPlot(plot, player, plugin.plotSchematic);
			if(couldReset) msg.send(player, State.SUCCESS, "You've successfully reset plot " + plotId + "!");
			else msg.send(player, State.FAILURE, "Sorry, couldn't reset the plot. Check the logs.");
			
			return true;
		}

		case "delete": {
			boolean couldReset = actions.resetPlot(plot, player, plugin.plotSchematic);
			if(couldReset) {
				actions.clearPlot(plot, player);
				msg.send(player, State.SUCCESS, "You've successfully deleted plot " + plotId + "!");
			}
			else msg.send(player, State.FAILURE, "Sorry, couldn't reset the plot. Check the logs.");
			
			return true;
		}
		
		case "setowner": {
			
		}

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

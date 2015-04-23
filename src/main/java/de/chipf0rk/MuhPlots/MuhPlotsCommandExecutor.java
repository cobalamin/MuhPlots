package de.chipf0rk.MuhPlots;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.evilmidget38.NameFetcher;
import com.evilmidget38.UUIDFetcher;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.chipf0rk.Helpers;
import de.chipf0rk.MuhPlots.MessageSender.State;

public class MuhPlotsCommandExecutor implements CommandExecutor {
	private MuhPlots plugin;
	private MessageSender msg;
	private PlotHelpers helpers;
	private PlotActions actions;
	
	private DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");

	// This is a list of commands that operate on the plot where a player is standing
	// We use this list to determine if the commands can be executed given the players' position
	private final List<String> cmdsOperatingOnCurrentPlot = Arrays.asList(
		"protect", "setowner",
		"add", "rem",
		"info",
		"clear", "reset", "delete"
	);

	MuhPlotsCommandExecutor(MuhPlots plugin) {
		this.plugin = plugin;
		this.msg = plugin.msg;
		this.actions = plugin.actions;
		this.helpers = plugin.helpers;
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
		LocalPlayer lp = plugin.worldGuard.wrapPlayer(player);
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
			SortedSet<Integer> playersPlots = new TreeSet<Integer>(new Comparator<Integer>() {
				@Override
				public int compare(Integer i1, Integer i2) { return i1 - i2; }
			});
			
			for(ProtectedRegion region : regionManager.getRegions().values()) {
				if(region.isOwner(lp)) {
					playersPlots.add(helpers.getNumber(region.getId()));
				}
			}

			if(playersPlots.size() > 0) msg.send(player, State.NOTICE, "You own the following plots: " + Helpers.join(playersPlots, ", ", "#"));
			else msg.send(player, State.NOTICE, "You don't own any plots in this world yet.");
			
			return true;
		}

		case "protect": {
			if(!helpers.canProtectPlot(player)) {
				msg.send(player, State.FAILURE, "Sorry, you can't protect any more plots.");
				return true;
			}
			
			DefaultDomain existingOwners = plot.getOwners();
			if(existingOwners.size() > 0) {
				if(existingOwners.contains(player.getUniqueId())) {
					msg.send(player, State.NOTICE, "You already own this plot.");
				}
				else {
					msg.send(player, State.FAILURE, "Sorry, this plot is already protected by someone.");
				}
				return true;
			}

			actions.protectPlot(plot, player);
			msg.send(player, State.SUCCESS, "You've successfully protected plot #" + helpers.getNumber(plot.getId()) + "!");
			
			break;
		}

		case "info": {
			Set<UUID> ownerUUIDs = plot.getOwners().getUniqueIds();
			Set<UUID> memberUUIDs = plot.getMembers().getUniqueIds();
			Collection<String> owners;
			Collection<String> members;
			try {
				owners = NameFetcher.getNames(ownerUUIDs).values();
				members = NameFetcher.getNames(memberUUIDs).values();
			} catch (Exception e) {
				msg.send(player, State.FAILURE, "Sorry, an error occurred while fetching player names.");
				plugin.severe(e.getMessage());
				return true;
			}
			
			Date ownerLastSeen = null;
			try {
				ownerLastSeen = plugin.dbm.getPlayerLastSeen(ownerUUIDs.iterator().next());
			} catch (SQLException e) {
				plugin.severe("SQLException occurred when trying to fetch last seen data for a plot owner!");
				e.printStackTrace();
			}
			String ownerLastSeenString = ownerLastSeen != null ?
				dateFormat.format(ownerLastSeen) :
				"Never";

			msg.send(player, State.NOTICE, "Plot ID: " + plotId);
			if(owners.size() > 0) {
				msg.send(player, State.NOTICE, "Owners: " + Helpers.join(owners, ", "));
				msg.send(player, State.NOTICE, "Owner last seen: " + ownerLastSeenString);
			}
			else {
				msg.send(player, State.NOTICE, "This plot has no owner.");
			}
			msg.send(player, State.NOTICE, members.size() > 0 ?
					"Members: " + Helpers.join(members, ", ") :
					"This plot has no members.");
			break;
		}

		case "clear": {
			actions.clearPlot(plot, player);
			msg.send(player, State.SUCCESS, "You've successfully cleared plot " + plotId + "!");
			break;
		}

		case "reset": {
			boolean couldReset = actions.resetPlot(plot, player, plugin.plotSchematic);
			if(couldReset) msg.send(player, State.SUCCESS, "Reset plot " + plotId + "!");
			else msg.send(player, State.FAILURE, "Sorry, couldn't reset the plot. Check the logs.");
			break;
		}

		case "delete": {
			boolean couldReset = actions.resetPlot(plot, player, plugin.plotSchematic);
			if(couldReset) {
				actions.clearPlot(plot, player);
				msg.send(player, State.SUCCESS, "Deleted plot " + plotId + "!");
			}
			else msg.send(player, State.FAILURE, "Sorry, couldn't reset the plot. Check the logs.");
			break;
		}
		
		case "setowner": {
			if(args.size() < 1) {
				msg.send(player, State.FAILURE, "Please specify a player to protect this plot for.");
				return true;
			}
			String playerName = args.get(0);
			boolean couldSetOwner = actions.setOwner(plot, player, playerName);
			
			if(couldSetOwner) msg.send(player, State.SUCCESS, "You've successfully protected plot " + plotId + " for " + playerName + "!");
			else msg.send(player, State.FAILURE, "Something went wrong when trying to protect this plot. Double check the player name.");
			return true;
		}
		
		case "add": {
			if(args.size() < 1) {
				msg.send(player, State.FAILURE, "Please specify at least one player to add to this plot.");
				return true;
			}
			try {
				Collection<UUID> uuids = UUIDFetcher.getUUIDsOf(args).values();				
				DefaultDomain members = plot.getMembers();
				
				if(uuids.size() < args.size()) {
					msg.send(player, State.NOTICE, "Couldn't resolve all names to UUIDs. Please check the spelling.");
				}
				
				for(UUID uuid : uuids) {
					 members.addPlayer(uuid);
				}
				msg.send(player, State.SUCCESS, "Added " + uuids.size() + " player(s) to your plot.");
			} catch (Exception e) {
				msg.send(player, State.FAILURE, "An error occurred when resolving names to UUIDs.");
				plugin.severe(e.getMessage());
			}
			break;
		}
		
		case "rem": {
			if(args.size() < 1) {
				msg.send(player, State.FAILURE, "Please specify at least one player to remove from this plot.");
				return true;
			}
			try {
				Collection<UUID> uuids = UUIDFetcher.getUUIDsOf(args).values();
				DefaultDomain members = plot.getMembers();
				
				if(uuids.size() < args.size()) {
					msg.send(player, State.NOTICE, "Couldn't resolve all names to UUIDs. Please check the spelling.");
				}
				
				for(UUID uuid : uuids) {
					members.removePlayer(uuid);
				}
				msg.send(player, State.SUCCESS, "Removed " + uuids.size() + " player(s) from your plot.");
			} catch (Exception e) {
				msg.send(player, State.FAILURE, "An error occurred when resolving names to UUIDs.");
				plugin.severe(e.getMessage());
			}
			break;
		}
		
		case "tp": {
			if(args.size() < 1) {
				msg.send(player, State.FAILURE, "Please specify a plot to teleport to.");
				return true;
			}
			// parse for number, then return full id string
			String id = args.get(0);
			String tpPlotId = helpers.getId(helpers.getNumber(id));
			ProtectedRegion tpPlot = regionManager.getRegion(tpPlotId);
			
			if(tpPlot == null) msg.send(player, State.FAILURE, "Couldn't find a plot with ID " + id);
			else {
				actions.teleportToPlot(tpPlot, player);
				msg.send(player, State.SUCCESS, "You've been teleported.");
			}
			break;
		}
		
		case "find": {
			String freePlotId = null;
			try {
				freePlotId = plugin.dbm.getFreePlotId(world);
			} catch (SQLException e) {
				msg.send(player, State.FAILURE, "An exception occurred; could not find a free plot for you :(");
				e.printStackTrace();
				return true;
			}
			
			if(freePlotId != null) {
				ProtectedRegion freePlot = regionManager.getRegion(freePlotId);
				String plotNumber = "#" + helpers.getNumber(freePlotId);
				if(freePlot != null) {
					msg.send(player, State.SUCCESS, "Free plot found: " + plotNumber);
					actions.teleportToPlot(freePlot, player);
				}
				else {
					msg.send(player, State.FAILURE, "Found free plot " + plotNumber + " in the database, but it doesn't exist");
				}
			}
			else {
				msg.send(player, State.FAILURE, "Could not find any free plots!");
			}

			return true;
		}

		default: msg.send(player, State.FAILURE, "Unknown command. Use /mp help to get a list of all commands.");
		}
		
		return true;
	}

	private boolean checkPermission(Player player, String cmd) {
		Permissions perm = Permissions.getByName(cmd);
		// TODO change this to return true as soon as the basic functionality is there
		if(perm != null) {
			return player.hasPermission(perm.toString());
		}

		return true;
	}
}

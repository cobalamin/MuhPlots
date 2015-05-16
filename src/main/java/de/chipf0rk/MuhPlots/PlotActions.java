package de.chipf0rk.MuhPlots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.evilmidget38.UUIDFetcher;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

@SuppressWarnings("deprecation")
public class PlotActions {
	private MuhPlots plugin;
	private EditSessionFactory esf;

	public PlotActions(MuhPlots plugin) {
		this.plugin = plugin;
		this.esf = new EditSessionFactory();
	}

	// Warning: the following actions are not checked for permissions at any time.
	// Permission checking should be done by other classes using these methods.
	public boolean setOwner(ProtectedRegion plot, Player issuer, String playerName) {
		plot.setFlag(DefaultFlag.BUILD, null);
		
		UUID playerUUID;
		try {
			playerUUID = UUIDFetcher.getUUIDsOf(Arrays.asList(playerName)).get(playerName);
		} catch (Exception e) {
			plugin.severe(e.getMessage());
			return false;
		}
		if(playerUUID == null) return false;
		
		DefaultDomain owners = new DefaultDomain();
		owners.addPlayer(playerUUID);
		plot.setOwners(owners);
		
		setPlotSigns(plot, issuer.getWorld(), playerName);
		
		return true;
	}

	public void protectPlot(ProtectedRegion plot, Player player) {
		plot.setFlag(DefaultFlag.BUILD, null);

		DefaultDomain owners = new DefaultDomain();
		owners.addPlayer(player.getUniqueId());
		plot.setOwners(owners);

		setPlotSigns(plot, player);
	}

	public void clearPlot(ProtectedRegion plot, PlotWorld pw, Player player) {
		if(pw.unprotectedPlotsArePublic) {
			// allow building to anyone
			plot.setFlag(DefaultFlag.BUILD, State.ALLOW);
		}

		plot.setOwners(new DefaultDomain()); // sets the owners to empty
		plot.setMembers(new DefaultDomain()); // sets the members to empty

		resetPlotSigns(plot, player.getWorld());
	}

	public boolean resetPlot(ProtectedRegion plot, Player player, CuboidClipboard plotSchematic) {
		World world = player.getWorld();
		PlotWorld pw = PlotWorld.getPlotWorld(world);
		LocalPlayer lp = plugin.worldEdit.wrapPlayer(player);
		if(plotSchematic != null) {
			try {
				int plotSize = pw.plotSize;
				int maxBlocks = plotSize * plotSize * world.getMaxHeight();
				Vector minPoint = plot.getMinimumPoint();
				
				EditSession es = esf.getEditSession(new BukkitWorld(world), maxBlocks, lp);
				plotSchematic.place(es, minPoint, false);
				
				resetPlotSigns(plot, world);
			} catch (MaxChangedBlocksException ex) {
				plugin.severe("MaxChangedBlocksException occurred while trying to reset a plot.");
				return false;
			}
		}

		plugin.warn("Plots can't be reset without a plot schematic file");
		return false;
	}
	
	public void teleportToPlot(ProtectedRegion plot, Player player) {
		BlockVector minPoint = plot.getMinimumPoint();
		World world = player.getWorld();
		PlotWorld pw = PlotWorld.getPlotWorld(world);
		
		Location safeLoc = new Location(world,
			minPoint.getX() - 0.5D, pw.walkwayY,
			minPoint.getZ() - 0.5D);

		player.teleport(safeLoc);
	}
	
	// === private
	
	private void resetPlotSigns(ProtectedRegion plot, World world) {
		List<Block> signs = getSignBlocksOfPlot(plot, world);

		for (Block sign : signs) {
			Material type = sign.getType();
			if (type == Material.SIGN || type == Material.SIGN_POST) {
				Sign currentSign = (Sign) sign.getState();
				currentSign.setLine(0, "---------------");
				currentSign.setLine(1, "Plot ID");
				currentSign.setLine(2, "Name");
				currentSign.setLine(3, "---------------");
				currentSign.update();
			}
		}
	}

	private void setPlotSigns(ProtectedRegion plot, World world, String ownerName) {
		List<Block> signs = getSignBlocksOfPlot(plot, world);
		PlotWorld pw = PlotWorld.getPlotWorld(world);
		
		for (Block sign : signs) {
			Material type = sign.getType();
			if (type == Material.SIGN || type == Material.SIGN_POST) {
				Sign currentSign = (Sign) sign.getState();
				currentSign.setLine(0, "---------------");
				currentSign.setLine(1, "Plot ID: " + ChatColor.BLUE + pw.getShortPlotId(plot.getId()));
				currentSign.setLine(2, ChatColor.WHITE + ownerName);
				currentSign.setLine(3, "---------------");
				currentSign.update();
			}
		}
	}
	private void setPlotSigns(ProtectedRegion plot, Player player) {
		setPlotSigns(plot, player.getWorld(), player.getName());
	}
	
	private List<Block> getSignBlocksOfPlot(ProtectedRegion plot, World world) {
		PlotWorld pw = PlotWorld.getPlotWorld(world);
		BlockVector min = plot.getMinimumPoint();
		Location minPoint = new Location(world,
				min.getBlockX() - 1.0, 0.0, min.getBlockZ() - 1.0);
		List<Block> blocks = new ArrayList<Block>(4);
		
		int size = plugin.getConfig().getInt("plots.size") + 1; // signs are 1 block outside of the plot
		int walkwayY = pw.walkwayY;

		blocks.add(world.getBlockAt(
				minPoint.getBlockX(),
				walkwayY,
				minPoint.getBlockZ()));
		blocks.add(world.getBlockAt(
				minPoint.getBlockX() + size,
				walkwayY,
				minPoint.getBlockZ()));
		blocks.add(world.getBlockAt(
				minPoint.getBlockX(),
				walkwayY,
				minPoint.getBlockZ() + size));
		blocks.add(world.getBlockAt(
				minPoint.getBlockX() + size,
				walkwayY,
				minPoint.getBlockZ() + size));
		
		return blocks;
	}
}

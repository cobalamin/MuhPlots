package de.chipf0rk.MuhPlots;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class PlotActions {
	private MuhPlots plugin;
	private EditSessionFactory esf;
	private int plotSize;

	public PlotActions(MuhPlots plugin) {
		this.plugin = plugin;
		this.esf = new EditSessionFactory();
		this.plotSize = plugin.getConfig().getInt("plots.size");
	}

	// Warning: the following actions are not checked for permissions at any time.
	// Permission checking should be done by other classes using these methods.
	public void protectPlot(ProtectedRegion plot, Player player) {
		plot.setFlag(DefaultFlag.BUILD, null);

		DefaultDomain owners = new DefaultDomain();
		owners.addPlayer(player.getUniqueId());
		plot.setOwners(owners);

		setPlotSigns(plot, player);
	}

	public void clearPlot(ProtectedRegion plot, Player player) {
		if(plugin.getConfig().getBoolean("plots.unprotected_are_public")) {
			// allow building to anyone
			plot.setFlag(DefaultFlag.BUILD, State.ALLOW);
		}

		plot.setOwners(new DefaultDomain()); // sets the owners to empty
		plot.setMembers(new DefaultDomain()); // sets the members to empty

		resetPlotSigns(plot, player.getWorld());
	}

	public boolean resetPlot(ProtectedRegion plot, Player player, CuboidClipboard plotSchematic) {
		World world = player.getWorld();
		if(plotSchematic != null) {
			try {
				int maxBlocks = plotSize * plotSize * world.getMaxHeight();
				Vector minPoint = plot.getMinimumPoint();

				EditSession es = esf.getEditSession((LocalWorld) world, maxBlocks, (LocalPlayer) player);
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

	private void setPlotSigns(ProtectedRegion plot, Player player) {
		World world = player.getWorld();
		List<Block> signs = getSignBlocksOfPlot(plot, world);
		
		for (Block sign : signs) {
			Material type = sign.getType();
			if (type == Material.SIGN || type == Material.SIGN_POST) {
				Sign currentSign = (Sign) sign.getState();
				currentSign.setLine(0, "---------------");
				currentSign.setLine(1, "Plot ID: " + ChatColor.BLUE + plugin.helpers.getNumber(plot.getId()));
				currentSign.setLine(2, ChatColor.WHITE + player.getName());
				currentSign.setLine(3, "---------------");
				currentSign.update();
			}
		}
	}
	
	private List<Block> getSignBlocksOfPlot(ProtectedRegion plot, World world) {
		BlockVector min = plot.getMinimumPoint();
		Location minPoint = new Location(world,
				min.getBlockX() - 1.0, 0.0, min.getBlockZ() - 1.0);
		List<Block> blocks = new ArrayList<Block>(4);
		
		int size = plugin.getConfig().getInt("plots.size") + 1; // signs are 1 block outside of the plot
		int walkway_y = plugin.getConfig().getInt("plots.walkway_y");

		blocks.add(world.getBlockAt(
				minPoint.getBlockX(),
				walkway_y,
				minPoint.getBlockZ()));
		blocks.add(world.getBlockAt(
				minPoint.getBlockX() + size,
				walkway_y,
				minPoint.getBlockZ()));
		blocks.add(world.getBlockAt(
				minPoint.getBlockX(),
				walkway_y,
				minPoint.getBlockZ() + size));
		blocks.add(world.getBlockAt(
				minPoint.getBlockX() + size,
				walkway_y,
				minPoint.getBlockZ() + size));
		
		return blocks;
	}
}

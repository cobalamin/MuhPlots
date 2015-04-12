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
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class PlotActions {
	private MuhPlots plugin;

	public PlotActions(MuhPlots plugin) {
		this.plugin = plugin;
	}

	public void protectPlot(ProtectedRegion plot, Player player) {
		plot.setFlag(DefaultFlag.BUILD, null);

		DefaultDomain owners = new DefaultDomain();
		owners.addPlayer(player.getUniqueId());
		plot.setOwners(owners);
		setPlotSigns(plot, player);
	}

	public void clearPlot(ProtectedRegion plot, Player player) {
		plot.setFlag(DefaultFlag.BUILD, State.ALLOW); // allow building to anyone if plot is ownerless
		World world = player.getWorld();

		plot.setOwners(new DefaultDomain()); // sets the owners to empty
		plot.setMembers(new DefaultDomain()); // sets the members to empty

		resetPlotSigns(plot, world);
	}
	
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
				currentSign.setLine(1,
						"Plot ID: " + ChatColor.BLUE + plugin.plotHelpers.getNumber(plot.getId()));
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
		
		int size = plugin.getConfig().getInt("plotsize") + 1; // signs are 1 block outside of the plot
		int walkway_y = plugin.getConfig().getInt("walkway_y");

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

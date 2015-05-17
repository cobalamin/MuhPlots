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
		LocalPlayer lp = plugin.worldEdit.wrapPlayer(player);
		if(plotSchematic != null) {
			try {
				BlockVector minPoint = plot.getMinimumPoint();
				BlockVector maxPoint = plot.getMaximumPoint();
				int xSize = (maxPoint.getBlockX() - minPoint.getBlockX()) + 1;
				int zSize = (maxPoint.getBlockZ() - minPoint.getBlockZ()) + 1;
				int maxBlocks = xSize * zSize * world.getMaxHeight();
				
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
		
		int x = minPoint.getBlockX();
		int z = minPoint.getBlockZ();
		Location safeLoc = new Location(world,
			(double)x - 0.5D,
			findFirstFreeBlock(world, x, z, true),
			(double)z - 0.5D);
		
		plugin.info(safeLoc.toString());

		player.teleport(safeLoc);
	}
	
	// === private
	
	private void resetPlotSigns(ProtectedRegion plot, World world) {
		List<Block> signs = getSignBlocksOfPlot(plot, world);

		for (Block sign : signs) {
			Material type = sign.getType();
			if (isSign(type)) {
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
			if (isSign(type)) {
				Sign currentSign = (Sign) sign.getState();
				currentSign.setLine(0, "---------------");
				currentSign.setLine(1, "Plot " + ChatColor.BLUE + pw.getShortPlotId(plot.getId()));
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
		BlockVector min = plot.getMinimumPoint();
		BlockVector max = plot.getMaximumPoint();

		int minX = min.getBlockX() - 1;
		int minZ = min.getBlockZ() - 1;
		int maxX = max.getBlockX() + 1;
		int maxZ = max.getBlockZ() + 1;
		int walkwayY = findFirstFreeBlock(world, minX, minZ, true);

		List<Block> blocks = new ArrayList<Block>(4);
		if(walkwayY < 0) { return blocks; } // early exit without any blocks if no free air block was found

		blocks.add(world.getBlockAt(
				minX,
				walkwayY,
				minZ));
		blocks.add(world.getBlockAt(
				maxX,
				walkwayY,
				minZ));
		blocks.add(world.getBlockAt(
				minX,
				walkwayY,
				maxZ));
		blocks.add(world.getBlockAt(
				maxX,
				walkwayY,
				maxZ));
		
		return blocks;
	}
	
	private int findFirstFreeBlock(World world, int x, int z, boolean includeSigns) {
		for(int y = 0; y < world.getMaxHeight(); y++) {
			Material blockType = world.getBlockAt(x, y, z).getType();

			if(blockType == Material.AIR
					|| includeSigns && isSign(blockType)) { return y; }
		}
		
		return -1;
	}
	
	private boolean isSign(Material m) {
		return m == Material.SIGN_POST || m == Material.WALL_SIGN;
	}
}

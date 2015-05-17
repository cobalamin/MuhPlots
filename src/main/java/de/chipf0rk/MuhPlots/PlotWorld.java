package de.chipf0rk.MuhPlots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.chipf0rk.Helpers;
import de.chipf0rk.MuhPlots.exceptions.MuhInitException;

public class PlotWorld {
	public static HashMap<String, PlotWorld> allWorlds = new HashMap<String, PlotWorld>();

	public final World world;
	public final String plotPrefix = "muhplot_";
	public final int maxPerPlayer;
	public final boolean unprotectedPlotsArePublic;
	
	public PlotWorld(World w, int maxPerPlayer, boolean unprotectedPlotsArePublic) throws MuhInitException {
		List<String> errs = new ArrayList<String>();
		if(maxPerPlayer <= 0) errs.add("Invalid maxPerPlayer setting: " + maxPerPlayer);
		
		if(errs.size() > 0) {
			throw new MuhInitException(Helpers.join(errs, "\n"));
		}
		
		this.world = w;

		this.maxPerPlayer = maxPerPlayer;
		this.unprotectedPlotsArePublic = unprotectedPlotsArePublic;
		
		allWorlds.put(world.getName(), this);
	}
	
	public boolean canProtectPlot(Player player) {
		RegionManager regionManager = WGBukkit.getRegionManager(player.getWorld());
		LocalPlayer lp = WGBukkit.getPlugin().wrapPlayer(player);
		
		int protectedRegions = regionManager.getRegionCountOfPlayer(lp);
		
		return protectedRegions < this.maxPerPlayer || Permissions.UNLIMITED.doesHave(player);
	}
	
	public String getFullPlotId(String shortId) {
		return this.plotPrefix + shortId;
	}
	public String getShortPlotId(String fullId) {
		return StringUtils.replaceOnce(fullId, this.plotPrefix, "");
	}

	public boolean isPlot(ProtectedRegion region) {
		return region.getId().startsWith(this.plotPrefix);
	}

	public ProtectedRegion getFreePlot() {
		RegionManager regionManager = WGBukkit.getRegionManager(this.world);

		for(ProtectedRegion rg : regionManager.getRegions().values()) {
			if(!rg.hasMembersOrOwners()
					&& rg.getId().startsWith(this.plotPrefix)) {
				return rg;
			}
		}
		
		return null;
	}

	// --- static
	
	public static boolean isPlotWorld(World w) {
		return allWorlds.containsKey(w.getName());
	}
	public static PlotWorld getPlotWorld(String worldName) {
		return allWorlds.get(worldName);
	}
	public static PlotWorld getPlotWorld(World w) {
		return allWorlds.get(w.getName());
	}
	
	public static ProtectedRegion getCurrentPlot(Player player) {
		RegionManager regionManager = WGBukkit.getRegionManager(player.getWorld());
		ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(player.getLocation());
		
		if(applicableRegions.size() > 0) {
			return applicableRegions.iterator().next();
		}
		return null;
	}
}

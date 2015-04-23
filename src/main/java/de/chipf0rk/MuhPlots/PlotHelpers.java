package de.chipf0rk.MuhPlots;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.chipf0rk.MuhPlots.exceptions.MuhInitException;

public class PlotHelpers {
	private MuhPlots plugin;
	private String plotPrefix;
	private int maxPlotsPerPlayer;
	private Pattern numberExtractPattern = Pattern.compile("(\\d+)$");
	
	public PlotHelpers(MuhPlots plugin) throws MuhInitException {
		this.plugin = plugin;
		
		this.plotPrefix = plugin.plotPrefix;
		// check if plot prefix is blank
		if(StringUtils.isBlank(plotPrefix)) {
			throw new MuhInitException("Invalid plot prefix in the configuration: " + plotPrefix);
		}
		
		this.maxPlotsPerPlayer = plugin.getConfig().getInt("plots.max_per_player");
		if(!(maxPlotsPerPlayer > 0)) {
			throw new MuhInitException("Invalid plots.max_per_player setting in the configuration.");
		}
	}
	
	public String getId(int num) {
		return plotPrefix + num;
	}
	
	public int getNumber(String id) {
		Matcher m = numberExtractPattern.matcher(id);
		if(m.find()) {
			return Integer.parseInt(m.group());
		}

		return -1;
	}
	
	public boolean canProtectPlot(Player player) {
		RegionManager regionManager = plugin.worldGuard.getRegionManager(player.getWorld());
		LocalPlayer lp = plugin.worldGuard.wrapPlayer(player);
		
		int protectedRegions = regionManager.getRegionCountOfPlayer(lp);
		
		return protectedRegions < maxPlotsPerPlayer || Permissions.UNLIMITED.doesHave(player);
	}
	
	public ProtectedRegion getCurrentPlot(Player player) {
		RegionManager regionManager = WGBukkit.getRegionManager(player.getWorld());
		ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(player.getLocation());
		
		if(applicableRegions.size() > 0) {
			return applicableRegions.iterator().next();
		}
		return null;
	}
	
	public boolean isPlot(ProtectedRegion region) {
		return region.getId().startsWith(plotPrefix);
	}
}

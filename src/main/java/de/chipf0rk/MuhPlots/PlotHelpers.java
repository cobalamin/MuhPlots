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
	private Pattern numberExtractPattern = Pattern.compile("(\\d+)$");
	
	public PlotHelpers(MuhPlots plugin) throws MuhInitException {
		this.plugin = plugin;
		this.plotPrefix = plugin.getConfig().getString("plots.prefix");
		// check if plot prefix is blank
		if(StringUtils.isBlank(plotPrefix)) {
			throw new MuhInitException("Invalid plot prefix in the configuration: " + plotPrefix);
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
		RegionManager regionManager = WGBukkit.getRegionManager(player.getWorld());
		
		int protectedRegions = regionManager.getRegionCountOfPlayer((LocalPlayer) player);
		int maxPlotCount = plugin.getConfig().getInt("plots.max_per_player");
		
		return protectedRegions < maxPlotCount || Permissions.UNLIMITED.doesHave(player);
	}
	
	public ProtectedRegion getCurrentPlot(Player player) {
		RegionManager regionManager = WGBukkit.getRegionManager(player.getWorld());
		ApplicableRegionSet applicapleRegions = regionManager.getApplicableRegions(player.getLocation());
		return applicapleRegions.iterator().next();
	}
	
	public boolean isPlot(ProtectedRegion region) {
		return region.getId().startsWith(plotPrefix);
	}
}

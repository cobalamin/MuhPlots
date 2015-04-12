package de.chipf0rk.MuhPlots;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlotHelpers {
	private MuhPlots plugin;
	private String plotPrefix;
	private Pattern numberExtractPattern = Pattern.compile("(\\d+)$");
	
	public PlotHelpers(MuhPlots plugin) {
		this.plugin = plugin;
		this.plotPrefix = plugin.getConfig().getString("plotprefix");
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
}

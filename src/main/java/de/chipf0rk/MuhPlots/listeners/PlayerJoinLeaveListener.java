package de.chipf0rk.MuhPlots.listeners;

import lib.PatPeter.SQLibrary.Database;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.chipf0rk.MuhPlots.MuhPlots;
 
public final class PlayerJoinLeaveListener implements Listener {
	MuhPlots plugin;
	
	public PlayerJoinLeaveListener(MuhPlots plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Database db = this.plugin.getDB();
		if(db != null) {
			// update/insert timestamp and guid + name
		}
	}
	
	@EventHandler
	public void onLogout(PlayerQuitEvent event) {
		Database db = this.plugin.getDB();
		if(db != null) {
			// update/insert timestamp and guid + name
		}
	}
}
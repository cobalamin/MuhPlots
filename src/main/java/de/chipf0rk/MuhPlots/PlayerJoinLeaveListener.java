package de.chipf0rk.MuhPlots;

import java.sql.SQLException;
import java.util.Date;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
 
public final class PlayerJoinLeaveListener implements Listener {
	MuhPlots plugin;
	DatabaseManager dbm;
	
	public PlayerJoinLeaveListener(MuhPlots plugin) {
		this.plugin = plugin;
		this.dbm = plugin.getDatabaseManager();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		updatePlayerLastSeen(event);
	}
	
	@EventHandler
	public void onLogout(PlayerQuitEvent event) {
		updatePlayerLastSeen(event);
	}
	
	private void updatePlayerLastSeen(PlayerEvent event) {
		try {
			dbm.updatePlayerLastSeen(event.getPlayer().getUniqueId(), new Date());
		} catch (SQLException e) {
			plugin.warn("An SQLException occurred when updating player last seen data!");
			e.printStackTrace();
		}
	}
}
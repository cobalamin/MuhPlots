package de.chipf0rk.MuhPlots;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import de.chipf0rk.MuhPlots.exceptions.MuhInitException;
import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;

public class DatabaseManager {
	private Database db;

	public DatabaseManager(MuhPlots plugin) throws MuhInitException {
		db = new SQLite(plugin.getLogger(), 
				"[" + plugin.getName() + "] ",
				plugin.getDataFolder().getAbsolutePath(),
				plugin.getName(),
				".sqlite");
		if(!db.open()) {
			throw new MuhInitException("Could not open a connection to the database! Plugin initialisation failed.");
		}
		
		try {
			setUpDatabase();
		} catch(SQLException e) {
			throw new MuhInitException("Could not set up the database!" + e.getMessage());
		}
	}
	
	// === player last seen methods
	
	public Date getPlayerLastSeen(UUID playerUUID) throws SQLException {
		dbOpenPrecondition();
		PreparedStatement stmt = db.prepare("SELECT last_seen FROM player_history WHERE uuid = ? LIMIT 1");
		stmt.setString(1, playerUUID.toString());
		ResultSet result = stmt.executeQuery();
		
		Timestamp lastSeen = null;
		if(result.next()) {
			lastSeen = result.getTimestamp("last_seen");
		}
		result.close();

		return lastSeen == null ? null : new Date(lastSeen.getTime());
	}
	
	public void updatePlayerLastSeen(UUID playerUUID, Date lastSeen) throws SQLException {
		dbOpenPrecondition();
		PreparedStatement stmt =
				db.prepare("INSERT OR REPLACE INTO player_history (uuid, last_seen) VALUES (?, ?)");
		
		Timestamp timestamp = new Timestamp(lastSeen.getTime());
		stmt.setString(1, playerUUID.toString());
		stmt.setTimestamp(2, timestamp);
		
		stmt.executeUpdate();
		stmt.close();
	}
	
	// === internal methods
	
	private void setUpDatabase() throws SQLException {
		dbOpenPrecondition();
		
		// set up player_history table
		db.query("CREATE TABLE IF NOT EXISTS player_history ( "
				+ "uuid TEXT PRIMARY KEY, "
				+ "last_seen TEXT "
			+ ")").close();
	}
	
	private void dbOpenPrecondition() throws SQLException {
		if (db instanceof SQLite ?
				!db.open() :
				!db.isOpen() && !db.open()) {
			throw new SQLException("Could not open connection to database!");
		}
	}
	
	public void close() {
		if(db != null) {
			db.close();
		}
	}
}

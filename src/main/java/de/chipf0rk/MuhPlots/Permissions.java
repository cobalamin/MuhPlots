package de.chipf0rk.MuhPlots;

import org.bukkit.entity.Player;

// A enum "of strings" to store available permissions
enum Permissions {
	PROTECT, INFO, SETOWNER, CLEAR, RESET, DELETE, FIND, FREE, UNLIMITED;
	
	private String full_perm;

	private Permissions() {
		this.full_perm = "muhplots." + this.name().toLowerCase();
	}

	@Override
	public String toString() {
		return full_perm;
	}
	
	public boolean doesHave(Player player) {
		return player.hasPermission(this.toString());
	}
	
	public static Permissions getByName(String name) {
		try {
			return valueOf(name.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}

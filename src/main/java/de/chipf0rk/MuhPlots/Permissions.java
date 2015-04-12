package de.chipf0rk.MuhPlots;

// A enum "of strings" to store available permissions
enum Permissions {
	PROTECT, SETOWNER, CLEAR, FIND, FREE, UNLIMITED;
	
	private String full_perm;

	private Permissions() {
		this.full_perm = "muhplots." + this.name().toLowerCase();
	}

	@Override
	public String toString() {
		return full_perm;
	}
}

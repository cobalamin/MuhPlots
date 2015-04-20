package de.chipf0rk.MuhPlots.exceptions;

public class MuhInitException extends Exception {
	private static final long serialVersionUID = 1768477657478466780L;
	private String msg;
	
	public MuhInitException(String msg) {
		this.msg = msg;
	}

	@Override
	public String getMessage() {
		return this.msg;
	}
}

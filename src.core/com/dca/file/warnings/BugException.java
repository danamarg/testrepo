package com.dca.file.warnings;

/**
 * Thrown when a bug is noticed.
 * 
 */
public class BugException extends FatalException {
	
	public BugException(String message) {
		super("BUG: " + message);
	}
	
	public BugException(Throwable cause) {
		super("BUG: " + cause.getMessage(), cause);
	}
	
	public BugException(String message, Throwable cause) {
		super("BUG: " + message, cause);
	}
}

package com.dca.file.warnings;

/**
* A superclass for all types of fatal error conditions.  This class is
* abstract so only subclasses can be used.
* 
* @see BugException
* @see ConfigurationException
*/
public abstract class FatalException extends RuntimeException {

	public FatalException() {
	}

	public FatalException(String message) {
		super(message);
	}

	public FatalException(Throwable cause) {
		super(cause);
	}

	public FatalException(String message, Throwable cause) {
		super(message, cause);
	}
	
}


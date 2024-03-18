package com.dca.file.simplesax;

import com.dca.file.warnings.BugException;

public class Reflection {

	
	/**
	 * Handles an InvocationTargetException gracefully.  If the cause is an unchecked
	 * exception it is thrown, otherwise it is encapsulated in a BugException.
	 * <p>
	 * This method has a return type of Error in order to allow writing code like:
	 * <pre>throw Reflection.handleInvocationTargetException(e)</pre>
	 * This allows the compiler verifying that the call will never succeed correctly
	 * and ending that branch of execution.
	 * 
	 * @param e		the InvocationTargetException that occurred (not null).
	 * @return		never returns normally.
	 */
	public static Error handleWrappedException(Exception e) {
		Throwable cause = e.getCause();
		if (cause == null) {
			throw new BugException("wrapped exception without cause", e);
		}
		if (cause instanceof RuntimeException) {
			throw (RuntimeException) cause;
		}
		if (cause instanceof Error) {
			throw (Error) cause;
		}
		throw new BugException("wrapped exception occurred", cause);
	}
}

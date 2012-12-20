// Copyright 2003 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown during initialization
 * 
 * @author Tom O'Reilly
 */
public class InitializeException extends Exception {

	public InitializeException() {
		super();
	}

	public InitializeException(String message) {
		super(message);
	}
}
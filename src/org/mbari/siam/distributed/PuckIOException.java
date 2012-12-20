// Copyright 2003 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when communications with the puck fails
 * 
 * @author Mike Risi
 */
public class PuckIOException extends Exception {

	public PuckIOException() {
		super();
	}

	public PuckIOException(String message) {
		super(message);
	}
}
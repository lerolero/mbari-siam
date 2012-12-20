// Copyright 2003 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when trying to scan port, but service is already using port
 * 
 * @author Tom O'Reilly
 */
public class PortOccupiedException extends Exception {

	public PortOccupiedException() {
		super();
	}

	public PortOccupiedException(String message) {
		super(message);
	}
}
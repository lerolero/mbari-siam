// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when unknown port is specified.
 * 
 * @author Tom O'Reilly
 */
public class PortNotFound extends Exception {

	public PortNotFound() {
		super();
	}

	public PortNotFound(String message) {
		super(message);
	}

}
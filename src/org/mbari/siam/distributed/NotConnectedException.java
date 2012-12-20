// Copyright 2004 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when client/server app is not connected to server
 * 
 * @author Bob Herlien
 */
public class NotConnectedException extends Exception {

	public NotConnectedException() {
		super();
	}

	public NotConnectedException(String message) {
		super(message);
	}
}
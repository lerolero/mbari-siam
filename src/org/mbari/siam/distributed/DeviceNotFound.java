// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when unknown device is specified.
 * 
 * @author Tom O'Reilly
 */
public class DeviceNotFound extends Exception {

	public DeviceNotFound() {
		super();
	}

	public DeviceNotFound(String message) {
		super(message);
	}

}
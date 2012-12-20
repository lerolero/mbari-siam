// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Indicates that Device location is unknown.
 * 
 * @author Tom O'Reilly
 */
public class UnknownLocationException extends Exception {

	public UnknownLocationException() {
		super();
	}

	public UnknownLocationException(String message) {
		super(message);
	}
}
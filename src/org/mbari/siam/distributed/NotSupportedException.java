// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when an operation is not supported
 * 
 * @author Tom O'Reilly
 */
public class NotSupportedException extends Exception {

	public NotSupportedException() {
		super();
	}

	public NotSupportedException(String message) {
		super(message);
	}
}
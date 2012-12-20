// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Indicates that Device has no parent.
 * 
 * @author Tom O'Reilly
 */
public class NoParentException extends Exception {

	public NoParentException() {
		super();
	}

	public NoParentException(String message) {
		super(message);
	}
}
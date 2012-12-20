// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Indicates that a Device has no children.
 * 
 * @author Tom O'Reilly
 */
public class NoChildrenException extends Exception {

	public NoChildrenException() {
		super();
	}

	public NoChildrenException(String message) {
		super(message);
	}
}
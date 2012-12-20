/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * PropertyException is thrown when a property-related error occurs
 */
public class PropertyException extends Exception {

	/** Create PropertyException object. */
	public PropertyException() {
		super();
	}

	/** Create PropertyException object, with specified message. */
	public PropertyException(String message) {
		super(message);
	}
}
/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * MissingPropertyException is thrown when a required property is missing.
 */
public class MissingPropertyException extends Exception {

	/** Create MissingPropertyException object. */
	public MissingPropertyException() {
		super();
	}

	/** Create MissingPropertyException object, with specified message. */
	public MissingPropertyException(String message) {
		super(message);
	}
}
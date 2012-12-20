/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * InvalidPropertyException is thrown when an invalid property is specified.
 */
public class InvalidPropertyException extends Exception {

	/** Create InvalidPropertyException object. */
	public InvalidPropertyException() {
		super();
	}

	/** Create InvalidPropertyException object, with specified message. */
	public InvalidPropertyException(String message) {
		super(message);
	}
}
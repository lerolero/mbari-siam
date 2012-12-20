/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * InvalidDataException is thrown when invalid data is determined.
 */
public class InvalidDataException extends Exception {

	/** Create InvalidDataException object. */
	public InvalidDataException() {
		super();
	}

	/** Create InvalidPropertyException object, with specified message. */
	public InvalidDataException(String message) {
		super(message);
	}
}


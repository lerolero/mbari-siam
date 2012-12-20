/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * InvalidPropertyException is thrown when an invalid property is specified.
 */
public class DuplicateIdException extends Exception {

	/** Create DuplicateIdException object. */
	public DuplicateIdException() {
		super();
	}

	/** Create DuplicateIdException object, with specified message. */
	public DuplicateIdException(String message) {
		super(message);
	}
}
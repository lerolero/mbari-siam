/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * Thrown when unauthorized operation is attempted.
 * 
 * @author Tom O'Reilly
 */
public class AuthenticationException extends Exception {

	public AuthenticationException(String message) {
		super(message);
	}

}
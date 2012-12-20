// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when no data is available.
 * 
 * @author Tom O'Reilly
 */
public class NoDataException extends Exception {

	public NoDataException() {
		super();
	}

	public NoDataException(String message) {
		super(message);
	}
        
}
// Copyright 2002 MBARI
package org.mbari.siam.distributed;

/**
 * Thrown when "too much" data has been requested. ("Too much" is determined by
 * data source.)
 * 
 * @author Tom O'Reilly
 */
public class TooMuchDataException extends Exception {

	long _nPacketsRequested = 0;

	long _maxPacketsAllowed = 0;

	public TooMuchDataException(long nPacketsRequested, long maxPacketsAllowed,
			String message) {
		super(message);
		_nPacketsRequested = nPacketsRequested;
		_maxPacketsAllowed = maxPacketsAllowed;
	}

	/** Return number of packets requested. */
	public long getNPacketsRequested() {
		return _nPacketsRequested;
	}

	/** Return maximum number of packets allowed. */
	public long getMaxPacketsAllowed() {
		return _maxPacketsAllowed;
	}
}
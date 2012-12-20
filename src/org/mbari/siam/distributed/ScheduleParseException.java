// Copyright MBARI 2002
package org.mbari.siam.distributed;

/**
 * ScheduleParseException indicates that an entry used incorrect syntax
 */
public class ScheduleParseException extends Exception {
	/** Constructor. */
	public ScheduleParseException() {
		super();
	}

	/** Constructor; specify message. */
	public ScheduleParseException(String msg) {
		super(msg);
	}
}
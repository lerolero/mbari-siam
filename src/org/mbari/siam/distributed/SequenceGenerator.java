//Copyright 2003 MBARI  (where 2003 is date processed or created)
//Monterey Bay Aquarium Research Institute Proprietary Information.
//All rights reserved.

package org.mbari.siam.distributed;

import org.mbari.siam.distributed.RangeException;

/**
 * A SequenceGenerator generates a (thread-safe) sequence of (long) integers.
 * The sequence may be implemented in a number of ways: simple up/down counters,
 * state machines, table-driven, non-linear functions... The sequence may
 * implement special behavior at the upper/lower limits of the sequence.
 * 
 * The SequenceGenerator interface suggests the basic methods needed to
 * implement such counters. A more general purpose approach might be to use this
 * interface as the basis for a number of abstract adapter classes which could
 * be extended to implement sequences of floating point numbers.
 * 
 * @author Kent Headley
 */
public interface SequenceGenerator {

	/** Get next number in sequence */
	public long getNext();

	/** Get minimum value in sequence */
	public long getLeast();

	/** Get maximum value in sequence */
	public long getGreatest();

	/** Set sequence range */
	public void initialize(long least, long greatest, long current)
			throws RangeException;

	/** Restart sequence from minimum value */
	public void reset();

	/**
	 * Look at nth number in sequence (relative to the current number) without
	 * actually incrementing. The offset may be less than zero. Using an offset
	 * of 0 returns the most recent number returned.
	 */
	public long peek(long offset, boolean wrap);

	/** Returns number of times rollover has occurred since reset */
	//public long rolloverCount();

}


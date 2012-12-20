/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
   PacketSubsampler filters every nth packet of specified types.
 */
public class PacketSubsampler 
    extends PacketFilter
    implements Serializable {

    /** Number of packets to skip between subsamples. _skipInterval of 0 means
     to sample every packet. _skipInterval of -1 means to never sample. */
    protected int _skipInterval = 0;

    /** Number of packets that have been read. */
    protected int _currentCount = 0;

    /** Specify subsample skip interval for specified packet types; 
	interval of 0 means don't skip any packets, interval of -1 means
	never sample any packets. */
    public PacketSubsampler(int skipInterval, int packetTypeFlags) {
	super(packetTypeFlags);
	_skipInterval = skipInterval;
	reset();
    }

    /** Reset filter in preparation for starting on an input data set. */
    public void reset() {
	_currentCount = 0;
    }


    /** If filter is not applicable to specified packet's type, or specified
	packet passes filter criteria, return true; else return false. */
    public boolean pass(DevicePacket packet) {

	if (filteredType(packet)) {

	    if (_skipInterval < 0) {
		// Negative skip interval means never sample
		return false;
	    }

	    if (_skipInterval == 0) {
		// Skip interval of 0 means never skip any packets
		return true;
	    }

	    // Keeping track of total packets processed, return true if
	    // we've hit the skip interval
	    if ((_currentCount++ % (_skipInterval + 1)) == 0) {
		return true;
	    }
	    else {
		return false;
	    }
	}
	else {
	    // This packet type is not filtered.
	    return true;
	}
    }


    /** Print string representation */
    public String toString() {
	return "types: " + super.toString() + 
	    new String(": skip " + _skipInterval);
    }
}

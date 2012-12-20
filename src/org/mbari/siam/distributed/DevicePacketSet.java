// Copyright 2003 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.util.Vector;

/**
 * A DevicePacketSet contains a vector of DevicePacket objects, and indicates
 * whether the vector is a "complete" set of packets. For example, a set of
 * packets may have been requested by a client which specified retrieval
 * criteria, such as a timestamp range. However, the returned set might be
 * smaller than the requested set, e.g. if the complete set would exhaust node
 * memory, or if the communication link bandwidth is insufficient to return the
 * entire set.
 */
public class DevicePacketSet implements Serializable {
	protected boolean _complete = false;

	/** Vector contains requested packets. */
	public Vector _packets;

	/**
	 * Create a DevicePacketSet with the specified packet vector, and indicate
	 * whether the set is complete.
	 */
	public DevicePacketSet(Vector packets, boolean complete) {
		_packets = packets;
		_complete = complete;
	}

	/** Return true if this DevicePacketSet contains all packets requested. */
	public boolean complete() {
		return _complete;
	}

}


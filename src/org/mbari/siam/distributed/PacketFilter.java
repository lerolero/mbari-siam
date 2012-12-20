/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;
import org.apache.log4j.Logger;

/**
   Specifies filtering criteria for specified packet types.
 */
abstract public class PacketFilter implements Serializable {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(PacketFilter.class);

    // Apply this filter to these packet types
    private int _filteredTypeFlags;

    /** If filter is not applicable to specified packet's type, or specified
	packet passes filter criteria, return true; else return false. */
    abstract public boolean pass(DevicePacket packet);

    /** Reset filter in preparation for starting on an input data set. */
    abstract public void reset();

    /** Create PacketFilter, specify packet types to filter. */
    public PacketFilter(int filteredTypeFlags) {
	_filteredTypeFlags = filteredTypeFlags;
    }

    /** Return true if specified packet is type to be filtered. */
    protected boolean filteredType(DevicePacket packet) {

	if ((packet instanceof SensorDataPacket) &&
	    (DevicePacket.SENSORDATA_FLAG & _filteredTypeFlags) != 0) {
	    return true;
	}
	else if ((packet instanceof MetadataPacket) &&
	    (DevicePacket.METADATA_FLAG & _filteredTypeFlags) != 0) {
	    return true;
	}
	else if ((packet instanceof DeviceMessagePacket) &&
	    (DevicePacket.DEVICEMESSAGE_FLAG & _filteredTypeFlags) != 0) {
	    return true;
	}
	else if ((packet instanceof SummaryPacket) &&
	    (DevicePacket.SUMMARY_FLAG & _filteredTypeFlags) != 0) {
	    return true;
	}
	
	// Specified packet is not included in filtered types
	return false;

    }

    /** Return string representation */
    public String toString() {
	StringBuffer buf = new StringBuffer();
	if ((_filteredTypeFlags & DevicePacket.SENSORDATA_FLAG) != 0) {
	    buf.append("SensorData ");
	}
	if ((_filteredTypeFlags & DevicePacket.METADATA_FLAG) != 0) {
	    buf.append("Metadata ");
	}
	if ((_filteredTypeFlags & DevicePacket.DEVICEMESSAGE_FLAG) != 0) {
	    buf.append("DeviceMessage ");
	}
	if ((_filteredTypeFlags & DevicePacket.SUMMARY_FLAG) != 0) {
	    buf.append("Summary ");
	}

	return new String(buf);
    }
}


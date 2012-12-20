// Copyright 2002 MBARI
package org.mbari.siam.distributed.portal;

import java.io.Serializable;

/**
Specifies device ID, its platform channel number, and text description 
of device.
*/
public class DeviceDescription implements Serializable {

    public DeviceDescription() {
	System.out.println("DeviceDescription ctr");
    }

    /** Device ID */
    public long _deviceID;

    /** Type of device (e.g. "SeabirdCTD"). */
    public String _type;

    /** Time tag of latest retrieved packet. */
    public long _latestPacketTime = 0;

    public String getName() {
	return _type + "-" + Long.toString(_deviceID);
    }

    /** Print description. */
    public String print() {
	return getName();
    }
}

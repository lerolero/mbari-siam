/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;
import org.mbari.siam.distributed.DeviceMessagePacket;

/**
HostStatus generates information about health and status of the
SIAM host computer. This information includes file system usage,
available memory, etc.
@author Tom O'Reilly
*/
public class HostStatus extends StatusReport {

    protected final static String DISK_SPACE_USAGE_CMD = "df";

    public HostStatus(long deviceID) {
	super(deviceID, 1024);
    }

    /** Fill message packet buffer with HostStatus information. */
    public DeviceMessagePacket getPacket() {

	String message = null;

	// Get disk usage
	try {
	    Process process = 
		Runtime.getRuntime().exec(DISK_SPACE_USAGE_CMD);

	    int nBytes = process.getInputStream().read(_reportBuf);
	    message = new String(_reportBuf, 0, nBytes);
	}
	catch (Exception e) {
	    message = "Disk usage not available";
	}

	// Get Java thread count
	ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
	message += "\n#jvm threads: " + threadGroup.activeCount() + "\n";

	message = message + "\n" +  
	    "jvm mem: " + Runtime.getRuntime().totalMemory() +
	    " bytes\n";

	_packet.setMessage(System.currentTimeMillis(), message.getBytes());
	return _packet;
    }
}

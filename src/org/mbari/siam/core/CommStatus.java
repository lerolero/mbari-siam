/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;
import org.mbari.siam.distributed.DeviceMessagePacket;

/**
CommStatus generates information about health and status of the
node's communication interfaces.
@author Tom O'Reilly
*/
public class CommStatus extends StatusReport {

    protected final static String RFIO_STATUS_CMD = 
	"echo RFIO status goes here";

    protected final static String GLOBALSTAR_STATUS_CMD = 
	"globalstarStat";

    protected final static String PPP_STATUS_CMD = 
	"sh plog -100";

    protected final static String OPTICAL_STATUS_CMD = 
	"echo Optical status goes here";

    public CommStatus(long deviceID) {
	super(deviceID, 10240);
    }

    /** Fill message packet buffer with HostStatus information. */
    public DeviceMessagePacket getPacket() {

	String message = null;
	Process process = null;
	int nBytes = 0;

	try {
	    process = 
		Runtime.getRuntime().exec(RFIO_STATUS_CMD);

	    nBytes = process.getInputStream().read(_reportBuf);

	    message = reportString(nBytes);
	    if (message == null || message.length() == 0) {
		message = "RF status not available";
	    }
	}
	catch (Exception e) {
	    message = "RF status unavailable";
	}

	String message2 = null;

	try {
	    process = 
		Runtime.getRuntime().exec(OPTICAL_STATUS_CMD);

	    nBytes = process.getInputStream().read(_reportBuf);
	    message2 = reportString(nBytes);
	}
	catch (Exception e) {
	    message2 = "Optical status not available";
	}


	String message3 = null;

	try {
	    process = 
		Runtime.getRuntime().exec(GLOBALSTAR_STATUS_CMD);

	    nBytes = process.getInputStream().read(_reportBuf);
	    message3 = reportString(nBytes);
	}
	catch (Exception e) {
	    message3 = "GlobalStar status not available";
	}


	message = "RFIO:\n" + message + "\n" + 
	    "Optical:\n" + message2 + "\n" + 
	    "GlobalStar:\n" + message3 + "\n";

	_packet.setMessage(System.currentTimeMillis(), message.getBytes());
	return _packet;
    }
}

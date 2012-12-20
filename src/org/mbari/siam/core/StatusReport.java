/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;
import org.mbari.siam.distributed.DeviceMessagePacket;

/**
Base class for objects which generate information about health and 
status of a node subsystem. 
@author Tom O'Reilly
*/
abstract public class StatusReport {

    protected DeviceMessagePacket _packet = null;

    // Buffer holds output of system commands
    protected byte _reportBuf[];

    /** Create a new StatusReport object, specifying device ID and
     maximum bytes in status message. */
    public StatusReport(long deviceID, int maxReportBytes) {
	_reportBuf = new byte[maxReportBytes];
	_packet = new DeviceMessagePacket(deviceID);
    }

    /** Fill message packet buffer with health/status information. */
    abstract public DeviceMessagePacket getPacket();


    /** Check that buffer hasn't filled; if it has, append 'overflow'
     message. */
    String reportString(int nReportBytes) {
	String report = new String(_reportBuf, 0, nReportBytes);
	if (nReportBytes >= _reportBuf.length) {
	    report += " **OVERFLOWED " + _reportBuf.length + " bytes **";
	}
	return report;
    }
}

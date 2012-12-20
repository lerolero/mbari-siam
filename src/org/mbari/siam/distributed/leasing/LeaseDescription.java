/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/**
LeaseDescription describes properties of a lease.
 */
package org.mbari.siam.distributed.leasing;

import java.net.InetAddress;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//public class LeaseDescription {
public class LeaseDescription implements Serializable {

    public final static int COMMS_LEASE = 1;
    public final static int CPU_LEASE = 2;

    /** Lease ID. */
    public int _id;

    /** Time at which lease was established/renewed. */
    public long _establishMsec;

    /** Lease duration. */
    public long _durationMsec;

    /** Poll current system time. */
    public long _currentTimeMsec = System.currentTimeMillis();

    /** Last lease renewal time */
    public long _renewalTime;

    /** number or times lease renewed */
    public long _renewalCount;

    /** Client notation. */
    public byte[] _clientNote;

    /** LeaseDescription.COMMS_LEASE or LeaseDescription.CPU_LEASE */
    public int _type;

    /** Time remaining before lease expires. */
    public long timeRemaining() {
	
	if (_type == COMMS_LEASE) {
	    return ((_durationMsec) - (_currentTimeMsec -  _renewalTime));
	}
	else if (_type == CPU_LEASE) {
	    return (_durationMsec - (_currentTimeMsec - _establishMsec));
	}
	else {
	    // Unknown type
	    
	    return 0;
	}
    }

    /** Print out on screen the following items. */	
    public String toString() {

	SimpleDateFormat dateFormat = 
	    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

	String type = "UNK";
	if (_type == COMMS_LEASE) {
	    type = "COMM";
	}
	else if (_type == CPU_LEASE) {
	    type = "CPU";
	}

    	StringBuffer buf = new StringBuffer();
	buf.append("id = " + _id + "\n");
	buf.append("type = " + type + "\n");
	buf.append("Current Node System Time = " + 
		   dateFormat.format(new Date(_currentTimeMsec)) + "\n");

	buf.append("Establishment Time = " + 
		   dateFormat.format(new Date(_establishMsec)) + "\n");

	if (_type == COMMS_LEASE) {
	    buf.append("Times Lease Renewed = " + (_renewalCount) + "\n");
	    buf.append("Total Lease Active Time = ");
	    buf.append(((_currentTimeMsec - _establishMsec)/1000L)+ 
		       " seconds\n");

	    buf.append("Lease Acquired By = " + new String(_clientNote) + 
		       "\n");
	}

	buf.append("Lease Duration = " + (_durationMsec/1000L) + 
		   " seconds\n");

	long remaining = timeRemaining() / 1000;

	buf.append("Estimated Lease Time Remaining =~ " + 
		   remaining + " seconds\n");

	if (remaining < 0) {
	    buf.append("EXPIRED?\n");
	}

	return new String(buf);
    }
}

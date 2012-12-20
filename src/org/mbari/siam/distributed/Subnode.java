/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.net.InetAddress;

/** 
This class represents a subnode. A subnode periodically contacts its parent
node. The 'contact time' is the time the parent node last had contact from the
subnode, expressed in msec since epoch. */
public class Subnode implements Serializable {

    protected InetAddress _address;
    protected long _contactTimestamp = 0;
    protected long _deviceID = -1;

    public Subnode(InetAddress address, long t, long deviceID) {
	_address = address;
	_contactTimestamp = t;
	_deviceID = deviceID;
    }

    /** Get subnode's address */
    public InetAddress getAddress() {
	return _address;
    }

    
    /** Set time of most recent contact from subnode. */
    public void setContactTime(long t) {
	_contactTimestamp = t;
    }

    /** Get time of most recent contact from subnode. */
    public long getContactTime() {
	return _contactTimestamp;
    }

    /** Update subnode fields */
    public void update(long contactTime, NodeNotifyMessage info) {
	setContactTime(contactTime);
	_deviceID = info._deviceID;
    }


    /** Output String representation. */
    public String toString() {

	long sec = (System.currentTimeMillis() - _contactTimestamp) / 1000;

	boolean started = false;
	StringBuffer timeString = new StringBuffer("");

	long ageDays = sec / (3600*24);
	if (ageDays > 0) {
	    started = true;
	    timeString.append(ageDays + "d ");
	}
	sec %= (3600*24);
	long ageHours = sec / 3600;
	if (started || ageHours > 0) {
	    started = true;
	    timeString.append(ageHours + "h:");
	}

	sec %= 3600;
	long ageMin = sec / 60;
	if (started || ageMin > 0) {
	    started = true;
	    timeString.append(ageMin + "m:");
	}

	sec %= 60;
	timeString.append(sec + "s");

	return (_address.toString() + ": deviceID=" + _deviceID + ", age=" + new String(timeString));
    }
}

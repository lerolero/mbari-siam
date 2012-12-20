/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.net.InetAddress;
import java.io.Serializable;

/** MOOS subnode description */
public class MOOSSubnode extends Subnode implements Serializable {

    InetAddress _switchCpuAddress = null;


    public MOOSSubnode(InetAddress address, long t, long deviceID, 
		       InetAddress switchCpuAddress) {

	super(address, t, deviceID);

	_switchCpuAddress = switchCpuAddress;
    }


    public void update(long contactTime, NodeNotifyMessage info) {
	super.update(contactTime, info);
	if (info instanceof MOOSNodeNotifyMessage) {
	    _switchCpuAddress = 
		((MOOSNodeNotifyMessage )info)._switchCardCpuAddress;
	}

    }

    public String toString() {
	String buf = super.toString();
	if (_switchCpuAddress != null) {
	    return (buf + "\nSwitch addr: " + _switchCpuAddress);
	}
	else {
	    return (buf + "\nUnknown switch IP addr\n");
	}
    }
}


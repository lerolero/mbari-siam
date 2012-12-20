/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.net.InetAddress;

/** Basic info specific to MOOS node, periodically sent to node's parent. */
public class MOOSNodeNotifyMessage 
    extends NodeNotifyMessage implements Serializable {

    public MOOSNodeNotifyMessage(long deviceID, 
				 InetAddress switchCardCpuAddress) {
	super(deviceID);
	_switchCardCpuAddress = switchCardCpuAddress;
    }

    /** Address of node's switch card CPU (e.g. Medusa) */
    public InetAddress _switchCardCpuAddress;
}

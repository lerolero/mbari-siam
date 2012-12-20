/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;
import java.io.Serializable;

/** NodeNotifyMessage defines some very basic information about a node, and
 is periodically sent to the node's parent. */
public class NodeNotifyMessage implements Serializable {

    public NodeNotifyMessage(long deviceID) {
	_deviceID = deviceID;
    }

    /** ISI ID of node */
    public long _deviceID;
}

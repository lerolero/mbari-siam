/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
   NodeSessionInfo contains information returned by the node at the start of 
   a "telemetry" session, including list of the node's ports, subnodes and 
   other information. Note: applications using this object must check boolean values within the object
   to determine whether errors occurred while renewing the node watchdog timer or executing the initial command
   command script.
 */
public class NodeSessionInfo implements Serializable {

    /** Node ID */
    public long _nodeID = -1;

    /** Time at which node service started */
    public long _startTimeMsec = 0;

    /** Node ports */
    public Port[] _ports = null;

    /** Subnodes */
    public Subnode[] _subnodes = null;

    /** Watchdog timer status */
    public byte[] _wdtStatus = "".getBytes();
    public boolean _wdtError = false;

    /** Initial connection command status */
    public byte[] _initCommandStatus = "".getBytes();
    public boolean _initCommandError = false;
}

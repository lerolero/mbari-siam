/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.distributed;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseDescription;

/**
 * Interface to MOOSNode, which hosts devices. This interface adds 
 * methods which are specific to MOOS.
 * 
 * @author Tom O'Reilly
 */
public interface MOOSNode extends Node {


    /** Keep watchdog from waking up and resetting Node */
    public byte[] renewWDT() 
	throws RemoteException,IOException,FileNotFoundException;

    /** Read watchdog time (WDT) status */
    public byte[] readWDT() 
	throws RemoteException,IOException,FileNotFoundException;

    /** Get status of DPA port associated with specified comm port. 
     Throws NotSupportedException if no power port is 
     associated with specified comm port. */
    public DpaPortStatus getDpaPortStatus(byte[] commPortName) 
	throws NotSupportedException, DeviceNotFound, RemoteException;

    /** Get status of all DPA ports. */
    public DpaPortStatus[] getDpaPortStatus()
	throws RemoteException;


    /** Send signal to wakeup specified node. */
    public void wakeupNode(InetAddress node) 
	throws RemoteException, IOException;

    /** Send signal to wakeup all nodes. */
    public void wakeupAllNodes() 
	throws RemoteException, IOException;
}

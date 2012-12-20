// Copyright 2001 MBARI
package org.mbari.siam.distributed.portal;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketStream;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Port;

/**
Inteface definition for Portal, which is a "gateway" between a 
low-bandwidth/intermittent node, and off-platform clients. 
@author Bob Herlien
*/
public interface PortalInterface extends Remote {

    /** Notify portal that link to remote node has changed status */
    public void nodeLinkNotify(InetAddress remoteAddr, boolean up)
	throws RemoteException; 


    /** Return true if portal has network connection to specified 
	primary node. */
    public boolean primaryLinkConnected(InetAddress address) 
	throws RemoteException, Exception;
}



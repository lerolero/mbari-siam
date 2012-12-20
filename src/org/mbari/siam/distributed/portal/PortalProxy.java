// Copyright 2001 MBARI
package org.mbari.siam.distributed.portal;

import java.lang.Long;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;
import org.mbari.siam.distributed.Authentication;
import org.mbari.siam.distributed.AuthenticationException;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketStream;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Port;

/**
Proxy for PortalServer, which is a "gateway" between a 
low-bandwidth/intermittent
node, and off-platform clients. 

@author Tom O'Reilly
*/
public interface PortalProxy extends Remote {

    /** Indicates "send command as soon as possible". */
    public static final long ASAP = 0;
    /** Indicates "send command, no matter how late. */
    public static final long WHENEVER = Long.MAX_VALUE;

    /** Name of service */
    public String getName() throws RemoteException;

    /** Name of portal host */
    public String getPortalHostName() throws RemoteException;

    /** Unique identifier of platform */
    public long getId() throws RemoteException;

    /** Returns true if communication link is up. */
    public boolean connected() throws RemoteException;

    /** Status of platform. */
    public int getStatus() throws RemoteException;

    /** Returns true of next communication link. */
    public long nextConnectTime() throws RemoteException;

    /** Get DevicePacketStream for all sensors. */
    public DevicePacketStream getDevicePacketStream(Authentication auth) 
	throws RemoteException, AuthenticationException;

    /** Get DevicePacketStream for specified sensor. */
    public DevicePacketStream getDevicePacketStream(long sensorID, 
						    Authentication auth) 
	throws RemoteException, DeviceNotFound, AuthenticationException;

    /** Return port information for node */
    public Port[] getPortConfiguration() 
	throws RemoteException, UnknownConfiguration;

    /** Return QueuedCommands which haven't been sent to platform yet. */
    public Vector getQueuedCommands() 
	throws RemoteException;

    /** Return QueuedCommands which have already been sent to platform. */
    public Vector getSentCommands() 
	throws RemoteException;

    /** Notify portal that link to remote node is "up". This method
     will likely be replaced by Rendezvous mechanisms. */
    public void nodeLinkConnected()
	throws RemoteException; 

    /** Notify portal that link to remote node is about to be 
	disconnected. */
    public void nodeLinkDisconnecting(long nextConnectTime)
	throws RemoteException; 
}



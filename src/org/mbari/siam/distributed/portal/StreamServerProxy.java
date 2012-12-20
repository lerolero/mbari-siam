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

/**
Proxy for StreamServer, which tests DevicePacketStream.
@author Tom O'Reilly
*/
public interface StreamServerProxy extends Remote {


    /** Get DevicePacketStream for all sensors. */
    public DevicePacketStream getDevicePacketStream(Authentication auth) 
	throws RemoteException, AuthenticationException;

    /** Get DevicePacketStream for specified sensor. */
    public DevicePacketStream getDevicePacketStream(long sensorID, 
						    Authentication auth) 
	throws RemoteException, DeviceNotFound, AuthenticationException;

}



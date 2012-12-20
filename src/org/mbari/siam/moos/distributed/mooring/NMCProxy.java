// Copyright 2001 MBARI
package org.mbari.siam.moos.distributed.mooring;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceConfiguration;
import org.mbari.siam.distributed.DeviceGeometry;
import org.mbari.siam.distributed.DeviceInfo;
import org.mbari.siam.distributed.EventManager;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Platform;

/**
   Proxy for NMC mooring server. 
   @author Tom O'Reilly
*/
public interface NMCProxy extends Platform {

    public final static String SERVER_NAME = "nmc";

    ///////////////////////////////////////////////////////////////
    // Methods for NMC in particular
    ///////////////////////////////////////////////////////////////
    /**
       Send a configuration string to the specified device.
       Returns device's reply string.
       @param deviceID ID of the device
       @param configString Configuration string recognized by device
    */
    public byte[] configureDevice(long deviceID, byte[] configString) 
	throws RemoteException, DeviceNotFound;

    /**
       Specify the polling period for specified device.
       @param deviceID ID of the device
       @param periodMillisec polling period in milliseconds
    */
    public void setPollPeriod(long deviceID, int periodMillisec)
	throws RemoteException, DeviceNotFound;

    /**
       Association of device IDs with channels.
    */
    public ChannelConfiguration[] getChannelConfiguration()
	throws RemoteException;

}


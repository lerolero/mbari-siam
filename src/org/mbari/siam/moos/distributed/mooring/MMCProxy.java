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
   Proxy for MMC mooring server. 
   @author Tom O'Reilly
*/
public interface MMCProxy extends Platform {

    public final static String SERVER_NAME = "mmc";

    ///////////////////////////////////////////////////////////////
    // Methods for MMC in particular
    ///////////////////////////////////////////////////////////////
}


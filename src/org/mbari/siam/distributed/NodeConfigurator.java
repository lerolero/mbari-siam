/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.FilteredDeviceLogIF;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.NetworkManager;

import org.mbari.siam.core.PortManager;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.NodeService;
import org.mbari.siam.core.SleepManager;
import org.mbari.siam.core.WDTManager;

/**
 * NodeConfigurator encapsulates the configuration of hardware-dependent components
 *
 * @author Bob Herlien
*/

public interface NodeConfigurator
{
    /** Return a NodeService appropriate to this configuration */
    NodeService createNodeService(PortManager portManager, String portalHost)
	throws RemoteException, MissingPropertyException,
	       InvalidPropertyException, IOException;

    /** Return SIAM home directory. */
    public String getSiamHome() throws MissingPropertyException;

    /** Return a PortManager appropriate to this configuration */
    PortManager createPortManager(NodeProperties nodeProps)
	throws MissingPropertyException, InvalidPropertyException;

    /** Return NodeProperties appropriate to this configuration */
    NodeProperties createNodeProperties(String cfgFileName)
	throws FileNotFoundException, MissingPropertyException, IOException;

    /** Create the Node Log */
    FilteredDeviceLogIF createDeviceLog(long nodeId, NodeProperties properties)
	throws MissingPropertyException, InvalidPropertyException, IOException;

    /** Create the NetworkManager */
    NetworkManager createNetworkManager(String localHostName)
	throws UnknownHostException, SocketException;

    /** Create the SleepManager */
    SleepManager createSleepManager();

    /** Create the WDTManager */
    WDTManager createWDTManager();
    
}

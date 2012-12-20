/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.deployed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.NodeService;
import org.mbari.siam.core.PortManager;
import org.mbari.siam.core.SleepManager;
import org.mbari.siam.core.WDTManager;
import org.mbari.siam.core.FilteredDeviceLog;
import org.mbari.siam.distributed.FilteredDeviceLogIF;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.NodeConfigurator;
import org.mbari.siam.distributed.NetworkManager;

import org.mbari.siam.moos.deployed.MOOSNetworkManager;


/**
 * MOOSNodeConfigurator encapsulates the configuration of hardware-dependent components
 * for MOOS
 *
 * @author Bob Herlien
*/

public class MOOSNodeConfigurator implements NodeConfigurator
{
    static private Logger _log4j = Logger.getLogger(MOOSNodeConfigurator.class);


    /** Return a NodeService appropriate to this configuration */
    public NodeService createNodeService(PortManager portManager, String portalHost)
	throws RemoteException, MissingPropertyException,
	       InvalidPropertyException, IOException
    {
	return( new MOOSNodeService(portManager, portalHost) );
    }


    /** Return SIAM home directory. */
    public String getSiamHome() 
	throws MissingPropertyException
    {
        Properties sysProperties = System.getProperties();
        String home = sysProperties.getProperty("siam_home");
	if (home == null) {
	    throw new MissingPropertyException("siam_home");
	}
	return home.trim();
    }


    /** Return a PortManager appropriate to this configuration */
    public PortManager createPortManager(NodeProperties nodeProps)
	throws MissingPropertyException, InvalidPropertyException
    {
	return( new MOOSPortManager(getSiamHome(), nodeProps) );
    }


    /** Return NodeProperties appropriate to this configuration */
    public NodeProperties createNodeProperties(String cfgFileName)
	throws FileNotFoundException, MissingPropertyException, IOException
    {
        FileInputStream in = 
            new FileInputStream(getSiamHome() + cfgFileName);

        NodeProperties nodeProperties = new MOOSNodeProperties();

	// Load Node properties from the file
        nodeProperties.load(in);

        in.close();

	return(nodeProperties);
    }


    /** Create the Node Log */
    public FilteredDeviceLogIF createDeviceLog(long nodeId, NodeProperties properties)
	throws MissingPropertyException, InvalidPropertyException, IOException
    {
	// Create log directory if it doesn't exist yet.
	String subdirName = properties.getDeviceLogDirectory().trim();
	String fullName = (getSiamHome() + File.separator + subdirName).trim();

        File f = new File(fullName);
        if ( !f.exists() ) {

	    _log4j.info("Creating log directory " + fullName);

            if ( !f.mkdirs() )
		_log4j.error("Failed to create log directory " + fullName);
        }

	// Create node log
	_log4j.debug("create node log");
	PacketFilter[] nullFilters = null;
	FilteredDeviceLog log = 
	    new FilteredDeviceLog(nodeId,
				  properties.getDeviceLogDirectory(),
				  nullFilters,
				  (long )(3600000 * properties.getDataShelfLifeHours()));
	return(log);
    }

    /** Create the NetworkManager */
    public NetworkManager createNetworkManager(String localHostName)
	throws UnknownHostException, SocketException
    {
	return(new MOOSNetworkManager(localHostName));
    }

    /** Return the MOOS Sleep Manager */
    public SleepManager createSleepManager()
    {
	return(MOOSSleepManager.getInstance());
    }

    /** Return the MOOS Watchdog Timer Manager */
    public WDTManager createWDTManager()
    {
	return(MOOSWDTManager.getInstance());
    }
    
}
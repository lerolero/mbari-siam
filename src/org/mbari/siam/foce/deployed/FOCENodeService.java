/****************************************************************************/
/* Copyright 2011 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.foce.deployed;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mbari.siam.core.NodeService;
import org.mbari.siam.core.PortManager;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.FOCENode;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.core.NullPowerPort;
import org.mbari.siam.utils.StopWatch;

/**
 * FOCENodeService extends NodeService and implements the FOCENode interface
 * 
 * @author Bob Herlien
 */
public class FOCENodeService extends NodeService implements FOCENode
{
    // CVS revision 
    private static String _versionID = "$Revision: 1.1 $";
    protected static final String CO2SubsysPowerKey = "CO2Subsystem.powerPort";
    protected static final String CO2SubsysServicesKey = "CO2Subsystem.services";
    protected static final String CO2SubsysDelayKey = "CO2Subsystem.delaySecs";
    protected static final String CO2SubsysRetriesKey = "CO2Subsystem.retries";

    protected PowerPort _powerPort = null;
    protected int _delaySecs = 10;
    protected int _retries = 5;
    protected String[] _services = null;

    private static Logger _log4j = Logger.getLogger(FOCENodeService.class);


    /** Create the NodeService. */
    FOCENodeService(PortManager portManager, String parentHost)
	throws RemoteException, MissingPropertyException, InvalidPropertyException, IOException
    {
	super(portManager, parentHost);

	_powerPort = _nodeProperties.getPowerPort(CO2SubsysPowerKey);
	_delaySecs = _nodeProperties.getIntegerProperty(CO2SubsysDelayKey);
	_retries = _nodeProperties.getIntegerProperty(CO2SubsysRetriesKey);

	try {
	    _services = _nodeProperties.getStringArrayProperty(CO2SubsysServicesKey, null, " \t");
	} catch (Exception e) {
	}

	if ((_services == null) || (_services.length == 0)) {
	    _log4j.info("No services found for " + CO2SubsysServicesKey);
	}	
    }

    /** Power up the CO2 Subsystem	*/
    public void powerUpCO2Subsystem()
	throws RemoteException, IOException, NotSupportedException
    {
	if ((_powerPort == null) || (_powerPort instanceof NullPowerPort))
	    throw new NotSupportedException("No power port defined");

	_powerPort.connectPower();
    }

    /** Power down the CO2 Subsystem	*/
    public void powerDownCO2Subsystem()
	throws RemoteException, IOException, NotSupportedException
    {
	if ((_powerPort == null) || (_powerPort instanceof NullPowerPort))
	    throw new NotSupportedException("No power port defined");

	_powerPort.disconnectPower();
    }

    /** Start up the Instrument Services associated with the CO2 Subsystem */
    public void startCO2SubsystemServices()
	throws RemoteException, IOException, NotSupportedException
    {
	boolean[] ok = new boolean[_services.length];

	for (int i = 0; i < _services.length; i++) {
	    ok[i] = false;
	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("Starting " + _services[i]);
	    }
	    for (int j = 0; !ok[i] && (j < _retries); j++) {
		try {
		    scanPort(_services[i].getBytes());
		    ok[i] = true;
		} catch (Exception e) {
		    StopWatch.delay(_delaySecs * 1000);
		}
	    }
	}

	StringBuffer badServices = new StringBuffer();
	for (int i = 0; i < _services.length; i++) {
	    if (!ok[i]) {
		badServices.append(_services[i]);
		badServices.append(' ');
	    }
	}
	if (badServices.length() > 0) {
	    badServices.insert(0, "startCO2SubsystemService() couldn't start ");
	    throw new NotSupportedException(badServices.toString());
	}
    }

    /** Stop the Instrument Services associated with the CO2 Subsystem */
    public void stopCO2SubsystemServices()
	throws RemoteException, IOException, NotSupportedException
    {
	for (int i = 0; i < _services.length; i++) {
	    try {
		shutdownDeviceService(_services[i].getBytes());
	    } catch (Exception e) {
		_log4j.warn("stopCO2SubsystemServices: " + _services[i] + " not found");
	    }
	}
    }
}



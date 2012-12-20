// Copyright 2008 MBARI
package org.mbari.siam.core;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.moos.deployed.SidearmPowerPort;


/**
 * DevicePort specifies deviceID and service associated with a Device port.
 * It is used for communication between PortManager, NodeService, NodeManager, and NodeProperties.
 */

public abstract class DevicePort
{
    private static Logger _log4j = Logger.getLogger(DevicePort.class);

    String _portName;

    /** Index of this port within the node properties file */
    int  _index;

    InstrumentPort _instrumentPort;

    PowerPort _powerPort = null;

    /** Name of jar file that contains service code for this device */
    String _jarName;

    public DeviceService _service = null;

    /** True if associated device is PUCK-enabled */
    boolean _hasPuck = false;

    /** For serial device, specifies RS232, RS422, or RS485 */
    CommsMode _commsMode = CommsMode.RS232;

    public DevicePort(int index, String portName, PowerPort powerPort, 
		      String jar,
		      DeviceService service)
    {
	_index = index;
	_portName = new String(portName);
	_powerPort = powerPort;
	_jarName = new String(jar);
	if ( _jarName.equalsIgnoreCase("PUCK") ) {
	    _hasPuck = true;
	}
	_service = service;
    }

    /** Set commsMode member and PowerPort HW */
    public void setCommsMode(CommsMode commsMode) {
	_log4j.debug("PortManager.Port.setCommsMode(): setting commsMode="+commsMode);
	_commsMode = commsMode;
	_powerPort.setCommsMode(_commsMode);
    }

    /** Get commsMode member */
    public CommsMode getCommsMode()
    {
	return(_commsMode);
    }

    /** Set power port member. */
    public void setPowerPort(PowerPort powerPort) {
	_powerPort = powerPort;
	_log4j.debug("PortManager.Port.setPowerPort(): setting commsMode="+_commsMode);
	_powerPort.setCommsMode(_commsMode);
    }

    /** Get DeviceService */
    public DeviceService getDeviceService()
    {
	return(_service);
    }

    /** Get power port member. */
    public PowerPort getPowerPort()
    {
	return(_powerPort);
    }

    /** Get port name */
    public String getPortName()
    {
	return(_portName);
    }

    /** Return whether port has a puck attached */
    public boolean hasPuck()
    {
	return(_hasPuck);
    }

    /** Return whether port has a power port */
    public boolean hasPowerPort()
    {
	return((_powerPort != null) && !(_powerPort instanceof NullPowerPort));
    }

    /** Open the underlying communications port */
    public abstract void openComms() throws Exception;

    /** Close the underlying communications port */
    public abstract void closeComms();

    /** Create the appropriate InstrumentPort */
    public abstract void createInstrumentPort() throws InitializeException;

    /** Placeholder for powerUpPort() in subclasses to set up CommsMode */
    void powerUpCommsMode()
    {
    }

    /**
     * Power up the port and enable the comms if a service is not already using it
     */
    public void powerUpPort(int currentLimit)
	throws PortOccupiedException, PortNotFound, RangeException,
	       NotSupportedException {

	//if the service is present don't mess with the port
	if (_service != null) {
	    String error = "port " + _portName
		+ " in use by service "
		+ new String(_service.getName());

	    _log4j.warn(error);
	    throw new PortOccupiedException(error);
	}

	if (_powerPort == null) {
	    _log4j.warn("power port is null, powerUpPort aborted");
	    return;
	}

	//get the port in a known state
	_powerPort.initialize();

	//set the current the limit
	_powerPort.setCurrentLimit(currentLimit);

	//enable the comms
	_powerPort.enableCommunications();

	powerUpCommsMode();

	//connect the power
	_powerPort.connectPower();


    }

    /**
     * Power down a port
     */
    public void powerDownPort()
	throws PortOccupiedException, PortNotFound
    {
	//if the service is present don't mess with the port
	if (_service != null) {
	    String error = "port " + _portName
		+ " in use by service "
		+ new String(_service.getName());

	    _log4j.warn(error);
	    throw new PortOccupiedException(error);
	}

	if (_powerPort == null) {
	    _log4j.warn("power port is null, powerDownPort aborted");
	    return;
	}

	//disable the comms
	_powerPort.disableCommunications();

	//disconnect the popwer
	_powerPort.disconnectPower();

	//isolate the port
	_powerPort.isolatePort();
    }

    /** Perform a CommPortIdentifier.addPortOwnershipListener() if this
     * DevicePort has a SerialPort.  Default implementation is null.
     */
    public void addCommPortListener()
    {
    }

    /** Get the Port index.  This is the number embedded in the property name
     *   e.g., serialPort7 has index 7
     */
    public int getIndex()
    {
	return(_index);
    }

    public String toString() {
	return _portName;
    }

    /** Equals method for finding object in collection	*/
    public boolean equals(Object obj)
    {
	if (obj instanceof DevicePort)
	    return(((DevicePort)obj).getIndex() == _index);
	else
	    return(false);
    }

    public int hashCode()
    {
	return(_portName.hashCode());
    }
}

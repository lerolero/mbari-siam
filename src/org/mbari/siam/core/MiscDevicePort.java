// Copyright 2010 MBARI
package org.mbari.siam.core;

import org.apache.log4j.Logger;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PowerPort;

/**
 * MiscDevicePort implements a miscellaneous DevicePort.
 */

public class MiscDevicePort extends DevicePort
{
    private static Logger _log4j = Logger.getLogger(MiscDevicePort.class);

    protected String	_params;

    public MiscDevicePort(int index, String portName,
			  PowerPort powerPort, String jar,
			  DeviceService service, String params)
    {
	super(index, portName, powerPort, jar, service);
	_params = params;
	_log4j.debug("MiscDevicePort() for port " + index + 
		     " Power port = " + powerPort.getName());
    }


    /** openComms() is null */
    public void openComms()
	throws NoSuchPortException, PortInUseException,
	       UnsupportedCommOperationException, Exception
    {
    }


    /** closeComms() is null */
    public void closeComms()
    {
    }

    /** Create the DigitalInputInstrumentPort */
    public void createInstrumentPort() throws InitializeException
    {
	_instrumentPort =
	    new MiscInstrumentPort(_portName, _powerPort, _params);

	_log4j.debug("MiscDevicePort.createInstrumentPort()");
	
	_instrumentPort.initialize();
    }
}

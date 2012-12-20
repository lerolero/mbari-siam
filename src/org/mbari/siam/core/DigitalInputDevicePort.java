// Copyright 2009 MBARI
package org.mbari.siam.core;

import org.mbari.siam.distributed.devices.DigitalInputBoard;

import org.apache.log4j.Logger;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PowerPort;

/**
 * DigitalInputDevicePort implements DevicePort for Digital Input bits on an AnalogBoard
 */

public class DigitalInputDevicePort extends DevicePort
{
    private static Logger _log4j = Logger.getLogger(DigitalInputDevicePort.class);

    protected DigitalInputBoard	_dioBoard;
    protected int[]		_params;

    public DigitalInputDevicePort(int index, String portName,
				  DigitalInputBoard dioBoard,
				  PowerPort powerPort,
				  String jar,
				  DeviceService service,
				  int[] parms)
    {
	super(index, portName, powerPort, jar, service);
	_dioBoard = dioBoard;
	_params = parms;
	_log4j.debug("DigitalInputDevicePort() for port " + index + 
		     " board: " + dioBoard.getName() + " Power port = "
		     + powerPort.getName());
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
	if (_params.length < 2)
	    throw new InitializeException("Not enough parameters specified");

	_instrumentPort =
	    new DigitalInputInstrumentPort(_portName, _dioBoard, _powerPort, _params);

	_log4j.debug("DigitalInputDevicePort.createInstrumentPort()");
	
	_instrumentPort.initialize();
    }
}

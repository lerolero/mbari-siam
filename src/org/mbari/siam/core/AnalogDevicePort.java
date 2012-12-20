// Copyright 2008 MBARI
package org.mbari.siam.core;

import org.mbari.siam.distributed.devices.AnalogBoard;

import org.apache.log4j.Logger;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PowerPort;

/**
 * AnalogDevicePort implements DevicePort for an A/D port.
 */

public class AnalogDevicePort extends DevicePort
{
    private static Logger _log4j = Logger.getLogger(AnalogDevicePort.class);

    protected AnalogBoard	_analogBoard;
    protected ChannelRange _channels[];
    int _boardNumber;

    public AnalogDevicePort(int index, String portName,
			    AnalogBoard analogBoard,
			    PowerPort powerPort,
			    String jar,
			    DeviceService service,
			    ChannelParameters portParams)
    {
	super(index, portName, powerPort, jar, service);
	_analogBoard = analogBoard;
	_channels=portParams.getChannels();
	_boardNumber=portParams.boardNumber();
	_log4j.debug("AnalogDevicePort() for port " + index + 
		     " board: " + analogBoard.getName() + " Power port = "
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

    /** Create the AnalogInstrumentPort */
    public void createInstrumentPort() throws InitializeException
    {
	_instrumentPort =
	    new AnalogInstrumentPort(_portName, _analogBoard, _powerPort,_boardNumber,_channels);

	_log4j.debug("AnalogDevicePort.createInstrumentPort()");
	
	_instrumentPort.initialize();
    }
}

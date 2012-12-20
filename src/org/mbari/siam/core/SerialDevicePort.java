// Copyright 2008 MBARI
package org.mbari.siam.core;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.CommPortOwnershipListener;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PowerPort;

/**
 * SerialDevicePort implements DevicePort for a serial device.
 */

public class SerialDevicePort extends DevicePort implements CommPortOwnershipListener
{
    private static Logger _log4j = Logger.getLogger(SerialDevicePort.class);

    SerialPort _serialPort = null;

    public SerialDevicePort(int index, String portName, PowerPort powerPort,
			    String jar,
			    DeviceService service,
			    CommsMode commsMode)
    {
	super(index, portName, powerPort, jar, service);
	if (commsMode != null)
	    _commsMode = commsMode;
	_log4j.debug("Created SerialDevicePort for " + portName +
		     ", powerPort = " + _powerPort.getName());
    }


    /** Open the serial port at a specified baud rate */
    public void openComms(int baudRate)
	throws NoSuchPortException, PortInUseException,
	       UnsupportedCommOperationException, Exception
    {
	_log4j.debug("Get port identifier for \"" + _portName + "\"...");
	CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(_portName);
	_log4j.debug("Got port identifier!");

	if ( portId.getPortType() != CommPortIdentifier.PORT_SERIAL ) {
	    throw new Exception("Port " + _portName + " is not a serial port.");
	}

	_log4j.debug("open ze serial port..." + _portName);
	_serialPort = (SerialPort) portId.open(_portName, 1000);

	_log4j.debug("set port params...");
	_serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, 
					SerialPort.PARITY_NONE);
    }


    /** Open the serial port */
    public void openComms()
	throws NoSuchPortException, PortInUseException,
	       UnsupportedCommOperationException, Exception
    {
	openComms(9600);
    }


    /** Close the serial port */
    public void closeComms()
    {
	if (_serialPort != null)
	    _serialPort.close();
    }

    /** Create the SerialInstrumentPort */
    public void createInstrumentPort() throws InitializeException
    {
	_instrumentPort =
	    new SerialInstrumentPort(_serialPort, _portName, _powerPort);
	
	_instrumentPort.initialize();

	_log4j.debug("Created and initialized new SerialInstrumentPort:  " + _instrumentPort);

	if(_commsMode==CommsMode.RS422)
	{
	    setCommsMode(CommsMode.RS422);
	    if(_serialPort!=null)
	    {
		((SerialInstrumentPort)_instrumentPort).setRTS(InstrumentPort.RTS_SENSE);
		_log4j.debug("instrumentPort asserting RTS "+InstrumentPort.RTS_SENSE+" for commsMode="
			     +_commsMode);
	    }else{
		_log4j.debug("instrumentPort: null serialPort; could not assert RTS for commsMode="
			     + _commsMode);
	    }
	}
    }


    /** Set up CommsMode for powerUpPort() */
    void powerUpCommsMode()
    {
	// assert RTS (false i.e., active low) for RS422
	if (_commsMode==CommsMode.RS422)
	{
	    try{
		SerialPort serialPort = ((SerialInstrumentPort)_instrumentPort).getSerialPort();
		_serialPort.getBaudRate();
		_instrumentPort.setCommsMode(CommsMode.RS422);
		serialPort.setRTS(InstrumentPort.RTS_SENSE);
		_log4j.info("powerUpPort asserting RTS "+InstrumentPort.RTS_SENSE+" for commsMode="+_commsMode);
	    }catch(Exception e){
		_log4j.error(e);
	    }
	}
    }

    /** Perform a CommPortIdentifier.addPortOwnershipListener()	*/
    public void addCommPortListener()
    {
	try {
	    CommPortIdentifier portId = 
		CommPortIdentifier.getPortIdentifier(_portName);

	    portId.addPortOwnershipListener(this);
	}
	catch ( NoSuchPortException e ) {
	    _log4j.error("NoSuchPortException for " + _portName + ": " + e);
	}

    }

    /** CommPortOwnwershipListener callback. */
    public void ownershipChange(int type) {
	_log4j.debug("Port ownership change: ");
	switch ( type ) {
	case CommPortOwnershipListener.PORT_OWNED:
	    _log4j.debug("OWNED");
	    break;

	case CommPortOwnershipListener.PORT_UNOWNED:
	    _log4j.debug("UNOWNED");
	    break;

	case CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED:
	    _log4j.debug("OWNERSHIP_REQUEST");
	    break;
	}
    }

}

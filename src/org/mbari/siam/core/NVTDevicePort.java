/**
 * @Title Network Virtual Terminal (NVT) SerialDevicePort
 * @author Bob Herlien
 * @version $Revision: 1.1 $
 * @date 8 July 2009
 *
 * Copyright 2009 MBARI
 */

package org.mbari.siam.core;

import gnu.io.SerialPort;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.core.nvt.NVTSerialPort;

/**
 * NVTDevicePort extends SerialDevicePort for an RFC2217 compliant serial device.
 * Put into core package because it inherits  from core.SerialDevicePort.
 */

public class NVTDevicePort extends SerialDevicePort
{
    private static Logger _log4j = Logger.getLogger(NVTDevicePort.class);
    protected NVTSerialPort _nvtSerialPort = null;
    protected String	  _portId;

    public NVTDevicePort(int index, String portId, String portName, PowerPort powerPort,
			 String jar, DeviceService service, CommsMode commsMode)
    {
	super(index, portName, powerPort, jar, service, commsMode);
	_portId = portId;
    }

    /** Open the serial port at a specified baud rate */
    public void openComms(int baudRate) throws UnknownHostException, IOException, Exception
    {
	StringTokenizer st;
	String	host = null;
	int	port = NVTSerialPort.DEFAULT_PORT;

	try {
	    st = new StringTokenizer(_portId, ":");
	} catch (Exception e) {
	    throw new UnknownHostException("Invalid port name: " + _portId);
	}

	try {
	    host = st.nextToken();
	    if (st.hasMoreTokens())
		port = Integer.parseInt(st.nextToken());
	} catch (Exception e) {
	    _log4j.warn("Exception in parseInt(), using default port: " + e);
	}

	_log4j.debug("Creating NVTSerialPort at host " + host + " port " + port);
	_nvtSerialPort = new NVTSerialPort(host, port);
	_serialPort = _nvtSerialPort;

	_log4j.debug("open serial port..." + _portName);
	_nvtSerialPort.open();

	if (_portName.equalsIgnoreCase("signature"))
	    try {
		_portName = _nvtSerialPort.getNVT().getSignature();
	    } catch (IOException e) {
		_log4j.error("Exception getting signature for " + _portName
			     + ": " + e);
	    } 

	_log4j.debug("set port params...");
	_nvtSerialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
					   SerialPort.STOPBITS_1, 
					   SerialPort.PARITY_NONE);
    }


    /** Close the serial port */
    public void closeComms()
    {
	if (_nvtSerialPort != null)
	    _nvtSerialPort.close();
    }

    /** Create the SerialInstrumentPort */
    public void createInstrumentPort() throws InitializeException
    {
	if (_nvtSerialPort == null)
	    try {
		openComms(9600);
	    } catch (Exception e) {
		throw new InitializeException(e.getMessage());
	    }

	_instrumentPort =
	    new NVTInstrumentPort(_nvtSerialPort, _powerPort);
	
	_instrumentPort.initialize();

	_log4j.debug("Created and initialized new SerialInstrumentPort:  " + _instrumentPort);

	if(_commsMode==CommsMode.RS422)
	    throw new InitializeException("RS422 not supported");
    }


    /** Null function.  We don't support RS422 */
    void powerUpCommsMode()
    {
    }

    /** Null function.  We don't support concept of ownership */
    public void addCommPortListener()
    {
    }
}

/**
 * @Title PUCK Utility Framework
 * @author Bob Herlien
 * @version $Revision: 1.3 $
 * @date 15 July 2009
 *
 * Copyright 2009 MBARI
 * Monterey Bay Aquarium Research Institute Proprietary Information.
 * All rights reserved.
 */

package org.mbari.siam.operations.utils;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.mbari.siam.core.nvt.NVTSerialPort;


public abstract class PuckUtil
{
    protected static Logger _log4j = Logger.getLogger(PuckUtil.class);

    public static final int _PUCK_DEFAULT_BAUD = 9600;

    // serial port vars
    protected CommPortIdentifier _commPortId = null;
    protected SerialPort _serialPort = null;

    public PuckUtil()
    {
	super();
    }


    protected SerialPort openLocalSerialPort(String comPortName)
	throws NoSuchPortException, PortInUseException
    {
	CommPortIdentifier id = CommPortIdentifier.getPortIdentifier(comPortName);
	return((SerialPort)(id.open(this.getClass().getName(), 1000)));
    }

    protected SerialPort openNVTSerialPort(String comPortName)
	throws UnknownHostException, IOException
    {
	String	host = null;
	int	port = NVTSerialPort.DEFAULT_PORT;

	try {
	    StringTokenizer st = new StringTokenizer(comPortName, ":");
	    host = st.nextToken();
	    if (st.hasMoreTokens())
		port = Integer.parseInt(st.nextToken());
	} catch (Exception e) {
	    throw new UnknownHostException("Invalid port name: " + comPortName);
	}

	NVTSerialPort serPort = new NVTSerialPort(host, port);
	serPort.open();
	return(serPort);
    }

    protected boolean initSerialPort(String comPortName, int baud_rate)
    {
        try {
	    _serialPort = openLocalSerialPort(comPortName);
        } catch (NoSuchPortException e) {
	    _log4j.debug("No local port: " + comPortName);
        } catch (PortInUseException e) {
            _log4j.error("Local port " + comPortName + " is in use: " + e);
            return false;
        }

	if (_serialPort == null)
	    try {
		_serialPort = openNVTSerialPort(comPortName);
	    } catch (Exception e) {
		_log4j.error("Can't open NVT port " + comPortName + ": " + e);
		return false;
	    }

        try {
            _serialPort.setSerialPortParams(baud_rate, 
					    _serialPort.getDataBits(), 
					    _serialPort.getStopBits(), 
					    _serialPort.getParity());

        } catch (UnsupportedCommOperationException e) {
            _log4j.error("Error while setting serial port parameters: " + e);
            return false;
        }

        return true;
    }
}

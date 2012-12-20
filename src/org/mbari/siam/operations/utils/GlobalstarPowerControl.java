/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEventListener;
import gnu.io.SerialPortEvent;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;


/**
   This class properly shuts down the Qualcomm 1620 (Globalstar) modem, per
   instructions in the Qualcomm users manual.
 */
public class GlobalstarPowerControl implements SerialPortEventListener {

    private static Logger _log4j = 
	Logger.getLogger(GlobalstarPowerControl.class);

    public static final int BAUD = 19200;
    public static final int MAX_WAIT_SEC = 15;
    SerialPort _serialPort;


    public GlobalstarPowerControl(String portName) throws Exception {

	initializeSerialPort(portName, BAUD);
    }


    /** Initiate modem power-down */
    protected void powerDown() throws Exception {
	// Clear DTR
	printLineStatus();
	_log4j.debug("Clear DTR");
	_serialPort.setDTR(false);

	while (_serialPort.isDSR()) {
	    printLineStatus();
	    Thread.sleep(1000);
	}

	return;
    }



    /** Initiate modem power-up */
    protected void powerUp() throws Exception {
	// Assert DTR
	printLineStatus();
	_log4j.debug("Assert DTR");
	_serialPort.setDTR(true);

	while (!_serialPort.isDSR()) {
	    printLineStatus();
	    Thread.sleep(1000);
	}

	return;
    }


    /** Initialize the serial port. */
    protected void initializeSerialPort(String portName, int baud) 
	throws Exception {

        CommPortIdentifier commPortId = null;

	commPortId = CommPortIdentifier.getPortIdentifier(portName);


	_serialPort = 
	    (SerialPort )commPortId.open(this.getClass().getName(), 1000);


	_serialPort.setSerialPortParams(baud, 
					_serialPort.getDataBits(), 
					_serialPort.getStopBits(), 
					_serialPort.getParity());

	_serialPort.addEventListener(this);

    }


    /** Propagate serial port event */
    public void serialEvent(SerialPortEvent event) {

	System.out.println("Serial event: " + event);
	printLineStatus();
	if (!_serialPort.isDSR()) {
	    System.out.println("DSR de-asserted - safe to power off?"); 
	}
    }


    /** Print out serial line status */
    protected void printLineStatus() {
	System.out.println("DTR: " + _serialPort.isDTR() + 
			   ", DSR: " + _serialPort.isDSR());
    }


    public static void main(String[] args) {

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	boolean turnOn = true;
	boolean error = false;

        if (args.length != 2) {
	    error = true;
        }
	else if (args[1].equalsIgnoreCase("on")) {
	    turnOn = true;
	}
	else if (args[1].equalsIgnoreCase("off")) {
	    turnOn = false;
	}
	else {
	    error = true;
	}
	
	if (error) {
            System.out.println("usage: serialPortName on|off");
            System.exit(1);
	}

	try {
	    _log4j.debug("Create power control object");
	    GlobalstarPowerControl powerControl = 
		new GlobalstarPowerControl(args[0]);

	    _log4j.debug("Initiate modem power-down");
	    if (turnOn) {
		powerControl.powerUp();
		System.out.println("Modem power-up sequence complete");
	    }
	    else {
		powerControl.powerDown();
		System.out.println("Modem power-down sequence complete");
	    }

	}
	catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
    }
}


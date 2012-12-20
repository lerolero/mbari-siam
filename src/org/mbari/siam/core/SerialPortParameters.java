/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
SerialPortParameters specifies baud, number of data bits, parity, and
number of stop bits.

@author Tom O'Reilly
*/
public class SerialPortParameters {

    /** Baud rate. */
    private int _baud;

    /** Data bits; must be one of DATABITS_5, DATABITS_6, DATABITS_7, or
	DATABITS_8 as defined in gnu.io.SerialPort. */
    private int _dataBits;

    /** Parity; must be one of PARITY_EVEN, PARITY_ODD, PARITY_NONE, 
	PARITY_MARK, or PARITY_SPACE as defined in gnu.io.SerialPort. */
    private int _parity;

    /** Stop bits; must be one of STOPBITS_1, STOPBITS_1_5, or
	STOPBITS_2 as defined in gnu.io.SerialPort. */
    private int _stopBits;


    /**
       SerialPortParameter constructor. 
       @param baud baud rate
       @param dataBits must be one of DATABITS_5, DATABITS_6, DATABITS_7, 
       or DATABITS_8 as defined in gnu.io.SerialPort.
       @param parity must be one of PARITY_EVEN, PARITY_ODD, PARITY_NONE, 
       PARITY_MARK, or PARITY_SPACE as defined in gnu.io.SerialPort.
       @param stopBits must be one of STOPBITS_1, STOPBITS_1_5, or
       STOPBITS_2 as defined in gnu.io.SerialPort.
    */
    public SerialPortParameters(int baud, int dataBits, int parity, 
				int stopBits) 
    throws UnsupportedCommOperationException {

	_baud = baud;

	if (dataBits == SerialPort.DATABITS_5 ||
	    dataBits == SerialPort.DATABITS_6 ||
	    dataBits == SerialPort.DATABITS_7 ||
	    dataBits == SerialPort.DATABITS_8) {

	    _dataBits = dataBits;
	}
	else
	    throw new UnsupportedCommOperationException();


	if (parity == SerialPort.PARITY_EVEN ||
	    parity == SerialPort.PARITY_ODD ||
	    parity == SerialPort.PARITY_NONE ||
	    parity == SerialPort.PARITY_MARK ||
	    parity == SerialPort.PARITY_SPACE) {

	    _parity = parity;
	}
	else 
	    throw new UnsupportedCommOperationException();


	if (stopBits == SerialPort.STOPBITS_1 ||
	    stopBits == SerialPort.STOPBITS_1_5 ||
	    stopBits == SerialPort.STOPBITS_2) {

	    _stopBits = stopBits;
	}
	else 
	    throw new UnsupportedCommOperationException();
    }


    /** Return baud. */
    public int getBaud() {
	return _baud;
    }

    /** Return number of data bits; will be one of DATABITS_5, DATABITS_6, 
	DATABITS_7, or DATABITS_8 as defined in gnu.io.SerialPort. */
    public int getDataBits() {
	return _dataBits;
    }

    public int getParity() {
	return _parity;
    }

    public int getStopBits() {
	return _stopBits;
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.waveSensor;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.rmi.RemoteException;

import org.mbari.siam.core.SerialPortParameters;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PacketParser;

/**
 * Implementation of TriAxis wave sensor.
 */
public class WaveSensor extends HeaveSensor implements Instrument {

	static private Logger _logger = Logger.getLogger(WaveSensor.class);

	/** Constructor; can throw RemoteException. */
	public WaveSensor() throws RemoteException {
		super();
	}

	/** Return parameters to use on serial port. */
	public SerialPortParameters getSerialPortParameters()
			throws UnsupportedCommOperationException {

		return new SerialPortParameters(9600, SerialPort.DATABITS_8,
				SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	}

	/** Return packet parser. */
	public PacketParser getParser() {
		return new WaveSensorPacketParser();
	}

	/**
	 * Note that Triaxys wave sensor prompt changes from '...' to '***'
	 * following acquisition of first sample.
	 */
	void setPostSamplePrompt() {
		setPromptString("**".getBytes());
		setSampleTerminator("**".getBytes());
	}

}
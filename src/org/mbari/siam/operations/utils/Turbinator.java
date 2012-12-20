/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.core.SiamTimer;
import org.mbari.siam.core.SiamTimerTask;

/** Periodically retrieve sensor data packet from specified instrument service,
    parse it, and write to data turbine channels */
public class Turbinator extends PortUtility {

    int _sampleIntervalSec = 10;
    PacketParser _parser;
    Instrument _instrument;
    org.mbari.siam.dataTurbine.Turbinator _turbinator;
    

    /** Print usage message. */
    public void printUsage() {
	System.err.println("Turbinator nodeURL port");
    }


    public void processPort(Node node, String portName) 
	throws RemoteException {

	System.out.println("get device");
	Device device = null;
	try {
	    device = node.getDevice(portName.getBytes());
	}
	catch (Exception e) {
	    throw new RemoteException(e.getMessage());
	}

	if (!(device instanceof Instrument)) {

	    throw new RemoteException("No instrument service on " + portName);
	}


	_instrument = (Instrument )device;
	try {
	    System.out.println("get parser");
	    _parser = _instrument.getParser();
	}
	catch (Exception e) {
	    throw new RemoteException(e.getMessage());
	}

	String instrumentName = new String(_instrument.getName());

	try {
	    System.out.println("create turbinator");
	    _turbinator = 
		new org.mbari.siam.dataTurbine.Turbinator(_parser, 
							  instrumentName,
							  "localhost",
							  "unknownLocation",
							  instrumentName,
							  false);
	}
	catch (Throwable e) {
	    throw new RemoteException(e.getMessage());
	}

	// Create task to periodically sample instrument and feed the 
	// DataTurbine ring buffer.
	System.out.println("create sampler task");
	(new SiamTimer()).schedule(new SamplerTask(), _sampleIntervalSec);

    }

    class SamplerTask extends SiamTimerTask {
	/** Get latest sample, parse it and write to DataTurbine ring buffer */
	public void run() {
	    try {
		_turbinator.write(_instrument.getLastSample());
	    }
	    catch (Exception e) {
		System.err.println(e);
	    }
	}
    }

    public static void main(String[] args) {
	Turbinator app = new Turbinator();
	app.processArguments(args);
	try {
	    app.run();
	}
	catch (Throwable e) {
	    System.err.println(e);
	    e.printStackTrace();
	}
    }
}

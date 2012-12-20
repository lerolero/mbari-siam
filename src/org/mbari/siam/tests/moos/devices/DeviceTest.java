/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.devices;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;

import org.mbari.siam.utils.SiamSocketFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;

/**
Perform tests on Device service.
*/
public class DeviceTest {

    static protected Logger _logger = Logger.getLogger(DeviceTest.class);
    private static final String _releaseName = new String("$Name: HEAD $");

    protected Device _device;

    public DeviceTest(String deviceURL) throws Exception {


	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	System.out.println("SIAM version " + _releaseName);

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new SecurityManager());
	}

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = getHostName(deviceURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    System.err.println("Malformed URL \"" + deviceURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	}


	// Get the device proxy
	System.out.println("Get device proxy at " + deviceURL.toString());
	try {
	    _device = (Device )Naming.lookup(deviceURL.toString());
	    _logger.info("Got device proxy");
	}
	catch (Exception e) {
	    System.err.println("Couldn't get device proxy at " + deviceURL + 
			       ":");
	    System.err.println(e);
	    throw e;
	}
    }


    /** Run the tests. */
    public void run() {

	DevicePacket packet = null;

	try {
	    packet = _device.getMetadata("Testing".getBytes(), Device.MDATA_ALL, false);
	    System.out.println("\nDevice metadata:\n" + packet);
	}
	catch (Exception e) {
	    _logger.error(e);
	}

	if (_device instanceof Instrument) {
	    Instrument instrument = (Instrument )_device;

	    try {
		packet = instrument.getLastSample();
		System.out.println("\nLast sample:\n" + packet);
	    }
	    catch (NoDataException e) {
		_logger.error("No data from instrument: " + e.getMessage());
	    }
	    catch (Exception e) {
		_logger.error(e);
	    }

	    try {
		packet = instrument.acquireSample(false);
		System.out.println("\nNew sample:\n" + packet);
	    }
	    catch (NoDataException e) {
		_logger.error("No data from instrument: " + e.getMessage());
	    }
	    catch (Exception e) {
		_logger.error(e);
	    }
	}
	else {
	    _logger.warn("Device is not an Instrument");
	}
    }


    /**
       Return SIAM device URL corresponding to input. Input may already
       be in URL format, in which case the input is simply returned. Input may
       also be abbreviated as just a hostname, in which case it is converted 
       a SIAM device RMI URL and returned.
    */
    public static final String getNodeURL(String input) {

	_logger.debug("getNodeURL(): input=" + input);

	if (input.startsWith("rmi://")) {
	    return input;
	}
	else if (input.startsWith("//")) {
	    return "rmi:" + input;
	}
	else {
	    return "rmi://" + input;
	}
    }

    /** Return the 'hostname' portion of the Device's URL. Note that this
	is a TOTAL hack, since Java's URL class does not support
	"rmi" as a valid protocol!!! */
    public static String getHostName(String nodeURL) {

	// NOTE: This method only works if the node's url starts with "rmi"!
	// Build a temporary valid URL by replacing "rmi" with "ftp"
	StringBuffer buf = new StringBuffer(nodeURL);
	buf.replace(0, 3, "ftp");
	try {
	    URL url = new URL(new String(buf));
	    return url.getHost();
	}
	catch (MalformedURLException e) {
	    System.err.println("MalformedURLException on \"" + 
			       new String(buf) + "\": " + e.getMessage());
	}
	return "dummy";
    }


    public static void main(String[] args) {

	if (args.length != 1) {
	    System.err.println("usage: deviceURL");
	    return;
	}

	try {
	    DeviceTest test = new DeviceTest(getNodeURL(args[0]));
	    test.run();
	}
	catch (Exception e) {
	    _logger.error(e);
	}
    }
}



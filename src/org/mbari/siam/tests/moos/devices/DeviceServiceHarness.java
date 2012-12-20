/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Oct 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mbari.siam.tests.moos.devices;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.EventObject;

import org.mbari.siam.distributed.Location;
import org.mbari.siam.core.ServiceEvent;
import org.mbari.siam.operations.utils.ExportablePacket;
import moos.ssds.jms.PublisherComponent;
import org.mbari.siam.utils.DeviceServiceLoader;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Parent;

/**
 * Instantiate, initialize, and run a device service. The service bytecode, any
 * related classes, and properties file are contained within a standard SIAM
 * instrument jar file.
 * 
 * @author oreilly
 *  
 */
public class DeviceServiceHarness {

    static final String PROPERTIES_FILENAME = "service.properties";
    static final String XML_FILENAME = "service.xml";
    static final String CACHE_FILENAME = "service.cache";

    static Logger _logger = Logger.getLogger(DeviceServiceHarness.class);

    ExportablePacket _exportablePacket = new ExportablePacket();
    PublisherComponent _ssdsPublisher = new PublisherComponent();
    DeviceServiceLoader _serviceLoader = new DeviceServiceLoader();
    String _serviceURL = "";
    Device _service;

    /**
     *  
     */
    public DeviceServiceHarness() {
	super();
	// TODO Auto-generated constructor stub
    }

    /** Run the test. */
    public void run(String jarFileName, String portName, 
		    String codebaseDirectory) throws Exception {

	String serviceURL = 
	    _serviceLoader.run(jarFileName, portName, 
			       codebaseDirectory, new MyParent(),
			       TestNodeService.INSTRUMENT_URL);

	// Try to get the service proxy
	_service = (Device )Naming.lookup(serviceURL);
    }


    /** Implements Parent for this test. */
    class MyParent implements Parent {

	public void runDiagnostics(String string) throws Exception {
	    System.out.println("MyParent.runDiagnostics()");
	}

	public String getSoftwareVersion() {
	    return "dummy version";
	}

	/** Return this parent's ISI ID. */
	public long getParentId() {
	    return 999;
	}

	/** Return location of specified device. */
	public Location getLocation(long deviceID) {
	    return new Location("In the lab");
	}

	/** Request power from the parent; return true if available, false if
	    not available. */
	public boolean powerAvailable(int milliamp) {
	    _logger.warn("powerAvailable() not implemented");
	    return true;
	}

	/** Publish the specified event. Event published must be an instance of
	    NodeEvent. */
	public void publish(EventObject event) {
	    _logger.debug("publish()");

	    if (event instanceof ServiceEvent) {
		ServiceEvent serviceEvent = (ServiceEvent )event;
		if (serviceEvent.getStateChange() == 
		    ServiceEvent.SAMPLE_LOGGED) {
		    // Must be an instrument service that generated this
		    publishLastSample((Instrument )_service);
		}
		else {
		    _logger.info("Got ServiceEvent type " + 
				 serviceEvent.getStateChange());
		}
	    }
	    else {
		_logger.warn("publish() - event is not a ServiceEvent");
	    }
	}

	/** Publish most recent sample to SSDS. */
	private void publishLastSample(Instrument service){
	    _logger.debug("publishLastSample()");
	    DevicePacket packet = null;
	    try {
		packet = service.getLastSample();
		System.out.println("Would publish: " + packet);
	    } catch (NoDataException e1) {
		_logger.error("No data from service");
		return;
	    }
	    catch (RemoteException e) {
		_logger.error(e);
		return;
	    }

	    /* ***
	    try {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		_exportablePacket.wrapPacket(packet);
		_exportablePacket.export(dos);
		dos.flush();
		byte[] exportedBytes = bos.toByteArray();
		dos.close();
		_ssdsPublisher.publishBytes(exportedBytes);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    *** */
	}
    }


    static public void main(String[] args) {

	if (args.length != 3) {
	    System.err.println("usage: jarfile portname codebaseDirectory");
	    return;
	}

	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	DeviceServiceHarness test = new DeviceServiceHarness();

	try {
	    test.run(args[0], args[1], args[2]);
	} catch (Exception e) {
	    System.err.println("Caught exception from run():\n" + e);
	    e.printStackTrace();
	}
    }
}

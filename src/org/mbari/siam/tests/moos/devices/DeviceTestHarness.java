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

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import javax.swing.Timer;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.core.InstrumentPort;
//import moos.deployed.Location;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.NullPowerPort;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.ServiceEvent;
import org.mbari.siam.core.PuckSerialInstrumentPort;
import org.mbari.siam.operations.utils.ExportablePacket;
import org.mbari.siam.utils.FileUtils;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.DeviceServiceLoader;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PropertyException;
import moos.ssds.jms.PublisherComponent;

/**
 * Instantiate, initialize, and run a device service. The service bytecode, any
 * related classes, and properties file are contained within a standard SIAM
 * instrument jar file.
 * 
 * @author oreilly
 *  
 */
public class DeviceTestHarness {

    static final String PROPERTIES_FILENAME = "service.properties";
    static final String XML_FILENAME = "service.xml";
    static final String CACHE_FILENAME = "service.cache";

    static Logger _log4j = Logger.getLogger(DeviceTestHarness.class);

    boolean _publish = false;

    ExportablePacket _exportablePacket = new ExportablePacket();
    PublisherComponent _ssdsPublisher = null;
    DeviceServiceLoader _serviceLoader = new DeviceServiceLoader();
    String _serviceURL = "";
    Device _service;

    /**
     *  
     */
    public DeviceTestHarness() {
	super();
	// TODO Auto-generated constructor stub

	if (_publish) {
	    _ssdsPublisher = new PublisherComponent();
	}
    }

    /** Run the test. */
    public void run(String jarFileName, String portName, 
		    String codebaseDirectory) throws Exception {

        System.setProperty("gnu.io.rxtx.SerialPorts", portName);

	_serviceURL = 
	    _serviceLoader.run(jarFileName, portName, 
			       codebaseDirectory, new MyParent(),
			       TestNodeService.INSTRUMENT_URL);

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
	    _log4j.warn("powerAvailable() not implemented");
	    return true;
	}


	/** Publish the specified event. Event published must be an instance of
	    NodeEvent. Since we want this utility to be usable on a Sidearm,
	    we can't use JMS stuff, and so can't publish to SSDS. Just print a 
	    message. 
	*/
	public void publish(EventObject event) {
	    _log4j.debug("publish()");

	    if (_service == null) {
		// Try to get the service proxy
		try {
		    _service = (Device )Naming.lookup(_serviceURL);
		    _log4j.info("Got service proxy");
		}
		catch (Exception e) {
		    _log4j.error(e);
		    return;
		}
	    }

	    if (event instanceof ServiceEvent) {
		ServiceEvent serviceEvent = (ServiceEvent )event;
		if (serviceEvent.getStateChange() == 
		    ServiceEvent.SAMPLE_LOGGED) {
		    // Must be an instrument service that generated this
		    Instrument service = (Instrument )_service;
		    DevicePacket packet = null;
		    try {
			packet = service.getLastSample();
			if (_publish) {
			    publishSample(packet);
			}	
			else {
			    System.out.println("Would publish: " + packet);
			}
		    } catch (NoDataException e1) {
			_log4j.error("No data from service");
			return;
		    }
		    catch (RemoteException e) {
			_log4j.error(e);
			return;
		    }
		}
		else {
		    _log4j.info("Got ServiceEvent type " + 
				 serviceEvent.getStateChange());
		}
	    }
	    else {
		_log4j.warn("publish() - event is not a ServiceEvent");
	    }
	}

    }

    void publishSample(DevicePacket packet) {
	try {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(bos);
	    _exportablePacket.wrapPacket(packet);
	    _exportablePacket.export(dos);
	    dos.flush();
	    byte[] exportedBytes = bos.toByteArray();
	    dos.close();
	    _ssdsPublisher.publishBytes(exportedBytes);
	}
	catch (IOException e) {
	    e.printStackTrace();
	}
    }

	
    static public void main(String[] args) {

	if (args.length < 3 || args.length > 4) {
	    System.err.println("usage: jarfile portname codebaseDirectory [-publish]");
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

	DeviceTestHarness test = new DeviceTestHarness();

	if (args.length == 4) {
	    // Look for -publish option
	    if (args[3].equals("-publish")) {
		test._publish = true;
	    }
	    else {
		System.err.println("usage: jarfile portname codebaseDirectory [-publish]");
		return;
	    }
	}

	try {
	    test.run(args[0], args[1], args[2]);
	} catch (Exception e) {
	    System.err.println("Caught exception from run():\n" + e);
	    e.printStackTrace();
	}
    }
}

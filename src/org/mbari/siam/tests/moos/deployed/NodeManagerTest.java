/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;

import java.lang.*;
import java.util.*;
//import java.util.Properties;
//import java.util.Vector;
//import java.util.Iterator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.NodeEventCallback;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.mbari.siam.core.NodeManager;
import org.mbari.siam.core.PortManager;
import org.mbari.siam.core.ScheduleTask;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.Scheduler;
import org.mbari.siam.devices.dummy.DummyInstrument;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.PrintUtils;

/**
JUnit test harness for testing NodeManager class.
@author Karen A. Salamy and Kent Headley
 */

//Creating a subclass of TestCase
public class NodeManagerTest extends TestCase
{
    static private Logger _logger=Logger.getLogger(NodeManagerTest.class);


    public NodeManagerTest(String methodName) {
	super(methodName);
	_logger.debug("NodeManageTest CTOR");	

    }
   
    //First thing to do is to make the NodeManager
    public void testNodeManagerConstructor() {

	_logger.debug("testNodeManagerConstructor()");	
	NodeManager manager = null;
	String portalHost = null;

	try {
	    manager = NodeManager.getInstance();
	    manager.start("node", portalHost);

	}
	catch (IOException e) {
	    _logger.error("IOException: " + e);
	    System.exit(1);
	}
	catch (MissingPropertyException e) {
	    _logger.error("MissingPropertyException: " + e);
	    System.exit(1);
	}
	catch (InvalidPropertyException e) {
	    _logger.error("InvalidPropertyException: " + e);
	    System.exit(1);
	}

	// Assert True that NodeManager does exist - Shows that it does exist.
	_logger.debug("Does NodeManager exist?");
	manager = NodeManager.getInstance();
	assertTrue(NodeManager.getInstance() != null);
	_logger.debug("This statement says that it does exist.");
	    
	// SampleInstrument code dump
	Node node = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());
        
	String nodeURL = "//localhost/node";
	String port = "/dev/ttyS0";
	try {

	    
		// Now sample instruments on specified ports


		try {
	
		    InstrumentService is = (InstrumentService)manager.getNodeService().getDevice(port.getBytes());

		    SensorDataPacket packet = 
			is.acquireSample(false);

		    assertTrue(packet.toString().indexOf("This is dummy instrument data")>=0);

		    _logger.debug(packet.toString());
		    
			
		    // Test the scheduler
		    _logger.debug("Test the instrument scheduler");

		    ScheduleTask schedule = is.getDefaultSampleSchedule();//simply the dummy
		    assertTrue(schedule != null);
		    _logger.debug("Instrument schedule exists.");
		    
		    String output = Scheduler.getInstance().showSchedule();//everything
		    _logger.debug("Scheduler shows" + output + "\nInstrument schedule " + schedule);
		    
		    int i = schedule.toString().indexOf("Name");
		    int j = schedule.toString().indexOf("Status");
		    assertTrue(output.indexOf(schedule.toString().substring(i,j)) >= 0);
		    
		}
		catch (PortNotFound e) {
		    System.err.println("Port " + port + " not found");
		}
		catch (DeviceNotFound e) {
		    System.err.println("Device not found on port " + 
				       port);
		}
		catch (NoDataException e) {
		    System.err.println("No data from instrument on port " + 
				       port);
		}
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    //System.exit(1);
	}
	//	System.exit(0);
    }


// End of SampleInstrument dump

  
    //Test method to assert expected results on the object under Test.
    public void testEmptyCollection() {
	Collection collection = new ArrayList();
	assertTrue(collection.isEmpty());
    }


    //Test getParentId method
    public void getParentId(){
	_logger.debug("getParentId() and (by default getId()");	
	NodeManager manager = NodeManager.getInstance(); //NodeManager constructor is created.
	int Id = 305;
	assertEquals(manager.getParentId(), Id);
	}


    // Implement a suite() method that uses reflection to dynamically create a test suire containing all the testXXX() methods.

    public static Test suite(){

	//Formatting output for log4J
	
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));


	_logger.debug("Test.suite() calling TestSuite()");	
	TestSuite suite = new TestSuite();

	_logger.debug("Test.suite() adding testEmpty...()");	
	suite.addTest(new NodeManagerTest("testEmptyCollection"));

	_logger.debug("Test.suite() adding getParent...()");	
	suite.addTest(new NodeManagerTest("getParentId"));

	_logger.debug("Test.suite() adding testNodeManager...()");	
	suite.addTest(new NodeManagerTest("testNodeManagerConstructor"));

	return suite;
}


} // End class

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.core.HostStatus;
import org.mbari.siam.core.CommStatus;

/**
JUnit test harness for testing StatusReport classes.
@author Tom O'Reilly
 */
public class StatusReportTest extends TestCase {

    static Logger _logger = Logger.getLogger(DeviceLogTest.class);
    static long _deviceID = 999;

    public StatusReportTest(String methodName) {
	super(methodName);
    }

    public void setUp() {
	BasicConfigurator.configure();
    }


    /** Test host status report. */
    public void hostStatus() {
	HostStatus hostStatus = new HostStatus(_deviceID);
	DeviceMessagePacket packet = hostStatus.getPacket();
	String message = new String(packet.getMessage());
	System.out.println(message);
    }

    /** Test comms status report. */
    public void commStatus() {
	CommStatus commStatus = new CommStatus(_deviceID);
	DeviceMessagePacket packet = commStatus.getPacket();
	String message = new String(packet.getMessage());
	System.out.println(message);
    }


    static public Test suite() {
	TestSuite suite = new TestSuite();
	suite.addTest(new StatusReportTest("hostStatus"));
	suite.addTest(new StatusReportTest("commStatus"));
	return suite;
    }
}


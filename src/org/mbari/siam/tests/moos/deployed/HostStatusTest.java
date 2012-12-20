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

public class HostStatusTest extends TestCase {

    static Logger _logger = Logger.getLogger(DeviceLogTest.class);
    static long _deviceID = 999;

    public HostStatusTest(String methodName) {
	super(methodName);
    }

    public void setUp() {
	BasicConfigurator.configure();
    }


    /** Verify that packet is properly appended to DeviceLog. */
    public void getPacket() {
	HostStatus hostStatus = new HostStatus(_deviceID);
	DeviceMessagePacket packet = hostStatus.getPacket();
	String message = new String(packet.getMessage());
	_logger.info(message);
    }


    static public Test suite() {
	TestSuite suite = new TestSuite();
	suite.addTest(new HostStatusTest("getPacket"));
	return suite;
    }
}


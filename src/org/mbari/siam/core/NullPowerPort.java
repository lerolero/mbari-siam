// Copyright MBARI 2003
package org.mbari.siam.core;

import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
Implementation of PowerPort interface; used when power control hardware
is absent.
@author Tom O'Reilly
*/
public class NullPowerPort implements PowerPort {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(NullPowerPort.class);

    public static final String TYPE_NAME = "NullPowerPort";

    private int _currentLimit = 0;
    private String _name = "null";

    /** Get name. */
    public String getName() {
	return _name;
    }

    /** Initialize the port. */
    public void initialize() {
	//_log4j.error("NullPowerPort.initialize()");
    }

    /** Set current limit on port. */
    public void setCurrentLimit(int currentLimit) 
	throws NotSupportedException {
	throw new NotSupportedException();
    }

    /** Get current limit of port. */
    public int getCurrentLimit() 
	throws NotSupportedException {
	throw new NotSupportedException();
    }

    /** Get the voltage level of the PowerPort; not supported by 
	NullPowerPort. */
    public float getVoltageLevel() 
	throws NotSupportedException {
	throw new NotSupportedException();
    }

    /** Get the current level of the PowerPort. not supported by 
	NullPowerPort. */
    public float getCurrentLevel() 
	throws NotSupportedException {
	throw new NotSupportedException();
    }

    /** Get the temperature the PowerPort. not supported by 
	NullPowerPort. */
    public float getTemperature() 
	throws NotSupportedException {
	throw new NotSupportedException();
    }

    /** Enable communications */
    public void enableCommunications() {
	//_log4j.error("NullPowerPort.enableCommunications()");
    }

    /** Disable communications */
    public void disableCommunications() {
	//_log4j.error("NullPowerPort.disableCommunications()");
    }

    /** Connect instrument to power. */
    public void connectPower() {
	//_log4j.error("NullPowerPort.connectPower()");
    }

    /** Disconnect instrument from power. */
    public void disconnectPower() {
	//_log4j.error("NullPowerPort.disconnectPower()");
    }

    /** Isolate comms and power from port. */
    public void isolatePort()
    {
	//_log4j.debug.println("NullPowerPort.isolatePort()");
    }

    /** Get status message. */
    public String getStatusMessage() {
	return TYPE_NAME;
    }

    /** Get status summary message. */
    public String getStatusSummaryMessage() {
	return TYPE_NAME;
    }

    /** Reset status. */
    public void resetStatus() {
	// No need to do anything here.
    }

    /** Get a terse status message */
    public String getTerseStatus() {
	return "OK";
    }

    /** Set communications mode (RS422,RS485,RS232)
	satisfies InstrumentPort interface
     */
    public void setCommsMode(CommsMode commsMode){
	// does nothing for NullPowerPort
	return;
    }

}

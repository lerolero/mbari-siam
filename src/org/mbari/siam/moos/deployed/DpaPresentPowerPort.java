// Copyright MBARI 2009
package org.mbari.siam.moos.deployed;

import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;

/**
This implementation of the PowerPort interface is used as a placeholder,
to indicate that the "DPA" string is present in siamPort.cfg.  The
actual SiamPowerPort is created later.
@author Bob Herlien
*/
public class DpaPresentPowerPort implements PowerPort {

    public static final String TYPE_NAME = "DpaPresentPowerPort";

    private String _name = "null";

    /** Get name. */
    public String getName() {
	return _name;
    }

    /** Initialize the port. */
    public void initialize() {
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

    /** Get the temperature the PowerPort. Not supported.
     */
    public float getTemperature() 
	throws NotSupportedException {
	throw new NotSupportedException();
    }

    /** Enable communications */
    public void enableCommunications() {
    }

    /** Disable communications */
    public void disableCommunications() {
    }

    /** Connect instrument to power. */
    public void connectPower() {
    }

    /** Disconnect instrument from power. */
    public void disconnectPower() {
    }

    /** Isolate comms and power from port. */
    public void isolatePort()
    {
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
    }
}

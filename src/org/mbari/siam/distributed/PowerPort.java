// Copyright 2003 MBARI
package org.mbari.siam.distributed;

import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;

/** 
    Interface to control a device's power. 
    @author Tom O'Reilly
*/
public interface PowerPort {

    /** Initialize the port. */
    public void initialize();

    /** Set current limit on port. */
    public void setCurrentLimit(int currentLimit) 
	throws RangeException, NotSupportedException;

    /** Get current limit of port. */
    public int getCurrentLimit()
	throws NoDataException, NotSupportedException;

    /** Get the voltage level of the PowerPort (volts). */
    public float getVoltageLevel()
	throws NoDataException, NotSupportedException;

    /** Get the current level of the PowerPort (amps). */
    public float getCurrentLevel()
	throws NoDataException, NotSupportedException;

    /** Get temperature. */
    public float getTemperature() 
	throws NoDataException, NotSupportedException;

    /** Get status message. */
    public String getStatusMessage();

    /** Get status summary message. */
    public String getStatusSummaryMessage();

    /** Get a terse status message */
    public String getTerseStatus();

    /** Reset status */
    public void resetStatus();

    /** Enable communications */
    public void enableCommunications();

    /** Disable communications */
    public void disableCommunications();

    /** Connect instrument to power. */
    public void connectPower();

    /** Disconnect instrument from power. */
    public void disconnectPower();

    /** Isolate comms and power from port. */
    public void isolatePort();

    /** Get name of power port. */
    public String getName();

    /** Set communications mode (RS422,RS485,RS232) */
    public void setCommsMode(CommsMode commsMode);

}

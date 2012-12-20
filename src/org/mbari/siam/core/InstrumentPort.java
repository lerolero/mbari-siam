// Copyright 2003 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;

import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.RangeException;

/**
   Interface that encompasses power control and communications to an 
   instrument.

   @author Mike Risi

 */
public interface InstrumentPort
{
    public static final boolean RTS_SENSE=false;
    
    /** Initialize the port. */
    public void initialize() throws InitializeException;
    
    /** resumes instruents comms from suspended state */
    public void resume();

    /** suspends comms so another application can communicate with the 
    instrument */
    public void suspend();

    /** Power down the instrument and close the communications port */
    public void shutDown();
    
    /** get the suspend state of the instrument port */
    public boolean isSuspended();

    /** get the name of the communications port */
    public String getCommPortName();
    
    /** get an InputStream to the instrument */
    public InputStream getInputStream() throws IOException;
    
    /** get an OutputStream to the instrument */
    public InstrumentPortOutputStream getOutputStream() throws IOException;

    /** get a console to an Instrument **/
    public InstrumentConsole getInstrumentConsole() throws IOException;
    
    /** Enable communications */
    public void enableCommunications();

    /** Disable communications */
    public void disableCommunications();

    /** Connect instrument to power. */
    public void connectPower();

    /** Disconnect instrument from power. */
    public void disconnectPower();

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

    /** Isolate comms and power from port. */
    public void isolatePort();

    /** Get health/status string. */
    public String getStatusMessage();

    /** Get health/status summary string. */
    public String getStatusSummaryMessage();

    /** Get terse summary string. */
    public String getTerseStatus();

    /** Reset status. */
    public void resetStatus();

    /** Set communications mode (RS422,RS485,RS232) */
    public void setCommsMode(CommsMode commsMode);
}

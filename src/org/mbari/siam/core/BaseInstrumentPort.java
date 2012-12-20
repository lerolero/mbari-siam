// Copyright 2003 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;

/**
   Implements power control and communications to an 
   instrument via serial port. May have a power port.
   @author Mike Risi
 */
public class BaseInstrumentPort {

    private static String _versionID = "$Revision: 1.2 $";

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(BaseInstrumentPort.class);


    boolean _instrumentPortSuspended = false;

    protected PowerPort _powerPort = null;

    
    public BaseInstrumentPort(PowerPort power) {
        _powerPort = power;
    }

    
    /** initialize the InstrumentPort */
    public void initialize() throws InitializeException
    {
	_log4j.debug("initialize()");

        if ( _powerPort == null ) {
            throw new InitializeException("PowerPort is null");
	}

        _powerPort.initialize();
       
        return;
    }

    /** shutdown the intrument port. This port must not be used again
     until the initialize() method is called again. */
    public void shutDown()
    {
        disconnectPower();
        disableCommunications();
        isolatePort();
    }
    
    /** get the suspend state of the instrument port */
    public boolean isSuspended()
    {
        return _instrumentPortSuspended; 
    }


    /** Enable communications */
    public void enableCommunications()
    {
        _powerPort.enableCommunications();
    }

    /** Disable communications */
    public void disableCommunications()
    {
        _powerPort.disableCommunications();
    }

    /** Connect instrument to power. */
    public void connectPower()
    {
        _powerPort.connectPower();
    }

    /** Disconnect instrument from power. */
    public void disconnectPower()
    {
        _powerPort.disconnectPower();
    }

    /** Set current limit on port. */
    public void setCurrentLimit(int currentLimit) 
	throws RangeException, NotSupportedException {
        _powerPort.setCurrentLimit(currentLimit);
    }

    /** Get current limit of port. */
    public int getCurrentLimit() 
	throws NoDataException, NotSupportedException {
        return _powerPort.getCurrentLimit();
    }

    /** Get the voltage level of the PowerPort. */
    public float getVoltageLevel() 
	throws NoDataException, NotSupportedException    {
        return _powerPort.getVoltageLevel();
    }

    /** Get the current level of the PowerPort. */
    public float getCurrentLevel()
	throws NoDataException, NotSupportedException {
        return _powerPort.getCurrentLevel();
    }

    /** Get the temperature of the PowerPort. */
    public float getTemperature()
	throws NoDataException, NotSupportedException {
        return _powerPort.getTemperature();
    }

    /** Isolate comms and power from port. */
    public void isolatePort()
    {
        _powerPort.isolatePort();
    }


    /** Sample health/status sensors.*/
    public String getStatusMessage() {

	return _powerPort.getStatusMessage();
    }


    /** Get health/status summary string, including extreme values. */
    public String getStatusSummaryMessage() {

	return _powerPort.getStatusSummaryMessage();
    }

    /** Reset status. */
    public void resetStatus() {
	_powerPort.resetStatus();
    }

    /** Get health/status summary string. */
    public String getTerseStatus() {
	return _powerPort.getTerseStatus();
    }


    /** Set communications mode (RS422,RS485,RS232)
	satisfies InstrumentPort interface.
	NOTE: this method really does not belong in the InstrumentPort 
	interface, since CommsMode is quite specific to serial instruments.
     */
    public void setCommsMode(CommsMode commsMode){
	_log4j.info("setCommsMode - does nothing by default");
    }

}

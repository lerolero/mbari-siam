// Copyright MBARI 2003
package org.mbari.siam.foce.deployed;

import org.mbari.siam.core.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;

public class FOCEPowerPort implements PowerPort
{
    static Logger _log4j = Logger.getLogger(FOCEPowerPort.class);

    protected String	_name = null;
    protected FOCERelayBoard _relayBoard;
    protected int	_powerBit;
    protected int	_numBits;
    protected boolean	_powerOn = false;

    public FOCEPowerPort(String name, FOCERelayBoard board, int powerBit, int numBits)
	throws NotSupportedException, IOException
    {
	_name = name;
	_relayBoard = board;
	_powerBit = powerBit;
	_numBits = numBits;

	if (powerBit + numBits > board.numRelays())
	    throw new NotSupportedException("Relay index too high or too many relays");

	_log4j.debug("Created FOCEPowerPort for " + board.getName() + 
		     ", power bit " + powerBit + ", " + numBits + " bit(s).");
    }

    /** Return name. */
    public String getName() {
	return _name;
    }

    /** Initialize the port. */
    public void initialize()
    {
	_log4j.debug("FOCEPowerPort.initialize()");
	disconnectPower();
    }

    /** Connect instrument to power. */
    public void connectPower() {
	_log4j.debug(getName() + " connectPower()");
	_powerOn = true;
	try {
	    if (_numBits <= 1)
		_relayBoard.powerOnBit(_powerBit);
	    else
		_relayBoard.powerOnBits(_powerBit, _numBits);

	} catch (IOException e) {
	    _log4j.error("Exception in connectPower(): " + e);
	}
    }

    /** Disconnect instrument from power. */
    public void disconnectPower() {
	_log4j.debug(getName() + " disconnectPower()");
	_powerOn = false;
	try {
	    if (_numBits <= 1)
		_relayBoard.powerOffBit(_powerBit);
	    else
		_relayBoard.powerOffBits(_powerBit, _numBits);
	} catch (IOException e) {
	    _log4j.error("Exception in disconnectPower(): " + e);
	}
    }

    /** Get current limit on port. */
    public int getCurrentLimit() 
	throws NotSupportedException {
	throw new NotSupportedException();
    }

    /** Get the voltage level in volts. */
    public float getVoltageLevel() {
	return((float)0.0);
    }

    /** Get the current level of the DpaChannel in amps. */
    public float getCurrentLevel()
	throws NotSupportedException
    {
	throw new NotSupportedException();
    }
    
    /** Set current limit on port. */
    public void setCurrentLimit(int currentLimit)
	throws NotSupportedException
    {
	throw new NotSupportedException();
    }

    /** Enable communications */
    public void enableCommunications()
    {
    }

    /** Disable communications */
    public void disableCommunications()
    {
    }

    /** Isolate comms and power from port. */
    public void isolatePort()
    {
    }


    public float getTemperature() throws NotSupportedException
    {
	throw new NotSupportedException();
    }


    /** Get status message. */
    public String getStatusMessage()
    {
	return("Power " + (_powerOn ? "on" : "off"));
    }


    /** Get status summary message. */
    public String getStatusSummaryMessage()
    {
	return(getStatusMessage());
    }

    /** Get a terse status message */
    public String getTerseStatus()
    {
	return(getStatusMessage());
    }


    /** Reset status. */
    public void resetStatus()
    {
    }

    /** Set communications mode (RS422,RS485,RS232)
	satisfies InstrumentPort interface
     */
    public void setCommsMode(CommsMode commsMode)
    {
    }

    /** True if this FOCEPowerPort represents the given relay board index
	and power bit
    */
    public boolean equals(int boardIndex, int powerBit)
    {
	return((_powerBit == powerBit) && (_relayBoard._boardIndex == boardIndex));
    }
}

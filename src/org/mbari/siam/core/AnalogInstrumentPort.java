// Copyright 2008 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.devices.AnalogBoard;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.RangeException;

/**
   Implements power control and communications to an 
   instrument via analog port. May have a power port.
   @author Bob Herlien
 */
public class AnalogInstrumentPort implements InstrumentPort
{
    private static String _versionID = "$Revision: 1.4 $";

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(AnalogInstrumentPort.class);

    protected String _portName = null;
    protected AnalogBoard _analogBoard = null;
    protected PowerPort _powerPort = null;
    protected boolean _instrumentPortSuspended = false;
    protected String _nullString = new String("");
    //protected int[] _params;
    protected ChannelParameters _params;


    /** Create an AnalogInstrumentPort. */
    public AnalogInstrumentPort(String portName,
				AnalogBoard analogBoard,
				PowerPort power,
				int boardNumber,
				ChannelRange channels[])
    {
	_portName = portName;
	_analogBoard = analogBoard;
        _powerPort = power;
	try{
	    _params=new ChannelParameters(boardNumber,channels);
	}catch(RangeException e){
	    _log4j.error(e);
	    e.printStackTrace();
	}
	//_params = params;
    }

    
    /** initialize the InstrumentPort */
    public void initialize() throws InitializeException
    {
        if ( _powerPort != null )
	    _powerPort.initialize();
    }

    /** Get the AnalogBoard		*/
    public AnalogBoard getAnalogBoard() {
	return(_analogBoard);
    }

    /** Get the parameters		*/
    public ChannelParameters getParams() {
	return(_params);
    }
    /*
    public int[] getParams() {
	return(_params);
    }
    */

    /** resume instrument comms from suspended state */
    public void resume()
    {
        _instrumentPortSuspended = false;
    }

    /** suspends comms so another application can communicate with the 
    instrument */
    public void suspend()
    {
        _instrumentPortSuspended = true;
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
        return(_instrumentPortSuspended);
    }

    /** get the commport name */
    public String getCommPortName()
    {
        return(_portName);
    }
    
    /** get an InputStream to the instrument */
    public InputStream getInputStream() throws IOException
    {
	// Should really throw IOException, but DeviceService will error out.
	return(null);
    }
    
    /** get an OutputStream to the instrument */
    public InstrumentPortOutputStream getOutputStream() throws IOException
    {
	// Should really throw IOException, but DeviceService will error out.
	return(null);
    }

    /** get a console to an Instrument **/
    public InstrumentConsole getInstrumentConsole() throws IOException
    {
	throw new IOException("Analog port has no InstrumentConsole");
    }   
    
    /** Enable communications */
    public void enableCommunications()
    {
	if (_powerPort != null)
	    _powerPort.enableCommunications();
    }

    /** Disable communications */
    public void disableCommunications()
    {
	if (_powerPort != null)
	    _powerPort.disableCommunications();
    }

    /** Connect instrument to power. */
    public void connectPower()
    {
	if (_powerPort != null)
	    _powerPort.connectPower();
    }

    /** Disconnect instrument from power. */
    public void disconnectPower()
    {
	if (_powerPort != null)
	    _powerPort.disconnectPower();
    }

    /** Set current limit on port. */
    public void setCurrentLimit(int currentLimit) 
	throws RangeException, NotSupportedException {
	if (_powerPort != null)
	    _powerPort.setCurrentLimit(currentLimit);
    }

    /** Get current limit of port. */
    public int getCurrentLimit() 
	throws NoDataException, NotSupportedException {
	if (_powerPort == null)
	    throw new NoDataException("Analog port has no power port.");
        return(_powerPort.getCurrentLimit());
    }

    /** Get the voltage level of the PowerPort. */
    public float getVoltageLevel() 
	throws NoDataException, NotSupportedException    {
	if (_powerPort == null)
	    throw new NoDataException("Analog port has no power port.");
        return(_powerPort.getVoltageLevel());
    }

    /** Get the current level of the PowerPort. */
    public float getCurrentLevel()
	throws NoDataException, NotSupportedException {
	if (_powerPort == null)
	    throw new NoDataException("Analog port has no power port.");
        return(_powerPort.getCurrentLevel());
    }

    /** Isolate comms and power from port. */
    public void isolatePort()
    {
	if (_powerPort != null)
	    _powerPort.isolatePort();
    }


    /** Sample health/status sensors.*/
    public String getStatusMessage()
    {
	if (_powerPort != null)
	    return(_powerPort.getStatusMessage());

	return(_nullString);
    }


    /** Get health/status summary string, including extreme values. */
    public String getStatusSummaryMessage() {

	if (_powerPort != null)
	    return _powerPort.getStatusSummaryMessage();

	return(_nullString);
    }

    /** Reset status. */
    public void resetStatus() {
	if (_powerPort != null)
	    _powerPort.resetStatus();
    }


    /** Get health/status summary string. */
    public String getTerseStatus() {
	if (_powerPort != null)
	    return _powerPort.getTerseStatus();

	return(_nullString);
    }

    /** Set communications mode (RS422,RS485,RS232)
	satisfies InstrumentPort interface
     */
    public void setCommsMode(CommsMode commsMode){
    }

    public int getBoard(){
	return _params.boardNumber();
    }
    public ChannelRange[] getChannels(){
	return _params.getChannels();
    }
    public int numberOfChannels(){
	return _params.numberOfchannels();
    }

}

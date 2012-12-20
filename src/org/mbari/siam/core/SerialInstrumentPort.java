// Copyright 2003 MBARI
package org.mbari.siam.core;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.UnsupportedCommOperationException;

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
public class SerialInstrumentPort 
    extends BaseInstrumentPort 
    implements InstrumentPort
{
    private static String _versionID = "$Revision: 1.4 $";

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(SerialInstrumentPort.class);

    protected SerialPort _serialPort = null;
    protected String _serialPortName = null;
    protected boolean _instrumentPortSuspeneded = false;
    protected boolean _serialOpen = false;

    protected InstrumentPortInputStream _fromInstrument = null;
    protected InstrumentPortOutputStream _toInstrument = null;
    
    /** create a SerialInstrumentPort. Note that we pass in the
     serial port name in addition to the serial port object, since
    SerialPort.getName() appears to be screwed up for Win32. */
    public SerialInstrumentPort(SerialPort serial, 
				String serialPortName,
				PowerPort power) {
	super(power);
        _serialPort = serial;
	_serialOpen = true;
	_serialPortName = serialPortName;
    }

    
    /** initialize the InstrumentPort */
    public void initialize() throws InitializeException
    {
	_log4j.debug("initialize() for " + _serialPortName);

	super.initialize();

	if (!_serialOpen) {
	    // (Re-)Open the serial port

	    CommPortIdentifier id = null;
	    try {
		id = CommPortIdentifier.getPortIdentifier(_serialPortName);
	    }
	    catch (Exception e) {
		_log4j.error("initialize() - caught exception trying to " + 
			     "get port identifier for " + _serialPortName);

		throw new InitializeException(e.getMessage());
	    }

	    if (id.getPortType() != CommPortIdentifier.PORT_SERIAL) {

		throw new InitializeException("Port " + _serialPortName
					      + " is not a serial port.");
	    }

	    try {
		_serialPort = (SerialPort) id.open(_serialPortName, 1000);

		_serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, 
						SerialPort.PARITY_NONE);
	    }
	    catch (Exception e) {
		_log4j.error("initialize() - Couldn't set serial port params");
		throw new InitializeException(e.getMessage());
	    }
	}

        //get the serialport input and output streams
        try {
            _fromInstrument = 
                new InstrumentPortInputStream(this, 
                                              _serialPort.getInputStream());
        }
        catch(Exception e)
        {
            throw new InitializeException("getInputStream() failure: " + e);
        }
        
        try {
            _toInstrument = 
                new InstrumentPortOutputStream(this, 
                                               _serialPort.getOutputStream());
        }
        catch(Exception e)
        {
            throw new InitializeException("getOutputStream() failure: " + e);
        }

        return;
    }

    /** resume instrument comms from suspended state */
    public void resume()
    {
        _instrumentPortSuspeneded = false;
    }

    /** suspends comms so another application can communicate with the 
    instrument */
    public void suspend()
    {
        _instrumentPortSuspeneded = true;
    }

    /** shutdown the intrument port. This port must not be used again
     until the initialize() method is called again. */
    public void shutDown()
    {
	super.shutDown();
	_serialPort.close();
	_serialOpen = false;
    }
    

    /** get an InputStream to the instrument */
    public InputStream getInputStream() throws IOException
    {
        if ( _fromInstrument == null)
            throw new IOException("InputStream is null");
        
        return _fromInstrument; 
    }
    
    public boolean getCarrierDetectStatus(){
    	if(_serialPort == null){
    		return false;
    	}
    	return _serialPort.isCD();
    }

    public boolean getClearToSendStatus(){
    	if(_serialPort == null){
    		return false;
    	}
    	return _serialPort.isCTS();
    }
    
    /** get an OutputStream to the instrument */
    public InstrumentPortOutputStream getOutputStream() throws IOException
    {
        if ( _toInstrument == null)
            throw new IOException("OutputStream is null");
        
        return _toInstrument; 
    }

    /** get a console to an Instrument **/
    public InstrumentConsole getInstrumentConsole() throws IOException
    {
        return new InstrumentConsole(this, 
                                     _serialPort.getInputStream(),
                                     _serialPort.getOutputStream());
    }   
    
    /** get the commport name */
    public String getCommPortName()
    {
        return _serialPort.getName();
    }
    

    /** set the serial port parameters for the serial port associated with
    this instrument port */
    public void setSerialPortParams(SerialPortParameters params) 
        throws IOException, UnsupportedCommOperationException  
    {
	_serialPort.setSerialPortParams(params.getBaud(),
				        params.getDataBits(),
				        params.getStopBits(),
				        params.getParity());
    }


    /** get the serial port parameters for the serial port associated with
    this instrument port */
    public SerialPortParameters getSerialPortParams() 
        throws IOException, UnsupportedCommOperationException  
    {
	return new SerialPortParameters(_serialPort.getBaudRate(), 
                                        _serialPort.getDataBits(), 
                                        _serialPort.getParity(),
                                        _serialPort.getStopBits());
    }

    /** get the state of the CTS line */
    public boolean isCTS() { return _serialPort.isCTS(); }

    /** set the state of the RTS line */
    public void setRTS(boolean state) { _serialPort.setRTS(state); }


    /**
     * set the TX line to a BREAK condition in 100 ms increments
     * 
     * @param duration = mS x 250 where mS is required duration to nearest 100mS
     */
    public void sendBreak(int duration) { 
        _serialPort.sendBreak(duration);
    }


    /** Return serial port. */
    public SerialPort getSerialPort() {
	return _serialPort;
    }


    /** Set communications mode (RS422,RS485,RS232)
	satisfies InstrumentPort interface
     */
    public void setCommsMode(CommsMode commsMode){
	_log4j.debug("setting commsMode via SerialInstrumentPort.setCommsMode() to "+commsMode);
	_powerPort.setCommsMode(commsMode);
    }

}

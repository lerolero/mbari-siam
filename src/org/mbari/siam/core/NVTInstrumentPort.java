/**
 * @Title Network Virtual Terminal (NVT) SerialInstrumentPort
 * @author Bob Herlien
 * @version $Revision: 1.1 $
 * @date 8 July 2009
 *
 * Copyright 2009 MBARI
 */

package org.mbari.siam.core;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import org.mbari.siam.core.nvt.NVTSerialPort;
import org.mbari.siam.core.nvt.NVTInputStream;
import org.mbari.siam.core.nvt.NVTOutputStream;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;

/**
   Implements power control and communications to an 
   instrument via ethernet-to-serial terminal server using RFC 2217.
   May have a power port.  Put into core package because it inherits
   from core.SerialInstrumentPort
 */
public class NVTInstrumentPort extends SerialInstrumentPort implements InstrumentPort
{
    private static String _versionID = "$Revision: 1.1 $";

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(NVTInstrumentPort.class);

    protected NVTSerialPort _nvtSerialPort = null;

    /** create an NVTInstrumentPort. */
    public NVTInstrumentPort(NVTSerialPort serial, PowerPort power)
    {
	super(serial, serial.getName(), power);
	_nvtSerialPort = serial;
    }

    
    /** initialize the InstrumentPort */
    public void initialize() throws InitializeException
    {
	_log4j.debug("NVTInstrumentPort.initialize() for " + _serialPortName);
	
	if (! _nvtSerialPort.isOpen())
	    try {
		_nvtSerialPort.open();
	    } catch (Exception e) {
		throw new InitializeException("Exception opening serial port: " + e);
	    }
    
        //get the serialport input and output streams
	_fromInstrument = new InstrumentPortInputStream(this, new NVTInputStream(_nvtSerialPort));
	_toInstrument = new InstrumentPortOutputStream(this, new NVTOutputStream(_nvtSerialPort));

	//Set zero interbyte delay -- terminal server should do its own pacing.
	_toInstrument.setInterByteMsec(0);
	
        if ( _powerPort == null )
            throw new InitializeException("PowerPort is null");

	_powerPort.initialize();
       
        return;
    }

    /** Set communications mode (RS422,RS485,RS232).
	Not implemented for NVT device.
     */
    public void setCommsMode(CommsMode commsMode)
    {
    }

}

/**
Copyright 2003 MBARI.
MBARI Proprietary Information. All rights reserved. */
package org.mbari.siam.devices.smartstar;

import java.rmi.RemoteException;
import java.lang.Integer;
import java.io.IOException;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.StopWatch;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class SmartStar extends InstrumentService implements Instrument
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(SmartStar.class);

    int _bytesCaptured = 0;
    
    /** Default SmartStar serial baud rate */
    static final int BAUD_RATE = 9600;
    /** Default SmartStar serial data bits */
    static final int DATA_BITS = SerialPort.DATABITS_8;
    /** Default SmartStar serial stop bits */
    static final int STOP_BITS = SerialPort.STOPBITS_1;
    /** Default SmartStar parity checking */
    static final int PARITY = SerialPort.PARITY_NONE;
    
    /** Default SmartStar sample timeout */
    static final int SAMPLE_TIMEOUT = 3000;

    public SmartStar() throws RemoteException
    {
    }

    /** SmartStar power policy POWER_ALWAYS. */
    protected PowerPolicy initInstrumentPowerPolicy()
    {
        return PowerPolicy.ALWAYS;
    }

    /** SmartStar communications power policy POWER_ALWAYS. */
    protected PowerPolicy initCommunicationPowerPolicy()
    {
        return PowerPolicy.ALWAYS;
    }

    /** SmartStar startup time 1000 millisec. */
    protected int initInstrumentStartDelay()
    {
        return 1000;
    }

    /** SmartStar current limit 500 milliamps. */
    protected int initCurrentLimit()
    {
        return 1000;
    }

    /** SmartStar sample terminator "RDY\r" */
    protected byte[] initSampleTerminator()
    {
        return "RDY\r".getBytes();
    }

    /** SmartStar prompt "RDY\r" */
    protected byte[] initPromptString()
    {
        return "RDY\r".getBytes();
    }

    /** SmartStar maximum number of bytes in a instrument 
	data sample 1000. */
    protected int initMaxSampleBytes()
    {
        return 1000;
    }

    /** Initialize the compass. */
    protected void initializeInstrument() 
    {
        try 
        {
            setSampleTimeout(SAMPLE_TIMEOUT);
    	}
    	catch (RangeException e) 
        {
    	  _log4j.error("SmartStar.initializeInstrument(): " + 
                             e.getMessage());
    	}
    	
	// Turn on DPA power/comms 
	managePowerWake();
        StopWatch.delay(getInstrumentStartDelay());

        //grab get instrument attention
        try
        {
            getInstrumentAttention(3);
        }
        catch(Exception e)
        {
	    _log4j.error("SmartStar initialization: " + e.getMessage());
        }
	
	_log4j.debug("SmartStar: done with initializeInstrumen()\n");
    }
    


    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters() 
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(BAUD_RATE,
					DATA_BITS,
					PARITY,
					STOP_BITS);
    }



    /** Signal the SmartSar that you are ready to fetch the data.  This 
    method must be called before request sample */
    protected void prepareToSample() throws Exception 
    {
        
        _log4j.debug(new String(getName()) + 
                             ".prepareToSample(): entry");
        
        byte[] temp_bytes = new byte[32];
        byte[] command = "DATA CAPTURE\r".getBytes();
	
        //flush the input
        _fromDevice.flush();
        //capture the data
        _toDevice.write(command);
	//read back how many bytes of data was captured
        int bytes_read = StreamUtils.readUntil(_fromDevice, 
                                   temp_bytes, 
                                   getSampleTerminator(), 
                                   getSampleTimeout());

        //make a string with the bytes returned less the <CR>
	// (throws exception if parseInt gets CR)
        String total_bytes = new String(temp_bytes, 0, (bytes_read - 1));

    	_log4j.debug(new String(getName()) + 
                             ".prepareToSample() bytes captured :[" +
	                     total_bytes + "]");
        
        _bytesCaptured = Integer.parseInt(total_bytes);
        
        _log4j.debug(new String(getName()) + 
                             ".prepareToSample(): exit");

    }

    /** Read captured data sample from the SmartStar. */
    protected int readSample(byte[] sample) throws TimeoutException, Exception 
    {
        int bytes_read = StreamUtils.readUntil(_fromDevice, 
                                   sample, 
                                   getSampleTerminator(), 
                                   getSampleTimeout());

        if (bytes_read == _bytesCaptured)
            return bytes_read;
        else
           return -1;
    }

    /** Return SmartStar metadata. */
    protected byte[] getInstrumentMetadata() 
    {
	byte[] tmp_buff = new byte[80];
        int bytes_read = 0;

	try 
        {
	    getInstrumentAttention(3);
	    _toDevice.write("vers all\r".getBytes());
	    bytes_read = StreamUtils.readUntil(_fromDevice, 
                                   tmp_buff, 
                                   getSampleTerminator(), 
                                   getSampleTimeout());
	}
	catch (Exception e) 
        {
	    // Couldn't get metadata...
            _log4j.error("Couldn't get SmartStar metadata");
	    tmp_buff = "Couldn't get SmartStar metadata\n".getBytes();
	}

	String meta_data = "<SmartStar Metadata>\n" + 
            new String(tmp_buff, 0, bytes_read).replace('\r', '\n') +
            "</SmartStar Metadata>\n";

	return meta_data.getBytes();
    }
    
    /** Set the sensor's clock.  This does nothing in the SmartStar driver */
    public void setClock(long time) 
    {
    }

    /** Self-test routine; This does nothing in the SmartStar driver */
    public int test() 
    {
	return Device.OK;
    }
      
    /** Request captured data from the SmartStar */
    protected void requestSample() throws IOException 
    {
    	_log4j.debug(new String(getName()) + 
                             ".requestSample(): entry");
        
        byte[] command = "DATA FETCH\r".getBytes();
        _fromDevice.flush();
        _toDevice.write(command);

    	_log4j.debug(new String(getName()) + 
                             ".requestSample(): exit");
    }

    void getInstrumentAttention(int tries) throws Exception
    {
        // May take several attempts to get SmartStar prompt
	for (int i = 0; i < tries; i++) 
        {
            _fromDevice.flush();
            _toDevice.write("\r".getBytes());
	    
            try 
            {
                StreamUtils.skipUntil(_fromDevice, getPromptString(), getSampleTimeout());
	    }
	    catch (Exception e) 
            {
		// Timed out, or some other problem
		_log4j.error("SmartStar.getInstrumentAttention(): " + 
				     e.getMessage());
		if (i == tries - 1) 
                {
		    // No more tries left
		    throw new Exception("SmartStar.getInstrumentAttention(): " + 
                                        "exceeded max tries");
		}
		
                StopWatch.delay(100);
	    }
	}
    }


    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

	// Sample every 60 seconds by default
	return new ScheduleSpecifier(60000);
    }

}


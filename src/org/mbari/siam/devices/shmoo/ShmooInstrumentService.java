/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.shmoo;

import java.rmi.RemoteException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.AttributeChecker;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidPropertyException;

import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.PuckSerialInstrumentPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.DebugMessage;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/** Performs some internal tests of MMC and DPAs.  
    The Smha instrument for testing purposes.
*/
public class ShmooInstrumentService 
    extends InstrumentService implements Instrument 
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(ShmooInstrumentService.class);

    static final byte[] SHMOO_REQUEST = "SHMOO SAYS HI\r".getBytes();
    static final String DURATION_KEY = "duration";

    //    protected int _powerDuration = 2000;

    SerialInstrumentPort _serInstPort = null;

    // Configurable Shmoo attributes
    Attributes _attributes = new Attributes(this);

    public ShmooInstrumentService() throws RemoteException 
    {
	_log4j.debug("ShmooInstrumentService");
    }

    protected void initializeInstrument() throws InitializeException, Exception
    {
        //if it's a SerialInstrument port init the _serialInstPort var
        if ( (_instrumentPort instanceof SerialInstrumentPort) || 
             (_instrumentPort instanceof PuckSerialInstrumentPort) )
        {
            _log4j.debug("ShmooInstrumentService has " + 
                                 _instrumentPort.getClass().getName() + 
                                 " InstrumentPort");
            _serInstPort = (SerialInstrumentPort)_instrumentPort;
        }
    }

    /** Specify compass device startup delay (millisec) */
    protected int initInstrumentStartDelay() 
    {
    	return 500;
    }
    
    /** Specify compass prompt string. */
    protected byte[] initPromptString() 
    {
	return ">".getBytes();
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() 
    {
	return "\r".getBytes();
    }

    /** Specify maximum bytes in raw compass sample. */
    protected int initMaxSampleBytes() 
    {
	return 32;
    }

    /** Specify current limit. */
    protected int initCurrentLimit() 
    {
	return 9000;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() 
    {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() 
    {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Request a data sample from the compass. */
    protected void requestSample() throws IOException 
    {
    	_log4j.debug(new String(getName()) + 
                             ".requestSample(): exit");
    }

    /** Get attention of the instrument. */
    protected void getAttention(int maxTries) throws Exception 
    {
	return;
    }

    /** Return metadata. */
    protected byte[] getInstrumentMetadata() 
    {
	return "ShmooInstrumentService".getBytes();
    }


    /** Make sure the loop back test worked OK and
         
         - check the RTS to CTS loopback
         - measures the PowerPort's voltage 
         - measures the PowerPort's current
         
        wrap the results up in a SensorPacket hand hand it off     
    */
    protected SensorDataPacket processSample(byte[] sample, int total_bytes) 
    {
        boolean loopback_failed;
        boolean handshake_hi_failed;
        boolean handshake_lo_failed;
        StringBuffer test_results = new StringBuffer(256);
	
        
        // Set timestamp
	_sensorDataPacket.setSystemTime(System.currentTimeMillis());
        
        //sensor packet buff format should be 
        //loopback <TAB> handshake <TAB> voltage <TAB> current <TAB>   comments    <CR><LF>
        //  [0|1]          [0|1]         [xxx]         [xxx]        [xxx;xxx;xxx]
        //
        
        //assume the loop back passed, then test it
        loopback_failed = false;

        //did you get what you sent?
        for(int i = 0; i < SHMOO_REQUEST.length; i++)
            if (sample[i] != SHMOO_REQUEST[i])
                loopback_failed = true;
        
        //check RS-232 loop back
        if ( loopback_failed )
            test_results.append("0\t");
        else
            test_results.append("1\t");
        
        //check RTS to CTS hi and low state
        
        if ( _serInstPort != null )
        {
            //set RTS HI and check CTS
            _serInstPort.setRTS(true);
            //wait from RTS to stabilize before reading CTS
            StopWatch.delay(100);

            if ( _serInstPort.isCTS() )
            {
                //handshake HI results
                handshake_hi_failed = false;
                test_results.append("1\t");
            }
            else
            {
                //handshake HI results
                handshake_hi_failed = true;
                test_results.append("0\t");
            }

            //set RTS LO and check CTS
            _serInstPort.setRTS(false);
            //wait from RTS to stabilize before reading CTS
            StopWatch.delay(100);

            if ( _serInstPort.isCTS() )
            {
                //handshake LO results
                handshake_lo_failed = true;
                test_results.append("0\t");
            }
            else
            {
                //handshake LO results
                handshake_lo_failed = false;
                test_results.append("1\t");
            }
        }
        else
        {
            //if it's not a SerialInstrumentPort you can't do this test
            handshake_lo_failed = false;
            handshake_hi_failed = false;
            test_results.append("NA\t");
            test_results.append("NA\t");
        }
        
        //may want to make power on time a settable property
        //note: the power on is the sum of the RTS-CTS delays
        //plus the dealy below
        
        //let power stabilize
        StopWatch.delay(_attributes.powerDuration);
        
        //and random delay to power duration
        int randomDelay = 0;
        if ( _attributes.randPowerDuration > 0 )
        {
            //the forbidden dance
            Random rand_a_lor = new Random(System.currentTimeMillis());
            randomDelay = rand_a_lor.nextInt(_attributes.randPowerDuration);
            StopWatch.delay(randomDelay);
        }
        
        //measure current and voltage
	try {
	    _log4j.debug("channel voltage: " + 
			 _instrumentPort.getVoltageLevel());
	    _log4j.debug("channel current: " + 
			 _instrumentPort.getCurrentLevel());
        
	    test_results.append(_instrumentPort.getVoltageLevel() + "\t");
	    test_results.append(_instrumentPort.getCurrentLevel() + "\t");
	} catch (Exception e) {
	    _log4j.error("Exception in processSample(): " + e);
	}
        
        if ( _attributes.randPowerDuration > 0 )
        {
            test_results.append(randomDelay + "\t");
        }
        
        //append any comments
        if ( loopback_failed )
            test_results.append("loopback failed, read '" + 
                                new String(sample, 0 , total_bytes) + "'");

        if ( handshake_hi_failed )
        {
            if ( loopback_failed )
                test_results.append(", ");
            
            test_results.append("CTS read LO when RTS was HI");
        }
        
        if ( handshake_lo_failed )
        {
            if ( loopback_failed || handshake_hi_failed )
                test_results.append(", ");
            
            test_results.append("CTS read HI when RTS was LO");
        }

        if ( !loopback_failed && !handshake_hi_failed && !handshake_lo_failed )
            test_results.append("all tests passed");

        test_results.append("\r\n");

        //send back the results
	_sensorDataPacket.setDataBuffer(test_results.toString().getBytes());

	
        return _sensorDataPacket;
    }


    /** Read until you time out or get the terminator.  ShmooInstrumentService
    catches the TimeOutException and returns to allow the testing of RTS-CTS 
    loopback, current measurement, and voltage measurement.
    */
    protected int readSample(byte[] sample) 
	throws TimeoutException, IOException, Exception 
    {
        //You have to lie and say you got a byte even if you did'nt
        //or InstrumentService will not continue with getData().
        int bytes_read = 1;
        
        sample[0] = '\0';

        try
        {
            bytes_read = StreamUtils.readUntil(_fromDevice, 
                                   sample, 
                                   getSampleTerminator(), 
                                   getSampleTimeout());

        }
        catch(TimeoutException e)
        {
            _log4j.error(e.getMessage());
            return bytes_read;
        }
	                 
    
        return bytes_read;
    }


    /** No internal clock. */
    public void setClock(long t) 
    {
	return;
    }

    /** Self-test not implemented. */
    public int test() 
    {
	return Device.OK;
    }


    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters() 
        throws UnsupportedCommOperationException 
    {
        //may want to grab baud rate from service.properties
        
        return new SerialPortParameters(19200, 
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

	// Sample every 60 seconds by default
	return new ScheduleSpecifier(60000);
    }



    /** Configurable shmoo attributes */
    class Attributes 
	extends InstrumentServiceAttributes {

	/** Constructor, with required InstrumentService argument */
	Attributes(DeviceServiceIF service) {
	    super(service);
	}
	/** Power duration, in millisec */
	int powerDuration = 2000;

        /** Random amount of millisecs tacked onto Power duration per sample */
	int randPowerDuration = 0;

	/** Throw InvalidPropertyException if any invalid attribute 
	    values found */
	public void checkValues() 
	    throws InvalidPropertyException {

	    if (powerDuration <= 0) {
		throw new InvalidPropertyException(powerDuration + 
						   ": invalid powerDuration." +
						   " must be positive integer");
	    }
	}
    }

}


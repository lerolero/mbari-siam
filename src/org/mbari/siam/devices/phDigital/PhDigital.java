/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.phDigital;

import org.apache.log4j.Logger;

import java.io.IOException;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.rmi.RemoteException;
import java.util.StringTokenizer;
import java.text.ParseException;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;

import org.mbari.siam.utils.PrintfFormat;

import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.Velocity;
import org.mbari.siam.distributed.Temperature;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.PacketParser;

/** PhDigital implements a PolledInstrumentService for a 
  digital pH sensor.

  @author John Graybeal
*/


public class PhDigital extends PolledInstrumentService
    implements Instrument, InstrumentDataListener
{
    // CVS revision 
    private static String _versionID = "$Revision: 1.20 $";
    
    static private Logger _log4j = Logger.getLogger(PhDigital.class);

    protected byte[] _requestSample = "$1RD\r".getBytes();
    protected byte[] _requestSampleEcho = null;
    
    /** Digital pH sample timeout in milliseconds */
    protected long _PHDIGITAL_SAMPLE_TIMEOUT = 1000;
    protected int _PHDIGITAL_MAX_SAMPLE_TRIES = 3;
    protected int  _PHDIGITAL_MAX_SAMPLE_BYTES = 100;
    protected int  _PHDIGITAL_CURRENT_LIMIT = 1000;
    protected int  _PHDIGITAL_INSTRUMENT_START_DELAY = 4000;
    protected String  _PHDIGITAL_PROMPT_STRING = "\r";
    protected String  _PHDIGITAL_SAMPLE_TERMINATOR = "\r";

    
    /** Digital pH serial baud rate */
    static final int _BAUD_RATE = 38400;

    /** Digital pH serial data bits */
    static final int _DATA_BITS = SerialPort.DATABITS_8;

    /** Digital pH serial stop bits */
    static final int _STOP_BITS = SerialPort.STOPBITS_1;

    /** Digital pH parity checking */
    static final int _PARITY = SerialPort.PARITY_NONE;

    // Configurable digital pH attributes
    protected PhDigitalAttributes _attributes = new PhDigitalAttributes(this);

    protected boolean _readResults[] = new boolean[10];
    protected boolean _lastResult = true;

    public static final double KELVIN_OFFSET_TO_CELSIUS = 273.15;
    public static final double K = 1.98416e-4;

    /** Locally used temperature 
     *  (typically acquired from other service, but defaults to attribute) 
     */
    protected double _temperature;

    /**
     * Zero-arg constructor.
     */
    public PhDigital() throws RemoteException
    {
        super();
    }

    /** Callback for InstrumentDataListener interface, called when the Temperature
    service is registered with the InstrumentRegistry
    */
    public void serviceRegisteredCallback(RegistryEntry entry)
    {
        _log4j.info("serviceRegisteredCallback for Temperature for HRPH-SBE18");
    }

    /** dataCallback from the Temperature device */
    public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields)
    {
	try{
	    PacketParser.Field tempField = PacketParser.getField(fields,"temperature");
	    Number dtemp = ((Number)tempField.getValue());
	    _temperature = dtemp.doubleValue()+KELVIN_OFFSET_TO_CELSIUS;
	}catch(NoDataException e){
	    _log4j.error("NoDataException in dataCallback");
	}
    }
    
    /** Self-test not implemented. */
    public int test() 
    {
        return -1;
    }

    /**
     * Return specifier for default sampling schedule.
     */
    protected ScheduleSpecifier createDefaultSampleSchedule()
    throws ScheduleParseException 
    {

        // Sample every 10 seconds by default
        return new ScheduleSpecifier(10000);
    }
    
    /**required by BaseInstrumentService */
    protected  PowerPolicy initInstrumentPowerPolicy() 
    {
        return PowerPolicy.ALWAYS;
    }
    /**required by BaseInstrumentService */
    protected  PowerPolicy initCommunicationPowerPolicy()
    {
        return PowerPolicy.ALWAYS;
    }
    
    /**required by BaseInstrumentService */
    protected int initMaxSampleBytes() 
    {
        // may have to be changed when metadata is returned
        return _PHDIGITAL_MAX_SAMPLE_BYTES;
    }

    /**required by BaseInstrumentService */
    protected byte[] initPromptString() 
    { 
        return _PHDIGITAL_PROMPT_STRING.getBytes();
    }

    /**required by BaseInstrumentService */
    protected byte[] initSampleTerminator() 
    { 
        return _PHDIGITAL_SAMPLE_TERMINATOR.getBytes();
    }

    /**required by BaseInstrumentService */
    protected int initCurrentLimit()
    { 
        return _PHDIGITAL_CURRENT_LIMIT;  // 1 amp
    }


    /**required by BaseInstrumentService */
    protected int initInstrumentStartDelay() 
    {
        return _PHDIGITAL_INSTRUMENT_START_DELAY;  // 4-second startup
    }

    /**required by DeviceService */
    /**
    * Return parameters to use on serial port.
    */
    public SerialPortParameters getSerialPortParameters()
    throws UnsupportedCommOperationException 
    {
        return new SerialPortParameters(_BAUD_RATE, _DATA_BITS, _PARITY,
                    _STOP_BITS);
    }


    /**
     * Digital pH sensor doesn't have a clock. Default method (in
       BaseInstrumentService) will throw exception.
     */


    /**
     * Initialize the sensor/driver.
     */
    protected void initializeInstrument() throws InitializeException, Exception, RangeException
    {
	for (int i = 0; i < 10; i++)
	    _readResults[i] = true;

        try
        {
            setSampleTimeout(_PHDIGITAL_SAMPLE_TIMEOUT);
            setMaxSampleTries(_PHDIGITAL_MAX_SAMPLE_TRIES);
        }
        catch (RangeException e) 
        {
            _log4j.error("RangeException: ", e);
        }

        try 
        {
            //This sets a default value, in case temperature is not successfully measured
            _temperature = _attributes.temperature;    /* Initialize temp to default */
            
            /** Register us for data callbacks from the temperature device */
            InstrumentRegistry reg = InstrumentRegistry.getInstance();
            
            /** This error generates a warning, rather than an error, because
              * the lookup can be null at this moment, depending on startup sequences. 
              * This is a self-recovering error.
              */
            if(reg.registerDataCallback(this, _attributes.temperatureLookup) == null)
                _log4j.warn("Temperature device not found (should recover gracefully).");

        }
        catch (Exception e) 
        {
            _log4j.error("initializeInstrument() caught Exception", e);
        }

    }
    
    /**
     * Get rid of any cruft before sampling.
     */
    protected void prepareToSample() throws Exception 
    {
        _fromDevice.flush();  // Clear first sample.
    }


    /**
     * Request a sample.  Digital pH sensor doesn't echo commands
     */
    protected void requestSample() throws Exception
    {
        try 
        {
            // Get status/config info...
            _log4j.debug("requestSample(): sending sample request...");
            _toDevice.write(_requestSample);
            _toDevice.flush();
        }
        catch (Exception e) 
        {
            _log4j.error("requestSample() caught Exception", e);
        }
        return;
    }
    

    /** Override readSample() in base class.  Get the original buffer, and
     * create new one that adds the calculated pH, plus the temperature and  
     * correction used for the calculation
     */
     //TODO: synchronized? (isn't in BIS) 
    protected int readSample(byte[] sample) 
    throws TimeoutException, IOException, Exception
    {
        
        _log4j.debug("PhDigital.readSample() starting up...");
        try
        {
            /** Get the raw data and set the RecordType 
             *  Raw data is of the form '*+02618.56<cr><lf>' (last 2 bytes get stripped)
             */
            int origLength = super.readSample(sample);

            String origSample = new String(sample).substring(0,origLength);
            // parse and scale to volts (after dropping the leading '*')
            double volts = Double.parseDouble(origSample.substring(1)) / 1000.0;
  
            double pH = 7.0 
                + (volts - _attributes.offset)/(K * _temperature * _attributes.slope) 
                + _attributes.correction;
            
            PrintfFormat pf = new PrintfFormat(" %.4f");
            String strFinal = origSample 
               + pf.sprintf(pH) 
               + pf.sprintf(_temperature)
               + pf.sprintf(_attributes.correction);

            // We have the string, let's copy it and return
            System.arraycopy(strFinal.getBytes(), 0, sample, 0, strFinal.length());
	    _lastResult = true;
            return(strFinal.length());
        }
        catch (Exception e)
        {
            _log4j.error("PhDigital.readSample() caught Exception", e);
	    _lastResult = false;
	    throw e;
        }
    }


    /** postSample() checks for excessive failed reads, and shuts down the
     * service if the failure count exceeds the maxFails attribute.
     */
    protected void postSample()
    {
	int	numFails = 0;
	for (int i = 9; i > 0; i--)
	{
	    _readResults[i] = _readResults[i-1];
	    if (!_readResults[i])
		numFails++;
	}
	_readResults[0] = _lastResult;
	if (!_lastResult)
	    numFails++;

	if ((numFails >= _attributes.maxFails) && (_attributes.maxFails > 0))
	{
	    _log4j.error("PhDigital exceeded max failure attribute - shutting down");
	    try {
		suspend();
		new ShutdownThread().start();
	    } catch (Exception e) {
	    }
	}
    }

    /** Override resume() to clear failure counter.   */
    public synchronized void resume()
    {
	for (int i = 0; i < 10; i++)	//Clear the error array
	    _readResults[i] = true;

	super.resume();
    }

    class ShutdownThread extends Thread
    {
	public void run()
	{
	    _log4j.info("PhDigital.ShutdownThread running...");
	    try {
		Thread.sleep(10000);
		powerOff();
	    } catch (Exception e) {
	    }
	}
    }


    /** Return a PacketParser. */
    public PacketParser getParser() throws NotSupportedException{
	return new PhDigitalPacketParser(_attributes.registryName);
    }

    /** 
     * Configurable PhDigital service attributes.
     * @author graybeal
     *
     */ 
    public class PhDigitalAttributes extends InstrumentServiceAttributes 
    {
        
        // constructor
        PhDigitalAttributes(DeviceServiceIF service) 
        {
            super(service);
        }
        
        /** Temperature in degrees Kelvin (default outside temp value, 4 deg C)  */
        protected double temperature = 277.15;

        // This registryName must be overridden by the Makefile for each 
        // instance of the driver
        public String registryName = "pH";

        public String temperatureLookup = "CTD1";

	/** Number of failures in last 10 tries that will lead to doing an instrument shutdown.
	 * Set to 11 or higher to turn off the shutdown-on-failure feature
	 */
	public int maxFails = 5;

    /** 'offset', 'slope', and 'correction' are doubles denoting
     * the corresponding values for converting each voltage to pH.
     * The equation in readSample() above uses these attributes.
     */
        protected double offset = 2.5;
        protected double slope =  4.0;
        protected double correction = 0.0;

    }

}

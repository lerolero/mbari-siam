/**
 * @Title Sunburst SAMI-CO2 instrument driver
 * @author Bob Herlien
 * @version 1.0
 * @date 4/30/2009
 *
 * Copyright MBARI 2009
 * @author MBARI
 * @revision $Id: SAMI.java,v 1.7 2009/07/16 15:07:11 headley Exp $
 *
 */

package org.mbari.siam.devices.sunburst;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.DateFormat;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.ScheduleTask;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.StopWatch;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;
import org.mbari.util.NumberUtil;

/**
 * This <code>SAMI</code> class is the <code>InstrumentServices</code> driver
 * for sampling the Sunburst SAMI CO2 sensor.  This driver has limited ability
 * to change the SAMI sampling parameters.  At initialization, or when the
 * system changes its sampling schedule or <code>blank</code> attribute, it
 * will stop the SAMI and restart it with the new schedule and/or blank
 * interval.  In order to do so, it must wait until the <b>next</b> sample time
 * (as defined by the default sampleSchedule), at which time it will issue 
 * ^C to stop it and then reprogram it.  Note that this process can take up to
 * one entire sampleSchedule interval, which can be up to 6 hours.
 *<p>
 * In all other cases, this SAMI driver simply listens passively for an instrument
 * sample every <code>sampleSchedule</code>
 * seconds.  The instrument must be set up externally prior to deployment.
 *
 * @see org.mbari.siam.core.BaseInstrumentService
 * @see org.mbari.siam.core.PolledInstrumentService
 * @see org.mbari.siam.distributed.Instrument
 */

public class SAMI extends PolledInstrumentService  implements Instrument
{
    // Max constants
    static final int MAX_RESPONSE_BYTES = 256;

    static final int MAX_SAMPLE_TRIES = 1; // NO RETRIES - this just receives
					   // unsolicited data
 
    static final int RESPONSE_TIME = 120000; // Takes approx 65 secs to generate sample

    static final int RUNTYPE_TIME = 2000;   // "RUNNING" or "RNBLANK" should come out in < 1 sec
    static final long DEFAULT_SCHEDULE_MS = 1800000;

    /** SensorDataPacket recordType for blank sample	*/
    static final long RECORDTYPE_BLANK = 2;

    static final long BLANK_RESPONSE_TIME = 25 * 60 * 1000;

    //Stuff for getting into Command mode
    static final int MAX_COMMAND_TRIES = 3; // Tries to get into Command mode
    static final int CTRLC = 3;		    // Control-C character
    static final int RUN_CMD = 18;	    // Control-R character
    static final int LF = 10;	   	    // Line Feed
    static final int CR = 13;	   	    // Carriage Return
    static final long GET_PROMPT_TIMEOUT = 2000;
    static final long INTERACTIVE_TIMEOUT = 1000;
    static final String RUNNING = "RUNNING";
    static final String RNBLANK = "RNBLANK";
    static final String RUNNING_ = "RUNNING,";
    static final String RNBLANK_ = "RNBLANK,";

    // Configurable attributes
    public SAMIAttributes _attributes = new SAMIAttributes(this);

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(SAMI.class);

    /** Sample interval seconds derived from default sample schedule */
    protected int _sampleIntervalSec;

    /** boolean to reinitialize the device	*/
    protected boolean	_reinit = false;

    /** TRUE if just finished initializing instrument	*/
    protected boolean	_firstInit = false;

    /** indicates we're currently running a blank sample */
    protected boolean _inBlank = false;

    /** Seconds to next sample		*/
    protected long _nextInterval;

    /** Line buffer for operations */
    protected byte[] _lineBuffer = new byte[512];

    protected byte[] _comma = ",".getBytes();
    protected byte[] _pound = "#".getBytes();
    protected byte[] _colon = ":".getBytes();
    protected byte[] _newline = "\n".getBytes();

    protected DateFormat _timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT);

    protected Calendar _calendar = Calendar.getInstance();

    /**
     * Allocates a new <code>SAMI</code>
     *
     * @throws RemoteException .
     */
    public SAMI() throws RemoteException {
    }

    /**
     * Specify startup delay (millisec)
     */
    protected int initInstrumentStartDelay() {
        return 2000;
    }

    /**
     * Specify prompt string.
     */
    protected byte[] initPromptString() {
        return(_pound);
    }

    /**
     * Specify sample terminator.
     */
    protected byte[] initSampleTerminator() {
        return(_newline);
    }

    /**
     * Specify maximum bytes in raw sample.
     */
    protected int initMaxSampleBytes() {
        return(MAX_RESPONSE_BYTES);
    }

    /**
     * Specify current limit in increments of 120 mA upto 11880 mA.
     */
    protected int initCurrentLimit() {
        return(5000);
    }

    /**
     * Return initial value of instrument power policy.
     */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return(PowerPolicy.NEVER);
    }

    /**
     * Return initial value of communication power policy.
     */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return(PowerPolicy.ALWAYS);
    }

    /**
     * Return specifier for initial sampling schedule.
     */
    protected ScheduleSpecifier createDefaultSampleSchedule()
            throws ScheduleParseException {
        return new ScheduleSpecifier(DEFAULT_SCHEDULE_MS);
    }

    /**
     * Return parameters to use on serial port.
     */
    public SerialPortParameters getSerialPortParameters()
            throws UnsupportedCommOperationException {

        return new SerialPortParameters(19200, SerialPort.DATABITS_8,
                SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
    }

    /**
     * Called by the framework to initialize the instrument prior to sampling.
     * Normally this method will use properties determined by the setProperties
     * method.
     *
     * @throws InitializeException
     * @throws Exception
     */
    protected void initializeInstrument() throws InitializeException, Exception {
        _log4j.info("Initializing SAMI...");

        String response, property;
        String[] token;

        setMaxSampleTries(MAX_SAMPLE_TRIES);
        setSampleTimeout(RESPONSE_TIME);

        // turn on communications/power
        managePowerWake();

	_calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

	getSetSampleInterval();

	// Force instrument initialization
	try {
	    reinitializeInstrument();
	    _firstInit = true;
	    _reinit = false;
	} catch (TimeoutException e) {
	    //If can't initialize, it must be sampling.  Wait for first
	    // sample and then reinitialize
	    _firstInit = false;
	    _reinit = true;
	    _log4j.info("Couldn't get command mode for initialization. Waiting for next sample.");
	}

	managePowerSleep();

        _log4j.info("Initializing completed");

    } /* initializeInstrument() */


    /**
     * This method should be called guardSec seconds before the expected
     * receipt of the sample. If 2*guardSec expires with no sample,
     * method will wait _sampleIntervalSec, and, if a character is available,
     * will sync() the driver.  If no character is still available, it will
     * throw a TimeoutException
     *
     * @throws IOException
     * @throws TimeoutException
     * @throws InterruptedException
     * @throws InvalidDataException
     */
    protected void requestSample() throws IOException, TimeoutException,
					  InterruptedException, InvalidDataException,
					  InitializeException
    {
        long t0 = System.currentTimeMillis();

        _log4j.debug(_timeFormatter.format(new Date()) + " requestSample()");
        _fromDevice.flush();

	// If we just did initializeInstrument and got "RUNNING", skip the waitForSample()
	if (_firstInit)
	{				//Should only happen once after initializeInstrument
	    _log4j.debug("firstInit syncing driver to " + _nextInterval + " seconds from now.");
	    sync(1000*_nextInterval);
	    _firstInit = false;		//Reset boolean for next cycle
	    return;			//Skip waitForSample, since we have "RUNNING"
	}

	// Blank cycle can take a SECOND 25 minute interval to get next real sample
	if (_inBlank)
	{
	    if (!waitForSample(BLANK_RESPONSE_TIME + _sampleIntervalSec))
		throw new TimeoutException("Failed to receive post-blank sample for " +  
					   ((System.currentTimeMillis()-t0)/1000)
					   + " seconds.");
	}
	else if (!waitForSample(2 * _attributes.guardSec))
	{
	    _log4j.warn("requestSample: No character for " + (2 * _attributes.guardSec)
			+ " seconds.  Will wait for " + _sampleIntervalSec + " seconds.");
	    if (!waitForSample(_sampleIntervalSec))
	    {
		throw new TimeoutException("Failed to receive a sample for " +  
					   ((System.currentTimeMillis()-t0)/1000)
					   + " seconds.");
	    }
	}

	int readlen;
	try {
	    readlen = StreamUtils.readUntil(_fromDevice, _lineBuffer, _comma, RUNTYPE_TIME);
	} catch (Exception e) {
	    throw new TimeoutException("Exception in StreamUtils.readUntil: " + e);
	}

	String runType = new String(_lineBuffer, 0, readlen);
	if (runType.indexOf(RNBLANK) >= 0)
	{
	    _inBlank = true;
	    _log4j.debug("requestSample() found \"" + runType + "\"");
	}
	else if (runType.indexOf(RUNNING) >= 0)
	    _inBlank = false;
	else
	    throw new InvalidDataException("Run type not recognized: " + runType);

	_nextInterval = _sampleIntervalSec - _attributes.guardSec;

	if (_reinit)
	{
	    reinitializeInstrument();
	    //reinitializeInstrument will set _nextInterval
	    _reinit = false;
	}

	_log4j.debug("requestSample: sync driver to " + _nextInterval + " seconds from now.");
	sync(1000*_nextInterval);

    } /* requestSample() */


    /**
     * Read raw sample bytes from serial port into buffer, return number of
     * bytes read. Reads characters from serial port until sample terminator
     * string is encountered.
     *
     * @param sample
     *            output buffer
     */
    protected int readSample(byte[] sample) throws TimeoutException,
						   IOException, Exception
    {
	if (_inBlank)
	    return(StreamUtils.readUntil(_fromDevice, sample, getSampleTerminator(),
					 BLANK_RESPONSE_TIME));

	return(super.readSample(sample));

    } /* readSample() */


    /**
     * Process raw sample bytes, return TimeStampedData object. By default, raw
     * sample is encapsulated in TimeStampedData object, and no additional
     * processing is done.
     *
     * @param sample
     *            raw sample
     * @param nBytes
     *            number of bytes in raw sample
     */
    protected SensorDataPacket processSample(byte[] sample, int nBytes)
	throws Exception 
    {
	_log4j.debug("SAMI.processSample() \"" + 
		     new String(sample, 0, nBytes) + "\", _inBlank = " + _inBlank);

	SensorDataPacket pkt = super.processSample(sample, nBytes);

	if (_inBlank)
	{
	    pkt.setRecordType(RECORDTYPE_BLANK);
	    _log4j.info("processSample() setting recordType to " + RECORDTYPE_BLANK);
	}

	return(pkt);

    } /* processSample() */


    /**
     * Shutdown instrument sampling.
     */
    protected String shutdownInstrument()
            throws Exception {

	throw new NotSupportedException();
    }


    /**
     * We override addschedule so we know when to reinitialize the device for
     * the new schedule.
     */
    public int addSchedule(String name, String schedule, boolean overwrite)
    {
	_reinit = true;
	return(super.addSchedule(name, schedule, overwrite));
    }


    /** Return a PacketParser. */
    public PacketParser getParser() throws NotSupportedException
    {
	return new SAMIPacketParser(_attributes.registryName,
				    _attributes.cal_T, _attributes.cal_a, 
				    _attributes.cal_b, _attributes.cal_c);
    }


    /**
     * Local protected method to wait for first character of sample
     *
     * @return true if sample character available, else false
     * @throws InterruptedException if Device status changes from Device.SAMPLING
     */
    protected boolean waitForSample(long waitSeconds) 
	throws IOException, InterruptedException
    {
        // Wait for 1st character yielding in the process
        long startTime = System.currentTimeMillis();

        while (_fromDevice.available() == 0)
	{
            if (getStatus() != Device.SAMPLING)
	    {
                _log4j.warn("requestSample() interrupted");
                throw new InterruptedException("requestSample() interrupted");
            }

            if (((System.currentTimeMillis() - startTime)/1000) > waitSeconds)
		return(false);

	    Thread.sleep(200);
        }

	return(true);

    } /* waitForSample() */


    /**
     * Sends a ^C to the SAMI and wait for a prompt
     *
     * @throws IOException
     * @throws TimeoutException
     */
    protected void enterCommandMode() throws IOException, TimeoutException
    {
	String msg = null;
	int	i;

        for (i = 0; i < MAX_COMMAND_TRIES; i++)
	{
            _log4j.debug("enterCommandMode() - send ^C #" + i);
            _toDevice.write(CTRLC);
	    _toDevice.flush();
	    try {
		StreamUtils.skipUntil(_fromDevice, getPromptString(),
				      GET_PROMPT_TIMEOUT);
		StopWatch.delay(250);
		return;
	    } catch (Exception e) {
		msg = e.getMessage();
	    }

	    // If didn't get prompt from ^C, we may already be in command mode.
	    // Try just sending a carriage return to see if it responds.
	    for (i = 0; i < 2; i++)
	    {
		_toDevice.write(CR);
		_toDevice.flush();
		try {
		    StreamUtils.skipUntil(_fromDevice, getPromptString(),
					  GET_PROMPT_TIMEOUT);
		    StopWatch.delay(250);
		    return;
		} catch (Exception e) {
		    msg = e.getMessage();
		}
	    }

	}
	throw new TimeoutException(msg);

    } /* enterCommandMode() */


    /** Get sample interval closest to that supported by device.  Change default sample schedule
     *  if it doesn't agree with supported interval.  Return new interval.
     */
    protected int getSetSampleInterval() throws InitializeException
    {
        ScheduleTask schedTask = getDefaultSampleSchedule();
        ScheduleSpecifier schedule = schedTask.getScheduleSpecifier();

        //Must check to verify schedule is relative.
        if (!schedule.isRelative()) {
            throw new InitializeException("Schedule MUST be relative!");
        }

        // Interval between samples is specified by sample schedule
        int schedSeconds = (int) schedule.getPeriod() / 1000;
        int schedMinutes = schedSeconds/60;
	int newMinutes = schedMinutes;

	if (schedMinutes < 25)
	    newMinutes = 15;
	else if (schedMinutes < 50)
	    newMinutes = 30;
	else if (schedMinutes < 100)
	    newMinutes = 60;
	else if (schedMinutes < 160)
	    newMinutes = 120;
	else if (schedMinutes < 300)
	    newMinutes = 180;
	else
	    newMinutes = 360;

        _sampleIntervalSec = 60 * newMinutes;

	// If schedule wasn't a supported interval, reset the instrument sample schedule
	if (schedSeconds != (60*newMinutes))
	{
	    try {
		schedTask.setSpecifier(new ScheduleSpecifier(60000L * newMinutes));

		annotate(("Reset instrument schedule to correspond to supported rate.  Was " +
			  schedSeconds + " seconds.  Changed to " + _sampleIntervalSec
			  + " seconds.").getBytes());
	    } catch (ScheduleParseException e) {
		throw new InitializeException("ScheduleParseException in getSetSampleInterval: "
					      + e.getMessage());
	    }
	}

	return(_sampleIntervalSec);

    } /* getSampleInterval() */


    /** Internal method to read one line from instrument */
    protected int readUntilDelayOrNewline(InputStream instream, byte[] buffer, long timeout)
	throws IOException, TimeoutException
    {
        int bytesRead = 0;
        long t0 = System.currentTimeMillis();
	int c;

        // Read until we receive timeout
        while ((System.currentTimeMillis() - t0) <= timeout)
	{
            if (instream.available() > 0)
	    {
		c = instream.read();
		if ((c == CR) || (c == LF))
		    return(bytesRead);
                buffer[bytesRead++] = (byte)c;
                t0 = System.currentTimeMillis();
	    }
            else
		StopWatch.delay(50);
	}

	if (bytesRead == 0)
	    throw new TimeoutException("No chars read");

	return(bytesRead);
    }


    /** Convert integer to ASCII, send it to instrument followed by carriage return.
     *  Then flush instrument stream
     */
    protected void writeDeviceInteger(int val) throws IOException
    {
	_toDevice.write(Integer.toString(val).getBytes());
	_toDevice.write(CR);
	_toDevice.flush();
    }

    protected void writeDebug(int val, String field, String prompt)
    {
	_log4j.debug("Writing " + val + " to field " + field + 
		    " as a result of: " + prompt);
    }

    /**
     * Called from within requestSample() whenever the system has updated a schedule
     * or attribute that causes the need to reinitialize the instrument
     *
     * @throws IOException
     * @throws InitializeException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    protected void reinitializeInstrument() 
	throws IOException, InitializeException, TimeoutException, InterruptedException
    {
	boolean done = false;
	int linelen;
	String  line;
        ScheduleSpecifier schedule = getDefaultSampleSchedule().getScheduleSpecifier();

        _log4j.info(_timeFormatter.format(new Date()) + " Initializing SAMI");

        //Must check to verify schedule is relative.
        if (!schedule.isRelative()) {
            throw new InitializeException("Schedule MUST be relative!");
        }

	enterCommandMode();
	_toDevice.write(RUN_CMD);
	_toDevice.flush();

	// Get Date and Time
	_calendar.setTime(new Date(System.currentTimeMillis()));
	_log4j.debug("_calendar set to " + _calendar);

        // Interval between samples is specified by sample schedule
	int intervalSec = getSetSampleInterval();

	for (int loopcnt = 0; !done && (loopcnt < 30); )
	{
	    linelen = readUntilDelayOrNewline(_fromDevice, _lineBuffer, INTERACTIVE_TIMEOUT);

	    line = (new String(_lineBuffer, 0, linelen)).toUpperCase();

	    if (line.indexOf("DO YOU") >= 0)
	    {
		_toDevice.write("1\r".getBytes());
		_toDevice.flush();
		writeDebug(1,"?", line);
	    }
	    else if (line.indexOf("ENTER") >= 0)
	    {
		/* Careful!!  MEASUREMENT must be before MINUTE, because prompt contains string MINUTE */
		if (line.indexOf("MEASUREMENT") >= 0)
		{
		    writeDeviceInteger(intervalSec/60);
		    writeDebug(intervalSec/60, "INTERVAL", line);
		}
		else if (line.indexOf("YEAR") >= 0)
		{
		    writeDeviceInteger(_calendar.get(Calendar.YEAR));
		    writeDebug(_calendar.get(Calendar.YEAR), "YEAR", line);
		}

		/* Careful!!  DAY must be before MONTH, because actual string is "DAY OF THE MONTH" */
		else if (line.indexOf("DAY OF") >= 0)
		{
		    writeDeviceInteger(_calendar.get(Calendar.DAY_OF_MONTH));
		    writeDebug(_calendar.get(Calendar.DAY_OF_MONTH),
			       "DAY", line);
		}
		else if (line.indexOf("MONTH") >= 0)
		{
		    writeDeviceInteger(_calendar.get(Calendar.MONTH) + 1);
		    writeDebug(_calendar.get(Calendar.MONTH)+1, "MONTH", line);
		}
		else if (line.indexOf("HOUR") >= 0)
		{
		    writeDeviceInteger(_calendar.get(Calendar.HOUR_OF_DAY));
		    writeDebug(_calendar.get(Calendar.HOUR_OF_DAY),
			       "HOUR", line);
		}
		else if (line.indexOf("MINUTE") >= 0)
		{
		    writeDeviceInteger(_calendar.get(Calendar.MINUTE));
		    writeDebug(_calendar.get(Calendar.MINUTE), "MINUTE", line);
		}
		else if (line.indexOf("SECOND") >= 0)
		{
		    writeDeviceInteger(_calendar.get(Calendar.SECOND));
		    writeDebug(_calendar.get(Calendar.SECOND), "SECOND", line);
		}
	    }
	    else if ((line.indexOf("WOULD YOU") >= 0) && (line.indexOf("BLANK") >= 0))
	    {
		if (line.indexOf("WHEN") >= 0)
		{
		    if ((_attributes.blank >= 0) && (_attributes.blank <= 5))
		    {
			writeDeviceInteger(_attributes.blank);
			writeDebug(_attributes.blank, "BLANK", line);
		    }
		    else
			writeDeviceInteger(5);
		}
		else
		{
		    if ((_attributes.blank >= 0) && (_attributes.blank <= 4))
			writeDeviceInteger(1);
		    else
			writeDeviceInteger(2);
		}
	    }
	    else if ((line.indexOf(RUNNING_) >= 0) || (line.indexOf(RNBLANK_) >= 0))
	    {
		done = true;
		_inBlank = (line.indexOf(RNBLANK) >= 0);
		_log4j.debug("reinitializeInstrument got \"" + line +
			     "\", _inBlank = " + _inBlank);
	    }
	}

	if (!done)
	    throw new InitializeException("Too many times through initialize prompts!");

	//Reset 'blank' attribute so next schedule change doesn't run it again
	_attributes.blank = -1;

	//Instrument tries to synchronize to next _sampleIntevalSec on clock
	_nextInterval = (_sampleIntervalSec -
			((System.currentTimeMillis()/1000) % _sampleIntervalSec))
	    		- _attributes.guardSec;

	if (_nextInterval < _attributes.guardSec)
	    _nextInterval += _sampleIntervalSec;

	_log4j.debug("reinitializeInstrument() done, set _nextInterval to " + _nextInterval);

    } /* reinitializeInstrument() */


    public int test() {
        return 0;
    }


    /**
     * Configurable SAMI attributes
     */

    public class SAMIAttributes extends InstrumentServiceAttributes {

        /**
         * Constructor, with required InstrumentService argument
         */
        public SAMIAttributes(DeviceServiceIF service) {
            super(service);
        }

        /**
         * Guard time (seconds) to allow us to catch the serial string.
         */
        int guardSec = 20;

        /**
         * Answer to "When would you like to run a blank?". Valid values:
	 * <ul>
	 * <li> 0 = 12 hours
	 * <li> 1 = 24 hours
	 * <li> 2 = 2 days
	 * <li> 3 = 3 days
	 * <li> 4 = immediately
	 * <li> 5 = no change
	 * <li> -1 (or anything else) = no blank
	 * </ul>
	 * This attribute acts as a "one-shot".  That is, when set to a value between
	 * 0-4, it will reinitialize the system to run the corresponding blank.  It
	 * will then reset the attribute to -1 so that further attribute changes don't
	 * re-run the blank.
         */
        int blank = -1;


	/** Temperature calibration for SAMIPacketParser */
	double cal_T = 4.83;

	/** "a" calibration for SAMIPacketParser */
	double cal_a = -0.01200;

	/** "b" calibration for SAMIPacketParser */
	double cal_b = 0.90102;

	/** "c" calibration for SAMIPacketParser */
	double cal_c = -1.81170;

	/* Throw InvalidPropertyException if any invalid attribute values found
         */
        public void checkValues() throws InvalidPropertyException
	{
	    if ((blank >= 0) && (blank <= 4))
		_reinit = true;

            if (guardSec <= 0)
                throw new InvalidPropertyException(guardSec
                        + " Invalid guardSec." + " Must be > 0");

        } /* checkValues() */

    } /* class SAMIAttributes */

} /* class SAMI */

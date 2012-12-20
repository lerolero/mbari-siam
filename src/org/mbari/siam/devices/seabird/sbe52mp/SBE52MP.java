/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.sbe52mp;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.devices.seabird.base.Seabird;

import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.ScheduleTask;
import org.mbari.siam.utils.StreamUtils;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.StringTokenizer;

/** SBE52MP implements a PolledInstrumentService for a Seabird SBE 52-MP Moored
  Profiler CTD and Optional Dissolved Oxygen Sensor.

  @author Bob Herlien
*/

public class SBE52MP extends Seabird implements Instrument
{
    // CVS revision 
    private static String _versionID = "$Revision: 1.4 $";
    
    static private Logger _log4j = Logger.getLogger(SBE52MP.class);

    static final long _GET_PROMPT_TIMEOUT = 8000;

    /**
     * Command to sample depends on whether sample is logged to instrument
     * FLASH (see initializeInstrument())
     */
    protected final byte[] _requestSamplePumped = "PTS\r".getBytes();
    
    protected final byte[] _requestSampleNotPumped = "TS\r".getBytes();
    
    protected byte[] _getPrompt = "\r".getBytes();

    protected byte[] _requestSample = _requestSamplePumped;
    
    protected byte[] _requestSampleEcho = null;

    protected byte[] _haltSample = "STOPPROFILE\r".getBytes();

    protected byte[] _haltSampleEcho = null;

    protected byte[] _startNow = "STARTPROFILE\r".getBytes();

    protected byte[] _startNowEcho = null;

    protected final String _startPump = "PUMPON\r";

    protected final String _stopPump = "PUMPOFF\r";

    protected boolean _continuousPumping = false;

    static final long DEFAULT_SAMPLE_TIMEOUT = 60000;

    private DevicePacketParser devicePacketParser;


    /**
     * Constructor.
     */
    public SBE52MP() throws RemoteException
    {
	super();
	_attributes = new SBE52Attributes(this);
    }

    /** This is not used in this subclass, but must be defined since
     *  it's an abstract method in Seabird
     */
    protected byte[] getFormatForSummaryCmd()
    {
	return(null);
    }

    /**
     * Return command to get calibrations
     */
    protected byte[] getCalibrationCmd()
    {
	return("DC\r".getBytes());
    }

    /**
     * Return CTD packet parser.
     */
    public PacketParser getParser() {
        return new Seabird52PacketParser(_attributes.registryName);
    }


    /**
     * SBE 52MP doesn't have a clock.
     */
    public void setClock() {
    }


    /**
     * Initialize the SBE 52MP.
     */
    protected void initializeInstrument() throws Exception
    {
        setSampleTimeout(DEFAULT_SAMPLE_TIMEOUT);

        // Stop autonomous logging by instrument (If was in safeMode).
        _log4j.info("initializeInstrument() - Getting SBE's attention (Stops any previous logging).");
        stopAutonomousLogging();

	setPumpMode(true);

        // Turn on DPA power/comms
        managePowerWake();

    }


    /**
     * Set mode of using the pump to either continuous pumping or pumped while sampling,
     * depending on the sample rate.
     *
     * @param forcePumpChange - true to always send the PUMPON or PUMPOFF command,
     *	even if the mode hasn't changed.
     */
    protected void setPumpMode(boolean forcePumpChange) 
    {
	boolean oldMode = _continuousPumping;
	boolean newMode = false;
	ScheduleTask schedTask = getDefaultSampleSchedule();
	ScheduleSpecifier schedule = schedTask.getScheduleSpecifier();

	if (schedule.isRelative())  //If sched not relative, default to pumped while sampling
	{
	    long schedSecs = schedule.getPeriod()/1000;
	    newMode = (schedSecs < ((SBE52Attributes)_attributes).pumpThresholdSecs);
	}

	if ((newMode != oldMode) || forcePumpChange)
	{
	    try {
		String cmd = newMode ? _startPump : _stopPump;
		_log4j.debug("setPumpMode writing " + cmd);
		getPrompt();
		_toDevice.write(cmd.getBytes());
		_toDevice.flush();

	    } catch (Exception e) {
		_log4j.error("Exception in setPumpMode: " + e);
		return;		//Don't set new mode
	    }
	}

	_continuousPumping = newMode;
	_requestSample = newMode ? _requestSampleNotPumped : _requestSamplePumped;
	_log4j.debug("Set continuous pumping mode to " + _continuousPumping);
    }


    /**
     * Request a sample.  SBE52MP doesn't echo commands
     */
    protected void requestSample() {
        try {
            // Verify connection...
            _log4j.debug("requestSample(): looking for prompt...");
            getPrompt();

            // Get status/config info...
            _log4j.debug("requestSample(): sending sample request...");
            _toDevice.write(_requestSample);

        }
        catch (Exception e) {
            _log4j.error("requestSample() caught Exception", e);
        }
        return;
    }


    /**
     * postSample processing.  If in continuous pumping mode, we must
     * turn the pump back on, because %*&*#* Seabird turns it off after "TS"
     */
    protected void postSample()
    {
	super.postSample();
	if (_continuousPumping)
	{
	    _log4j.debug("postSample getting prompt");
	    try {
		Thread.sleep(200);
		getPrompt(3);
		_toDevice.write(_startPump.getBytes());
		_toDevice.flush();

	    } catch (Exception e) {
		_log4j.error("Exception in postSample: " + e);
	    }
	}
    }


    /**
     * Stop instrument sampling.
     * Overrides base class because SBE52 doesn't re-prompt.
     */
    protected void stopAutonomousLogging() throws IOException, Exception,
					 TimeoutException {
        // Verify connection...
        getPrompt();

        // Halt Sampling...
        _log4j.debug("Stop sampling...");
        _toDevice.write(_haltSample);
        _toDevice.flush();

	try {
	    Thread.sleep(2000);
	} catch (Exception e) {
	}

	// Get next prompt
	getPrompt();
    }

    protected void getPrompt() throws TimeoutException, IOException,
				      NullPointerException, Exception {
        _log4j.debug("getPrompt-");
	_fromDevice.flush();
        _toDevice.write(_getPrompt);
	_toDevice.flush();
        StreamUtils.skipUntil(_fromDevice, getPromptString(),
			      _GET_PROMPT_TIMEOUT);
    }

    /**
     * Get device's notion of its state: a Seabird status packet.
     * Overrides base class because the SBE52MP doesn't echo commands.
     */
    protected byte[] getInstrumentStateMetadata() {
	
	// Try to get prompt
	boolean gotPrompt = false;
	int maxTries = 3;
	for (int i = 0; i < maxTries; i++) {
	    try {
		_log4j.debug("getInstrumentStateMetadata() - get prompt");
		getPrompt();
		_log4j.debug("getInstrumentStateMetadata() - got the prompt");
		gotPrompt = true;
		break;
	    }
	    catch (Exception e) {
		// Try again?
		_log4j.debug("getInstrumentStateMetadata() - didn't get prompt");
	    }
	}

	if (!gotPrompt) {
	    // Couldn't get instrument prompt
	    return "Couldn't get prompt".getBytes();
	}

	try {

            // Get status/config info...
            _log4j.debug("Requesting Status Info...");
            _toDevice.write(_getStatusInfo);
            _toDevice.flush();

            // Skip command echo...
	    // StreamUtils.skipUntil(_fromDevice, _getStatusInfo,
	    //		  _GET_PROMPT_TIMEOUT);

            byte[] statusBuf = new byte[_maxStatusBytes];

            int statusBytes =
		StreamUtils.readUntil(_fromDevice, statusBuf,
				      getPromptString(), getSampleTimeout());

            // Get cal info...
            _log4j.debug("Requesting Cal Info...");
            _toDevice.write(getCalibrationCmd());

            // Skip command echo...
            // StreamUtils
	    //	.skipUntil(_fromDevice, getCalibrationCmd(),
	    //		_GET_PROMPT_TIMEOUT);

            byte[] calBuf = new byte[_maxCalBytes];
            int calBytes =
		StreamUtils.readUntil(_fromDevice, calBuf,
				      getPromptString(), getSampleTimeout());

            // copy the sample into the exactly-right-sized byte array
            byte[] stateBuf = new byte[statusBytes + calBytes];
            System.arraycopy(statusBuf, 0, stateBuf, 0, statusBytes);
            System.arraycopy(calBuf, 0, stateBuf, statusBytes, calBytes);

            return stateBuf;

        }
        catch (Exception e) {
            String err = "Caught Exception while reading status/calibration data: "
		+ e.getMessage();

            _log4j.error(err, e);
            return err.getBytes();
        }
    }

    /**
     * We override addschedule so we know when to reinitialize the device for
     * sample mode.
     */
    public int addSchedule(String name, String schedule, boolean overwrite)
    {
	int retval = super.addSchedule(name, schedule, overwrite);
	setPumpMode(false);
	return(retval);
    }

    protected class SBE52Attributes extends Seabird.Attributes
    {
	public int pumpThresholdSecs = 60;

	public SBE52Attributes(DeviceServiceIF service)
	{
	    super(service);
	}
    }


    /** Parse the CTD data into a CTDData struct	*/
    public Object parseDataPacket(SensorDataPacket pkt) throws InvalidDataException
    {
	try{
	    PacketParser parser=getParser();
	    // return PacketParser.Field[]
	    return parser.parseFields(pkt);
	}catch(NotSupportedException e){
	    _log4j.error(e.toString());
	}catch(ParseException p){
	    throw new InvalidDataException("ParseException caught: "+p.toString());
	}
	return null;
	/*
	String dataString = new String(pkt.dataBuffer());
	StringTokenizer st = new StringTokenizer(dataString, ",");
	CTDData parsedData = new CTDData();

	try {
	    parsedData._conductivity = Double.parseDouble(st.nextToken());
	    parsedData._temperature = Double.parseDouble(st.nextToken());
	} catch (Exception e) {
	    String err = "Can't parse CTD record: " + dataString;
	    _log4j.error(err);
	    throw new InvalidDataException(err);
	}

	parsedData._hasPressure = false;
	parsedData._hasOxygen = false;

	try {
	    parsedData._pressure = Double.parseDouble(st.nextToken());
	    parsedData._hasPressure = true;
	    parsedData._oxygen = Double.parseDouble(st.nextToken());
	    parsedData._hasOxygen = true;
	} catch (Exception e) {
	}

	return(parsedData);
	*/
    }


    public org.mbari.siam.distributed.DevicePacketParser getDevicePacketParser() throws NotSupportedException {
        if (devicePacketParser == null) {
            devicePacketParser = new DevicePacketParser();
        }
        return devicePacketParser;    //To change body of overridden methods use File | Settings | File Templates.
    }

    public class DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser {
        public static final String TEMPERATURE = "temperature";
        public static final String CONDUCTIVITY = "conductivity";
        public static final String PRESSURE = "Pressure";
        public static final String OXYGEN = "oxygen";

        protected void parseFields(DevicePacket packet) throws NotSupportedException, Exception {
            if (packet instanceof SensorDataPacket) {

                /*
                 * We're parsing Temperature and Conductivity and ignoring the other fields for now.
                 *
                 * TODO this assumes OUTPUTFORMAT=3 (SBE-16) or FORMAT=1(SBE37); we may need to support other formats too.
                 */

                final SensorDataPacket p = (SensorDataPacket) packet;
                final String data = new String(p.dataBuffer());
                final StringTokenizer tokenizer = new StringTokenizer(data, ",");
                final Float conductivity = new Float(tokenizer.nextToken());
                final Float temperature = new Float(tokenizer.nextToken());
                final Float pressure = new Float(tokenizer.nextToken());

                addMeasurement(CONDUCTIVITY, "Conductivity", "S/m", conductivity);
                addMeasurement(TEMPERATURE, "Temperature", "C [ITS-90]", temperature);
                addMeasurement(PRESSURE, "Pressure", "decibars", pressure);

		if (tokenizer.hasMoreTokens())
		{
		    final Float oxygen = new Float(tokenizer.nextToken());
		    addMeasurement(OXYGEN, "Oxygen", "ml/l", oxygen);
		}
            }
        }

    } /* class DevicePacketParser */

} /* class SBE52MP */

/**
 * @Title RD Instruments Workhorse ADCP instrument driver
 * @author Martyn Griffiths
 * @version 1.0
 * @date 7/28/2003
 *
 * Copyright MBARI 2003
 * @author MBARI
 * @revision $Id: WorkhorseADCP.java,v 1.7 2010/09/21 23:47:33 salamy Exp $
 *
 */

package org.mbari.siam.devices.workhorse;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.StringTokenizer;
import java.text.ParseException;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.ScheduleTask;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;
import org.mbari.util.NumberUtil;

/**
 * The <code>WorkhorseADCP</code> class represents the
 * <code>InstrumentServices</code> driver for controlling the RDI Workhorse
 * ADCP. WorkhorseADCP is sub-classed from <CODE>AggregateInstrumentService
 * <CODE>. The primary responsibilities of this class is to:-
 * <p/>
 * <BLOCKQUOTE>
 * <p/>
 * <PRE>
 * <p/>
 * Capture and verify sample data from the instrument.
 * <p/>
 * </PRE>
 * <p/>
 * </BLOCKQUOTE>
 *
 * @see org.mbari.siam.distributed.Device
 * @see org.mbari.siam.distributed.Instrument
 * @see org.mbari.siam.distributed.PowerPort
 * @see org.mbari.siam.core.AggregateInstrumentService
 */

public class WorkhorseADCP extends PolledInstrumentService
        implements Instrument, Safeable {

    // Max constants
    static final int MAX_RESPONSE_BYTES = 4096; // Doubled for 10s samples on MTM4 Axis Bin.

    static final int MAX_SAMPLE_TRIES = 1; // NO RETRIES - this just receives

    // unsolicited data

    static final int MAX_COMMAND_TRIES = 3; // In command mode we need to be

    // more rigourous

    static final int MAX_REQUEST_BYTES = 2048; // Max number of bytes sent back

    // from a request

    static final int MAX_PROMPT_TRIES = 3; // max times to send CR in

    // enterCommandMode

    static final int MAX_BREAK_TRIES = 2; // max times to send BREAK in

    // enterCommandMode

    //static final int RESPONSE_TIME = 1000; // This needs to be longer than
    // the longest likely intercharacter delay
    static final int RESPONSE_TIME = 5000; // This needs to be longer than the

    // longest time to read a response
    // (MAX_REQUEST_BYTES) from
    // instrument

    static final int PROMPT_RESPONSE_TIME = 3000;

    static final int ECHO_RESPONSE_TIME = 2000;

    static final int BREAK_FOR_400MS = 400 * 250;

    // Commands
    static final String COMMAND_FORCE_PROMPT = ""; // "\r" implied

    static final String COMMAND_ENSEMBLE_INTERVAL = "TE?";

    static final String COMMAND_PING_INTERVAL = "TP?";

    static final String COMMAND_PINGS_PER_ENSEMBLE = "WP?";

    static final String COMMAND_START_PINGING = "CS";

    static final String COMMAND_SET_RTC = "TS";

    static final String COMMAND_SET_FIRST_PING = "TF";

    static final String COMMAND_POWER_DOWN = "CZ";

    // **********************************//
    // ****** SAFE MODE PARAMETERS ******//
    // **********************************//
    static final String COMMAND_SET_CF_SAFE = "CF=11101";

    static final String COMMAND_SET_CF_NORMAL = "CF=11110";
    static final String COMMAND_SET_CF_LOGGING = "CF=11111";

    static final String COMMAND_SET_EX = "EX=";
    static final String COMMAND_DEFAULT_EX = "11010";

    static final String COMMAND_SAVE_PARAMETERS = "CK";


    // Commands for acquiring instrument metadata
    //static final String COMMAND_GET_DEPLOYMENT_PARAMS = "DEPLOY?\r\n\r\n";
    static final String COMMAND_GET_DEPLOYMENT_WD = "WD?";

    static final String COMMAND_GET_DEPLOYMENT_WF = "WF?";

    static final String COMMAND_GET_DEPLOYMENT_WN = "WN?";

    static final String COMMAND_GET_DEPLOYMENT_WP = "WP?";

    static final String COMMAND_GET_DEPLOYMENT_WS = "WS?";

    static final String COMMAND_GET_DEPLOYMENT_WV = "WV?";

    static final String COMMAND_GET_DEPLOYMENT_TE = "TE?";

    static final String COMMAND_GET_DEPLOYMENT_TP = "TP?";

    static final String COMMAND_GET_DEPLOYMENT_TS = "TS?";

    static final String COMMAND_GET_DEPLOYMENT_EA = "EA?";

    static final String COMMAND_GET_DEPLOYMENT_EB = "EB?";

    static final String COMMAND_GET_DEPLOYMENT_ED = "ED?";

    static final String COMMAND_GET_DEPLOYMENT_ES = "ES?";

    static final String COMMAND_GET_DEPLOYMENT_EX = "EX?";

    static final String COMMAND_GET_DEPLOYMENT_EZ = "EZ?";

    static final String COMMAND_GET_DEPLOYMENT_CF = "CF?";

    static final String COMMAND_GET_SYSTEM_FEATURES = "OL";

    static final String COMMAND_GET_SYSTEM_SERIAL_CONFIG = "CB?";

    static final String COMMAND_GET_SYSTEM_CONFIG = "PS0";

    static final String COMMAND_GET_SYSTEM_TRANSFORM_MATRIX = "PS3";

    static final String COMMAND_GET_SYSTEM_RECORDER_SPACE = "RF";

    static final String COMMAND_GET_SYSTEM_COMPASS_CAL = "AC";

    static final String COMMAND_GET_DEPLOYMENTS_RECORDED = "RA?";

    static final String COMMAND_GET_RECORDER_FILE_DIR = "RR?";

    static final String COMMAND_GET_RECORDER_FREE_SPACE = "RF?";


    // Responses
    static final String RESPONSE_PROMPT = ">";

    // Others
    static final byte HEADER_ID = 0x7f; // Header ID - Table D1 Workhorse manual

    static final byte SOURCE_ID = 0x7f; // Workhorse ID

    // Configurable Workhorse attributes
    public Attributes _attributes = new Attributes(this);

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(WorkhorseADCP.class);

    protected int _ensembleIntervalSec;


    protected long _initialInterval;

    protected DevicePacketParser devicePacketParser;

    protected boolean _safed = false;

    protected PD0DataStructure _pd0Struct = new WorkhorseADCP.PD0DataStructure();

    /**
     * Allocates a new <code>WorkhorseADCP</code>
     *
     * @throws RemoteException .
     */
    public WorkhorseADCP() throws RemoteException {
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
        return RESPONSE_PROMPT.getBytes();
    }

    /**
     * Specify sample terminator.
     */
    protected byte[] initSampleTerminator() {
        return new byte[0]; // none
    }

    /**
     * Specify maximum bytes in raw sample.
     */
    protected int initMaxSampleBytes() {
        return MAX_RESPONSE_BYTES;
    }

    /**
     * Specify current limit in increments of 120 mA upto 11880 mA.
     */
    protected int initCurrentLimit() {
        return 1000; //!! to do
    }

    /**
     * Return initial value of instrument power policy.
     */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.ALWAYS;
    }

    /**
     * Return initial value of communication power policy.
     */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }

    /**
     * Return specifier for initial sampling schedule.
     */
    protected ScheduleSpecifier createDefaultSampleSchedule()
            throws ScheduleParseException {
        // Five minute default interval
        return new ScheduleSpecifier(300);
    }

    /**
     * Return parameters to use on serial port.
     */
    public SerialPortParameters getSerialPortParameters()
            throws UnsupportedCommOperationException {

        return new SerialPortParameters(9600, SerialPort.DATABITS_8,
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
        _log4j.info("Initializing Workhorse...");

        String response, property;
        String[] token;

        setMaxSampleTries(MAX_SAMPLE_TRIES);
        setSampleTimeout(RESPONSE_TIME);

        // turn on communications/power
        managePowerWake();

        ScheduleSpecifier schedule = getDefaultSampleSchedule().getScheduleSpecifier();

        //Must check to verify schedule is relative.
        if (!schedule.isRelative()) {
            throw new InitializeException("Schedule MUST be relative!");
        }

        // Interval between ensembles is specified by sample schedule
        _ensembleIntervalSec = (int) schedule.getPeriod() / 1000;

        try {
            enterCommandMode();

            // First Determine whether the ADCP should be logging internally at start.
            _log4j.debug("initializeInstrument() - Reviewing Workhorse logging attribute.");
            if (!_attributes.internalLog) {
                // DEFAULT setting is set to NOT log internally. CF=11110
                _log4j.info("initializeInstrument() - Setting Workhorse to NOT log internally.");
                sendCommand(COMMAND_SET_CF_NORMAL);
            }
            else {
                // Otherwise, set to log internally.
                _log4j.info("initializeInstrument() - Setting Workhorse to log internally.");
                sendCommand(COMMAND_SET_CF_LOGGING);
            }

            _log4j.info("initializeInstrument() - Setting Workhorse Coordinate system to " +
			_attributes.exCommand);
            sendCommand(COMMAND_SET_EX + _attributes.exCommand);

            // Set TE - Sample Interval (seconds). Default is 300s.
            _log4j.info("initializeInstrument() - Setting Workhorse normal sample interval (TE).");
            int hrs = _ensembleIntervalSec / 3600;
            int rem = _ensembleIntervalSec % 3600;
            int min = rem / 60;
            int sec = rem % 60;
            sendCommand("TE" + hrs + ":" + min + ":" + sec + ".0");

            ////////////////////////////////////////////////
            // !! NOTE: Be careful.  Changing WP and TP may affect the value of TE.  The following
            // situation MUST be met in order to have TE set to users value, otherwise the system
            // will automatically adjust the value of TE.
            // WP x TP > TE;     This means that the product of WP and TP MUST NOT exceed the desired
            // value of TE or the system will automatically adjust TE for you.
            ////////////////////////////////////////////////

            // Set WP - Pings Per Ensemble. Default is 60.
            _log4j.info("initializeInstrument() - Setting Workhorse normal Pings Per EnSemble (WP).");
            sendCommand("WP" + _attributes.pingsPerEnsemble);

            // Set TP - Time Per Ping (seconds). Default is 1s.
            _log4j.info("initializeInstrument() - Setting Workhorse normal Time Per Ping (TP).");
	    if (_attributes.secPerPing > 0) {
		sendCommand("TP0:" + _attributes.secPerPing + ".0");
	    } else if (_attributes.msecPerPing > 0) {
		String cmd = "TP00:" + _attributes.msecPerPing/1000 + "." +
		    new PrintfFormat("%02d").sprintf((_attributes.msecPerPing%1000)/10);
		_log4j.debug("Sending " + cmd);
		sendCommand(cmd);
	    }

            // Save this change to the configuration
            _log4j.info("initializeInstrument() - Saving Workhorse Parameters.");
            sendCommand(COMMAND_SAVE_PARAMETERS);

            // TE = 00:00:00.00 --------- Time per Ensemble
            // (hrs:min:sec.sec/100)
            response = sendRequest(COMMAND_ENSEMBLE_INTERVAL);
            property = getProperty(response);   // Should read attribute set value TE. 
            token = parseProperty(property, ":.", 3);

            // TP = 00:01.00 ------------ Time per Ping (min:sec.sec/100)
            response = sendRequest(COMMAND_PING_INTERVAL);
            property = getProperty(response); // Should read attribute set value TP.
            token = parseProperty(property, ":.", 2);

            // WP = 00180 --------------- Pings per Ensemble (0-16384)
            response = sendRequest(COMMAND_PINGS_PER_ENSEMBLE);
            property = getProperty(response); // Should read attribute set value WP.

            if (_attributes.timeSynch) {
                sendCommand(COMMAND_SET_RTC + AsciiTime.getDate("YY/MM/DD")
                        + "," + AsciiTime.getTime("HH:mm:ss"));
            }


        }
        catch (NumberFormatException e) {
            throw new InitializeException("parseInt()" + e);
        }
        catch (InvalidPropertyException e) {
            throw new InitializeException("getProperty()" + e);
        }
        catch (Exception e) {
            throw new InitializeException("sendRequest() " + e);
        }
        finally {
            exitCommandMode();
            // turn off communications/power
            managePowerSleep();
        }

        _log4j.info("Initializing completed");
    }


    /**
     * This method should be called _preemptionSec seconds before the expected
     * receipt of the sample. If a sample fails to be be detected for a period
     * of 2 * _preemptionSec then a timeout exception will be called. If a
     * sample is detected the method reschedules the service for
     * _ensembleIntervalSec-_preemptionSec returning control to the framework
     * to continue reading the sample in the normal way.
     *
     * @throws TimeoutException thrown if no data is detected for a period of twice the
     *                          preemptionSec
     * @throws Exception        not thrown
     */
    protected void requestSample() throws TimeoutException, Exception {

        _log4j.debug("requestSample()");
        _fromDevice.flush();

        // Wait for 1st character yielding in the process
        long t0 = System.currentTimeMillis();
        long remainingSecs = 0;
	long maxWaitSeconds = _attributes.preemptionSec * 2;

//        long maxWaitSeconds = (_attributes.secPerPing *
//                _attributes.pingsPerEnsemble +
//                _attributes.preemptionSec * 2);
	/* Changed 15may2008, rah, for addition of msecPerPing */
	if (_attributes.secPerPing > 0) {
	    maxWaitSeconds += _attributes.secPerPing * _attributes.pingsPerEnsemble;
	} else if (_attributes.msecPerPing > 0) {
	    maxWaitSeconds += ((_attributes.msecPerPing * _attributes.pingsPerEnsemble)+999)/1000;
	}

        long elapsedSeconds = ((System.currentTimeMillis() - t0) / 1000);
        long newInterval = 0L;
        while (_fromDevice.available() == 0) {
            if (getStatus() != Device.SAMPLING) {
                _log4j.warn("requestSample() interrupted");
                throw new InterruptedException("requestSample() interrupted");
            }

            elapsedSeconds = ((System.currentTimeMillis() - t0) / 1000);
            remainingSecs = maxWaitSeconds - elapsedSeconds;
            //_log4j.debug("Waiting for ADCP data..." + remainingSecs + "
            // \r");
            Thread.sleep(100);
            // if((System.currentTimeMillis()-t0) >
            // _attributes.preemptionSec*1000*2)
            if (elapsedSeconds > maxWaitSeconds) {
                // Resync Workhorse
                // Is it always OK to just resync immediately? see comment 2
                // below
                // NOTES (klh):
                // 1) Every time you go from to deployment mode to command mode
                // and back again,
                // another data file is created if data logging is turned on.
                // There are a finite number of data files that may be created
                // (512), due to the DOS filenaming system
                // used in the ADCP. Once no more files can be created, it is no
                // longer possible to sample.
                // It is possible to append to a data file, but this service
                // doesn't currently do that.
                //
                // 2) This way of resynching won't work correctly if absolute
                // time of day or time relative to
                // top of hour is relevant; it just restarts sampling
                // immediately. An alternative is to wait for a complete
                // cycle to pass (do a "long-sync", perhaps doing an immediate
                // resync after some number of long-sync failures

                enterCommandMode();// This resets time of first ping, causing
                // the ADCP to ping as soon as it leaves
                // command mode
                exitCommandMode();

                // Reschedule for ping time
                _log4j.error("requestSample: Rescheduling service for "
                        + _initialInterval + " seconds...");
                if (_initialInterval < 0L) {
                    _log4j
                            .error("requestSample: schedule interval < 0; PingInterval*PingsPerEnsemble should be >= preemptionSec(gaurd interval)= "
                                    + _attributes.preemptionSec);
                }
                newInterval = _initialInterval * 1000;

                if (newInterval < 0L) {
                    throw new Exception("newInterval is invalid (" + newInterval + "; must be >0. Make sure preemptionSec<ensembleInterval)");
                }

                sync(newInterval);

                // if we don't replace the ScheduleSpecifier, the period will be
                // wrong,
                // causing the timeremaining to return the wrong value
                _log4j.debug("requestSample: post-resync reset schedulespecifier interval="
                        + newInterval);

                getDefaultSampleSchedule().setSpecifier(
                        new ScheduleSpecifier(newInterval));

                throw new TimeoutException(
                        "Failed to receive a sample during preempted interval - resynched");
            }
        }
        _log4j.debug("requestSample: Data detected after " + remainingSecs
                + " secs, rescheduling for "
                + (_ensembleIntervalSec - _attributes.preemptionSec)
                + " seconds.");

        // Reschedule for next wake up
        if ((_ensembleIntervalSec - _attributes.preemptionSec) < 0L) {
            _log4j.error("requestSample: schedule interval < 0; " +
                    "EnsembleIntervalTime should be >= preemptionSec(gaurd interval)= " +
                    _attributes.preemptionSec);
        }

        newInterval = (_ensembleIntervalSec - _attributes.preemptionSec) * 1000;

        if (newInterval < 0L) {
            throw new Exception("newInterval is invalid (" + newInterval + "; must be >0. Make sure preemptionSec<ensembleInterval)");
        }

        sync(newInterval);

        // if we don't replace the ScheduleSpecifier, the period will be wrong,
        // causing the timeremaining to return the wrong value
        _log4j.debug("requestSample: normal reset schedulespecifier interval="
                + newInterval);
        getDefaultSampleSchedule().setSpecifier(
                new ScheduleSpecifier(newInterval));
        _log4j.debug("requestSample() - return");
    }

    /**
     * Called by the framework to fetch the sample data returned from the
     * instrument and copy to sample buffer. The packet is examined to retrieve
     * the length from the header. This is used to determine how many further
     * bytes to read.
     *
     * @param sample
     * @return sample size (bytes)
     * @throws TimeoutException sample time exceeded
     * @throws IOException      error in input stream
     * @throws Exception        Bad header id (should really be in validateSample(..))
     *                          Packet exceeded packet length indicator (should really be
     *                          in validateSample(..))
     */
    protected int readSample(byte[] sample) throws TimeoutException,
            IOException, Exception {

        int byteCount = readUntilDelay(_fromDevice, sample, getSampleTimeout());

        _log4j.debug("Data captured");
        return byteCount;
    }

    /**
     * Read characters from input stream into buffer and returns the number of
     * bytes read.
     *
     * @param instream stream to read from
     * @param outbuf   buffer to place read data into
     * @param timeout  the time it takes to determine if a short packet has been sent
     * @return bytes read
     * @throws TimeoutException short packet sent
     * @throws IOException
     * @throws Exception        Bad header id (should really be in validateSample(..))
     *                          Packet exceeded packet length indicator (should really be
     *                          in validateSample(..))
     */
    protected int readUntilDelay(InputStream instream, byte[] outbuf,
                                 long timeout) throws TimeoutException, IOException, Exception {

        int bytesRead = 0;
        int pktLength = 0;

        _log4j.debug("readUntilDelay()");

        long t0 = System.currentTimeMillis();

        // Read until we receive timeout
        while (true) {

            if (getStatus() != Device.SAMPLING) {
                _log4j.warn("readUntilDelay() interrupted");
                throw new InterruptedException("readUntilDelay() interrupted");
            }

            ///////////////////////////////////////////////////////////
            // Read one byte at a time - this might be VERY
            // inefficient, depending on whether input is buffered!
            //
            if (instream.available() > 0) {

                outbuf[bytesRead++] = (byte) instream.read();

                t0 = System.currentTimeMillis();

                if (bytesRead == 2) { // We now have header and data source id
                    if (outbuf[0] != HEADER_ID) {
                        _log4j.debug("readUntilDelay() - bad header id");
                        throw new Exception("Bad header id");
                    }
                    if (outbuf[1] != SOURCE_ID) {
                        _log4j.debug("readUntilDelay() - bad id");
                        throw new Exception("Bad source id");
                    }
                }
                else if (bytesRead == 4) { // We now have packet size bytes
                    pktLength = mkInt(outbuf[2], outbuf[3]) + 2;// 2 for the
                    // first 2 bytes
                    // not included
                    // in size bytes
                }
                else if (bytesRead > 4 && bytesRead == pktLength) {
                    _log4j.debug("readUntilDelay() - return #1");
                    return bytesRead;
                }

                // if end of buffer, then packet too long

                if (bytesRead > outbuf.length) {
                    _log4j.debug("readUntilDelay() - throw exception");
                    throw new Exception("Output buf in instrument driver "
                            + "exceeded (buffer max length is " + outbuf.length
                            + ")");
                }
            }
            else {
                //Thread.yield();
                Thread.sleep(50);
            }

            long elapsed = System.currentTimeMillis() - t0;

            if (elapsed > timeout) {
                _log4j.debug("readUntilDelay() - throw timeout");
                throw new TimeoutException("Timed out");
            }
        }
    }

    /**
     * Validates the sampled data by summing nBytes-2 bytes modulo 0x10000 and
     * comparing this with the last two bytes of the packet.
     *
     * @param buffer sample data
     * @param nBytes Length of sample data
     * @throws InvalidDataException checksum failed
     */

    protected void validateSample(byte[] buffer, int nBytes)
            throws InvalidDataException {

        int sum = 0;
        int i;

        // Check checksum
        for (i = 0; i < nBytes - 2; i++) {
            sum += mkInt(buffer[i], (byte) 0);
        }
        sum %= 0x10000;
        // fetch last 2 bytes and convert to little endian integer
        int checksum = mkInt(buffer[i], buffer[i + 1]);

        if (sum != checksum) {
            throw new InvalidDataException("Checksum error");
        }
    }

    /** Return a PacketParser. */
    public PacketParser getParser() throws NotSupportedException{
	return new WorkhorsePacketParser(_attributes.registryName);
    }
    
    /** Parse the ADCP data into a Velocity object	*/
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
	double x, y, z;
	int[] vel;

	byte[] data = pkt.dataBuffer();

	if ((data[0] != PD0DataStructure.ID_HEADER) || (data[1] != PD0DataStructure.ID_DATA_SOURCE))
	    throw new InvalidDataException("Packet not a recognized Workhorse format");

	_pd0Struct.setData(data);

	// ADCP velocitys are integer mm/s.  Velocity is double cm/s 
	vel = _pd0Struct.getVelocity(1);
	x = 0.1 * vel[0];

	vel = _pd0Struct.getVelocity(2);
	y = 0.1 * vel[0];

	vel = _pd0Struct.getVelocity(3);
	z = 0.1 * vel[0];

	return(new Velocity(x,y,z));
	*/
    }


    /**
     * Not implemented
     *
     * @return N/A
     */
    protected byte[] getInstrumentStateMetadata() {

        // This method should return any available configuration
        // information that is available from the instrument

        // find out how long until the next sample (if the schedule exists)
        ScheduleTask st = getDefaultSampleSchedule();

        // suspend the schedule
        if (st != null) {
            st.suspend();
        }

        String response = "Metadata Unavailable";

        // if there is enough time (or no default schedule), do this operation
        try {
            enterCommandMode();
            response = "ADCP metadata:";
            // Get DEPLOYMENT parameters
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_WD).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_WF).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_WN).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_WP).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_WS).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_WV).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_TE).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_TP).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_TS).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_EA).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_EB).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_ED).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_ES).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_EX).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_EZ).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_DEPLOYMENT_CF).trim();
            Thread.sleep(250);
            response += "\nNumber of deployments recorded: " + sendRequest(COMMAND_GET_DEPLOYMENTS_RECORDED).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_RECORDER_FILE_DIR).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_RECORDER_FREE_SPACE).trim();

            // Get SYSTEM parameters
            Thread.sleep(250);
            response += "\n"
                    + sendRequest(COMMAND_GET_SYSTEM_SERIAL_CONFIG).trim();
            Thread.sleep(250);
            response += "\n" + sendRequest(COMMAND_GET_SYSTEM_CONFIG).trim();
            Thread.sleep(250);
            response += "\n"
                    + sendRequest(COMMAND_GET_SYSTEM_TRANSFORM_MATRIX).trim();
            Thread.sleep(250);
            response += "\n"
                    + sendRequest(COMMAND_GET_SYSTEM_RECORDER_SPACE).trim();
            Thread.sleep(250);
            response += "\n"
                    + sendRequest(COMMAND_GET_SYSTEM_COMPASS_CAL).trim();
            _log4j.debug("getInstrumentMetadata: metadata= " + response);

            // put back in deployment mode (will ping now if TimeToFirstPing
            // (TF) is not set)
            // The ScheduleTask should still be running, so no need to sync that
            _log4j.debug("getInstrumentMetadata: return to sampling ");

        }
        catch (Exception e) {
            _log4j.error("getInstrumentMetadata: Exception - " + e, e);
            response += "Error acquiring instrument metadata";
        }
        finally {
            try {
                exitCommandMode();
            }
            catch (Exception i) {
                _log4j.error("getInstrumentMetadata: could not return to command mode", i);
            }

            if (st != null) {
                st.resume();
            }

            // Reschedule for ping time
            _log4j.error("getInstrumentMetadata: Rescheduling service for "
                    + _initialInterval + " seconds...");

            if (_initialInterval <= 0L) {
                _log4j
                        .error("getInstrumentMetadata: invalid _initialInterval "
                                + _initialInterval);
            }
            else {
                long newInterval = _initialInterval * 1000;
                sync(newInterval);
                // if we don't replace the ScheduleSpecifier, the period will be
                // wrong,
                // causing the timeremaining to return the wrong value
                _log4j
                        .debug("getInstrumentMetadata: post-resync reset schedulespecifier interval="
                                + newInterval);
                try {
                    getDefaultSampleSchedule().setSpecifier(
                            new ScheduleSpecifier(newInterval));
                }
                catch (ScheduleParseException s) {
                    _log4j
                            .error("getInstrumentMetadata: parse exception caught while rescheduling ");
                    s.printStackTrace();
                }
            }
        }
        return response.trim().getBytes();

    }//end getInstrumentMetadata()

    /**
     * Not implemented. Samples are locally timestamped
     *
     * @param t
     */
    public void setClock(long t) {
        // This method must take the instrument out of sample mode
        // This could cause the service to become out of sync with
        // the instrument if it is sampling, unless the sampling
        // method(s) are synchronized
        // Maybe this method should also be synchronized...

        // stop sampling; enter command mode
        //enterCommandMode();

        // set the time
        //sendCommand(COMMAND_SET_RTC + AsciiTime.getDate("YY/MM/DD") + "," +
        // AsciiTime.getTime("HH:mm:ss") );

        // resume sampling
        //exitCommandMode();

        return;
    }


    /**
     * Enter mode for resource-restricted environment.
     */
    public synchronized void enterSafeMode() throws Exception {

        managePowerWake();

        _log4j.info("enterSafeMode() - Setting Workhorse Instrument to SAFE mode.");

        //Sends a break and gets to a prompt
        _log4j.info("enterSafeMode() - Getting Workhorse's attention.");
        enterCommandMode();

        // Send command to make change to set internal logging.  CF = 11101
        _log4j.info("enterSafeMode() - Setting Workhorse to log internally for SAFE mode.");
        sendCommand(COMMAND_SET_CF_SAFE);

        _log4j.info("enterSafeMode() - Setting Workhorse to Earth Coordinates (EX=11010).");
        sendCommand(COMMAND_SET_EX + COMMAND_DEFAULT_EX); // Always set Earth Coordinates in case of batt. failure.

        // Set TE - Sample Interval (seconds). Default is 300s.
        _log4j.info("enterSafeMode() - Setting Workhorse SAFE sample interval (TE).");
        int hrs = _attributes.safeSampleIntervalSec / 3600;
        int rem = _attributes.safeSampleIntervalSec % 3600;
        int min = rem / 60;
        int sec = rem % 60;
        String interval = ("TE" + hrs + ":" + min + ":" + sec + ".0");
        _toDevice.write(mkCmd(interval));

        ////////////////////////////////////////////////
        // !! NOTE: Be careful.  Changing WP and TP may affect the value of TE.  The following
        // situation MUST be met in order to have TE set to users value, otherwise the system
        // will automatically adjust the value of TE.
        // WP x TP > TE;     This means that the product of WP and TP MUST NOT exceed the desired
        // value of TE or the system will automatically adjust TE for you.
        ////////////////////////////////////////////////

        // Set WP - Pings Per Ensemble. Default is 60.
        _log4j.info("enterSafeMode() - Setting Workhorse SAFE Pings Per EnSemble (WP).");
        String pings = ("WP" + _attributes.safePingsPerEnsemble);
        _toDevice.write(mkCmd(pings));

        // Set TP - Time Per Ping (seconds). Default is 1s.
        _log4j.info("enterSafeMode() - Setting Workhorse SAFE Time Per Ping (TP).");
        String time = ("TP0:" + _attributes.safeSecPerPing + ".0");
        _toDevice.write(mkCmd(time));

        _log4j.info("enterSafeMode() - Saving Workhorse Parameters.");
        sendCommand(COMMAND_SAVE_PARAMETERS);

        _log4j.info("enterSafeMode() - Deploying Workhorse in SAFE mode NOW.");
        exitCommandMode();

        _safed = true;

    } //end of method enterSafeMode

    public org.mbari.siam.distributed.DevicePacketParser getDevicePacketParser() throws NotSupportedException {
        if (devicePacketParser == null) {
            if (_log4j.isInfoEnabled()) {
                _log4j.debug("Creating a DevicePacketParser");
            }
            devicePacketParser = new DevicePacketParser();
        }
        return devicePacketParser;    //To change body of overridden methods use File | Settings | File Templates.
    }

    /***********************/
    /** PROTECTED METHODS **/
    /***********************/

    /**
     * Sends a request to the Workhorse for parametric data. The response is
     * returned as a string.
     * <p/>
     * sendRequest makes every attempt to communicate with the unit by a process
     * of resetting, flushing input buffer and resending.
     *
     * @param request string
     * @return Response returned by the Workhorse
     * @throws Exception
     */
    protected String sendRequest(String request) throws Exception {

        byte[] response = new byte[MAX_REQUEST_BYTES];
        for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
            // Prepare to send message
            try {
                _fromDevice.flush();
                _toDevice.write(mkCmd(request));
                _log4j.debug("sendRequest: wrote request "
                        + new String(mkCmd(request)));

                StreamUtils.skipUntil(_fromDevice, "\r\n".getBytes(),
                        ECHO_RESPONSE_TIME);
                _log4j.debug("sendRequest: got echo response: "
                        + (new String(response)).trim());

                StreamUtils.readUntil(_fromDevice, response, RESPONSE_PROMPT
                        .getBytes(), RESPONSE_TIME);
                _log4j.debug("sendRequest: got instrument response: "
                        + (new String(response)).trim());

                return new String(response);
            }
            catch (IOException e) { // This is bad - not sure a retry would
                // help
                _log4j.error("sendRequest caught IOException " + e);
                throw new Exception("sendRequest(" + request
                        + ") - Stream I/O failure");
            }
            catch (NullPointerException e) { // This is bad - a retry isn't
                // going to make this one batter
                _log4j.error("sendRequest caught Null Pointer Exception " + e);
                throw new Exception("sendRequest(" + request
                        + ") - Null pointer");
            }
            catch (TimeoutException e) {
                _log4j.error("sendRequest caught Timeout Exception: " + e
                        + "Request=" + request);
                // incTimeoutCount(); Don't include these as they occur often
                // when resyncing
            }
            catch (Exception e) { // Probably exceeded max bytes - bad command
                // maybe
                _log4j.error("sendRequest caught Exception " + e);
                //incBadResponseCount();
            }
            // Reset interface using Esc + \r
            _toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
            Thread.sleep(500);
        }
        //incRetryExceededCount();
        throw new Exception("sendRequest(" + request
                + ") - Maximum retries attempted");
    }

    /**
     * Method to send commands to the Workhorse. sendCommand makes every attempt
     * to communicate with the unit by a process of resetting, flushing input
     * buffer and resending.
     * <p/>
     * Note: Trailing '\r' is automatically added to command string.
     *
     * @param cmd Command string to send
     * @throws Exception thrown if the method fails to send the command.
     */
    protected void sendCommand(String cmd) throws Exception {

        for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
            // Pr< to send message
            try {
                _fromDevice.flush();
                _toDevice.write(mkCmd(cmd));
                _toDevice.flush();
                StreamUtils.skipUntil(_fromDevice, RESPONSE_PROMPT.getBytes(),
                        RESPONSE_TIME);
                _log4j.debug("sendCommand: returning");
                return;
            }
            catch (IOException e) { // This is bad - not sure a retry would
                // help
                _log4j.error("sendCommand caught IOException " + e);
                throw new Exception("sendCommand(" + cmd
                        + ") - Stream I/O failure");
            }
            catch (NullPointerException e) { // This is bad - a retry isn't
                // going to make this one batter
                _log4j.error("sendCommand caught Null Pointer Exception " + e);
                throw new Exception("sendCommand(" + cmd + ") - Null pointer");
            }
            catch (TimeoutException e) {
                _log4j.error("sendCommand caught Timeout Exception: " + e
                        + "Cmd=" + cmd);
                // incTimeoutCount(); Don't include these as they occur often
                // when resyncing
            }
            catch (Exception e) { // Probably exceeded max bytes - bad command
                // maybe
                _log4j.error("sendCommand caught Exception " + e);
                //incBadResponseCount();
            }
            // Reset interface using "\r"
            _toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
            Thread.sleep(500);
        }
        //incRetryExceededCount();
        throw new Exception("sendCommand(" + cmd
                + ") - Maximum retries attempted");
    }

    /**
     * sends a break to the Workhorse a waits for a prompt
     *
     * @throws Exception
     */
    protected void enterCommandMode() throws Exception {

        for (int i = 0; i < MAX_BREAK_TRIES; i++) {
            _log4j.debug("enterCommandMode() - send break #" + i);
            sendBreak();
            Thread.sleep(1500);
            for (int j = 0; j < MAX_PROMPT_TRIES; j++) {
                _log4j.debug("enterCommandMode() - force prompt #" + j);

                _toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));

                try {

                    StreamUtils.skipUntil(_fromDevice, RESPONSE_PROMPT
                            .getBytes(), PROMPT_RESPONSE_TIME);
                    _log4j.debug("enterCommandMode(): returning");
                    return;
                }
                catch (IOException e) { // This is bad - not sure a retry
                    // would help
                    _log4j.error("enterCommandMode caught IOException " + e);
                    //throw new Exception("StreamUtils.skipUntil() " + e);
                }
                catch (NullPointerException e) { // This is bad - a retry
                    // isn't going to make this
                    // one batter
                    _log4j
                            .error("enterCommandMode caught Null Pointer Exception "
                                    + e);
                    //throw new Exception("StreamUtils.skipUntil() " + e);
                }
                catch (TimeoutException e) {
                    _log4j.error("enterCommandMode caught Timeout Exception: "
                            + e);
                }
                catch (Exception e) { // Probably exceeded max bytes - bad
                    // command maybe
                    _log4j.error("enterCommandMode caught Exception " + e);
                }
                Thread.sleep(1500);
            }
        }
        throw new Exception("enterCommandMode() - Maximum retries attempted");
    }

    /**
     * restarts sampling and exits command mode
     *
     * @throws IOException
     * @throws Exception
     */
    protected void exitCommandMode() throws IOException, Exception {
        _log4j.debug("exitCommandMode:sending " + COMMAND_START_PINGING);
        _toDevice.write(mkCmd(COMMAND_START_PINGING));
    }

    /**
     * sends a break longer then 350mS required by the Workhorse
     */
    protected void sendBreak() {
        if (_instrumentPort instanceof SerialInstrumentPort) {
            _log4j.debug("Sending break.....");
            SerialInstrumentPort sip = (SerialInstrumentPort) _instrumentPort;
	    for (int i = 0; i < _attributes.numBreaks; i++) {
		sip.sendBreak(BREAK_FOR_400MS);
	    }
        }
    }


    /**
     * Utility method to construct a message of the form: -
     * <p/>
     * <BLOCKQUOTE>
     * <p/>
     * <PRE>
     * <p/>
     * "cmd + \r" </BLOCKQUOTE>
     * <p/>
     * </PRE>
     * <p/>
     * and returns this as a byte array for transmission
     *
     * @param cmd basic command string to construct
     * @return byte array of command
     */
    protected byte[] mkCmd(String cmd) {
        return (new String(cmd + "\r")).getBytes();
    }

    /**
     * Cludge to create an int from 2 bytes (There is probably a better way of
     * doing this but this works)
     *
     * @param loByte ls byte
     * @param hiByte ms byte
     * @return int value of hiByte:loByte
     */
    protected int mkInt(byte loByte, byte hiByte) {
        int temp1 = loByte & 0xff;
        int temp2 = hiByte & 0xff;
        return temp1 + (temp2 << 8);
    }

    /**
     * Returns the property value from the Workhorse response
     *
     * @param response response string return from Workhorse
     * @return string representing the property (rhs of ='s)
     * @throws InvalidPropertyException
     */
    protected String getProperty(String response) throws InvalidPropertyException {
        StringTokenizer tkzResponse = new StringTokenizer(response, " ", false);
        while (tkzResponse.countTokens() > 0) {
            String property = tkzResponse.nextToken();
            if (property.equals("=")) {
                property = tkzResponse.nextToken();
                return property;
            }
        }
        throw new InvalidPropertyException("Invalid Workshorse property");
    }

    /**
     * splits the response into fields defined by the delim string and returns
     * these in an array. fieldCount limits the number of fields generated.
     *
     * @param response   string to parse
     * @param delim      field delimiter(s)
     * @param fieldCount number of expected fields
     * @return array of Strings of each field detected
     * @throws InvalidPropertyException an illegal response string has been determined
     */
    protected String[] parseProperty(String response, String delim, int fieldCount)
            throws InvalidPropertyException {
        StringTokenizer tkzResponse = new StringTokenizer(response, delim,
                false);
        if (fieldCount > tkzResponse.countTokens()) {
            throw new InvalidPropertyException("Invalid Workshorse property");
        }

        String[] property = new String[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            property[i] = tkzResponse.nextToken();
        }
        return property;
    }

    public void printData(byte[] buf) {
        PrintUtils.printFull(buf, 0, 64);
    }

    public int test() {
        return 0;
    }


    /**
     * Called after power is applied; return when instrument is ready for use.
     */
    protected void powerOnCallback() throws Exception {
	_log4j.debug("powerOnCallback(): sleeping for " + 
		     _attributes.warmupSec + " seconds.");
        Thread.sleep(_attributes.warmupSec * 1000);
    }


    /**
     * Shutdown instrument sampling.
     */
    protected String shutdownInstrument()
            throws Exception {

        if (_safed) {
            return "Already in safe mode";
        }

	if (nPowerRequests() == 0) {
	    return "Never powered up; assume instrument in low-power sleep";
	}

        _log4j.debug("shutdownPort() - managePowerWake()");
        managePowerWake();

        _log4j.debug("shutdownPort() - send multi breaks!");
        for (int i = 0; i < 3; i++) {
            sendBreak();
            Thread.sleep(500);
        }

        _log4j.debug("shutdownPort() - enterCommandMode()");
        enterCommandMode();
        _log4j.debug("shutdownPort() - Power down the ADCP");
        _fromDevice.flush();
        _toDevice.write(mkCmd(COMMAND_POWER_DOWN));
        _toDevice.flush();
        StreamUtils.skipUntil(_fromDevice, "Powering Down".getBytes(),
                RESPONSE_TIME);


        return "Workhorse in low-powered sleep";
    }


    /**
     * Configurable Workhorse attributes
     */

    public class Attributes extends InstrumentServiceAttributes {

        /**
         * Constructor, with required InstrumentService argument
         */
        public Attributes(DeviceServiceIF service) {
            super(service);
            summaryVars = new String[]{"velocity-1", "velocity-2", "velocity-3", "velocity-4"};
        }

        /**
         * Internal logging
         */
        boolean internalLog = false;

/////////////////////////////////////////////////////////////
////                SAFE MODE ATTRIBUTES         	 ////
/////////////////////////////////////////////////////////////

        /**
         * Set TE (Time per Ensemble) to be set on enterSafeMode.
         */
        int safeSampleIntervalSec = 300; //Default to 5 minutes.

        /**
         * Set WP (Pings per Ensemble) to be set on enterSafeMode.
         */
        int safePingsPerEnsemble = 60; // Default to 60 pings.

        /**
         * Set TP (Time per Ping) to be set on enterSafeMode.
         */
        int safeSecPerPing = 1; // Default to 1 sec.

/////////////////////////////////////////////////////////////
////                NORMAL MODE ATTRIBUTES		 ////
/////////////////////////////////////////////////////////////

        /**
         * Set WP (Pings per Ensemble) to be set on initializeInstrument (on startup or following safeMode).
         */
        int pingsPerEnsemble = 60; // Default to 60 pings.

        /**
         * Set TP (Time per Ping) to be set on initializeInstrument (on startup or following safeMode).
         */
        int secPerPing = 1; // Default to 1 sec.

	/** Added msecPerPing to allow ping rates > 1 Hz, 15may2008, rah.
	 *  To use this, set secPerPing to 0.  Driver will then use msecPerPing.
	 *  Defined as milliseconds per ping
	 */
	int msecPerPing = 0;

/////////////////////////////////////////////////////////////

        /**
         * Guard time (seconds) to allow mooring controller wake-up.
         */
        int preemptionSec = 20;


        /**
         * Time for instrument to warm up after power-on
         */
        int warmupSec = 150;


        /**
         * Number of break characters to wake up
         */
        int numBreaks = 1;


	/** EX Command String		*/
	String exCommand = COMMAND_DEFAULT_EX;  // Always set Earth Coordinates in case of batt. failure.

        /**
         * Throw InvalidPropertyException if any invalid attribute values found
         */
        public void checkValues() throws InvalidPropertyException {
            if (preemptionSec <= 0) {
                throw new InvalidPropertyException(preemptionSec
                        + "Invalid preemptionSec." + " Must be > 0");
            }
        }
    }

    /**
     * A Parser that prduces JDDAC records for use by InstrumentServiceBlocks (such as the summarizer)
     */
    class DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser {

        private final PD0DataStructure dataStructure = new PD0DataStructure();

        protected void parseFields(DevicePacket packet) throws NotSupportedException, Exception {

            if ((packet == null) || !(packet instanceof SensorDataPacket)) {
                throw new NotSupportedException("Expected a SensorDataPacket");
            }

            SensorDataPacket p = (SensorDataPacket) packet;
            byte[] data = p.dataBuffer();
            if (data[0] == PD0DataStructure.ID_HEADER && data[1] == PD0DataStructure.ID_DATA_SOURCE) {
                dataStructure.setData(data);

                int nBeams = dataStructure.getNumberOfBeams();
                addMeasurement("nBeams", "Number of beams", "count", nBeams);
                addMeasurement("nCells", "Number of cells", "count", dataStructure.getNumberOfCells());
                addMeasurement("heading", "Heading of ADCP", "degrees", new Float(dataStructure.getHeading()));
                addMeasurement("pitch", "Pitch of ADCP", "degrees", new Float(dataStructure.getPitch()));
                addMeasurement("roll", "Heading of ADCP", "degrees", new Float(dataStructure.getRoll()));
                addMeasurement("pings", "Pings per ensemble", "count", dataStructure.getPingsPerEnsemble());
                addMeasurement("depth", "Depth of transducer", "meters", new Float(dataStructure.getDepthOfTransducer()));
                for (int i = 1; i <= nBeams; i++) {

                    int[] v = dataStructure.getVelocity(i);
                    if (v != null) {
                        addArrayMeasurement("velocity-" + i, "Velocity for beam " + i, "mm/s", v);
                    }
                    float[] ei = dataStructure.getEchoIntensity(i);
                    if (ei != null) {
                        addArrayMeasurement("echoIntensity-" + i, "Echo Intensity for beam " + i, "dB", ei);
                    }
                    int[] cm = dataStructure.getCorrelationMagnitude(i);
                    if (cm != null) {
                        addArrayMeasurement("correlationMagnitude-" + i, "Correlation Magnitude for beam " + i, "[0=bad,255=good]", cm);
                    }
                    int[] pg = dataStructure.getPercentGood(i);
                    if (pg != null) {
                        addArrayMeasurement("percentGood-" + i, "Percent Good for beam " + i, "%", pg);
                    }
                }

            }

        }

    }

    public interface DataStructure {

    }

    /**
     * This class encapsulates access to the workhorses binary data format
     */
    public static class PD0DataStructure implements DataStructure, Serializable {
        private byte[] data;

        public static final byte ID_HEADER = 0x7F;
        public static final byte ID_DATA_SOURCE = 0x7F;
        public static final int ID_FIXED_LEADER = 0x0000;
        public static final int ID_VELOCITY = 0x0100;
        public static final int ID_CORRELATION_MAGNITUDE = 0x0200;
        public static final int ID_ECHO_INTENSITY = 0x0300;
        public static final int ID_PERCENT_GOOD = 0x0400;
        public static final int ID_VARIABLE_LEADER = 0x0080;
        public static final int DATATYPE_FIXED_LEADER = 1;
        public static final int DATATYPE_VARIABLE_LEADER = 2;


        /**
         * Convert two contiguous bytes to a short, starting at specified
         * index of dataBytes; assumes little-endian.
         *
         * @param index
         * @return short int
         */
        public int getShort(int index) {
            short lsb = (short) (data[index] & 0xFF);
            short msb = (short) (data[index + 1] & 0xFF);
            return (int) (lsb | (msb << 8));
        }

        public byte[] getBytes(int index, int length) {
            byte[] copy = new byte[length];
            System.arraycopy(data, index, copy, 0, length);
            return copy;
        }

        /**
         * @param data
         */
        public void setData(byte[] data) {

            boolean ok = false;

            /* verify that this is a valid data ensemble */
            try {
                ok = data != null && data[0] == ID_HEADER && data[1] == ID_DATA_SOURCE;
                if (ok) {
                    this.data = data;
                    ok = getShort(getHeaderLength() + 1) != ID_FIXED_LEADER;
                }
            }
            catch (Exception e) {
                if (_log4j.isDebugEnabled()) {
                    _log4j.debug("Not a valid workhorse ensemble", e);
                }
                ok = false;
            }

            /*
            * If not valid throw set teh data to null and throw an exception
            */
            if (!ok) {
                this.data = null;
                throw new IllegalArgumentException("Not a a valid workhorse data ensemble");
            }

        }

        /**
         * @return data byte array member variable
         */
        public byte[] getData() {
            return data;
        }

        /* ------------------------------------------------------------------- 
          Header Info
        */

        /**
         * @return The number of bytes in the ensemble, as reported by the fixed header.
         */
        public int getNumberOfBytesInEnsemble() {
            return getShort(2);
        }

        /**
         * @return The number of data types that the ADCP is configured for. By default,
         *         the ADCP returns fixed/variable leader, velocity, correlation magnitude, echo intensity,
         *         and percent good.
         */
        public int getNumberOfDataTypes() {
            return (int) data[5];
        }

        /**
         * @return The length in bytes of the fixed header
         */
        public int getHeaderLength() {
            return 2 * getNumberOfDataTypes() + 6;
        }

        /**
         * @param i The data type number this can range form 1-6. The exact upper
         *          limit is returned
         * @return The offset for data type #i. Adding '1' to this offset number
         *         gives the absolute byte number in the ensemble where data type #i
         *         begins.
         */
        public int getOffsetForDataType(int i) {
            if (i == 0 || i > getNumberOfDataTypes()) {
                throw new IllegalArgumentException("The valid range for data-types is 1-" +
                        getNumberOfDataTypes() + ". The value " + i + " is invalid.");
            }
            return getShort(6 + (i - 1) * 2);
        }

        /* -------------------------------------------------------------------
          Fixed Leader Info
        */

        /**
         * @return The starting index of the start of the fixed header
         */
        public int getOffsetForFixedLeader() {
            return getOffsetForDataType(DATATYPE_FIXED_LEADER);
        }

        /**
         * @return number of beams
         */
        public int getNumberOfBeams() {
            return NumberUtil.toUnsignedInt(data[getOffsetForFixedLeader() + 8]);
        }

        /**
         * @return number of cells
         */
        public int getNumberOfCells() {
            return NumberUtil.toUnsignedInt(data[getOffsetForFixedLeader() + 9]);
        }

        /**
         * @return pings per ensemble
         */
        public int getPingsPerEnsemble() {
            return (int) getShort(getOffsetForFixedLeader() + 10);
        }

        /**
         * @return length of depth cell
         */
        public int getDepthCellLength() {
            return (int) getShort(getOffsetForFixedLeader() + 12);
        }

        /**
         * @return max error velocity
         */
        public int getErrorVelocityMaximum() {
            return (int) getShort(getOffsetForFixedLeader() + 20);
        }

        /**
         * @return Bin One Distance
         */
        public int getBinOneDistance() {
            return (int) getShort(getOffsetForFixedLeader() + 32);
        }

        /* ------------------------------------------------------------------- 
          Variable Leader Info
        */

        /**
         * @return The index into the data that's the start of the Variable Leader
         */
        public int getOffsetForVariableLeader() {
            return getOffsetForDataType(DATATYPE_VARIABLE_LEADER);
        }

        /**
         * @return speed of sound (m/s)
         */
        public int getSpeedOfSound() {
            return getShort(getOffsetForVariableLeader() + 14);
        }

        /**
         * @return depth in meters
         */
        public float getDepthOfTransducer() {
            return getShort(getOffsetForVariableLeader() + 16) * 0.1F;
        }

        /**
         * @return heading in degrees (0 -360)
         */
        public float getHeading() {
            return getShort(getOffsetForVariableLeader() + 18) * 0.01F;
        }

        /**
         * @return pitch in degrees (-20 - 20)
         */
        public float getPitch() {
            return getShort(getOffsetForVariableLeader() + 20) * 0.01F;
        }

        /**
         * @return roll in degrees(-20 - 20)
         */
        public float getRoll() {
            return getShort(getOffsetForVariableLeader() + 22) * 0.01F;
        }

        /**
         * @return salinity in ppt
         */
        public int getSalinity() {
            return getShort(getOffsetForVariableLeader() + 24);
        }

        /**
         * @return temperature in celsius (-5 - 40 degrees)
         */
        public float getTemperature() {
            return getShort(getOffsetForVariableLeader() + 26) * 0.01F;
        }

        /* ------------------------------------------------------------------- 
          Data Info
        */


        /**
         * @param beam The beam number to return (values are 1-4)
         * @return An array of velocities for the beam. (mm/s along beam axis).
         *         <b>null</b> is returned if no velocity data was found
         */
        public int[] getVelocity(int beam) {

            if (beam < 1 || beam > 4) {
                throw new IllegalArgumentException("Beam number must be between 1 and 4. You specified " + beam);
            }

            /*
             * Find the index into the data for the start of the velocity record
             */
            int idx = -1;
            int nTypes = getNumberOfDataTypes();
            for (int i = 3; i <= nTypes; i++) {
                int idxTest = getOffsetForDataType(i);
                int id = getShort(idxTest);
                if (id == ID_VELOCITY) {
                    idx = idxTest;
                    break;
                }
            }

            int[] velocities = null;
            if (idx > 0) {
                int nBeams = getNumberOfBeams();
                idx += 2 + ((beam - 1) * 2); // Skip to start of velocity data
                velocities = new int[getNumberOfCells()];
                for (int i = 0; i < velocities.length; i++) {
                    velocities[i] = (int) NumberUtil.toShort(getBytes(idx, 2), true);
                    idx += (nBeams * 2);
                }
            }
            return velocities;
        }


        /**
         * @param beam The beam number to return (values are 1-4)
         * @return Magnitude of normalized echo autocorrelation at the lag used for estimating Doppler phase change.
         *         0 = bad; 255 = perfect (linear scale)
         */
        public int[] getCorrelationMagnitude(int beam) {
            return getValues(beam, ID_CORRELATION_MAGNITUDE);
        }

        /**
         * @param beam The beam number to return (values are 1-4)
         * @return echo intensity in dB
         */
        public float[] getEchoIntensity(int beam) {
            int[] ei = getValues(beam, ID_ECHO_INTENSITY);
            float[] out;
            if (ei != null) {
                out = new float[ei.length];
                for (int i = 0; i < ei.length; i++) {
                    out[i] = ei[i] * 0.45F;
                }
            }
            else {
                out = null;
            }
            return out;
        }

        /**
         * @param beam The beam number to return (values are 1-4)
         * @return Data-quality indicator that reports percentage (0 - 100) of good data collected for each depth
         *         cell of the velocity profile. The settings of the EX command determines how the Workhorse references percent-
         *         good data. Refer to Workhorse manual for moe details.
         */
        public int[] getPercentGood(int beam) {
            return getValues(beam, ID_PERCENT_GOOD);
        }

        /**
         * @param beam THe beam number to return (values are 1-4)
         * @return An array of values for the beam.
         *         <b>null</b> is returned if nodata was found
         */
        private int[] getValues(int beam, int type) {
            if (beam < 1 || beam > 4) {
                throw new IllegalArgumentException("Beam number must be between 1 and 4. You specified " + beam);
            }

            /*
             * Find the index into the data for the start of the record type you want
             */
            int idx = -1;
            int nTypes = getNumberOfDataTypes();
            for (int i = 3; i <= nTypes; i++) {
                int idxTest = getOffsetForDataType(i);
                int id = getShort(idxTest);
                if (id == type) {
                    idx = idxTest;
                    break;
                }
            }

            int[] values = null;
            if (idx > 0) {
                int nBeams = getNumberOfBeams();
                idx += 2 + (beam - 1); // Skip to start of data record
                values = new int[getNumberOfCells()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = NumberUtil.toUnsignedInt(data[idx]);
                    idx += nBeams;
                }
            }
            return values;
        }

    }
}


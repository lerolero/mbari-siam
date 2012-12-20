/** 
 * @Title Satlantic ISUS instrument driver
 * @author Martyn Griffiths
 * @version 1.0
 * @date 8/6/2003
 *
 * Copyright MBARI 2003
 * 
 * REVISION HISTORY:
 * $Log: SatlanticISUS.java,v $
 * Revision 1.4  2009/07/16 17:06:12  headley
 * javadoc syntax fixes
 *
 * Revision 1.3  2009/07/16 15:07:35  headley
 * javadoc syntax fixes
 *
 * Revision 1.2  2009/07/16 05:37:00  headley
 * javadoc syntax fixes
 *
 * Revision 1.1  2008/11/04 22:17:56  bobh
 * Initial checkin.
 *
 * Revision 1.1.1.1  2008/11/04 19:02:04  bobh
 * Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
 *
 * Revision 1.29  2006/07/24 16:19:46  headley
 *        The sampling strategy in this version has been completely revised, and
 *        appears to be more robust: The instrument must be configured in Scheduled
 *        mode, and given a power policy of POWER_ALWAYS. At initialization, the
 *        instrument is power-cycled, and the sample period is obtained from the
 *        instrument, by traversing its menus.
 *
 *        Following successful initialization, the instrument service attempts to
 *        obtain a sample. The instrument service begins by waiting for text
 *        indicating that the UV light source is powered on, which precedes the
 *        output of data by some (variable) relatively short time period. If the
 *        UV-on text is not found, the service power cycles the instrument and
 *        reschedules itself to make another attempt in the next sample interval.
 *
 *        If UV-on text is found, the service framework calls readSample, which
 *        in turn calls the method readISUSRecords. readISUSRecords then reads
 *        lines (using StreamUtils.skipUntil); a pair of timeouts governs the
 *        reading of each line and the total time allowed to obtain the data. This
 *        is not ideal, since the timeout for the reading of each line must be
 *        empirically determined, and is somewhat arbitrary, and as a result is
 *        probably longer than necessary. It seems to work fairly robustly, however.
 *        When the UV-on message is found, the service is re-synchronized to
 *        execute again in one sample period less some gaurd time (specified in
 *        _attributes.preemptionSec).
 *
 *        The loop acquires lines, which it must parse to eliminate non-data lines
 *        and to extract the data, which is coalesced into the sample buffer and
 *        returned to the instrument service. When the expected number of light
 *        and dark records (set through _attributes.lightRecords, etc.), the
 *        data collection ends.
 *
 *        Two additional tuning parameters, _attributes.requestAdjustSec and
 *        _attributes.readAdjustSec, may be used to adjust the timing in situ,
 *        should it change significantly, as it has been known to do, perhaps
 *        owing to file access timing within the instrument's flash file system.
 *        These default to zero.
 *
 * Revision 1.28  2006/03/03 23:25:07  oreilly
 * Modified javadoc
 *
 * Revision 1.27  2005/10/14 22:03:38  headley
 * appended units to all time mnemonic names (e.g. MILLIS, Secs, etc)
 *
 * Revision 1.26  2004/10/15 20:19:34  oreilly
 * utilizes ServiceAttributes framework
 *
 * Revision 1.25  2004/08/18 22:17:29  oreilly
 * Use _toDevice.write(), _fromDevice.flush()
 *
 * Revision 1.24  2004/05/18 22:52:49  headley
 * modified timing parameters, changed powerManageSleep in resync to
 * use a instrumentPort method (managePowerSleep won't work since port is
 * POWER_ALWAYS)
 *
 * Revision 1.23  2004/05/03 21:53:38  headley
 * bug fixes:
 * - fixed max wait time and sync behavior in requestSample
 * - detects unrecoverable state (spitting asterisks) in readLine
 *
 * Revision 1.22  2003/10/20 19:47:27  martyn
 * Removed superfluous debug output
 *
 * Revision 1.21  2003/10/17 22:45:54  martyn
 * Tweak to remove a timeout during initialization
 *
 * Revision 1.20  2003/10/16 00:26:04  martyn
 * Spurious initialization failures:-
 * - ISUS set time requires 2 '\r' chars to reliably set it
 * - Kick starting a little more persisitent
 * Spurious capture failures:
 *  - capture times increased
 *
 * Revision 1.19  2003/10/15 00:52:18  martyn
 * initialInterval back the way it was - made things worse
 *
 * Revision 1.18  2003/10/15 00:19:55  martyn
 * Converted yield to sleep
 *
 * Revision 1.17  2003/10/15 00:18:02  martyn
 * Initial sample interval is shorter (180 seconds) than acquired revisited
 *
 * Revision 1.16  2003/10/14 18:45:59  martyn
 * Initial sample interval is shorter (180 seconds) than acquired
 *
 * Revision 1.15  2003/10/11 00:10:41  martyn
 * Refined log message types
 *
 * Revision 1.14  2003/10/10 18:24:24  martyn
 * Adjusted debug some messages - cosmetic.
 *
 * Revision 1.13  2003/10/09 22:30:06  mrisi
 * added a Thread.yield() to a polling loop
 *
 * Revision 1.12  2003/10/06 23:06:03  mrisi
 * modified to use StreamUtils.skipUntil
 *
 * Revision 1.11  2003/10/03 00:50:29  martyn
 * Mainly updating print statements to use log4j logging
 * Some tweaks.
 *
 * Revision 1.10  2003/09/24 01:23:05  martyn
 * Removal of irrelevent comment lines
 *
 * Revision 1.9  2003/09/17 01:40:55  martyn
 * Increased current limit to 4000mA!
 *
 * Revision 1.8  2003/09/17 00:03:37  martyn
 * Mainly cosmetic - commenting out println etc
 *
 * Revision 1.7  2003/09/15 23:38:46  martyn
 * Improved AsciiTime utility class
 * Updated all drivers to use revised class.
 * Removed registryName service property
 * Moved service property keys to ServiceProperties
 *
 * Revision 1.6  2003/09/03 21:50:19  oreilly
 * Replaced carriage returns with newlines
 *
 * Revision 1.5  2003/08/26 00:40:21  martyn
 * Added printData method
 * Revised power policy
 * Revised current limit
 *
 * Revision 1.4  2003/08/22 22:26:04  martyn
 * Initial reschedule done by initializeInstrument() determining schedule and
 * createDefaultSampleSchedule() returning this schedule to framework.
 * Note: a sampleSchedule property MUST not appear in puck for this to work!!
 *
 * Revision 1.3  2003/08/20 00:18:09  martyn
 * Updated to use postSample() in framework
 * Made debug strings in initializeInstrument and loadProperties consistent and more informative.
 *
 * Revision 1.2  2003/08/12 00:14:20  martyn
 * Added time synchronization
 *
 * Revision 1.1  2003/08/08 21:17:59  martyn
 * Initial version of MBARI-ISUS driver.
 * Gets ISUS sampling interval from instrument.
 * Gets ISUS data, parses and saves.
 * Problem: Trying to reschedule in initializeInstrument before schedule entry in table.
 *
 *
 */

package org.mbari.siam.devices.slisus;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
 * The <code>MBARI-ISUS</code> class represents the
 * <code>InstrumentServices</code> driver for controlling the Satlantic
 * MBARI_ISUS. The primary responsibilities of this class is to:-
 * <p>
 * <BLOCKQUOTE>
 * 
 * <PRE>
 * 
 * Capture sample data from the instrument.
 * 
 * </PRE>
 * 
 * </BLOCKQUOTE>
 * 
 * 
 * 
 * @see org.mbari.siam.distributed.Device
 * @see org.mbari.siam.distributed.Instrument
 * @see org.mbari.siam.distributed.PowerPort
 * @see org.mbari.siam.core.InstrumentService
 */

public class SatlanticISUS extends InstrumentService implements Instrument {

    // Menu Sequences
    static final String SEQ_GET_SCHEDULE = "\r,S>,S,UP>,D,OY>,S,\r\n\r\n";

    static final String SEQ_SET_RTC = "\r,S>,S,UP>,R,RTC>,T,?,1,?";

    // Max constants
    static final int MAX_L_SAMPLES = 5;

    static final int MAX_D_SAMPLES = 1;

    static final int MAX_FRAME_SIZE = 1711; // FULL telemetry size (Ops Man D-4)

    static final int MAX_RESPONSE_BYTES = 
	(MAX_D_SAMPLES + MAX_L_SAMPLES)* MAX_FRAME_SIZE+128;

    static final int MAX_SAMPLE_TRIES = 1; // NO RETRIES - this just receives

    // unsolicited data

    static final int MAX_COMMAND_TRIES = 3; // In command mode we need to be

    // more rigourous

    static final int MAX_REQUEST_BYTES = 80; // Max number of bytes sent back

    // from a request

    static final int MAX_MENU_LEVELS = 10; //!! confirm this

    static final int MAX_POWERUP_BYTES = 2000;

    // Timeouts (mS)
    static final int RESPONSE_TIME_MILLIS = 20000; // This needs to be longer than the

    // longest likely intercharacter
    // delay

    static final int PROMPT_RESPONSE_TIME_MILLIS = 20000; //!! Todo measure this

    static final int ECHO_RESPONSE_TIME_MILLIS = 2000;

    static final int BREAK_FOR_400MS = 400 * 250;

    static final int POWERUP_TIME_MILLIS = 180000;

    static final int WAKEUP_TIME_MILLIS = 30000;//was 15000

    static final int RESYNC_SLEEP_MILLIS = 30000;

    static final int SAMPLE_TIME_MILLIS = 20000;

    static final int READRECORD_TIMEOUT_MILLIS = 300000;

    // Commands
    static final String COMMAND_FORCE_PROMPT = ""; // "\r" implied

    static final String COMMAND_CONTROL_C = "\003";

    // Responses
    static final String RESPONSE_PROMPT = ">";

    static final String RESPONSE_STARTUP = "Wakeup";

    // Configurable ISUS attributes
    Attributes _attributes = new Attributes(this);

    // log4j Logger
    static private Logger _logger = Logger.getLogger(SatlanticISUS.class);

    private int _samplingTimeSecs = 0;

    long _initialIntervalSecs;

    /**
     * @throws RemoteException .
     */
    public SatlanticISUS() throws RemoteException {
    }

    /** Specify startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return 2000;
    }

    /** Specify prompt string. */
    protected byte[] initPromptString() {
	return RESPONSE_PROMPT.getBytes();
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	return new byte[0]; // none
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return MAX_RESPONSE_BYTES;
    }

    /** Specify current limit in increments of 120 mA upto 11880 mA. */
    protected int initCurrentLimit() {
	return 4000; //!! to do
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	_initialIntervalSecs = _samplingTimeSecs - _attributes.preemptionSec;
	_logger.debug("Rescheduling service for " + _initialIntervalSecs
		      + " seconds...");
	return new ScheduleSpecifier(_initialIntervalSecs * 1000);
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(_attributes.baud, //!! change back to 38400
					SerialPort.DATABITS_8, SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }

    /**
     * This method tries to initialize the ISUS. However the instrument is
     * extraordinarily difficult to program since it can be in a number of
     * states, each one requiring a different scheme to initialize the
     * instrument. This has made this method complex and potentially unreliable
     * as there is no documentation on the various states in which the
     * instrument can find itself. Empirically determining this is not the right
     * way but alas the only way.
     * 
     * @exception InitializeException
     * @exception Exception
     */
    protected void initializeInstrument() throws InitializeException, Exception {
	_logger.info("Initializing...");

	String response, property;
	String[] token;

	setMaxSampleTries(MAX_SAMPLE_TRIES);
	setSampleTimeout(RESPONSE_TIME_MILLIS);

	// turn on comms and instrument
	// (I think it is assumed that the power is always on (klh))
	_logger.debug("doing managePowerWake...");
	managePowerWake();

	// turn off instrument power until no activity for 3 seconds
	_logger.debug("disconnecting power...");
	_instrumentPort.disconnectPower();

	long t0 = System.currentTimeMillis();
	do {
	    _logger.info("Waiting for activity to stop");
	    _fromDevice.flush();
	    sleep(3000);
	    if ((System.currentTimeMillis() - t0) > 60000) {
		throw new InitializeException("Cannot reset ISUS");
	    }
	} while (_fromDevice.available() > 0);

	// Resupply power after short delay after last power save indicator has
	// been transmitted
	_logger.info("Starting up...");
	sleep(5000);

	// turn on ISUS power
	managePowerWake();

	// let instrument power up
	sleep(1000);

	// try to get instrument's attention
	// this driver assumes that the ISUS is in Scheduled Mode and is always
	// powered
	boolean bOK = false;
	for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
	    _toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
	    try {
		// Wait for power up cycle to complete; look for 'Wakeup'
		// response
		StreamUtils.skipUntil(_fromDevice, RESPONSE_STARTUP.getBytes(),
				      POWERUP_TIME_MILLIS, MAX_POWERUP_BYTES);
		bOK = true;
		break;
	    } catch (Exception e) {
		_logger.error("Exception during wakeup:" + e);
		// Try again
	    }
	}
	if (!bOK)
	    throw new InitializeException("No recognizable response");

	_logger.info("wakeup text found");
	try {
	    // read sample interval
	    // (assumes that the instrument is in scheduled mode (klh))
	    enterCommandMode();

	    // ISUS returns sampling interval in *minutes*
	    // we display it understanding it's min, but immediately convert
	    // to seconds
	    _samplingTimeSecs = getSamplingInterval();
	    _logger.info("sample interval:" + _samplingTimeSecs + " min");

	    // Convert to seconds
	    _samplingTimeSecs *= 60;

	    // set _initialSample interval here in case it isn't set by
	    // createDefaultSampleInterval, which is only called if no sample
	    // interval is defined (klh 07/21/2006)
	    _initialIntervalSecs = _samplingTimeSecs - _attributes.preemptionSec;

	    exitCommandMode();

	    if (_attributes.timeSynch) {
		// Set ISUS RTC time
		// Allow ISUS to power down
		_logger.info("setting ISUS RTC");
		sleep(10000);
		enterCommandMode();
		setTime();
		exitCommandMode();
		_logger.info("time synch'd");
	    }

	} catch (NumberFormatException e) {
	    throw new InitializeException("parseInt()" + e);
	} catch (InvalidPropertyException e) {
	    throw new InitializeException("getProperty()" + e);
	} catch (Exception e) {
	    throw new InitializeException("sendSequence() " + e);
	} finally {
	    // turn off communications/power
	    managePowerSleep();
	}

	_logger.info("Initializing completed");
    }

    private int getSamplingInterval() throws Exception {
	sendSequence(SEQ_GET_SCHEDULE);
	String str = readLine(_fromDevice, 1000);
	str = getProperty(str);
	return new Integer(str).intValue();
    }

    private void setTime() throws Exception {
	// Format - 08/11/2003 20:31:50
	sendSequence(SEQ_SET_RTC);
	sendCommand(AsciiTime.getDate("MM/DD/YYYY") + " "
		    + AsciiTime.getTime("HH:mm:ss") + "\r", "?");
    }

    /**
     * Called by the framework before sampling.
     * 
     * @exception Exception
     */
    protected void prepareToSample() throws Exception {
	// Nothing to do
    }

    /**
       requestSample does not actually poll the instrument in this case,
       since it is assumed to be in a scheduled output mode, periodically 
       coming out of sleep, taking a measurement, and sending the data
       out the serial port.

       Devising a robust sampling strategy for the instrument is difficult, 
       since there are not an explicit delimiters marking the beginning and end
       of the data, and the timing is variable and not deterministic; there is
       some text that comes out before the data appears, but nothing to indicate
       the end of the data output. It would have been better if it were possible
       to disable all output other than the data, or if there were a delimiter
       to mark the end of data, and some known bound on the time it could take
       to produce a sample. A polled mode would also suffice. 
       The sampling strategy is further complicated by the fact that the power-on
       timing is such that if the instrument is not allowed enough time between
       power-off and power-on, it can go into an unresponsive state. Also, if
       the instrument is kept awake through a sampling cycle, it goes into
       an unresponsive state. 

       Another difficulty presented by the instrument is that its configuration 
       consists of multi-layered menus which present an obstacle to programming, 
       since traversing them is cumbersome in software. Moreover, the menu text
       contains subtle changes in different firmware versions, creating additional
       opportunities for failure through broken parsing. Another configuration
       related issue is that some areas of the menu system cannot be exited 
       without power cycling the instrument, making it awkward to discover the
       instrument's current state in a robust and efficient way.

       The sampling strategy in this version has been completely revised, and 
       appears to be more robust: The instrument must be configured in Scheduled
       mode, and given a power policy of POWER_ALWAYS. At initialization, the 
       instrument is power-cycled, and the sample period is obtained from the
       instrument, by traversing its menus.
       
       Following successful initialization, the instrument service attempts to
       obtain a sample. The instrument service begins by waiting for text
       indicating that the UV light source is powered on, which precedes the
       output of data by some (variable) relatively short time period. If the
       UV-on text is not found, the service power cycles the instrument and 
       reschedules itself to make another attempt in the next sample interval.

       If UV-on text is found, the service framework calls readSample, which
       in turn calls the method readISUSRecords. readISUSRecords then reads
       lines (using StreamUtils.skipUntil); a pair of timeouts governs the
       reading of each line and the total time allowed to obtain the data. This
       is not ideal, since the timeout for the reading of each line must be
       empirically determined, and is somewhat arbitrary, and as a result is
       probably longer than necessary. It seems to work fairly robustly, however.
       When the UV-on message is found, the service is re-synchronized to 
       execute again in one sample period less some gaurd time (specified in
       _attributes.preemptionSec).

       The loop acquires lines, which it must parse to eliminate non-data lines
       and to extract the data, which is coalesced into the sample buffer and
       returned to the instrument service. When the expected number of light
       and dark records (set through _attributes.lightRecords, etc.), the
       data collection ends.

       Two additional tuning parameters, _attributes.requestAdjustSec and 
       _attributes.readAdjustSec, may be used to adjust the timing in situ, 
       should it change significantly, as it has been known to do, perhaps 
       owing to file access timing within the instrument's flash file system.
       These default to zero.

       * @exception TimeoutException
       *                thrown if no data is detected within the specified timeout period
       * @exception Exception
       *                not thrown
       */
    protected void requestSample() throws TimeoutException, Exception {
	
	// flush input stream
	_fromDevice.flush();

	// set timeout for obtaining UV-ON message
	// wait a sample period plus two guard times
	// plus a fudge factor (defaults to 0)
	long maxWaitSeconds = (_initialIntervalSecs * 1000 + 
			       2000 * _attributes.preemptionSec +
			       1000 * _attributes.requestAdjustSec) / 1000;
	
	// string to look for	
	final String DATA_TERMINATOR = "ON UV";

	// data buffer
	int bufSize=(2048);
	byte[] buf= new byte[bufSize];

	// state variables
	long t0=System.currentTimeMillis();
	boolean UVON=false;

	// wait for the UV ON message
	try{
	    _logger.debug("requestSample: looking for UV ON tmout="+maxWaitSeconds+" s");
	    StreamUtils.readUntil(_fromDevice,
				  buf,
				  DATA_TERMINATOR.getBytes(),
				  1000L*maxWaitSeconds);
	    _logger.debug("requestSample: found UV ON");
	    UVON=true;
	}catch(TimeoutException t){
	    _logger.debug("requestSample: timed out UV ON not found");
	}catch(Exception e){
	    _logger.debug("requestSample: overflow "+e);
	}

	// show what was found (useful for debugging,
	// if the string wasn't found)
	String s=new String(buf);
	s.trim();
	_logger.debug("requestSample: read "+s);
	
	long newInterval = 0L;

	// If the UV ON message wasn't found, 
	// power cycle the instrument and reschedule the
	// service sample interval
	if(!UVON){
	    // Resync ISUS - power cycle instrument
	    _logger.info("requestSample: Turning power off for 30 seconds...");
	    
	    // Note - we need to turn power off for 30 seconds to let the
	    // internal resevoir cap decay
	    // completely. Premature reapplication of power will cause ISUS
	    // to end up in an unresponsive loop.
	    // can't use managePowerSleep, since it does nothing when the 
	    // instrument's power policy is POWER_ALWAYS. (klh)
	    _instrumentPort.disconnectPower();
	    
	    // Without this sleep,
	    // the instrument goes into an unresponsive state
	    sleep(RESYNC_SLEEP_MILLIS);
	    
	    // Power instrument on
	    managePowerWake();
	    _logger.info("requestSample: power restored");
	    
	    // Reschedule for the initial interval after power up
	    newInterval = (_initialIntervalSecs*1000)+POWERUP_TIME_MILLIS;
	    _logger.info("requestSample: rescheduling service for "
			 + newInterval + 
			 " seconds...");

	    sync(newInterval);
	    
	    throw new TimeoutException("requestSample: request failed; instrument restarted");
	    
	}
	
	// UV ON message was found
	// Reschedule for next wake up
	// note that sample interval-preemption time is used here
	newInterval = _initialIntervalSecs * 1000;
	_logger.info("Instrument ON after " + (System.currentTimeMillis()-t0)
		     + " secs, rescheduling for " + (newInterval / 1000)
		     + " seconds.");
	
	sync(newInterval);
	
    }

    /**
     * Called by the framework to fetch the sample data returned from the
     * instrument and copy to sample buffer. 
     * 
     * @param sample
     * 
     * @return sample size (bytes)
     * @exception TimeoutException
     *                sample time exceeded
     * @exception IOException
     *                error in input stream
     * @exception Exception
     *                Packet exceeded packet length indicator (should really be
     *                in validateSample(..))
     */
    protected int readSample(byte[] sample) throws TimeoutException,
						   IOException, Exception {

	// get sample data
	int byteCount = readIsusRecords(_fromDevice, 
					sample,
					(READRECORD_TIMEOUT_MILLIS+1000*_attributes.readAdjustSec),
					60000);

	_logger.debug("Data captured, byte count = " + byteCount + " sample="
		      + new String(sample));

	// return sample data size
	return byteCount;
    }
   
   /**
     * Called by the readSample to fetch the sample data returned from the
     * instrument and copy to sample buffer. 
     * 
     * @param instream
     * @param sample
     * @param totalTimeoutMSec total time allowed to find data
     * @param lineTimeoutMSec  time allowed to obtain a single line
     * 
     * @return    sample size (bytes)
     * @exception TimeoutException
     *                sample time exceeded
     * @exception IOException
     *                error in input stream
     * @exception Exception
     */

    protected int readIsusRecords(InputStream instream, 
				  byte[] sample,
				  long totalTimeoutMSec, 
				  long lineTimeoutMSec) 
	throws TimeoutException, IOException, Exception {

	// flush input stream
	_fromDevice.flush();

	// mark entry time for debugging
	long t0=System.currentTimeMillis();

	// end of line marker
	final String LINE_TERMINATOR = "\r";

	// line capture buffer
	int bufSize=(2*MAX_FRAME_SIZE);
	byte[] buf= new byte[bufSize];

	// elapsed time for debugging
	long elapsed=0L;

	// data String
	String sData="";

	// number of light records found
	int lRecords=0;
	// number of dark records found
	int dRecords=0;
	// number of lines for debugging
	int lines=0;

	// start looking for lines
	while(elapsed<totalTimeoutMSec){

	    // clear line capture buffer
	    for(int i=0;i<buf.length;i++)
		buf[i]='\0';

	    // read a line
	    try{
		_logger.debug("readISUSRecords: reading line tmout="+lineTimeoutMSec);
		StreamUtils.readUntil(_fromDevice,
				      buf,
				      LINE_TERMINATOR.getBytes(),
				      lineTimeoutMSec);
		elapsed=System.currentTimeMillis()-t0;
		_logger.debug("readISUSRecords: returned line after "+elapsed+" ms");
		lines++;
	    }catch(TimeoutException e){
		_logger.debug("readISUSRecords: timed out, processing data; time="+elapsed+" ms");
	    }

	    // put the line into a String for
	    // easy handling
	    String sLine=new String(buf);
	    sLine.trim();
	    //_logger.debug("readISUSRecords: line: "+sLine);

	    // check for dark record
	    if(sLine.indexOf("NDF")>=0){
		// trim to size and add to string
		StringTokenizer st=new StringTokenizer(sLine,"\r\n");
		while(st.hasMoreTokens()){
		    String s=st.nextToken();
		    if(s.indexOf("NDF")>=0){
			sData+=s+"\r\n";
			break;
		    }
		}
		// bump the dark record count
		dRecords++;
	    }

	    // check for light record
	    if(sLine.indexOf("NLF")>=0){
		// trim to size and add to string
		StringTokenizer st=new StringTokenizer(sLine,"\r\n");
		while(st.hasMoreTokens()){
		    String s=st.nextToken();
		    if(s.indexOf("NLF")>=0){
			sData+=s+"\r\n";
			break;
		    }
		}
		// bump the light record count
		lRecords++;
	    }

	    // if we've found all the records expected, break out 
	    // of the main loop
	    if( (lRecords>=_attributes.lightRecords) && (dRecords>=_attributes.darkRecords)){
		break;
	    }

	    // detect and log unresponsive state
	    if(sLine.indexOf("***")>=0){
		// needs a power cycle
		_logger.error("readISUSRecords: *** detected; may need to power cycle");
	    }
	}
		
	_logger.debug("readISUSRecords: "+lines+" lines "+lRecords+" light records "+dRecords+" dark records found");
	//_logger.debug("readISUSRecrods: sData "+sData);

	// copy the String data into the sample buffer
	// used by the service framework
	System.arraycopy(sData.getBytes(), 0, sample, 0, sData.length());

	return sData.length();
    }

    // still used by initializeInstrument; TODO: eliminate
    private String readLine(InputStream instream, long timeout)
	throws TimeoutException, IOException {
	final int EOF = -1;
	final int CR = 0x0d;
	final int LF = 0x0a;
	final int MAX_BUFFER_SIZE = MAX_FRAME_SIZE + 10;
	int ch;
	byte[] buf = new byte[MAX_BUFFER_SIZE];
	int i = 0;

	long t0 = System.currentTimeMillis();
	int starCount = 0;
	while (i < MAX_BUFFER_SIZE) {

	    if ((System.currentTimeMillis() - t0) > timeout)
		throw new TimeoutException("readline()"); // There is no unique
	    // End Of Sample
	    // delimiter - timeout

	    if (instream.available() == 0) {
		sleep(50);
		continue; // No more characters for now
	    }
	    //_logger.debug("readLine checking available:
	    // "+instream.available());
	    if (instream.available() > 0) {
		//_logger.debug("readLine reading...");
		ch = instream.read();
		//_logger.debug("readLine read "+ch);
		if (ch == CR)
		    break; // We have a complete line
		else if (ch == LF)
		    continue; // Ignore line feeds
		buf[i++] = (byte) ch;
		t0 = System.currentTimeMillis();
		// if the instrument is powered off and on again without
		// sufficient
		// delay between, the instrument goes into a state where it
		// spews an
		// endless stream of asterisks at 0.5 Hz...oh, NICE!
		if (ch == '*')
		    if (starCount++ > 5)
			throw new IOException(
					      "Instrument improperly power cycled");
	    }
	}
	return new String(buf, 0, i);
    }

    /**
     * Not implemented
     * 
     * @return Instrument metadata (byte array)
     */
    protected byte[] getInstrumentMetadata() {

	// This method should return any available configuration
	// information that is available from the instrument

	return "Not supported".getBytes();
    }//end getInstrumentMetadata()

    /**
     * Not implemented. Samples are locally timestamped
     * 
     * @param t
     */
    public void setClock(long t) {
	return;
    }

    /** PRIVATE METHODS * */

    private void sendSequence(String cmdSequence) throws Exception {
	StringTokenizer strtok = new StringTokenizer(cmdSequence, ",");

	while (strtok.hasMoreTokens()) {
	    String cmd = strtok.nextToken();
	    String rsp = strtok.nextToken();
	    sendCommand(cmd, rsp);
	}
    }

    /**
     * Method to send commands to the Workhorse. sendCommand makes every attempt
     * to communicate with the unit by a process of resetting, flushing input
     * buffer and resending.
     * 
     * Note: Trailing '\r' is automatically added to command string.
     * 
     * @param cmd
     *            Command string to send
     * 
     * @exception Exception
     *                thrown if the method fails to send the command.
     */
    private void sendCommand(String cmd, String rsp) throws Exception {
	for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
	    // Prepare to send message
	    try {
		_fromDevice.flush();
		_toDevice.write(mkCmd(cmd));
		StreamUtils.skipUntil(_fromDevice, rsp.getBytes(),
				      RESPONSE_TIME_MILLIS, MAX_RESPONSE_BYTES);
		return;
	    } catch (IOException e) { // This is bad - not sure a retry would
		// help
		_logger.error("IOException " + e);
		throw new Exception("sendCommand(" + cmd
				    + ") - Stream I/O failure");
	    } catch (NullPointerException e) { // This is bad - a retry isn't
		// going to make this one batter
		_logger.error("Null Pointer Exception " + e);
		throw new Exception("sendCommand(" + cmd + ") - Null pointer");
	    } catch (TimeoutException e) {
		_logger.error("Timeout Exception: " + e + "Cmd=" + cmd);
		// incTimeoutCount(); Don't include these as they occur often
		// when resyncing
	    } catch (Exception e) { // Probably exceeded max bytes - bad command
		// maybe
		_logger.error("Exception " + e);
		//incBadResponseCount();
	    }
	    // Reset interface using "\r"
	    _toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
	    sleep(500);
	}
	//incRetryExceededCount();
	throw new Exception("sendCommand(" + cmd
			    + ") - Maximum retries attempted");
    }

    /**
     * sends a '\r' to the ISUS and waits for a prompt "15 secs" message This
     * works when the ISUS is either in command (menu) mode or sleep mode.
     * 
     * @exception Exception
     */
    private void enterCommandMode() throws Exception {
	for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
	    try {
		_fromDevice.flush();
		_toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
		StreamUtils.skipUntil(_fromDevice, "15 secs".getBytes(),
				      PROMPT_RESPONSE_TIME_MILLIS, MAX_RESPONSE_BYTES);
		_toDevice.write("M".getBytes());
		_logger.debug("got command mode");
		return;
	    } catch (IOException e) { // This is bad - not sure a retry would
		// help
		_logger.error("IOException " + e);
		throw new Exception("StreamUtils.skipUntil() " + e);
	    } catch (NullPointerException e) { // This is bad - a retry isn't
		// going to make this one batter
		_logger.error("Null Pointer Exception " + e);
		throw new Exception("StreamUtils.skipUntil() " + e);
	    } catch (TimeoutException e) {
	    } catch (Exception e) { // Probably exceeded max bytes - bad command
		// maybe
		_logger.error("Exception " + e);
	    }
	}
	throw new Exception("enterCommandMode() - Maximum retries attempted");
    }

    /**
     * restarts sampling and exits command mode
     * 
     * @exception IOException
     * @exception Exception
     */
    private void exitCommandMode() throws IOException, Exception {
	for (int i = 0; i < MAX_MENU_LEVELS; i++) {
	    _fromDevice.flush();
	    _toDevice.write(COMMAND_CONTROL_C.getBytes());
	    sleep(1000);
	    if (_fromDevice.available() == 0)
		break;
	}
    }

    /**
     * Method for suspending task.
     * 
     * @param msDelay
     *            number of milliseconds to delay task
     */
    private void sleep(int msDelay) {
	for (;;) {
	    try {
		Thread.sleep(msDelay);
		return;
	    } catch (InterruptedException e) {

	    }
	}
    }

    /**
     * Utility method to construct a message of the form: -
     * <p>
     * <BLOCKQUOTE>
     * 
     * <PRE>
     * 
     * "cmd + \r" </BLOCKQUOTE>
     * 
     * </PRE>
     * 
     * and returns this as a byte array for transmission
     * 
     * @param cmd
     *            basic command string to construct
     * 
     * @return byte array of command
     */
    private byte[] mkCmd(String cmd) {
	return (new String(cmd + "\r")).getBytes();
    }

    /**
     * Returns the property value from the ISUS response
     * 
     * @param response
     *            response string return from ISUS
     * 
     * @return string representing the property (rhs of ='s)
     * @exception InvalidPropertyException
     */
    private String getProperty(String response) throws InvalidPropertyException {
	StringTokenizer tkzResponse = new StringTokenizer(response, " \r",
							  false);
	while (tkzResponse.countTokens() > 0) {
	    String property = tkzResponse.nextToken();
	    if (property.equals("=")) {
		property = tkzResponse.nextToken();
		return property;
	    }
	}
	throw new InvalidPropertyException("Invalid ISUS property");
    }

    /** For some reason, this method is required by the framework */
    public int test() {
	return 0;
    }
    
    /** Configurable ISUS attributes */
    class Attributes extends InstrumentServiceAttributes {
	
	/**
	 * Guard interval before the expected sample transmission.
	 */
	int preemptionSec = 20;
	
	/**
	 * Adjustment to timeout in requestSample
	 */
	int requestAdjustSec = 0;
	
	/**
	 * Adjustment to timeout in readSample
	 */
	int readAdjustSec = 0;
	
	/**
	 * Instrument baud rate (so it shows up in properties
	 */
	int baud = 38400;

	/**
	 * Expected light records
	 */
	int lightRecords = 3;
	
	/**
	 * Expected dark records
	 */
	int darkRecords = 1;
	
	
	/** Constructor, with required InstrumentService argument */
	Attributes(DeviceServiceIF service) {
	    super(service);
	}
	
	/**
	 * Throw InvalidPropertyException if any invalid attribute values found
	 */
	public void checkValues() throws InvalidPropertyException {
	    
	    if (preemptionSec <= 0) {
		throw new InvalidPropertyException(preemptionSec
						   + ": invalid preemptionTime. " + "must be > 0");
	    }
	    if (lightRecords < 0) {
		throw new InvalidPropertyException(lightRecords
						   + ": invalid lightRecords. " + "must be >= 0");
	    }
	    if (darkRecords < 0) {
		throw new InvalidPropertyException(darkRecords
						   + ": invalid darkRecords. " + "must be >= 0");
	    }
	    if (baud <= 0) {
		throw new InvalidPropertyException(baud
						   + ": invalid baud. " + "must be > 0");
	    }
	}
    }

}


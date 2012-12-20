/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.garmin;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.mbari.siam.distributed.devices.GPS;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.ByteUtility;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InitializeException;

class NMEAString {

	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(NMEAString.class);

	static final int GPRMC = 0; // Recommended Min Specific GPS/Transit (RMC)

	static final int GPRMC_FIELDS = 13;

	static final int GPRMC_TYPE = 0;

	static final int GPRMC_UTC_FIX_TIME = 1;

	static final int GPRMC_STATUS = 2;

	static final int GPRMC_LAT = 3;

	static final int GPRMC_LAT_HEMISPHERE = 4;

	static final int GPRMC_LON = 5;

	static final int GPRMC_LON_HEMISPHERE = 6;

	static final int GPRMC_SPEED_OVER_GROUND = 7;

	static final int GPRMC_COURSE_OVER_GROUND = 8;

	static final int GPRMC_UTC_FIX_DATE = 9;

	static final int GPRMC_MAGNETIC_VARIATION = 10;

	static final int GPRMC_MAGNETIC_VARIATION_DIRECTION = 11;

	static final int GPRMC_MODE = 12;// only used if NMEA 2.3 is active

	static final int GPRMC_CHECKSUM = 12;

	static final int GPGSV = 1;// Satellites in View (GSV)

	static final int GPGSV_FIELDS = 21;

	static final int GPGSV_TYPE = 0;

	static final int GPGSV_TOTAL_SENTENCES = 1;

	static final int GPGSV_CURRENT_SENTENCE = 2;

	static final int GPGSV_TOTAL_SATELLITES = 3;

	static final int GPGSV_PRN0 = 4;

	static final int GPGSV_ELEV0 = 5;

	static final int GPGSV_AZIM0 = 6;

	static final int GPGSV_SN0 = 7;

	static final int GPGSV_PRN1 = 8;

	static final int GPGSV_ELEV1 = 9;

	static final int GPGSV_AZIM1 = 10;

	static final int GPGSV_SN1 = 11;

	static final int GPGSV_PRN2 = 12;

	static final int GPGSV_ELEV2 = 13;

	static final int GPGSV_AZIM2 = 14;

	static final int GPGSV_SN2 = 15;

	static final int GPGSV_PRN3 = 16;

	static final int GPGSV_ELEV3 = 17;

	static final int GPGSV_AZIM3 = 18;

	static final int GPGSV_SN3 = 19;

	static final int GPGSV_CHECKSUM = 20;

	static final int PGRMT = 2;// Proprietary Garmin Sensor Status (PGRMT)

	static final int PGRMT_FIELDS = 10;

	static final int PGRMT_TYPE = 0;

	static final int PGRMT_PRODUCT = 1;

	static final int PGRMT_ROM_CHKSUM = 2;

	static final int PGRMT_RX_FAILURE = 3;

	static final int PGRMT_STORED_DATA_LOST = 3;

	static final int PGRMT_REALTIME_CLOCK_LOST = 4;

	static final int PGRMT_OSC_DRIFT = 5;

	static final int PGRMT_DATA_COLLECTION = 6;

	static final int PGRMT_BOARD_TEMP = 7;

	static final int PGRMT_BOARD_CFG = 8;

	static final int PGRMT_CHECKSUM = 9;

	int _numberOfFields = 0;

	String[] fields;

	String theString;

	public NMEAString(int n) {
		if (n > 0) {
			_numberOfFields = n;
			fields = new String[_numberOfFields];
			for (int i = 0; i < n; i++)
				fields[i] = "";

			clearFields(0, n);
		}
		theString = "";
	}

	public NMEAString(String s, int n) {
		if (n > 0) {
			_numberOfFields = n;
			fields = new String[_numberOfFields];
			parseString(s);
		}
	}

	public NMEAString(byte[] b, int n) {
		if (n > 0) {
			_numberOfFields = n;
			fields = new String[_numberOfFields];
			parseByteArray(b);
		}
	}
    
    public int indexOf(String str){
	return theString.indexOf(str);
    }

	public void clearFields(int start, int end) {
		if (start < 0)
			return;
		int i = 0;

		for (i = start; (i < end) && (i < _numberOfFields); i++)
			fields[i] = "";
		return;
	}

	public void clearAllFields() {

		for (int i = 0; i < _numberOfFields; i++)
			fields[i] = "";
		theString = "";
		return;
	}

	public int parseString(String s) {

		StringTokenizer lt = new StringTokenizer(s, "\r\n");

		String line;
		theString = null;
		clearAllFields();

		if (lt.hasMoreTokens())
			line = lt.nextToken();
		else {
			return 0;
		}

		StringTokenizer st = new StringTokenizer(line, ",", false);
		int i = 0;

		theString = line;

		// replace ",," with ",-," so that tokenizer won't jump over missing
		// fields
		int x = 0;
		while ((x = line.indexOf(",,")) >= 0) {
			String before = line.substring(0, x);
			String after = line.substring((x + 2));
			line = before + ",-," + after;
		}

		st = new StringTokenizer(line, ",", false);

		while (st.hasMoreTokens()) {
			fields[i] = st.nextToken();
			if ((++i) >= _numberOfFields)
				break;
		}
		return i;
	}

	public int parseByteArray(byte[] b) {
		if (b.length <= 0)
			return 0;
		return parseString(new String(b));
	}

	public int getNumberOfFields() {
		return _numberOfFields;
	}

	public String getField(int f) {
		return fields[f];
	}

	public void setField(int f, String s) {
		fields[f] = s;
	}

	public String toString() {

		return theString;
	}

	public byte[] getBytes() {
		return toString().getBytes();
	}

	public int length() {
		return toString().length();
	}

	public static int lookupNumberOfFields(int id) {
		int n = 0;
		switch (id) {
		case NMEAString.GPRMC:
			n = GPRMC_FIELDS;
			break;
		case NMEAString.GPGSV:
			n = GPGSV_FIELDS;
			break;
		case NMEAString.PGRMT:
			n = PGRMT_FIELDS;
			break;
		default:
			_log4j
					.error("Unsupported Message type " + id
							+ "; returning null");
		}
		return n;
	}

	public static String lookupMessageName(int id) {
		String n = null;
		switch (id) {
		case NMEAString.GPRMC:
			n = "GPRMC";
			break;
		case NMEAString.GPGSV:
			n = "GPGSV";
			break;
		case NMEAString.PGRMT:
			n = "PGRMT";
			break;
		default:
			_log4j
					.error("Unsupported Message type " + id
							+ "; returning null");
		}
		return n;
	}

}// end class NMEAString

/**
 * Service implementation for several models of Garmin GPS, including GPS16,
 * GPS25.
 */

public class GarminGPS extends PolledInstrumentService implements GPS {

        static final byte[] GET_CONFIG_CMD0 = "$PGRMCE\r\n".getBytes();
        static final byte[] GET_CONFIG_CMD1 = "$PGRMCE1\r\n".getBytes();

        final static int WARMUP_DELAY = 2000;

	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(GarminGPS.class);


        static final int GPS_BAUD = 4800;

	static final int GPS_DATA_BITS = SerialPort.DATABITS_8;

	static final int GPS_STOP_BITS = SerialPort.STOPBITS_1;

	static final int GPS_PARITY = SerialPort.PARITY_NONE;

	boolean _warmupDone = false;

	protected Attributes _attributes = new Attributes(this);

	/** Define diagnostic record type */
	static final long RECORDTYPE_DIAGNOSTIC = RECORDTYPE_DEFAULT + 1;

	public GarminGPS() throws RemoteException {

		_warmupDone = false;

		try {
			setSampleTimeout(10000);// timeout for getting one fix
			setMaxSampleTries(15);

		} catch (RangeException e) {
			_log4j.error(new String(getName()) + " caught exception " + e);
		}
	}// end Garmin25HVS_GPS()

	/** Return initial value for instrument's "prompt" character. */
	protected byte[] initPromptString() {
		return "\r\n".getBytes();
	}

	/** Return initial value for instrument's sample terminator */
	protected byte[] initSampleTerminator() {
		return "\r\n".getBytes();
	}

	/** Return initial value of DPA current limit. */
	protected int initCurrentLimit() {
		return 1000;
	}

	/** Return initial value of instrument power policy. */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/** Return initial value of instrument power policy. */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/** Return initial value of instrument startup time in millisec. */
	protected int initInstrumentStartDelay() {
	    return WARMUP_DELAY;
	}

	/**
	 * Return initial value for maximum number of bytes in a instrument data
	 * sample.
	 */
	protected int initMaxSampleBytes() {
		return 512;
	}

	public void initializeInstrument()
	throws InitializeException, Exception {
		// Turn on power (needed if POWER_WHEN_SAMPLING)
		managePowerWake();

		// Set initial LAT,LON, ALT
		// GPS will only have GPGGA output after initializeGPS()
		_log4j.debug("initializeInstrument(): Setting Up GPS");
		try {
			initializeGPS();
		} catch (Exception e) {
			_log4j.error("initialize() caught Exception initializeGPS() ", e);
			throw e;
		}

		// Put back in lowest power state
		managePowerSleep();

	}// end initializeInstrument()

	/** One time initialization, done at system power up */
	private void initializeGPS() throws IOException, Exception {

		//According to the local GPS guru,
		//it's better NOT to send ANY initialization to the GPS.
		//It's factory defaults lead to the best fix times and power
		//management. Also, the GPRMC string provides everything
		//we need in one string.

		// Wait to warm up
		try {
			Thread.sleep(WARMUP_DELAY);
		} catch (InterruptedException e) {
		}

		disableAllNMEA();
		setNMEAMessage("GPRMC,1");
		setNMEAMessage("GPGSV,1");
		setNMEAMessage("PGRMT,1");
		_warmupDone = false;

	} // end setupGPS()

	public void cyclePower(int delay) {
		_log4j.debug("Cycling Power OFF");
		_instrumentPort.disconnectPower();

		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}

		_log4j.debug("Cycling Power ON");
		_instrumentPort.connectPower();
		return;
	}

	public boolean searchSky(long timeout, NMEAString theMessage, byte[] sample)
			throws Exception {
		long now = System.currentTimeMillis();
		while ((System.currentTimeMillis() - now) < timeout) {
			long minute = System.currentTimeMillis();
			_log4j.debug("searchSky() GPS warming up..."
					+ (timeout - (System.currentTimeMillis() - now))
					+ " remaining");

			while ((System.currentTimeMillis() - minute) < 60000) {
				//         Thread.yield();
				StopWatch.delay(50);
			}
			_log4j.debug("searchSky(): attempting fix");
			if (getFix(theMessage, sample))
				return true;
		}
		return false;
	}

	/**
	 * Enable/Disable a NMEA message
	 * 
	 * @param msg
	 *            String: "NMEA_TYPE,1|2|3"
	 */
	public void setNMEAMessage(String msg) throws Exception {
		String m = "$PGRMO," + msg + "\r\n";
		_log4j.debug("setNMEAMessage(): setting " + m);
		try {
			_toDevice.write(m.getBytes());
			StreamUtils.skipUntil(_fromDevice, m.getBytes(),
					getSampleTimeout(), 0);
		} catch (Exception e) {
			_log4j.error("setNMEAMessage():  caught Exception ", e);
		  throw e;
		}
	}

	public void sync() {
		// Synchronize to record end...
		// Note: uses prompt as record terminator
		_log4j.debug("getNextMessage(): Synchronizing...");

		try {
			// Very important to flush first!
			_fromDevice.flush();
			StreamUtils.skipUntil(_fromDevice, getPromptString(),
					getSampleTimeout(), 0);
		} catch (Exception e) {
			_log4j.error("sync() caught ", e);
		}
	}

	/**
	 * Enable a NMEA message
	 * 
	 * @param msg
	 *            String: "NMEA_TYPE"
	 */
	public void enableNMEAMessage(String msg) throws Exception {
		setNMEAMessage((msg + ",1"));
	}

	/**
	 * Disable a NMEA message
	 * 
	 * @param msg
	 *            String: "NMEA_TYPE"
	 */
	public void disableNMEAMessage(String msg) throws Exception {
		setNMEAMessage((msg + ",2"));
	}

	/** Turn off all all NMEA output */
	public void disableAllNMEA() throws Exception {

		_log4j.debug("disableAllNMEA(): Turning all NMEA messages OFF ");
		setNMEAMessage(",2");
	}// end disableAllNMEA()

	/** Prepare to read sample (per sample initialization) */
	protected void requestSample() {	
	sync();
		// Garmin GPS sometimes locks up
		// when config is done on the fly...
		// Best to set messages at start up and leave it alone

		// turn on GPS messaging
		//_log4j.debug("requestSample(): Enabling GPRMC messages");
		//setNMEAMessage("GPRMC,1");

		_log4j.debug("requestSample(): done");

	} // end requestSample()

	/**
	 * Find and validate a specific NMEA String
	 * 
	 * @param sample
	 *            Byte array to read data into
	 * @param retries
	 *            Number of times to attempt to get fix
	 * @param timeout
	 *            Milliseconds to wait for each read
	 */
	private void getMessage(String type, NMEAString msg, byte[] sample,
			long skipTimeout, long readTimeout) throws Exception {

		int bytesRead = 0;
		ByteUtility.fillByteArray(sample, '\0');
		try {
			StreamUtils.skipUntil(_fromDevice, type.getBytes(), skipTimeout, 0);
			bytesRead = StreamUtils.readUntil(_fromDevice, sample,
					getPromptString(), readTimeout);
			msg.parseByteArray(ByteUtility.concatByteArrays(type.getBytes(),
					sample));
			_log4j.debug("getMessage(): looking for " + type + " got " + msg
					+ " (" + bytesRead + "," + msg.length() + ") bytes");
			return;
		} catch (Exception e) {
			_log4j.debug("getMessage() caught ", e);
		}

		return;
	}// end getMessage()

	protected boolean getFix(NMEAString theMessage, byte[] sample)
			throws Exception {

		long now = System.currentTimeMillis();
		_log4j.debug("getFix() - call sync()");
		sync();
		now = System.currentTimeMillis();
		while (((System.currentTimeMillis() - now) < getSampleTimeout())) {
			_log4j.debug("getFix() - call getMessage()");
			getMessage("$GPRMC", theMessage, sample, getSampleTimeout(),
					getSampleTimeout());
			_log4j.debug("getFix() - check theMessage()");
			if (theMessage != null) {
				if (theMessage.getField(0).equals("$GPRMC"))
					if (theMessage.getField(NMEAString.GPRMC_STATUS)
							.equals("A")) {
						_log4j.debug("Got valid GPRMC message");
						return true;
					}
			} else {
				_log4j.error("getFix() - got null message!");
			}

		}
		return false;
	}

	protected int readSample(byte[] sample) throws Exception {

		int bytesRead = 0;

		NMEAString theMessage = null;

		// Get a valid NMEA string
		_log4j.debug("readSample(): acquiring fix...");

		boolean gotValidFix = false;

		theMessage = new NMEAString(25);
		long now = System.currentTimeMillis();

		gotValidFix = getFix(theMessage, sample);

		// Still no fix?
		// Try a long warmup (15 minutes),
		// or a power cycle if that doesn't work
		if (!gotValidFix)
			if (!_warmupDone) {
				gotValidFix = 
				    searchSky(_attributes.fixTimeoutMsec, 
					      theMessage, sample);

				_warmupDone = true;
				if (!gotValidFix)
					gotValidFix = getFix(theMessage, sample);

				if (!gotValidFix) {
					cyclePower(10000);
					initializeGPS();// Sets _warmupDone=false;
					gotValidFix = getFix(theMessage, sample);
				}
			}

		if (gotValidFix) {
			// Form reply
			ByteUtility.fillByteArray(sample, '\0');
			for (int i = 0; i < theMessage.length(); i++)
				sample[i] = theMessage.getBytes()[i];
			setRecordType(RECORDTYPE_DEFAULT);
			return theMessage.length();
		} else {

			_log4j.debug("Could not acquire fix; getting Status Info...");

			// Set up array of 4 NMEAStrings (3 GSV and 1 PGRMT)
			NMEAString[] status = new NMEAString[4];
			for (int i = 0; i < 4; i++)
				status[i] = new NMEAString(25);

			// Get 3 GPGSV (satellites in view) messages
			sync();
			for (int i = 0; i < 3; i++) {
				getMessage("$GPGSV", status[i], sample, getSampleTimeout(),
						getSampleTimeout());
			}

			// Get PGRMT (sensor status) message (sent 1/minute, regardless of
			// baud rate)
			sync();
			getMessage("$PGRMT", status[3], sample, 75000, getSampleTimeout());

			// Form reply
			String retString = "\n";
			for (int j = 0; j < 4; j++)
				if (status[j] != null && status[j].indexOf("$")>=0)
					retString += (status[j] + "\r\n");

			if (retString.indexOf("$")<0)
			    throw new Exception("Could not get valid GPS fix or device status information");

			for (int i = 0; i < retString.length(); i++)
				sample[i] = (retString.getBytes())[i];

			_log4j.debug("Returning status info:\n" + retString);
			setRecordType(RECORDTYPE_DIAGNOSTIC);
			return retString.length();
		}

	} // end readSample()


    /** Get configuration metadata from GPS. */
    protected byte[] getInstrumentStateMetadata() {

	int nBytes = 0;
	String strBuf = "";
	String err = "";
	byte[] buf = new byte[4096];

	try {

	    // Turn off output
	    disableAllNMEA();

	    _fromDevice.flush();

	    _log4j.debug("Invoking '" + new String(GET_CONFIG_CMD0));

	    _toDevice.write(GET_CONFIG_CMD0);

	    nBytes = 
		StreamUtils.readUntil(_fromDevice, buf,
				      getPromptString(),
				      getSampleTimeout());

	    strBuf += new String(buf, 0, nBytes) + "\n";

	    _log4j.debug("Invoking '" + new String(GET_CONFIG_CMD1));

	    _toDevice.write(GET_CONFIG_CMD1);

	    nBytes = 
		StreamUtils.readUntil(_fromDevice, buf,
				      getPromptString(),
				      getSampleTimeout());

	    strBuf += new String(buf, 0, nBytes) + "\n";

	    setDefaultNMEAMessages();

	    return strBuf.getBytes();

	} catch (TimeoutException e) {
	    err = "TimeoutException collecting metadata: " + e;
	    _log4j.error(err);
	} catch (Exception e) {
	    err = "Exception collecting metadata: " + e;
	    _log4j.error(err);
	}
	try {
	    setDefaultNMEAMessages();
	}
	catch (Exception e) {
	    err = "Exception while setting messages: " + e;
	}

	return err.getBytes();
    }

	/** Garmin does not have an internal clock? */
	public void setClock(long t) {
		return;
	}

	/** Self-test not implemented. */
	public int test() {
		return Device.OK;
	}

	/** Return parameters to use on serial port. */
	public SerialPortParameters getSerialPortParameters()
			throws UnsupportedCommOperationException {

	    return new SerialPortParameters(GPS_BAUD,
					    GPS_DATA_BITS, GPS_PARITY,
					    GPS_STOP_BITS);
	}

	/** Return specifier for default sampling schedule. */
	protected ScheduleSpecifier createDefaultSampleSchedule()
			throws ScheduleParseException {

		// Sample every 60 seconds by default
		return new ScheduleSpecifier(60000);
	}


    /** Get most recent NMEA string from GPS. */
    public byte[] getLatestNMEA() throws NoDataException {
	SensorDataPacket packet = getLastSample();
	return packet.dataBuffer();
    }

    /** Set default NMEA messages. */
    public void setDefaultNMEAMessages() throws Exception {

	setNMEAMessage("GPRMC,1");
	setNMEAMessage("GPGSV,1");
	setNMEAMessage("PGRMT,1");
    }


    /** 
     * Configurable Garmin service attributes.
     * @author oreilly
     *
	 */	
    class Attributes extends InstrumentServiceAttributes {
		
	Attributes(DeviceServiceIF service) {
	    super(service);
	}

	/** First fix timeout; ~15 minutes by default. */
	int fixTimeoutMsec = 900000;

	void setAttributeCallback(String name) {
	    // Could do something here...
	}
    }

}

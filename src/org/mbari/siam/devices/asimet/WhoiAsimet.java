/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/** 
 * @Title Generalized WhoiAsimet Instrument Driver
 * @author Martyn Griffiths
 * @version 1.0
 * @date 6/22/2003
 * 
 * REVISION HISTORY:
 * $Log: WhoiAsimet.java,v $
 * Revision 1.3  2012/12/17 21:33:23  oreilly
 * added copyright header
 *
 * Revision 1.2  2009/02/19 01:17:31  oreilly
 * make Attributes class and constructor protected
 *
 * Revision 1.1  2008/11/04 22:17:56  bobh
 * Initial checkin.
 *
 * Revision 1.1.1.1  2008/11/04 19:02:04  bobh
 * Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
 *
 * Revision 1.26  2006/06/03 17:18:57  oreilly
 * extends PolledInstrumentService
 *
 * Revision 1.25  2006/01/05 00:16:07  salamy
 * Added Barometric Pressure Instrument Service to WhoiAsimet.java
 *
 * Revision 1.24  2005/01/11 02:19:28  oreilly
 * More lenient treatment of errors during getInstrumentState()
 *
 * Revision 1.23  2004/10/15 20:12:22  oreilly
 * utilizes ServiceAttributes framework
 *
 * Revision 1.22  2004/08/18 22:09:16  oreilly
 * use _toDevice.write(). Use 'I' to get instrument metadata
 *
 * Revision 1.21  2004/02/28 01:05:55  oreilly
 * Removed instrument start delay; now handled by base class
 *
 * Revision 1.20  2004/02/25 00:22:51  oreilly
 * Added newline after instrument state metadata bytes
 *
 * Revision 1.19  2004/02/24 00:43:43  oreilly
 * Trim excess null bytes from metadata string
 *
 * Revision 1.18  2004/02/23 21:35:50  oreilly
 * Fixed and simplified instrument state metadata method
 *
 * Revision 1.17  2004/02/20 17:42:35  oreilly
 * Added support for WND sensor
 *
 * Revision 1.16  2004/02/05 18:54:52  oreilly
 * Fixed comments
 *
 * Revision 1.15  2004/02/03 20:35:19  oreilly
 * Removed <cr>'s
 *
 * Revision 1.14  2003/10/20 17:13:03  martyn
 * Changed typo ";;" (line 394) to";" - creates problems w/ some compilers
 *
 * Revision 1.13  2003/10/16 22:03:53  martyn
 * Removed class  moos.deployed.TimeoutException
 * Changed all references to this class to class org.mbari.isi.interfaces.TimeoutException
 *
 * Revision 1.12  2003/10/10 23:41:21  martyn
 * Converted DebugMessages to log4j messages
 *
 * Revision 1.11  2003/10/06 23:05:06  mrisi
 * modified to use StreamUtils.skipUntil
 *
 * Revision 1.10  2003/10/03 23:08:17  mrisi
 * switched readUntil to moos.utils.StreamUtils readUntil
 *
 * Revision 1.9  2003/09/16 00:56:26  martyn
 * Now uses new property "sensorType"  to configure WhoiAsimet driver
 *
 * Revision 1.8  2003/09/15 23:38:47  martyn
 * Improved AsciiTime utility class
 * Updated all drivers to use revised class.
 * Removed registryName service property
 * Moved service property keys to ServiceProperties
 *
 * Revision 1.7  2003/07/24 02:35:58  martyn
 * Updated to reflect recent changes to the underlying property retrieval code
 *
 * Revision 1.6  2003/07/11 23:11:29  martyn
 * Changed some system.out messages to DebugMessage
 *
 * Revision 1.5  2003/07/11 23:06:31  martyn
 * Changed sampling timeout from 2 seconds to 3 seconds
 *
 * Revision 1.4  2003/07/10 18:22:56  martyn
 * Increased current limit to 1000mA to overcome problem with right hand
 * channel going into current limit with 500mA setting.
 *
 * Revision 1.3  2003/07/09 22:22:52  martyn
 * Adjusted schedule for ASIMET sampling to 10 minutes
 * Removed annoying debug messages from ASIMET driver
 * Reduced sampling timeout to 2 seconds
 *
 * Revision 1.2  2003/07/09 01:43:44  martyn
 * Working version of ASIMET drivers
 * Checkin includes:
 * 1. A slight modification to PortManager (reversal of setProperties() and initializeInstrument() methods)
 * so properties are setup before the driver is initialized.
 * 2. Official instrument id's in puck properties.
 *
 * Revision 1.1  2003/07/04 02:56:56  martyn
 * First cut of ASIMET class - supports Short and Long Wave Radiometers.
 * Note: registryName and moduleAddress properties must being correctly set in puck:-
 * registryName = ASIMET_SWR or ASIMET_LWR
 * moduleAddress = 01 (typically)
 *
 * 
 */

/** Adaptation for the new MOOS framework from code orginated by Bob Herlien */

package org.mbari.siam.devices.asimet;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
 * Implements SIAM service for all classes of WHOI Asimet instruments. The
 * follwing ASIMET instruments, available from WHOI, are supported by this
 * driver:
 * <ul>
 * <li>BPR Barometric Pressure
 * <li>HRH Relative Humidity and Air Temperature
 * <li>LWR Longwave Radiation
 * <li>PRC Precipitation
 * <li>SWR Shortwave Radiation
 * <li>WND Wind speed and Direction
 * </ul>
 * Only LWR and SWR have been tested with this version of the driver.
 * 
 * <p>
 * The initial intent was to make this a base class for Asimet instruments,
 * which could be subclassed for specific instruments. But we happily discovered
 * that the Asimet instruments are so uniform in their command structure that
 * this single driver can support any type of Asimet instrument, by simply
 * passing the type of instrument to the constructor.
 * 
 * WhoiAsimet has constructors with zero, one, two, or three strings, as
 * follows:
 * <ul>
 * <li>Instrument Type - any one of "BPR", "HRH", "LWR", "PRC", "SST", "SWR",
 * or "WND" is allowed. The default is "LWR".
 * <li>Instrument Number as a string ("01" through "32"). Default is "01"
 * <li>Communications Mode - "RS232" or "RS485". Default is "RS232"
 * </ul>
 * The default (no parameter) constructor is "LWR", "01", "RS232".
 * <p>
 * For configuration options, see comments in processConfigLine
 */

public class WhoiAsimet extends PolledInstrumentService implements Instrument {

	// ASIMET commands need to be built dynamically
	// because they use the logical address of the
	// instrument, which may change during the course
	// of a deployment if instruments are swapped.
	// ASIMET commands have the format

	// #TTTNNc

	// where
	// TTT : module type (BPR|HRH|LWR|PRC|SST|SWR|WND)
	// NN : module 'serial number' (address) (00-32 w/o repeaters)
	// c : command (may be more than one character)

	// type and serial number should not be hard
	// coded to enable the driver to accomodate
	// changes of instruments in the field and
	// so that one driver may be used for
	// different ASIMET modules

	static final int MAX_RETRIES = 5;

	static final int MAX_SENSOR_TYPES = 5;

	static final int CAL_QUERY_TIMEOUT = 15000;

	static final String COMMAND_PREFIX = "#";

	static final String ADDRESS_ACK_COMMAND = "A"; // Used by attention method

	// Fetch calibration constants from front end
	static final String QUERY_CALCON_COMMAND = "Q";

	// Collect id, serial # and calibration constants
	static final String GET_ID_COMMAND = "L";

	// Various ID, version, etc.
	static final String GET_ID2_COMMAND = "I";

	// Used when fetching constants from front end
	static final String OKACK_RESPONSE = "OK!";

	static final String RESPONSE_TERMINATOR = "\r\n\003";

	static final String RETURN_TO_KNOWN_STATE = "\r\r\r\r\r\r0";

	// log4j Logger
	static private Logger _logger = Logger.getLogger(WhoiAsimet.class);

	private ASIMETSensor _sensors[];

	private ASIMETSensor _sensor;

	// Note: ASIMET modules do not wait for end-of-line
	// to execute a command, and doesn't echo crlf

	// ASIMET Commands (may vary by sensor):

	// Data may be returned as (R)aw, (C)alibrated, or (B)oth
	// This should also be a driver configuration option.
	// We may also want to include options to select any
	// meta-data that might be returned.

	public WhoiAsimet() throws RemoteException {
		_sensors = new ASIMETSensor[MAX_SENSOR_TYPES];
		_sensors[0] = new SWaveRadiometer();
		_sensors[1] = new LWaveRadiometer();
		_sensors[2] = new RelHumidity();
		_sensors[3] = new Wind();
		_sensors[4] = new Pressure();
	}

	/** Specify ASIMET startup delay (millisec) */
	protected int initInstrumentStartDelay() {
		return 4000;
	}

	/** Specify ASIMET prompt string. */
	protected byte[] initPromptString() {
		return (RESPONSE_TERMINATOR).getBytes();
	}

	/** Specify sample terminator. */
	protected byte[] initSampleTerminator() {
		return "\r\n\003".getBytes();
	}

	/** Specify maximum bytes in raw ASIMET sample. */
	protected int initMaxSampleBytes() {
		return 512;
	}

	/** Specify current limit in increments of 120 mA upto 11880 mA. */
	protected int initCurrentLimit() {
		// 3-6mA quiescent 30mA sampling (WND 25mA)
		// http://frodo.whoi.edu/asimet/asimet_module_ops.html
		return 1000; // !! THIS NEEDS TO BE RESOLVED !!
	}

	/** Return initial value of instrument power policy. */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/** Return initial value of communication power policy. */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.WHEN_SAMPLING;
	}

	Attributes _attributes = new Attributes(this);

	/** Initialize the instrument. */
	protected void initializeInstrument() throws InitializeException, Exception {

		// Set inter-byte delay on bytes to instrument
		_toDevice.setInterByteMsec(400);

		_logger.debug("initializeInstrument(): initialize "
				+ new String(getName()));
		setSampleTimeout(3000);

		// Turn on DPA power/comms
		managePowerWake();

		if (!getAttention(MAX_RETRIES)) {
			throw new InitializeException(new String(getName())
					+ " failed to initialize");
		}
	}

	/** Request a data sample from instrument. */
	protected void requestSample() throws TimeoutException, Exception {
		// Verify connection...
		_logger.debug("Verifying Connection...");
		getAttention(MAX_RETRIES);

		// Get data sample
		_logger.debug("Requesting Sample...");
		_toDevice.write(_sensor.mkCmd(_sensor.getDataCmd()));
	}

	/** Return metadata. */
	protected byte[] getInstrumentStateMetadata() {

		// This method should return any available configuration
		// information that is available from the instrument
		// Create temporary buffers

		// Verify connection...
		if (!getAttention(MAX_RETRIES)) {
			_logger.error("getAttention() failed - ");
		}

		int nBytes = 0;
		String strBuf = "";
		String err = "";

		byte[] buf = new byte[4096];
		// Get config info...
		if (_sensor.hasVOSHPS()) {

		    try {
			_fromDevice.flush();
			// First query VOSHPS for calibration data
			_logger.debug("Fetch calibration info from front end...");
			_toDevice.write(_sensor.mkCmd(QUERY_CALCON_COMMAND));
			// This takes a few seconds to acquire...
			nBytes = 
			    StreamUtils.readUntil(_fromDevice, buf,
						  "!".getBytes(),
						  CAL_QUERY_TIMEOUT);

			strBuf += new String(buf, 0, nBytes) + "\n";
		    }
		    catch (Exception e) {
			_logger.error(e);
			strBuf += "reading VOSHPS: " + e.getMessage();
		    }
		}

		try {
		    _fromDevice.flush();

		    _logger.debug("Invoking '" + 
				  GET_ID_COMMAND + "' command...");
		    _toDevice.write(_sensor.mkCmd(GET_ID_COMMAND));

		    nBytes = 
			StreamUtils.readUntil(_fromDevice, buf,
					      RESPONSE_TERMINATOR.getBytes(), 
					      getSampleTimeout());

		    strBuf += new String(buf, 0, nBytes) + "\n";


		    _logger.debug("Invoking '" + GET_ID2_COMMAND + 
				  "' command...");

		    _toDevice.write(_sensor.mkCmd(GET_ID2_COMMAND));

		    nBytes = 
			StreamUtils.readUntil(_fromDevice, buf,
					      RESPONSE_TERMINATOR.getBytes(), 
					      getSampleTimeout());

		    strBuf += new String(buf, 0, nBytes) + "\n";

		    return strBuf.getBytes();

		} catch (TimeoutException e) {
		    err = "TimeoutException collecting metadata: " + e;
		    _logger.error(err);
		    return err.getBytes();
		} catch (Exception e) {
		    err = "Exception collecting metadata: " + e;
		    _logger.error(err);
		    return err.getBytes();
		}
	}

	/** Samples are locally timestamped */
	public void setClock(long t) {
		return;
	}

	/** Self-test not implemented. */
	public int test() {
		return Device.OK;
	}

	/** Return specifier for default sampling schedule. */
	protected ScheduleSpecifier createDefaultSampleSchedule()
			throws ScheduleParseException {
		// Sample every minute by default
		return new ScheduleSpecifier(60000);
	}

	/** Return parameters to use on serial port. */
	public SerialPortParameters getSerialPortParameters()
			throws UnsupportedCommOperationException {

		return new SerialPortParameters(9600, SerialPort.DATABITS_8,
				SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	}

	/** PRIVATE METHODS * */

	// Attempt to get instrument to a known state...
	private boolean getAttention(int maxTries) {
		_logger.debug("getting attention...");

		// Try to get prompt; Hopefully this won't do
		// anything bad if in some menu....
		if (getPrompt()) {
			_logger.debug("sanity check ok");
			return true;
		}

		_logger.debug("are we in menu mode?..");
		// Try a more serious attempt..
		for (int i = 0; i < maxTries; i++) {
			// Eat any recent output
			try {
				_fromDevice.flush();

				// A series of (7) carriage returns
				// followed by zero are used to bring it up
				// from anywhere in the configuration menu.
				_toDevice.write(RETURN_TO_KNOWN_STATE.getBytes());
			} catch (IOException e) {
				_logger.error("flushInput: " + e);
			}

			if (getPrompt()) {
				_logger.debug("sanity check ok");
				return true;
			}
		}
		return false;
	}// end getAttention()

	/** Get prompt */
	private boolean getPrompt() {

		try {
			// Consume any recent input...
			_fromDevice.flush();

			// Do a sanity check by doing an AddressAck
			_toDevice.write(_sensor.mkCmd(ADDRESS_ACK_COMMAND));

			int s = StreamUtils.skipUntil(_fromDevice, _sensor
					.getAddressReply(), getSampleTimeout(), 0);

			if (s >= 0) {
				_logger.debug("prompt ok");
				return true;
			} else {
				_logger.error("no prompt");
				return false;
			}
		} catch (Exception e) {
			_logger.error("getPrompt: " + e);
			return false;
		}// end catch

	} // end getPrompt()

	/** Configurable WhoiAsimet attributes */
	protected class Attributes extends InstrumentServiceAttributes {

		/** Constructor, with required InstrumentService argument */
		protected Attributes(DeviceServiceIF service) {
			super(service);
		}

		String sensorType;

		String moduleAddress;

		/**
		 * Throw MissingPropertyException if specified attribute is mandatory.
		 */
		public void missingAttributeCallback(String attributeName)
				throws MissingPropertyException {

			if (attributeName.equals("sensorType")) {
				throw new MissingPropertyException(attributeName);
			}
			if (attributeName.equals("moduleAddress")) {
				throw new MissingPropertyException(attributeName);
			}
		}

		/**
		 * Throw InvalidPropertyException if any invalid attribute values found
		 */
		public void checkValues() throws InvalidPropertyException {

			// We have sensor properties - check against current list
			for (int i = 0; i < MAX_SENSOR_TYPES; i++) {
				if (_sensors[i].getType().equals(sensorType)) {
					_sensor = _sensors[i];
					_sensor.setModuleAddr(moduleAddress);
					return;
				}
			}
			// If we get here, couldn't find sensor type in list
			throw new InvalidPropertyException(sensorType);
		}
	}
}

/** ******** ASIMET Sensor classes ***************** */

abstract class ASIMETSensor {

	// log4j Logger
	static private Logger _logger = Logger.getLogger(ASIMETSensor.class);

	// Over written by puck property
	private final String DEFAULT_MODULE_ADDRESS = "99";

	protected String _serviceDescription;

	protected String _sensorType;

	protected String _moduleAddr;

	protected int _maxIDbytes;

	protected int _maxCalBytes;

	protected int _maxSampleBytes;

	ASIMETSensor() {
		_moduleAddr = DEFAULT_MODULE_ADDRESS;
	}

	public String getServiceDescription() {
		return _serviceDescription;
	}

	public String getType() {
		return _sensorType;
	}

	public int getMaxSampleBytes() {
		return _maxSampleBytes;
	}

	public int getMaxIDBytes() {
		return _maxIDbytes;
	}

	public int getMaxCalBytes() {
		return _maxCalBytes;
	}

	public void setModuleAddr(String moduleAddr) {
		_moduleAddr = moduleAddr;
	}

	public String getModuleAddr() {
		return _moduleAddr;
	}

	public byte[] getAddressReply() {
		return (_sensorType + _moduleAddr + WhoiAsimet.RESPONSE_TERMINATOR)
				.getBytes();
	}

	/** Make command using current ID */
	public byte[] mkCmd(String cmdSuffix) {
		byte[] command = (WhoiAsimet.COMMAND_PREFIX + _sensorType + _moduleAddr + cmdSuffix)
				.getBytes();
		_logger.debug(_serviceDescription + " cmd: " + new String(command));
		return command;
	}

	/** Command to output calibrated and/or raw data. */
	String getDataCmd() {
		return "B";
	}

	/** Return true if sensor has VOSHPS front-end. */
	abstract boolean hasVOSHPS();
}

class SWaveRadiometer extends ASIMETSensor {
	SWaveRadiometer() {
		_serviceDescription = "Short Wave Radiometer";
		_sensorType = "SWR";
		_maxIDbytes = 512;
		_maxCalBytes = 200;
	}

	boolean hasVOSHPS() {
		return true;
	}

}

class LWaveRadiometer extends ASIMETSensor {
	LWaveRadiometer() {
		_serviceDescription = "Long Wave Radiometer";
		_sensorType = "LWR";
		_maxIDbytes = 512;
		_maxCalBytes = 350;
	}

	boolean hasVOSHPS() {
		return true;
	}
}

class RelHumidity extends ASIMETSensor {
	RelHumidity() {
		_serviceDescription = "Relative Humidity Sensor";
		_sensorType = "HRH";
		_maxIDbytes = 512;
		_maxCalBytes = 350;
	}

	boolean hasVOSHPS() {
		return true;
	}
}

class Wind extends ASIMETSensor {
	Wind() {
		_serviceDescription = "Windspeed and direction";
		_sensorType = "WND";
		_maxIDbytes = 512;
		_maxCalBytes = 350;
	}

	/** WND doesn't have VOSHPS front-end. */
	boolean hasVOSHPS() {
		return false;
	}

	/**
	 * Command to output calibrated and/or raw data. Note that WND sensor
	 * doesn't support the 'B' command supported by the other sensor types.
	 */
	String getDataCmd() {
		return "C";
	}
}

class Pressure extends ASIMETSensor {
	Pressure() {
		_serviceDescription = "Barometric Pressure Sensor";
		_sensorType = "BPR";
		_maxIDbytes = 512;
		_maxCalBytes = 350;
	}

	/** BPR doesn't have VOSHPS front-end. */
	boolean hasVOSHPS() {
		return false;
	}
} // end of Pressure class

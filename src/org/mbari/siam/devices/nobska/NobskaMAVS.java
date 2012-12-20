/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nobska;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
 * Implementation of Nobska MAVS-3 acoustic current meter service. This service
 * puts the Nobska into a mode that continually acquires data, logging to the
 * Nobska's internal flash, and writing to its serial port. The SIAM 'node'
 * application can "dip" into the instrument's serial stream and sample it on a
 * specified schedule, logging the sampled data to the SIAM data stream. Thus,
 * the instrument produces two datasets; a dataset logged internally to the
 * instrument, and a subsample of that which is logged to the SIAM data stream.
 * 
 * NOTE: Use the writeCmd() method to send commands to the Nobska, as
 * instrument's command parser is somewhat finicky with respect to timing.
 * 
 * @author Tom O'Reilly
 */
public class NobskaMAVS extends InstrumentService implements NobskaMAVS_IF {

	static private Logger _logger = Logger.getLogger(NobskaMAVS.class);

	boolean _nowSampling = false;

	static private final byte[] CNTRL_C = { 0x3 };

	static private final byte[] START_SAMPLING = { 0x7 };

	static private final byte BELL = 0x7;

	static private final byte BACKSPACE = 0x8;

	static private final byte[] CR = { 0x0D };

	static private final byte[] CR_LF = { 0x0D, 0x0A };

	// Have to build this in the constructor
	byte[] _mainMenuPrompt = null;

	private byte[] _stateBuf = new byte[4096];

	// Maximum size of a single Nobska record
	static private final int MAX_RECORD_BYTES = 256;

	byte[] _record = new byte[MAX_RECORD_BYTES];

	// Service state objects
	Attributes _attributes = new Attributes(this);

	/** Specify device startup delay (millisec) */
	protected int initInstrumentStartDelay() {
		return 5000;
	}

	/** Specify prompt string. */
	protected byte[] initPromptString() {
		return "?".getBytes();
	}

	/** Specify sample terminator. */
	protected byte[] initSampleTerminator() {
		return "\r\n".getBytes();
	}

	/** Specify maximum bytes in raw sample. */
	protected int initMaxSampleBytes() {
		return _attributes.recordsPerSample * MAX_RECORD_BYTES;
	}

	/** Specify current limit. */
	protected int initCurrentLimit() {
		return 1000;
	}

	/** Return initial value of instrument power policy. */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/** Return initial value of communication power policy. */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/**
	 * Nobska service constructor
	 */
	public NobskaMAVS() throws RemoteException {
		// Lame way to build main menu prompt string, which contains
		// 'bell' character (no Java standard escape sequence for bell!)

		byte[] escapeBytes = { BELL, BACKSPACE };
		String escapeStr = new String(escapeBytes, 0, 2);

		_mainMenuPrompt = ("Selection " + escapeStr + " ?").getBytes();
	}

	/**
	 * Write specified command to Nobska, throttling character flow. NOTE: 'cmd'
	 * argument does NOT include lf-cr
	 */
	protected void writeCmd(String cmd) throws IOException {

		_logger.debug("writeCmd(" + cmd + ")");
		cmd += "\r";

		byte[] cmdBytes = cmd.getBytes();
		_toDevice.write(cmdBytes);

		_nowSampling = false;
	}

	/** Wait for instrument to prompt */
	protected void waitForPrompt() throws IOException, Exception {

		StreamUtils.skipUntil(_fromDevice, getPromptString(), 10000);
	}

	/** Initialize instrument. */
	protected void initializeInstrument() throws InitializeException, Exception {

		// Throttle commands to instrument
		_toDevice.setInterByteMsec(100);

		// Set sample timeout (include safety margin)
		setSampleTimeout(_attributes.sampleIntervalMsec + 2000);

		// Set the maximum sample size
		setMaxSampleBytes(_attributes.recordsPerSample * MAX_RECORD_BYTES);

		// Get instrument's attention
		wakeupInstrument();

		// Got to "deploy" menu item
		gotoMainMenu();

		writeCmd("6");

		try {
			StreamUtils.skipUntil(_fromDevice, getPromptString(), 10000);
			_logger.debug("in deployment menu");
		} catch (Exception e) {

			_logger.error("initializeInstrument(): "
					+ "couldn't get into deployment menu", e);

			throw new InitializeException("couldn't get into deployment menu");
		}

		// Set current time
		writeCmd("N");
		waitForPrompt();
		setClock(System.currentTimeMillis());

		// Set start time to long-ago, as we want to start
		// immediately
		writeCmd("S");
		waitForPrompt();
		writeCmd("01 01 1970 0 0 0");
		waitForPrompt();

		// Set end time to maximum allowed
		writeCmd("T");
		waitForPrompt();
		writeCmd("01 01 2038 0 0 0");
		waitForPrompt();

		// Enable 'monitor' (i.e. output records to serial port)
		writeCmd("M");
		waitForPrompt();
		writeCmd("Y");
		waitForPrompt();
		writeCmd("Y");
		waitForPrompt();
		writeCmd("Y");
		waitForPrompt();
		writeCmd("Y");
		waitForPrompt();
		writeCmd("D");
		waitForPrompt();

		// Disable verbose mode
		writeCmd("V");
		waitForPrompt();
		writeCmd("N");
		waitForPrompt();

		if (_attributes.enableInternalLog) {

			// Enable data logging
			writeCmd("L");
			waitForPrompt();
			writeCmd("Y");
			waitForPrompt();
			writeCmd("Y");
			waitForPrompt();
			writeCmd("Y");
			waitForPrompt();
			writeCmd("Y");
			waitForPrompt();
			writeCmd("D");
			waitForPrompt();

			// Enable data append mode
			writeCmd("A");
			waitForPrompt();
			writeCmd("Y");
			waitForPrompt();
		} else {
			// Disable data logging
			writeCmd("L");
			waitForPrompt();
			writeCmd("N");
			waitForPrompt();
		}

		// Select Earth frame
		writeCmd("F");
		waitForPrompt();
		writeCmd("3");
		waitForPrompt();

		// Set measurement frequency
		writeCmd("4");
		waitForPrompt();
		writeCmd(new Float(_attributes.sampleHz).toString());
		waitForPrompt();

		// Measurements per sample
		writeCmd("5");
		waitForPrompt();
		writeCmd(new Integer(_attributes.averageSamples).toString());
		waitForPrompt();

		// Sample period
		double period = _attributes.sampleIntervalMsec / 1000.;
		writeCmd("6");
		waitForPrompt();
		writeCmd(new Double(period).toString());
		waitForPrompt();

		// Samples per burst
		writeCmd("7");
		waitForPrompt();
		writeCmd("1");
		waitForPrompt();

		// Burst interval
		writeCmd("8");
		waitForPrompt();
		writeCmd("0");
		waitForPrompt();
		writeCmd("0");
		waitForPrompt();
		writeCmd("0");
		waitForPrompt();
		writeCmd("0");
		waitForPrompt();

		// Bursts per file
		writeCmd("9");
		waitForPrompt();
		writeCmd(new Integer(_attributes.burstsPerFile).toString());
	}

	/** Put Nobska in sampling mode (i.e. "run deployment") */
	synchronized void startSampling() throws IOException {

		_logger.debug("startSampling()");

		if (_nowSampling) {
			_logger.debug("startSampling() - already sampling");
			return;
		}

		// Got to "deploy" menu item
		try {
			gotoMainMenu();
		} catch (Exception e) {
			_logger.error("startSampling():", e);
			return;
		}

		writeCmd("6");

		try {
			waitForPrompt();
			_logger.debug("in deployment menu");
		} catch (Exception e) {

			_logger.error("initializeInstrument(): "
					+ "couldn't get into deployment menu", e);
			return;
		}

		// Start sampling
		_logger.debug("startSampling() - write ^G");
		_toDevice.write(START_SAMPLING);
		_toDevice.flush();
		StopWatch.delay(300);
		_toDevice.write(CR);
		_toDevice.flush();

		String startupMsg = "MAVS3 is ready to deploy";

		try {
			StreamUtils.skipUntil(_fromDevice, startupMsg.getBytes(), 20000);

			_logger.debug("startSampling - got startupMsg");

			// Seven more lines of stuff before actual start of data
			for (int i = 0; i < 10; i++) {
				int nBytes = StreamUtils.readUntil(_fromDevice, _stateBuf, CR,
						20000);
				_logger.debug("startSampling - got "
						+ new String(_stateBuf, 0, nBytes));
			}

			_nowSampling = true;
			_logger.debug("startSampling() - now sampling");
		} catch (Exception e) {
			_logger.error("startSampling(); startupMsg \"" + startupMsg
					+ "\" not found", e);
		}
	}

	/** Go to instrument's main menu. */
	public void gotoMainMenu() throws Exception {
		int maxTries = 6;

		Exception exception = null;

		for (int i = 0; i < maxTries; i++) {
			try {
				_toDevice.write(CNTRL_C);
				_toDevice.flush();

				_nowSampling = false;

				int nBytes = StreamUtils.skipUntil(_fromDevice,
						_mainMenuPrompt, 5000);

				_logger.debug("gotoMainMenu() - got prompt");
				_nowSampling = false;
				return;
			} catch (Exception e) {
				_logger.error("gotoMainMenu()", e);
				_logger.error("gotoMainMenu() - try again");
				exception = e;
				StopWatch.delay(300);
			}
		}

		throw new Exception("gotoMainMenu() - failed: "
				+ exception.getMessage());

	}

	/** Wake-up instrument from low-power sleep */
	public void wakeupInstrument() {

		_logger.debug("wakeupInstrument()");
		try {
			_toDevice.write(CNTRL_C);
			_toDevice.flush();
			StopWatch.delay(1000);

			_toDevice.write(CNTRL_C);
			_toDevice.flush();
			waitForPrompt();

			_toDevice.write(CNTRL_C);
			_toDevice.flush();
			waitForPrompt();
			_logger.debug("wakeupInstrument() - got prompt");
		} catch (Exception e) {
			_logger.error("wakeupInstrument()", e);
		}
		_nowSampling = false;
	}

	/** Return parameters to use on serial port. */
	public SerialPortParameters getSerialPortParameters()
			throws UnsupportedCommOperationException {

		return new SerialPortParameters(38400, SerialPort.DATABITS_8,
				SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	}

	/**
	 * Set Nobska clock to specified millisec past epoch. Method presumes that
	 * instrument is waiting for date/time input.
	 */
	public void setClock(long t) {

		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"MM dd yy HH mm ss");

		Date date = new Date(t);

		try {
			String dateString = dateFormatter.format(date);
			writeCmd(dateString);
			waitForPrompt();
			// That's it; answer "no" to further "change time" prompt
			writeCmd("N");
			waitForPrompt();
		} catch (Exception e) {
			_logger.error("setClock() failed", e);
		}

		return;
	}

	/** Self-test not implemented. */
	public int test() {
		return Device.OK;
	}

	/** Return specifier for default sampling schedule. */
	protected ScheduleSpecifier createDefaultSampleSchedule()
			throws ScheduleParseException {

		// Sample every 10 minutes by default
		return new ScheduleSpecifier(60 * 10 * 1000);
	}

	/**
	 * Get device's notion of its state: a Nobska housekeeping packet.
	 */
	synchronized protected byte[] getInstrumentStateMetadata() {

		int nBytes = 0;
		String msg = null;

		_logger.debug("getInstrumentStateMetadata()");

		try {
			gotoMainMenu();
		} catch (Exception e) {
			msg = e.getMessage();
			return msg.getBytes();
		}

		try {
			writeCmd("s");
			StreamUtils.skipUntil(_fromDevice, "Password:  ".getBytes(), 1000);
			writeCmd("whipr");

			nBytes = StreamUtils.readUntil(_fromDevice, _stateBuf,
					getPromptString(), 10000);

			msg = new String(_stateBuf, 0, nBytes);

			gotoMainMenu();

			// Get deployment parameters
			writeCmd("6");

			nBytes = StreamUtils.readUntil(_fromDevice, _stateBuf,
					getPromptString(), 10000);

			msg = msg + "\n" + new String(_stateBuf, 0, nBytes);

		} catch (Exception e) {
			msg = e.getMessage();
			_logger.error(msg, e);
		}

		_logger.debug("getInstrumentStateMetadata() - done");

		return msg.getBytes();
	}

	/**
	 * Called after power is applied; return when instrument is ready for use.
	 */
	protected void powerOnCallback() {

		_nowSampling = false;

		try {
			// Wait for system to start
			StreamUtils.skipUntil(_fromDevice, "to assert operator control"
					.getBytes(), 10000);
		} catch (Exception e) {
			_logger.warn("waitForInstrumentStart(); didn't get countdown", e);
			// Instrument apparently was already powered up; return
			return;
		}

		int maxTries = 3;

		// Get instrument prompt after power-up
		for (int i = 0; i < maxTries; i++) {
			try {
				StopWatch.delay(100);
				_toDevice.write(CNTRL_C);
				_toDevice.flush();
				StopWatch.delay(100);

				// After power-up and user cntrl-c, instrument
				// prompts for clock-setting
				StreamUtils.skipUntil(_fromDevice, getPromptString(), 5000);

				// Yes, set the clock
				writeCmd("Y");
				waitForPrompt();
				setClock(System.currentTimeMillis());
				return;
			} catch (Exception e) {
				_logger.error("waitForInstrumentStart()", e);
			}
		}
		_logger.error("waitForInstrumentStart() failed after " + maxTries
				+ " tries");
	}

	/**
	 * Read raw sample bytes from serial port into buffer, return number of
	 * bytes read. Nobska continously streams data to its serial port; read
	 * specified number of complete, newline-terminated "records" into the
	 * sample buffer.
	 * 
	 * @param sample
	 *            output buffer
	 */
	protected int readSample(byte[] sample) throws TimeoutException,
			IOException, Exception {

		int nBytes = 0;
		// First find the end of latest record, which may be
		// incomplete
		_logger.debug("readSample(): skip incomplete record");
		_logger.debug("readSample(): sampleTimeout=" + getSampleTimeout());
		nBytes = StreamUtils.skipUntil(_fromDevice, getSampleTerminator(),
				getSampleTimeout());

		_logger.debug("readSample(): skipped " + nBytes + " bytes");

		// Now read in the specified number of complete records
		int totalBytes = 0;

		for (int i = 0; i < _attributes.recordsPerSample; i++) {
			_logger.debug("readSample(): Read record " + i);

			try {
				nBytes = StreamUtils.readUntil(_fromDevice, _record,
						getSampleTerminator(), getSampleTimeout());

				_logger.debug("readSample(): read " + nBytes
						+ " bytes of data: " + new String(_record, 0, nBytes));

				if (nBytes == 0) {
					// Sampling has stopped? Set flag to
					// false so we try to start sampling again
					_logger.debug("readSample() - got 0 bytes");
					_nowSampling = false;
					return 0;
				}

				// Copy into sample array
				System.arraycopy(_record, 0, sample, totalBytes, nBytes);
				totalBytes += nBytes;

				// Append carriage-return/linefeed to record
				_logger.debug("readSample() - append CR-LF");
				System.arraycopy(CR_LF, 0, sample, totalBytes, 2);
				totalBytes += 2;
			} catch (TimeoutException e) {
				// Sampling has stopped? Set flag to
				// false so we try to start sampling again
				_nowSampling = false;
				_logger.debug("readSample() - got TimeoutException");
				throw e;
			}
		}

		return totalBytes;
	}

	/** If Nobska is not currently sampling, then put it in sampling mode. */
	protected void requestSample() throws TimeoutException, Exception {
		if (!_nowSampling) {
			_logger.debug("requestSample() - not currently sampling - start");
			startSampling();
			_logger.debug("requestSample() - _nowSampling=" + _nowSampling);
		}

		// "Catch up" with input stream before sampling;
		// read and discard any characters which have streamed in
		// to this point.
		long available = 0;
		while ((available = _fromDevice.available()) > 0) {
			long n = _fromDevice.skip(available);
			_logger.debug("requestSample() - " + available
					+ " bytes available; skipped " + n + " of them");
		}

		// Next sample will be "fresh"...
	}

	/** Take instrument out of sampling mode, put into low-power sleep. */
	protected String shutdownInstruction() throws Exception {

		try {
			// Get out of sampling mode
			gotoMainMenu();

			// Go to sleep
			writeCmd("4");
		} catch (Exception e) {
			_logger.error("shutdown(): caught exception", e);
		}

		return "OK";
	}

	/** Set sampling interval (millisec). */
	public void setSampleInterval(int msec) {
		_logger.info("setSampleInterval() NOT IMPLEMENTED.");
	}

	
	/**
	 * Nobska service attributes
	 * @author oreilly
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	class Attributes extends InstrumentServiceAttributes {
		
		Attributes(InstrumentService service) {
			super(service);
		}
		// Interval between samples
		int sampleIntervalMsec = 2000;

		// Enable/disable internal Nobska log
		boolean enableInternalLog = true;

		// Sampling frequency
		float sampleHz = 0.5f;

		// Average sample size
		int averageSamples = 1;

		// Bursts per file
		int burstsPerFile = 3942;
		
		// Records per SIAM subsample
		int recordsPerSample = 1;	
	}
}

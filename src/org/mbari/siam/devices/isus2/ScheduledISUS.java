/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.isus2;

import java.util.StringTokenizer;
import java.rmi.RemoteException;
import java.io.IOException;
import org.apache.log4j.Logger;
import gnu.io.UnsupportedCommOperationException;
import gnu.io.SerialPort;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.DeviceServiceIF;

/**
Satlantic ISUS-2 instrument service, as operated in SCHEDULED mode. 
Compatible with ISUS firmware version 2.7.1.

This instrument serial interface is menu-driven, resulting in some tricky
state transitions. 

@author Tom O'Reilly
*/
public class ScheduledISUS
    extends ISUS2
    implements Instrument {

    static final String NO_FRAMES_MSG = "No frames in spooler";
    static final int INTERFRAME_DELAY_MSEC = 1000;

    static private Logger _log4j = Logger.getLogger(ScheduledISUS.class);

    /** Required constructor. */
    public ScheduledISUS() throws RemoteException {
	super();
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return _attributes.sampleFrames * MAX_ASCII_FRAME_BYTES;
    }


    /** Put instrument in correct mode. */
    protected void configureInstrument() throws Exception {

	_log4j.debug("initializeInstrument() - goto main menu");
	gotoMainMenu();

	// Enable OASIS mode
	_log4j.debug("initializeInstrument() - ensure OASIS mode ENABLED");
	doCommand("config", MENU_PROMPT);
	doCommand("admin", PASSWORD_PROMPT);
	doCommand("dunlin", MENU_PROMPT);
	doCommand("oasis", "?");
	doCommand("1", MENU_PROMPT);
	doCommand("quit", MENU_PROMPT);


	// Set to SCHEDULED mode
	_log4j.debug("initializeInstrument() - set SCHEDULED mode");
	doCommand("setup", MENU_PROMPT);
	doCommand("deploy", MENU_PROMPT);
	doCommand("operational", ASK_NO);
	doCommand("yes", "?");
	doCommand("0", MENU_PROMPT);
	doCommand("quit", MENU_PROMPT);
	doCommand("quit", ASK_YES);
	doCommand("yes", ASK_NO);
	doCommand("yes", MENU_PROMPT);

	// Turn off status messages
	_log4j.debug("initializeInstrument() - turn off status msgs");
	doCommand("setup", MENU_PROMPT);
	doCommand("output", MENU_PROMPT);
	doCommand("status", ASK_NO);
	doCommand("yes", " ? ");
	doCommand("0", MENU_PROMPT);
	    
	// Disable frame transfer to serial out
	doCommand("transfer", ASK_NO);
	doCommand("yes", " ? ");
	doCommand("0", MENU_PROMPT);

	// Enable frame logging
	doCommand("logging", ASK_NO);
	doCommand("yes", " ? ");
	doCommand("1", MENU_PROMPT);

	// Enable daily logs
	doCommand("daily", ASK_NO);
	doCommand("yes", " ? ");
	doCommand("0", MENU_PROMPT);

	// Enable interactive frame download
	doCommand("W", ASK_NO);
	doCommand("yes", " ? ");
	doCommand("1", MENU_PROMPT);

	doCommand("quit", MENU_PROMPT);
	doCommand("quit", ASK_YES);
	doCommand("yes", ASK_NO);
	doCommand("yes", MENU_PROMPT);
    }



    /** Request a data sample. */
    protected synchronized void requestSample() throws IOException {

	int maxTries = 10;
	for (int i = 0; i < maxTries; i++) {
	    // Try to put instrument into 'ready' mode. This may fail if
	    // instrument happens to be acquiring a sample
	    try {
		gotoReady();
		return;
	    }
	    catch (Exception e) {
		_log4j.info("requestSample() - didn't get ack from device - maybe sampling?");
	    }
	    try {
		Thread.sleep(1000);
	    }
	    catch (Exception e) {
	    }
	}

	throw new IOException("Couldn't get ack from instrument");
    }


    /**
     * Read raw sample bytes from serial port into buffer, return number of
     * bytes read. ISUS service reads specified number of frames from 
     * serial port.
     * 
     * @param sample
     *            output buffer
     */
    protected int readSample(byte[] sample) throws TimeoutException,
						   IOException, Exception {


	// Determine how many frames are available
	_fromDevice.flush();
	_toDevice.write("R".getBytes());
	_toDevice.flush();

	int nBytes = 
	    StreamUtils.readUntil(_fromDevice, _buffer, 
				  "\r\n".getBytes(), 2000);
	int framesAvailable = 0;
	try {
	    framesAvailable = Integer.parseInt(new String(_buffer, 0, nBytes));
	    _log4j.debug("readSample() - framesAvailable=" + framesAvailable);
	}
	catch (NumberFormatException e) {
	    _log4j.error("Invalid number of records available: " + 
			 new String(_buffer, 0, nBytes));
	    throw new Exception("Invalid number of records available: " + 
				new String(_buffer, 0, nBytes));
	}

	if (framesAvailable <= 0) {
	    System.arraycopy(NO_FRAMES_MSG.getBytes(), 0, sample, 0, 
			     NO_FRAMES_MSG.length());

	    return NO_FRAMES_MSG.length();
	}

	final byte[] frameTerminator = getSampleTerminator();
	int totalBytes = 0;
	boolean error = false;

	for (int frame = 0; frame < framesAvailable; frame++) {

	    // Give instrument time to get ready to supply next frame
	    Thread.sleep(INTERFRAME_DELAY_MSEC);

	    _log4j.debug("readSample() - get frame " + frame);

	    // Request a frame
	    _toDevice.write("D".getBytes());
	    _toDevice.flush();

	    try {
		// Read next frame into frame buffer
		nBytes = StreamUtils.readUntil(_fromDevice, _frameBuffer,
					       frameTerminator,
					       getSampleTimeout());

		_log4j.debug("readSample() - GOT frame " + frame);

		String prefix = 
		    new String(_frameBuffer, 0, FRAME_PREFIX.length());

		// Check for valid frame prefix
		if (!prefix.equals(FRAME_PREFIX)) {
		    _log4j.error("Bad frame? " + 
				 new String(_frameBuffer, 0, nBytes));

		    continue;
		}

		if (frame < _attributes.sampleFrames) {
		    // Copy from frame buffer to sample buffer
		    System.arraycopy(_frameBuffer, 0, sample, 
				     totalBytes, nBytes);

		    totalBytes += nBytes;

		    // Append terminator to each frame (since it's discarded by
		    // StreamUtils.readUntil()).
		    System.arraycopy(frameTerminator, 0, sample, totalBytes,
				     frameTerminator.length);

		    totalBytes += frameTerminator.length;
		}
		else {
		    _log4j.debug("readSample() - discarding extra frame");
		}
	    }
	    catch (Exception e) {
		String errMsg = "readSample() - got " + 
		    e.getClass().getName() + 

		    ": " + e.getMessage();

		_log4j.error(errMsg);
		throw new Exception(errMsg);
	    }
	}

	// Set the instrument's clock
	setDeviceClock();

	return totalBytes;
    }



    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	return new ScheduleSpecifier(600 * 1000);
    }

}


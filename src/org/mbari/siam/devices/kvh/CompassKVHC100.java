// Copyright MBARI 2002
package org.mbari.siam.devices.kvh;

import java.rmi.RemoteException;
import java.io.IOException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.distributed.RangeException;

import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
Driver for KVH C100 compass.

@author Tom O'Reilly
*/
public class CompassKVHC100 extends InstrumentService implements Instrument {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(CompassKVHC100.class);

    private static final int _SAMPLE_INTERVAL = 60000;

    public CompassKVHC100()
	throws RemoteException {
    }

    /** Maximum number of bytes in KVH metadata. */
    protected static final int KVH_METADATA_BYTES = 50;

    /** Specify compass device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
    	return 500;
    }
    
    /** Specify compass prompt string. */
    protected byte[] initPromptString() {
	return ">".getBytes();
    }

    /** Specify compass sample terminator. */
    protected byte[] initSampleTerminator() {
	return "\r\n".getBytes();
    }

    /** Specify maximum bytes in raw compass sample. */
    protected int initMaxSampleBytes() {
	return 32;
    }

    /** Specify compass current limit. */
    protected int initCurrentLimit() {
	return 5000;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Request a data sample from the compass. */
    protected void requestSample() throws IOException {
    	_fromDevice.flush();
	_toDevice.write("d0\r".getBytes());
    }


    /** Get attention of the instrument; puts compass into polled mode. */
    protected void getAttention(int maxTries) 
	throws Exception {

	// May take several attempts to get KVH into polled mode
	for (int i = 0; i < maxTries; i++) {

	    _toDevice.write("h\r".getBytes());
	    try {
		StreamUtils.skipUntil(_fromDevice, getPromptString(), 500);
		break;
	    }
	    catch (Exception e) {
		// Timed out, or some other problem
		_log4j.error(new String(getName()) + 
				   ".getAttention(): " + 
				   "Couldn't set to polled mode");

		if (i == maxTries - 1) {
		    // No more tries left
		    throw e;
		}
		try {
		    Thread.sleep(100);
		}
		catch (InterruptedException e2) {
		}
	    }
	}
    }

    /** Initialize the compass. */
    protected void initializeInstrument() 
	throws InitializeException, Exception {
    	
	setSampleTimeout(1000);
    	
	// Turn on DPA power/comms 
	managePowerWake();  

	// Get instrument attention; puts compass into polled mode.
	getAttention(5);
    }


    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters() 
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(4800,
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }


    /** Return KVH metadata. */
    protected byte[] getInstrumentMetadata() {

	byte[] tmpBuf = new byte[KVH_METADATA_BYTES];

	byte[] buf = "\nKVH metadata: ".getBytes();

	System.arraycopy(buf, 0, tmpBuf, 0, buf.length);
	int offset = buf.length;

	buf = new byte[KVH_METADATA_BYTES];
	int nBytes = 0;

	try {
	    getAttention(20);
	    // Ask for KVH serial number, software version, etc...
	    _toDevice.write("?w\r".getBytes());
	    nBytes = StreamUtils.readUntil(_fromDevice, buf, getSampleTerminator(), 
			       500);
	}
	catch (Exception e) {
	    // Couldn't get KVH metadata...
            _log4j.error("Couldn't get KVH metadata!");
	    buf = "Couldn't get KVH metadata".getBytes();
	    nBytes = buf.length;
	}

	_log4j.debug("Copy KVH metadata: offset=" + offset + 
			     ", nBytes=" + nBytes);
	System.arraycopy(buf, 0, tmpBuf, offset, nBytes);

	// Copy into exact-sized buffer and return
	byte[] returnBuf = new byte[tmpBuf.length];
	System.arraycopy(tmpBuf, 0, returnBuf, 0, tmpBuf.length);
	return returnBuf;
    }

    /** KVH does not have an internal clock. */
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

	// Sample every _SAMPLE_INTERVAL ms by default
	return new ScheduleSpecifier(_SAMPLE_INTERVAL);
    }
}

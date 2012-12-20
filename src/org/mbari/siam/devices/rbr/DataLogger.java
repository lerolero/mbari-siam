/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.rbr;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.TimeoutException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;
import org.mbari.siam.utils.DelimitedStringParser;
import org.mbari.siam.distributed.PacketParser;

/**
   Base class for RBR data loggers. 
*/
public class DataLogger 
    extends PolledInstrumentService 
    implements Instrument {

    // CVS revision 
    private static String _versionID = "$Revision: 1.5 $";

    static private Logger _log4j = Logger.getLogger(DataLogger.class);

    static final protected byte[] REQUEST_ID = "A".getBytes();
    static final protected byte[] REQUEST_SAMPLE = "F00".getBytes();

    static protected int MAX_STATUS_BYTES = 256;

    static protected byte[] _statusBuf = new byte[MAX_STATUS_BYTES];

    Attributes _attributes = new Attributes(this);

    public DataLogger() throws RemoteException {
    }

    /**
     * Return initial value for instrument's "prompt" character.
     */
    protected byte[] initPromptString() {
        return "".getBytes();
    }

    /**
     * Return initial value for instrument's sample terminator
     */
    protected byte[] initSampleTerminator() {
        return "\r\n".getBytes();
    }

    /**
     * Return initial value of DPA current limit.
     */
    protected int initCurrentLimit() {
        return 5000;
    }

    /**
     * Return initial value of instrument power policy.
     */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.NEVER;
    }

    /**
     * Return initial value of instrument power policy.
     */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }

    /**
     * Return initial value of instrument startup time in millisec.
     */
    protected int initInstrumentStartDelay() {
        return 2000;
    }

    /**
     * Return initial value for maximum number of bytes in a instrument data
     * sample.
     */
    protected int initMaxSampleBytes() {
        return 100;
    }


    /**
     * Return parameters to use on serial port.
     */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

        return new SerialPortParameters(_attributes.baud, 
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }


    /**
     * Return specifier for default sampling schedule.
     */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

        // Sample every 60 seconds by default
        return new ScheduleSpecifier(60000);
    }

    /** Set instrument clock */
    public void setClock(long epochMsec) {

	_log4j.info("setClock() not implemented");
    }


    /**
     * Self-test routine; not yet implemented.
     */
    public int test() {
        return Device.OK;
    }


    public void requestSample() throws TimeoutException, Exception {

	// Wake up the device
	_toDevice.write("\n".getBytes());
	Thread.sleep(1000);

	// This step must preceed every sample request???
	getInstrumentStateMetadata();

	_toDevice.write(REQUEST_SAMPLE);
    }


    /**
     * Get device's notion of its state
     */
    synchronized protected byte[] getInstrumentStateMetadata() 
	throws Exception {

	// Wake up the device
	_toDevice.write("\n".getBytes());
	Thread.sleep(1000);

	_toDevice.write(REQUEST_ID);

	int nBytes =
	    StreamUtils.readUntil(_fromDevice, _statusBuf, 
				  getSampleTerminator(),
				  getSampleTimeout());
	
	byte[] statusBuf = new byte[nBytes];
	System.arraycopy(_statusBuf, 0, statusBuf, 0, nBytes);
	return statusBuf;
    }

    /** Create and return PacketParser */
    public PacketParser getParser() {
	return new RbrPacketParser();
    }


    protected class Attributes 
	extends InstrumentServiceAttributes {

	public Attributes(DeviceServiceIF service) {
	    super(service);
	}

	int baud = 19200;

    }

    public class RbrPacketParser extends DelimitedStringParser {

	public static final String TEMPERATURE_KEY = "temperature";
	public static final String CONDUCTIVITY_KEY = "conductivity";
	public static final String PRESSURE_KEY = "pressure";

	public RbrPacketParser() {
	    super(" ");
	}

	/** Process each token in Seabird's ASCII output record; this method is
	    called by the framework for each token in the record. */
	protected PacketParser.Field processToken(int nToken, String token) 
	    throws ParseException {

	    // Note that each token in RBR output represents a number.
	    Number value = decimalValue(token);

	    switch (nToken) {

	    case 0:
		// Skip "TIM"
	    case 1:
		// Skip YYMMDDhhmmss
		break;

	    case 2:
		// Conductivity
		if (value == null) {
		    throw new ParseException("Invalid conduct: " + token, 0);
		}

		return new Field(CONDUCTIVITY_KEY, value, "siemens/meter");	    
	    case 3:
		// Temperature
		if (value == null) {
		    throw new ParseException("Invalid tmprt: " + token, 0);
		}
		return new Field(TEMPERATURE_KEY, value, "deg C");

	    case 4:
		// Pressure
		if (value == null) {
		    throw new ParseException("Invalid pressure: " + token, 0);
		}
		return new Field(PRESSURE_KEY, value, "decibars");

	    default:
		return null;

	    }
	    return null;
	}
    }

}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/** 
 */

package org.mbari.siam.devices.axis;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.core.HttpInstrumentPort;

/**
   Implements instrument service for Axis web camera, which utilizes the
   "VAPIX" api. Note that this service communicates with the camera through 
   an HttpInstrumentPort.
 */

public class AxisCamera
    extends PolledInstrumentService 
    implements Instrument {

    static final int MAX_COMMAND_TRIES = 3;

    static final int MAX_SAMPLE_BYTES = 1000000;

    static final int RESPONSE_TIME = 60000; 

    static final int SAMPLE_RESPONSE_TIME = RESPONSE_TIME;

    static final String IMAGE_CHANNEL_NAME = "image";

    protected Attributes _attributes = new Attributes(this);


    // log4j Logger
    static private Logger _log4j = Logger.getLogger(AxisCamera.class);


    /**
     * Allocates a new <code>AxisCamera</code>
     * 
     * @throws RemoteException .
     */
    public AxisCamera() throws RemoteException {
	Authenticator.setDefault(new MyAuthenticator());
    }

    /** Specify startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return 2000;
    }

    /** Specify prompt string. */
    protected byte[] initPromptString() {
	return "".getBytes();
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	// No sample terminator string, as the HttpInstrumentPort just returns
	// a web page
	return "".getBytes();
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return MAX_SAMPLE_BYTES;
    }

    /** Specify current limit in increments of 120 mA upto 11880 mA. */
    protected int initCurrentLimit() {
	return 1000; 
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	// Sample every 10 minutes by default
	return new ScheduleSpecifier(600000);
    }


    protected void initializeInstrument()
	throws InitializeException, Exception {

	// Specify jpeg mime type for camera data
	if (_turbinator != null) {
	    _turbinator.setMimeType(IMAGE_CHANNEL_NAME, "image/jpeg");
	}
    }

    /**
     * 
     * @exception TimeoutException
     *                thrown if no data is detected for a period of twice the
     *                _preemptionTime
     * @exception Exception
     *                not thrown
     * see setNumSensors
     * see _preemptionTime
     */
    protected void requestSample() throws TimeoutException, Exception {
	_log4j.debug("requestSample()");
	_toDevice.write("/axis-cgi/jpg/image.cgi".getBytes());
    }


    public int test() {
	return 0;
    }


    /** Get camera image parser. */
    public PacketParser getParser() throws NotSupportedException {
	return new ImageParser();
    }

    protected class Attributes extends InstrumentServiceAttributes {

        public Attributes(DeviceServiceIF service) {
            super(service);
        }

	/** User name for server login */
	String user = "root";

	/** Password for server login */
	String password = "rootme";
    }

    /** Parse jpeg image from packet */
    static public class ImageParser extends PacketParser {
	public PacketParser.Field[] parseFields(DevicePacket packet) 
	    throws NotSupportedException, ParseException {
	    if (!(packet instanceof SensorDataPacket)) {
		throw new NotSupportedException("expecting SensorDataPacket");
	    }

	    SensorDataPacket sensorPacket = (SensorDataPacket )packet;

	    // Just one field, which contains image data
	    PacketParser.Field[] field = new PacketParser.Field[1];

	    field[0] = 
		new PacketParser.Field(AxisCamera.IMAGE_CHANNEL_NAME, 
				       sensorPacket.dataBuffer(), "");
	    return field;
	}

    }


    /** MyAuthenticator supplies username/password to protected server */
    class MyAuthenticator extends Authenticator {
	protected PasswordAuthentication getPasswordAuthentication() {
	    // TEST TEST TEST
	    return new PasswordAuthentication(_attributes.user, _attributes.password.toCharArray());
	}
    }
}


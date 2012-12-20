/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/** 
 */

package org.mbari.siam.devices.acti;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.core.HttpInstrumentPort;

/**
   Implements instrument service for Acti video server.
   Note that this service communicates with the camera through an 
   HttpInstrumentPort.
 */

public class ActiVideoServer
    extends PolledInstrumentService 
    implements Instrument {

    protected Attributes _attributes = new Attributes(this);

    static final int MAX_COMMAND_TRIES = 3;

    static final int MAX_SAMPLE_BYTES = 1000000;

    static final int RESPONSE_TIME = 60000; 

    static final int SAMPLE_RESPONSE_TIME = RESPONSE_TIME;

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(ActiVideoServer.class);


    /**
     * Allocates a new <code>ActiVideoServer</code>
     * 
     * @throws RemoteException .
     */
    public ActiVideoServer() throws RemoteException {
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
	_toDevice.write(("/cgi-bin/encoder?USER=" + _attributes.user + 
			 "&PWD=" + _attributes.password + 
			 "&SNAPSHOT").getBytes());

	_toDevice.flush();
    }


    public int test() {
	return 0;
    }


    protected class Attributes extends InstrumentServiceAttributes {

        public Attributes(DeviceServiceIF service) {
            super(service);
        }

	/** User name for server login */
	String user = "Admin";

	/** Password for server login */
	String password = "123456";
    }
}


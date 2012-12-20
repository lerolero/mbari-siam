/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.microStrain;

import java.rmi.RemoteException;
import org.apache.log4j.Logger;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.StreamingInstrumentService;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InitializeException;

/**
Instrument driver/service for MicroStrain 3DM-GX1 AHRS
*/
public class M3dmGx1 extends StreamingInstrumentService 
    implements Instrument {

    static final byte[] STREAM_QUATERNIONS_CMD = {0x10, 0x00, 0x04};
    static final byte[] STOP_STREAMING_CMD = {0x10, 0x00, 0x00};

    /** log4j Logger */
    static private Logger _log4j = Logger.getLogger(M3dmGx1.class);

    Attributes _attributes = new Attributes(this);


    public M3dmGx1() throws RemoteException {
    }


    /** Initialize the instrument */
    protected void initializeInstrument()
	throws InitializeException, Exception {

	_log4j.debug("Put instrument into 'continuous' mode");
	startStreaming();
    }


    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
    throws UnsupportedCommOperationException {
        
        return new SerialPortParameters(_attributes.baud, 
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }


    /** Put instrument into streaming mode. */
    protected void startStreaming() throws Exception {
	_log4j.debug("startStreaming()");
	_toDevice.write(STREAM_QUATERNIONS_CMD);
	_toDevice.flush();
    }
	
    /** Take instrument out of streaming mode. */
    protected void stopStreaming() throws Exception {
	_log4j.warn("stopStreaming()");
	_toDevice.write(STOP_STREAMING_CMD);
	_toDevice.flush();
    }

    /** Return true if device currently in streaming mode, else 
	return false. */
    protected boolean isStreaming() {
	_log4j.warn("isStreaming() not implemented");
	return false;
    }


    /** Return specifier for default sampling schedule. Subclasses MUST
     * provide the default sample schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
    throws ScheduleParseException {
        
        // Sample every 30 seconds by default
        return new ScheduleSpecifier(30000);
    }


    /** Sets AHRS power policy */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }
    
    /** Sets AHRS communications power policy */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }
    
    /** Set AHRS startup delay time. Set to 0 above. */
    protected int initInstrumentStartDelay() {
        return 100;
    }
    
    /** Sets AHRS current limit. Set to 1000 above. */
    protected int initCurrentLimit() {
        return 1000;
    }
    
    
    /** Sets the AHRS sample terminator. The string is terminated by
     * CR (\r) and LF (\n).*/
    protected byte[] initSampleTerminator() {
        return "\r\n".getBytes();
    }
    
    /** Sets the AHRS command prompt. There is no command prompt. */
    protected byte[] initPromptString() {
        return "".getBytes();
    }
    
    /** Sets the AHRS maximum number of bytes in an instrument
     * data sample */
    protected int initMaxSampleBytes() {
        // ~499 bytes maximum per sample - in full RS-232C comprehensive output
        return 1024;  // !May need to be decreased to 512!
    }
    

    /** Self-test routine; This does nothing in the AHRS driver */
    public int test() {
        return Device.OK;
    }

    /** Service attributes. */
    public class Attributes extends StreamingInstrumentService.Attributes {
        
        Attributes(StreamingInstrumentService service) {
            super(service);
        }
        

	/** Instrument baud rate */
	int baud = 38400;
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.powerswitch;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.mbari.siam.core.DebugMessage;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PowerPolicy;

/**
 * Basic power switching, no instrument communications.
 * Each sample cycle, turns instrument on, sleeps for instrumentStartDelay milliseconds,
 * (allowing other processes to run) and then turns power off.
 */
public class PowerSwitch extends PolledInstrumentService 
    implements Instrument {

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(PowerSwitch.class);

    private static int sampleNo = 1;

    public PowerSwitch() throws RemoteException {
    }

    /** Specify compass device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return 0;
    }

    /** Specify compass prompt string. */
    protected byte[] initPromptString() {
	return ">".getBytes();
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	return "\r\n".getBytes();
    }

    /** Specify maximum bytes in raw compass sample. */
    protected int initMaxSampleBytes() {
	return 32;
    }

    /** Specify current limit. */
    protected int initCurrentLimit() {
	return 500;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Do instrument- and driver-specific initialization. By default,
	base class implementation does nothing.
    */
    protected void initializeInstrument() 
	throws InitializeException, Exception {
	setMaxSampleTries(1);
    }

    /** Request a data sample from the compass. */
    protected void requestSample() throws IOException {
	_log4j.debug("PowerSwitch.requestSample()");
	/** Turn off power because it was already turned on for the required 
	 *  interval (instrumentStartDelay) in managePowerWake
	 */
	managePowerSleep();
    }

    /** override to return something to prevent a no data error */
    protected int readSample(byte[] sample) 
	throws TimeoutException, IOException, Exception {
	// In the default case, this will be RECORDTYPE_DEFAULT
	setRecordType(_recordType);
	sample="OK".getBytes();
	return sample.length;
    }


    /** Return metadata. */
    protected byte[] getInstrumentMetadata() {
	return "PowerSwitch does not return metadata".getBytes();
    }


    /** No internal clock. */
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
	// Sample every hour by default
	return new ScheduleSpecifier(3600000);
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, 
					SerialPort.STOPBITS_1);
    }

}

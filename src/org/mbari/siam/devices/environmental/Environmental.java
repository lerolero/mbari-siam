// Copyright MBARI 2002
package org.mbari.siam.devices.environmental;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;

/**
 * Driver for KVH C100 compass.
 * 
 * @author Tom O'Reilly
 */
public class Environmental extends InstrumentService implements Instrument {

	public Environmental() throws RemoteException {
	}

	/** Maximum number of bytes in environmental metadata. */
	protected static final int ENV_METADATA_BYTES = 50;

	/** Specify prompt string. */
	protected byte[] initPromptString() {
		return "> ".getBytes();
	}

	/** Specify sample terminator. */
	protected byte[] initSampleTerminator() {
		return "\r\n\n\r".getBytes();
	}

	/** Specify maximum bytes in raw sample. */
	protected int initMaxSampleBytes() {
		return 256;
	}

	/** Specify current limit. */
	protected int initCurrentLimit() {
		return 500;
	}

	/** Return initial value of instrument power policy. */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/** Return initial value of communication power policy. */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/** Specify device startup delay (millisec) */
	protected int initInstrumentStartDelay() {
		return 500;
	}

	/** Request a data sample. */
	protected void requestSample() throws IOException {
		_fromDevice.flush();

		_toDevice.write("\n".getBytes()); // CR here breaks the parsing
		// prompt is not consumed during flush unless a delay is inserted
		_fromDevice.skip(_fromDevice.available());

		_toDevice.write("get data\n".getBytes());
	}

	/** Initialize the sensors. */
	protected void initializeInstrument() throws InitializeException, Exception {

		setSampleTimeout(3000);

		// Turn on DPA power/comms
		managePowerWake();

		_fromDevice.flush();
		_toDevice.write("set echo off\n".getBytes());
		_fromDevice.flush();
	}

	/** Return parameters to use on serial port. */
	public SerialPortParameters getSerialPortParameters()
			throws UnsupportedCommOperationException {

		return new SerialPortParameters(9600, SerialPort.DATABITS_8,
				SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	}

	/** Environmental sensor does not have an internal clock. */
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

		// Sample every 10 seconds by default
		return new ScheduleSpecifier(10000);
	}
	/** Return specifier for default sampling schedule. */
}
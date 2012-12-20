/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.valve;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteObject;
import java.util.Iterator;
import java.util.StringTokenizer;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.ServiceSandBox;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.devices.bbElec.BB232SDD16;
import org.mbari.siam.distributed.devices.ValveIF;
import org.mbari.siam.distributed.devices.Valve2WayIF;
import org.mbari.siam.distributed.devices.ValveServiceIF;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.RangeException;

/**
 * ValveService implements a service to control 1-4 Hanbay MDx-xxxDT valves controlled by
 * a B&B Electronics 232SDD16 Serial-to-Digital-I/O module.
 *<p>It implements ValveServiceIF, which means that it can return up to 4 ValveIF objects,
 * each of which controls a single valve.
 */
public class ValveService extends PolledInstrumentService implements Instrument, ValveServiceIF
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(ValveService.class);

    protected final byte[] _nullBytes = "".getBytes();

    protected int _sampleBytes = 8;

    ValveAttributes	_attributes = new ValveAttributes(this);
    ValveControl[]	_valves = new ValveControl[MAX_VALVES];
    BB232SDD16		_digitalIO;

    public ValveService() throws RemoteException {
	super();
    }

    /** Specify device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return(100);
    }

    /** Specify prompt string. */
    protected byte[] initPromptString() {
	return(_nullBytes);
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	return(_nullBytes);
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return(_sampleBytes);
    }

    /** Specify current limit. */
    protected int initCurrentLimit() {
	return(5000);
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return(PowerPolicy.ALWAYS);
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return(PowerPolicy.ALWAYS);
    }

    /** Request a data sample */
    protected void requestSample() throws Exception {
    }

    protected void initializeInstrument() 
	throws InitializeException, Exception
    {
	int i, outputs = 0;
	InstrumentPort port = getInstrumentPort();

	if (!(port instanceof SerialInstrumentPort))
	    throw new InitializeException("Not a serial instrument");

	_digitalIO = new BB232SDD16(((SerialInstrumentPort)port).getSerialPort(),
				    getSampleTimeout());

	for (i = 0; i < _attributes.numValves; i++) {
	    outputs |= (1 << _attributes.cmd1Bits[i]);
	    outputs |= (1 << _attributes.cmd2Bits[i]);
	}

	_digitalIO.defineIO(outputs);

	for (i = 0; i < _attributes.numValves; i++) {
	    try {
		getValveControl(i).setPosition(ValveIF.POSITION_CENTER);
	    } catch (IOException e) {
		// Log the exception but allow initializeInstrument to proceed
		_log4j.error("Exception in initializeInstrument: " + e);
	    }
	}
    }

    /** Get a ValveControl for the given valveNum. */
    public ValveControl getValveControl(int valveNum)
	throws RangeException, IOException
    {
	ValveControl	valve;

	if ((valveNum < 0) || (valveNum >= _attributes.numValves))
	    throw new RangeException("valveNum out of range");

	valve = _valves[valveNum];
	if (valve == null) {
	    valve = new ValveControl(_digitalIO, _attributes, valveNum);
	    _valves[valveNum] = valve;
	    UnicastRemoteObject.exportObject(valve);
	}

	return (valve);
    }

    /** Get a ValveIF for the given valveNum.
     *  Supports the getValve() method of ValveIF. 
    */
    public ValveIF getValve(int valveNum)
	throws RangeException, IOException
    {
	return ((Valve2WayIF)(RemoteObject.toStub(getValveControl(valveNum))));
    }

    /** Acquire Data	*/
    protected int readSample(byte[] sample) throws Exception
    {
	StringBuffer sb = new StringBuffer();

	for (int i = 0; i < _attributes.numValves; i++)
	{
	    StopWatch.delay(500);
	    sb.append(getValveControl(i).getPosition());
	    sb.append(" ");
	}

	byte[] rtn = sb.toString().trim().getBytes();

	System.arraycopy(rtn, 0, sample, 0, rtn.length);

	return(rtn.length);
    }

    /** Return metadata. */
    protected byte[] getInstrumentStateMetadata()
    {
	StringBuffer	sb = new StringBuffer();

	try {
	    int val = _digitalIO.readConfiguration();
	    sb.append("Output definition (hex) = ");
	    sb.append(Integer.toHexString((val>>16)&0xffff));

	    sb.append("\nPowerup State (hex) = ");
	    sb.append(Integer.toHexString(val&0xffff));
	} catch (Exception e) {
	    sb.append("Exception reading device configuration: ");
	    sb.append(e.toString());
	}

	return(sb.toString().getBytes());
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	// Sample every 60 seconds by default
	return new ScheduleSpecifier(60000);
    }

    /**
     * Return parameters to use on serial port.
     */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException
    {
        return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }

    /** Return a PacketParser. */
    public PacketParser getParser() throws NotSupportedException
    {
	return(new ValveServicePacketParser(_attributes.registryName));
    }

    /**
     * Self-test routine; not yet implemented.
     */
    public int test() {
        return(Device.OK);
    }

} /* class ValveService */

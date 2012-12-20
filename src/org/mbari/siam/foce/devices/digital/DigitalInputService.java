/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.digital;

import java.util.Vector;
import java.util.Iterator;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.DigitalInputInstrumentPort;
import org.mbari.siam.core.ServiceSandBox;
import org.mbari.siam.core.ChannelParameters;
import org.mbari.siam.core.ChannelRange;

import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.distributed.devices.DigitalInputBoard;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;

/**
 * Instrument composed of digital input bits from the Diamond Systems DMM-32X-AT Data acquisition board,
 * as configured for FOCE.
 */
public class DigitalInputService extends PolledInstrumentService implements Instrument
{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(DigitalInputService.class);

    DigitalInputAttributes _attributes = new DigitalInputAttributes(this);

    protected DigitalInputInstrumentPort _instPort = null;
    protected DigitalInputBoard _dioBoard = null;
    protected int[] _params = null;
    protected int _dioPort=0, _bitNum=0, _numBits=8;
    protected int _sampleBytes = 8;
    protected byte[] _nullBytes = "".getBytes();


    public DigitalInputService() throws RemoteException {
	super();
    }


    public void initialize(NodeProperties nodeProperties, Parent parent,
			   InstrumentPort port, ServiceSandBox sandBox,
			   String serviceXMLPath, String servicePropertiesPath,
			   String cachedServicePath)

	throws MissingPropertyException, InvalidPropertyException,
	       PropertyException, InitializeException, IOException,
	       UnsupportedCommOperationException {

	if (!(port instanceof DigitalInputInstrumentPort))
	    throw new InitializeException("Not a DigitalInputInstrumentPort!");

	_instPort = (DigitalInputInstrumentPort)port;
	_dioBoard = _instPort.getDigitalInputBoard();
	_params = _instPort.getParams();

	if (_params.length >= 2)
	    _dioPort = _params[1];
	if (_params.length >= 3)
	    _bitNum = _params[2];
	if (_params.length >= 4)
	    _numBits = _params[3];

	_sampleBytes = (_numBits > 4) ? (2*_numBits) : 8;

	subclassInit();

	super.initialize(nodeProperties, parent, port, sandBox,
			 serviceXMLPath, servicePropertiesPath,
			 cachedServicePath);
    }

    /** Null method provided here so subclasses can override to provide additional initialization */
    protected void subclassInit() {}

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
	super.initializeInstrument();
	managePowerWake();
	_log4j.debug("initializeInstrument() - done");
    }


    /** Acquire Data	*/
    protected int readSample(byte[] sample) throws Exception
    {
	_log4j.debug("DigitalInputService.readSample()");
	int result = _dioBoard.readDIO(_dioPort);
	_log4j.debug("DigitalInputService.readSample() result = " + result);

	StringBuffer sb = new StringBuffer();
	
	for (int i = _bitNum; i < (_bitNum + _numBits); i++)
	    sb.append(((result & (1 << i)) == 0) ? "0 " : "1 ");

	byte[] rtn = sb.toString().trim().getBytes();

	System.arraycopy(rtn, 0, sample, 0, rtn.length);

	_log4j.debug("DigitalInputService.readSample() returning " + rtn.length);
	return(rtn.length);
    }


    /** Return metadata. */
    protected byte[] getInstrumentStateMetadata()
    {
	StringBuffer sb = new StringBuffer();

	sb.append(_dioBoard.getName());
	sb.append("\nPort = ");
	sb.append(_dioPort);
	sb.append("\nStarting bit number = ");
	sb.append(_bitNum);
	sb.append("\nNumber of bits = ");
	sb.append(_numBits);
	sb.append("\n");

	return(sb.toString().getBytes());
    }

    /** No internal clock. */
    public void setClock() throws NotSupportedException {
	throw new NotSupportedException("Dummy.setClock() not supported");
    }

    /** Self-test not implemented. */
    public int test() {
	return Device.OK;
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	// Sample every 30 seconds by default
	return new ScheduleSpecifier(30000);
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    /** Return a PacketParser. */
    public PacketParser getParser() throws NotSupportedException
    {
	_log4j.debug("parseNames = " + _attributes.parseNames +
		     " parseUnits = " + _attributes.parseUnits);

	if ((_attributes.parseNames == null) ||
	    (_attributes.parseNames.length() == 0))
	    throw new NotSupportedException("No parseNames defined");

	String[] names = new String[_numBits];
	String[] units = new String[_numBits];

	StringTokenizer nameSt = new StringTokenizer(_attributes.parseNames);
	StringTokenizer unitSt = new StringTokenizer(_attributes.parseUnits);
	String nullStr = "";

	for (int i = 0; i < _numBits; i++)
	{
	    try {
		names[i] = nameSt.nextToken();
	    } catch (Exception e) {
		names[i] = ("DIn" + i);
	    }
	    try {
		units[i] = unitSt.nextToken();
	    } catch (Exception e) {
		units[i] = (i > 0) ? units[i-1] : "bit";
	    }

	    _log4j.debug("Chan " + i + ": " + names[i] + ", " +
			 units[i]);
	}

	return new DigitalPacketParser(_numBits, _attributes.registryName, names, units);
    }

    /** Attributes for general DigitalInput Instrument
     * @author Bob Herlien
     */
    class DigitalInputAttributes extends InstrumentServiceAttributes
    {
	DigitalInputAttributes(DeviceServiceIF service) {
	    super(service);
	}

	public String registryName = "DigitalInputs";
	protected String parseNames = "DIn0";
	protected String parseUnits = "bit";

    }

} // end of class

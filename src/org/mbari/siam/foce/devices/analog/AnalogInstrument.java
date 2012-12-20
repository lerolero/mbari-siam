/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.analog;

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
import org.mbari.siam.core.AnalogInstrumentPort;
import org.mbari.siam.core.ServiceSandBox;
import org.mbari.siam.core.ChannelParameters;
import org.mbari.siam.core.ChannelRange;

import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.foce.deployed.FOCEAnalogBoard;

import org.mbari.siam.distributed.devices.AnalogBoard;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InitializeException;
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
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.PacketParser;

/**
 * Analog instrument using the Diamond Systems DMM-32X-AT Data acquisition board,
 * as configured for FOCE.
 */
public class AnalogInstrument extends PolledInstrumentService implements Instrument
{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(AnalogInstrument.class);

    AnalogAttributes _attributes = new AnalogAttributes(this);

    protected AnalogInstrumentPort _port;
    protected AnalogBoard _analogBoard = null;
    protected int _numChans = 1;
    protected int _sampleBytes = 10;
    protected double[] _results;
    protected PrintfFormat _format = new PrintfFormat(" %8.4lf");
    protected byte[] _nullBytes = "".getBytes();

    public AnalogInstrument() throws RemoteException {
	super();
    }


    public void initialize(NodeProperties nodeProperties, Parent parent,
			   InstrumentPort port, ServiceSandBox sandBox,
			   String serviceXMLPath, String servicePropertiesPath,
			   String cachedServicePath)

	throws MissingPropertyException, InvalidPropertyException,
	       PropertyException, InitializeException, IOException,
	       UnsupportedCommOperationException {

	if (!(port instanceof AnalogInstrumentPort))
	    throw new InitializeException("Not an AnalogInstrumentPort!");

	_port = (AnalogInstrumentPort)port;
	_analogBoard = _port.getAnalogBoard();
	_numChans = _port.numberOfChannels();
	_sampleBytes = 10 * _numChans;
	_results = new double[_numChans];

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
	ChannelRange[] channels=_port.getParams().getChannels();
	for(int i=0;i<channels.length;i++){
	    _analogBoard.analogSetup(channels[i].first(), _attributes.range,
				 _attributes.polarity, _attributes.gain);
	}
	_log4j.debug("initializeInstrument() - done");
    }

    /** Acquire A/D Data */
    protected double[] acquireAnalogData() throws IOException, NumberFormatException
    {
	int	r;

	if (_attributes.samplesToAvg > 1){

	    for (int i = 0; i < _numChans; i++){
		_results[i] = 0.0;
	    }
	    r=0;
	    ChannelRange[] channelRanges=_port.getParams().getChannels();
	    for(int i=0;i<channelRanges.length;i++){
		int startChannel=channelRanges[i].first();
		int rangeChans=channelRanges[i].length;
		for (int j = 0; j < _attributes.samplesToAvg; j++){
		    if (rangeChans > 1){		    
			double[] scan = _analogBoard.analogScan(startChannel, rangeChans);
			for (int k = 0; k < rangeChans; k++){
			    _results[r+k] += scan[k];
			}
		    }else{
			_results[r] += _analogBoard.analogSample(startChannel);
		    }
		    StopWatch.delay(_attributes.avgDelayMs);
		}
		for (int p = 0; p < rangeChans; p++){
		    _results[r+p] /= _attributes.samplesToAvg;
		}
		r+=rangeChans;
	    }
	}else{
	    r=0;
	    ChannelRange[] channelRanges=_port.getParams().getChannels();
	    for(int i=0;i<channelRanges.length;i++){
		int startChannel=channelRanges[i].first();
		int rangeChans=channelRanges[i].length;

		if (rangeChans > 1){		    
		    double[] scan = _analogBoard.analogScan(startChannel, rangeChans);
		    for (int k = 0; k < rangeChans; k++){
			_results[r+k] = scan[k];
		    }
		}else{
		    _results[r] = _analogBoard.analogSample(startChannel);
		}

		r+=rangeChans;
	    }
	}

	return(_results);
    }


    /** Acquire A/D Data	*/
    protected int readSample(byte[] sample) throws Exception
    {
	double[] results = acquireAnalogData();

	StringBuffer sb = new StringBuffer();
	
	for (int i = 0; i < results.length; i++)
	    sb.append(_format.sprintf(results[i]));

	byte[] rtn = sb.toString().getBytes();

	System.arraycopy(rtn, 0, sample, 0, rtn.length);

	return(rtn.length);
    }


    /** Return a PacketParser. */
    public PacketParser getParser() throws NotSupportedException
    {
	_log4j.debug("parseNames = " + _attributes.parseNames +
		     " parseUnits = " + _attributes.parseUnits);

	if ((_attributes.parseNames == null) ||
	    (_attributes.parseNames.length() == 0))
	    throw new NotSupportedException("No parseNames defined");

	String[] names = new String[_numChans];
	String[] units = new String[_numChans];

	StringTokenizer nameSt = new StringTokenizer(_attributes.parseNames);
	StringTokenizer unitSt = new StringTokenizer(_attributes.parseUnits);
	String nullStr = "";

	for (int i = 0; i < _numChans; i++)
	{
	    try {
		names[i] = nameSt.nextToken();
	    } catch (Exception e) {
		names[i] = nullStr;
	    }
	    try {
		units[i] = unitSt.nextToken();
	    } catch (Exception e) {
		units[i] = nullStr;
	    }
	    _log4j.debug("Chan " + i + ": " + names[i] + ", " +
			 units[i]);
	}

	return new AnalogPacketParser(_numChans, _attributes.registryName,
				      names, units);
    }

    /** Parse a SensorDataPacket into a double[] array */
    public Object parseDataPacket(SensorDataPacket pkt) throws InvalidDataException
    {
	// If this is the last packet we processed, just return a clone of the
	// _results array so we don't have to parse what we just wrote
	if (pkt == _sensorDataPacket)
	    return(_results.clone());

	String dataString = new String(pkt.dataBuffer());
	StringTokenizer st = new StringTokenizer(dataString);
	double[] results = new double[_results.length];

	for (int i = 0; i < results.length; i++)
	{
	    try {
		results[i] = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		String err = "Can't parse AnalogInstrument record: " + dataString;
		_log4j.error(err);
		throw new InvalidDataException(e.toString());
	    }
	}

	return(results);
    }


    /** Return metadata. */
    protected byte[] getInstrumentStateMetadata()
    {
	StringBuffer sb = new StringBuffer();

	sb.append(_analogBoard.getName());
	sb.append("\nChannel Ranges [first,last,span]:\n");
	ChannelRange[] channelRanges=_port.getParams().getChannels();
	for(int i=0;i<channelRanges.length;i++){
	    sb.append("  "+channelRanges[i]+"\n");
	}
	sb.append("numChans = ");
	sb.append(_numChans);
	sb.append("\nrange = ");
	sb.append(_attributes.range);
	sb.append("\npolarity = ");
	sb.append(_attributes.polarity);
	sb.append("\ngain = ");
	sb.append(_attributes.gain);
	sb.append("\nSamples to average = ");
	sb.append(_attributes.samplesToAvg);
	sb.append("\nDelay between samples = ");
	sb.append(_attributes.avgDelayMs);
	sb.append(" ms\n");

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

    /** Attributes for general Analog Instrument
     * @author Bob Herlien
     */
    class AnalogAttributes extends InstrumentServiceAttributes
    {
	AnalogAttributes(DeviceServiceIF service) {
	    super(service);
	}

	protected int range = 10;
	protected int polarity = 0;
	protected int gain = 2;
	protected int samplesToAvg = 1;
	protected int avgDelayMs = 10;
	public String registryName = "Analog";
	protected String parseNames;
	protected String parseUnits;

        /**
         * Throw InvalidPropertyException if any invalid attribute values found
         */
        public void checkValues() throws InvalidPropertyException {
	    if (samplesToAvg <= 0)
                throw new InvalidPropertyException("samplesToAvg must be > 0");

	    if (avgDelayMs <= 0)
                throw new InvalidPropertyException("avgDelayMs must be > 0");

	    try {
		_log4j.debug("Setting new attribute values: range=" + range +
			    " polarity=" + polarity + " gain=" + gain +
			     " samplesToAvg=" + samplesToAvg + " avgDelayMs="
			     + avgDelayMs);

		ChannelRange[] channels=_port.getParams().getChannels();
		_analogBoard.analogSetup(channels[0].first(), range, polarity, gain);
	    } catch (IOException e) {
		_log4j.error("IOException when setting Attribute(s): " + e);
	    }
	}
    }

} // end of class

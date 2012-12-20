/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.cpuTemp;

import java.util.Vector;
import java.util.Iterator;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.MiscInstrumentPort;
import org.mbari.siam.core.ServiceSandBox;

import org.mbari.siam.foce.deployed.IOMapper;
import org.mbari.siam.foce.deployed.FOCENodeConfigurator;

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
 * Instrument to return CPU temperature from Lippert CRR-LX800 CPU board.
 */
public class CPUTempService extends PolledInstrumentService implements Instrument
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(CPUTempService.class);

    CPUTempAttributes _attributes = new CPUTempAttributes(this);

    protected MiscInstrumentPort _instPort = null;
    protected IOMapper _ioMapper = null;
    protected String _params = null;
    protected byte[] _nullBytes = "".getBytes();

    public CPUTempService() throws RemoteException {
	super();
    }

    public void initialize(NodeProperties nodeProperties, Parent parent,
			   InstrumentPort port, ServiceSandBox sandBox,
			   String serviceXMLPath, String servicePropertiesPath,
			   String cachedServicePath)

	throws MissingPropertyException, InvalidPropertyException,
	       PropertyException, InitializeException, IOException,
	       UnsupportedCommOperationException {

	if (!(port instanceof MiscInstrumentPort))
	    throw new InitializeException("Not a MiscInstrumentPort!");

	_instPort = (MiscInstrumentPort)port;
	_params = _instPort.getParams();
	_ioMapper = IOMapper.getInstance();
	subclassInit();

	super.initialize(nodeProperties, parent, port, sandBox,
			 serviceXMLPath, servicePropertiesPath,
			 cachedServicePath);
    }

    /** Null method provided here so subclasses can override to provide additional initialization */
    protected void subclassInit() {}

    /** Specify device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return(0);
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
	return(8);
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
	_log4j.debug("CPUTempService.readSample()");

	byte[] result = _ioMapper.transact("CPUTemp\n").getBytes();

	System.arraycopy(result, 0, sample, 0, result.length);

	_log4j.debug("CPUTempService.readSample() returning " + result.length);
	return(result.length);
    }


    /** Return metadata. */
    protected byte[] getInstrumentStateMetadata()
    {
	return("CPUTempService".getBytes());
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
	return new CPUTempPacketParser(_attributes.registryName);
    }

    /** Attributes for CPU Temperature Instrument
     * @author Bob Herlien
     */
    class CPUTempAttributes extends InstrumentServiceAttributes
    {
	CPUTempAttributes(DeviceServiceIF service) {
	    super(service);
	}

	public String registryName = "CPUTemperature";

    }

} // end of class

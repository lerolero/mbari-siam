/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.analog;

import java.io.IOException;
import java.util.StringTokenizer;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Temperature;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.PacketParser;


/**
 * SBE18 implements an analog pH sensor driver for the Seabird SBE-18.
 * It extends AnalogInstrument, and uses temperature records from
 * the CTD to compute pH from the the voltages returned by the SBE-18.
 */
public class SBE18 extends AnalogInstrument
    implements Instrument, InstrumentDataListener
{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(SBE18.class);

    public static final double KELVIN = 273.15;
    public static final double K = 1.98416e-4;

    SBE18Attributes _attributes = new SBE18Attributes(this);

    /** Extended results (voltages, pH, temperature) */
    protected double[] _extResults;

    /** Temperature acquired */
    protected double _temp;

    /** pH correction values for each of (potentially) 8 channels */
    protected double[] _pHcorrections = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	

    /** Zero-arg constructor	*/
    public SBE18() throws RemoteException
    {
	super();
    }

    /** Allocate additional buffer space needed for converting volts to pH */
    protected void subclassInit()
    {
	_log4j.debug("subclassInit(), _numChans = " + _numChans);
	_extResults = new double[2*_numChans + 1];
	_sampleBytes = 20 * (_numChans + 1);
	_temp = _attributes.temperature;	/* Initialize temp to 4 deg C */
    }

    /** Register us for data callbacks from the temperature device */
    protected void initializeInstrument() throws InitializeException, Exception
    {
	InstrumentRegistry reg = InstrumentRegistry.getInstance();

	if(reg.registerDataCallback(this, _attributes.tempLookup) == null)
	    _log4j.warn("Temperature device not found");
    }

    /** Callback for InstrumentDataListener interface, called when the Temperature
	service is registered with the InstrumentRegistry
    */
    public void serviceRegisteredCallback(RegistryEntry entry)
    {
	_log4j.info("serviceRegisteredCallback for Temperature for SBE18");
    }

    /** dataCallback from the Temperature device */
    public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields)
    {
	try{
	    PacketParser.Field tempField = PacketParser.getField(fields, "temperature");
	    Number dtemp=((Number)tempField.getValue());
	    _temp = dtemp.doubleValue() + KELVIN;
	}catch(NoDataException e){
	    _log4j.error("NoDataException in dataCallback");
	}
    }

    /** Override acquireAnalogData() in base class.  Get the double[] array, and
     * create new one that adds pH's and temperature used for calculation
     */
    protected double[] acquireAnalogData() throws IOException, NumberFormatException
    {
	double[] volts = super.acquireAnalogData();
	int numChans = volts.length;

	if (_extResults.length < 2*numChans + 1)
	    _extResults = new double[2*numChans + 1];

	for (int i = 0; i < numChans; i++)
	{
	    _extResults[i] = volts[i];
	    _extResults[i+numChans] = 7.0 +
		(volts[i] - _attributes.offsets[i])/(K * _temp * _attributes.slopes[i]);

	    if (i < _pHcorrections.length)
		_extResults[i+numChans] += _pHcorrections[i];
	}

	_extResults[2*numChans] = _temp;

	return(_extResults);
    }


    /**
     * SBE18Attributes
     */
    public class SBE18Attributes extends AnalogInstrument.AnalogAttributes
    {
	/** Temperature in degrees Kelvin	*/
	protected double temperature = 277.15;

	public String registryName = "pH";

	public String tempLookup = "CTD1";

	/** 'offset', 'slope', and 'correction are arrays of doubles denoting
	 * the offset, slope values, or correction values for converting each SBE18 to pH.
	 */
	protected double[] offsets = {2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5};
	protected double[] slopes =  {4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0};
	protected double correction0 = 0.0;
	protected double correction1 = 0.0;
	protected double correction2 = 0.0;
	protected double correction3 = 0.0;
	protected double correction4 = 0.0;
	protected double correction5 = 0.0;
	protected double correction6 = 0.0;
	protected double correction7 = 0.0;

	public SBE18Attributes(DeviceServiceIF service) {
	    super(service);
	}

        public void checkValues() throws InvalidPropertyException
	{
	    _pHcorrections[0] = correction0;
	    _pHcorrections[1] = correction1;
	    _pHcorrections[2] = correction2;
	    _pHcorrections[3] = correction3;
	    _pHcorrections[4] = correction4;
	    _pHcorrections[5] = correction5;
	    _pHcorrections[6] = correction6;
	    _pHcorrections[7] = correction7;
	    super.checkValues();
	}
    }

} // end of class

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

import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;


/**
 * ScaledAnalog extends AnalogInstrument by implementing a slope (multiplier)
 * and offset (addend) to each measurement.
 */
public class ScaledAnalog extends AnalogInstrument implements Instrument
{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(ScaledAnalog.class);


    /** Zero-arg constructor	*/
    public ScaledAnalog() throws RemoteException
    {
	super();
	_attributes = new ScaledAnalogAttributes(this);
    }


    /** Override acquireAnalogData() in base class.  Apply scale & offset to double[] array of base driver.
     */
    protected double[] acquireAnalogData() throws IOException, NumberFormatException
    {
	double[] volts = super.acquireAnalogData();
	int numChans = volts.length;

	for (int i = 0; i < volts.length; i++)
	{
	    try {
		volts[i] = ((ScaledAnalogAttributes)_attributes).slopes[i] * volts[i] + 
		    	   ((ScaledAnalogAttributes)_attributes).offsets[i];
	    } catch (Exception e) {
		_log4j.warn("Exception in scaling analog result for channel " + i + ": " + e);
	    }
	}

	return(volts);
    }


    /**
     * ScaledAnalogAttributes
     */
    public class ScaledAnalogAttributes extends AnalogInstrument.AnalogAttributes
    {
	/** 'offsets' and 'slopes', are arrays of doubles denoting the scale factors to apply
	 * to each channel to get the final result.
	 */
	protected double[] offsets = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	protected double[] slopes =  {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

	public ScaledAnalogAttributes(DeviceServiceIF service) {
	    super(service);
	}

        public void checkValues() throws InvalidPropertyException
	{
	    if ((offsets.length < _numChans) || (slopes.length < _numChans))
		throw new InvalidPropertyException("Not enough slopes or offsets.");
	}
    }

} // end of class

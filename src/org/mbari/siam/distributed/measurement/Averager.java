/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/

package org.mbari.siam.distributed.measurement;

import org.mbari.siam.distributed.jddac.MutableIntegerArray;
import org.mbari.siam.distributed.jddac.SiamMeasurement;
import org.mbari.siam.distributed.jddac.SiamRecord;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/** Keep a running average of all passed-in numeric measurements. Summary
 for each measurement includes mean, standard deviation, number of samples,
 min value, max value, and times at which min and max values occurred. */
public class Averager extends SummarizerBlock {

    public final static String MEAN = "mean";
    public final static String STD_DEV = "stdDev";
    public final static String MIN_VALUE = "min";
    public final static String MIN_VALUE_TIME = "minTime";
    public final static String MAX_VALUE = "max";
    public final static String MAX_VALUE_TIME = "maxTime";
    public final static String NSAMPLES = "nSamples";
    public final static String START_TIME = "startTime";
    public final static String STOP_TIME = "stopTime";

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(Averager.class);

    /** SummaryRecord holds the Measurement summaries. */
    SummaryRecord _summary = new SummaryRecord();

    /** Create an Averager, summarize on specified schedule. */
    public Averager(ScheduleSpecifier schedule) 
	throws ScheduleParseException {
	super(schedule);
	resetSummary();
    }


    /** Create an Averager, summarize on every nth sample. */
    public Averager(int everyNthSample) {
	super(everyNthSample);
	resetSummary();
    }


    public void configure(ArgArray config) throws Exception {
	// Not yet implemented
    }


    /** Compute summary statistics and return to caller. */
    synchronized public SiamRecord computeSummary() {
	_summary.compute();
	return _summary;
    }


    /** Incorporate specified SiamRecord into SummaryRecord. */
    synchronized protected void addSampleRecord(SiamRecord payload) 
	throws Exception {

	Enumeration elements = payload.elements();

	for (int n = 0; elements.hasMoreElements(); n++) {

	    Object element = elements.nextElement();

	    if (!(element instanceof SiamMeasurement)) {
		_log4j.info("element " + n + " is a " + 
			    element.getClass().getName() + 
			    ", not a SiamMeasurement");
		continue;
	    }

	    SiamMeasurement measurement = (SiamMeasurement )element;


	    // Check whether this one should be summarized
	    if (!include(measurement.getName())) {
		_log4j.debug("Skipping measurement " + measurement.getName());
		continue;
	    }

	    _log4j.debug("process SiamMeasurement " + 
			 measurement.getName() + " (" + measurement.getUnits() + ")");

	    // Is this measurement name already in summary record? If not then 
	    // add it to the record
	    MeasurementSummary measurementSummary = 
		(MeasurementSummary )_summary.get(measurement.getName());

	    if (measurementSummary == null) {

		// Create new measurement summary
		measurementSummary = 
		    new MeasurementSummary(measurement.getName(), 
					   measurement.getUnits());

		// Add new measurement summary object to the summary record
		_summary.put(measurement.getName(), 
			     measurementSummary);
	    }

	    try {
		measurementSummary.addSample(payload.getTimeStamp(), 
					     measurement);

		_summary.defineTimeInterval(payload.getTimeStamp());
	    }
	    catch (Exception e) {
		_log4j.error(e);
		continue;
	    }
	}
    }


    /** Reset statistics */
    synchronized public void resetSummary() {

	_summary.resetSummary();

    }


    /** Return false if this measurement should be skipped, else 
	return true. */
    protected boolean include(String measurementName) {

	// NOT YET IMPLEMENTED
	return true;
    }


    /** Holds summary statistics for a measurement */
    static class MeasurementSummary 
	extends SiamMeasurement 
	implements Serializable {

	protected String _name;
	protected Vector _samples = new Vector();
	protected double _mean;
	protected double _stdDeviation;

	/** Minimum value */
	protected double _minimum;
	/** Time-tag of minimum value. */
	protected long _minimumTime;

	/** Maximum value */
	protected double _maximum;
	/** Time-tag of maximum value. */
	protected long _maximumTime;

	MeasurementSummary(String name, String units) {
            super(name);            
            if (units == null) {
                if (_log4j.isDebugEnabled()) {
                    _log4j.warn("Attempted to add a measurement with out units. Using 'Unknown'");
                }
                name = "Unknown";
            }
	    put(MeasAttr.UNITS, units);
	    resetSummary();
	}

	/** Reset field values. */
	synchronized void resetSummary() {
	    _samples.clear();
	    _mean = _stdDeviation = Double.NaN;
	    _minimum = Double.MAX_VALUE;
	    _maximum = -Double.MAX_VALUE;
	}


	/** Add a sample to the summary. */
	synchronized void addSample(long timeStamp, 
				    SiamMeasurement measurement) 
	    throws Exception {

	    Object value = null;
	    if ((value = measurement.get(MeasAttr.VALUE)) == null) {

		throw new Exception ("Measurement " + 
				     measurement.getName() + 
				     " doesn't contain value");
	    }

	    if (!(value instanceof Number) && 
		!(value instanceof MutableIntegerArray)) {

		throw new Exception("Value of " + measurement.getName() + 
				    " is not a Number " + 
				    "and not a MutableIntegerArray");
	    }
	    else if (value instanceof Number) {
		_log4j.debug(measurement.getName() + " has a Number value");
	    }
	    else if (value instanceof MutableIntegerArray) {
		throw new Exception (measurement.getName() + 
				     " has a MutableIntegerArray - not yet implemented");
	    }

	    Double dvalue = 
		new Double(((Number )measurement.get(MeasAttr.VALUE)).doubleValue());

	    _samples.addElement(dvalue);

	    if (dvalue.doubleValue() < _minimum) {
		_minimum = dvalue.doubleValue();
		_minimumTime = timeStamp;
	    }
	    if (dvalue.doubleValue() > _maximum) {
		_maximum = dvalue.doubleValue();
		_maximumTime = timeStamp;
	    }
	}


	/** Compute summary statistics for measurement. */
	synchronized void compute() throws Exception {

	    if (_samples.size() <= 0) {
		throw new Exception(_name + ": nSamples=0");
	    }
	    else if (_samples.size() == 1) {
		_mean = ((Double )_samples.elementAt(0)).doubleValue();
		_stdDeviation = 0.;
	    }
	    else {
		// Compute mean and standard deviation
		double sum = 0.;
		for (int i = 0; i < _samples.size(); i++) {
		    sum += ((Double )_samples.elementAt(i)).doubleValue();
		}

		_mean = sum / _samples.size();

		sum = 0.;

		for (int i = 0; i < _samples.size(); i++) {

		    double sample = 
			((Double )_samples.elementAt(i)).doubleValue();
		    
		    // Sum of squared deviations
		    sum += Math.pow((sample - _mean), 2.);
		}

		_stdDeviation = Math.sqrt(sum /( _samples.size() - 1));
	    }

	    // Set attributes in this summary measurement
	    put(MEAN, new Double(_mean));
	    put(STD_DEV, new Double(_stdDeviation));
	    put(MIN_VALUE, new Double(_minimum));
	    put(MIN_VALUE_TIME, new Long(_minimumTime));
	    put(MAX_VALUE, new Double(_maximum));
	    put(MAX_VALUE_TIME, new Long(_maximumTime));
	    put(NSAMPLES, _samples.size());
	}
    }

    /** Record to hold measurement summaries. */
    class SummaryRecord 
	extends SiamRecord 
	implements Serializable {

	long _startTime = 0;
	long _stopTime = 0;

	/** Create */
	SummaryRecord() {
	    resetSummary();
	}

	/** Reset statistics */
	synchronized protected void resetSummary() {
	    _startTime = Long.MAX_VALUE;
	    _stopTime = -Long.MAX_VALUE;

	    Enumeration elements = elements();

	    for (int n = 0; elements.hasMoreElements(); n++) {
		Object element = elements.nextElement();
		if (element instanceof MeasurementSummary) {
		    ((MeasurementSummary )element).resetSummary();
		}
	    }
	}


	/** Compute mean and standard deviation for all fields in summary 
	    record. */
	public void compute() {

	    Enumeration elements = elements();

	    for (int n = 0; elements.hasMoreElements(); n++) {
		Object element = elements.nextElement();
		if (element instanceof MeasurementSummary) {
		    MeasurementSummary measurementSummary = 
			(MeasurementSummary )element;

		    try {
			measurementSummary.compute();
		    }
		    catch (Exception e) {
			_log4j.error(measurementSummary.getName() + ": " + e);
		    }
		}
	    }
	}



	/** Keep track of summary time interval. */
	void defineTimeInterval(long t) {
	    if (t < _startTime) {
		_startTime = t;
	    }
	    if (t > _stopTime) {
		_stopTime = t;
	    }
	}


	/** Generate String representation of summary. */
	public String toString() {

	    StringBuffer buf = new StringBuffer(super.toString());
	    buf.append("startTime: " + _startTime + "\n");
	    buf.append("stopTime: **** " + _stopTime + "\n");
	    return new String(buf);
	}
    }
}

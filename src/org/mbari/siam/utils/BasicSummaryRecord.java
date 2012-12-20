/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.text.ParseException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;

/**
   BasicSummaryRecord consists of multiple fields representing numeric values.
   each field stores the mean, standard deviation, minimum, 
   and maximum.
 */
public class BasicSummaryRecord {

    protected boolean _recompute = false;

    private static Logger _logger = Logger.getLogger(BasicSummaryRecord.class);

    // Minimum sample timetag
    protected long _startTime = Long.MAX_VALUE;
    // Maximum sample timetag
    protected long _stopTime = -Long.MAX_VALUE;

    // Vector of Fields, each Field characterized by a name. There is a 
    // field corresponding to all numeric fields in all added samples.
    protected Vector _fields = new Vector(100);


    public BasicSummaryRecord() {
	reset();
    }

    public void reset() {
	_fields.clear();
	_startTime = Long.MAX_VALUE;
	_stopTime = -Long.MAX_VALUE;
	_recompute = false;
    }

    /** Add another sample to the statistics */
    public void addSample(DevicePacket packet, PacketParser parser) 
	throws NotSupportedException, ParseException {

	if (parser == null) {
	    throw new NotSupportedException("addSample() - null parser");
	}

	// Process all fields in samle 
	PacketParser.Field sampleField[] = parser.parseFields(packet);

	boolean addedField = false;

	for (int i = 0; i < sampleField.length; i++) {

	    // Is this a numerical record?
	    if (!(sampleField[i].getValue() instanceof Number)) {
		// Not a number - no need to process
		continue;
	    }

	    if (!summarizeField(sampleField[i].getName())) {
		// Don't summarize this field
		continue;
	    }

	    Field summaryField = null;

	    // Is this field already present in summary?
	    for (int j = 0; j < this._fields.size(); j++) {

		Field field = (Field )_fields.elementAt(j);

		if (sampleField[i].getName().equals(field._name)) {
		    summaryField = field;

		    break;
		}
	    }

	    if (summaryField == null) {
		// Sample field not yet included in the summary; create it
		// and add it to summary fields vector.
		summaryField = new Field(sampleField[i].getName());
		_fields.addElement(summaryField);
	    }

	    try {
		summaryField.addSample(sampleField[i]);
		addedField = true;
		_recompute = true;
	    }
	    catch (Exception e) {
		_logger.error(e);
	    }

	}
	if (addedField) {
	    if (packet.systemTime() < _startTime) {
		_startTime = packet.systemTime();
	    }
	    if (packet.systemTime() > _stopTime) {
		_stopTime = packet.systemTime();
	    }
	}


    }

    /** Compute mean and standard deviation for all fields in summary 
	record. */
    public void compute() {
	for (int i = 0; i < _fields.size(); i++) {
	    Field field = (Field )_fields.elementAt(i);
	    try {
		field.compute();
	    }
	    catch (Exception e) {
		_logger.error("compute(): " + e);
	    }
	}
	_recompute = false;
    }


    
    /** Return true if specified field should be summarized; 
	else return false. By default, always returns true; subclass
	can override method to summarize only specific fields. */
    public boolean summarizeField(String fieldName) {
	return true;
    }

    /** Generate string representation of record. */
    public String toString() {

	StringBuffer summary = new StringBuffer("Summary:\n");
	for (int i = 0; i < _fields.size(); i++) {
	    Field field = (Field )_fields.elementAt(i);
	    summary.append(field.toString() + "\n");
	}
	return new String(summary);
    }


    /** Field consists of basic statistics*/
    class Field {
	protected String _name;
	protected Vector _samples = new Vector();
	protected double _mean;
	protected double _stdDeviation;
	protected double _minimum;
	protected double _maximum;

	/** Generate string representation. */
	public String toString() {
	    StringBuffer field = new StringBuffer(_name + ";");
	    
	    if (Double.isNaN(_mean)) {

		try {
		    compute();
		}
		catch (Exception e) {
		    field.append("Exception while computing: " + e.getMessage());
		}
	    }

	    field.append("nSamples=" + _samples.size() + ";");
	    field.append("mean=" + _mean + ";");
	    field.append("stdDev=" + _stdDeviation + ";");
	    field.append("min=" + _minimum + ";");
	    field.append("max=" + _maximum + ";");

	    return new String(field);
	}

	/** Create new Field with specified name. */
	Field(String name) {
	    _name = name;
	    reset();
	}


	/** Reset field values. */
	void reset() {
	    _samples.clear();
	    _mean = _stdDeviation = Double.NaN;
	    _minimum = Double.MAX_VALUE;
	    _maximum = -Double.MAX_VALUE;
	}


	/** Add a sample to the summary. */
	void addSample(PacketParser.Field field) throws Exception {

	    if (!(field.getValue() instanceof Number)) {
		// Only numbers are allowed
		throw new Exception("Added field value is not a number");
	    }

	    Double value = 
		new Double(((Number )field.getValue()).doubleValue());

	    _samples.addElement(value);

	    if (value.doubleValue() < _minimum) {
		_minimum = value.doubleValue();
	    }
	    if (value.doubleValue() > _maximum) {
		_maximum = value.doubleValue();
	    }
	}

	/** Compute statistics for field. */
	void compute() throws Exception {

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
	}
    }
}

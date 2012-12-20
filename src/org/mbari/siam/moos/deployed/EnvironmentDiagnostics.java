/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.deployed;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.mbari.siam.distributed.devices.Environmental;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NoDataException;


/** Encapsulate node's environmmental diagnostics. */
public class EnvironmentDiagnostics {

    MOOSNodeService _nodeService;

    // Time at which tracking started
    long _startTime;

    static private Logger _logger = 
	Logger.getLogger(EnvironmentDiagnostics.class);

    static SimpleDateFormat _dateFormat = 
	new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

	static NumberFormat _nf=NumberFormat.getInstance();
	
    DiagnosticRecord _currentRecord = new DiagnosticRecord("current");

    // These contain extreme sampled values
    DiagnosticRecord _minGfLowRecord = new DiagnosticRecord("minGfLo");
    DiagnosticRecord _maxGfLowRecord = new DiagnosticRecord("maxGfLo");
    DiagnosticRecord _minGfHighRecord = new DiagnosticRecord("minGfHi");
    DiagnosticRecord _maxGfHighRecord = new DiagnosticRecord("maxGfHi");
    DiagnosticRecord _minTmprtRecord = new DiagnosticRecord("minTmprt");
    DiagnosticRecord _maxTmprtRecord = new DiagnosticRecord("maxTmprt");
    DiagnosticRecord _minPressRecord = new DiagnosticRecord("minPress");
    DiagnosticRecord _maxPressRecord = new DiagnosticRecord("maxPress");
    DiagnosticRecord _minHumidRecord = new DiagnosticRecord("minHumid");
    DiagnosticRecord _maxHumidRecord = new DiagnosticRecord("maxHumid");
    DiagnosticRecord _minTurnsRecord = new DiagnosticRecord("minTurns");
    DiagnosticRecord _maxTurnsRecord = new DiagnosticRecord("maxTurns");

    int _nSamples = 0;

    EnvironmentDiagnostics(MOOSNodeService nodeService) {
	_nodeService = nodeService;
	reset();
    }

    /** Run diagnostics. */
    protected void run(String note) throws Exception {
	// Run ground fault test on Environmental processor
	_logger.debug("run diagnostics (" + note + ")");

	Environmental environmental = null;
	try {
	    environmental = _nodeService.getEnvironmental();
	}
	catch (DeviceNotFound e) {
	    String err = 
		"runDiagnostic(): could not find Environmental service";

	    _logger.info(err);
	    throw new Exception(err);
	}

	try {
	    _logger.debug("run() - get environmental data");
	    Environmental.Data data = environmental.getDataValues(false);
	    _currentRecord.setData(data, note);

	    _nSamples++;
	    _logger.debug("run() - got environmental data");

	    _logger.debug(_currentRecord);
		
	    // Look for "low-side" ground fault extremes
	    _logger.debug("minGfLow: " + _minGfLowRecord);
	    if (data.groundFaultLow < 
		_minGfLowRecord.data.groundFaultLow) {
		_minGfLowRecord.setData(data, note);
	    }
	    _logger.debug("maxGfLow: " + _maxGfLowRecord);
	    if (data.groundFaultLow > 
		_maxGfLowRecord.data.groundFaultLow) {
		_maxGfLowRecord.setData(data, note);
	    }

	    // Look for "high-side" ground fault extremes
	    _logger.debug("minGfHigh: " + _minGfHighRecord);
	    if (data.groundFaultHigh < 
		_minGfHighRecord.data.groundFaultHigh) {
		_minGfHighRecord.setData(data, note);
	    }
	    _logger.debug("maxGfHigh: " + _maxGfHighRecord);
	    if (data.groundFaultHigh > 
		_maxGfHighRecord.data.groundFaultHigh) {
		_maxGfHighRecord.setData(data, note);
	    }

	    // Look for temperature extremes
	    if (data.temperature < 
		_minTmprtRecord.data.temperature) {
		_minTmprtRecord.setData(data, note);
	    }
	    if (data.temperature > 
		_maxTmprtRecord.data.temperature) {
		_maxTmprtRecord.setData(data, note);
	    }

	    // Look for humidity extremes
	    if (data.humidity < 
		_minHumidRecord.data.humidity) {
		_minHumidRecord.setData(data, note);
	    }
	    if (data.humidity > 
		_maxHumidRecord.data.humidity) {
		_maxHumidRecord.setData(data, note);
	    }

	    // Look for pressure extremes
	    if (data.pressure < 
		_minPressRecord.data.pressure) {
		_minPressRecord.setData(data, note);
	    }
	    if (data.pressure > 
		_maxPressRecord.data.humidity) {
		_maxPressRecord.setData(data, note);
	    }

	    // Look for compass turns
	    if (data.turnsCount < 
		_minTurnsRecord.data.turnsCount) {
		_minTurnsRecord.setData(data, note);
	    }
	    if (data.turnsCount > 
		_maxTurnsRecord.data.turnsCount) {
		_maxTurnsRecord.setData(data, note);
	    }



	}
	catch (NoDataException e) {
	    throw new Exception("NoDataException: " + e.getMessage());
	}
	catch (Exception e) {
	    _logger.error(e);
	    throw e;
	}
    }



    /** Reset min/max values. */
    protected void reset() {
	_nSamples = 0;

	_minGfLowRecord.resetMin();
	_minGfHighRecord.resetMin();
	_minTmprtRecord.resetMin();
	_minPressRecord.resetMin();
	_minHumidRecord.resetMin();
	_minTurnsRecord.resetMin();

	_maxGfLowRecord.resetMax();
	_maxGfHighRecord.resetMax();
	_maxTmprtRecord.resetMax();
	_maxPressRecord.resetMax();
	_maxHumidRecord.resetMax();
	_maxTurnsRecord.resetMax();

	_startTime = System.currentTimeMillis();
    }


    String environmentalString(Environmental.Data data) {
		_nf.setMinimumIntegerDigits(1);
	    _nf.setMinimumFractionDigits(2);
	    _nf.setMaximumFractionDigits(2);
	    _nf.setGroupingUsed(false);
		
	return 
	    _nf.format(data.temperature) + ";" + 
	    _nf.format(data.pressure) + ";" + 
	    _nf.format(data.humidity) + ";" + 
	    _nf.format(data.groundFaultHigh) + ";" + 
	    _nf.format(data.groundFaultLow) + ";" + 
	    _nf.format(data.turnsCount) + ";" + 
	    _dateFormat.format(new Date(data.timestamp));
    }

    /** Get status summary. */
    String getStatusSummary() {

	_logger.debug("getStatusSummary()");

	if (_nSamples == 0) {
	    return "Env not yet sampled";
	}

	String msg = 
	    "name;tmprt;pressure;humidity;gfHi;gfLo;nTurns;time;note\n";
		
	msg += _currentRecord.toString() + "\n";
	msg += "nSample=" + _nSamples + "\n";

	if (_nSamples <= 1) {
	    return msg;
	}
		
	msg += _minGfLowRecord + "\n" + 
   _maxGfLowRecord + "\n" + 
	_minGfHighRecord + "\n" + 
	_maxGfHighRecord + "\n" +
	_minTmprtRecord + "\n" + 
	_maxTmprtRecord + "\n" +
	_minPressRecord + "\n" + 
	_maxPressRecord + "\n" +
	_minHumidRecord + "\n" + 
	_maxHumidRecord + "\n" +
	_minTurnsRecord + "\n" + 
	_maxTurnsRecord + "\n";


	// Reset min/max values for next cycle
	reset();

	return msg;
    }



    /** Contains environmental data, application-defined note. */
    class DiagnosticRecord {
	    
	String name;
	Environmental.Data data;
	String note;

	DiagnosticRecord(String name) {
	    this.name = name;
	    this.note = "";
	    this.data = new Environmental.Data();
	}

	void setData(Environmental.Data data, String note) {
	    this.data = (Environmental.Data )data.clone();
	    this.note = new String(note);
	}

	/** Generate string representation. */
	public String toString() {

	    return name + ";" + environmentalString(data) + 
		";" + note + ";";
	}


	// Reset for maximum value tracking
	void resetMax() {
	    data.temperature = data.pressure = data.humidity = 
		data.groundFaultHigh = data.groundFaultLow = 
		-Float.MAX_VALUE;

	    data.turnsCount = -Integer.MAX_VALUE;
	}


	// Reset for minimum value tracking
	void resetMin() {
	    data.temperature = data.pressure = data.humidity = 
		data.groundFaultHigh = data.groundFaultLow = 
		Float.MAX_VALUE;

	    data.turnsCount = Integer.MAX_VALUE;
	}
    }
}

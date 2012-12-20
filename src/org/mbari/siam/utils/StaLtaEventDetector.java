/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.LinkedList;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
   Detect events based on ratio of moving short-term average ("STA") to 
   moving long-term average ("LTA") of some input data. Detector is in
   "triggered" state when STA/LTA exceeds specified "trigger ratio".
   Detector is in "de-triggered" state when STA/LTA is less than specified
   "de-trigger" ratio. Notify listeners when state transitions between 
   triggered and de-triggered states. Based on seismology algorithm 
   described in "Instrumentation in Earthquake Seismology" by Havskov and
   Alguacil, p 117 (available at http://www.terraip.co.jp/Seismometers.pdf)
 */
public class StaLtaEventDetector {

    protected static Logger _log4j = 
	Logger.getLogger(StaLtaEventDetector.class);

    protected String _parameterName;
    protected int _staSampleWidth;
    protected int _ltaSampleWidth;

    protected long _nSample = 0;

    /** Maximum consecutive sample accumulated in un-triggered state */
    protected int _maxTriggeredSamples;

    protected float _triggerRatio;
    protected float _deTriggerRatio;
    protected double _sta = 0.;
    protected double _lta = 0.;
    protected LinkedList _staSamples = new LinkedList();
    protected LinkedList _ltaSamples = new LinkedList();
    protected int _nConsecTriggeredSamples = 0;
    protected boolean _triggeredState = false;
    protected boolean _ready = false;
    protected long _transitionEpochMsec = 0;

    /** Event listeners */
    protected Vector _listeners = new Vector();


    public StaLtaEventDetector(String parameterName, 
			       int staSampleWidth, int ltaSampleWidth,
			       float triggerRatio, float deTriggerRatio,
			       int maxTriggeredSamples) 
	throws Exception {

	_parameterName = new String(parameterName);

	// Set parameters; this will throw exception if parameters are invalid
	setParameters(staSampleWidth, ltaSampleWidth, 
		      triggerRatio, deTriggerRatio, 
		      maxTriggeredSamples);
    }

    /** Return String representation */
    public String toString() {
	String output =  new String("dataStream: " + _parameterName +
				    ", STA sample width: " + _staSampleWidth + 
				    ", LTA sample width: " + _ltaSampleWidth +
				    ", trigger ratio: " + _triggerRatio +
				    ", de-trigger ratio: " + _deTriggerRatio +
				    ", nSamples: " + _nSample +
				    ", STA: " + _sta + 
				    ", LTA: " + _lta + 
				    ", ready: " + _ready +
				    ", triggered: " + _triggeredState);

	if (_triggeredState) {
	    // Append info about max triggered 
	    output += ", # triggered samples: " + _nConsecTriggeredSamples + 
		", max triggered samples: " + _maxTriggeredSamples;
	}

	return output;
    }


    public double getLTA() {
	return _lta;
    }

    public double getSTA() {
	return _sta;
    }

    /** Return time of last transition, in epoch millsec */
    public long getTransitionTime() {
	return _transitionEpochMsec;
    }

    public long getNsamples() {
	return _nSample;
    }

    /** Reset; set averages to zero, restart accumulation */
    public void reset() {
	_ready = false;
	_triggeredState = false;
	_staSamples.clear();
	_ltaSamples.clear();
	_sta = _lta = 0.;
    }


    /** Return true if detector is in 'triggered' state, else return false. */
    public boolean triggeredState() {
	return _triggeredState;
    }


    /** Set parameters. If input parameters are invalid, throws Exception
     without changing current parameters. */
    public void setParameters(int staSampleWidth, int ltaSampleWidth,
			      float triggerRatio, float deTriggerRatio,
			      int maxTriggeredSamples) 
	throws Exception {

	checkParameters(staSampleWidth, ltaSampleWidth,
			triggerRatio, deTriggerRatio, maxTriggeredSamples);

	
	_staSampleWidth = staSampleWidth;
	_ltaSampleWidth = ltaSampleWidth;
	_triggerRatio = triggerRatio;
	_deTriggerRatio = deTriggerRatio;
	_maxTriggeredSamples = maxTriggeredSamples;

	reset();
    }


    /** Check that user-set parameters are valid. */
    protected void checkParameters(int staSampleWidth, int ltaSampleWidth,
				   float triggerRatio, float deTriggerRatio,
				   int maxTriggeredSamples) 
	throws Exception {

	String error = "";

	if (staSampleWidth <= 0 || ltaSampleWidth <= 0) {
	    error += "STA and LTA widths must be positive. ";
	}

	if (staSampleWidth >= ltaSampleWidth) {
	    error += "LTA width must exceed STA width. ";
	}
	else if (triggerRatio <= deTriggerRatio) {
	    error += "Trigger ratio must exceed de-trigger ratio. ";
	}

	if (maxTriggeredSamples < 1) {
	    error += "Max triggered samples must be positive. ";
	}

	if (error.length() > 0) {
	    throw new Exception(error);
	}
    }

    /** Return running average */
    protected double getRunningAverage(LinkedList samples, 
				       Number newSample, int width,
				       double prevAverage) {

	samples.addLast(newSample);
	double newAverage = 0.;

	if (samples.size() > width) {
	    Number oldestSample = 
		(Number )samples.get(samples.size() - width - 1);

	    _log4j.debug("oldest sample: " + oldestSample + 
			 ", latest sample: " + 
			 samples.get(samples.size()-1));

	    // Compute moving STA, by removing oldest sample and adding
	    // latest sample
	    newAverage = 
		prevAverage  + (newSample.doubleValue() - 
				oldestSample.doubleValue()) / width;

	    // Remove oldest sample from the list
	    samples.removeFirst();
	}
	else {
	    // Window not full yet

	    // Previous sum
	    double prevSum = prevAverage * (samples.size() - 1);

	    newAverage = (prevSum + newSample.doubleValue()) / samples.size();
	}

	return newAverage;
    }


    /** Add next sample, compute moving averages and evaluate state */
    public void addSample(Number newSample, long epochMsecTimestamp) 
	throws Exception {

	_nSample++;

	_log4j.debug("addSample " + newSample);

	_sta = getRunningAverage(_staSamples, newSample, _staSampleWidth,
				 _sta);

	_log4j.debug("STA: " + _sta + ", nSample: " + _nSample);

	// LTA window is full, so can compute state
	if (_ltaSamples.size() >= _ltaSampleWidth) {
	    if (!_ready) {
		_log4j.debug("LTA window is full - begin computing state");
		_ready = true;
	    }

	    if (_lta == 0.) {
		// Can't compute ratio; just return
		_log4j.error("LTA = 0, can't compute ratio yet");
		return;
	    }

	    double ratio = _sta / _lta;
	    _log4j.debug(this.toString());
	    _log4j.debug("STA/LTA: " + ratio);

	    if (ratio >= _triggerRatio) {
		if (!_triggeredState) {
		    trigger(epochMsecTimestamp);
		}
	    }
	    else if (ratio <= _deTriggerRatio) {
		if (_triggeredState) {
		    deTrigger(epochMsecTimestamp);
		}
	    }
	}

	// Only accumulate LTA when in non-triggered state
	if (!_triggeredState) {

	    _lta = getRunningAverage(_ltaSamples, newSample,
				     _ltaSampleWidth, _lta);

	    _log4j.debug("LTA: " + _lta);

	    _nConsecTriggeredSamples = 0;
	}
	else {
	    _nConsecTriggeredSamples++;
	    if (_nConsecTriggeredSamples > _maxTriggeredSamples) {
		_log4j.warn("Exceeded " + _maxTriggeredSamples + 
			    " samples in triggered state; " + 
			    "transition to non-triggered state");

		reset();
		deTrigger(epochMsecTimestamp);
	    }
	}
    }


    /** Transition to triggered state */
    protected void trigger(long epochMsec) {
	// Transition from de-triggered to triggered state
	_log4j.info("Transition to TRIGGERED state at sample #" + 
		    _nSample);

	_triggeredState = true;
	_transitionEpochMsec = epochMsec;
	notifyListeners(_triggeredState);
    }


    /** Transition to detriggered state */
    protected void deTrigger(long epochMsec) {
	// Transition from triggered to de-triggered state
	_log4j.info("Transition to DE-TRIGGERED state at sample #" +
		    _nSample);

	_triggeredState = false;
	_transitionEpochMsec = epochMsec;
	notifyListeners(_triggeredState);
    }


    /** Notify listeners of state transition */
    protected void notifyListeners(boolean triggeredState) {
	for (int i = 0; i < _listeners.size(); i++) {
	    Listener listener = (Listener )_listeners.elementAt(i);
	    if (triggeredState) {
		listener.triggeredCallback(this);
	    }
	    else {
		listener.detriggeredCallback(this);
	    }
	}
    }


    /** Add event listener */
    public void addListener(StaLtaEventDetector.Listener listener) {

	_listeners.add(listener);
    }


    /** Interface for event listeners */
    public interface Listener {

	/** Called on state transition to triggered */
	public void triggeredCallback(StaLtaEventDetector detector);

	/** Called on state transition to de-triggered */
	public void detriggeredCallback(StaLtaEventDetector detector);
    }

}

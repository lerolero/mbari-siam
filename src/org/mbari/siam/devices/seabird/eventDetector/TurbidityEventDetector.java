/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.eventDetector;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.io.OutputStream;
import org.apache.log4j.Logger;
import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.distributed.ShoreMessaging;
import org.mbari.siam.distributed.ShoreMessagingHelper;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.core.NodeManager;
import org.mbari.siam.distributed.NodeConfigurator;
import org.mbari.siam.distributed.NetworkManager;
import org.mbari.siam.utils.StaLtaEventDetector;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;

/**
   Detects turbidity events based on transmissometer data.
 */
public class TurbidityEventDetector 
    implements StaLtaEventDetector.Listener {

    static private Logger _log4j = 
	Logger.getLogger(TurbidityEventDetector.class);

    protected StaLtaEventDetector _eventDetector = null;
    boolean _enabled = false;

    /** Flag to enable/disable use of shore messaging service */
    boolean _shoreMsgEnabled = false;

    /** Use this object to wake up other nodes */
    NetworkManager _network;

    /** This is the ShoreMessaging service host */
    String _modemHostName;
    InetAddress _modemHost;

    int _shoreMsgSrvcTimeoutMsec = 120000;

    /** Interval between samples. */
    int _sampleIntervalSec = 0;

    /** Annotate service log with detector status at this interval */
    int _statusIntervalSec = 3600;

    int _secSinceLastStatus = 0;

    private boolean _useThreadPool = true;

    BaseInstrumentService _seabirdService;

    Parameters _parameters;

    Dispatcher _dispatcher;

    ExecutorService _threadPool = Executors.newFixedThreadPool(10);

    public TurbidityEventDetector(BaseInstrumentService seabirdService,
				  Parameters params, int statusIntervalSec) 
	throws Exception {

	_seabirdService = seabirdService;
	_network = NodeManager.getInstance().getNodeConfigurator().createNetworkManager("");
	_parameters = params;
	_statusIntervalSec = statusIntervalSec;

	if (_useThreadPool) {
	    _log4j.info("Use thread pool");
	}
	else {
	    _log4j.info("Do NOT use thread pool; new dispatch thread each time");
	}
    }


    public void initialize(String modemHostName, int sampleIntervalSec) 
	throws Exception
    {

	_sampleIntervalSec = sampleIntervalSec;

	// Event detector will notify this object on transition between
	// triggered and de-triggered states
	_eventDetector = createEventDetector(_sampleIntervalSec);

	_modemHostName = new String(modemHostName);
	try {
	    _modemHost = InetAddress.getByName(_modemHostName);
	}
	catch (Exception e) {
	    _log4j.error("Couldn't get address of modem host: " + _modemHostName);
	    _log4j.error("NULL modemHost");
	    _seabirdService.annotate(("ERROR: couldn't get address of modem host: " + 
				      _modemHostName).getBytes());
	}


	_dispatcher = new Dispatcher(_network, _modemHost);
    }


    /** Create new event detector */
    StaLtaEventDetector createEventDetector(int sampleIntervalSec)
	throws Exception {

	if (sampleIntervalSec >= _parameters._staWidthSec || 
	    sampleIntervalSec >= _parameters._ltaWidthSec) {
	    
	    String errMsg = "Event detector disabled: sample interval (" + 
		sampleIntervalSec + "sec) exceeds staWidth (" + 
		_parameters._staWidthSec + 
		" sec)";

	    _log4j.warn(errMsg);
	    dispatchMessage(errMsg);

	    _enabled = false;

	    throw new Exception(errMsg);
	}

	int staSampleWidth = _parameters._staWidthSec / sampleIntervalSec;
	int ltaSampleWidth = _parameters._ltaWidthSec / sampleIntervalSec;
	int maxTriggeredSamples = 
	    _parameters._maxTriggeredSec / sampleIntervalSec;

	StaLtaEventDetector eventDetector = 
	    new StaLtaEventDetector("attenuation",
				    staSampleWidth, 
				    ltaSampleWidth,
				    _parameters._attenuationTriggerRatio,
				    _parameters._attenuationDetriggerRatio,
				    maxTriggeredSamples);

	// Event detector will notify this object on trigger state transition
	eventDetector.addListener(this);

	_enabled = true;

	return eventDetector;
    }


    /** Return true if event detector is enabled; else false */
    public boolean enabled() {
	return _enabled;
    }


    /** Invoked by STA/LTA event detector on detriggered->triggered 
	transition */
    public void triggeredCallback(StaLtaEventDetector detector) {

	String msg = 
	    "TurbidityEventDetector triggered: " + detector.toString();

	// Launch thread to queue message with shore messaging service
	dispatchMessage(msg);
   }


    /** Queue specified message for downlink through shore-messaging service */
    public void dispatchMessage(String msg) {

	_seabirdService.annotate(msg.getBytes());

	_log4j.debug("shoreMsgEnabled = " + _shoreMsgEnabled);

	if (_shoreMsgEnabled) {

	    _dispatcher.setMessage(msg.getBytes());

	    if (_useThreadPool) {

		try {
		    // Launch thread to queue message with shore messaging 
		    // service
		    _log4j.debug("dispatchMessage() - get thread from pool");
		    _threadPool.execute(_dispatcher);
		}
		catch (RejectedExecutionException e) {
		    _log4j.error("Couldn't execute dispatcher thread: " + e);
		}
	    }
	    else {
		// Don't use thread pool
		_log4j.debug("dispatchMessage() - create new thread");
		new Thread(_dispatcher).start();
	    }
	}
    }


    /** Invoked by STA/LTA event detector on triggered->detriggered 
	transition */
    public void detriggeredCallback(StaLtaEventDetector detector) {

	String msg = 
	    "TurbidityEventDetector de-triggered: " + detector.toString();

	dispatchMessage(msg);
    }


    /**
       Transform and add latest sample to STA/LTA event detector.
     */
    public void processSample(float voltage,
			      long sampleTime,
			      float transmittSlope, 
			      float transmittIntcpt,
			      int sampleIntervalSec)
	throws Exception {

	_log4j.debug("Processing sample voltage = " + voltage + 
		     " sampleTime = " + sampleTime);

	if (sampleIntervalSec != _sampleIntervalSec) {
	    String msg = "Sample interval changed from " + 
		_sampleIntervalSec + " sec to " + sampleIntervalSec + 
		"sec; resetting event detector";
	    _log4j.info(msg);
	    _sampleIntervalSec = sampleIntervalSec;
	    dispatchMessage(msg);
	    _eventDetector.reset();
	}

	float pctAttenuation = 
	    100.f - (voltage * transmittSlope + transmittIntcpt);

	// Add to event detector
	try {
	    _eventDetector.addSample(new Float(pctAttenuation), sampleTime);
	}
	catch (Exception e) {
	    _log4j.error("Caught exception from eventDetector.addSample(): " + e);
	}	 

	_secSinceLastStatus += _sampleIntervalSec;
	if (_secSinceLastStatus >= _statusIntervalSec) {
	    String msg = "TurbidityEventDetector status: " + 
		_eventDetector.toString();

	    dispatchMessage(msg);
	    _secSinceLastStatus = 0;
	}
    }


    /** Set interval at which detector status messages are generated */
    public void setStatusInterval(int statusIntervalSec) {
	_statusIntervalSec = statusIntervalSec;
	_secSinceLastStatus = 0;
    }


    /** Enable use of shore messaging service */
    public void enableShoreMessaging() {
	_log4j.debug("enableShoreMessaging()");
	_shoreMsgEnabled = true;
    }

    /** Disable use of shore messaging service */
    public void disableShoreMessaging() {
	_log4j.debug("disableShoreMessaging()");
	_shoreMsgEnabled = false;
    }


    /** Run thread to dispatch message to ShoreMessage service */
    class Dispatcher implements Runnable {

	byte[] _message;
	InetAddress _modemHost;
	NetworkManager _network;

	Dispatcher(NetworkManager network, 
		   InetAddress modemHost) {
	    _log4j.debug("Dispatcher constructor");
	    _message = null;
	    _modemHost = modemHost;
	    _network = network;
	}

	/** Set message contents */
	public void setMessage(byte[] message) {
	    _message = message;
	}


	public void run() {
	    _log4j.debug("Dispatcher.run()");

	    if (_modemHost == null) {
		_log4j.error("Dispatch.run() - NULL modemHost");
		return;
	    }

	    try {
		// Wakeup store-and-forward service node
		_network.wakeupNode(_modemHost);
		_network.keepNodeAwake(_modemHost, _shoreMsgSrvcTimeoutMsec);
	    }
	    catch (Exception e) {
		_log4j.error("Dispatcher.run() - wakeup node failed: " 
			     + e);
	    }
	    
		

	    try {
		// Get new reference to shore messaging service each time,
		// in case service host has restarted
		ShoreMessaging shoreMessaging = 
		    ShoreMessagingHelper.getProxy(_modemHostName);

		_log4j.debug("queue downlink msg: " + new String(_message));

		byte[] fullMsg = 
		    ShoreMessagingHelper.prependHeader(_message, 
						       _seabirdService.getId(),
						       System.currentTimeMillis());
		shoreMessaging.queueDownlinkMessage(fullMsg);
	    }
	    catch (Exception e) {
		_log4j.error("Dispatcher.run() - error queuing msg to " + 
			     "shore messaging service at " + 
			     _modemHostName + ": " + 
			     e);

	    }
	}
    }

    /** Parameters used for STA/LTA event detection */
    public static class Parameters {
	int _staWidthSec;
	int _ltaWidthSec;
	float _attenuationTriggerRatio;
	float _attenuationDetriggerRatio;
	int _maxTriggeredSec;

	public Parameters(int staWidthSec,
			  int ltaWidthSec,
			  float attenuationTriggerRatio,
			  float attenuationDetriggerRatio,
			  int maxTriggeredSec) {

	    _staWidthSec = staWidthSec;
	    _ltaWidthSec = ltaWidthSec;
	    _attenuationTriggerRatio = attenuationTriggerRatio;
	    _attenuationDetriggerRatio = attenuationDetriggerRatio;
	    _maxTriggeredSec = maxTriggeredSec;
	}
    }
}



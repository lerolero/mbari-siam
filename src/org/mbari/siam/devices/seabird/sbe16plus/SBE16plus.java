/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.sbe16plus;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.devices.seabird.base.Seabird;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.devices.seabird.eventDetector.TurbidityEventDetector;

import java.rmi.RemoteException;
import java.util.StringTokenizer;

public class SBE16plus extends Seabird
    implements Instrument, Safeable {

    static private Logger _log4j = Logger.getLogger(SBE16plus.class);

    /**
     * This is configured to return comma-separted engineering units. This can be parsed by
     * the DevicePacketParser.
     */
    public static final byte[] CMD_OUTPUTFORMAT = "OUTPUTFORMAT=3\r".getBytes();
    public static final byte[] CMD_CALIBRATION = "DCAL\r".getBytes();
    

    protected TurbidityEventDetector _eventDetector;

    /**
     * Constructor; can throw RemoteException.
     */
    public SBE16plus() throws RemoteException {
        super();

	_attributes = new Attributes(this);
    }

    protected byte[] getFormatForSummaryCmd() {
        return CMD_OUTPUTFORMAT;
    }

    /**
     * Implementation of Seabird 16plus Safe Mode operation.
     */
    public synchronized void enterSafeMode() throws Exception {

	stopAutonomousLogging();

        // Get to a prompt.
        getPrompt();

        // Set the internal clock before going into autonomous mode.
        _log4j.info("enterSafeMode() - Setting SBE-16plus clock to RTC.");
        setClock(System.currentTimeMillis());

        // Get to a prompt again.
        getPrompt();

        // Set sample interval (seconds). Default is 300s.
        _log4j.info("enterSafeMode() - Setting SBE-16plus safe sample interval.");
	setAutonomousSampleInterval(_attributes.safeSampleIntervalSec);

        // Start autonomous sampling immediately.
        _log4j.info("enterSafeMode() - Instructing SBE-16plus to start autonomous sampling NOW.");
	_safeMode=true;
        startAutonomousLogging();

        _toDevice.flush();

    }

    protected byte[] getCalibrationCmd() {
        return CMD_CALIBRATION;
   }



   protected void initializeInstrument() throws Exception {
	super.initializeInstrument();

	_fromDevice.flush();

	sendCommand("PumpMode=" + ((Attributes )_attributes).pumpMode + "\r");

	sendCommand("DelayBeforeSampling=" + 
		    ((Attributes )_attributes).delayBeforeSamplingSec + "\r");

	if (((Attributes )_attributes).detectEvents)
	{
	    // Get current sampling schedule - sample interval needed by event
	    // detector
	    ScheduleSpecifier schedule = 
	    getDefaultSampleSchedule().getScheduleSpecifier();

	    if (!schedule.isRelative()) {
		throw new Exception("event detector requires relative schedule");
	    }

	    int sampleIntervalSec = (int )schedule.getPeriod()/1000;

	    try {
		Attributes attr = (Attributes )_attributes;
		TurbidityEventDetector.Parameters eventParams = 
		    new TurbidityEventDetector.Parameters(attr.staWidthSec,
							  attr.ltaWidthSec,
							  attr.attenuationTriggerRatio,
							  attr.attenuationDetriggerRatio,
							  attr.maxTriggerSec);

		_eventDetector = 
		    new TurbidityEventDetector(this, eventParams,
					       attr.eventDetectorStatusIntervalSec);

		_eventDetector.initialize(attr.modemHost, sampleIntervalSec);
		_eventDetector.dispatchMessage("Created TurbidityEventDetector");
	    } catch (Exception e) {
		_log4j.error("Exception creating TurbidityEventDetector: "+e);
		annotate(("ERROR - event detector constructor failed: " + e.toString()).getBytes());
	    }

	    // Ensure that output format shows comma-separated engineering units
	    // (since this service will parse the data)
	    sendCommand("outputformat=3\r");
	    
	    // Ensure that output includes analog sensor voltages
	    sendCommandConfirm("volt0=y\r");
	    sendCommandConfirm("volt1=y\r");
	    sendCommandConfirm("volt2=y\r");
	    sendCommandConfirm("volt3=y\r");

	}
	else {
	    _log4j.info("Event detection disabled by attribute");
	    annotate("Event detection disabled by attribute".getBytes());
	}
    }

    protected float getTransmisChannel(String sample)
	throws NumberFormatException
    {
	    StringTokenizer tokenizer =  new StringTokenizer(sample, ", ");
	    String token = null;
				
	    for (int nToken = 0; tokenizer.hasMoreTokens(); nToken++)
	    {
		try {
		    token = tokenizer.nextToken();

		    if (nToken == ((Attributes)_attributes).transmisChannel+3)
			return(Float.parseFloat(token));
		}
		catch (NumberFormatException e) {
		    String errMsg = "Invalid voltage: " + token + 
			" : token #" + nToken + " of record: " + sample;

		    _log4j.error(errMsg);

		    throw new NumberFormatException(errMsg);
		}
	    }
	    return((float)0.0);
    }


    protected SensorDataPacket processSample(byte[] sample, int nBytes) 
	throws Exception {

	SensorDataPacket packet = super.processSample(sample, nBytes);

	if (((Attributes )_attributes).detectEvents == false) {
	    return packet;
	}

	Attributes attributes = (Attributes )_attributes;

	if (_eventDetector.enabled())
	{

	    // Get current sampling schedule, check to if it has changed.
	    ScheduleSpecifier schedule = 
		getDefaultSampleSchedule().getScheduleSpecifier();

	    if (!schedule.isRelative()) {
		throw new Exception("event detector requires relative schedule");
	    }

	    int sampleIntervalSec = (int )schedule.getPeriod()/1000;


	    _eventDetector.processSample(getTransmisChannel(new String(packet.dataBuffer())),
					 packet.systemTime(),
					 attributes.transmittSlope,
					 attributes.transmittIntcpt,
					 sampleIntervalSec);
	}
	else {
	    _log4j.warn("event detection is disabled");
	}

	return packet;
    }


    protected class Attributes 
	extends Seabird.Attributes {

	public Attributes(DeviceServiceIF service) {
	    super(service);
	}

	/** Seconds for instrument to delay before acquiring polled sample */
	public int delayBeforeSamplingSec = 0;

	/** This property is useful only for pumped instruments. 
	    (e.g. SBE-16+-V2). Values: 0 - no pumping, 1 - run pump 0.5 sec 
	    before each sample, 2 - pump during sampling */
	public int pumpMode = 0;

	/** Enable/disable event detection */
	public boolean detectEvents = true;

	/** Transmissometer channel, ZERO-based */
	public int transmisChannel = 0;

	/** Linear calibration %transmittance slope */
	public float transmittSlope = 20.f;

	/** Linear calibration %transmittance intercept */
	public float transmittIntcpt = 0.f;

	/** Width (in seconds) of short-term average (STA) */
	public int staWidthSec = 21600;

	/** Width (in seconds) of long-term average (LTA) */
	public int ltaWidthSec = 144000;

	/** Attenuation STA/LTA event trigger ratio */
	public float attenuationTriggerRatio = 1.5f;

	/** Attenuation STA/LTA event detrigger ratio */
	public float attenuationDetriggerRatio = 1.05f;

	/** Maximum consecutive seconds to acquire in triggered state
	 before resetting event detector */
	public int maxTriggerSec = 1800000;

	/** Shore messaging service host name */
	public String modemHost = "surface";

	/** Periodically log status of event detector */
	public int eventDetectorStatusIntervalSec = 3600;

    }


} // end of class

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.sbe37;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.devices.seabird.base.Seabird;

import java.rmi.RemoteException;

public class SBE37 extends Seabird
        implements Instrument, Safeable {

    static private Logger _log4j = Logger.getLogger(SBE37.class);

    /**
     * This is configured to return comma-separted engineering units. This can be parsed by
     * the DevicePacketParser.
     */
    public static final byte[] CMD_OUTPUTFORMAT = "FORMAT=1\r".getBytes();
    public static final byte[] CMD_CALIBRATION = "DC\r".getBytes();

    /**
     * Constructor; can throw RemoteException.
     */
    public SBE37() throws RemoteException {
        super();
    }

    protected byte[] getFormatForSummaryCmd() {
        return CMD_OUTPUTFORMAT;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Implementation of Seabird SBE37SM Safe Mode operation.
     */
    public synchronized void enterSafeMode() throws Exception {

	stopAutonomousLogging();

        // Get to a prompt.
        getPrompt();

        // Set the internal clock before going into autonomous mode.
        _log4j.info("enterSafeMode() - Setting SBE37 clock to RTC.");
        setClock(System.currentTimeMillis());

        // Get to a prompt again.
        getPrompt();

        // Set sample interval (seconds). Default is 300s.
        _log4j.info("enterSafeMode() - Setting SBE-37SM safe sample interval.");
	setAutonomousSampleInterval(_attributes.safeSampleIntervalSec);

        // Start autonomous sampling immediately.
        _log4j.info("enterSafeMode() - Instructing SBE to start autonomous sampling NOW.");
	_safeMode=true;
	startAutonomousLogging();

        _toDevice.flush();

    } // end of method


    protected byte[] getCalibrationCmd() {
        return CMD_CALIBRATION;
    }

} // end of class

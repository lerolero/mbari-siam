/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import net.java.jddac.common.fblock.Entity;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.exception.OpException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.MeasurementPacket;
import org.mbari.siam.distributed.jddac.SiamRecord;

/** 
This FunctionBlock logs SiamRecords into MeasurementPackets.
*/
public class MeasurementLogger extends FunctionBlock {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(MeasurementLogger.class);

    private BaseInstrumentService _instrument = null;

    public MeasurementLogger(BaseInstrumentService instrumentService) {
	super();
	_instrument = instrumentService;
    }


    /** This is the F-block's public "client-server" interface method. */
    synchronized public ArgArray perform(String operationID, 
				  ArgArray input) 
	throws OpException, Exception {

	if (operationID.equals(Entity.PerformInputArg)) {
	    if (input instanceof SiamRecord) {
		logSample((SiamRecord )input);
		return null;
	    }
	    else {
		throw new OpException("Input is not a SiamRecord");
	    }
	}
	else {
	    return super.perform(operationID, input);
	}
    }
    
    /** Log the input SiamRecord to the device log as a MeasurementPacket. */
    synchronized protected void logSample(SiamRecord record) {

	_log4j.debug("MeasurementLogger.logSample()");
	MeasurementPacket packet = 
	    new MeasurementPacket(_instrument.getId(), record);

    packet.setSystemTime(System.currentTimeMillis());

    _instrument.logPacket(packet);
    }
}


/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.pumpedSeabird;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.rmi.RemoteException;
import java.rmi.Naming;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.AttributeChecker;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.devices.seabird.base.Seabird;
import org.mbari.siam.devices.pump.PumpAttributes;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class PumpedSeabird
    extends Seabird implements Instrument{

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PumpedSeabird.class);

    public static final byte[] CMD_OUTPUTFORMAT = "OUTPUTFORMAT=3\r".getBytes();

    /** Pump Service */
    public Device _pumpService=null;

    // Configurable pumped Seabird attributes
    PumpAttributes _attributes = new PumpAttributes(this);

    public PumpedSeabird() 
	throws RemoteException {
	super();
    }

    protected void requestSample(){

	if(getPump()==null){
	    _log4j.info("No Pump available...forging ahead");
	}
	else
	try{
	    _log4j.debug("PumpedSeabird  requestSample()");
	    _log4j.debug("PumpedSeabird  requestSample(): turning pump ON");
	    _pumpService.powerOn();
	    _log4j.debug("PumpedSeabird  requestSample(): waiting 10 sec...");
	    Thread.sleep(10000);
	    _log4j.debug("PumpedSeabird  requestSample(): turning pump OFF");
	    _pumpService.powerOff();
	}catch(InterruptedException e){
	}
	catch(RemoteException r){
	    _log4j.error("PumpedSeabird  requestSample: remote exception"+r);
	}

	try{
	    // Verify connection...
	    _log4j.debug("requestSample(): looking for prompt...");
	    super.getPrompt();
        
	    // Get status/config info...
	    _log4j.debug("requestSample(): sending sample requesting...");
	    _toDevice.write(_requestSample );
	}catch(Exception e){
	    _log4j.error("PumpedSeabird  requestSample() caught exception "+e);
	}
	return;
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

	// Sample every 30 seconds by default
	return new ScheduleSpecifier(30000);
    }


    /** Get Pump Service */
    protected Device getPump(){

	if(_pumpService != null ){
	    _log4j.debug("Already have pump service");
	    return _pumpService;
	}

	_log4j.debug("getPump(): lookup pump service (" +
			     _attributes.rmiPumpServiceName+")");

	if(_attributes.rmiPumpServiceName==null){
	    _log4j.error("getPump(): No Registry Name; setting _pumpService=null");
	    _pumpService=null;
	}else
	try{
	    _pumpService = 
		(Device)Naming.lookup("rmi://localhost/"+
				      _attributes.rmiPumpServiceName);

	}catch(Exception e){
	    _log4j.error("getPump() caught exception: "+e);
	    _pumpService=null;
	}
	return _pumpService;
    }

   protected byte[] getFormatForSummaryCmd() {
        return CMD_OUTPUTFORMAT;
    }

    protected byte[] getCalibrationCmd() {
	return "DCAL\r".getBytes();
    }

}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.foce.devices.controlLoop.*;
import org.mbari.siam.distributed.devices.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/* Control Loop Worker thread
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public class ControlLoopWorker extends WorkerThread  {
	
    static protected Logger _log4j = Logger.getLogger(ControlLoopWorker.class);  
	
	protected ControlResponseIF _controlResponse;
	protected ControlProcessIF _controlProcess;
	
	public ControlLoopWorker(ControlProcessIF controlProcess, ControlResponseIF controlResponse, long updatePeriodMillisec){
		this(controlProcess,controlResponse,updatePeriodMillisec,"foce control loop");
	}
	public ControlLoopWorker(ControlProcessIF controlProcess, ControlResponseIF controlResponse, long updatePeriodMillisec, String name){
		super(name,updatePeriodMillisec);
		_controlResponse=controlResponse;
		_controlProcess=controlProcess;
	}

	public void doWorkerAction(){
		try{
			_controlResponse.update();
		}catch (Exception e) {
			_log4j.error("["+this.name()+"] - "+e);
		}				
		return;
	}
	
	public void setResponse(ControlResponseIF response){
		_controlResponse=response;
	}
	
}
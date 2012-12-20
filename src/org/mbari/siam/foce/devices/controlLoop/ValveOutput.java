/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.devices.ValveIF;
import org.mbari.siam.distributed.devices.Valve2WayIF;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.distributed.devices.ControlOutputIF;

/** ControlOutput implementation for FOCE ESW valve, a 2-way (open/close) valve
	connecting to the ESW delivery sub-system to either end of the FOCE apparatus.
 */
public class ValveOutput extends ControlOutputImpl{
	
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(ValveOutput.class);

	Valve2WayIF _valve;
	
	public ValveOutput(Valve2WayIF valve){
		this(valve,ControlOutputIF.DEFAULT_ID,ControlOutputIF.DEFAULT_NAME);
	}
	
	public ValveOutput(Valve2WayIF valve, int id){
		this(valve,id,ControlOutputIF.DEFAULT_NAME);
	}
	
	public ValveOutput(Valve2WayIF valve, int id, String name){
		_valve=valve;
		_id=id;
		_name=name;
		_statusBuffer=new StringBuffer();
		if(_valve!=null){
			setState(STATE_READY);
		}else{
			setState(STATE_UNINITIALIZED);
		}
	}
	
	////////////////////////////////////////
	// Control Output implementation classes
	////////////////////////////////////////

	public void setOutputValue(int value) throws Exception{
		if(_valve==null){
			// this is more descriptive than letting a NullPointerException happen
			setState(STATE_UNINITIALIZED);
			throw new Exception("valve is null");
		}
		setState(STATE_UPDATING);
		clearStatus(STATUS_BAD_COMMAND);
		switch (value) {
			case Valve2WayIF.OPEN_FUNC:
				try{
					_valve.open();
				}catch(Exception e){
					setState(STATE_READY);
					clearStatus(STATUS_UPDATE_ERROR);
					throw e;
				}
				break;
			case Valve2WayIF.CLOSE_FUNC:
				try{
					_valve.close();
				}catch(Exception e){
					setState(STATE_READY);
					clearStatus(STATUS_UPDATE_ERROR);
					throw e;
				}
				break;
			default:
				setState(STATE_READY);
				setStatus(STATUS_BAD_COMMAND);
				throw new Exception("Invalid valve state ["+value+"]");
		}
		setState(STATE_READY);
		clearStatus(STATUS_UPDATE_ERROR);
	}
	public void setOutputValue(boolean value) throws Exception{
		throw new Exception("setOutputValue(boolean): Operation not supported");
	}
	public void setOutputValue(double value) throws Exception{
		throw new Exception("setOutputValue(double): Operation not supported");
	}
	public Object getDevice(){
		return _valve;
	}	
}
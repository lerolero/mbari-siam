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

import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.distributed.devices.ControlOutputIF;

/** ControlOutput implementation for FOCE ESW pump, an Elmo motor controller
 connected to the ESW delivery pump on the FOCE apparatus.
 */
public class MotorVelocityOutput extends ControlOutputImpl{
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(MotorVelocityOutput.class);
	
	ElmoIF _motor;

	public MotorVelocityOutput(ElmoIF motor){
		this(motor,ControlOutputIF.DEFAULT_ID,ControlOutputIF.DEFAULT_NAME);
	}
	
	public MotorVelocityOutput(ElmoIF motor,int id){
		this(motor,id,ControlOutputIF.DEFAULT_NAME);
	}
	
	public MotorVelocityOutput(ElmoIF motor,int id, String name){
		_motor=motor;
		_id=id;
		_name=name;
		_statusBuffer=new StringBuffer();
		if(_motor!=null){
			setState(STATE_READY);
		}else{
			setState(STATE_UNINITIALIZED);
		}
	}
	
	////////////////////////////////////////
	// Control Output implementation classes
	////////////////////////////////////////
	
	public void setOutputValue(double value) throws Exception{
		if(_motor==null){
			setState(STATE_UNINITIALIZED);
			throw new Exception("motor is null");
		}
		setState(STATE_UPDATING);
		try{
			/*if(_log4j.isDebugEnabled()){
				_log4j.debug("setOutputValue - updating using orpm value ["+value+"]");
			}*/
			int speedCounts=_motor.orpm2counts(value);
			/*if(_log4j.isDebugEnabled()){
				_log4j.debug("setOutputValue - counts value ["+speedCounts+"]; calling motor.jog");
			}*/
			_motor.jog(speedCounts);
		}catch (Exception e) {
			setState(STATE_READY);
			setStatus(STATUS_UPDATE_ERROR);
			throw e;
		}
		/*if(_log4j.isDebugEnabled()){
			_log4j.debug("setOutputValue - cleaning up state");
		}*/
		setState(STATE_READY);
		clearStatus(STATUS_UPDATE_ERROR);
	}
	public void setOutputValue(int value) throws Exception{
		throw new Exception("setOutputValue(int): Operation not supported");
	}

	public void setOutputValue(boolean value) throws Exception{
		throw new Exception("setOutputValue(boolean): Operation not supported");
	}
	
	public Object getDevice(){
		return _motor;
	}		
	
}
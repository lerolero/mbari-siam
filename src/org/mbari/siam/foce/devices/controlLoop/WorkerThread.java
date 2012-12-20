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

/*
import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;
*/
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/*   Worker Thread
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public abstract class WorkerThread extends Thread  {
	
    static protected Logger _log4j = Logger.getLogger(WorkerThread.class);  
	static String DEFAULT_NAME="worker_thread";
	static long DEFAULT_PERIOD=-1L;
	protected boolean _terminate=false;
	protected boolean _pause=false;
	protected boolean _running=false;
	protected boolean _initialized=false;
	protected long _updatePeriodMillisec=-1L;
	protected String _name=DEFAULT_NAME;
	
	public WorkerThread(){
		super();
	}
	public WorkerThread(String name, long updatePeriodMillisec){
		this();
		setWorkerName(name);
		setUpdatePeriod(updatePeriodMillisec);
	}
	public WorkerThread(long updatePeriodMillisec){
		this(DEFAULT_NAME,updatePeriodMillisec);
	}
	public WorkerThread(String name){
		this(name,DEFAULT_PERIOD);
	}
	
	abstract public void doWorkerAction();

	public void run(){
		if(_log4j.isDebugEnabled()){
		_log4j.debug("WorkerThread ["+_name+"] starting period="+_updatePeriodMillisec);
		}
		_running=true;
		try{
			initialize();
		}catch (Exception e) {
			_log4j.error("["+_name+"] Initialization error:"+e);
			return;
		}
		_initialized=true;
		while(this._terminate==false){
			long start=System.currentTimeMillis();
			
			if(_pause){
				if(_log4j.isDebugEnabled()){
				_log4j.debug("WorkerThread ["+_name+"] paused");
				}
			}else{
				if(_log4j.isDebugEnabled()){
				_log4j.debug("WorkerThread ["+_name+"] updating");
				}
				try{
					doWorkerAction();
				}catch (Exception e) {
					_log4j.error("WorkerThread ["+_name+"] error:");
					e.printStackTrace();
				}
			}
			
			if(_updatePeriodMillisec<=0L){
				if(_log4j.isDebugEnabled()){
				_log4j.debug("period<=0; exiting");
				}
				break;
			}
			
			long elapsedTime=System.currentTimeMillis()-start;
			if(elapsedTime < _updatePeriodMillisec){
				long remainingTime=_updatePeriodMillisec-elapsedTime;
				WorkerThread.delay(remainingTime);
			}
		}
		_running=false;
		shutdown();
		_initialized=false;
		if(_log4j.isDebugEnabled()){
		_log4j.debug("WorkerThread ["+_name+"] terminating");
		}
		return;
	}
	
	public void setWorkerName(String name){
		_name=name;
		super.setName(_name);
	}
	
	public void initialize() throws Exception{	
	}
	public void shutdown(){	
	}
	
	public void setUpdatePeriod(long periodMillisec){
		//_log4j.debug("worker thread ["+_name+"] setting update period to ["+_updatePeriodMillisec+"]");
		_updatePeriodMillisec=periodMillisec;
		return;
	}
	
	public void terminate(){
		_terminate=true;
		return;
	}
	public void pause(boolean pauseValue){
		_pause=pauseValue;
		return;
	}
	public boolean isRunning(){
		return _running;
	}
	public boolean isInitialized(){
		return _initialized;
	}
	
	public static void delay(long delayMillisec){
		try{
			Thread.sleep(delayMillisec);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}
	public String name(){
		return _name;
	}
}
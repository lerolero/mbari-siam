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

/** check the status of the inputs and outputs and 
 handle errors
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */
public class ProcessMonitor extends WorkerThread{
	
	protected Logger _log4j = Logger.getLogger(ProcessMonitor.class);
	
	protected ControlProcessIF _controlProcess;
	Vector _iostates=null;
	ControlInputIF[] _inputs;
	ControlOutputIF[] _outputs;
	
	String[] _sampleBuffers=null;
	
	public ProcessMonitor(ControlProcessIF controlProcess, String name, long updatePeriodMillisec){
		super(name,updatePeriodMillisec);
		_iostates=new Vector();
		_controlProcess=controlProcess;
	}
	
	/** Return a set of sample buffers for logging */
	public String[] getSampleBuffers(){
		synchronized (this) {
			return _sampleBuffers;
		}
	}
	
	/** Set the sample buffers*/
	protected void setSampleBuffers(ControlInputIF[] inputs, ControlOutputIF[] outputs){
		synchronized (this) {
			// get input state strings
			for(int i=0;i<inputs.length;i++){
				try{
					String state=inputs[i].getInputState().toString();
					if(state!=null){
						_iostates.add(state);
					}else{
						_log4j.warn("Null InputState in setSampleBuffers ["+i+"/"+inputs.length+"]");
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// get output state strings
			for(int i=0;i<outputs.length;i++){
				try{
					String state=outputs[i].getOutputState().toString();
					if(state!=null){
						_iostates.add(state);
					}else{
						_log4j.warn("Null OutputState in setSampleBuffers ["+i+"/"+outputs.length+"]");
					}
					
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			if( (_iostates==null) || (_iostates.size()==0)){
				_log4j.error("_iostates is null or zero length in setSampleBuffers");
				return;
			}
			// only resize array if it is larger than
			// the  vector, since toArray will
			// reallocate if needed otherwise
			if( (_sampleBuffers==null) || 
			   (_sampleBuffers.length>_iostates.size())){
				
				_sampleBuffers=new String[_iostates.size()];
				
			}
			_sampleBuffers=(String[])_iostates.toArray(_sampleBuffers);
		}
	}
	
	/** evaluate input/output health and status.
	 take appropriate action when faults are 
	 detected.
	 */
	public void evaluateIO(ControlInputIF[] inputs,ControlOutputIF[] outputs){
		synchronized (this) {
			if( inputs==null || inputs.length<=0){
				// what, NO INPUTS?!
			}
			
			if( outputs==null || outputs.length<=0){
				// what, NO OUTPUTS?!
			}
			
			// check inputs
			for(int i=0;i<inputs.length;i++){
				try{
					InputState istate=inputs[i].getInputState();
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// check outputs
			for(int i=0;i<outputs.length;i++){
				try{
					OutputState ostate=outputs[i].getOutputState();
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void debugShow(){
		if(_log4j.isDebugEnabled()){

		// $ISTATE,iname,stateStr,statStr,utmo,tslu,toex,vcount,vratio
		_log4j.debug("************* "+_name+" found ["+_inputs.length+"] _inputs ");
		
		// $OSTATE,oname,stateStr,statStr
		_log4j.debug("************* "+_name+" found ["+_outputs.length+"] _outputs ");
		
		for(Iterator i=_iostates.iterator();i.hasNext();){
			String iostate=(String)i.next();
			_log4j.debug("************* "+iostate);
		}
		}
	}
	
	public void doWorkerAction(){
		synchronized (this) {
			
			// get inputs
			try{	
				_inputs=_controlProcess.getInputs();
			}catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
			// get outputs
			try{	
				_outputs=_controlProcess.getOutputs();
			}catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
			// empty the vector of data (io state) records
			_iostates.clear();
			setSampleBuffers(_inputs,_outputs);
			
			debugShow();
			
			// The main event: check it and
			// fix it or shut it down
			evaluateIO(_inputs,_outputs);
			
		}
	}
}

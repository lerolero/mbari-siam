/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.utils;

import java.util.*;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ElmoLouverIF;
import org.mbari.siam.distributed.devices.ElmoThrusterIF;
import org.mbari.siam.foce.devices.elmo.base.Elmo;

/** ElmoCI (Elmo Command Interface)
	Maps user text commands into methods calls on classes implemententing 
	ElmoIF and its subclasses (interfaces), like ElmoLouverIF and ElmoThrusterIF.
	
	Provides common interface to ElmoRMI and ElmoLocal, which combine ElmoCI with 
	various implementations of ElmoIF to create utilities that may be used with or
	without SIAM.
 */

public class ElmoCI{
	static private Logger _log4j = Logger.getLogger(ElmoCI.class);

	private ElmoIF _elmoIF=null;
	private ElmoLouverIF _louverIF=null;
	private ElmoThrusterIF _thrusterIF=null;
	private Vector _cmdQueue=new Vector();

	public ElmoCI(){
	}
	
	public ElmoCI(ElmoIF elmoIF){
		setElmo(elmoIF);
	}

	public void setElmo(ElmoIF elmoIF){
		_elmoIF=elmoIF;
		if(_elmoIF instanceof ElmoLouverIF){
			_louverIF=(ElmoLouverIF)elmoIF;
		}
		if(_elmoIF instanceof ElmoThrusterIF){
			_thrusterIF=(ElmoThrusterIF)elmoIF;
		}
	}
	
	/** Print application-specific usage message to stdout. 
	 */
	public  void printUsage(){
		StringBuffer sb=new StringBuffer();
		String _programName=System.getProperty("exec_name","<program>");
		sb.append("\n");
		sb.append( " #\n");
		sb.append( " # "+_programName+": configure and run motor\n");
		sb.append( " #\n");
		sb.append("\n");
		sb.append( " usage: "+_programName+" [nodeURL port|-p <port>] \"[option[;option...]]\"\n");
		sb.append("\n");
		sb.append( " Options:\n");
		sb.append( " #### program options ###                           \n"); 
		sb.append( " -v,--verbose   : verbose output                    \n");
		sb.append( " -h,--help      : print this help message           \n");
		sb.append("\n");
		sb.append( " #### controller config options ###                 \n"); 
		sb.append( " ci             : init controller                   \n"); 
		sb.append( " ch=<0:1>       : invert louver hall feedback       \n"); 
		sb.append( " cr=<reg>       : read register value               \n"); 
		sb.append( " cs             : show configuration                \n"); 
		sb.append( " csm=<l|r>      : set serial mode local/remote      \n"); 
		sb.append("\n");
		sb.append( " #### motor options ###                             \n"); 
		sb.append( " md             : disable motor                     \n");
		sb.append( " me             : enable motor                      \n");
		sb.append( " mg=<ratio>     : set gear ratio                    \n");
		sb.append( " mvp=<counts>   : set motor PTP speed (counts)      \n");
		sb.append( " mvc=<c/s>      : set motor speed (counts)          \n");
		sb.append( " mvr=<rpm>      : set motor speed (rpm)             \n");
		sb.append( " mvo=<rpm>      : set output speed (rpm)            \n");
		sb.append( " mp=<counts>    : set position counter              \n");
		sb.append( " mb             : begin motion                      \n");
		sb.append( " mr=<counts>    : move relative  (counts)           \n");
		sb.append( " ma=<counts>    : move absolute  (counts)           \n");
		sb.append( " mw=<msec>      : wait (msec)                       \n");
		sb.append( " ms             : get motor status (register)       \n");
		sb.append( " mf             : get motor fault (register)        \n");
		sb.append("\n");
		if(_louverIF!=null || _elmoIF==null){
			sb.append( " #### louver options ###                            \n"); 
			sb.append( " lc=<c/s>       : center louver on current position \n");
			sb.append( " lb=<0:1>,<c/s> : find boundary (0:below 1: above)  \n");
			sb.append( " lp=<%>         : set louver position (%)           \n");
			sb.append( " ld=<deg>       : set louver position (deg)         \n");
			sb.append( " lh=<n>,<0:1>,<px>,<vlo>,<vhi>: home on feedback position n   \n");
			sb.append( " ls             : get louver status                 \n");
			sb.append("\n");
		}
		if(_thrusterIF!=null || _elmoIF==null){
			sb.append( " #### thruster options ###                          \n"); 
			sb.append( " tjc=<counts>   : jog  (counts)                     \n");
			sb.append( " tjr=<rpm>      : jog  (rpm)                        \n");
			sb.append( " tjo=<rpm>      : jog  (output rpm)                 \n");
			sb.append( " te=<0:1>       : enable/disable turn sensor        \n");
			sb.append( " tss            : get turn sensor state             \n");
			sb.append( " tc             : get turn sensor count             \n");
			sb.append( " tt             : get turn sensor elapsed time      \n");
			sb.append( " ts             : get thruster status               \n");
			sb.append("\n");		
		}		
		System.out.println(sb.toString());
	}
	
	/** Perform the actions indicated on the command line.
	 @return 0 on success
	 */
	public int run(String options[]) 
	{
		try{			
			for(int i=0;i<options.length;i++){
				// tokenize a sequence of delimited commands
				StringTokenizer seqST=new StringTokenizer(options[i],";\n");
				while(seqST.hasMoreTokens()){
					// tokenize each command 
					// separating command from arguments (delimited by "="
					String cmdString=seqST.nextToken().trim();
					StringTokenizer cmdST=new StringTokenizer(cmdString,"=");
					String cmd=cmdST.nextToken();
					String args=null;
					if(cmdST.hasMoreTokens()){
						args=cmdST.nextToken();
					}
					// validate commands and enqueue them
					parseExecCmd(cmd,args);
					_log4j.debug("\n");
				}
			}
			// now that all are validated, play them back
			for(int i=0;i<_cmdQueue.size();i++){
				((CmdCallback)_cmdQueue.get(i)).callbackAction();
			}
			return 0;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	protected int getIntArg(StringTokenizer stok)
	throws Exception
	{
		return Integer.parseInt(stok.nextToken().trim());
	}
	
	protected long getLongArg(StringTokenizer stok)
	throws Exception
	{
		return Long.parseLong(stok.nextToken().trim());
	}

	protected double getDoubleArg(StringTokenizer stok)
	throws Exception
	{
		return Double.parseDouble(stok.nextToken().trim());
	}
	protected String getStringArg(StringTokenizer stok)
	throws Exception
	{
		return stok.nextToken().trim();
	}
	
	public interface CmdCallback{
		public void callbackAction() throws Exception;
	}	
	
	protected void parseExecCmd(String cmd,String args)
	throws Exception
	{

		_log4j.debug("cmd:"+cmd+" args:"+args);
		StringTokenizer stok=null;
		
		if(args!=null){
			stok=new StringTokenizer(args,",");
		}

		if(cmd.equals("-h") ||cmd.equals("-help") || cmd.equals("--help")){
			try{
				printUsage();
				return;
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("ch")){
			try{
				final boolean invert=(getIntArg(stok)==0?false:true);
				//_louverIF.setInvertHallPosition(invert);
				_cmdQueue.add(new CmdCallback(){
							 public void callbackAction() throws Exception{
							 _louverIF.setInvertHallPosition(invert);
							 }
							 });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("ci")){
			try{
				//_elmoIF.initializeController();
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.initializeController();
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("cs")){
			try{
				//System.out.println(_elmoIF.showConfiguration());
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println(_elmoIF.showConfiguration());
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("cr")){
			try{
				final String register=getStringArg(stok);
				//System.out.println(_elmoIF.showConfiguration());
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println(_elmoIF.readRegister(register));
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("csm")){
			try{
				final String serialMode=getStringArg(stok);
				
				if(serialMode.equals("r")){
					//_elmoIF.setSerialMode(Elmo.MODE_SERIAL_RFC1722);
					_cmdQueue.add(new CmdCallback(){
								  public void callbackAction() throws Exception{
								  _elmoIF.setSerialMode(Elmo.MODE_SERIAL_RFC1722);
								  }
								  });
				}else if(serialMode.equals("l")){
					//_elmoIF.setSerialMode(Elmo.MODE_SERIAL_LOCAL);
					_cmdQueue.add(new CmdCallback(){
								  public void callbackAction() throws Exception{
								  _elmoIF.setSerialMode(Elmo.MODE_SERIAL_LOCAL);
								  }
								  });
				}else{
					throw new Exception("Invalid serial mode: ["+stok+"]");
				}
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("lb")){
			try{
				final boolean direction=(getIntArg(stok)==0?false:true);
				final int speedCounts=getIntArg(stok);
				//_louverIF.findBoundary(direction, speedCounts);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _louverIF.findBoundary(direction, speedCounts);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("lc")){
			try{
				final int speedCounts=getIntArg(stok);
				//_louverIF.center(speedCounts);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _louverIF.center(speedCounts);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("ld")){
			try{
				final double positionDegrees=getDoubleArg(stok);
				//_louverIF.setLouverUnits(positionDegrees);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _louverIF.setLouverDegrees(positionDegrees);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("lh")){
			try{
				final int hallPosition=getIntArg(stok);
				final boolean setPx=(getIntArg(stok)==0?false:true);
				final long pxValue=getLongArg(stok);
				final int vLo=getIntArg(stok);
				final int vHi=getIntArg(stok);
				//_louverIF.home(hallPosition,setPx,pxValue,vLo,vHi);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _louverIF.home(hallPosition,setPx,pxValue,vLo,vHi);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("lp")){
			try{
				final double positionPercent=getDoubleArg(stok);
				//_louverIF.setLouverPercent(positionPercent);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _louverIF.setLouverPercent(positionPercent);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("ls")){
			try{
				//System.out.println(_louverIF.getLouverStatusMessage());
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println(_louverIF.getLouverStatusMessage());
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("ma")){
			try{
				final long positionCounts=getLongArg(stok);
				//_elmoIF.ptpAbsolute(positionCounts,true);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.ptpAbsolute(positionCounts,true);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mb")){
			try{
				//_elmoIF.beginMotion();
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.beginMotion();
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("md")){
			try{
				//_elmoIF.setEnable(false,Elmo.TM_CMD_MSEC);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setEnable(false,Elmo.TM_CMD_MSEC);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("me")){
			try{
				//_elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mf")){
			try{
				//System.out.println("MF:0x"+Integer.toHexString(_elmoIF.getFaultRegister()));
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println("MF:0x"+Integer.toHexString(_elmoIF.getFaultRegister()));
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mg")){
			try{
				final double ratio=getDoubleArg(stok);
				//_elmoIF.setGearRatio(ratio);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setGearRatio(ratio);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("mp")){
			try{
				final long positionCounts=getLongArg(stok);
				//_elmoIF.setPositionCounter(positionCounts);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setPositionCounter(positionCounts);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mr")){
			try{
				final long distanceCounts=getLongArg(stok);
				//_elmoIF.ptpRelative(distanceCounts,true);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.ptpRelative(distanceCounts,true);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mw")){
			try{
				final long delayMsec=getLongArg(stok);
				//_elmoIF.delay(delayMsec);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.delay(delayMsec);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("ms")){
			try{
				//System.out.println("SR:0x"+Integer.toHexString(_elmoIF.getStatusRegister()));
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println("SR:0x"+Integer.toHexString(_elmoIF.getStatusRegister()));
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mvc")){
			try{
				final int speedCounts=getIntArg(stok);
				//_elmoIF.setJoggingVelocity(speedCounts);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setJoggingVelocity(speedCounts);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mvp")){
			try{
				final int speedCounts=getIntArg(stok);
				//_elmoIF.setPTPSpeed(speedCounts);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setPTPSpeed(speedCounts);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mvr")){
			try{
				final double speedRPM=getDoubleArg(stok);
				//_elmoIF.setJoggingVelocity(Elmo.rpm2counts(speedRPM,_elmoIF.getCountsPerRevolution()));
				//System.out.println("speedRPM="+speedRPM+" counts="+_elmoIF.rpm2counts(speedRPM));
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setJoggingVelocity(_elmoIF.rpm2counts(speedRPM));
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("mvo")){
			try{
				final double speedRPM=getDoubleArg(stok);
				//_elmoIF.setJoggingVelocity(Elmo.rpm2counts(speedRPM*_elmoIF.getGearRatio(),_elmoIF.getCountsPerRevolution()));
				//System.out.println("ospeedRPM="+speedRPM+" counts="+_elmoIF.orpm2counts(speedRPM));
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setJoggingVelocity(_elmoIF.orpm2counts(speedRPM));
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("tc")){
			try{
				//System.out.println("TC:"+_thrusterIF.getTSTriggerCount());
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println("TC:"+_thrusterIF.getTSTriggerCount());
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("te")){
			try{
				final boolean value=(getIntArg(stok)==0?false:true);
				//_thrusterIF.setTurnsSensorEnable(value);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _thrusterIF.setTurnsSensorEnable(value);
							  }
							  });
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("tjc")){
			try{
				final int speedCounts=getIntArg(stok);
				//_elmoIF.setJoggingVelocity(speedCounts);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setJoggingVelocity(speedCounts);
							  }
							  });
				//_elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
							  }
							  });
				//_elmoIF.beginMotion();
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.beginMotion();
							  }
							  });				
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("tjr")){
			try{
				final double speedRPM=getDoubleArg(stok);
				//_elmoIF.setJoggingVelocity(Elmo.rpm2counts(speedRPM,_elmoIF.getCountsPerRevolution()));
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setJoggingVelocity(_elmoIF.rpm2counts(speedRPM));
							  }
							  });
				//_elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
							  }
							  });
				//_elmoIF.beginMotion();
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.beginMotion();
							  }
							  });				
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("tjo")){
			try{
				final double speedRPM=getDoubleArg(stok);
				//_elmoIF.setJoggingVelocity(Elmo.rpm2counts(speedRPM*_elmoIF.getGearRatio(),_elmoIF.getCountsPerRevolution()));
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setJoggingVelocity(_elmoIF.orpm2counts(speedRPM));
							  }
							  });
				//_elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.setEnable(true,Elmo.TM_CMD_MSEC);
							  }
							  });
				//_elmoIF.beginMotion();
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  _elmoIF.beginMotion();
							  }
							  });				
			}catch (Exception e) {
				throw e;
			}														
		}else if(cmd.equals("ts")){
			try{
				//System.out.println(_thrusterIF.getThrusterSampleMessage());
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println(_thrusterIF.getThrusterSampleMessage());
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("tss")){
			try{
				//System.out.println("TS: ("+_thrusterIF.getTSStateName()+") ["+_thrusterIF.getTSState()+"]");
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println("TS: ("+_thrusterIF.getTSStateName()+") ["+_thrusterIF.getTSState()+"]");
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else if(cmd.equals("tt")){
			try{
				//System.out.println("TT:"+_thrusterIF.getTSElapsedMsec()+" ms");
				_cmdQueue.add(new CmdCallback(){
							  public void callbackAction() throws Exception{
							  System.out.println("TT:"+_thrusterIF.getTSElapsedMsec()+" ms");
							  }
							  });
			}catch (Exception e) {
				throw e;
			}											
		}else{
			System.err.println("unrecognized command ["+cmd+"("+args+")]");
		}
		
		return;
	}
	
	/*
	public interface ArgTypes{
		public final static int T_INT=0;
		public final static int T_DOUBLE=1;
		public final static int T_LONG=2;
		public final static int T_BOOL=3;
		public final static int T_STRING=4;
	}
	public class UserCmd{
		ArgTypes[] types;
		String cmd;
		StringTokenizer tok;
		Vector args;
		CmdCallback action;
		
		public UserCmd(String acmd, StringTokenizer atok, ArgTypes aTypes[]){
			action=callback;
			cmd=acmd;
			tok=atok;
			types=aTypes;
			validate();
		}
		public void validate(){
			
		}
		public void setCallback(CmdCallback callback){
			action=callback;
		}
		public void doAction(){
			action.callbackAction();
		}
	}
	 */
}
/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.util.Vector;
import java.util.Iterator;
import java.util.Map;
import java.text.ParseException;
import java.io.IOException;
import java.rmi.RemoteException;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger; 

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.core.DeviceService;
import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.AnalogInstrumentPort;
import org.mbari.siam.core.ServiceSandBox;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.BoxcarFilter;
import org.mbari.siam.utils.WeightedAverageFilter;
import org.mbari.siam.utils.RangeValidator;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.Velocity;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ControlResponseIF;
import org.mbari.siam.distributed.devices.ControlProcessIF;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.distributed.devices.ProcessStateIF;
import org.mbari.siam.distributed.devices.ProcessConfigIF;
import org.mbari.siam.distributed.devices.ProcessParameterIF;
import org.mbari.siam.distributed.devices.ControlLoopConfigIF;
import org.mbari.siam.distributed.devices.ControlOutputIF;
import org.mbari.siam.distributed.devices.Valve2WayIF;

/**
 ControlLoop implements control system for the FOCE CO2 delivery subsystem.
 ControlLoop reads sensors (pH, current) and acutates 
 motors and valves to maintain desired pH within the FOCE apparatus. 
 Control inputs are received via DataTurbine (OSDT) 
 or the instrument service registry.
 
 Control inputs include:
 - pH (4 internal, 2 external, 1 in the ESW subsystem)
 - ADV (internal chamber water velocity)
 - ADCP(ambient environmental water velocity)
 
 Control outputs include:
 - ESW pump
 - ESW valves
 - Thruster motors
 
 The ControlLoop service manages separate threads for two independent
 control loops. The pH loop servos around pH by actuating the ESW pump
 and valves. The velocity control loop maintains water velocity in one
 of several control modes. A third thread, the process monitor, monitors 
 the state/status of control inputs and outputs.
 
 Each of these threads is independent, and may be run at different 
 update periods. 
 
 In addition to the control threads, a ControlProcessIF instance manages all
 of the data connections and filtering, providing access to all of the control
 system internal signals and state information. The control loop implements the
 ControlProcessIF interface as a thin wrapper, passing calls to the actual
 ControlProcessIF instance.
 
 The ControlLoopService produces a number of record types each readSample() is called.
 The primary record type is pH control loop data. Additional packets are published for
 the velocity control loop and status packets for each of the control inputs and outputs.
 
 Separate PacketParsers exist for each record type. These are encapsulated in a single
 ControlLoopParser class, which delegates to the appropriate parser for each record type
 received.
 
 A stand-alone Attributes class (subclasses InstrumentServiceAttributes) contains all
 of the configuration settings for the control loop. A single ControlLoopAttributes
 instance is shared by the ControlLoopService and the various control threads.
 
 */
/*
  $Id:$
  $Name:$
  $Revision:$
*/
public class ControlLoopService extends PolledInstrumentService
    implements Instrument, InstrumentDataListener,ProcessConfigIF,ControlLoopConfigIF, ProcessParameterIF
	{
		//////////////////////////////
		// Public constant members
		//////////////////////////////
		
		/** Record type (use base class default record type).
			Other record types are indexed from this value.
		 */
		public static final long RECORDTYPE_BASE         = 1L;
		/** pH control loop data record type */
		public static final long RECORDTYPE_PH_PID       = RECORDTYPE_BASE+1;
		/** velocity control loop data record type */
		public static final long RECORDTYPE_VELOCITY_PID = RECORDTYPE_BASE+2;
		/** control input state/status data record type */
		public static final long RECORDTYPE_INPUT_STATE  = RECORDTYPE_BASE+3;
		/** control output state/status data record type */
		public static final long RECORDTYPE_OUTPUT_STATE = RECORDTYPE_BASE+4;

		/** max raw data buffer size */
		public static final int _MAX_DATA_BYTES=512;
		
		//////////////////////////////
		// Data members
		//////////////////////////////
		
		/** Log4j logger */
		protected static Logger _log4j = Logger.getLogger(ControlLoopService.class);
				
		/** Service attributes */
		public ControlLoopAttributes _attributes;

		/** Instrument registry instance */
		InstrumentRegistry _registry = InstrumentRegistry.getInstance();
		/** pH control loop */
		ControlResponseIF _phControlResponse;
		/** velocity control loop */
		ControlResponseIF _velocityControlResponse;
		/** ControlProcess (manages control connections and signals) */
		ControlProcessIF  _foceControlProcess;
		/** worker thread for pH control loop */
		ControlLoopWorker _phLoopWorker;
		/** worker thread for velocity control loop */
		ControlLoopWorker _velocityLoopWorker;
		/** process monitor loop (monitors status of inputs and outputs) 
		 */
		ProcessMonitor _processMonitor;
		
		/** packet parser instance */
		PacketParser _parser=null;
		/** (reusable) container to pass raw data between methods */
		String[] _dataBox=null;
		/** (reusable) data packet instance */
		SensorDataPacket _dataPacket;
				
		/** Zero-arg constructor	*/
		public ControlLoopService() throws RemoteException
		{
			super();
			_attributes=new ControlLoopAttributes(this);
		}
		
		//////////////////////////////
		// Instrument Implementations
		//////////////////////////////
		
		/** Self-test not implemented. */
		public int test() {return -1;}
		
		/** required by PolledInstrumentService */
		protected ScheduleSpecifier createDefaultSampleSchedule()
		throws ScheduleParseException{
			return new ScheduleSpecifier(600000L);
		}
		
		/**required by BaseInstrumentService */
		protected void requestSample() throws Exception {}

		/**required by BaseInstrumentService */
		protected  PowerPolicy initInstrumentPowerPolicy(){return PowerPolicy.NEVER;}
		/**required by BaseInstrumentService */
		protected  PowerPolicy initCommunicationPowerPolicy(){return PowerPolicy.NEVER;}
		/**required by BaseInstrumentService */
		protected int initMaxSampleBytes() {return _MAX_DATA_BYTES;}
		/**required by BaseInstrumentService */
		protected byte[] initPromptString() { return "none".getBytes();}
		/**required by BaseInstrumentService */
		protected byte[] initSampleTerminator() {return "\n".getBytes();}
		/**required by BaseInstrumentService */
		protected int initCurrentLimit() {return 1000;}
		/**required by BaseInstrumentService */
		protected int initInstrumentStartDelay() {return 0;}
		/**required by DeviceService */
		public SerialPortParameters getSerialPortParameters()
		throws UnsupportedCommOperationException
		{
			return new SerialPortParameters(19200, SerialPort.DATABITS_8,
											SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
		}
				
		/** Register us for data callbacks from the temperature device */
		protected void initializeInstrument() throws InitializeException, Exception
		{
			super.initializeInstrument();
			_log4j.debug("ControlLoopService.initializeInstrument: done with super.initialize()");
			_dataPacket=new SensorDataPacket(getId(),1);
			_foceControlProcess=new FOCEProcess(_attributes);
			_log4j.debug("ControlLoopService.initializeInstrument: got FOCEProcess:"+_foceControlProcess);
			_log4j.debug("ControlLoopService.initializeInstrument: pH response mode is "+_attributes.ph_response_mode);
			switch(_attributes.ph_response_mode){
				case RESPONSE_MODE_LIN:
					_phControlResponse=new PH_LIN_Responder((FOCEProcess)_foceControlProcess,_attributes);
					break;
				case RESPONSE_MODE_PID:
				case RESPONSE_MODE_EXP:
					_log4j.debug("ControlLoopService.initializeInstrument: creating PID_PH");
					_phControlResponse=new PH_PID_Responder((FOCEProcess)_foceControlProcess,_attributes);
					break;
				default:
					throw new InitializeException("Invalid response mode ("+_attributes.ph_response_mode+") in initializeInstrument");
			}
			_log4j.debug("ControlLoopService.initializeInstrument: creating PID_Velocity");
			_velocityControlResponse=new VEL_PID_Responder((FOCEProcess)_foceControlProcess,_attributes);
			// null worker so that initializeControl creates a new one
			_phLoopWorker=null;
			_velocityLoopWorker=null;
			_log4j.debug("ControlLoopService.initializeInstrument: starting FOCE controlProcess");
			_foceControlProcess.startProcess();
			_log4j.debug("ControlLoopService.initializeInstrument: initializing control loops");
			initializeControl(LID_ALL);
			_log4j.debug("ControlLoopService.initializeInstrument: starting control loops");
			startControl(LID_ALL);
			_log4j.debug("ControlLoopService.initializeInstrument: starting process monitor");
			_processMonitor=new ProcessMonitor(_foceControlProcess,"Process Monitor",_attributes.monitor_period_msec);
			_processMonitor.start();
		}
		
		/** The instrument service framework architecture allowss one sample
			per cycle. We'll treat the pH PID data as the one sample, and 
		    log additional records using logSamples() (defined in this class).
		 */
		protected int readSample(byte[] sample) 
		throws TimeoutException, IOException, Exception
		{
			// the instrument service is designed for one sample
			// per cycle. We'll treat the pH PID data as the
			// one sample, but log the rest on our own.
			StringBuffer phPIDData= _phControlResponse.getSampleBuffer();
			// get default raw sample buffer (holds MAX_SAMPLE_BYTES)
			byte[] sampleBuf=getSampleBuf();
			
			// copy data into sample buffer
			System.arraycopy(phPIDData.toString().getBytes(),0,sampleBuf,0,phPIDData.length());

			// set record type for pH PID data
			_recordType=RECORDTYPE_PH_PID;
			
			// get the velocity PID and I/O connector state data
			StringBuffer velocityPIDData= _velocityControlResponse.getSampleBuffer();
			String[] iostates= _processMonitor.getSampleBuffers();
			_dataBox=new String[iostates.length+1];
			_dataBox[0]=velocityPIDData.toString();
			System.arraycopy(iostates,0,_dataBox,1,iostates.length);
			logSamples(_dataBox);
						
			// return size of pH PID data
			return phPIDData.length();
		}
		
		/** Return a PacketParser. */
		public PacketParser getParser() throws NotSupportedException{
			if(_parser==null){
				_parser=new ControlServicePacketParser(_attributes.registryName);
			}
			return _parser;
		}
		
		/** Parse a SensorDataPacket into a double[] array (used by infrastructure)  */
		public Object parseDataPacket(SensorDataPacket pkt) throws InvalidDataException
		{
			try{
				PacketParser parser=getParser();
				return parser.parseFields(pkt);
			}catch(NotSupportedException e){
				_log4j.error(e.toString());
			}catch(ParseException p){
				throw new InvalidDataException("ParseException caught: "+p.toString());
			}
			return null;
		}
		
		/////////////////////////////////////////////////////////
		//  Internal service methods
		/////////////////////////////////////////////////////////
		
		protected long getRecordType(Map map,String key){

			try{
				Iterator iterator= map.keySet().iterator();
				while(iterator.hasNext()){
					Integer nextKey=((Integer)iterator.next());
					if( ((String)map.get(nextKey)).equals(key) ){
						return nextKey.longValue();
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}			
			_log4j.error("could not find key ["+key+"] in map");
			return RECORDTYPE_UNDEFINED;
		}
		
		/** Log additional sample record types. */
		protected void logSamples(String[] samples){
			
			// iterate through all samples
			// (potentially a mixture of PID_Velocity
			// and input/output state records)
			for(int i=0; (i < samples.length) ;i++){
				
				if(samples[i]!=null){					
					
					// get next sample string
					String dstring=samples[i];
					byte[] dbytes=dstring.getBytes();
					long recordType=RECORDTYPE_UNDEFINED;
					if(_log4j.isDebugEnabled()){
						_log4j.debug("logging record:"+dstring);
					}
					
					// determine generic type based on header
					if(dstring.indexOf(InputState.RECORD_HEADER)>=0){
						recordType=RECORDTYPE_INPUT_STATE;
					}else if(dstring.indexOf(OutputState.RECORD_HEADER)>=0){
						recordType=RECORDTYPE_OUTPUT_STATE;
					}else if(dstring.indexOf(VEL_Responder.RECORD_HEADER)>=0){
						recordType=RECORDTYPE_VELOCITY_PID;
					}else{
						_log4j.error("Unrecognized record type ["+dstring+"]");
						continue;
					}
					
					// set the data buffer, time and generic record type
					_dataPacket.setDataBuffer(dbytes);
					_dataPacket.setSystemTime(System.currentTimeMillis());
					if(_log4j.isDebugEnabled()){
						_log4j.debug("setting record type="+recordType);
					}
					_dataPacket.setRecordType(recordType);
					
					// now reassign the record type for each input/output
					// this allows data turbine to differentiate each
					// input/output record stream
					if(recordType==RECORDTYPE_INPUT_STATE){
						try{
							PacketParser.Field[] fields=(PacketParser.Field[])parseDataPacket(_dataPacket);
							String ioname=(String)fields[InputStateParser.NAME_INDEX+1].getValue();
							long newType=getRecordType(input_id2iname,ioname);
							_dataPacket.setRecordType(newType);
						}catch(Exception e){
							_log4j.error("error setting record type for i/o state packet - using generic record type");
						}
					}
					if(recordType==RECORDTYPE_OUTPUT_STATE){
						try{
							PacketParser.Field[] fields=(PacketParser.Field[])parseDataPacket(_dataPacket);
							String ioname=(String)fields[OutputStateParser.NAME_INDEX+1].getValue();
							long newType=getRecordType(output_id2name,ioname);
							_dataPacket.setRecordType(newType);
						}catch(Exception e){
							_log4j.error("error setting record type for i/o state packet - using generic record type");
						}
					}
					
					// Call the DataListeners that have registered for dataCallbacks
					// OUTPUT_STATE packets have no numeric fields, Turbinator 
					// throws Exceptions when it recieves them (but shouldn't)
					callDataListeners(_dataPacket);
					
					// always log velocity PID packets
					// optionally log input/ouput state packets
					if( (recordType==RECORDTYPE_VELOCITY_PID) ||
					   (recordType==RECORDTYPE_OUTPUT_STATE && _attributes.logOutputState==true) ||
					   (recordType==RECORDTYPE_INPUT_STATE && _attributes.logInputState==true)
					   ){
						// write packet to log
						logPacket(_dataPacket);
					}
				}else{
					_log4j.warn("null sample in logSamples ["+i+"/"+samples.length+"]");
				}
			}
		}
		
		/** Stop the ESW pump */
		public void stopESWPump()
		throws Exception{
			ControlOutputIF pumpOutput=_foceControlProcess.getOutput(OUTPUT_ESW_PUMP_VELOCITY);
			if(pumpOutput!=null){
				ElmoIF eswPump=(ElmoIF)pumpOutput.getDevice();
				if(eswPump!=null){
					eswPump.jog(0);
				}else{
					_log4j.warn("eswPump device is null");
				}
			}else{
				_log4j.warn("eswPump output is null");
			}
		}
		
		/** Close the ESW valves */
		public void closeESWValves()
		throws Exception{
			ControlOutputIF fwdValveOutput=_foceControlProcess.getOutput(OUTPUT_FWD_ESW_VALVE);
			if(fwdValveOutput!=null){
				Valve2WayIF fwdValve=(Valve2WayIF)fwdValveOutput.getDevice();
				if(fwdValve!=null){
					fwdValve.close();
				}else{
					_log4j.warn("fwdValve device is null");
				}
			}else{
				_log4j.warn("fwdValve output is null");
			}
			
			ControlOutputIF aftValveOutput=_foceControlProcess.getOutput(OUTPUT_AFT_ESW_VALVE);
			if(aftValveOutput!=null){
				Valve2WayIF aftValve=(Valve2WayIF)aftValveOutput.getDevice();
				if(aftValve!=null){
					aftValve.close();
				}else{
					_log4j.warn("aftValve device is null");
				}
			}else{
				_log4j.warn("aftValve output is null");
			}
		}
		
		/** Stop the thrusters */
		public void stopThrusters()
		throws Exception{
			ControlOutputIF fwdThrusterOutput=_foceControlProcess.getOutput(OUTPUT_FWD_THRUSTER_VELOCITY);
			if(fwdThrusterOutput!=null){
				ElmoIF fwdThruster=(ElmoIF)fwdThrusterOutput.getDevice();
				if(fwdThruster!=null){
					fwdThruster.jog(0);
				}else{
					_log4j.warn("fwdThruster device is null");
				}
			}else{
				_log4j.warn("fwdThruster output is null");
			}
			ControlOutputIF aftThrusterOutput=_foceControlProcess.getOutput(OUTPUT_AFT_THRUSTER_VELOCITY);
			if(aftThrusterOutput!=null){
				ElmoIF aftThruster=(ElmoIF)aftThrusterOutput.getDevice();
				if(aftThruster!=null){
					aftThruster.jog(0);
				}else{
					_log4j.warn("aftThruster device is null");
				}
			}else{
				_log4j.warn("aftThruster output is null");
			}
		}
		
		
		/////////////////////////////////////////////////////////
		//        InstrumentDataListener Implementations       //
		/////////////////////////////////////////////////////////
		
		/** dataCallback from the sensors.
		 Fulfills InstrumentDataListener interface
		 */
		public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields)
		{
			_log4j.warn("dataCallback does nothing");
		}
		
		/** Callback for InstrumentDataListener interface, called when 
		 service is registered with the InstrumentRegistry
		 Fulfills InstrumentDataListener interface
		 */
		public void serviceRegisteredCallback(RegistryEntry entry)
		{
			_log4j.info("serviceRegisteredCallback does nothing");
		}

		/////////////////////////////////////////////////////////
		// ControlLoopConfigIF implementations
		/////////////////////////////////////////////////////////

		/** Initialize one or more control loops */
		public void initializeControl(int id)
		throws Exception{
			if(id!=LID_VELOCITY_LOOP && id!=LID_PH_LOOP && id!=LID_ALL){
				_log4j.error("Invalid ID ["+id+"] in initializeControl");
				return;
			}
			
			if(id==LID_PH_LOOP || id==LID_ALL){
				if(_phLoopWorker==null){
					if(_log4j.isDebugEnabled()){
						_log4j.debug("initializing phLoopWorker...");
					}
					_phLoopWorker=new ControlLoopWorker(_foceControlProcess,_phControlResponse,_attributes.loop_period_msec,"pH loop");
					if(_log4j.isDebugEnabled()){
						_log4j.debug("phLoopWorker initialized...");
					}
				}else{
					if(_log4j.isDebugEnabled()){
						_log4j.debug("ControlLoopService worker already initialized");
					}
				}
			}
			if(id==LID_VELOCITY_LOOP || id==LID_ALL){
				if(_velocityLoopWorker==null){
					if(_log4j.isDebugEnabled()){
						_log4j.debug("initializing velocityLoopWorker...");
					}
					_velocityLoopWorker=new ControlLoopWorker(_foceControlProcess,_velocityControlResponse,_attributes.loop_period_msec,"velocity loop");
					if(_log4j.isDebugEnabled()){
						_log4j.debug("velocityLoopWorker initialized...");
					}
				}else{
					if(_log4j.isDebugEnabled()){
						_log4j.debug("ControlLoopService worker already initialized");
					}
				}
			}
		}

		/** Reset control service (not implemented) */
		public void resetControl(int id) 
		throws Exception{
			throw new Exception("resetControl not implemented (ControlLoopService)");
		}
		
		/** start one or more control loops */
		public void startControl(int id)
		throws Exception{
			if(id!=LID_VELOCITY_LOOP && id!=LID_PH_LOOP && id!=LID_ALL){
				_log4j.error("Invalid ID ["+id+"] in startControl");
				return;
			}
			
			if(id==LID_PH_LOOP || id==LID_ALL){
				if(!_phLoopWorker.isRunning()){
					if(_log4j.isDebugEnabled()){
						_log4j.debug("starting phLoopWorker...");
					}
					_phLoopWorker.start();
					if(_log4j.isDebugEnabled()){
						_log4j.debug("phLoopWorker running.");
					}
				}else{
					if(_log4j.isDebugEnabled()){
						_log4j.debug("pH ControlLoopService worker already running");
					}
				}
			}
			if(id==LID_VELOCITY_LOOP || id==LID_ALL){
				if(!_velocityLoopWorker.isRunning()){
					_log4j.debug("starting velocityLoopWorker...");
					_velocityLoopWorker.start();
					if(_log4j.isDebugEnabled()){
						_log4j.debug("velocityLoopWorker running.");
					}
				}else{
					if(_log4j.isDebugEnabled()){
						_log4j.debug("velocity ControlLoopService worker already running");
					}
				}
			}
		}
		
		/** stop one or more control loops */
		public void stopControl(int id)
		throws Exception{
			if(id!=LID_VELOCITY_LOOP && id!=LID_PH_LOOP && id!=LID_ALL){
				_log4j.error("Invalid ID ["+id+"] in stopControl");
				return;
			}
			if(id==LID_PH_LOOP || id==LID_ALL){
				if(_phLoopWorker.isRunning()){
					if(_log4j.isDebugEnabled()){
						_log4j.debug("stopping phLoopWorker...");
					}
					_phLoopWorker.terminate();
					if(_log4j.isDebugEnabled()){
						_log4j.debug("phLoopWorker stopped.");
					}
				}else{
					if(_log4j.isDebugEnabled()){
						_log4j.debug("pH ControlLoopService worker already stopped");
					}
				}
			}
			if(id==LID_VELOCITY_LOOP || id==LID_ALL){
				if(_velocityLoopWorker.isRunning()){
					if(_log4j.isDebugEnabled()){
						_log4j.debug("stopping velocityLoopWorker...");
					}
					_velocityLoopWorker.terminate();
					if(_log4j.isDebugEnabled()){
						_log4j.debug("velocityLoopWorker stopped.");
					}
				}else{
					if(_log4j.isDebugEnabled()){
						_log4j.debug("velocity ControlLoopService worker already stopped");
					}
				}
			}
		}
		
		/** pause one or more control loops */
		public void pauseControl(int id){
			if(id!=LID_VELOCITY_LOOP && id!=LID_PH_LOOP && id!=LID_ALL){
				_log4j.error("Invalid ID ["+id+"] in pauseControl");
				return;
			}
			if(id==LID_PH_LOOP || id==LID_ALL){
				_phLoopWorker.pause(true);
			}
			if(id==LID_VELOCITY_LOOP || id==LID_ALL){
				_velocityLoopWorker.pause(true);
			}
		}
		
		/** resume one or more (paused) control loops */
		public void resumeControl(int id){
			if(id!=LID_VELOCITY_LOOP && id!=LID_PH_LOOP && id!=LID_ALL){
				_log4j.error("Invalid ID ["+id+"] in resumeControl");
				return;
			}
			if(id==LID_PH_LOOP || id==LID_ALL){
				_phLoopWorker.pause(false);
			}
			if(id==LID_VELOCITY_LOOP || id==LID_ALL){
				_velocityLoopWorker.pause(false);
			}
		}

		/** Enter a pre-defined operation mode indicated by modeID */
		public void setPHResponseMode(int modeID)
		throws Exception, RemoteException{
			
			if(modeID==_attributes.ph_response_mode){
				// mode already set to requested value
				return;
			}
			boolean isError=false;
			ControlResponseIF response=null;
			switch (modeID) {
				case MODE_LIN:
					_attributes.ph_response_mode=RESPONSE_MODE_LIN;
					response=new PH_LIN_Responder((FOCEProcess)_foceControlProcess,_attributes);
					break;
				case MODE_PID:
					_attributes.ph_response_mode=RESPONSE_MODE_PID;
					response=new PH_PID_Responder((FOCEProcess)_foceControlProcess,_attributes);
					break;
				case MODE_EXP:
					//_attributes.ph_response_mode=RESPONSE_MODE_EXP;
					//break;
				default:
					isError=true;
					break;
			}

			if(isError){
				throw new Exception("Invalid mode ID ["+modeID+"] in setPHResponseMode");
			}
			// stop the loop and associated workers
			pauseControl(LID_PH_LOOP);
			// assign new reponse instance
			_phControlResponse=response;
			// assign new response to control loop worker
			_phLoopWorker.setResponse(_phControlResponse);
			// start the control loop
			resumeControl(LID_PH_LOOP);
			
			return;
		}

		/** Get response mode indicated (modeID) */
		public int getPHResponseMode()throws Exception, RemoteException{
			return _attributes.ph_response_mode;
		}

		/** Enter a pre-defined operation mode indicated by modeID */
		public void setPHControlMode(int modeID)
		throws Exception, RemoteException{
			
			// disengage the control loops
			pauseControl(LID_PH_LOOP);
			boolean isError=false;
			switch (modeID) {
				case MODE_MANUAL:
					_attributes.ph_control_mode=CONTROL_MODE_MANUAL;
					break;
				case MODE_CONSTANT:
					_attributes.ph_control_mode=CONTROL_MODE_CONSTANT;
					break;
				case MODE_OFFSET:
					_attributes.ph_control_mode=CONTROL_MODE_OFFSET;
					break;
				case MODE_PANIC:
					// shut it down fast
					if(_log4j.isDebugEnabled()){
						_log4j.debug("stopping pH control loop...");
					}
					stopControl(LID_PH_LOOP);
					if(_log4j.isDebugEnabled()){
						_log4j.debug("stopping ESW pump...");
					}
					stopESWPump();
					// confirm motor is stopped...
					if(_log4j.isDebugEnabled()){
						_log4j.debug("closing ESW valves...");
					}
					closeESWValves();
					if(_log4j.isDebugEnabled()){
						_log4j.debug("shutdown complete");
					}
					return;
				default:
					isError=true;
					break;
			}
			// resume the control loops
			resumeControl(LID_PH_LOOP);
			if(isError){
				throw new Exception("Invalid mode ID ["+modeID+"] in setPHControlMode");
			}
			return;
		}

		/** Get pH control mode indicated (modeID) */
		public int getPHControlMode()throws Exception, RemoteException{
			return _attributes.ph_control_mode;
		}

		/** Enter a pre-defined operation mode indicated by modeID */
		public void setVelocityControlMode(int modeID)
		throws Exception, RemoteException{
			
			// disengage the control loops
			pauseControl(LID_VELOCITY_LOOP);
			boolean isError=false;
			switch (modeID) {
				case MODE_MANUAL:
					_attributes.velocity_control_mode=CONTROL_MODE_MANUAL;
					break;
				case MODE_CONSTANT:
					_attributes.velocity_control_mode=CONTROL_MODE_CONSTANT;
					break;
				case MODE_OFFSET:
					_attributes.velocity_control_mode=CONTROL_MODE_OFFSET;
					break;
				case MODE_DEADBAND:
					_attributes.velocity_control_mode=CONTROL_MODE_DEADBAND;
					break;
				case MODE_PANIC:
					// shut it down fast
					if(_log4j.isDebugEnabled()){
						_log4j.debug("stopping velocity control loop...");
					}
					stopControl(LID_VELOCITY_LOOP);
					if(_log4j.isDebugEnabled()){
						_log4j.debug("stopping thrusters...");
					}
					stopThrusters();
					if(_log4j.isDebugEnabled()){
						_log4j.debug("shutdown complete");
					}
					return;
				default:
					isError=true;
					break;
			}
			// resume the control loops
			resumeControl(LID_VELOCITY_LOOP);
			if(isError){
				throw new Exception("Invalid mode ID ["+modeID+"] in setVelocityControlMode");
			}
			return;
		}
		
		/** Get velocity control mode indicated (modeID) */
		public int getVelocityControlMode()throws Exception, RemoteException{
			return _attributes.velocity_control_mode;
		}
		
		/** Enter a pre-defined velocity response mode indicated by modeID */
		public void setVelocityResponseMode(int modeID)throws Exception, RemoteException{
			if(modeID==_attributes.velocity_response_mode){
				// mode already set to requested value
				return;
			}
			boolean isError=false;
			ControlResponseIF response=null;
			switch (modeID) {
					// This falls through for now...
				case MODE_LIN:
				//	_attributes.velocity_response_mode=RESPONSE_MODE_LIN;
				//	response=new VEL_LIN_Responder((FOCEProcess)_foceControlProcess,_attributes);
				//	break;
				case MODE_PID:
					_attributes.velocity_response_mode=RESPONSE_MODE_PID;
					response=new VEL_PID_Responder((FOCEProcess)_foceControlProcess,_attributes);
					break;
				case MODE_EXP:
					//_attributes.ph_response_mode=RESPONSE_MODE_EXP;
					//break;
				default:
					isError=true;
					break;
			}
			
			if(isError){
				throw new Exception("Invalid mode ID ["+modeID+"] in setVelocityResponseMode");
			}
			// stop the loop and associated workers
			pauseControl(LID_VELOCITY_LOOP);
			// assign new reponse instance
			_velocityControlResponse=response;
			// assign new response to control loop worker
			_velocityLoopWorker.setResponse(_velocityControlResponse);
			// start the control loop
			resumeControl(LID_VELOCITY_LOOP);
			
			return;
		}
		
		/** Get velocity response mode indicated (modeID) */
		public int getVelocityResponseMode()throws Exception, RemoteException{
			return _attributes.velocity_response_mode;
		}
		
		/** Fast shutdown of control loop, inputs and outputs. 
		 */
		public void panicStop(int id)throws Exception, RemoteException{
			// shut it down fast
			if(_log4j.isDebugEnabled()){
				_log4j.debug("stopping control loops...");
			}
			stopControl(LID_ALL);
			if(_log4j.isDebugEnabled()){
				_log4j.debug("stopping ESW pump...");
			}
			stopESWPump();
			// confirm motor is stopped...
			if(_log4j.isDebugEnabled()){
				_log4j.debug("closing ESW valves...");
			}
			closeESWValves();
			if(_log4j.isDebugEnabled()){
				_log4j.debug("stopping thrusters...");
			}
			stopThrusters();
			if(_log4j.isDebugEnabled()){
				_log4j.debug("stopping control process (disconnects inputs and outputs)");
			}
			_foceControlProcess.stopProcess();					
			if(_log4j.isDebugEnabled()){
				_log4j.debug("shutdown complete");
			}
			return;
			
		}
		
		
		/** Set the input weighting for the specified (weighted average) filter input. 

		 In this context, the weight value is generally >= 0, though it could be set negative
		 to create linear combinations of inputs.

		 The input weights may be used in conjuction with divisor to dynamically configure signal filtering.

		 @see #setFilterDivisor(int filterID, int divisor)
		 
		 */
		public void setFilterInputWeight(int inputID, double weight)
		throws Exception{
			
			FOCEProcess foceProcess=(FOCEProcess)_foceControlProcess;
			if(weight<0){
				//throw new Exception("Invalid weight ["+weight+"] in setFilterInputWeight - must be >=0");
				_log4j.warn("Input weight ["+inputID+"] < 0 ["+weight+"] in setFilterInputWeight");
			}
			FilterInput filterInput=null;
			switch(inputID){
				case IID_PH_FCOM_L:
					filterInput =foceProcess.getFilterInput(IFC_PH_INT_FWD_L);
					break;
				case IID_PH_FCOM_R:
					filterInput =foceProcess.getFilterInput(IFC_PH_INT_FWD_R);
					break;
				case IID_PH_ACOM_L:
					filterInput =foceProcess.getFilterInput(IAC_PH_INT_AFT_L);
					break;
				case IID_PH_ACOM_R:
					filterInput =foceProcess.getFilterInput(IAC_PH_INT_AFT_R);
					break;
				case IID_PH_ICOM_FWD:
					filterInput =foceProcess.getFilterInput(IIC_PH_INT_FWD);
					break;
				case IID_PH_ICOM_AFT:
					filterInput =foceProcess.getFilterInput(IIC_PH_INT_AFT);
					break;
				case IID_PH_ECOM_L:
					filterInput =foceProcess.getFilterInput(IEC_PH_EXT_MID_L);
					break;
				case IID_PH_ECOM_R:
					filterInput =foceProcess.getFilterInput(IEC_PH_EXT_MID_R);
					break;
				default:
					throw new Exception("Invalid input ID ["+inputID+"] in setFilterInputWeight");
			}
			if(filterInput!=null){
				filterInput.setWeight(weight);
			}else{
				throw new Exception("Input ID ["+inputID+"] is null in setFilterInputWeight");
			}
		}
		
		/** Set the divisor for the specified (weighted average) filter. 
		 The divisor behavior is defined as follows:

		 If divisor == 0, 
		 the average is computed using the sum of the uninhibited inputs
		 divided by the total number of uninhibited inputs.
		 If divisor != 0, 
		 the average is computed using the sum of the uninhibited inputs
		 divided by the divisor.
		 
		 In this context, the divisor must be >=0; 
		 
		 The divisor may be used in conjuction with input weights to dynamically configure signal filtering.
		 @see #setFilterInputWeight(int inputID, double weight)
		 */
		public void setFilterDivisor(int filterID, int divisor)
		throws Exception{
			if(divisor<0){
				throw new Exception("Invalid divisor ["+divisor+"] in setFilterDivisor - must be >= 0");
			}
			FOCEProcess foceProcess=(FOCEProcess)_foceControlProcess;
			WeightedAverageFilter filter=null;
			switch (filterID) {
				case FID_FWD_COMBINER:
					filter=(WeightedAverageFilter)(foceProcess.getFilter(FC_PH_INT_FWD));
					break;
				case FID_AFT_COMBINER:
					filter=(WeightedAverageFilter)(foceProcess.getFilter(AC_PH_INT_AFT));
					break;
				case FID_INT_COMBINER:
					filter=(WeightedAverageFilter)(foceProcess.getFilter(IC_PH_INT));
				case FID_EXT_COMBINER:
					filter=(WeightedAverageFilter)(foceProcess.getFilter(EC_PH_EXT));
					break;
				default:
					throw new Exception("Invalid filterID ["+filterID+"] in setFilterDivisor");
			}
			if(filter!=null){
				filter.setDivisor(divisor);			
			}else{
				throw new Exception("Filter ID ["+filterID+"] is null in setFilterDivisor");
			}
		}
		
		/** Change filter configuration.
		 This method is used by clients to change which inputs are used to produce 
		 filtered signals used for control, e.g. Internal pH. 
		 This enables some flexibility combining and including/excluding sensors 
		 to produce filtered control signals.
		 
		 @param filterID A filter ID, as defined in the ControlLoopConfigIF interface
		 @param inputIDs An array of signal IDs that should be used as inputs to the filter indicated by filterID.
                         The valid signal IDs are defined in the ControlLoopConfigIF interface. 
		 @param inputWeights The respective weight of each of the specified input signals. If a signal weight is set to
							zero, the signal input is disabled and not included 
		 @param divisor  Divides filter value
		 
		 @see #setFilterDivisor(int filterID, int divisor)
		 
		 */
		public void configureFilter(int filterID, int[] inputIDs, double[] inputWeights,int divisor)
		throws Exception{
			if( !(_foceControlProcess instanceof FOCEProcess) ){
				throw new Exception("Cast failed: FOCE control process to FOCEProcess ");
			}
			
			// validate arguments
			// (specified input IDs should belong to filter specified by filterID)
			switch (filterID) {
				case FID_FWD_COMBINER:
					for(int i=0;i<inputIDs.length;i++){
						switch(inputIDs[i]){
							case IID_PH_FCOM_L:
							case IID_PH_FCOM_R:
								break;
							default:
								throw new Exception("Input ["+inputIDs[i]+"] does not belong to specified filter ["+filterID+"]");
						}
					}					
					break;
				case FID_AFT_COMBINER:
					for(int i=0;i<inputIDs.length;i++){
						switch(inputIDs[i]){
							case IID_PH_ACOM_L:
							case IID_PH_ACOM_R:
								break;
							default:
								throw new Exception("Input ["+inputIDs[i]+"] does not belong to specified filter ["+filterID+"]");
						}
					}					
					break;
				case FID_INT_COMBINER:
					for(int i=0;i<inputIDs.length;i++){
						switch(inputIDs[i]){
							case IID_PH_ICOM_FWD:
							case IID_PH_ICOM_AFT:
								break;
							default:
								throw new Exception("Input ["+inputIDs[i]+"] does not belong to specified filter ["+filterID+"]");
						}
					}					
					break;
				case FID_EXT_COMBINER:
					for(int i=0;i<inputIDs.length;i++){
						switch(inputIDs[i]){
							case IID_PH_ECOM_L:
							case IID_PH_ECOM_R:
								break;
							default:
								throw new Exception("Input ["+inputIDs[i]+"] does not belong to specified filter ["+filterID+"]");
						}
					}				
				default:
					throw new Exception("configureFilterWeights - Invalid filterID ["+filterID+"]");
			}

			for(int i=0;i<inputIDs.length;i++){
				setFilterInputWeight(inputIDs[i],inputWeights[i]);
			}
			setFilterDivisor(filterID,divisor);					
		}
		
		///////////////////////////////////
		// ProcessStateIF implementations //
		///////////////////////////////////
		/** get numeric value of specified parameter (attribute) by ID */
		public Number getParameter(int paramID) 
		throws Exception{
			return _foceControlProcess.getParameter(paramID);
		}

		/** get signal value by ID */
		public Number getSignal(int signalID) 
		throws Exception,RemoteException{
			return _foceControlProcess.getSignal(signalID);
		}
				
		/** get ID of specified parameter (attribute) by name */
		public int parameterID(String parameterName)
		throws Exception{
			return _foceControlProcess.parameterID(parameterName);
		}

		/** get name of specified parameter (attribute) by ID */
		public String parameterName(int parameterID)
		throws Exception{
			return _foceControlProcess.parameterName(parameterID);
		}

		/** get ID of specified signal by name */
		public int signalID(String signalName)
		throws Exception{
			return _foceControlProcess.signalID(signalName);
		}
		/** get list of parameter names */
		public String[] parameterNames()
		throws RemoteException{
			return _foceControlProcess.parameterNames();
		}
		/** get list of signal names */
		public String[] signalNames()
		throws RemoteException{
			return _foceControlProcess.signalNames();
		}

		/** get signal name by ID */
		public String signalName(int signalID) 
		throws RemoteException{
			return _foceControlProcess.signalName(signalID);
		}	
		
		/** return a signal name for the given signal ID */
		public String filterInputName(int inputID) 
		throws RemoteException{
			return _foceControlProcess.filterInputName(inputID);
		}
		
		///////////////////////////////////
		// ProcessConfigIF implementations //
		///////////////////////////////////
		
		/** set a numeric control system parameter (attribute) */
		public void setParameter(int parameterName, Number parameterValue) 
		throws Exception{
			_foceControlProcess.setParameter(parameterName,parameterValue);
		}
		
		/** parse and set a control system parameter (attribute) */
		public void setParameter(String parameterName, String parameterValue)
		 throws Exception{
			_foceControlProcess.setParameter(parameterName,parameterValue);
		}
		
	}

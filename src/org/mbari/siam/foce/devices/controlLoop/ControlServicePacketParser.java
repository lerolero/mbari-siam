/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Vector;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.devices.ProcessParameterIF;

import org.mbari.siam.foce.devices.controlLoop.ControlLoopService;

/** Packet parser for Control Loop Service record types.
	Delegates parsing to appropropriate parser for each packet type:
	- pH PID
    - H2O velocity PID
	- input state
	- output state
 */
public class ControlServicePacketParser extends PacketParser{
	
	static private Logger _log4j = Logger.getLogger(ControlServicePacketParser.class);
	
    private static final long serialVersionUID=1L;

	//protected static Velocity_PIDParser _velocityParser=null;
	//protected static PH_PIDParser _phParser=null;
	protected static VEL_ResponseParser _velocityParser=null;
	protected static PH_ResponseParser _phParser=null;
	protected static InputStateParser _inputParser=null;
	protected static OutputStateParser _outputParser=null;


	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public ControlServicePacketParser(){
		this("Control Loop Service");
	}
	
	public ControlServicePacketParser(String registryName){
		super(registryName);
		this.initialize();
	}
    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException {
		
		if(packet==null){
			throw new NotSupportedException("packet is null in parseFields");
		}
		if (!(packet instanceof SensorDataPacket)) {
			throw new NotSupportedException("packet is not a SensorDataPacket");
		}
		
		SensorDataPacket sdpacket=(SensorDataPacket)packet;

		if(sdpacket.getRecordType()==ControlLoopService.RECORDTYPE_PH_PID){
			if(_phParser==null){
				//_phParser       = new PH_PIDParser(_registryName,ControlResponseImpl.DELIMITERS);
				_phParser       = new PH_ResponseParser(_registryName,BaseResponder.DELIMITERS);
			}
			return _phParser.parseFields(sdpacket);
		}
		if(sdpacket.getRecordType()==ControlLoopService.RECORDTYPE_VELOCITY_PID){
			if(_velocityParser==null){
				//_velocityParser = new Velocity_PIDParser(_registryName,ControlResponseImpl.DELIMITERS);
				_velocityParser = new VEL_ResponseParser(_registryName,BaseResponder.DELIMITERS);
			}
			return _velocityParser.parseFields(sdpacket);
		}
		if(sdpacket.getRecordType()==ControlLoopService.RECORDTYPE_INPUT_STATE){
			if(_inputParser==null){
				_inputParser    = new InputStateParser(_registryName,InputState.DELIMITERS);
			}
			return _inputParser.parseFields(sdpacket);
		}
		if(sdpacket.getRecordType()==ControlLoopService.RECORDTYPE_OUTPUT_STATE){
			if(_outputParser==null){
				_outputParser   = new OutputStateParser(_registryName,OutputState.DELIMITERS);
			}
			return _outputParser.parseFields(sdpacket);
		}
		int packetType=new Long(sdpacket.getRecordType()).intValue();
		Integer iPacketType=new Integer(packetType);
		switch (packetType) {
			case ProcessParameterIF.IBX_PH_INT_FWD_L:
			case ProcessParameterIF.IBX_PH_INT_FWD_R:
			case ProcessParameterIF.IBX_PH_INT_AFT_L:
			case ProcessParameterIF.IBX_PH_INT_AFT_R:
			case ProcessParameterIF.IBX_PH_EXT_MID_L:
			case ProcessParameterIF.IBX_PH_EXT_MID_R:
			case ProcessParameterIF.IFC_PH_INT_FWD_L:
			case ProcessParameterIF.IFC_PH_INT_FWD_R:
			case ProcessParameterIF.IAC_PH_INT_AFT_L:
			case ProcessParameterIF.IAC_PH_INT_AFT_R:
			case ProcessParameterIF.IEC_PH_EXT_MID_L:
			case ProcessParameterIF.IEC_PH_EXT_MID_R:
			case ProcessParameterIF.IIC_PH_INT_FWD:
			case ProcessParameterIF.IIC_PH_INT_AFT:
			case ProcessParameterIF.IBX_PH_ESW:
			case ProcessParameterIF.IBX_VH2O_INT_X:
			case ProcessParameterIF.IBX_VH2O_INT_Y:
			case ProcessParameterIF.IBX_VH2O_EXT_X:
			case ProcessParameterIF.IBX_VH2O_EXT_Y:
			case ProcessParameterIF.IVC_VH2O_INT_X:
			case ProcessParameterIF.IVC_VH2O_INT_Y:
			case ProcessParameterIF.IVC_VH2O_EXT_X:
			case ProcessParameterIF.IVC_VH2O_EXT_Y:
			case ProcessParameterIF.IMA_MAG_INT_X:
			case ProcessParameterIF.IMA_MAG_INT_Y:
			case ProcessParameterIF.IHD_HDG_INT_X:
			case ProcessParameterIF.IHD_HDG_INT_Y:
			case ProcessParameterIF.IMA_MAG_EXT_X:
			case ProcessParameterIF.IMA_MAG_EXT_Y:
			case ProcessParameterIF.IHD_HDG_EXT_X:
			case ProcessParameterIF.IHD_HDG_EXT_Y:
			case ProcessParameterIF.IBX_VTHR_FWD:
			case ProcessParameterIF.IBX_VTHR_AFT:
			case ProcessParameterIF.IVS_ESWV_FWD:
			case ProcessParameterIF.IVS_ESWV_AFT:
				if(_inputParser==null){
					_inputParser    = new InputStateParser(_registryName,InputState.DELIMITERS);
				}
				try{
					// set a naming prefix equal to the input/output name to provide
					// a unique and easily identifiable DataTurbine channel name.
					// ** The packetType is assumed to have been set equal to the 
					// input/output ID by the control loop service **
					String prefix=(String)ProcessParameterIF.input_id2iname.get(iPacketType);
					if(prefix!=null){
						prefix=prefix.toLowerCase();
					}
					_inputParser.setNamingPrefix(prefix+"_");
				}catch (Exception e) {
					throw new NotSupportedException("Could not set name prefix for packetType ["+packetType+"]: "+e.getMessage());
				}
				return _inputParser.parseFields(sdpacket);
				
			case ProcessParameterIF.OUTPUT_ESW_PUMP_VELOCITY:
			case ProcessParameterIF.OUTPUT_FWD_THRUSTER_VELOCITY:
			case ProcessParameterIF.OUTPUT_AFT_THRUSTER_VELOCITY:
			case ProcessParameterIF.OUTPUT_FWD_ESW_VALVE:
			case ProcessParameterIF.OUTPUT_AFT_ESW_VALVE:
				if(_outputParser==null){
					_outputParser   = new OutputStateParser(_registryName,OutputState.DELIMITERS);
				}
				try{
					// set a naming prefix equal to the input/output name to provide
					// a unique and easily identifiable DataTurbine channel name.
					// ** The packetType is assumed to have been set equal to the 
					// input/output ID by the control loop service **
					String prefix=(String)ProcessParameterIF.input_id2iname.get(iPacketType);
					if(prefix!=null){
						prefix=prefix.toLowerCase();
					}
					_outputParser.setNamingPrefix(prefix+"_");
				}catch (Exception e) {
					throw new NotSupportedException("Could not set name prefix for packetType ["+packetType+"]: "+e.getMessage());
				}
				return _outputParser.parseFields(sdpacket);
		}
		throw new NotSupportedException("record type not supported in parseFields ["+sdpacket.getRecordType()+"]");
	}		
	
	public void initialize(){
		//_velocityParser = new Velocity_PIDParser(_registryName,ControlResponseImpl.DELIMITERS);
		//_phParser       = new PH_PIDParser(_registryName,ControlResponseImpl.DELIMITERS);
		_velocityParser = new VEL_ResponseParser(_registryName,BaseResponder.DELIMITERS);
		_phParser       = new PH_ResponseParser(_registryName,BaseResponder.DELIMITERS);
		_inputParser    = new InputStateParser(_registryName,InputState.DELIMITERS);
		_outputParser   = new OutputStateParser(_registryName,OutputState.DELIMITERS);
	}
}
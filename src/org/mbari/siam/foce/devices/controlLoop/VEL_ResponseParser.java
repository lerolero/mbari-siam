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
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.utils.DelimitedStringParser;


public class VEL_ResponseParser extends BaseResponseParser{
	
	static private Logger _log4j = Logger.getLogger(VEL_ResponseParser.class);
	
	public static final int VEL_INDEX_BASE         =MAX_BASE_INDEX;
	public static final int FWD_THRUSTER_CMD_INDEX =VEL_INDEX_BASE+1;
	public static final int AFT_THRUSTER_CMD_INDEX =VEL_INDEX_BASE+2;
	
	private static final long serialVersionUID=1L;
	
	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public VEL_ResponseParser(){
		super();
		this.initialize();
	}
	
	public VEL_ResponseParser(String registryName, String delimiters){
		super(registryName,delimiters,"vx_");
		this.initialize();
	}

	protected void initialize(){
		
		super.initialize();
		if(_log4j.isDebugEnabled()){
			_log4j.debug("VEL_ResponseParser.initialize: called super");
			_log4j.debug("VEL_ResponseParser.initialize: adding VEL field names; prefix:"+_prefix);
		}
		fieldNames.add(FWD_THRUSTER_CMD_INDEX,_prefix+"tfwdCmd");
		fieldNames.add(AFT_THRUSTER_CMD_INDEX,_prefix+"taftCmd");

		if(_log4j.isDebugEnabled()){
			_log4j.debug("VEL_ResponseParser.initialize: adding VEL field units");
		}
		fieldUnits.add(FWD_THRUSTER_CMD_INDEX,"rpm");
		fieldUnits.add(AFT_THRUSTER_CMD_INDEX,"rpm");
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug("VEL_ResponseParser.initialize: setting Base field units");
		}

		fieldUnits.set(PROCESS_VALUE_INDEX,"cm/s");
		fieldUnits.set(OFFSET_INDEX,"cm/s");
		fieldUnits.set(SETPOINT_INDEX,"cm/s");
		fieldUnits.set(ERROR_INDEX,"cm/s");
		fieldUnits.set(CORRECTION_INDEX,"cm/s");
		fieldUnits.set(RAW_CORR_INDEX,"cm/s");
		if(_log4j.isDebugEnabled()){
			_log4j.debug("VEL_ResponseParser.initialize: done");
		}
		
	}
	
	/** Process the token, whose position in string is nToken. If
	 token corresponds to a Field, create and return the field. 
	 Otherwise return null. */
    protected PacketParser.Field processToken(int nToken,String token)
	throws ParseException{
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("parsing token ["+token+"/"+nToken+"]");
		//}
		try{
			PacketParser.Field field=super.processToken(nToken,token);
			if( field != null){
				return field;
			}
		}catch (ParseException e) {
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("parse exception in base...trying Thruster_PID parser");
			//}
		}
		switch (nToken) {
			case FWD_THRUSTER_CMD_INDEX:
			case AFT_THRUSTER_CMD_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing Velocity Response packet",nToken);
		}
	}	
}
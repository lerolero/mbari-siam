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


public class PH_ResponseParser extends BaseResponseParser{
	
	static private Logger _log4j = Logger.getLogger(PH_ResponseParser.class);
	
	public static final int PH_INDEX_BASE       =MAX_BASE_INDEX;
	public static final int PUMP_CMD_INDEX      =PH_INDEX_BASE+1;
	public static final int FWD_VALVE_CMD_INDEX =PH_INDEX_BASE+2;
	public static final int AFT_VALVE_CMD_INDEX =PH_INDEX_BASE+3;

	private static final long serialVersionUID=1L;
	
	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public PH_ResponseParser(){
		super();
		this.initialize();
	}
	
	public PH_ResponseParser(String registryName, String delimiters){
		super(registryName,delimiters,"ph_");
		this.initialize();
	}

	protected void initialize(){
		super.initialize();		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("PH_ResponseParser.initialize: called super");
		_log4j.debug("PH_ResponseParser.initialize: adding PH field names; prefix:"+_prefix);
		}
		fieldNames.add(PUMP_CMD_INDEX     ,_prefix+"pumpCmd");
		fieldNames.add(FWD_VALVE_CMD_INDEX,_prefix+"vfwdCmd");
		fieldNames.add(AFT_VALVE_CMD_INDEX,_prefix+"vaftCmd");
		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("PH_ResponseParser.initialize: adding PH field units");
		}
		fieldUnits.add(PUMP_CMD_INDEX     ,"rpm");
		fieldUnits.add(FWD_VALVE_CMD_INDEX,"valve position");
		fieldUnits.add(AFT_VALVE_CMD_INDEX,"valve position");
		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("PH_ResponseParser.initialize: setting Base field units");
		}
		fieldUnits.set(PROCESS_VALUE_INDEX,"pH units");
		fieldUnits.set(OFFSET_INDEX,"pH units");
		fieldUnits.set(SETPOINT_INDEX,"pH units");
		fieldUnits.set(ERROR_INDEX,"pH units");
		fieldUnits.set(CORRECTION_INDEX,"pH units");
		fieldUnits.set(RAW_CORR_INDEX,"pH units");
		if(_log4j.isDebugEnabled()){
		_log4j.debug("PH_ResponseParser.initialize: done");
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
			//_log4j.debug("parse exception in base...trying PH_PID parser");
			//}
		}
		switch (nToken) {
			case PUMP_CMD_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			case FWD_VALVE_CMD_INDEX:
			case AFT_VALVE_CMD_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Integer(token),(String)fieldUnits.get(nToken));
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing PH Response packet",nToken);
		}
	}	
}
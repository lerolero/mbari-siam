/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.co2subsys;

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


public class CO2SubsystemMonitorParser extends DelimitedStringParser{
	
	static private Logger _log4j = Logger.getLogger(CO2SubsystemMonitorParser.class);
	
	public static final int INDEX_BASE           =0;
	public static final int VOLTAGE_24V_INDEX    =INDEX_BASE+0;
	public static final int CURRENT_24V_INDEX    =INDEX_BASE+1;
	public static final int HUMIDITY_INDEX       =INDEX_BASE+2;
	public static final int TEMPERATURE_INDEX    =INDEX_BASE+3;
	public static final int FAN_STATE_INDEX      =INDEX_BASE+4;
	public static final int WATER_SENSOR_1_INDEX =INDEX_BASE+5;
	public static final int WATER_SENSOR_2_INDEX =INDEX_BASE+6;
	public static final int FAN_CONTROL_INDEX    =INDEX_BASE+7;

	public Vector fieldNames=new Vector();
	public Vector fieldUnits=new Vector();
	
	
	private static final long serialVersionUID=1L;
	
	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public CO2SubsystemMonitorParser(){
		super();
		this.initialize();
	}
	
	public CO2SubsystemMonitorParser(String registryName, String delimiters){
		super(registryName,delimiters);
		this.initialize();
	}

	protected void initialize(){
		fieldNames.add(VOLTAGE_24V_INDEX,   "voltage_24v");
		fieldNames.add(CURRENT_24V_INDEX,   "current_24v");
		fieldNames.add(HUMIDITY_INDEX,      "humidity");
		fieldNames.add(TEMPERATURE_INDEX,   "temperature");
		fieldNames.add(FAN_STATE_INDEX,     "fan_state");
		fieldNames.add(WATER_SENSOR_1_INDEX,"water_1");
		fieldNames.add(WATER_SENSOR_2_INDEX,"water_2");
		fieldNames.add(FAN_CONTROL_INDEX,    "fan_control");

		fieldUnits.add(VOLTAGE_24V_INDEX,   "volts");
		fieldUnits.add(CURRENT_24V_INDEX,   "amperes");
		fieldUnits.add(HUMIDITY_INDEX,      "percent");
		fieldUnits.add(TEMPERATURE_INDEX,   "deg C");
		fieldUnits.add(FAN_STATE_INDEX,     "~0:on ~4096:off");
		fieldUnits.add(WATER_SENSOR_1_INDEX,"1:off 0:on");
		fieldUnits.add(WATER_SENSOR_2_INDEX,"1:off 0:on");
		fieldUnits.add(FAN_CONTROL_INDEX,   "0:on  1:off");
	}
	
	
	/** Process the token, whose position in string is nToken. If
	 token corresponds to a Field, create and return the field. 
	 Otherwise return null. */
    protected PacketParser.Field processToken(int nToken,String token)
	throws ParseException{
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("parsing token ["+token+"/"+nToken+"]");
		}
		switch (nToken) {
			case VOLTAGE_24V_INDEX:
			case CURRENT_24V_INDEX:
			case HUMIDITY_INDEX:
			case TEMPERATURE_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			case FAN_STATE_INDEX:
			case FAN_CONTROL_INDEX:
			case WATER_SENSOR_1_INDEX:
			case WATER_SENSOR_2_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Integer(token),(String)fieldUnits.get(nToken));
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing CO2SubsystemMonitor packet",nToken);
		}
	}	
}
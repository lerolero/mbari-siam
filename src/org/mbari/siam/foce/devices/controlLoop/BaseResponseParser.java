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
import org.mbari.siam.distributed.devices.ProcessParameterIF;
import org.mbari.siam.utils.DelimitedStringParser;


public class BaseResponseParser extends DelimitedStringParser{
	
	static private Logger _log4j = Logger.getLogger(BaseResponseParser.class);
	public static final int INDEX_BASE          =0;
	public static final int RECORD_TYPE_INDEX   =INDEX_BASE+0;
	public static final int PROCESS_VALUE_INDEX =INDEX_BASE+1;
	public static final int OFFSET_INDEX        =INDEX_BASE+2;
	public static final int SETPOINT_INDEX      =INDEX_BASE+3;
	public static final int ERROR_INDEX         =INDEX_BASE+4;
	public static final int CORRECTION_INDEX    =INDEX_BASE+5;
	public static final int RAW_CORR_INDEX      =INDEX_BASE+6;
	public static final int MAX_BASE_INDEX      =INDEX_BASE+6;

	public Vector fieldNames=new Vector();
	public Vector fieldUnits=new Vector();
	
	
	private static final long serialVersionUID=1L;
	/** field prefix */
	protected String _prefix=null;
	
	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public BaseResponseParser(){
		super();
		//this.initialize();
	}
	
	public BaseResponseParser(String registryName, String delimiters){
		this(registryName,delimiters,null);
	}
	
	public BaseResponseParser(String registryName, String delimiters, String prefix){
		super(registryName,delimiters);
		_prefix=prefix;
		//this.initialize();
	}

	protected void initialize(){
		_log4j.debug("BaseResponseParser.initialize: clearing");
		fieldNames.clear();
		fieldUnits.clear();
		_log4j.debug("BaseResponseParser.initialize: adding response base field names; prefix:"+_prefix);
		_log4j.debug("BaseResponseParser.initialize: fieldNames.size:"+fieldNames.size());
		fieldNames.add(RECORD_TYPE_INDEX,_prefix+"record_type");
		fieldNames.add(PROCESS_VALUE_INDEX,_prefix+"process");
		fieldNames.add(OFFSET_INDEX,_prefix+"offset");
		fieldNames.add(SETPOINT_INDEX,_prefix+"setpoint");
		fieldNames.add(ERROR_INDEX,_prefix+"err");
		fieldNames.add(CORRECTION_INDEX,_prefix+"corr");
		fieldNames.add(RAW_CORR_INDEX,_prefix+"rawCorr");
		
		_log4j.debug("BaseResponseParser.initialize: adding response base field units");
		fieldUnits.add(RECORD_TYPE_INDEX," ");
		fieldUnits.add(PROCESS_VALUE_INDEX," ");
		fieldUnits.add(OFFSET_INDEX," ");
		fieldUnits.add(SETPOINT_INDEX," ");
		fieldUnits.add(ERROR_INDEX," ");
		fieldUnits.add(CORRECTION_INDEX," ");
		fieldUnits.add(RAW_CORR_INDEX," ");
		_log4j.debug("BaseResponseParser.initialize: fieldNames.size:"+fieldNames.size());
		_log4j.debug("BaseResponseParser.initialize: done");
	}
	
	
	/** Process the token, whose position in string is nToken. If
	 token corresponds to a Field, create and return the field. 
	 Otherwise return null. */
    protected PacketParser.Field processToken(int nToken,String token)
	throws ParseException{
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("parsing token ["+token+"/"+nToken+"]");
		//}
		switch (nToken) {
			case RECORD_TYPE_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new String(token),(String)fieldUnits.get(nToken));
			case ERROR_INDEX:
			case OFFSET_INDEX:
			case SETPOINT_INDEX:
			case PROCESS_VALUE_INDEX:
			case CORRECTION_INDEX:
			case RAW_CORR_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing base Response packet field",nToken);
		}
	}	
}
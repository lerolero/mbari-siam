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

public class InputStateParser extends DelimitedStringParser{
	
	static private Logger _log4j = Logger.getLogger(InputStateParser.class);
	
	public static final int BASE_INDEX     =0;
	public static final int HEADER_INDEX       =BASE_INDEX+0;
	public static final int NAME_INDEX         =BASE_INDEX+1;
	public static final int STATE_INDEX        =BASE_INDEX+2;
	public static final int STATUS_INDEX       =BASE_INDEX+3;
	public static final int UTMO_INDEX         =BASE_INDEX+4;
	public static final int TSLU_INDEX         =BASE_INDEX+5;
	public static final int TOEX_INDEX         =BASE_INDEX+6;
	public static final int SAMPLE_COUNT_INDEX =BASE_INDEX+7;
	public static final int VALID_RATIO_INDEX  =BASE_INDEX+8;

	//public static final int MAX_PREFIX_LENGTH  =100;
	
	protected Vector fieldNames=new Vector();
	protected Vector fieldUnits=new Vector();
	protected String _namingPrefix=null;

	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public InputStateParser(){
		super();
		this.initialize();
	}
	
	public InputStateParser(String registryName, String delimiters){
		super(registryName,delimiters);
		this.initialize();
	}
	/** setNamimgPrefix allows callers to set a naming prefix 
		to prevent naming collisions in DataTurbine.
		Since all InputState packets parse the same way,
		different inputs can set the prefix equal to the
		input name+"_", giving each data field a unique
		(and easily identifiable) OSDT channel name.
		This method is invoked by the main 
		control loop packet parser
	 */
	public void setNamingPrefix(String prefix)
	throws Exception{
		
		if(prefix==null){
			_namingPrefix="";
			return;
		}
		// validate here...(may not contain...? max length?)
		for(int i=0;i<prefix.length();i++){
			char c=prefix.charAt(i);
			if(Character.isLetterOrDigit(c) ||
			   c=='-' ||
			   c=='_' ||
			   c=='.' ||
			   c==' ' ||
			   c=='\\' ||
			   c=='/')
			{
				continue;
			}else{
				throw new Exception("prefix ["+prefix+"] contains invalid character ["+c+"]");			
			}
		}
		/*
		if(prefix.length()>MAX_PREFIX_LENGTH){
			throw new Exception("prefix ["+prefix+"] exceeds max length ["+MAX_PREFIX_LENGTH+"]");
		}
		*/
			
		// anything goes...
		_namingPrefix=prefix;
	}
	
	protected void initialize(){
		fieldNames.add(HEADER_INDEX       ,"header");
		fieldNames.add(NAME_INDEX         ,"input name");
		fieldNames.add(STATE_INDEX        ,"state");
		fieldNames.add(STATUS_INDEX       ,"status");
		fieldNames.add(UTMO_INDEX         ,"update timeout");
		fieldNames.add(TSLU_INDEX         ,"time since last update");
		fieldNames.add(TOEX_INDEX         ,"timeout expired");
		fieldNames.add(SAMPLE_COUNT_INDEX ,"sample count");
		fieldNames.add(VALID_RATIO_INDEX  ,"valid ratio");
		
		fieldUnits.add(HEADER_INDEX       ,"string");
		fieldUnits.add(NAME_INDEX         ,"string");
		fieldUnits.add(STATE_INDEX        ,"string");
		fieldUnits.add(STATUS_INDEX       ,"string");
		fieldUnits.add(UTMO_INDEX         ,"msec");
		fieldUnits.add(TSLU_INDEX         ,"msec");
		fieldUnits.add(TOEX_INDEX         ,"boolean");
		fieldUnits.add(SAMPLE_COUNT_INDEX ,"long");
		fieldUnits.add(VALID_RATIO_INDEX  ,"double");
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
			case HEADER_INDEX:
			case NAME_INDEX:
			case STATE_INDEX:
			case STATUS_INDEX:
				return new PacketParser.Field(_namingPrefix+(String)fieldNames.get(nToken),new String(token),(String)fieldUnits.get(nToken));
				
			case UTMO_INDEX:
			case TSLU_INDEX:
			case SAMPLE_COUNT_INDEX:
				return new PacketParser.Field(_namingPrefix+(String)fieldNames.get(nToken),new Long(token),(String)fieldUnits.get(nToken));
				
			case TOEX_INDEX:
				return new PacketParser.Field(_namingPrefix+(String)fieldNames.get(nToken),new Boolean(token),(String)fieldUnits.get(nToken));
				
			case VALID_RATIO_INDEX:
				return new PacketParser.Field(_namingPrefix+(String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing InputState packet",nToken);
		}
	}	
}
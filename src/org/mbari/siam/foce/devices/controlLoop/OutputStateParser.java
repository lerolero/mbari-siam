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


public class OutputStateParser extends DelimitedStringParser{
	
	static private Logger _log4j = Logger.getLogger(OutputStateParser.class);
	
	public static final int BASE_INDEX     =0;
	public static final int HEADER_INDEX       =BASE_INDEX+0;
	public static final int NAME_INDEX         =BASE_INDEX+1;
	public static final int STATE_INDEX        =BASE_INDEX+2;
	public static final int STATUS_INDEX       =BASE_INDEX+3;
	
	//public static final int MAX_PREFIX_LENGTH  =100;
	
	protected Vector fieldNames=new Vector();
	protected Vector fieldUnits=new Vector();
	protected String _namingPrefix=null;
	
	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public OutputStateParser(){
		super();
		this.initialize();
	}
	
	public OutputStateParser(String registryName, String delimiters){
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
		fieldNames.add(NAME_INDEX         ,"output name");//was input
		fieldNames.add(STATE_INDEX        ,"state");
		fieldNames.add(STATUS_INDEX       ,"status");
		
		fieldUnits.add(HEADER_INDEX       ,"string");
		fieldUnits.add(NAME_INDEX         ,"string");
		fieldUnits.add(STATE_INDEX        ,"string");
		fieldUnits.add(STATUS_INDEX       ,"string");
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
				
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing OutputState packet",nToken);
		}
	}	
}
/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.dummy;

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

public class SineDummyParser extends DelimitedStringParser{
	
	static private Logger _log4j = Logger.getLogger(SineDummyParser.class);
	
	public static final int BASE_INDEX     =0;
	public static final int SIN0_INDEX     =BASE_INDEX+0;
	public static final int SIN1_INDEX     =BASE_INDEX+1;
	public static final int SIN2_INDEX     =BASE_INDEX+2;
	public static final int SIN3_INDEX     =BASE_INDEX+3;
	public static final int SIN4_INDEX     =BASE_INDEX+4;
	public static final int SIN5_INDEX     =BASE_INDEX+5;
	public static final int SIN6_INDEX     =BASE_INDEX+6;
	public static final int SIN7_INDEX     =BASE_INDEX+7;
	public static final int SIN8_INDEX     =BASE_INDEX+8;
	public static final int SIN9_INDEX     =BASE_INDEX+9;

	//public static final int MAX_PREFIX_LENGTH  =100;
	
	protected Vector fieldNames=new Vector();
	protected Vector fieldUnits=new Vector();
	protected String _namingPrefix=null;

	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public SineDummyParser(){
		super();
		this.initialize();
	}
	
	public SineDummyParser(String registryName, String delimiters){
		super(registryName,delimiters);
		this.initialize();
	}
	
	/** setNamimgPrefix allows callers to set a naming prefix 
		to prevent naming collisions in DataTurbine.
		Sinece all InputState packets parse the same way,
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
		fieldNames.add(SIN0_INDEX  ,"sin0");
		fieldNames.add(SIN1_INDEX  ,"sin1");
		fieldNames.add(SIN2_INDEX  ,"sin2");
		fieldNames.add(SIN3_INDEX  ,"sin3");
		fieldNames.add(SIN4_INDEX  ,"sin4");
		fieldNames.add(SIN5_INDEX  ,"sin5");
		fieldNames.add(SIN6_INDEX  ,"sin6");
		fieldNames.add(SIN7_INDEX  ,"sin7");
		fieldNames.add(SIN8_INDEX  ,"sin8");
		fieldNames.add(SIN9_INDEX  ,"sin9");
		
		fieldUnits.add(SIN0_INDEX  ,"double");
		fieldUnits.add(SIN1_INDEX  ,"double");
		fieldUnits.add(SIN2_INDEX  ,"double");
		fieldUnits.add(SIN3_INDEX  ,"double");
		fieldUnits.add(SIN4_INDEX  ,"double");
		fieldUnits.add(SIN5_INDEX  ,"double");
		fieldUnits.add(SIN6_INDEX  ,"double");
		fieldUnits.add(SIN7_INDEX  ,"double");
		fieldUnits.add(SIN8_INDEX  ,"double");
		fieldUnits.add(SIN9_INDEX  ,"double");
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
			case SIN0_INDEX:
			case SIN1_INDEX:
			case SIN2_INDEX:
			case SIN3_INDEX:
			case SIN4_INDEX:
			case SIN5_INDEX:
			case SIN6_INDEX:
			case SIN7_INDEX:
			case SIN8_INDEX:
			case SIN9_INDEX:
				return new PacketParser.Field(_namingPrefix+(String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing SineDummy packet",nToken);
		}
	}	
}
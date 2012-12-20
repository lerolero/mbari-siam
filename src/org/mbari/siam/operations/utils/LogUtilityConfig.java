/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.File;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.utils.TimeUtils;
import org.mbari.siam.operations.portal.PacketStats;
import org.mbari.siam.core.DeviceLogIndex;

import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.MeasurementPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
 <p>
 Encapsulates configuration options for a LogUtility.
 </p>
 <p>
 The options affect packet filtering, utility actions, and output formatting.<br>
 Options include:<br>
 </p>
 
 <code>
 # Device log path<br>
 deviceID     Device ID<br>
 logDirectory Path to log file directory<br>
<br> 
 # Packet filtering<br>
 startTime    Time window start<br>
 stopTime     Time window end<br>
 typeFlags    Packet type flags (all|data|metadata|message|summary)<br>
 skipInterval Process every skipInterval-th packet<br>
<br> 
 # Output formatting<br>
 parserName   Name of PacketParser class<br>
 formatFlags  Formatting flags (per packet type data formats)<br>
 sdformat       Format specifier string (sensor data)<br>
 mdformat       Format specifier string (metadata)<br>
 dmformat       Format specifier string (device message)<br>
 spformat       Format specifier string (summary packet)<br>
 <br>
 # Utility actions<br>
 doCheck      Action flag: do index integrity check<br>
 doRepair     Action flag: repair index file if errors are found<br>
 doCalcAge    Action flag: return age of most recent record in the log file<br>
 </code>
 <p>
 In addition, there are methods that create instances of DeviceLog,
 PacketFilter, and PacketParser, based on configuration options.
 </p>
 @see org.mbari.siam.operations.utils.LogUtility
 */
public class LogUtilityConfig {
	
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(LogUtilityConfig.class);
	
	// Data buffer format flags
	/** format flag mnemonic: (show all data in ASCII format) */
	public static final int ASCII_ALL     =0x0001;
	/** format flag mnemonic: (show all data in raw binary format) */
	public static final int BIN_ALL       =0x0002;
	/** format flag mnemonic: (show all data in ASCII hex format) */
	public static final int HEX_ALL       =0x0004;
	/** format flag mnemonic: (show sensor data in ASCII format) */
	public static final int ASCII_DATA    =0x0008;
	/** format flag mnemonic: (show sensor data in raw binary format) */
	public static final int BIN_DATA      =0x0010;
	/** format flag mnemonic: (show sensor data in ASCII hex format) */
	public static final int HEX_DATA      =0x0020;
	/** format flag mnemonic: (show summary packets in raw binary format) */
	public static final int ASCII_MESSAGE =0x0040;
	/** format flag mnemonic: (show device message packets in raw binary format) */
	public static final int BIN_MESSAGE   =0x0080;
	/** format flag mnemonic: (show device message packets in ASCII hex format) */
	public static final int HEX_MESSAGE   =0x0100;
	/** format flag mnemonic: (show metadata packets in ASCII format) */
	public static final int ASCII_METADATA=0x0200;
	/** format flag mnemonic: (show metadata packets in raw binary format) */
	public static final int BIN_METADATA  =0x0400;
	/** format flag mnemonic: (show metadata packets in ASCII hex format) */
	public static final int HEX_METADATA  =0x0800;
	/** format flag mnemonic: (show summary packets in ASCII format) */
	public static final int ASCII_SUMMARY =0x1000;
	/** format flag mnemonic: (show summary packets in raw binary format) */
	public static final int BIN_SUMMARY   =0x2000;
	/** format flag mnemonic: (show summary packets in ASCII hex format) */
	public static final int HEX_SUMMARY   =0x4000;
	
	/** Device ID of input log */
	long _deviceID=-1L;
	
	/** Directory path to input log */
	String _logDirectory=null;

	/** PacketParser class name */
	String _parserName = null;
	
	/** Packet output format specifier string (sensor data)
	 @see LogUtility
	 */
	String _sdformat=null;
	/** Packet output format specifier string (metadata)
	 @see LogUtility
	 */
	String _mdformat=null;
	/** Packet output format specifier string (summary packet)
	 @see LogUtility
	 */
	String _spformat=null;
	/** Packet output format specifier string (device message)
	 @see LogUtility
	 */
	String _dmformat=null;
	
	/** Log age calculation flag */
	boolean _doCalcAge=false;
	/** Log integrity check flag */
	boolean _doCheck=false;
	/** Log index repair check flag */
	boolean _doRepair=false;
	/** Data buffer formatting flags (logical OR) */
	int _formatFlags=ASCII_ALL;
	/** Verbose output flag */
	boolean _verbose=false;
	/** Log filter time window start  */
	long _startTime = Long.MIN_VALUE;
	/** Log filter time window end  */
	long _stopTime = Long.MAX_VALUE;
	/** Log filter packet skip modulus (process every nth packet) */
	int _skipInterval=0;
	/** Log filter packet type flags (logical OR) */
	int _typeFlags=DevicePacket.ALL_TYPES;
	
	/** StringBuffer instance (for building output strings) */
	StringBuffer _stringBuffer=null;
	
	/** No-arg constructor */
	public LogUtilityConfig(){}
	
	/** Initializing constructor
	 @param deviceID     Device ID
	 @param logDirectory Path to log file directory
	 @param sdformat       Format specifier string
	 @param mdformat       Format specifier string
	 @param dmformat       Format specifier string
	 @param spformat       Format specifier string
	 @param parserName   Name of PacketParser class
	 @param formatFlags  Formatting flags (BIN|HEX|SDB|SDH|DMB|DMH|SPB|SPH|MPB|MPH)
	 @param startTime    Time window start (MM/DD/YYYYTHH:MM[:SS])
	 @param stopTime     Time window end (MM/DD/YYYYTHH:MM[:SS])
	 @param typeFlags    Packet type flags (all|data|metadata|message|summary)
	 @param skipInterval Process every skipInterval-th packet
	 @param doCheck      Action flag: do index integrity check
	 @param doRepair     Action flag: repair index file if errors are found
	 @param doCalcAge    Action flag: return age of most recent record in the log file
	 */
	public LogUtilityConfig(long deviceID,
							String logDirectory, 
							String sdformat,
							String mdformat,
							String dmformat,
							String spformat,
							String parserName,
							int formatFlags,
							long startTime,
							long stopTime,
							int typeFlags,
							int skipInterval,
							boolean doCheck,
							boolean doRepair,
							boolean doCalcAge){
		// set the fields
		setDeviceID(deviceID);
		setLogDirectory(logDirectory);
		setParserName(parserName);
		setSDFormat(sdformat);
		setMDFormat(mdformat);
		setDMFormat(dmformat);
		setSPFormat(spformat);
		setFormatFlags(formatFlags);
		
		setStartTime(startTime);
		setStopTime(stopTime);
		setTypeFlags(typeFlags);
		setSkipInterval(skipInterval);
		
		setDoCheck(doCheck);
		setDoRepair(doRepair);
		setDoCalcAge(doCalcAge);
	}
	
	// Accessor methods 
	
	/** Set deviceID  */
	public void setDeviceID(long deviceID){_deviceID=deviceID;}
	/** Set log directory  */
	public void setLogDirectory(String logDirectory){_logDirectory=logDirectory;}
	/** Set format string (sensor data) */
	public void setSDFormat(String format){_sdformat=format;}
	/** Set format string (metadata)  */
	public void setMDFormat(String format){_mdformat=format;}
	/** Set format string (device message)  */
	public void setDMFormat(String format){_dmformat=format;}
	/** Set format string (summary packet)  */
	public void setSPFormat(String format){_spformat=format;}
	/** Set format flags  */
	public void setFormatFlags(int formatFlags){_formatFlags=formatFlags;}
	/** Set parser class name  */
	public void setParserName(String parserName){_parserName=parserName;}
	/** Set start time  */
	public void setStartTime(long startTime){_startTime=startTime;}
	/** Set stop time  */
	public void setStopTime(long stopTime){_stopTime=stopTime;}
	/** Set skip interval  */
	public void setSkipInterval(int skipInterval){_skipInterval=skipInterval;}
	/** Set type flags */
	public void setTypeFlags(int typeFlags){_typeFlags=typeFlags;}
	/** Set verbose output flag */
	public void setVerbose(boolean verbose){_verbose=verbose;}
	/** Set log integrity check flag */
	public void setDoCheck(boolean doCheck){_doCheck=doCheck;}
	/** Set log index repair flag */
	public void setDoRepair(boolean doRepair){_doRepair=doRepair;}
	/** Set log age calculation flag */
	public void setDoCalcAge(boolean doCalcAge){_doCalcAge=doCalcAge;}
	
	/** Get deviceID  */
	public long getDeviceID(){return _deviceID;}
	/** Get log directory  */
	public String getLogDirectory(){return _logDirectory;}
	/** Get parser class name  */
	public String getParserName(){return _parserName;}
	/** Get format string (sensor data) */
	public String getSDFormat(){return _sdformat;}
	/** Get format string (metadata)  */
	public String getMDFormat(){return _mdformat;}
	/** Get format string (device message)  */
	public String getDMFormat(){return _dmformat;}
	/** Get format string (summary packet)  */
	public String getSPFormat(){return _spformat;}
	/** Get format flags  */
	public int getFormatFlags(){return _formatFlags;}
	/** Get verbose output flag */
	public boolean getVerbose(){return _verbose;}
	
	/** Get start time  */
	public long getStartTime(){return _startTime;}
	/** Get stop time  */
	public long getStopTime(){return _stopTime;}
	/** Get type flags */
	public int getTypeFlags(){return _typeFlags;}
	/** Get skip interval  */
	public int getSkipInterval(){return _skipInterval;}
	
	/** Get log integrity check flag */
	public boolean getDoCheck(){return _doCheck;}
	/** Get log index repair flag */
	public boolean getDoRepair(){return _doRepair;}
	/** Get log age calculation flag */
	public boolean getDoCalcAge(){return _doCalcAge;}
	
	/** Get PacketParser instance using parserName member variable.
	 
		@return PacketParser instance (or null if parserName is null)

		@see org.mbari.siam.distributed.PacketParser
	 */
	public PacketParser getPacketParser() 
	throws Exception{
		
		if (_parserName != null) {
			// get packet parser class
			Class c = 
			ClassLoader.getSystemClassLoader().loadClass(_parserName);
			// make and return a packet parser instance
			return (PacketParser) c.newInstance();
		}
		// null if parser name not set
		return null;
	}
	
	/** Get new DeviceLog instance using logDirectory and deviceID member variables
	 @return new DeviceLog instance for the deviceID in logDirectory associated with
	 this instance.
	 */
	public DeviceLog getDeviceLog() 
	throws Exception{
		return new DeviceLog(_deviceID, _logDirectory);
	}

	/** Return an array of PacketFilters. The PacketFilters are 
	 PacketSubsamplers that filter on packet type and skip interval,
	 and reject unselected packet types.

	 @return Array of PacketFilters used to constrain the processed packet set
	 
	 @see org.mbari.siam.distributed.PacketFilter
	 
	 */
	public PacketFilter[] getPacketFilters(){
		try{
			
			PacketFilter[] filters = new PacketFilter[2];
			
			// Allow specified packet type(s)
			filters[0] =
			new PacketSubsampler(_skipInterval, _typeFlags);
			
			// Exclude all other types
			filters[1] = 
			new PacketSubsampler(-1, ~(DevicePacket.ALL_TYPES & _typeFlags));
			return filters;
		}catch(Exception e){
			System.err.println(e);
		}
		return null;
	}
	
	/** Return the mnemonic(s) for the specified packet type flag. 
		Returns a list of packet type names, separated by a 
		pipe symbol "|" if more than one type is selected.
		
	 @param typeFlags packet type bit field
	 @return Mnemonic string indicating selected types
	 
	 @see org.mbari.siam.distributed.DevicePacket

	 */
	public String typeFlagName(int typeFlags){
		String retval=null;

		// ALL types
		if ( (typeFlags==DevicePacket.ALL_TYPES) ) {
			retval= LogUtility.FLAGNAME_ALL;
			return retval;
		}

		// METADATA
		if ( (typeFlags&DevicePacket.METADATA_FLAG)>0) {
			if (retval==null) {
				retval=LogUtility.FLAGNAME_METADATA;
			}else{
				retval="|"+LogUtility.FLAGNAME_METADATA;
			}
		}

		// SENSOR_DATA
		if ( (typeFlags&DevicePacket.SENSORDATA_FLAG)>0) {
			if (retval==null) {
				retval=LogUtility.FLAGNAME_SENSORDATA;
			}else{
				retval+="|"+LogUtility.FLAGNAME_SENSORDATA;
			}
		}

		// DEVICE_MESSAGE
		if ( (typeFlags&DevicePacket.DEVICEMESSAGE_FLAG)>0) {
			if (retval==null) {
				retval=LogUtility.FLAGNAME_MESSAGE;
			}else{
				retval+="|"+LogUtility.FLAGNAME_MESSAGE;
			}
		}
		// SUMMARY
		if ( (typeFlags&DevicePacket.SUMMARY_FLAG)>0) {
			if (retval==null) {
				retval=LogUtility.FLAGNAME_SUMMARY;
			}else{
				retval+="|"+LogUtility.FLAGNAME_SUMMARY;
			}
		}
		
		return retval;
	}
	
	/** Return String representation of this LogUtilityConfig instance 
	 
	 @return This object as a String
	 */
	public String toString(){
		// lazy create string buffer
		if (_stringBuffer==null) {
			_stringBuffer=new StringBuffer();
		}
		// set delimiter for reuse
		String del="\n";
		// empty string buffer
		_stringBuffer.setLength(0);

		// write values to buffer
		_stringBuffer.append("deviceID:"+_deviceID+del);
		_stringBuffer.append("logDirectory:"+_logDirectory+del);
		_stringBuffer.append("parser name:"+_parserName+del);
		
		_stringBuffer.append("start:"+_startTime+del);
		_stringBuffer.append("stop:"+_stopTime+del);
		_stringBuffer.append("type flags:"+typeFlagName(_typeFlags)+del);
		_stringBuffer.append("skip interval:"+_skipInterval+del);
		_stringBuffer.append("verbose:"+_verbose+del);
		
		_stringBuffer.append("doCalcAge:"+_doCalcAge+del);
		_stringBuffer.append("doCheck:"+_doCheck+del);
		_stringBuffer.append("doRepair:"+_doRepair+del);
		_stringBuffer.append("formatFlags:0x"+Integer.toHexString(_formatFlags)+del);
		_stringBuffer.append("sdformat:["+_sdformat+"]"+del);
		_stringBuffer.append("mdformat:["+_mdformat+"]"+del);
		_stringBuffer.append("dmformat:["+_dmformat+"]"+del);
		_stringBuffer.append("spformat:["+_spformat+"]"+del);
		
		// return String
		return _stringBuffer.toString();
	}
}
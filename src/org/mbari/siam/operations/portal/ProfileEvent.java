/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.util.Date;
import java.util.Hashtable;
import java.io.File;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;

/*
==== event profiling ====
eventName:startMsg:endMsg
connected:info."got connection":info."Link terminated"
reset WDT:start."reset WDT":end."reset WDT"
node info:start."getID":start."getDeviceIDs"
retrieveAndDistributeData:start."retrieveAndDistributeData":end."retrieveAndDistributeData"
retrieve data:start."retrievePackets":end."retrievePackets"
get packets:start."getDevicePackets":end."getDevicePackets"
process packets:end."getDevicePackets":end."retrievePackets"
				
event start/stop->event duration

==== transfer stats ====
start/end.getDevicePacket,info.totalPackets/packets,info.stats(deviceID,packetSize)
->transferSize,effectiveBitRate,%Nominal

*/

public class ProfileEvent{

    String _name=null;
    String _prefix="profile";
    String _delimiter=";";
    String _message=null;
    String _fileName=null;
    File _inFile=null;
    long _startTime=-1;
    long _endTime=-1;

    public static Integer END=new Integer(0);
    public static Integer START=new Integer(1);
    public static Integer UNDEFINED=new Integer(-1);
    public static Integer INFO=new Integer(2);
    public static String _start="start";
    public static String _end="end";
    public static String _info="info";

    public ProfileEvent(String file){
	 _inFile=new File(file);

	if(!_inFile.exists())
	    exitError("File not found",1);
    }
    
    public String  toString(){
	return null;
    }

    public void setDelimiter(String delimiter){
	_delimiter=delimiter;
    }
    public String getDelimiter(){
	return _delimiter;
    }

    public void setName(String name){
	_name=name;
    }
    public String getName(){
	return _name;
    }
    
    public void setPrefix(String prefix){
	_prefix=prefix;
    }
    public String getPrefix(){
	return _prefix;
    }
    
    public void setMessage(String message){
	_message=message;
    }
    public String getMessage(){
	return _message;
    }
    
    public void analyze(){
	_inFile=new File(_fileName);

    }

    public static void printUsage(){
	System.out.println("");
    }

    public static void exitError(String msg, int exitCode){
	System.out.println(msg);
	printUsage();
	System.exit(exitCode);
    }

    public static void main(String args[]) {
	if(args.length<1){
	    exitError("missing file name",1);
	}

	ProfileEvent pe=new ProfileEvent(args[0]);

    }

}

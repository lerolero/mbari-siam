/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.Serializable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Properties;
import java.text.ParseException;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.Exportable;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.distributed.RangeException;
import moos.ssds.jms.PublisherComponent;
import org.mbari.siam.utils.TimeUtils;
import org.mbari.siam.operations.utils.ExportablePacket;

/**
   Publish DevicePackets from log to SSDS.
   @author Tom O'Reilly, Kent Headley
 */
public class LogPublisher {

    static protected Logger _log4j = Logger.getLogger(LogPublisher.class);
    PublisherComponent _ssdsPublisher = null;
    ExportablePacket exportablePacket = new ExportablePacket();
    Properties _properties=new Properties();
    boolean _sticky=false;
    boolean _wrapPackets=true;
    boolean _test=false;
    boolean _print=false;
    String _cfgFile="logPostpublish.last";
    long _deviceID = 0L;
    long _startTime = 0L;
    long _stopTime = Long.MAX_VALUE;
    String _directory;


    public LogPublisher() {
	_ssdsPublisher = new PublisherComponent();
    }


    public void publish()
    throws FileNotFoundException, IOException, RangeException {

	DeviceLog log = new DeviceLog(_deviceID, _directory);
	_log4j.info("Log contains " + log.nPackets() + " packets");
	DeviceLogIterator iterator = new DeviceLogIterator(log,_startTime);

	int nPackets = 0;

	long sequenceStart = 0L;
	long lastSequenceNumber = 0L;
	if (_sticky) {
	    sequenceStart = getLastSequenceNumber();
	    lastSequenceNumber = sequenceStart;
	}

	_log4j.debug("lastSequenceNumber="+lastSequenceNumber);

	while (iterator.hasNext()) {

	    DevicePacket packet = (DevicePacket )iterator.next();
	    long timestamp = packet.systemTime();
	    if (timestamp < _startTime || timestamp > _stopTime) {
		// Outside of time window
		// do we want to break if > _stopTime or continue
		// on the chance that there are more packets with
		// earlier timestamps?
		_log4j.debug("packet outside of time window");
		continue;
	    }

	    try {
		ByteArrayOutputStream byteArrayOutput = 
		    new ByteArrayOutputStream();

		DataOutputStream dataOutput = 
		    new DataOutputStream(byteArrayOutput);

		long currentSequenceNumber = packet.sequenceNo();

		if( _sticky==false || (_sticky==true && currentSequenceNumber >= sequenceStart)){

		    // Publish this packet
		    _log4j.debug("export");
		    if(_wrapPackets){
			exportablePacket.wrapPacket(packet);
			exportablePacket.export(dataOutput);
		    }else{
			Exportable ep=(Exportable)packet;
			ep.export(dataOutput);
		    }
		    dataOutput.flush();
		    byte[] exportedBytes = byteArrayOutput.toByteArray();
		    dataOutput.close();
		    _log4j.debug("export done");
		    if(_test==false){
			// Publish this packet
			_log4j.info("Publishing packet " + nPackets);
			// _log4j.debug(new String(exportedBytes));
			_ssdsPublisher.publishBytes(exportedBytes);
			_log4j.debug("Published " + nPackets);
		    }else{
			_log4j.info("Not publishing packet " + nPackets );
			if(_print==true)
			    _log4j.info("\n"+new Date(packet.systemTime())+"\n"+packet);
		    }
		    nPackets++;
		    lastSequenceNumber=currentSequenceNumber;
		}
	    }
	    catch (Exception e) {
		_log4j.error(e);
	    }
	}
	if(_sticky==true)
	    try{
		FileOutputStream fos = new FileOutputStream(_cfgFile,false);
		_properties.setProperty("lastSequenceNumber",Long.toString(lastSequenceNumber));
		_properties.store(fos,"Last Sequence Number");
	    }catch(Exception e){
		_log4j.error(e);
	    }

	_log4j.info("Published " + nPackets + " packets");
    }

    protected long getLastSequenceNumber(){
	String logDir = System.getProperty("LogPostpublishDir",".");
	_cfgFile = logDir+"/logPostpublish."+_deviceID+".last";

	_log4j.debug("cfgFile="+_cfgFile);
	long lastSequenceNumber=0L;
	try{
	    _log4j.debug("opening properties file input stream");
	    FileInputStream in = 
		new FileInputStream(_cfgFile);

	    _log4j.debug("creating properties");
	    _properties=new Properties();

	    _log4j.debug("loading properties");
	    
	    // Load Node properties from the file
	    _properties.load(in);

	    _log4j.debug("closing properties");
	    in.close();
	    String p=_properties.getProperty("lastSequenceNumber","0");
	_log4j.debug("read p= "+p);
	    lastSequenceNumber=Long.valueOf(p).longValue();
	}catch(Exception e) {
		_log4j.error("getLastSequenceNumber failed: "+e);
	}
	_log4j.debug("read lastSequenceNumber: "+lastSequenceNumber);
	return lastSequenceNumber;
    }

    public void printUsageAndExit(){
	System.err.println("");
	System.err.println("usage: LogPublisher <options> deviceId directory");
	System.err.println("");
	System.err.println("Options:");
	System.err.println(" -h     print this message");
	System.err.println(" -s     sticky, i.e., record last sequence number in file (1).");
	System.err.println("        On subsequent sticky calls, publish sequence numbers");
	System.err.println("        greater than lastSequenceNumber                        [not sticky]");
	System.err.println(" -n     no wrap i.e., do not use Exportable wrapper            [wrap]");
	System.err.println(" -t     test, i.e., do everything but publish                  [false]");
	System.err.println(" -p     print, i.e., print packets (w/ -test only)             [false]");
	System.err.println(" -start <time> publish packets with timestamp after  <time>(2) [all]");
	System.err.println(" -stop  <time> publish packets with timestamp before <time>(2) [all]");
	System.err.println("");
	System.err.println("Notes:");
	System.err.println("  (1) sequence number file: $LogPostpublishDir/logPostpublish.<isiDeviceID>.last");
	System.err.println("  (2) <time> format: 'mm/dd/yyyy[Thh:mm[:ss]]'");
	System.err.println("");

	System.exit(0);
    }

    public static void main(String[] args) {

	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	String directory = "./";
	LogPublisher publisher=null;
	try {
	    publisher = new LogPublisher();
	}
	catch (Exception e) {
	    _log4j.error(e);
	    return;
	}

	boolean error = false;
	if (args.length < 2) {
	    System.err.println("Missing required arguments deviceID and logDirectory");
	    publisher.printUsageAndExit();	
	}

	/* Parse options; note that last two arguments MUST specify device ID
	   and log directory, respectively. */
	for (int i = 0; i < args.length-2; i++) {
	    if (args[i].equals("-h")) {
		// Print help message and exit
		publisher.printUsageAndExit();
		return;
	    }
	    else if (args[i].equals("-s")) {
		// Sticky option
		publisher._sticky = true;
	    }
	    else if (args[i].equals("-n")) {
		// Don't 'wrap' packets in Exportable interface
		publisher._wrapPackets = false;
	    }else if (args[i].equals("-t")) {
		// set test flag; don't actually publish packets
		publisher._test = true;
	    }else if (args[i].equals("-p")) {
		// set print flag; print packets (test only)
		publisher._print=true;
	    }
	    else if (args[i].equals("-start") && i < args.length-3) {
		// Parse lower end of time window
		try {
		    publisher._startTime = TimeUtils.parseDateTime(args[++i]);
		} catch (ParseException e) {
		    System.err.println("Invalid timestring: " + args[i]);
		    error = true;
		}
	    }
	    else if (args[i].equals("-stop") && i < args.length-3) {
		// Parse upper end of time window
		try {
		    publisher._stopTime = TimeUtils.parseDateTime(args[++i]);
		} catch (ParseException e) {
		    System.err.println("Invalid timestring: " + args[i]);
		    error = true;
		}

	    }
	    else {
		System.err.println(args[i] + ": unknown option");
		error = true;
	    }
	}



	try {
	    publisher._deviceID = Long.parseLong(args[args.length-2]);
	}
	catch (NumberFormatException e) {
	    _log4j.error("Invalid device ID:"+args[args.length-2]);
	    error = true;
	}

	publisher._directory = args[args.length-1];


	if (publisher._startTime > publisher._stopTime) {
	    error = true;
	    System.err.println("start time must precede stop time");
	}


	if (error) {
	    publisher.printUsageAndExit();
	}

	try{
	    publisher.publish();
	}
	catch (Exception e) {
	    System.err.println(e);
	}
    }
}

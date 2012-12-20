/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.utils.TimeUtils;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.core.DeviceLog;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
 Simple utility to extract JPEG images from SIAM SensorDataPackets  
 */
public class CameraUtil {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(CameraUtil.class);

    /**
       Save images from SIAM log file
     */

    public static void saveImages(DeviceLog log, long startTime) 
	throws Exception {

	SimpleDateFormat dateFormatter = 
	    new SimpleDateFormat("MMddyyyyHHmmss");

	// Fixed file type for now...
	String fileType = "jpg";

	DeviceLogIterator iterator = new DeviceLogIterator(log, startTime);

	while (iterator.hasNext()) {

	    DevicePacket packet = (DevicePacket) iterator.next();

	    if (packet instanceof SensorDataPacket) {

		// Extract image from packet and save
		byte[] imageBytes = ((SensorDataPacket )packet).dataBuffer();

		_log4j.info("got " + imageBytes.length + " bytes");

		// Build output file name from timetag
		String fileName = 
		    dateFormatter.format(new Date(packet.systemTime())) + 
		    "." + fileType;

		// Create output file
		FileOutputStream output = 
		    new FileOutputStream(new File(fileName));

		// Write image to file
		output.write(imageBytes);

		// Close file
		output.close();
	    }
	}

    }


    public static void main(String [] args) {

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));


	if (args.length < 2) {
	    System.err.println("usage: deviceID directory");
	    return;
	}

	long sensorID = Long.parseLong(args[0]);
	String directory = args[1];

	DeviceLog log = null;
	try {
	    log = new DeviceLog(sensorID, directory);
	}
	catch (Exception e) {
	    _log4j.error(e.getMessage());
	    return;
	}

	long startTime = 0;

	try {
	    saveImages(log, startTime);
	}
	catch (Exception e) {
	    _log4j.error(e.getMessage());
	}
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.distributed.RangeException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SensorDataPacket;

/**
   Print out DevicePackets contained in DeviceLog
   @author Tom O'Reilly
 */
public class LogViewer {

    static protected Logger _logger = Logger.getLogger(LogPublisher.class);

    public void printPackets(long deviceID, String directory) 
    throws FileNotFoundException, IOException, RangeException {
	DeviceLog log = new DeviceLog(deviceID, directory);
	_logger.info("Log contains " + log.nPackets() + " packets");
	DeviceLogIterator iterator = new DeviceLogIterator(log);

	int nPackets = 0;
	while (iterator.hasNext()) {

	    try {
		DevicePacket packet = (DevicePacket )(iterator.next());

		// Print this packet
		System.out.println(DateFormat.getDateTimeInstance().format(new Date(packet.systemTime())));

		System.out.println("src: " + packet.sourceID() + 
				   " time: " + packet.systemTime() + 
				   " seqNo: " + packet.sequenceNo() +
				   " metadataRef: " + packet.metadataRef());

		if (packet instanceof SensorDataPacket) {

		    SensorDataPacket dataPacket = 
			(SensorDataPacket )packet;

		    System.out.println("data: ");
		    System.out.println(new String(dataPacket.dataBuffer()));
		    System.out.println("");
		}
		else if (packet instanceof MetadataPacket) {
		    MetadataPacket metadataPacket = 
			(MetadataPacket )packet;

		    System.out.println("metadata: ");
		    System.out.println(new String(metadataPacket.getBytes()));
		    System.out.println("");
		}
		else if (packet instanceof DeviceMessagePacket) {
		    DeviceMessagePacket messagePacket = 
			(DeviceMessagePacket )packet;

		    System.out.println("device message: ");
		    System.out.println(new String(messagePacket.getMessage()));
		    System.out.println("");
		}

		nPackets++;
	    }
	    catch (Exception e) {
		_logger.error(e);
	    }
	}
	_logger.info("Processed " + nPackets + " packets");
    }

    public static void main(String[] args) {

	BasicConfigurator.configure();

	if (args.length != 2) {
	    System.err.println("usage: LogPublisher [isiDeviceId][directory]");
	    return;
	}

	long deviceID = 0;

	try {
	    deviceID = Long.parseLong(args[0]);
	}
	catch (NumberFormatException e) {
	    _logger.error("Invalid device ID");
	    return;
	}

	String directory = args[1];

	try {
	    LogViewer viewer = new LogViewer();
	    viewer.printPackets(deviceID, directory);
	}
	catch (Exception e) {
	    _logger.error(e);
	}
    }
}

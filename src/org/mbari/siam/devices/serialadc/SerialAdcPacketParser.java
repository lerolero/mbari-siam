/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.serialadc;

import java.util.Vector;
import java.util.StringTokenizer;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NotSupportedException;

/** Parses SIAM data packets */
public class SerialAdcPacketParser extends PacketParser {

    static private Logger _logger = 
	Logger.getLogger(SerialAdcPacketParser.class);

    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException {

	Vector fields = new Vector();

	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	SensorDataPacket sensorDataPacket = (SensorDataPacket )packet;
	String foo = new String(sensorDataPacket.dataBuffer());

	DecimalFormat decimalFormat = new DecimalFormat();

	StringTokenizer lineTokenizer = new StringTokenizer(foo.trim(),"\r\n");

	while (lineTokenizer.hasMoreTokens()) {

	    String channelName = "";

	    String line = lineTokenizer.nextToken().trim();

	    if (line.indexOf("N0000000") >= 0) {
		// No samples yet for this sensor; skip it
		continue;
	    }

	    // _logger.debug("line: " + line);

	    boolean waterSensor = false;
	    if (line.startsWith("WS")) {
		waterSensor = true;
	    }

	    StringTokenizer fieldTokenizer = 
		new StringTokenizer(line, " ");

	    int nField = 0;
	    while (fieldTokenizer.hasMoreTokens()) {

		String token = fieldTokenizer.nextToken().trim();
		// _logger.debug("token: " + token);
		switch (nField++) {

		case 0:
		    channelName = token;
		    break;

		default:
		    String fieldName = channelName;
		    if (waterSensor) {
			// Parse water subsensors
		    }
		    else {

			// Non-water sensor channel
			if (token.startsWith("A")) 
			    fieldName += " avg";
			else if (token.startsWith("L"))
			    fieldName += " lo";
			else if (token.startsWith("H"))
			    fieldName += " hi";
			else {
			    // Don't parse this field
			    continue;
			}

			try {
			    Double value = Double.valueOf(token.substring(1));
			    _logger.debug("create new field " + fieldName);
			    Field field = new Field(fieldName, value, "");
			    fields.add(field);
			}
			catch (NumberFormatException e) {
			    throw new ParseException("bad number format: " + 
						     token, nField);
			}
		    } 
		}
	    }
	}

	// Transfer field vector elements to field array.
	Field[] fieldArray = new Field[fields.size()];
	for (int i = 0; i < fields.size(); i++) {
	    fieldArray[i] = (Field )fields.elementAt(i);
	}

	return fieldArray;

    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.base;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;

/** Parses SIAM data packets */
public class CTDPacketParser extends PacketParser {

    static private Logger _logger = Logger.getLogger(CTDPacketParser.class);

    private static final long serialVersionUID=1L;

    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException {

	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	SensorDataPacket sensorDataPacket = (SensorDataPacket )packet;
	String foo = new String(sensorDataPacket.dataBuffer());
	PacketParser.Field[] fields = new PacketParser.Field[2];
	DecimalFormat decimalFormat = new DecimalFormat();
	StringTokenizer tokenizer = new StringTokenizer(foo.trim(),",");

	int nTokens = 0;
	while (tokenizer.hasMoreTokens()) {
	    String token = tokenizer.nextToken().trim();
	    // Token should be numeric value...
	    Number value = null;
	    if ((value = 
		 decimalFormat.parse(token, new ParsePosition(0))) == null) {
		// Not a number - try next token...
		continue;
	    }

	    switch (nTokens++) {

	    case 0:
		_logger.debug("creating first field");
		fields[0] = new PacketParser.Field("tmprt", value, "deg C");
		break;

	    case 1:
		_logger.debug("creating second field");
		fields[1] = new PacketParser.Field("conduct", value,
						   "siemens/meter");
		break;
	    }
	}
	
	if (nTokens < 2) {
	    // Couldn't parse - some kinda error
	    _logger.error("found only " + nTokens + " tokens");
	    throw new ParseException("found only " + nTokens + " tokens", 0);
	}

	_logger.debug("returning fields");
	return fields;
    }
}

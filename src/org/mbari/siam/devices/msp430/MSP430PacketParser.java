/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.msp430;

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
public class MSP430PacketParser extends PacketParser {

    public static final String TEMPERATURE_MNEM = "tmprt";
    public static final String HUMIDITY_MNEM = "humid";
    public static final String PRESSURE_MNEM = "press";
    public static final String GRND_FAULT_LO_MNEM = "GF-low";
    public static final String GRND_FAULT_HI_MNEM = "GF-high";
    public static final String HEADING_MNEM = "heading";
    public static final String TURNS_MNEM = "nTurns";

    static private Logger _logger = Logger.getLogger(MSP430PacketParser.class);

    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException {

	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	PacketParser.Field[] fields = new PacketParser.Field[8];

	SensorDataPacket sensorDataPacket = (SensorDataPacket )packet;
	String foo = new String(sensorDataPacket.dataBuffer());

	DecimalFormat decimalFormat = new DecimalFormat();
	StringTokenizer tokenizer = new StringTokenizer(foo.trim(),",");

	int nTokens = 0;
	int nValues = 0;
	String fieldName = null;
	String units = null;
	int offset = 0;  // Offset into field at which numeric value starts

	while (tokenizer.hasMoreTokens()) {
	    String token = tokenizer.nextToken().trim();

	    switch (nTokens++) {

	    case 0:
		if (!token.equals("$PEDATA")) {
		    throw new ParseException("Expected '$PEDATA'", 0);
		}
		continue;

	    case 1:
		if (!token.toUpperCase().startsWith("P")) {
		    throw new ParseException("Expected 'P' field", 0);
		}
		offset = 1;
		fieldName = PRESSURE_MNEM;
		units = "millibar";
		break;

	    case 2:
		if (!token.toUpperCase().startsWith("T")) {
		    throw new ParseException("Expected 'T' field", 0);
		}
		offset = 1;
		fieldName = TEMPERATURE_MNEM;
		units = "deg C";
		break;

	    case 3:
		if (!token.toUpperCase().startsWith("H")) {
		    throw new ParseException("Expected 'H' field", 0);
		}
		offset = 1;
		fieldName = HUMIDITY_MNEM;
		units = "rel %";
		break;

	    case 4:
		if (!token.toUpperCase().startsWith("GFL")) {
		    throw new ParseException("Expected 'GFL' field", 0);
		}
		offset = 3;
		fieldName = GRND_FAULT_LO_MNEM;
		units = "microAmps";
		break;

	    case 5:
		if (!token.toUpperCase().startsWith("GFH")) {
		    throw new ParseException("Expected 'GFH' field", 0);
		}
		offset = 3;
		fieldName = GRND_FAULT_HI_MNEM;
		units = "microAmps";
		break;

	    case 6:
		if (!token.toUpperCase().startsWith("C")) {
		    throw new ParseException("Expected 'C' field", 0);
		}
		offset = 1;
		fieldName = HEADING_MNEM;
		units = "deg";
		break;

	    case 7:
		if (!token.toUpperCase().startsWith("TC")) {
		    throw new ParseException("Expected 'TC' field", 0);
		}
		offset = 2;
		fieldName = TURNS_MNEM;
		units = "turns";
		break;

	    default:
		continue;
	    }

	    String valueStr = token.substring(offset);
	    Number value = decimalFormat.parse(valueStr, new ParsePosition(0));
	    if (value == null) {
		throw new ParseException("'" + valueStr + 
					 "' is not a number", 0);
	    }

	    fields[nValues++] = 
		new PacketParser.Field(fieldName, value, units);
	}


	if (nValues != 7) {
	    throw new ParseException("Only found " + nValues + " values", 0);
	}

	return fields;
    }
}

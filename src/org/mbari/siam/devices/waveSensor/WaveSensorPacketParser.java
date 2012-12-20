/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.waveSensor;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;

public class WaveSensorPacketParser extends PacketParser {

	static private Logger _logger = Logger
			.getLogger(WaveSensorPacketParser.class);

	/** Return fields parsed from DevicePacket. */
	public PacketParser.Field[] parseFields(DevicePacket packet)
			throws NotSupportedException, ParseException {

		if (!(packet instanceof SensorDataPacket)) {
			throw new NotSupportedException("expecting SensorDataPacket");
		}

		SensorDataPacket sensorDataPacket = (SensorDataPacket) packet;
		String buffer = new String(sensorDataPacket.dataBuffer());
		DecimalFormat decimalFormat = new DecimalFormat();
		StringTokenizer tokenizer = new StringTokenizer(buffer.trim(), ",");
		ParsePosition parsePos = new ParsePosition(0);

		// Accomodate six fields...
		Field[] fields = new Field[6];

		int nTokens = 0;
		int nFields = 0;

		while (tokenizer.hasMoreTokens()) {

			String token = tokenizer.nextToken().trim();
			Number value = null;

			switch (nTokens) {
			case 0:
				if (!token.equals("$WC")) {
					throw new NotSupportedException("expecting $WC message");
				}

				nTokens++;
				continue;

			default:
				parsePos.setIndex(0);
				if ((value = decimalFormat.parse(token, parsePos)) == null) {
					throw new ParseException(token
							+ ": expecting numeric value", 0);
				}
			}

			String fieldName = null;
			String units = null;

			switch (nTokens++) {

			case 14:
				// This is just the number of bytes in raw message; skip it
				continue;

			case 15:
				fieldName = "Hs";
				units = "meters";
				break;

			case 16:
				fieldName = "Tp";
				units = "sec";
				break;

			case 17:
				fieldName = "Hmax";
				units = "meters";
				break;

			case 18:
				fieldName = "Mean direction";
				units = "deg";
				break;

			case 19:
				fieldName = "Direction spread";
				units = "deg";
				break;

			default:
				continue;
			}

			fields[nFields++] = new Field(fieldName, value, units);

		}
		return fields;
	}
}
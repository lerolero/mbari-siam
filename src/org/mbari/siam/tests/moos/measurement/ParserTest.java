/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.measurement;

import java.util.StringTokenizer;
import java.util.Enumeration;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NotSupportedException;

public class ParserTest {


    public static void main(String[] args) {

	if (args.length != 1) {
	    System.err.println("Usage: measName");
	    System.err.println("(Will attempt to modify measurment having specified name)");
	    return;
	}

	// Attempt to modify measurement having this name.
	String measurementName = args[0];

	// Create a SensorDataPacket
	SensorDataPacket packet = new SensorDataPacket(999, 1024);
	packet.setSystemTime(System.currentTimeMillis());

	byte[] buf = "111; 222; 333; 444; 555".getBytes();

	packet.setDataBuffer(buf);

	// Create the parser
	MyParser parser = new MyParser();

	Record record = null;
	try {
	    record = parser.parse(packet);
	}
	catch (Exception e) {
	    System.out.println(e);
	    return;
	}

	System.out.println("Record:\n" + record);
	System.out.println("record has " + record.size() + " keys");

	try {
	    parser.setMeasurement(measurementName, 6666);
	    System.out.println("After set of " + measurementName + ":\n");
	    System.out.println(record);
	}
	catch (Exception e) {
	    System.out.println(e);
	}

	try {
	    parser.setArrayMeasurement(measurementName, 0, 6666);
	    System.out.println("After setArray of " + 
			       measurementName + ":\n");

	    System.out.println(record);
	}
	catch (Exception e) {
	    System.out.println(e);
	}
    }


    static class MyParser extends DevicePacketParser {

	protected void parseFields(DevicePacket devicePacket) 
	    throws NotSupportedException, Exception {

	    SensorDataPacket packet = (SensorDataPacket )devicePacket;


	    String input = new String(packet.dataBuffer());
	    System.out.println("parseFields(): input = " + input);

	    StringTokenizer tokenizer = 
		new StringTokenizer(input, "; ");

	    int field = 0;
	    while (tokenizer.hasMoreTokens()) {

		String token = tokenizer.nextToken();

		int value;
		try {
		    value = Integer.parseInt(token);
		    System.out.println("value=" + value);
		}
		catch (NumberFormatException e) {
		    System.out.println("NumberFormatException: " + token);
		    value = -999;
		}

		System.out.println("call addMeasurement() for token " +
				   token);

		addMeasurement("myField-"+field,
			       "fake field #" + field,
			       "no units!", value);

		field++;
	    }

	    // Now add an array of int values
	    int[] values = {1001, 1002, 1003, 1004, 1005};
	    addArrayMeasurement("bogosity", "Bogus factor", "bushes", 
				values);
	}
    }

}

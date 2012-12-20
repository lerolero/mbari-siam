/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.state;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Array;

public class StateTest {

    /** StaticState represents attributes that are set at service 
	startup, and do not change while service is running. */
    static public class StaticState 
	implements Serializable {

	String name = "This is the name";

	byte[] mnemonic = "Nobska-MAVS".getBytes();

	// Interval between samples
	int sampleIntervalMsec = 2000;

	// Enable/disable internal Nobska log
	boolean enableInternalLog = true;

	// Sampling frequency
	float sampleHz = 5.f;

	// Average sample size
	int averageSamples = 3;

	/** Verify that attributes are valid. */
	void validate() 
	    throws Exception {
	}
    }


    /** DynamicState represents attributes that can be modified while 
	service is running. */
    public class DynamicState 
	implements Serializable {

	// Records per SIAM subsample
	int recordsPerSample = 3;

	/** Verify that attributes are valid. */
	void validate() 
	    throws Exception {
	    
	    if (recordsPerSample <= 0) {
		throw new Exception("recordsPerSample must be positive int");
	    }
	}
    }


    static void printObject(Object object) {

	System.out.println("class " + object.getClass().getName() + ":");
	Class c = object.getClass();
	Field fields[] = c.getDeclaredFields();
	System.out.println("has " + fields.length + " fields");
	for (int i = 0; i < fields.length; i++) {
	    Class typeClass = fields[i].getType();
	    System.out.print("  " + 
			     fields[i].getName() + " = ");
	    try {
		Object value = fields[i].get(object);
		if (value.getClass().isArray()) {
		    int arrayLength = Array.getLength(value);
		    String typeName = 
			value.getClass().getComponentType().getName();

		    // If byte array, print out as a string
		    if (typeName.equals("byte")) {
			byte[] buf = new byte[arrayLength];
			for (int j = 0; j < arrayLength; j++) {
			    buf[j] = Array.getByte(value, j);
			}
			System.out.println(new String(buf, 0, arrayLength));
		    }
		    else {
			for (int j = 0; j < arrayLength; j++) {
			    System.out.print(Array.get(value, j) + " ");
			}
			System.out.println();
		    }
		}
		else {
		    System.out.println(value);
		}
	    }
	    catch (IllegalAccessException e) {
		System.out.println("VALUE NOT ACCESSIBLE");
	    }
	}
    }


    public static void main(String[] args) {

	printObject(new StaticState());
    }

}


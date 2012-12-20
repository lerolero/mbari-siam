/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.base;

import org.mbari.siam.utils.DelimitedStringParser;
import org.mbari.siam.distributed.PacketParser;

import java.text.ParseException;

/** Parses SIAM data packets */
/*
  $Id: SeabirdPacketParser.java,v 1.7 2012/12/17 21:34:26 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.7 $
*/

public class SeabirdPacketParser extends DelimitedStringParser {

    public static final String TEMPERATURE_KEY = "temperature";
    public static final String CONDUCTIVITY_KEY = "conductivity";
    public static final String PRESSURE_KEY = "pressure";
    public static final String VOLT_KEY = "volt";

    private static final long serialVersionUID=1L;

    public SeabirdPacketParser() {
	super(",");
    }

    /** Process each token in Seabird's ASCII output record; this method is
     called by the framework for each token in the record. */
    protected PacketParser.Field processToken(int nToken, String token) 
	throws ParseException {

	// Note that each token in Seabird output represents a number.
	Number value = decimalValue(token);

	switch (nToken) {

	case 0:
	    // Temperature
	    if (value == null) {
		throw new ParseException("Invalid tmprt: " + token, 0);
	    }
	    return new Field(TEMPERATURE_KEY, value, "deg C");

	case 1:
	    // Conductivity
	    if (value == null) {
		throw new ParseException("Invalid conduct: " + token, 0);
	    }
	    return new Field(CONDUCTIVITY_KEY, value, "siemens/meter");

	case 2:
	    // Pressure
	    if (value == null) {
		throw new ParseException("Invalid pressure: " + token, 0);
	    }
	    return new Field(PRESSURE_KEY, value, "decibars");

	case 3:
	case 4:
	case 5:
	case 6:
	case 7:
	case 8:
	    // Voltage 0-n
	    String voltKey = VOLT_KEY + (nToken-3);
	    if (value == null) {
		throw new ParseException("Invalid " + voltKey + ": " + token, 0);
	    }
	    return new Field(voltKey, value, "volts");

	}

	return null;
    }
}

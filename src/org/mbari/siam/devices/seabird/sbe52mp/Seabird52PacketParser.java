/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.sbe52mp;

import org.mbari.siam.utils.DelimitedStringParser;
import org.mbari.siam.distributed.PacketParser;

import java.text.ParseException;

/** Parses SIAM data packets *
/*
  $Id: Seabird52PacketParser.java,v 1.7 2012/12/17 21:34:32 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.7 $
*/

public class Seabird52PacketParser extends DelimitedStringParser {

    public static final String TEMPERATURE_KEY = "temperature";
    public static final String CONDUCTIVITY_KEY = "conductivity";
    public static final String PRESSURE_KEY = "pressure";
    public static final String OXYGEN_KEY = "oxygen";

    private static final long serialVersionUID=1L;

    public Seabird52PacketParser() {
	super(",");
    }
    public Seabird52PacketParser(String registryName) {
	super(registryName,",");
    }

    /** Process each token in Seabird 52's ASCII output record; this method is
     called by the framework for each token in the record. */
    protected PacketParser.Field processToken(int nToken, String token) 
	throws ParseException {

	// Note that each token in Seabird output represents a number.
	Number value = decimalValue(token);

	switch (nToken) {

	case 0:
	    // Conductivity
	    if (value == null) {
		throw new ParseException("Invalid conduct: " + token, 0);
	    }
	    return new Field(CONDUCTIVITY_KEY, value, "siemens/meter");

	case 1:
	    // Temperature
	    if (value == null) {
		throw new ParseException("Invalid tmprt: " + token, 0);
	    }
	    return new Field(TEMPERATURE_KEY, value, "deg C");

	case 2:
	    // Pressure
	    if (value == null) {
		throw new ParseException("Invalid pressure: " + token, 0);
	    }
	    return new Field(PRESSURE_KEY, value, "db");

	case 3:
	    // Oxygen
	    if (value == null) {
		throw new ParseException("Invalid oxygen: " + token, 0);
	    }
	    return new Field(OXYGEN_KEY, value, "ml/l");
	}

	return null;
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.kvh;

import java.text.ParseException;
import org.mbari.siam.utils.DelimitedStringParser;
import org.mbari.siam.distributed.PacketParser;


/** Parses SIAM data packets */
public class CompassPacketParser extends DelimitedStringParser {

    public static final String HEADING_MNEM = "heading";

    public CompassPacketParser() {
	super(",");
    }

    /** Return field corresponding to specified token. */
    protected PacketParser.Field processToken(int nToken, String token) 
	throws ParseException {

	Number value = decimalValue(token);

	switch (nToken) {

	case 1:
	    if (value == null) {
		throw new ParseException("Invalid heading: " + token, 0);
	    }
	    return new Field(HEADING_MNEM, value, "degrees");
	}

	return null;
    }
}


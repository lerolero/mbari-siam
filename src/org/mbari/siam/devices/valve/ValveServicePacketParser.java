/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.valve;

import org.mbari.siam.utils.DelimitedStringParser;
import org.mbari.siam.distributed.PacketParser;

import java.text.ParseException;

/** Parses SIAM data packets from ValveService */
/*
  $Id $
  $Name: HEAD $
  $Revision: 1.3 $
*/

public class ValveServicePacketParser extends DelimitedStringParser {

    public static final String POSITION_KEY = "position";

    private static final long serialVersionUID=1L;

    public ValveServicePacketParser() {
	super(" \t");
    }

    public ValveServicePacketParser(String registryName) {
	super(registryName, " \t");
    }

    /** Process each token in the ValveService ASCII output record; this method is
	called by the framework for each token in the record.
    */
    protected PacketParser.Field processToken(int nToken, String token) throws ParseException
    {
	return(new Field((POSITION_KEY + nToken), decimalValue(token), "Valve position"));
    }
}

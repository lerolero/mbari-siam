/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.valve;

import org.mbari.siam.distributed.devices.ValveServiceIF;

import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.RangeException;

/** Attributes for ValveService
 $Id: ValveAttributes.java,v 1.3 2012/12/17 21:34:42 oreilly Exp $
 $Name: HEAD $
 $Revision: 1.3 $
 * @author Bob Herlien
 */

class ValveAttributes extends InstrumentServiceAttributes
{
    ValveAttributes(DeviceServiceIF service) {
	super(service);
    }

    /** Name for InstrumentRegistry */
    public String registryName = "ValveService";

    /** Number of valves actually present */
    public int numValves = 2;

    /** Number of retries before failing */
    public int retries = 3;

    /** Timeout for valve to reach commanded position */
    public int timeoutMS = 5000;

    /** Array indicating bit number for CMD1 bit (Input#1 in Hanbay doc) for each valve */
    public int[] cmd1Bits = {3, 1, 0, 0};

    /** Array indicating bit number for CMD2 bit (Input#2 in Hanbay doc) for each valve */
    public int[] cmd2Bits = {4, 2, 0, 0};

    /** Array indicating bit number for FEEDBACK1 bit (Output#1 in Hanbay doc) for each valve */
    public int[] feedback1Bits = {11, 9, 0, 0};

    /** Array indicating bit number for FEEDBACK2 bit (Output#2 in Hanbay doc) for each valve */
    public int[] feedback2Bits = {12, 10, 0, 0};

    /** Array indicating the valve position that open() represents, per valve */
    public String[] openPos = {"LEFT", "LEFT", "LEFT", "LEFT"};

    /** Array indicating the valve position that close() represents, per valve */
    public String[] closedPos = {"CENTER", "CENTER", "CENTER", "CENTER"};

    /** String identifiers for the openPos and closedPos attributes */
    protected final static String valvePositions[] = {"LEFT", "RIGHT", "CENTER", "BACK"};

    protected int[] _openpos = new int[openPos.length];
    protected int[] _closedpos = new int[closedPos.length];

    /**
     * Throw InvalidPropertyException if any invalid attribute values found.
     */
    public void checkValues() throws InvalidPropertyException
    {
	if ((numValves < 1) || (numValves > ValveServiceIF.MAX_VALVES))
	    throw new InvalidPropertyException("numValves must be between 1 and " + 
					       ValveServiceIF.MAX_VALVES);

	for (int i = 0; i < ValveServiceIF.MAX_VALVES; i++)
	{
	    for (int j = 0; j < valvePositions.length; j++)
	    {
		if (openPos[i].equalsIgnoreCase(valvePositions[j])) {
		    _openpos[i] = j;
		}
		if (closedPos[i].equalsIgnoreCase(valvePositions[j])) {
		    _closedpos[i] = j;
		}
	    }
	}
    } /* checkValues() */
} /* Class ValveAttributes */

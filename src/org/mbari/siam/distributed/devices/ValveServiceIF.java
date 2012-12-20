/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.devices;

import java.rmi.Remote;
import java.io.Serializable;
import java.io.IOException;
import org.mbari.siam.distributed.RangeException;

/** Remote Interface to allow ValveService to return multiple ValveIF's
 */
/*
 $Id: ValveServiceIF.java,v 1.2 2012/12/17 21:35:13 oreilly Exp $
 $Name: HEAD $
 $Revision: 1.2 $
 */

public interface ValveServiceIF extends Remote
{
    /** Forward location valve		*/
    public static final int VALVE_FWD   = 0;
    /** Aft location valve		*/
    public static final int VALVE_AFT   = 1;
    /** Extra valve 0 			*/
    public static final int VALVE_EXTRA0 = 2;
    /** Extra valve 1 			*/
    public static final int VALVE_EXTRA1 = 3;

    /** Max number of valves supported by ValveService */
    public static final int MAX_VALVES = 4;

    /** Get a ValveIF for the given valveNum */
    public ValveIF getValve(int valveNum) throws RangeException, IOException;

} /* interface ValveServiceIF */

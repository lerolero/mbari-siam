/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.devices;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.io.IOException;
import org.mbari.siam.distributed.RangeException;

/** Remote Interface for performing closed loop control via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ValveIF extends Remote
{
    /** Valve in Left position	*/
    public static final int POSITION_LEFT   = 0;
    /** Valve in Right position	*/
    public static final int POSITION_RIGHT  = 1;
    /** Valve in Center position (usually represents closed) */
    public static final int POSITION_CENTER = 2;
    /** Valve in Back position	*/
    public static final int POSITION_BACK   = 3;


    /** Explicitly set valve position	*/
    public void setPosition(int position) throws RangeException, IOException;

    /** Get current position of valve.  This MAY perform I/O to the valve
	to query the position */
    public int  getPosition() throws IOException;

    /** Set the meaning of a function (open() or close(), in terms of valve position */
    public void setFunctionMap(int function, int position)
	throws RangeException, IOException;

    /** Get the meaning of a function (open() or close(), in terms of valve position */
    public int  getFunctionMap(int function) throws RangeException, IOException;

    /** Get the String name for the given valve position */
    public String getPositionName(int position) throws RemoteException;

    /** Parse the given String name into a valve position */
    public int parsePositionName(String name) 
	throws IllegalArgumentException, RemoteException;

} /* interface ValveIF */

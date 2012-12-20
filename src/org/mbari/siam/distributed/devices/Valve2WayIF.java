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
 $Id: Valve2WayIF.java,v 1.3 2012/12/17 21:35:11 oreilly Exp $
 $Name: HEAD $
 $Revision: 1.3 $
 */

public interface Valve2WayIF extends ValveIF
{
    /** Valve is still moving, is stalled, or has given up */
    public static final int POSITION_MOVING   = -1;

    /** Represents open() function in FunctionMap */
    public static final int OPEN_FUNC  = 0;
    /** Represents close() function in FunctionMap */
    public static final int CLOSE_FUNC = 1;


    /** Open the valve.  This sets the valve to the position indicated by
	getFunctionMap(OPEN_FUNC) */
    public void open() throws IOException;

    /** Close the valve.  This sets the valve to the position indicated by
	getFunctionMap(CLOSE_FUNC) */
    public void close() throws IOException;

    /** Returns true if valve is in an open position */
    public boolean isOpen() throws IOException;

    /** Returns true if valve is in an open position */
    public boolean isClosed() throws IOException;

} /* interface Valve2WayIF */

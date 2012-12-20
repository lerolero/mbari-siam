/****************************************************************************/
/* Copyright 2004 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.SleepRollCallListener;


/**
CpuLeaseSleepRollcallListener is responsible for keeping the CPU
awake as requested by external processes.

@author Bob Herlien
*/

public class CpuLeaseSleepRollcallListener implements SleepRollCallListener
{
    Vector	_lessees;
    private static Logger _logger = 
	Logger.getLogger(CpuLeaseSleepRollcallListener.class);

    CpuLeaseSleepRollcallListener()
    {
	_lessees = new Vector();
	SleepManager.getInstance().addSleepRollCallListener(this);
	_logger.info("CpuLeaseSleepRollcallListener() constructor");

    } /* CpuLeaseSleepRollcallListener() */


    /** Request that the CPU come on at a certain time in the future
	and/or remain on for a certain duration.
	@param requestorID Unique ID (externally assigned) to identify
	who is requesting the CPU to remain on.  Allows for multiple
	requestors each requesting that the CPU be on.
	@param when Milliseconds until the requestor needs the CPU on.
	Use 0 (zero) to request that the CPU remain on starting now.
	@param howLong Number of milliseconds that the CPU should remain
	on.  Use 0 (zero) to cancel an earlier request.
    */
    public void cpuLease(int requestorID, long when, long howLong)
	throws RemoteException
    {
	CpuLessee lessee = new CpuLessee(requestorID, when, howLong);

	_logger.debug("Requestor " + requestorID + " when = " +
		      when + " howLong = " + howLong);

	synchronized(_lessees)
	{
	    _lessees.remove(lessee);
	    if ((howLong > 0) &&
		(lessee.getEndTime() > System.currentTimeMillis()))
		_lessees.add(lessee);
	}
	_logger.debug("There are " +_lessees.size() + " leases.");
    }


/** Return <= 0 if need to stay awake.  Else, return number of milliseconds
    until we need to wake up.
*/
    public long okToSleep()
    {
	long now = System.currentTimeMillis();
	long min = Long.MAX_VALUE;
	
	synchronized(_lessees)
	{
	    Iterator it = _lessees.iterator();
	    while(it.hasNext())
	    {
		CpuLessee lessee = (CpuLessee)it.next();
		if (lessee.getEndTime() <= now) {
		    it.remove();		//Expired, remove it
		}
		else if (lessee.getStartTime() < min) {
		    min = lessee.getStartTime(); //Find earliest start time
		}

		if (min <= now) {		//If wants CPU now, return
		    //_logger.debug("okToSleep() - lease active; don't sleep");
		    return(0);
		}
	    }
	}

	long nextStart = min - now;

	if (nextStart <= 0) {
	    // _logger.debug("okToSleep() - lease active? Don't sleep yet");
	}
	else {
	    // _logger.debug("okToSleep() - next lessee start time in " + 
	    //	  nextStart + " msec");
	}
	return nextStart;
    }

    /** 
	A CpuLessee is constructed and put on the _lessees list for
	every new cpuLease().
    */
    class CpuLessee
    {
        int		_requestorID;
	long		_startTime;
	long		_endTime;

	public CpuLessee(int requestID, long when, long howLong)
	{
	    _requestorID = requestID;
	    _startTime = System.currentTimeMillis() + when;
	    _endTime = _startTime + howLong;
	}

	public boolean equals(Object o)
	{
	    return((o instanceof CpuLessee) &&
		   (_requestorID == ((CpuLessee)o)._requestorID));
	}

	public int hashCode()
	{
	    return(_requestorID);
	}

	long getStartTime()
	{
	    return(_startTime);
	}

	long getEndTime()
	{
	    return(_endTime);
	}
    }


    public Vector getLessees() {

	// First make a pass through the list and remove expired leases
	long now = System.currentTimeMillis();
	synchronized(_lessees) {
	    Iterator it = _lessees.iterator();
	    while(it.hasNext()) {
		CpuLessee lessee = (CpuLessee)it.next();
		if (lessee.getEndTime() <= now) {
		    it.remove();		//Expired, remove it
		}
	    }
	}

	return _lessees;
    }


} /* CpuLeaseSleepRollcallListener */

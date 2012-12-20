/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
/** 
* @Title LeaseManager
* @author Bob Herlien
* @version $Revision: 1.2 $
* @date $Date: 2009/07/16 22:08:01 $
*
* REVISION HISTORY:
* $Log: LeaseManager.java,v $
* Revision 1.2  2009/07/16 22:08:01  headley
* javadoc syntax fixes for 1.5 JDK
*
* Revision 1.1  2008/11/04 22:17:52  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:04  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.16  2008/02/05 17:49:56  oreilly
* check for null leaseTimer
*
* Revision 1.15  2007/03/28 19:33:13  oreilly
* added nLessees()
*
* Revision 1.14  2007/03/09 21:58:15  oreilly
* changed some variable names
*
* Revision 1.13  2007/03/06 00:42:21  oreilly
* synchronized block includes lease-establish callbacks
*
* Revision 1.12  2006/04/04 21:19:23  oreilly
* terminate() throws LeaseRefused
*
* Revision 1.11  2006/04/04 20:33:36  oreilly
* Added lease duration to debug msg
*
* Revision 1.10  2006/03/14 22:46:17  oreilly
* getLessee() has public access
*
* Revision 1.9  2005/03/17 19:24:53  salamy
* Added lease renewal and lease time remaining capabilities for getLeases utility.  KAS 3/17/2005
*
* Revision 1.8  2005/02/09 21:00:00  salamy
* Refactored Lessee name, added note to 'lease establish' methods
*
* Revision 1.7  2004/03/24 23:45:06  bobh
* Tighter synchronized regions that don't keep monitor during RMI calls
*
* Revision 1.6  2004/03/02 00:45:50  bobh
* Added log4j
*
* Revision 1.5  2004/02/24 18:23:24  bobh
* no message
*
*
*/
package org.mbari.siam.distributed.leasing;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.RemoteException;
import org.mbari.siam.core.SiamTimer;
import org.mbari.siam.core.SiamTimerTask;
//import moos.interfaces.leasing.LeaseID;
import org.mbari.siam.distributed.leasing.LeaseListener;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.apache.log4j.Logger;

/**
LeaseManager was written to manage an intermittent communication link,
though it could be used to manage other resources that fit the paradigm.

The paradigm consists of two types of "clients".  Lessees are clients
who want to keep the resource (link) open, and do so by calling
establish(), renew() and terminate().  LeaseManager can manage multiple
such clients simultaneously, and keeps them in a vector.

LeaseListeners are clients who actually perform the resource management
based on whether there are any outstanding lessees.  In the current
implementation, there is just one LeaseListener, who simply turns on/off
the RF power to the link.  But in principle there can also be multiple
listeners, who presumably perform different functions based on the
lease state.

NOTE: It is important that the callbacks implemented by LeaseListeners
execute in a timely manner, so that the remote client is not blocked
for an excessive amount of time.

@author Tom O'Reilly
Modified by Bob Herlien
*/
public class LeaseManager {

    /** Constant which indicates an indeterminate time. */
    public final static long UNKNOWN_TIME = 0;
    private static Logger _log4j = Logger.getLogger(LeaseManager.class);

    Vector	_lessees = null;
    Vector	_listeners = null;
    int		_nextLeaseID = 1;
    LeaseManager _leaseManager  = null;
    DateFormat	_dateFormatter = null;
    boolean	_debug = true;

    /**
       Constructor.
       connect/disconnect occurs.
    */
    public LeaseManager()
    {
        _lessees = new Vector();
        _listeners = new Vector();
	_leaseManager = this;
	_dateFormatter = DateFormat.getDateTimeInstance();
    }

    /**
       Add a lease listener.
    */
    public synchronized void addListener(LeaseListener listener) {
	_listeners.add(listener);
    }


    /** 
	Called to establish the communications link.
	@param leaseMillisec lease period in milliseconds
	@param nextEstablishTime Estimated time that this task expects to
		call establish() again.
	@param clientNote note about connection
	@return leaseID to use with renew(), terminate().  Will be >= 1.
    */
    public int establish(long leaseMillisec,
		  long nextEstablishTime, String clientNote) 
	throws LeaseRefused
    {
	Lessee lessee;
	int leaseID, numLessees;
	Enumeration e;

	synchronized(this)
	{
	    lessee = new Lessee(leaseMillisec, nextEstablishTime,
				clientNote);
	    leaseID = lessee.getLeaseID();
	    numLessees = _lessees.size() + 1;
	    e = _listeners.elements();
	}

	debugPrint("LeaseManager.establish(" + leaseMillisec + 
		   ") returns " + leaseID + " at " +
		  _dateFormatter.format(new Date(System.currentTimeMillis())));

	/* Invoke listener callbacks, update list of lessees. Note that any callback
	 can throw LeaseRefused exception. Note that callback invocation is included
	 in this synchronized block, as a callback may attempt to access the
	 node's modem device in order to establish a communication link to shore.
	*/
	synchronized(this) {

	    while (e.hasMoreElements()) {
		LeaseListener listener = (LeaseListener)e.nextElement();
		listener.leaseEstablishCallback(leaseID, leaseMillisec,
						nextEstablishTime, numLessees);
	    }

	    _lessees.add(lessee);
	    _log4j.debug("establish() - call lessee.schedule()");
	    lessee.schedule(leaseMillisec);

	}
	return(leaseID);
    }


    /** 
	Called to establish the communications link.
	@param leaseMillisec lease period in milliseconds
	@return leaseID to use with renew(), terminate().  Will be >= 1.
    */
    public int establish(long leaseMillisec, String clientNote) 
	throws LeaseRefused
    {
        return(establish(leaseMillisec, UNKNOWN_TIME, clientNote));
    }


    /** Get the Lessee that has a given LeaseID */
    public synchronized Lessee getLessee(int leaseID)
    {
        Lessee lessee;

        for (Enumeration e = _lessees.elements(); e.hasMoreElements(); )
	{
	    lessee = (Lessee)e.nextElement();
	    if (lessee.getLeaseID() == leaseID)
	      return(lessee);
	}
	return(null);
    }
    


    /** Return number of lessees */
    public int nLessees() {
	return _lessees.size();
    }


    /** Return vector of lessess. */
    public Vector getLessees() {
    	return _lessees;
    }


    /** 
	Called when lease is being renewed. 
	@param leaseID lease identifier
	@param leaseDurationMsec lease period in milliseconds
    */
    public void renew(int leaseID, long leaseDurationMsec)
      throws LeaseRefused
    {
	Lessee lessee;
	int numLessees;
	Enumeration e;

        debugPrint("LeaseManager.renew() LeaseID " + leaseID + " at " +
		   _dateFormatter.format(new Date(System.currentTimeMillis())));

	if ((lessee = getLessee(leaseID)) == null)
	{
	    _log4j.error("Can't find Lease, assume already expired");
	    throw new LeaseRefused("Lease ID " + leaseID + " not found");
	}

	synchronized(this)
	{
	    numLessees = _lessees.size();
	    e = _listeners.elements();
	}

	// Call listener callbacks. Any of them can throw LeaseRefused.
	while (e.hasMoreElements())
	{
	    LeaseListener listener = (LeaseListener)e.nextElement();
	    listener.leaseRenewalCallback(leaseID, leaseDurationMsec, numLessees);
	}

	synchronized(this)
	{
	    lessee.cancelTimer();
	    lessee.schedule(leaseDurationMsec);
	    lessee._leaseRenewalTime=System.currentTimeMillis();
	    lessee._leaseDurationMsec=leaseDurationMsec;
	}

    }

    /** 
	Called when communications link is being disconnected. 
	@param nextConnectTime next scheduled connection time 
	(millisec from epoch)
     */
    public void terminate(int leaseID, long nextConnectTime) 
	throws LeaseRefused {
	Lessee lessee;
	int numLessees;
	Enumeration e;

        debugPrint("LeaseManager.terminate() LeaseID " + leaseID + " at " +
		 _dateFormatter.format(new Date(System.currentTimeMillis())));

	if ((lessee = getLessee(leaseID)) == null)
	{
	    _log4j.error("Can't find lease ID " + leaseID);
	    throw new LeaseRefused("Lease ID " + leaseID + " not found");
	}

	synchronized(this)
	{
	    lessee.cancelTimer();
	    _lessees.remove(lessee);
	    lessee = null;
	    numLessees = _lessees.size();
	    e = _listeners.elements();
	}

	while (e.hasMoreElements())
	{
	    LeaseListener listener = (LeaseListener)e.nextElement();
	    listener.leaseTerminatedCallback(leaseID, nextConnectTime,
					     numLessees);
	}
    }

    /** Print debug message. */
    protected void debugPrint(String message) {
	_log4j.debug(message);
    }

    /** 
	Runs when Lease expires.
     */
    class LeaseExpiredTask extends SiamTimerTask
    {
        Lessee	_lessee;

        LeaseExpiredTask(Lessee lessee)
	{
	    _lessee = lessee;
	}

	// Runs when lease expires
	public void run()
	{
	    int leaseID, numLessees;
	    Enumeration e;
	    long nextEstablishTime;

	    debugPrint("Lease ID " + _lessee.getLeaseID() +  " expired at " + 
		  _dateFormatter.format(new Date(System.currentTimeMillis())));

	    synchronized(_leaseManager)
	    {
	        // Remove us from _lessees list
		_lessee.cancelTimer();
	        _lessees.remove(_lessee);
		numLessees = _lessees.size();
		leaseID = _lessee.getLeaseID();
		nextEstablishTime = _lessee._nextEstablishTime;
		e = _listeners.elements();
		_lessee = null;
	    }

	    // Call listener callbacks
	    while (e.hasMoreElements())
	    {
		LeaseListener listener = (LeaseListener)e.nextElement();
		listener.leaseExpiredCallback(leaseID, nextEstablishTime,
					      numLessees);
	    }
	}
    } /* LeaseExpiredTask */


    /** 
     A Lessee is constructed and put on the _lessees list for
     every LeaseManager.establish().  It creates a SiamTimer
     and LeaseTimeoutTask to expire the lease
     */
    public class Lessee	{
    	/** Unique identifier for this lease. */
    	public int		_leaseID;
    	/** Timer goes off when lease expires. */
    	SiamTimer	_leaseTimer = null;
    	/** Time at which client might renew lease. */
    	public long		_nextEstablishTime;
    	/** Duration in millisec */
    	public long        _leaseDurationMsec;
    	/** Time at which lease originally established. */
    	public long        _leaseEstablishTime;
    	/** Time at which lease renewed. */
    	public long        _leaseRenewalTime;
    	/** Number of times lease timer was run. */
    	public long        _scheduleCount;


	/** Client annotation. */
	public String _clientNote;

    	Lessee(long leaseDurationMsec, long nextEstablishTime, 
	       String clientNote) {
    		// Save the creation time of this lesee
    		_leaseDurationMsec = leaseDurationMsec;
    		_nextEstablishTime = nextEstablishTime;
		_scheduleCount=0;
		_clientNote = clientNote;

    		synchronized(_leaseManager)
			{
    			_leaseID = _nextLeaseID++;
    			if (_nextLeaseID <= 0)
    				_nextLeaseID = 1;
			}
		}
    	
	int getLeaseID()
	{
	    return(_leaseID);
	}

	void schedule(long leaseDurationMsec)
	{
	    _leaseTimer = new SiamTimer();
	    _leaseTimer.schedule(new LeaseExpiredTask(this), leaseDurationMsec);
	    _log4j.debug("done with Lessee.schedule()");
	    _scheduleCount++;
	    if (_scheduleCount == 1) {
		// This is the first time timer was scheduled, so this is "establish" time
		_leaseEstablishTime = System.currentTimeMillis();
		_leaseRenewalTime=_leaseEstablishTime;
	    }
	}

	/** Cancel the lease timer. */
	void cancelTimer() {

	    if (_leaseTimer != null) {

		try {
		    _leaseTimer.cancel();
		} 
		catch (Exception e) {
		    _log4j.error("Exception cancelling timer for leaseID " +
				 _leaseID + ": " + e, e);
		}	  
		_leaseTimer = null;
	    }
	    else {
		_log4j.info("cancelTimer() - timer is already null");
	    }
	}

    } /* Lessee */

} /* LeaseManager */

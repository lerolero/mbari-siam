// MBARI copyright 2003
package org.mbari.siam.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.mbari.siam.distributed.SleepRollCallListener;
import org.mbari.siam.utils.SyncProcessRunner;

import org.apache.log4j.Logger;

/**
SleepManager is a singleton object that manages the transition
of the CPU into and out of sleep mode.  It determines when it's OK
to sleep, turns off any circuitry necessary, and puts the CPU to
sleep.  On wakeup, it reinitializes any circuitry necessary.

This is a generic version that calls an external script to put the
CPU to sleep.  It may be sub-classed for specific hardware.  

@author Bob Herlien
*/
public class SleepManager extends Thread
{
    /** Name of property indicating environmental service registry name */
    public static final String PROP_PREFIX = "SleepManager.";

				// Following are in seconds
    public static final int DFLT_WAKEUP_TIME = 3;	 //Early wakeup time
    public static final int DFLT_MIN_SLEEP_TIME = 3;    //Min time to sleep, else stay awake
    public static final int DFLT_MIN_AWAKE_TIME = 60;   //Min time to stay awake
    public static final int DFLT_POLL_TIME = 2;	 //How often to check if OK to sleep
    //Delay b4 going to sleep.  This one's in milliseconds
    public static final int DFLT_SLEEP_DELAY = 500;

    // Keep track of instances created - only one allowed
    protected static int _instanceCount = 0;
    protected static SleepManager _instance = null;
    private static Logger _log4j = Logger.getLogger(SleepManager.class);
    protected Thread _testThread = null;
    //    protected boolean _runSleepThread = true;
    protected NodeManager _nodeManager = null;
    protected NodeProperties _nodeProperties;
    protected boolean _sleepManagerEnabled = false;
    protected String _sleepString = "/root/suspend ";
    protected int _wakeupTime = DFLT_WAKEUP_TIME;
    protected int _pollTime = DFLT_POLL_TIME;
    protected int _minSleepTime = DFLT_MIN_SLEEP_TIME;
    protected int _minAwakeTime = DFLT_MIN_AWAKE_TIME;
    protected int _sleepDelay = DFLT_SLEEP_DELAY;
    protected long _lastWakeupTime = 0;
    protected Vector _sleepRollCallList;
    protected boolean _debug = true;
    protected SimpleDateFormat _dateFormatter = null;
    protected SyncProcessRunner _procRunner = null;
    public SleepLog _log = new SleepLog();

    /** Constructor just ensures one instance and creates SleepThread (unless
     SleepManager.enabled is false */

    public SleepManager()
    {
      // Only allowed to create one instance
	if (++_instanceCount > 1)
	{
	    _log4j.error("Only ONE instance of SleepManager allowed!");
	    return;
	}

	_log4j.info("SleepManager constructor");
	_instance = this;
	_nodeManager = NodeManager.getInstance();
	_nodeProperties = _nodeManager.getNodeProperties();
	_sleepRollCallList = new Vector();
	String enblString = _nodeProperties.getProperty(PROP_PREFIX+"enabled");

	if (enblString != null)
	    _sleepManagerEnabled = enblString.equalsIgnoreCase("TRUE");

	_procRunner = new SyncProcessRunner();
	_dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    } /* SleepManagerImpl() */

    /** Method to get singleton instance. */
    public static synchronized SleepManager getInstance()
    {
	if (_instanceCount < 1)
	    _instance = new SleepManager();
	return _instance;
    }

    /** Add a SleepRollCallListener to the callback list.	*/
    public void addSleepRollCallListener(SleepRollCallListener listener)
    {
	synchronized(_sleepRollCallList)
	{
	    _sleepRollCallList.add(listener);
	}
    }

    /** Loop forever, polling for whether it's time to put CPU to sleep */
    public void run()
    {
	int	sleepTime;

	_log4j.debug("SleepManager thread starting, enabled=" + 
		     _sleepManagerEnabled + ", NodeManager = " + 
			  _nodeManager.toString());

	getSleepManagerProperties();

	while (true) {

	    if (!_sleepManagerEnabled) {

		    try {
			Thread.sleep(10000);
		    }
		    catch (InterruptedException e) {
		    }

		    continue;
		}

	    try
	    {
		Thread.sleep(_pollTime * 1000);

		sleepTime = okToSuspend()-_wakeupTime-_sleepDelay/1000;

		if (sleepTime >= _minSleepTime)
		{

		    // Delay to allow file system to flush
		    if (_sleepDelay > 0)
			Thread.sleep(_sleepDelay);

		    long start = System.currentTimeMillis();

		    _log4j.info(
				 _dateFormatter.format(new Date(start)) +
				 " Going to sleep for " + sleepTime + " sec");

		    doSuspend(sleepTime);

		    postSuspend();

		    long now = System.currentTimeMillis();
		    _lastWakeupTime = now;
		    _log.add(new SleepLogEntry(start, sleepTime, 
					       (int )(now - start)/1000));
		}
	    } catch (Exception e) {
		_log4j.error("SleepThread.run(): ", e);
	    }
	} /* while */
    } /* run() */


    /** Get our properties from the NodeProperties object	*/
    private void getSleepManagerProperties()
    {
	String newString;

	if ((newString = _nodeProperties.getProperty(
			   PROP_PREFIX+"sleepString")) != null)
	{
	    // remove any '\n', so we can append sleep time
	    int index = newString.indexOf('\n');
	    if (index > 0)
		_sleepString = newString.substring(0, index).trim();
	    else
		_sleepString = newString.trim();
	}

	_wakeupTime    = _nodeProperties.getIntegerProperty(
			    PROP_PREFIX+"wakeupSeconds", DFLT_WAKEUP_TIME);
	_pollTime      = _nodeProperties.getIntegerProperty(
			    PROP_PREFIX+"pollSeconds", DFLT_POLL_TIME);
	_minSleepTime  = _nodeProperties.getIntegerProperty(
			    PROP_PREFIX+"minSleepSeconds",
			    DFLT_MIN_SLEEP_TIME);
	_minAwakeTime  = _nodeProperties.getIntegerProperty(
			    PROP_PREFIX+"minAwakeSeconds",
			    DFLT_MIN_AWAKE_TIME);
	_sleepDelay    = _nodeProperties.getIntegerProperty(
			    PROP_PREFIX+"sleepDelay",
			    DFLT_SLEEP_DELAY);

	_log4j.debug("SleepManager sleepString = " + _sleepString);
	_log4j.debug("SleepManager wakeupTime = " + _wakeupTime);

    } /* getSleepManagerProperties() */


    /** Go to sleep for fixed time		*/
    public boolean doSuspend(int seconds)
    {
	_log4j.debug("doSuspend() = procRunner will wait for " + 
		      (1000 * (seconds + 5)) + " msec");
	try
	{
	    _procRunner.exec(_sleepString + " " + seconds);
	    _procRunner.waitFor(1000 * (seconds + 60));
	    String pro=_procRunner.getOutputString();
	    if(pro!=null)
		_log4j.info(pro);
	    _log4j.info("Awake:  " +
	       _dateFormatter.format(new Date(System.currentTimeMillis())));

	} catch (Exception e) {
	    _log4j.error("SleepManager.doSuspend(): ", e);
	    return(false);
	}
	return(true);

    } /* doSuspend() */

    /** Do any necessary cleanup after returning from suspend */
    public void postSuspend()
    {
	// This does a notify() to all Scheduler's SiamTimers.  
	// Fixes a problem where Timers wait too long, due to 
	// Object.wait(timeout) not recognizing
	// the time that expires while we're sleeping.
	SiamTimer.recalculate();
    }

    /** Return earliest wakeup time reported by any of the rollcall 
	listeners. */
    private long doSleepRollCall()
    {
	long thisTime;
	long wakeupTime = Long.MAX_VALUE;

	synchronized(_sleepRollCallList)
	{
	    Enumeration e = _sleepRollCallList.elements();
	    while(e.hasMoreElements())
	    {
		SleepRollCallListener listener = 
		    (SleepRollCallListener )e.nextElement();

		thisTime = listener.okToSleep();

		if (thisTime <= 0) {
		    return(0);
		}

		if (thisTime < wakeupTime) {
		    wakeupTime = thisTime;
		}
	    }
	}
	return(wakeupTime);
    }

    /** Check to see if it's OK to suspend.
       Gets times from SleepRollCall and SiamTimer as long milliseconds,
       and returns time to next wakeup as int seconds	*/
    private int okToSuspend()
    {
	long		rollCallTime, sleepTime;

	// Guarantee _minAwakeTime
	if ((System.currentTimeMillis() - _lastWakeupTime)/1000 < _minAwakeTime)
	    return(0);

	// Get earliest wakeup time from any of the rollcall listeners
	if ((rollCallTime = doSleepRollCall()) <= 0)
	    return(0);

	// Static call checks all SiamTimers to see which is next to run
	sleepTime = SiamTimer.nextScheduledTask();

	if (rollCallTime < sleepTime)
	    sleepTime = rollCallTime;

	if (sleepTime > 0) {
	    _log4j.debug("okToSuspend returning " + sleepTime/1000);
	}

	return((int)(sleepTime/1000));

    } /* okToSuspend() */


	
    /** Return true if sleep manager is enabled, else false. */
    public boolean enabled() {
	return _sleepManagerEnabled;
    }


    /** Enable or disable */
    public void set(boolean enable) {
	_sleepManagerEnabled = enable;
    }


    /** Sleep log entry records when sleep starts, and how long it lasts. */
    public class SleepLogEntry {
	private long _startTime;
	private int _predictedSec;
	private int _actualSec;

	public SleepLogEntry(long startTime, int predictedSec, int actualSec) {
	    _startTime = startTime;
	    _predictedSec = predictedSec;
	    _actualSec = actualSec;
	}

	public String toString() {
	    return "sleep," +
		_dateFormatter.format(new Date(_startTime)) + "," +
		_predictedSec + "," + _actualSec;
	}
    }

    /** Collection of SleepLogEntry objects. */
    public class SleepLog {
	long _logStartTime;
	Vector _entries;

	/** Create a new SleepLog. */
	public SleepLog() {
	    _entries = new Vector();
	    reset();
	}

	/** Reset the log. */
	public void reset() {
	    _entries.clear();
	    _logStartTime = System.currentTimeMillis();
	}

	/** Add an entry to the log. */
	public void add(SleepLogEntry entry) {
	    _entries.add(entry);
	}


	/** Return String representation. */
	public String toString() {
	
	    StringBuffer summary = 
		new StringBuffer("<sleepLog>\nSleep log since " + 
				 _dateFormatter.format(new Date(_logStartTime))
				 +  "\nstart,predictedDurSec,actualDurSec\n");

	    for (int i = 0; i < _entries.size(); i++) {
		SleepManager.SleepLogEntry entry = 
		    (SleepManager.SleepLogEntry )_entries.elementAt(i);

		summary.append(entry.toString() + "\n");
	    }
	    summary.append("</sleepLog>\n");
	    return new String(summary);
	}
    }
}



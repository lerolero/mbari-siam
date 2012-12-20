// MBARI copyright 2002
package org.mbari.siam.core;

import org.mbari.siam.distributed.ScheduleSpecifier;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
/**
   The ScheduleTimerTask is the SiamTimerTask managed by each ScheduleTask.
   It sets things in motion when the timer goes off.
*/

public class ScheduleTimerTask extends SiamTimerTask {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(ScheduleTimerTask.class);

    /** ScheduleTask that owns this JobRunner */
    ScheduleTask _owner=null;

    public ScheduleTimerTask(){
	super();
    }
    public ScheduleTimerTask(ScheduleTask owner){
	super();
	_owner=owner;
    }

    /** Set _owner member */
    public void setOwner(ScheduleTask owner){
	_owner=owner;
    }

    /** Action Performed on Timer expiration */
    public void run(){
	// Check the mask/spec and run if it's time
	// Used to call isReady(), which checks time as well
	// as mask state, but this can create a race condition
	// if it we call System.currentTimeMillis a bit too late.
	// Also, isReady() used to get the time on entry, which 
	// stacks the odds against in the race.
	// Because we are here, the timer has expired, and this
	// implies that it IS time, so all we really need to do is
	// check that we are not masked off for some other reason
	// (e.g., already running, counter expired, etc.). So now
	// we call isMasked() instead of isReady(time)

	if( !_owner.isMasked() ){
	    try{
		_owner.setState(ScheduleTask.EXECUTING);
		_owner.execute();
		_owner.setState(ScheduleTask.WAITING);
	    }catch(Throwable e){
		_log4j.error("ScheduleTimerTask.run() Caught Exception "+e);
		_owner.setState(ScheduleTask.WAITING);
	    }
	    // Decrement cycle counter
	    long cycles=_owner.getLongCycles();
	    if(cycles > 0L)
		_owner.setLongCycles(--cycles);
	    
	    _log4j.debug("ScheduleTimerTask.run: cycles="+cycles);
	}else
	    _log4j.debug("skipping "+_owner.get(ScheduleSpecifier.JOB));

	// reset absolute timer according to next
	// scheduled time
	if(_owner.isAbsolute()) {
	    _owner.resetAbsoluteTimer();
	}
    }

    /**
     If executing thread is sleeping, return time (msec since epoch) at 
     which task will resume. If executing thread is not sleeping, return
     0.
    */
    public long sleepingUntil() {
	return _owner.sleepingUntil();
    }
}


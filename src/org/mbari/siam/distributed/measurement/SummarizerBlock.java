/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/

package org.mbari.siam.distributed.measurement;

import org.mbari.siam.core.ScheduleOwner;
import org.mbari.siam.core.ScheduleTask;
import org.mbari.siam.core.ScheduleTaskImpl;
import org.mbari.siam.core.Scheduler;
import org.mbari.siam.distributed.jddac.SiamRecord;
import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.fblock.Entity;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;

import java.util.Collection;
import java.util.Vector;

/** 
 * SummarizerBlock summarizes a series of Measurements. The summary is 
 * computed either on a specified schedule, or on every nth sample. 
 */
abstract public class SummarizerBlock extends FunctionBlock 
        implements ScheduleOwner {

    public static final String SCHEDULE_NAME = "summary";
    public static final String OpIdAddSample = "add sample";
    public static final String OpIdAddListener = "add listener";

    /** Log4j logger */
    private static final Logger log4j = Logger.getLogger(SummarizerBlock.class);

    /** Keep track of total number of added samples. */
    int _totalSamplesAdded = 0;

    /** Indicates summarization on every nth sample, if > 0. */
    int _everyNthSample = 0;

    /** Indicates summarization on specified schedule, if non-null. */
    ScheduleTask _scheduledTask = null;
    Vector _scheduledTasks = new Vector();
    Scheduler _scheduler = Scheduler.getInstance();

    /** We store "listener" F-Blocks here. Don't yet know how to use 
     JDDAC pub-sub, so instead we use client-server method. When this
     SummarizerBlock computes a summary, it will notify all of the 
     "listener" F-Blocks via their "perform" method. 
     HOW DOES JDDAC KEEP TRACK OF SERVICES TO CALL??? */
    Vector _listeners = new Vector();

    /** Create SummarizerBlock, generate summary on specified schedule. */
    public SummarizerBlock(ScheduleSpecifier schedule) 
	throws ScheduleParseException {
	setSchedule(schedule);
    }


    /** Create SummarizerBlock, generate summary on every nth sample. */
    public SummarizerBlock(int everyNthSample) {
	summarizeEveryNthSample(everyNthSample);
    }


    /** Summarize on every nth sample. */
    final public void summarizeEveryNthSample(int everyNthSample) {

	// Remove scheduled task if it exists
	if (_scheduledTask != null) {
	    _scheduler.removeSchedule(_scheduledTask);
	    _scheduledTask = null;
	}

	_everyNthSample = everyNthSample;
    }


    /** Summarize on specified schedule. */
    final public void setSchedule(ScheduleSpecifier schedule) 
	throws ScheduleParseException {

	// Don't summarize on every nth sample.
	_everyNthSample = 0;

	// Remove existing scheduled task
	if (_scheduledTask != null) {
	    _scheduler.removeSchedule(_scheduledTask);
	}

	// Create new scheduled task
	_scheduledTask = new ScheduleTaskImpl(SCHEDULE_NAME, schedule, this);
	_scheduledTask.setOwnerName((this.getClass().getName() + ":dummy"));
				     
	_scheduledTasks.clear();
	_scheduledTasks.add(_scheduledTask);
	_scheduler.addSchedule(_scheduledTask, true);
    }


    /** Set state to ACTIVE; if summary is generated on a schedule, this 
     method activates the schedule timer. */
    public void goActive() 
	throws OpException {
	super.goActive();
	if (_scheduledTask != null) {
	    // Activate the schedule timer
	    _scheduledTask.resetTimer();
	}
    }

    /** Return unique owner ID. */
    final public Scheduler.ScheduleKey getScheduleKey() {
	return Scheduler.getScheduleKey(this);
    }

    /** Return all owned schedules. */
    final public Collection getAllSchedules() {
	return _scheduledTasks;
    }


    /** Execute scheduled task, which is to compute summary. */
    final public void doScheduledTask(ScheduleTask task) {
	log4j.debug("doScheduledTask() - compute summary");
	SiamRecord summary = computeSummary();
	log4j.debug("Computed summary: " + summary);
	updateListeners(summary);

	// Reset summary
	resetSummary();
    }
    


    /** Add or replace schedule. Return integer code defined by Scheduler. */
    public int addSchedule(String name, String specifier, boolean overwrite) {
	log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }

    /** "Synchronize" specified schedule. Return integer code defined by 
	Scheduler.*/
    public int syncSchedule(String name, long delay) {
	log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }

    /** Remove all schedules. Return integer code defined by Scheduler. */
    public int removeAllSchedules() {
	log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }

    /** Remove specified schedule. Return integer code defined by Scheduler. */
    public int removeSchedule(String name) {
	log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }


    /** Suspend specified schedule.Return integer code defined by Scheduler. */
    public int suspendSchedule(String name) {
	log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }


    /** Resume specified schedule. Return integer code defined by Scheduler. */
    public int resumeSchedule(String name) {
	log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }

    /** This is the F-block's public "client-server" interface method. */
    synchronized public ArgArray perform(String operationID, 
				  ArgArray input) 
	throws OpException, Exception {

	if (operationID.equals(OpIdAddSample)) {
	    if (input instanceof SiamRecord) {
		addSample((SiamRecord )input);
		return null;
	    }
	    else {
		throw new OpException("Input is not a SiamRecord");
	    }
	}
	else {
	    return super.perform(operationID, input);
	}
    }


    /** Add sample record to the summary. */
    synchronized final protected void addSample(SiamRecord record) 
	throws Exception {

	// Call subclass-implemented method
	addSampleRecord(record);

	// Keep track of total number of samples added
	++_totalSamplesAdded;

	log4j.debug("addSample() - totalSamplesAdded=" + _totalSamplesAdded);

	// Check for summary computation on every nth sample
	if (_everyNthSample > 0 && 
	    (_totalSamplesAdded % _everyNthSample == 0)) {
	    // Time to compute summary
	    log4j.debug("Got " + _totalSamplesAdded + 
			 " samples; compute summary");

	    SiamRecord summary = computeSummary();
	    log4j.debug("Computed summary: " + summary);

	    updateListeners(summary);

	    // Reset summary
	    resetSummary();
	}
    }


    /** Send summary to all listeners. */
    private void updateListeners(SiamRecord summary) {
	for (int i = 0; i < _listeners.size(); i++) {
	    FunctionBlock listener = (FunctionBlock )_listeners.elementAt(i);
	    try {
		listener.perform(Entity.PerformInputArg, summary);
	    }
	    catch (Exception e) {
		log4j.error("updateListeners() - got Exception: " + e);
	    }
	}
    }

    /** Input data is received from publishers here. */
    final public void notifySubscriber(short publicationID,
				 String topic,
				 ArgArray payload) throws OpException {

	if (payload instanceof SiamRecord) {
	    
	    try {
		addSample((SiamRecord )payload);
	    }
	    catch (Exception e) {
		throw new OpException(e.getMessage());
	    }
	}
	else {
	    throw new OpException("payload is not a SiamRecord");
	}
    }

    /** If execution thread of specified task is sleeping, return time
	at which it will resume; otherwise return 0. */
    public final long sleepingUntil(ScheduleTask task) {
	log4j.warn("sleepingUntil() not implemented");
	return 0;
    }


    /** Add a "listener" F-Block; when the Summarizer computes a result,
     it will invoke each listener's perform() method. HOW DOES JDDAC ADD
     LISTENERS???*/
    public void addListener(FunctionBlock listener) {
	_listeners.add(listener);
    }

    /** Add specified sample record; subclass must implement. */
    abstract protected void addSampleRecord(SiamRecord record) 
	throws Exception;

    /** Compute summary and return result in a Record. */
    abstract public SiamRecord computeSummary();

    /** Reset summary results. */
    abstract public void resetSummary();

}



// MBARI copyright 2002
package org.mbari.siam.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;


/**
   - Scheduler: provides application global view of schedules, supporting operations like showSchedule and getNextScheduledJob 
   - ScheduleTaskImpl: an implementation of ScheduleTask, which encapsulates scheduling timing and control
   - ScheduleSpecifier: cron-like representation of schedules
   - Scheduler.ScheduleKey: an inner class of Scheduler, a unique ScheduleOwnerID used in some contexts to resolve ownership of ScheduleTasks (e.g., to differentiate between two ScheduleTasks with the same name, owned by objects of the same class).
   - ScheduleOwner: an interface that must be fulfilled by things that use schedules

   To use a schedule, a class must:
   - implement ScheduleOwner (an interface with 3 methods)
   - obtain a ScheduleKey object 
   - create one or more ScheduleTasks
   - register the ScheduleTasks with the Scheduler
   - start the schedules running (or have them started by something other class)
   - optionally provide any accessor methods required by the application

Scheduler is a cron-like class that schedules periodic events like sampling, using an event/listener model.
// HashMap is not inherently synchronized; synchronization must
// be manually provided when performing structural mods to the HashMap.
// Failure to do this synchronization may result in 
// undeterministic behavior

	Set s = _schedules.keySet();
	synchronized(_schedules) {  // Synchronizing on _schedules, not s!
	    Iterator i = s.iterator(); // Must be in synchronized block
	    while (i.hasNext()){
	    }
	}

NewScheduler differs from Scheduler as follows:
- 

Objects that wish to receive notification of various events must implement the Schedulable interface, which requires an implementation of SchedulerListener, which extends EventListener.

*/
public class Scheduler{

    static private Logger _log4j = Logger.getLogger(Scheduler.class);

    //Constants
    public static boolean OVERWRITE=true;
    public static boolean PRESERVE=false;
    public static final int OK=0;
    public static final int ALREADY_EXISTS=-1;
    public static final int NOT_FOUND=-2;
    public static final int UNDEFINED=-3;
    public static final int INVALID_SCHEDULE=-4;

    /** Max seconds to look ahead for next absolute event */
    public static final long MAX_LOOKAHEAD_SEC=3600;

    // Data Members
    public static long _nextKey=1L;

    /** True if defaults have been loaded */
    protected static boolean _defaultsLoaded=false;

    /** Schedule (analagous to crontab) Collection */
    private static Map _schedules = Collections.synchronizedMap(new HashMap());

    /** the scheduler instance */
    public static Scheduler _theScheduler = null;

    // Constructors
    public Scheduler(){
	super();
    }

    // Methods
    /** Get Collection of Schedules */
    public Map getSchedules(){
	return _schedules;
    }

    /** Get Scheduler instance */
    public synchronized static Scheduler getInstance(){
	if ( _theScheduler == null ){
	    _theScheduler=new Scheduler();
	}
	return _theScheduler;
    }

    /** Print error message */
    public static String getStatusString(int status){
	switch(status){
	case OK:
	    return "OK";
	case ALREADY_EXISTS:
	    return "Schedule already exists";
	case NOT_FOUND:
	    return "Schedule not found";
	case UNDEFINED:
	    return "Undefined error";
	case INVALID_SCHEDULE:
	    return "Invalid schedule";
	default:
	    return "Unknown status";
	}
    }

    /** Add schedules with overwrite */
    public synchronized int setSchedules(Collection schedules){
	int retval=OK;
	
	for(Iterator e=schedules.iterator();e.hasNext();){
	    ScheduleTask st = (ScheduleTask)e.next();
	    int test=setSchedule(st);
	    if(test!=Scheduler.OK)
		retval=test;
	}
	return retval;
    }

    /** Remove multiple schedules */
    public synchronized int removeSchedules(Collection schedules){
	int retval=OK;
	
	for(Iterator e=schedules.iterator();e.hasNext();){
	    ScheduleTask st = (ScheduleTask)e.next();
	    int test=removeSchedule(st);
	    if(test!=Scheduler.OK)
		retval=test;
	}
	return retval;
    }

    /** Add schedule with overwrite */
    public synchronized int setSchedule(ScheduleTask schedule){
	return addSchedule(schedule,true);
    }

    /** Add schedule with optional overwrite */
    public synchronized int addSchedule(ScheduleTask schedule, boolean overwrite){

	Integer newKey = new Integer(schedule.hashCode());
	_log4j.debug("Scheduler.addSchedule(): schedule newKey (hash)="+newKey);

	boolean isMatch=false;
	ScheduleTask match= (ScheduleTask)_schedules.get(newKey);

	if(match != null)
	    isMatch=true;

	if(isMatch==true && overwrite==false){
	    _log4j.debug("addSchedule(): schedule exists");
	    return ALREADY_EXISTS;
	}
	if(isMatch==true){
	    _log4j.debug("Scheduler.addSchedule(): schedule exists");
	}
	if(isMatch==false || (isMatch==true && overwrite==true)){
	    // HashMap is not inherently synchronized; synchronization must
	    // be manually provided when performing structural mods to the HashMap.
	    // Failure to do this synchronization may result in 
	    // undeterministic behavior
	    synchronized(_schedules){
		// remove matching schedule, if it exists
		_schedules.remove(match);
		_schedules.put(newKey,schedule);
		schedule.setKey(newKey);
		// Don't start the schedule, leave it up to
		// the calling ScheduleOwner;
		// but should wait for Scheduler.serviceInstalled() to start)
		_log4j.debug("Added schedule "+schedule.getName());
	    }
	}
	return OK;	
    }


    /** Remove schedule file (scheduleFile is name only; no path) */
    public synchronized int removeSchedule(ScheduleTask schedule){

	Integer key = schedule.getKey();

	if(!_schedules.containsKey(schedule.getKey())){
	    _log4j.warn("Scheduler: Could not find schedule");
	    return NOT_FOUND;	    
	}

	_log4j.debug("Scheduler: Found schedule");
	ScheduleTask st = (ScheduleTask)_schedules.get(key);

	_log4j.debug("Scheduler: Cancelling timer for "+st.getName());
	st.cancelTimer();

	_log4j.debug("Scheduler: Removing schedule");
	synchronized(_schedules){
	    _schedules.remove(key);
	}

	_log4j.debug("Scheduler: Schedule removed");
	return OK;
    }

    /** Show schedule file */
    public String showSchedule(){
	return showSchedule(Scheduler.MAX_LOOKAHEAD_SEC);
    }

    /** Show all schedules  */
    public String showSchedule(long lookAheadSeconds){
	return showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC);
    }


    /** Show all schedules  */
    public String showSchedule(ScheduleKey key, long lookAheadSeconds){

	String retval = "";

	Set s = _schedules.keySet();
	synchronized(_schedules) {  // Synchronizing on _schedules, not s!
	    Iterator i = s.iterator(); // Must be in synchronized block
	    while (i.hasNext()){
		ScheduleTask entry = (ScheduleTask)_schedules.get(i.next());
		// do all schedules if service is null
		// or only schedules belonging to the specified service otherwise
		if(key==null){
		    retval+=entry.toString(lookAheadSeconds);
		    retval+="\n";
		}else{
		    if(entry.getOwner().getScheduleKey()==key){
			retval+=entry.toString(lookAheadSeconds);
			retval+="\n";
		    }
		}
	    }
	    return retval;
	}
    }

    /** Get next Scheduled job */
    public long getNextScheduledJob(){
	return getNextScheduledJob(MAX_LOOKAHEAD_SEC);
    }

    /** Get next Scheduled job */
    public synchronized long getNextScheduledJob(long lookAheadSeconds){	
	long nextTime = Long.MAX_VALUE;

	Set s = _schedules.keySet();
	synchronized(_schedules) {  // Synchronizing on _schedules, not s!
	    Iterator i = s.iterator(); // Must be in synchronized block
	    while (i.hasNext()){
		ScheduleTask st = (ScheduleTask)_schedules.get(i.next());
		long thisTime = st.timeRemaining(lookAheadSeconds);
		if(thisTime<nextTime && thisTime>=0)
		    nextTime=thisTime;
	    }
	}
	return nextTime;
    }

    /** Get SchedulerProperties */
    public static Properties getSchedulerProperties(){

        //create new properties object and load port props
        Properties _nodeProps = new Properties();
	String _siamHome;

        //determine siam home location
        _siamHome = getSiamHome();

	// load properties
	try{
	    FileInputStream in = 
		new FileInputStream(_siamHome + "/properties/scheduler.cfg");
	    _nodeProps.load(in);
	    in.close();
	}catch(FileNotFoundException e){
	    _log4j.error("NodeManager: FileNotFound "+e);
	}catch(IOException e){
	    _log4j.error("NodeManager: IOException "+e);
	}
	return _nodeProps;
    }

    /** Get SiamHome Directory */
    public static String getSiamHome(){
        //determine siam home location
	return System.getProperties().getProperty("siam_home").trim();
    }

    /** Get Schedule Directory */
    public static String getScheduleDirectory(){
        Properties _nodeProps = getSchedulerProperties();
        return _nodeProps.getProperty("scheduleDirectory").trim();
    }

    /** Get Default System Schedule name */
    public String getDefaultSystemScheduleName(){
        Properties _nodeProps = getSchedulerProperties();
        return _nodeProps.getProperty("defaultSystemSchedule").trim();
    }

    /** Get Default Sample Schedule name */
    public String getDefaultSampleScheduleName(){
        Properties _nodeProps = getSchedulerProperties();
        return _nodeProps.getProperty("defaultSampleSchedule").trim();
    }

    /** Get Full Schedule Path ($SIAM_HOME/scheduleDirectory)*/
    public static String getSchedulePath(){

	String siamHome = getSiamHome();
	String scheduleDirectory = getScheduleDirectory();

	// Trim leading and trailing "/" from scheduleDirectory
	while(scheduleDirectory.startsWith("/"))
	    scheduleDirectory=scheduleDirectory.substring(scheduleDirectory.indexOf("/")+1);

	while(scheduleDirectory.endsWith("/"))
	    scheduleDirectory=scheduleDirectory.substring(0,scheduleDirectory.lastIndexOf("/"));

	//Don't trim leading "/" from siamHome...it may be under root

	while(siamHome.endsWith("/"))
	    siamHome=siamHome.substring(0,siamHome.lastIndexOf("/"));
	
        return (siamHome+"/"+scheduleDirectory);
    }

    /** Load default schedules (System, Sampling) */
    public static void loadDefaults(){

	if(_defaultsLoaded==true)
	    return;

	Scheduler s = Scheduler.getInstance();

	// Load System schedule(s) and other defaults...

	_defaultsLoaded = true;

	return;
    }

//    /** Refresh all schedules (after sleep; test method) */
//    public void refreshSchedules(){
//	Set s = _schedules.keySet();
//	synchronized(_schedules) {  // Synchronizing on _schedules, not s!
//	    Iterator i = s.iterator(); // Must be in synchronized block
//	    while (i.hasNext()){
//		ScheduleTask st = (ScheduleTask)_schedules.get(i.next());
//		st.getTimer().forceNotify();
//	    }
//	}
//    }

    /**   Scheduler.ScheduleKey: an inner class of Scheduler, a unique ScheduleOwnerID used in some contexts to resolve ownership of ScheduleTasks (e.g., to differentiate between two ScheduleTasks with the same name, owned by objects of the same class).

	  To avoid multiple schedule keys, ScheduleOwners should only call Scheduler.ScheduleKey.getScheduleKey only once and never null out it's ScheduleKey member.


     */
    public final class ScheduleKey{
	private final long _key;
	private ScheduleKey(long key){
	    _key  = key;
	}
	public long value(){
	    return _key;
	}
	public String toString(){
	    return Long.toString(_key);
	}
    }

    private final ScheduleKey nextScheduleKey(){	
	return new ScheduleKey(_nextKey++);
    }

    public static final ScheduleKey getScheduleKey(ScheduleOwner owner){
	if(owner.getScheduleKey()!=null)
	    return owner.getScheduleKey();
	Scheduler s=Scheduler.getInstance();
	return s.nextScheduleKey();
    }

    public static void main(String args[]){
	Scheduler s = Scheduler.getInstance();


    }
}



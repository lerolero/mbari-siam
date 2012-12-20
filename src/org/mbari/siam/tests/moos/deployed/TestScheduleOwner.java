// MBARI copyright 2002
package org.mbari.siam.tests.moos.deployed;

import java.util.Collection;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Map;

import org.mbari.siam.core.ScheduleOwner;
import org.mbari.siam.core.Scheduler;
import org.mbari.siam.core.ScheduleTask;
import org.mbari.siam.core.ScheduleTaskImpl;
import org.mbari.siam.core.Scheduler.ScheduleKey;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
   TestScheduleOwner is a class to demonstrate the use of the Scheduler and related classes:

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


@author Kent Headley
@see Scheduler
@see ScheduleTaskImpl
@see ScheduleSpecifier
@see ScheduleOwner

*/

public class TestScheduleOwner implements ScheduleOwner{

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(TestScheduleOwner.class);

    /** Container for this classes schedules */
    Vector _schedules=new Vector();

    /** This class' unique scheduler key */
    ScheduleKey _scheduleKey;

    /** Empty Constructor */
    public TestScheduleOwner(){
	super();
    }

    /** One of the methods this class wants to execute on a schedule */
    public void theDefaultThingToDo(ScheduleTask st){
	_log4j.debug("key "+_scheduleKey.value()+" hash:"+st.getKey()+" Doing my default scheduled thing");
    }

    /** Another of the methods this class wants to execute on a schedule */
    public void theAlternativeThingToDo(ScheduleTask st){
	_log4j.debug("key "+_scheduleKey.value()+" hash:"+st.getKey()+" Doing my alternative scheduled thing");
    }

    /** A factory method to generate ScheduleTasks 
	(Not strictly necessary; can do this on the fly) 
    */
    public ScheduleTask createTask(String name, ScheduleSpecifier schedule, ScheduleOwner parent){
	try{
	    ScheduleTaskImpl newTask = new ScheduleTaskImpl(name,schedule,parent);
	    newTask.setOwnerName(this.getClass().getName());
	    return newTask;
	}catch(ScheduleParseException e){
	    _log4j.error("createTask:"+e);
	}
	return null;
    }

    /** Create schedules and register them with the scheduler. */
    public void setupSchedules(String dfltSchedule, String altSchedule){

	// Get a Scheduler instance
	Scheduler s = Scheduler.getInstance();

	// Get our unique schedule key from the Scheduler
	_scheduleKey = s.getScheduleKey(this);

	try{
	    // Create a couple of different schedule tasks
	    ScheduleTask defaultScheduleTask = defaultScheduleTask = createTask("default",new ScheduleSpecifier(dfltSchedule),this);
	    ScheduleTask alternativeScheduleTask = alternativeScheduleTask = createTask("alternative",new ScheduleSpecifier(altSchedule),this);

	    // Add them to our own vector of schedules and
	    // register our schedules with the Scheduler (this does not begin to execute the schedules)
	    addSchedule(defaultScheduleTask,true);
	    addSchedule(alternativeScheduleTask,true);

	    // for demo, set the names to show owner name and key value
	    // must do this after giving them to the Scheduler b/c the 
	    // Scheduler sets the (hashcode) key (different from the ScheduleKey,
	    // which identfies the ScheduleOwner)
	    defaultScheduleTask.setOwnerName(defaultScheduleTask.getOwnerName()+":"+getScheduleKey().value()+" id:"+defaultScheduleTask.getKey());
	    alternativeScheduleTask.setOwnerName(alternativeScheduleTask.getOwnerName()+":"+getScheduleKey().value()+" id:"+alternativeScheduleTask.getKey());

	}catch(ScheduleParseException e){
	    _log4j.error(e);
	    e.printStackTrace();
	}

    }

    /** Get a schedule by name */
    public ScheduleTask getSchedule(String name){

	for(Enumeration e = _schedules.elements();e.hasMoreElements();){
	    ScheduleTask st = (ScheduleTask)e.nextElement();
	    if(st.getName().equals(name)){
		return st;
	    }
	}
	return null;
    }

    public int addSchedule(ScheduleTask schedule,boolean overwrite){
	Scheduler s = Scheduler.getInstance();
	ScheduleTask st = getSchedule(schedule.getName());

	if(st != null && overwrite==false)
	    return Scheduler.ALREADY_EXISTS;

	// doesn't exist or is OK to overwrite
	int i=Scheduler.UNDEFINED;
	if(st != null){
	    s.removeSchedule(st);
	    _schedules.remove(st);
	}
	if(schedule != null){
	    _schedules.add(schedule);
	    i=s.addSchedule(schedule,overwrite);
	}
	return i; 
    }

    /** Set schedule key (unique schedule owner ID) */
    public void setKey(ScheduleKey key){
	_scheduleKey=key;
    }

    public void badRemoveSchedule(String name){
	// This is BAD, because it doesn't let the 
	// Scheduler know that a schedule is being removed
	_schedules.remove(this.getSchedule(name));
    }

    ///////////////////////////////////////////////////// 
    // These methods fulfill the ScheduleOwner Interface
    /////////////////////////////////////////////////////
    /** Default method to execute */
    public void doScheduledTask(ScheduleTask task){

	// select which action we should take
	// based on the task name
	if(task.getName().equals("default")){
	    theDefaultThingToDo(task);
	}
	if(task.getName().equals("alternative")){
	    theAlternativeThingToDo(task);	
	    //_log4j.debug("Next task in "+Scheduler.getInstance().getNextScheduledJob());
	}
    }

    /** Return schedules */
    public Collection getAllSchedules(){
	return _schedules;
    }

    /** Return schedule key (unique schedule owner ID) */
    public ScheduleKey getScheduleKey(){
	return _scheduleKey;
    }


    /** Add or replace schedule. Return integer code defined by Scheduler. */
    public int addSchedule(String name, String specifier, boolean overwrite) {
	_log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }

    /** "Synchronize" specified schedule. Return integer code defined by 
	Scheduler.*/
    public int syncSchedule(String name, long delay) {
	_log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }

    /** Remove all schedules. Return integer code defined by Scheduler. */
    public int removeAllSchedules(){

	int retval=Scheduler.getInstance().removeSchedules(_schedules);
	_log4j.debug("removeAllSchedules: Scheduler returned "+retval);
	_schedules.removeAllElements();
	return 0;
    }

    /** Remove specified schedule. Return integer code defined by Scheduler. */
    public int removeSchedule(String name) {
	ScheduleTask task = getSchedule(name);
	int i = Scheduler.OK;
	if (task != null) {
	    i = Scheduler.getInstance().removeSchedule(task);
	    _schedules.remove(task);
	} else
	    i = Scheduler.NOT_FOUND;
	return i;

	//_log4j.error("Not yet implemented");
	//return Scheduler.UNDEFINED;
    }


    /** Suspend specified schedule.Return integer code defined by Scheduler. */
    public int suspendSchedule(String name) {
	_log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }


    /** Resume specified schedule. Return integer code defined by Scheduler. */
    public int resumeSchedule(String name) {
	_log4j.error("Not yet implemented");
	return Scheduler.UNDEFINED;
    }

    /** If execution thread of specified task is sleeping, return time
	at which it will resume; otherwise return 0. */
    public long sleepingUntil(ScheduleTask task) {
	_log4j.error("sleepUntil() not yet implemented");
	return 0;
    }

    ////////// End ScheduleOwner methods /////////////////

    /** A demonstration of this class 
	Using schedules is easy, nutritious and fun for the whole family.
	Schedules...they're really neat!
     */
    public static void main(String args[]){
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);

	// Demonstrate two objects of the same class,
	// each with multiple schedules

	// Create first object...
	TestScheduleOwner tso = new TestScheduleOwner();

	// Create and install first object's schedules...
	tso.setupSchedules("2000","5000");

	// Create second object...
	TestScheduleOwner tso2 = new TestScheduleOwner();

	// Create and install second object's schedules...
	tso2.setupSchedules("3000","7000");


	// Start first object's schedules 
	// (could do this automatically using a ServiceEvent)
        for(Iterator e=tso.getAllSchedules().iterator();e.hasNext();){
	    ScheduleTask st = (ScheduleTask)e.next();
	    _log4j.info("Starting service "+st.getName());
	    st.sync();
	}

	// Start second object's schedules
        for(Iterator e=tso2.getAllSchedules().iterator();e.hasNext();){
	    ScheduleTask st = (ScheduleTask)e.next();
	    _log4j.info("Starting service "+st.getName());
	    st.sync();
	}

	// Print schedules
	_log4j.info("\nShowing Schedules\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));
	StopWatch.delay(10000);

	// Continues to execute since there are ScheduleTasks running
	_log4j.info("\nRemoving default the schedule the WRONG way\n(default still runs, persists in Scheduler list)");
	tso.badRemoveSchedule("default");

	// Print schedules
	_log4j.info("\nShowing Schedules\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));

	StopWatch.delay(10000);

	// Continues to execute since there are ScheduleTasks running
	_log4j.info("\nRemoving the schedule the RIGHT way");
	tso2.removeSchedule("default");

	// Print schedules
	_log4j.info("\nShowing Schedules\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));

	StopWatch.delay(10000);

	// Continues to execute since there are ScheduleTasks running
	_log4j.info("\nTrying to make a multiple, duplicate schedules\n(old default still runs, new default and alt don't run since they haven't been started)");
	tso.setupSchedules("2000","5000");
	_log4j.info("\nShowing Schedules\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));
	// do the same bad trick to create a second duplicate schedule
	// in the Scheduler
	tso.badRemoveSchedule("default");
	tso.setupSchedules("2000","5000");

	// It's not allowed for ScheduleOwners to remove schedules 
	// without telling the Scheduler by using ScheduleOwner.removeSchedule,
	// since it leaves those Schedules running and will 
	// display them in showSchedules; the ScheduleOwner no longer
	// has a reference to the deleted ScheduleTask.
	// The following code demonstrates how to recover from 
	// this.
	// Here is how to find and eliminate ScheduleTasks
	// in the Scheduler when the Schedule owner
	// had deleted the from its own vector of schedules
	// and no longer has a reference to them.
	// If the ScheduleOwner has a valid schedule, delete
	// any schedules with the same ScheduleKey and name
	// whose keys (hashcodes) don't match the valid 
	// Schedule
	_log4j.info("\nHunting down rogue schedules...");
	Collection c = Scheduler.getInstance().getSchedules().values();
	Vector  rogues=new Vector();
	
	for(Iterator e=c.iterator();e.hasNext();){
	    try{
		ScheduleTask st = (ScheduleTask)e.next();
		if(st.getName().equals("default") && st.getOwner().getScheduleKey()==tso.getScheduleKey()){
		    _log4j.info("found a possibly duplicate schedule "+st.getName()+":"+st.getKey()+" now");
		    if(st.getKey()!=tso.getSchedule("default").getKey())
			rogues.add(st);
		}
	    }catch(Exception x){
		x.printStackTrace();
		_log4j.error(x);
	    }
	}
	if(rogues.size()>0){
	    for(Iterator r=rogues.iterator();r.hasNext();){
		ScheduleTask st=(ScheduleTask)r.next();
		_log4j.info("Found rogue schedule "+st.getKey()+" giving it the chop");
		Scheduler.getInstance().removeSchedule(st);
	    }
	}
	_log4j.info("\nShowing Schedules\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));

	_log4j.info("\nRemoving all schedules for tso1");
	tso.removeAllSchedules();
	_log4j.info("Showing Schedules\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));

	_log4j.info("\nRemoving all schedules for tso2");
	tso2.removeAllSchedules();
	_log4j.info("\nShowing Schedules\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));

    }

}






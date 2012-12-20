// MBARI copyright 2002
package org.mbari.siam.core;

import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;

import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;

/**
The ScheduleTask is a standalone job that may be scheduled, encapsulating both
the represention and execution of the job.

The ScheduleTask has a 
- ScheduleEntry representing the schedule itself
- SiamTimerTask that does timing
- ScheduleTimerTask timer task that is associated (created and destroyed with) the 
  timer task

Subclasses should 
- implement the execute method, calling owner methods

@author Kent Headley
*/

public abstract class ScheduleTask implements Schedulable{

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(ScheduleTask.class);

    // Constants
    // Limits

    // ScheduleTask States
    /** Waiting */
    public static final int WAITING=0;
    /** Executing */
    public static final int EXECUTING=1;
    /** Suspended */
    public static final int SUSPENDED=2;
    /** Ready */
    public static final int READY=3;
    /** Counter Expired */
    public static final int COUNTER_EXPIRED=4;
    /** readyAction: RUN */
    public static final int RUN=1;
    /** readyAction: CHECK_TIME */
    public static final int CHECK_TIME=2;

    // String Constants

    // Class Data Members
    /** Name used for lookup */
    String _name;

    /** Name of owner (constructor sets to owner class name) */
    String _ownerName;
    
    /** Key used for programmatic lookup */
    Integer _key;

    /** String representing the line in the Schedule file */
    String _line=null;

    /** Schedule Specifier */
    ScheduleSpecifier _scheduleSpecifier=null;

    /** The ScheduleTask's calendar */
    Calendar _calendar;

    /** Timer */
    SiamTimer  _timer=new SiamTimer();

    /** TimerTask; This ScheduleTask can not extend SiamTimerTask
	because when timers are cancelled, the timer task as well as
	the timer must be destroyed; i.e., timer tasks cannot be reused.
	That is why we need TaskRunner
     */
    SiamTimerTask _timerTask;
  
    /** State */
    int _state;

    /** Action to do when timerTask expires */
    int _readyAction=RUN;

    /** ID of owner (the service that registered this entry) */
    long _ownerID=-1L;

    /** Owner if this schedule */
    ScheduleOwner _owner=null;
    
    public ScheduleTask(){
	super();
    }

    /** Constructor 
	@param schedule cron-like schedule
	@param name schedule name
	@param owner owner of this ScheduleTask
     */
    public ScheduleTask(String schedule, String name, ScheduleOwner owner) throws ScheduleParseException{
	parse(schedule);
	_owner = owner;
	_name = name;
    }

    /** Constructor 
	@param schedule ScheduleSpecifier
	@param name schedule name
	@param owner owner of this ScheduleTask
     */
    public ScheduleTask(ScheduleSpecifier schedule, String name, ScheduleOwner owner) throws ScheduleParseException{
	_scheduleSpecifier = schedule;
	_owner = owner;
	_name = name;
	_ownerName = owner.getClass().getName();
    }

    /** Constructor 
	@param interval sample interval
	@param name schedule name
	@param owner owner of this ScheduleTask
     */
    public ScheduleTask(long interval, String name, ScheduleOwner owner) throws ScheduleParseException{
	this(new ScheduleSpecifier(interval),name,owner);
    }

    public ScheduleOwner getOwner(){
	return _owner;
    }

    /** Get name of state */
    public String getStateName(int state){
	switch(state){
	case WAITING:
	    return "Waiting";
	case EXECUTING:
	    return "Executing";
	case SUSPENDED:
	    return "Suspended";
	case READY:
	    return "Ready";
	case COUNTER_EXPIRED:
	    return "Counter Expired";
	default:
	    return "Invalid State";
	}
    }

    public String getName(){
	return _name;
    }

    public void setName(String name){
	_name = name;
    }

    public String getOwnerName(){
	return _ownerName;
    }

    public void setOwnerName(String name){
	_ownerName = name;
    }

    public Integer getKey(){
	return _key;
    }

    public void setKey(int key){
	_key = new Integer(key);
    }
    public void setKey(Integer key){
	_key = key;
    }


    /** Get _scheduleSpecifier member */
    public ScheduleSpecifier getScheduleSpecifier(){
	return _scheduleSpecifier;
    }

    /** Set _scheduleSpecifier member */
    public void setSpecifier(ScheduleSpecifier spec){
	_scheduleSpecifier = spec;
	return;
    }

    /** Get _state member */
    public int getState(){
	return _state;
    }

    /** Set _state member */
    protected void setState(int state){
	_state=state;
	return;
    }

    /** getTimer */
    public SiamTimer getTimer(){
	return _timer;
    }
    /** setTimer */
    public void setTimer(SiamTimer timer){
	_timer=timer;
    }

    /** Set TimerTask */
    public void setTimerTask(SiamTimerTask task){
	_timerTask=task;
    }

    /** Get TimerTask */
    public SiamTimerTask getTimerTask(){
	return _timerTask;
    }

    /** Get (ScheduleSpecifier) period */
    public long getPeriod(){
	return _scheduleSpecifier.getPeriod();
    }

    // field accessor methods
    /** Get field */
    public String get(int field){
	return _scheduleSpecifier.get(field);
    }

    /** Get TimeZone Object */
    public TimeZone getTZ(){
	return _scheduleSpecifier.getTZ();
    }

    /** Get Long Cycles */
    public long getLongCycles(){
	return _scheduleSpecifier.getLongCycles();
    }

    /** Set Long Cycles */
    public void setLongCycles(long cycles){
	_scheduleSpecifier.setLongCycles(cycles);
    }

    /** Get owner ID */
    public long getOwnerID(){
	return _ownerID;
    }

    /** Set Long Cycles */
    public void setOwnerID(long id){
	_ownerID=id;
	try {
	    _timer.setThreadName("sampler-" + id);
	}
	catch (SecurityException e) {
	    _log4j.error(e);
	}
    }

    /** Return true if schedule type is absolute */
    public boolean isAbsolute(){
	return _scheduleSpecifier.isAbsolute();
    }

    /** Return true if schedule type is relative */
    public boolean isRelative(){
	return _scheduleSpecifier.isRelative();
    }

    /** Return true is calendar matches the schedule spec */
    public boolean isSelectedTime(Calendar calendar){
	return _scheduleSpecifier.isSelectedTime(calendar);
    }

    /** parse a line from the schedule file.
	Returns parsed ScheduleTask object (itself).
    */
    public  ScheduleTask parse(String line) throws ScheduleParseException{
    
	// Throw away leading/trailing whitespace
	line.trim();

	// Throw away comments
	StringTokenizer lt = new StringTokenizer(line,"#");
	if(lt.hasMoreTokens())
	    line=lt.nextToken();

	_log4j.debug("parsing line "+line);
        _scheduleSpecifier = new ScheduleSpecifier(line);

	_log4j.debug("Entry Complete; period = "+getPeriod());
	return this;
    }

    /** Get calendar representing current time in time zone 
	indicate in this ScheduleTask
    */
    public Calendar getEntryTime(){
	return getEntryTime(System.currentTimeMillis()); 	
    }

    /** Get calendar representing time in time zone 
	indicate in this ScheduleTask
    */
    public Calendar getEntryTime(long time){
	_calendar = Calendar.getInstance();
	long localOffset=_calendar.get(Calendar.ZONE_OFFSET);
	_calendar.setTimeZone(getTZ());
	long entryOffset=_calendar.get(Calendar.ZONE_OFFSET);
	time = time-localOffset+entryOffset;
	_calendar.setTime(new Date(time));
	return _calendar; 	
    }

    /** Returns true if time matches or exceeds a scheduled time */
    public boolean isReady(){
	return isReady(System.currentTimeMillis());
    }

    public boolean isMasked(){

	if(isAbsolute() && _readyAction==CHECK_TIME)
	    return true;

	if(_state==SUSPENDED || _state==COUNTER_EXPIRED || _state==EXECUTING)
	    return true;

	// check to see if cycles counter has expired
	// If set to -1, job always runs
	if( getLongCycles()==0L ){
	    _log4j.debug("Cycle counter expired for "+get(ScheduleSpecifier.JOB));
	    cancelTimer();
	    setState(COUNTER_EXPIRED);
	    return true;
	}

	return false;
    }

    /** Returns true if time matches or exceeds a scheduled time */
    // JobRunner used to call isReady(), which checks time as well
    // as mask state, but this can create a race condition
    // if it we call System.currentTimeMillis a bit too late.
    // Also, isReady() gets the time on entry, which 
    // stacks the odds against in the race.
    // Now, the portion of isReady that checks to see if the job is
    // masked for some other reason (currently executing, 
    // counter expired, etc.) has been moved to isMasked(), which is
    // called by JobRunner. 
    public boolean isReady(long time){

	// check to see if not ready for reasons other than time
	if(isMasked())
	    return false;

	// get current time in *entry* time zone
	Calendar entryTime=getEntryTime(time);
	_log4j.debug("ScheduleTask.isReady(): time= "+entryTime.getTime());
	boolean ready=isSelectedTime(entryTime);

	if(ready)
	    setState(READY);

	return ready;
    }

    /** Subclasses must implement.  Fulfills Schedulable interface */
    public abstract void execute();

    /** Execute fake Job associated with this entry */
    public void fakeJob(){

	    setState(EXECUTING);

	_log4j.debug("executing "+get(ScheduleSpecifier.JOB)+" at "+getEntryTime().getTime());
	    _log4j.debug(" ");

	    setState(WAITING);
	    
    }

    /** toString() convert schedule entry to string */
    public String toString(){
	return toString(0L);
    }

    public String toString(long lookAheadSeconds){
	String retval = "";

	try {
	    long r = timeRemaining(lookAheadSeconds);
	    String t = ((r==Long.MAX_VALUE|r<0L)?"-":Long.toString(r));

	    retval += ("    Name: "+getName()+"\n");
	    retval += ("   Owner: "+getOwnerName()+" key="+getOwner().getScheduleKey()+"\n");
	    retval += ("Schedule: "+getScheduleSpecifier().get(ScheduleSpecifier.SCHEDULE_TIME)+"\n");
	    retval += ("  Status: "+getStateName(getState())+"\t"+t+"\n");
	}
	catch (NullPointerException e) {
	    retval = "Not initialized?";
	}

	return retval;
    }


    /** Calculate time remaining to next scheduled execution */
    public long timeRemaining(){
	if(isAbsolute())
	    return timeRemaining(Scheduler.MAX_LOOKAHEAD_SEC);
	else
	    return timeRemaining(0L);
    }

    /** Calculate time remaining to next scheduled execution */
    public long timeRemaining(long lookAheadSeconds){

	long remaining=0L;

	int state=getState();

	if(state==EXECUTING || state==COUNTER_EXPIRED || state==SUSPENDED)
	    return Long.MAX_VALUE;

	if(isRelative()==true && _timerTask!=null ){
	    // compute time until next selected time
	    // use getEntryTime to make sure that timezone for exectime and now are the same (3/12/04 klh)
	    long set=_timerTask.scheduledExecutionTime();
	    long execTime = getEntryTime(set).getTime().getTime();
	    long period = getPeriod();

	    // get calendar with selected timezone
	    // and currentTime
	    long now = getEntryTime().getTime().getTime();


	    // remaining sometimes comes up to be a small negative number...why? (3/12/04 klh)
	    //remaining=period-Math.abs(now-execTime);
	    if(execTime<now)
		remaining=period-(now-execTime);
	    else
		remaining=(execTime-now);
		
	    _log4j.debug("timeRemaining: "+remaining+" period = " + period +
	    			" now = " + now + " schedExecTime = " + set+" execTime="+execTime);
	}

	if(isAbsolute()==true){
	    // Look ahead lookAhead seconds to see when the
	    // next run time occurs. If not found, then
	    // schedule another check then.
	    _readyAction=CHECK_TIME;

	    // get calendar with selected timezone
	    // and currentTime
	    Calendar cal = getEntryTime();
	    long now = cal.getTime().getTime();

	    for(long i=0;i<lookAheadSeconds;i++){
		cal.add(Calendar.SECOND,1);
		if(isSelectedTime(cal)==true){
		    _readyAction=RUN;
		    break;
		}
	    }

	    // compute time until next selected time
	    remaining=cal.getTime().getTime()-now;
	}

	return remaining;
    }

    /** "Sync"* this scheduler entry after the specified delay.
	For relative schedules, waits delay ms and runs with 
	it's original period from that point forward.

	For absolute schedules, waits delay ms, runs, and then
	returns to it's original schedule.

	(*) The term "sync" is a holdover from the OASIS
	controller, which had a "sync" command to perform this
	function.
     */
    public void sync(){
	sync(0L);
	return ;
    }

    public int sync(long delayMillis){
	if(delayMillis<0L){
	    throw new IllegalArgumentException("sync(): delay milliseconds must be < 0");
	}
	    
	if(isRelative()){
	    _log4j.debug("sync("+delayMillis+") rescheduling relative");
	    // reschedule to run after specified delay
	    rescheduleTimer(delayMillis);
	}

	if(isAbsolute()){
	    SiamTimer timerSave = _timer;
	    _log4j.debug("sync("+delayMillis+") running absolute");
	    // Run once after specified delay
	    setTimer(new SiamTimer());
	    SiamTimerTask t = new ScheduleTimerTask(this);
	    setTimerTask(t);
	    getTimer().schedule(t,delayMillis);

	    // Cancel the old timer
	    if( timerSave != null ){
		_log4j.debug("sync("+delayMillis+") cancelling timer");
		timerSave.cancel();
	    }

	    _log4j.debug("sync("+delayMillis+") rescheduling absolute");
	}
	return Scheduler.OK;	    
    }

    /** Restart a timer; this differs from sync in that it 
	doesn't cause an absolute schedule to run immediately.
	Use this to start timer initially.
    */
    public void resetTimer(){
	if(isAbsolute())
	    resetAbsoluteTimer();
	if(isRelative())
	    sync();
    }

    /** Reset (absolute) task according to next scheduled
	execution time.
    */
    public void resetAbsoluteTimer(){
	// Absolute schedule entries must create a new
	// Timer and TimerTask (JobRunner) each time, because it 
	// a Timer/TimerTask can't be restarted once it
	// has stopped

	if(_state==EXECUTING || _state==SUSPENDED || _state==COUNTER_EXPIRED)
	    return;
	if(isAbsolute()){
	    // Should the cancel be moved down to right after
	    // the scheduling of the new timer? there may be 
	    // some problems in sleep if we are between timers for
	    // a long time...
	    SiamTimer timerSave = _timer;
	    setTimer(new SiamTimer());
	    long remaining=0L;
	    while(remaining<=0L)
		remaining=timeRemaining();
	    SiamTimerTask t = new ScheduleTimerTask(this);
	    setTimerTask(t);
	    getTimer().schedule(t,remaining);
	    if( timerSave != null )
		timerSave.cancel();
	}
    }

    /** Reschedule Timer */
    protected void rescheduleTimer(long delay){
	if(isRelative()){
	    SiamTimer timerSave = _timer;
	    setTimer(new SiamTimer());
	    SiamTimerTask t = new ScheduleTimerTask(this);
	    setTimerTask(t);
	    getTimer().scheduleAtFixedRate(t,delay,getPeriod());
	    if(timerSave !=null)
		timerSave.cancel();
	}
    }

    /** Reschedule Timer */
    protected void rescheduleTimer(Date startTime){
	if(isRelative()){
	    SiamTimer timerSave = _timer;
	    setTimer(new SiamTimer());
	    SiamTimerTask t = new ScheduleTimerTask(this);
	    setTimerTask(t);
	    getTimer().scheduleAtFixedRate(t,startTime,getPeriod());
	    if(timerSave !=null)
		timerSave.cancel();
	}
    }

    /** Cancel Timer associated with this entry */
    public void cancelTimer(){
	if(_timerTask!=null){
	    _timerTask.cancel();
	    _log4j.debug("ScheduleTask.cancelTimer(): cancelling timerTask");
	}
	if(_timer!=null){
	    _timer.cancel();
	    _log4j.debug("ScheduleTask.cancelTimer(): cancelling timer");
	}
	_timer=null;
	_timerTask=null;
    }

    /** Suspend ScheduleTask operation */
    public int suspend(){
	_log4j.debug("Suspending entry "+getName());

	setState(ScheduleTask.SUSPENDED);

	return Scheduler.OK;
    }

    /** Resume ScheduleTask operation */
    public int resume(){
	_log4j.debug("Resuming schedule "+getName());
	setState(ScheduleTask.WAITING);
	if(isAbsolute())
	    resetAbsoluteTimer();
	return Scheduler.OK;
    }

    /** Compare two ScheduleTasks */
    public boolean equals(ScheduleTask schedule){
	return _scheduleSpecifier.equals(schedule.getScheduleSpecifier());
    }

    /**
     If owner says that executing thread is for this task is sleeping, 
     return time (msec since epoch) at which task will resume. If executing 
     thread is not sleeping, return 0.
    */
    public long sleepingUntil() {
	return _owner.sleepingUntil(this);
    }
}



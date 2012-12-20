// MBARI copyright 2002
package org.mbari.siam.core;

import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
An implementation of the abstract ScheduleTask.
Implements execute method, which calls getData();

@author Kent Headley
*/

public class ScheduleTaskImpl extends ScheduleTask{

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(ScheduleTaskImpl.class);

    public ScheduleTaskImpl(){
	super();
    }

    /** Constructor 
	@param schedule cron-like schedule
	@param name schedule name
	@param owner owner of this ScheduleTask
     */
    public ScheduleTaskImpl(String name,String schedule, ScheduleOwner owner) throws ScheduleParseException{
	super(schedule,name,owner);
    }

    /** Constructor 
	@param schedule ScheduleSpecifier
	@param name schedule name
	@param owner owner of this ScheduleTask
     */
    public ScheduleTaskImpl(String name, ScheduleSpecifier schedule,ScheduleOwner  owner) throws ScheduleParseException{
	super(schedule,name,owner);
    }

    /** Constructor 
	@param interval sample interval
	@param name schedule name
	@param owner owner of this ScheduleTask
     */
    public ScheduleTaskImpl(String name, long interval,ScheduleOwner  owner) throws ScheduleParseException{
	super(interval,name,owner);
    }

    public void execute(){
	try{
	    ((ScheduleOwner)_owner).doScheduledTask(this);
	}catch(Exception e){
	    _log4j.error("ScheduleTaskImpl: caught exception "+e);
	    e.printStackTrace();
	}
	
    }
}



// MBARI copyright 2002
package org.mbari.siam.core;
import java.util.Collection;

import org.mbari.siam.core.Scheduler.ScheduleKey;

/**
Interface for objects that own scheduled tasks 
@author Kent Headley
@see ScheduleTask
*/
public interface ScheduleOwner {

    /** Return schedule key (unique schedule owner ID) */
    public ScheduleKey getScheduleKey();

    /** Return schedules */
    public Collection getAllSchedules();

    /** Default method to execute */
    public void doScheduledTask(ScheduleTask task);

    /** Add or replace schedule. Return integer code defined by Scheduler. */
    public int addSchedule(String name, String specifier, boolean overwrite);

    /** Remove specified schedule. Return integer code defined by Scheduler. */
    public int removeSchedule(String name);

    /** Remove all schedules. Return integer code defined by Scheduler. */
    public int removeAllSchedules();

    /** "Synchronize" specified schedule. Return integer code defined by 
	Scheduler.*/
    public int syncSchedule(String name, long delay);

    /** Suspend specified schedule.Return integer code defined by Scheduler. */
    public int suspendSchedule(String name);

    /** Resume specified schedule. Return integer code defined by Scheduler. */
    public int resumeSchedule(String name);


    /** If execution thread of specified task is sleeping, return time
	at which it will resume; otherwise return 0. */
    public long sleepingUntil(ScheduleTask task);
}

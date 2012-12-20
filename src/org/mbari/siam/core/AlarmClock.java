/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import org.mbari.siam.distributed.Device;

/** 
    AlarmClock lets a service thread go to sleep in such a way that 
    SleepManager could also put the processor to sleep while the service
    thread is sleeping, but the processor will wake up in time for the
    thread wakeup. 
*/
public class AlarmClock {

    public final static String SCHEDULE_NAME = "wakeup";

    DeviceService _service = null;

    public AlarmClock(DeviceService service) 
	throws Exception {

	if (! (service instanceof ScheduleOwner) ) {
	    throw new Exception("DeviceService is not a ScheduleOwner");
	}
	_service = service;
    }

    /** Put thread to sleep for specified seconds; indicate to 
	SleepManager that processor could also sleep for duration of the
	thread's sleep. Throw InterruptedException if the thread 
	is interrupted during Thread.sleep(). */
    public void snooze(int seconds) 
	throws InterruptedException {
	
	// Schedule wakeup time
	ScheduleOwner owner = (ScheduleOwner )_service;
	owner.addSchedule(SCHEDULE_NAME, Integer.toString(seconds * 1000),
			  true);

	// Save current status
	int status = _service.getStatus();

	// Set status to SLEEPING (so SleepManager knows we can sleep for
	// at least a while)
	_service.setStatus(Device.SLEEPING);

	Thread.sleep(seconds * 1000);

	// Restore status as it was prior to sleeping
	_service.setStatus(status);

	// Remove the wakeup schedule
	owner.removeSchedule(SCHEDULE_NAME);

    }
}

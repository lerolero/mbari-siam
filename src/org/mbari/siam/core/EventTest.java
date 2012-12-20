/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

public class EventTest {
    public void frobnicate(){
	// Get an EventManager instance
	EventManager em = EventManager.getInstance();

	// Some code to test Scheduler's response to 
	// various service events
	System.out.println("EventTest injecting SvcINSTALLED");
	ServiceEvent se = new ServiceEvent(this,ServiceEvent.STATE_CHANGE,ServiceEvent.INSTALLED,1001);
	em.postEvent(se);

	System.out.println("EventTest injecting SvcREQ_COMPLETE");
	se = new ServiceEvent(this,ServiceEvent.STATE_CHANGE,ServiceEvent.REQUEST_COMPLETE,1001);
	em.postEvent(se);
	
	System.out.println("EventTest taking a catnap...");
	try{
	    Thread.sleep(15000);
	}catch(InterruptedException e){}
	System.out.println("EventTest awake again...");

	System.out.println("EventTest injecting SvcREMOVED");
	se = new ServiceEvent(this,ServiceEvent.STATE_CHANGE,ServiceEvent.REMOVED,1001);
	em.postEvent(se);	
    }
}

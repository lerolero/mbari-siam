// MBARI copyright 2002
package org.mbari.siam.tests.moos.deployed;

import java.util.*;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Vector;
import java.util.Collection;
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import java.rmi.RemoteException;
import org.mbari.siam.core.Scheduler;
import org.mbari.siam.core.NodeManager;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.core.ScheduleTask;
import org.mbari.siam.core.InstrumentService;

/**
JUnit test harness for testing SiamProperties class.
@author Karen A. Salamy & Kent Headley
 */


// Creating a subclass of TestCase
public class SchedulerTest extends TestCase
{
    /** Log4j logger */
    static private Logger _log4j=Logger.getLogger(SchedulerTest.class);

    public SchedulerTest(String methodName) {
		super(methodName);
		_log4j.debug("SchedulerTest " + methodName );

	}

    //******************************************************
    // Test method to assert expected results on the object under test
    public void testEmptyCollection() {
	    Collection collection = new ArrayList();
	    assertTrue(collection.isEmpty());
	}
    //******************************************************


    //-------------------------------------------------------------------
    // Test Scheduler
    public void testScheduler(){

	// Demonstrate two objects of the same class,
	// each with multiple schedules

	// Create first object...
	TestScheduleOwner tso = new TestScheduleOwner();

	// Get a Scheduler instance
	Scheduler s = Scheduler.getInstance();

	// Get our unique schedule key from the Scheduler
	tso.setKey(s.getScheduleKey(tso));
	ScheduleTask defaultScheduleTask=null;
	ScheduleTask alternativeScheduleTask=null;
	ScheduleTask newAlternativeScheduleTask=null;

	try{
	defaultScheduleTask = tso.createTask("default",new ScheduleSpecifier("15000"),tso);
	alternativeScheduleTask = tso.createTask("alternative",new ScheduleSpecifier("20000"),tso);
	newAlternativeScheduleTask = tso.createTask("alternative", new ScheduleSpecifier("25000"),tso);
    

	//TESTING: add, remove, show and get next Schedule before schedules started.

	    //************************************************
	    // Testing removeSchedule (Null) before it is started
	    int testVal = s.removeSchedule(defaultScheduleTask);
	    assertTrue(testVal==Scheduler.NOT_FOUND); 

	    // Testing showSchedule (Empty String) before it is started
	    String retVal = s.showSchedule();
	    assertTrue(retVal.equals(""));

	    // Test getNextScheduledJob
	    long retlong = s.getNextScheduledJob();
	    assertTrue(retlong==Long.MAX_VALUE);

	    // Test addSchedule from ScheduleTask(before schedules are started)
	    //Overwrite = false:  Should default to a return of OK
	    int testAddS = s.addSchedule(defaultScheduleTask, false);
	    assertTrue(testAddS==Scheduler.OK);

	    // Test addSchedule from ScheduleTask(before schedules are started)
	    // Overwrite = true - With ismatch= false - should default to OK
	    int testNullS = s.addSchedule(alternativeScheduleTask, true);
	    assertTrue(testNullS==Scheduler.OK);
	    //***********************************************


	    //------------------------------------------------------------------------
	    // for Demo, set the names to show owner name and key value
	    defaultScheduleTask.setOwnerName(defaultScheduleTask.getOwnerName()+":"+tso.getScheduleKey().value());
	    alternativeScheduleTask.setOwnerName(alternativeScheduleTask.getOwnerName()+":"+tso.getScheduleKey().value());



	    // Add them to our own vector of schedules
	    tso.getAllSchedules().add(defaultScheduleTask);
	    tso.getAllSchedules().add(alternativeScheduleTask);
	    //------------------------------------------------------------------------
	    

	    //***********************************************
	    // Test addSchedule from ScheduleTask(after schedules are added to vector)
	    int reAddS = s.addSchedule(defaultScheduleTask, false);
	    assertTrue(reAddS==Scheduler.ALREADY_EXISTS);

	    int reAddS2 = s.addSchedule(alternativeScheduleTask, false);
	    assertTrue(reAddS2==Scheduler.ALREADY_EXISTS);
	    //**********************************************

	    //------------------------------------------------------------------------
	    // Register our schedules with the Scheduler (this does not begin to execute the schedules)
	    s.setSchedules(tso.getAllSchedules());

	}catch(ScheduleParseException e){
	    _log4j.error(e);
	    e.printStackTrace();
	}


	// Start first object's schedules 
	// (could do this automatically using a ServiceEvent)
        for(Iterator e=tso.getAllSchedules().iterator();e.hasNext();){
	    ScheduleTask st = (ScheduleTask)e.next();
	    _log4j.debug("\nStarting service "+st.getName() + "...");
	    st.sync();

	//****************************************************************	
	//Perform a test of the showSchedule() method.  Should show all running methods.
	_log4j.debug("Performing a check of the queued " +st.getName() + " task.");
	String listSchedule = s.showSchedule();

	//Verify that at least one listing of "Name" is there indicating the schedule(s) were started.
	assertTrue(listSchedule.indexOf("Name") >= 1);
	_log4j.debug("Schedule " + st.getName()+ " is running!\n");

	} 
	//*******************************************************************


	// Print Current list of schedules
	_log4j.debug("\nListing Current Schedules:\n"+
			   s.showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));

	// From Scheduler version 1.37
	//_log4j.debug("\nListing Current Schedules:\n"+
	//		   s.showScheduleTest(null,Scheduler.MAX_LOOKAHEAD_SEC));

	//StopWatch.delay(5000);


	while(s.getNextScheduledJob()<=4000L || s.getNextScheduledJob()>20000L)
	    _log4j.debug("Next task in " + s.getNextScheduledJob());

	_log4j.debug("\nBefore sync: Next task in " + s.getNextScheduledJob() + " milliseconds");
	alternativeScheduleTask.sync(15000); //Wait 15 seconds to restart sample cycle
	_log4j.debug("During sync: Next task in " + s.getNextScheduledJob()+ " milliseconds");
	defaultScheduleTask.sync(10000); //Wait 10 seconds to restart sample cycle
	_log4j.debug("After sync: Next task in " + s.getNextScheduledJob() + " milliseconds");


	long timeRem = s.getNextScheduledJob(); //Should return 10 +/- seconds for default task

	_log4j.debug("\nNext task is in " + timeRem + " milliseconds");

	//Test to see if the next run following a sync matches in the time frame set +/- 1 sec. slop
	assertTrue(timeRem<=10000 && timeRem>=9000);
	//assertTrue(timeRem == 10000);


	//****************************************************************	
	//Perform a test of the removeSchedule() method.  Should remove running schedules from queue.
	_log4j.debug("\n\nRemoving the `" + defaultScheduleTask.getName()+ "' task from run operation.\n");
	int remSchedule = s.removeSchedule(defaultScheduleTask);

	//Test that removeSchedule() worked.
	assertTrue(remSchedule == Scheduler.OK);
	_log4j.debug("Schedule task " + defaultScheduleTask.getName()+ " cancelled!\n");

	//*******************************************************************

	//What's the Schedule listing again?  FROM Scheduler VERSION 1.38 where showScheduleTest was removed!
	_log4j.debug("\nListing Current Schedules:\n"+
			   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));
	
	//From Scheduler Version 1.37
	//_log4j.debug("\nListing Current Schedules:\n"+
	//		   Scheduler.getInstance().showScheduleTest(null,Scheduler.MAX_LOOKAHEAD_SEC));

	//****************************************************************
	//Set Schedule to show owner name and key value
	alternativeScheduleTask.setOwnerName(alternativeScheduleTask.getOwnerName()+":"+tso.getScheduleKey().value());

	// Add new Schedule to Schedule Task list (OVERWRITE old alternative task)
	tso.addSchedule(newAlternativeScheduleTask,true);


	//Testing getScheduleSpecifier() method.  Overwrites old alternativeScheduleTask with new file information.
	assertTrue(newAlternativeScheduleTask.getScheduleSpecifier().toString().equals("R */25 */0 */0 */0 * * * * GMT *"));
	//****************************************************************

	
	//Check Schedule listing again to see if changes were made (should show a time 25 seconds versus 20 seconds)

	// FROM Scheduler VERSION 1.38 where showScheduleTest was removed!
	_log4j.debug("\nNew Schedules List with updated changes:\n"+
		   Scheduler.getInstance().showSchedule(null,Scheduler.MAX_LOOKAHEAD_SEC));


	//From Scheduler Version 1.37
	//_log4j.debug("\nNew Schedules List with updated changes:\n"+
	//	   Scheduler.getInstance().showScheduleTest(null,Scheduler.MAX_LOOKAHEAD_SEC));

 } //end testScheduler method


// Implement a suite() method that uses reflection to dynamically create a test suite containing all the testXXX() methods
	public static Test suite() {
	    TestSuite suite = new TestSuite();
	    suite.addTest(new SchedulerTest("testEmptyCollection"));
	    suite.addTest(new SchedulerTest("testScheduler"));
	    return suite;

	}

} //end class

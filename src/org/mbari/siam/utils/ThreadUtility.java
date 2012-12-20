/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;
import org.apache.log4j.Logger;

/**
Various Thread-related utilities. 
@author Tom O'Reilly
*/
public class ThreadUtility {

    static private Logger _log4j = Logger.getLogger(ThreadUtility.class);
    static private int _key = 0;
    static private final String DEBUG_TOKEN = "-DBG-";

    // Set this to true if you want to add a "debugging tag" to each
    // thread name. 
    private static boolean _tagThreadNames = true;

    /** Print thread information. */
    public static void printThreads() {
	System.out.println("ThreadUtility.printThreads()");
	ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();

	_log4j.info("ThreadGroup \"" + threadGroup.getName() + 
			   "\" includes about " + threadGroup.activeCount() + 
			   " threads");

	int numThreads = threadGroup.activeCount();
	Thread[] threads = new Thread[numThreads];
	String sThreads="";

	try {
	    threadGroup.enumerate(threads);
	    for (int i = 0; i < numThreads; i++) {
		if (threads[i] != null) {
		    
		    if (_tagThreadNames) {

			// Try to tag thread name with a unique ID if not
			// already tagged
			String name = threads[i].getName();
			if (name.indexOf(DEBUG_TOKEN) == -1) {
			    try {
				String newName = name + DEBUG_TOKEN + _key++;
				threads[i].setName(newName);
			    }
			    catch (SecurityException e) {
				_log4j.error("Couldn't change thread name: " + 
					      threads[i].getName());
			    }
			}
		    }
		    sThreads+=("thread: \"" + 
			       threads[i].getName() + "\"");

		    if (threads[i].isAlive()) {
			sThreads+=" alive ";
		    }
		    else {
			sThreads+=" dead ";
		    }

		    if (threads[i].isInterrupted()) {
			sThreads+=" interrupted ";
		    }

		    if (threads[i].isDaemon()) {
			sThreads+=" daemon ";
		    }
		    sThreads+="\n";
		}
	    }
	    if(sThreads.length()>0)
		_log4j.info(sThreads);
	}
	catch (Exception e) {
	    _log4j.error("printThreads(): ", e);
	}
    }
}

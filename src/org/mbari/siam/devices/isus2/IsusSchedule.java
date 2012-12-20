/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.isus2;
import org.mbari.siam.utils.PrintfFormat;

/**
Generate a file suitable for use by ISUS instrument in "scheduled" mode.
*/
public class IsusSchedule {

    final static int SECS_PER_DAY = 60 * 60 * 24;
    final static int WARMUP_SEC = 45;

    final static int MIN_INTERVAL_SEC = 15;
    final static int MAX_INTERVAL_SEC = SECS_PER_DAY;

    // Minimum and maximum sample integration seconds
    final static int MIN_INTEGRATION_SEC = 2;
    final static int MAX_INTEGRATION_SEC = 60;

    static PrintfFormat _formatter = new PrintfFormat("%02d");

    public static void main(String[] args) {

	if (args.length != 3) {
	    System.err.println("usage: intervalSec integrationSec startMinutes");
	    return;
	}

	boolean error = false;
	int intervalSec = 0;
	try {
	    intervalSec = Integer.parseInt(args[0]);
	    if (intervalSec <= MIN_INTERVAL_SEC || 
		intervalSec > MAX_INTERVAL_SEC) {
		error = true;
		System.err.println("Invalid interval seconds: " + args[0]);
		System.err.println("Must be integer in range " + 
				   MIN_INTERVAL_SEC + " to " + 
				   MAX_INTERVAL_SEC);
	    }
	}
	catch (Exception e) {
	    System.err.println("Integer required: " + args[0]);
	    error = true;
	}


	int integrationSec = 0;

	try {
	    integrationSec = Integer.parseInt(args[1]);
	    if (integrationSec < MIN_INTEGRATION_SEC || 
		integrationSec > MAX_INTEGRATION_SEC) {
		error = true;
		System.err.println("Invalid integration seconds: " + args[1]);
		System.err.println("Must be integer in range " + 
				   MIN_INTEGRATION_SEC + " to " + 
				   MAX_INTEGRATION_SEC);
	    }
	}
	catch (Exception e) {
	    System.err.println("Integer required: " + args[1]);
	    error = true;
	}


	int startSec = 0;

	try {
	    startSec = Integer.parseInt(args[2]) * 60;
	    if (startSec < 0 || 
		startSec > 3600) {
		error = true;
		System.err.println("Invalid start minutes: " + args[2]);
		System.err.println("Must be integer in range 0 to 60");
	    }
	}
	catch (Exception e) {
	    System.err.println("Integer required: " + args[2]);
	    error = true;
	}


	if (error) {
	    return;
	}

	if (intervalSec - 2 < integrationSec) {
	    System.err.println("Invalid parameters: " +
			       "integration seconds must be less than " +
			       "interval seconds");
	    return;
	}


	// Generate ISUS schedule entries;
	for (int daySeconds = startSec; daySeconds < SECS_PER_DAY; 
	     daySeconds += intervalSec) {

	    int sec = daySeconds;
	    System.out.println(timeString(sec) + " POWER    +ISUS");
	    sec += WARMUP_SEC;
	    System.out.println(timeString(sec) + " ACQUIRE " + 
			       integrationSec +  " ISUS");

	    sec += integrationSec + 2;

	    System.out.println(timeString(sec) + " POWER     -ISUS\n");
	}			       
    }


    static String timeString(int daySeconds) {
	int hours = daySeconds / 3600;
	int remainder = daySeconds % 3600;
	int minutes = remainder / 60;
	int seconds = remainder % 60;

	return (_formatter.sprintf(hours) + ":" + 
		_formatter.sprintf(minutes) + ":" + 
		_formatter.sprintf(seconds));
    }
}

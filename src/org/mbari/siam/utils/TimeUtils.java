/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Date;

/**
   Various utilities for manipulating and converting times, dates, and
   their string representations.
 */
public class TimeUtils {

    /**
       Return time (millisec since epoch) corresponding to input
       date/time string. Input string format must conform to one of
       the following: "M/d/yyyy'T'H:m:s", "M/d/yyyy'T'H:m", or "M/d/yyyy".
       I.e., if clock time is specified, it must be separated from
       the mm/dd/yyyy by upper case 'T'.
    */
    static public long parseDateTime(String dateTimeString) 
	throws ParseException {

	// Input string might be representation of millisec since epoch.
	try {
	    return Long.parseLong(dateTimeString);
	}
	catch (NumberFormatException e) {
	    // Nope, not an integer/long... try to parse date/time
	}

	String patterns[] = {"M/d/yyyy'T'H:m:s", "M/d/yyyy'T'H:m",
			     "M/d/yyyy"};

	SimpleDateFormat dateFormat = new SimpleDateFormat();
	dateFormat.setLenient(true);
	ParsePosition position = new ParsePosition(0);
	Date date = null;
	for (int i = 0; i < patterns.length; i++) {
	    position.setIndex(0);
	    position.setErrorIndex(-1);
	    dateFormat.applyPattern(patterns[i]);
	    date = dateFormat.parse(dateTimeString, position);
	    if (date != null) {
		break;
	    }
	}

	if (date == null) {
	    StringBuffer buffer = 
		new StringBuffer(dateTimeString + 
				 " - invalid date/time. " + 
				 "Acceptable formats:\n");

	    for (int i = 0; i < patterns.length; i++) {
		buffer.append(patterns[i] + "\n");
	    }
	    throw new ParseException(new String(buffer), 0);
	}
	return date.getTime();
    }
}

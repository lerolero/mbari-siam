/** 
* @Title OasisTimestamp
* @author Kent Headley
*
* Copyright MBARI 2004
* 
* REVISION HISTORY:
*/
package org.mbari.siam.utils;

import java.util.*;
import java.lang.Integer;
import java.lang.Long;
import java.text.NumberFormat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
/**
   An adapter class to convert SIAM timestamps (Java time, long ms since epoch)
   to OASIS timestamps (decimal day of year).
   Notes:
   - This class is immutable
   - Assumes that all times are UTC
 */
public class OasisTimestamp{

    private long _time;
    private Date _date;
    private Calendar _calendar;
    private double _oasisTime = -1.0;
    private NumberFormat _nf;

    /** Constructor */
    public OasisTimestamp(long time){
	super();
	_time = time;
	_date = new Date(_time);
	_calendar = Calendar.getInstance();
	_calendar.setTime(_date);
	_calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

	// set up a number format
	_nf = NumberFormat.getInstance();
	_nf.setMaximumIntegerDigits(3);
	_nf.setMaximumFractionDigits(5);
	_nf.setMinimumIntegerDigits(1);
	_nf.setMinimumFractionDigits(5);

	// compute oasis time
	double dayOfYear = (double)_calendar.get(Calendar.DAY_OF_YEAR);
	double hourOfDay = (double)_calendar.get(Calendar.HOUR_OF_DAY);
	double minute = (double)_calendar.get(Calendar.MINUTE);
	double second = (double)_calendar.get(Calendar.SECOND);
	double fracDay = hourOfDay+minute/60.0/24.0+second/3600.0/24.0;
	fracDay=fracDay/24.0;
	_oasisTime = (double)(dayOfYear+fracDay);
    }

    /** get time (ms since epoch represented by this timestamp */
    public long getTime(){
	return _time;
    }
    /** get Date represented by this timestamp */
    public Date getDate(){
	return _date;
    }

    /** get OASIS timestamp time as double */
    public double getOasisTime(){
	return _oasisTime;
    }

    /** get OASIS timestamp as String */    
    public String toString(){
	return _nf.format(_oasisTime);
    }

    /** Convert specified long time to OasisTimestamp
	and send to stdout
     */
    public static void main(String args[]){

	// print use message if incorrect number of args
	if(args.length<1){
	    System.err.println("Convert long ms since epoch to OASIS timestamp");
	    System.err.println("usage: OasisTimestamp longTime");
	    System.exit(0);
	}

	OasisTimestamp ts = new OasisTimestamp(Long.parseLong(args[0]));
	System.out.println(ts);
    }

}








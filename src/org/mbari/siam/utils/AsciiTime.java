/** 
* @Title AsciiTime utility class
* @author Martyn Griffiths
*
* Copyright MBARI 2003
* 
* REVISION HISTORY:
* $Log: AsciiTime.java,v $
* Revision 1.2  2009/07/16 22:02:09  headley
* javadoc syntax fixes for 1.5 JDK
*
* Revision 1.1  2008/11/04 22:17:53  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:05  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.5  2004/03/03 23:57:01  headley
* added methods to format an arbitrary time (for getting formatted
* future times for setting instrument clocks)
*
* Revision 1.4  2003/09/24 01:21:57  martyn
* Added day of the year "ddd" facility to getDate(..)
*
* Revision 1.3  2003/09/15 23:38:46  martyn
* Improved AsciiTime utility class
* Updated all drivers to use revised class.
* Removed registryName service property
* Moved service property keys to ServiceProperties
*
* Revision 1.2  2003/09/11 21:26:06  martyn
* 24 hour clock assumed
*
* Revision 1.1  2003/09/10 23:00:44  martyn
* Time utility
*
*/
package org.mbari.siam.utils;

import java.util.*;

/**
 * A utility for acquiring the formatted date and time by passing 
 * a simple format string. There are two forms of getDate() and
 * getTime(). One returns UTC time and date by default, the other is timezone
 * selectable.
 */
public class AsciiTime{

    static private Calendar _time;
    static protected int _month,_date,_year,_day;
    static protected int _hour24,_hour12,_minute,_second;

    /**
     * 
     * @param timeZone normally this will be "UTC"
     */
    private AsciiTime(){
        // singleton
    }

    /**
     * Method returning the current time in Ascii format
     * 
     * @param formatString
     *               String representing the required time format:-
     *               HH is substituted w/ current hour using 24 clock
     *               hh is substituted w/ current hour using 12 clock
     *               mm is substituted w/ minutes past the hour
     *               ss is substituted w/ seconds past the hour
     * 
     * @return formatted time string
     * Example - A format string "HH:mm:ss" would return the string "23:59:59"
     * if current time is one second before midnight
     */
    static public synchronized String getTime(String formatString){
        return AsciiTime.getTime(formatString,"UTC");
    }

    /**
     * Same as above except you have the option of selecting any timezone.
     * 
     * @param formatString
     * @param timeZone
     * 
     * @return formatted time string
     * @example see above
     */

    /** Get current time in specified format and time zone */
    static public synchronized String getTime(String formatString,String timeZone){
	return getTime(System.currentTimeMillis(),formatString,timeZone);
    }

    /** Get specified time in specified format and default timezone (UTC) */
    static public synchronized String getTime(long time, String formatString){
	return getTime(time,formatString,"UTC");
    }

    /** Formats the specified time as a time string using the given format specifier and timezone */
    static public synchronized String getTime(long time, String formatString,String timeZone){
        _time = Calendar.getInstance();
	_time.setTime(new Date(time));
        _time.setTimeZone(TimeZone.getTimeZone(timeZone));

        _hour24 = _time.get(Calendar.HOUR_OF_DAY);
        _hour12 = _time.get(Calendar.HOUR);
        _minute = _time.get(Calendar.MINUTE);
        _second = _time.get(Calendar.SECOND);

        int H2 = formatString.indexOf("HH");
        int h2 = formatString.indexOf("hh");
        int m2 = formatString.indexOf("mm");
        int s2 = formatString.indexOf("ss");
        
        char str[] = new char[formatString.length()];
        formatString.getChars(0,formatString.length(),str,0);
        
        if(H2>=0){
            str[H2] = Character.forDigit(_hour24/10,10); str[H2+1] = Character.forDigit(_hour24%10,10);
        }
        if(h2>=0){
            str[h2] = Character.forDigit(_hour12/10,10); str[h2+1] = Character.forDigit(_hour12%10,10);
        }
        if(m2>=0){
            str[m2] = Character.forDigit(_minute/10,10); str[m2+1] = Character.forDigit(_minute%10,10);
        }
        if(s2>=0){
            str[s2] = Character.forDigit(_second/10,10); str[s2+1] = Character.forDigit(_second%10,10);
        }
        return new String(str);
    }

    /**
     * Method returning the current date in Ascii format
     * 
     * @param formatString
     *               String representing the required date format:-
     *               MM is substituted w/ current month
     *               DD is substituted w/ current day of month
     *               YY or YYYY is substituted w/ current year
     *               ddd is substituted w/ current day of year
     * 
     * @return Formatted date string
     * Example - A format string "MM/DD/YY" would return a date string "09/15/03".
     * A format string "DD/MM/YYYY" would return the string "15/09/2003"
     */
    static public synchronized String getDate(String formatString){
        return AsciiTime.getDate(formatString,"UTC");
    }

    /**
     * Same as above except you have the option of selecting any timezone.
     * 
     * @param formatString
     *                 See above
     * @param timeZone as supported by the java.util.TimeZone class
     * 
     * @return formatted date string
     * @example see above
     */

    /** Gets current time in specified format and timezone */
    static public synchronized String getDate(String formatString,String timeZone){
	return getDate(System.currentTimeMillis(),formatString, timeZone);
    }

    /** Get specified date using specified format string and default timezone (UTC) */
    static public synchronized String getDate(long time, String formatString){
	return getDate(time,formatString,"UTC");
    }

    /** Formats the specified time as a date string using the given format specifier and timezone */
    static public synchronized String getDate(long time, String formatString,String timeZone){
        _time = Calendar.getInstance();
        _time.setTime(new Date(time));
        _time.setTimeZone(TimeZone.getTimeZone(timeZone));

        _month = _time.get(Calendar.MONTH) + 1;
        _date = _time.get(Calendar.DATE);
        _year = _time.get(Calendar.YEAR);
        _day  = _time.get(Calendar.DAY_OF_YEAR);

        int m2 = formatString.indexOf("MM");
        int d1 = formatString.indexOf("ddd");
        int d2 = formatString.indexOf("DD");
        int y4 = formatString.indexOf("YYYY");
        int y2 = formatString.indexOf("YY");
        
        if(y4>=0) y2=-1;
        char str[] = new char[formatString.length()];
        formatString.getChars(0,formatString.length(),str,0);
        
        if(m2>=0){
            str[m2] = Character.forDigit(_month/10,10); str[m2+1] = Character.forDigit(_month%10,10);
        }
        if(d1>=0){
            str[d1] = Character.forDigit(_day/100,10); str[d1+1] = Character.forDigit((_day%100)/10,10);
            str[d1+2] = Character.forDigit(_day%10,10);
        }
        if(d2>=0){
            str[d2] = Character.forDigit(_date/10,10); str[d2+1] = Character.forDigit(_date%10,10);
        }
        
        if(y2>=0){
            str[y2] = Character.forDigit((_year%100)/10,10); str[y2+1] = Character.forDigit(_year%10,10);
        }
        if(y4>=0){
            str[y4] = Character.forDigit(_year/1000,10); str[y4+1] = Character.forDigit((_year%1000)/100,10);
            str[y4+2] = Character.forDigit((_year%100)/10,10); str[y4+3] = Character.forDigit(_year%10,10);
        }

        return new String(str);
    }


    public static void main(String args[]) {
        String date = AsciiTime.getDate("MM/DD/YYYY");
        String time = AsciiTime.getTime("hh:mm:ss");
        System.out.println(date + " " + time);
        while(true){
            try {
                Thread.sleep(2000);
                break;
            }
            catch ( Exception e){
            }
        }
        date = AsciiTime.getDate("Day:ddd Month:MM Date:DD Year:YY");
        time = AsciiTime.getTime("HH:hh:mm:ss");
        System.out.println(date + " " + time);
    }
}








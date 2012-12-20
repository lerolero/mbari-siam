// MBARI copyright 2003
package org.mbari.siam.distributed;

/** Interface to allow interested parties to add callbacks to SleepManager.
    Allows anyone to specify whether SleepManager can go to sleep, and if so,
    when to wake up.
*/
public interface SleepRollCallListener {

    public static long NO_TIME_SPECIFIED = Long.MAX_VALUE;

/** Return <= 0 if need to stay awake.  Else, return number of milliseconds
    until we need to wake up.
*/
    public long okToSleep();

}
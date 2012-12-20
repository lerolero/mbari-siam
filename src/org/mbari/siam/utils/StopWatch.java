/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class StopWatch
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(StopWatch.class);

    private boolean _active;
    private long _refTime;
    private long _totalTime;

    /** Ceate a new StopWatch object.  A StopWatch object acts as a stop 
    watch that can be stopped and started multiple times during the life 
    of the object.  The stop watch will track the total amount time between
    starts and stops. */
    public StopWatch()
    {
        _active = false;
        _totalTime = 0;
    }
    
    
    /** Ceate a new running StopWatch object.  If the create_running boolean
    is true the StopWatch object will be created running. */
    public StopWatch(boolean create_running)
    {
        _active = false;
        _totalTime = 0;
        
        if (create_running)
            start();
    }
    
    
    /** Find out if the stopwatch is currently keeping track of time */
    public boolean isRunning()
    {
        return _active;
    }
    
    /** Clear the stopwatch */
    public void clear()
    {
        _totalTime = 0;
        _refTime = System.currentTimeMillis();
    }

    
    /** Read the total amount of elapsed time the stopwatch has running */
    public long read()
    {
        long current_time = System.currentTimeMillis();
        
        if (_active)
            _totalTime += (current_time - _refTime);

        _refTime = current_time;

        return _totalTime;
    }

    /** Start the stopwatch */
    public long start()
    {
        read();
        _active = true;
        return _totalTime;
    }

    /** Stop the stopwatch */
    public long stop()
    {
        read();
        _active = false;
        return _totalTime;
    }

    /** Delay for the specified number milliseconds in the calling thread */
    public static void delay(int millisecs)
    {
        try 
        {
            Thread.sleep( millisecs );
        } 
        catch ( Exception e ) 
        {
            _log4j.error("wait(...) failed: " + e);
        }
    }

    public static void main(String[] args) 
    {
        //use args from main to set com port
        StopWatch app = new StopWatch();
	app.stopWatchTest();
        System.exit(0);
    }

    public void stopWatchTest()
    {
        System.out.println("[Time shoud equal to 0     ] time = " + read());
        
        this.start();
        delay(500);
        
        System.out.println("[Time shoud be approx 500  ] time = " + read());
        
        delay(500);
        this.stop();
        
        System.out.println("[Time shoud be approx 1000 ] time = " + read());
        
        delay(1000);
        
        System.out.println("[Time shoud equal last read] time = " + read());
    }


}


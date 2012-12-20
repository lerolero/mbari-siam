// Copyright 2001 MBARI
package org.mbari.siam.distributed.platform;

import java.io.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

//TODO : 
//  *???

class CommStreamSupport
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(CommStreamSupport.class);

    InputStream commIn;
    OutputStream commOut;

    CommStreamSupport(InputStream in, OutputStream out)
    {
        commIn = in;
        commOut = out;
    }
    
    boolean waitForChars(int total_chars)
    {
        int msec = 2000 + total_chars;

        return waitForChars(total_chars, msec);
    }

    boolean waitForChars(int total_chars, int msec)
    {
        boolean got_chars = false;
        boolean timed_out = false;
        long start_time;
        
        start_time = System.currentTimeMillis();
        
        while ( (!timed_out) && (!got_chars) )
        {
            if ( (System.currentTimeMillis() - start_time) > msec )
                timed_out = true;

            delay(1);
        
            try
            {
                if ( commIn.available() > (total_chars - 1) )
                    got_chars = true;
            }
            catch(IOException e)
            {
                _log4j.error("waitForChars(...) IOException");
                _log4j.error(e.getMessage());
            }
        }
        
        return got_chars;
    }

    // Read and discard all characters in input 
    void flushInput() 
    {
        try
        {
            commIn.skip(commIn.available());
        }
        catch(IOException e)
        {
            _log4j.error("flushInput() IOException");
            _log4j.error(e.getMessage());
        }
    }


    private void delay(int msecs) 
    {
        try 
        {
            Thread.sleep( msecs );
        } 
        catch ( Exception e ) 
        {
            _log4j.error("delay(...) failed");
        }
    }
}

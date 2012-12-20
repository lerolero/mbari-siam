/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.utils;

import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.TimeoutException;

public class StreamUtils
{
    // CVS revision
    private static String _versionID = "$Revision: 1.2 $";

    /** Log4j logger */
    static Logger _logger = Logger.getLogger(StreamUtils.class);

    /** Read characters from input stream into buffer, until specified
	terminator is encountered. 
	@param instream input stream
	@param outbuf output buffer
	@param terminator terminator character string
    */
    public static int readUntil(InputStream instream, 
                                byte[] outbuf, 
                                byte[] terminator, 
                                long timeout) 
	throws TimeoutException, NullPointerException, IOException, Exception 
    {
	//total bytesRead less the terminator length
        int bytesRead = 0;
        //elapsed time out the read
	long elapsed = 0;
        //total loops used while reading bytes
        int loopCount = 0;
        // index into terminator
        int termIndex = 0;
        // single shote timeout boost 
        boolean timeoutIncreased = false;
        
        // throw exception if any bad args
        checkReadUntilArgs(instream, outbuf, terminator);
        
	// try 
	// {
            //capture the start time of readUnil
            long t0 = System.currentTimeMillis();
            
            //read until we receive terminator, exceed outbuf, or time out
            while (true) 
            {
                ++loopCount;
                
                if (instream.available() > 0) 
                {
                    byte c = (byte)instream.read();
                    outbuf[bytesRead++] = c;
                    
                    if (c == terminator[termIndex])
                    {
                        termIndex++;
                    }
                    else
                    {
                        termIndex = 0;

                        //this may be the start of the terminator as well
                        if (c == terminator[0])
                            termIndex++;
                    }
                     
                    //if the termIndex is the same size as the 
                    //terminator you got it 
                    if (termIndex == terminator.length)
                        return (bytesRead - terminator.length);
                    
                    if (bytesRead >= outbuf.length) 
                    {
                        throw new Exception("readUntil() outbuf exceeded, " +
                                            "outbuf length is " + 
                                            outbuf.length + " bytes");
                    }

                }
                else
                {
                    //Thread.yield();
                    StopWatch.delay(50);
                }

                elapsed = System.currentTimeMillis() - t0;

                if (elapsed > timeout)
                {
                    if ((instream.available() > 0) && !timeoutIncreased)
                    {
                        timeoutIncreased = true;
                        //give the the method 10% of timeout more time to get
                        //the bytes
                        timeout = elapsed + (timeout / 10);
			// _logger.debug("*** readUntil timeout increased");
                        // _logger.debug("instream.available() = " + instream.available());
                        // _logger.debug("timeout = " + timeout);
                    }
                    else
                    {
			String msg="readUntil() Timed out after "+
			    (System.currentTimeMillis()-t0)+" ms"+
			    " outbuf.len="+outbuf.length+
			    " avail: "+instream.available()+
			    " read: "+bytesRead;
                        throw new TimeoutException(msg);
                    }
                }
            }
	    // }
	/*
        finally 
        {
            //set this to one for debugging info
            // _logger.debug("*** leaving readUntil()");
             _logger.debug("bytesRead = " + bytesRead);
             _logger.debug("instream.available() = " + instream.available());
             _logger.debug("outbuf.length = " + outbuf.length);
             _logger.debug("elapsed = " + elapsed);
             _logger.debug("timeoutIncreased = " + timeoutIncreased);
             _logger.debug("loopCount = " + loopCount);
        }
	*/
    }


    /** Read characters from input stream into buffer, until specified
	terminator is encountered, maxBytes are received, or timeout 
	milliseconds elapse.
	Note that it is possible to ignore timeout and/or maxBytes by setting
	them to 0. 
	@param instream input stream
	@param terminator terminator character string
	@param timeout stop after timeout ms
	@param maxBytes stop after maxBytes skipped
    */
    public static int skipUntil(InputStream instream, 
                                byte[] terminator, 
                                long timeout, 
                                int maxBytes) 
	throws TimeoutException, NullPointerException, IOException, Exception 
    {

	// Total number of bytes skipped over
	int bytesSkipped = 0;
	// Elapsed time
	long elapsed = 0;
        //number of loops to skip over chars
        int loopCount = 0;
        // index into terminator
        int termIndex = 0;
        // single shote timeout boost 
        boolean timeoutIncreased = false;

        // throw exception if any bad args
        checkSkipUntilArgs(instream, terminator);
        
        try 
        {
            //capture the start time of skipUntil
            long t0 = System.currentTimeMillis();
            
            //read until we receive terminator, reach maxBytes, or time out
            while (true) 
            {
                ++loopCount;

                if (instream.available() > 0)
                {
                    byte c = (byte)instream.read();
                
                    if (c == terminator[termIndex])
                    {
                        termIndex++;
                    }
                    else
                    {
                        termIndex = 0;

                        //this may be the start of the terminator as well
                        if (c == terminator[0])
                            termIndex++;
                    }
                     
                    bytesSkipped++;
                    
                    //if the termIndex is the same size as the 
                    //terminator you got it 
                    if (termIndex == terminator.length)
                        return (bytesSkipped - terminator.length);
                }
                else
                {
                    //Thread.yield();
                    StopWatch.delay(50);
                }

                //if maxBytes were specified, checkem
                if (maxBytes > 0)
                    if (bytesSkipped >= maxBytes) 
                        throw new Exception("SkipUntil() Exceeded maxBytes" );
                               
                // Check timeout; exit if expired
                if(timeout > 0)
                {
                    elapsed = System.currentTimeMillis() - t0;
                    if (elapsed > timeout)
                    {
                        if ((instream.available() > 0) && !timeoutIncreased)
                        {
                            timeoutIncreased = true;
                            //give the the method 10% of timeout more 
                            //time to get the bytes
                            timeout = elapsed + (timeout / 10);
                            // _logger.debug("*** skipUntil timeout increased");
                            // _logger.debug("instream.available() = " + instream.available());
                            // _logger.debug("timeout = " + timeout);
                        }
                        else
                        {
                            throw new TimeoutException("SkipUntil() Timed out");
                        }
                    }
                }
            }
        }
        finally 
        {
            // _logger.debug("*** leaving skipUntil()");
            // _logger.debug("bytesSkipped = " + bytesSkipped);
            // _logger.debug("instream.available() = " + instream.available());
            // _logger.debug("elapsed = " + elapsed);
            // _logger.debug("timeoutIncreased = " + timeoutIncreased);
            // _logger.debug("loopCount = " + loopCount);
        }
    }

    /** Skip streaming characters until specified terminator is encountered or 
	until buffer if full (deprecated) 
        Note that it is possible to ignore timeout by setting it to 0. 
        @param instream input stream
	@param terminator termination character string
	@param timeout timeout in milliseconds
     */
    public static int skipUntil(InputStream instream, 
                                byte[] terminator, 
                                long timeout)
	throws TimeoutException, IOException, NullPointerException, Exception
    {
        return skipUntil(instream, terminator, timeout, 0);
    }


    /** Verify valid arguments for readUntil() method. 
     */
    private static void checkReadUntilArgs(InputStream input, 
                                           byte[] output, 
                                           byte[] terminator) 
	throws NullPointerException 
    {

        if (input == null) 
        {
            throw new NullPointerException("readUntil called with "
                                           + "null InputStream");
        }

        if (output == null) 
        {
            throw new NullPointerException("readUntil called with "
                                           + "null output buffer");
        }

        if (terminator == null) 
        {
            throw new NullPointerException("readUntil called with "
                                           + "null terminator");
        }
    }
    

    /** Verify valid arguments for skipUntil() method. 
     */
    private static void checkSkipUntilArgs(InputStream input, 
                                           byte[] terminator) 
	throws NullPointerException 
    {
        if (input == null) 
        {
            throw new NullPointerException("skipUntil called with "
                                           + "null InputStream");
        }

        if (terminator == null) 
        {
            throw new NullPointerException("skipUntil called with "
                                           + "null terminator");
        }
    }


    /** Read characters from input stream into buffer, until specified
	number of characters have been read, or operation times out.
	@param instream input stream
	@param outbuf output buffer
	@param startIndex start filling output buffer fromt this index
	@param nBytes number of bytes to read
	@param timeout in millisec
    */
    public static int readBytes(InputStream instream, 
                                byte[] outbuf, 
				int startIndex,
                                int nBytes,
                                long timeout) 
	throws IOException, Exception 
    {
	// total bytesRead
        int bytesRead = 0;
        // elapsed time out the read
	long elapsed = 0;
        // index into terminator
        int termIndex = 0;
        // single shot timeout boost 
        boolean timeoutIncreased = false;
        
	// capture the start time of readUnil
	long t0 = System.currentTimeMillis();
           
	// read until we receive nBytes bytes, or time out
	while (bytesRead < nBytes) {
                
	    if (instream.available() > 0) {
		byte c = (byte)instream.read();
		outbuf[startIndex + bytesRead] = c;
		bytesRead++;

		if (bytesRead >= outbuf.length) {
		    return bytesRead;
		}

	    }
	    else {
		//Thread.yield();
		StopWatch.delay(50);
	    }

	    elapsed = System.currentTimeMillis() - t0;

	    if (elapsed > timeout) {
		if ((instream.available() > 0) && !timeoutIncreased) {
		    timeoutIncreased = true;
		    //give the the method 10% of timeout more time to get
		    //the bytes
		    timeout = elapsed + (timeout / 10);
		}
		else {
		    // Time's up!
		    break;
		}
	    }
	}

	return bytesRead;
    }
}


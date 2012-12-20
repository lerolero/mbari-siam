// Copyright 2001 MBARI
package org.mbari.siam.distributed.platform;

import java.io.*;

import org.apache.log4j.Logger;

import gnu.io.*;

//TODO : 


/**
    The PuckUtils class provides various methods to manipulate the puck such
    as erasing the non volitale memory and putting it into pass through mode.
*/

public class PuckUtils
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PuckUtils.class);
    
    private InputStream puckIn;
    private OutputStream puckOut;
    private boolean isInitialized = false;

    CommStreamSupport commSupport;

    private final int maxHeaderSize = 64;

    //number of times initPuckUtils tries to start communications
    //with puck before it just throws up its hands
    private final int maxInitRetries = 4;

/**
    Creates a PuckUtils objetc by using an intput and output stream to a device
    that implements the pucks serial protocol.
*/
    public PuckUtils(InputStream in, OutputStream out)
    {
        puckIn = in;
        puckOut = out;
    
        commSupport = new CommStreamSupport(puckIn, puckOut);
    
        isInitialized = initPuckUtils();
    }


/**
    Creates a PuckUtils object by using a SerialPort object to a device that
    supports the pucks serial protocol.
*/
    public PuckUtils(SerialPort serial_port)
    {
        try
        {
            puckIn = serial_port.getInputStream();
            puckOut = serial_port.getOutputStream();
        }
        catch (IOException e)
        {
            _log4j.error("Failed to acquire Serial Streams."+e);
        }
    
        commSupport = new CommStreamSupport(puckIn, puckOut);
    
        isInitialized = initPuckUtils();
    }

/**
    Returns a puck header entry.  Puck header entries are strings that can 
    be used by a host to describe the contents of the pucks non-volatile
    memory.
*/
    public String getPuckHeader(int entry) throws IOException
    {
        byte[] response = new byte[maxHeaderSize];
        String cmd_str;
        String response_str;

        if ( !isInitialized )
            throw new IOException("PuckUtils not initialized");

        if ( (entry < 0) || (entry > 3) )
            throw new IOException("puck error, header entry out of range");
        
        //form command string 
        cmd_str = "get h" + entry + "\r";
        
        //send the cmd string
        puckOut.write(cmd_str.getBytes());

        //wait for 65 bytes to come back
        if ( !commSupport.waitForChars(65))
            throw new IOException("puck error, failed to receive header value");

        for (int i = 0; i < 64; ++i)
            response[i] = (byte)puckIn.read();

        response_str = new String(response);
        
        //read out the \r and toss in the bit bucket
        puckIn.read();

        //remove whitespace from string and return
        return response_str.trim();

    }

/**
    Sets a puck header entry.  Puck header entries are strings that can 
    be used by a host to describe the contents of the pucks non-volatile
    memory.
*/
    public void setPuckHeader(int entry, String val) throws IOException
    {
        String cmd_str;

        if ( !isInitialized )
            throw new IOException("PuckUtils not initialized");
        
        if ( (entry < 0) || (entry > 3) )
            throw new IOException("puck error, header entry out of range");
        
        if ( val.length() > maxHeaderSize)
            throw new IOException("puck error, header val too large");
        
        //form command string 
        cmd_str = "set h" + entry + " " + val + "\r";
        
        //send the cmd string
        puckOut.write(cmd_str.getBytes());
        
        //wait for CR
        if ( !commSupport.waitForChars(1) )
            throw new IOException("puck i/o error");

        //check for CR
        if ( puckIn.read() != '\r' )
            throw new IOException("puck error setting header val");
    }

/**
    Erases pucks non-volatile memory.  The erasePuck method erases all of the
    pucks headers as well was all of the pucks bulk non-volatile storage.
*/
    public void erasePuck() throws IOException
    {
        
        if ( !isInitialized )
            throw new IOException("PuckUtils not initialized");
        
        //send the cmd
        puckOut.write("erase_puck\r".getBytes());
        
        //wait for CR
        if ( !commSupport.waitForChars(1) )
            throw new IOException("puck i/o error");

        //check for CR
        if ( puckIn.read() != '\r' )
            throw new IOException("puck error while erasing puck");
    }

/**
    Places the puck into pass through mode.  When the puckOut method is
    called the serial connection is handed over to the instrument.
*/
    public void puckOut() throws IOException
    {
        if ( !isInitialized )
            throw new IOException("PuckUtils not initialized");
        
        //send the cmd
        puckOut.write("puck out\r".getBytes());

        //this version of PLP sends back a CR as a last good bye,
        //may not need or want to do this in the future

        //wait for CR
        if ( !commSupport.waitForChars(1) )
            throw new IOException("puck i/o error");

        //check for CR
        if ( puckIn.read() != '\r' )
            throw new IOException("puck error shutting puck down");
    }

/**
    This method is used to synchronize communications with the puck.  Though 
    called by the constructor when the utility object is created it may be 
    necessary to call this method if a puck was not attached when the utility
    object was created.  When this method is called after the utility object
    has been created it will try to synchronize communications with the puck.
*/    

    public boolean initPuckUtils()
    {
        for (int i = 0; i < maxInitRetries; ++i)
        {
            try
            {
                //clear out the input buffer
                puckIn.skip(puckIn.available());
                
                //send CR
                puckOut.write('\r');

                //wait for CR
                if ( !commSupport.waitForChars(1) )
                    throw new IOException("puck i/o error");

                //check for CR
                if ( puckIn.read() != '\r' )
                    throw new IOException("error synchronizing with puck");
            }
            catch(IOException e)
            {
                _log4j.error("failed to synchronize on try : " + i);
                _log4j.error("received : " + e.getMessage());
                if (i > maxInitRetries)
                    return false;
            }
        }
        return true;
    }

}

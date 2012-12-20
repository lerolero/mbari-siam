// Copyright 2001 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mbari.siam.operations.utils.PuckCommandSpec;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
    The PuckInputStream constructor needs an input and output stream 
    to a device that supports the puck serial protocol.  The
    PuckInputStream provides a basic InputStream by using the pucks
    serial protocol to query the device for data.
*/

public class PuckInputStream extends InputStream
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PuckInputStream.class);

    private InputStream puckIn;
    private OutputStream puckOut;

    //stream position variables
    private int currentPosition = 0;
    private int dataBufferPosition = 0;
    private boolean streamOpen = false;
    private byte[] dataBuffer = new byte[_BUFFER_SIZE];
    private byte[] _tempBuffer = new byte[_BUFFER_SIZE + 16];
    private static final byte[] PUCK_PROMPT = "RDY\r".getBytes();

    //number of times initPuckStream tries to start communications
    //with puck before it just throws up its hands
    private final int maxInitRetries = 4;
    private static final int _BUFFER_SIZE = 256;

    /** Creates a PuckInputStream by using an intput and output stream to 
    device that implements the pucks serial protocol.*/
    public PuckInputStream(InputStream in, OutputStream out)
    {
        puckIn = in;
        puckOut = out;
        
        initPuckStream();
    }

    /** Set the PuckInputStream to the begining.  The puck stores binary data
    that a host may use for various purposes.  The resetStream method allows 
    the internal stream pointer to be set back to the begining of the data if
    necessary. */
    public void resetStream() throws IOException
    {
        //set the read position to zero
        String setAddressCommand = PuckCommandSpec.getInstance().setAddressOfMemoryPointerCommand() +" 0\r"; 
        puckOut.write(setAddressCommand.getBytes());

        //wait for CR
        try
        {
            StreamUtils.skipUntil(puckIn, PUCK_PROMPT, 500);
        }
        catch(Exception e)
        {
            throw new IOException("puck error: " + e.getMessage());
        }
        
        currentPosition = 0;
        dataBufferPosition = 0;
        streamOpen = true;
    }
    
/**
    Closes the PuckInputStream.  Calling the close method releases the 
    control the PuckInputStream has over the puck.  Any subsequent call 
    to a PuckInputStream method will cause an IOException to be thrown.
*/
    public void close() throws IOException
    {
        currentPosition = 0;
        streamOpen = false;
    }

/**
    The available method returns the number of buffered puck bytes available 
    to be read.  The PuckInputStream extracts bytes from the puck in packets.  
    Calling the PuckInputStream available method will return the number of 
    bytes left in the last packet received from the puck.
*/
    public int available() throws IOException
    {
        if ( !streamOpen )
            throw new IOException("puck stream not open");
        
        return (_BUFFER_SIZE - dataBufferPosition);
    }

/**
    The read method returns the next available byte from the puck.  
    If the number of bytes read from the PuckInputStream exceeds the 
    number of bytes of binary data stored on the puck zeroes will 
    be returned.
*/
    public int read() throws IOException
    {
        if (dataBufferPosition < _BUFFER_SIZE)
        {
            //increment the stream position
            currentPosition++;
            //return the next byte in the data buffer
            return (int)(0xff & dataBuffer[dataBufferPosition++]);
        }
        else
        {
            //get another packet of data
            getPacket();

            //clear the internal data buffer position
            dataBufferPosition = 0;

            //increment the stream position
            currentPosition++;
            
            //return the next byte in the buffer position
            return (int)(0xff & dataBuffer[dataBufferPosition++]);
        }
    }

/**
    Skips over and discards n bytes of data from the input stream. The skip 
    method repositions the internal stream pointer to the position indicated
    by n.  The skip method will return the number of bytes actually skipped.
*/    
    public long skip(long n) throws IOException
    {
        if ( !streamOpen )
            throw new IOException("puck stream not open");

//need to make this more efficient with puck SA command
        long i;
        for(i = 0; i < n; i++)
            read();

        return i;
    }

    /** Requests a byte packet from the puck. */
    private void getPacket() throws IOException
    {
        if ( !streamOpen )
            throw new IOException("puck stream not open");

        //send request to puck for data
        puckOut.write(new String(PuckCommandSpec.getInstance().readFromMemoryCommand() + " ").getBytes());
        puckOut.write(Integer.toString(_BUFFER_SIZE).getBytes());
        puckOut.write("\r".getBytes());
        
        //read the data back
        try
        {
            StreamUtils.readUntil(puckIn, _tempBuffer, PUCK_PROMPT, 3000);
        }
        catch(Exception e)
        {
            throw new IOException("puck error: " + e.getMessage());
        }

        //check for errors 
        String tmp_string = new String(_tempBuffer, 0, 3);
        if (tmp_string.compareTo("ERR") == 0)
        {
            throw new IOException("puck error: " + 
                                  new String(_tempBuffer, 0, 8));
        }

        //skip the first '[' and copy the 
        //data to the data internal data buffer
        for (int i = 0; i < _BUFFER_SIZE; i++)
            dataBuffer[i] = _tempBuffer[i + 1]; 

    }

/**
    This method is used to synchronize communications with the puck.  Though 
    called by the constructor when the stream is created it may be necessary
    to call this method if a puck was not attached when the stream object 
    was created.  When this method is called after the stream has been created
    it will try to synchronize communications with the puck.
*/    
    public boolean initPuckStream()
    {
        
        for (int i = 0; i < maxInitRetries; ++i)
        {
            try
            {
                //clear out the input buffer
                puckIn.skip(puckIn.available());
                
                //reset the puck internal memory pointer
                resetStream();

                //prime the data buffer with it's first packet
                getPacket();

                _log4j.debug("initPuckStream() successful");
                return true;
            }
            catch(IOException e)
            {
                _log4j.error("failed to resetStream() on try : " + i);
                _log4j.error("received : " + e.getMessage());
                if (i > maxInitRetries)
                    return false;
            }
        }

        _log4j.error("initPuckStream() failed: max retires exceeded");
        return false;
    }
}

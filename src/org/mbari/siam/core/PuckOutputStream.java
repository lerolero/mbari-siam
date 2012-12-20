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
    The PuckOutputStream is used to store binary data on the
    pucks non volatile memory.  The PuckOutputStream can only 
    write to a blank puck.  If the the puck is not blank this 
    implementation of the PuckOutputStream will throw an 
    IOException if a puck write is attepmted.  To erase the
    pucks non volatile memory the erasePuck method can be
    used from PuckUtils.
    
    @see org.mbari.siam.utils.PuckUtils
*/
public class PuckOutputStream extends OutputStream
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PuckOutputStream.class);

    private InputStream puckIn;
    private OutputStream puckOut;

    //stream position variables
    private int currentPosition = 0;
    private int dataBufferPosition = 0;
    private boolean streamOpen = false;
    private byte[] dataBuffer = new byte[BUFFER_SIZE];

    //number of times initPuckStream tries to start communications
    //with puck before it just throws up its hands
    private final int maxInitRetries = 4;
    private static final int BUFFER_SIZE = 32;
    private static final byte[] PUCK_PROMPT = "RDY\r".getBytes();

        
    /**
        Creates a PuckOutputStream by using an intput and output stream to a to device
        that implements the pucks serial protocol.
    */
    public PuckOutputStream(InputStream in, OutputStream out)
    {

        puckIn = in;
        puckOut = out;
        
        //try to synch up with the puck
        if ( initPuckStream() != true )
        {
            _log4j.error("failed to open PuckOutPutStream");
            return;
        }
    }


    /**
        Closes the PuckOutputStream.  The close method must always be called 
        after all the binary data has been written to the puck.  Failure to
        call the close method could result in a portion of the binary data
        not being stored to the pucks non volatile memory.
    */
    public void close() throws IOException
    {
        //make sure the last packet is written
        while (dataBufferPosition < BUFFER_SIZE)
            write((int)0xFF);

        writePacket();

        //send out flush memory command
        puckOut.write(new String(PuckCommandSpec.getInstance().flushMemoryCommand() + "\r").getBytes());
        
        //skip until RDY prompt
        try
        {
            StreamUtils.skipUntil(puckIn, PUCK_PROMPT, 1000);
        }
        catch(Exception e)
        {
            throw new IOException("puck error: " + e.getMessage());
        }
        
        //close the stream        
        streamOpen = false;
    }

    /**
        Writes a byte to the pucks non volatile memory.  The write method stores
        a byte of data in an internal packet buffer.  When an entire packet is
        formed it is sent to the puck to be stored on the pucks non volatile
        memory.  If the close method is not called after the last byte is written
        then a portion of the data sent to the puck may be lost.
    */
    public void write(int b) throws IOException
    {
        if (dataBufferPosition < BUFFER_SIZE)
        {
            //increment the stream position
            currentPosition++;
            //store a byte in the data buffer
            dataBuffer[dataBufferPosition++] = (byte)b;
        }
        else
        {
            //write another packet of data
            writePacket();

            //clear the internal data buffer position
            dataBufferPosition = 0;

            //increment the stream position
            currentPosition++;

            //stroe the next byte in the buffer position
            dataBuffer[dataBufferPosition++] = (byte)b;
        }
    }

    /**
        Internal method for writing a packet of data to the pucks non-volatile
        memory.
    */
    private boolean writePacket() throws IOException
    {
        //send out write command
        puckOut.write(new String(PuckCommandSpec.getInstance().writeToMemoryCommand() + " ").getBytes());
        puckOut.write(Integer.toString(BUFFER_SIZE).getBytes());
        puckOut.write("\r".getBytes());

        //send out data
        for(int i = 0; i < BUFFER_SIZE; i++)
            puckOut.write((int)dataBuffer[i]);

        //skip until RDY prompt
        try
        {
            StreamUtils.skipUntil(puckIn, PUCK_PROMPT, 500);
        }
        catch(Exception e)
        {
            throw new IOException("puck error: " + e.getMessage());
        }

        return true;
    }

    /** Get the attention of the puck and erase it's contents */    
    public boolean initPuckStream()
    {
        for (int i = 0; i < maxInitRetries; ++i)
        {
            try
            {
                //clear out the input buffer
                puckIn.skip(puckIn.available());
                
                //send CR
                puckOut.write('\r');

                //skip until RDY prompt
                StreamUtils.skipUntil(puckIn, PUCK_PROMPT, 500);

                //erase the puck
                puckOut.write(new String(PuckCommandSpec.getInstance().eraseMemoryCommand() + "\r").getBytes());

                //skip until RDY prompt
                StreamUtils.skipUntil(puckIn, PUCK_PROMPT, 5000);

            }
            catch(Exception e)
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

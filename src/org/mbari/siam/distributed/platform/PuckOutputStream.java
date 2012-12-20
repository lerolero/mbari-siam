// Copyright 2001 MBARI
package org.mbari.siam.distributed.platform;

import java.lang.*; 
import java.io.*;
import gnu.io.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

//TODO : 
//  *Need to implement retry mechinism on writePacket method
//  *Need packet checksum scheme for packet writes
//  *Make mod to output stream with start up init retry as in input stream


/**
    The PuckOutputStream is used to store binary data on the
    pucks non volatile memory.  The PuckOutputStream can only 
    write to a blank puck.  If the the puck is not blank this 
    implementation of the PuckOutputStream will throw an 
    IOException if a puck write is attepmted.  To erase the
    pucks non volatile memory the erasePuck method can be
    used from PuckUtils.
    
    @see PuckUtils
*/
public class PuckOutputStream extends OutputStream
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PuckOutputStream.class);

    private boolean streamOpen = false;
    private InputStream puckIn;
    private OutputStream puckOut;

    CommStreamSupport commSupport;
    
    //stream position variables
    private int currentPosition = 0;
    private final int packetSize = 16;
    private byte[] currentPacket = new byte[packetSize];

    //number of times initPuckStream tries to start communications
    //with puck before it just throws up its hands
    private final int maxInitRetries = 4;

        
    /**
        Creates a PuckOutputStream by using an intput and output stream to a to device
        that implements the pucks serial protocol.
    */
    public PuckOutputStream(InputStream in, OutputStream out)
    {
        puckIn = in;
        puckOut = out;
        
        commSupport = new CommStreamSupport(puckIn, puckOut);
        
        //try to synch up with the puck
        if ( initPuckStream() != true )
        {
            _log4j.error("failed to open PuckOutPutStream");
            return;
        }

        try
        {
            streamOpen = isPuckBlank();
        }
        catch (IOException e)
        {
            _log4j.error("Failed puck blank check.");
            return;
        }
    }

    /**
        Creates a PuckOutputStream by using a SerialPort Object to a to device
        that implements the pucks serial protocol.
    */
    public PuckOutputStream(SerialPort serial_port)
    {
        try
        {
            puckIn = serial_port.getInputStream();
            puckOut = serial_port.getOutputStream();
        }
        catch (IOException e)
        {
            _log4j.error("Failed to acquire Serial Streams.");
            return;
        }
        
        commSupport = new CommStreamSupport(puckIn, puckOut);

        //try to synch up with the puck
        if ( initPuckStream() != true )
        {
            _log4j.error("failed to open PuckOutPutStream");
            return;
        }
        
        try
        {
            streamOpen = isPuckBlank();
        }
        catch (IOException e)
        {
            _log4j.error("Failed puck blank check.");
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
        byte rx_byte;

        //if a partial packet remains pad it with zeros
        //and send it call flush
        if ( currentPosition != 0)
        {
            for(int i = currentPosition; i < packetSize; ++i)
                currentPacket[i] = 0;
        }

        //send last packet
        writePacket(true);
        
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
        if ( !streamOpen )
            throw new IOException("puck I/O error, stream closed");
        
        if ( currentPosition < packetSize)
            currentPacket[currentPosition++] = (byte)b;
        
        if ( currentPosition == packetSize)
        {
            writePacket(false);
            currentPosition = 0;
        }
    }

    /**
        Internal method for writing a packet of data to the pucks non-volatile
        memory.
    */
    private boolean writePacket(boolean flush) throws IOException
    {
        byte rx_byte;
        
        //send write command
        puckOut.write("write\r".getBytes());

        //send data to write
        puckOut.write(currentPacket);

        //wait for CR
        if ( !commSupport.waitForChars(1) )
            throw new IOException("puck I/O error, did not receive CR");

        //make sure you got a CR
        rx_byte = (byte)puckIn.read();

        if ( rx_byte != '\r' )
        {
            //put out bad char
            System.err.write(rx_byte);
            
            //wait a bit
            commSupport.waitForChars(3);
            
            //put out anything else
            while ( puckIn.available() > 0 )
                System.err.write(puckIn.read());
            
            throw new IOException("puck I/O error, did not receive CR");
        }
        
        if ( flush )
        {
            //make sure any data stored on the puck write buffer 
            //is written to flash
        
            //send command to puck
            puckOut.write("flush\r".getBytes());

            //wait for CR
            if ( !commSupport.waitForChars(1) )
                throw new IOException("puck I/O error, did not receive CR");

            //make sure you got a CR
            rx_byte = (byte)puckIn.read();
            
            if ( rx_byte != '\r' )
                throw new IOException("puck I/O error, did not receive CR");
        }

        return true;
    }

    /**
        Internal method used to determine if the puck is blank.  The
        PuckOutputStream currently only supports writing to a blank puck
    */
    private boolean isPuckBlank() throws IOException
    {
        //serial communications variables
        byte[] response = new byte[32];
        int response_size = 0;
        String response_string;

        //send GET WP command
        puckOut.write("get wp\r".getBytes());

        //wait for at leat 5 characters 'WP 0\r'
        if ( !commSupport.waitForChars(5) )
            return false;

        while ( (puckIn.available() > 0) && (response_size < 32) )
            response[response_size++] = (byte)puckIn.read();

        response_string = new String(response, 0, response_size);
        //get rid of leading 'WP '
        response_string = response_string.substring(3);
        //get rid of any whitespace chars
        response_string = response_string.trim();

        try
        {
            if ( Integer.parseInt(response_string) == 0 )
                return true;
        }
        catch (NumberFormatException e)
        {
            _log4j.error("Received " + response_string + " instead of 0");
            return false;
        }
        
        return false;
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

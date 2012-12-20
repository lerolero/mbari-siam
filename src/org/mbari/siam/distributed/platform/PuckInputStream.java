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
//  *fix skip method bug before it's too late...
//  *use larger packet size with checksum and retry
//  *query puck for PLP version and don't initiate 
//      xfer unless there is a match
//  *Need packet retry mechanism implemented
//  *Need packet checksum scheme


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

    CommStreamSupport commSupport;
    
    //stream position variables
    private int currentPosition = 0;
    private final int packetSize = 16;
    private boolean currentPacketValid = false;
    private byte[] currentPacket = new byte[packetSize];
    private boolean streamOpen = false;

    //number of times initPuckStream tries to start communications
    //with puck before it just throws up its hands
    private final int maxInitRetries = 4;
    
/**
    Creates a PuckInputStream by using an intput and output stream to device
    that implements the pucks serial protocol.
*/
    public PuckInputStream(InputStream in, OutputStream out)
    {
        puckIn = in;
        puckOut = out;
        
        commSupport = new CommStreamSupport(puckIn, puckOut);
        
        initPuckStream();
    }

/**
    Creates a PuckInputStream by using a SerialPort object to a device that
    supports the pucks serial protocol.
*/
    public PuckInputStream(SerialPort serial_port)
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

        initPuckStream();
    }

/**
    Set the PuckInputStream to the begining.  The puck stores binary data
    that a host may use for various purposes.  The resetStream method allows 
    the internal stream pointer to be set back to the begining of the data if
    necessary.
*/
    public void resetStream() throws IOException
    {
        //set the read position to zero
        puckOut.write("set rp 0\r".getBytes());

        //wait for CR
        if ( !commSupport.waitForChars(1) )
            throw new IOException("failure to communicate with puck");

        if ( (puckIn.read()) != '\r' )
            throw new IOException("received error from puck puck");
        
        currentPosition = 0;
        currentPacketValid = false;
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
        currentPacketValid = false;
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
        
        if ( !currentPacketValid )
            return 0;
        
        return ( packetSize - (currentPosition % packetSize) );
    }

/**
    The read method returns the next available byte from the puck.  
    If the number of bytes read from the PuckInputStream exceeds the 
    number of bytes of binary data stored on the puck zeroes will 
    be returned.
*/
    
    public int read() throws IOException
    {
        int ret_val;
        int packet_pos;
        int packet_idx;

        if ( !streamOpen )
            throw new IOException("puck stream not open");


        packet_idx = currentPosition % packetSize;

        if ( !currentPacketValid && ((currentPosition % packetSize) == 0) )
        {
            if  ( !getPacket(currentPosition) )
                throw new IOException("Failed to get a packet from the puck.");
            
            currentPacketValid = true;
            ++currentPosition;
            return (int)currentPacket[packet_idx];
        }

        if ( currentPacketValid )
        {
            ++currentPosition;
            ret_val = (int)currentPacket[packet_idx];

            if ( (currentPosition % packetSize) == 0 )
                currentPacketValid = false;

            return ret_val;
        }
        
        packet_pos = currentPosition - (currentPosition % packetSize);
        
        //get the packet at packet_pos
        if  ( !getPacket(packet_pos) )
            throw new IOException("Failed to get a packet from the puck.");
        
        ++currentPosition;
        ret_val = (int)currentPacket[packet_idx];

        if ( (currentPosition % packetSize) == 0 )
            currentPacketValid = false;

        return ret_val;
    }

/**
    Skips over and discards n bytes of data from the input stream. The skip 
    method repositions the internal stream pointer to the position indicated
    by n.  The skip method will return the number of bytes actually skipped.
*/    
    public long skip(long n) throws IOException
    {
        int old_packet_pos;
        
        if ( !streamOpen )
            throw new IOException("puck stream not open");

        old_packet_pos = currentPosition - (currentPosition % packetSize);
        
        //adjust the internal pointer to the new position
        currentPosition = (int)n;

        //tag the cache as bad if you have skipped out of the current packet
        if ( (old_packet_pos + packetSize - 1) < currentPosition )
            currentPacketValid = false;

        return n;
    }

/**
    Requests a byte packet from the puck.
*/
    private boolean getPacket(int address) throws IOException
    {
        byte rx_byte;

        if ( !streamOpen )
            throw new IOException("puck stream not open");
        
        //if address is not 
        
        //send command to puck
        puckOut.write("read\r".getBytes());

        //wait for (packetSize + 1) chars if time out return false
        if ( !commSupport.waitForChars(packetSize + 1))
        {
            _log4j.error("Timed out waiting for data from puck.");
            return false;
        }
        
        //read the bytes and store the first sixteen bytes in classBytes
        for (int i = 0; i < packetSize; ++i)
            currentPacket[i] = (byte)puckIn.read();

        //read out the \r and toss it into the bit bucket
        puckIn.read();
        
        return true;
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
                
                resetStream();
            }
            catch(IOException e)
            {
                _log4j.error("failed to resetStream() on try : " + i);
                _log4j.error("received : " + e.getMessage());
                if (i > maxInitRetries)
                    return false;
            }
        }
        return true;
    }

}

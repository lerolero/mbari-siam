// Copyright 2003 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;

public class InstrumentPortInputStream extends InputStream
{
    private InstrumentPort _instrumentPort = null;
    private InputStream _fromInstrument = null;

    /** Create a new InstrumentPortInputStream */
    public InstrumentPortInputStream(InstrumentPort port, InputStream is)
    {
        _instrumentPort = port;
        _fromInstrument = is;
    }

    /** returns the number of bytes that can be read from this inputstream 
    without blocking */
    public int available() throws IOException
    {
        if ( _instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort suspended");

        return _fromInstrument.available();
    }
    
    /** close the inputstream */
    public void close() throws IOException 
    {
        if ( _instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort suspended");

        _fromInstrument.close();
    }
    
    /** read a character from the inputstream */
    public int read() throws IOException 
    {
        if ( _instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort suspended");

        return _fromInstrument.read();
    }

    /** skip n bytes of data from the inputstream */
    public long skip(long n) throws IOException 
    {
        if ( _instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort suspended");

        return _fromInstrument.skip(n);
    }    
}

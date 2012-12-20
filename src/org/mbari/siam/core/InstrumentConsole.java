// Copyright 2003 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InstrumentConsole
{
    private InputStream _fromInstrument;
    private OutputStream _toInstrument;
    private InstrumentPort _instrumentPort;

    
    /** create a new InstrumentConsole */
    public InstrumentConsole(InstrumentPort instPort, 
                             InputStream is,
                             OutputStream os) throws IOException
    {
        if (instPort == null)
            throw new IOException("InstrumentPort is null");

        _instrumentPort = instPort;
        //these streams need to be directly to the serial port
        _fromInstrument = is;
        _toInstrument = os;
    }

    /** write a byte to the instrument */
    public void write(int b) throws IOException
    {
        if ( !_instrumentPort.isSuspended()  )
            throw new IOException("InstrumentPort not suspended");

        _toInstrument.write(b);
    }

    /** read a byte from the instrument */
    public int read() throws IOException
    {
        if ( !_instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort not suspended");
    
        return _fromInstrument.read();
    }

    /** see if any bytes are available */
    public int available() throws IOException
    {
        if ( !_instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort not suspended");
    
        return _fromInstrument.available();
    }


}

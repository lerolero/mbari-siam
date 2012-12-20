// Copyright 2003 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.log4j.Logger;

/**
InstrumentPortOutputStream enforces inter-byte delay when writing to the
instrument */
public class InstrumentPortOutputStream extends OutputStream {
    private static final int DEFAULT_INTERBYTE_MSEC = 50;
    private InstrumentPort _instrumentPort = null;
    private OutputStream _toInstrument = null;
    private int _interByteMsec = DEFAULT_INTERBYTE_MSEC;

    protected static Logger _log4j = 
	Logger.getLogger(InstrumentPortOutputStream.class);

    /** Create a new InstrumentPortInputStream */
    public InstrumentPortOutputStream(InstrumentPort port, OutputStream os)
    {
        _instrumentPort = port;
        _toInstrument = os;

	if (_instrumentPort instanceof HttpInstrumentPort) {
	    // No interbyte delay for HTTP instruments
	    setInterByteMsec(0);
	}
    }

    /** Write a char to the outputstream */
    public void write(int b) throws IOException 
    {
        if ( _instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort suspended");

        _toInstrument.write(b);
    }

    /** Writes b.length bytes from specified byte array to output stream,
     enforcing inter-byte delay. */
    public void write(byte[] b) throws IOException {
	write(b, 0, b.length);
    }


    /** Writes nBytes bytes from specified byte array starting at 
	specified offset to output stream, enforcing inter-byte delay. */
    public void write(byte[] b, int offset, int nBytes) 
	throws IOException {

	if (_interByteMsec == 0) {
	    _log4j.debug("write() - no interbyte delay");
	    _toInstrument.write(b, offset, nBytes);
	}
	else { 
	    _log4j.debug("write() - enforce interbyte delay");
	    for (int i = 0; i < nBytes; i++) {
		write(b[i+offset]);
		super.flush();

		try {
		    Thread.sleep(_interByteMsec);
		}
		catch (InterruptedException e) {
		}
	    }
	}
    }


    /** Flush the outputstream */
    public void flush() throws IOException
    {
        if ( _instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort suspended");

        _toInstrument.flush();
    }

    /** Close the outputstream */
    public void close() throws IOException
    {
        if ( _instrumentPort.isSuspended() )
            throw new IOException("InstrumentPort suspended");

        _toInstrument.close();
    }


    /** Set inter-byte millisec delay to specified value. */
    public void setInterByteMsec(int interByteMsec) {
	_interByteMsec = interByteMsec;
    }
}

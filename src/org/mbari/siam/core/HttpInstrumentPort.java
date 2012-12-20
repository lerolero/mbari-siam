/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;

/**
   InstrumentPort implementation for Web-based instruments with URL interface.
*/
public class HttpInstrumentPort 
    extends BaseInstrumentPort implements InstrumentPort {

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(HttpInstrumentPort.class);

    protected String _name;

    protected InputStream _input;
    protected InstrumentPortOutputStream _output;


    public HttpInstrumentPort(String serverURLString, PowerPort powerPort) 
	throws MalformedURLException, IOException {

	super(powerPort);

	_name = new String(serverURLString);

	OutputStream output = new OutputStream(serverURLString);
	_output = new InstrumentPortOutputStream(this, output);

	_input = new InputStream(output);
    }


    /** initialize the InstrumentPort */
    public void initialize() throws InitializeException
    {
	_log4j.debug("initialize()");

	super.initialize();

        return;
    }

    /** resumes instrument comms from suspended state */
    public void resume() {
	return;
    }

    /** suspends comms so another application can communicate with the 
    instrument */
    public void suspend() {
	return;
    }

    /** get the name of the communications port */
    public String getCommPortName() {
	return _name;
    }
    
    /** get an InputStream to the instrument */
    public java.io.InputStream getInputStream() throws IOException {
	return _input;
    }
    
    /** get an OutputStream to the instrument */
    public InstrumentPortOutputStream getOutputStream() throws IOException {
	return _output;
    }

    /** get a console to an Instrument **/
    public InstrumentConsole getInstrumentConsole() throws IOException {
	throw new IOException("Not implemented for HttpInstrument");
    }


    /** 
	Applications send "commands" through the OutputStream, in the form
	of URLs. The OutputStream is constructed with the server address.
	Subsequent "write" operations consist of relative URLS, which 
	the OutputStream prepends with the server address to form an absolute
	URL. */
    class OutputStream extends java.io.OutputStream {

	URL _serverURL;
	URLConnection _currentConnection;

	public OutputStream(String serverURLString) 
	    throws MalformedURLException, IOException{

	    _serverURL = new URL(serverURLString);
	    _currentConnection = _serverURL.openConnection();
	}


	public void close() throws IOException {
	    // _currentConnection.getOutputStream().close();
	    _log4j.debug("close() does nothing");
	}

	public void flush() throws IOException {
	    // _currentConnection.getOutputStream().flush();
	    _log4j.debug("flush() does nothing");
	}

	/** Form absolute URL from _serverURL and relative "command" URL,
	    and connect to it. */
	public void write(byte[] relativeURLString) throws IOException {
	    try {
		URL currentURL = 
		    new URL(_serverURL, new String(relativeURLString));

		_log4j.debug("write(): currentURL = " + currentURL);

		_currentConnection = currentURL.openConnection();
		_currentConnection.connect();
		
	    }
	    catch (Exception e) {
		_log4j.debug("write(): got exception " + e);
		throw new IOException(e.getMessage());
	    }
	}

	/** Form absolute URL from _serverURL and relative "command" URL,
	    and connect to it. */
	public void write(byte[] relativeURLString, int offset, int len) 
	    throws IOException {
	    try {
		URL currentURL = 
		    new URL(_serverURL, 
			    new String(relativeURLString, offset, len));

		_log4j.debug("write(3 args): currentURL = " + currentURL);

		_currentConnection = currentURL.openConnection();
		_currentConnection.connect();
		
	    }
	    catch (Exception e) {
		_log4j.debug("write(): got exception " + e);
		throw new IOException(e.getMessage());
	    }
	}


	public void write(int b) throws IOException {
	    throw new IOException("write(int b) not supported");
	}

	/** Get connection corresponding to URL resulting from latest write()
	    operation */
	public URLConnection getConnection() {
	    return _currentConnection;
	}
    }


    /** Input stream */
    public class InputStream extends java.io.InputStream {

	/** Referenced output stream holds latest URL resulting from 
	    output.write(). InputStream uses this to read the response 
	    from output.write() */
	OutputStream _output;

	public InputStream(OutputStream output) {
	    _output = output;
	}

	public int available() throws IOException {
	    return _output.getConnection().getInputStream().available();
	}

	public void close() throws IOException {
	    _output.getConnection().getInputStream().close();
	}

	public boolean markSupported() {
	    try {
		return _output.getConnection().getInputStream().markSupported();
	    }
	    catch (Exception e) {
		_log4j.error("markSupported()", e);
		return false;
	    }
	}

	public void mark(int readlimit) {
	    try {
		_output.getConnection().getInputStream().mark(readlimit);
	    }
	    catch (Exception e) {
		_log4j.error("mark()", e);
	    }
	}

	/** Read and discard input, return number of bytes read */
	public int read() throws IOException {
	    return _output.getConnection().getInputStream().read();
	}
       
	/** Read input into byte array, return number of bytes read */
	public int read(byte[] b) throws IOException {
	    _log4j.debug("read(byte[] b)");
	    return _output.getConnection().getInputStream().read(b);
	}

	/** Read input into byte array, return number of bytes read */
	public int read(byte[] b, int off, int len) 
	    throws IOException {
	    _log4j.debug("read(byte[] b, int off, int len)");
	    return _output.getConnection().getInputStream().read(b, off, len);
	}

	public void reset() throws IOException {

	    _output.getConnection().getInputStream().reset();
	}

	public long skip(long n) throws IOException {
	    return _output.getConnection().getInputStream().skip(n);
	}

    }
}

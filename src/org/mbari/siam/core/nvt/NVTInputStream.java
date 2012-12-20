/**
 * @Title Network Virtual Terminal (NVT) InputStream
 * @author Bob Herlien
 * @version $Revision: 1.1 $
 * @date 9 July 2009
 *
 * Copyright 2009 MBARI
 */

package org.mbari.siam.core.nvt;

import java.io.IOException;
import java.io.InputStream;
import it.m2.net.telnet.NVTCom;

import org.apache.log4j.Logger;

public class NVTInputStream extends InputStream
{
    private static Logger _log4j = Logger.getLogger(NVTInputStream.class);

    protected NVTSerialPort _nvtPort;

    /** Create a new NVTInputStream */
    public NVTInputStream(NVTSerialPort port)
    {
	_log4j.debug("NVTInputStream constructor for " + port.getName());
        _nvtPort = port;
    }

    /** returns the number of bytes that can be read from this inputstream 
    without blocking */
    public int available() throws IOException
    {
	return(_nvtPort.getNVT().available());
    }
    
    /** close the InputStream */
    public void close()
    {
	_nvtPort.closeInputStream();
    }
    
    /** Read next byte of data */
    public int read() throws IOException 
    {
	NVTCom nvt = _nvtPort.getNVT();

	try {
	    return(nvt.read());
	} catch (IOException e) {
	    _log4j.error("IOException on read(): " + e);
	    _nvtPort.close();
	    throw e;
	}
    }

    /** Read array of bytes */
    public int read(byte[] b) throws IOException 
    {
	NVTCom nvt = _nvtPort.getNVT();

	try {
	    return(nvt.read(b));
	} catch (IOException e) {
	    _log4j.error("IOException on read(byte[]): " + e);
	    _nvtPort.close();
	    throw e;
	}
    }

    /** Read array of bytes with offset */
    public int read(byte[] b, int off, int len) throws IOException 
    {
	NVTCom nvt = _nvtPort.getNVT();

	try {
	    return(nvt.read(b, off, len));
	} catch (IOException e) {
	    _log4j.error("IOException on read(byte[],int,int): " + e);
	    _nvtPort.close();
	    throw e;
	}
    }

    /** markSupported() returns false */
    public boolean markSupported()
    {
	return(false);
    }

    /** Does nothing */
    public void mark(int readlimit)
    {
    }
}

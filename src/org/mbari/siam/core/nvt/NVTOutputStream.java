/**
 * @Title Network Virtual Terminal (NVT) OutputStream
 * @author Bob Herlien
 * @version $Revision: 1.1 $
 * @date 9 July 2009
 *
 * Copyright 2009 MBARI
 */

package org.mbari.siam.core.nvt;

import java.io.IOException;
import java.io.OutputStream;
import it.m2.net.telnet.NVTCom;

import org.apache.log4j.Logger;

public class NVTOutputStream extends OutputStream
{
    private static Logger _log4j = Logger.getLogger(NVTOutputStream.class);

    protected NVTSerialPort _nvtPort;

    /** Create a new NVTOutputStream */
    public NVTOutputStream(NVTSerialPort port)
    {
	_log4j.debug("NVTOutputStream constructor for " + port.getName());
        _nvtPort = port;
    }

    /** close the OutputStream */
    public void close()
    {
	_nvtPort.closeOutputStream();
    }
    
    /** Write a byte of data */
    public void write(int b) throws IOException 
    {
	NVTCom nvt = _nvtPort.getNVT();

	try {
	    nvt.write(b);
	} catch (IOException e) {
	    _log4j.error("IOException on write(int): " + e);
	    _nvtPort.close();
	    throw e;
	}
    }

    /** Write b.len bytes of data */
    public void write(byte[] b) throws IOException 
    {
	NVTCom nvt = _nvtPort.getNVT();

	try {
	    nvt.write(b);
	} catch (IOException e) {
	    _log4j.error("IOException on write(byte[]): " + e);
	    _nvtPort.close();
	    throw e;
	}
    }

    /** Write len bytes of data starting at offset off */
    public void write(byte[] b, int off, int len) throws IOException 
    {
	NVTCom nvt = _nvtPort.getNVT();

	try {
	    nvt.write(b, off, len);
	} catch (IOException e) {
	    _log4j.error("IOException on write(byte[],int,int): " + e);
	    _nvtPort.close();
	    throw e;
	}
    }

    /** Flush the underlying OutputStream */
    public void flush() throws IOException 
    {
	_nvtPort.flush();
    }

}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.bbElec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import gnu.io.SerialPort;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.utils.StreamUtils;

/** Base class for Serial-to-whatever devices from B&B Electronics, Ottawa IL
 */
/*
 $Id: BBElec.java,v 1.7 2012/12/17 21:33:26 oreilly Exp $
 $Name: HEAD $
 $Revision: 1.7 $
 */

public class BBElec
{
    // CVS revision
    private static String _versionID = "$Revision: 1.7 $";
    protected static Logger _log4j = Logger.getLogger(BBElec.class);
    protected static final long DFLT_TMOUT = 1000;

    protected SerialPort	_serPort;
    protected InputStream	_in;
    protected OutputStream	_out;
    protected long		_tmout = DFLT_TMOUT;

    /** Constructor
	@param serport gnu.io.SerialPort that's connected to the device
	@param tmout Timeout for I/O in milliseconds
     */	
    public BBElec(SerialPort serport, long tmout) throws IOException
    {
	_serPort = serport;
	_in = _serPort.getInputStream();
	_out = _serPort.getOutputStream();
	_tmout = tmout;
    }

    /** Constructor
	@param serport gnu.io.SerialPort that's connected to the device
     */	
    public BBElec(SerialPort serport) throws IOException
    {
	this(serport, DFLT_TMOUT);
    }

    /** Flush InputStream  */
    protected void inFlush()
    {
	try {
	    _in.skip(_in.available());
	} catch (IOException e) {
	}
    }

    /** Write a command to the device
	@param cmd Command to send to device
    */
    public synchronized void sendCmd(byte[] cmd) throws IOException
    {
	if (_log4j.isDebugEnabled())
	{
	    StringBuffer sb = new StringBuffer("Sending: ");

	    for (int i = 0; i < cmd.length; i++) {
		sb.append(Integer.toHexString((byte)cmd[i]));
		sb.append(' ');
	    }
	    _log4j.debug(sb.toString());
	}

	_out.write(cmd);
	_out.flush();
    }

    /** Write a command with 16 bit parameter to the device
	@param cmd Command to send to device
	@param parm 16 bit parameter
    */
    public synchronized void send16bitCmd(byte[] cmd, int parm) throws IOException
    {
	int cmdLen = cmd.length;
	byte[] sendCmd = new byte[cmdLen + 2];

	System.arraycopy(cmd, 0, sendCmd, 0, cmdLen);
        sendCmd[cmdLen] = (byte)((parm>>8) & 0xff);
	sendCmd[cmdLen+1] = (byte)(parm & 0xff);
	sendCmd(sendCmd);
    }

    /** Read a response from the device
	@param buf Byte buffer to receive response
	@param offset Where to start filling in buffer
	@param nbytes Number of bytes expected
	@param tmout Time to wait in milliseconds
    */
    public synchronized int readResponse(byte[] buf, int offset, int nbytes, long tmout)
	throws IOException
    {
	try {
	    int rtn = StreamUtils.readBytes(_in, buf, offset, nbytes, tmout);

	    if (_log4j.isDebugEnabled())
	    {
		StringBuffer sb = new StringBuffer("Received ");
		sb.append(rtn);
		sb.append(" Bytes: ");

		for (int i = 0; i < rtn; i++) {
		    sb.append(Integer.toHexString((byte)buf[i]));
		    sb.append(' ');
		}
		_log4j.debug(sb.toString());
	    }

	    return(rtn);
	} catch (Exception e) {
	    _log4j.error("Timed out in readResponse()");
	    throw new IOException("Nested Exception: " + e);
	}
    }

    /** Read a response from the device
	@param buf Byte buffer to receive response
	@param nbytes Number of bytes expected
	@param tmout Time to wait in milliseconds
    */
    public int readResponse(byte[] buf, int nbytes,long tmout)
	throws IOException
    {
	return(readResponse(buf, 0, nbytes, tmout));
    }
	    
    /** Read a response from the device
	@param buf Byte buffer to receive response
	@param nbytes Number of bytes expected
    */
       public int readResponse(byte[] buf, int nbytes)
	throws IOException
    {
	return(readResponse(buf, 0, nbytes, _tmout));
    }

    /** Send a command, get 16 bit reply
	@param cmd Command to send
	@param tmout Time to wait in milliseconds
    */
    public synchronized int cmdWith16bitReply(byte[] cmd, long tmout) throws IOException
    {
	byte[] replyBuf = new byte[4];

	inFlush();
	sendCmd(cmd);
	long startTime = System.currentTimeMillis();

	for (int i = 0; i < 2; )
	{
	    i += readResponse(replyBuf, i, 2-i, tmout);
	    if (System.currentTimeMillis() > startTime+tmout)
		throw new IOException("Timeout");
	}

	return((replyBuf[0]<<8) + replyBuf[1]);
    }

    /** Send a command, get 16 bit reply
	@param cmd Command to send
    */
    public int cmdWith16bitReply(byte[] cmd) throws IOException
    {
	return(cmdWith16bitReply(cmd, _tmout));
    }

    /** Send a command, get 32 bit reply
	@param cmd Command to send
	@param tmout Time to wait in milliseconds
    */
    public synchronized int cmdWith32bitReply(byte[] cmd, long tmout) throws IOException
    {
	byte[] replyBuf = new byte[4];

	inFlush();
	sendCmd(cmd);
	long startTime = System.currentTimeMillis();

	for (int i = 0; i < 4; )
	{
	    i += readResponse(replyBuf, i, 4-i, tmout);
	    if (System.currentTimeMillis() > startTime+tmout)
		throw new IOException("Timeout");
	}

	return((replyBuf[0]<<24) + (replyBuf[1]<<16) + (replyBuf[2]<<8) + replyBuf[3]);
    }

    /** Send a command, get 32 bit reply
	@param cmd Command to send
    */
    public int cmdWith32bitReply(byte[] cmd) throws IOException
    {
	return(cmdWith32bitReply(cmd, _tmout));
    }

} /* class BBElec */

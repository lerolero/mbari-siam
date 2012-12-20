/**
 * @Title Network Virtual Terminal (NVT) Serial Port
 * @author Bob Herlien
 * @version $Revision: 1.2 $
 * @date 8 July 2009
 *
 * Copyright 2009 MBARI
 *
 */

package org.mbari.siam.core.nvt;

import gnu.io.SerialPort;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.utils.StopWatch;

import it.m2.net.telnet.NVTCom;
import it.m2.net.telnet.TelnetOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.TooManyListenersException;

import org.apache.log4j.Logger;


/**
   Implements gnu.io.SerialPortInterface for NVT (RFC 2217) serial devices.
   <p> This class does not implement EventListeners or any of
   notifyXXX() methods that impact notification.
   @author Bob Herlien
 */
public class NVTSerialPort extends gnu.io.SerialPort
{
    private static String _versionID = "$Revision: 1.2 $";

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(NVTSerialPort.class);

    public static final int DEFAULT_PORT = 2001;

    protected NVTCom		_nvtCom;
    protected String		_server = null;
    protected String		_serialPortName = null;
    protected int		_ipPort;
    protected NVTInputStream	_inStream = null;
    protected NVTOutputStream	_outStream = null;
    protected boolean		_isOpen = false;

    /** Constructor with IP address and port
     * @param host  Host name, either as DNS name ("focets4.mars.mbari.org") or
     *		    dotted quad ("134.89.42.127")
     * @param port  TCP port to use.
     */
    public NVTSerialPort(String host, int port)
    {
	_server = host;
	_ipPort = port;
    }

    /** Constructor with default TCP Port */
    public NVTSerialPort(String name)
    {
	this(name, DEFAULT_PORT);
    }
    
    /** Open the NVTCom
     * @throws IOException if can't connect to server
     */
    public void open() throws IOException, UnknownHostException
    {
	_serialPortName = _server + ":" + _ipPort;
	_log4j.debug("NVTSerialPort.open() for " + _serialPortName);

	// create the NVTCom, which connects to the server
	_nvtCom = new NVTCom(_server, _ipPort);
//	_nvtCom.setDefaults();
	_nvtCom.setModemMask(0xff);	//Enable detection of modem signals

	_isOpen = true;
    }

    /** Close the connection to the server
     */
    public void close()
    {
	_isOpen = false;
	_log4j.debug("close()");
	if (_nvtCom != null)
	    try {
		_nvtCom.close();
	    } catch (IOException e) {
		_log4j.warn("IOException in close(): " + e);
	    }
	_nvtCom = null;
    }

    public boolean isOpen()
    {
	return(_isOpen);
    }

    /** Get the underlying NVTCom.  If not open, open it.
     * throws IOException if open fails
     */
    public NVTCom getNVT() throws IOException
    {
	if (!_isOpen || (_nvtCom == null))
	{
	    _log4j.debug("getNVT() calling open()");
	    open();
	}

	return(_nvtCom);
    }

    /** Close the associated InputStream. */
    public void closeInputStream()
    {
	_inStream = null;
    }

    /** Close the associated OutputStream. */
    public void closeOutputStream()
    {
	_outStream = null;
    }

    /** Flush the OutputStream */
    public void flush() throws IOException
    {
	if (_isOpen)
	    _nvtCom.flush();
    }

    /** Get the associated NVTInputStream */
    public synchronized InputStream getInputStream() throws IOException
    {
	if (_inStream == null)
	    _inStream = new NVTInputStream(this);
	return(_inStream);
    }

    /** Get the associated NVTOutputStream */
    public synchronized OutputStream getOutputStream() throws IOException
    {
	if (_outStream == null)
	    _outStream = new NVTOutputStream(this);
	return(_outStream);
    }

    public int getBaudRate()
    {
	try {
	    return(_nvtCom.getBaud());
	} catch (IOException e) {
	    _log4j.error("IOException in getBaudRate(): " + e);
	}
	return(0);
    }

    public int getDataBits()
    {
	try {
	    switch(_nvtCom.getDataSize())
	    {
	      case 5:
		  return(DATABITS_5);
	      case 6:
		  return(DATABITS_6);
	      case 7:
		  return(DATABITS_7);
	      case 8:
		  return(DATABITS_8);
	    }

	} catch (IOException e) {
	    _log4j.error("IOException in getDataSize(): " + e);
	}
	return(0);
    }

    public int getFlowControlMode()
    {
	try {
	    switch(_nvtCom.getFlow())
	    {
	      case NVTCom.FLOW_NONE:
		  return(FLOWCONTROL_NONE);
	      case NVTCom.FLOW_CTSRTS:
		  return(FLOWCONTROL_RTSCTS_OUT);
	      case NVTCom.FLOW_XONXOFF:
		  return(FLOWCONTROL_XONXOFF_OUT);
	    }

	} catch (IOException e) {
	    _log4j.error("IOException in getFlow(): " + e);
	}
	return(FLOWCONTROL_NONE);
    }

    public int getParity()
    {
	String parity = "N";

	try {
	    parity = _nvtCom.getParity();
	} catch (IOException e) {
	    _log4j.error("IOException in getParity(): " + e);
	}

	switch (parity.charAt(0))
	{	
	  case 'O':
	  case 'o':
	      return(PARITY_ODD);
	  case 'E':
	  case 'e':
	      return(PARITY_EVEN);
	  case 'M':
	  case 'm':
	      return(PARITY_MARK);
	  case 'S':
	  case 's':
	      return(PARITY_SPACE);

	  default:
	      return(PARITY_NONE);
	}
    }

    public int getStopBits()
    {
	try {
	    switch(_nvtCom.getStopSize())
	    {
	      case 1:
		  return(STOPBITS_1);
	      case 2:
		  return(STOPBITS_2);
	      case 3:
		  return(STOPBITS_1_5);
	    }

	} catch (IOException e) {
	    _log4j.error("IOException in getStopSize(): " + e);
	}

	return(STOPBITS_1);
    }

    public boolean isCD()
    {
	try {
	    return(_nvtCom.getDCD());
	} catch (IOException e) {
	    _log4j.error("IOException in isCD(): " + e);
	}
	return(false);
    }

    public boolean isCTS()
    {
	try {
	    return(_nvtCom.getCTS());
	} catch (IOException e) {
	    _log4j.error("IOException in isCTS(): " + e);
	}
	return(false);
    }

    public boolean isDSR()
    {
	try {
	    return(_nvtCom.getDSR());
	} catch (IOException e) {
	    _log4j.error("IOException in isDSR(): " + e);
	}
	return(false);
    }

    public boolean isDTR()
    {
	try {
	    return(_nvtCom.getDCD());
	} catch (IOException e) {
	    _log4j.error("IOException in isCD(): " + e);
	}
	return(false);
    }

    public boolean isRI()
    {
	try {
	    return(_nvtCom.getRI());
	} catch (IOException e) {
	    _log4j.error("IOException in isRI(): " + e);
	}
	return(false);
    }

    public boolean isRTS()
    {
	try {
	    return(_nvtCom.getRTS());
	} catch (IOException e) {
	    _log4j.error("IOException in isRTS(): " + e);
	}
	return(false);
    }

    /** Not Implemented.  Just throws TooManyListenersException. */
    public void addEventListener(SerialPortEventListener listener)
	throws TooManyListenersException
    {
	throw new TooManyListenersException("Not supported");
    }

    /** Not Implemented */
    public void removeEventListener() {}

    /** Not Implemented */
    public void notifyOnBreakInterrupt(boolean enable) {}
    /** Not Implemented */
    public void notifyOnCarrierDetect(boolean enable) {}
    /** Not Implemented */
    public void notifyOnCTS(boolean enable) {}
    /** Not Implemented */
    public void notifyOnDataAvailable(boolean enable) {}
    /** Not Implemented */
    public void notifyOnDSR(boolean enable) {}
    /** Not Implemented */
    public void notifyOnFramingError(boolean enable) {}
    /** Not Implemented */
    public void notifyOnOutputEmpty(boolean enable) {}
    /** Not Implemented */
    public void notifyOnOverrunError(boolean enable) {}
    /** Not Implemented */
    public void notifyOnParityError(boolean enable) {}
    /** Not Implemented */
    public void notifyOnRingIndicator(boolean enable) {}

    /** Send BREAK for duration milliseconds */
    public void sendBreak(int duration)
    {
	try {
	    _nvtCom.setBREAK(true);
	    StopWatch.delay(duration);
	    _nvtCom.setBREAK(false);
	} catch (IOException e) {
	    _log4j.error("IOException in sendBreak(): " + e);
	}
    }

    /** Turn on/off DTR line */
    public void setDTR(boolean state)
    {
	try {
	    _nvtCom.setDTR(state);
	} catch (IOException e) {
	    _log4j.error("IOException in setDTR(): " + e);
	}
    }

    /** Turn on/off RTS line */
    public void setRTS(boolean state)
    {
	try {
	    _nvtCom.setRTS(state);
	} catch (IOException e) {
	    _log4j.error("IOException in setRTS(): " + e);
	}
    }

    /** Set serial port baud, datasize, etc */
    public void setSerialPortParams(int baud, int databits, int stopbits, int parity)
    {
	int parm;

	try {
	    _nvtCom.setBaud(baud);

	    switch (databits)
	    {
	    case DATABITS_7:
	      parm = 7;
	      break;
	    case DATABITS_6:
	      parm = 6;
	      break;
	    case DATABITS_5:
	      parm = 5;
	      break;
	    default:
	      parm = 8;
	      break;
	    }
	    _nvtCom.setDataSize(parm);

	    switch (stopbits)
	    {
	    case STOPBITS_2:
	      parm = 2;
	      break;
	    case STOPBITS_1_5:
	      parm = 3;
	      break;
	    default:
	      parm = 1;
	      break;
	    }
	    _nvtCom.setStopSize(parm);

	    String s;

	    switch(parity)
	    {
	    case PARITY_ODD:
	      s = "ODD";
	      break;
	    case PARITY_EVEN:
	      s = "EVEN";
	      break;
	    case PARITY_MARK:
	      s = "MARK";
	      break;
	    case PARITY_SPACE:
	      s = "SPACE";
	      break;
	    default:
	      s = "NONE";
	    }
	    _nvtCom.setParity(s);
	} catch (IOException e) {
	    _log4j.error("IOException in setSerialPortParms(): " + e);
	}
    }

    public void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException
    {
	int mode;

	switch(flowcontrol)
	{
	  case FLOWCONTROL_RTSCTS_IN:
	  case FLOWCONTROL_RTSCTS_OUT:
	      mode = NVTCom.FLOW_CTSRTS;
	      break;
	  case FLOWCONTROL_XONXOFF_IN:
	  case FLOWCONTROL_XONXOFF_OUT:
	      mode = NVTCom.FLOW_XONXOFF;
	      break;
	  default:
	      mode = NVTCom.FLOW_NONE;
	      break;
	}
	try {
	    _nvtCom.setFlow(mode);
	} catch (IOException e) {
	    _log4j.error("IOException in setFlowControlMode(): " + e);
	}
    }

    public String getName()
    {
	return(_serialPortName);
    }

    public String toString()
    {
	return(_serialPortName);
    }

    /** Not Implemented */
    public void disableReceiveFraming() {}
    /** Not Implemented */
    public void disableReceiveThreshold() {}
    /** Not Implemented */
    public void disableReceiveTimeout() {}
    /** Not Implemented */
    public void setInputBufferSize(int size) {}
    /** Not Implemented */
    public void setOutputBufferSize(int size) {}

    public void enableReceiveFraming(int f) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public void enableReceiveThreshold(int threshold) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public void enableReceiveTimeout(int time) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public int getInputBufferSize()
    {
	// Actual value of ringbuffer in NVTCom code
	return(1024);
    }

    public int getOutputBufferSize()
    {
	// Just a guess
	return(1024);
    }

    public int getReceiveFramingByte()
    {
	return(TelnetOptions.IAC);
    }

    public int getReceiveThreshold()
    {
	return(0);
    }

    public int getReceiveTimeout()
    {
	return(0);
    }

    public boolean isReceiveFramingEnabled()
    {
	return(false);
    }

    public boolean isReceiveThresholdEnabled()
    {
	return(false);
    }

    public boolean isReceiveTimeoutEnabled()
    {
	return(false);
    }

    public boolean getCallOutHangup() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean setCallOutHangup(boolean noHup) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public int getBaudBase() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean setBaudBase(int base) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public int getDivisor() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean setDivisor(int divisor) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public byte getEndOfInputChar() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean setEndOfInputChar(byte b) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean getLowLatency() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean setLowLatency() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public String getUARTType() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean setUARTType(String type, boolean test) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public byte getParityErrorChar() throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }

    public boolean setParityErrorChar(byte b) throws UnsupportedCommOperationException
    {
	throw new UnsupportedCommOperationException();
    }


} /* class NVTSerialPort */

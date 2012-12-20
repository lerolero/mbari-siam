/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.mbari.siam.operations.utils.AnnotateService;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.RangeException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
//import org.apache.log4j.net.SocketAppender;



/** Base class for thread which implements remote control of serial port */
public class RemoteSerialPortServer extends Thread 
{
    private static Logger _log4j = 
	Logger.getLogger(RemoteSerialPortServer.class);

    ServerSocket _serverSocket = null;
    InstrumentConsole _instrumentConsole = null;
    DeviceService _deviceService = null;
    byte[] _annotationBuffer = new byte[AnnotateService.MAX_ANNOTATION_SIZE];
    
//testing out log4j
//static Logger _logger = Logger.getLogger(RemoteSerialPortServer.class);

    
    //min and max server timeout values
    private static final int _MAX_TIMEOUT = 3600000; /* one hour */
    private static final int _MIN_TIMEOUT = 1000;    /* one second */
    
    //maxim chars the server will send or recv on the socket at once
    private static final int _SOCKET_WRITES_MAX = 32;
    private static final int _SOCKET_READS_MAX = 32;

    
    //the server default timeout is one minute
    public static final int _DEFAULT_TIMEOUT = 60000; /* milliseconds */
    private int _serverTimeout = _DEFAULT_TIMEOUT; 
    private boolean _shutdownRequest = false;

    public RemoteSerialPortServer(InstrumentPort instPort, int sockPort)
	throws IOException {
	
        //initialize the RemoteSerialPortServer according to constructor args
        initialize(instPort, null, sockPort);
    }

    public RemoteSerialPortServer(InstrumentPort instPort) throws IOException
    {
        //initialize the RemoteSerialPortServer according to constructor args
        initialize(instPort, null, 0);
    }
    
    public RemoteSerialPortServer(InstrumentPort instPort, 
                                  DeviceService deviceService) 
        throws IOException
    {
        //initialize the RemoteSerialPortServer according to constructor args
        initialize(instPort, deviceService, 0);
    }

    

    private void initialize(InstrumentPort instPort, 
                            DeviceService deviceService, 
                            int sockPort)
        throws IOException
    {
        //create the socket server
        _serverSocket = new ServerSocket(sockPort);

        //create a new InstrumentConsole using instrument port
        _instrumentConsole = instPort.getInstrumentConsole();
        
        //get the device serivce reference
        _deviceService = deviceService;
        
//_logger.addAppender(new SocketAppender("babelfish.shore.mbari.org", 4445));
    }

    public int getServerPort()
    {
        return _serverSocket.getLocalPort();
    }

    public InetAddress getServerInetAddress() throws UnknownHostException
    {
        return InetAddress.getLocalHost();
    }

    public int getTimeout()
    {
        return _serverTimeout;
    }
    
    /* set the server timeout in milliseconds */
    public void setTimeout(int timeout) throws RangeException
    {
        if ( timeout > _MAX_TIMEOUT )
            throw new RangeException ("timeout value of " + timeout + 
                                      " exceeds max timeout of " + 
                                      _MAX_TIMEOUT);
        
        if ( timeout < _MIN_TIMEOUT )
            throw new RangeException ("timeout value of " + timeout + 
                                      " is less than min timeout of " + 
                                      _MIN_TIMEOUT);
        
        _serverTimeout = timeout;
    }

    public void shutdown()
    {
        _shutdownRequest = true;
    }

    /**
       Wait for and accept new client connection.
    */
    public void run() 
    {
	Socket clientSocket = null;
        boolean socketConnected = false;

	try 
        {
            _serverSocket.setSoTimeout(_serverTimeout);
	    clientSocket = _serverSocket.accept();
            socketConnected = true;
	}
        catch (InterruptedIOException e)
        {
            _log4j.error("RemoteSerialPortServer shutting down, " +
                               "timed out waiting for client: " + e);
        }
        catch (IOException e)
        {
            _log4j.error("RemoteSerialPortServer shutting down, " +
                               "got unexpected IOException while waiting " + 
                               " for client: " + e);
        }
        finally
        {
            if ( !socketConnected )
            {
                if ( _deviceService != null)
                    _deviceService.resume();

                return;
            }
        }

        int annotationByteCount = 0;
        try 
        {
            OutputStream os = clientSocket.getOutputStream();
	    InputStream is = clientSocket.getInputStream();

            int socketWrites = 0;
            int socketReads = 0;

	    int c;
            
            //create a running stop watch to time the session
            StopWatch sessionTimer = new StopWatch(true);

	    for (;;) 
            {
                //if available read chars and push'em out
                socketWrites = 0;
                while ( _instrumentConsole.available() > 0 )
                {
                    os.write(_instrumentConsole.read());
                    
                    //only send _SOCKET_WRITES_MAX chars before checking for 
                    //socket input
                    if ( ++socketWrites >= _SOCKET_WRITES_MAX)
                        break;
                }

// This request is disabled, because DEBUG < INFO.
//if ( socketWrites > 0)
//    _logger.debug("sockWrites = " + socketWrites);

                //if you got chars giv'em to the instrument
                socketReads = 0;
                while ( is.available() > 0) 
                {
                    //get a char from the input stream
                    c = is.read();
                    
                    //put a byte in the annotation buffer if there is room
                    if ( annotationByteCount < _annotationBuffer.length)
                        _annotationBuffer[annotationByteCount++] = (byte)c;
                    
                    _instrumentConsole.write(c);
                    //if the user uses the console clear the session timer
                    sessionTimer.clear();

                    //only recv _SOCKET_READS_MAX chars before checking 
                    //for sock writes
                    if ( ++socketReads >= _SOCKET_READS_MAX)
                        break;
                }
                
                //if StopWatch expires kill the thread
                if (sessionTimer.read() > _serverTimeout) 
                {
                    _log4j.error("RemoteSerialPortServer shutting " +
                                       "down, in activity timeout");

                    if ( _deviceService != null)
                        _deviceService.resume();

                    break;
                }
                
                //if StopWatch expires kill the thread
                if (_shutdownRequest) 
                {
                    _log4j.debug("RemoteSerialPortServer shutting " +
                                         "down");
                    break;
                }
	    }
	}
	catch (IOException e) 
        {
            _log4j.error("RemoteSerialPortServer shutting down, " +
                               "got unexpected IOException: " + e);
            return;
	
        }
        finally
        {
            
// simple info test
//_logger.info("RemoteSerialPortServer " + this + " shutting down");

            try 
            { 
                clientSocket.close(); 
            } 
            catch (Exception e) 
            { 
                
            }
        }

        
        //if it's an InstrumentSerivce, annotate the port
        if ( _deviceService instanceof Instrument )
        {
            // create a byte buffer the exact same size as the number 
	    // of annotation bytes
            byte[] b = new byte[annotationByteCount];

            for (int i = 0; i < annotationByteCount; ++i)
                b[i] = _annotationBuffer[i];

            Instrument instrument = (Instrument)_deviceService;

	    try {
		instrument.annotate(b);
	    }
	    catch (Exception e) {
		_log4j.error(e);
	    }
        }
    }
}


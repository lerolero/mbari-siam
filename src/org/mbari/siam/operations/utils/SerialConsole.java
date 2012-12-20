/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.AccessException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.RemoteSerialPort;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.PortNotFound;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
SerialConsole opens a RemoteSerialPort to the specified service.
*/
public class SerialConsole 
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(SerialConsole.class);

    Node _node;
    RemoteSerialPort _remoteSerialPort;

    public SerialConsole(String nodeURL, String portName) 
	throws NotBoundException, RemoteException, AccessException,
	       MalformedURLException, UnknownHostException, 
	       PortOccupiedException,
	       IOException, PortNotFound
    {
	// Look for service on specified port
	_log4j.debug("Looking for node service at " + nodeURL);

	_node = (Node )Naming.lookup(nodeURL);

	_log4j.debug("Got proxy for node service \"" + 
			       new String(_node.getName()) + "\"");

	_log4j.debug("getRemoteSerialPort()");
	_remoteSerialPort = _node.getRemoteSerialPort(portName.getBytes());
        _log4j.debug("RemoteSerialPort.getServerInetAddress() = " +
                           _remoteSerialPort.getServerInetAddress());
        
        _log4j.debug("RemoteSerialPort.getServerPort() = " +
                           _remoteSerialPort.getServerPort());
        
        //_remoteSerialPort.connect();
	_log4j.debug("getRemoteSerialPort() - done");

    }

    /** Runs console session. */
    void run() 
	throws IOException {

	ReaderThread reader = new ReaderThread();
	reader.start();

	_log4j.debug("get BufferedReader");
        BufferedReader stdIn = 
	    new BufferedReader(new InputStreamReader(System.in));

	char userInput;

	_log4j.debug("while user input...");
        while ((userInput = (char )System.in.read()) != -1) 
        {
	    System.out.print(userInput);
            
            //!!! DEBUG CODE !!!
            if (userInput == 'x') 
            {
                _log4j.debug("got an 'x' bailing out");
                System.exit(0);
            }
            //!!! DEBUG CODE !!!
	    
            if (userInput != '\n') 
                _remoteSerialPort.write(userInput);
	}

	stdIn.close();
    }


    public static void main(String[] args) {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);

	System.out.println("************* SerialConsole start *************");

	if (args.length != 2) {
	    System.err.println("Usage: nodeURL portName");
	    return;
	}
	String nodeURL = NodeUtility.getNodeURL(args[0]);
	String portName = PortUtility.getPortName(args[1]);

	try {
	    System.out.println("Create console");
	    SerialConsole console = new SerialConsole(nodeURL, portName);
	    System.out.println("Now run()");
	    console.run();
	}
	catch (NotBoundException e) {
	    _log4j.error("NotBoundException");
	    return;
	}
	catch (AccessException e) {
	    _log4j.error("AccessException");
	    return;
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException");
	    return;
	}
	catch (UnknownHostException e) {
	    _log4j.error("UnknownHostException");
	    return;
	}
	catch (MalformedURLException e) {
	    _log4j.error("MalformedURLException");
	    return;
	}
	catch (PortNotFound e) {
	    _log4j.error("PortNotFound");
	}
	catch (PortOccupiedException e) {
	    _log4j.error("PortOccupiedException");
	    return;
	}
	catch (IOException e) {
	    _log4j.error("IOException " + e);
            e.printStackTrace();
	    return;
	}
        finally
        {
            System.out.println("************** SerialConsole end **************");
        }
    }


    /** Read from RemoteSerialPort and display. */
    class ReaderThread extends Thread {

	public void run() {

	    char c;
	    while (true) {
		try {
		    if ((c = (char )_remoteSerialPort.read()) == -1) 
			// Done
			break;

		    System.out.print(c);
                    if ( c == '\r' )
                        System.out.write('\n');
		}
		catch (IOException e) {
		    _log4j.error("ReaderThread.run() - got IOException");
		}
	    }
	}
    }
}



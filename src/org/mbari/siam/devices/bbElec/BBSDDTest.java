/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.bbElec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import org.mbari.siam.distributed.TimeoutException;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/** User test routines 232SDD16 RS-232 Digital I/O module from B&B Electronics
/*
 $Id: BBSDDTest.java,v 1.4 2012/12/17 21:33:27 oreilly Exp $
 $Name: HEAD $
 $Revision: 1.4 $
 */

public class BBSDDTest
{
    static protected Logger _log4j = Logger.getLogger(BBSDDTest.class);

    public BBSDDTest()
    {
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

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new SecurityManager());
	}
    }

    /** Open the serial port */
    public SerialPort openCommPort(String commPortName) throws Exception
    {
	System.out.println("Comm port is " + commPortName);

	CommPortIdentifier portid = CommPortIdentifier.getPortIdentifier(commPortName);
	if (portid.isCurrentlyOwned())
	    throw new IOException("Port is currently in use");

	CommPort commPort = portid.open("BBSDDTest", 2000);
	if ( !(commPort instanceof SerialPort) )
	    throw new IOException("Can only use a serial port to control device");

	SerialPort serPort = (SerialPort)commPort;
	serPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
				    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

	return(serPort);
    }

    /** Run the test */
    public void runTest(SerialPort serPort) throws Exception
    {
	BB232SDD16 digIO = new BB232SDD16(serPort);
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	String		inStr, tok;
	StringTokenizer st;
	int		val;

	while (true)
	{
	    System.out.println("\nR - Read Inputs");
	    System.out.println("RC - Read Configuration");
	    System.out.println("S [value] - Set output lines to hex [value]");
	    System.out.println("D [value] - Define output lines by hex [value] - 1=output, 0=input");
	    System.out.println("P [value] - Set powerup state to hex [value]");
	    System.out.println("Q - Quit test");
	    System.out.println();

	    inStr = in.readLine().trim().toLowerCase();

	    try {
		st = new StringTokenizer(inStr);
		tok = st.nextToken();

		if (tok.compareTo("r") == 0)
		{
		    val = digIO.readInputs();
		    System.out.println("Input lines = " + Integer.toHexString(val) + " (hex)");
		}
		else if (tok.compareTo("rc") == 0)
		{
		    val = digIO.readConfiguration();
		    System.out.println("Output definition = " + Integer.toHexString((val>>16)&0xffff) + " (hex)");
		    System.out.println("Powerup State     = " + Integer.toHexString(val&0xffff) + " (hex)");
		}
		else if (tok.compareTo("s") == 0)
		{
		    val = Integer.valueOf(st.nextToken(), 16).intValue() & 0xffff;
		    System.out.println("Setting outputs to " + Integer.toHexString(val));
		    digIO.setOutput(val);
		}
		else if (tok.compareTo("d") == 0)
		{
		    val = Integer.valueOf(st.nextToken(), 16).intValue() & 0xffff;
		    System.out.println("Defining outputs to " + Integer.toHexString(val));
		    digIO.defineIO(val);
		}
		else if (tok.compareTo("p") == 0)
		{
		    val = Integer.valueOf(st.nextToken(), 16).intValue() & 0xffff;
		    System.out.println("Setting powerup state to " + Integer.toHexString(val));
		    digIO.setPwrupState(val);
		}
		else if (tok.compareTo("q") == 0)
		    return;

	    } catch (IOException e) {
		System.err.println("IOException: " + e);
		e.printStackTrace();
	    } catch (Exception e) {
		System.err.println("Error parsing input string \"" + inStr + "\": " + e);
		e.printStackTrace();
	    }
	}
	
    }

    public static void main(String[] args)
    {
	BBSDDTest test = new BBSDDTest();

	if (args.length < 1)
	{
	    System.err.println("Usage: BBSDDTest [commPortName]");
	    System.exit(1);
	}

	try {
	    SerialPort serPort = test.openCommPort(args[0]);
	    test.runTest(serPort);
	    serPort.close();
	} catch (Exception e) {
	    System.err.println("Exception: " + e);
	    e.printStackTrace();
	}

    }

} /* class BBSDDTest */

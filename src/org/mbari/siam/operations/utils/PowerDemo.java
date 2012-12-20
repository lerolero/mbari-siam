/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.CommPortOwnershipListener;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;

public class PowerDemo {

    /** Log4j logger */
    static private Logger _log4j=Logger.getLogger(PowerDemo.class);

    private static final String MSP_PORT_NAME = "/dev/ttySX15";
    private static final int MSP_BAUD = 9600;
    private static File linuxSuspendFile = null;
    private static FileInputStream linuxSuspendInputStream = null;

    private static void usage() {
      System.out.println("Usage: java moos.operations.PowerDemo <delay in seconds>");
      return;
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

      int sleepSeconds = 0;
      String serPortName = MSP_PORT_NAME;
      SerialPort mspSerialPort;
      InputStream mspInputStream = null;
      PrintStream mspOutputStream = null;
      char exitChr = 2;

      if (args.length == 0) {
	usage();
	return;
      }

      try {
	sleepSeconds = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
	usage();
	return;
      }

      if (args.length > 1) {
	serPortName = args[1];
      }

      System.err.println("Opening serial port " + serPortName);
      System.setProperty("gnu.io.rxtx.SerialPorts", serPortName);

      try {
	mspSerialPort = openSerialPort(serPortName, MSP_BAUD);
      } catch (NoSuchPortException e) {
	System.err.println("NoSuchPortException: " + serPortName);
	return;
      }
      catch (Exception e) {
	System.err.println("Exception on opening serial port " + serPortName);
	System.err.println(e.getMessage());
	return;
      }

      try {
	mspInputStream = mspSerialPort.getInputStream();
	mspOutputStream = new PrintStream(mspSerialPort.getOutputStream());
      }
      catch (Exception e) {
	System.err.println("Exception on opening streams for " + serPortName);
	System.err.println(e.getMessage());
	return;
      }

      System.err.println("Setting MSP430 alarm  for " + sleepSeconds + " seconds");
      mspOutputStream.println();
      mspOutputStream.println("set alarm " + sleepSeconds );
      mspOutputStream.print(exitChr);
      mspOutputStream.println("x");
      mspOutputStream.flush();

      System.err.println("Opening linux /proc file");
      if (!setupLinuxSleepProcFile()) {
	System.err.println("Failed to open /proc/sys/pm/suspend");
	return;
      }
      System.err.println("Sleeping... ");
      System.err.println();
      System.err.flush();
      try {
	Thread.sleep(1000);
      }
      catch (Exception e) {}
      doSuspend();

      System.err.println();
      System.err.println("Awake!");
      System.err.println();      
    }

    /** Open serial port with specified name.  Set parameters to N81
    */
    static SerialPort openSerialPort(String devicePortName, int baud) 
	throws NoSuchPortException, PortInUseException, 
	       UnsupportedCommOperationException, Exception {

	CommPortIdentifier portId;
	System.out.println("Get port identifier for \"" + devicePortName + 
			     "\"...");
	portId = CommPortIdentifier.getPortIdentifier(devicePortName);
	System.out.println("Got port identifier!");

	if (portId.getPortType() != CommPortIdentifier.PORT_SERIAL) {
	    throw new Exception("Port " + devicePortName + 
				" is not a serial port.");
	}

	SerialPort serialPort;

	System.out.println("open ze serial port..." + devicePortName);
	serialPort = (SerialPort )portId.open(devicePortName, 1000);

	System.out.println("set port params...");
	serialPort.setSerialPortParams(baud,
				       SerialPort.DATABITS_8, 
				       SerialPort.STOPBITS_1, 
				       SerialPort.PARITY_NONE);

	return serialPort;
    }

    private static boolean setupLinuxSleepProcFile() {
	boolean retVal = false;
	//
	// try to open the file to suspend the operating system (linux)
	// NOTE: this will only work if this program is run as root
	//

	try {
	    linuxSuspendFile = new java.io.File("/proc/sys/pm/suspend");

	    if (linuxSuspendFile.exists() == true) {
		// TODO: check for file existence

		linuxSuspendInputStream
		    = new java.io.FileInputStream(linuxSuspendFile);

		retVal = true;
	    }
	} 
	catch (Exception e) {
	    System.err.println("exception on suspend file open");
	    System.err.println(e.getMessage());
	}

	return retVal;
    }

    private static void doSuspend() {
	if (linuxSuspendInputStream != null) {
	    try {
		linuxSuspendInputStream.read();
	    }
	    catch(IOException ioe) {
		System.err.println("doSuspend: IOException on read");
		System.err.println(ioe.getMessage());
	    }
	}
    }

}


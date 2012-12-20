/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.doomdark.uuid.UUID;
import org.mbari.puck.Puck;
import org.mbari.puck.Puck_1_3;
import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import gnu.io.NoSuchPortException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.utils.PuckUtils;

public class PuckRegister extends PuckUtil
{
    private static Logger _log4j = Logger.getLogger(PuckRegister.class);

    /** Modify PUCK registry. If specified uuid is null, then read datasheet
     from PUCK on specified serial port. */
    public void run(String uuidString, String serialPortName, 
		    String jarfilePath) 

	throws Exception {

	// Check for jar file existence
	File jarfile = new File(jarfilePath);
	if (!jarfile.exists()) {
	    throw new Exception("Service jar file " + jarfilePath + 
				" not found");
	}

	// Get last component in file name
	String jarName = jarfile.getName();

	_log4j.debug("Load PUCK registry file " + 
		     PuckUtils.PUCK_REGISTRY_NAME);

	// Load PUCK registry file
	FileInputStream in = null;
        Properties registry = new Properties();
	try {
	    in = 
		new FileInputStream(PuckUtils.PUCK_REGISTRY_NAME);

	    registry.load(in);
	    in.close();
	}
	catch (Exception e) {
	    _log4j.info("Unable to open file input stream for " + 
			 PuckUtils.PUCK_REGISTRY_NAME + ": " + e);
	}


	UUID uuid = null;

	// If null UUID string has been specified, then read UUID from 
	// PUCK on specified serial port. 
	if (uuidString == null) {

	    // Open serial port to PUCK
	    _log4j.debug("Open PUCK serial port " + serialPortName);
	    initSerialPort(serialPortName, 9600);

	    _log4j.debug("Create PUCK...");
	    Puck puck = new Puck_1_3(_serialPort);
	    _log4j.debug("setPuckMode()...");
	    puck.setPuckMode(3);

	    // Get PUCK datasheet, and UUID
	    Puck.Datasheet datasheet = puck.readDatasheet();
	    uuid = datasheet.getUUID();

	    _serialPort.close();
	}
	else {
	    uuid = new UUID(uuidString);
	}

	// Check for existing entry for this UUID
	String value;
	if (((value = registry.getProperty(uuid.toString())) != null) &&
	    !jarName.equals(value)) {
	    System.out.println("Replacing " + value + " with " + jarName);
	}
	else {
	    System.out.println("Adding " + uuid.toString() + " = " + jarName);
	}

	registry.setProperty(uuid.toString(), jarName);

	// Now write out the registry
        FileOutputStream out = 
	    new FileOutputStream(PuckUtils.PUCK_REGISTRY_NAME);

	registry.store(out, "Maintain this registry with regpuck. DO NOT EDIT MANUALLY!");
	out.close();
    }

    /** Process command line arguments, then modify registry accordingly. */
    void start(String[] args) throws Exception {

	String serialPortName = null;
	String uuid = null;
	String jarfileName = null;

	boolean error = false;
	if (args.length < 3) {
	    error = true;
	}
	else {
	    // Can specify either the serial port on which to contact PUCK, or
	    // UUID value directly.
        
	    /*
	      Process command line arguments. Last argument is jar file.
	    */
	    boolean specdPuck = false;
	    boolean specdUUID = false;
	    for (int i = 0; i < args.length - 1; i++) {
		_log4j.debug("args " + i + ": " + args[i]);
		if (args[i].equals("-puck") && i < args.length - 2) {
		    serialPortName = args[++i];
		    specdPuck = true;
		}
		else if (args[i].equals("-uuid") && i < args.length - 2) {
		    uuid = args[++i];
		    specdUUID = true;
		}
	    }
	    jarfileName = args[args.length-1];

	    if (!specdPuck && !specdUUID) {
		error = true;
	    }
	    if (specdPuck && specdUUID) {
		error = true;
	    }
	}

	if (error) {
	    System.err.println("usage:\n");
	    System.err.println("  -puck serialPort  jarfile");
	    System.err.println("  -uuid UUID  jarfile");
	    System.err.println("\nMust specify either -puck OR -uuid\n");
	    return;
	}

	run(uuid, serialPortName, jarfileName);
    }


    public static void main(String[] args) {

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));


        PuckRegister app = new PuckRegister();
        
        try {
            app.start(args);
        }
        catch(Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }


}


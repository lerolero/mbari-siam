/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.state;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.AttributeChecker;
import org.mbari.siam.distributed.PropertyException;

public class AttrTest2 {

    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(AttrTest2.class);

    public void run(String propertyFileName) 
	throws Exception {

	Shmoo shmoo = new Shmoo();

	InstrumentServiceAttributes attributes = shmoo._attributes;

	if (attributes instanceof Serializable) {
	    _logger.info("attributes are serializable");
	}
	else {
	    _logger.info("attributes are NOT serializable");
	}

	Properties properties = new Properties();

	try {
	    properties.load(new FileInputStream(propertyFileName));
	}
	catch (IOException e) {
	    _logger.error(e);
	}

	_logger.debug("convert from property strings...");

	try {
	    attributes.fromProperties(properties, true);
	}
	catch (PropertyException e) {
	    _logger.error("Errors while loading attributes from " + 
			  "properties file " + propertyFileName + ":" +
			  e.getMessage());
	}


	_logger.debug("print the damn attributes...");
	System.out.println(attributes);
    }


    public static void main(String[] args) {

	/* Set up a simple configuration that logs on the console.
	   Note that simply using PropertyConfigurator doesn't work
	   unless JavaBeans classes are available on target. 
	   For now, we configure a PropertyConfigurator, using properties
	   passed in from the command line, followed by BasicConfigurator
	   which sets default console appender, etc.
	*/
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));
	AttrTest2 test = new AttrTest2();

	try {
	    test.run(args[0]);
	}
	catch (Exception e) {
	    System.err.println(e);
	}
    }
}

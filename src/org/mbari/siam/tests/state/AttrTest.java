/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.state;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.AttributeChecker;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ServiceAttributes;

public class AttrTest implements DeviceServiceIF {

	/** Log4j logger */
	protected static Logger _logger = Logger.getLogger(AttrTest.class);

	public byte[] getName() {
		return this.getClass().getName().getBytes();
	}
	
	// Working copy of attributes;
	// initialize to default generic InstrumentServiceAttributes object
	InstrumentServiceAttributes _attributes = null;

	/** Set reference to attributes. */
	public void setAttributes(ServiceAttributes attributes) {
		_logger.info("setAttributes() called");
		_attributes = (InstrumentServiceAttributes) attributes;
	}

	/** Constructor. */
	AttrTest() {
		_logger.info("AttrTest() - construct default attributes object");
		_attributes = new InstrumentServiceAttributes(this);
	}

	public void run() throws Exception {

		_attributes.powerPolicy = PowerPolicy.NEVER;
		_attributes.commPowerPolicy = PowerPolicy.ALWAYS;

		System.out.println("attributes.toString():\n" + _attributes);

		System.out.println("\nattributes properties:");
		Properties properties = _attributes.toProperties();
		System.out.println(ServiceAttributes.toPropertyStrings(properties) + "\n");

		NewMetadataPacket packet = new NewMetadataPacket(999, "test".getBytes());

		packet.setStaticDoc("XML goes here".getBytes());
		packet.setDeviceStatus("stuff read directly from device goes here"
				.getBytes());
		packet.setServiceAttributes(_attributes);

		System.out.println("MetadataPacket:");
		System.out.println(packet);
	}

	public void runParseTest(String propertyFileName) {

		System.out.println("NOW parse attributes from properties in file " +
				propertyFileName + "...");

		if (_attributes instanceof Serializable) {
			_logger.info("_attributes are serializable");
		} else {
			_logger.info("_attributes are NOT serializable");
		}

		Properties properties = new Properties();

		try {
			properties.load(new FileInputStream(propertyFileName));
		} catch (IOException e) {
			_logger.error(e);
		}

		_logger.debug("convert from property strings...");

		try {
			_attributes.fromProperties(properties, true);
		} catch (PropertyException e) {
			_logger.error("Errors while loading attributes from "
					+ "properties file " + propertyFileName + ":"
					+ e.getMessage());
		}

		_logger.debug("print the damn attributes...");
		System.out.println(_attributes);

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
		WaveSensor test = new WaveSensor();

		try {
			test.run();

			if (args.length == 1) {
				test.runParseTest(args[0]);
			} else {
				System.out.println("No properties input file");
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
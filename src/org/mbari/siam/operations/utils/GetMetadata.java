// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.server.RMISocketFactory;

import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
 * Acquire and print metadata from specified instrument. The following subsets
 * of metadata may be requested: s - service state p - service properties c -
 * service cache x - service xml file i - instrument state a - all of the above
 */
public class GetMetadata {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(GetMetadata.class);

	public static void printUsage() {
		System.err
		    .println("\n usage: getMetadata nodeURL commPortName <components...>");
		System.err.println("  components:");
		System.err.println("  s - service attributes (current values");
		System.err.println("  p - service properties (file defaults)");
		System.err.println("  c - service cache");
		System.err.println("  x - service xml file");
		System.err.println("  i - instrument state");
		System.err.println("  a - components (default)");
		System.err.println("  l - log sample (for testing)");
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

		Node node = null;
		Device devices[] = null;

		//need to setSecurityManager
		if (System.getSecurityManager() == null)
			System.setSecurityManager(new SecurityManager());

		if (args.length < 2) {
			printUsage();
			System.exit(1);
		}

		String nodeURL = NodeUtility.getNodeURL(args[0]);

		// Create socket factory; overcomes problems with RMI 'hostname'
		// property.
		try {
			String host = NodeUtility.getHostName(nodeURL);
			RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
		} catch (MalformedURLException e) {
			System.err.println("Malformed URL \"" + nodeURL + "\": "
					+ e.getMessage());
		} catch (IOException e) {
			System.err.println("RMISocketFactory.setSocketFactory() failed");
			System.err.println(e);
		}

		int components = 0;
		boolean logPacket = false;

		if (args.length >= 3) {
			if (args[2].toLowerCase().indexOf("h") >= 0) {
				printUsage();
				System.exit(1);
			}
			if (args[2].indexOf("a") >= 0)
				components |= Device.MDATA_ALL;
			if (args[2].indexOf("s") >= 0)
				components |= Device.SERVICE_ATTRIBUTES;
			if (args[2].indexOf("p") >= 0)
				components |= Device.SERVICE_PROPERTIES;
			if (args[2].indexOf("c") >= 0)
				components |= Device.SERVICE_CACHE;
			if (args[2].indexOf("x") >= 0)
				components |= Device.SERVICE_XML;
			if (args[2].indexOf("i") >= 0)
				components |= Device.INSTRUMENT_STATE;
			if (args[2].indexOf("l") >= 0)
				logPacket = true;

			// default is all
			if (components == 0)
				components |= Device.MDATA_ALL;

			// bail out helpfully if invalid option specified
			if (components < 0 || components > Device.MDATA_ALL) {
				printUsage();
				System.exit(1);
			}
		} else {
			// use default if none specified
			components |= Device.MDATA_ALL;
		}

		try {

			System.out.println("Looking for node server stub at " + nodeURL);

			node = (Node) Naming.lookup(nodeURL);

			System.out.println("Got proxy for node service \""
					+ new String(node.getName()) + "\"");

			devices = node.getDevices();

			for (int i = 0; i < 1; i++) {
				// Now sample instruments on specified ports
				String portName = PortUtility.getPortName(args[i + 1]);
				try {
					Device device = node.getDevice(portName.getBytes());
					if (device instanceof Instrument) {
						Instrument instrument = (Instrument) device;

						System.out.println("Requesting packet...components = 0x"
										+ Integer.toHexString(components));
						MetadataPacket packet = device.getMetadata(
								"just checking".getBytes(), components,
								logPacket);

						System.out.println("Packet received:\n");

						System.out.println(packet.toString());
					} else {
						System.err.println("Device on port " + portName
								+ " is not an Instrument");
					}
				} catch (PortNotFound e) {
					System.err.println("Port " + portName + " not found");
				} catch (DeviceNotFound e) {
					System.err.println("Device not found on port " + portName);
				}
			}

		} catch (RemoteException e) {
			System.err.println("RemoteException: " + e);
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
			System.exit(1);
		}
		System.exit(0);

	}
}

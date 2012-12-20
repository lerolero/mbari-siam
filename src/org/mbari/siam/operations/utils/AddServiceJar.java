/****************************************************************************/
/* Copyright 2005 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.io.File;
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

public class AddServiceJar
{
    public static void main(String[] args) {

	if (args.length != 2) {
            System.err.println("usage: java AddServiceJar PUCK-serial-port service.jar");
            System.exit(1);	
        }

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	String serialPortName = args[0];
	String jarFile = args[1];
        
        AddServiceJar app = new AddServiceJar();
        
        try {
            app.execute(serialPortName, jarFile);
        }
        catch(Exception e) {
            System.err.println(e);
        }
    }


    public void execute(String serialPortName, String jarFile) 
	throws Exception {

        //create service_code dir if it's not there
        ServiceJarUtils.creatServiceCodePath();

	SerialPort serialPort = openSerialPort(serialPortName, 9600);

	Puck puck = new Puck_1_3(serialPort);
	puck.setPuckMode(3);

	// Get PUCK datasheet, and UUID
	Puck.Datasheet datasheet = puck.readDatasheet();
        UUID uuid = datasheet.getUUID();
        
        //check for service jar
        if ( ServiceJarUtils.isServiceLoaded(uuid) ) {
            System.out.println("Add service canceled, service file " +
                               "with UUID '" + uuid + "' already loaded.");
            return;
        }

        //add the serivce code to the service code directory
        ServiceJarUtils.addServiceCode(new File(jarFile), uuid);

        return;
    }


    /** Open specified serial port */
    private SerialPort openSerialPort(String portName, int baud_rate) 
	throws Exception {

        
	CommPortIdentifier commPortId = 
	    CommPortIdentifier.getPortIdentifier(portName);

	SerialPort serialPort = 
                (SerialPort)commPortId.open(this.getClass().getName(), 1000);

	serialPort.setSerialPortParams(baud_rate, 
				       serialPort.getDataBits(),
				       serialPort.getStopBits(),
				       serialPort.getParity());
        return serialPort;
    }


}

    

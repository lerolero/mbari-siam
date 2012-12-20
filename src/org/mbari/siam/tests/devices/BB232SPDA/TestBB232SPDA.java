/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.devices.BB232SPDA;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;
 
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.core.nvt.*;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;

import org.mbari.siam.foce.devices.co2subsys.*;


/* TestBB232SPDA - Test TestBB232SPDA serial A/D + digital IO module
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public class TestBB232SPDA{
	
	/** log4j logger */
    static protected Logger _log4j = Logger.getLogger(TestBB232SPDA.class);  

	/** The BB232SPDA module */
	BB232SPDA _iomodule;
	
	/** Service attributes */
	protected CO2SubsystemMonitorAttributes _attributes;
	
	NVTSerialPort _serialPort;
	String _serialTCPHost="localhost";
	int _serialTCPPort=NVTSerialPort.DEFAULT_PORT;
	
	public TestBB232SPDA(){
		super();
	}
	public TestBB232SPDA(String[] args)
	throws IOException, Exception{
		this();
		configure(args);
		_serialPort=configurePort(_serialTCPHost,_serialTCPPort);
		_iomodule=new BB232SPDA(_serialPort,BB232SPDA.MODE_SERIAL_RFC1722);
	}
	
	public void printHelp(){
		StringBuffer sb=new StringBuffer();
		sb.append("\n");
		sb.append("Test BB232SPDA - test BB 232SPDA IO module \n");
		sb.append("\n");
		sb.append("testbb [-h <host> -p <port>\n");
		sb.append("\n");
		sb.append("-h <host> : RFC2217 TCP/IP address            ["+_serialTCPHost+"]\n");
		sb.append("-p <port> : server TCP/IP port                ["+_serialTCPPort+"]\n");
		sb.append("\n");
		System.out.println(sb.toString());
	}
	
	
	public void configure(String[] args) throws Exception{
	
		if(args.length<=0){
			return;
		}
		for(int i=0;i<args.length;i++){
			String arg=args[i];
			if(arg.equals("-help") || arg.equals("--help")){
				printHelp();
				System.exit(1);
			}
			if(arg.equals("-h")){
				_serialTCPHost=args[i+1];
				i++;
			}else if(arg.equals("-p")){
				_serialTCPPort=Integer.parseInt(args[i+1]);
				i++;
			}
		}
	}
	
	public NVTSerialPort configurePort(String host, int port)
	throws IOException{
		NVTSerialPort newPort=new NVTSerialPort(host,port);
		newPort.open();
		newPort.setSerialPortParams(9600,
								  SerialPort.DATABITS_8,
								  SerialPort.STOPBITS_1,
								  SerialPort.PARITY_NONE);
		return newPort;
		
	}
	
	public void testReadDigitalIO() 
	throws Exception{
		_log4j.info("Requesting Digital I/O channels...");
		short[] dio_channels= _iomodule.readDigitalIO();
		for(int j=0;j<dio_channels.length;j++){
			_log4j.info("Digital IO channel["+j+"]:"+Integer.toHexString(dio_channels[j]));
		}
	}
	
	public void testReadAnalogIn(int maxChannel) 
	throws Exception{
		short[] ad_channels= _iomodule.readADChannels(maxChannel);
		for(int i=0;i<ad_channels.length;i++){
			_log4j.info("AD channel["+i+"]:"+Integer.toHexString(ad_channels[i]));
		}
	}
	
	public void testReadAll() 
	throws Exception{
		short[] all_channels= _iomodule.readAll();
		for(int j=0;j<all_channels.length;j++){
			_log4j.info("channel["+j+"]:"+Integer.toHexString(all_channels[j]));
		}
	}
	
	public void run()
	throws Exception{
		
		for(int i=0;i<BB232SPDA.ANALOG_INPUTS;i++){
			_log4j.info("Requesting ["+i+"] A/D channels...");
			testReadAnalogIn(i);
			_log4j.info("\n");
		}
		
		testReadDigitalIO();
		
		_log4j.info("Reading all channels...");
		testReadAll();
		
		_log4j.info("writing TRUE to digital out...");
		_iomodule.writeDigitalOut(true);
		_log4j.info("reading digital IO...");
		testReadDigitalIO();
		testReadAnalogIn(4);

		_log4j.info("writing FALSE to digital out...");
		_iomodule.writeDigitalOut(false);
		_log4j.info("reading digital IO...");
		testReadDigitalIO();
		delay(500);
		testReadAnalogIn(4);

		_log4j.info("done");
	}
	public void delay(long msec){
		try{
			Thread.sleep(msec);
		}catch (Exception e) {
			//
		}
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
		
		try{
			TestBB232SPDA app=new TestBB232SPDA(args);
			app.run();
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
}

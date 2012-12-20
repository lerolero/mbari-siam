/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.io.InputStream;
import java.io.OutputStream;


import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;


public class RXTXTest
{
    SerialPort _serialPort;
    String _serialPortName;
    static final int _TEST_BAUD = 38400;

    public static void main(String[] args) 
    {
        //use args from main to set com port
        RXTXTest app = null;

        if ( args[0] != null)
        {
            app = new RXTXTest(args[0]);
        }
        else
        {
            System.out.println("need to specify a comm port");
            System.exit(0);
        }

        try
        {
            app.execute();
        }
        catch(Exception e)
        {
            System.err.println("RXTX.execute() caught Exception: " + e);
            e.printStackTrace();
            System.exit(0);
        }

        System.exit(0);
    }

    public RXTXTest(String port_name)
    {
        _serialPortName = port_name;
        System.setProperty("gnu.io.rxtx.SerialPorts", port_name);
    }

    public void showSettings(){
	System.out.println("Baud Rate:"+_serialPort.getBaudRate());
	System.out.println("Data Bits:"+_serialPort.getDataBits());
	System.out.println("Flow Control Mode:"+_serialPort.getFlowControlMode());
	System.out.println("Parity:"+_serialPort.getParity());
	System.out.println("Stop Bits:"+_serialPort.getStopBits());
	System.out.println("CD:"+_serialPort.isCD());
	System.out.println("CTS:"+_serialPort.isCTS());
	System.out.println("DSR:"+_serialPort.isDSR());
	System.out.println("DTR:"+_serialPort.isDTR());
	System.out.println("RI:"+_serialPort.isRI());
	System.out.println("RTS:"+_serialPort.isRTS());
    }

    public void execute() throws Exception
    {
        InputStream is = null;
        OutputStream os = null;
        
        while(true)
        {
            //open the serial port
            System.out.println("-----------------------------------");
            System.out.println("...opening the serial port");
            initSerialPort(_serialPortName);

            //get input and output streams
            is = _serialPort.getInputStream();
            os = _serialPort.getOutputStream();

            //clear out the serial streams
//            is.skip(is.available());
//            os.flush();
            
            //change baud rate
            System.out.println("...setting baud rate to " + _TEST_BAUD);

            _serialPort.setSerialPortParams(_TEST_BAUD, 
                                            _serialPort.getDataBits(), 
                                            _serialPort.getStopBits(), 
                                            _serialPort.getParity());
	    
	    showSettings();

            //write some stuff
            System.out.println("...writing some stuff");
            os.write("hello from tx pin".getBytes());

            //wait for all the bytes to be received
            delay(10);

            //read some stuff
            System.out.println("...reading some stuff");
            while ( is.available() > 0 )
                System.out.write(is.read());

            System.out.println("");

            //set the RTS line
            System.out.println("...setting the RTS line");
            _serialPort.setRTS(true);

	    showSettings();
            
            //check CTS
            System.out.println("...checking the CTS line");
            if ( _serialPort.isCTS() )
                System.out.println("CTS is set");
            else
                System.out.println("CTS is clear");

            System.out.println("...closing the serial port");
            //close the serial port
            _serialPort.close();

            System.out.println("...waiting 5 seconds");
            //wait a bit
            delay(5000);
        
        }
    }
    
    private void initSerialPort(String port_name) throws Exception
    {
        CommPortIdentifier commPortId = 
            CommPortIdentifier.getPortIdentifier(port_name);

        _serialPort = 
            (SerialPort)commPortId.open(this.getClass().getName(), 1000);

        System.out.println("Serial port " + 
                           port_name + 
                           " opened by " + 
                           this.getClass().getName());
    }
    
    private void delay(int msecs) 
    {
        try 
        {
            Thread.sleep( msecs );
        } 
        catch ( Exception e ) 
        {
            System.out.println("delay(...) failed");
            e.printStackTrace();
        }
    }

}


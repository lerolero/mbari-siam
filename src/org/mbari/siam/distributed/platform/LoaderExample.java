/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/

package org.mbari.siam.distributed.platform;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.jar.*;
import gnu.io.*;
import org.mbari.siam.distributed.platform.PortService;
import org.mbari.siam.distributed.platform.PortServiceClassLoader;

public class LoaderExample
{
    //serial port vars
    private CommPortIdentifier commPortId;
    private SerialPort serialPort;

    //other class data
    private PortService portService;
    private boolean quittingTime = false;

    public static void main(String[] args) 
    {
        if ( args.length < 2)
        {
            System.err.println("usage: java LoaderExample [comm_port] [jar_file]");
            return;
        }
        
        LoaderExample app = new LoaderExample();
        app.execute(args[0], args[1]);
        System.exit(0);
    }

    private void execute(String port_name, String jar_file)
    {
        int input_char;
        
        BufferedReader input_reader
            = new BufferedReader(new InputStreamReader(System.in));
 
        //set up the serial port
        if ( !initSerialPort(port_name, 9600) )
        {
            System.out.println("Failed to initialize " + port_name);
            return;
        }
        
        showMenu(); 

        while ( !quittingTime )
        {
            try
            {
                if (input_reader.ready())
                {
                    input_char = input_reader.read();
                    switch (input_char)
                    {
                        case 's': startService(jar_file); break;
                        case 'k': stopService(); break;
                        case 'h': showMenu(); break;
                        case 'q': quittingTime = !quittingTime; break;
                        default: /*do nothing*/ break;
                    }
                }

            }
            catch(IOException e)
            {
                //handle exception
            }

            delay(10);
        }
        
        //close the serial port
        serialPort.close();
    }
    
    private void showMenu()
    {
        System.out.println("s - start the service");
        System.out.println("k - stop the service");
        System.out.println("h - show this menu");
        System.out.println("q - quit this example");
        return;
    }

    private boolean startService(String jar_file)
    {
        String class_name = null;
        PortServiceClassLoader cl;
        URL url;

        try
        {
            //form a url with using the jar copied file
            url = new URL("file:" + jar_file);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return false;
        }

        //create a port service class loader
        cl = new PortServiceClassLoader(url);
        
        try 
        {
            //get the class name of the Port-Service-Class
            class_name = cl.getEntryClassName();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            return false;
        }
        
        if (class_name == null) 
        {
            System.out.println("Specified jar file does not contain a " +
                               "'Port-Service-Class' manifest attribute");
            return false;
        }
        
        try 
        {
            //start the service using a the class name from the 
            //manifest and the serialport for the driver.
            cl.startService(class_name, serialPort);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    private void stopService()
    {
        //stop the service
        try
        {
            portService.stop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean initSerialPort(String port_name, int baud_rate)
    {
        try 
        {
            commPortId = CommPortIdentifier.getPortIdentifier(port_name);
        } 
        catch (NoSuchPortException e) 
        {
            System.out.println("No such port " + port_name + " " + e);
            e.printStackTrace();
            return false;
        }
                    
        try 
        {
            serialPort = 
                (SerialPort)commPortId.open(this.getClass().getName(), 1000);
        }
        catch (PortInUseException e) 
        {
            System.out.println("Port " + port_name + " is in use " + e);
            e.printStackTrace();
            return false;
        }

        try
        {
            serialPort.setSerialPortParams(baud_rate, 
                                           serialPort.getDataBits(),
                                           serialPort.getStopBits(),
                                           serialPort.getParity());
        }
        catch (UnsupportedCommOperationException e) 
        {
            e.printStackTrace();
            return false;
        }

        return true;
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


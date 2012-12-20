/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class CommIoTest
{
    /** Log4j logger */
    //protected static Logger _log4j = Logger.getLogger(CommIoTest.class);

    CommPortIdentifier commPortId;
    SerialPort serialPort;
    
    public static void main(String[] args) 
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
	//Logger.getRootLogger().setLevel((Level)Level.INFO);

        //use args from main to set com port
        CommIoTest test = null;

        if ( args[0] != null)
        {
            test = new CommIoTest(args[0]);
        }
        else
        {
            System.out.println("need to specify a comm port");
            System.exit(0);
        }

        test.TestExecute(args);
        System.exit(0);
    }

    public void showIoMap()
    {
        
        System.out.println("");
        System.out.println("The wiring for the DPA ports is as follows for a DTE connector");
        System.out.println("out of the back plane.");
        System.out.println("");
        System.out.println("---------------------------------------------------------------");
        System.out.println("|   DB-9 male  |   5 pin DPA    |   Function   |  wire color* |");
        System.out.println("---------------------------------------------------------------");
        System.out.println("|    3         |       1        |  TX  (out)   |  green       |");
        System.out.println("|    7         |       2        |  RTS (out)   |  yellow      |");
        System.out.println("|    2         |       3        |  RX  (in)    |  orange      |");
        System.out.println("|    8         |       4        |  CTS (in)    |  red         |");
        System.out.println("|    5         |       5        |  GND         |  brown       |");
        System.out.println("---------------------------------------------------------------");
        System.out.println("");
        System.out.println("*Wire colors as of last batch (2/2003) of connectors.  Note: this");
        System.out.println("is not a standard, do not count on it!!!");
        System.out.println("");
    }
    
    public CommIoTest(String port_name)
    {
        try
        {
            initSerialPort(port_name);
        }
        catch(Exception e)
        {
            System.err.println("CommIoTest threw exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void TestExecute(String[] args)
    {
        
        if (args[1] == null)
            showHelp();
        
        if ( args[1].trim().compareTo("-help") == 0)
            showHelp();

        if ( args[1].trim().compareTo("-pinout") == 0)
            showIoMap();
        
        if ( args[1].trim().compareTo("-rts_loop") == 0)
        {
            while(true)
            {
                serialPort.setRTS(true);
                System.out.println("RTS set hi");
                delay(1500);
                serialPort.setRTS(false);
                System.out.println("RTS set lo");
                delay(1500);
            }
        }

        if ( args[1].trim().compareTo("-cts_loop") == 0)
        {
            System.out.println("RTS set hi");

            while(true)
            {
                delay(1000);
                
                if (serialPort.isCTS())
                    System.out.println("CTS is hi");
                else
                    System.out.println("CTS is lo");
            }
        }
        
    }
    
    void showHelp()
    {
        System.out.println("-help     - show this menu");
        System.out.println("-rts_loop - toggle RTS output");
        System.out.println("-cts_loop - read CTS input");
        System.out.println("-pinout   - display serial io pinout diagram");
        return;
    }

    private void initSerialPort(String port_name) throws Exception
    {
        commPortId = CommPortIdentifier.getPortIdentifier(port_name);
        serialPort = 
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
            System.err.println("delay(...) failed");
            e.printStackTrace();
        }
    }

}


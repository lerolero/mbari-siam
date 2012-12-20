/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;

import java.io.InputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class JarViewer
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(JarViewer.class);

    public void execute(String[] args)
    {
        int manifestSize;
        ZipEntry ze = null;
        ZipFile zipFile = null;
        InputStream zis = null;
        Enumeration zipEntries = null;
        
        try
        {
            zipFile = new ZipFile(args[0]);
        }
        catch(IOException e)
        {
            System.err.println("Failed to open jarfile: " + e);
            System.exit(1);
        }
        
        //get the zip entry for the manifest
        ze = zipFile.getEntry("META-INF/MANIFEST.MF");

        //list it out if it exists
        System.out.println("-------------------manifest-------------------");
        if (ze == null)
        {
            System.out.println("Entry META-INF/MANIFEST.MF not found");
        }
        else
        {
            try
            {
                zis = zipFile.getInputStream(ze);

                while ( zis.available() > 0)
                    System.out.write(zis.read());
            }
            catch(IOException e)
            {
                System.err.println("Failure while reading manifest: " + e);
            }
        }
        
        //get the zip entry for the service.properties
        ze = zipFile.getEntry("service.properties");

        //list it out if it exists
        System.out.println("");
        System.out.println("--------------service.properties--------------");
        if (ze == null)
        {
            System.out.println("Entry service.properties not found");
        }
        else
        {
            try
            {
                zis = zipFile.getInputStream(ze);

                while ( zis.available() > 0)
                    System.out.write(zis.read());
            }
            catch(IOException e)
            {
                System.err.println("Failure while reading properties: " + e);
            }
        }
        
        //if the 'x' option was declared list out the xml
        if (args.length > 1) 
        {
            if ( args[1].equals("x") )
            {
                //get the zip entry for the service.properties
                ze = zipFile.getEntry("service.xml");

                //list it out if it exists
                System.out.println("");
                System.out.println("------------------service.xml--------" + 
                                   "---------");
                if (ze == null)
                {
                    System.out.println("Entry service.xml not found");
                }
                else
                {
                    try
                    {
                        zis = zipFile.getInputStream(ze);

                        while ( zis.available() > 0)
                            System.out.write(zis.read());
                    }
                    catch(IOException e)
                    {
                        System.err.println("Failure while reading xml: " + e);
                    }
                }
            }
	}

        
        //list out all the jar file entries
        zipEntries = zipFile.entries();

        System.out.println("");
        System.out.println("---------------jar file entries---------------");
        
        while ( zipEntries.hasMoreElements() )
            System.out.println(zipEntries.nextElement());
        
        //close the zip file
        try
        {
            zipFile.close();
        }
        catch(IOException e)
        {
            System.err.println("Failed to close jarfile: " + e);
            System.exit(1);
        }

        System.out.println("\n");

        return;
    }

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

        if (args.length < 1) 
        {
	    System.err.println("usage: JarViewer jarFile [x]");
	    System.err.println("x - display contents of service.xml");
	    System.exit(1);
	}

        JarViewer app = new JarViewer();
        app.execute(args);
        System.exit(0);
    }
}

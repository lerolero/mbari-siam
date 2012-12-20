/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

// import java.util.zip.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
 * JarResources: JarResources maps all resources included in a
 * Zip or Jar file. Additionaly, it provides a method to extract one
 * as a blob. 
 * Based on code found in "Java Tip 70" at "Java World".
 */
public final class JarResources {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(JarResources.class);


    // jar resource mapping tables
    private Hashtable htSizes=new Hashtable();  
    private Hashtable htJarContents=new Hashtable();

    // a jar file
    private String jarFileName;

    /**
      * creates a JarResources. It extracts all resources from a Jar
      * into an internal hashtable, keyed by resource names.
      * @param jarFileName a jar or zip file
      */
    public JarResources(String jarFileName)
	{
	this.jarFileName=jarFileName;
	init();
	}

    /**
      * Extracts a jar resource as a blob.
      * @param name a resource name.
      */
    public byte[] getResource(String name)
	{
	return (byte[])htJarContents.get(name);
	}

    /** initializes internal hash tables with Jar file resources.  */
    private void init()
	{
	try
	    {
	    // extracts just sizes only. 
	    ZipFile zf=new ZipFile(jarFileName);
	    Enumeration e=zf.entries();
	    while (e.hasMoreElements())
		{
		ZipEntry ze=(ZipEntry)e.nextElement();

		_log4j.debug(dumpZipEntry(ze));

		htSizes.put(ze.getName(),new Integer((int)ze.getSize()));
		}
	    zf.close();


	    // extract resources and put them into the hashtable.
	    FileInputStream fis=new FileInputStream(jarFileName);
	    BufferedInputStream bis=new BufferedInputStream(fis);
	    ZipInputStream zis=new ZipInputStream(bis);
	    ZipEntry ze=null;
	    while ((ze=zis.getNextEntry())!=null)
		{
		if (ze.isDirectory())
		    {
		    continue;
		    }

		    _log4j.debug("ze.getName()="+ze.getName()+
				       ","+"getSize()="+ze.getSize() );


		int size=(int)ze.getSize();
		// -1 means unknown size.
		if (size==-1)
		    {
		    size=((Integer)htSizes.get(ze.getName())).intValue();
		    }

		byte[] b=new byte[(int)size];
		int rb=0;
		int chunk=0;
		while (((int)size - rb) > 0)
		    {
		    chunk=zis.read(b,rb,(int)size - rb);
		    if (chunk==-1)
			{
			break;
			}
		    rb+=chunk;
		    }

		// add to internal resource hashtable
		htJarContents.put(ze.getName(),b);

		_log4j.debug( ze.getName()+"  rb="+rb+
					",size="+size+
					",csize="+ze.getCompressedSize() );
		}
	    }
	catch (NullPointerException e)
		{
		_log4j.debug("done.");
		}
	catch (FileNotFoundException e)
	    {
	    e.printStackTrace();
	    }
	catch (IOException e)
	    {
	    e.printStackTrace();
	    }
	}

    /**
      * Dumps a zip entry into a string.
      * @param ze a ZipEntry
      */
    private String dumpZipEntry(ZipEntry ze)
	{
	StringBuffer sb=new StringBuffer();
	if (ze.isDirectory())
	    {
	    sb.append("d ");
	    }
	else
	    {
	    sb.append("f ");
	    }

	if (ze.getMethod()==ZipEntry.STORED)
	    {
	    sb.append("stored   ");
	    }
	else
	    {
	    sb.append("defalted ");
	    }

	sb.append(ze.getName());
	sb.append("\t");
	sb.append(""+ze.getSize());
	if (ze.getMethod()==ZipEntry.DEFLATED)
	    {
	    sb.append("/"+ze.getCompressedSize());
	    }

	return (sb.toString());
	}

    }	// End of JarResources class.

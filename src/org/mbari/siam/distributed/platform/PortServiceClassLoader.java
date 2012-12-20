/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.platform;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.util.jar.Attributes;
import java.io.IOException;
import java.io.File;
import gnu.io.CommPort;

/**
   Load PortService class from jar file.
   @author Mike Risi
*/
public class PortServiceClassLoader extends URLClassLoader 
{
    private URL url;

    public PortServiceClassLoader(URL url) 
    {
        super(new URL[] { url });
        this.url = url;
    }

    public PortServiceClassLoader(File file) throws MalformedURLException 
    {
        super(new URL[] { file.toURL() });
        url = file.toURL();
    }

    /** Get name of PortService implementation. */
    public String getEntryClassName() throws IOException 
    {
        URL u = new URL("jar", "", url + "!/");
        JarURLConnection uc = (JarURLConnection)u.openConnection();
        Attributes attr = uc.getMainAttributes();
        return attr.getValue("Port-Service-Class");
    }

    /** Invoke start() method of specified PortService object. */
    public PortService startService(String name, CommPort port)
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException
    {
	String[] args = {""};

        //sir debug line
        System.out.println("loading class : " + name);
        
        Class c = loadClass(name);
        //return (Device)c.newInstance();
	
	PortService port_service = (PortService)c.newInstance();
        port_service.start(port, args);
	return port_service;
    }

}

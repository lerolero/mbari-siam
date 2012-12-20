/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.Remote;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.mbari.siam.utils.JarClassLoader;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.distributed.PacketParser;

import org.apache.log4j.Logger;

/**
 * Load DeviceService classes from jar file.
 * 
 * @author Mike Risi
 */
public class DeviceServiceClassLoader extends JarClassLoader {
    private static Logger _log4j = Logger
	.getLogger(DeviceServiceClassLoader.class);

    private String _jarFileName = null;

    private static final String SERVICE_TAG = "Port-Service-Class";

    /** Create new class loader from specified jar file. */
    public DeviceServiceClassLoader(String fileName) {
	super(fileName);
	_jarFileName = fileName;
    }

    /** Load service and related classes; return service instance. */
    public DeviceService instantiateService(String codebaseDirectory) 
	throws ClassNotFoundException,
	       InstantiationException, IllegalAccessException, IOException,
	       Exception {

	_log4j.debug("instantiateService() for " + _jarFileName);

	File file = new File(_jarFileName);
	JarFile jarFile = new JarFile(file);

	String serviceClassName = jarFile.getManifest().getMainAttributes()
	    .getValue(SERVICE_TAG);

	if (serviceClassName == null) {
	    throw new ClassNotFoundException("No " + SERVICE_TAG + 
					     " attribute in service jar file");
	}

	Class serviceClass = null;

	// Load all classes from JarFile
	Enumeration zipEntries = jarFile.entries();
	while (zipEntries.hasMoreElements()) {
	    ZipEntry entry = (ZipEntry) zipEntries.nextElement();
	    if (entry.getName().endsWith(".class")) {

		// Determine class name
		String name = entry.getName();
		_log4j.debug("class file name: " + name);
		name = name.replace('/', '.');
		// Remove '.class' suffix'
		int index = name.lastIndexOf(".class");
		String className = name.substring(0, index);
		_log4j.debug("className: " + className);

		String msg = null;

		// Load the class
		Class c = null;
		try {
		    /* First, remove the class file from the codebase 
		       directory, if it exists - in case we've
		       got a corrupted class file there. Corruption could have 
		       occurred if class was only partially copied to the 
		       codebase (e.g. if user terminated the app prematurely).
		    */
		    removeClassFile(entry, codebaseDirectory);

		    // Now load the class; will get loaded from the jar file, 
		    // since we've deleted it from the codebase in previous 
		    // step.
		    c = loadClass(className, true);
		    _log4j.debug("loaded class " + className);
					
		    // Stub files and remote interfaces need to be copied to 
		    // codebase, so that they are available to clients across 
		    // the network.
		    if ((className.endsWith("_Stub") || isRemoteInterface(c)) || 
			isPacketParser(c) || 
			name.lastIndexOf(".class")>0) {
			_log4j.debug("Installing classfile for " + className +
				      " to codebase " + codebaseDirectory);
						
			installClassFile(jarFile, entry, codebaseDirectory);
		    }

		} catch (ClassNotFoundException e) {
		    msg = className + ": " + e.toString();
		    _log4j.error(msg);
		    throw new ClassNotFoundException(msg);
		}
		catch (NoClassDefFoundError e) {
		    msg = className + ": " + e.toString();
		    _log4j.error(msg);
		    throw new Exception(msg);
		}

		catch (Exception e) {
		    msg = className + ": " + e.toString();
		    _log4j.error(msg);
		    throw new Exception(msg);
		}

		if (className.equals(serviceClassName)) {
		    // This is the service; keep reference to it
		    _log4j.debug("Set reference to service class");
		    serviceClass = c;
		}
	    }
	}

	jarFile.close();

	if (serviceClass == null) {
	    _log4j.error("service class is null");
	    throw new ClassNotFoundException("service class is null");
	}

	DeviceService deviceService = (DeviceService) serviceClass
	    .newInstance();

	_log4j.debug("Created DeviceService " + deviceService);
	return deviceService;
    }

    /** Return true if class represents an interface that directly or 
	indirectly extends Remote. */
    boolean isRemoteInterface(Class c) {

	if (!c.isInterface()) {
	    // This is not an interface
	    return false;
	}
	Class[] interfaces = c.getInterfaces();
	for (int i = 0; i < interfaces.length; i++) {
	    if (interfaces[i] == Remote.class) {
		return true;
	    }
	    // Recursively look for extension of remote
	    if (isRemoteInterface(interfaces[i])) {
		return true;
	    }
	}
	return false;
    }


    /** Return true if class represents a PacketParser. */
    boolean isPacketParser(Class c) {

	while (c != null) {
	    if (c == PacketParser.class) {
		return true;
	    }
	    c = c.getSuperclass();
	}
	return false;
    }


    /** Remove class file. */
    void removeClassFile(ZipEntry zipEntry,
			 String installDirectory) throws Exception {


	// Build the class file object
	File classFile = new File(installDirectory + File.separator
				  + zipEntry.getName());

	// If the file exists, try to remove it
	if (classFile.exists()) {
	    _log4j.debug("Removing " + classFile + " from " + installDirectory);

	    classFile.delete();
	}
    }


    /** Install class file to appropriate location. */
    void installClassFile(JarFile jarFile, ZipEntry zipEntry,
			  String installDirectory) throws Exception {

	//create directory tree for the class file
	StringTokenizer tok = new StringTokenizer(zipEntry.getName(),
						  "/");

	StringBuffer classDir = new StringBuffer(installDirectory);

	while ((tok.countTokens() - 1) > 0) {
	    classDir.append(File.separator);
	    classDir.append(tok.nextToken());
	}

	File classFile = new File(new String(classDir));
	classFile.mkdirs();

	// make sure you really created the directory tree
	if (!classFile.exists()) {
	    throw new Exception("failed to created the directory " + classDir);
	}

	// create a new class file in the right directory
	classFile = new File(installDirectory + File.separator
			     + zipEntry.getName());

	_log4j.debug("classFile: " + classFile.toString());

	// create the file
	classFile.createNewFile();

	// make sure you really created the class file
	if (!classFile.exists()) {
	    throw new Exception("failed to create file " + classFile);
	}

	//get an OutputStream to the classFile and an input stream
	//to the zip entry
	FileOutputStream os = new FileOutputStream(classFile);
	InputStream is = jarFile.getInputStream(zipEntry);

	//copy away!!!
	//maximum 10 seconds to copy class
	long MAX_CLASS_COPY_TIME = 300000;
	long classFileSize = zipEntry.getSize();

	if (classFileSize < 0) {
	    throw new Exception(" could not determine size of "
				+ zipEntry.getName() + " in file " + jarFile.getName());
	}

	//create a running timer
	StopWatch sw = new StopWatch(true);

	while ((classFileSize > 0) && (sw.read() < MAX_CLASS_COPY_TIME)) {
	    if (is.available() > 0) {
		os.write(is.read());
		--classFileSize;
	    }
	}

	//close input and output streams to release system resources
	os.close();
	is.close();

	sw.stop();
	_log4j.debug(zipEntry.getName() + " - total copy time: " + 
		     sw.read() + " millisec");

	//if all the bytes are gone you copied it
	if (classFileSize == 0) {
	    _log4j.debug(zipEntry.getName()
			  + " successfully copied to codebase");
	} else {
	    throw new Exception("failed to copy " + zipEntry.getName()
				+ " to code base, exceeded " + 
				MAX_CLASS_COPY_TIME + " msec");
	}
    }
}

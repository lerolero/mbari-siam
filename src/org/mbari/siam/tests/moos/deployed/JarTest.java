/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;


public class JarTest {

    final static String PROPERTIES_FILENAME = "service.properties";

    public void run(String jarFilename) throws Exception {


	File file = new File(jarFilename);
	if (!file.exists()) {
	    System.err.println(jarFilename + " does not exist");
	    return;
	}

	JarFile jarFile = new JarFile(file);

	ZipEntry zipEntry = jarFile.getEntry(PROPERTIES_FILENAME);
	if (zipEntry == null) {
	    System.err.println(PROPERTIES_FILENAME + " not found in " + 
			       jarFilename);
	    return;
	}
	InputStream input = jarFile.getInputStream(zipEntry);
	Properties properties = new Properties();
	properties.load(input);
	input.close();

	properties.list(System.out);

	JarEntry jarEntry;
	System.out.println("Enumerate jarfile entries:");
	Enumeration entries = jarFile.entries();
	while (entries.hasMoreElements()) {
	    jarEntry = (JarEntry )entries.nextElement();
	    System.out.println(jarEntry);
	    input = jarFile.getInputStream(jarEntry);
	    if (jarEntry.getName().equals(PROPERTIES_FILENAME)) {
		System.out.println("*** Found properties file entry");
		properties = new Properties();
		input = jarFile.getInputStream(jarEntry);
		properties.load(input);
		properties.list(System.out);
	    }
	    input.close();
	}
	

	jarFile.close();
    }


    public static void main(String[] args) {
	if (args.length != 1) {
	    System.err.println("usage: SIAM-instrument-jarfile");
	    return;
	}
	JarTest test = new JarTest();

	try {
	    test.run(args[0]);
	}
	catch (Exception e) {
	    System.err.println(e);
	}
    }
}


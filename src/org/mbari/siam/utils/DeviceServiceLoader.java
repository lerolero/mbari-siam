/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Oct 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mbari.siam.utils;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import javax.swing.Timer;

import org.mbari.siam.core.DeviceService;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.DeviceServiceClassLoader;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.NullPowerPort;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.ServiceEvent;
import org.mbari.siam.core.ServiceSandBox;
import org.mbari.siam.utils.FileUtils;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.PuckUtils;
import org.apache.log4j.Logger;
import org.mbari.puck.Puck_1_3;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PropertyException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
 * Instantiate, initialize, and run a device service. The service bytecode, any
 * related classes, and properties file are contained within a standard SIAM
 * instrument jar file.
 * 
 * @author oreilly
 *  
 */
public class DeviceServiceLoader {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(DeviceServiceLoader.class);

    static final String XML_FILENAME = "service.xml";
    static final String CACHE_FILENAME = "service.cache";

    static Logger _logger = Logger.getLogger(DeviceServiceLoader.class);

    DeviceService _service;

    /**
     *  
     */
    public DeviceServiceLoader() {
	super();
	// TODO Auto-generated constructor stub
    }

    /** Load and run the device service, returning its URL. */
    public String run(String jarFileName, String portName, 
		      String codebaseDirectory, 
		      Parent parent, String serviceURL) throws Exception {


	_log4j.debug("create node properties");
	NodeProperties nodeProperties = new NodeProperties();

	String siamHome = System.getProperty("siam_home");
	FileInputStream in = 
	    new FileInputStream(siamHome +
				"/properties/siamPort.cfg");

	// Load Node properties from the file
	nodeProperties.load(in);
	in.close();

	SerialPort serialPort = openSerialPort(portName, 9600);

	String workingDirectory = ".";
		
	InstrumentPort instrumentPort = null;

	if (jarFileName.equals("-puck")) {

	    jarFileName = "classes/puck.jar";

	    Puck_1_3 puck = new Puck_1_3(serialPort);

	    // Get jar file from PUCK
	    PuckUtils.readSiamPayload(puck, jarFileName);
	}

	_log4j.debug("create SerialInstrument port");
	instrumentPort = new SerialInstrumentPort(serialPort, portName,
						  new NullPowerPort());
	instrumentPort.initialize();

        // Create the sand box if it doesn't already exist
        ServiceSandBox sandBox = 
	    new ServiceSandBox(siamHome + File.separator + "portSandbox");

	// Copy specified jar file to sandbox (whether it's differnt or not)
	sandBox.deleteAllFiles();

	// Copy the file to the sand box
	FileUtils.copyFile(jarFileName, sandBox.getJarPath());
	// Extract the jar file contents
	updateSandBox(sandBox);

	// Create class loader based on service jar (now in sandbox)
	_log4j.debug("create device service class loader");
	DeviceServiceClassLoader classLoader = 
	    new DeviceServiceClassLoader(sandBox.getJarPath());

	_log4j.debug("Instantiate service");
	_service = classLoader.instantiateService(codebaseDirectory);

	_log4j.debug("Initialize service");
	try {
	    _service.initialize(nodeProperties, parent, instrumentPort,
				sandBox,
				workingDirectory + File.separator + XML_FILENAME, 
				workingDirectory + File.separator + ServiceSandBox.PROPERTIES_NAME, 
				workingDirectory + File.separator + CACHE_FILENAME);
			
	} catch (Exception e) {
	    _log4j.error("run() - caught some Exception");
	    e.printStackTrace();
	    if (e instanceof MissingPropertyException
		|| e instanceof InvalidPropertyException
		|| e instanceof PropertyException) {

		_log4j.error(e);
		_log4j.error(_service.getAttributes().getHelp());
		throw e;
	    } else if (e instanceof InitializeException) {
		_log4j.error(e);
		_log4j.error("Keep going...");
	    }
	}

	_service.prepareToRun();

	_log4j.debug("Attributes:\n" + _service.getAttributes());

	_log4j.debug("Run service");

	if (_service instanceof InstrumentService) {
	    final InstrumentService instrument = (InstrumentService)_service;
	    // This call to sync will kick of the scheduler, without this
	    // call, the scheduler will not be initiated.
	    instrument.getDefaultSampleSchedule().sync();
	}
	_log4j.debug("Done with run()");

	// Start rmiregistry
	try {
	    _logger.info("Starting registry... ");
	    LocateRegistry.createRegistry(1099);
	    _logger.info("registry started.");
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    _logger.info(e.getMessage());
	}

	// Bind service 
	_logger.info("Binding device service to " + serviceURL);
	Naming.rebind(serviceURL, _service);
	_logger.info("Device service is bound to " + serviceURL);
	
	// Return service URL
	return serviceURL;
    }
	

    /** Return reference to DeviceService */
    public DeviceService getService() {
	return _service;
    }


    /**
     * Load device service properties from file that is included within
     * service's jar file.
     */
    static Properties getServiceProperties(String filename)
	throws ZipException, IOException, SecurityException, Exception {

	// Get service properties from jar file
	JarFile jar = new JarFile(filename);

	ZipEntry zipEntry = jar.getEntry("service.properties");

	if (zipEntry == null) {
	    throw new Exception("property file not found");
	}

	InputStream input = jar.getInputStream(zipEntry);
	Properties properties = new Properties();
	properties.load(input);
	input.close();
	return properties;
    }

    /**
     * Open serial port with specified name. Set port parameters to default
     * 96008N1.
     */
    static SerialPort openSerialPort(String devicePortName, int baud)
	throws NoSuchPortException, PortInUseException,
	       UnsupportedCommOperationException, Exception {

	CommPortIdentifier portId;
	_log4j.debug("get identifier for " + devicePortName);

	portId = CommPortIdentifier.getPortIdentifier(devicePortName);

	_log4j.debug("Obtained port");
	if (portId.getPortType() != CommPortIdentifier.PORT_SERIAL) {
	    throw new Exception("Port " + devicePortName
				+ " is not a serial port.");
	}

	SerialPort serialPort;
	_log4j.debug("open the identifier...");
	serialPort = (SerialPort) portId.open("DeviceTestHarness", 1000);

	_log4j.debug("set serial port params");
	serialPort.setSerialPortParams(baud, SerialPort.DATABITS_8,
				       SerialPort.STOPBITS_1, 
				       SerialPort.PARITY_NONE);

	return serialPort;
    }

    /** Copy stub class to codebase. */
    void updateCodebase(File file, String codebaseDirectory)
	throws MissingPropertyException, Exception {
	//create the jar file
	JarFile jarFile = new JarFile(file);

	//get all the zip entries in the jar file
	Enumeration zipEntries = jarFile.entries();

	//check all the zip entries to see if they are classes
	while (zipEntries.hasMoreElements()) {
	    //get the next zip entry
	    ZipEntry ze = (ZipEntry) zipEntries.nextElement();

	    //if it's a stub class copy it to the codebase
	    if (ze.getName().endsWith("_Stub.class")) {
		//create directory tree for the class file
		StringTokenizer tok = new StringTokenizer(ze.getName(), "/");
		String classDir = codebaseDirectory;

		while ((tok.countTokens() - 1) > 0)
		    classDir = classDir + File.separator + tok.nextToken();

		File classFile = new File(classDir);
		classFile.mkdirs();

		//make sure you really created the direcotry tree
		if (!classFile.exists())
		    throw new Exception("failed to created the directory "
					+ classDir);

		//create a new class file in the right directory
		classFile = new File(codebaseDirectory + File.separator
				     + ze.getName());

		//create the file
		classFile.createNewFile();

		//make sure you really created the class file
		if (!classFile.exists())
		    throw new Exception("failed to created the file "
					+ classFile);

		//get an OutputStream to the classFile and an input stream
		//to the zip entry
		FileOutputStream os = new FileOutputStream(classFile);
		InputStream is = jarFile.getInputStream(ze);

		//copy away!!!
		//maximum 10 seconds to copy class
		long MAX_CLASS_COPY_TIME = 10000;
		long classFileSize = ze.getSize();

		if (classFileSize < 0)
		    throw new Exception(" could not determine size of "
					+ ze.getName() + " in file " + 
					jarFile.getName());

		StopWatch sw = new StopWatch();

		while ((classFileSize > 0) && 
		       (sw.read() < MAX_CLASS_COPY_TIME)) {
		    if (is.available() > 0) {
			os.write(is.read());
			--classFileSize;
		    }
		}

		//close input and output streams to release system resources
		os.close();
		is.close();

		sw.stop();
		_logger.debug("total copy time: " + sw.read() + " millisec");

		//if all the bytes are gone you copied it
		if (classFileSize == 0)
		    _logger.debug(ze.getName()
				  + " successfully copied to codebase");
		else
		    throw new Exception("failed to copy " + ze.getName()
					+ " to code base");
	    }
	}

	//close the jar file to release any system resources
	if (jarFile != null)
	    jarFile.close();
    }


    /** Extract service.properties and service.xml the to sand box */
    void updateSandBox(ServiceSandBox sandBox) throws Exception {
	_log4j.debug("create JarFile...");
	JarFile jarFile = new JarFile(sandBox.getJarPath());
	ZipEntry zipEntry = null;
	InputStream input = null;

	//if there is no properties file extract it from the jar
	File propFile = new File(sandBox.getPropertiesPath());

	String servicePropertiesLocation = sandBox.getPath() + File.separator
	    + sandBox.PROPERTIES_NAME;

	if ( !propFile.exists() ) {
	    zipEntry = jarFile.getEntry(sandBox.PROPERTIES_NAME);

	    if ( zipEntry == null ) {
		throw new Exception("Service properties file \""
				    + sandBox.PROPERTIES_NAME
				    + "\" not found in jar file " + 
				    jarFile.getName());
	    }

	    input = jarFile.getInputStream(zipEntry);
	    FileUtils.copyFile(input, sandBox.getPropertiesPath());
	}

	try {
	    // Extract service XML into sandbox directory
	    String xmlFilename = "service.xml";
	    String destinationName = sandBox.getPath() + File.separator
		+ xmlFilename;

	    _log4j.debug("Getting service.xml zipEntry");
	    zipEntry = jarFile.getEntry(xmlFilename);
	    if ( zipEntry == null ) {
		throw new Exception("Service XML file \"" + xmlFilename
				    + "\" not found in jar file " + 
				    jarFile.getName());
	    }
	    //_log4j.debug("Closing input");
	    //input.close();
	    _log4j.debug("Getting new input stream");
	    input = jarFile.getInputStream(zipEntry);
	    _log4j.debug("copying to " + destinationName);
	    FileUtils.copyFile(input, destinationName);
	}
	catch ( Exception e ) {
	    _log4j.debug("couldn't extract service.xml...that's OK ");
	    _log4j.debug(e);
	}
	// close the input stream to release in system resources
	if ( jarFile != null ) {
	    jarFile.close();
	}
	if ( input != null ) {
	    input.close();
	}
    }


    /** Extract service.properties and service.xml to specified directory */
    void extractFiles(String jarFileName, String directory) 
	throws IOException {
		
	JarFile jarFile = new JarFile(jarFileName);
	ZipEntry zipEntry = null;
	InputStream input = null;

	try {
	    _logger.debug("Get properties file from jar");
	    zipEntry = jarFile.getEntry(ServiceSandBox.PROPERTIES_NAME);

	    if (zipEntry == null) {
		_log4j.error(ServiceSandBox.PROPERTIES_NAME + " not found in "
				   + jarFile.getName());
	    } else {
		input = jarFile.getInputStream(zipEntry);
		FileUtils.copyFile(input, directory + File.separator
				   + ServiceSandBox.PROPERTIES_NAME);
	    }
	} catch (IOException e) {
	    _log4j.error("Caught exception while extracting properties file: " + e);
	}
	if (input != null) {
	    input.close();
	}

	try {
	    zipEntry = jarFile.getEntry(XML_FILENAME);

	    if (zipEntry == null) {
		_log4j.error(XML_FILENAME + " not found in "
				   + jarFile.getName());
	    } else {
		input = jarFile.getInputStream(zipEntry);
		FileUtils.copyFile(input, directory + File.separator
				   + XML_FILENAME);
	    }
	} catch (IOException e) {
	    _log4j.error("Caught exception while extracting xml file: " 
			       + e);
	}

	//close the input stream to release in system resources
	if (jarFile != null)
	    jarFile.close();

	if (input != null)
	    input.close();
    }
}

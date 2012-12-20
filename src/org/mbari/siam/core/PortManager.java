// Copyright 2003 MBARI
package org.mbari.siam.core;
 
import gnu.io.SerialPort;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.mbari.siam.utils.FileUtils;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.ByteUtility;
import org.mbari.siam.utils.PuckUtils;

import org.mbari.siam.operations.utils.ServiceJarUtils;

import org.apache.log4j.Logger;
import org.doomdark.uuid.UUID;

import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.Device;
import org.mbari.puck.Puck;
import org.mbari.puck.Puck_1_3;

/**
 * PortManager keeps track of devices installed on node ports.
 * 
 * @author Tom O'Reilly
 */
public class PortManager
{
    private static Logger _log4j = Logger.getLogger(PortManager.class);

    Vector _ports;

    NodeProperties _nodeProps;

    NodeManager _nodeManager;

    String _siamHome;

    String _serviceJarLocation;

    String _serviceXMLLocation = "service.xml";

    String _servicePropertiesLocation = "service.properties";

    String _serviceCacheLocation = "service.cache";

    String _codebaseLocation;

    String _logLocation;

    private static int _MAX_PUCK_READ_TRIES = 3;

    // Keep track of instances created - only one allowed
    private static int _instanceCount = 0;

    //timeout for puck read in ms, may need more than 30000
    private static final int _PUCK_READ_TIMEOUT = 60000;

    private static final int _DEFAULT_PUCK_CURRENT = 8000;

    /** Flag indicates whether to use PUCK payload (as opposed to registry) */
    private boolean _usePuckPayload = true;


    /** Create PortManager object. */
    public PortManager(String siamHome, NodeProperties nodeProps)
	throws MissingPropertyException, InvalidPropertyException
    {
	// Only allowed to create one instance
        if ( ++_instanceCount > 1 )
	{
	    _log4j.error("Only ONE instance of PortManager allowed!");
	    System.exit(1);
	}

	//get the rest of the node properties from NodeProperties
        _nodeProps = nodeProps;
        _siamHome = siamHome;
        _serviceJarLocation = _nodeProps.getServiceJarDirectory();
        _codebaseLocation = _nodeProps.getCodebaseDirectory();
        _logLocation = _nodeProps.getDeviceLogDirectory();

	String str = 
	    NodeManager.getInstance().getNodeProperties().getProperty(
					NodeProperties.USE_PUCK_PAYLOAD_KEY, 
					"true");
	if (str.equalsIgnoreCase("true")) {
	    _usePuckPayload = true;
	}
	else {
	    _usePuckPayload = false;
	}
	_log4j.debug("_usePuckPayload: " + _usePuckPayload);

    }

    /** Get port configuration from properties file and store in vector. */
    public void initPortVector() throws IOException, MissingPropertyException,
					InvalidPropertyException
    {
        String service_jar;
        String serial_port;
	int    portsFound = 0;

	_log4j.debug("initPortVector()");

        //create port vector
        _ports = new Vector();

        //if the serial ports are specified in the properties file
        //set them as a system property so RXTX can find them

        String platform_ports = _nodeProps.getSerialPortNames();

        System.setProperty("gnu.io.rxtx.SerialPorts", platform_ports);
        _log4j.debug("serial ports: " + platform_ports);

        //build list of ports from info in cfg file
        for ( int i = 0; i < _nodeProps.getMaxPorts(); i++ )
	{
	    try
	    {
		DevicePort port = _nodeProps.getPort(i);
		_ports.add(port);
		portsFound++;
	    }
	    catch ( MissingPropertyException e )
	    {
	    }
	}
	_log4j.info("Found " + portsFound
			    + " configured ports in properties file");

	// Assign a NullPowerPort to any port that doesn't already have
	// a PowerPort
        for ( int i = 0; i < _ports.size(); i++ )
	{
	    DevicePort port = (DevicePort)_ports.elementAt(i);
	    if ( port._powerPort == null )
	    {
		_log4j.debug("Assign NullPowerPort to port #" + port.getIndex());
		port.setPowerPort(new NullPowerPort());
	    }
	}

    }


    /** Get configuration of all ports. */
    public Vector getPorts() {
        return _ports;
    }


    /** Scan specified port. */
    public void scanPort(DevicePort port, String serviceSource) 
	throws LinkageError, PortOccupiedException, 
	       IOException, DuplicateIdException, PortNotFound, Exception
    {
        if ( port._service != null )
            throw new PortOccupiedException(new String(port._service.getName()) + 
					    " already on port");

	if (serviceSource.length() == 0) {
	    // Use source file specified by node configuration
	    serviceSource = port._jarName;
	}

        // bail out if no jar file has been specified
        if ( serviceSource.length() == 0 ) {
	    _log4j.info("Service jar not specified for port "
			 + port._portName);
	    throw new Exception("Service jar not specified for port " + 
				port._portName);
	}

        //create the sand box for this port
        String sandBoxPath = _siamHome + File.separator + 
            _serviceJarLocation + File.separator + "port" + port.getIndex();

        //create the sand box directory
        ServiceSandBox sandBox = new ServiceSandBox(sandBoxPath);

        // Configure PowerPort and SerialPort
        try {
	    _log4j.debug("Opening instrument port...");
	    port.openComms();
	}
	catch (Exception e) {
	    logError(e.getClass().getName() + " " + port._portName, e);
	    throw new Exception(port._portName + ": " + e.toString());
	}

        //if it's configured for a PUCK check for a PUCK, else look 
        //for the Jar file
	// This stuff should go into DevicePort with null implementation,
	// and the serial implementation should be in SerialDevicePort.
	// But that's more surgery than I want to do right now.  rah, 22may2008
        if ( port instanceof SerialDevicePort )
	{
	    SerialPort serialPort = ((SerialDevicePort)port)._serialPort;

	    if ( (serialPort != null) && (serviceSource.equalsIgnoreCase("PUCK")) )
	    {
		// Turn on port power
		try {
		    port._powerPort.initialize();
		    port._powerPort.setCurrentLimit(_DEFAULT_PUCK_CURRENT);
		    port._powerPort.enableCommunications();
		    port._powerPort.connectPower();
		}
		catch (Exception e) {
		    _log4j.info("Caught exception while managing port " + 
				port._portName + " power: " + e);
		}

		Puck puck = null;
		boolean error = false;
		try {
		    puck = new Puck_1_3(serialPort);
		    puck.setPuckMode(5);

		    // Get PUCK InstrumentDatasheet
		    Puck.Datasheet datasheet = puck.readDatasheet();

		    serviceSource = loadFromPuck(puck, datasheet, sandBox);
		}
		catch (Exception e) {
		    error = true;
		    _log4j.error(e.getMessage(), e);
		    throw e;
		}
		finally {

		    if (puck != null) {
			boolean instrumentMode = false;
			for (int i = 0; i < 3; i++) {
			    // Put device into instrument mode
			    try {
				puck.setInstrumentMode();
				instrumentMode = true;
				break;
			    }
			    catch (Exception e) {
				_log4j.error("puckSetInstrumentMode()", e);
			    }
			}
			if (!instrumentMode) {
			    throw new Exception("Couldn't get PUCK into instrument mode");
			}
		    }

		    if (port._powerPort != null) {
			//power down the puck
			try {
			    port._powerPort.disconnectPower();
			}
			catch (Exception e) {
			    _log4j.info("Caught exception while disconnecting power to  port " + 
					port._portName  + ": " + e);
			}
		    }
		    if (error) {
			_log4j.info("close port");
			closePort(port);
		    }
		}
	    }
	}


	// Look for instrument service jar file in onboard file system
	String jarFileName = _siamHome + File.separator + 
	    _serviceJarLocation + File.separator + serviceSource;

	File jarFile = new File(jarFileName);

	// bail out if the user specified jar file is not found
	if ( !jarFile.exists() ) {
	    logError("Service jar file " + jarFile.getName() + 
		     " not found.", null);

	    closePort(port);

	    throw new Exception("Service jar file " +
				jarFile.getName() + " not found");
	}

	File sandBoxJar = new File(sandBox.getJarPath());

	// if the jar file specified is different than the sandBox jar 
	// file, delete all sandBox files and copy the new file over
	if ( (!sandBoxJar.exists()) || 
	     (!FileUtils.compareFile(jarFileName, 
				     sandBox.getJarPath())) ) {
	    _log4j.info("found new service jar: " + jarFile.getName());
	    //things have changed, blow out the sandBox
	    sandBox.deleteAllFiles();
	    //copy the file to the sand box
	    FileUtils.copyFile(jarFileName, sandBox.getJarPath());
	}


	// Load and instantiate the service class
	try {
	    _log4j.debug("Creating DeviceServiceClassLoader " + " from file "
			  + jarFile.getName() + " size=" + jarFile.length());

	    //hand the class loader the jar file in the sandBoxy
	    DeviceServiceClassLoader classLoader = 
		new DeviceServiceClassLoader(sandBox.getJarPath());

	    _log4j.info("Starting service...");

	    // Load service classes, install appropriate class files in 
	    // codebase, and instantiate the service.
	    port._service = classLoader.instantiateService(_codebaseLocation);
	}
	catch (LinkageError e) {
	    _log4j.error("LinkageError: ", e);

	    throw new Exception("GOT LINKAGE-EXCEPTION");
	}
	catch (Exception e) {
	    logError("Caught exception loading/instantiating service class ", 
		     e);

	    // Close the port
	    closePort(port);

	    throw e;
	}


	try {
	    // Create a SerialInstrumentPort
	    port.createInstrumentPort();
	}
	catch ( Exception e ) {
	    logError("scanPort, port " + port._portName + 
		     " - failed to initialize InstrumentPort", e);
	    closePort(port);
	    throw e;
	}

	// check ISI-ID against services already running
	try {
	    checkIsiID(sandBox.getJarPath());
	}
	catch ( Exception e ) {
	    logError("scanPort(), port " + port._portName + 
		     ", checking ISI ID: - caught Exception", e);

	    closePort(port);
	    throw e;
	}

	// Update the service cache
	try {
	    updateSandbox(sandBox);
	}
	catch ( Exception e ) {
	    logError("updateSandbox(): ", e);
	    closePort(port);
	    throw e;
	}

	boolean serviceStarted = false;
	Exception initException = null;

	// start the service
	try {
	
	    // Initialize service
	    port._service.initialize(_nodeProps, NodeManager.getInstance(), 
				     port._instrumentPort, sandBox,
				     _serviceXMLLocation,
				     _servicePropertiesLocation, 
				     _serviceCacheLocation);


	    // Run the service
	    for ( int i = 0; i < 3; i++ ) {

		if (port._service.getStatus() == Device.SHUTDOWN) {
		    throw new Exception("Shutting down");
		}

		try {
		    port._service.prepareToRun();
		    serviceStarted = true;
		    break;
		}
		catch ( InitializeException e ) {
		    //save the service.initialize() exception
		    initException = e;
		    // power down the instrument port
		    port._instrumentPort.disconnectPower();
		    port._instrumentPort.disableCommunications();
		    _log4j.warn("service.run() failed on try " + i
				+ ": " + e);
		}
	    }

	    if ( !serviceStarted ) {
		throw initException;
	    }
	}
	catch ( Exception e ) {
	    logError("Initialize error on port "
		     + port._portName + " (" + 
		     jarFile.getName() + ") ", e);
	    e.printStackTrace();
	    closePort(port);
	    throw e;
	}


	// Service started; generate an event and post it to the 
	// EventManager
	ServiceEvent event = 
	    new ServiceEvent(this, ServiceEvent.STATE_CHANGE,
			     ServiceEvent.INSTALLED, 
			     (int )port._service.getId());

	// Post event to the EventManager queue
	EventManager.getInstance().postEvent(event);


	_log4j.info("service started");

	NodeManager nodeManager = NodeManager.getInstance();

	// Document new instrument list in log file
	nodeManager.logNodeConfiguration("Added service for device " + 
					 port._service.getId() + " on port " + 
					 port._portName);
    }


    /** Close the port, shutdown associated service if any, free resources. */
    public byte[] closePort(DevicePort port) {
	return closePort(port, false);
    }

    /** Close the port, shutdown associated service if any, free resources;
     optionally put device in 'safe mode' before shutting down service. */
    synchronized public byte[] closePort(DevicePort port, boolean enterSafeMode) {

	byte[] msg = removeService(port, enterSafeMode);

	port.closeComms();

	if (port._powerPort != null) {
	    // Disconnect and isolate port
	    port._powerPort.disconnectPower();
	    port._powerPort.isolatePort();
	}

	return msg;
    }


    /** Extract service.properties and service.xml the to sand box */
    void updateSandbox(ServiceSandBox sandBox) throws Exception {
	_log4j.debug("create JarFile...");
	JarFile jarFile = new JarFile(sandBox.getJarPath());
	ZipEntry zipEntry = null;
	InputStream input = null;

	//if there is no properties file extract it from the jar
	File propFile = new File(sandBox.getPropertiesPath());

	String servicePropertiesLocation = sandBox.getPath() + File.separator
	    + sandBox.PROPERTIES_NAME;
	_servicePropertiesLocation = servicePropertiesLocation;
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
	    _serviceXMLLocation = destinationName;
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


    /** Verify that each service has a unique ID. */
    void checkIsiID(String jarFilename) 
	throws DuplicateIdException, Exception {

	//create the jar file
	String propertyFilename = "service.properties";
	JarFile jarFile = null;
	ZipEntry zipEntry = null;

	jarFile = new JarFile(jarFilename);
	zipEntry = jarFile.getEntry(propertyFilename);

	if ( zipEntry == null ) {
	    throw new Exception("Service properties file \"" + 
				propertyFilename
				+ "\" not found in jar file " + 
				jarFile.getName());
	}

	Properties properties = new Properties();
	InputStream input = jarFile.getInputStream(zipEntry);
	properties.load(input);
	input.close();
	_log4j.debug("checkIsiID() - properties from " + jarFilename + ": " + 
		     properties);

	ServiceAttributes attributes = new ServiceAttributes();

	_log4j.debug("checkIsiID() - get attributes from properties");
	attributes.fromProperties(properties, false);
	_log4j.debug("checkIsiID() - GOT attributes from properties");

	//check ID against existing services
	for ( int i = 0; i < _ports.size(); i++ ) {
	    DevicePort port = (DevicePort) _ports.get(i);

	    if ( port._service != null ) {
		if ( port._service.getId() == attributes.isiID ) {
		    String es = "service \""
			+ new String(port._service.getName())
			+ "\" on port \""
			+ new String(port._service.getCommPortName())
			+ "\" already has isiID " + attributes.isiID;

		    throw new DuplicateIdException(es);
		}
	    }
	}
    }

    /** Determine configuration of all ports. */
    public void scanPorts()
    {
	for ( int i = 0; i < _ports.size(); i++ )
	{
	    DevicePort port = (DevicePort) _ports.elementAt(i);
	    String serviceJarName = port._jarName;

	    port.addCommPortListener();

	    // bail out if no jar file has been specified
	    if ( (serviceJarName == null) || (serviceJarName.length() == 0) )
	    {
		_log4j.info("Service jar not specified for port "
			    + port._portName);
		continue;
	    }

	    try {
		_log4j.debug("scanPorts() - call scanPort() for " + 
			      port._portName);
		scanPort(port, serviceJarName);
	    }
	    catch ( PortOccupiedException e ) {
		logError("scanPorts() - Service already running on port "
			 + port._portName, e);
	    }
	    catch ( DuplicateIdException e ) {
		logError("scanPorts() - Duplicate ID on port " + 
			 port._portName, e);
	    }
	    catch ( PortNotFound e ) {
		logError("scanPorts() - Port " + port + " not found ", e);
	    }
	    catch ( IOException e ) {
		logError("scanPorts() failed on " + 
			 port._portName, e);
	    }
	    catch ( LinkageError e ) {
		logError("scanPorts() LinkageError on port" + 
			 port._portName, e);
	    }
	    catch ( Exception e ) {
		logError("scanPorts() Exception on port " + 
			 port._portName, e);
	    }

	}
    }

    /** Remove service on specified port. */
    protected byte[] removeService(DevicePort port, boolean enterSafeMode) {

	_log4j.info("removeService()");
	if ( port._service == null ) {
	    // No service on port
	    _log4j.debug("removeService() - no service on port");
	    return "".getBytes();
	}

	NodeManager nodeMgr = NodeManager.getInstance();

	try {
	    // Put service into safe mode if specified
	    if (enterSafeMode) {
		if (port._service instanceof Safeable) {
		    nodeMgr.logMessage("Safing device " + 
				       port._service.getId() + 
				       " (" + 
				       new String(port._service.getName()) + 
				       ")");


		    if (port._service instanceof BaseInstrumentService) {
			// Interrupt any threads currently accessing 
			// service's instrument
			BaseInstrumentService instrument = 
			    (BaseInstrumentService )port._service;

			instrument.interruptDeviceAccess(5000);
		    }

		    port._service.managePowerWake();

		    ((Safeable )port._service).enterSafeMode();
		    nodeMgr.logMessage("Device " + port._service.getId() + 
				       " in safe mode");
		}
		else {
		    // Service not safeable
		    nodeMgr.logMessage("Device " + 
				       port._service.getId() + 
				       " (" + port._service.getName() + 
				       ") is not safeable");
		}
	    }
	}
	catch (Exception e) {
	    nodeMgr.logMessage("Caught exception while safing device " + 
			       port._service.getId() + ": " + e);
	}

	byte[] msg = null;
	try {

	    // Shutdown the service
	    _log4j.info("Invoke service.shutdown()");
	    msg = port._service.shutdown();
	    _log4j.info("service.shutdown() complete");

	    long deviceID = port._service.getId();

	    // Remove service from registry
	    if (!UnicastRemoteObject.unexportObject(port._service, true)) {
		logError("unexportObject() failed for service on port "
			 + port._portName, null);
	    }
	    else {
		_log4j.info("unexportObject() succeeded for service "
			      + "on port " + port._portName);
	    }
	    port._service = null;

	    nodeMgr.logNodeConfiguration("Removed service for device " + 
					 deviceID + " from port "
					 + port._portName);

	    // Create ServiceEvent
	    ServiceEvent event = 
		new ServiceEvent(this, ServiceEvent.STATE_CHANGE,
				 ServiceEvent.REMOVED, 
				 (int) deviceID);

	    _log4j.info("removeService() - postEvent()");
	    // Post event to the EventManager queue
	    EventManager.getInstance().postEvent(event);

	}
	catch ( NoSuchObjectException e ) {
	    logError("PortManager couldn't unexport object", e);
	}

	_log4j.info("removeService() - done");
	return msg;
    }

    /** Return Port with specified name. */
    DevicePort getPortByName(String portName) throws PortNotFound {

	for ( int i = 0; i < _ports.size(); i++ ) {
	    DevicePort port = (DevicePort) _ports.elementAt(i);

	    if ( port == null )
		continue;

	    String pName = port._portName;
	    if ( pName == null )
		continue;

	    if ( pName.equals(portName) )
		{
		    return port;
		}
	}
	// No matching port with specified name
	logError("getPortByName() - no port matching " + portName, null);
	throw new PortNotFound();
    }


    /** Print error/exception to stdout/stderr log, and write message to 
	node log. */
    void logError(String message, Throwable e) {
	String exceptionString;
	if ( e != null )
	    {
		_log4j.error(message, e);
		exceptionString = e.toString();
	    }
	else
	    {
		_log4j.error(message);
		exceptionString = "";
	    }
	// Write to node log
	NodeManager.getInstance().logMessage("ERR: " + message + ": " +
					     exceptionString);
    }


    /** Return name of jar file retrieved from PUCK, or if no PUCK payload, 
	return jar file name based on PUCK UUID. Extract jar file if 
	necessary and manage sandbox. */
    protected String loadFromPuck(Puck puck, Puck.Datasheet datasheet, 
				  ServiceSandBox sandBox) 
	throws Exception {

	String jarName = null;

	// If there is a payload and it's changed from the 
	// last load, retrieve it
	PuckUtils.SiamPayloadParams payload = null;

	if (_usePuckPayload && puck.hasPayload() && 
	    (payload = PuckUtils.readSiamPayloadParams(puck)) != null) {

	    // Assume you have the same payload, then check it;
	    // trust but verify!
	    boolean payloadChanged = false;

	    try {
		// Check the UUID of the PUCK
		UUID sandBoxUUID = sandBox.getInstrumentUUID(); 
		if (!sandBoxUUID.equals(datasheet.getUUID())) {
		    payloadChanged = true;
		    _log4j.info("PUCK UUID has changed");
		    _log4j.info("PUCK UUID    : " + 
				datasheet.getUUID());
		    _log4j.info("service cache UUID: " + 
				sandBox.getInstrumentUUID());
		}
		else {
		    _log4j.info("Instrument has not changed");
		}
			
		// check the MD5 of the PUCK payload
		byte[] cachedMD5 = sandBox.getJarMD5();
		for (int i = 0; i < cachedMD5.length; i++) {
		    if ( payload._md5Checksum[i] != 
			 cachedMD5[i] ) {
			payloadChanged = true;
			_log4j.info("PUCK payload checksum has changed");
			break;
		    }
		}
	    }
	    catch (Exception e) {
		// Assume payload has changed
		payloadChanged = true;
	    }

	    if (payloadChanged) {
		// If you are retrieving the payload from the PUCK 
		// blowout the service cache
		sandBox.deleteAllFiles();

		boolean gotIt = false;
		// copy the payload to the service cache
		for ( int i = 0; i < _MAX_PUCK_READ_TRIES; ++i ) {

		    PuckUtils.readSiamPayload(puck, sandBox.getJarPath());
		    gotIt = true;
		    break;
		}

		if (!gotIt) {
		    throw new Exception("Failed reading payload");
		}

	    }
	    else {
		_log4j.info("Instrument payload has not changed");
	    }


	    File jarfile = new File(_siamHome + File.separator + 
				    _serviceJarLocation + File.separator + 
				    "service-" + 
				    datasheet.getUUID().toString() + 
				    ".jar");

	    // Copy from sandbox to standard service jar directory
	    FileUtils.copyFile(sandBox.getJarPath(), jarfile.getPath());

	    jarName = jarfile.getName();
	}
	else { 
	    _log4j.info("No SIAM payload, look up service by UUID: " + 
			datasheet.getUUID());
                
	    // Load PUCK registry file
	    Properties registry = new Properties();
	    FileInputStream in = 
		new FileInputStream(PuckUtils.PUCK_REGISTRY_NAME);

	    registry.load(in);
	    in.close();

	    jarName = 
		registry.getProperty(datasheet.getUUID().toString());

	    if ( jarName == null ) {
		throw new Exception("Service jar not specified in registry for PUCK UUID " +
				    datasheet.getUUID().toString());
	    }
                

	    _log4j.debug("Use Jar file " + jarName);

	    File jarfile = new File(_siamHome + File.separator + 
				    _serviceJarLocation + File.separator + 
				    jarName);


	    // bail out if the user specified jar file is not found
	    if ( !jarfile.exists() ) {

		String err = "Service jarfile \"" + jarName + 
		    "\" for UUID: " + 
		    datasheet.getUUID() + " not found";

		logError(err, null);
		throw new Exception(err);

	    }

	    _log4j.debug("Check sandbox... " + jarName);

	    File sandBoxJar = new File(sandBox.getJarPath());

	    // if the jar file specified is different than the sandBox jar 
	    // file, delete all sandBox files and copy over the new Jar
	    if ( (!sandBoxJar.exists()) || 
		 (!FileUtils.compareFile(jarfile.getPath(), 
					 sandBox.getJarPath())) ) {
		_log4j.info("found new service jar: " + jarfile.getName());
		// things have changed, blow out the sandBox
		sandBox.deleteAllFiles();
		// copy the file to the sand box
		FileUtils.copyFile(jarfile.getPath(), sandBox.getJarPath());
	    }
	}

	// Store the new puck instrument datasheet
	_log4j.debug("Store PUCK datasheet in sandbox... " + jarName);
	sandBox.storeDatasheet(datasheet);

	return jarName;
    }
}

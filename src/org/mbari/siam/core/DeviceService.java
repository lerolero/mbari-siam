/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.Hashtable;

import org.mbari.siam.utils.SiamBufferedInputStream;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.RemoteSerialPort;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.distributed.UnknownLocationException;
import org.mbari.siam.distributed.RangeException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.ZeroConf;

/**
 * DeviceService implements the Device interface.
 * 
 * @author Tom O'Reilly
 */
public abstract class DeviceService extends UnicastRemoteObject 
    implements Device, DeviceServiceIF {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(DeviceService.class);

    public static final String XML_FILENAME = "service.xml";

    public static final String PROPERTIES_FILENAME = "service.properties";

    public static final String CACHED_PROPERTIES_FILENAME = "service.cache";

    private String _frameworkVersion = new String("$Name: HEAD $");

    /** Reference to parent node */
    protected Parent _parentNode = null;

    /** Reference to host node properties. */
    protected NodeProperties _nodeProperties = null;

    /** Device instrument port (power and comms) */
    protected InstrumentPort _instrumentPort = null;

    /** InputStream from instrument */
    protected SiamBufferedInputStream _fromDevice;

    /** OutputStream to instrument */
    protected InstrumentPortOutputStream _toDevice;

    /** Path to service XML metadata */
    protected String _serviceXMLPath;

    /** Path to service state cache metadata */
    protected String _serviceCachePath;

    /** Path to service properties file */
    protected String _servicePropertiesPath;

    /** Service 'sandbox' on node file system */
    protected ServiceSandBox _sandBox;

    /** Our RegistryEntry for InstrumentRegistry */
    protected RegistryEntry _regEntry;

    /** Basic service attributes. */
    private ServiceAttributes _attributes = new ServiceAttributes(this);

    /** remote serial port server for this device */
    private RemoteSerialPortServer _serialPortServer = null;

    private int _preSuspendState = Device.OK;

    /** String representation of service name; use this for debugging purposes,
	to avoid excessive string construction */
    protected String _serviceName = null;

    /**
     * Constructor. Need no-argument constructor so class can be instantiated
     * via loader.
     */
    public DeviceService() throws RemoteException {

	super();
	_attributes.serviceName = getClass().getName().getBytes();
	setStatusInitial();
    }

    /** Unique identifier for device instance */
    public long getId() {
	return _attributes.isiID;
    }

    ////////////////////////////////
    // Accessor methods

    /** Set parent node */
    public void setParent(Parent parent) {
	_parentNode = parent;
    }

    /** Returns name of driver class. */
    public final byte[] getName() {
	if (_serviceName == null) {
	    // Allocate service name once, for use in debug messages 
	    _serviceName = new String(_attributes.serviceName);
	}

	return _attributes.serviceName;
    }

    /** Returns name of host port. */
    public byte[] getCommPortName() {
	return (_instrumentPort.getCommPortName()).getBytes();
    }

    /** Return remote serial port to client. */
    public RemoteSerialPort getRemoteSerialPort() throws IOException,
							 UnknownHostException {
	RemoteSerialPort rs = null;

	try {
	    rs = getRemoteSerialPort(RemoteSerialPortServer._DEFAULT_TIMEOUT);
	} catch (RangeException e) {
	    //you'll never get here
	    _log4j.error(e);
	}

	return rs;
    }

    /** Return remote serial port to client. */
    public RemoteSerialPort getRemoteSerialPort(int timeout)
	throws IOException, RangeException, UnknownHostException {
	//if device not suspended throw exception
	if (getStatus() != Device.SUSPEND)
	    throw new IOException("service on port "
				  + new String(getCommPortName()) + " not suspended");

	//if there is already a server run throw an exception, you
	//can't have two
	if (_serialPortServer != null)
	    throw new IOException("remote serial port already active on "
				  + new String(getCommPortName()));

	_serialPortServer = new RemoteSerialPortServer(getInstrumentPort(),
						       this);
	_serialPortServer.setTimeout(timeout);

	_serialPortServer.start();

	return new RemoteSerialPort(_serialPortServer.getServerInetAddress(),
				    _serialPortServer.getServerPort());
    }

    /** Returns a reference to the instrument port. */
    public InstrumentPort getInstrumentPort() {
	return _instrumentPort;
    }

    /** Return InetAddress of device service host. */
    public InetAddress host() throws UnknownHostException {
	return InetAddress.getLocalHost();
    }

    /** Initialize the service. */
    public void initialize(NodeProperties nodeProperties, Parent parent,
			   InstrumentPort port, ServiceSandBox sandBox,
			   String serviceXMLPath, String servicePropertiesPath,
			   String cachedServicePath)

	throws MissingPropertyException, InvalidPropertyException,
	       PropertyException, InitializeException, IOException,
	       UnsupportedCommOperationException {

	_log4j.debug(this.getClass().getName() + ".initialize()");
	_sandBox = sandBox;

	setNodeProperties(nodeProperties);
	setParent(parent);


	_serviceXMLPath = serviceXMLPath;

	_servicePropertiesPath = servicePropertiesPath;

	_serviceCachePath = cachedServicePath;

	setInstrumentPort(port);

	// Initialize non-configurable attributes
	_attributes.frameworkVersion = getFrameworkVersion();
	_attributes.parentID = _parentNode.getParentId();
	_attributes.serviceStatus = Device.INITIAL;

    }

    /** Stop the service. */
    public byte[] shutdown() {

	_log4j.debug("shutdown() - shutdown instrumentPort");

	// This might get called before service initializes - check for
	// non-null instrument port before shutting it down.
	if (_instrumentPort != null) {
	    _instrumentPort.shutDown();
	}

	setStatusShutdown();

	_log4j.debug("shutdown() - return");

	return "".getBytes();
    }

    /** Set serial parameters if the InsrumentPort is a SerialInsrumentPort */
    public void setSerialPort() throws IOException,
				       UnsupportedCommOperationException {

	if (_instrumentPort instanceof SerialInstrumentPort) {
	    // Get serial port parameters from subclass
	    SerialInstrumentPort sip = (SerialInstrumentPort) _instrumentPort;
	    sip.setSerialPortParams(getSerialPortParameters());

	    _fromDevice = new SiamBufferedInputStream(_instrumentPort
						      .getInputStream());

	    _toDevice = _instrumentPort.getOutputStream();
	} else {
	    throw new IOException("setSerialPort() failed: InstrumentPort "
				  + "is not SerialInstrumentPort");
	}
    }

    /**
     * Set the instrument port. (Called at startup.)
     * 
     * @param port
     *            instrument port to be assigned to driver
     */
    public void setInstrumentPort(InstrumentPort port) {
	_log4j.debug("setInstrumentPort(): " + port);
	_instrumentPort = port;
    }


    /**
     * Put service in SUSPEND state. Release resources (e.g. serial port) for
     * use by other applications. Note that this method should NOT be 
     * be synchronized, so that a sampling thread can be interrupted.
     */
    public void suspend() {

	_preSuspendState = getStatus();

	setStatusSuspend();

	_instrumentPort.suspend();
	// Enable communications
	_instrumentPort.enableCommunications();

	_log4j.debug("Now in DeviceService.suspend()");
    }

    /**
     * Put service in OK state. Re-acquire resources (e.g. serial port).
     */
    public synchronized void resume() {

	_log4j.debug("Now in DeviceService.resume()");

	//shutdown the remote serial port server if it was started
	if (_serialPortServer != null) {
	    _serialPortServer.shutdown();
	    _serialPortServer = null;
	}

	// Reacquire serial port
	try {
	    _instrumentPort.resume();

	    if (_instrumentPort instanceof SerialInstrumentPort) {
		setSerialPort();
	    }

	    setStatus(Device.OK);

	    _log4j.debug("Done with DeviceService.resume()");
	} catch (UnsupportedCommOperationException e) {
	    _log4j.error("DeviceService.resume(): "
			       + "caught UnsupportedCommOperationException");
	} catch (IOException e) {
	    _log4j.error("DeviceService.resume(): "
			       + "caught IOException");
	} catch (Exception e) {
	    _log4j.error("DeviceService.resume(): " + "caught Exception");
	}
    }

    private int _samplingErrorCount = 0;

    private int _samplingCount = 0;

    private int _samplingRetryCount = 0;

    /** Return instrument service status. */
    public int getStatus() {
	return _attributes.serviceStatus;
    }

    /** return sampling error count */
    public int getSamplingErrorCount() {
	return _samplingErrorCount;
    }

    /** return sample count */
    public int getSamplingCount() {
	return _samplingCount;
    }

    public int getSamplingRetryCount() {
	return _samplingRetryCount;
    }

    /** Set service status to Device.ERROR. */
    protected synchronized final void setStatusError() {
	if (_attributes.serviceStatus == Device.SAMPLING)
	    _samplingErrorCount++;
	_attributes.serviceStatus = Device.ERROR;
    }

    protected synchronized final void incRetryCount() {
	if (_attributes.serviceStatus == Device.SAMPLING)
	    _samplingRetryCount++;
    }

    /** Set service status to Device.SHUTDOWN */
    protected final void setStatusShutdown() {
	_attributes.serviceStatus = Device.SHUTDOWN;
    }

    /** Set service status to Device.INITIALIZING. */
    protected final void setStatusInitial() {
	_attributes.serviceStatus = Device.INITIAL;
    }

    /** Set service status to Device.OK. */
    protected final void setStatusOk() {
	if (_attributes.serviceStatus == Device.SAMPLING)
	    _samplingCount++;
	_attributes.serviceStatus = Device.OK;
    }

    /** Set service status to Device.SAMPLING. */
    protected synchronized final void setStatusSampling() {
	_attributes.serviceStatus = Device.SAMPLING;
    }

    /** Set service status to Device.SUSPEND. */
    protected final void setStatusSuspend() {
	_attributes.serviceStatus = Device.SUSPEND;
    }


    /** Set service status to Device.SAFE. */
    public final void setStatusSafe() {
	_log4j.debug(new String(getName()) + " status change: SAFE");
	_attributes.serviceStatus = Device.SAFE;
    }

    
    
    /** Set status to specified value */
    void setStatus(int status) {
	_attributes.serviceStatus = status;
    }

    /** Put device power into active state */
    protected void managePowerWake() {
	_instrumentPort.connectPower();
	_instrumentPort.enableCommunications();
    }

    /** Put device power into quiescent state */
    protected void managePowerSleep() {
	_instrumentPort.disableCommunications();
	_instrumentPort.disconnectPower();
    }


    /** Subclass should return serial port parameters to use on port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {
	throw new UnsupportedCommOperationException("Not implemented");
    }
	
    /** Get device metadata packet. */
    public abstract MetadataPacket getMetadata(byte[] cause, int components,
					       boolean logPacket) 
	throws RemoteException;


    /** Set (hard coded) property defaults () */
    public void initializePropertyDefaults() throws InitializeException {
    }

    /** Set reference to host node properties. */
    public void setNodeProperties(NodeProperties nodeProperties)
	throws MissingPropertyException, InvalidPropertyException {
	_nodeProperties = nodeProperties;
    }

    /**
     * Initialize driver and instrument. This method must be invoked by outside
     * entity before data acquisition starts. 
     */
    public void prepareToRun() throws InitializeException, InterruptedException {

	if (_attributes.advertiseService) {
	    _log4j.debug("call advertiseService()");
	    advertiseService();
	}
	else {
	    _log4j.debug("advertiseService not set");
	}

	setStatusInitial();
	_log4j.debug("run() - initialize instrumentPort: " + _instrumentPort);
	_instrumentPort.initialize();

	_log4j.debug("run() - acquire streams");

	try {
	    //acquire streams to the device
	    _log4j.debug("run() - acquire device streams");

	    _fromDevice = new SiamBufferedInputStream(_instrumentPort
						      .getInputStream());

	    _toDevice = _instrumentPort.getOutputStream();

	    if ((_instrumentPort instanceof SerialInstrumentPort)
		|| (_instrumentPort instanceof PuckSerialInstrumentPort)) {

		_log4j.debug("start(): " + "InstrumentPort instanceof "
			      + _instrumentPort.getClass().getName());

		// Get serial port parameters from subclass if this
		//is a serial input stream
		SerialInstrumentPort sip = (SerialInstrumentPort) _instrumentPort;

		sip.setSerialPortParams(getSerialPortParameters());
	    }

	} catch (Exception e) {
	    _log4j.error("Exception in DeviceService.run(): " + e);
	    e.printStackTrace();
	    throw new InitializeException(e.getMessage());
	}

    }

    /** Set the ServiceAttributes object for this service. */
    public void setAttributes(ServiceAttributes attributes) {
	_attributes = attributes;
	// Always set framework version
	_attributes.frameworkVersion = getFrameworkVersion();
    }


    /** Create a RegistryEntry for the InstrumentRegistry */
    protected void createRegistryEntry()
    {
	if ((_attributes.registryName != null) &&
	    (_attributes.registryName.length() > 0))
	{
	    _regEntry = new RegistryEntry(this, _attributes.registryName);
		
	    if (_regEntry != null) {
		try
		{
		    InstrumentRegistry.getInstance().add(_regEntry);
		} catch (Exception e) {
		    _log4j.error("Exception in adding RegistryEntry: " + e);
		}
	    }
	}
    }


    /** Return byte-string that specifies the SIAM framework version. */
    public byte[] getFrameworkVersion() {
	return _frameworkVersion.getBytes();
    }

    /** Return service's registry name */
    public String registryName() {
	return(_attributes.registryName);
    }

    /** Return service's RegistryEntry */
    public RegistryEntry registryEntry() {
	return(_regEntry);
    }

    /** Add an InstrumentDataListener		*/
    public void addDataListener(InstrumentDataListener listener)
    {
	if (_regEntry != null)
	    _regEntry.addDataListener(listener);
    }

    /** Remove an InstrumentDataListener		*/
    public void removeDataListener(InstrumentDataListener listener)
    {
	if (_regEntry != null)
	    _regEntry.removeDataListener(listener);
    }

    /**
     * Return service's attributes object.
     */
    public ServiceAttributes getAttributes() {
	return _attributes;
    }

    /** Return Location of device. NOT YET IMPLEMENTED. */
    public Location getLocation() throws UnknownLocationException {
	throw new UnknownLocationException("Not implemented");
    }



    /** Advertise service on network */
    protected void advertiseService() {

	// Form service name from last element of service class name
	String serviceName = 
	    serviceName = getClass().getName();
	    
	int index = serviceName.lastIndexOf(".");
	if (index >= 0) {
	    serviceName = serviceName.substring(index+1);
	}

	serviceName = serviceName + "-" + getId();

	// NOTE - need to put RMI server port here?
	int port = 1099;

	String type = ZeroConf.SIAM_DEVICE_TYPE;
	if (this instanceof Instrument) {
	    type = ZeroConf.SIAM_INSTRUMENT_TYPE;
	}
    }
}


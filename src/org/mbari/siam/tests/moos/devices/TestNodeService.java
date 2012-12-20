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
package org.mbari.siam.tests.moos.devices;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.Timer;

import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.RemoteSerialPort;
import org.mbari.siam.distributed.PowerSwitch;
import org.mbari.siam.distributed.NodeEventCallback;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.distributed.UnknownLocationException;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.PortConfiguration;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.Subnode;
import org.mbari.siam.distributed.NodeSessionInfo;

import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.Scheduler;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.NullPowerPort;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.ServiceEvent;
//import org.mbari.siam.moos.deployed.SleepManager;
import org.mbari.siam.core.SleepManager;
import org.mbari.siam.core.*;
//single-type import breaks build b/c Location also exists in another imported package org.mbari.siam.tests.moos.devices
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.operations.utils.ExportablePacket;
import org.mbari.siam.utils.FileUtils;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.DeviceServiceLoader;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseDescription;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;

import moos.ssds.jms.PublisherComponent;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
 * TestNodeService implements the Node interface; instantiate, initialize, 
 * and run a device service. The service bytecode, any
 * related classes, and properties file are contained within a standard SIAM
 * instrument jar file or PUCK.
 * 
 * @author oreilly
 *  
 */
public class TestNodeService 
    extends UnicastRemoteObject 
    implements MOOSNode {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(TestNodeService.class);

    static final String PROPERTIES_FILENAME = "service.properties";
    static final String XML_FILENAME = "service.xml";
    static final String CACHE_FILENAME = "service.cache";

    public static final String INSTRUMENT_URL = 
	"rmi://localhost/siamInstrument";

    long _nodeID = 999;

    boolean _publish = false;

    ExportablePacket _exportablePacket = new ExportablePacket();
    PublisherComponent _ssdsPublisher = null;
    DeviceServiceLoader _serviceLoader = new DeviceServiceLoader();
    String _portName = "";
    String _serviceURL = "";
    Device _service;

    /**
     *  
     */
    public TestNodeService() throws RemoteException {
	super();
	// TODO Auto-generated constructor stub

	if (_publish) {
	    _ssdsPublisher = new PublisherComponent();
	}

	Scheduler scheduler = Scheduler.getInstance();
	scheduler.loadDefaults();
    }

    /** Run the test. */
    public void run(String jarFileName, String portName, 
		    String codebaseDirectory) throws Exception {

	_log4j.debug("run(): jarfile=" + jarFileName + ", port=" + portName + 
		     ", codebaseDir=" + codebaseDirectory);
        System.setProperty("gnu.io.rxtx.SerialPorts", portName);

	_portName = portName;

	_serviceURL = 
	    _serviceLoader.run(jarFileName, portName, 
			       codebaseDirectory, new MyParent(),
			       INSTRUMENT_URL);

	if (_service == null) {
	    // Try to get the service proxy
	    try {
		_log4j.debug("publish() - get proxy " + _serviceURL);
		_service = (Device )Naming.lookup(_serviceURL);
		_log4j.info("Got service proxy");
	    }
	    catch (Exception e) {
		_log4j.error("Exception while retrieving proxy: ", e);
		return;
	    }
	}


	// Create and start sleep manager
	SleepManager sleepMgr = SleepManager.getInstance();
	sleepMgr.start();

	_log4j.info("Instrument service URL: " + _serviceURL);
    }

    /** Return true if specified device can supply Summary packets. */
    public boolean summarizing(long deviceID) 
	throws DeviceNotFound, RemoteException {

	Device device = getDevice(deviceID);

	if (device instanceof Summarizer) {
	    return true;
	}
	else {
	   return false;
	}
    }

    /** Implements Parent for this test. */
    class MyParent implements Parent {

	public void runDiagnostics(String string) throws Exception {
	    System.out.println("MyParent.runDiagnostics()");
	}

	public String getSoftwareVersion() {
	    return "dummy version";
	}

	/** Return this parent's ISI ID. */
	public long getParentId() {
	    return _nodeID;
	}

	/** Return location of specified device. */
	public org.mbari.siam.distributed.Location getLocation(long deviceID) {
	    return new org.mbari.siam.distributed.Location("In the lab");
	}

	/** Request power from the parent; return true if available, false if
	    not available. */
	public boolean powerAvailable(int milliamp) {
	    _log4j.warn("powerAvailable() not implemented");
	    return true;
	}


	/** Publish the specified event. Event published must be an instance of
	    NodeEvent. Since we want this utility to be usable on a Sidearm,
	    we can't use JMS stuff, and so can't publish to SSDS. Just print a 
	    message. 
	*/
	public void publish(EventObject event) {
	    _log4j.debug("publish()");

	    if (_service == null) {
		// Try to get the service proxy
		try {
		    _log4j.debug("publish() - get proxy " + _serviceURL);
		    _service = (Device )Naming.lookup(_serviceURL);
		    _log4j.info("Got service proxy");
		}
		catch (Exception e) {
		    _log4j.error("Exception while retrieving proxy: ", e);
		    return;
		}
	    }

	    if (event instanceof ServiceEvent) {
		ServiceEvent serviceEvent = (ServiceEvent )event;
		if (serviceEvent.getStateChange() == 
		    ServiceEvent.SAMPLE_LOGGED) {
		    // Must be an instrument service that generated this
		    Instrument service = (Instrument )_service;
		    DevicePacket packet = null;
		    try {
			packet = service.getLastSample();
			if (_publish) {
			    publishSample(packet);
			}	
			else {
			    System.out.println("Would publish: " + packet);
			}
		    } catch (NoDataException e1) {
			_log4j.error("No data from service");
			return;
		    }
		    catch (RemoteException e) {
			_log4j.error(e);
			return;
		    }
		}
		else {
		    _log4j.info("Got ServiceEvent type " + 
				 serviceEvent.getStateChange());
		}
	    }
	    else {
		_log4j.warn("publish() - event is not a ServiceEvent");
	    }
	}

    }

    void publishSample(DevicePacket packet) {
	try {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(bos);
	    _exportablePacket.wrapPacket(packet);
	    _exportablePacket.export(dos);
	    dos.flush();
	    byte[] exportedBytes = bos.toByteArray();
	    dos.close();
	    _ssdsPublisher.publishBytes(exportedBytes);
	}
	catch (IOException e) {
	    e.printStackTrace();
	}
    }

	
    /** Return InetAddress of device service host. */
    public InetAddress host() throws RemoteException, UnknownHostException {
	return InetAddress.getLocalHost();
    }

    /** Name of Node service class. */
    public byte[] getName() throws RemoteException {
	return (this.getClass().getName()).getBytes();
    }

    /** Unique identifier for Node instance */
    public long getId() throws RemoteException {
	return _nodeID;
    }

    /** Initialize the device. */
    public void initialize() throws RemoteException, InitializeException {
	// Does nothing
    }

    /** Turn Node power off. */
    public int powerOff() throws RemoteException {
	// Does nothing;
	return Device.OK;
    }

    /** Get configuration of Node ports. */
    public PortConfiguration[] getPortConfiguration() 
	throws RemoteException {
	PortConfiguration[] config = new PortConfiguration[1];
	config[0] = new PortConfiguration();
	config[0].set(_service.getId(), _portName.getBytes());
	return config;
    }

    /** Get specified device service proxy. */
    public Device getDevice(long deviceId) throws RemoteException,
						  DeviceNotFound {
	if (deviceId == _service.getId()) {
	    return (Device )_service;
	}
	else {
	    throw new DeviceNotFound();
	}
    }

    /** Get all device service proxies. */
    public Device[] getDevices() throws RemoteException {
	Device[] devices = new Device[1];
	devices[0] = (Device )_service;
	return devices;
    }

    /** Shutdown and remove device service from specified port. */
    public byte[] shutdownDeviceService(byte[] commPortName)
	throws RemoteException, PortNotFound, DeviceNotFound {
	throw new RemoteException("shutdownDeviceService() not implemented");
    }

    /** Scan all ports, load services. */
    public void scanPorts() throws RemoteException {
	throw new RemoteException("scanPorts() not implemented");
    }



    /** Scan specified ports, load service. */
    public void scanPort(byte[] commPortName) throws RemoteException,
						     PortNotFound, 
						     DeviceNotFound, 
						     IOException, 
						     PortOccupiedException,
						     DuplicateIdException {

	scanPort(commPortName, null);
    }


    /** Scan specified ports, load service. */
    public void scanPort(byte[] commPortName, byte[] source) 
	throws RemoteException,
	       PortNotFound, 
	       DeviceNotFound, 
	       IOException, 
	       PortOccupiedException,
	       DuplicateIdException {

	throw new RemoteException("scanPort() not implemented");
    }

    /** Get device service (if any) associated with specified port. */
    public Device getDevice(byte[] commPortName) throws RemoteException,
							PortNotFound, 
							DeviceNotFound {
	String inputName = new String(commPortName);
	if (inputName.equals(_portName)) {
	    return (Device )_service;
	}
	else {
	    throw new DeviceNotFound("Serice is running on port " + _portName);
	}
    }

    /** Get array of Node's Port objects. */
    public Port[] getPorts() throws RemoteException {
	_log4j.debug("getPorts() - create ports array");
	Port ports[] = new Port[1];
	_log4j.debug("getPorts() - create port[0]: _portName=" + _portName + 
		     ", _service=" + _service);
	ports[0] = new Port(_portName.getBytes(), _service.getId(), 
			    _service.getName());

	_log4j.debug("getPorts() - done - return");
	return ports;
    }

    /** Get array of Node's power switches. */
    public PowerSwitch[] getPowerSwitches() throws RemoteException {
	throw new RemoteException("getPowerSwitches() not implemented");
    }

    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.
     */
    public DevicePacketSet getDevicePackets(long sensorID, long startTime,
					    long endTime) 
	throws RemoteException, DeviceNotFound, NoDataException {

	if (sensorID == _service.getId()) {
	    Instrument instrument = (Instrument )_service;
	    return instrument.getPackets(startTime, endTime);
	}
	else {
	    throw new DeviceNotFound("Device has ID " + _service.getId());
	}
    }


    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window; only return packets of type specified in typemask parameter.
     */
    public DevicePacketSet getDevicePackets(long sensorID, long startTime,
					    long endTime, int typeMask) 
	throws RemoteException, DeviceNotFound,
	       NoDataException {

	// First get all packets within time window
	DevicePacketSet packetSet = getDevicePackets(sensorID, 
						  startTime,
						  endTime);

	// Filter packets based on typeMask
	Iterator iterator = packetSet._packets.iterator();
	while (iterator.hasNext()) {

	    DevicePacket packet = (DevicePacket )iterator.next();

	    boolean remove = false;

	    if ((packet instanceof MetadataPacket) && 
		(typeMask & DevicePacket.METADATA_FLAG) == 0) {
		remove = true;
	    }

	    else if ((packet instanceof SensorDataPacket) &&
		     (typeMask & DevicePacket.SENSORDATA_FLAG) == 0) {
		remove = true;
	    }
	    else if ((packet instanceof DeviceMessagePacket) &&
		     (typeMask & DevicePacket.DEVICEMESSAGE_FLAG) == 0) {
		remove = true;
	    }
	    else if ((packet instanceof SummaryPacket) &&
		 (typeMask & DevicePacket.SUMMARY_FLAG) == 0) {
		remove = true;
	    }

	    if (remove) {
		// Remove this packet from return vector.
		iterator.remove();
	    }
	}

	return packetSet;
    }


    /** Suspend service (if any) associated with specified port. */
    public void suspendService(byte[] portName) 
	throws RemoteException,
	       PortNotFound, DeviceNotFound {

	String inputName = new String(portName);
	if (inputName.equals(_portName)) {
	    _service.suspend();
	}
	else {
	    throw new PortNotFound("Service running on port " + _portName);
	}
    }

    /** Resume service (if any) associated with specified port. */
    public void resumeService(byte[] portName) 
	throws RemoteException,
	       PortNotFound, DeviceNotFound {

	String inputName = new String(portName);
	if (inputName.equals(_portName)) {
	    _service.resume();
	}
	else {
	    throw new PortNotFound("Service running on port " + _portName);
	}

    }


    /** Restart service (if any) associated with specified port. */
    public void restartService(byte[] portName) 
	throws RemoteException,
	       PortNotFound, DeviceNotFound, Exception {

	String inputName = new String(portName);
	if (inputName.equals(_portName)) {
	    _service.shutdown();
	    _service.run();
	}
	else {
	    throw new PortNotFound("Service running on port " + _portName);
	}

    }

    /** Get remote serial port. */
    public RemoteSerialPort getRemoteSerialPort(byte[] portName)
	throws RemoteException, UnknownHostException, PortNotFound,
	       PortOccupiedException, IOException {

	//if device not suspended throw exception
	if (_service.getStatus() != Device.SUSPEND)
	    throw new IOException("service on port " + new String(portName)
				  + " not suspended");

	// Get service implementation reference
	DeviceService implementation = _serviceLoader.getService();

	return implementation.getRemoteSerialPort();

    }

    /** Get remote serial port with specified timeout in milliseconds. */
    public RemoteSerialPort getRemoteSerialPort(byte[] portName, int timeout)
	throws RemoteException, UnknownHostException, PortNotFound,
	       PortOccupiedException, RangeException, IOException {

	throw new RemoteException("getRemoteSerialPort() not implemented");
    }

    /** Run Node's self-test routine. */
    public int test() throws RemoteException {
	throw new RemoteException("test() not implemented");
    }

    /** Return Location of Node. */
    public org.mbari.siam.distributed.Location getLocation() 
	throws RemoteException,
	       UnknownLocationException {
	throw new UnknownLocationException("getLocation() not implemented");
    }

    /** Get Node metadata. */
    public byte[] getMetadata() throws RemoteException {
	throw new RemoteException("getMetadata() not implemented");
    }


    /** Get Printable Schedule */
    public byte[] getSchedule() throws RemoteException {
	return getSchedule(Scheduler.MAX_LOOKAHEAD_SEC);
    }

    /** Get Printable Schedule */
    public byte[] getSchedule(long lookAheadSeconds) throws RemoteException {
	Scheduler scheduler = Scheduler.getInstance();
	String schedule = scheduler.showSchedule(lookAheadSeconds);
	return schedule.getBytes();
    }


    /** Get Printable Schedule for a specified device, lookahead */
    public byte[] getSchedule(byte[] port, long lookAheadSeconds)
	throws RemoteException {

	String inputName = new String(port);
	if (inputName.equals(_portName)) {

	    Instrument instrument = (Instrument )_service;
	    return instrument.getSampleSchedule();
	}
	else {
	    return ("Port not found - service on port " + _portName).getBytes();
	}
    }

    /** Add Schedule */
    public byte[] addSchedule(byte[] port, byte[] scheduleName,
			      byte[] schedule, boolean overwrite) 
	throws RemoteException {
	throw new RemoteException("addSchedule() not implemented");
    }

    /** Remove Schedule */
    public byte[] removeSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException {
	throw new RemoteException("removeSchedule() not implemented");
    }

    /** Suspend Schedule Entry */
    public byte[] suspendSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException {
	throw new RemoteException("suspendSchedule() not implemented");
    }

    /** Resume Schedule Entry */
    public byte[] resumeSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException {
	throw new RemoteException("resumeSchedule() not implemented");
    }

    /** Synchronize a (default sample) schedule entry */
    public byte[] syncSchedule(byte[] port, byte[] scheduleName,
			       long delayMillis) throws RemoteException {
	throw new RemoteException("syncSchedule() not implemented");
    }

    /** Register to receive notification of node events. */
    public void addEventCallback(NodeEventCallback callback)
	throws RemoteException {
	throw new RemoteException("addEventCallback() not implemented");
    }

    /** Called when IP link to shore is connected */
    public void shoreLinkUpCallback(InetAddress localAddress,
				    InetAddress remoteAddress)
	throws RemoteException, Exception {
	// Do something here when link-to-shore is connected. 
    }


    /** Called when IP link to shore is disconnected */
    public void shoreLinkDownCallback(InetAddress localAddress, 
				      InetAddress remoteAddress)
	throws RemoteException, Exception {
	// Do something here when link-to-shore is connected. 
    }


    /**
     * Request a lease of the Node's primary comms medium
     */
    public int establishLease(long leaseMillisec, byte[] clientNote) 
	throws RemoteException,
	       LeaseRefused {

	return establishLease(leaseMillisec, clientNote, true);
    }

    /**
     * Request a lease of the Node's primary or auxillary comms medium
     */
    public int establishLease(long leaseMillisec, byte[] clientNote,
			      boolean usePrimary) 
	throws RemoteException,
	       LeaseRefused {
	throw new LeaseRefused("establishLease() not implemented");
    }


    /**
     * Renew a lease with the Node's primary comms medium
     */
    public void renewLease(int leaseID, long leaseMillisec) 
	throws RemoteException, LeaseRefused {

	renewLease(leaseID, leaseMillisec, true);
    }


    /**
     * Renew a lease with the Node's primary or secondary comms medium
     */
    public void renewLease(int leaseID, long leaseMillisec, boolean usePrimary)
	throws RemoteException, LeaseRefused {
	throw new LeaseRefused("renewLease() not implemented");
    }

    /**
     * Terminate the session with the primary communications link.
     */
    public void terminateLease(int leaseID) throws RemoteException {
	terminateLease(leaseID, true);
    }


    /**
     * Terminate the session with the communications link.
     */
    public void terminateLease(int leaseID, boolean usePrimary) 
	throws RemoteException {
	throw new RemoteException("terminateLease() not implemented");
    }

    /**
     * Request that the CPU come on at a certain time in the future and/or
     * remain on for a certain duration.
     */
    public void cpuLease(int requestorID, long when, long howLong)
	throws RemoteException {
	throw new RemoteException("cpuLease() not implemented");
    }

    /** Return byte string with node health/status information. */
    public byte[] getStatus(boolean logPacket) throws RemoteException {
	throw new RemoteException("getStatus() not implemented");
    }

    /** Shutdown all services and exits the SIAM application. */
    public void exitApplication() throws RemoteException {
	throw new RemoteException("exitApplication() not implemented");
    }

    /**
     * Power up specified port and enable the comms if a service is not already
     * using it
     */
    public void powerUpPort(byte[] commPortName, int currentLimit)
	throws RemoteException, PortOccupiedException, PortNotFound,
	       RangeException, NotSupportedException {
	_log4j.error("powerUpPort() not yet implemented");
    }

    /**
     * Power down specified port and enable the comms if a service is not
     * already using it
     */
    public void powerDownPort(byte[] commPortName) 
	throws RemoteException,
	       PortOccupiedException, PortNotFound {
	_log4j.error("powerDownPort() not yet implemented");
    }

    /** Get list of immediate subnodes. */
    public InetAddress[] getSubnodes() throws RemoteException {
	throw new RemoteException("getSubnodes() not implemented");
    }

    /** Get list of immediate subnodes. */
    public Subnode[] getSubnodeObjects() throws RemoteException {
	throw new RemoteException("getSubnodeObjects() not implemented");
    }

    /** Remove specified subnode from list. */
    public void removeSubnode(InetAddress address) 
	throws RemoteException, Exception {
	throw new Exception("removeSubnode() not implemented");
    }


    /** Get list of all active leases managed by the node. */
    public LeaseDescription[] getLeases(boolean usePrimary) 
	throws RemoteException {
	throw new RemoteException("getLeases() not implemented");
    }

    /** Keep watchdog from waking up and resetting Node */
    public byte[] renewWDT() 
	throws RemoteException, IOException, FileNotFoundException {
	throw new RemoteException("renewWDT() not implemented");
    }

    /** Read watchdog time (WDT) status */
    public byte[] readWDT() 
	throws RemoteException, IOException, FileNotFoundException {
	throw new RemoteException("readWDT() not implemented");
    }

    /** Get status of DPA port associated with specified comm port. 
     Throws NotSupportedException if no power port is 
     associated with specified comm port. */
    public DpaPortStatus getDpaPortStatus(byte[] commPortName) 
	throws NotSupportedException, DeviceNotFound, RemoteException {
	throw new RemoteException("getDpaPortStatus() not implemented");
    }

    /** Get status of all DPA ports. */
    public DpaPortStatus[] getDpaPortStatus()
	throws RemoteException {
	throw new RemoteException("getDpaPortStatus() not implemented");
    }

    /** Return byte-string representation of service's NodeProperties 
	object. */
    public byte[] getProperties() throws RemoteException {
	throw new RemoteException("getProperties() not implemented");
    }

    /** Send signal to wakeup specified node. */
    public void wakeupNode(InetAddress node) 
	throws RemoteException, IOException {
	throw new RemoteException("wakeupNode() not implemented");
    }

    /** Send signal to wakeup all nodes. */
    public void wakeupAllNodes() 
	throws RemoteException, IOException {
	throw new RemoteException("wakeupAllNodes() not implemented");
    }

    /** Tell node to execute a Linux or shell command. */
    public byte[] runCommand(byte[] cmd, int timeoutSec)
        throws RemoteException, IOException, TimeoutException
    {
	throw new RemoteException("runCommanda() not implemented");
    }


    /** Put node and its devices into "safe" mode. */
    public void enterSafeMode() throws RemoteException, Exception {
	if (_service instanceof Safeable) {
	    ((Safeable )_service).enterSafeMode();
	}
    }


    /** Put node and its devices into "safe" mode. */
    public void enterSafeMode(long wait) throws RemoteException, Exception {
	if (_service instanceof Safeable) {
	    ((Safeable )_service).enterSafeMode();
	}
    }

    /** Return from "safe" mode; resume normal operations. */
    public void resumeNormalMode() throws RemoteException, Exception {
	if (_service instanceof Safeable) {
	    _service.shutdown();
	    _service.run();
	}
    }


    /** Append annotation to node data stream. */
    public void annotate(byte[] annotation) throws RemoteException {
	_log4j.warn("annotate() - not implemented");
    }

    /** Shutdown all services and exits the SIAM application. */
    public void exitApplication(boolean doSafemode, 
				boolean doHalt,
				boolean enableBackups,
				boolean doNotify,
				boolean recursive,
				int quitDelaySec,
				int haltDelaySec,
				String msg)
	throws RemoteException,Exception{
	_log4j.warn("exitApplication() - not implemented");
	
    }

    /** Prepare for telemetry retrieval session */
    public NodeSessionInfo startSession(boolean renewWDT, 
					byte[] initScript, 
					int scriptTimeoutSec) 
	throws Exception {
	throw new Exception("startSession() - not implemented");
    }

    static public void main(String[] args) {

	if (args.length < 3 || args.length > 4) {
	    System.err.println("usage: jarfile portname codebaseDirectory [-publish]");
	    return;
	}

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

	TestNodeService nodeService = null;

	try {
	    nodeService = new TestNodeService();
	}
	catch (RemoteException e) {
	    System.err.println(e);
	    return;
	}

	if (args.length == 4) {
	    // Look for -publish option
	    if (args[3].equals("-publish")) {
		nodeService._publish = true;
	    }
	    else {
		System.err.println("usage: jarfile portname codebaseDirectory [-publish]");
		return;
	    }
	}

	try {
	    _log4j.debug("invoke run()");
	    nodeService.run(args[0], args[1], args[2]);
	} catch (Exception e) {
	    _log4j.error("Caught exception from run():\n", e);
	}

	/* ***
	// Start rmiregistry
	try {
	    System.out.println("Starting registry... ");
	    LocateRegistry.createRegistry(1099);
	    System.out.println("registry started.");
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    System.out.println("Caught exception while starting registry: " +
			       e);
	}
	*** */

	// Bind to localhost, so bind() succeeds in absence of
	// network connection.
	String url = "rmi://localhost/testnode";
	System.out.println("binding TestNodeService to " + url);
	try {
	    Naming.rebind(url, nodeService);
	    System.out.println("TestNodeService is bound to " + url);
	}
	catch (Exception e) {
	    System.err.println("Caught exception while registering service: " +
			       e);
	}
    }
}

/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.lang.Class;
import java.lang.ThreadGroup;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteObject;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Hashtable;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.ChannelMap;
import org.mbari.siam.distributed.leasing.LeaseDescription;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.distributed.devices.Power;
import org.mbari.siam.distributed.devices.Environmental;
import org.mbari.siam.distributed.ZeroConf;
import org.mbari.siam.distributed.leasing.LeaseManager;
import org.mbari.siam.utils.ThreadUtility;
import org.mbari.siam.utils.SyncProcessRunner;
import org.mbari.siam.utils.NodeProbe;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.MOOSNodeNotifyMessage;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.Subnode;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NetworkManager;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.distributed.NodeEventCallback;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.PortConfiguration;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.PowerSwitch;
import org.mbari.siam.distributed.RemoteSerialPort;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.UnknownLocationException;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.NodeSessionInfo;
import org.mbari.siam.distributed.NodeInfo;
import org.mbari.siam.distributed.MOOSNodeInfo;
import org.mbari.siam.distributed.devices.Power;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.devices.serialadc.PowerCan;
import org.mbari.siam.registry.InstrumentRegistry;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
 * NodeService implements the Node interface, and manages multiple Devices
 * (including Instruments).
 * 
 * @author Tom O'Reilly
 */
public class NodeService extends UnicastRemoteObject 
    implements Node, PowerListener {

    // CVS revision 
    private static String _versionID = "$Revision: 1.19 $";

    private static Logger _log4j = Logger.getLogger(NodeService.class);
    static final long LEASE_DURATION = (2 * 60 * 1000);
    static final long LEASE_RENEWAL_INTERVAL = (15 * 60 * 1000);
    protected PortManager _portManager = null;

    /** Name of "parent node" - note that primary surface node's parent is
	the portal. */
    protected String _parentHost = null;
    protected InetAddress _parentAddr = null;
    protected InetAddress _surfaceAddr = null;

    protected NodeProperties _nodeProperties = null;
    protected Properties _scratchProperties = new Properties();

    // Keep track of instances created - only one allowed
    private static int _instanceCount = 0;

    LeaseManager _leaseManager = null;

    protected long _leaseDuration = LEASE_DURATION;
    protected long _leaseRenewalInterval = LEASE_RENEWAL_INTERVAL;
    SiamTimer _commsTimer = null;
    CommsSchedulerTask _commsSchedulerTask = null;

    boolean _auxCommsEnabled = false;
    boolean _advertiseService = false;

    // Need attribute for this
    boolean _publishData = true;

    protected AuxComms _auxComms = null;

    Socket _socket = null;

    CpuLeaseSleepRollcallListener _cpuLeaseManager = null;

    SubnodeListener _subnodeListener = null;

    protected SimpleDateFormat _dateFormat = 
	new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public String _wdtStatus = "Unknown WDT status";

    // Wait at most 5 minutes to send socket messages
    static final int SO_TIMEOUT = 300000;

    static final int SO_LINGER = 30;

    protected Vector _ports = null;

    protected NetworkManager _networkManager = null;

    protected NodeManager _nodeManager = null;

    protected PacketFilter[] _typeFilters = new PacketFilter[1];

    protected DevicePacketAggregator _aggregator = null;

    protected Turbinator _turbinator = null;

    //waits until RMI call to exitApplication returns before killing the app
    class ApplicationExitTask extends SiamTimerTask {
	public void run() {
	    _log4j.info("exiting application now");
	    System.exit(0);
	}
    }

    /** Create the NodeService. */
    public NodeService(PortManager portManager, String parentHost)
	throws RemoteException, MissingPropertyException,
	       InvalidPropertyException, IOException {
	super();

	// Only allowed to create one instance
	if (++_instanceCount > 1) {
	    _log4j.error("Only ONE instance of NodeService allowed!");
	    System.exit(1);
	}

	_portManager = portManager;
	_ports = _portManager.getPorts();
	_parentHost = parentHost;
	_nodeManager = NodeManager.getInstance();


	// If non-null parent, get its IP address
	if (_parentHost != null) {
	    try {
		_parentAddr = InetAddress.getByName(_parentHost);
		_log4j.debug("NodeService ctr: parentAddr=" + _parentAddr);

	    } catch (UnknownHostException e) {
		_log4j.error("Couldn't find parent host \"" + 
			     _parentHost + "\": "
			     + e.getMessage());

		throw new RemoteException("Couldn't find parent host \"" + 
					  _parentHost + "\"");
	    }

	    try {
		_surfaceAddr = InetAddress.getByName("surface");
	    }
	    catch (UnknownHostException e) {
		_log4j.info("Couldn't find address of \"surface\"");
	    }
	}

	_nodeProperties = _nodeManager.getNodeProperties();

	String advertiseService = 
	    _nodeProperties.getProperty(
					NodeProperties.ADVERTISE_SERVICE_KEY, 
					"false");

	_leaseDuration = 
	    _nodeProperties.getLongProperty(
					    "NodeService.leaseInterval", 
					    LEASE_DURATION);
	_leaseRenewalInterval = 
	    _nodeProperties.getLongProperty(
					    "NodeService.leaseRenewalInterval",
					    LEASE_RENEWAL_INTERVAL);

	_auxCommsEnabled = false;
	String value = _nodeProperties.getProperty("CommsManager.auxEnabled");
	if (value != null && value.equalsIgnoreCase("true")) {
	    _auxCommsEnabled = true;
	}

	/* Create SubnodeListener, which listens for existence message from
	   any subnodes. */
	try {
	    _log4j.debug("Creating SubnodeListener");
	    _subnodeListener = new SubnodeListener();
	    _log4j.debug("Starting SubnodeListener");
	    _subnodeListener.start();
	} catch (IOException e) {
	    _log4j.error(
			  "Caught IOException from SubnodeListener ctr", e);
	}

	/* Get lease manager, add comms listener, turn on RF power */
	if ((_leaseManager = new LeaseManager()) == null) {
	    _log4j.error("NodeService error: Can't find LeaseManager!");
	    throw new IOException("Couldn't get LeaseManager");
	}

	// create parent comms lease listener
	CommsLeaseListener listener = new CommsLeaseListener(this);
	listener.initialize();
	_leaseManager.addListener(listener);

	if (parentHost != null) {
	    try {
		InetAddress inetAddr = InetAddress.getByName(parentHost);
		_log4j.debug("Found " + parentHost + " inetAddress "
			      + inetAddr.toString());
	    } catch (UnknownHostException e) {
		_log4j.error("Couldn't find parent host \"" + parentHost
			      + "\": " + e.getMessage());
		return;
	    }

	}

	// Get MOOS network manager (to wake other nodes)
	_networkManager =
	    _nodeManager.getNodeConfigurator().createNetworkManager(InetAddress.getLocalHost().getHostName());

	_cpuLeaseManager = new CpuLeaseSleepRollcallListener();
	new CpuLease(this).start();

	_aggregator = new DevicePacketAggregator(this);

	if (advertiseService.equalsIgnoreCase("true")) {
	    _advertiseService = true;

	    String hostName = host().getHostName();
	    // No '.' allowed in zeroconf name, so remove anything after and
	    // including left-most '.' in hostname
	    String shortName = hostName;
	    int endIndex = hostName.indexOf(".");
	    if (endIndex > -1) {
		shortName = hostName.substring(0, endIndex);
	    }

	    String type = ZeroConf.SIAM_NODE_TYPE;
	    String serviceName = shortName;

	    // ZeroConf stuff goes here...
	} 

	if (_publishData) {
	    try {
		_log4j.debug("Create Turbinator");
		// Create RBNB source with an "event" channel
		_turbinator = new Turbinator("localhost");
		_log4j.debug("Turbinator created");
	    }
	    catch (Exception e) {
		_log4j.error("Turbinator constructor failed: " + e);
	    }
	}

	// Register as a listener for power failure events
	_log4j.debug("registering as PowerListener");
	EventManager.getInstance().addListener(PowerListener.class, this);
    }


    /** Start task that periodically schedules comms power */
    public void startComms() {
	// Create comms scheduler task, specify the period of the lease
	_commsTimer = new SiamTimer();
	_commsSchedulerTask = 
	    new CommsSchedulerTask(_leaseManager,
				   _leaseDuration, _leaseRenewalInterval);

	_commsTimer.schedule(_commsSchedulerTask, 0, _leaseRenewalInterval);

	if (_auxCommsEnabled) {
	    _log4j.info("startComms() - create AuxComms");
	    _auxComms = new AuxComms(this, _nodeProperties);
	}
    }

    /** Return InetAddress of node service host. */
    public InetAddress host() throws UnknownHostException {
	return InetAddress.getLocalHost();
    }

    /** Returns name of Node's class. */
    public final byte[] getName() {
	return (this.getClass().getName()).getBytes();
    }

    /** Unique identifier for Node instance */
    public long getId() {
	return _nodeManager.getId();
    }

    /** Initialize the Node. */
    public void initialize() throws InitializeException {
	// NOT YET IMPLEMENTED.
    }

		/** Return Device associated with specified comm port. */
		public Device getDevice(byte[] portName) throws PortNotFound,
		DeviceNotFound,
		RemoteException {
			
			DevicePort port = _portManager.getPortByName(new String(portName));
			
			if (port._service == null)
				throw new DeviceNotFound();
			
			try {
				return (Device )RemoteObject.toStub(port._service);
			}
			catch (Exception e) {
				throw new RemoteException(e.getMessage());
			}
		}
		
		/** Return DeviceService associated with specified comm port. 
			This method should have protected access to ensure that 
		    only local (same JVM) objects can access it.
		 */
		protected DeviceService getDeviceService(byte[] portName) throws PortNotFound,
		DeviceNotFound,
		RemoteException {
			
			DevicePort port = _portManager.getPortByName(new String(portName));
			
			if (port._service == null)
				throw new DeviceNotFound();
			
			try {
				return (DeviceService)port._service;
			}
			catch (Exception e) {
				throw new RemoteException(e.getMessage());
			}
		}
		
    /** Shutdown and remove service from specified port - 
	return human-readable message. */
    public byte[] shutdownDeviceService(byte[] portName) throws PortNotFound,
							      DeviceNotFound {
	return shutdownDeviceService(portName, false);
    }


    /** Shutdown and remove service from specified port - 
	return human-readable message; optionally put device into 
	'safe mode' before shutting down service. */
    public byte[] shutdownDeviceService(byte[] portName, boolean enterSafeMode)
	throws PortNotFound, DeviceNotFound {
	DevicePort port = _portManager.getPortByName(new String(portName));

	return _portManager.closePort(port, enterSafeMode);
    }

    /** Get array of Node's Port objects. */
    public Port[] getPorts() {
	// THIS IS A REALLY INEFFICIENT IMPLEMENTATION!!!!
	// NodeService/PortManager should probably use
	// org.mbari.isi.interfaces.Port objects directly.
	Port ports[] = new Port[_ports.size()];
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.elementAt(i);
	    if (port._service != null) {
		// There's a DeviceService running on this port.

		String portName;
		// Check for port name alias; return that if it's defined
		if ((portName = 
		     (String )_nodeProperties._portAliases.get(port._portName)) == null) {
		    portName = port._portName;
		}
		_log4j.debug("port " + i + ": name=" + portName);

		ports[i] = new Port(portName.getBytes(),
				    port._service.getId(), 
				    port._service.getName());
	    } else {
		// No service on port.
		ports[i] = new Port(port._portName.getBytes());
	    }
	}
	return ports;
    }

    /** Run diagnostic procedure; called through Parent interface. */
    /*  Base implementation does nothing.			   */
    public void runDiagnostics(String note) 
	throws Exception {
    }


    /** Get the Status Summary from the Diagnostics */
    public String getDiagnosticsStatusSummary() {
	return("No Diagnostics");
    }


    /** Get array of Node's power switches. */
    public PowerSwitch[] getPowerSwitches() {
	// Determine how many instrument power switches there are
	int nSwitches = 0;
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.elementAt(i);
	    if (port._powerPort != null)
		nSwitches++;
	}

	_log4j.debug("getPowerSwitches(): nSwitches=" + nSwitches);

	PowerSwitch switches[] = new PowerSwitch[nSwitches];

	nSwitches = 0;
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.elementAt(i);
	    if (port._powerPort != null) {
		_log4j.debug("getPowerSwitches(): found a switch");
		byte[] switchName = port._powerPort.getName().getBytes();
		long deviceID = 0;
		if (port._service != null) {
		    deviceID = port._service.getId();
		    _log4j.debug("getPowerSwitches(): assoc ID: " + deviceID);
		}

		switches[nSwitches++] = new PowerSwitch(port._powerPort
							.getName().getBytes(), deviceID);
	    }
	}
	_log4j.debug("getPowerSwitches(): returning " + switches.length
		      + " switch objects");
	return switches;
    }

    /** Scan specified port, load service. */ 
    public void scanPort(byte[] commPortName)
	throws DeviceNotFound,
	       PortOccupiedException, PortNotFound, IOException,
	       DuplicateIdException {

	scanPort(commPortName, null);
    }
    
    /** Scan specified port, load service. */
    public void scanPort(byte[] commPortName, byte[] serviceSource) 
	throws DeviceNotFound,
	       PortOccupiedException, PortNotFound, IOException,
	       DuplicateIdException {

	_log4j.info("NodeService.scanPort(" + new String(commPortName) + ")");

	DevicePort port = 
	    _portManager.getPortByName(new String(commPortName));

	String source = "";
	if (serviceSource != null) {
	    source = new String(serviceSource);
	}

	try {
	    _portManager.scanPort(port, source.trim());
	}
	catch (Exception e) {
	    throw new IOException(e.getMessage());
	}
    }

    /** Scan all ports, load services. */
    public void scanPorts() {
	_portManager.scanPorts();
    }

    /** Turn device power off to all devices. */
    public int powerOff() {
	return Device.OK;
    }

    /** Run Node's self-test routine. */
    public int test() {
	return Device.OK;
    }

    /** Return Location of Node. */
    public Location getLocation() throws UnknownLocationException {
	throw new UnknownLocationException("getLocation() not implemented");
    }

    /** Return the NetworkManager */
    public NetworkManager getNetworkManager()
    {
	return(_networkManager);
    }

    /** Get Node metadata. */
    public byte[] getMetadata() {
	return "Not implemented".getBytes();
    }

    /** Get configuration of Node ports. */
    public PortConfiguration[] getPortConfiguration() {
	int nPorts = 0;
	// Determine how many occupied ports...
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.get(i);
	    if (port._service != null) {
		nPorts++;
	    }
	}

	PortConfiguration[] configuration = new PortConfiguration[nPorts];
	int j = 0;
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.get(i);
	    if (port._service != null) {
		configuration[j] = new PortConfiguration();
		configuration[j++].set(port._service.getId(),
				       port._portName.getBytes());
	    }
	}

	return configuration;
    }

    /** Get specified device service proxy. */
    public Device getDevice(long deviceID) 
	throws DeviceNotFound, RemoteException {

	// Find specified device ID
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.get(i);

	    if (port._service != null && deviceID == port._service.getId()) {
		try {
		    return (Device )RemoteObject.toStub(port._service);
		}
		catch (Exception e) {
		    throw new RemoteException(e.getMessage());
		}
	    }
	}
	throw new DeviceNotFound(Long.toString(deviceID) + " not found");
    }

    /** Get all device service proxies. */
    public Device[] getDevices() throws RemoteException {
	int nServices = 0;
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.get(i);
	    if (port._service != null)
		nServices++;
	}

	Device[] devices = new Device[nServices];
	int j = 0;
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.get(i);
	    if (port._service != null) {
		// devices[j++] = (Device) port._service;
		try {
		    devices[j++] = (Device )RemoteObject.toStub(port._service);
		}
		catch (Exception e) {
		    throw new RemoteException(e.getMessage());
		}
	    }
	}
	return devices;
    }


    /** Get basic information about node and its subnodes */
    public NodeInfo getNodeInfo() throws Exception, RemoteException {

	return new NodeInfo(InetAddress.getLocalHost(),
			    getId(), 
			    getSubnodeObjects(), 
			    _nodeManager._nodeStartTime);
    }


    /** Just return - indicates service is alive */
    public void ping() throws RemoteException {
	return;
    }

    /** This is a worker thread class to allow 
	parallel entry/exit to/from safe mode
     */
    private class SafeWorker extends Thread {
	DevicePort _port;

	SafeWorker(ThreadGroup group, DevicePort port) { 
	    super(group, (Thread)null);
	    _port = port;
	}

	public void run() {

	    _log4j.debug("SafeWorker.run() - get device lock");

	    try {
		_log4j.debug("safeworker thread starting; id="+
			     _port._service.getId());

		shutdownDeviceService(_port._portName.getBytes(), true);
	    }
	    catch(Exception e) {
		_nodeManager.logMessage("Exception while " + 
						     "putting " + 
						     _port._portName + 
						     " into safe mode");
	    }
	}
    }

    /** Put node and its devices into "safe" mode. */
    public void enterSafeMode() throws RemoteException, Exception {
	enterSafeMode(300);
    }
    public void enterSafeMode(long timeoutSec) throws RemoteException, Exception {

	_nodeManager.logMessage("Entering safe mode");
	_log4j.debug("enterSafeMode: Entering safe mode; ports="+_ports.size());

	// create a thread group to manage SafeWorker group
	ThreadGroup safeWorkerGroup=new ThreadGroup("SafeWorkerGroup");

	// Put all Safeable devices into safe mode
	long now=System.currentTimeMillis();

	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.get(i);
	    if ( (port._service != null) && 
		 (port._service instanceof Safeable) ) {
		_log4j.debug("port "+i+" is safeable, spinning safeworker...");
		Safeable safeable = (Safeable )port._service;

		// spin worker threads...we're in a hurry!
		SafeWorker worker = new SafeWorker(safeWorkerGroup,port);

		worker.start();
		_log4j.debug("port "+i+" safeworker started");

	    }
	}

	_log4j.info("Waiting for instruments to complete safe mode transition");
	while(safeWorkerGroup.activeCount()>0 && (System.currentTimeMillis()-now)<(timeoutSec*1000L)){
	    try{
		Thread.sleep(100);
	    }catch(InterruptedException e){
		e.printStackTrace();
	    }
	}

	String msg="safe mode transition complete after "+(System.currentTimeMillis()-now)+" ms";
	_nodeManager.logNodeConfiguration(msg);
	_log4j.info(msg);

    }

    /** Return from "safe" mode; resume normal operations. */
    public void resumeNormalMode() throws RemoteException, Exception {

	// Resume normal operation of all Safeable devices
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.get(i);
	    if (port._service != null && port._service instanceof Safeable &&
		port._service.getStatus() == Device.SAFE) {
		// Restart safeable service to restore 'normal' mode
		port._service.shutdown();
		port._service.prepareToRun();
	    }
	}

	_nodeManager.logNodeConfiguration("Left safe mode");

    }


    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.
     */
    public DevicePacketSet getDevicePackets(long deviceID, long startTime,
					    long endTime) 
	throws RemoteException, DeviceNotFound, NoDataException {

	// Is requested device the node itself?
	if (getId() == deviceID) {
	    // Return packets from node log
	    return _nodeManager._log.getPackets(startTime, endTime, 100);
	}

	Device device = getDevice(deviceID);

	if (!(device instanceof Instrument))
	    throw new NoDataException("Device " + deviceID
				      + " is not an Instrument");

	Instrument instrument = (Instrument)device;

	return instrument.getPackets(startTime, endTime);
    }


    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.
     */
    public DevicePacketSet getDevicePackets(long deviceID, long startTime,
					    long endTime, int typeMask)
	throws RemoteException, DeviceNotFound, NoDataException {

	_typeFilters[0] = new PacketSubsampler(0, typeMask);
	
	// Is requested device the node itself?
	if (getId() == deviceID) {
	    // Return packets from node log
	    return _nodeManager._log.getPackets(startTime, endTime, 
						100, _typeFilters, true);
	}

	Device device = getDevice(deviceID);

	if (!(device instanceof Instrument))
	    throw new NoDataException("Device " + deviceID
				      + " is not an Instrument");

	Instrument instrument = (Instrument ) device;

	return instrument.getPackets(startTime, endTime, _typeFilters, true);
    }

    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.  Aggregate <b>numBytes</b> bytes.  Timeout after <b>timeout</b> milliseconds.
     */
    public DevicePacketSet getDevicePackets(long sensorId, long startTime, long endTime,
					       int numBytes, int timeout)
	throws RemoteException, TimeoutException, IllegalArgumentException,
	       DeviceNotFound, NoDataException
    {
	return(_aggregator.getDevicePackets(sensorId, startTime, endTime,
					    numBytes, timeout));
    }

    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.  Filter packets on <b>typeMask</b>.  Aggregate <b>numBytes</b> bytes.
     * Timeout after <b>timeout</b> milliseconds.
     */
    public DevicePacketSet getDevicePackets(long sensorId, long startTime, long endTime,
					       int numBytes, int typeMask, int timeout)
	throws RemoteException, TimeoutException, IllegalArgumentException,
	       DeviceNotFound, NoDataException
    {
	return(_aggregator.getDevicePackets(sensorId, startTime, endTime, 
					    numBytes, typeMask, timeout));
    }



    /** Suspend service (if any) associated with specified port. */
    public void suspendService(byte[] portName) throws RemoteException,
						       PortNotFound, DeviceNotFound {

	Device device = getDevice(portName);
	device.suspend();
    }

    /** Resume service (if any) associated with specified port. */
    public void resumeService(byte[] portName) 
	throws RemoteException, PortNotFound, DeviceNotFound {

	Device device = getDevice(portName);
	device.resume();
    }

    /** Restart service (if any) associated with specified port, initialize
	service and device. */
    public void restartService(byte[] portName) 
	throws RemoteException, PortNotFound, DeviceNotFound, 
	       InitializeException, Exception {

	Device device = getDevice(portName);
	// Shutdown and re-run service
	device.shutdown();
	device.prepareToRun();
	_nodeManager.logNodeConfiguration("Restarted service on port " +
						       new String(portName));

    }

    /** Return remote serial port to client. */
    public RemoteSerialPort getRemoteSerialPort(byte[] portName)
	throws RemoteException, UnknownHostException, PortNotFound,
	       PortOccupiedException, IOException {
	// If service running on specified port, suspend it
	//If service is not suspened throw exception
	DeviceService deviceService = _portManager.getPortByName(new String(
									    portName))._service;

	//make sure you have a real device
	if (deviceService == null)
	    throw new IOException("could not get a RemoteSerialPort, "
				  + "the associated Device was null");

	//if device not suspended throw exception
	if (deviceService.getStatus() != Device.SUSPEND)
	    throw new IOException("service on port " + new String(portName)
				  + " not suspended");

	return deviceService.getRemoteSerialPort();
    }

    /** Return remote serial port with a timeout milliseconds */
    public RemoteSerialPort getRemoteSerialPort(byte[] portName, int timeout)
	throws RemoteException, UnknownHostException, PortNotFound,
	       PortOccupiedException, RangeException, IOException {
	// If service running on specified port, suspend it
	//If service is not suspened throw exception
	DeviceService deviceService = _portManager.getPortByName(new String(
									    portName))._service;

	//make sure you have a real device
	if (deviceService == null)
	    throw new IOException("could not get a RemoteSerialPort, "
				  + "the associated Device was null");

	//if device not suspended throw exception
	if (deviceService.getStatus() != Device.SUSPEND)
	    throw new IOException("service on port " + new String(portName)
				  + " not suspended");

	return deviceService.getRemoteSerialPort(timeout);
    }
	
	/** This returns a ScheduleOwner for the schedule methods.
		It should retain protected access since the object backing
		the ScheduleOwner is not a remote stub
	 */
		protected ScheduleOwner getScheduleOwner(byte[] port)
		throws Exception{
			try {
				DeviceService service = getDeviceService(port);
				if(service==null){
					throw new Exception("Device is null");
				}
				
				if (! (service instanceof ScheduleOwner) ) {
					throw new Exception("Device is not a ScheduleOwner");
				}				
				ScheduleOwner owner = (ScheduleOwner)service;
				return owner;
			} catch (PortNotFound e) {
				throw new Exception("Port Not Found");
			} catch (DeviceNotFound d) {
				throw new Exception("Device Not Found");
			}
		}
		
    /** Get Printable Schedule */
    public byte[] getSchedule() throws RemoteException {
	return getSchedule(Scheduler.MAX_LOOKAHEAD_SEC);
    }

    /** Get Printable Schedule */
    public byte[] getSchedule(long lookAheadSeconds) throws RemoteException {
	Scheduler s = Scheduler.getInstance();
	String schedule = s.showSchedule(lookAheadSeconds);
	return schedule.getBytes();
    }

    /** Get Printable Schedule from one service */
    public byte[] getSchedule(byte[] port, long lookAheadSeconds)
	throws RemoteException {
	String retval = "Unknown Error";

	try {
		ScheduleOwner owner=getScheduleOwner(port);
	    Scheduler s = Scheduler.getInstance();

	    retval = s.showSchedule(owner.getScheduleKey(), lookAheadSeconds);
	} catch (PortNotFound e) {
	    _log4j.error("getSchedule caught exception " + e, e);
	    retval = "Port Not Found";
	} catch (DeviceNotFound d) {
	    _log4j.error("getSchedule caught exception " + d, d);
	    retval = "Device Not Found";
	}catch (Exception ex) {
	    _log4j.error("getSchedule caught exception " + ex, ex);
	    retval = ex.getMessage();
	}
	return retval.getBytes();
    }

    /** Add Schedule */
    public byte[] addSchedule(byte[] port, byte[] scheduleName,
			      byte[] schedule, boolean overwrite) 
		throws RemoteException {

			int i = Scheduler.UNDEFINED;
			String retval = "Unknown Error";
			try {
				ScheduleOwner owner=getScheduleOwner(port);

				i = owner.addSchedule(new String(scheduleName), 
									  new String(schedule), overwrite);
				
				if (i == Scheduler.OK) {
					i = owner.syncSchedule(new String(scheduleName), 0L);
				}
				retval = Scheduler.getStatusString(i);
			} catch (PortNotFound e) {
				_log4j.error("addSchedule caught exception " + e, e);
				retval = "Port Not Found";
			} catch (DeviceNotFound d) {
				_log4j.error("addSchedule caught exception " + d, d);
				retval = "Device Not Found";
			}catch (Exception ex) {
				_log4j.error("getSchedule caught exception " + ex, ex);
				retval = ex.getMessage();
			}
			return retval.getBytes();
		}

    /** Remove Schedule */
    public byte[] removeSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException {

	int i = Scheduler.UNDEFINED;
	String retval = "Unknown Error";
	try {
		ScheduleOwner owner=getScheduleOwner(port);

	    i = owner.removeSchedule(new String(scheduleName));
	    retval = Scheduler.getStatusString(i);
	} catch (PortNotFound e) {
	    _log4j.error("removeSchedule caught exception " + e, e);
	    retval = "Port Not Found";
	} catch (DeviceNotFound d) {
	    _log4j.error("removeSchedule caught exception " + d, d);
	    retval = "Device Not Found";
	}catch (Exception ex) {
	    _log4j.error("getSchedule caught exception " + ex, ex);
	    retval = ex.getMessage();
	}
	return retval.getBytes();
    }

    /** Suspend Schedule Entry */
    public byte[] suspendSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException {

	int i = Scheduler.UNDEFINED;
	String retval = "Unknown Error";
	try {
		ScheduleOwner owner=getScheduleOwner(port);

	    i = owner.suspendSchedule(new String(scheduleName));
	    retval = Scheduler.getStatusString(i);
	} catch (PortNotFound e) {
	    _log4j.error("suspendSchedule caught exception " + e, e);
	    retval = "Port Not Found";
	} catch (DeviceNotFound d) {
	    _log4j.error("suspendSchedule caught exception " + d, d);
	    retval = "Device Not Found";
	}catch (Exception ex) {
	    _log4j.error("getSchedule caught exception " + ex, ex);
	    retval = ex.getMessage();
	}
	return retval.getBytes();
    }

    /** Resume Schedule Entry */
    public byte[] resumeSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException {
	int i = Scheduler.UNDEFINED;
	String retval = "Unknown Error";
	try {
		ScheduleOwner owner=getScheduleOwner(port);

	    i = owner.resumeSchedule(new String(scheduleName));
	    retval = Scheduler.getStatusString(i);
	} catch (PortNotFound e) {
	    _log4j.error("resumeSchedule caught exception " + e, e);
	    retval = "Port Not Found";
	} catch (DeviceNotFound d) {
	    _log4j.error("resumeSchedule caught exception " + d, d);
	    retval = "Device Not Found";
	}catch (Exception ex) {
	    _log4j.error("getSchedule caught exception " + ex, ex);
	    retval = ex.getMessage();
	}
	return retval.getBytes();
    }

    /** Synchronize (default) Schedule */
    public byte[] syncSchedule(byte[] port, byte[] scheduleName,
			       long delayMillis) throws RemoteException {
	int i = Scheduler.UNDEFINED;
	String retval = "Unknown Error";
	try {
		ScheduleOwner owner=getScheduleOwner(port);
	    i = owner.syncSchedule(new String(scheduleName), delayMillis);
	    retval = Scheduler.getStatusString(i);
	} catch (PortNotFound e) {
	    _log4j.error("syncSchedule caught exception " + e, e);
	    retval = "Port Not Found";
	} catch (DeviceNotFound d) {
	    _log4j.error("syncSchedule caught exception " + d, d);
	    retval = "Device Not Found";
	}catch (Exception ex) {
	    _log4j.error("getSchedule caught exception " + ex, ex);
	    retval = ex.getMessage();
	}
	return retval.getBytes();
    }

    /*
     * Following is for management of communications medium between the Node
     * (deployed system) and parent (which is the shore-side portal, in the
     the case of the primary surface node) 16sept2003 Bob
     * Herlien and Tom O'Reilly
     */

    /* Local function to check for presence of _leaseManager */
    void checkLeaseManager() throws LeaseRefused {
	if (_leaseManager == null)
	    throw new LeaseRefused("Lease Manager not found");
    }


    /** Return true if parent is the surface node */
    protected boolean parentIsPrimary() {

	if (_parentAddr == null || _surfaceAddr == null) {
	    _log4j.error("Null _parentAddr or _surfaceAddr");
	    return false;
	}

	if (_parentAddr.equals(_surfaceAddr)) {
	    _log4j.debug("parentIsPrimary() - TRUE");
	    return true;
	}
	else {
	    _log4j.debug("parentIsPrimary() - FALSE");
	    return false;
	}
    }


    /** 
	Request a lease of the primary comms medium.
    */
    public int establishLease(long leaseMillisec, byte[] clientNote) 
    throws RemoteException, LeaseRefused {

	return establishLease(leaseMillisec, clientNote, true);
    }


    /**
     * Request a lease of either the primary or auxillary comms medium
     * 
     * @param leaseMillisec
     *            lease period in milliseconds
     * @return leaseID for use with renewLease(), terminateLease()
     */
    public int establishLease(long leaseMillisec, byte[] clientNote,
			      boolean usePrimary) 
	throws RemoteException,
	       LeaseRefused {

	int id = -1;

	_log4j.debug("establishLease()");

	if (usePrimary) {

	    checkLeaseManager();

	    id = _leaseManager.establish(leaseMillisec,
					 new String(clientNote));

	    // If parent is the "primary" surface node, then also establish a comms
	    // lease on surface in order to keep wireless link alive
	    if (parentIsPrimary()) {
		_log4j.debug("establishLease() - also get lease on surface node");
		SurfaceLeaseWorker worker = 
		    new SurfaceLeaseWorker(leaseMillisec, id, clientNote);

		new Thread(worker).start();
	    }
	}
	else {
	    id = 
		_auxComms.getLeaseManager().establish(leaseMillisec,
						      new String(clientNote));
	}

	return id;
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
     * 
     * @param leaseID
     *            lease ID returned by establishLease()
     * @param leaseMillisec
     *            lease period in milliseconds
     */
    public void renewLease(int leaseID, long leaseMillisec, 
			   boolean usePrimary)
	throws RemoteException, LeaseRefused {

	if (usePrimary) {
	    checkLeaseManager();
	    _leaseManager.renew(leaseID, leaseMillisec);
	}
	else {
	    _auxComms.getLeaseManager().renew(leaseID, leaseMillisec);
	}
    }

    /**
     * Terminate the session with the communications link.
     * 
     * @param leaseID
     *            lease ID returned by establishLease()
     * @param nextConnectTime
     *            Estimated time of next connection
     */
    public void terminateLease(int leaseID, long nextConnectTime)
	throws RemoteException, LeaseRefused {
	if (_leaseManager != null)
	    _leaseManager.terminate(leaseID, nextConnectTime);
    }

    /**
     * Terminate the session with the primary communications link.
     * 
     * @param leaseID
     *            lease ID returned by establishLease()
     */
    public void terminateLease(int leaseID) 
	throws RemoteException, LeaseRefused {
	terminateLease(leaseID, true);
    }


    /**
     * Terminate the session with the either the primary 
     * or auxillary communications link.
     * 
     */
    public void terminateLease(int leaseID, boolean usePrimary) 
	throws RemoteException, LeaseRefused {

	if (usePrimary) {
	    terminateLease(leaseID, LeaseManager.UNKNOWN_TIME);
	}
	else {
	    _auxComms.getLeaseManager().terminate(leaseID,
						  LeaseManager.UNKNOWN_TIME);
	}
    }



    /** Terminate all comms leases with notations that match the specified note. 
	If noteMatch is null, then terminate all leases. Returns the number
	of leases that were terminated.
    */
    public int terminateLeases(byte[] noteMatch) 
	throws RemoteException, LeaseRefused {

	String matchString = null;
	boolean terminateAll = true;

	if (noteMatch != null) {
	    matchString = new String(noteMatch);
	    terminateAll = false;
	}

	Vector commsLeases = _leaseManager.getLessees();
	int nTerminated = 0;
	_log4j.debug("terminateLeases() - there are " + commsLeases.size() + 
		     "comms leases");

	while (commsLeases.size() > 0) {

	    LeaseManager.Lessee lessee = 
		(LeaseManager.Lessee )commsLeases.elementAt(0);

	    if (terminateAll || lessee._clientNote.indexOf(matchString) >= 0) {
		_log4j.debug("terminateLeases() - try to terminate leaseID " + 
			     lessee._leaseID);		

		try {
		    _leaseManager.terminate(lessee._leaseID, LeaseManager.UNKNOWN_TIME);
		    nTerminated++;
		}
		catch (LeaseRefused e) {
		    _log4j.error("terminateLeases() - got exception trying to cancel " + 
				 lessee._leaseID);
		}
	    }
	}
	return nTerminated;
    }


    /**
     * Tell the parent that the comms link is up
     */
    public void notifyParentLinkConnected() {

	if (_parentAddr == null) //If no host (nodeTest), just return
	    return;

	try {
	    // Send signal to wakeup parent node
	    _networkManager.wakeupNode(_parentAddr);

	} catch (UnknownHostException e) {
	    _log4j.error("Couldn't find parent host \"" + _parentHost + "\": "
			  + e.getMessage());
	    return;
	}
	catch (IOException e) {
	    _log4j.error("wakeupNode() failed");
	}
	
	int portalTCPPort=Portals.portalTCPPort();
	try {
	    _log4j.debug("notifyParentLinkConnected() - open socket on " + 
			 _parentAddr.toString() + " port " + 
			 portalTCPPort);

	    _socket = new Socket(_parentAddr, portalTCPPort);
	} catch (Exception e) {
	    _log4j.error("Got " + e.getClass().getName());
	    _log4j.error("Can't connect to host \"" + _parentHost + 
			 "\" on port " + portalTCPPort + ": " +
			 e.getMessage());
	    _socket = null;
	    return;
	}

	try {
	    // Set a timeout so we don't hang forever
	    _socket.setSoTimeout(SO_TIMEOUT);

	    if (parentIsPrimary()) {
		// Write full node info to primary node
		ObjectOutputStream out = 
		    new ObjectOutputStream(_socket.getOutputStream());
		
		out.writeObject(new MOOSNodeNotifyMessage(getId(), 
						 _nodeManager._networkSwitchAddress));
	    }
	    else {
		// This is the primary node, so portal is listening; 
		// just write an arbitrary byte of ff to show we're here
		_socket.getOutputStream().write(-1);
	    }
	} catch (Exception e) {
	    _log4j.error("Error in notifyParentLinkConnected: ", e);
	}
    }

    /**
     * Tell the parent that the comms link is being torn down
     * 
     * @param nextConnectTime
     *            Time we expect to next establish the link (ms since epoch)
     */
    public void notifyParentLinkDisconnecting(long nextConnectTime) {
	if (_socket == null)
	    return;

	try {
	    DataOutputStream outstr = new DataOutputStream(_socket
							   .getOutputStream());
	    outstr.writeLong(nextConnectTime);
	    Thread.sleep(3000);
	} catch (Exception e) {
	    _log4j.error("Exception writing to parent's socket", e);
	}

	try {
	    // Whether successful or not, try to close the socket
	    _socket.close();
	} catch (Exception e) {
	    _log4j.error("Exception in socket close", e);
	}

	_socket = null;
    }

    /**
     * Request that the CPU come on at a certain time in the future and/or
     * remain on for a certain duration.
     * 
     * @param requestorID
     *            Unique ID (externally assigned) to identify who is requesting
     *            the CPU to remain on. Allows for multiple requestors each
     *            requesting that the CPU be on.
     * @param when
     *            Milliseconds until the requestor needs the CPU on. Use 0
     *            (zero) to request that the CPU remain on starting now.
     * @param howLong
     *            Number of milliseconds that the CPU should remain on. Use 0
     *            (zero) to cancel an earlier request.
     */
    public void cpuLease(int requestorID, long when, long howLong)
	throws RemoteException {
	if (_cpuLeaseManager != null) {
	    _cpuLeaseManager.cpuLease(requestorID, when, howLong);
	}
    }


    /** Add a RMI node event callback object. */
    public void addEventCallback(NodeEventCallback callback)
	throws RemoteException {
	_log4j.debug("addEventCallback()");
	_nodeManager.addEventCallback(callback);
    }


    /** Return byte string with node health/status information. */
    public byte[] getStatus(boolean logPackets) {

	String output;
	try {
	    output = _nodeManager.getStatus(logPackets);
	} catch (Exception e) {
	    output = e.toString();
	}

	return output.getBytes();
    }

    /** Reset port diagnostics. */
    public void resetPortDiagnostics() throws RemoteException {
    }

    /** Get diagnostics message from device's port and optionally log it. */
    public byte[] getPortDiagnostics(boolean logPacket) throws RemoteException {
	return "Not Implemented".getBytes();
    }

    /** Shutdown all services except those of classes indicated
	Exclusions is a array of Strings representing fully qualified
	class names or interfaces
     */
    public void shutdownServices(String[] exclusions) throws RemoteException {

	// take out a lease to get enough time to perform any application clean up
	_log4j.info("shutting down services");
	cpuLease(3, 0, 3600000);

	//try to remove all services
	for (int i = 0; i < _ports.size(); i++) {
	    DevicePort port = (DevicePort) _ports.elementAt(i);

	    boolean excludePort=false;
	    if(port._service!=null && exclusions!=null){
		for(int j=0;j<exclusions.length;j++){
		    Device device=(Device)port._service;
		    Class[] interfaces=device.getClass().getInterfaces();
		    if(exclusions!=null && device!=null){
			// is device of an excluded class?
			if(device.getClass().getName().equalsIgnoreCase(exclusions[j])){
			    excludePort=true;
			    break;
			}
			// does device implement an excluded interface?
			for(int k=0;k<interfaces.length;k++){
			    if(interfaces[k].getName().equalsIgnoreCase(exclusions[j]))
			    excludePort=true;
			    break;
			}
		    }
		}
	    }

	    //if the service is present, remove it
	    if (!excludePort && (port._service != null)) {
		_log4j.info("shutting down service on '"
			     + port._portName + "'");
		_portManager.removeService(port, false);
	    }
	}
	
    }

    /** Exits the SIAM application after specified delay. */
    public void quitApplication(int delaySec) throws RemoteException {
	SiamTimer timer;
	timer = new SiamTimer();

	// take out a lease
	_log4j.info("application shutdown initiated");
	cpuLease(3, 0, ((delaySec+3600)*1000));

	_log4j.info("SIAM application shutdown in " + delaySec + " seconds");
	timer.schedule(new ApplicationExitTask(), (delaySec * 1000));
 	
    }

    /** Notify parent; send message to parent node log */
    public void notifyParent(String message){
	try{
	    // Try to wake up the surface node (and keep it awake)
	    _networkManager.wakeupNode(_surfaceAddr);
	    _networkManager.keepNodeAwake(_surfaceAddr, 15000);
	    
	    // Get the surface NodeService
	    Node node = (Node )Naming.lookup("rmi://surface/node");
	    
	    // have the surface node log the power failure
	    node.annotate(message.getBytes());
	}catch(Exception e){
	    _log4j.error("notifyParent: Exception: unable to notify parent "+e);
	}

    }

    /** Set battery backups and holdup capacitors in local power system.
	Finds local serialADC (P2/PowerCan) service and issues binBackup 
	command
     */
    public void setBackups(int enableBatteries, int enableCapacitors){
	try{
	    // take out a lease to get enough time to perform any application clean up
	    _log4j.info("setting backups; batt:"+enableBatteries+" caps:"+enableCapacitors);
	    cpuLease(3, 0, 3600000);
	    Device[] devices=getDevices();
	    for(int i=0;i<devices.length;i++){
		if(devices[i] instanceof Power){
		    Power power=(Power)devices[i];
		    power.binBackups(enableBatteries,enableCapacitors);
		    return;
		}
	    }
	    
	} catch (Exception e) {
	    _log4j.error("setBackups caught exception " + e, e);
	}
	return;
	
    }

    /***/
    public void writePostExitScript(boolean doHalt,boolean doNotify, String msg, int haltDelaySec){
	try{
	    Date date=new Date(System.currentTimeMillis());
	    Properties p=System.getProperties();
	    String exitScript=p.getProperty("siam.exit.script");
	    if(exitScript==null){
		_log4j.error("writePostExitScript: siam.exit.script not defined");
		return;
	    }
	    File f=new File(exitScript);
	    f.createNewFile();
	    PrintWriter out
		= new PrintWriter(new BufferedWriter(new FileWriter(f)));

	    out.println("#!/bin/bash");
	    out.println("##########################################");
	    out.println("# This script is automatically generated #");
	    out.println("# Do not edit or delete.                 #");	
	    out.println("##########################################");
	    out.println("#");
	    out.println("# "+f.getName()+" generated at "+date);
	    out.println("#");
	    out.println("# sync file system");
	    out.println("sync");
	    if(doNotify){
		String dfltMsg=host().getHostName()+
		    " shutdown notification: halting system";
		out.println("# notify parent");
		out.println("logMessage surface \""+
			    (msg==null?dfltMsg:msg)+
			    "\"");
	    }
	    if(haltDelaySec>0){
		out.println("sleep "+haltDelaySec);
	    }
	    out.println("# save managed logs");
	    out.println("cp ${MANAGELOG_DIR:-\"/mnt/hda/logs\"}/*.bak $SIAM_HOME/logs/");
	    out.println("sync");
	    if(doHalt){
		out.println("# halt system");
		out.println(p.getProperty("siam.halt.command","halt.ops"));
	    }
	    // killManageLog doesn't work because it uses kill,
	    // which is a priviledged shell built-in command
	    // and killManageLog doesn't have permission to 
	    // kill the manageLog processes. kill can't be
	    // suid and there is currently no sudo 
	    //out.println("# kill manageLog processes");
	    //out.println("killManageLog");

	    out.flush();
	    out.close();
	}catch(IOException e){
	    _log4j.error("writePostExitScript: Exception - "+e);
	}

    }

    /** Shutdown all services and exits the SIAM application.
	Catch exceptions at each step and log them, but
	always try to proceed.
     */
    public void exitApplication(boolean doSafemode, 
				boolean doHalt,
				boolean enableBackups,
				boolean doNotify,
				boolean recursive,
				int quitDelaySec,
				int haltDelaySec,
				String msg)
	throws RemoteException,Exception {
	
	annotate(("exitApplication invoked: "+msg).getBytes());

	// Shutdown downstream nodes
	if(recursive){
	    _log4j.info("exitApplication: recursive shutdown not implemented...");	    
	}

	// Put this node into safe mode
	if(doSafemode){
	    long timeoutSec = 300;
	    try{
		_log4j.info("exitApplication: putting node into safe mode at "+
			    new Date(System.currentTimeMillis())+
			    " (timeout="+timeoutSec+" sec)" );
		enterSafeMode(timeoutSec);
		_log4j.info("exitApplication: safe mode transition complete at "+
			    new Date(System.currentTimeMillis()));

	    }catch(Exception e){
		_log4j.info("exitApplication: enterSafemode failed");
	    }
	}

	/*
	  Shut down the services as quickly as possible.
	  They must be shut down BEFORE disabling backups
	  (if backups are disabled) -- this requires the
	  PowerCan service to continue running.
	  Exclude PowerCan service from being shutdown now
	  if backups will need to be disabled afterwards.
	  
	  NOTE: we are assuming that the PowerCan service
	  is the only service that implements the Power
	  interface.
	*/

	String[] exclusions=null;
	if(!enableBackups){
	    try{
		exclusions=new String[1];
		exclusions[0]="org.mbari.siam.distributed.devices.Power";
	    }catch(Exception e){
		_log4j.info("exitApplication: set exclusions failed");
	    }

	}
	
	_log4j.info("exitApplication: shutting down services...");
	try{
	    shutdownServices(exclusions);
	}catch(Exception e){
	    _log4j.info("exitApplication: shutdownServices failed");
	}


	// NOW disable the backups, if requested
	// and shutdown the PowerCan service
	if(!enableBackups){
	    try{
		setBackups(PowerCan.BIN_BACKUP_DI,PowerCan.BIN_BACKUP_DI);
	    }catch(Exception e){
		_log4j.info("exitApplication: setBackups failed");
	    }
	    try{

		// shut down power service
		shutdownServices(null);
	    }catch(Exception e){
		_log4j.info("exitApplication: shutdown remaining services failed");
	    }
	}

	if(doHalt || doNotify){
	    _log4j.info("exitApplication: enabling system halt and/or notification("+haltDelaySec+")");
	    try{
		writePostExitScript(doHalt,doNotify,msg,haltDelaySec);	
	    }catch(Exception e){
		_log4j.info("exitApplication: writePostExitScript failed");
	    }
	}

	_log4j.info("exitApplication: exiting application ("+quitDelaySec+")");
	try{
	    quitApplication(quitDelaySec);
	}catch(Exception e){
	    _log4j.info("exitApplication: quitApplication failed");
	}

    }

    /** Shutdown all services and exits the SIAM application. */
    public void exitApplication() throws RemoteException {

	shutdownServices(null);
	quitApplication(5);

    }

    /**
     * power up a port and enable the comms if a service is not already using it
     */
    public void powerUpPort(byte[] commPortName, int currentLimit)
	throws PortOccupiedException, PortNotFound, RangeException,
	       NotSupportedException {

	_log4j.warn("in powerUpPort...");

	DevicePort port = _portManager.getPortByName(new String(commPortName));

	port.powerUpPort(currentLimit);
    }

    /**
     * Power down a port
     */
    public void powerDownPort(byte[] commPortName)
	throws PortOccupiedException, PortNotFound {
	DevicePort port = _portManager.getPortByName(new String(commPortName));

	port.powerDownPort();
    }

    /** Get list of immediate subnodes. */
    public InetAddress[] getSubnodes() {
	Vector subnodeVector = _subnodeListener.getSubnodes();

	int nSubnodes = subnodeVector.size();

	InetAddress addresses[] = new InetAddress[nSubnodes];

	for (int i = 0; i < nSubnodes; i++) {
	    addresses[i] = 
		((Subnode ) subnodeVector.elementAt(i)).getAddress();
	}

	return addresses;

    }

    /** Get list of immediate subnodes. */
    public Subnode[] getSubnodeObjects() {

	Vector subnodeVector = _subnodeListener.getSubnodes();

	int nSubnodes = subnodeVector.size();

	Subnode subnodes[] = new Subnode[nSubnodes];

	for (int i = 0; i < nSubnodes; i++) {
	    subnodes[i] = (Subnode ) subnodeVector.elementAt(i);
	}

	return subnodes;
    }

    public String getWdtStatus()
    {
	return(_wdtStatus);
    }

    protected void pingWDT(NodeSessionInfo sessionInfo)
    {
	sessionInfo._wdtStatus = this._wdtStatus.getBytes();
	_log4j.debug("NodeService.pingWDT()");
    }


    /** Prepare for telemetry retrieval session. This method will throw exception if getId(), getPorts() or
     getSubnodeObjects() fails. */
    public NodeSessionInfo startSession(boolean renewWDT, 
					byte[] initScript, 
					int scriptTimeoutSec) 
	throws Exception {

	NodeSessionInfo sessionInfo = new NodeSessionInfo();

	sessionInfo._nodeID = getId();
	sessionInfo._startTimeMsec = _nodeManager._nodeStartTime;

	if (renewWDT) {
	    pingWDT(sessionInfo);
	}

	try {
	    // Execute specified script
	    sessionInfo._initCommandStatus = 
		(new String(runCommand(initScript, scriptTimeoutSec))).getBytes();
	}
	catch (Exception e) {
	    sessionInfo._initCommandError = true;
	    sessionInfo._initCommandStatus = e.getMessage().getBytes();
	}

	sessionInfo._ports = getPorts();

	sessionInfo._subnodes = getSubnodeObjects();


	return sessionInfo;
	
    }


    /** Remove specified subnode from list. */
    public void removeSubnode(InetAddress address) 
	throws RemoteException, Exception {
	_subnodeListener.removeSubnode(address);
    }


    /** Get list of all active leases managed by the node */
    public LeaseDescription[] getLeases(boolean usePrimary) {

	Vector commsLeases = null;
	if (usePrimary) {
	    commsLeases = _leaseManager.getLessees();
	}
	else {
	    commsLeases = _auxComms.getLeaseManager().getLessees();
	}

	Vector cpuLeases = _cpuLeaseManager.getLessees();

	int total = commsLeases.size() + cpuLeases.size();

	_log4j.debug("total leases=" + total);

	LeaseDescription leases[] = new LeaseDescription[total];

	_log4j.debug("load commsLease descriptions");
	for (int i = 0; i < commsLeases.size(); i++) {
	    LeaseManager.Lessee lessee = 
		(LeaseManager.Lessee )commsLeases.elementAt(i);

	    /*leases[i] = new LeaseDescription(lessee); 
	    For constructor: see LeaseDescription.java */
	    leases[i] = new LeaseDescription();
	    leases[i]._type = LeaseDescription.COMMS_LEASE;
	    leases[i]._id = lessee._leaseID;
	    leases[i]._establishMsec = lessee._leaseEstablishTime;
	    leases[i]._durationMsec = lessee._leaseDurationMsec;
	    leases[i]._clientNote = lessee._clientNote.getBytes();
	    leases[i]._renewalTime = lessee._leaseRenewalTime;
	    leases[i]._renewalCount = lessee._scheduleCount-1;
	}

	_log4j.debug("load cpuLease descriptions");
	for (int i = commsLeases.size(); i < total; i++) {

	    Object element = cpuLeases.elementAt(i - commsLeases.size());

	    CpuLeaseSleepRollcallListener.CpuLessee lessee = 
		(CpuLeaseSleepRollcallListener.CpuLessee )element;

	    _log4j.debug("load cpuLease descriptions");

	    leases[i] = new LeaseDescription();
	    leases[i]._type = LeaseDescription.CPU_LEASE;
	    leases[i]._id = lessee._requestorID;
	    leases[i]._establishMsec = lessee._startTime;
	    leases[i]._durationMsec = lessee._endTime - lessee._startTime;
	    leases[i]._clientNote = "CPU lease".getBytes();
	}
		
	_log4j.debug("getLeases() - leases.size()=" + leases.length);
	return leases;
    }


    /**
     * Get Vector of node properties; each Vector element consists of
     * byte array with form "key=value".
     */
    public Vector getProperties() {

	Vector propList = new Vector();

	for (Enumeration keys = _nodeProperties.propertyNames(); 
	     keys.hasMoreElements();) {
	    String key = (String) keys.nextElement();
	    String entry = key + "=" + _nodeProperties.getProperty(key);
	    propList.addElement(entry.getBytes());
	}
	return propList;
    }



    /** Tell node to execute a Linux or shell command.
	This method doesn't return unil the command has completed,
	or until timeoutSec expires.
	Added 13jan2006, rah			*/
    public byte[] runCommand(byte[] cmd, int timeoutSec)
        throws RemoteException, IOException, TimeoutException
  {
      SyncProcessRunner procRunner = new SyncProcessRunner();
      String	strCmd = new String(cmd);
      byte[]	response;

      _log4j.info("Calling SyncProcessRunner(\"" + strCmd + "\")");

      try {
	  procRunner.exec(strCmd);
      } catch (IllegalThreadStateException e) {
	  _log4j.error("Got IllegalThreadStateException! Should never happen!"
			+ e);
      }

      try {
	  procRunner.waitFor((long)timeoutSec * 1000);
      } catch (InterruptedException e) {
	  throw new TimeoutException(e.getMessage());
      }

      response = procRunner.getOutputString().getBytes();
      _log4j.info("Command response: \n\"" + new String(response) + "\"");
      return(response);

  }

    /** Append annotation to node data stream. */
    public void annotate(byte[] annotation) throws RemoteException {
	_nodeManager.logMessage(new String(annotation));
    }

    /** Return true if specified device can supply Summary packets. */
    public boolean summarizing(long deviceID) 
	throws DeviceNotFound, RemoteException {

	// Is deviceID the node itself?
	if (deviceID == getId()) {
	    // Node is not a summarizer
	    return false;
	}

	Device device = getDevice(deviceID);

	if (!(device instanceof Instrument)) {
	    // Only instruments can summarize
	    return false;
	}

	Instrument instrument = (Instrument )device;
	if (instrument.summaryEnabled()) {
	    return true;
	}
	else {
	    return false;
	}
    }

    /** Action performed when system is shutting down
	Satisfies PowerListener interface 
    */
    public void shutdown(PowerEvent e){
	_log4j.debug("shutdown callback: "+e);
	/*
	try{
	
	}catch(RemoteException r){
	    r.printStackTrace();
	}
	catch(Exception x){
	    x.printStackTrace();
	}
	*/
	return;
    }

    /** Action performed when power failure is detected
	Satisfies PowerListener interface 
    */
    public void failureDetected(PowerEvent e){
	_log4j.debug("failureDetected callback: "+e);
	boolean doSafemode=true;
	boolean doHalt=false;
	boolean enableBackups=true;
	boolean doNotify=true;
	boolean recursive=false;
	int quitDelaySec=5;
	int haltDelaySec=0;

	    // this msg should not contain newlines, etc., since it
	    // will eventually be double quoted as an arg on the bash
	    // command line as part of the post-exit script
	    String msg="PowerFailureDetected:trigger:"+e.getReading();
	try{
	    String s=msg+
		"\ncalling exitApplication:\n"+
		"doSafemode="+doSafemode+"\n"+
		"doHalt="+doHalt+"\n"+
		"enableBackups="+enableBackups+"\n"+
		"doNotify="+doNotify+"\n"+
		"recursive="+recursive+"\n"+
		"quitDelaySec="+quitDelaySec+"\n"+
		"haltDelaySec"+haltDelaySec+"\n";
	    annotate(s.getBytes());
	    _log4j.info(s);
	}catch(Exception f){
	    // do nothing , keep going
	}
	try{
	    exitApplication(doSafemode,doHalt,enableBackups,doNotify,recursive,quitDelaySec,haltDelaySec,msg);
	}catch(RemoteException r){
	    r.printStackTrace();
	}
	catch(Exception x){
	    x.printStackTrace();
	}
    }


    /** Set value of specified node service properties. Input consists
	of one or more 'key=value' pairs. Each key=value pair is separated from
	the next pair by newline ('\n'). */
    final public void setProperties(byte[] propertyStrings)
	throws RemoteException, InvalidPropertyException {

	ByteArrayInputStream input = 
	    new ByteArrayInputStream(propertyStrings);

	_scratchProperties.clear();

	try {
	    // Add specified passed-in property settings
	    _scratchProperties.load(input);
	}
	catch (IOException e) {
	    throw new InvalidPropertyException(e.getMessage());
	}
	finally {
	    try {
		input.close();
	    }
	    catch (IOException e) {
		_log4j.error("setProperties() - IOException while closing inputstream: " + e);
	    }
	}

	String value;
	if ((value = _scratchProperties.getProperty(NodeProperties.REASSERT_COMMLINK)) != null) {
	    _nodeProperties.setProperty(NodeProperties.REASSERT_COMMLINK, value);
	}

	String propertyName = 
	    SleepManager.PROP_PREFIX + "enabled";

	if ((value = _scratchProperties.getProperty(propertyName)) != null) {

	    _nodeProperties.setProperty(propertyName, value);
	    if (Boolean.valueOf(value).booleanValue()) {
		_nodeManager._sleepManager.set(true);
	    }
	    else {
		_nodeManager._sleepManager.set(false);
	    }
	}

	// Save metadata packet with new property values here!
	StringBuffer buffer = new StringBuffer();
	buffer.append(MetadataPacket.SERVICE_ATTR_TAG);
	buffer.append(_scratchProperties.toString());
	buffer.append(MetadataPacket.SERVICE_ATTR_CLOSE_TAG);

	MetadataPacket packet = 
	    new MetadataPacket(getId(), 
			       "setProperty()".getBytes(),
			       (new String(buffer)).getBytes());
		
	packet.setSystemTime(System.currentTimeMillis());

	_nodeManager._log.appendPacket(packet, true, true);
    }



    /** Called when IP link to shore is connected */
    public void shoreLinkUpCallback(String interfaceName,
				    String serialName,
				    InetAddress localAddress,
				    InetAddress remoteAddress)
	throws RemoteException, Exception {
	// Do something here when link-to-shore is connected. 
	_log4j.info("shoreLinkUpCallback(): " + interfaceName + "  " + 
		    serialName + "  " + localAddress + "  " + 
		    remoteAddress);
    }


    /** Called when IP link to shore is disconnected */
    public void shoreLinkDownCallback(String interfaceName,
				      String serialName,
				      InetAddress localAddress, 
				      InetAddress remoteAddress)
	throws RemoteException, Exception {

	_log4j.info("shoreLinkDownCallback(): " + interfaceName + "  " + 
		    serialName + "  " + localAddress + "  " + 
		    remoteAddress);

	// Terminate comms leases on all subnodes 
	Subnode[] subnodes = getSubnodeObjects();
	_log4j.debug("shoreLinkDownCallback() - cancel leases on " + 
		     subnodes.length + " subnodes");

	for (int i = 0; i < subnodes.length; i++) {

	    // Check for connection to subnode
	    NodeProbe nodeProbe = new NodeProbe();
	    boolean subnodeConnected = false;
	    InetAddress address = subnodes[i].getAddress();

	    try {
		subnodeConnected = 
		    nodeProbe.probe(address, Portals.nodeProbePort(), 5000);
	    }
	    catch (Exception e) {
		_log4j.error("shoreLinkDownCallback() - nodeprobe got exception from " + 
			     address + ": " + e);
	    }
	    if (!subnodeConnected) {
		// No connection - skip this one
		_log4j.debug("shoreLinkDownCallback() - subnode " + address + " not connected?");
		continue;
	    }

	    // Get subnode proxy
	    _log4j.debug("shoreLinkDownCallback() - get proxy for subnode " + address);
	    Node node = 
		(MOOSNode )Naming.lookup(Portals.mooringURL(address.getHostName()));

	    _log4j.debug("shoreLinkDownCallback() - cancel leases on subnode " + address);

	    // Terminate all comms leases on the subnode
	    try {
		int n = node.terminateLeases(null);
		_log4j.debug("shoreLinkDownCallback() - terminated " + n + 
			     " leases on " + address);
	    }
	    catch (Exception e) {
		_log4j.error("shoreLinkDownCallback() - caught exception trying " +
			     "to terminate leases on " + address);
	    }
	}

	// Check for active comms leases
	Vector commsLeases = _leaseManager.getLessees();

	// Number of active leases at time of disconnect.
	int nLeases = commsLeases.size();

	if (nLeases > 0) {
	    // Need to properly shut-down communications device. Device may already have
	    // been shut-down, if shore-link disconnect was due to all leases expiring.
	    // However, if there are still active leases, then disconnect was due to a problem 
	    // (e.g. loss of satellite signal), and device is still powered. Therefore, we
	    // terminate all active leases, which will cause device to be powered off.
	    _log4j.debug("shoreLinkDownCallback() - found " + commsLeases.size() + " comms leases");

	    // Terminate all the leases; note that this lease vector is modified by LeaseManager as leases
	    // are terminated. 
	    _log4j.info("shoreLinkDownCallback() - terminate leases:");

	    if (_nodeProperties.reassertCommsLink()) {
		// Before terminating leases, get a cpu lease so we stay
		// awake afterwards, and can immediately reassert a new
		// comms lease
		try {
		    cpuLease(3, 0, 60000);
		}
		catch (Exception e) {
		    _log4j.error("shoreLinkDownCallback(): " + e);
		}
	    }

	    while (commsLeases.size() > 0) {

		LeaseManager.Lessee lessee = 
		    (LeaseManager.Lessee )commsLeases.elementAt(0);

		try {
		    _log4j.debug("shoreLinkDownCallback() - terminate leaseID " + lessee._leaseID);

		    _leaseManager.terminate(lessee._leaseID, LeaseManager.UNKNOWN_TIME);
		}
		catch (Exception e) {
		    _log4j.error("Got exception trying to cancel leaseID " + lessee._leaseID + ": " + e);
		}
	    }

	
	    // If there were active leases at time link disconnected, and 
	    // reassert of comms link is enabled, then re-assert connection by
	    // establishing a new lease.
	    if (_nodeProperties.reassertCommsLink()) {

		// Request a lease - since lessee count is zero, modem will be powered on
		_log4j.debug("shoreLinkDownCallback() - request a new lease");
		long id = _leaseManager.establish(_leaseDuration, "re-established connection");
		_log4j.debug("shoreLinkDownCallback() - got new lease " + id);
	    }
	    else {
		_log4j.debug("shoreLinkDownCallback() - reassert comms link DISABLED");
	    }
	}
	else {
	    _log4j.debug("shoreLinkDownCallback() - no leases active");
	}
    }


    /* Added 3 Nov 2008, rah, to interface to InstrumentRegistry */

		/** Find an Instrument in the InstrumentRegistry by registryName */
		public Instrument lookupService(String registryName) throws RemoteException
		{
			DeviceService service = InstrumentRegistry.getInstance().find(registryName);
			
			// return((service instanceof Instrument) ? (Instrument)service : null);
			if (service instanceof Instrument) {
				try {
					return (Instrument )RemoteObject.toStub(service);
				}
				catch (Exception e) {
					throw new RemoteException(e.getMessage());
				}
			}
			else {
				return null;
			}
		}

    /** Get InstrumentRegistry status */
    public byte[] instrumentRegistryStatus() throws RemoteException
    {
	return(InstrumentRegistry.getInstance().registryStatus().getBytes());
    }

	/** Reload log4j configuration. 
		Enables logging configuration to be changed at run time.
	 */	
	public void readLog4jConfig() throws RemoteException{
		/* Read the log4j configuration file for SIAM.
		The path comes from the system properties 
		 */		
		Properties p=System.getProperties();
		String dflt_log4j=p.getProperty("siam_home","/mnt/hda/siam")+"/properties/siam.log4j";
		String siam_log4j=p.getProperty("siam.log4j",dflt_log4j);
		PropertyConfigurator.configure(siam_log4j);
		
		/* Pattern layout is typically defined in the log4j configuration file
		 PatternLayout layout = 
		 new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		 
		 BasicConfigurator.configure(new ConsoleAppender(layout));
		 */
		return;
	}

    /** When a lease ("primary lease") is established on this node, 
	SurfaceLeaseWorker tries to establish a "secondary" lease with the 
	surface node, where the wireless link-to-shore is located.
    */
    class SurfaceLeaseWorker implements Runnable {

	long _leasePeriodMsec;
	int _primaryLeaseID;
	String _primaryNote;

	/** Create SurfaceLeaseWorker to establish a lease on the surface,
	 in response to a "primary" lease established on this node. */
	public SurfaceLeaseWorker(long leasePeriodMsec, 
				  int primaryLeaseID, byte[] primaryNote) {

	    _leasePeriodMsec = leasePeriodMsec;
	    _primaryLeaseID = primaryLeaseID;
	    _primaryNote = new String(primaryNote, 0, primaryNote.length);
	}

	public void run() {

	    try {
		// Try to wake up the surface node
		_networkManager.wakeupNode(_surfaceAddr);

		Node node = (Node )Naming.lookup("rmi://surface/node");

		String note = "From " + InetAddress.getLocalHost() + 
		    ", response to primary lease " + _primaryLeaseID + 
		    "; " + _primaryNote;

		// Try to establish lease.
		int leaseId = node.establishLease(_leasePeriodMsec,
						  note.getBytes());
	    }
	    catch (Exception e) {
		_log4j.error("SurfaceLeaseWorker.run(): " + e);
	    }
	}
    }


    /** Creates and writes data to RBNB source */
    static class Turbinator {

	ChannelMap _dtChannelMap;
	Source _dtSource;
	public static final String EVENT_CHANNEL_NAME = "events";
	int _eventChannel = -1;

	Turbinator(String dtHostName) throws Exception {

	    // Create the DataTurbine "source" (we'll write to this)
	    int cacheSize = 20;
	    int archiveSize= 10000;
	    _dtSource = new Source(cacheSize, "none", archiveSize);
	    _dtSource.OpenRBNBConnection(dtHostName, "SIAM-node");
	    _dtChannelMap = new ChannelMap();

	    _eventChannel = _dtChannelMap.Add(EVENT_CHANNEL_NAME);
	    _dtChannelMap.PutUserInfo(_eventChannel, "test");
	    _dtChannelMap.PutMime(_eventChannel, "text/plain");
	    _dtSource.Register(_dtChannelMap);
	    _dtSource.Flush(_dtChannelMap, false);
	}

	/** Write an event */
	void writeEvent(String eventMessage) throws Exception {

	    _dtChannelMap.PutDataAsString(_eventChannel, eventMessage); 
	    _dtSource.Flush(_dtChannelMap, false);
	}

	/** Close the source */
	void close() {
	    _dtSource.CloseRBNBConnection();
	}
    }
}



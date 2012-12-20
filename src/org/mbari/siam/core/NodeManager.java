/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.EventObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.InetAddress;
import java.rmi.Naming;
import org.mbari.siam.distributed.devices.GPS;
import org.mbari.siam.distributed.devices.NetworkSwitch;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.FilteredDeviceLogIF;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NodeEventCallback;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.distributed.NodeConfigurator;
import org.mbari.siam.distributed.NodePacketTypes;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.moos.deployed.MOOSNodeConfigurator;

/**
NodeManager implements node functionality, and instantiates component 
objects including NodeService, PortManager, etc.
*/
public class NodeManager implements Parent, ServiceListener {

    private static String _releaseName = new String("$Name: HEAD $");
    static private Logger _log4j = Logger.getLogger(NodeManager.class);

    protected final static String RFIO_STATUS_CMD = 
	"echo RFIO status goes here";

    protected final static String GLOBALSTAR_STATUS_CMD = 
	"globalstarStat";

    protected final static String GET_MAC_ADDRESS_CMD = "ethaddr";

    protected final static String PPP_STATUS_CMD = "sh plog -100";

    protected final static String OPTICAL_STATUS_CMD = 
	"echo Optical status goes here";

    protected final static String DISK_SPACE_USAGE_CMD = "df";

    /** SensorDataPackets to hold status data. */
    SensorDataPacket _statusPacket;

    private SimpleDateFormat _dateFormat = 
	new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    protected long _nodeID = -1L;
    protected NodeConfigurator _nodeConfigurator = null;
    protected PortManager _portManager = null;
    protected NodeService _nodeService = null;
    protected SleepManager _sleepManager = null;
    protected WDTManager _wdtManager = null;
    protected static NodeManager _theNodeManager = null;
    protected NodeProperties _nodeProperties = null;
    protected String _siamHome = null;
    protected FilteredDeviceLogIF _log = null;
    protected Vector _eventCallbacks = new Vector();
    protected InetAddress _networkSwitchAddress = null;
    protected long _nodeStartTime = 0;

    protected String _cfgFile;

    // Buffer for status reports 
    private byte[] _reportBuf = new byte[10240];

    // Objects used for logging messages to Node log
    private DeviceMessagePacket _messagePacket = null;

    /** Timer and TimerTask to enable periodic sampling of node status. */
    SiamTimer _nodeStatusTimer;
    SiamTimerTask _nodeStatusSampler;

    /** Method to get singleton instance. */
    public synchronized static NodeManager getInstance(NodeConfigurator configurator)
    {
	if (_theNodeManager == null) {
	    try {
		_theNodeManager = new NodeManager(configurator);
	    }
	    catch (IOException e) {
		_log4j.error("IOException: " + e.getMessage());
		// Absolutely fatal error
		System.exit(1);
	    }
	    catch (MissingPropertyException e) {
		_log4j.error("MissingPropertyException: " + 
				   e.getMessage());
		// Absolutely fatal error
		System.exit(1);
	    }
	    catch (InvalidPropertyException e) {
		_log4j.error("InvalidPropertyException: " + 
				   e.getMessage());
		// Absolutely fatal error
		System.exit(1);
	    }
	    catch (RangeException e) {
		_log4j.error("RangeException: " + 
				   e.getMessage());
		// Absolutely fatal error
		System.exit(1);
	    }
	}
	return _theNodeManager;
    }

    /** Method to get singleton instance. 			*/
    /*  The default version configures the node for MOOS/MMC	*/
    public synchronized static NodeManager getInstance()
    {	
	if (_theNodeManager == null)
	    return(getInstance(new MOOSNodeConfigurator()));
	else
	    return(_theNodeManager);
    }


    /** Create NodeManager object. */
    NodeManager(NodeConfigurator configurator) 
    throws IOException, MissingPropertyException, 
	   InvalidPropertyException, FileNotFoundException, RangeException {


	_log4j.info("Starting NodeManager: " + _releaseName);
	_log4j.info("Node IP: " + InetAddress.getLocalHost().toString());

	_nodeConfigurator = configurator;

        // Create node properties object and load 'em in
	_cfgFile = System.getProperty("siamPortCfg",
					    "/properties/siamPort.cfg");

        _nodeProperties = _nodeConfigurator.createNodeProperties(_cfgFile);

	// Set the Node ID
	_nodeID = _nodeProperties.getNodeID();
	_log4j.info("Node ID has been set to "+_nodeID);

	// Create node log
	_log4j.debug("create node log");
	_log = _nodeConfigurator.createDeviceLog(_nodeID, _nodeProperties);

	// Create a log message packet; we always use this instance for
	// writing log entries...
	_messagePacket = new DeviceMessagePacket(getId());

	// Register to hear about events on the node
	EventManager.getInstance().addListener(ServiceListener.class, this);

	// Create packets to hold node diagnostic information
	_statusPacket = new SensorDataPacket(getId(), 0);
    }


    /** Start the NodeManager. */
    public void start(String name, String portalHost) 
	throws IOException, 
	       InvalidPropertyException, 
	       MissingPropertyException {

	_nodeStartTime = System.currentTimeMillis();

	try {
	
	    _log4j.debug("new PortManager()");
	    _portManager = _nodeConfigurator.createPortManager(_nodeProperties);

	    //intialize port vector from cfg info stored in siamPort.cfg
	    try {
		_portManager.initPortVector();
	    }
	    catch ( IOException ioe ) {
		_log4j.error("initPortVector threw IOException", ioe);
	    }

	    _log4j.debug("WDTManager.getInstance()");
	    _wdtManager = _nodeConfigurator.createWDTManager();		
	    
      _log4j.debug("SleepManager.getInstance()");
	    _sleepManager = _nodeConfigurator.createSleepManager();

	    _log4j.debug("new NodeService()");
	    _nodeService = _nodeConfigurator.createNodeService(_portManager, portalHost);

	    // Start rmiregistry
	    try {
		_log4j.info("Starting registry... ");
		LocateRegistry.createRegistry(1099);
		_log4j.info("registry started.");
	    }
	    catch (RemoteException e) {
		// Already running on port 1099?
		_log4j.info(e.getMessage());
	    }

	    // Bind to localhost, so bind() succeeds in absence of
	    // network connection.
	    String url = "rmi://localhost/" + name;
	    _log4j.info("binding NodeService to " + url);
	    Naming.rebind(url, _nodeService);
	    _log4j.info("NodeService is bound to " + url);
	    

	}
	catch (RemoteException e) {
	    _log4j.error("NodeManager: Caught RemoteException:", e);
	    throw new IOException(e.getMessage());
	}
	catch (Exception e) {
	    _log4j.error("NodeManager: Caught Exception:" + e);
	    e.printStackTrace();
	    throw new IOException(e.getMessage());
	}

	// Start the watchdog timer manager
	if (_wdtManager != null) {
	   _wdtManager.start();
  }
  
	// Get a scheduler instance
	// (creates Scheduler singleton instance)
	Scheduler s = Scheduler.getInstance();

	// load default schedules
	s.loadDefaults();

        // Load services on ports
	_portManager.scanPorts();

	// Start up the SleepManager
	if (_sleepManager != null) {
	    _sleepManager.start();
	}
	

  
	// Now that service is bound and available, 
	// start the comms and scheduling
	_nodeService.startComms();

	try {
	    // Get diagnostic sampling interval, convert to millisec
	    int interval = _nodeProperties.getDiagnosticInterval() * 1000;
	    _log4j.debug("Diagnostic interval = " + interval);
	    _nodeStatusTimer = new SiamTimer();
	    _nodeStatusSampler = new NodeStatusSampler();
	
	    _nodeStatusTimer.schedule(_nodeStatusSampler, 0, 
				      interval);
	}
	catch (InvalidPropertyException e) {
	    _log4j.error(e.getMessage());
	} catch (IllegalArgumentException e) {
	    _log4j.error(e.getMessage());
	}

	// Now we can log messages; note start of application
	logMessage("SIAM release " + _releaseName + ": NodeManager started");

	// Log node property values
	StringBuffer buffer = new StringBuffer(4096);
	buffer.append(MetadataPacket.SERVICE_ATTR_TAG);

	// Put actual contents of node configuration properties file to metadata packet
	try {
	    BufferedReader reader = 
		new BufferedReader(new InputStreamReader(new FileInputStream(getSiamHome() + _cfgFile)));

	    String line = null;
	    while ((line = reader.readLine()) != null) {
		buffer.append(line);
		buffer.append("\n");
	    }
	    reader.close();
	}
	catch (Exception e) {
	    buffer.append(e.getMessage());
	}

	buffer.append(MetadataPacket.SERVICE_ATTR_CLOSE_TAG);

	buffer.append(MetadataPacket.DEVICE_INFO_TAG);
	buffer.append("\nipAddress=" + InetAddress.getLocalHost().toString() +
		      "\n");

	// Get mac address
	String macAddress = null;

	try {
	    Process process = 
		Runtime.getRuntime().exec(GET_MAC_ADDRESS_CMD);

	    int nBytes = process.getInputStream().read(_reportBuf);
	    macAddress = new String(_reportBuf, 0, nBytes);
	}
	catch (Exception e) {
	    macAddress = "UNKNOWN";
	}

	buffer.append("macAddress=" + macAddress + "\n");
	buffer.append(MetadataPacket.DEVICE_INFO_CLOSE_TAG);

	MetadataPacket packet = 
	    new MetadataPacket(getId(), 
			       "initialization".getBytes(),
			       (new String(buffer)).getBytes());

	logPacket(packet);
	_log4j.info("NodeManager \"" + name + "\" started.");
    }


    /** Return ISI ID of node. */
    protected long getId() {
	return _nodeID;
    }

    /** Return node properties. */
    protected NodeProperties getNodeProperties() {
	return _nodeProperties;
    }

    /** Return SIAM home directory. */
    protected String getSiamHome() 
	throws MissingPropertyException {

        Properties sysProperties = System.getProperties();
        String home = sysProperties.getProperty("siam_home");
	if (home == null) {
	    throw new MissingPropertyException("siam_home");
	}
	return home.trim();
    }


    /** Write a DevicePacket to the Node log, and publish the LogSampleServiceEvent*/
    protected void logPacket(DevicePacket packet)
    {
	packet.setSystemTime(System.currentTimeMillis());
	_log.appendPacket(packet, true, true);

	publish(new LogSampleServiceEvent(this, LogSampleServiceEvent.SAMPLE_LOGGED,
					  (int)getId(), packet));
    }



    /** Write a message packet to the Node log. */
    public synchronized void logMessage(String message) {
	// Set message packet content
	_messagePacket.setMessage(System.currentTimeMillis(),
				  message.getBytes());

	// Write message packet to platform log
	logPacket(_messagePacket);
    }



    /** Return PortManager singleton. */
    public PortManager getPortManager() {
	return _portManager;
    }

    /** Return NodeService singleton. */
    public NodeService getNodeService() {
	return _nodeService;
    }


    /** Return NodeConfigurator. */
    public NodeConfigurator getNodeConfigurator() {
	return _nodeConfigurator;
    }


    /** Print device service status for each port. */
    String portServicesStatus(String prefix) {
	String message = prefix + "\n";

	message += ("Node started at: " + 
	    _dateFormat.format(new Date(_nodeStartTime)) + "\n");

	Vector ports = _portManager.getPorts();
	for (int i=0; i < ports.size(); i++) {
	    DevicePort port = (DevicePort)ports.elementAt(i);
	    // If port has service, document service's device ID
	    if (port._service != null) {
		message += "port=" + port._portName + "|" + 
		    "id=" + port._service.getId() + "|" + 
		    "name=" + new String(port._service.getName()) + "|" + 
		    "samples=" + port._service.getSamplingCount() + "|" +
		    "err=" + port._service.getSamplingErrorCount() + "|" + 
		    "retries=" + port._service.getSamplingRetryCount() + "|" +
		    "status=" + port._service.getStatus() + 
		    "\n";
	    }
	}
	return message;
    }

    /** Log the current instrument list, etc. */
    protected void logNodeConfiguration(String prefix) {

	logMessage(portServicesStatus(prefix));
    }

    /** Add a remote callback to the to-be-notified list. */
    public void addEventCallback(NodeEventCallback callback) {
	_log4j.debug("addEventCallback()");
	_eventCallbacks.add(callback);
    }


    public void serviceInstalled(ServiceEvent event) {

	_log4j.info("serviceInstalled(): invoke remote callbacks");

	// Look for specific service installations (e.g. Medusa)
	Vector ports = _portManager.getPorts();
	for (int i = 0; i < ports.size(); i++) {
	    DevicePort port = (DevicePort)ports.get(i);

	    if (port._service != null && 
		event._serviceID == port._service.getId()) {

		if (port._service instanceof NetworkSwitch) {
		    // Get IP of network switch 
		    try {
			_networkSwitchAddress = ((NetworkSwitch )(port._service)).getCpuAddress();
			_log4j.debug("NetworkSwitch address: " + _networkSwitchAddress);
		    }
		    catch (Exception e) {
			_log4j.error("serviceInstalled() - got exception from NetworkSwitch.getCpuAddress(): " +
				     e);
		    }
		}

		break;
	    }	    
	}


	_log4j.debug("serviceInstalled(): invoke remote callbacks");

	// Notify remote callbacks
	for (int i = 0; i < _eventCallbacks.size(); i++) {
	    NodeEventCallback callback = 
		(NodeEventCallback )_eventCallbacks.elementAt(i);

	    try {
		callback.serviceStarted(event._serviceID);
	    }
	    catch (RemoteException e) {
		_log4j.info("Got RemoteException from eventCallback " + i);
		_eventCallbacks.remove(i);
	    }
	}
    }

    public void serviceRemoved(ServiceEvent event) {

	_log4j.info("serviceRemoved(): invoke remote callbacks");

	// Notify remote callbacks
	Iterator iterator = _eventCallbacks.iterator();
	int i = 0;
	while (iterator.hasNext()) {
	    NodeEventCallback callback = (NodeEventCallback )iterator.next();
	    _log4j.info("Invoking callback #" + i);
	    try {
		callback.serviceTerminated(event._serviceID);
	    }
	    catch (RemoteException e) {
		_log4j.info("Got RemoteException from eventCallback");
		iterator.remove();
	    }
	    _log4j.info("Done with callback #" + i);
	    i++;
	}
	_log4j.info("Done with serviceRemoved()");
    }

    public void serviceRequestComplete(ServiceEvent e) {
    }

    public void serviceSampleLogged(LogSampleServiceEvent e) {
    }


    /** NodeStatusSampler reads and logs node status items at a specified
	interval. */
    class NodeStatusSampler extends SiamTimerTask {

	/** Read and log node status items */
	public void run() {
	    _nodeService.getStatus(true);

	    // Reset the sleep manager log
	    if (_sleepManager != null)
		_sleepManager._log.reset();
	}
    }

    /** Return summary of sleep manager operations. */
    public String getSleepSummary()
    {
	if (_sleepManager != null)
	{
	    if (!_sleepManager.enabled()) {
		return "sleep mgr disabled";
	    }

	    return _sleepManager._log.toString();
	}

	return("No sleep mgr");
    }


    ///////////////////////////
    // Parent Interface Methods
    ///////////////////////////


    /** Return the ISI ID of this node.
	In fulfullment of the Parent Interface.
     */
    public long getParentId() {
	return getId();
    }

    /** Return the location of the requested ISI ID.
	In fulfillment of the Parent Interface.
     */
    public Location getLocation(long deviceID) {
	return new Location("Location not implemented");
    }

    /** Run diagnostic procedure; called through Parent interface */
    public void runDiagnostics(String note) 
	throws Exception {

	// Run environmental diagnostics method
	_nodeService.runDiagnostics(note);
    }

    /** Return software version information. */
    public String getSoftwareVersion() {
	return _releaseName;
    }

    /** Return Node start time */
    public long getNodeStartTime()
    {
	return(_nodeStartTime);
    }

    /** Return Network Switch Inet Address */
    public InetAddress getNetworkSwitchAddress()
    {
	return(_networkSwitchAddress);
    }

    /** Publish the specified event. Event published must be an instance of
	NodeEvent. */
    public void publish(EventObject event) {
	_log4j.debug("NodeManager got Event: " + event.toString());

	if (event instanceof NodeEvent) {
	    EventManager.getInstance().postEvent((NodeEvent )event);
	}
	else {
	    _log4j.error("publish() - event is not a NodeEvent");
	}
    }


    /** Get status of various node subsystems, and return string 
	representation; log status packets if 'logPackets' is true. */
    protected String getStatus(boolean logPackets) throws RemoteException {

	StringBuffer output = new StringBuffer(1024);

	String data = portServicesStatus("");
	output.append(data);

	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.PORT_SUMMARY);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	// Get status summary of each instrument port; look for GPS
	Device[] devices = _nodeService.getDevices();
	int gpsIndex = -1;

	StringBuffer buffer = new StringBuffer();

	for (int i = 0; i < devices.length; i++) {

	    if (!(devices[i] instanceof Instrument)) {
		// Only instruments currently offer summary information!
		continue;
	    }

	    Instrument instrument = (Instrument) devices[i];

	    try {
		data = new String(instrument.getCommPortName()) + "\n" + 
		    new String(instrument.getPortDiagnosticsSummary(false));

		if (data.indexOf(NullPowerPort.TYPE_NAME) >= 0) {
		    // Don't log NULL power port data
		    continue;
		}

		buffer.append("\n");
		buffer.append(data);

		// Reset, prepare for next sampling cycle
		instrument.resetPortDiagnostics();

		// Check for GPS
		if (instrument instanceof GPS) {
		    gpsIndex = i;
		}

	    } catch (RemoteException e) {
		_log4j.error("RemoteException: ", e);
	    }
	}

	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.POWER_PORT);
	    _statusPacket.setDataBuffer((new String(buffer)).getBytes());
	    logPacket(_statusPacket);
	}

	output.append(new String(buffer));

	// Found a GPS on the node?
	if (gpsIndex >= 0) {

	    GPS gps = (GPS )devices[gpsIndex];
	    try {
		data = new String(gps.getLatestNMEA()) + "\n";
	    }
	    catch (Exception e) {
		data = "GPS: SERVICE NOT AVAILABLE\n";
	    }

	    output.append("\n");
	    output.append(data);

	    if (logPackets) {
		_statusPacket.setRecordType(NodePacketTypes.GPS);
		_statusPacket.setDataBuffer(data.getBytes());
		logPacket(_statusPacket);
	    }
	}


	// Get environmental summary
	data = _nodeService.getDiagnosticsStatusSummary();
	output.append("\n");
	output.append(data);

	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.ENVIRONMENTAL);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	int nBytes = 0;
	Process process = null;

	// Get file system status
	try {
	    process = 
		Runtime.getRuntime().exec(DISK_SPACE_USAGE_CMD);

	    nBytes = process.getInputStream().read(_reportBuf);
	    data = new String(_reportBuf, 0, nBytes);
	}
	catch (IOException e) {
	    data = "Caught IOException from \"" + 
		DISK_SPACE_USAGE_CMD + "\" command: " + e.getMessage();
	}
	catch (SecurityException e) {
	    data = "Caught SecurityException from \"" + 
		DISK_SPACE_USAGE_CMD + "\" command: " + e.getMessage();
	}

	output.append("\n");
	output.append(data);

	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.FILESYSTEM);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	
	// Get Java thread count
	ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
	data = "#jvmthreads: " + threadGroup.activeCount() + "\n";

	data += "\n" +  
	    "jvmmem: total=" + Runtime.getRuntime().totalMemory() +
	    ", free=" + Runtime.getRuntime().freeMemory() + " (bytes)\n";

	output.append("\n");
	output.append(data);

	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.JVM);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}


	// Get RFIO status
	try {
	    process = 
		Runtime.getRuntime().exec(RFIO_STATUS_CMD);

	    nBytes = process.getInputStream().read(_reportBuf);

	    data = new String(_reportBuf, 0, nBytes);

	    if (data == null || data.length() == 0) {
		data = "RF status not available";
	    }
	}
	catch (Exception e) {
	    data = "RF status unavailable";
	}

	output.append("\n");
	output.append(data);
	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.RFIO);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	// Get optical status
	try {
	    process = 
		Runtime.getRuntime().exec(OPTICAL_STATUS_CMD);

	    nBytes = process.getInputStream().read(_reportBuf);
	    data = new String(_reportBuf, 0, nBytes);
	}
	catch (Exception e) {
	    data = "Optical status not available";
	}

	output.append("\n");
	output.append(data);
	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.OPTICAL);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	// Get Globalstar status
	try {
	    process = 
		Runtime.getRuntime().exec(GLOBALSTAR_STATUS_CMD);

	    nBytes = process.getInputStream().read(_reportBuf);
	    data = new String(_reportBuf, 0, nBytes);
	}
	catch (Exception e) {
	    data = "GlobalStar status not available";
	}

	output.append("\n");
	output.append(data);
	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.GLOBALSTAR);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	// Latest watchdog timer renewal...
	data = _nodeService._wdtStatus;
	output.append("\n");
	output.append(data);
	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.WATCHDOG);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	// Get sleep manager information 
	data = NodeManager.getInstance().getSleepSummary();

	output.append("\n");
	output.append(data);
	if (logPackets) {
	    _statusPacket.setRecordType(NodePacketTypes.SLEEPLOG);
	    _statusPacket.setDataBuffer(data.getBytes());
	    logPacket(_statusPacket);
	}

	return new String(output);
    }

    /** Request power from the parent; return true if available, false if
	not available. */
    public boolean powerAvailable(int milliamp) {
		// demoted this to debug...
		// this is taking up way too much space in the logs
		// and doesn't do anything, since it always returns true
	_log4j.debug("powerAvailable() not implemented");
	return true;
    }
}

// Copyright 2002 MBARI
package org.mbari.siam.operations.portal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Runtime;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseDescription;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.distributed.portal.UnknownConfiguration;
import org.mbari.siam.operations.utils.ExportablePacket;
import moos.ssds.jms.PublisherComponent;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketOutputStream;
import org.mbari.siam.distributed.DevicePacketServerThread;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.distributed.Port;

import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Subnode;
import org.mbari.siam.distributed.NodeSessionInfo;

import org.mbari.siam.operations.portal.Profiler;
import org.mbari.siam.utils.NodeProbe;
import org.mbari.siam.utils.ThreadUtility; // *** DEBUG

/**
 * PortalConnection provides the interface and maintains state for one
 * connection to a SIAM node from which data is retrieved (aka "target" 
 * node").
 * <p>
 * When Portal determines that the communications link to the target node is 
 * available, it calls attempts to "wake up" the node (which may be sleeping),
 * and invokes PortalConnection.nodeLinkConnected() to retrieve data 
 * from the node's sensors. The retrieved packets are then distributed to 
 * shore clients, as well as saved in a file buffer. PortalConnection keeps 
 * track of which packets it has retrieved from each sensor; on subsequent 
 * retrievals it requests the "latest" data from the sensor.
 * <p>
 * PortalConnection stores client requests ("commands") until a communication
 * link to the node is available, at which time PortalConnection sequentially
 * forwards the requests to the node. Thus PortalConnection manages network
 * traffic over the low-bandwidth link.
 * <p>
 * While interacting with a node on the MOOS network, the shore need only
 * maintain a lease on the node from which data is being retrieved; 
 * the target node will ensure that any intermediate nodes/comms links are
 * kept awake.
 *
 * @author Bob Herlien - adapted from Portal.java
 */
public class PortalConnection {
    static private Logger _log4j = Logger.getLogger(PortalConnection.class);

    // CVS revision 
    private static String _versionID = "$Revision: 1.5 $";

    Vector _availableTargets = new Vector();
	
    String _targetNodeName;

    /** Address of the remote node. */
    InetAddress _targetNodeAddr;

    /** Proxy for NodeService on target node */
    MOOSNode _targetNode = null;

    /** Array of subnode InetAddresses */
    Subnode[] _subnodes = null;

    /** Indicates whether Internet link to _targetNode (or parent) is connected */
    boolean _nodeLinkConnected = false;
    
    /** Indicates whether to process subnodes in reverse order */
    boolean _reverseSubnodeProcessing = false;

    long _targetNodeID = -1111; // bogus value for now
    long _targetDeviceID = -2222; // bogus value for now

    long _nextConnectTime = 0;
	
    ExportablePacket _exportablePacket = new ExportablePacket();

    /** Default duration for auto-lease */
    long _leaseDurationSec = 3600;


    NodeProbe _nodeProbe = new NodeProbe();

    /** Timeout for NodeProbe "ping" call */
    long _nodeProbeTimeout=30000;

    /** How many times to allow the main thread to sleep
        while waiting for nodes to become available for 
        download **/
    int mainThreadSleepCycles = 30;

    /** Number of subnodes to wake up and have
        waiting for download **/
    int subnodesAwake = 3;

    /** Default lease renewals allowed for auto-lease */
    int _leaseRenewals = 999;
    int _renewalReset = _leaseRenewals;

    String _leaseNote = "";
    //    AutoLease _autoLease = null;

    /** Indicates whether target node has wireless link to portal */
    InetAddress _primaryNodeAddr;

    WakeupWorker _wakeupWorker = null;

    /** System command to send at end of session	*/
    String _systemCommand = null;

    /** Timeout for System command, in seconds		*/
    int _systemCommandTimeout = 15;

    /** Time of last download, to implement downloadInterval */
    long _lastDownloadTime = 0;

    /** Current port configuration */
    Port[] _ports = null;

    /**
     * Devices that have been detected on node, and their download statistics.
     */
    Map _devices = new HashMap();

    /** Log files for packet storage. */
    Map _logs = new HashMap();

    /** To publish or not to publish packets... to JMS that is. */
    boolean _publishPackets = false;

    PublisherComponent _ssdsPublisher = null;

    // Initial value for "most recent packet timestamp"
    long _startRetrieveTime = 0;

    // Worker thread
    ConnectionWorker _connectionWorker = null;

    // Portal which contains this connection
    Portal _portal = null;

    /** Asynchronous worker thread to save and distribute DevicePacketSets */
    DistributeWorker _distributeWorker = new DistributeWorker("_distributeWorker");

    /** Used to send DevicePacketSets to DistributeWorker */
    Vector _distributePacketSets = new Vector();

    /** Where to log the data */
    String _logDirectoryBase;

    String _logDirectory;

    boolean _logDirCreated = false;
	
    // profiler
    Profiler _profiler=new Profiler(_log4j,"portal.profiler",null);

    // PortalSession
    PortalSession _session=null;

    // LeaseID returned from Node.establishLease()
    static final int NO_LEASE = -1;

    // ID of lease on "target" node. 
    int _leaseID = NO_LEASE;

    /** Number of bytes to transfer per getDevicePackets() */
    // int _getDeviceBytes = 32768;

    /** Timeout for getDevicePackets() in ms */
    // int _getDeviceTimeout = 15000;


    /**
     * Construct PortalConnection for node at specified address.
     * 
     * @param remoteAddr
     *            Address of target node
     * @param primaryAddr
     * @param portal
     *            Portal which contains this connection
     **/

    public PortalConnection(InetAddress remoteAddr, InetAddress primaryAddr,
			    Portal portal) { 

	_targetNodeAddr = remoteAddr;
	_primaryNodeAddr = primaryAddr;
	_portal = portal;

	_distributeWorker = new DistributeWorker("_distributeWorker-" + remoteAddr);

	_leaseDurationSec = portal._leaseTimeMsec / 1000;
	_systemCommand = portal._systemCommand;
	_systemCommandTimeout = portal._systemCommandTimeout;
	_publishPackets = portal._publishPackets;
	_startRetrieveTime = portal._startRetrieveTime;
	_logDirectoryBase = portal._logDirectory;
	_profiler.setEnabled(portal._doProfile);

	try {
	    _targetNodeName = remoteAddr.getHostName();
	    _logDirectory = (_logDirectoryBase + File.separator + 
			     _targetNodeName).trim();
	    _log4j.debug("new PortalConnection() for node " + 
			 _targetNodeName);

	} catch (SecurityException e) {
	    _log4j.error("SecurityException when trying to get host name");
	}

	if (_publishPackets) {
	    _ssdsPublisher = new PublisherComponent();
	}

	_log4j.debug("Retrieve all packets created since "
		     + Portal.timeString(_startRetrieveTime) + " ("
		     + _startRetrieveTime + " sec)");



	// Note to attach to leases
	try {
	    _leaseNote = 
		"portal@" + InetAddress.getLocalHost().getHostName() + 
		" connect to " + remoteAddr;

	} catch (Exception e) {
	    _leaseNote = "portal@UNKNOWN!";
	}
	
	try {
	    _distributeWorker.setPriority((Thread.MIN_PRIORITY + Thread.NORM_PRIORITY)/2);
	    _distributeWorker.start();
	} catch (Exception e) {
	    _log4j.error("Can't start DistributeWorker: " + e);
	}
    }
		


    /**
     * Return InetAddress of our remote connection
     */
    public InetAddress getInetAddress() {
	return (_targetNodeAddr);
    }

    /**
     * Returns true if object is PortalConnection to same InetAddress or if
     * object is InetAddress which equals our _targetNodeAddr
     */
    public boolean equals(Object o) {
	if (o instanceof InetAddress)
	    return (_targetNodeAddr.equals(o));
	else if (o instanceof PortalConnection)
	    return (_targetNodeAddr.equals(((PortalConnection) o)
					   .getInetAddress()));
	else
	    return (false);
    }

    /** Return hashCode of the connection's InetAddress */
    public int hashCode() {
	return (_targetNodeAddr.hashCode());
    }

    /**
     * Returns true if portal link is active and portal has valid RMI stub to remote node
     */
    public synchronized boolean nodeConnected() {
	if (!primaryLinkConnected() || 
	    !_nodeLinkConnected || (_targetNode == null)) {
	    return false;
	}

	return true;
    }


    /** Return descriptions of devices on node. */
    public Port[] getPortConfiguration() throws UnknownConfiguration {

	if (_ports == null)
	    throw new UnknownConfiguration();

	return _ports;
    }

    /**
     * Create a log subdirectory named for the remote host.
     * 
     * @param subDirName -
     *            name of subdirectory (appended to _logDirectoryBase)
     */
    void createLogDirectory(String subDirName) {
	_logDirectory = (_logDirectoryBase + File.separator + subDirName)
	    .trim();
	File f = new File(_logDirectory);
	if (!f.exists()) {
	    System.out.println("Creating log directory " + _logDirectory);

	    if (!f.mkdirs())
		_log4j
		    .error("Failed to create log directory "
			   + _logDirectory);
	}
	_logDirCreated = true;
    }

    /** set session tracker */
    public void setSession(PortalSession session){
	_session=session;
    }

    /** get session tracker */
    public PortalSession getSession(){
	return _session;
    }

    /** Get node server proxy, retrieve configuration, packets, etc. */
    public void startSession() throws Exception {

	// *** DEBUG *** //
	ThreadUtility.printThreads();

	_log4j.info("startSession()[" + _targetNodeName + "]: "
		    + currentTimeString());
	// Try to get node proxy
	_log4j.debug("startSession()[" + _targetNodeName
		     + "] - get node proxy");

	_log4j.debug("portal JVM Runtime.totalMemory: "+Runtime.getRuntime().totalMemory());
	_log4j.debug("portal JVM Runtime.freeMemory: "+Runtime.getRuntime().freeMemory());
	_log4j.debug("portal JVM Thread.currentCount - startSession: "+Thread.currentThread().getThreadGroup().activeCount());

	getSession().notify(new PortalEvent(this,
					    PortalEvent.CONNECT_NODE_START));

	// Connection for subnodes is made in the wakeup thread
	// So only connect if it's surface (primary node)
	if (_targetNodeAddr.equals(_primaryNodeAddr)) {  
	    if (!connectNode()) {
		_log4j.info("Not connnected to node");
		getSession().notify(new PortalEvent(this,
						    PortalEvent.CONNECT_NODE_END));
		return;
	    }
	}

	getSession().notify(new PortalEvent(this,
					    PortalEvent.CONNECT_NODE_END));

	long nodeID=-1L;
	try{
	    getSession().notify(new PortalEvent(this,PortalEvent.START_SESSION_START));
	    NodeSessionInfo sessionInfo = 
		_targetNode.startSession(_portal._maintainWDT, _systemCommand.getBytes(), 
					 _systemCommandTimeout);
	    getSession().notify(new PortalEvent(this,PortalEvent.START_SESSION_END));

	    getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_START,"startSession-returnInfo"));

	    _log4j.info("WDT status: " + new String(sessionInfo._wdtStatus));
	    _log4j.info(_targetNodeName + " Command Response: \n\"" + 
			new String(sessionInfo._initCommandStatus) + "\"");

	    _targetNodeID = sessionInfo._nodeID;

	    _log4j.info("node address and deviceID: " + 
			_targetNodeAddr + " " + _targetNodeID);


	    _ports = sessionInfo._ports;

	    _subnodes = sessionInfo._subnodes;


	    StringBuffer buf = new StringBuffer("Node " + _targetNodeID + 
						" has " + 
						_subnodes.length + 
						" subnodes: ");

      // Check to see if a wakeup thread is already running. 
      // Set to done if it is. 
      if ( (_wakeupWorker != null) && (_wakeupWorker._thread.isAlive()) ) {
        	_wakeupWorker._done = true;
        }


	    if(_subnodes.length > 0) {
  
		// Create vector of subnode objects, keep track of retrievals
		Vector targets = new Vector(_subnodes.length);
		
		// Check to see which way to load the Vector for the wakeup thread
		// Normal order or reverse order. 
		if (_reverseSubnodeProcessing) {
		  for (int i=_subnodes.length-1; i >= 0; i--) {
		    buf.append(_subnodes[i].getAddress() + " ");
		    targets.add(new TargetNode(_subnodes[i].getAddress()));
		  }	
		}
		else {
		  for (int i = 0; i < _subnodes.length; i++) {
		    buf.append(_subnodes[i].getAddress() + " ");
		    targets.add(new TargetNode(_subnodes[i].getAddress()));
		  }
		}
	  _reverseSubnodeProcessing = !_reverseSubnodeProcessing;		


		_log4j.debug("portal JVM Thread.currentCount - Before new WakeupWorker: "+Thread.currentThread().getThreadGroup().activeCount());
		
		_log4j.debug("wakeup thread: calling wakeup thread");

		// Initialize available targets vector
		_availableTargets.removeAllElements();

		_wakeupWorker = new WakeupWorker(targets, "_wakeupWorker");

		_log4j.debug("portal JVM Thread.currentCount - After new WakeupWorker: "+Thread.currentThread().getThreadGroup().activeCount());		
		
		_log4j.info(new String(buf));
	    }

	    getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
	    
	}
	catch (Exception e) {
	    
	    _log4j.error("startSession(): Caught IOException: ", e);
	    getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
	    getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
	    // Don't use invalid proxy
	    getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_START,(_targetNodeName+":"+false)));
	    if (_nodeLinkConnected) {
		disconnectNode(false);
	    }
	    getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_END));
	    _log4j.error("startSession() - " + _targetNodeName + " - throw IOException: " + e);
	    throw e;
	}


	getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_START,"startSession-deviceListDbg"));
	if (!_logDirCreated) {
	    createLogDirectory(Long.toString(_targetNodeID));
	}

	for (int i = 0; i < _ports.length; i++) {
	    try {
		System.out.println(new String(_ports[i].getName()) + ", "
				   + new String(_ports[i].getServiceMnemonic())
				   + ", ID=" + _ports[i].getDeviceID());
	    } catch (DeviceNotFound e) {
		System.out.println("NO SERVICE");
		//getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
	    }
	}
	getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));

	// Keep track of all devices detected on this node.
	getSession().notify(new PortalEvent(this,PortalEvent.DEVICE_LIST_START));

	updateDeviceList(_targetNodeID, _ports);


	// Create DeviceLog if it doesn't yet exist.
	// Set latest timestamp to MAX(deviceLogMaxTimestamp,devicesLatest)
	// to make sure we start at the right place for new logs
	// when start time is specified.
	// This should be done before retrieving packets, but it shouldn't
	// take too much time (and so it is not done in separate thread).
	
	try {
	    for(Iterator devs=_devices.keySet().iterator();devs.hasNext();){
		Long deviceID=(Long)devs.next();
		long lDeviceID=deviceID.longValue();

		// create log if it doesn't exist
		createLog(lDeviceID);
		
	    }
	} catch (Exception e) {
	    _log4j.error("Exception in createLog():", e);
	}
	
	getSession().notify(new PortalEvent(this,PortalEvent.DEVICE_LIST_END));

	boolean error = false;
	try {
	    // Retrieve data and distribute to clients
	    _log4j.debug("portal JVM Thread.currentCount - Before retrieveAndDistributeData: "+Thread.currentThread().getThreadGroup().activeCount());
	    retrieveAndDistributeData();
	    _log4j.debug("portal JVM Thread.currentCount - After retrieveAndDistributeData: "+Thread.currentThread().getThreadGroup().activeCount());
	}
	catch (IOException e) {
	    getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
	    _log4j.error("Caught IOException from retrieveAndDistributeData(): " + e);
	    getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_START,(_targetNodeName+":"+false)));
	    disconnectNode(false);
	    getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_END));
	    error = true;
	    if (lostConnection(e)) {
		// Lost connection - Don't process subnodes 
		throw e;
	    }
	}
	catch (Exception e) {
	    getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
	    _log4j.error("Caught Exception from retrieveAndDistributeData(): " + e);
	    error = true;
	}

	if (error) {
	    if (!primaryLinkConnected()) {

		// No point in continuing if primary link is 
		// disconnected
		_log4j.debug("main thread: No connection. Exiting");
		throw new Exception("startSession() - primary link disconnected");
	    }  
	}


	_log4j.debug("portal JVM Thread.currentCount - Before subnode processing: "+Thread.currentThread().getThreadGroup().activeCount());
	if(_subnodes.length > 0) {
	    int k = 0;
	    int l = 0;
	    while (k < _subnodes.length) { // Loop until all 

		if (!primaryLinkConnected()) {
		    // No point in continuing if primary link is 
		    // disconnected
		    _log4j.debug("main thread: No connection. Exiting");
		    throw new Exception("startSession() - primary link disconnected");
		}  

		int numAvailable = 0;
		TargetNode target;
  	  
		synchronized(_availableTargets) {
		    numAvailable  = _availableTargets.size();
		}
		if (numAvailable > 0) { // subnodes are available for download
		    synchronized(_availableTargets) {
		        target = (TargetNode )_availableTargets.elementAt(0);
		        _availableTargets.removeElementAt(0);
		    }
 	       
		    _log4j.debug("main thread: calling process node: " + target._address);
		    k++;

		    // Note that subnode is not "primary" as far as portal is
		    // concerned.
		    PortalConnection conn = 
			_portal.getPortalConnection(target._address, 
						    _primaryNodeAddr); 

		    try {
			conn.setSession(_session);
			conn._nodeLinkConnected = _nodeLinkConnected;
			conn.startSession();
			target._retrieved = true;
		    }
		    catch (Exception e) {
			_log4j.error("startSession(): got exception from startSession() on subnode " + 
				     target._address);

			// Primary connection status is checked at top of this 'while' loop
		    }
		}
		else { // There are no more available subnodes in the vector
		    getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_START,"startSession-subNodeWait"));
		    try {
			_log4j.debug("main thread: available targets empty. Sleeping for a bit");	
			Thread.sleep(1000);
		    } catch (InterruptedException e) {
			_log4j.debug("startSession: got interrupted exception while waiting for subnodes");
		    }
		    getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));

		    // Allow the above sleep to occur l times before breaking out of while loop. 
		    // Available targets has been empty for l seconds, so it is safe to assume 
		    // that all the subnodes that can be downloaded have been
		    l++;
		    if (l > mainThreadSleepCycles) {
			break;	
		    }
    	  	  
		}
	  
	    } 
	    _log4j.debug("portal JVM Thread.currentCount - After subnode processing: "+Thread.currentThread().getThreadGroup().activeCount());
	    _log4j.debug("main thread: Exited subnode while loop for node ID " + _targetNodeID);	

	} // Subnodes.length > 0

	getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_START,("startSession-disconnectNode "+_targetNodeName+":"+true)));
	disconnectNode(true);
	getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_END));
	_log4j.debug("portal JVM Thread.currentCount - After disconnectNode (startSession end): "+Thread.currentThread().getThreadGroup().activeCount());

	_log4j.info("\nstartSession() [" + _targetNodeName + "] - done: "
		    + currentTimeString());
    }


    /** Retrieve packets from node and publish. */
    void retrieveAndDistributeData() 
	throws IOException, Exception {
		
		getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_START,"retrieveAndDistributeData-nodeConnected"));
		if (!nodeConnected()) {
			// Try to get connection
			if (!_nodeLinkConnected || !connectNode()){
				// Couldn't get connection
				getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
				return;
			}
		}
		
		getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
		
		// Create array to hold device IDs of node and its instruments
		long deviceID[] = new long[_ports.length + 1];
		
		// Set first element of device ID array to be id of node
		deviceID[0] = _targetNodeID;
		
		// Copy device IDs from instrument ports into array
		for (int i = 0; i < _ports.length; i++) {
			try {
				deviceID[i + 1] = _ports[i].getDeviceID();
			} catch (DeviceNotFound e) {
				deviceID[i + 1] = -1;
			}
		}
		long latest = 0;
		
		for (int i = 0; i < deviceID.length; i++) {
			
			int totalPackets = 0;
			
			if (deviceID[i] == -1)
				continue;
			
			getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_START,"retrieveAndDistributeData-nodeConnected"));
			if (!nodeConnected()) {
				// Node is no longer connected; terminate packet retrieval
				_log4j.info("Node is not connected; " + 
							"terminate packet retrieval");
				getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
				break;
			}
			getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
			
			try {
				// Timestamp retrieval criterion should start at most
				// recent received so far, plus one millisec
				Long key = new Long(deviceID[i]);
				
				DevicePacketSet packetSet = null;
				
				// Retrieve all packets created since last retrieval.
				// This may require several invocations of retrieveData(),
				// depending on the maximum allowed retrieval size enforced
				// by the node.
				// while (true) { //changed to nodeConnected() rah 18sep2003
				
				while (nodeConnected()) {
					
					getSession().notify(new PortalEvent(this,PortalEvent.RX_PACKET_SET_START,"devID "+deviceID[i]));
					System.out.println("Retrieve data from device " + 
									   deviceID[i]);
					
					PacketStats packetStats = (PacketStats) _devices.get(key);
					
					latest = packetStats._latestPacketTime;
					
					_targetDeviceID=deviceID[i];
					packetSet = retrieveData(deviceID[i], latest + 1,
											 Long.MAX_VALUE);
					
					totalPackets += packetSet._packets.size();
					
					getSession().notify(new PortalEvent(this,PortalEvent.RX_PACKET_SET_END,packetSet));
					
					getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_START,"retrieveAndDistributeData-mostRecent"));
					
					// Determine most recent packet retrieved
					for (int j = 0; j < packetSet._packets.size(); j++) {
						DevicePacket packet = (DevicePacket) packetSet._packets.get(j);
						
						// Under some circumstances (i.e. corruption of onboard
						// log file), can get null packet objects
						if (packet == null) {
							_log4j.error("retrieveAndDistributeData(): " +
										 "Packet #" + j + " is NULL");
							continue;
						}
						if (packet.systemTime() > latest) {
							latest = packet.systemTime();
						}
					}
					
					// This will be used for lower end of time window when
					// data is next retrieved from node
					_log4j.debug("new latest=" + latest);
					packetStats = new PacketStats(latest);
					_devices.put(key, packetStats);
					_log4j.debug("put packet stats");
					getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
					// Save data
					if (packetSet._packets != null)
					{
						getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_START,"retrieveAndDistributeData-distribute"));
						// Save data
						synchronized(_distributePacketSets) {
							_log4j.debug("adding packet set for deviceID "+deviceID[i]+"");
							_distributePacketSets.add(new DistributePacketSet(deviceID[i], packetSet));
						}
						synchronized(_distributeWorker) {
							try {
								_log4j.debug("notifying distributeWorker for deviceID "+deviceID[i]+"");
								_distributeWorker.notify();
							} catch (IllegalMonitorStateException e) {
								_log4j.error("Error notifying distributeWorker: " + e);
							}
						}
						getSession().notify(new PortalEvent(this,PortalEvent.UNSPECIFIED_END));
					}else{
						_log4j.error("packet set was NULL");
					}
					
					// Check for complete packet set
					
					if (packetSet.complete()) {
						System.out.println("Retrieved total of " + totalPackets
										   + " packets from device " + deviceID[i]);
						break;
					}
				}
				
			} 
			
			catch (TimeoutException e) {
				_log4j.error("Timeout exception for device " + deviceID[i]);
				getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				getSession().notify(new PortalEvent(this,PortalEvent.RX_PACKET_SET_END));
			}
			
			catch (RemoteException e) {
				_log4j.error("Remote exception for device " + deviceID[i], e);
				getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));	    
				if (lostConnection(e)) {
					// Lost connection to node - bail out here
					throw e;
				}	
			}
			
			catch (DeviceNotFound e) {
				// Couldn't find this device
				
				_log4j.error("Couldn't find device " + deviceID[i]);
				
				getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				
			}
			catch (IOException e) {
				
				_log4j.error("Caught IOException while retrieving data");
				_log4j.error(e.getClass().getName());
				_log4j.error(e.getMessage());
				
				getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				
				if (lostConnection(e)) {
					// Lost connection to node - bail out here
					throw e;
				}
				
			} catch (NoDataException e) {
				
				getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				
				System.out.println("No data from device " + deviceID[i]);
				_log4j.info("No data from device " + deviceID[i]);
			}
			catch (Exception e) {
				
				_log4j.error("Exception while retrieving data from device "
							 + deviceID[i] +": " + e);
				
				getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				if (lostConnection(e)) {
					// Lost connection to node - bail out here
					throw e;
				}
			}
		}
    }

    /** 
	Instantiate device log, determine log age. 
    */
    void createLog(long deviceID) throws Exception {

	// Does log file already exist?
	Long key = new Long(deviceID);
	DeviceLog log = (DeviceLog) _logs.get(key);
	if (log == null) {
	    // Haven't encountered this device before
	    try {

		System.out.println("creating device log "+deviceID+" in " + _logDirectory);

		// If not, create new log and add it to logs map
		log = new DeviceLog(deviceID, _logDirectory);

		_log4j.debug("recording log entry");
		_logs.put(key, log);
					    
	    } catch (Exception e) {
		_log4j.error("createLog(): caught exception while " + 
			     "constructing log: ", e);
		throw e;
	    }
		

	    // now that a log exists, initialize 
	    // the most recent packet to use as 
	    // a start point.
	    // by default, _resumeStreams is set, in which 
	    // case the newer of the most recent packet or
	    // _startRetrieveTime is used.
	    // If _resumeStreams is not set (-clear is used)
	    // then use _startRetrieveTime if specified, get
	    // entire log (0L) if not.
	    long mostRecent=_startRetrieveTime;
	    long maxTimestamp=((DeviceLog)_logs.get(key)).getMaxTimestamp();
	    if (_portal._resumeStreams) {
		mostRecent=(maxTimestamp>_startRetrieveTime?maxTimestamp:_startRetrieveTime);
	    }
	    _log4j.debug("initializing device log "+deviceID+
			 " mostRecent:"+mostRecent+
			 " startRetrieveTime:"+_startRetrieveTime+
			 " maxTimestamp:"+maxTimestamp);

	    _devices.put(key,new PacketStats(mostRecent));
	}
    }


    /** Return true if distribute-worker thread is busy */
    boolean isBusy()
    {
	return _distributeWorker.isBusy();
    }


    /**
     * DistributePacketSet is the class that's passed to DistributeWorker in the
     * _distributePacketSets vector.  It contains just the ID of the device that the
     * packet set is from, and the DevicePacketSet.
     */
    class DistributePacketSet
    {
	long		_deviceID;
	DevicePacketSet _packetSet;

	DistributePacketSet(long deviceID, DevicePacketSet packetSet)
	{
	    _deviceID = deviceID;
	    _packetSet = packetSet;
	}
    }

    /**
     * DistributeWorker is a single thread (per connection) responsible for saving and
     * distributing data contained in DevicePacketSets.  These are passed to the
     * DistributeWorker through the _distributePacketSets vector
     */
    class DistributeWorker extends Thread
    {
	DistributeWorker(String name) {
	    setName(name);
    	}
    	 	
	boolean _working = false;

	public void run()
	{
	    DistributePacketSet distPacketSet = null;
	    Vector workerPacketSets = null;

	    _log4j.debug("\nDistributeWorker - started: " + currentTimeString());

	    while(true)
		{
		    synchronized(_distributePacketSets)
			{
			    workerPacketSets = (Vector)_distributePacketSets.clone();
			    _distributePacketSets.clear();
			}

		    if (workerPacketSets.size() == 0)
			{
			    _working = false;
			    synchronized(this)
				{
				    try
					{
					    wait(1000);
					} catch (Exception e) {
					    _log4j.error("Exception in DistributeWorker: " + e);
					}
				}
			}
		    else
			{
			    _working = true;
			    Iterator i = workerPacketSets.iterator();
			    while (i.hasNext())
				{
				    distPacketSet = (DistributePacketSet)(i.next());
					_log4j.debug("calling save and distribute for deviceID "+distPacketSet._deviceID);
				    saveAndDistributeData(distPacketSet._deviceID, distPacketSet._packetSet);
				}
				//_log4j.debug("nulling workerPacketSets and yielding");
			    workerPacketSets = null;
			    yield();
			}
		}
	}

	boolean isBusy()
	{
	    return _working;
	}

	void saveAndDistributeData(long deviceID, DevicePacketSet packetSet)
	{
		if (packetSet._packets != null) {
			
			// Save data
			try {
				_log4j.debug("saving data");
				//getSession().notify(new PortalEvent(this,PortalEvent.SAVE_DATA_START));
				saveData(deviceID, packetSet._packets);
				//getSession().notify(new PortalEvent(this,PortalEvent.SAVE_DATA_END));
				
			} catch (Exception e) {
				
				//getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				//getSession().notify(new PortalEvent(this,PortalEvent.SAVE_DATA_END));
				_log4j.error("Exception from saveData():"+e);
			}
			// Catch anything thrown by publishing component
			// after saving to log.
			// Must ensure that all data is logged even if publishing
			// isn't possible.
			try{
				if (_publishPackets) {
					// Publish packets to JMS
					_log4j.debug("publishing packets");
					//getSession().notify(new PortalEvent(this,PortalEvent.PUBLISH_DATA_START));
					publishData(packetSet._packets);
					//getSession().notify(new PortalEvent(this,PortalEvent.PUBLISH_DATA_END));
				}
			}catch(Exception e){
				//getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				//getSession().notify(new PortalEvent(this,PortalEvent.PUBLISH_DATA_END));
				_log4j.error("Exception from publishData:"+e);				
			}catch(Throwable t){
				//getSession().notify(new PortalEvent(this,PortalEvent.EXCEPTION,e));
				//getSession().notify(new PortalEvent(this,PortalEvent.PUBLISH_DATA_END));
				_log4j.error("Throwable from publishData:"+t);				
			}
		}else {
			_log4j.debug("packetSetPackets=null");
		}
	}

	
	/**
	 * Write data into appropriate log file.
	 */
	void saveData(long deviceID, Vector packets) 
	    throws Exception {

	    // Does log file already exist?
	    Long key = new Long(deviceID);
	    DeviceLog log = (DeviceLog) _logs.get(key);
	    if (log == null) {
		_log4j.error("DeviceLog not found for " + deviceID);
		throw new Exception("DeviceLog not found for " + deviceID);
	    }

	    // Write each packet to log file
	    for (int i = 0; i < packets.size(); i++) {
		DevicePacket packet = (DevicePacket) packets.get(i);

		// Under some circumstances (i.e. corruption of onboard
		// log file), can get null packet objects
		if (packet == null) {
		    _log4j.error("saveData(): Packet #" + i + " is NULL");
		    continue;
		}

		// Append packet, but don't modify sequence number or
		// metadata reference.
			//_log4j.debug("logging packet "+i+" deviceID "+deviceID);
			log.appendPacket(packet, false, false);
			//_log4j.debug("done logging packet "+i+" deviceID "+deviceID);
	    }
	}

	/** Publish packets to JMS server. */
	void publishData(Vector packets) {
		int nPacket = 0;
		Iterator i = packets.iterator();
		while (i.hasNext()) {
			
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(bos);
				DevicePacket packet = (DevicePacket )i.next();
				// Under some circumstances (i.e. corruption of onboard
				// log file), can get null packet objects
				if (packet == null) {
					_log4j.error("publishData(): Packet #" + nPacket + 
								 " is NULL");
					continue;
				}
				
				_exportablePacket.wrapPacket(packet);
				_exportablePacket.export(dos);
				dos.flush();
				byte[] exportedBytes = bos.toByteArray();
				dos.close();
				//_log4j.debug("<<<< calling publishBytes >>>>");
				// if SSDS is down, this call takes 5 sec
				// and does not fail, though it prints some 
				// debug info. Instead of failing cleanly,
				// the portal fails to log subsequent packet sets.
				// For some reason, the isConnected() method in
				// PublisherComponent is not found in prey or bob jars.
				// it would be better for PublisherComponent to throw
				// an exception or implement that method.
				_ssdsPublisher.publishBytes(exportedBytes);
				//_log4j.debug("<<<< done calling publishBytes >>>>");
			} catch (IOException e) {
				_log4j.debug("publishing error (ioexception):");
				e.printStackTrace();
			}catch (Throwable t) {
				_log4j.debug("publishing error (throwable):");
				t.printStackTrace();
			}
			//_log4j.debug("<<<< nPacket="+nPacket+" >>>>");
			nPacket++;
		}
	}

    } /* Class DistributeWorker */


    /**
     * Called by node when communication link is going down.
     */
    public void nodeLinkDisconnecting(long nextConnectTime) {
	long now = System.currentTimeMillis();

	_log4j.info("nodeLinkDisconnecting() [" + _targetNodeName + "]");
 
	try {
	    terminateLease();
	}
	catch (Exception e) {
	    _log4j.info("nodeLinkDisconnecting() - got exception while terminating lease: " + e);
	}

	_nextConnectTime = nextConnectTime;

	System.out.println("Link terminated at " + Portal.timeString(now));
	System.out.println("Next connection at "
			   + Portal.timeString(_nextConnectTime + now));

    }

    /** Called by portal when "link up" message received from node. */
    public void nodeLinkConnected() {
	_nodeLinkConnected = true;
	long now = System.currentTimeMillis();
	_log4j.info("PortalConnection.nodeLinkConnected() [" + _targetNodeName
		    + "]: " + Portal.timeString(now));

	if ((_connectionWorker == null) || !_connectionWorker.isAlive()) {
	    if (now >= _lastDownloadTime + _portal._minDownloadIntervalMsec) {
		// Retrieve/distribute data, send cmds, etc
		_lastDownloadTime = now;
		_connectionWorker = new ConnectionWorker("_connectionWorker");

		_connectionWorker.start();

	    } else {

		System.out.println(_targetNodeName
				   + " - Too soon for another download");
	    }
	} else {

	    System.out.println(_targetNodeName + " already connected? "
			       + "Don't launch another worker...");
	    // when starting over, clean up profiling sessions
	    //_log4j.debug("nodeLinkConnected: cleaning up profiler session");
	    int sessionStatus = getSession().status();
	    _log4j.debug("nodeLinkConnected: session status - "+(sessionStatus==PortalSession.OPEN?"OPEN"
								 :(sessionStatus==PortalSession.CLOSED?"CLOSED"
								   :"INITIALIZED")));
	    /*
	      switch(sessionStatus){
	      case PortalSession.OPEN:
	      _log4j.debug("NodeLinkNotify closing OPEN profiler session");
	      String msg="NodeLinkNotify SESSION_END - "+_targetNodeName;
	      getSession().notify(new PortalEvent(this,PortalEvent.SESSION_END,(String)msg));
	      _profiler.info(getSession().summarize());
	      break;
	      case PortalSession.CLOSED:
	      break;
	      default:
	      break;
	      }
	    */
	}
    }


    /** Notify PortalConnection that network link has come up/down */
    public void nodeLinkNotify(boolean on)
    {
	PortalConnection conn;

	_log4j.debug("PortalConnection.nodeLinkNotify(" +  (on ? "ON)" : "OFF)"));

	if (on) {
	    _nodeLinkConnected = true;
	}
	else{
	    getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_START,(_targetNodeName+":"+false)));
	    disconnectNode(false);
	    getSession().notify(new PortalEvent(this,PortalEvent.DISCONNECT_NODE_END));
	}
	if (_subnodes != null)
	    for (int i = 0; i < _subnodes.length; i++)
		{
		    conn = _portal.findPortalConnection(_subnodes[i].getAddress());
		    if (conn != null) {
			conn.nodeLinkNotify(on);
		    }
		}
    }

    /** Mark node link as disconnected and discard the node proxy object. */
    void disconnectNode(boolean ok) {

	_log4j.debug(_targetNodeAddr + " disconnectNode() - "
		     + currentTimeString());

	_nodeLinkConnected = false;
	_log4j.debug("_nodeLinkConnected set FALSE");

	if (ok) {// Normal disconnect, terminate the lease

	    _log4j.debug(_targetNodeAddr + 
			 " disconnectNode(): normal connection termination");
			
	    // If target is 'primary' surface node, terminate any leases 
	    // created incidentally to leasing on its subnodes

	    try {
		String leaseNoteSubstring = _leaseNote;
		int index = _leaseNote.indexOf(" connect");
		if (index > 0) {
		    leaseNoteSubstring = _leaseNote.substring(0, index);
		}

		_log4j.debug("disconnectNode(): Trying to terminate all " +
			     "leases with description containing \"" +
			     leaseNoteSubstring + "\"");

		LeaseDescription[] leases = _targetNode.getLeases(true);
		for (int i = 0; i < leases.length; i++) {

		    if (leases[i]._type != LeaseDescription.COMMS_LEASE) {
			// Only dealing with COMMS leases
			continue;
		    }

		    String note = 
			new String (leases[i]._clientNote, 0, 
				    leases[i]._clientNote.length);
			
		    if (note.indexOf(leaseNoteSubstring) >= 0) {
			// This lease was created directly or 
			// incidentally by the portal connection; so
			// terminate the lease.
			_log4j.debug("disconnectNode(): terminating lease "
				     + leases[i]._id);
			try {
			    _targetNode.terminateLease(leases[i]._id);
			}
			catch (Exception e) {
			    _log4j.error("Unable to terminate lease " + 
					 leases[i]._id);
			}
		    }

		}
	    }
	    catch (Exception e) {
		_log4j.error("Unable to retrieve leases from node");
	    }
	}
	else { // Not normal disconnect
	    _log4j.debug(_targetNodeName + 
			 " disconnectNode(): abrupt connection termination");
	    if (_subnodes != null)
		for (int i = 0; i < _subnodes.length; i++)
		    {
			PortalConnection conn = _portal.findPortalConnection(_subnodes[i].getAddress());
			if (conn != null)
			    conn.disconnectNode(ok);
		    }
	}
	_targetNode = null;
    }


    /**
     * Obtain the node proxy object and get a lease. Return true on success,
     * false on error.
     */
    protected boolean connectNode() {
	String nodeURL;

	nodeURL = Portals.mooringURL(_targetNodeName);

	_log4j.debug("Looking for node server stub at " + nodeURL);

	_targetNode = null;

	try {
	    _targetNode = (MOOSNode) Naming.lookup(nodeURL);
	    _nodeLinkConnected = true;		//If succeeded, we have a link

	    _log4j.debug("Connected to node " + _targetNodeAddr);

	    _leaseID=_targetNode.establishLease(_leaseDurationSec*1000L, _leaseNote.getBytes());
	    
	    _log4j.debug("Lease established for " + _leaseDurationSec + 
			 " sec, node " + _targetNodeName + 
			 "; id="+_leaseID);
		
	} 
	catch (LeaseRefused e) {
	    _log4j.error("LeaseRefused: " + e.getMessage());
	} catch (Exception e) {
	    _log4j.error("connectNode() - Exception: ", e);
	}

	_log4j.debug("connectNode() done");
	return nodeConnected();
    }

    /** Return a formatted time string corresponding to current time. */
    static String currentTimeString() {
	long t = System.currentTimeMillis();
	return DateFormat.getDateTimeInstance().format(new Date(t));
    }

    /**
     * Keep track of all devices detected on this node; add any new devices 
     * from ports array to list.
     */
    void updateDeviceList(long nodeID, Port[] ports) {

	Long key = new Long(nodeID);
	if (!_devices.containsKey(key)) {
	    System.out.println("New node ID: " + key);
	    // Add nodeID to list
	    _devices.put(key, new PacketStats(_startRetrieveTime));
	}

	for (int i = 0; i < ports.length; i++) {
	    try {
		key = new Long(ports[i].getDeviceID());
		if (!_devices.containsKey(key)) {
		    System.out.println("New device ID: " + key);
		    // Add device to list
		    _devices.put(key, new PacketStats(_startRetrieveTime));
		}
	    } catch (DeviceNotFound e) {
		// No device on this port; skip it
	    }
	}
    }

    /**
     * Retrieve data from specified device, within specified time-window. */
    protected DevicePacketSet retrieveData(long deviceID, long startTime,
					   long endTime) 
	throws RemoteException, DeviceNotFound,
	       NoDataException, IOException, TimeoutException {

	System.out.println("Retrieving data from device #" + deviceID);

	DevicePacketSet packetSet = null;

	if (_targetNode.summarizing(deviceID)) {

	    _log4j.info("Retrieving data from SUMMARIZING device #" + deviceID + 
			", startTime=" + startTime + 
			", maxbytes=" + 
			_portal._summarizerMaxPacketSetBytes + 
			", timeout=" + 
			_portal._summarizerPacketSetTimeoutMsec);

	    // Get SummaryPackets instead of SensorDataPackets
	    int typeMask =
		DevicePacket.METADATA_FLAG |
		DevicePacket.DEVICEMESSAGE_FLAG |
		DevicePacket.SUMMARY_FLAG;

	    packetSet = 
		_targetNode.getDevicePackets(deviceID, startTime, endTime,
					     _portal._summarizerMaxPacketSetBytes, 
					     typeMask, 
					     _portal._summarizerPacketSetTimeoutMsec);
	}
	else {
	    // Get all packet types
	    _log4j.debug("retrieveData() - device " + deviceID + 
			 " is NOT summarizing");

	    _log4j.info("Retrieving data from device #" + deviceID + 
			", startTime=" + startTime + 
			", maxbytes=" + 
			_portal._maxPacketSetBytes + 
			", timeout=" + 
			_portal._packetSetTimeoutMsec);

	    packetSet = 
		_targetNode.getDevicePackets(deviceID, startTime, endTime,
					     _portal._maxPacketSetBytes,
					     _portal._packetSetTimeoutMsec);
	}

	System.out.println("Retrieved " + packetSet._packets.size()
			   + " packets from device #" + deviceID);

	_log4j.info("Retrieved " + packetSet._packets.size()
		    + " packets from device #" + deviceID);

	// Print packet info
	for (int j = 0; j < packetSet._packets.size(); j++) {
	    DevicePacket packet = (DevicePacket) packetSet._packets
		.elementAt(j);

	    _log4j.debug(packet);

	    // Find number of payload bytes 
	    int nPayloadBytes = -1;
	    if (packet instanceof SensorDataPacket) {
		nPayloadBytes = ((SensorDataPacket )packet).dataBuffer().length;
	    }
	    else if (packet instanceof MetadataPacket) {
		nPayloadBytes = ((MetadataPacket )packet).getBytes().length;
	    }
	    else if (packet instanceof DeviceMessagePacket) {
		nPayloadBytes = ((DeviceMessagePacket )packet).getMessage().length;
	    }
	    else if (packet instanceof SummaryPacket) {
		nPayloadBytes = ((SummaryPacket )packet).getData().length;
	    }

	    if (nPayloadBytes >= 0) {
		_log4j.debug("nPayloadBytes: " + nPayloadBytes);
	    }
	    else {
		_log4j.debug("nPayloadBytes: UNKNOWN");
	    }
	    
	}

	return packetSet;
    }



    /** Return connection's Node proxy. */
    MOOSNode getNode() {
	return _targetNode;
    }


    /** Return true if exception indicates no connection to node, or connection has gone down and 
     then been restored while the PortalConnection is in use. */
    boolean lostConnection(Exception exception) {

	if (exception instanceof NoRouteToHostException) {
	    return true;
	}

	if (!nodeConnected()) {
	    return true;
	}

	String msg = exception.getMessage();
	if (msg.indexOf("Exception creating connection") >= 0) {
	    // Definite indicator of connection problem
	    return true;
	}
	else if (msg.indexOf("Connection refused to host") >= 0) {
	    return true;
	}
	else if (msg.indexOf("No route to host") >= 0) {
	    return true;
	}


	try {
	    // If node can be 'probed', then connection is not lost;
	    // otherwise connection is lost.
	    return !_nodeProbe.probe(_targetNodeAddr,
				    Portals.nodeProbePort(), 
				    _nodeProbeTimeout);
	}
	catch (Exception e) {
	    // Connection is lost
	    return true;
	}
    }



    /**
     * ConnectionWorker runs in thread launched when a connection with node has
     * been established. Retrieve and distribute data, send queued commands.
     */
    class ConnectionWorker extends Thread {
	ConnectionWorker(String name) {
	    setName(name);
	}
    	
	public void run() {
	    _log4j.debug("\nConnectionWorker - started: "
			 + currentTimeString());

	    String msg=("ConnectionWorker SESSION_START - "+_targetNodeName);
	    getSession().notify(new PortalEvent(this,PortalEvent.SESSION_START,(String)msg));

	    try {
		startSession();
	    }
	    catch (Exception e) {
		_log4j.error("ConnnectionWorker.run() - caught exception from startSession(): " + e);
	    }

	    // this consistently takes 5-10 min longer than PPP connect
	    // times shown in var/log/messages...
	    // because 240 sec autoleases take time to time out???

	    msg=("ConnectionWorker SESSION_END - "+_targetNodeName);
	    getSession().notify(new PortalEvent(this,PortalEvent.SESSION_END,(String)msg));

	    // send session info to profiler log
	    _profiler.info(getSession().summarize());

	    _connectionWorker = null;
	    _log4j.debug("\nConnectionWorker - done: " + currentTimeString());
	}
    }

    /** Terminate lease on the target node */
    public void terminateLease() throws LeaseRefused,RemoteException{
	_log4j.debug("terminateLease(); id="+_leaseID + " on " + 
		     _targetNodeName);
	try{
	    if(_targetNode!=null) {
		_targetNode.terminateLease(_leaseID);
	    }
	    _log4j.debug("terminateLease(); terminated lease id=" + 
			 _leaseID + " on " + _targetNodeName);
	}catch(LeaseRefused e){
	    _log4j.error("terminateLease(); Lease Refused exception for lease id=" + _leaseID);  	
	    e.printStackTrace();
	}
    }
 
    class WakeupWorker implements Runnable {
  	
	    Vector targets_loc;
    	Thread _thread = new Thread(this);
      boolean _done = false; // used to keep multiple wakeup threads from being invoked
  	
  	WakeupWorker(Vector newTargets, String name) {
	    targets_loc = newTargets;
	    _thread.setName(name);
	    _thread.start();
  	}
  	
  	public void run() {
	    int j = 0;
	    while ( (targets_loc.size() > 0) && (!_done) ) {
  	  	
		if (!primaryLinkConnected()) {
		    _log4j.debug("wakeup thread: No connection. Exiting");
		    return;
  	  	}
		    
		// Grab this target node
		TargetNode target = (TargetNode )targets_loc.elementAt(j);
		    
		// See how many subnodes have been awakened for download
		int numAvailable = 0; 
		synchronized(_availableTargets) {
		    numAvailable = _availableTargets.size();
		}
		    	
		// If it's less than subnodesAwake, keep going through the list
		if(numAvailable < subnodesAwake) {
		    _log4j.info("Attempt to wakeup " + target._address);
		    		
		    // Attempt wakeup itry times
		    boolean awake = false;
		    for (int itry = 0; itry < 3; itry++) {
			if (!nodeConnected()) {
		            _log4j.debug("Aborting wakeup of " + 
					 target._address);
		            return;
			}
			try {
		            _log4j.info("Send wakeup signal to " + 
					target._address + " , try #" + itry);

			    // Send wakeup signal to the subnode
			    _targetNode.wakeupNode(target._address);
			    awake = true;
			    _log4j.info("wakeup succeeded for " + 
					target._address);
		            break;
			}
			catch (IOException e) {
		            _log4j.error("Exception trying to wake " +
					 target._address, e);
			}
		    }


		    _log4j.info("Attempt to probe " + target._address);

		    boolean ret = false;
		    try {
			NodeProbe pt = new NodeProbe();
			ret = pt.probe(target._address, 
				       Portals.nodeProbePort(), 
				       _nodeProbeTimeout);
		    }
		    catch(Exception e){
			_log4j.error("Exception during NodeProbe of " + 
				     target._address); 

			e.printStackTrace();
		    }
           	
		    if (ret) {
	          	
	          	_log4j.info("NodeProbe returned true for " 
				     + target._address); 
	          	
			// Note that subnode is not "primary" as far as portal is
			// concerned.
			PortalConnection conn = 
			    _portal.getPortalConnection(target._address, _primaryNodeAddr); 
		    		
			// connection WAS successful. Remove from targets and start at beginning of node list
			if(conn.connectNode()) { // connection was successful
			    synchronized(_availableTargets) {
				_availableTargets.add( targets_loc.elementAt(j) );	
			    }
			    targets_loc.removeElementAt(j); 
			    _log4j.debug("wakeup thread: added to available: " + target._address);
		            j = 0;
			}
			// Connection NOT successful despite good ping. Remove from targets and move to next node
			else { 
			    targets_loc.removeElementAt(j); 
   	    		    _log4j.error("connectNode() failed for " + 
					 target._address);
			    j++;
			}
		    }
		    // ping NOT successful. Remove from targets and move to next node
		    else { 
			targets_loc.removeElementAt(j); 
			_log4j.error("NodeProbe returned false for " + 
				     target._address);
			j++;
		    }
		    if(j >= targets_loc.size()) { // Adjust j to stay inside bounds of vector
			j = 0;	
		    }    		  		
		}
		// there are "subnodesAwake" available subnodes still waiting to be downloaded. Time to sleep
		else { 
		    try {
			_log4j.debug("wakeup thread: going to sleep");
			Thread.sleep(1000);
		    }
		    catch (InterruptedException e) {
    		    }
		}	  
	    } 	
	    _log4j.debug("wakeup thread: exiting wakeup thread");
	    return;
	}
    }


    public long getTargetNodeID(){
	return _targetNodeID;
    }
    public long getTargetDeviceID(){
	return _targetDeviceID;
    }


    /**
       Subnode class represents a subnode for the purposes of telemetry 
       retrieval.
    */
    class TargetNode {

	TargetNode(InetAddress address) {
	    _address = address;
	}

	/** Set true when retrieved telemetry */
	boolean _retrieved = false;

	/** IP address of subnode */
	InetAddress _address;
    }



    boolean primaryLinkConnected() {
	try {
	    return _portal.primaryLinkConnected(_primaryNodeAddr);
	}
	catch (Exception e) {
	    _log4j.error("primaryLinkConnected(): caught exception: " + e);
	    return false;
	}
    }


} /* PortalConnection */



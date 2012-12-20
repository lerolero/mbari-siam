// Copyright 2002 MBARI
package org.mbari.siam.operations.portal;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

import org.mbari.siam.distributed.portal.PortalInterface;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.utils.NodeProbe;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Appender;
import org.apache.log4j.varia.StringMatchFilter;
import org.apache.log4j.varia.DenyAllFilter;


/**
Portal is the "gateway" between clients on the local network,
and a remote node server located on the far side of a 
low-bandwidth/intermittent connection. 
<p>
When a communications link to the node is available, the Portal
attempts to retrieve data from the node's sensors. The retrieved
packets are then distributed to shore clients, as well as saved
in a file buffer. The Portal keeps track of which packets
it has retrieved from each sensor; on subsequent retrievals it
requests the "latest" data from the sensor.
<p>

@author Tom O'Reilly
*/
public class Portal extends UnicastRemoteObject implements PortalInterface
{
    static private Logger _log4j = Logger.getLogger(Portal.class);

    // CVS revision 
    private static String _versionID = "$Revision: 1.4 $";

    /** Lease time to request via establishLease().  Defaults to
	60 seconds, can be overridden via system property "leaseTime" */
    long _leaseTimeMsec = 60*1000;

    /** Read timeout passed to the socket factory.  Defaults to
	60 seconds, can be overridden via system property "leaseTime" */
    int _soReadTimeoutMsec = Portals.DEFAULT_WIRELESS_SOCKET_TIMEOUT;

    //The NodeConnectedWorker that reads the raw socket from node
    NodeConnectedWorker _nodeWorker = null;

    /** Download interval in seconds.  If we receive a
	notifyPortalLinkConnected() before this time has elapsed, we
	won't do another download.  Can overridden via system property
	"downloadInterval" */
    long _minDownloadIntervalMsec = 0;

    /** System command to send at end of session	*/
    String _systemCommand = null;

    /** Timeout for System command, in seconds		*/
    int _systemCommandTimeout = 15;

    // Wait at most 15 minutes for socket messages
    static final int SO_TIMEOUT = 900000;

    /** To publish or not to publish packets... to JMS that is. */
    boolean _publishPackets = false;

    /** Maintain nodes' watchdog timers. */
    boolean _maintainWDT = true;

    /** Exit when PPP connection goes down */
    boolean _exitWhenDone = false;

    /** Initial value for "most recent packet timestamp" */
    long _startRetrieveTime = 0;

    /** Socket on which we listen for messages from node that we're
	connected */
    ServerSocket _srvSocket;

    /** Vector of PortalConnections (nodes) that we're servicing. */
    Vector _connections;

    /** Where to log the data */
    String _logDirectory;

    Vector _preferredNodes = new Vector();

    /** Enable profiling*/
    boolean _doProfile=true;

    /** True if there is a network connection from portal to the node network */
    Map _primaryLinks = new HashMap();

    /** Start datastream retrievals based on latest logged data */
    boolean _resumeStreams = false;

    /** Number of bytes to transfer per getDevicePackets() */
    int _maxPacketSetBytes = 32768;
    int _summarizerMaxPacketSetBytes = 2048;

    /** Timeout for getDevicePackets() in ms */
    int _packetSetTimeoutMsec = 30000;
    int _summarizerPacketSetTimeoutMsec = 30000;

    /** Construct Portal for specified node. */
    public Portal(String[] primaryNodeNames,
		  long startRetrieveTime,
		  boolean publishPackets,
		  String logDirectory,
		  boolean doProfile,
		  boolean doWDT,
		  Vector preferredNodes,
		  boolean resumeStreams)
	throws RemoteException, UnknownHostException, IOException, Exception
    {

	_doProfile=doProfile;
	_maintainWDT=doWDT;
	_publishPackets = publishPackets;
	_preferredNodes = preferredNodes;
	_resumeStreams = resumeStreams;

	if (_publishPackets) {
	    System.out.println("Will publish packets to JMS");
	}
	else {
	    System.out.println("Will not publish packets");
	}

	// Start rmiregistry
	try {
	    _log4j.info("Starting registry... ");
	    LocateRegistry.createRegistry(1099);
	    _log4j.info("registry started.");
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    _log4j.error(e.getMessage());
	}

	// Bind RMI services
	try
	{
	    Naming.rebind(Portals.portalURL("localhost"), this);
	} catch (Exception e) {
	    _log4j.error("Exception in Naming.rebind(): " + e);
	    e.printStackTrace();
	}

	// Create server socket to listen for messages from node
	_log4j.debug("Creating Portal Server Socket on port "+Portals.portalTCPPort());
	_srvSocket = new ServerSocket(Portals.portalTCPPort());

	// Create Vector of PortalConnections
	_connections = new Vector();

	_startRetrieveTime = startRetrieveTime;
	System.out.println("Retrieve all packets created since " + 
		     Portal.timeString(_startRetrieveTime) + 
		     " (" + _startRetrieveTime + " sec)");

	_logDirectory = logDirectory;
	System.out.println("Save data to directory \"" + _logDirectory + "\"");

        getProperties();

	/** Create socket factory. */
	try {
	    _log4j.debug("Set socket factory");
	    RMISocketFactory.setSocketFactory(new PortalSocketFactory(_soReadTimeoutMsec));
	}
	catch (IOException e) {
	    _log4j.error("RMISocketFactory.setSocketFactory() failed", e);
	}


	NodeProbe nodeProbe = new NodeProbe();

	for (int i = 0; i < primaryNodeNames.length; i++)
	{
	    // Build connections for those passed on command line
	    // NOTE: Nodes passed on command line are assumed to 
	    // be surface nodes, i.e. to be comms lease manager nodes!
	    String name = primaryNodeNames[i];


	    if (name != null) {
		PortalConnection conn=null;
		try {
		    _log4j.debug("Trying to find node " + name);
		    InetAddress addr = InetAddress.getByName(name);

		     conn = getPortalConnection(addr, addr);

		    // Determine connection status of primary node
		     boolean connected = false;
		     try {
			connected = nodeProbe.probe(addr, Portals.nodeProbePort(), 10000);
		     }
		     catch(Exception e){
			 _log4j.error("Exception in NodeProbe: " + e); 
		     }

		     // Add to primary nodes list
		     PrimaryLink link = (PrimaryLink )_primaryLinks.get(addr);
		     if (link == null) {
			 link = new PrimaryLink(addr, connected);
			 _primaryLinks.put(addr, link);
		     }

		    _log4j.debug("Trying to download from " + name);

		    // Since this is a new session, create a new
		    // PortalSession and give it to the PortalConnection.
		    // The PortalSession is part of the profiler that
		    // generates portal statistics in the profile log.
		    // If the portal session is open, it probably
		    // means that the ConnectionWorker.startSession()
		    // is not finished.
		    // In this case, we should leave the session to finish.
		    // the subsequent call to nodeLinkConnected() 
		    // will detect that the ConnectedWorker is still
		    // active and return.
		    PortalSession session=conn.getSession();
		    if(session!=null){
			if(session.status()!=PortalSession.OPEN)
			    conn.setSession(new PortalSession(name));
		    }else{
			conn.setSession(new PortalSession(name));
		    }

		    // This notifies the PortalSession of a PortalEvent (in this case,
		    // the start of the session
		    // PortalConnection sends notifications of many other events to
		    // the PortalSession.
		    conn.getSession().notify(new PortalEvent(conn,PortalEvent.SESSION_START,("Portal session_start - "+name)));
		
		    conn.startSession();

		    // notify the PortalSession that the session has ended
		    conn.getSession().notify(new PortalEvent(conn,PortalEvent.SESSION_END,("Portal session_end - "+name)));

		    // This causes the profiler (PortalSession) to dump a summary of
		    // the completed session to the profile log
		    conn._profiler.info(conn.getSession().summarize());

		} catch (UnknownHostException e) {

		    _log4j.error("Unknown Host: " + name);

		}
		catch (Exception e) {
		    _log4j.error("Exception in portal constructor: " + e);
		}
	    }
	}


	_log4j.debug("Portal constructor finished");

	if (_exitWhenDone) {
	    System.exit(0);
	}

// To debug RMI problems, uncomment the following two lines
//	System.getProperties().put("java.rmi.server.logCalls", "true");
//	RemoteServer.setLog(System.out);
    }

    /**
       Wait for and accept new client connection; add new client to list
       of clients.
    */
    public void run() {

	while (true)
	{
	    try
	    {
		_log4j.debug("run() - _srvSocket.accept()");
		Socket sock = _srvSocket.accept();
		InetAddress addr = sock.getInetAddress();
		_log4j.debug("run() - accepted connection from " + addr);

		PrimaryLink link = (PrimaryLink )_primaryLinks.get(addr);
		if (link == null) {
		    // Add to primary nodes list
		    link = new PrimaryLink(addr, true);
		    _primaryLinks.put(addr, link);
		}

		link.setConnected(true);

		// Since portal got signal from node, it must be 'primary'
		_nodeWorker = 
		    new NodeConnectedWorker(sock,
					    sock.getInetAddress().getHostName());
		_nodeWorker.start();

		_log4j.info("Portal got connection from " + 
			     sock.getInetAddress().getHostName()
			     + " " +
			     timeString(System.currentTimeMillis()));
	    }
	    catch (Exception e) {

		_log4j.error("Portal.run(): ", e);

	    }
	}
    }


    /** Look for System property "leaseTime", "readTimeout",
        "downloadInterval", and "systemCommand"*/
    protected void getProperties()
    {
        try {
	    Properties sysProperties = System.getProperties();
	    String propertyStr = sysProperties.getProperty("leaseTimeMsec");
	    if (propertyStr != null)
	    {
	        _leaseTimeMsec = Long.parseLong(propertyStr);
		_log4j.debug("Lease time is " + _leaseTimeMsec + " ms");
	    }

	    propertyStr = sysProperties.getProperty("soReadTimeoutMsec");
	    if (propertyStr != null)
	    {
	        _soReadTimeoutMsec = Integer.parseInt(propertyStr);
		_log4j.debug("Read timeout is " + _soReadTimeoutMsec + " ms");
	    }

	    propertyStr = sysProperties.getProperty("minDownloadIntervalSec");
	    if (propertyStr != null)
	    {
	        _minDownloadIntervalMsec = 1000*Long.parseLong(propertyStr);
		_log4j.debug("Download interval is " + 
			     _minDownloadIntervalMsec/1000
			     + " seconds");
	    }

	    _systemCommand = sysProperties.getProperty("systemCommand");
	    if (_systemCommand != null)
	    {
		_log4j.debug("System command is \"" + _systemCommand + "\"");
	    }

	    propertyStr = sysProperties.getProperty("systemCommandTimeout");
	    if (propertyStr != null)
	    {
	        _systemCommandTimeout = Integer.parseInt(propertyStr);
		_log4j.debug("System command timeout is " +
			      _systemCommandTimeout + " seconds");
	    }

	    propertyStr = sysProperties.getProperty("exitWhenDone");
	    if (propertyStr != null)
	    {
	        _exitWhenDone = Boolean.valueOf(propertyStr).booleanValue();
		_log4j.debug("exitWhenDone is " + 
			     (_exitWhenDone ? "true" : "false"));
	    }

	    propertyStr = sysProperties.getProperty("maxPacketSetBytes");
	    if (propertyStr != null)
	    {
	        _maxPacketSetBytes = Integer.parseInt(propertyStr);
		_log4j.debug("Bytes per getDevicePacket(): " + _maxPacketSetBytes);
	    }

	    propertyStr = sysProperties.getProperty("packetSetTimeoutMsec");
	    if (propertyStr != null)
	    {
	        _packetSetTimeoutMsec = Integer.parseInt(propertyStr);
		_log4j.debug("Timeout for getDevicePacket() is " + _packetSetTimeoutMsec + " ms");
	    }

	    // Properties applied to summarizing services
	    propertyStr = sysProperties.getProperty("summarizer.maxPacketSetBytes");
	    if (propertyStr != null)
	    {
	        _summarizerMaxPacketSetBytes = Integer.parseInt(propertyStr);
		_log4j.debug("Bytes per getDevicePacket(): " + _summarizerMaxPacketSetBytes);
	    }

	    propertyStr = sysProperties.getProperty("summarizer.packetSetTimeoutMsec");
	    if (propertyStr != null)
	    {
	        _summarizerPacketSetTimeoutMsec = Integer.parseInt(propertyStr);
		_log4j.debug("Timeout for getDevicePacket() is " + _summarizerPacketSetTimeoutMsec + " ms");
	    }

	} catch (Exception e) {
	    _log4j.error("Exception trying to get properties. "
			  + e + " Using defaults.");
	}
    }


    /**
       Return time (millisec since epoch) corresponding to input
       date/time string.
    */
    static long parseDateTime(String dateTimeString) 
	throws ParseException {

	String patterns[] = {"M/d/yyyy'T'H:m:s", "M/d/yyyy'T'H:m",
			     "M/d/yyyy"};

	SimpleDateFormat dateFormat = new SimpleDateFormat();
	dateFormat.setLenient(true);
	ParsePosition position = new ParsePosition(0);
	Date date = null;
	for (int i = 0; i < patterns.length; i++) {
	    position.setIndex(0);
	    position.setErrorIndex(-1);
	    dateFormat.applyPattern(patterns[i]);
	    date = dateFormat.parse(dateTimeString, position);
	    if (date != null) {
		break;
	    }
	}

	if (date == null) {
	    StringBuffer buffer = 
		new StringBuffer(dateTimeString + 
				 " - invalid date/time. " + 
				 "Acceptable formats:\n");

	    for (int i = 0; i < patterns.length; i++) {
		buffer.append(patterns[i] + "\n");
	    }
	    throw new ParseException(new String(buffer), 0);
	}
	return date.getTime();
    }


    /** Return a formatted time string corresponding to current time. */
    static String timeString(long t) {
	return DateFormat.getDateTimeInstance().format(new Date(t));
    }


    /** Look for existing PortalConnection. 
	Return null if connection not found. */
    synchronized PortalConnection findPortalConnection(InetAddress remoteAddr) {

	Iterator 	it = _connections.iterator();

	while (it.hasNext())
	{		// Look for existing PortalConnection
	    PortalConnection conn = (PortalConnection)(it.next());
	    if (conn.getInetAddress().equals(remoteAddr))
	    {
		_log4j.debug("Found existing connection to " +
			      remoteAddr.getHostName());
		return conn;
	    }
	}
	// Couldn't find connection with specified address
	return null;
    }

    /** Get the PortalConnection for a given InetAddress */
    synchronized PortalConnection getPortalConnection(InetAddress addr,
						      InetAddress primaryAddr)
    {
	PortalConnection conn = findPortalConnection(addr);
	if (conn != null) {
	    // Found the connection
	    return conn;
	}

	_log4j.debug("Creating new PortalConnection for " + addr);


	// If got here, there's no such PortalConnection (yet)
	conn = new PortalConnection(addr, primaryAddr, this);

	_connections.add(conn);
	_log4j.debug("Got new connection from " + addr.getHostAddress() +
		     "  total = " + _connections.size());
	return(conn);
    }

    /** Notify PortalConnections that network link has come up/down */
    public void nodeLinkNotify(InetAddress remoteAddr, boolean connected)
    {
	_log4j.debug("Portal.nodeLinkNotify(" + remoteAddr.toString() + ", " +
		     (connected ? "ON)" : "OFF)"));

	PrimaryLink link = (PrimaryLink )_primaryLinks.get(remoteAddr);
	if (link == null) {
	    // This node is not being managed by portal
	    return;
	}


	// Set connection state
	link.setConnected(connected);


	PortalConnection conn = findPortalConnection(remoteAddr);

	if (conn != null) {
	    if (!connected && _exitWhenDone) {
		while (conn.isBusy()) {
		    try {
			Thread.sleep(1000);
		    } catch (InterruptedException e) {
		    }
		}
		System.exit(0);
	    }

	    conn.nodeLinkNotify(connected);

	    if (!connected) {
		_nodeWorker.disconnect();
	    }
	}
    }


    /** Return true if portal has network connection to specified 
	primary node. */
    public boolean primaryLinkConnected(InetAddress address) 
	throws RemoteException, Exception {

	PrimaryLink link = (PrimaryLink )_primaryLinks.get(address);
	if (link == null) {
	    throw new Exception(address + " not a primary node");
	}

	return link.connected();
    }




    /** NodeConnectedWorker runs in a thread launched when a connection
	with node has been established. Note that the notifying node
	is assumed to be a lease manager (surface) node!
	Call appropriate PortalConnection and wait for disconnect message
    */
    class NodeConnectedWorker extends Thread
    {
	Socket _socket = null;
	boolean _wirelessNode;
	String _name="unknown";

	public NodeConnectedWorker(Socket s, String name){
	    this(s);
	    _name=name;
	}
	public NodeConnectedWorker(Socket s) 
	{
	    _socket = s;
	}

	public void run()
        {
	    long nextConnectTime = 0;

	    // Get portal connection, which must be 'wireless', since we
	    // got a message from the node via socket
	    PortalConnection conn = getPortalConnection(_socket.getInetAddress(), 
							_socket.getInetAddress());

	    try
	    {
		// Set a timeout so we don't hang forever
		_socket.setSoTimeout(SO_TIMEOUT);

		// Node writes one byte to the socket to establish the fact
		// that it's connected.  Read it and throw it away.
		InputStream instream = _socket.getInputStream();
		int b = instream.read();

		// Since this is a new session, create a new
		// PortalSession and give it to the PortalConnection.
		// The PortalSession is part of the profiler that
		// generates portal statistics in the profile log.
		// If the portal session is open, it probably
		// means that the ConnectionWorker.startSession()
		// is not finished.
		// In this case, we should leave the session to finish.
		// the subsequent call to nodeLinkConnected() 
		// will detect that the ConnectedWorker is still
		// active and return.
		PortalSession session=conn.getSession();

		if(session!=null){
		    if(session.status()!=PortalSession.OPEN)

			conn.setSession(new PortalSession(_name));

		}else{
		    conn.setSession(new PortalSession(_name));
		}

		conn.nodeLinkConnected();

		// Note that nodeLinkConnected() spins up a thread
		// and returns here to wait 900 s for the node (and it's subnodes)
		// to finish and write the next connect time to the socket.
		// In reality, this almost always times out...
		// we need a way to get the default interval and still to 
		// nodeLinkDisconnecting even if this times out.
		// Perhaps the node could write that as it's initial message
		// instead of a throw-away int.

		// Wait for node to say the link is going down
		DataInputStream din = new DataInputStream(instream);
		nextConnectTime = din.readLong();
		_log4j.debug("NodeConnectedWorker - got disconnect message.");
		conn.nodeLinkDisconnecting(nextConnectTime);

	    } catch (Exception e) {
		_log4j.error("Exception in NodeConnectedWorker:", e);
	    }

	    // Whether successful or not, try to close the socket
	    if (_socket != null) {
		try
		{
		    _socket.close();
		    
		} catch (Exception e) {
		    _log4j.error("Exception in socket close "+ e);
		}
		_socket = null;
	    }
	}

	/** Close the socket and exit */
	public void disconnect()
        {
	    if (_socket != null) {
		try
		{
		    _socket.shutdownInput();
		    _socket.close();
		} catch (IOException e) {
		    _log4j.error("Exception in NodeConnectedWorker.disconnect() "+ e);
		}
		_socket = null;
	    }
	}
    }


    public static void main(String args[]) {

	boolean publishPackets = false;
	boolean doProfile=true;
	String logDirectory = ".";
	String[] primaryNodeNames = new String[args.length];
	int	numNodes = 0;

	/* Set up a simple configuration that logs on the console.
	   Note that simply using PropertyConfigurator doesn't work
	   unless JavaBeans classes are available on target. 
	   For now, we configure a PropertyConfigurator, using properties
	   passed in from the command line, followed by BasicConfigurator
	   which sets default console appender, etc.
	*/
	Properties p=System.getProperties();
	String dflt_log4j=p.getProperty("siam_home","/mnt/hda/siam")+"/properties/portal.log4j";
	String portal_log4j=p.getProperty("portal.log4j",dflt_log4j);
	PropertyConfigurator.configure(portal_log4j);

	// Exclude profiler logging from stdout appender
	Profiler.excludeAppender("stdout","portal.profiler");

	// Exclude all but portal.profiler logging from profiler appender
	Profiler.excludeExternal("profile","portal.profiler");

	_log4j.info("Start portal at " + System.currentTimeMillis() + 
		    " epoch msec");

	if (System.getSecurityManager() == null) {
	    _log4j.debug("Setting security manager");
	    System.setSecurityManager(new RMISecurityManager());
	}

	long startRetrieveTime = System.currentTimeMillis();
	boolean error = false;
	boolean doWDT=true;
	Vector preferredNodes = new Vector();
	boolean resumeStreams = true;

	// Process command line options
	for (int i = 0; i < args.length; i++) {

	    if (args[i].equals("-publish")) {
		publishPackets = true;
	    }
	    else if (args[i].equals("-all")) {
		// Retrieve all packets
		startRetrieveTime = 0;
	    }
	    else if (args[i].equals("-logdir"))
	    {
		i++;
		if (i >= args.length)
		{
		    _log4j.error("Missing parameter(s)");
		    error = true;
		    continue;
		}
		logDirectory = args[i];
	    }
	    else if (args[i].equals("-start") || args[i].equals("-since")) {
		i++;
		if (i >= args.length) {
		    _log4j.error("Missing parameter(s)");
		    error = true;
		    continue;
		}
		if (args[i].equals("now")) {
		    // Retrieve packets collected from this time onward
		    startRetrieveTime = System.currentTimeMillis();
		}
		else if (args[i].equals("0") || args[i].equals("all")) {
		    // Retrieve all packets
		    startRetrieveTime = 0;
		}
		else {
		    try {
			startRetrieveTime = parseDateTime(args[i]);
		    }
		    catch (ParseException e) {
			_log4j.error(e.getMessage());
			error = true;
			continue;
		    }
		}
		System.out.println("startRetrieveTime=" + startRetrieveTime);
	    }
	    else if (args[i].equals("-clear")) {
		resumeStreams = false;
	    }
	    else if (args[i].equals("-first") && i < args.length-2) {
		// Build vector of preferred node addresses - ALL PREFERRED NODES MUST BE INCLUDED AS A SINGLE
		// ARGUMENT (i.e. multiple nodes enclosed in quotes)
		StringTokenizer tokenizer = new StringTokenizer(args[++i], " ");
		_log4j.debug("process 'preferred' subnodes list: " + args[i]);
		while (tokenizer.hasMoreTokens()) {
		    String nodeName = tokenizer.nextToken();
		    try {
			InetAddress address = InetAddress.getByName(nodeName);
			preferredNodes.add(address);
		    }
		    catch (UnknownHostException e) {
			_log4j.error("Unknown host: " + nodeName);
			error = true;
		    }
		}
	    }
	    else if (args[i].equals("-noprofile")) {
		doProfile=false;
	    }
	    else if (args[i].equals("-nowdt")) {
		doWDT=false;
	    }
	    else if (args[i].equalsIgnoreCase("-help") || args[i].equalsIgnoreCase("--help")) {
		error = true;
	    }
	    else {
		// Last argument is node's IP address
		primaryNodeNames[numNodes++] = args[i];
	    }
	}

	if (error) {
	    System.err.println("Usage: Portal " + 
			       "[option(s)] " + 
			       "[nodeAddress [nodeAddress [...]]]");

	    System.err.println("Options:");
	    System.err.println("-publish     " + 
			       "publish packets to SSDS");
	    System.err.println("-logdir <dir> " + 
			       "log data to directory <dir>");
	    System.err.println("-start time   " + 
			       "retrieve packets newer than date/time");
	    System.err.println("-start 0      " + 
			       "retrieve all packets");
	    System.err.println("-all          " + 
			       "retrieve all packets");
	    System.err.println("-start now    " + 
			       "retrieve packets newer than current date/time");
	    System.err.println("-noprofile      " + 
			       "disable profiling");

	    System.err.println("-clear      " + 
			       "retrieve packets based on '-start' option only, ignore most recent logged time-tags");

	    System.err.println("-nowdt      " + 
			       "disable watchdog timer maintenance *WILL CAUSE REMOTE SYSTEM(S) TO PERIDICALLY REBOOT*");
	    System.err.println("-first 'node(s)'     " + 
			       "process specified subnode(s) first, after processing surface");

	    return;
	}

	try {

	    System.out.println("Constructing Portal...");

	    (new Portal(primaryNodeNames, startRetrieveTime,
			publishPackets, logDirectory,
			doProfile,doWDT, preferredNodes, 
			resumeStreams)).run();

	}
	catch (Exception e) {
	    _log4j.error("Exception: ", e); 
	}
    } /* main */



    /** PrimaryLink keeps track of address and connection state of primary 
	node. */
    class PrimaryLink {

	protected InetAddress _address;

	protected boolean _connected = false;

	PrimaryLink(InetAddress address, boolean connected) {
	    _address = address;
	    _connected = connected;
	}

	/** Return true if link is connected, else false. */
	boolean connected() {
	    return _connected;
	}

	/** Set connected state */
	void setConnected(boolean connected) {
	    _connected = connected;
	}

	/** Return address of node. */
	InetAddress address() {
	    return _address;
	}
	
    }

} /* Portal */








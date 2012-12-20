/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.distributed;

import java.util.Vector;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseDescription;

/**
 * Interface to an ISI Node, which hosts devices.
 * 
 * @author Tom O'Reilly
 */
public interface Node extends RemoteService {

    /** Server name */
    public static final String SERVER_NAME = "node";


    /** Return InetAddress of device service host. */
    public InetAddress host() throws RemoteException, UnknownHostException;

    /** Name of Node service class. */
    public byte[] getName() throws RemoteException;

    /** Unique identifier for Node instance */
    public long getId() throws RemoteException;

    /** Initialize the device. */
    public void initialize() throws RemoteException, InitializeException;

    /** Turn Node power off. */
    public int powerOff() throws RemoteException;

    /** Get configuration of Node ports. */
    public PortConfiguration[] getPortConfiguration() throws RemoteException;

    /** Get specified device service proxy. */
    public Device getDevice(long deviceId) throws RemoteException,
						  DeviceNotFound;

    /** Get all device service proxies. */
    public Device[] getDevices() throws RemoteException;

    /** Shutdown and remove device service from specified port. */
    public byte[] shutdownDeviceService(byte[] commPortName)
	throws RemoteException, PortNotFound, DeviceNotFound;

    /** Scan all ports, load services. */
    public void scanPorts() throws RemoteException;


    /** Load service for specified port. */
    public void scanPort(byte[] commPortName)
	throws RemoteException,
	       PortNotFound, 
	       DeviceNotFound, 
	       IOException, 
	       PortOccupiedException,
	       IOException, 
	       DuplicateIdException;

    /** Scan specified port, load service, optionally specifying source
	of the service. If serviceSource is NULL, service will be loaded
	from source specified by node configuration. If serviceSource is
	"PUCK", service will be loaded from PUCK. Else service will be
	loaded from file named "serviceSource" in node-specified 
	directory. */
    public void scanPort(byte[] commPortName,
			 byte[] serviceSource) 
	throws RemoteException,
	       PortNotFound, 
	       DeviceNotFound, 
	       IOException, 
	       PortOccupiedException,
	       IOException, 
	       DuplicateIdException;

    /** Get device service (if any) associated with specified port. */
    public Device getDevice(byte[] commPortName) 
	throws RemoteException,
	       PortNotFound, DeviceNotFound;

    /** Get array of Node's Port objects. */
    Port[] getPorts() throws RemoteException;

    /** Get array of Node's power switches. */
    PowerSwitch[] getPowerSwitches() throws RemoteException;

    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.
     */
    public DevicePacketSet getDevicePackets(long sensorID, long startTime,
					    long endTime) 
	throws RemoteException, DeviceNotFound,
	       NoDataException;

    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window; only return packets of type specified in typemask parameter.
     */
    public DevicePacketSet getDevicePackets(long sensorID, long startTime,
					    long endTime, int typeMask) 
	throws RemoteException, DeviceNotFound,
	       NoDataException;


    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.  Aggregate numBytes bytes.  Timeout after timeout milliseconds.
     */
    public DevicePacketSet getDevicePackets(long sensorId, long startTime, long endTime,
					       int numBytes, int timeout)
	throws RemoteException, TimeoutException, IllegalArgumentException,
	       DeviceNotFound, NoDataException;

    /**
     * Get DevicePacket objects, from specified sensor, within specified time
     * window.  Filter packets on typeMask.  Aggregate numBytes bytes.
     * Timeout after timeout milliseconds.
     */
    public DevicePacketSet getDevicePackets(long sensorId, long startTime, long endTime,
					       int numBytes, int typeMask, int timeout)
	throws RemoteException, TimeoutException, IllegalArgumentException,
	       DeviceNotFound, NoDataException;


    /** Return true if specified device can supply Summary packets. */
    boolean summarizing(long deviceID) 
	throws RemoteException, DeviceNotFound;

    /** Suspend service (if any) associated with specified port. */
    public void suspendService(byte[] portName) 
	throws RemoteException,
	       PortNotFound, DeviceNotFound;

    /** Resume service (if any) associated with specified port. */
    public void resumeService(byte[] portName) 
	throws RemoteException,
	       PortNotFound, DeviceNotFound;

    /** Resume service (if any) associated with specified port. */
    public void restartService(byte[] portName) 
	throws RemoteException,
	       PortNotFound, DeviceNotFound, InitializeException, Exception;

    /** Get remote serial port. */
    public RemoteSerialPort getRemoteSerialPort(byte[] portName)
	throws RemoteException, UnknownHostException, PortNotFound,
	       PortOccupiedException, IOException;

    /** Get remote serial port with specified timeout in milliseconds. */
    public RemoteSerialPort getRemoteSerialPort(byte[] portName, int timeout)
	throws RemoteException, UnknownHostException, PortNotFound,
	       PortOccupiedException, RangeException, IOException;

    /** Run Node's self-test routine. */
    public int test() throws RemoteException;

    /** Return Location of Node. */
    public Location getLocation() throws RemoteException,
					 UnknownLocationException;

    /** Get Node metadata. */
    public byte[] getMetadata() throws RemoteException;

    /** Get Printable Schedule (use default lookahead) */
    public byte[] getSchedule() throws RemoteException;

    /** Get Printable Schedule, specify lookahead */
    public byte[] getSchedule(long lookAheadSeconds) throws RemoteException;

    /** Get Printable Schedule for a specified device, lookahead */
    public byte[] getSchedule(byte[] port, long lookAheadSeconds)
	throws RemoteException;

    /** Add Schedule */
    public byte[] addSchedule(byte[] port, byte[] scheduleName,
			      byte[] schedule, boolean overwrite) throws RemoteException;

    /** Remove Schedule */
    public byte[] removeSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException;

    /** Suspend Schedule Entry */
    public byte[] suspendSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException;

    /** Resume Schedule Entry */
    public byte[] resumeSchedule(byte[] port, byte[] scheduleName)
	throws RemoteException;

    /** Synchronize a (default sample) schedule entry */
    public byte[] syncSchedule(byte[] port, byte[] scheduleName,
			       long delayMillis) throws RemoteException;

    /** Register to receive notification of node events. */
    public void addEventCallback(NodeEventCallback callback)
	throws RemoteException;



    /**
     * Request a lease of the Node's primary comms medium
     * 
     * @param leaseMillisec
     *            lease period in milliseconds
     *@param clientNote 
     *            note attached by client
     * @return leaseID for use with renewLease(), terminateLease(). Will always
     *         be >= 1.
     */
    public int establishLease(long leaseMillisec, byte[] clientNote) 
	throws RemoteException,
	       LeaseRefused;


    /** 
	Request a lease of either the primary or auxillary comms medium.
    */
    public int establishLease(long leaseMillisec, byte[] clientNote,
			      boolean usePrimary) 
	throws RemoteException,
	       LeaseRefused;

    /**
     * Renew a lease with the Node's primary comms medium
     * 
     * @param leaseID
     *            lease ID returned by establishLease()
     * @param leaseMillisec
     *            lease period in milliseconds
     */
    public void renewLease(int leaseID, long leaseMillisec)
	throws RemoteException, LeaseRefused;



    /**
     * Renew a lease with the Node's primary or secondary comms medium
     * 
     * @param leaseID
     *            lease ID returned by establishLease()
     * @param leaseMillisec
     *            lease period in milliseconds
     */
    public void renewLease(int leaseID, long leaseMillisec, 
			   boolean userPrimary)
	throws RemoteException, LeaseRefused;


    /**
     * Terminate the session with the communications link.
     * 
     * @param leaseID
     *            lease ID returned by establishLease()
     */
    public void terminateLease(int leaseID) 
	throws RemoteException, LeaseRefused;


    /**
     * Terminate the session with the priamry or auxillary communications link.
     * 
     * @param leaseID
     *            lease ID returned by establishLease()
     */
    public void terminateLease(int leaseID, boolean usePrimary) 
	throws RemoteException, LeaseRefused;


    /** Terminate all leases with notations that match the specified note. 
	If noteMatch is null, then terminate all leases. Returns the number
	of leases that were terminated.
    */
    public int terminateLeases(byte[] noteMatch) 
	throws RemoteException, LeaseRefused;


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
	throws RemoteException;

    /** Return byte string with node health/status information. */
    public byte[] getStatus(boolean logPacket) throws RemoteException;

    /** Shutdown all services and exits the SIAM application. */
    public void exitApplication() throws RemoteException;

    /**
     * Power up specified port and enable the comms if a service is not already
     * using it
     */
    public void powerUpPort(byte[] commPortName, int currentLimit)
	throws RemoteException, PortOccupiedException, PortNotFound,
	       RangeException, NotSupportedException;

    /**
     * Power down specified port and enable the comms if a service is not
     * already using it
     */
    public void powerDownPort(byte[] commPortName) 
	throws RemoteException,
	       PortOccupiedException, PortNotFound;

    /** Get list of immediate subnode IP addresses. */
    public InetAddress[] getSubnodes() throws RemoteException;

    /** Get list of immediate subnode objects. */
    public Subnode[] getSubnodeObjects() throws RemoteException;

    /** Remove specified subnode from list. */
    public void removeSubnode(InetAddress address) throws RemoteException, Exception;

    /** Get list of all active leases managed by the node. */
    public LeaseDescription[] getLeases(boolean usePrimary) 
	throws RemoteException;



    /**
     * Get Vector of node properties; each Vector element consists of 
     byte array with form "key=value".
     */
    public Vector getProperties() throws RemoteException;


  /** Tell node to execute a Linux command.
      This method doesn't return unil the command has completed,
      or until timeoutSec expires	*/
    public byte[] runCommand(byte[] cmd, int timeoutSec)
        throws RemoteException, IOException, TimeoutException;


    /** Put node and its devices into "safe" mode. */
    public void enterSafeMode() throws RemoteException, Exception;
    public void enterSafeMode(long timeoutSec) throws RemoteException, Exception;

    /** Return from "safe" mode; resume normal operations. */
    public void resumeNormalMode() throws RemoteException, Exception;

    /** Append annotation to node's data stream. */
    public void annotate(byte[] annotation) throws RemoteException;

    /** Shutdown all services and exits the SIAM application. */
    public void exitApplication(boolean doSafemode, 
				boolean doHalt,
				boolean enableBackups,
				boolean doNotify,
				boolean recursive,
				int quitDelaySec,
				int haltDelaySec,
				String msg)
	throws RemoteException,Exception;


    /** Set value of specified node service properties. Input consists
	of one or more 'key=value' pairs. Each key=value pair is separated from
	the next pair by newline ('\n'). */
    public void setProperties(byte[] propertyStrings)
	throws RemoteException, InvalidPropertyException;


    /** Get basic information about node and its subnodes */
    public NodeInfo getNodeInfo() 
	throws Exception, RemoteException;

    /** Prepare node for telemetry retrieval over low-bandwidth link; combines
	several operations into one RMI method invocation. */
    public NodeSessionInfo startSession(boolean renewWDT, 
					byte[] initScript,
					int initScriptTimeoutSec)
	throws RemoteException, Exception;


    /** Called when IP link to shore is connected */
    public void shoreLinkUpCallback(String interfaceName, 
				    String serialName, 
				    InetAddress localAddress,
				    InetAddress remoteAddress)
	throws RemoteException, Exception;


    /** Called when IP link to shore is disconnected */
    public void shoreLinkDownCallback(String interfaceName,
				      String serialName,
				      InetAddress localAddress,
				      InetAddress remoteAddress)
	throws RemoteException, Exception;


    /* Added 3 Nov 2008, rah, to interface to InstrumentRegistry */

    /** Find an Instrument in the InstrumentRegistry by registryName */
    public Instrument lookupService(String registryName) throws RemoteException;

    /** Get InstrumentRegistry status */
    public byte[] instrumentRegistryStatus() throws RemoteException;
	
	/** Reload log4j configuration */
	public void readLog4jConfig() throws RemoteException;
}

/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.moos.deployed;

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
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import gnu.io.CommPortIdentifier;

import org.mbari.siam.distributed.leasing.LeaseDescription;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.distributed.devices.Power;
import org.mbari.siam.distributed.devices.Environmental;
import org.mbari.siam.distributed.leasing.LeaseManager;
import org.mbari.siam.utils.ThreadUtility;
import org.mbari.siam.moos.deployed.MOOSNetworkManager;
import org.mbari.siam.utils.SyncProcessRunner;
import org.mbari.siam.utils.NodeProbe;

import org.apache.log4j.Logger;
import org.mbari.siam.core.NodeManager;
import org.mbari.siam.core.NodeService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.PortManager;
import org.mbari.siam.core.PowerListener;
import org.mbari.siam.core.DevicePort;
import org.mbari.siam.moos.deployed.MOOSWDTManager;
import org.mbari.siam.core.WDTManager;
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
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.PowerSwitch;
import org.mbari.siam.distributed.RemoteSerialPort;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.UnknownLocationException;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.NodeSessionInfo;
import org.mbari.siam.distributed.NodeInfo;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.MOOSNodeInfo;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;
import org.mbari.siam.distributed.devices.Power;
import org.mbari.siam.devices.serialadc.PowerCan;


/**
 * MOOSNodeService implements the MOOSNode interface, and manages multiple Devices
 * (including Instruments).
 * 
 * @author Bob Herlien
 */
public class MOOSNodeService extends NodeService
    implements MOOSNode, PowerListener
{
    // CVS revision 
    private static String _versionID = "$Revision: 1.5 $";

    private static Logger _log4j = Logger.getLogger(MOOSNodeService.class);

    public static final String WDT_FILENAME = "/proc/watchdog";

    /** Millseconds it takes for CPU to wake up completely (approximately). */
    static final int CPU_WAKEUP_MSEC = 3000;

    /** Millseconds to lease CPU after waking it via wakeup() method.  */
    static final int CPU_KEEPAWAKE_MSEC = 60000;

    protected EnvironmentDiagnostics _diagnostics = null;

    protected MOOSWDTManager _moosWDTManager = null;    

    /** Create the NodeService. */
    MOOSNodeService(PortManager portManager, String parentHost)
	throws RemoteException, MissingPropertyException,
	       InvalidPropertyException, IOException {
	super(portManager, parentHost);

	_diagnostics = 
	    new EnvironmentDiagnostics(this);


	// Get reason for latest reset
	int resetCode = 
	    MOOSWDTManager.readProcRegister(MOOSWDTManager.RESET_REASON_REGISTER);
	NodeManager.getInstance().logMessage("Last reset code: " + resetCode);

    }


    /** Return Environmental service proxy. Throws DeviceNotFound if
     environmental service is not found. */
    protected Environmental getEnvironmental() 
	throws DeviceNotFound {
	Vector ports = _portManager.getPorts();
	Environmental environmental = null;
	for (int i=0; i < ports.size(); i++) {
	    DevicePort port = (DevicePort )ports.elementAt(i);
	    if (port._service != null && 
		port._service instanceof Environmental) {
		// Found environmental service
		environmental = (Environmental )port._service;
	    }
	}
	if (environmental == null) {
	    throw new DeviceNotFound("Environmental service not found"); 
	}
	return environmental;
    }


    /** Run diagnostic procedure; called through Parent interface */
    public void runDiagnostics(String note) 
	throws Exception {

	// Run environmental diagnostics method
	_diagnostics.run(note);
    }


    /** Get the Status Summary from the Diagnostics */
    public String getDiagnosticsStatusSummary() {
	return(_diagnostics.getStatusSummary());
    }



    /** Get basic information about node and its subnodes */
    public NodeInfo getNodeInfo() throws Exception, RemoteException {

	return new MOOSNodeInfo(InetAddress.getLocalHost(),
				getId(), 
				getSubnodeObjects(), 
				NodeManager.getInstance().getNodeStartTime(),
				NodeManager.getInstance().getNetworkSwitchAddress());
    }

    /** Disables the watchdog timer. */
    public void quitApplication(int delaySec) throws RemoteException {
	disableWDT();
  super.quitApplication(delaySec);
    }

    public void disableWDT() {
  _moosWDTManager = (MOOSWDTManager)MOOSWDTManager.getInstance();
  _moosWDTManager.disableWDT();	
    }    

    protected void pingWDT(NodeSessionInfo sessionInfo)
    {
	_log4j.debug("MOOSNodeService.pingWDT()");

	// Reset watchdog timer
	try {
	    sessionInfo._wdtStatus = (new String(renewWDT())).getBytes();
	}
	catch (Exception e) {
	    sessionInfo._wdtError = true;
	    sessionInfo._wdtStatus = e.getMessage().getBytes();
	}
    }

    /** Get status of power port associated with specified comm port. 
	Throws NotSupportedException if no power port is 
	associated with specified comm port. */
    synchronized public DpaPortStatus getDpaPortStatus(byte[] pName) 
	throws NotSupportedException, DeviceNotFound {
	String portName = new String(pName);
	// Get reference to this port's power port.
	Vector ports = NodeManager.getInstance().getPortManager().getPorts();
	boolean found = false;
	for (int i = 0; i < ports.size(); i++) {
	    DevicePort port = (DevicePort )ports.elementAt(i);
	    PowerPort powerPort = port.getPowerPort();

	    if (port.getPortName().equals(portName)) {
		if (powerPort == null) {
		    throw new NotSupportedException("No power port");
		}
		if (!(powerPort instanceof SidearmPowerPort)) {
		    throw new NotSupportedException("Not a SidearmPowerPort");
		}
		SidearmPowerPort saPowerPort = (SidearmPowerPort)powerPort;

		if (port._service != null) {
		    return new DpaPortStatus(i, saPowerPort, 
					     port.getDeviceService().getName(),
					     port.getDeviceService().getId(),
					     port.hasPuck());
		}
		else {
		    return new DpaPortStatus(i, saPowerPort, port.hasPuck());
		}

	    }
	}
	// If we get here, then specified port was not found.
	throw new DeviceNotFound(portName);
    }


    /**
       Report status of all DPA ports 
     */
    synchronized public DpaPortStatus[] getDpaPortStatus() {
	Vector ports = NodeManager.getInstance().getPortManager().getPorts();
	boolean found = false;
	int nDpaPorts = 0;
	for (int i = 0; i < ports.size(); i++) {
	    DevicePort port = (DevicePort )ports.elementAt(i);
	    if (port.getPowerPort() != null &&
		port.getPowerPort() instanceof SidearmPowerPort) {
		nDpaPorts++;
	    }
	}

	_log4j.debug("Found " + nDpaPorts + " DPA ports");

	DpaPortStatus[] portStatus = new DpaPortStatus[nDpaPorts];
	int nPort = -1;
	for (int i = 0; i < ports.size(); i++) {
	    DevicePort port = (DevicePort)ports.elementAt(i);
	    _log4j.debug("Check port " + i);
	    if (port.getPowerPort() != null &&
		port.getPowerPort() instanceof SidearmPowerPort) {
		nPort++;
		_log4j.debug("Port " + i + " has powerPort");
		SidearmPowerPort powerPort = 
		    (SidearmPowerPort)port.getPowerPort();

		if (port._service != null) {
		    portStatus[nPort] = 
			new DpaPortStatus(i, powerPort, 
					  port.getDeviceService().getName(),
					  port.getDeviceService().getId(),
					  port.hasPuck());
		}
		else {
		    portStatus[nPort] = new DpaPortStatus(i, powerPort,
							  port.hasPuck());
		}


	    }
	}
	return portStatus;
    }

    /** Keep watchdog timer (WDT) from expiring */
    public byte[] renewWDT()
	throws RemoteException, IOException, FileNotFoundException{

	_log4j.info("renewWDT()");
	File wdt = new File(WDT_FILENAME);
	BufferedWriter bw = new BufferedWriter(new FileWriter(wdt));
	bw.write("feed watchdog");
	bw.flush();
	bw.close();


	try {
	    _log4j.debug("renewWDT() - sleep before reading...");
	    Thread.sleep(500);
	}
	catch (InterruptedException e) {
	}
	byte[] result = readWDT();
	_wdtStatus = _dateFormat.format(new Date(System.currentTimeMillis())) +
	    ";  " + new String(result);

	_log4j.info(_wdtStatus);
	return result;
    }

    /** Read watchdog timer (WDT) state */
    public byte[] readWDT()
	throws RemoteException, IOException,FileNotFoundException
    {
	File wdt = new File(WDT_FILENAME);
	char[] buf = new char[128];
	FileReader reader = new FileReader(wdt);

	for (int nTry = 0; nTry < 3; nTry++) {

	    try {
		Thread.sleep(1000);
	    }
	    catch (InterruptedException e) {
	    }

	    if (reader.ready()) {
		int nchar = reader.read(buf, 0, buf.length);
		if (nchar == -1) {
		    _log4j.warn("readWDT() - EOF!");
		}
		else {
		    reader.close();
		    return (new String(buf, 0, nchar)).getBytes();
		}
	    }
	    else {
		_log4j.warn("readWDT() - not ready");
	    }

	}

	reader.close();
	return ("NO CHARS IN " + WDT_FILENAME).getBytes();

    }


    /** Send signal to wakeup specified node. */
    public void wakeupNode(InetAddress node) 
	throws IOException {
	_log4j.info("wakeupNode(): " + node.getHostName());
	_networkManager.wakeupNode(node);

	// Wait a bit for target node to fully wake up
	try {
	    _log4j.info("Wait " + CPU_WAKEUP_MSEC/1000 + 
			" sec for node to wake");

	    Thread.sleep(CPU_WAKEUP_MSEC);
	}
	catch (InterruptedException e) {
	    _log4j.error("wakeupNode() - InterruptedException");
	}

	_log4j.info("Keep node " + node + " awake for " + 
		    CPU_KEEPAWAKE_MSEC/1000 + " sec...");

	_networkManager.keepNodeAwake(node, CPU_KEEPAWAKE_MSEC);
    }

    /** Send signal to wakeup all nodes. */
    public void wakeupAllNodes() 
	throws IOException {
	_log4j.info("wakeupAllNodes()");
	_networkManager.wakeupAllNodes();
    }

}



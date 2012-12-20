/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;
import java.util.Vector;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.NotBoundException;
import java.rmi.AccessException;
import java.rmi.registry.LocateRegistry;
import javax.rmi.PortableRemoteObject;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.distributed.portal.PortalProxy;
import org.mbari.siam.distributed.AuthenticationException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DevicePacketStream;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Authentication;
import org.mbari.siam.distributed.portal.UnknownConfiguration;
import java.rmi.RemoteException;

public class PortalTest 
    extends PortableRemoteObject
    implements PortalProxy {

    private static Logger _logger = Logger.getLogger(PortalTest.class);

    public PortalTest(String publicPortalAddress,
		      String privatePortalAddress,
		      String remoteNodeAddress) throws RemoteException {

	_logger.debug("PortalTest()");
    }

    /** Name of portal host */
    public String getName() {
	return "Portal Test";
    }

    /** Name of portal host */
    public String getPortalHostName() {
	return "Portal Test Host";
    }

    /** Unique identifier of platform */
    public long getId() { 
	return 9999;
    }

    /** Returns true if communication link is up. */
    public boolean connected() {
	return true;
    }

    /** Status of platform. */
    public int getStatus() {
	return -1;
    }

    /** Returns true of next communication link. */
    public long nextConnectTime() {
	return 0;
    }

    /** Get DevicePacketStream for all sensors. */
    public DevicePacketStream getDevicePacketStream(Authentication auth) 
	throws AuthenticationException {
	throw new AuthenticationException("Not implemented");
    }

    /** Get DevicePacketStream for specified sensor. */
    public DevicePacketStream getDevicePacketStream(long sensorID, 
						    Authentication auth) 
	throws DeviceNotFound, AuthenticationException {
	throw new AuthenticationException("Not implemented");
    }

    /** Return port information for node */
    public Port[] getPortConfiguration() 
	throws RemoteException, UnknownConfiguration {
	throw new UnknownConfiguration("Not implemented");
    }

    /** Return QueuedCommands which haven't been sent to platform yet. */
    public Vector getQueuedCommands() {
	return null;
    }


    /** Return QueuedCommands which have already been sent to platform. */
    public Vector getSentCommands() {
	return null;
    }


    /** Notify portal that link to remote node is "up". This method
     will likely be replaced by Rendezvous mechanisms. */
    public void nodeLinkConnected() {
	_logger.debug("nodeLinkConnected()");
    }


    /** Notify portal that link to remote node is about to be 
	disconnected. */
    public void nodeLinkDisconnecting(long nextConnectTime) {
	_logger.debug("nodeLinkDisconnecting()");
    }

    public static void main(String args[]) {

	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	if (System.getSecurityManager() == null) {
	    System.out.println("Setting security manager");
	    System.setSecurityManager(new RMISecurityManager());
	}

	boolean error = false;
	if (args.length < 3) {
	    _logger.error(args.length + " arguments: " +
			       "at least 3 arguments required");
	    error = true;
	}

	if (error) {
	    System.err.println("Usage: PortalTest " + 
			       "publicAddress privateAddress remoteNode");

	}

	// Last three arguments are addresses
	String publicPortalAddress = args[args.length-3];
	String privatePortalAddress = args[args.length-2];
	String remoteNodeAddress = args[args.length-1];

	// Start rmiregistry
	try {
	    System.out.println("Starting registry...");
	    LocateRegistry.createRegistry(1099);
	    System.out.println("Started registry");
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    _logger.info(e.getMessage());
	}

	try {

	    System.out.println("Constructing PortalTest...");

	    PortalTest server = 
		new PortalTest(publicPortalAddress, privatePortalAddress,
			       remoteNodeAddress);

	    String url = Portals.portalURL(publicPortalAddress);
	    _logger.info("Binding PortalTest to " + url + "...");
	    _logger.debug("Naming.bind(" + url + ")");
	    Naming.bind(url, server);
	    _logger.info("PortalTest is bound to " + url);
	}
	catch (Exception e) {
	    _logger.error("Exception: ", e); 
	    e.printStackTrace();
	}
    } 
}


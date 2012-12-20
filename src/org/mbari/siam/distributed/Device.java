// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Interface to a device that can be remotely controlled.
 * 
 * @author Tom O'Reilly
 */
public interface Device extends Remote {

    /** Metadata update modes */
    public static final int INSTRUMENT_STATE = 0x1;

    public static final int SERVICE_XML = 0x2;

    public static final int SERVICE_ATTRIBUTES = 0x4;

    public static final int SERVICE_PROPERTIES = 0x8;

    public static final int SERVICE_CACHE = 0x10;

    public static final int MDATA_ALL = 0x1F;

    /** Device status; all is well */
    public final static int OK = 0;

    /** Device status; error occurred */
    public final static int ERROR = 1;

    /** Device status; initializing */
    public final static int INITIAL = 2;

    /** Device status; shutting down */
    public final static int SHUTDOWN = 3;

    /** Device status; sampling disabled */
    public final static int SUSPEND = 4;

    /** Device status; acquiring sample */
    public final static int SAMPLING = 5;

    /** Device status; sleeping */
    public final static int SLEEPING = 6;

    /** Device status; safe mode */
    public final static int SAFE = 7;

    /** Device status; unknown */
    public final static int UNKNOWN = -1;

    /** Name of device service class. */
    public byte[] getName() throws RemoteException;

    /** Unique identifier for device instance */
    public long getId() throws RemoteException;

    /** Run the device. */
    public void prepareToRun() 
	throws RemoteException, InitializeException, InterruptedException;

    /** Turn device power off. */
    public int powerOff() throws RemoteException;

    /** Turn device power on. */
    public int powerOn() throws RemoteException;

    /** Get device status. */
    public int getStatus() throws RemoteException;

    /** Return sampling error count */
    public int getSamplingErrorCount() throws RemoteException;

    public int getSamplingCount() throws RemoteException;

    public int getSamplingRetryCount() throws RemoteException;

    /** Run device's self-test routine. */
    public int test() throws RemoteException;

    /** Return parent Device. */
    public Device getParent() throws NoParentException, RemoteException;

    /** Return child Devices. */
    public Device[] getChildren() throws NoChildrenException, RemoteException;

    /** Return InetAddress of device service host. */
    public InetAddress host() throws RemoteException, UnknownHostException;

    /** Return Location of device. */
    public Location getLocation() throws RemoteException,
					 UnknownLocationException;

    /** Get device metadata packet. */
    public MetadataPacket getMetadata(byte[] cause, int components,
				      boolean logPacket) 
	throws RemoteException;

    /** Get name of host port for this device. */
    public byte[] getCommPortName() throws RemoteException;

    /** Get framework version. */
    public byte[] getFrameworkVersion() throws RemoteException;

    /**
     * Put service in SUSPEND state. Release resources (e.g. serial port) for
     * use by other applications.
     */
    public void suspend() throws RemoteException;

    /**
     * Put service in OK state. Re-acquire resources (e.g. serial port).
     */
    public void resume() throws RemoteException;


    /**
       Shut down the service; release all associated resources; returns 
       optional human-readable message 
    */
    public byte[] shutdown() throws RemoteException;
}


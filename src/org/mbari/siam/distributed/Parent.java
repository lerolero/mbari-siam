// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.util.EventObject;
import java.rmi.RemoteException;
import org.mbari.siam.distributed.Location;

/**
 * Parent represents the processing/management environment into which an
 * Instrument is installed. Parent exposes some functionality of a 
 * an implementation (e.g. NodeService) to Instrument.
 * 
 * @author Kent Headley
 */

public interface Parent {

    /** Return this parent's ISI ID. */
    public long getParentId() throws RemoteException;

    /** Return location of specified device. */
    public Location getLocation(long deviceID) throws RemoteException;

    /**
     * Run parent's diagnostics. Throws exception if diagnostics cannot be run.
     */
    public void runDiagnostics(String note) throws Exception, RemoteException;

    /** Return software version information for parent environment. */
    public String getSoftwareVersion();

    /** Publish specified event. */
    public void publish(EventObject event);

    /** Request power from the parent; return true if available, false if
	unavailable. */
    public boolean powerAvailable(int milliamp);
}

// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.rmi.RemoteException;
import java.util.Vector;
import org.mbari.siam.core.ScheduleTask;

/**
 * An Instrument is a Device that can acquire data.
 * 
 * @author Tom O'Reilly
 */
public interface Instrument extends Device {

    
    /** Set instrument's internal clock to specified time. 
     DEPRECATED - use setClock() with no arguments. */
    public void setClock(long millisec) 
	throws NotSupportedException, RemoteException;


    /** Set instrument's internal clock to current time. */
    public void setClock()
	throws NotSupportedException, RemoteException;

    /**
     * Acquire SensorDataPacket, optionally write it to Instrument's 
     * log file, and return
     * it to caller.
     */
    public SensorDataPacket acquireSample(boolean logPacket) 
	throws RemoteException, NoDataException;


    /** Get the most recently logged SensorDataPacket and return it to 
	caller. */
    public SensorDataPacket getLastSample() throws RemoteException,
						   NoDataException;

    /** Append annotation to device data stream. */
    public void annotate(byte[] annotation) throws RemoteException;

    /** Return byte-string representation of instrument sampling schedule. */
    public byte[] getSampleSchedule() throws RemoteException;

    /** Return byte-string representation of instrument schedules. */
    public Vector getSchedules() throws RemoteException;

    /** Get all packets having time-tag within specified range. */
    public DevicePacketSet getPackets(long startTime, long stopTime)
	throws RemoteException, NoDataException;

    /** Get all packets having time-tag within specified range, filtered
     by specified filters; exclude "stale" packets older than service's
    "data shelf life". */
    public DevicePacketSet getPackets(long startTime, long stopTime, 
				      PacketFilter[] filters, 
				      boolean excludeStale)
	throws RemoteException, NoDataException;
 
    /** Add default packet filters for data retrieval. */
    public void addDefaultPacketFilters(PacketFilter[] filters) 
	throws RemoteException;

    /** Clear default packet filters for data retrieval. */
    public void clearDefaultPacketFilters()
	throws RemoteException;

    /** Get default packet filters for data retrieval. */
    public PacketFilter[] getDefaultPacketFilters()
	throws RemoteException;

    /** Get value of specified instrument service property. */
    public byte[] getProperty(byte[] key) throws RemoteException,
						 MissingPropertyException;


    /** Set value of specified instrument service properties. First argument
	consists of one or more 'key=value' pairs. Each key=value pair is 
	separated from the next pair by newline ('\n'). Note that second
	argument is currently unused. */
    public void setProperty(byte[] keyEqualsValueStrings, byte[] unused) 
	throws RemoteException,
	       InvalidPropertyException;

    /**
     * Get Vector of instrument properties; each Vector element consists of 
     byte array with form "key=value".
     */
    public Vector getProperties() throws RemoteException;


    /** Cache service properties on the "instrument host" node, 
	such that current service property values will be restored next time 
	service is created on current port of current node. */
    public void cacheProperties(byte[] note) throws RemoteException, Exception;

    /** Clear properties cache. */
    public void clearPropertiesCache(byte[] note) 
	throws RemoteException, Exception;


    /** Get diagnostics message from device's port and optionally log it. */
    public byte[] getPortDiagnostics(boolean logPacket) throws RemoteException;

    /** Reset port diagnostics. */
    public void resetPortDiagnostics() throws RemoteException;

    /**
     * Get diagnostics summary message from device's port and optionally 
     log it.
     */
    public byte[] getPortDiagnosticsSummary(boolean logPacket)
	throws RemoteException;

    /** Get instrument's data parser. */
    public PacketParser getParser() throws RemoteException,
					   NotSupportedException;

    /** Enable generation of data summary. */
    public void enableSummary() throws RemoteException;

    /** Disable data summary generation. */
    public void disableSummary() throws RemoteException;

    /** Return true if summary generation is enabled. */
    public boolean summaryEnabled() throws RemoteException;

}

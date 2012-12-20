/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.Arrays;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.FilteredDeviceLogIF;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.TimeoutException;
import org.apache.log4j.Logger;

/** 
 * DevicePacketAggregator implements two new method signatures of getDevicePackets()
 * on behalf of the Node interface.  These method signatures add the following
 * capabilities to getDevicePackets
 * <ol>
 * <li> It aggregates DevicePacketSets returned from Instrument or NodeManager._log,
 * creating larger DevicePacketSets that can be transferred more efficiently over 
 * the telemetry system.  The amount of aggregation is specified by the <b>numBytes</b>
 * parameter to the two new methods.
 * <li> It provides a <b>timeout</b> parameter to these methods.  This allows the calling
 * object (e.g. Portal) to limit the amount of time waiting for non-responsive Instruments,
 * and thus allows it (the Portal) to move on to collecting data from the other Instruments
 * and subnodes.
 * </ol>
 *
 * The first capability is implemented by repeatedly calling the data source (as specified
 * by the isiId) until we get at least <b>numBytes</b> bytes.  Note that the data source
 * (e.g. Instrument or NodeManager log) returns DevicePacketSets aggregated using its own
 * native DevicePacketSet size.  Thus the DevicePacketAggregator can return a DevicePacketSet
 * containing a number of bytes that exceeds the request by one "native" DevicePacketSet
 * from the isiId.
 *<p>
 * The second capability is implemented using threads.  The calling method simply passes
 * the request to a worker thread, and waits for the worker thread to signal that it's
 * done.  It uses Object.wait(timeout) to accomplish this, thus implementing the timeout
 * function.  To guard against the possibility that a "stuck" Instrument can block the
 * entire system, we use a worker thread for each ISI ID.  Thus a "stuck" Instrument can
 * block later requests on that same Instrument, but will not block requests for a
 * different Instrument.
 *<p>
 * This class implements ServiceListener.  The reason is that we need to be notified
 * when a user does a shutdownPort.  In that case, we destroy the associated
 * DevicePacketWorker.  If the user then does a scanPort and later calls
 * getDevicePackets() for that isiId, we'll generate a new worker with the new
 * Device reference that resulted from the scanPort.
 */

public class DevicePacketAggregator implements ServiceListener
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(DevicePacketAggregator.class);

    protected Vector _workers = new Vector();
    protected NodeService _node = null;
    protected boolean _testing = false;
    protected String  _directory = ".";
    protected long _testDelay = 10;

    /** Normal constructor when operating in SIAM
     */
    public DevicePacketAggregator(NodeService node)
    {
	_node = node;

	// Register to hear about events on the node
	EventManager.getInstance().addListener(ServiceListener.class, this);
    }

    /** Test constructor, used by tests.Aggregator
     */
    public DevicePacketAggregator(String directory, long delay)
    {
	_directory = directory;
	_testDelay = delay;
	_testing = true;

	// Register to hear about events on the node
	EventManager.getInstance().addListener(ServiceListener.class, this);
    }

    /** Get a DevicePacketSet.
	@param  isiId = Device id
	@param  startKey = Earliest time (key) to request
	@param  endKey = End time of request
	@param  numBytes = Requested size of DevicePacketSet, in bytes
	@param  typeMask = Mask to filter requested packets.  See DevicePacket.
	@param  timeout = timeout in milliseconds
    */
    public DevicePacketSet getDevicePackets(long isiId, long startKey, long endKey,
					       int numBytes, int typeMask, int timeout)
	throws TimeoutException, IllegalArgumentException,
	       DeviceNotFound, NoDataException
    {
	DevicePacketWorker worker = getWorker(isiId);
	
	return(worker.getDevicePackets(startKey, endKey, numBytes, typeMask, timeout));
    }

    /** Get a DevicePacketSet.
	@param  isiId = Device id
	@param  startKey = Earliest time (key) to request
	@param  endKey = End time of request
	@param  numBytes = Requested size of DevicePacketSet, in bytes
	@param  timeout = timeout in milliseconds
    */
    public DevicePacketSet getDevicePackets(long isiId, long startKey, long endKey,
					       int numBytes, int timeout)
	throws TimeoutException, IllegalArgumentException,
	       DeviceNotFound, NoDataException
    {
	return(getDevicePackets(isiId, startKey, endKey, numBytes,
				DevicePacket.ALL_TYPES, timeout));
    }

    /** Creates a DevicePacketSource for this deviceID
     */
    protected DevicePacketSource getDevicePacketSource(long deviceID)
	throws DeviceNotFound, NoDataException
    {
	if (_testing) {
	    try {
		FilteredDeviceLog log = new FilteredDeviceLog(deviceID, _directory);
		return(new DeviceLogTestSource(log, _testDelay));
	    } catch (Exception e) {
		throw new DeviceNotFound("Cannot create DeviceLogTestSource.  " + e);
	    }
	}

	// Is requested device the node itself?
	if (_node.getId() == deviceID) {
	    // Return packets from node log
	    return(new DeviceLogSource(NodeManager.getInstance()._log));
	}

	Device device = null;
	try {
	    device = _node.getDevice(deviceID);
	} catch (RemoteException e) {
	    throw new DeviceNotFound("RemoteException on getDevice()");
	}

	if (!(device instanceof BaseInstrumentService))
	    throw new NoDataException("Device " + deviceID
				      + " is not a BaseInstrumentService");

	return(new DeviceInstrumentSource((BaseInstrumentService)device));

    }

    /** Find the DevicePacketWorker that is servicing this isiId.
	If none exists, create a new one.
    */
    protected synchronized DevicePacketWorker getWorker(long isiId)
	throws DeviceNotFound, NoDataException
    {
	Iterator it = _workers.iterator();
	DevicePacketWorker worker;

	while (it.hasNext()) {
	    worker = (DevicePacketWorker)(it.next());
	    if (worker.equals(isiId))
	    {
		if (worker.verify())
		    return(worker);
		else
		{
		    worker.exitWorker();
		    _workers.remove(worker);
		    _log4j.debug("worker.verify() failed for ISI ID " + isiId +
				 ".  Removed.  Total number of workers = " + _workers.size());
		}
	    }
	}

	worker = new DevicePacketWorker(isiId, getDevicePacketSource(isiId));
	_workers.add(worker);
	worker.start();
	_log4j.debug("Added worker for ISI ID " + isiId +
		     ".  Total number of workers = " + _workers.size());

	return(worker);
    }

    /** Tell all DevicePacketWorkers to exit */
    public synchronized void exitWorkers()
    {
	for (Iterator it = _workers.iterator(); it.hasNext(); ) {
	    DevicePacketWorker worker = (DevicePacketWorker)(it.next());
	    worker.exitWorker();
	}
	_workers = new Vector();
    }

    /** serviceInstalled method for the ServiceListener interface.
        Action performed when service installed.  We ignore this event.
    */
    public void serviceInstalled(ServiceEvent e)
    {
    }

    /** serviceRemoved method for the ServiceListener interface.
	Action performed when service removed.  We look for a 
	DevicePacketWorker for the corresponding ISI ID, and shut it
	down if it exists.
    */
    public synchronized void serviceRemoved(ServiceEvent e) {

	_log4j.info("serviceRemoved()");

	long isiId = e.getServiceID();
	Iterator it = _workers.iterator();
	DevicePacketWorker worker;

	while (it.hasNext()) {
	    worker = (DevicePacketWorker)(it.next());
	    if (worker.equals(isiId)) {
		worker.exitWorker();
		_workers.remove(worker);
		_log4j.info("Removed worker for ISI ID " + isiId +
			    ".  Total number of workers = " + _workers.size());
	    }
	}
	_log4j.info("done with serviceRemoved()");
    }

    /** serviceRequestComplete method for the ServiceListener interface.
        Action performed when service installed.  We ignore this event.
    */
    public void serviceRequestComplete(ServiceEvent e)
    {
    }


    /** serviceSampleLogged method for the ServiceListener interface.
        Action performed when service logs a device sample.  We ignore this event.
    */
    public void serviceSampleLogged(LogSampleServiceEvent e) {
    }


    /** Worker thread that actually fetches the DevicePacketSet data.
	One DevicePacketWorker is created for each isiId that has received
	a getDevicePacket() request
    */
    protected class DevicePacketWorker extends Thread
    {
	protected long  _isiId;
	protected DevicePacketSource _pktSource;
	protected Vector _requests = new Vector();
	protected boolean _runIt = true;

	/** Create a worker for the given isiId */
	public DevicePacketWorker(long isiId, DevicePacketSource pktSource)
	{
	    _isiId = isiId;
	    _pktSource = pktSource;
	}

	/** Send DevicePacketRequest to worker thread, wait for response */
	public DevicePacketSet getDevicePackets(long startKey, long endKey, int numBytes,
						int typeMask, int timeout)
	    throws TimeoutException, IllegalArgumentException,
	       DeviceNotFound, NoDataException
	{
	    if ((timeout < 0) || (numBytes <= 0)) {
		throw new IllegalArgumentException("timeout or numBytes value is negative");
	    }

	    DevicePacketRequest request = new DevicePacketRequest(startKey, endKey,
								  numBytes, typeMask);
	    synchronized(this) {
		_requests.add(request);
		notify();
	    }

	    synchronized(request) {
		if (!request._finished) {  //Worker COULD have finished really fast
		    try {
			request.wait(timeout);
		    } catch (Exception e) {
		    }
		}

		if (!request._finished) {
		    request._interrupted = true;
		    try {
			interrupt();
		    } catch (Exception e) {
		    }
		    throw new TimeoutException("Timeout in getDevicePackets()");
		}
	    }

	    if (request._result._packets.size() <= 0) {
		throw new NoDataException();
	    }

	    return(request._result);
	}

	/** Verify that DevicePacketSource is still valid */
	protected boolean verify()
	{
	    try {
		return(_pktSource.verify(_isiId));
	    } catch (DeviceNotFound e) {
	    }
	    return(false);
	}


	/** Determine size of DevicePacket in bytes */
	protected int pktSize(DevicePacket pkt)
	{
	    int size = 0;

	    try {
		if (pkt instanceof SensorDataPacket)
		    size = ((SensorDataPacket)pkt).dataBuffer().length;
		else if (pkt instanceof MetadataPacket)
		    size = ((MetadataPacket)pkt).getBytes().length;
		else if (pkt instanceof SummaryPacket)
		    size = ((SummaryPacket)pkt).getData().length;
		else if (pkt instanceof DeviceMessagePacket)
		    size = ((DeviceMessagePacket)pkt).getMessage().length;

	    } catch (Exception e) {
		_log4j.error("Unknown packet type: " + e);
	    }

	    return(size + DevicePacket.HEADER_BYTES);
	}

	/** Return the ISI ID we're servicing */
	public long isiId()
	{
	    return(_isiId);
	}

	public boolean equals(Object obj)
	{
	    if (obj instanceof DevicePacketWorker) {
		return(((DevicePacketWorker)obj).isiId() == _isiId);
	    }
	    return(false);
	}

	public boolean equals(long isiId)
	{
	    return(isiId == _isiId);
	}

	/** Force our thread to exit */
	public synchronized void exitWorker()
	{
	    _runIt = false;
	    notify();
	    interrupt();
	}

	public void run()
	{
	    int totBytes = 0;
	    DevicePacketSet pktSet = null;
	    DevicePacket pkt = null;
	    Vector results;
	    DevicePacketRequest request;
	    long curKey;
	    boolean complete;
	    Iterator it;
	    PacketFilter[] typeFilters = new PacketFilter[2];

	    while(_runIt)
	    {
		//Wait for a packet request
		synchronized(this) {
		    while ((_requests.size() == 0) && _runIt) {
			try {
			    wait(2000);
			} catch (InterruptedException e) {
			} catch (Exception e) {
			    _log4j.warn("Exception in run().wait()" + e);
			}
		    }

		    // Check for shutdown request
		    if (!_runIt) {
			return;
		    }

		    try {
			request = (DevicePacketRequest)(_requests.firstElement());
			_requests.remove(0);
		    } catch (Exception e) {
			_log4j.error("Exception in getting request from Vector: " + e);
			continue;
		    }
		}

		// Got Request
		curKey = request._startKey;
		typeFilters[0] = new PacketSubsampler(-1, ~request._typeMask);
		typeFilters[1] = new PacketSubsampler(0, request._typeMask);

		results = new Vector();
		complete = false;
		totBytes = 0;

		try {
		    while ((totBytes < request._numBytes) && !complete &&
			   !request._interrupted) {
			try {
			    pktSet = _pktSource.getPackets(curKey, request._endKey, typeFilters);
			} catch (NoDataException e) {
			    complete = true;
			    break;
			}

			for (it = pktSet._packets.iterator(); it.hasNext(); ) {
			    pkt = (DevicePacket)(it.next());
			    totBytes += pktSize(pkt);
			
			    if (pkt.systemTime() >= curKey) {
				curKey = pkt.systemTime() + 1;
			    }
			}

			if (pktSet.complete()) {
			    complete = true;
			}

			try {
			    results.addAll(pktSet._packets);
			} catch (NullPointerException e) {
			    _log4j.error("run() caught exception in Vector.addAll(): " + e);
			}

			pktSet = null;
		    }

		    //
		    // Got complete result set. Signal calling routine.
		    //
		    synchronized(request) {
			request._result = new DevicePacketSet(results, complete);
			request._finished = true;
			request.notify();
		    }

		    _log4j.debug("Returning DevicePacketSet containing " + 
				 totBytes + " bytes in " +
				 results.size()+ " packets");

		} catch (InterruptedException e) {
		    _log4j.error("run() caught InterruptedException: " + e);
		}

	    } /* while(true) */

	} /* run() */

    } /* class DevicePacketWorker */


    /** Class representing one getDevicePackets() request and response */
    protected class DevicePacketRequest
    {
	protected long _startKey, _endKey;
	protected int  _numBytes, _typeMask;
	protected boolean _finished, _interrupted;
	protected DevicePacketSet _result;

	protected DevicePacketRequest(long startKey, long endKey,
				      int numBytes, int typeMask)
	{
	    _startKey = startKey;
	    _endKey = endKey;
	    _numBytes = numBytes;
	    _typeMask = typeMask;
	    _finished = false;
	    _interrupted = false;
	    _result = null;
	}
    }

    /** Interface class that defines a source of DevicePackets.
     */
    public interface DevicePacketSource
    {
	/** Get all packets having key within specified range. */
	public DevicePacketSet getPackets(long startKey, long endKey,
					     PacketFilter[] filters)
	    throws NoDataException, InterruptedException;

	public boolean verify(long isiId) throws DeviceNotFound;
    }

    /** Implementation of DevicePacketSource that uses a FilteredDeviceLog
	as its packet source.  Used for DeviceIDs that map to a Node.
    */
    protected class DeviceLogSource implements DevicePacketSource
    {
	protected FilteredDeviceLogIF _log;

	protected DeviceLogSource(FilteredDeviceLogIF log)
	{
	    _log = log;
	}

	public DevicePacketSet getPackets(long startKey, long endKey, 
					  PacketFilter[] filters)
	    throws NoDataException
	{
	    return(_log.getPackets(startKey, endKey, 10, filters, true));
	}

	public boolean verify(long isiId) throws DeviceNotFound
	{
	    return(_log.getDeviceId() == isiId);
	}
    }


    /** Implementation of DevicePacketSource that uses an Instrument as
	its packet source.  Used for DeviceIDs that map to an Instrument.
    */
    protected class DeviceInstrumentSource implements DevicePacketSource
    {
	protected BaseInstrumentService _instrument = null;

	protected DeviceInstrumentSource(BaseInstrumentService instrument)
	{
	    _instrument = instrument;
	}

	public DevicePacketSet getPackets(long startKey, long endKey, 
					  PacketFilter[] filters)
	    throws NoDataException
	{
	    return(_instrument.getPackets(startKey, endKey, filters, true));
	}

	public boolean verify(long isiId) throws DeviceNotFound
	{
	    try {
		return(_node.getDevice(isiId) == _instrument);
	    } catch (RemoteException e) {
		throw new DeviceNotFound("RemoteException on getDevice()");
	    }
	}
    }

    /** Implementation of DevicePacketSource used for testing with
	tests.Aggregator.  Similar to DeviceLogSource, it implements
	a DevicePacketSource using FilteredDeviceLog.  But this
	test source inserts a delay after each getPackets() to
	allow timeout testing.
    */
    public class DeviceLogTestSource implements DevicePacketSource
    {
	protected FilteredDeviceLog _log;
	protected long _delay;

	public DeviceLogTestSource(FilteredDeviceLog log, long delay)
	{
	    _log = log;
	    _delay = delay;
	}

	public DevicePacketSet getPackets(long startKey, long endKey, 
					  PacketFilter[] filters)
	    throws NoDataException
	{
	    DevicePacketSet set = _log.getPackets(startKey, endKey, 10, filters, true);

	    if (_delay > 0) {
		try {
		    Thread.sleep(_delay);
		} catch (Exception e) {
		}
	    }

	    return(set);
	}

	public boolean verify(long isiId) throws DeviceNotFound
	{
	    return(_log.getDeviceId() == isiId);
	}

    }

} /* class DevicePacketWorker */

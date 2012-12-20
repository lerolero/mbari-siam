/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.Vector;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.FilteredDeviceLogIF;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NoDataException;

/**
   FilteredDeviceLog enables retrieval of DevicePackets based on filtering
   criteria.
 */
public class FilteredDeviceLog extends DeviceLog implements FilteredDeviceLogIF
{
    /** Log4j logger */
    static private Logger _log4j = Logger.getLogger(FilteredDeviceLog.class);

    static final private int CHUNK_SIZE = 256;

    /** Default filters to use */
    protected PacketFilter[] _defaultFilters = new PacketFilter[0];

    protected Vector _outputPackets = new Vector();

    protected boolean _defaultFiltersEnabled = true;

    // By default don't retrieve packets older than _shelfLifeMsec. 
    protected long _shelfLifeMsec = Long.MAX_VALUE;  // Assume 'infinite' shelf life


    /** Create filtered device log for specified device in specified directory.
	By default, filter will pass all packets with no subsampling.
     */
    public FilteredDeviceLog(long deviceID, String directory) 
	throws IOException, FileNotFoundException {
	super(deviceID, directory);

	// Create default filter to accept all types, no subsampling
	PacketFilter[] defaultFilters = new PacketFilter[1];
	defaultFilters[0] = new PacketSubsampler(0, DevicePacket.ALL_TYPES);

	setDefaultFilters(defaultFilters);
    }


    /** Create filtered device log for specified device in specified directory,
	with specified default filters.
     */
    public FilteredDeviceLog(long deviceID, String directory, 
			     PacketFilter[] defaultFilters) 
	throws IOException, FileNotFoundException {
	super(deviceID, directory);

	if (defaultFilters == null) {
	    defaultFilters = new PacketFilter[1];
	    defaultFilters[0] = new PacketSubsampler(0, DevicePacket.ALL_TYPES);
	}
	setDefaultFilters(defaultFilters);
    }


    /** Create filtered device log for specified device in specified directory,
	with specified default filters and "shelf life".
     */
    public FilteredDeviceLog(long deviceID, String directory, 
			     PacketFilter[] defaultFilters, 
			     long shelfLifeMsec) 
	throws IOException, FileNotFoundException {
	super(deviceID, directory);
	if (defaultFilters == null) {
	    defaultFilters = new PacketFilter[1];
	    defaultFilters[0] = new PacketSubsampler(0, DevicePacket.ALL_TYPES);
	}
	setDefaultFilters(defaultFilters);
	_shelfLifeMsec = shelfLifeMsec;
	if (_shelfLifeMsec < 0) {
	    // Negative value indicates infinite shelf life
	    _shelfLifeMsec = Long.MAX_VALUE;
	}
    }


    /** Return packet set, using default filter criteria, exclude 
	'stale' packets.. */
    public DevicePacketSet getPackets(long startKey, long endKey,
				      int maxEntries) 
	throws NoDataException {

	if (!_defaultFiltersEnabled || _defaultFilters.length == 0) {
	    // No filtering at all
	    return super.getPackets(startKey, endKey, maxEntries);
	}
	else {
	    return getPackets(startKey, endKey, maxEntries, _defaultFilters,
			      true);
	}
    }


    /** 
	Return a time-sorted packet set, using specified time window and 
	filter criteria; exclude any 'stale' packets older than this log's
	_shelfLifeMsec attribute. 
	The returned packet set may be "complete" or "incomplete"; a complete 
	packet set contains all	packets from the log that meet the retrieval 
	criteria. An incomplete packet set contains packets from the "lower 
	end" of the time-window. 
    */
    public synchronized DevicePacketSet getPackets(long startKey, long endKey,
						   int maxReturnedPackets,
						   PacketFilter[] filters) 
	throws NoDataException {

	// Get packets, but reject "stale" 
	return getPackets(startKey, endKey, maxReturnedPackets, filters, true);
    }


    /** 
	Return a time-sorted packet set, using specified time window and 
	filter criteria. The boolean 'excludeStale' argument indicates whether
	to exclude packets that exceed the _shelfLifeMsec value specified 
	for this FilteredDeviceLog. The returned packet set may be 
	"complete" or "incomplete"; a complete packet set contains all 
	packets from the log that meet the retrieval criteria. An incomplete 
	packet set contains packets from the "lower end" of the time-window. 
    */
    public synchronized DevicePacketSet getPackets(long startKey, long endKey,
						   int maxReturnedPackets,
						   PacketFilter[] filters, 
						   boolean excludeStale) 
	throws NoDataException {

	if (excludeStale) {
	    // Don't get packets older than 'staleTime'
	    long staleTime = System.currentTimeMillis() - _shelfLifeMsec;

	    if (endKey < staleTime) {
		throw new NoDataException("No fresh data in range for device " + 
					  getDeviceId());
	    }

	    if (startKey < staleTime) {
		startKey = staleTime;
		_log4j.info("getPackets(): moving startKey up to freshness date");
	    }
	}

	_log4j.debug("getPackets() - startKey=" + startKey + 
		     ", endKey=" + endKey);

	// Query the range and get the number of index entries
	_log4j.debug("getPackets() - index.nEntries()");
	int entriesInRange = _index.nEntries(startKey, endKey);
	_log4j.debug("getPackets() - done with index.nEntries()");
	if (entriesInRange <= 0) {
	    _log4j.info("No data in range for device " + getDeviceId());
	    throw new NoDataException("No data in range for device " + 
				      getDeviceId());
	}

	boolean complete = false;
	int nPassedPackets = 0;
	boolean gotMaxPackets = false;
	boolean done = false;
	int entriesLeft = entriesInRange;
	long lastTimestamp = -1;
	_outputPackets.clear();
	int iter = 0;
	while (!done) {
	    // Get the next "chunk" of index entries within the search range
	    int vecSize = Math.min(CHUNK_SIZE, entriesInRange);

	    _log4j.debug("getPackets() - index.getEntries(), iter=" + iter++);
	    Vector indexEntries = _index.getEntries(startKey, vecSize);
	    _log4j.debug("getPackets() - done with index.getEntries()");

	    // Iterate through the index vector, get packet at each index 
	    // entry, pass through filters, and if passed add packet to output
	    // vector.
	    int entryIndex = 0;
	    for (int i = 0; i < indexEntries.size(); i++) {

		DeviceLogIndex.Entry entry = 
		    (DeviceLogIndex.Entry)indexEntries.elementAt(i);

		entryIndex = entry.getEntryIndex();

		// Pass through filters
		DevicePacket packet= getPacket(entry);

		if (gotMaxPackets && packet.systemTime() != lastTimestamp) {
		    // We've got or exceeded the maximum number of packets
		    // in the returned packet set; quit as soon as the 
		    // timestamp changes, to avoid possible duplicate 
		    // retrievals later.
		    done = true;
		    break;
		}
		lastTimestamp = packet.systemTime();

		entriesLeft--;

		boolean pass = true;
		_log4j.debug("getPackets(): apply filters");
		for (int n = 0; n < filters.length; n++) {
		    if (!filters[n].pass(packet)) {
			pass = false;
			break;
		    }
		}
		_log4j.debug("getPackets(): done with filters");

		if (!pass) {
		    // Packet didn't pass filters - process next packet
		    _log4j.debug("packet " + i + " didn't pass");
		    continue;
		}

		// Add packet to output vector.
		_outputPackets.addElement((Object)packet);
		nPassedPackets++;
		_log4j.debug("getPackets() - got " + nPassedPackets + 
			     " packets");

		if (nPassedPackets > maxReturnedPackets) {
		    // PacketSet is full; may be incomplete though (check 
		    // below). We may still add packets until it is clear we
		    // are out of a "run" of duplicate time-stamps, so that
		    // the requestor can avoid duplicated packets when 
		    // retrieving the next packet set.
		    gotMaxPackets = true;
		}
	    }

	    // Compute number of entries not yet processed
	    if (entriesLeft <= 0) {
		done = true;
		// No more packets in time range
		complete = true;
		break;
	    }
	    // Starting index for next chunk read
	    startKey = lastTimestamp;
	}

	if (nPassedPackets == 0) {
	    _log4j.info("No packets for device " + getDeviceId() + 
			" after filtering");

	    throw new NoDataException("No packets in time range after filtering" );

	}

	// Trim to actual number of entries
	_outputPackets.setSize(nPassedPackets);
		
	return new DevicePacketSet(_outputPackets, complete);
	    
    }


    /** Remove all default filters */
    public void clearDefaultFilters() {
	_defaultFilters = new PacketFilter[0];
    }

    /** Add specified filters to default filters */
    public void addDefaultFilters(PacketFilter[] addFilters) {
	PacketFilter[] newFilters = 
	    new PacketFilter[addFilters.length + _defaultFilters.length];

	// Copy existing default filters to new array
	System.arraycopy(_defaultFilters, 0, newFilters, 0, 
			 _defaultFilters.length);

	// Append additional filters to new array
	System.arraycopy(addFilters, 0, 
			 newFilters, _defaultFilters.length, 
			 addFilters.length);

	// Set default filter to reference new array
	_defaultFilters = newFilters;
    }


    /** Set default filters */
    public void setDefaultFilters(PacketFilter[] filters) {
	for (int i = 0; i < filters.length; i++) {
	    _log4j.debug("setDefaultFilters(): filter[" + i + "]: " + 
			 filters[i]);
	}

	_defaultFilters = filters;
    }


    /** Return default filters */
    public PacketFilter[] getDefaultFilters() {
	return _defaultFilters;
    }


    /** Enable default filters */
    public void enableDefaultFilters() {
	_defaultFiltersEnabled = true;
    }

    /** Disable default filters */
    public void disableDefaultFilters() {
	_defaultFiltersEnabled = false;
    }

    /** Return true if default filters are enabled */
    public boolean defaultFiltersEnabled() {
	return _defaultFiltersEnabled;
    }
}    

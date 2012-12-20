/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.IOException;

/* $Id: FilteredDeviceLogIF.java,v 1.4 2012/12/17 21:35:16 oreilly Exp $ */

/**
   <code>FilteredDeviceLogIF</code> is the interface for a DevicePacket logger
 
   @see org.mbari.siam.core.DeviceLog
   @author Bob Herlien
*/

public interface FilteredDeviceLogIF extends DeviceLogIF
{
    /** 
	Return a time-sorted packet set, using specified time window and 
	filter criteria. The boolean 'excludeStale' argument indicates whether
	to exclude packets that exceed the _shelfLifeMsec value specified 
	for this FilteredDeviceLog. The returned packet set may be 
	"complete" or "incomplete"; a complete packet set contains all 
	packets from the log that meet the retrieval criteria. An incomplete 
	packet set contains packets from the "lower end" of the time-window. 
    */
    public DevicePacketSet getPackets(long startKey, long endKey,
				      int maxReturnedPackets,
				      PacketFilter[] filters, 
				      boolean excludeStale) 
	throws NoDataException;

}

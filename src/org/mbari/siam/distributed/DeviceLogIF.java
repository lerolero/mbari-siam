/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.IOException;

/* $Id: DeviceLogIF.java,v 1.6 2012/12/17 21:34:57 oreilly Exp $ */

/**
   <code>DeviceLogIF</code> is the interface for a DevicePacket logger
 
   @see org.mbari.siam.core.DeviceLog
   @author Bob Herlien
*/

public interface DeviceLogIF
{
    /** Return last metadata reference number. */
    public long getLastMetadataRef();
	
    /** returns the ISI deviceId associated with this <code>DeviceLog</code> */
    public long getDeviceId();
	
    /** This method appends the specified <code>DevicePacket</code> object to
	the log that is being maintained. The object is serialized and 
	subsequently written to a data file while indexing information is 
	maintained. If argument setSequenceNos is set to 'true', then
	appendPacket() will automatically set the packet's sequence number
	and metadata reference.
	@param packet <code>DevicePacket</code> object to append to active log
    */
    public void appendPacket(DevicePacket packet, 
			     boolean setSequenceNos, 
			     boolean checkTime);
	
    /** This method appends the specified <code>DevicePacket</code> object to
	the log that is being maintained. The object is serialized and 
	subsequently written to a data file while indexing information is 
	maintained. This version of appendPacket() will automatically set 
	the packet's sequence number and metadata reference.
	@param packet <code>DevicePacket</code> object to append to active log
    */
    public void appendPacket(DevicePacket packet);

    public DevicePacketSet getPackets(long startKey, long endKey,
				      int maxEntries) throws NoDataException;
    /**
       Call getPacketKeyRange() - calls getPackets().
       getPacketKeyRange() is an obsolete method name, but we keep it
       for patch compatibility.
    */
    public DevicePacketSet getPacketKeyRange(long startKey, long endKey,
					     int maxEntries) throws NoDataException;

    /**
     * Return the last sequential packet in the log.
     * @return last sequential packet in log (DevicePacket)
     * @throws NoDataException
     */
    DevicePacket getLastPacket() throws NoDataException;
	
    /** returns the next unread DevicePacket in sequential order. 
	@see org.mbari.siam.distributed.DevicePacket
    */
    public DevicePacket getNextPacket() throws NoDataException;

    /** Return total number of packets in log. */
    public int nPackets();
	
    /** returns the number of unread packets remaining if packets are retrieved
	using the <code>getNextPacket()</code> method
	@see DeviceLogIF#getNextPacket()
    */
    public int getNumUnreadPackets();
	
    /** returns the minimum key that has been registered by all DevicePacket
	storage operations
	@see org.mbari.siam.core.DeviceLog#getPacketKeyRange
    */
    public long getMinTimestamp();
    
    /** returns the maximum key that has been registered by all DevicePacket
	storage operations
	@see org.mbari.siam.core.DeviceLog#getPacketKeyRange
    */
    public long getMaxTimestamp();
	
    /** Close the DeviceLog and associated files. This DeviceLog instance
	is no longer usable after close() has been called. */
    public void close() throws IOException;
}

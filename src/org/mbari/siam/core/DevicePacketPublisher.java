/****************************************************************************/
/* Copyright 2007 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
// $Header: /home/cvs/siam2/src/org/mbari/siam/core/DevicePacketPublisher.java,v 1.2 2009/07/16 17:39:20 headley Exp $
// $Revision: 1.2 $
// $Log: DevicePacketPublisher.java,v $
// Revision 1.2  2009/07/16 17:39:20  headley
// javadoc syntax fixes
//
// Revision 1.1  2008/11/04 22:17:47  bobh
// Initial checkin.
//
// Revision 1.1.1.1  2008/11/04 19:02:04  bobh
// Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
//
// Revision 1.1  2007/06/07 18:36:24  bobh
// Publish DevicePacket to SSDS whenever you get a LogSampleServiceEvent
//
//

package org.mbari.siam.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.mbari.siam.operations.utils.ExportablePacket;
import moos.ssds.jms.PublisherComponent;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SummaryPacket;

/**
 * DevicePacketPublisher instantiates an SSDS PublisherComponent and
 * uses it to publish DevicePackets.  This class was invented to facilitate
 * creating a single application that acquires SIAM data and publishes it.
 * Borrowed from class OasisPublisher in the OasisToSSDS project.
 *
 * @see NodeManager
 * 
 * @author Bob Herlien
 */

public class DevicePacketPublisher implements LogSampleListener
{
    private static Logger _log4j = Logger.getLogger(DevicePacketPublisher.class);
    protected static SimpleDateFormat _dateFormatter = new SimpleDateFormat("HH:mm:ss");

    protected PublisherComponent _ssdsPublisher = null;
    protected ExportablePacket	_exportablePacket = null;
    protected String		_defaultMetadata = null;
    protected boolean		_publish = false;


    /** Create a PublisherComponent
	@param publish - true to publish to SSDS, false simply
	outputs packets to System.out
     */
    public DevicePacketPublisher(boolean publish)
    {
	_publish = publish;

	if (_publish)
	{
	    _ssdsPublisher = new PublisherComponent();
	    _exportablePacket = new ExportablePacket();
	}

	// Register to hear about events on the node
	EventManager.getInstance().addListener(ServiceListener.class, this);
    }


    /** Create a PublisherComponent */
    public DevicePacketPublisher()
    {
	this(false);
    }


    /** Publish packet to JMS server.
	@param pkt DevicePacket
     */
    public void publishData(DevicePacket pkt) throws IOException
    {
        if (_publish)
	{
	    _log4j.debug("Publishing data for ID " + pkt.sourceID());

	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(bos);

	    try {
		_exportablePacket.wrapPacket(pkt);
		_exportablePacket.export(dos);
		dos.flush();
		byte[] exportedBytes = bos.toByteArray();
		_ssdsPublisher.publishBytes(exportedBytes);
	    } finally {
		dos.close();
	    }
	}
	else
	    printPacket(pkt);
    }


    /** Print the pkt to System.out
	@param pkt DevicePacket
     */
    public void printPacket(DevicePacket pkt)
    {
	String pktType;

	if (pkt instanceof SensorDataPacket)
	    pktType = "SensorDataPacket";
	else if (pkt instanceof MetadataPacket)
	    pktType = "MetaDataPacket";
	else if (pkt instanceof DeviceMessagePacket)
	    pktType = "DeviceMessagePacket";
	else if (pkt instanceof SummaryPacket)
	    pktType = "SummaryPacket";
	else
	    pktType = "Unknown DevicePacket";

	_log4j.debug(pktType + ", Time = " + 
		     _dateFormatter.format(new Date(pkt.systemTime()))
		     + ", " + pkt.toString());
    }


    /** Called whenever an InstrumentService logs a sample */
    public void sampleLogged(LogSampleServiceEvent event)
    {
	DevicePacket sample = event.getLogSample();

	if (sample != null)
	{
	    try {
		publishData(sample);
	    } catch (IOException e) {
		_log4j.error("IOException publishing DevicePacket: " + e);
		e.printStackTrace();
	    }
	}
    }

} /* DevicePacketPublisher */

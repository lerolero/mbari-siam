/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.DataOutput;
import java.io.DataInput;
import java.io.IOException;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.Exportable;

/**
 * ExportablePacket wraps DevicePacket objects with an Exportable interface.
 */
public class ExportablePacket implements Exportable {

    /** StringBuffer, to avoid many new String allocations. */
    protected StringBuffer _buffer = new StringBuffer(10240);
	
    protected DevicePacket _packet = null;

    /** Wrap a DevicePacket. */
    public void wrapPacket(DevicePacket packet) {
	_packet = packet;
    }

    /** Export the current DevicePacket */
    public void export(DataOutput out) throws IOException {

	// KLUDGE for now; serialVersionUID is not publicly accessible
	// from DevicePacket classes
	long serialVersionUID = 0L;

	out.writeShort(Exportable.EX_DEVICEPACKET);
	out.writeLong(serialVersionUID);
	out.writeLong(_packet.sourceID());
	out.writeLong(_packet.systemTime());
	out.writeLong(_packet.sequenceNo());
	out.writeLong(_packet.metadataRef());
	out.writeLong(_packet.getParentId());
	out.writeLong(_packet.getRecordType());

	if (_packet instanceof SensorDataPacket) {
	    SensorDataPacket sensorData = (SensorDataPacket) _packet;
	    out.writeShort(Exportable.EX_SENSORDATAPACKET);
	    out.writeLong(serialVersionUID);
	    out.writeInt(sensorData.dataBuffer().length);
	    out.write(sensorData.dataBuffer());
	} else if (_packet instanceof MetadataPacket) {
	    MetadataPacket metadata = (MetadataPacket) _packet;
	    out.writeShort(Exportable.EX_METADATAPACKET);
	    out.writeLong(serialVersionUID);
	    out.writeInt(metadata.cause().length);
	    out.write(metadata.cause());
	    // Write payload bytes
	    out.writeInt(metadata.getBytes().length);
	    out.write(metadata.getBytes());
	} else if (_packet instanceof DeviceMessagePacket) {
	    DeviceMessagePacket message = (DeviceMessagePacket) _packet;
	    out.writeShort(Exportable.EX_DEVICEMESSAGEPACKET);
	    out.writeLong(serialVersionUID);
	    out.writeInt(message.getMessage().length);
	    out.write(message.getMessage());
	} else if (_packet instanceof SummaryPacket) {
	    SummaryPacket summary = (SummaryPacket) _packet;
	    out.writeShort(Exportable.EX_SENSORDATAPACKET);
	    // TBD: Set record type to special "summary type"
	    out.writeLong(serialVersionUID);
	    out.writeInt(summary.getData().length);
	    out.write(summary.getData());
	}
    }

    /** Import a DevicePacket */
    public DevicePacket importPacket(DataInput in) throws IOException {

	// KLUDGE for now; serialVersionUID is not publicly accessible
	// from DevicePacket classes
	long packetSVUID = 0L;

	short type= in.readShort();
	if(type != Exportable.EX_DEVICEPACKET){
	    throw new IOException("type "+type+" not supported");
	}
	packetSVUID=in.readLong();
	long sourceID=in.readLong();
	long systemTime=in.readLong();
	long sequenceNo=in.readLong();
	long metadataRef=in.readLong();
	long parentID=in.readLong();
	long recordType=in.readLong();

	short packetType=in.readShort();
	long dataSVUID=0L;

	if (packetType== Exportable.EX_SENSORDATAPACKET){
	    dataSVUID=in.readLong();
	    int bufferLength=in.readInt();
	    byte[] buffer=new byte[bufferLength];
	    in.readFully(buffer);

	    SensorDataPacket packet = new SensorDataPacket(sourceID,bufferLength);
	    packet.setSystemTime(systemTime);
	    packet.setSequenceNo(sequenceNo);
	    packet.setMetadataRef(metadataRef);
	    packet.setParentId(parentID);
	    packet.setRecordType(recordType);
	    packet.setDataBuffer(buffer);

	    return packet;

	} else if (packetType == Exportable.EX_METADATAPACKET) {

	    dataSVUID=in.readLong();

	    int causeBufferLength=in.readInt();
	    byte[] causeBuffer=new byte[causeBufferLength];
	    in.readFully(causeBuffer);

	    int dataBufferLength=in.readInt();
	    byte[] dataBuffer=new byte[dataBufferLength];
	    in.readFully(dataBuffer);

	    MetadataPacket packet = new MetadataPacket(sourceID,causeBuffer,dataBuffer);
	    packet.setSystemTime(systemTime);
	    packet.setSequenceNo(sequenceNo);
	    packet.setMetadataRef(metadataRef);
	    packet.setParentId(parentID);
	    packet.setRecordType(recordType);

	    return packet;

	} else if (packetType == Exportable.EX_DEVICEMESSAGEPACKET) {

	    dataSVUID=in.readLong();

	    int msgBufferLength=in.readInt();
	    byte[] msgBuffer=new byte[msgBufferLength];
	    in.readFully(msgBuffer);

	    DeviceMessagePacket packet = new DeviceMessagePacket(sourceID);
	    //packet.setSystemTime(systemTime);
	    packet.setSequenceNo(sequenceNo);
	    packet.setMetadataRef(metadataRef);
	    packet.setParentId(parentID);
	    packet.setRecordType(recordType);
	    packet.setMessage(systemTime,msgBuffer);

	    return packet;

	} else if (_packet instanceof SummaryPacket) {
	    //SummaryPacket summary = (SummaryPacket) _packet;
	    //in.readShort(Exportable.EX_SENSORDATAPACKET);
	    // TBD: Set record type to special "summary type"
	    //in.readLong(serialVersionUID);
	    //in.readInt(summary.getData().length);
	    //in.read(summary.getData());
	}
	throw new IOException("Unsupported packet type "+type);

    }
}

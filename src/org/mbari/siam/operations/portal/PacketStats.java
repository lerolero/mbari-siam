// Copyright 2002 MBARI
package org.mbari.siam.operations.portal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.operations.utils.ExportablePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketOutputStream;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SummaryPacket;


import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.NoDataException;


/** Describe Device packet retrieval statistics. */
public class PacketStats {
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(PacketStats.class);
    
    String _dateString="";
    long _sourceID=-1L;
    long _systemTime=-1L;
    long _sequenceNo=-1L;
    long _metadataRef=-1L;
    long _parentID=-1L;
    long _recordType=-1L;

    /** data size is the size of the data buffer(s).
	Metadata packets and message packets each
	carry one additional buffer of data 
	(cause and message, respectively).
     */
    long _dataSize=-1L;

    /** Header size is the size of 6 longs:
	sourceID,systemTime,metadataRef,
	sequenceNo,parentID,recordType
    */
   int _headerSize=DevicePacket.HEADER_BYTES;

    /** packet size is the size of the 
	header and data buffer(s), with
	no serialization overhead.
	It's "just the packet".
    */
    long _packetSize=-1L;

    /** export size is the size of the
	serialized form exported to SSDS.
	This size includes additional serialization
	overhead for describing serialization version,
	packet components, and buffer sizes.
     */
    long _exportSize=-1L;

    /** logged size is the size of the 
	serialized form used for disk logging.
	This serialized form uses Java default
	serialization, adding about 200 bytes
	of serialization information per packet
	to the raw packet size (data+header)
     */
    long _loggedSize=-1L;

    boolean _includeHeader=false;
    String _cause="";
    String _delimiter=";";
    byte[] buffer;
    boolean _valid=false;

    public long _latestPacketTime;

    public PacketStats(long latestPacketTime) {
	_latestPacketTime = latestPacketTime;
    }

    public PacketStats(DevicePacket packet,boolean includeHeader) {
	parse(packet);
	_includeHeader=includeHeader;
    }

    public PacketStats(DevicePacket packet) {
	parse(packet);
    }

    public PacketStats(String record) {
	parse(record);
    }
    public PacketStats(String record,String delimiter) {
	setDelimiter(delimiter);
	parse(record);
    }

    public void setDelimiter(String s){
	_delimiter=s;
    }

    public void includeHeader(boolean include){
	_includeHeader=include;
    }

    public  String getStatsHeader(){
	String header="EpochTime"+_delimiter+
	    "Time"+_delimiter+
	    "DeviceID"+_delimiter+
	    "ParentID"+_delimiter+
	    "RecordType"+_delimiter+
	    "SeqNo"+_delimiter+
	    "MetadataRef"+_delimiter+
	    "DataSize"+_delimiter+
	    "HeaderSize"+_delimiter+
	    "PacketSize"+_delimiter+
	    "ExportSize"+_delimiter+
	    "LoggedSize"+_delimiter+
	    "Cause";
	return header;
    }

    public boolean parse(String record){
	    StringTokenizer st=new StringTokenizer(record,_delimiter);
	    if(st.countTokens()<8)
		return false;
	    try{
		    _systemTime=Long.parseLong(st.nextToken());
		    _dateString=st.nextToken();
		    _sourceID=Long.parseLong(st.nextToken());
		    _parentID=Long.parseLong(st.nextToken());
		    _recordType=Long.parseLong(st.nextToken());
		    _sequenceNo=Long.parseLong(st.nextToken());
		    _metadataRef=Long.parseLong(st.nextToken());
		    _dataSize=Long.parseLong(st.nextToken());
		    _headerSize=Integer.parseInt(st.nextToken());
		    _packetSize=Long.parseLong(st.nextToken());
		    _exportSize=Long.parseLong(st.nextToken());
		if(st.hasMoreTokens())
		    _loggedSize=Long.parseLong(st.nextToken());
		if(st.hasMoreTokens())
		    _cause=st.nextToken();
		_valid=true;
	    }catch(Exception e){
		e.printStackTrace();
		return false;
	    }
	    return true;
    }

    public boolean isValid(){
	return _valid;
    }

    public void parse(DevicePacket packet){
	_exportSize=getExportSize(packet);
	_loggedSize=getLoggedSize(packet);
	_dataSize=getDataSize(packet);
	// header size is fixed and the same for all packet types
	_packetSize=_dataSize+(long)_headerSize;
	_systemTime= packet.systemTime();
	_dateString = 
	    DateFormat.getDateTimeInstance().format(new Date(_systemTime));
	 _sourceID=packet.sourceID();
	 _parentID=packet.getParentId();
	 _recordType=packet.getRecordType();
	 _sequenceNo=packet.sequenceNo();
	 _metadataRef=packet.metadataRef();
	 if (packet instanceof MetadataPacket){
	     _cause=new String(((MetadataPacket)packet).cause());
	 }
	 _valid=true;
    }

    public String toString(){
	String stats=_systemTime+_delimiter+
	    _dateString+_delimiter+
	    _sourceID+_delimiter+
	    _parentID+_delimiter+
	    _recordType+_delimiter+
	    _sequenceNo+_delimiter+
	    _metadataRef+_delimiter+
	    _dataSize+_delimiter+
	    _headerSize+_delimiter+
	    _packetSize+_delimiter+
	    _exportSize+_delimiter+
	    _loggedSize+_delimiter+
	    "\""+_cause+"\"";
	if(_includeHeader)
	    return (getStatsHeader()+"\n"+stats);
	else
	    return stats;
    }

    public long getSystemTime(){return _systemTime;}
    public String getDateString(){return _dateString;}
    public long getSourceID(){return _sourceID;}
    public long getParentID(){return _parentID;}
    public long getRecordType(){return _recordType;}
    public long getSequenceNo(){return _sequenceNo;}
    public long getMetadataRef(){return _metadataRef;}
    public String getMetadataCause(){return _cause;}
    public long getDataSize(){return _dataSize;}
    public int getHeaderSize(){return _headerSize;}
    public long getExportSize(){return _exportSize;}
    public long getLoggedSize(){return _loggedSize;}
    public long getPacketSize(){return getDataSize()+getHeaderSize();}

    public long getExportSize(DevicePacket packet){
	DataOutputStream dos=new DataOutputStream(new ByteArrayOutputStream());
	ExportablePacket ep=new ExportablePacket();
	ep.wrapPacket(packet);
	try{
	    ep.export(dos);
	}catch(IOException e){}
	return dos.size();
    }

    public long getLoggedSize(DevicePacket packet){
	long objectSize=0L;
	try {
	    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();	
	    byteOutput.write(DeviceLog.SYNC_PATTERN);
	    ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			
	    objectOutput.writeObject(packet);
	    objectOutput.flush();
			
	    byte[] serializedPacket = byteOutput.toByteArray();
	    objectSize=serializedPacket.length;
	}
	catch (Exception e) {
	    _log4j.error("getLoggedPacketSize: serialization exception", e);
	}
	return objectSize;
    }

    public byte[] getBuffer(){
	return buffer;
    }

    public long getDataSize(DevicePacket packet){

	if(packet instanceof SensorDataPacket){
	    buffer=((SensorDataPacket)packet).dataBuffer();
	    return buffer.length;
	}else if (packet instanceof MetadataPacket){
	    buffer=((MetadataPacket)packet).getBytes();
	    return buffer.length+((MetadataPacket)packet).cause().length;
	}else if (packet instanceof DeviceMessagePacket){
	    buffer=((DeviceMessagePacket)packet).getMessage();
	    return buffer.length;
	}else if (packet instanceof SummaryPacket){
	    buffer=((SummaryPacket)packet).getData();
	    return buffer.length;
	}
	return -1L;
    }    


    public static void main(String args[]) {

	long sourceID=1L;
	long parentID=2L;

	SensorDataPacket sdp=new SensorDataPacket(1000,64);
	DeviceMessagePacket dmp=new DeviceMessagePacket(2000);
	MetadataPacket mdp=new MetadataPacket(3000,"test012345".getBytes(),"Metadata0123456".getBytes());
	SummaryPacket sp=new SummaryPacket(4000);

	long now=System.currentTimeMillis();
	sdp.setSystemTime(now);
	sdp.setSequenceNo(100L);
	sdp.setMetadataRef(0L);
	sdp.setParentId(parentID);
	sdp.setRecordType(1L);
	sdp.setDataBuffer("SensorData01234".getBytes());
	
	dmp.setSystemTime(now);
	dmp.setSequenceNo(100L);
	dmp.setMetadataRef(0L);
	dmp.setParentId(parentID);
	dmp.setRecordType(2L);
	dmp.setMessage(now,"DeviceMessage01".getBytes());

	mdp.setSystemTime(now);
	mdp.setSequenceNo(100L);
	mdp.setMetadataRef(0L);
	mdp.setParentId(parentID);
	mdp.setRecordType(0L);

	sp.setSystemTime(now);
	sp.setSequenceNo(100L);
	sp.setMetadataRef(0L);
	sp.setParentId(parentID);
	sp.setRecordType(1000L);
	sp.setData(now,"Summary01234567".getBytes());
	
	PacketStats psSDP=new PacketStats(sdp);
	psSDP.includeHeader(true);
	System.out.println(psSDP);
	PacketStats psDMP=new PacketStats(dmp,false);
	System.out.println(psDMP);
	PacketStats psMDP=new PacketStats(mdp,false);
	System.out.println(psMDP);
	PacketStats psSP=new PacketStats(sp,false);
	System.out.println(psSP);
	try{
	    DeviceLog dlSDP=new DeviceLog(1000L,"./");
	    dlSDP.appendPacket(sdp,false,false);
	    DeviceLog dlDMP=new DeviceLog(2000L,"./");
	    dlDMP.appendPacket(dmp,false,false);
	    DeviceLog dlMDP=new DeviceLog(3000L,"./");
	    dlMDP.appendPacket(mdp,false,false);
	    DeviceLog dlSP=new DeviceLog(4000L,"./");
	    dlSP.appendPacket(sp,false,false);
	}catch(FileNotFoundException e){
	    e.printStackTrace();
	}catch(IOException e){
	    e.printStackTrace();
	}

	try{
	    FileOutputStream fos=new FileOutputStream("./exportSDP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(sdp);
	    ep.export(dos);
	}catch(IOException e){e.printStackTrace();}
	try{
	    FileOutputStream fos=new FileOutputStream("./exportMDP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(mdp);
	    ep.export(dos);
	}catch(IOException e){e.printStackTrace();}
	try{
	    FileOutputStream fos=new FileOutputStream("./exportDMP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(dmp);
	    ep.export(dos);
	}catch(IOException e){e.printStackTrace();}
	try{
	    FileOutputStream fos=new FileOutputStream("./exportSP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(sp);
	    ep.export(dos);
	}catch(IOException e){e.printStackTrace();}

	// parse test
	System.out.println("Checking format conversion, creating new PacketStats from exported string");
	PacketStats foo = new PacketStats(psMDP.toString());
	System.out.println("originalPacket:\n"+psMDP+"\nnew object:\n"+foo);

    }
}

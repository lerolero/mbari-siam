// Copyright 2002 MBARI
package org.mbari.siam.operations.portal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import java.util.EventObject;
import java.util.NoSuchElementException;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseDescription;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.distributed.portal.QueuedCommand;
import org.mbari.siam.distributed.portal.UnknownConfiguration;
import org.mbari.siam.operations.utils.ExportablePacket;
import org.mbari.siam.operations.portal.PortalConnection;
import org.mbari.siam.operations.portal.PacketStats;
import org.mbari.siam.operations.portal.Profiler;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketOutputStream;
import org.mbari.siam.distributed.DevicePacketServerThread;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.MeasurementPacket;


/**
 * PortalSession
 * @author Kent Headley
 */
public class PortalSession extends SessionParameter{
    /** log4j logger */
    static private Logger _log4j = Logger.getLogger(PortalSession.class);
    // profiler
    Profiler _profiler=new Profiler(_log4j,"portal.profiler",null);

    // CVS revision 
    private static String _versionID = "$Revision: 1.2 $";

    // data type IDs
    public static final int TOTAL_PACKETS=10;
    public static final int TOTAL_PACKET_BYTES=20;
    public static final int TOTAL_DATA_BYTES=30;
    public static final int SENSORDATA_PACKETS=40;
    public static final int SENSORDATA_PACKET_BYTES=50;
    public static final int SENSORDATA_DATA_BYTES=60;
    public static final int METADATA_PACKETS=70;
    public static final int METADATA_PACKET_BYTES=80;
    public static final int METADATA_DATA_BYTES=90;
    public static final int MESSAGE_PACKETS=100;
    public static final int MESSAGE_PACKET_BYTES=110;
    public static final int MESSAGE_DATA_BYTES=120;
    public static final int MEASUREMENT_PACKETS=130;
    public static final int MEASUREMENT_PACKET_BYTES=140;
    public static final int MEASUREMENT_DATA_BYTES=150;
    public static final int SESSION_SUMMARY=160;
    // events
    public static final int CONNECT_NODE=170;
    public static final int RESET_WDT=180;
    public static final int SYNC_NTP=190;
    public static final int DEVICE_LIST=200;
    public static final int GET_DATA=210;
    public static final int RX_PACKET_SET=220;
    public static final int SAVE_DATA=230;
    public static final int DISTRIBUTE_DATA=240;
    public static final int PUBLISH_DATA=250;
    public static final int NODE_LIST=260;
    public static final int DISCONNECT_NODE=270;
    public static final int WAKE_SUBNODE=280;
    public static final int EXCEPTION=290;
    public static final int ERROR=300;
    public static final int RECORD_TYPE=310;
    public static final int START_SESSION=320;
    public static final int SEND_COMMANDS=330;
    public static final int RETRIEVE_DISTRIBUTE_DATA=340;
    public static final int PROCESS_NODE=350;
    public static final int PROCESS_PACKET_SET=360;
    public static final int UNSPECIFIED=370;

    public static final int INITIALIZED=0;
    public static final int OPEN=1;
    public static final int CLOSED=2;

    private Hashtable _nodeData=new Hashtable();

    int _status=INITIALIZED;
    String _summary=null;

    private TypeCounter _exceptionTypes=new TypeCounter("exception",EXCEPTION);
    private TypeCounter _errorTypes=new TypeCounter("error",ERROR);

    boolean _enableNodeDetail=false;
    boolean _enableDeviceDetail=false;


    public PortalSession(String sessionName){
	this();
	_sessionName=sessionName;
    }

    public PortalSession(){
	super();

	// session ID is current time (ms since epoch)
	_sessionID=System.currentTimeMillis();
	_log4j.debug("new PortalSession: sid "+_sessionID);
	// Enable profiler output
	_profiler.setEnabled(true);

    }

    public int status(){
	return _status;
    }

    public long id(){
	return _sessionID;
    }

    /** get NodeData object with specified nodeID, 
	create new one if it doesn't exist 
    */
    public spNodeData getNodeData(long node){
	Long lid=new Long(node);
	spNodeData nd = (spNodeData)_nodeData.get(lid);
	if(nd==null){
	    nd=new spNodeData(_sessionID,node,_deviceID);
	    _nodeData.put(lid,nd);
	}
	return nd;
    }

    public void notify(PortalEvent event){

	switch(event.getID()){
	case(PortalEvent.SESSION_START):
 	    _log4j.debug(_sessionName+".sid "+_sessionID+" SESSION_START source: "+event.getSource());
 	    _log4j.debug(_sessionName+".sid "+_sessionID+" SESSION_START message: "+event.getMessage());
	    if(_status==OPEN){
		_errorTypes.add("unexpected SESSION_START");
		_log4j.error(_sessionName+".sid "+_sessionID+" unexpected SESSION_START");
		break;
	    }

 	    if(event.getTime()>0L){
		_log4j.debug(_sessionName+".sid "+_sessionID+" setting session start time "+event.getTime());
 		_startTime=event.getTime();
	    }else{
		_log4j.error(_sessionName+".sid "+_sessionID+" eventTime<=0 ("+event.getTime()+"); using current time");
		_startTime=System.currentTimeMillis();
	    }

	    _status=OPEN;
	    break;
	case(PortalEvent.SESSION_END):
 	    _log4j.debug(_sessionName+".sid "+_sessionID+" SESSION_END source: "+event.getSource());
 	    _log4j.debug(_sessionName+".sid "+_sessionID+" SESSION_END message: "+event.getMessage());

	    if(_status!=OPEN){
		_errorTypes.add("unexpected SESSION_END");
		_log4j.error(_sessionName+".sid "+_sessionID+" unexpected SESSION_END");
		break;
	    }

 	    if(event.getTime()>0L){
		_log4j.debug(_sessionName+".sid "+_sessionID+" setting session end time "+event.getTime());
 		_endTime=event.getTime();
	    }else{
		_log4j.error(_sessionName+".sid "+_sessionID+" eventTime<=0 ("+event.getTime()+"); using current time");
		_endTime=System.currentTimeMillis();
	    }

	    if(_endTime<_startTime){
		_log4j.error(_sessionName+".sid "+_sessionID+" SESSION_END<SESSION_START");
	    }
	    _status=CLOSED;
	    break;
	case(PortalEvent.EXCEPTION):
	    _exceptionTypes.add(event.getException().getClass().getName());
	    break;
	case(PortalEvent.CONNECT_NODE_START):
	case(PortalEvent.CONNECT_NODE_END):
	case(PortalEvent.START_SESSION_START):
	case(PortalEvent.START_SESSION_END):
	case(PortalEvent.SEND_COMMANDS_START):
	case(PortalEvent.SEND_COMMANDS_END):
	case(PortalEvent.RESET_WDT_START):	    
	case(PortalEvent.RESET_WDT_END):	    
	case(PortalEvent.SYNC_NTP_START):	    
	case(PortalEvent.SYNC_NTP_END):	    
	case(PortalEvent.DEVICE_LIST_START):	    
	case(PortalEvent.DEVICE_LIST_END):	    
	case(PortalEvent.GET_DATA_START):
	case(PortalEvent.GET_DATA_END):
	case(PortalEvent.RETRIEVE_DISTRIBUTE_DATA_START):
	case(PortalEvent.RETRIEVE_DISTRIBUTE_DATA_END):
	case(PortalEvent.RX_PACKET_SET_START):
	case(PortalEvent.RX_PACKET_SET_END):
	case(PortalEvent.PROCESS_PACKET_SET_START):
	case(PortalEvent.PROCESS_PACKET_SET_END):
	case(PortalEvent.SAVE_DATA_START):	    
	case(PortalEvent.SAVE_DATA_END):	    
	case(PortalEvent.DISTRIBUTE_DATA_START):	    
	case(PortalEvent.DISTRIBUTE_DATA_END):	    
	case(PortalEvent.PUBLISH_DATA_START):	    
	case(PortalEvent.PUBLISH_DATA_END):	    
	case(PortalEvent.NODE_LIST_START):	    
	case(PortalEvent.NODE_LIST_END):	    
	case(PortalEvent.DISCONNECT_NODE_START):
	case(PortalEvent.DISCONNECT_NODE_END):
	case(PortalEvent.WAKE_SUBNODE_START):
	case(PortalEvent.WAKE_SUBNODE_END):
	case(PortalEvent.PROCESS_NODE_START):
	case(PortalEvent.PROCESS_NODE_END):
	case(PortalEvent.UNSPECIFIED_START):
	case(PortalEvent.UNSPECIFIED_END):
	    if(event.getSource() instanceof PortalConnection){
		long nodeID=((PortalConnection)event.getSource()).getTargetNodeID();
		spNodeData spnd =getNodeData(nodeID);
		spnd.notify(event);
	    }
	    break;
	default:
	    break;
	}
    }

    public  Object getParameter(int key){
	return null;
    }


    public String toString(){
	StringBuffer sb=new StringBuffer();
	sb.append("name;"+_sessionName);
	sb.append(";id;"+_sessionID);
	sb.append(";start;"+dateFormat.format(new Date(_startTime)));
	sb.append(";end;"+dateFormat.format(new Date(_endTime)));
	sb.append(";durationSec;"+((_endTime-_startTime)/1000L));
	sb.append(";nodes;"+nodes());
	sb.append(";devices;"+devices());
	sb.append(";packets;"+packets());
	sb.append(";bytes;"+bytes());
	String record=formatRecord(_sessionID,
				   _nodeID,_deviceID,
				   "SessionSummary",SESSION_SUMMARY,
				   sb.toString())+"\n";
	return record;
    }

    public int devices(){
	int devices=0;
	try{
	    for(Enumeration e=_nodeData.elements();e.hasMoreElements();){
		spNodeData nd=(spNodeData)e.nextElement();
		devices+=nd.devices();
	    }
	}catch(NoSuchElementException e){
	    _profiler.error("portalSession.devices:exception: "+e);
	}
	return devices;
    }

    public int nodes(){
	int nodes=0;
	try{
	    for(Enumeration e=_nodeData.elements();e.hasMoreElements();){
		spNodeData nd=(spNodeData)e.nextElement();
		if(nd.node()>0L)
		    nodes++;
	    }
	}catch(NoSuchElementException e){
	    _profiler.error("portalSession.packets:exception: "+e);
	}
	return nodes;
    }

    public int packets(){
	int packets=0;
	try{
	    for(Enumeration e=_nodeData.elements();e.hasMoreElements();){
		spNodeData nd=(spNodeData)e.nextElement();
		packets+=nd.packets();
	    }
	}catch(NoSuchElementException e){
	    _profiler.error("portalSession.packets:exception: "+e);
	}
	return packets;
    }

    public long bytes(){
	long bytes=0;
	try{
	    for(Enumeration e=_nodeData.elements();e.hasMoreElements();){
		spNodeData nd=(spNodeData)e.nextElement();
		bytes+=nd.bytes();
	    }
	}catch(NoSuchElementException e){
	    _profiler.error("portalSession.bytes:exception: "+e);
	}
	return bytes;
    }

    public void setEnableNodeDetail(boolean value){
      	_enableNodeDetail=value;
    }
    public void setEnableDeviceDetail(boolean value){
	_enableDeviceDetail=value;
    }

    public String summarize(){
	StringBuffer sb=new StringBuffer();
	// return top level summary
	sb.append("# start session summary "+_sessionID+"\n");
	sb.append(this.toString());
	// if finer levels of detail are enabled,
	// add those to the summary, delegating everything
	// below the next level down
	_enableNodeDetail=true;
	_enableDeviceDetail=true;
	try{
	    sb.append("\n\n# Exceptions and Errors\n");
	    sb.append(_errorTypes.summarize());
	    sb.append(_exceptionTypes.summarize());
	    sb.append("\n");

	    if(_enableNodeDetail || _enableDeviceDetail){
		for(Enumeration e=_nodeData.elements();e.hasMoreElements();){
		    spNodeData nd=(spNodeData)e.nextElement();
		    nd.setEnableDeviceDetail(_enableDeviceDetail);
		    sb.append(nd.summarize());
		}
	    }
	}catch(NoSuchElementException e){
		    _profiler.error("portalSession.summarize:exception while processing nodes:"+e);
	}
	sb.append("# end session summary "+_sessionID+"\n");
	return sb.toString();
    }

    class spNodeData extends SessionParameter{
	EventTracker _connectNode= new EventTracker("connectNode",CONNECT_NODE);
	EventTracker _startSession= new EventTracker("startSession",START_SESSION);
	EventTracker _sendCommands= new EventTracker("sendCommands",SEND_COMMANDS);
	EventTracker _resetWDT= new EventTracker("resetWDT",RESET_WDT);
	EventTracker _syncNTP= new EventTracker("syncNTP",SYNC_NTP);
	EventTracker _deviceList= new EventTracker("deviceList",DEVICE_LIST);
	EventTracker _nodeList= new EventTracker("nodeList",NODE_LIST);
	EventTracker _getData= new EventTracker("getData",GET_DATA);
	EventTracker _retrieveDistributeData= new EventTracker("retrieveDistributeData",RETRIEVE_DISTRIBUTE_DATA);
	EventTracker _saveData= new EventTracker("saveData",SAVE_DATA);
	EventTracker _distributeData= new EventTracker("distributeData",DISTRIBUTE_DATA);
	EventTracker _publishData= new EventTracker("publishData",PUBLISH_DATA);
	EventTracker _disconnectNode = new EventTracker("disconnectNode",DISCONNECT_NODE);
	EventTracker _rxPacketSet=new EventTracker("rxPacketSet",RX_PACKET_SET);
	EventTracker _wakeSubnode=new EventTracker("wakeSubnode",WAKE_SUBNODE);
	EventTracker _processNode=new EventTracker("processNode",PROCESS_NODE);
	EventTracker _processPacketSet=new EventTracker("processPacketSet",PROCESS_PACKET_SET);
	EventTracker _unspecified=new EventTracker("unspecified",UNSPECIFIED);

	Vector _events = new Vector();

	int _deviceCount=0;
	int _subnodeCount=0;
	int _packetCount=0;
	int _totalBytes=0;

	boolean _enableDeviceDetail=false;

	Hashtable _deviceData=new Hashtable();

	public spNodeData(long sessionID, long nodeID, long deviceID){
	    super(sessionID,nodeID,deviceID);
	    initEvents();
	}
	public spNodeData(){
	    super();
	    initEvents();
	}

	private void initEvents(){
	    _events.add(_connectNode);
	    _events.add(_startSession);
	    _events.add(_resetWDT);
	    _events.add(_syncNTP);
	    _events.add(_deviceList);
	    _events.add(_sendCommands);
	    _events.add(_nodeList);
	    _events.add(_getData);
	    _events.add(_saveData);
	    _events.add(_retrieveDistributeData);
	    _events.add(_distributeData);
	    _events.add(_publishData);
	    _events.add(_disconnectNode);
	    _events.add(_rxPacketSet);
	    _events.add(_wakeSubnode);
	    _events.add(_processNode);
	    _events.add(_processPacketSet);
	    _events.add(_unspecified);
	}

	/** get DeviceData object with specified deviceID, 
	    create new one if it doesn't exist 
	*/
	public spDeviceData getDeviceData(long device){
	    Long lid=new Long(device);
	    spDeviceData dd = (spDeviceData)_deviceData.get(lid);
	    if(dd==null){
		dd=new spDeviceData(_sessionID,_nodeID,device);
		_deviceData.put(lid,dd);
	    }
	    return dd;
	}

	public  void notify(PortalEvent event){
	    switch(event.getID()){
	    case(PortalEvent.CONNECT_NODE_START):
		_connectNode.start(event.getTime());
		break;
	    case(PortalEvent.CONNECT_NODE_END):
		_connectNode.end(event.getTime());
		break;
	    case(PortalEvent.START_SESSION_START):
		_startSession.start(event.getTime());
		break;
	    case(PortalEvent.START_SESSION_END):
		_startSession.end(event.getTime());
		break;
	    case(PortalEvent.SEND_COMMANDS_START):
		_sendCommands.start(event.getTime());
		break;
	    case(PortalEvent.SEND_COMMANDS_END):
		_sendCommands.end(event.getTime());
		break;
	    case(PortalEvent.RESET_WDT_START):	
		_resetWDT.start(event.getTime());
		break;
	    case(PortalEvent.RESET_WDT_END):	    
		_resetWDT.end(event.getTime());
		break;
	    case(PortalEvent.SYNC_NTP_START):	    
		_syncNTP.start(event.getTime());
		break;
	    case(PortalEvent.SYNC_NTP_END):	    
		_syncNTP.end(event.getTime());
		break;
	    case(PortalEvent.DEVICE_LIST_START):
		_deviceList.start(event.getTime());
		break;
	    case(PortalEvent.DEVICE_LIST_END):	    
		_deviceList.end(event.getTime());
		break;
	    case(PortalEvent.GET_DATA_START):
		_getData.start(event.getTime());
		break;
	    case(PortalEvent.GET_DATA_END):
		_getData.end(event.getTime());
		break;
	    case(PortalEvent.PROCESS_NODE_START):
		_processNode.start(event.getTime());
		break;
	    case(PortalEvent.PROCESS_NODE_END):
		_processNode.end(event.getTime());
		break;
	    case(PortalEvent.RETRIEVE_DISTRIBUTE_DATA_START):	    
		_retrieveDistributeData.start(event.getTime());
		break;
	    case(PortalEvent.RETRIEVE_DISTRIBUTE_DATA_END):	    
		_retrieveDistributeData.end(event.getTime());
		break;
	    case(PortalEvent.RX_PACKET_SET_START):
		_rxPacketSet.start(event.getTime());
		break;
	    case(PortalEvent.RX_PACKET_SET_END):		
		_rxPacketSet.end(event.getTime());
		long deviceID=((PortalConnection)event.getSource()).getTargetDeviceID();
		spDeviceData spdd=getDeviceData(deviceID);
		spdd.notify(event);
		break;
	    case(PortalEvent.SAVE_DATA_START):	    
		_saveData.start(event.getTime());
		break;
	    case(PortalEvent.SAVE_DATA_END):	    
		_saveData.end(event.getTime());
		break;
	    case(PortalEvent.PROCESS_PACKET_SET_START):	    
		_processPacketSet.start(event.getTime());
		break;
	    case(PortalEvent.PROCESS_PACKET_SET_END):	    
		_processPacketSet.end(event.getTime());
		break;
	    case(PortalEvent.DISTRIBUTE_DATA_START):	    
		_distributeData.start(event.getTime());
		break;
	    case(PortalEvent.DISTRIBUTE_DATA_END):	    
		_distributeData.end(event.getTime());
		break;
	    case(PortalEvent.PUBLISH_DATA_START):	    
		_publishData.start(event.getTime());
		break;
	    case(PortalEvent.PUBLISH_DATA_END):	    
		_publishData.end(event.getTime());
		break;
	    case(PortalEvent.NODE_LIST_START):	    
		_nodeList.start(event.getTime());
		break;
	    case(PortalEvent.NODE_LIST_END):	    
		_nodeList.end(event.getTime());
		break;
	    case(PortalEvent.DISCONNECT_NODE_START):
		_disconnectNode.start(event.getTime());
		break;
	    case(PortalEvent.DISCONNECT_NODE_END):
		_disconnectNode.end(event.getTime());
		break;
	    case(PortalEvent.WAKE_SUBNODE_START):
		_wakeSubnode.start(event.getTime());
		break;
	    case(PortalEvent.WAKE_SUBNODE_END):
		_wakeSubnode.end(event.getTime());
		break;
	    case(PortalEvent.UNSPECIFIED_START):
		String msg=event.getMessage();
		_unspecified.start(event.getTime(),(msg!=null?msg:"-"));
		break;
	    case(PortalEvent.UNSPECIFIED_END):
		msg=event.getMessage();
		_unspecified.end(event.getTime(),(msg!=null?msg:"-"));
		break;
	    default:
		break;
	    }
	    
	}
	public  Object getParameter(int key){
	    return null;
	}

	public int nodes(){
	    return -1;
	}

	public int devices(){
	    return _deviceData.size();
	}

	public int packets(){
	    int packets=0;
	    try{
		for(Enumeration e=_deviceData.elements();e.hasMoreElements();){
		    spDeviceData dd=(spDeviceData)e.nextElement();
		    packets+=dd.packets();
		}
	    }catch(NoSuchElementException e){
		_profiler.error("nodeData.packets:exception: "+e);
	    }
	    return packets;
	}

	public long bytes(){
	    long bytes=0;
	    try{
		for(Enumeration e=_deviceData.elements();e.hasMoreElements();){
		    spDeviceData dd=(spDeviceData)e.nextElement();
		    bytes+=dd.bytes();
		}
	    }catch(NoSuchElementException e){
		_profiler.error("nodeData.bytes:exception: "+e);
	    }
	    return bytes;
	}

	public String toString(){
	    return "[NodeData content]\n";
	}

	public void setEnableDeviceDetail(boolean value){
	    _enableDeviceDetail=value;

	}
	public String summarize(){
	    StringBuffer sb=new StringBuffer();
	    // return top level summary
	    sb.append("\n# Node Data Summary (node "+_nodeID+")\n");
	    sb.append("# devices:"+ devices());
	    String s="";
	    s="\n# event summary:\n";
	    sb.append(s);

	    for(Enumeration ev=_events.elements();ev.hasMoreElements();){
		// add indivudual events
		EventTracker et = (EventTracker)ev.nextElement();
		Vector vEvents=et.getEvents();
		for(Enumeration ect=vEvents.elements();ect.hasMoreElements();){
		    String sCycleTime=(String)(ect.nextElement());
		    if(sCycleTime!=null){
			String record=formatRecord(_sessionID,
						   _nodeID,_deviceID,
						   "event",et.id(),
						   sCycleTime);
			sb.append(record+"\n");		   
		    } 
		}

		String sEvent=et.toString();
		if(sEvent!=null){
		    String record=formatRecord(_sessionID,
					       _nodeID,_deviceID,
					       "eventSummary",et.id(),
					       sEvent);
		    sb.append(record+"\n");
		}
	    }
	    //sb.append(this.toString());
	    // if finer levels of detail are enabled,
	    // add those to the summary, delegating everything
	    // below the next level down
	    if(_enableDeviceDetail){
		try{
		    for(Enumeration e=_deviceData.elements();e.hasMoreElements();){
			s="\n# device summary:\n";
			sb.append(s);
			spDeviceData dd=(spDeviceData)e.nextElement();
			sb.append(dd.summarize());			
		    }
		}catch(NoSuchElementException e){
		    _profiler.error("nodeData.summarize: exception while processing "+s+" for node "+_nodeID+":"+e);
		}
	    }
	    return sb.toString();
	}

    }

    class spDeviceData extends SessionParameter{
	int _totalPacketCount=0;
	int _sensorDataPacketCount=0;
	int _metadataPacketCount=0;
	int _messagePacketCount=0;
	int _measurementPacketCount=0;

	int _sensorDataPacketBytes=0;
	int _metadataPacketBytes=0;
	int _messagePacketBytes=0;
	int _measurementPacketBytes=0;

	int _sensorDataBytes=0;
	int _metadataDataBytes=0;
	int _messageDataBytes=0;
	int _measurementDataBytes=0;

	long _totalPacketBytes=0;
	long _totalDataBytes=0;

	private TypeCounter _recordTypes= new TypeCounter("record type",RECORD_TYPE);

	public spDeviceData(long session, long node, long device){
	    super(session,node,device);
	}

	public  void notify(PortalEvent event){
	    switch(event.getID()){
	    case(PortalEvent.EXCEPTION):
		break;
	    case(PortalEvent.RX_PACKET_SET_END):
		DevicePacketSet packetSet=event.getPacketSet();
		if(packetSet==null)
		    break;

		Vector packets=packetSet._packets;
		if(packets!=null){
		    try{
			for(Enumeration e=packets.elements();e.hasMoreElements();){
			    DevicePacket packet=(DevicePacket)e.nextElement();
			    processPacket(packet);
			}
		    }catch(NoSuchElementException e){
			_profiler.error("deviceData.notify:exception: "+e);
		    }

		}
		break;
	    default:
		break;
	    }
	}

	public void processPacket(DevicePacket packet){
	    PacketStats stats=new PacketStats(packet,false);
	    //uses size of packet data buffer(s) and header
	    // not including any serialization overhead
	    long packetSize=stats.getPacketSize();
	    long dataSize=stats.getDataSize();
	    _totalPacketBytes+=packetSize;
	    _totalDataBytes+=dataSize;
	    _totalPacketCount++;
	    
	    long recordType=packet.getRecordType();
	    _recordTypes.add(new Long(recordType));
	    
	    if(packet instanceof SensorDataPacket){
		_sensorDataPacketCount++;
		_sensorDataPacketBytes+=packetSize;
		_sensorDataBytes+=dataSize;
	    }
	    if(packet instanceof MetadataPacket){
		_metadataPacketCount++;
		_metadataPacketBytes+=packetSize;
		_metadataDataBytes+=dataSize;
	    }
	    if(packet instanceof DeviceMessagePacket){
		_messagePacketCount++;
		_messagePacketBytes+=packetSize;
		_messageDataBytes+=dataSize;
	    }
	    if(packet instanceof MeasurementPacket){
		_measurementPacketCount++;
		_measurementPacketBytes+=packetSize;
		_measurementDataBytes+=dataSize;
	    }
	    return;
	}

	public int packets(){
	    return _totalPacketCount;
	}

	public long bytes(){
	    return _totalPacketBytes;
	}
	
	public  Object getParameter(int key){
	    return null;
	}

	public String toString(){
	    return "[DeviceData content]\n";
	}

	public String summarize(){
	    StringBuffer sb=new StringBuffer();
	    //sb.append(this.toString());
	    sb.append("\n# device ID "+_deviceID);

	    sb.append("\n"+formatRecord(_sessionID,
					_nodeID,_deviceID,
					"total packets",TOTAL_PACKETS,
					Long.toString(_totalPacketCount)));
	    sb.append("\n"+formatRecord(_sessionID,
					_nodeID,_deviceID,
					"total packet bytes",TOTAL_PACKET_BYTES,
					Long.toString(_totalPacketBytes)));
	    sb.append("\n"+formatRecord(_sessionID,
					_nodeID,_deviceID,
					"total data bytes",TOTAL_DATA_BYTES,
					Long.toString(_totalDataBytes)));

	    if(_sensorDataPacketCount>0L){
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "sensor data packets",SENSORDATA_PACKETS,
					    Long.toString(_sensorDataPacketCount)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "sensor data packet bytes",SENSORDATA_PACKET_BYTES,
					    Long.toString(_sensorDataPacketBytes)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "sensor data data bytes",SENSORDATA_DATA_BYTES,
					    Long.toString(_sensorDataBytes)));
	    }
	    if(_metadataPacketCount>0L){
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "metadata packets",METADATA_PACKETS,
					    Long.toString(_metadataPacketCount)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "metadata packet bytes",METADATA_PACKET_BYTES,
					    Long.toString(_metadataPacketBytes)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "metadata bytes",METADATA_DATA_BYTES,
					    Long.toString(_metadataDataBytes)));
	    }

	    if(_messagePacketCount>0L){
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "message packets",MESSAGE_PACKETS,
					    Long.toString(_messagePacketCount)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "message packet bytes",MESSAGE_PACKET_BYTES,
					    Long.toString(_messagePacketBytes)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "message bytes",MESSAGE_DATA_BYTES,
					    Long.toString(_messagePacketBytes)));
	    }

	    if(_measurementPacketCount>0L){
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "measurement packets",MEASUREMENT_PACKETS,
					    Long.toString(_measurementPacketCount)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "measurement packet bytes",MEASUREMENT_PACKET_BYTES,
					    Long.toString(_measurementPacketBytes)));
		sb.append("\n"+formatRecord(_sessionID,
					    _nodeID,_deviceID,
					    "measurement bytes",MEASUREMENT_DATA_BYTES,
					    Long.toString(_measurementPacketBytes)));
	    }
	    sb.append("\n\n# Record Type Distribution ("+_deviceID+")");
	    sb.append("\n");
	    sb.append(_recordTypes.summarize());
	    sb.append("\n");

	    return sb.toString();
	}
    }


    public static void main(String args[]){
	PortalSession ps=new PortalSession();

	PortalEvent start=new PortalEvent(ps,PortalEvent.SESSION_START);
	ps.notify(start);

	try{
	    Thread.sleep(5000);
	}catch(Exception e){}

	System.out.println("timing 1000 events...");
	long startTimes[]=new long[1000];
	long endTimes[]=new long[1000];
	for(int i=0;i<1000;i++){
	    startTimes[i]=System.currentTimeMillis();
	    ps.notify(new PortalEvent(ps,PortalEvent.UNSPECIFIED_START));
	    endTimes[i++]=System.currentTimeMillis();
	    startTimes[i]=System.currentTimeMillis();
	    ps.notify(new PortalEvent(ps,PortalEvent.UNSPECIFIED_END));
	    endTimes[i]=System.currentTimeMillis();
	}

	double sum=0L;
	for(int i=0;i<1000;i++){
	    sum+=(double)(endTimes[i]-startTimes[i]);
	    //System.out.println(startTimes[i]+","+endTimes[i]+","+(endTimes[i]-startTimes[i])+","+sum);
	}
	System.out.println("average time to add event: "+(sum/1000L)+" sum="+sum);

	PortalEvent end=new PortalEvent(ps,PortalEvent.SESSION_END);
	ps.notify(end);
	
	System.out.println(ps);
	ps.setEnableNodeDetail(true);
	ps.setEnableDeviceDetail(true);
	ps.notify(new PortalEvent(ps,PortalEvent.SESSION_END));
	System.out.println(ps.summarize());

	TypeCounter tc = ps.new TypeCounter("foo",PortalSession.NULL_PARAMETER);
	tc.add("fu");
	tc.add("bar");
	tc.add("quux");
	tc.add("quux");
	tc.add("fu");
	System.out.println(tc.summarize());

    }

}

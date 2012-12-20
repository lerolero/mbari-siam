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
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import java.util.EventObject;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseDescription;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.distributed.portal.QueuedCommand;
import org.mbari.siam.distributed.portal.UnknownConfiguration;
import org.mbari.siam.operations.utils.ExportablePacket;
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


/**
 * PortalEvent
 * @author Kent Headley
 */
public class PortalEvent extends EventObject {
    /** log4j logger */
    static private Logger _log4j = Logger.getLogger(PortalEvent.class);

    // CVS revision 
    private static String _versionID = "$Revision: 1.2 $";

    public static final int NULL_ID=0;

    /** Event ID */
    protected int _id;

    /** timestamp */
    protected long _time=0L;

    protected boolean _consumed;

    /** DevicePacketSet member */
    DevicePacketSet _packetSet=null;

    /** Exception member */
    Exception _exception=null;

    /** DevicePacket member */
    DevicePacket _packet=null;

    /** String message member */
    String _message=null;


    /**  SESSION_START event ID */
    public static final int SESSION_START=10;

    /**  SESSION_END event ID */
    public static final int SESSION_END=15;

    /**  EXCEPTION event ID */
    public static final int EXCEPTION=20;

    /**  CONNECT_NODE event ID */
    public static final int CONNECT_NODE_START=30;
    public static final int CONNECT_NODE_END=35;

    /**  RESET_WDT event ID */
    public static final int RESET_WDT_START=40;
    public static final int RESET_WDT_END=45;

    public static final int SYNC_NTP_START = 50;	    
    public static final int SYNC_NTP_END = 55;	    

    public static final int DEVICE_LIST_START = 60;	    
    public static final int DEVICE_LIST_END = 65;	    

    /**  RX_PACKET_SET event ID */
    public static final int RX_PACKET_SET_START=70;
    public static final int RX_PACKET_SET_END=75;

    /**  START_DOWNLOAD event ID */
    public static final int GET_DATA_START=80;
    /**  END_DOWNLOAD event ID */
    public static final int GET_DATA_END=85;

    public static final int SAVE_DATA_START = 90;	    
    public static final int SAVE_DATA_END = 95;	    
    public static final int DISTRIBUTE_DATA_START = 100;	    
    public static final int DISTRIBUTE_DATA_END = 105;	    
    public static final int PUBLISH_DATA_START = 110;	    
    public static final int PUBLISH_DATA_END = 115;	    
    public static final int NODE_LIST_START = 120;	    
    public static final int NODE_LIST_END = 125;	    

    /**  DISCONNECT_NODE event ID */
    public static final int DISCONNECT_NODE_START=130;
    public static final int DISCONNECT_NODE_END=135;

    public static final int WAKE_SUBNODE_START = 140;	    
    public static final int WAKE_SUBNODE_END = 145;	    

    public static final int START_SESSION_START = 150;	    
    public static final int START_SESSION_END = 155;	    

    public static final int PROCESS_NODE_START = 160;	    
    public static final int PROCESS_NODE_END = 165;	    

    public static final int PROCESS_PACKET_SET_START = 170;	    
    public static final int PROCESS_PACKET_SET_END = 175;	    

    public static final int RETRIEVE_DISTRIBUTE_DATA_START = 180;	    
    public static final int RETRIEVE_DISTRIBUTE_DATA_END = 185;	    

    public static final int SEND_COMMANDS_START = 190;	    
    public static final int SEND_COMMANDS_END = 195;	    

    public static final int UNSPECIFIED_START = 200;	    
    public static final int UNSPECIFIED_END = 205;	    

    //public static final int _START = ;	    
    //public static final int _END = ;	    

    /** Constructs a  PortalEvent with the specified source and type */
    public PortalEvent(){
	this(null,NULL_ID);
    }

    /** Constructs a  PortalEvent with the specified source and type */
    public PortalEvent(Object source){
	this(source,NULL_ID);
    }

    /** Constructs a  PortalEvent with the specified source and type and PacketSet*/
    public PortalEvent(Object source, int id, DevicePacketSet packetSet){
	this(source,id);
	_packetSet=packetSet;
    }

    /** Constructs a  PortalEvent with the specified source and type and Exception*/
    public PortalEvent(Object source, int id, Exception exception){
	this(source,id);
	_exception=exception;
    }

    /** Constructs a  PortalEvent with the specified source and type and message*/
    public PortalEvent(Object source, int id, String message){
	this(source,id);
	_message=message;
    }


    /** Constructs a  PortalEvent with the specified source and type */
    public PortalEvent(Object source, int id){
	super(source);
	_id=id;
	_time=System.currentTimeMillis();
    }


    /** Returns the event timestamp */
    public long getTime(){
	return _time;
    }

    /** Returns the event type */
    public int getID(){
	return _id;
    }

    /** getException */
    public Exception getException(){
	return _exception;
    }

    /** getException */
    public void setException( Exception exception){
	 _exception=exception;
    }

    /** Attach a DevicePacketSet to this event */
    public void setPacketSet(DevicePacketSet packetSet){
	_packetSet=packetSet;
	return;
    }

    /** get the DevicePacketSet attached to this event */
    public DevicePacketSet getPacketSet(){
	return _packetSet;
    }

    /** get the message attached to this event */
    public String getMessage(){
	return _message;
    }

    /** Returns a string representing the state of this event */
    public String paramString(){
	return "ParamString";
    }

    /** Returns a string representation this event object */
    public String toString(){
	return "PortalEvent";
    }

    /** Consume event */
    protected void consume(){}

    /** Returns current value of consumed member */
    protected boolean isConsumed(){
	return _consumed;
    }

    /** Called by the garbage collector on an object when garbage collection determines that there are no more references to the object. */
    protected void finalize(){}
    

}

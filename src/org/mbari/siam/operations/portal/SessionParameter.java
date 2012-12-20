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
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.TimeZone;
import java.util.SimpleTimeZone;
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
import moos.ssds.jms.PublisherComponent;
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
 * SessionParameter
 * @author Kent Headley
 */
public abstract class SessionParameter {
    /** log4j logger */
    static private Logger _log4j = Logger.getLogger(SessionParameter.class);

    // CVS revision 
    private static String _versionID = "$Revision: 1.1 $";
    
    public static final long NULL_SESSION = 0L;
    public static final long NULL_NODE    = -1L;
    public static final long NULL_DEVICE  = -1L;
    public static final int NULL_PARAMETER  = -1;
    public static final int  NULL_KEY     = 0;

    long _sessionID = NULL_SESSION;
    long _nodeID    = NULL_NODE;
    long _deviceID  = NULL_DEVICE; 
    int _parameterID  = NULL_PARAMETER; 
    String _sessionName="unknown";
    long _startTime=0L;
    long _endTime=0L;
    static SimpleDateFormat dateFormat
	= new SimpleDateFormat ("MM/dd/yyyy HH:mm:ss zzz");
    String _delimiter=";";

    public SessionParameter(){
	super();
	dateFormat.setTimeZone((TimeZone)new SimpleTimeZone(0,"UTC"));
    }

    public SessionParameter(long session, long node, long device){
	super();
	_sessionID=session;
	_nodeID=node;
	_deviceID=device;
    }

    public abstract void notify(PortalEvent event);
    public abstract Object getParameter(int key);
    public abstract String summarize();


    public void setSessionName(String name){
	_sessionName=name;
    }
    public String sessionName(){
	return _sessionName;
    }
    public void setNodeID(long node){
	_nodeID=node;
    }
    public void setDeviceID(long device){
	_deviceID=device;
    }
    public void setSessionID(long sessionID){
	_sessionID=sessionID;
    }
    public long node(){
	return _nodeID;
    }
    public long session(){
	return _sessionID;
    }
    public long device(){
	return _deviceID;
    }
    public long startTime(){
	return _startTime;
    }
    public void setStartTime(long start){
	 _startTime=start;
    }
    public long endTime(){
	return _endTime;
    }
    public void setEndTime(long end){
	 _endTime=end;
    }



    public String formatRecord(String message){
	return formatRecord( System.currentTimeMillis(), NULL_NODE, NULL_DEVICE,"message",NULL_PARAMETER,message,_delimiter);
    }

    public String formatRecord(long timestamp,long nodeID,long deviceID,String parameterName,int parameterID,String value){
	return formatRecord( timestamp, nodeID, deviceID, parameterName,parameterID, value,_delimiter);
    }
    public String formatRecord(long timestamp,long nodeID,long deviceID,String parameterName,int parameterID,String value, String delimiter){
	StringBuffer sb=new StringBuffer();

	sb.append(dateFormat.format(new Date(timestamp))+delimiter);
	sb.append(nodeID+delimiter);
	sb.append(deviceID+delimiter);
	sb.append("\""+parameterName+"\""+delimiter);
	sb.append(parameterID+delimiter);
	sb.append(value);
	return sb.toString();
    }

    class TypeCounter extends Hashtable{
	String _name="null";
	int _parameterID=NULL_PARAMETER;

	public TypeCounter(){
	    super();
	}

	public TypeCounter(String name,int parameterID){
	    this();
	    _name=name;
	    _parameterID=parameterID;
	}

	public void add(Object key){
	    if(containsKey(key)){
		long count = ((Long)get(key)).longValue()+1L;
		put(key,new Long(count));
	    }else{
		put(key,new Long(1L));
	    }
	}
	public long count(Object key){
	    if(!containsKey(key))
		return -1L;
	    return ((Long)get(key)).longValue();
	}
	public String summarize(){
	    StringBuffer sb = new StringBuffer();
	    try{
		for(Enumeration keys=keys();keys.hasMoreElements();){
		    Object key=keys.nextElement();
		    String value="\""+key+"\""+_delimiter+((Long)get(key)).toString();
		    sb.append(formatRecord(_sessionID,_nodeID,_deviceID,_name,_parameterID,value));
		    sb.append("\n");
		}
	    }catch(NoSuchElementException e){
		_log4j.error("TypeCounter.summarize:exception: "+e);
	    }
	    return sb.toString();
	}
    }

    /** keep track of event state and basic statistics about the frequency
	and duration of events */
    class EventTracker {
	long _start=0L;
	long _end=Long.MAX_VALUE;
	long _cycleTime=Long.MAX_VALUE;
	String _startMessage=null;
	String _endMessage=null;
	boolean _pending=false;
	int _cycles=0;
	int _sequenceErrors=0;
	int _timeErrors=0;
	Vector _cycleTimes=new Vector();
	Vector _startTimes=new Vector();
	Vector _endTimes=new Vector();
	Vector _startMessages=new Vector();
	Vector _endMessages=new Vector();
	long _sum=0L;
	long _min=Long.MAX_VALUE;
	long _max=Long.MIN_VALUE;
	double _mean=0L;
	String _name="undefined";
	int _parameterID=NULL_PARAMETER;

	    public EventTracker(){
		super();
	    }
	    public EventTracker(String name, int parameterID){
		this();
		_name=name;
		_parameterID=parameterID;
	    }


	    public boolean start(long time){
		return start(time,"-");
	    }
	    public boolean start(long time, String message){
		boolean retval=true;
		if(_pending){
		    _sequenceErrors++;
		    retval=false;
		}
		_start=time;
		_end=Long.MAX_VALUE;
		_pending=true;
		_startMessage=message;
		return retval;
	    }
	    public boolean end(long time){
		return end(time,"-");
	    }

	    public boolean end(long time, String message){

		boolean retval=true;
		if(!_pending){
		    _sequenceErrors++;
		    retval=false;
		}
		if(time<_start){
		    _timeErrors++;
		    retval=false;
		}

		if(retval){
		    _cycles++;
		    _cycleTime=time-_start;
		    _endMessage=message;
		    _cycleTimes.add(new Long(_cycleTime));
		    _startTimes.add(new Long(_start));
		    _endTimes.add(new Long(time));

		    _sum+=_cycleTime;
		    if(_cycleTime>_max)
			_max=_cycleTime;
		    if(_cycleTime<_min)
			_min=_cycleTime;
		    if(_cycles>0)
			_mean=((double)_sum/(double)_cycles);
		}
		_end=time;
		_pending=false;

		_startMessages.add(_startMessage);
		_endMessages.add(_endMessage);
	 
		return retval;
	    }
	    public Vector cycleTimes(){return _cycleTimes;}
	    public Vector startTimes(){return _startTimes;}
	    public Vector endTimes(){return _endTimes;}
	    public long lastCycleTime(){return _cycleTime;}
	    public boolean pending(){return _pending;}
	    public int cycles(){return _cycles;}
	    public int sequenceErrors(){return _sequenceErrors;}
	    public int timeErrors(){return _timeErrors;}
	    public int errors(){return (_timeErrors+_sequenceErrors);}
	    public long min(){return _min;}
	    public long max(){return _max;}
	    public double mean(){return _mean;}
	    public double stdev(){
		long S=0L;
		double variance=0L;
		for(Enumeration e=_cycleTimes.elements();e.hasMoreElements();){
		    long xi=((Long)e.nextElement()).longValue();
		    S+=(xi-_mean)*(xi-_mean);
		}
		variance=((double)S/(double)_cycles);
		return Math.sqrt(variance);
	    }
	public int id(){return _parameterID;}

	public String toString(){
	    StringBuffer sb=new StringBuffer();
	    NumberFormat nf=NumberFormat.getInstance();
	    nf.setMaximumFractionDigits(3);
	    nf.setMinimumFractionDigits(3);
	    nf.setGroupingUsed(false);
	    if(_cycles > 0L){
		sb.append(_name+_delimiter);
		sb.append(dateFormat.format(new Date(_start))+_delimiter);

		long end=_end;
		if( _end>=Long.MAX_VALUE && !_endTimes.isEmpty() )
		    end=((Long)_endTimes.lastElement()).longValue();
		sb.append(dateFormat.format(new Date(end))+_delimiter);

		sb.append(_cycles+_delimiter);
		String s=_delimiter;
		if(_min>Long.MIN_VALUE && _min<Long.MAX_VALUE)
		    s=(_min+_delimiter);
		sb.append(s);
		s=_delimiter;
		if(_max>Long.MIN_VALUE && _max<Long.MAX_VALUE)
		    s=(_max+_delimiter);
		sb.append(s);
		sb.append(nf.format(_mean)+_delimiter);
		sb.append(nf.format(stdev())+_delimiter);
		sb.append(_sum);
		return sb.toString();
	    }
	    return null;
	}

	public Vector getEvents(){
	    Vector retval=new Vector();
	    if(_cycles > 0L){
		Enumeration t=_cycleTimes.elements();
		Enumeration s=_startTimes.elements();
		Enumeration e=_endTimes.elements();
		Enumeration ms=_startMessages.elements();
		Enumeration me=_endMessages.elements();
		while(t.hasMoreElements()){
		    StringBuffer sb=new StringBuffer();
		    long cycleTime=((Long)t.nextElement()).longValue();
		    long startTime=((Long)s.nextElement()).longValue();
		    long endTime=((Long)e.nextElement()).longValue();
		    String startMessage=(String)ms.nextElement();
		    String endMessage=(String)me.nextElement();
		    sb.append(_name+_delimiter);
		    sb.append(startTime+_delimiter);
		    sb.append(endTime+_delimiter);
		    sb.append(cycleTime+_delimiter);
		    sb.append(startMessage+_delimiter);
		    sb.append(endMessage);
		    retval.add(sb.toString());
		}
	    }
	    return retval;
	}
    }

}

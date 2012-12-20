// Copyright MBARI 2003
package org.mbari.siam.moos.utils.chart.sampler;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;

import java.io.OutputStream;
import java.io.IOException;

import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;
import java.lang.Math;
import java.util.Random;

import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DevicePacketStream;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.portal.PortalProxy;
import org.mbari.siam.distributed.Authentication;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class SamplerImpl extends UnicastRemoteObject implements Sampler {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(SamplerImpl.class);

    protected String _nodeURL = null;
    protected String _portalURL = null;
    protected String _commPortName = null;
    protected String _dataItem = null;
    private Thread _myThread=null;
    protected boolean _isRunning=false;
    protected boolean _isPaused=false;
    protected OutputStream _ostream;
    protected long _deviceID=0;
    protected boolean _isApplet = false;
    protected ParserFactory _parserFactory = ParserFactory.getInstance();
    protected DevicePacketStream _packetStream=null;
    protected PortalProxy _portalProxy=null;

    public SamplerImpl()
	throws RemoteException{
	super();
    }

    /** Test code */
    public double pickANumber() 
	throws RemoteException{
	Random random = new Random();
	double barf =  Math.sin((2.0*random.nextDouble()-1.0)*50);
	//_log4j.debug("I'd rather pick a number than my nose: "+barf);
	return barf;
    }
    public void setApplet(boolean isApplet){
	_isApplet=isApplet;
    }

    public void setNode(String node) throws RemoteException{
	_nodeURL = node;
    }
    public void setPortal(String portal) throws RemoteException{
	_portalURL = portal;
    }
    public void setPort(String port)throws RemoteException{
	_commPortName = port;
    }
    public void setDeviceID(long id) throws RemoteException{
	_deviceID=id;
	    }
    public void setDataItem(String item) throws RemoteException{
	_dataItem = item;
    }

    public double getNodeSample(long isiID,String dataItem)throws RemoteException{
	SensorDataPacket packet = getPacketFromNode(isiID);
	if(packet==null)
	    throw new RemoteException("SamplerImpl.getSample: Could not obtain packet");
	SensorPacketParser parser = (SensorPacketParser)_parserFactory.getParser(isiID);
	if(parser!=null){
	    _log4j.debug("Parsing packet id="+isiID);
	    parser.parse(packet);
	    _log4j.debug("Getting item "+dataItem);
	    Double ret = (Double)parser.get(dataItem);
	    _log4j.debug("returning value "+ret);
	    return ret.doubleValue();
	}else{
	    _log4j.debug("Could not obtain a parser for ID "+isiID);
	    return 0;
	}
    }

    public double getPortalSample(long isiID,String dataItem) throws RemoteException{
	SensorDataPacket packet = getPacketFromStream(isiID);
	if(packet==null)
	    throw new RemoteException("SamplerImpl.getSample: Could not obtain packet");
	SensorPacketParser parser = (SensorPacketParser)_parserFactory.getParser(isiID);
	if(parser!=null){
	    _log4j.debug("Parsing packet id="+isiID);
	    parser.parse(packet);
	    _log4j.debug("Getting item "+dataItem);
	    Double ret = (Double)parser.get(dataItem);
	    _log4j.debug("returning value "+ret);
	    return ret.doubleValue();
	}else{
	    _log4j.debug("Could not obtain a parser for ID "+isiID);
	    return 0;
	}
    }

    public Vector getParsedPacket(long isiID) throws RemoteException{
	SensorDataPacket packet = getPacketFromStream(isiID);
	SensorPacketParser parser = (SensorPacketParser)_parserFactory.getParser(isiID);
	if(parser!=null)
	    return parser.getAll();
	else
	    return null;
    }

    public DevicePacketStream getPacketStream(){
	if(_packetStream != null)
	    return _packetStream;

	try {
	    _log4j.debug("Looking for portal proxy at " + _portalURL);

	    //need to setSecurityManager 
	    if ( System.getSecurityManager() == null ){
		System.setSecurityManager(new SecurityManager());
	    }

	    // Get a portal proxy
	    if(_portalProxy==null)
		_portalProxy = (PortalProxy)Naming.lookup(_portalURL);

	    // Now get a device packet stream
		_packetStream = _portalProxy.getDevicePacketStream(new Authentication());
		return _packetStream;
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    _log4j.error("Exception: " + e.getMessage());
	}
	return null;
    }

    public SensorDataPacket getPacketFromStream(long isiID){
	if( getPacketStream()==null){
	    _log4j.debug("getPacketFromStream: Whoa! couldn't get packet stream!");	    
	    return null;
	}
	while(true){
	    try{
		_log4j.debug("getPacketFromStream: Blocking for packet...");
		DevicePacket packet = _packetStream.read();
		if(packet instanceof SensorDataPacket){
		    if(packet.sourceID()==isiID)
			return (SensorDataPacket)packet;
		    else
			_log4j.debug("getPacketFromStream: not a "+isiID+" packet");
		}else
		    _log4j.debug("getPacketFromStream: not a SensorDataPacket");
	    }catch(IOException e){
		_log4j.error("getPacketFromStream: caught IOException: "+e);
	    }
	    catch(ClassNotFoundException c){
		_log4j.error("getPacketFromStream: caught IOException: "+c);
	    }
	}
    }

    public SensorDataPacket getPacketFromNode(long isiID){
	/*
	SensorDataPacket p=new SensorDataPacket(999,256);
	p.setSequenceNo(0);
	p.setSystemTime(System.currentTimeMillis());
	Random random=new Random();
	double foo=(20+(2.0*random.nextDouble()-1.0)*10.0);
	p.setDataBuffer(("$pdata, T"+foo+", P2.0,H3.0,GFLO4.0, GFHI5.0,*3456").getBytes());
	return p;
	*/
	
	try {
	    //_log4j.debug("Looking for node server stub at " + _nodeURL);
	    //need to setSecurityManager 
	    if ( System.getSecurityManager() == null ){
		//_log4j.debug("getPacket(): Getting Security Manager");
		System.setSecurityManager(new SecurityManager());
	    }

	    Node node = (Node )Naming.lookup(_nodeURL);

	    //_log4j.debug("Got proxy for node service \"" + 
	    //	       new String(node.getName()) + "\"");
	    
	    // Now sample instruments on specified ports
	    try {
		Device device = node.getDevice(isiID);
		if (device instanceof Instrument) {
		    Instrument instrument = (Instrument )device;
		    SensorDataPacket packet = instrument.acquireSample(false);
		    //_log4j.debug(packet.toString());
		    return packet;
		}
		else {
		    _log4j.error("Device on port " + _commPortName + 
				       " is not an Instrument");
		}
	    }
	    catch (DeviceNotFound e) {
		_log4j.error("Device not found on port " + _commPortName);
	    }
	    catch (NoDataException e) {
		_log4j.error("No data from instrument on port " + _commPortName);
	    }
	    
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    _log4j.error("Exception: " + e.getMessage());
	}
	return null;
    }
}



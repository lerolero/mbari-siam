// Copyright MBARI 2003
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.awt.event.ActionEvent;
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

import org.mbari.siam.distributed.Port;
import org.mbari.siam.operations.utils.ListNodePorts;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DevicePacketStream;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.portal.PortalProxy;
import org.mbari.siam.distributed.Authentication;
import org.mbari.siam.distributed.PortOccupiedException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;

public class NodeProxyImpl extends UnicastRemoteObject implements NodeProxy {
    /** log4j logger */
    protected static Logger _log4j = Logger.getLogger(NodeProxyImpl.class);

    /** for now, we use a factory method to retrieve
        custom made parsers; later, we'll use XML or SSDS interface
    */
    protected ParserFactory _parserFactory = ParserFactory.getInstance();

    /** Default Constructor */
    public NodeProxyImpl()
	throws RemoteException{
	super();
    }

    /** Test code generates a random number */
    public double pickANumber() 
	throws RemoteException{
	Random random = new Random();
	double barf =  Math.sin((2.0*random.nextDouble()-1.0)*50);
	return barf;
    }

    /** Get and parse a sample from device with isiID on a specified node;
	Parser is expected to recognize dataItem mnemonic to identify the
	particular data item to be returned.
     */
    public double getNodeSample(String nodeURL,long isiID,String dataItem)throws RemoteException{
	SensorDataPacket packet = getPacketFromNode(nodeURL,isiID);
	if(packet==null)
	    throw new RemoteException("NodeProxyImpl.getSample: Could not obtain packet");
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

    /** Get and parse a sample from device with isiID from a portal packet stream;
	Parser is expected to recognize dataItem mnemonic to identify the
	particular data item to be returned.
     */
    public double getPortalSample(String portalURL,long isiID,String dataItem) throws RemoteException{
	SensorDataPacket packet = getPacketFromStream(portalURL,isiID);
	if(packet==null)
	    throw new RemoteException("NodeProxyImpl.getSample: Could not obtain packet");
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

    /** Get a vector of all data contained in a specified packet
	from a portal packet stream
     */
    public Vector getParsedPacket(String portalURL,long isiID) throws RemoteException{
	SensorDataPacket packet = getPacketFromStream(portalURL,isiID);
	SensorPacketParser parser = (SensorPacketParser)_parserFactory.getParser(isiID);
	if(parser!=null)
	    return parser.getAll();
	else
	    return null;
    }

    /** Return a device packet stream from the specified portal */
    public DevicePacketStream getPacketStream(String portalURL){

	try {
	    _log4j.debug("Looking for portal proxy at " + portalURL);

	    //need to setSecurityManager 
	    if ( System.getSecurityManager() == null ){
		System.setSecurityManager(new SecurityManager());
	    }

	    // Get a portal proxy
	    PortalProxy portalProxy = (PortalProxy)Naming.lookup(portalURL);

	    // Now get a device packet stream
		DevicePacketStream packetStream = portalProxy.getDevicePacketStream(new Authentication());
		return packetStream;
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    _log4j.error("Exception: " + e.getMessage());
	}
	return null;
    }

    /** Get a packet from a portal data stream */
    public SensorDataPacket getPacketFromStream(String portalURL,long isiID){

	DevicePacketStream packetStream=getPacketStream(portalURL);
	if( packetStream==null){
	    _log4j.debug("getPacketFromStream: Whoa! couldn't get packet stream!");	    
	    return null;
	}
	while(true){
	    try{
		_log4j.debug("getPacketFromStream: Blocking for packet...");
		DevicePacket packet = packetStream.read();
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

    /** Set security manager is it has not been set */
    public void setSecurityManager(){
	try {

	    //need to setSecurityManager 
	    if ( System.getSecurityManager() == null ){
		//_log4j.debug("getPacket(): Getting Security Manager");
		System.setSecurityManager(new SecurityManager());
	    }
	}
	catch (Exception e) {
	    _log4j.error("Exception: " + e.getMessage());
	}

    }

    /** Return the specified node service */
    public Node getNodeService(String nodeURL){
	Node nodeService=null;
	setSecurityManager();
	try {
	    _log4j.debug("Looking for node service at " + nodeURL);

	    nodeService = (Node )Naming.lookup(nodeURL);
	    
	    //_log4j.debug("Got proxy for node service \"" + 
	    //	       new String(nodeService.getName()) + "\"");
	    
	}
	catch (Exception e) {
	    _log4j.error("Caught exception: " + e.getMessage());
	    _log4j.error("Couldn't get service at \"" + nodeURL + "\"");
	    return null;
	}
	return nodeService;
    }

    /** Return a device service with the specified id from the 
	specified node
     */
    public Device getDevice(Node node,long id){
	Device device=null;
	try {
	    device = node.getDevice(id);
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (DeviceNotFound e) {
	    _log4j.error("Device not found on port for id " + id);
	}

	return device;	
    }

    /** Request a data packet w/o logging from the specified node */
    public SensorDataPacket getPacketFromNode(String nodeURL,long isiID){

	/*
	SensorDataPacket p=new SensorDataPacket(999,256);
	p.setSequenceNo(0);
	p.setSystemTime(System.currentTimeMillis());
	Random random=new Random();
	double foo=(20+(2.0*random.nextDouble()-1.0)*10.0);
	p.setDataBuffer(("$pdata, T"+foo+", P2.0,H3.0,GFLO4.0, GFHI5.0,*3456").getBytes());
	return p;
	*/
	
	setSecurityManager();
	Node node = getNodeService(nodeURL);
	Device device=getDevice(node,isiID);
	try{
	if (device instanceof Instrument) {
	    Instrument instrument = (Instrument )device;
	    SensorDataPacket packet = instrument.acquireSample(false);
	    //_log4j.debug(packet.toString());
	    return packet;
	}
	else {
	    _log4j.error("Device on port for id " + isiID + 
			       " is not an Instrument");
	}
	}catch(NoDataException n){
	    _log4j.error("NoDataException: " + n.getMessage());
	}catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}

	//_log4j.debug("Got proxy for node service \"" + 
	//	       new String(node.getName()) + "\"");
	    	    
	return null;
    }

    /** Return an array of all available ports on the 
	specified node
     */
    public Port[] getPorts(String nodeURL) throws RemoteException{
	Node nodeService = null;
	Port[] ports = null;
	String portStatus=null;

	setSecurityManager();

	nodeService = getNodeService(nodeURL);
	
	try {
	    ports = nodeService.getPorts();
	    
	    _log4j.debug("Node has " + ports.length + " ports\n");
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    _log4j.error("Got some exception: " + e.getMessage());
	}
	
	return ports;

    }

    /** Return an array of all available ports on the 
	specified node
     */
    public Port[] getPorts(Node nodeService) throws RemoteException{

	Port[] ports = null;
	String portStatus=null;

	setSecurityManager();

	try {
	    ports = nodeService.getPorts();
	    
	    _log4j.debug("Node has " + ports.length + " ports\n");
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    _log4j.error("Got some exception: " + e.getMessage());
	}
	
	return ports;

    }

    /** Get an array of strings indicating the status of all
	ports on a given node
     */
    public String[][] getPortStatusStrings(String nodeURL) 
	throws RemoteException{
	String statusStrings[][]=null;
	String test=null;
	Device device;
	Node node = getNodeService(nodeURL);
	Port ports[]=getPorts(node);

	if( (ports!=null) && (ports.length>0) ){
	    statusStrings=new String[ports.length][4];
	    for(int i=0;i<ports.length;i++){
		try{
		    if(ports[i]!=null){
			test=new String(ports[i].getName());
			statusStrings[i][0]=((test==null?"-":test));
			test=new String(ports[i].getServiceMnemonic());
			statusStrings[i][1]=((test==null?"-":test));
			test=Long.toString(ports[i].getDeviceID());
			statusStrings[i][2]=((test==null?"-":test));
			try{
			    device=node.getDevice(ports[i].getDeviceID());
			    test=ListNodePorts.statusMnem(device.getStatus());
			    statusStrings[i][3]=((test==null?"-":test));
			}catch(DeviceNotFound d){
			    statusStrings[i][3]=("-");
			}catch(NullPointerException n){
			    statusStrings[i][3]=("-");
			}
		    }else{
			statusStrings[i][0]=("-");
			statusStrings[i][1]=("-");
			statusStrings[i][2]=("-");
			statusStrings[i][3]=("-");
		    }
		}catch(DeviceNotFound d){
		    _log4j.error("DeviceNotFound: "+d);
		}
		catch(RemoteException r){
		    _log4j.error("RemoteException: "+r);		
		}
	    }
	}
	return statusStrings;
    }

    /** Do a specified operation (event Action command) on a node
	for a given set of ports
    */
    public void doPortOperation(ActionEvent e, String nodeURL, String selectedPorts[]) 
	throws RemoteException{

	if(e.getActionCommand().equals("SCAN")){

	    String scanPorts="";
	    try{
		Node node = getNodeService(nodeURL);

		for(int port=0;port<selectedPorts.length;port++){
		    node.scanPort(selectedPorts[port].getBytes());
		    scanPorts+=(selectedPorts[port]+" ");
		}
		_log4j.debug("SCANNING PORTS: "+nodeURL+" "+scanPorts);
	    }catch(RemoteException r1){
		_log4j.error(r1);
	    }
	    catch (IOException ie) {
		_log4j.error(ie);
	    }catch(PortNotFound p1){
	    }catch(DeviceNotFound d1){
	    }catch(DuplicateIdException d2){
	    }catch(PortOccupiedException po1){
	    }catch(NullPointerException np1){}
	}
	if(e.getActionCommand().equals("SHUTDOWN")){

	    String scanPorts="";
	    try{
		Node node = getNodeService(nodeURL);
	    
		for(int port=0;port<selectedPorts.length;port++){
		    node.shutdownDeviceService(selectedPorts[port].getBytes());
		    scanPorts+=(selectedPorts[port]+" ");
		}
		_log4j.debug("SHUTDOWN PORTS: "+nodeURL+" "+scanPorts);
	    }catch(RemoteException r2){
		_log4j.error(r2);
	    }catch(PortNotFound p2){
	    }catch(DeviceNotFound d2){
	    }catch(NullPointerException np2){}
	}
	if(e.getActionCommand().equals("SUSPEND")){

	    String scanPorts="";
	    try{
		Node node = getNodeService(nodeURL);
	    
		for(int port=0;port<selectedPorts.length;port++){
		    node.suspendService(selectedPorts[port].getBytes());
		    scanPorts+=(selectedPorts[port]+" ");
		}
		_log4j.debug("SUSPEND PORTS: "+nodeURL+" "+scanPorts);
	    }catch(RemoteException r3){
		_log4j.error(r3);
	    }catch(PortNotFound p3){
	    }catch(DeviceNotFound d3){
	    }catch(NullPointerException np3){}
	}
	if(e.getActionCommand().equals("RESUME")){

	    String scanPorts="";
	    try{
		Node node = getNodeService(nodeURL);
	    
		for(int port=0;port<selectedPorts.length;port++){
		    node.resumeService(selectedPorts[port].getBytes());
		    scanPorts+=(selectedPorts[port]+" ");
		}
		_log4j.debug("RESUME PORTS: "+nodeURL+" "+scanPorts);
	    }catch(RemoteException r4){
		_log4j.error(r4);
	    }catch(PortNotFound p4){
	    }catch(DeviceNotFound d4){
	    }catch(NullPointerException np4){}
	}
	return;
    }

}







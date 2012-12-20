/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.osdt;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.net.*;
import java.io.*;
import java.text.NumberFormat;

import com.rbnb.sapi.*;
import org.mbari.siam.foce.devices.controlLoop.OSDTConnectorWorker;
import org.mbari.siam.foce.devices.controlLoop.OSDTInputConnector;
import org.mbari.siam.foce.devices.controlLoop.WorkerThread;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.utils.TCPServer;
import org.mbari.siam.utils.TCPProtocol;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class OSDTTestServer extends TCPServer{
	static protected Logger _log4j = Logger.getLogger(OSDTTestServer.class);  

	String osdtServer="localhost:3333";
	String dataSourceName="OSDTTestServer";
	Source source;
	Vector workers=new Vector();
	Vector _channels=new Vector();
	NumberFormat nf=null;
	ListenerWorker _lworker=null;
	
	//Vector<OSDTTestServer.ChannelSpec> _channels=new Vector<OSDTTestServer.ChannelSpec>();

	public OSDTTestServer(){
		super();
		
		nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(5);
		nf.setMinimumFractionDigits(3);
		nf.setMinimumIntegerDigits(1);
		nf.setGroupingUsed(false);
	}
	public OSDTTestServer(String args[]) throws Exception{
		this();
		configure(args);
		_protocol=new OSDTTestProtocol(this);
	}
	
	public void connect()
	throws Exception{
		System.out.println("starting source...");
		//_log4j.debug("starting source...");
		startSource();
		System.out.println("starting workers...");
		//_log4j.debug("starting workers...");
		startWorkers();
		System.out.println("starting listener...");
		//_log4j.debug("starting listener...");
		_lworker=new ListenerWorker(this);
		_lworker.start();
		System.out.println("connected");
		//_log4j.debug("connected");
	}
	
	public void addChannel(ChannelSpec channel){
		_channels.add(channel);
	}
		
	public void startSource()
	throws Exception{
		// Create both a source and a sink, connect both:
		source=new Source();
		_log4j.debug("opening server connection...");
		source.OpenRBNBConnection(osdtServer,dataSourceName);
		while(!source.VerifyConnection()){
			_log4j.debug("validating connection...");
			WorkerThread.delay(1000L);
		}
	}

	public void startWorkers()
	throws Exception{
		_log4j.debug("starting "+_channels.size()+" channels...");
		for(Iterator i=_channels.iterator();i.hasNext();){
			ChannelSpec cspec=(ChannelSpec)i.next();
			_log4j.debug(cspec);
			OSDTTestWorker worker=new OSDTTestWorker(source,cspec);
			new Thread(worker).start();
			workers.add(worker);
		}
	}
	
	public OSDTTestWorker getWorker(String workerName){
		_log4j.debug("instance has "+workers.size()+" workers");
		for(Iterator i=workers.iterator();i.hasNext();){
			OSDTTestWorker worker=(OSDTTestWorker)i.next();
			if(worker._channel._name.equals(workerName)){
				_log4j.debug("found worker ["+worker._channel._name+"]");
				return worker;
			}
		}
		return null;
	}
	public OSDTTestWorker getWorker(int channelNumber){
		_log4j.debug("instance has "+workers.size()+" workers");
		if (channelNumber<0) {
			_log4j.error("channel number <0 ["+channelNumber+"]");
			return null;
		}
		if(workers.size()>channelNumber){
			return (OSDTTestWorker)workers.get(channelNumber);
		}else {
			_log4j.error("channel number >max ["+channelNumber+">"+(workers.size()-1)+"]");
		}

		return null;
	}
	
	public void shutdown(){
		
		for(Iterator i=workers.iterator();i.hasNext();){
			OSDTTestWorker worker=(OSDTTestWorker)i.next();
			_log4j.debug("terminating worker "+worker._channel._name);
			worker.terminate();
		}
		_log4j.debug("closing server connection...");
		source.CloseRBNBConnection();
	}
	
	public class ListenerWorker extends Thread{
		OSDTTestServer _server=null;
		public ListenerWorker(OSDTTestServer server){
			super("TestServer Listener");	
			_server=server;
		}
		public void run(){
			try{
			_server.listen();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public class ChannelSpec{
		
		public String _name;
		public int _typeID;
		//public int _index=0;
		public long _periodMillisec;
		Number _numberValue;
		String _stringValue;
		NumberFormat nf=NumberFormat.getInstance();
		public ChannelSpec(String name, int type, long periodMillisec){
			_name=name;
			_typeID=type;
			_periodMillisec=periodMillisec;
			nf=NumberFormat.getInstance();
			nf.setMaximumFractionDigits(5);
			nf.setMinimumFractionDigits(3);
			nf.setMinimumIntegerDigits(1);
			nf.setGroupingUsed(false);			
			//_index=index;
		}
		public ChannelSpec(String name, String type, long periodMillisec)
		throws Exception{
			_name=name;
			_typeID=parseType(type);
			_periodMillisec=periodMillisec;
			nf=NumberFormat.getInstance();
			nf.setMaximumFractionDigits(5);
			nf.setMinimumFractionDigits(3);
			nf.setMinimumIntegerDigits(1);
			nf.setGroupingUsed(false);			
			//_index=index;
		}
		public int parseType(String typeString) throws Exception{
			if(typeString.equalsIgnoreCase("FLOAT64"))
				return ChannelMap.TYPE_FLOAT64;
			if(typeString.equalsIgnoreCase("FLOAT32"))
				return ChannelMap.TYPE_FLOAT32;
			if(typeString.equalsIgnoreCase("INT64"))
				return ChannelMap.TYPE_INT64;
			if(typeString.equalsIgnoreCase("INT32"))
				return ChannelMap.TYPE_INT32;
			if(typeString.equalsIgnoreCase("INT16"))
				return ChannelMap.TYPE_INT16;
			if(typeString.equalsIgnoreCase("INT8"))
				return ChannelMap.TYPE_INT8;
			if(typeString.equalsIgnoreCase("STRING"))
				return ChannelMap.TYPE_STRING;
			throw new Exception("Invalid type ["+typeString+"]");
		}
		public String toString(){
			return (_name+","+typeName(_typeID)+","+_periodMillisec+","+stringValue());
		}
		
		public String stringValue(){
			if(_typeID==ChannelMap.TYPE_STRING)
				return _stringValue;
			else
				return nf.format(doubleValue());
		}
		public double doubleValue(){return _numberValue.doubleValue();}
		public float floatValue(){return _numberValue.floatValue();}
		public long longValue(){return _numberValue.longValue();}
		public int intValue(){return _numberValue.intValue();}
		public short shortValue(){return _numberValue.shortValue();}
		public byte byteValue(){return _numberValue.byteValue();}
		
		public void setValue(Number numberValue){
			_numberValue=numberValue;
		}
		public void setValue(String value)
		throws Exception{
			switch (_typeID) {
				case ChannelMap.TYPE_STRING:
					_stringValue=value;
					break;
				case ChannelMap.TYPE_FLOAT64:
					setValue(new Double(Double.parseDouble(value)));
					break;
				case ChannelMap.TYPE_FLOAT32:
					setValue(new Float(Float.parseFloat(value)));
					break;
				case ChannelMap.TYPE_INT64:
					setValue(new Long(Long.parseLong(value)));
					break;
				case ChannelMap.TYPE_INT32:
					setValue(new Integer(Integer.parseInt(value)));
					break;
				case ChannelMap.TYPE_INT16:
					setValue(new Short(Short.parseShort(value)));
					break;
				case ChannelMap.TYPE_INT8:
					setValue(new Byte(Byte.parseByte(value)));
					break;
				default:
					throw new Exception("invalid type ID converting "+value+" ["+_typeID+"]");
			}
			return;
		}
		
		public void setPeriod(long periodMsec )
		throws Exception{
			if(periodMsec<=0L){
				throw new Exception("invalid period ["+periodMsec+"] must be > 0L");
			}
			_periodMillisec=periodMsec;
		}
		
		public String typeName(int type){
			switch (type) {
				case ChannelMap.TYPE_FLOAT64:
					return "FLOAT64";
				case ChannelMap.TYPE_FLOAT32:
					return "FLOAT32";
				case ChannelMap.TYPE_INT64:
					return "INT64";
				case ChannelMap.TYPE_INT32:
					return "INT32";
				case ChannelMap.TYPE_INT16:
					return "INT16";
				case ChannelMap.TYPE_INT8:
					return "INT8";
				case ChannelMap.TYPE_STRING:
					return "STRING";
				default:
					break;
			}
			return null;
		}
	}
	
	protected ChannelSpec parseChannel(String channelString) 
	throws Exception{
		
		StringTokenizer st=new StringTokenizer(channelString,",");
		String cname=st.nextToken();
		String ctype=st.nextToken();
		String cperiod=st.nextToken();
		String cvalue=null;
		
		OSDTTestServer.ChannelSpec channelSpec=new OSDTTestServer.ChannelSpec(cname, ctype,Long.parseLong(cperiod));		
		// process optional initial value
		if(st.hasMoreTokens()){
			cvalue=st.nextToken();
			channelSpec.setValue(cvalue);
		}
		return channelSpec;
	}
		
	public void printHelp(){
		StringBuffer sb=new StringBuffer();
		sb.append("\n");
		sb.append("OSDT Test Server - Creates Open Source Data Turbine streams for testing\n");
		sb.append("\n");
		sb.append("testHarnessServer [-h <host> -p <port> -s <name> -c <channel> -f <file> -help]\n");
		sb.append("\n");
		sb.append("-h <host>    : OSDT server host address (addr[:port]) ["+osdtServer+"]\n");
		sb.append("-p <port>    : test server TCP/IP port                ["+_tcpPort+"]\n");
		sb.append("-s <name>    : OSDT data source name                  ["+dataSourceName+"]\n");
		sb.append("-c <channel> : channel specification (see below)\n");
		sb.append("-f <file>    : configuration file path\n");
		sb.append("\n");
		sb.append("channelSpec:name,type,periodMsec[,initialValue]\n");
		sb.append("where\n");
		sb.append("name       - channel name, e.g. pH1/pH\n");
		sb.append("type       - data type, one of FLOAT64|FLOAT32|INT64|INT32|INT16|INT8|STRING\n");
		sb.append("periodMsec - update period, in milliseconds\n");
		sb.append("initialValue - optional initial value\n");
		sb.append("\n");
		sb.append("A configuration file (specified with the -f option) may be used to set all options\n");
		sb.append("e.g. testHarnessServer -f /path/to/configFile:\n");
		sb.append("\n");
		sb.append("       serverHost=localhost\n");
		sb.append("       serverPort=4445\n");
		sb.append("       sourceName=OSDTTestServer\n");
		sb.append("\n");
		sb.append("       channel.0=pH1/pH,FLOAT64,10000,6.5\n");
		sb.append("       channel.1=pH2/pH,FLOAT64,10000,6.5\n");
		sb.append("\n");
		System.out.println(sb.toString());
	}

	/** Configure all settings (interface for to create server programatically)
		To use defaults set to null (or <0 for serverPort)
	 */
	public void configure(String sourceName, int serverPort, String osdtServer, String propertiesFile,Vector channels)
	throws Exception{
	
		if(propertiesFile!=null){
			loadConfiguration(propertiesFile);
		}
		if(sourceName!=null){
			dataSourceName=sourceName;
		}
		if(serverPort>0){
			_tcpPort=serverPort;
		}
		if (osdtServer!=null) {
			this.osdtServer=osdtServer;
		}
		if (channels!=null) {
			_channels=channels;
		}
		if(_protocol==null){
			_protocol=new OSDTTestProtocol(this);
		}
	}
	
	/** Configure all settings from properties file 	 
	 */
	public void loadConfiguration(String propertiesFile)
	throws FileNotFoundException, IOException, Exception{
		// read configuration from a properties file
		_log4j.debug("configuring from properties file:"+propertiesFile);
		FileInputStream fis=new FileInputStream(propertiesFile);
		Properties properties=new Properties();
		properties.load(fis);
		fis.close();
		for(Enumeration e=properties.propertyNames();e.hasMoreElements();){
			String propertyName=(String)e.nextElement();
			propertyName.trim();
			String propertyString=properties.getProperty(propertyName);
			if(propertyName.equals("osdtServer")){
				_log4j.debug("setting osdtServer:"+propertyString);
				osdtServer=propertyString;
			}
			if(propertyName.equals("serverPort")){
				_log4j.debug("setting _tcpPort:"+propertyString);
				_tcpPort=Integer.parseInt(propertyString);
			}
			if(propertyName.equals("sourceName")){
				_log4j.debug("setting dataSourceName:"+propertyString);
				dataSourceName=propertyString;
			}
			if(propertyName.startsWith("channel")){
				OSDTTestServer.ChannelSpec channelSpec=parseChannel(propertyString);
				_log4j.debug("setting channel:"+propertyString);
				addChannel(channelSpec);						
			}
		}
	}
	
	public Vector getChannels(){
		return _channels;
	}
	
	/** Configure settings from command line (and possibly from properties file)
	 */
	public void configure(String[] args) throws Exception{
		if(args.length<=0){
			return;
		}
	
		// first catch help or load properties from file
		for(int i=0;i<args.length;i++){
			String arg=args[i];
			if(arg.equals("-help") || arg.equals("--help")){
				printHelp();
				System.exit(1);
			}
			if(arg.equals("-f")){
				// read configuration from a properties file
				loadConfiguration(args[i+1]);
			}
			i++;
		}
		// override properties file settings using
		// command line options
		for(int i=0;i<args.length;i++){
			String arg=args[i];
			if(arg.equals("-h")){
				osdtServer=args[i+1];
				i++;
			}
			if(arg.equals("-p")){
				_tcpPort=Integer.parseInt(args[i+1]);
				i++;
			}
			if(arg.equals("-s")){
				dataSourceName=args[i+1];
				i++;
			}
			if(arg.equals("-c")){
				OSDTTestServer.ChannelSpec channelSpec=parseChannel(args[i+1]);
				addChannel(channelSpec);
				i++;
			}
			i++;
		}
	}
	
	public static void main(String[] args){
		/*
		 * Set up a simple configuration that logs on the console. Note that
		 * simply using PropertyConfigurator doesn't work unless JavaBeans
		 * classes are available on target. For now, we configure a
		 * PropertyConfigurator, using properties passed in from the command
		 * line, followed by BasicConfigurator which sets default console
		 * appender, etc.
		 */
		PropertyConfigurator.configure(System.getProperties());
		PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		BasicConfigurator.configure(new ConsoleAppender(layout));
		
		try {
			OSDTTestServer osdtSource=new OSDTTestServer(args);

			_log4j.debug("server: "+osdtSource.osdtServer);
			_log4j.debug("data source: "+osdtSource.dataSourceName);

			_log4j.debug("OSDT test source connecting");
			osdtSource.startSource();

			_log4j.debug("OSDT test starting data source(s)");
			osdtSource.startWorkers();

			_log4j.debug("listening...");
			osdtSource.listen();

			_log4j.debug("shutting down...");
			osdtSource.shutdown();

			_log4j.debug("done");
		} catch (Exception se) { se.printStackTrace(); }
    }
	
}


	

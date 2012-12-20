/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.utils.TimeUtils;
import org.mbari.siam.operations.portal.PacketStats;
import org.mbari.siam.core.DeviceLogIndex;

import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.MeasurementPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
 * Print DeviceLog contents.
 */
public class DeviceLogUtil {
	
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(DeviceLogUtil.class);
    PacketFormat _packetFormat=new PacketFormat();
	
    SimpleDateFormat _dateFormatter = 
	new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
    static Calendar _calendar = Calendar.getInstance();
	
    /**
     * Print ascii representation of packets in log, selecting which packets to
     * print based on input criteria.
     * 
     * @param log
     * @param startTime
     *            Print packets with timestamp later than this
     * @param stopTime
     *            Print packets with timestamp earlier than this
     * @param packetFilters
     *            1=ALL_TYPES, 2=SENSORDATA, 3=METADATA, 4=MESSAGE
     * @param parser
     */
	
    public void printPackets(DeviceLog log, 
							 long startTime, 
							 long stopTime,
							 PacketFilter[] packetFilters,
							 PacketParser parser)
	throws Exception {
		
		DeviceLogIterator iterator = new DeviceLogIterator(log,startTime);
		
		while (iterator.hasNext()) {
			
			DevicePacket packet = (DevicePacket) iterator.next();
			
			// Check time window criterion
			long timestamp = packet.systemTime();
			if (timestamp < startTime || timestamp > stopTime) {
				// Outside of time window
				continue;
			}
			
			// Check against packet filters
			_log4j.debug("check against filters");
			boolean passed = true;
			for (int i = 0; i < packetFilters.length; i++) {
				if (!packetFilters[i].pass(packet)) {
					_log4j.debug("didn't pass filter #" + i);
					passed = false;
					break;
				}
			}
			
			if (!passed) {
				continue;
			}
			
			_log4j.debug("passed filters");
			
			printPacket(packet, parser);
		}
		
    }
    public void printPackets(DeviceLog log, 
							 long startTime, long stopTime,
							 PacketFilter[] packetFilters,
							 String convertBinary, boolean convertTime,
							 PacketParser parser, 
							 boolean showStats, boolean showData,boolean statsHeaders)
	throws Exception {
		
		DeviceLogIterator iterator = new DeviceLogIterator(log);
		
		while (iterator.hasNext()) {
			
			DevicePacket packet = (DevicePacket) iterator.next();
			
			// Check time window criterion
			long timestamp = packet.systemTime();
			if (timestamp < startTime || timestamp > stopTime) {
				// Outside of time window
				continue;
			}
			
			// Check against packet filters
			_log4j.debug("check against filters");
			boolean passed = true;
			for (int i = 0; i < packetFilters.length; i++) {
				if (!packetFilters[i].pass(packet)) {
					_log4j.debug("didn't pass filter #" + i);
					passed = false;
					break;
				}
			}
			
			if (!passed) {
				continue;
			}
			
			_log4j.debug("passed filters");
			
			printPacket(packet, convertBinary, convertTime, parser,
						showStats, showData,statsHeaders);
		}
    }
	
    public String getStatsHeader() {
		return "EpochTime;Time;DeviceID;ParentID;RecordType;SeqNo;PacketSize;DataSize";
    }
	
    /** Print ascii representation of packet. */
    void printPacket(DevicePacket packet, String convertBinary,
					 boolean convertTime, PacketParser parser, 
					 boolean showStats,
					 boolean showData,
					 boolean statsHeaders) {
		
		PacketParser.Field[] fields;
		
		if(showData){
			if (packet instanceof MetadataPacket) {
				System.out.print("\nMetadataPacket");
			} else if (packet instanceof DeviceMessagePacket) {
				if (packet instanceof MeasurementPacket) {
					System.out.print("\nMeasurementPacket");
				} else {
					System.out.print("\nDeviceMessagePacket");
				}
			} else if (packet instanceof SensorDataPacket) {
				System.out.print("\nSensorDataPacket");
			} else if (packet instanceof SummaryPacket) {
				System.out.print("\nSummaryPacket");
			}
		}
		
		if (convertTime) {
			long t = packet.systemTime();
			t -= utcOffset(_calendar);
			String dateString = _dateFormatter.format(new Date(t)) + " UTC";
			
			System.out.println(" - " + dateString);
		} else {
			if(showData)
				System.out.println("");
		}
		
		String statsDelimiter = ";";
		
		if (convertBinary.equals("bin")) {
			// Show the packet header
			if(showData)
				System.out.println(parseHeader(packet));
			
			PacketStats stats = new PacketStats(packet,statsHeaders);
			if (showData)
				System.out.print(new String(stats.getBuffer()));
			if (showStats) {
				System.out.println(stats);
			}
			
			
		} else if (convertBinary.equals("hex")) {
			// Show the packet header
			if(showData)
				System.out.println(parseHeader(packet));
			
			PacketStats stats = new PacketStats(packet,statsHeaders);
			if (showData){
				byte buffer[]=stats.getBuffer();
				for (int i = 0; i < buffer.length; i++) {
					System.out.print(Integer.toHexString((0xFF & buffer[i]) | 0x100).substring(1));
				}
			}
			if (showStats) {
				System.out.println(stats);
			}
			
		} else {
			if (showData) {
				_log4j.debug("invoke packet.toString()");
				System.out.println(packet);
				if (parser != null) {
					try {
						fields = parser.parseFields(packet);
						
						System.out.println("Got " + fields.length + " fields");
						for (int j = 0; j < fields.length; j++) {
							
							if (fields[j] == null) {
								continue;
							}
							
							System.out.println(fields[j].getName() + ": "
											   + fields[j].getValue() + " "
											   + fields[j].getUnits());
						}
					} catch (Exception e) {
						System.err.println("Exception while parsing: " + e);
					}
				}
			}
			if (showStats) {
				PacketStats stats = new PacketStats(packet,statsHeaders);
				System.out.println(stats);
			}
		}
    }
    /** Utility method; return true if byte is printable. */
    static protected boolean isPrintable(byte c) {
		
		if ((c < 0x20 || c > 0x7e) && (c < 0x09 || c > 0x0D)) {
			return false;
		} else {
			return true;
		}
    }
	
    /** Utility method used in subclass' toString() methods. */
    protected static void convertToAscii(byte[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			
			if (!isPrintable(buffer[i])) {
				buffer[i] = '*';
			}
		}
    }
	
    /** Print ascii representation of packet. */
    void printPacket(DevicePacket packet, 
					 PacketParser parser){
		
		PacketParser.Field[] fields;
		
		
		String statsDelimiter = ";";
		StringBuffer sb = new StringBuffer("");
		
		// add header according to packet format
		sb.append(formatHeader(packet,_packetFormat).trim());
		
		if(_packetFormat._headerSeparator!=null)
			sb.append(_packetFormat._headerSeparator);
		
		if (_packetFormat._dataFormat.equals("bin")) {
			
			PacketStats stats = new PacketStats(packet,_packetFormat._doStatsHeader);
			if (_packetFormat._doData)
				sb.append(new String(stats.getBuffer()));
			if (_packetFormat._doStats) {
				sb.append(stats.toString());
				sb.append("\n");
			}
			
		} else if (_packetFormat._dataFormat.equals("hex")) {
			
			PacketStats stats = new PacketStats(packet,_packetFormat._doStatsHeader);
			if (_packetFormat._doData){
				byte buffer[]=stats.getBuffer();
				for (int i = 0; i < buffer.length; i++) {
					sb.append(Integer.toHexString((0xFF & buffer[i]) | 0x100).substring(1));
				}
			}
			if (_packetFormat._doStats) {
				sb.append(stats.toString());
				sb.append("\n");
			}
			
		} else {
			if (_packetFormat._doData) {
				
				PacketStats stats = new PacketStats(packet,_packetFormat._doStatsHeader);
				DeviceLogUtil.convertToAscii(stats.getBuffer());
				sb.append(new String(stats.getBuffer()));
				sb.append("\n");
				if (parser != null) {
					try {
						fields = parser.parseFields(packet);
						
						sb.append("Got " + fields.length + " fields\n");
						for (int j = 0; j < fields.length; j++) {
							
							if (fields[j] == null) {
								continue;
							}
							
							sb.append(fields[j].getName() + ": "
									  + fields[j].getValue() + " "
									  + fields[j].getUnits()+"\n");
						}
					} catch (Exception e) {
						System.err.println("Exception while parsing: " + e);
					}
				}
			}
			if (_packetFormat._doStats) {
				PacketStats stats = new PacketStats(packet,_packetFormat._doStatsHeader);
				sb.append(stats.toString());
				sb.append("\n");
			}
		}
		// Note: this has the effect of removing 
		// whitespace, even if they were part of the data
		String retval=sb.toString().trim();
		if(_packetFormat._recordSeparator!=null)
			retval+=_packetFormat._recordSeparator;
		
		System.out.print(retval);
    }
    public void setPacketFormat(String dataFormat,
								String headerSeparator,
								String recordSeparator,
								boolean doDefault,
								boolean doTimeLong,
								boolean doTimeDate,
								boolean doPacketType,
								boolean doData,
								boolean doStats,
								boolean doStatsHeader){
		
		_packetFormat=new PacketFormat( dataFormat,
									   headerSeparator,
									   recordSeparator,
									   doDefault,
									   doTimeLong,
									   doTimeDate,
									   doPacketType,
									   doData,
									   doStats,
									   doStatsHeader);
    }
	
    public class PacketFormat{
		public boolean _doStats=false;
		public boolean _doData=true;
		public boolean _doStatsHeader=false;
		public boolean _doTimeLong=false;
		public boolean _doTimeDate=false;
		public boolean _doPacketType=false;
		public boolean _doDefault=false;
		public String _dataFormat="ascii";
		public String _headerSeparator="\n";
		public String _recordSeparator="\n";
		
		public PacketFormat(String dataFormat,
							String headerSeparator,
							String recordSeparator,
							boolean doDefault,
							boolean doTimeLong,
							boolean doTimeDate,
							boolean doPacketType,
							boolean doData,
							boolean doStats,
							boolean doStatsHeader){
			this();
			_dataFormat=dataFormat;
			_headerSeparator=headerSeparator;
			_recordSeparator=recordSeparator;
			_doDefault=doDefault;
			_doTimeLong=doTimeLong;
			_doTimeDate=doTimeDate;
			_doPacketType=doPacketType;
			_doData=doData;
			_doStats=doStats;
			_doStatsHeader=doStatsHeader;
		}
		public PacketFormat(){
			
		}
		public String toString(){
			StringBuffer sb=new StringBuffer();
			sb.append("\n");
			sb.append("_dataFormat      :"+_dataFormat+"\n");
			sb.append("_headerSeparator :"+_headerSeparator+"\n");
			sb.append("_recordSeparator :"+_recordSeparator+"\n");
			sb.append("_doDefault       :"+_doDefault+"\n");
			sb.append("_doTimeLong      :"+_doTimeLong+"\n");
			sb.append("_doTimeDate      :"+_doTimeDate+"\n");
			sb.append("_doPacketType    :"+_doPacketType+"\n");
			sb.append("_doData          :"+_doData+"\n");
			sb.append("_doStats         :"+_doStats+"\n");
			sb.append("_doStatsHeader   :"+_doStatsHeader+"\n");
			return sb.toString();
		}
    }
	
    /** format a header */
    public String formatHeader(DevicePacket packet, PacketFormat format){
		StringBuffer sb=new StringBuffer("");
		
		if(format._doPacketType){
			if (packet instanceof MetadataPacket) {
				sb.append("\nMetadataPacket");
			} else if (packet instanceof DeviceMessagePacket) {
				if (packet instanceof MeasurementPacket) {
					sb.append("\nMeasurementPacket");
				} else {
					sb.append("\nDeviceMessagePacket");
				}
			} else if (packet instanceof SensorDataPacket) {
				sb.append("\nSensorDataPacket");
			} else if (packet instanceof SummaryPacket) {
				sb.append("\nSummaryPacket");
			}
		}
		//System.out.println("doTimeDate="+format._doTimeDate);
		if(format._doTimeDate){
			long t = packet.systemTime();
			t -= utcOffset(_calendar);
			String dateString = 
			_dateFormatter.format(new Date(t)) + " UTC";
			if(format._doPacketType)
				sb.append(" - ");
			sb.append(dateString);
		}
		
		if(format._doTimeLong){
			if(format._doPacketType){
				sb.append(" - ");
			}else if(format._doTimeDate)
				sb.append(" ");
			sb.append(packet.systemTime());
		}
		
		if(format._doDefault){
			sb.append(parseHeader(packet));
		}
		
		return sb.toString();
		
    }
	
    /** parse out just the header from a packet */
    public String parseHeader(DevicePacket packet) {
		
		BufferedReader br = new BufferedReader(new StringReader(packet
																.toString()));
		String header = "";
		
		try {
			// Set what to look for based on packet type
			// Default id mdref, which should work for DevicePacket
			// and DeviceMessagePacket
			String lookFor = "mdref";
			if (packet instanceof SensorDataPacket) {
				lookFor = "nBytes";
			} else if (packet instanceof MetadataPacket) {
				lookFor = "cause";
			}
			
			// Assemble the header;
			// Add each line until the line containing the lookFor value
			String line = br.readLine();
			while (line.indexOf(lookFor) < 0) {
				header += "\n" + line;
				line = br.readLine();
			}
			header += "\n" + line;
			
		} catch (IOException e) {
			System.err.println(e);
		}
		return header;
    }
	
    /** Replace substring find with substring replace in the specified string str
	 Useful for replacing escape characters and general find/replace.
	 */
    public static String findReplace(String str,String find,String replace){
		while(str.indexOf(find)>=0){
			str=str.substring(0,str.indexOf(find))+
			replace+
			str.substring(str.indexOf(find)+find.length());
		}
		return str;
    }
	
    /** Replace escaped hex, octal or unicode characters with 
	 the appropriate character.
	 
	 Syntax for escaped characters is as follows:
	 hex: \\xnn,  n=0-9a-f
	 oct: \\onnn, n=0-7
	 uni: \\unnnn, n=0-9a-f
	 
	 @param str   the string containing escaped characters
	 @param find  the escape sequence to find (e.g. "\\x")
	 @param len   the number of characters following escape sequence (hex:2, unicode:4, octal:3)
	 @param radix the radix to use for parsing the escape sequence 
	 
	 @return returns a string containing the specified substitution
	 
	 */
    public static String unescape(String str,String find, int len, int radix){
		
		while(str.indexOf(find)>=0){
			int begin=str.indexOf(find)+find.length();
			int end=begin+len;
			char c = (char)Integer.parseInt(str.substring(begin,end),radix);
			str=str.substring(0,str.indexOf(find))+
			c+
			str.substring(end);
		}
		return str;
    }
	
    public static void check(DeviceLog log,boolean repair){
		log.check(log, repair);
    }
	
	
    public static void main(String[] args) {
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
		
		long startTime = Long.MIN_VALUE;
		long stopTime = Long.MAX_VALUE;
		String convertBinary = "ascii";
		boolean convertTime = false;
		String parserName = null;
		PacketParser parser = null;
		boolean showStats = false;
		boolean showData = true;
		boolean statsHeaders = false;
		boolean noheaders=false;
		boolean dotype=true;
		boolean utime=false;
		boolean etime=false;
		String headerSeparator="\n";
		String recordSeparator="\n";
		boolean doCheck=false;
		boolean usageError = false;
		boolean doRepair=false;
		
		// Maximum number of 'option' tokens in args (last two args are
		// mandatory)
		int maxOptionTokens = args.length - 2;
		
		// By default get all packet types
		int typeFlags = DevicePacket.ALL_TYPES; 
		// By default don't subsample
		int skipInterval = 0;
		
		boolean calcAge = false;
		
		for (int i = 0; i < maxOptionTokens; i++) {
			
			if (args[i].equals("-start") && i < maxOptionTokens - 1) {
				// Parse lower end of time window
				try {
					startTime = TimeUtils.parseDateTime(args[++i]);
					startTime += utcOffset(_calendar);
				} catch (ParseException e) {
					System.err.println("Invalid timestring: " + args[i]);
					usageError = true;
				}
			} else if (args[i].equals("-stop") && i < maxOptionTokens - 1) {
				// Parse upper end of time window
				try {
					stopTime = TimeUtils.parseDateTime(args[++i]);
					stopTime += utcOffset(_calendar);
				} catch (ParseException e) {
					System.err.println("Invalid timestring: " + args[i]);
					usageError = true;
				}
			} else if (args[i].equals("-type") && i < maxOptionTokens - 1) {
				// Determine type of packets to print
				if (args[++i].equals("all"))
					typeFlags = DevicePacket.ALL_TYPES;
				else if (args[i].equals("metadata"))
					typeFlags = DevicePacket.METADATA_FLAG;
				else if (args[i].equals("data"))
					typeFlags = DevicePacket.SENSORDATA_FLAG;
				else if (args[i].equals("message"))
					typeFlags = DevicePacket.DEVICEMESSAGE_FLAG;
				else if (args[i].equals("summary"))
					typeFlags = DevicePacket.SUMMARY_FLAG;
				else {
					System.err.println("Invalid packet type: " + args[i]);
					usageError = true;
				}
			}else if (args[i].equals("-check")){
				doCheck=true;
			}else if (args[i].equals("-repair")){
				doRepair=true;
			}else if (args[i].equals("-skip") && i < maxOptionTokens-1) {
				try {
					skipInterval = Integer.parseInt(args[++i]);
				}
				catch (NumberFormatException e) {
					System.err.println("Invalid skip interval; integer required");
					usageError = true;
				}
			} else if (args[i].equals("-bin")) {
				convertBinary = "bin";
			} else if (args[i].equals("-hex")) {
				convertBinary = "hex";
			} else if (args[i].equals("-stats")) {
				showStats = true;
			} else if (args[i].equals("-statsOnly")) {
				showStats = true;
				showData = false;
			}else if (args[i].equals("-e")) {
				etime = true;
			}else if (args[i].equals("-u")) {
				utime = true;
			}else if (args[i].equals("-r")) {
				recordSeparator = args[++i];
				
				// replace escaped chars here
				if(recordSeparator.indexOf("\\0")>=0)
					recordSeparator=findReplace(recordSeparator,"\\0","");
				if(recordSeparator.indexOf("\\n")>=0)
					recordSeparator=findReplace(recordSeparator,"\\n","\n");
				if(recordSeparator.indexOf("\\s")>=0)
					recordSeparator=findReplace(recordSeparator,"\\s"," ");
				if(recordSeparator.indexOf("\\t")>=0)
					recordSeparator=findReplace(recordSeparator,"\\t","\t");
				if(recordSeparator.indexOf("\\x")>=0)
					recordSeparator=unescape(recordSeparator,"\\x",2,16);
				if(recordSeparator.indexOf("\\u")>=0)
					recordSeparator=unescape(recordSeparator,"\\u",4,16);
				if(recordSeparator.indexOf("\\o")>=0)
					recordSeparator=unescape(recordSeparator,"\\o",3,8);
				
			}else if (args[i].equals("-stream")) {
				dotype = false;
				noheaders=true;
				etime=false;
				utime=false;
				recordSeparator="";
			}else if (args[i].equals("-notype")) {
				dotype = false;
			}else if (args[i].equals("-h")) {
				headerSeparator=args[++i];
				
				// replace escaped chars here 
				if(headerSeparator.indexOf("\\0")>=0)
					headerSeparator=findReplace(headerSeparator,"\\0","");
				if(headerSeparator.indexOf("\\n")>=0)
					headerSeparator=findReplace(headerSeparator,"\\n","\n");
				if(headerSeparator.indexOf("\\s")>=0)
					headerSeparator=findReplace(headerSeparator,"\\s"," ");
				if(headerSeparator.indexOf("\\t")>=0)
					headerSeparator=findReplace(headerSeparator,"\\t","\t");
				if(headerSeparator.indexOf("\\x")>=0)
					headerSeparator=unescape(headerSeparator,"\\x",2,16);
				if(headerSeparator.indexOf("\\u")>=0)
					headerSeparator=unescape(headerSeparator,"\\u",4,16);
				if(headerSeparator.indexOf("\\o")>=0)
					headerSeparator=unescape(headerSeparator,"\\o",3,8);
				
				
			}else if (args[i].equals("-noheader")) {
				noheaders = true;
			}else if (args[i].equals("-sheader")) {
				statsHeaders = true;
			} else if (args[i].equals("-utc")) {
				convertTime = true;
			} else if (args[i].equals("-parse") && i < maxOptionTokens - 1) {
				parserName = args[++i];
			} 
			else if (args[i].equals("-age")) {
				// Just compute age of data log (i.e. age of latest packet)
				calcAge = true;
			}
			else {
				System.err.println("Invalid or incomplete option: " + args[i]);
				System.err.println("i=" + i + ", maxOptionTokens="
								   + maxOptionTokens);
				usageError = true;
			}
		}
		
		if (args.length < 2) {
			usageError = true;
		}
		
		if (usageError) {
			System.err.println("");
			System.err.println("usage: DeviceLogUtil "
							   + "<options> deviceId directory");
			System.err.println("");
			System.err.println("Options:");
			
			System.err.println(" -check            check logs and summarize");
			System.err.println(" -repair           attempt to repair corrupt log");
			System.err.println(" -start <time>     show records after <time>");
			System.err.println(" -stop             show records before <time>");
			System.err.println(" -type             packetype ["
							   + "'all'|'metadata'|'data'|'message'|'summary']");
			System.err.println(" -skip <interval>  skip every <interval>th record");
			System.err.println(" -bin              show data in raw binary form");
			System.err.println(" -hex              show data in ASCII hex format");
			System.err.println(" -utc              display human readable UTC timestamps");
			System.err.println(" -parse <class>    parse data using <class> (must implement PacketParser)");
			System.err.println(" -noheader         suppress headers");
			System.err.println(" -stream           native bytes only; no headers or separators");
			System.err.println(" -e                timestamp header (UTC ms since epoch)");
			System.err.println(" -u                timestamp header (UTC human readable)");
			System.err.println(" -r <separator>    record separator (between packets)");
			System.err.println(" -h <separator>    header separator (between header and data)");
			System.err.println(" -notype           suppress packet type text");
			System.err.println(" -stats            include packet statistics");
			System.err.println(" -statsOnly        show packet statistics only");
			System.err.println(" -sheader          include stats header");
			System.err.println(" -age              display age (seconds) of latest packet in log");
			System.err.println("");
			return;
		}
		
		try {
			long sensorId = Long.parseLong(args[args.length - 2]);
			String directory = args[args.length - 1];
			
			PacketFilter[] filters = new PacketFilter[2];
			
			// Allow specified packet type(s)
			filters[0] =
			new PacketSubsampler(skipInterval, typeFlags);
			
			// Exclude all other types
			filters[1] = 
			new PacketSubsampler(-1, 
								 ~(DevicePacket.ALL_TYPES & typeFlags));
			
			DeviceLog log = new DeviceLog(sensorId, directory);
			
			if(doCheck || doRepair){
				DeviceLogUtil logViewer = new DeviceLogUtil();
				logViewer.check(log,doRepair);
				return;
			}
			
			if (calcAge) {
				// Just display age of most recent packet, in seconds
				System.out.println((System.currentTimeMillis() - log.getMaxTimestamp())/1000);
				return;
			}
			
			
			if (parserName != null) {
				Class c = 
				ClassLoader.getSystemClassLoader().loadClass(parserName);
				
				parser = (PacketParser) c.newInstance();
			}
			DeviceLogUtil logViewer = new DeviceLogUtil();
			
			if(noheaders || utime || etime || dotype){
				logViewer.setPacketFormat(convertBinary,
										  headerSeparator,
										  recordSeparator,
										  (!noheaders),
										  etime,
										  (utime||convertTime),
										  dotype,
										  showData,
										  showStats,
										  statsHeaders);
				logViewer.printPackets(log, 
									   startTime, stopTime, filters,
									   parser);
				
			}else
				logViewer.printPackets(log, 
									   startTime, stopTime, filters,
									   convertBinary, 
									   convertTime, parser, 
									   showStats, showData,statsHeaders);
			
		} catch (Exception e) {
			System.err.println(e);
		}
    }
	
    /** Compute offset of local timezone from UTC */
    static int utcOffset(Calendar calendar) {
		return calendar.get(Calendar.ZONE_OFFSET) + 
	    calendar.get(Calendar.DST_OFFSET);
    }
	
}

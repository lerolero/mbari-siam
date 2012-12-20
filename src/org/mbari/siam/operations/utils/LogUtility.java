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
import java.io.File;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
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
 <p>
 Base class for utilities that manipulate SIAM device logs.
 </p>
 <p>
 Provides support for iterating through a device log, filtering
 the packets, and performing operations on the resulting set.
 </p>
 
 <p>
 In addition, it provides methods for formatting and printing 
 packet contents, enabling the user to generate new data logs 
 in arbitrary convenient formats, for example delimited text, 
 binary streams, etc.
 </p>
 
 <p>
Replaces DeviceLogUtil class, providing:
 <ul>
 <li> better separation of concerns</li>
 <li> streamlined code to eliminate similar functions</li>
 <li> simplified and expanded option set (for example, per-packet type formatting)</li>
 <li> structured for sub-classing</li>
 <li> maintains DeviceLogUtil command semantics and output format</li>
 <li> implements several basic log utiltiies</li>
 <li> new structure makes it easy to develop DeviceLog based utilities (e.g. LogSplitter)</li>
</ul>
 </p>
 
 <p>
 Basic built-in utility functions include 
 <ul>
 <li> log viewer         [formatted and display packets filtered on time, type and other parameters]</li>
 <li> log check/repair   [reports log errors and stats, and optionally repairs log index files]</li>
 <li> log age calculator [calculates age of most recent log entry]</li>
</ul>
 </p>
 
 <p>
 One major change is to replace some of the DeviceLogUtil command line output formatting options:
</p>
 <p>
 <code>
 -bin              show data in raw binary form<br>
 -hex              show data in ASCII hex format<br>
 -utc              display human readable UTC timestamps<br>
 -noheader         suppress headers<br>
 -stream           native bytes only; no headers or separators<br>
 -e                timestamp header (UTC ms since epoch)<br>
 -u                timestamp header (UTC human readable)<br>
 -r <separator>    record separator (between packets)<br>
 -h <separator>    header separator (between header and data)<br>
 -notype           suppress packet type text<br>
 -stats            include packet statistics<br>
 -statsOnly        show packet statistics only<br>
 -sheader          include stats header<br>
</code>
 </p>
 
 <p>
 with a format string, similar to C printf, with format specifiers indicating data elements
 like sequence number, packet type, timestamp and data buffer.
 This enables the user define what parts of the packet to display and in what order, and 
 may include additional arbitrary text, including binary and unicode characters.
 This may be used to transform device log data into comma separated values or replicate 
 raw binary instrument output.
 The default format generates output in DeviceLogUtil default format.
 </p>
 
 <p>
In addition to formatting flags, it is possible to filter multiple packet types, 
 for example to process only data and message packets.
 </p>
 
 <p>
 New formatting flags enable the user to control data buffer formatting based on 
 packet type. This could be used to display message and metadata packets as ASCII text,
 and display sensor data in ASCII hex or binary formats.
 </p>
 
 <p>
 <code>
 usage: logUtil <options> deviceId directory
 </code>
 </p>
 
 <p>
 Options:
 </p>
 <p>
 <code>
 -verbose          verbose output (for debug)<br>
 -check            check logs and summarize<br>
 -repair           attempt to repair corrupt log<br>
 -age              display age (seconds) of latest packet in log<br>
 
 -start <time>     show records after <time><br>
 -stop             show records before <time><br>
 -type             packet type (may be OR'd) [all|metadata|data|message|summary]<br>
 -skip <interval>  skip every <interval>th record<br>
 -parse <class>    parse data using <class> (must implement PacketParser)<br>
<br> 
 -fflags <flags>     set data format flags [BIN|HEX|SDB|SDH|DMB|DMH|SPB|SPH|MPB|MPH]<br>
 -format <string>    global format string [applies to all packet types, unless overridden]<br>
 -sdformat <string>  sensor data format string<br>
 -mdformat <string>  metadata format string<br>
 -dmformat <string>  device message format string<br>
 -spformat <string>  summary format string<br>
 <br>
 Valid format strings may use arbitrary text along with these<br>
 format specifiers:<br>
 %e timestamp (epoch time)<br>
 %u timestamp (UTC date time)<br>
 %b data buffer<br>
 %p parsed data<br>
 %c metadata cause<br>
 %i device ID<br>
 %P parent ID<br>
 %r record type<br>
 %k record type mnemonic<br>
 %n sequence number<br>
 %s data buffer size<br>
 %S data packet size<br>
 %m metadata reference number<br>
 %y packet stats<br>
 plus common escape sequences: \\ \\n \\t \\r \\f \\b \\xnn \\onnn \\unnn etc.<br>
<br>
 default format [used for all packet types except metadata]:<br>
 "%k\nparentID=%P, recordType=%r\ndevid=%i, t=%e, seqNo=%n, mdref=%m\nnBytes=%s\n%b\n"<br>
 <br>
 default metadata format:<br>
 "%k\nparentID=%P, recordType=%r\ndevid=%i, t=%e, seqNo=%n, mdref=%m\n cause=%c\n%b\n"<br>
 </code>
 </p>

 <p>
 ////////////////////////////////////////////////////////<br>
 Example: show log in default format<br>
 ////////////////////////////////////////////////////////<br>
 </p>

 <p>
 logView-new 1510 test/tmp
 </p>
 
 <p>
 output:
 </p>
 <p>
 <code>
 MetadataPacket<br>
 parentID=1598, recordType=0<br>
 devid=1599, t=1256776460432, seqNo=0<br>
 nBytes=5554<br>
 <SRVC_MD><br>
 powerOnDelaySec=0<br>
 instrumentName=UNKNOWN<br>
 ...<br>
 SensorDataPacket<br>
 parentID=1598, recordType=1<br>
 devid=1599, t=1256776461265, seqNo=1<br>
 nBytes=78<br>
 $PEDATA, P888.04, T35.26, H3.13, GFL-461.62, GFH123.48, C289.86, TC0.00, *3887<br>
 </code>
 </p>

 <p>
 ////////////////////////////////////////////////////////<br>
 Example: generate CSV file of <br>
 sensor data in a time window, and <br>
 format the data buffer in ASCII hex<br>
 ////////////////////////////////////////////////////////<br>
 </p>
 
 <p>
 logView-new -type data -start "10/29/2009T04:00" -stop "10/29/2009T06:00" -format "%u,%n,%b\n" -fflags "SDH" 1553 test/tmp
 </p>
 
<p>
 output:
</p>
 <p>
 <code>
 10/29/2009 12:00:25 UTC,2599,a52a7c0055432910091...<br>
 10/29/2009 12:00:40 UTC,2600,a52a7c0055532910091...<br>
 ...<br>
 10/29/2009 13:59:40 UTC,3079,a52a7c0055232912091...<br>
 10/29/2009 13:59:55 UTC,3080,a52a7c0055332912091...<br>
 </code>
</p>
 <p>
 ////////////////////////////////////////////////////////<br>
 Example: generate file consisting of <br>
 binary sensor data, with records <br>
 separated by CRLF. Redirect output <br>
 to a file.<br>
 ////////////////////////////////////////////////////////<br>
 </p>
 
 <p>
 logView-new -type data -start "10/29/2009T04:00" -stop "10/29/2009T06:00" -format "%b\x0a\x0d" -fflags "SDB" 1553 test/tmp > foo-bin.out
 </p>
<p>
 <code>
 hexdump -C foo-bin.out<br>
 00000000  a5 2a 7c 00 55 43 29 10  09 10 00 00 00 00 87 00  |.*|.UC).........|<br>
 00000010  bc 39 f9 0a 06 00 f7 ff  00 33 ef 19 ab 07 00 00  |.9.......3......|<br>
 00000020  00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |................|<br>
 00000030  00 00 00 00 00 00 83 fe  86 f8 20 f9 56 08 72 f7  |.......... .V.r.|<br>
 00000040  80 01 0a 00 03 00 92 00  d0 ff e9 00 61 fe 91 f9  |............a...|<br>
 00000050  a1 fe 01 00 bb 00 77 00  3b 01 f3 00 6c fe a5 01  |......w.;...l...|<br>
 00000060  90 00 01 00 ff ff eb ff  f1 ff de ff 45 00 f5 01  |............E...|<br>
 00000070  65 00 01 00 e3 ff a6 ff  b0 fe b3 fe 76 01 8b fe  |e...........v...|<br>
 00000080  45 00 00 00 01 00 19 00  fa ff 24 00 b0 ff 9f fe  |E.........$.....|<br>
 00000090  bd ff 00 00 23 00 ad 63  40 39 2c 3f af c3 70 5b  |....#..c@9,?..p[|<br>
 000000a0  67 71 58 57 9a 8c c2 7a  5e 63 5c 5b b4 ca a1 7c  |gqXW...z^c\[...||<br>
 000000b0  6c 53 3e 4f 91 7b ba 6b  51 54 4e 50 aa c9 9f 74  |lS>O.{.kQTNP...t|<br>
 000000c0  64 56 39 3e 7d 7b 45 13  22 21 1d 3c 64 64 63 4e  |dV9>}{E."!.&lt;ddcN|<br>
 000000d0  2d 2d 18 4a 64 3a 64 53  63 63 61 4e 64 64 64 64  |--.Jd:dSccaNdddd|<br>
 000000e0  63 56 48 61 64 5d 63 59  64 63 5e 55 64 64 64 63  |cVHad]cYdc^Udddc|<br>
 000000f0  64 58 28 5c 64 64 81 c2  0a 0d a5 2a 7c 00 55 53  |dX(\dd.....*|.US|<br>
 ... <br>
 </code>
 </p>
<p>
Note CRLF (0a 0d) at 000000f8.
</p>
 */
public class LogUtility{// implements SpecifierParser{
	
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(LogUtility.class);
	
	// packet format specifiers
	/** Format specifier: parent ID */
	protected static final String FMT_PARENT_ID       ="%P";
	/** Format specifier: timestamp (epoch seconds) */
	protected static final String FMT_EPOCH_TIME      ="%e";
	/** Format specifier: timestamp (UTC) */
	protected static final String FMT_UTC_TIME        ="%u";
	/** Format specifier: primary data buffer */
	protected static final String FMT_DATA_BUFFER     ="%b";
	/** Format specifier: parsed data buffer */
	protected static final String FMT_PARSED_DATA     ="%p";
	/** Format specifier: metadata packet cause field */
	protected static final String FMT_MD_CAUSE        ="%c";
	/** Format specifier: device ID */
	protected static final String FMT_DEVICE_ID       ="%i";
	/** Format specifier: record type (numeric) */
	protected static final String FMT_RECORD_TYPE     ="%r";
	/** Format specifier: packet sequence number */
	protected static final String FMT_SEQ_NUMBER      ="%n";
	/** Format specifier: primary data buffer record size */
	protected static final String FMT_DATA_SIZE       ="%s";
	/** Format specifier: total packet size (including headers) */
	protected static final String FMT_PACKET_SIZE     ="%S";
	/** Format specifier: metadata reference number (sequence number of last metadata packet) */
	protected static final String FMT_MDREF_NUMBER    ="%m";
	/** Format specifier: packet statistics */
	protected static final String FMT_PACKET_STATS    ="%y";
	/** Format specifier: packet type (mnemonic) */
	protected static final String FMT_PACKET_TYPENAME ="%k";
	/** Format specifier: escape sequence (character expressed in hex) */
	protected static final String FMT_ESC_HEX         ="\\x";
	/** Format specifier: escape sequence (character expressed in octal) */
	protected static final String FMT_ESC_OCT         ="\\o";
	/** Format specifier: escape sequence (character expressed in unicode) */
	protected static final String FMT_ESC_UNI         ="\\u";
	/** Format specifier: escape sequence (double quote) */
	protected static final String FMT_ESC_DQUOTE      ="\\\"";
	/** Format specifier: escape sequence (single quote)*/
	protected static final String FMT_ESC_SQUOTE      ="\\\'";
	/** Format specifier: escape sequence (newline) */
	protected static final String FMT_ESC_NEWLINE     ="\\n";
	/** Format specifier: escape sequence (tab) */
	protected static final String FMT_ESC_TAB         ="\\t";
	/** Format specifier: escape sequence (backspace) */
	protected static final String FMT_ESC_BS          ="\\b";
	/** Format specifier: escape sequence (carriage return) */
	protected static final String FMT_ESC_CR          ="\\r";
	/** Format specifier: escape sequence (form feed) */
	protected static final String FMT_ESC_FF          ="\\f";
	/** Format specifier: escape sequence (double backslash) */
	protected static final String FMT_ESC_ESC         ="\\\\";
	
	
	// packet type flag names (for command line)
	/** packet type mnemonic for command line (all packet types) */
	protected static final String FLAGNAME_ALL        ="all";
	/** packet type mnemonic for command line (metadata packets) */
	protected static final String FLAGNAME_METADATA   ="metadata";
	/** packet type mnemonic for command line (sensor data packets) */
	protected static final String FLAGNAME_SENSORDATA ="data";
	/** packet type mnemonic for command line (message packet types) */
	protected static final String FLAGNAME_MESSAGE    ="message";
	/** packet type mnemonic for command line (data summary packets) */
	protected static final String FLAGNAME_SUMMARY    ="summary";
	
	// packet type names (for output)
	/** packet type mnemonic for output (metadata packet) */
	protected static final String TYPENAME_METADATA    ="MetadataPacket";
	/** packet type mnemonic for output (measurement packet) */
	protected static final String TYPENAME_MEASUREMENT ="MeasurementPacket";
	/** packet type mnemonic for output (sensor data packet) */
	protected static final String TYPENAME_SENSORDATA  ="SensorDataPacket";
	/** packet type mnemonic for output (message packet) */
	protected static final String TYPENAME_MESSAGE     ="DeviceMessagePacket";
	/** packet type mnemonic for output (data summary packet) */
	protected static final String TYPENAME_SUMMARY     ="SummaryPacket";
	/** packet type mnemonic for output (unknown packet type) */
	protected static final String TYPENAME_UNKNOWN     ="UnknownPacketType";
	
	// command line options
	/** command line option: (start time) */
	protected static final String OPT_START  ="-start";
	/** command line option: (stop time) */
	protected static final String OPT_STOP   ="-stop";
	/** command line option: (record skip modulus) */
	protected static final String OPT_SKIP   ="-skip";
	/** command line option: (packet type filter flags) */
	protected static final String OPT_TYPE   ="-type";
	/** command line option: (packet parser class) */
	protected static final String OPT_PARSE  ="-parse";
	/** command line option: (stream age action flag) */
	protected static final String OPT_AGE    ="-age";
	/** command line option: (index integrity check action flag) */
	protected static final String OPT_CHECK  ="-check";
	/** command line option: (index repair action flag) */
	protected static final String OPT_REPAIR ="-repair";
	/** command line option: (specify global output format string) */
	protected static final String OPT_FORMAT ="-format";
	/** command line option: (specify override metadata format string) */
	protected static final String OPT_MDFORMAT ="-mdformat";
	/** command line option: (specify override sensor data format string) */
	protected static final String OPT_SDFORMAT ="-sdformat";
	/** command line option: (specify override device message format string) */
	protected static final String OPT_DMFORMAT ="-dmformat";
	/** command line option: (specify override summary packet format string) */
	protected static final String OPT_SPFORMAT ="-spformat";
	/** command line option: (format flags) */
	protected static final String OPT_FFLAGS ="-fflags";
	/** command line option: (verbose output) */
	protected static final String OPT_VERBOSE ="-verbose";
	
	// data format flags (for command line)
	/** format flag mnemonic: (show all data in raw binary format) */
	protected static final String FFLAG_BIN   ="BIN";
	/** format flag mnemonic: (show all data in ASCII hex format) */
	protected static final String FFLAG_HEX   ="HEX";
	/** format flag mnemonic: (show sensor data in raw binary format) */
	protected static final String FFLAG_SDBIN ="SDB";
	/** format flag mnemonic: (show sensor data in ASCII hex format) */
	protected static final String FFLAG_SDHEX ="SDH";
	/** format flag mnemonic: (show device message packets in raw binary format) */
	protected static final String FFLAG_DMBIN ="DMB";
	/** format flag mnemonic: (show device message packets in ASCII hex format) */
	protected static final String FFLAG_DMHEX ="DMH";
	/** format flag mnemonic: (show summary packets in raw binary format) */
	protected static final String FFLAG_SPBIN ="SPB";
	/** format flag mnemonic: (show summary packets in ASCII hex format) */
	protected static final String FFLAG_SPHEX ="SPH";
	/** format flag mnemonic: (show metadata packets in raw binary format) */
	protected static final String FFLAG_MPBIN ="MPB";
	/** format flag mnemonic: (show metadata packets in ASCII hex format) */
	protected static final String FFLAG_MPHEX ="MPH";
	
	/** 
	 <p>Default output format.</p>
	 <p>
	 Used for all packet types except metadata for backwards compatibility.
	 </p>
	 <p>
	 Provides best compatibility with original logView 
	 utility. There are whitespace differences, since the
	 original logView utility trims trailing whitespace from
	 data buffers, and LogUtility preserves it.
	 </p>
	 */
	public static final String DEFAULT_GENERIC_FORMAT=
	LogUtility.FMT_PACKET_TYPENAME+"\\n"+
	"parentID="+LogUtility.FMT_PARENT_ID+", recordType="+LogUtility.FMT_RECORD_TYPE+"\\n"+
	"devid="+LogUtility.FMT_DEVICE_ID+", "+"t="+LogUtility.FMT_EPOCH_TIME+
	", seqNo="+LogUtility.FMT_SEQ_NUMBER+", mdref="+LogUtility.FMT_MDREF_NUMBER+"\\n"+
	"nBytes="+LogUtility.FMT_DATA_SIZE+"\\n"+
	LogUtility.FMT_DATA_BUFFER+"\\n";
	
	/** 
	 Default sensor data output format.
	 */
	public static final String DEFAULT_SD_FORMAT=LogUtility.DEFAULT_GENERIC_FORMAT;
	/** 
	 Default device message output format.
	 */
	public static final String DEFAULT_DM_FORMAT=LogUtility.DEFAULT_GENERIC_FORMAT;
	/** 
	 Default summary packet output format.
	 */
	public static final String DEFAULT_SP_FORMAT=LogUtility.DEFAULT_GENERIC_FORMAT;
	/** 
	 Default metadata output format.
	 */
	public static final String DEFAULT_MD_FORMAT=
	LogUtility.FMT_PACKET_TYPENAME+"\\n"+
	"parentID="+LogUtility.FMT_PARENT_ID+", recordType="+LogUtility.FMT_RECORD_TYPE+"\\n"+
	"devid="+LogUtility.FMT_DEVICE_ID+", "+"t="+LogUtility.FMT_EPOCH_TIME+
	", seqNo="+LogUtility.FMT_SEQ_NUMBER+", mdref="+LogUtility.FMT_MDREF_NUMBER+"\\n"+
	" cause="+LogUtility.FMT_MD_CAUSE+"\\n"+
	LogUtility.FMT_DATA_BUFFER+"\\n";
	
	/** Log utility configuration instance */
    private LogUtilityConfig _configuration;
	
	/** Date formatter for UTC date output */
	private SimpleDateFormat _dateFormatter = 
	new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
	/** Calendar instance for UTC date output */
    private static Calendar _calendar = Calendar.getInstance();
	
	/** Fill character used to replace unprintable binary 
	 characters in ascii mode
	 */
	private char _binaryFillChar='*';
	
	/** Constructor */
	public LogUtility(){
		
		_configuration=new LogUtilityConfig();
	}
	
	/** Set configuration instance 
	 @param configuration A LogUtilityConfig instance
	 */
	public void setConfiguration(LogUtilityConfig configuration){
		_configuration=configuration;
	}
	
	/** Get configuration instance 
	 @return current configuration instance or null if not set
	 */
	public LogUtilityConfig getConfiguration(){
		return _configuration;
	}
	
	/** Get the default global format string for this utility.
	 Sub-classes may override to provide alternative 
	 default formatting.
	 
	 @return Default output format (String)
	 */
	public String getDefaultFormat(){
		return LogUtility.DEFAULT_GENERIC_FORMAT;
	}
	
	/** Get the default sensor data format string for this utility.
	 Sub-classes may override to provide alternative 
	 default formatting.
	 
	 @return Default sensor data output format (String)
	 */
	public String getDefaultSDFormat(){
		return LogUtility.DEFAULT_SD_FORMAT;
	}
	
	/** Get the default metadata format string for this utility.
	 Sub-classes may override to provide alternative 
	 default formatting.
	 
	 @return Default metadata output format (String)
	 */
	public String getDefaultMDFormat(){
		return LogUtility.DEFAULT_MD_FORMAT;
	}
	
	/** Get the default device message format string for this utility.
	 Sub-classes may override to provide alternative 
	 default formatting.
	 
	 @return Default device message output format (String)
	 */
	public String getDefaultDMFormat(){
		return LogUtility.DEFAULT_DM_FORMAT;
	}
	
	/** Get the default summary packet format string for this utility.
	 Sub-classes may override to provide alternative 
	 default formatting.
	 
	 @return Default summary packet output format (String)
	 */
	public String getDefaultSPFormat(){
		return LogUtility.DEFAULT_SP_FORMAT;
	}
	
	/** Set the fill character used to replace unprintable binary 
	 characters in ASCII mode output. 
	 
	 @param fillChar character to substitute
	 */
	public void setBinaryFillChar(char fillChar){
		_binaryFillChar=fillChar;
	}
	
	/** Get the fill character used to replace unprintable binary 
	 characters in ASCII mode output.
	 
	 @return binary fill character (char)
	 */
	public char getBinaryFillChar(){
		return _binaryFillChar;
	}
	
	/** Get the a DeviceLogIterator. Uses the specified device log and
	 LogUtilityConfig instance.
	 If the config instance defines a start time, the log iterator
	 will start at that time. Otherwise, it will start at the beginning of the log.
	 
	 @param log     DeviceLog instance
	 @param config  LogUtilityConfig instance (indicates log location device ID and filter options)
	 @return DeviceLogIterator instance for the given log and configuration
	 
	 */
	public DeviceLogIterator getDeviceLogIteratorInstance(DeviceLog log, LogUtilityConfig config){
		DeviceLogIterator logIterator=null;
		long startTime=config.getStartTime();
		
		if (startTime>Long.MIN_VALUE) {
			logIterator = new DeviceLogIterator(log,startTime);
		}else{
			logIterator = new DeviceLogIterator(log);
		}		
		return logIterator;
	}
	
	/** Perform utility action for the specified packet.
	 Default action is to print the packet to stdout
	 using the current configuration.
	 
	 @param packet A DevicePacket instance
	 
	 */
	public void doPacketAction(DevicePacket packet)
	throws Exception{
		// default action is to print the packet to stdout
		System.out.print(getFormattedPacket(packet,this));
	}
	
	/** Applies filtering to the packet iterator.
	 The default filter checks packet against
	 configuration start and stop times, and
	 specified PacketFilters.
	 
	 @param packet DevicePacket instance
	 @param config  LogUtilityConfig instance (indicates log location device ID and filter options)
	 @return true if packet meets filter criteria, false otherwise
	 */
	public boolean applyFilter(DevicePacket packet,LogUtilityConfig config){
		
		PacketFilter[] packetFilters=config.getPacketFilters();
		long startTime=config.getStartTime();
		long stopTime=config.getStopTime();
		
		// Check time window criterion
		long timestamp = packet.systemTime();
		if (timestamp < startTime || timestamp > stopTime) {
			// Outside of time window
			return false;
		}
		
		// Check against packet filters
		if(_log4j.isDebugEnabled()){
			_log4j.debug("check against filters");
		}
		boolean passed = true;
		for (int i = 0; i < packetFilters.length; i++) {
			if (!packetFilters[i].pass(packet)) {
				if(_log4j.isDebugEnabled()){
					_log4j.debug("didn't pass filter #" + i);
				}
				passed = false;
				break;
			}
		}
		if (!passed) {
			return false;
		}
		return true;
	}
	
	/* Iterate through the device log, and perform the utility action.
	 If applyFilter is true, the action is performed only on packets
	 that pass the applyFilter method. Otherwise, the action is performed 
	 on all packets in the log.
	 
	 @param log DeviceLog instance to operate on
	 @param util LogUtility instance (has configuration and method overrides)
	 @param applyFilter Filter packets (using applyFilter) if true, process all packets otherwise
	 */
	public void processLog(DeviceLog log, LogUtility util, boolean applyFilter)
	throws Exception {
		
		LogUtilityConfig config=util.getConfiguration();
		DeviceLogIterator logIterator=util.getDeviceLogIteratorInstance(log,config);
		
		while (logIterator.hasNext()) {
			
			DevicePacket packet = (DevicePacket) logIterator.next();
			
			// either apply a packet filter 
			if (applyFilter) {
				//_log4j.debug("applying Filter for packet ["+packet.sequenceNo()+"]");
				if (util.applyFilter(packet,config)) {
					//_log4j.debug("passed Filter for packet ["+packet.sequenceNo()+"]");
					util.doPacketAction(packet);
				}
			}else{
				//_log4j.debug("NOT applying Filter for packet ["+packet.sequenceNo()+"]");
				util.doPacketAction(packet);
			}
		}
	}
	
	
	/** Return a String representation of the packet provided,
	 formatted using the configuration of the DeviceLogUtility.
	 
	 @param packet DevicePacket instance
	 @param util LogUtility instance (has configuration and method overrides)
	 @return String representation of packet, formatted according to configured format specifier string
	 
	 */
    public static String getFormattedPacket(DevicePacket packet, LogUtility util)
	throws Exception{
		
		// get log utility configuration
		LogUtilityConfig config=util.getConfiguration();
		
		// get format (note that escape sequences are replaced
		// during the configuration process)
		String format=null;
		
		// get packet format string from configuration
		if(packet instanceof SensorDataPacket) {
			format=config.getSDFormat();
		}else if (packet instanceof MetadataPacket) {
			format=config.getMDFormat();
		}else if (packet instanceof DeviceMessagePacket) {
			format=config.getDMFormat();
		}else if (packet instanceof SummaryPacket) {
			format=config.getSPFormat();
		}else {
			// if somehow, no format has been specified
			// use the default for the utility
			format=util.getDefaultFormat();
		}

		// get data and packet sizes, etc.
		PacketStats stats=new PacketStats(packet);
				
		// copy the format string to 
		// operate on.
		String build=new String(format);
		
		// find format specifiers and replace them with 
		// the appropriate information
		// rolled our own find/replace for 1.3 compatibility (newer Java String versions have replace methods)
		if(format.indexOf(LogUtility.FMT_EPOCH_TIME)>=0){
			build=findReplace(build,LogUtility.FMT_EPOCH_TIME,Long.toString(packet.systemTime()));
		}
		if(format.indexOf(LogUtility.FMT_UTC_TIME)>=0){
			build=findReplace(build,LogUtility.FMT_UTC_TIME,formatDateTime(packet.systemTime(),util.getCalendar(),util.getDateFormat()));
		}
		if(format.indexOf(LogUtility.FMT_PARSED_DATA)>=0){
			build=findReplace(build,LogUtility.FMT_PARSED_DATA,getParsedData(packet,config.getPacketParser()));
		}
		if(format.indexOf(LogUtility.FMT_DEVICE_ID)>=0){
			build=findReplace(build,LogUtility.FMT_DEVICE_ID,Long.toString(packet.sourceID()));
		}
		if(format.indexOf(LogUtility.FMT_PARENT_ID)>=0){
			build=findReplace(build,LogUtility.FMT_PARENT_ID,Long.toString(packet.getParentId()));
		}
		if(format.indexOf(LogUtility.FMT_RECORD_TYPE)>=0){
			build=findReplace(build,LogUtility.FMT_RECORD_TYPE,Long.toString(packet.getRecordType()));
		}
		if(format.indexOf(LogUtility.FMT_PACKET_TYPENAME)>=0){
			build=findReplace(build,LogUtility.FMT_PACKET_TYPENAME,packetTypeName(packet));
		}
		if(format.indexOf(LogUtility.FMT_SEQ_NUMBER)>=0){
			build=findReplace(build,LogUtility.FMT_SEQ_NUMBER,Long.toString(packet.sequenceNo()));
		}
		if(format.indexOf(LogUtility.FMT_DATA_SIZE)>=0){
			build=findReplace(build,LogUtility.FMT_DATA_SIZE,Long.toString(stats.getDataSize()));
		}
		if(format.indexOf(LogUtility.FMT_PACKET_SIZE)>=0){
			build=findReplace(build,LogUtility.FMT_PACKET_SIZE,Long.toString(stats.getPacketSize()));
		}
		if(format.indexOf(LogUtility.FMT_MDREF_NUMBER)>=0){
			build=findReplace(build,LogUtility.FMT_MDREF_NUMBER,Long.toString(packet.metadataRef()));
		}
		if(format.indexOf(LogUtility.FMT_PACKET_STATS)>=0){
			build=findReplace(build,LogUtility.FMT_PACKET_STATS,stats.toString());
		}
		if(format.indexOf(LogUtility.FMT_MD_CAUSE)>=0){
			// metadata cause returns empty string for non-metadata packets
			build=findReplace(build,LogUtility.FMT_MD_CAUSE,stats.getMetadataCause());
		}
		
		// replace escape sequences 
		// These need only be replaced once, during the configuration process
		// unneccessary to do here
		//build=unescape(build);
		
		// replace data specifiers last, since data may contain binary information 
		// that could be interpreted as formats or escape sequences
		
		// get the data buffer in the appropriate format
		// i.e., bin, hex or ASCII
		String bufferString=null;
		if(format.indexOf(LogUtility.FMT_DATA_BUFFER)>=0){
			int dataFormat=config.getFormatFlags();
			if (packet instanceof MetadataPacket) {
				if ( (dataFormat & LogUtilityConfig.BIN_ALL)>0 ||
					(dataFormat & LogUtilityConfig.BIN_METADATA)>0 ) {
					// show data buffer as BIN
					bufferString=new String(stats.getBuffer());
				}else if ( (dataFormat & LogUtilityConfig.HEX_ALL)>0 ||
						  (dataFormat & LogUtilityConfig.HEX_METADATA)>0 ) {
					// show data buffer as HEX
					bufferString=bytes2hex(stats.getBuffer());
				}else{
					// show data buffer as ASCII
					bufferString=new String(bytes2ascii(stats.getBuffer(),(byte)util.getBinaryFillChar()));
				}
			} else if (packet instanceof DeviceMessagePacket) {
				if ( (dataFormat & LogUtilityConfig.BIN_ALL)>0 ||
					(dataFormat & LogUtilityConfig.BIN_MESSAGE)>0 ) {
					// show data buffer as BIN
					bufferString=new String(stats.getBuffer());
				}else if ( (dataFormat & LogUtilityConfig.HEX_ALL)>0 ||
						  (dataFormat & LogUtilityConfig.HEX_MESSAGE)>0 ) {
					// show data buffer as HEX
					bufferString=bytes2hex(stats.getBuffer());
				}else{
					// show data buffer as ASCII
					bufferString=new String(bytes2ascii(stats.getBuffer(),(byte)util.getBinaryFillChar()));
				}
			} else if (packet instanceof SensorDataPacket) {
				if ( (dataFormat & LogUtilityConfig.BIN_ALL)>0 ||
					(dataFormat & LogUtilityConfig.BIN_DATA)>0 ) {
					// show data buffer as BIN
					bufferString=new String(stats.getBuffer());
				}else if ( (dataFormat & LogUtilityConfig.HEX_ALL)>0 ||
						  (dataFormat & LogUtilityConfig.HEX_DATA)>0 ) {
					// show data buffer as HEX
					bufferString=bytes2hex(stats.getBuffer());
				}else{
					// show data buffer as ASCII
					bufferString=new String(bytes2ascii(stats.getBuffer(),(byte)util.getBinaryFillChar()));
				}
			} else if (packet instanceof SummaryPacket) {
				if ( (dataFormat & LogUtilityConfig.BIN_ALL)>0 ||
					(dataFormat & LogUtilityConfig.BIN_SUMMARY)>0 ) {
					// show data buffer as BIN
					bufferString=new String(stats.getBuffer());
				}else if ( (dataFormat & LogUtilityConfig.HEX_ALL)>0 ||
						  (dataFormat & LogUtilityConfig.HEX_SUMMARY)>0 ) {
					// show data buffer as HEX
					bufferString=bytes2hex(stats.getBuffer());
				}else{
					// show data buffer as ASCII
					bufferString=new String(bytes2ascii(stats.getBuffer(),(byte)util.getBinaryFillChar()));
				}
			}
			
			// do the find replace
			build=findReplace(build,LogUtility.FMT_DATA_BUFFER,bufferString);
		}
		
		return build;
	}
	
	/** Return a mnemonic for the packet type of the specified packet. 
	 
	 @param packet A DevicePacket instance
	 @return Mnemonic for packet type (String)
	 
	 */
	protected static String packetTypeName(DevicePacket packet){
		if (packet instanceof MetadataPacket) {
			// MetadataPacket
			return LogUtility.TYPENAME_METADATA;
		} else if (packet instanceof DeviceMessagePacket) {
			// DeviceMessagePacket
			if (packet instanceof MeasurementPacket) {
				// MeasurementPacket
				return LogUtility.TYPENAME_MEASUREMENT;
			} else {
				// MessagePacket
				return LogUtility.TYPENAME_MESSAGE;//orig returned: "\n"+LogUtility.TYPENAME_MESSAGE
			}
		} else if (packet instanceof SensorDataPacket) {
			// SensorDataPacket
			return LogUtility.TYPENAME_SENSORDATA;
		} else if (packet instanceof SummaryPacket) {
			// SummaryPacket
			return LogUtility.TYPENAME_SUMMARY;
		}
		// Unknown
		return LogUtility.TYPENAME_UNKNOWN;
	}
	
	/** Return the Calendar instance */
	protected Calendar getCalendar(){return _calendar;}
	
	/** Return the DateFormatter instance */
	protected SimpleDateFormat getDateFormat(){return _dateFormatter;}
	
    /** Compute offset of local timezone from UTC */
    static int utcOffset(Calendar calendar) {
		return calendar.get(Calendar.ZONE_OFFSET) + 
	    calendar.get(Calendar.DST_OFFSET);
    }
	
	/** Return a String representation of parsed packet data, using the
	 specified parser.
	 @param packet DevicePacket instance
	 @param parser PacketParser instance capable of parsing the device packet
	 @return parsed packet as String, or null on error
	 */
	public static String getParsedData(DevicePacket packet, PacketParser parser){
		try {
			// Create new string buffer (could reuse...)
			StringBuffer sb=new StringBuffer();
			// get packet fields from parser
			PacketParser.Field[] fields = parser.parseFields(packet);
			// iterate over packet fields, adding name:value pair lines
			for (int j = 0; j < fields.length; j++) {
			    if (fields[j] == null) {
					continue;
			    }
			    sb.append(fields[j].getName() + ": "
						  + fields[j].getValue() + " "
						  + fields[j].getUnits());
			}
			// return string
			return sb.toString();
		} catch (Exception e) {
			System.err.println("Parse exception: " + e);
		}	
		// return null on error
		return null;
	}
	
	/** Returns a String version of the specifiec byte[]
	 in which the characters have been converted to ASCII hex.
	 
	 @param buffer Data buffer byte array
	 
	 @return String representation of buffer, in ASCII hex
	 
	 */
	public static String bytes2hex(byte[] buffer){
		StringBuffer sb=new StringBuffer();
		// iterate over buffer,
		// convert to ASCII hex
		for (int i = 0; i < buffer.length; i++) {
		    sb.append(Integer.toHexString((0xFF & buffer[i]) | 0x100).substring(1));
		}
		// stringify and return
		return sb.toString();
	}
	
	/** Returns a date/time string.
	 Uses the Calendar and SimpleDateFormat instances.
	 
	 @param time
	 @param calendar
	 @param dateFormat
	 
	 @return formatted date and time (UTC)
	 */
	public static String formatDateTime(long time, Calendar calendar, SimpleDateFormat dateFormat){
		// copy time
	    long t = time;
		// take out UTC offset
		t -= utcOffset(calendar);
		// format the date string
	    String dateString = 
		dateFormat.format(new Date(t)) + " UTC";
		// return it
		return dateString;
	}
	
	/** Check device log and repair (reconstruct index file) if true
	 
	 @param log DeviceLog instance to check
	 @param repair Repair index file if true, just check otherwise
	 
	 */
	public static void check(DeviceLog log,boolean repair){
		// delegate log check to the device log
		log.check(log,repair);
    }
	
    /** Return true if byte is printable.
	 
	 @param character byte to evaluate
	 @return true if character is printable ASCII, false otherwise
	 */
    protected static  boolean isPrintable(byte character) {
		// compare char to printable ASCII
		if ((character < 0x20 || character > 0x7e) && (character < 0x09 || character > 0x0D)) {
			// if it isn't printable,
			// return false
			return false;
		} else {
			// if it is, return true
			return true;
		}
    }
	
    /** Return ASCII string representation of byte buffer, 
	 in which unprintable characters are replaced with character sub 
	 
	 @param buffer A byte buffer
	 @param sub    Substitute character (to replace unprintable characters)
	 
	 @return String representation of buffer containing only printable ASCII
	 */
    private static byte[] bytes2ascii(byte[] buffer,byte sub) {
		// iterate over buffer
		for (int i = 0; i < buffer.length; i++) {
			if (!isPrintable(buffer[i])) {
				// substitute char sub for non-printable
				buffer[i] = sub;
			}
		}
		return buffer;
    }
	
	/**  
	 Returns new String, replacing escaped characters 
	 with corresponding binary characters 
	 in String str.
	 
	 @param str String to transform
	 
	 @return A string in which escape sequences have been replaced with their binary counterparts.
	 
	 */
	public static String unescape(String str){
		//System.out.println("unescaping ["+str+"]");
		String build=new String(str);
		
		if(str.indexOf(LogUtility.FMT_ESC_HEX)>=0)
			build=unescape(build,LogUtility.FMT_ESC_HEX,2,16);
		if(str.indexOf(LogUtility.FMT_ESC_OCT)>=0)
			build=unescape(build,LogUtility.FMT_ESC_OCT,3,8);
		if(str.indexOf(LogUtility.FMT_ESC_UNI)>=0)
			build=unescape(build,LogUtility.FMT_ESC_UNI,4,16);
		if(str.indexOf(LogUtility.FMT_ESC_DQUOTE)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_DQUOTE,"\"");
		if(str.indexOf(LogUtility.FMT_ESC_SQUOTE)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_SQUOTE,"\'");
		if(str.indexOf(LogUtility.FMT_ESC_NEWLINE)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_NEWLINE,"\n");
		if(str.indexOf(LogUtility.FMT_ESC_TAB)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_TAB,"\t");
		if(str.indexOf(LogUtility.FMT_ESC_BS)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_BS,"\b");
		if(str.indexOf(LogUtility.FMT_ESC_CR)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_CR,"\r");
		if(str.indexOf(LogUtility.FMT_ESC_FF)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_FF,"\f");
		if(str.indexOf(LogUtility.FMT_ESC_ESC)>=0)
			build=findReplace(build,LogUtility.FMT_ESC_ESC,"\\");
		
		
		return build;		
	}
	
    /** Replace substring find with substring replace in the specified string str
	 Used for replacing escape characters and general find/replace.
	 
	 @param str string to transform
	 @param find String to find
	 @param replace String to replace find with
	 @return Transformed version of str, with substitutions performed
	 */
    public static String findReplace(String str,String find,String replace){
		// find remaining occurences of find string
		while(str.indexOf(find)>=0){
			// break string around find string and substitute
			// replace string
			str=str.substring(0,str.indexOf(find))+
			replace+
			str.substring(str.indexOf(find)+find.length());
		}
		// return str
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
		
		// find remaining occurences of find string
		while(str.indexOf(find)>=0){
			// break string around find string
			int begin=str.indexOf(find)+find.length();
			int end=begin+len;
			//  and substitute replace string (parsed int)
			char c = (char)Integer.parseInt(str.substring(begin,end),radix);
			str=str.substring(0,str.indexOf(find))+
			c+
			str.substring(end);
		}
		return str;
    }
	
	/** Process one custom command line argument at specified index.
	 @param args command line arguments
	 @param index of arg to be processed
	 @return number of options processed.
	 */
	public int processCustomOption(String[] args, int index)
	throws Exception{
		System.out.println("Processing Custom Options [base class does nothing]");
		return 0;
	}
	
	/** Configure LogUtility from command line arguments.
	 
	 @param args String array containing command line arguments
	 @return true if configuration is successful, false otherwise.
	 */
	public boolean configure(String[] args)
	throws Exception{
		// temporary variables to hold configuration 
		// settings
		// deviceID
		long deviceID=-1L;
		// log directory path
		String logDirectory=null;
		// output format specifier string (global)
		String format=null;
		// output format specifier string (sensor data)
		String sdformat=null;
		// output format specifier string (metadata)
		String mdformat=null;
		// output format specifier string (summary packet)
		String spformat=null;
		// output format specifier string (device message)
		String dmformat=null;
		// packet parser name
		String parserName = null;
		// format flags (bit field)
		int formatFlags=0;
		// filter start time
		long startTime = Long.MIN_VALUE;
		// filter end time
		long stopTime = Long.MAX_VALUE;
		// filter type flags (bit field)
		int typeFlags = 0; 
		// packet modulus (do every skipInterval-th sample)
		int skipInterval = 0;
		
		// perform age calc flag
		boolean doCalcAge = false;
		// perform log index integrity check flag
		boolean doCheck=false;
		// perform log index repair flag
		boolean doRepair=false;
		
		// verbose application output 
		boolean verbose=false;
		
		// error flags for this method
		boolean usageError = false;
		boolean runTimeError=false;
		
		// Maximum number of 'option' tokens in args (last two args are
		// mandatory)
		int maxOptionTokens = args.length - 2;
		
		for (int i = 0; i < maxOptionTokens; i++) {
			
			if (args[i].equals(LogUtility.OPT_START) && i < maxOptionTokens - 1) {
				// Parse lower end of time window
				try {
					startTime=TimeUtils.parseDateTime(args[++i]);
				} catch (ParseException e) {
					System.err.println("Invalid timestring: " + args[i]);
					usageError = true;
				}
			} else if (args[i].equals(LogUtility.OPT_STOP) && i < maxOptionTokens - 1) {
				// Parse upper end of time window
				try {
					stopTime=TimeUtils.parseDateTime(args[++i]);
				} catch (ParseException e) {
					System.err.println("Invalid timestring: " + args[i]);
					usageError = true;
				}
			} else if (args[i].equals(LogUtility.OPT_TYPE) && i < maxOptionTokens - 1) {
				// Determine type of packets to print
				// all,metadata,data,message,summary
				// or any combination of these
				String stflags=args[++i];
				if(_log4j.isDebugEnabled()){
					_log4j.debug("typeFlag arg:["+stflags+"]");
				}
				if (stflags.indexOf(LogUtility.FLAGNAME_ALL)>=0){
					typeFlags|=DevicePacket.ALL_TYPES;
					if(_log4j.isDebugEnabled()){
						_log4j.debug("setting all flag:"+typeFlags);
					}
				}
				if (stflags.indexOf(LogUtility.FLAGNAME_METADATA)>=0){
					typeFlags |= DevicePacket.METADATA_FLAG;
					if(_log4j.isDebugEnabled()){
						_log4j.debug("setting metadata flag:"+typeFlags+" "+DevicePacket.METADATA_FLAG);
					}
				}
				// have to parse data differently, since it occurs within metadata
				// must be at beginning of string, be preceded by space or '\'
				if (stflags.indexOf(LogUtility.FLAGNAME_SENSORDATA)==0 ||
					stflags.indexOf(" "+LogUtility.FLAGNAME_SENSORDATA)>=0 ||
					stflags.indexOf("|"+LogUtility.FLAGNAME_SENSORDATA)>=0					
					){
					typeFlags |= DevicePacket.SENSORDATA_FLAG;
					if(_log4j.isDebugEnabled()){
						_log4j.debug("setting data flag:"+typeFlags+" "+DevicePacket.SENSORDATA_FLAG);
					}
				}
				if (stflags.indexOf(LogUtility.FLAGNAME_MESSAGE)>=0){
					typeFlags |= DevicePacket.DEVICEMESSAGE_FLAG;
					if(_log4j.isDebugEnabled()){
						_log4j.debug("setting message flag:"+typeFlags);
					}
				}
				if (stflags.indexOf(LogUtility.FLAGNAME_SUMMARY)>=0){
					typeFlags |= DevicePacket.SUMMARY_FLAG;
					if(_log4j.isDebugEnabled()){
						_log4j.debug("setting summary flag:"+typeFlags);
					}
				}
				
				if(_log4j.isDebugEnabled()){
					_log4j.debug("setting type flags in["+stflags+"]:"+typeFlags);
				}
			}else if (args[i].equals(LogUtility.OPT_CHECK)){
				// parse log check option
				doCheck=true;
			}else if (args[i].equals(LogUtility.OPT_REPAIR)){
				// parse log repair option
				doRepair=true;
				doCheck=true;
			}else if (args[i].equals(LogUtility.OPT_SKIP) && i < maxOptionTokens-1) {
				// parse skip interval
				try {
					skipInterval=Integer.parseInt(args[++i]);
				}
				catch (NumberFormatException e) {
					System.err.println("Invalid skip interval; integer required");
					usageError = true;
				}
			} else if (args[i].equals(LogUtility.OPT_FFLAGS) && i < maxOptionTokens - 1) {
				// parse format flags (specify data buffer output formats for various message types)
				String sfflags=args[++i];
				//BIN|HEX|SDB|SDH|DMB|DMH|SPB|SPH|MPB|MPH
				if(sfflags.indexOf(LogUtility.FFLAG_BIN)>=0){				
					formatFlags|=LogUtilityConfig.BIN_ALL;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_HEX)>=0){				
					formatFlags|=LogUtilityConfig.HEX_ALL;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_SDBIN)>=0){				
					formatFlags|=LogUtilityConfig.BIN_DATA;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_SDHEX)>=0){				
					formatFlags|=LogUtilityConfig.HEX_DATA;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_DMBIN)>=0){				
					formatFlags|=LogUtilityConfig.BIN_MESSAGE;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_DMHEX)>=0){				
					formatFlags|=LogUtilityConfig.HEX_MESSAGE;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_SPBIN)>=0){				
					formatFlags|=LogUtilityConfig.BIN_SUMMARY;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_SPHEX)>=0){				
					formatFlags|=LogUtilityConfig.HEX_SUMMARY;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_MPBIN)>=0){				
					formatFlags|=LogUtilityConfig.BIN_METADATA;
				}
				if(sfflags.indexOf(LogUtility.FFLAG_MPHEX)>=0){				
					formatFlags|=LogUtilityConfig.HEX_METADATA;
				}
				if(_log4j.isDebugEnabled()){
					_log4j.debug("sfflags:"+sfflags+" formatFlags:0x"+Integer.toHexString(formatFlags));
				}
			}else if (args[i].equals(LogUtility.OPT_PARSE) && i < maxOptionTokens - 1) {
				// parse parser class name option
				parserName = args[++i];
			} else if (args[i].equals(LogUtility.OPT_AGE)) {
				// parse compute age action option (calculate age of latest packet)
				doCalcAge=true;
			}else if (args[i].equals(LogUtility.OPT_FORMAT)) {
				// parse format string option
				format=args[++i];
			}else if (args[i].equals(LogUtility.OPT_SDFORMAT)) {
				// parse format string option
				sdformat=args[++i];
			}else if (args[i].equals(LogUtility.OPT_MDFORMAT)) {
				// parse format string option
				mdformat=args[++i];
			}else if (args[i].equals(LogUtility.OPT_SPFORMAT)) {
				// parse format string option
				spformat=args[++i];
			}else if (args[i].equals(LogUtility.OPT_DMFORMAT)) {
				// parse format string option
				dmformat=args[++i];
			}else if (args[i].equals(LogUtility.OPT_VERBOSE)) {
				// parse verbose output option
				verbose=true;
			}
			else {
				// argument not recognized...
				// pass it to custom processing method
				int test=processCustomOption(args,i);
				
				if (test>0){
					// if some arguments were processed
					// advance argument index
					i+=test;
				}else{
					// if nothing was processed,
					// indicate parsing error 
					System.err.println("Invalid or incomplete option: " + args[i]);
					System.err.println("i=" + i + ", maxOptionTokens="
									   + maxOptionTokens);
					usageError = true;
				}
			}
		}
		
		try{
			// validate device ID
			deviceID=Long.parseLong(args[args.length - 2]);
			if (deviceID<0) {
				throw new Exception("Invalid deviceID:"+deviceID);
			}
		}catch (Exception e) {
			System.err.println("Error parsing deviceID:" + args[args.length - 2]);
			usageError=true;
		}
		
		try{
			// validate log directory
			logDirectory=args[args.length - 1];
			
			File checkFile=new File(logDirectory);
			if (!checkFile.isDirectory()) {
				throw new Exception("Invalid log directory:"+checkFile);
			}
		}catch (Exception e) {
			System.err.println("Error parsing log directory:" + args[args.length - 1]);
			usageError=true;
		}
		
		// exit on error
		if (usageError || runTimeError) {
			return false;
		}
		
		// use global format, overrides, or default format if none specified
		if (format!=null) {
			// if format is set, use it for all unless
			// other per-packet overrides are set
			if (sdformat==null) {
				sdformat=format;
			}
			if (mdformat==null) {
				mdformat=format;
			}
			if (dmformat==null) {
				dmformat=format;
			}
			if (spformat==null) {
				spformat=format;
			}
		}else{
			// if no global format set, 
			// use overrides or class default
			// if none specified
			if (sdformat==null) {
				sdformat=this.getDefaultSDFormat();
			}
			if (mdformat==null) {
				mdformat=this.getDefaultMDFormat();
			}
			if (dmformat==null) {
				dmformat=this.getDefaultDMFormat();
			}
			if (spformat==null) {
				spformat=this.getDefaultSPFormat();
			}
		}
		
		// use default flags if none specified
		if (formatFlags==0) {
			formatFlags=LogUtilityConfig.ASCII_ALL;
		}
		
		// use default packet type flags if none specified
		if (typeFlags==0) {
			typeFlags=DevicePacket.ALL_TYPES;
		}
		
		// create the configuration instance
		LogUtilityConfig config = this.getConfiguration();
		
		// set configuration options
		config.setDeviceID(deviceID);
		config.setParserName(parserName);
		config.setLogDirectory(logDirectory);
		//config.setFormat(this.unescape(format));
		config.setSDFormat(this.unescape(sdformat));
		config.setMDFormat(this.unescape(mdformat));
		config.setSPFormat(this.unescape(spformat));
		config.setDMFormat(this.unescape(dmformat));
		config.setFormatFlags(formatFlags);
		
		config.setStartTime(startTime);
		config.setStopTime(stopTime);
		config.setSkipInterval(skipInterval);
		config.setTypeFlags(typeFlags);
		
		config.setDoCalcAge(doCalcAge);
		config.setDoCheck(doCheck);
		config.setDoRepair(doRepair);
		config.setVerbose(verbose);
		
		// return successfully
		return true;
	}
	
	/** Print use message to stderr.
	 */
	public void printUsage(){
		Properties sysProps=System.getProperties();
		String execName=sysProps.getProperty("exec.name","LogUtil");
		System.err.println("");
		System.err.println("usage: "+execName+" "
						   + "<options> deviceId directory");
		System.err.println("");
		System.err.println("Options:");
		
		System.err.println(" "+LogUtility.OPT_VERBOSE+"            verbose output (for debug)");
		System.err.println(" "+LogUtility.OPT_CHECK+"              check logs and summarize");
		System.err.println(" "+LogUtility.OPT_REPAIR+"             attempt to repair corrupt log");
		System.err.println(" "+LogUtility.OPT_AGE+"                display age (seconds) of latest packet in log");
		
		System.err.println(" "+LogUtility.OPT_START+" <time>       show records after <time>");
		System.err.println(" "+LogUtility.OPT_STOP+"               show records before <time>");
		System.err.println(" "+LogUtility.OPT_TYPE+"               packet type (may be OR'd) ["
						   + "all|metadata|data|message|summary]");
		
		System.err.println(" "+LogUtility.OPT_SKIP+" <interval>    skip every <interval>th record");
	    System.err.println(" "+LogUtility.OPT_PARSE+" <class>      parse data using <class> (must implement PacketParser)");
		System.err.println(" "+LogUtility.OPT_FFLAGS+" <flags>     set data format flags [BIN|HEX|SDB|SDH|DMB|DMH|SPB|SPH|MPB|MPH]");
		System.err.println(" "+LogUtility.OPT_FORMAT+" <string>    global format string [applies to all packet types, unless overridden]");
		System.err.println(" "+LogUtility.OPT_SDFORMAT+" <string>  sensor data format string");
		System.err.println(" "+LogUtility.OPT_MDFORMAT+" <string>  metadata format string");
		System.err.println(" "+LogUtility.OPT_DMFORMAT+" <string>  device message format string");
		System.err.println(" "+LogUtility.OPT_SPFORMAT+" <string>  summary format string");
		System.err.println("");
		System.err.println(" Valid format strings may use arbitrary text along with these");
		System.err.println(" format specifiers:");
		System.err.println("                   "+LogUtility.FMT_EPOCH_TIME+" timestamp (epoch time)");
		System.err.println("                   "+LogUtility.FMT_UTC_TIME+" timestamp (UTC date time)");
		System.err.println("                   "+LogUtility.FMT_DATA_BUFFER+" data buffer");
		System.err.println("                   "+LogUtility.FMT_PARSED_DATA+" parsed data");
		System.err.println("                   "+LogUtility.FMT_MD_CAUSE+" metadata cause");
		System.err.println("                   "+LogUtility.FMT_DEVICE_ID+" device ID");
		System.err.println("                   "+LogUtility.FMT_PARENT_ID+" parent ID");
		System.err.println("                   "+LogUtility.FMT_RECORD_TYPE+" record type");
		System.err.println("                   "+LogUtility.FMT_SEQ_NUMBER+" sequence number");
		System.err.println("                   "+LogUtility.FMT_DATA_SIZE+" data buffer size");
		System.err.println("                   "+LogUtility.FMT_PACKET_SIZE+" data packet size");
		System.err.println("                   "+LogUtility.FMT_MDREF_NUMBER+" metadata reference number");
		System.err.println("                   "+LogUtility.FMT_PACKET_STATS+" packet stats");
		System.err.println("                   "+LogUtility.FMT_PACKET_TYPENAME+" packet type name");
		System.err.println("                   plus escape sequences: \\ \\n \\t \\r \\f \\b \\xnn \\onnn \\unnn etc");
		System.err.println("");
		System.err.println("  default format [used for all packet types except metadata]:\n\""+this.getDefaultFormat()+"\"");
		System.err.println("");
		System.err.println("  default metadata format:\n\""+this.getDefaultMDFormat()+"\"");
		
		System.err.println("");
		return;
		
	}
	
	/** Main entry point for LogUtility.
	 Implements several default functions:
	 - calculate age of most recent packet in log
	 - check and optionally repair log index file
	 - print logs packets according to configured options
	 
	 @param args String array containing command line arguments
	 
	 */
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
		
		// Create log utility
		LogUtility util=new LogUtility();
		
		// Check command line options and print use message
		// if not enough options
		if (args.length < 2) {
			util.printUsage();
			return;
		}
		
		try{
			// Configure log utility using 
			// command line options
			if(util.configure(args)==false){
				// if configuration fails, 
				// print use message and exit
				util.printUsage();
				return;			
			}
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		try {
			// get log utility configuration
			LogUtilityConfig config=util.getConfiguration();
			// create DeviceLog instance
			DeviceLog log=new DeviceLog(config.getDeviceID(),config.getLogDirectory());
			
			if (config.getVerbose()) {
				// print configuration (verbose output mode)
				System.out.println(util.getConfiguration());
			}
			
			// Check action flags...
			
			if (config.getDoCalcAge()) {
				// Just display age of most recent packet, in seconds
				// and return
				System.out.println((System.currentTimeMillis() - log.getMaxTimestamp())/1000);
				return;
			}
			
			if(config.getDoCheck()){
				// do log index integrity check (optional repair)
				// and return
				util.check(log,config.getDoRepair());
				return;
			}
			
			// do utility action, per configuration options
			util.processLog(log,util,true);
			
		} catch (Exception e) {
			System.err.println(e);
		}		
    }
}

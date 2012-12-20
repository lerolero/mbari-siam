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
 Split a SIAM device log file into a new log containing a subset of the original log. 
 </p>
 <p> 
 At present, the underlying DeviceLog class does not have any public methods
 to support setting the name of the DeviceLog to an arbitrary name. Instead,
 it uses the device ID to create a file name according to a naming convention:
 </p>
 <p>
<code>
 &lt;deviceID&gt;_0.&lt;suffix&gt;
</code>
 </p>
 <p>
 and uses the suffix ".idx" for index files and ".dat" for data files.
 </p>
 
 <p>
 Until that constraint can be loosened, the new log created by the log splitter utility
 has the same name as the original and is not allowed to overwrite a file of the same name.
 So, a directory other than the original log directory must be specified, and it may not 
 contain files with the original log file name.
 </p>

 @see org.mbari.siam.operations.utils.LogUtility
 */
public class LogSplitter extends LogUtility {
	
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(LogSplitter.class);
	
	// command line options
	/** Output directory path (String) */
	protected static final String OPT_ODIR  ="-odir";
	
	/** Device log path (may not be the same as the input log path) */
	String _outputDirectoryPath=null;
	
	/** Output device log */
	DeviceLog _outputLog=null;
	
	/** No-arg Constructor */
	public LogSplitter(){
		super();
	}
	
	/** Get output file name.
	 
	 @return Path to output directory as a String.
	 */
	public String getOutputDirectoryPath(){
		if (_outputDirectoryPath!=null) {
			return _outputDirectoryPath;
		}
		return null;
	}
	
	/** Perform utility action for the specified packet.
	 Default action is to print the packet to stdout
	 using the current configuration.
	 
	 Overrides base class method, writing selected packets
	 (per filtering options) to the specified log file.
	 
	 @param packet A DevicePacket instance
	 
	 */
	public void doPacketAction(DevicePacket packet)
	throws Exception{
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug("processing packet ["+packet.sequenceNo()+"]");
		}
		
		// create output log if it doesn't exist
		// (should happen when processing first packet)
		if(_outputLog==null){
			// get device ID 
			long deviceID=packet.sourceID();
			// get output directory
			File outputDirectory=new File(_outputDirectoryPath);
			
			if(!outputDirectory.exists() || !outputDirectory.isDirectory()){
				// throw exception if file is doesn't exist or isn't directory
				throw new Exception("Output directory does not exist or is not a directory");
			}
			
			// check log file name
			// (this would be done better if there were methods
			// in DeviceLog to support manipulating log names)
			String dataFileName=_outputDirectoryPath+File.separator+deviceID+"_" + 0 +""+ ".dat";
			File test=new File(dataFileName);
			if(test.exists()){
				// throw exception if log file already exists
				// to prevent overwriting an existing log
				throw new Exception("Data file already exists ["+dataFileName+"]");
			}
			
			// create the log file
			if(_log4j.isDebugEnabled()){
				_log4j.debug("creating log ["+dataFileName+"]");
			}
			_outputLog=new DeviceLog(deviceID,outputDirectory.getPath());
		}
		
		// append the packet to the log
		// and don't check timestamps or metadata reference
		_outputLog.appendPacket(packet,false,false);
	}
	
	/** Print use message.
	 Appends to base class method output.
	 */
	public void printUsage(){
		super.printUsage();
		System.err.println("LogSplitter options:");
		System.err.println("");
		System.err.println(" "+LogSplitter.OPT_ODIR+"            name of output log");
		System.err.println("");
		return;
	}
	
	/** Process application-specific command line options.
	 Base class calls this method when it encounters an
	 argument that it doesn't recognize.
	 Method processes the argument if applicable, and 
	 returns the number of arguments it uses.
	 If it returns a number <=0, the base class issues 
	 an error indicating an unsupported option.
	 
	 @param args command line argument array
	 @param index argument to process
	 @return total number of arguments used
	 */
	public int processCustomOption(String[] args,int index)
	throws Exception{
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug("Processing Custom Options ["+index+"]");
		}
		
		// Maximum number of 'option' tokens in args (last two args are
		// mandatory)
		int maxOptionTokens = args.length - 2;
		int i=index;
		int optionCount=0;
		
		if (args[i].equals(LogSplitter.OPT_ODIR) && i < maxOptionTokens - 1) {
			// parse output file path option
			String odir=args[++i];
			
			if(_log4j.isDebugEnabled()){
				_log4j.debug("parsing odir["+odir+"]");
			}
			// increase argument count by two, one for the 
			// one for the option ('-ofile') and one for its
			// argument (e.g. '/path/to/output/directory')
			optionCount+=2;
			_outputDirectoryPath=odir;
		}
		
		// return number of arguments processed
		return optionCount;
	}
	
	/** Configure LogSplitter from command line arguments 
	 Overrides base class method
	 */
	public boolean configure(String[] args)
	throws Exception{
		// call super.configure, which will
		// pass custom options via processCustomOption()
		super.configure(args);
		
		// verify that (required) output directory is set
		if(getOutputDirectoryPath()==null){
			System.err.println("Output file name not set");
			return false;
		}
		
		// return true
		return true;
	}
	
	/** Main entry point for DeviceLogUtilBase.
	 Implements several default functions:
	 - calculate age of most recent packet in log
	 - check and optionally repair log index file
	 - print logs packets according to configured options
	 
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
		LogSplitter util=new LogSplitter();
		
		// Check command line options and print use message
		// if not enough options
		if (args.length < 4) {
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
			// get utility configuration
			LogUtilityConfig config=util.getConfiguration();
			// get device log
			DeviceLog log=new DeviceLog(config.getDeviceID(),config.getLogDirectory());
			
			if (config.getVerbose()) {
				// print configuration
				System.out.println("Configuration:\n"+util.getConfiguration());
			}
			
			// do utility action, per configuration options
			util.processLog(log,util,true);
			
		} catch (Exception e) {
			System.err.println(e);
		}		
    }
	
}

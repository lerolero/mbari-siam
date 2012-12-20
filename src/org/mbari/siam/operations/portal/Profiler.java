/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.util.Date;
import java.util.Hashtable;
import java.util.Enumeration;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Appender;
import org.apache.log4j.varia.StringMatchFilter;
import org.apache.log4j.varia.DenyAllFilter;
import org.mbari.siam.utils.StopWatch;

/**
   Help profile java apps by printing formatted messages to log4j.

   Profiler is NOT intented to be a hard real time profiling tool.

   It is intended to be used as a general debugging tool enabling
   users to 
   - get an idea for how much time is being spent on a code block (~ 1 msec resolution)
   - mark related sets of debug statements so that they may be easily separated from
     other program output
   - produce structured debug statements that may be easily loaded into statistical
     tools like Excel and Matlab for analysis
   
*/
public class Profiler {

    private Logger _log4j;
    String _prefix="siam.profiler";
    String _delimiter=";";
    public boolean _enabled=false;
    Hashtable _events=new Hashtable();
    public static Integer END=new Integer(0);
    public static Integer START=new Integer(1);
    public static Integer UNDEFINED=new Integer(-1);
    public static Integer INFO=new Integer(2);
    public static String _start="start";
    public static String _end="end";
    public static String _info="info";

    public static final long NO_ID=-1;

    public static final int NO_STATUS=-1;
    public static final int OK=0;
    public static final int EXCEPTION=1;
    public static final int TIMEOUT_EXCEPTION=2;
    public static final int LEASEREFUSED_EXCEPTION=3;
    public static final int DEVICENOTFOUND_EXCEPTION=4;
    public static final int REMOTE_EXCEPTION=5;
    public static final int NODATA_EXCEPTION=6;
    public static final int IO_EXCEPTION=7;
    public static final int UNKNOWNHOST_EXCEPTION=8;

    public Profiler(){
    }

    public Profiler(Logger logger){
	this();
	setLogger(logger);
    }

    public Profiler(String prefix,String delimiter){
	this();
	setPrefix(prefix);
	setDelimiter(delimiter);
    }

    public Profiler(Logger logger, String prefix,String delimiter){
	this(logger);
	setPrefix(prefix);
	setDelimiter(delimiter);
    }

    public void setLogger(Logger logger){
	_log4j=logger;
	//System.out.println("setLogger:"+ _log4j.getName());
	for(Enumeration e=_log4j.getAllAppenders();e.hasMoreElements();){
	    Appender a = (Appender)e.nextElement();
	    System.out.println(_log4j.getName()+" appender: "+a.getName());
	}
    }

    public static void excludeAppender(String appender,String matchString){
	StringMatchFilter smf=new StringMatchFilter();
	smf.setStringToMatch(matchString);
	smf.setAcceptOnMatch(false);
	Logger.getRootLogger().getAppender(appender).addFilter(smf);
    }

    public static void excludeExternal(String appender,String matchString){
	StringMatchFilter smf=new StringMatchFilter();
	smf.setStringToMatch(matchString);
	smf.setAcceptOnMatch(true);
	Logger.getRootLogger().getAppender(appender).addFilter(smf);
   
	DenyAllFilter daf=new DenyAllFilter();
	Logger.getRootLogger().getAppender(appender).addFilter(daf);
    }

    public void setPrefix(String prefix){
	if(prefix!=null)
	    _prefix=prefix;
    }

    public void setDelimiter(String delimiter){
	if(delimiter!=null)
	    _delimiter=delimiter;
    }

    public void out(String msg){	
	if(_enabled)
	    _log4j.debug(_prefix+_delimiter+msg);
    }

    public void info(String msg){
	out(msg);
    }

    public void error(String msg){
	out(msg);
    }

    public void info(String name, int status, long deviceID, String msg){
	if(_enabled)
	    out( System.currentTimeMillis()+_delimiter+
			  _info+_delimiter+
			  name+_delimiter+
			  status+_delimiter+
			  deviceID+_delimiter+
			  msg);
    }

    public void start(String name, int status, long deviceID, String msg){
	Integer state=(Integer)_events.get(name);

	if(state!=null){
	    if(state==Profiler.START)
		end(name,status,deviceID,"closing open start");
	}
	if(_enabled)
	    out(System.currentTimeMillis()+_delimiter+
			  _start+_delimiter+
			  name+_delimiter+
			  status+_delimiter+
			  deviceID+_delimiter+
			  msg);
	_events.put(name,Profiler.START);
    }

    public void end(String name, int status, long deviceID, String msg){
	Integer state=(Integer)_events.get(name);

	if(state!=null){
	    if(state==Profiler.END){
		info("warning",status,deviceID,"end request: "+name+" not started");
		return;
	    }
	}

	if(_enabled)
	    out(System.currentTimeMillis()+_delimiter+
			  _end+_delimiter+
			  name+_delimiter+
			  status+_delimiter+
			  deviceID+_delimiter+
			  msg);
	_events.put(name,Profiler.END);
    }

    public void setEnabled(boolean value){
	_enabled=value;
    }
    public boolean isEnabled(){
	return _enabled;
    }

    public static void main (String[] args)
	throws Exception
    {
	StopWatch s = new StopWatch();

	// set up logging...
	PatternLayout layout =new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	// Get server/servlet debug level from environment
	Logger logger=Logger.getLogger("moos.utils");
	String logLevel=System.getProperty("log4j.threshold");
	logger.setLevel(Level.DEBUG);
	
	Profiler p = new Profiler(logger);
	p.setEnabled(true);
	logger.debug("--------------");
	p.start("foo",Profiler.OK,0,"a");
	s.delay(10);
	p.end("foo",Profiler.OK,0,"a");

	logger.debug("--------------");
	p.start("foo",Profiler.OK,0,"a");
	p.start("foo",Profiler.OK,0,"a");
	s.delay(10);
	p.end("foo",Profiler.OK,0,"b");
	p.end("foo",Profiler.OK,0,"b");

	logger.debug("--------------");
	p.info("just because",Profiler.OK,0,"fyi...");
    }    
}


/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.io.*;
import java.util.Date;
import java.util.Arrays;
import java.util.AbstractCollection;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.NoSuchElementException;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;


public class StatsAnalyzer{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(StatsAnalyzer.class);


    public static String DFLT_DIR=".";

    String _fileName=null;
    File _inFile=null;
    String _outputDirectory=DFLT_DIR;

    HashSet _deviceIDSet=new HashSet();
    HashSet _recordTypeSet=new HashSet();
    String _delimiter=";";

    public StatsAnalyzer(){}

    public StatsAnalyzer(String file){
	super();
	this.setInputFile(file);
    }

    public StatsAnalyzer(String file,String outputDirectory){
	this(file);
	this.setOutputDirectory(outputDirectory);
    }    

    public static void printUsage(){
	System.out.println("\nUsage: pstats [-d <outputDirectory>] <statsFile>");
	System.out.println("\nstats: file containing PacketStats records");
	System.out.println("\nOptions:");
	System.out.println("  -d: directory in which to place output ["+DFLT_DIR+"]\n\n");
    }

    public static void exitError(String msg, int exitCode){
	System.out.println(msg);
	printUsage();
	System.exit(exitCode);
    }
    public void setInputFile(String file){
	_inFile=new File(file);
	
	if(!_inFile.exists())
	    exitError("StatsAnalyzer: File not found ("+file+")",1);
    }
    public String getPath(String name){
	return _outputDirectory+"/"+name;
    }

    public void setOutputDirectory(String dir){
	_outputDirectory=dir;
    }

    public void analyze(){
	try{
	    BufferedReader br = new BufferedReader(new FileReader(_inFile));
	    String line=null;
	    Vector statSets=new Vector();
	    //	    int i=0;
	    while(br.ready()){

		line=br.readLine();
		try{
		    if(line!=null && line.length()>0){
			PacketStats ps = new PacketStats(line);
			//_log4j.debug("packetStats: "+ps);
			if(ps.isValid()){
			    PacketStatSet ss =null;
			    //_log4j.debug("looking for match "+ps.getSourceID()+","+ps.getRecordType());
			    for(Enumeration e=statSets.elements();e.hasMoreElements();){
				PacketStatSet x = (PacketStatSet)e.nextElement();
				if(x.recordType()==ps.getRecordType() && 
				   x.sourceID()==ps.getSourceID()){
				    x.process(ps);
				    ss=x;
				    break;
				}
			    }
			    if(ss==null){
				//_log4j.debug("match NOT found "+ps.getSourceID()+","+ps.getRecordType());
				PacketStatSet y=new PacketStatSet();
				y.process(ps);
				statSets.add(y);
				_deviceIDSet.add(new Long(ps.getSourceID()));
				_recordTypeSet.add(new Long(ps.getRecordType()));
			    }
			}
		    }
		}catch(Exception e){
		    e.printStackTrace();
		    System.exit(0);
		    _log4j.error(e);
		}
	    }
	    
	    export(statSets,true);
	    exportTable(statSets,true);

	}catch(FileNotFoundException e){
	    e.printStackTrace();
	    return;
	}catch(IOException i){
	    i.printStackTrace();
	    return;
	}
    }

    class PacketStatSet extends StatSet{

	long _firstTime=Long.MAX_VALUE;
	long _lastTime=Long.MIN_VALUE;
	String _firstDate="";
	String _lastDate="";
	long _recordType=-1L;
	long _sourceID=-1L;
	long _parentID=-1L;
	long _loggedSizeBytes=0L;
	double _loggedSizeAverageBytes=0.0;

	public PacketStatSet(){
	    super();
	}

	public PacketStatSet(String outFile){
	    super(outFile);
	}

	public void process(Object value){
	    PacketStats ps=(PacketStats)value;

	    //_log4j.debug("PacketStatSet.process");
	    if(_sourceID<0)
		_sourceID=ps.getSourceID();
	    if(_parentID<0)
		_parentID=ps.getParentID();
	    if(_recordType<0)
		_recordType=ps.getRecordType();

	    long t=ps.getSystemTime();
	    if(t<_firstTime){
		_firstTime=t;
		_firstDate=ps.getDateString();
	    }
	    if(t>_lastTime){
		_lastTime=t;
		_lastDate=ps.getDateString();
	    }
	    long packetBytes=ps.getLoggedSize();

	    if(packetBytes>_max)
		_max=packetBytes;
	    if(packetBytes<_min)
		_min=packetBytes;
	    
	    //_log4j.debug(_count+","+_loggedSizeBytes+","+packetBytes+":::"+ps);
	    _loggedSizeBytes+=packetBytes;
	    _loggedSizeAverageBytes=_loggedSizeBytes/(++_count);
	    //_log4j.debug(_count+","+_loggedSizeBytes+","+packetBytes+":::"+ps);

	}

	public long recordType(){return _recordType;}
	public long sourceID(){return _sourceID;}
	public long parentID(){return _parentID;}
	public long loggedSizeTotal(){return _loggedSizeBytes;}
	public double loggedSizeAverage(){return _loggedSizeAverageBytes;}

	public String header(){
	    String s="parentID"+this._delimiter+
		"sourceID"+this._delimiter+
		"recordType"+this._delimiter+
		"firstDate"+this._delimiter+
		"lastDate"+this._delimiter+
		"count"+this._delimiter+
		"min"+this._delimiter+
		"max"+this._delimiter+
		"loggedSizeAverageBytes"+this._delimiter+
		"loggedSizeBytes";
	    return s;
	}

	public String toString(){
	    String s=_parentID+this._delimiter+
		_sourceID+this._delimiter+
		_recordType+this._delimiter+
		_firstDate+this._delimiter+
		_lastDate+this._delimiter+
		_count+this._delimiter+
		_min+this._delimiter+
		_max+this._delimiter+
		_loggedSizeAverageBytes+this._delimiter+
		_loggedSizeBytes;
	    return s;
	}
    }

    public void export(Vector statSets,boolean showHeader){
	if(showHeader)
	    System.out.println(((PacketStatSet)(statSets.get(0))).header());
	for(Enumeration e=statSets.elements();e.hasMoreElements();){
	    PacketStatSet ss = (PacketStatSet)e.nextElement();
	    System.out.println(ss);
	}
    }

    public String getTableHeader(Long[] deviceIDs){
	String header="";
	for(int i=0;i<Arrays.asList(deviceIDs).size();i++){
	    // add delimiter to all (even 0) so that cols
	    // match up
	    header+=_delimiter+deviceIDs[i].longValue();
	}    
	return header;
    }

    public String getTableTotals(Long[] totals){
	String retval="";
	for(int i=0;i<Arrays.asList(totals).size();i++){
	    // add delimiter to all (even 0) so that cols
	    // match up
	    retval+=_delimiter+totals[i].longValue();
	}    
	return retval;
    }
    public String getTableTotals(Double[] totals){
	String retval="";
	for(int i=0;i<Arrays.asList(totals).size();i++){
	    // add delimiter to all (even 0) so that cols
	    // match up
	    retval+=_delimiter+totals[i].doubleValue();
	}    
	return retval;
    }

    public void exportTable(Vector statSets,boolean showHeader){
	
	Long[] deviceIDs=new Long[_deviceIDSet.size()];
	int idx=0;
	for(Iterator i=_deviceIDSet.iterator();i.hasNext();){
	    deviceIDs[idx++]=(Long)i.next();

	}
	Long[] recordTypes=new Long[_recordTypeSet.size()];
	idx=0;
	for(Iterator i=_recordTypeSet.iterator();i.hasNext();){
	    recordTypes[idx++]=(Long)i.next();
	}

	Arrays.sort(deviceIDs);
	Arrays.sort(recordTypes);
	int rows=Array.getLength(recordTypes);
	int cols=Array.getLength(deviceIDs);

	Long[][] cellsTotal=new Long[rows][cols];
	Double[][] cellsAverage=new Double[rows][cols];
	Long[] totalSum=new Long[cols];
	Double[] averageSum=new Double[cols];

	for(int i=0;i<rows;i++){
	    for(int j=0;j<cols;j++){
		//_log4j.debug("id "+id+" type "+type);
		long id=deviceIDs[j].longValue();
		long type=recordTypes[i].longValue();
		for(Enumeration e=statSets.elements();e.hasMoreElements();){
		    PacketStatSet ss = (PacketStatSet)e.nextElement();
		    if(ss.sourceID()==id && ss.recordType()==type){
			cellsTotal[i][j]=new Long(ss.loggedSizeTotal());
			cellsAverage[i][j]=new Double(ss.loggedSizeAverage());
			if(totalSum[j]==null)
			    totalSum[j]=new Long(cellsTotal[i][j].longValue());
			else
			    totalSum[j]=new Long(totalSum[j].longValue()+cellsTotal[i][j].longValue());

			if(averageSum[j]==null)
			    averageSum[j]=new Double(cellsAverage[i][j].doubleValue());
			else
			    averageSum[j]=new Double(averageSum[j].doubleValue()+cellsAverage[i][j].doubleValue());
		    }
		}
	    }
	}

	System.out.println("\nTotal Packet Size by deviceID and packetType");
	System.out.println(getTableHeader(deviceIDs));
	for(int i=0;i<rows;i++){
	    System.out.print(recordTypes[i].longValue());
	    for(int j=0;j<cols;j++){
		System.out.print(_delimiter);
		if(cellsTotal[i][j]!=null)
		System.out.print(cellsTotal[i][j]);
	    }
	    System.out.print("\n");
	}
	System.out.println(getTableTotals(totalSum));

	System.out.println("\nAverage Packet Size by deviceID and packetType");
	System.out.println(getTableHeader(deviceIDs));
	for(int i=0;i<rows;i++){
	    System.out.print(recordTypes[i].longValue());
	    for(int j=0;j<cols;j++){
		System.out.print(_delimiter);
		if(cellsAverage[i][j]!=null)
		System.out.print(cellsAverage[i][j]);
	    }
	    System.out.print("\n");
	}
	System.out.println(getTableTotals(averageSum));
    }

    public static void processArgs(StatsAnalyzer sa, String args[]){
	for(int i=0;i<args.length;i++){
	    if(args[i].equals("-d")){
		sa.setOutputDirectory(args[i+1]);
		i++;
	    }else{
		sa.setInputFile(args[i]);
	    }
	}
    }

    public static void main(String args[]) {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	//PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	PatternLayout layout = new PatternLayout(" %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);
	/*
	System.out.println(args.length+" args:");
	for(int i=0;i<args.length;i++)
	    System.out.println(args[i]);
	*/
	if(args.length<1){
	    exitError("\nMissing file name",1);
	}
	StatsAnalyzer pe=new StatsAnalyzer();
	StatsAnalyzer.processArgs(pe,args);
	pe.analyze();
	
    }

}

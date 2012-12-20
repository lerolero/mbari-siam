/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.io.*;
import java.util.Arrays;
import java.util.AbstractCollection;
import java.lang.reflect.Array;
import java.util.Date;
import java.text.DateFormat;
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

import org.mbari.siam.operations.portal.PacketStats;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;

/*
==== event profiling ====
eventName:startMsg:endMsg
connected:info."got connection":info."Link terminated"
reset WDT:start."reset WDT":end."reset WDT"
node info:start."getID":start."getDeviceIDs"
retrieveAndDistributeData:start."retrieveAndDistributeData":end."retrieveAndDistributeData"
retrieve data:start."retrievePackets":end."retrievePackets"
get packets:start."getDevicePackets":end."getDevicePackets"
process packets:end."getDevicePackets":end."retrievePackets"
				
event start/stop->event duration

==== transfer stats ====
start/end.getDevicePacket,info.totalPackets/packets,info.stats(deviceID,packetSize)
->transferSize,effectiveBitRate,%Nominal

*/

public class ProfileAnalyzer{

    // CVS revision 
    private static String _versionID = "$Revision: 1.2 $";

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(ProfileAnalyzer.class);

    public static final int FORMAT_LEGACY=0;
    public static final int FORMAT_ABSTIME=1;
    public static Integer END=new Integer(0);
    public static Integer START=new Integer(1);
    public static Integer UNDEFINED=new Integer(-1);
    public static Integer INFO=new Integer(2);
    public static String _start="start";
    public static String _end="end";
    public static String _info="info";
    public static String DFLT_DIR=".";
    public static int DFLT_EVENTFORMAT=FORMAT_ABSTIME;
    public final int NOMINAL_BITRATE=7800;

    String _delimiter=";";
    HashSet _eventNameSet=new HashSet();
    HashSet _eventTypeSet=new HashSet();

    String _fileName=null;
    File _inFile=null;
    String _outputDirectory=DFLT_DIR;
    int _eventFormat=DFLT_EVENTFORMAT;

    public ProfileAnalyzer(){}
    public ProfileAnalyzer(String file){
	super();
	this.setInputFile(file);
    }
    public ProfileAnalyzer(String file,String outputDirectory,int eventFormat){
	this(file);
	this.setOutputDirectory(outputDirectory);
    }    
    public static void printUsage(){
	System.out.println("\nportalProfile: portal profile data extracted from portal log");
	System.out.println("\nUsage: gpStats [-d <outputDirectory>] <portalProfile>");
	System.out.println("Options:");
	System.out.println("  -d: directory in which to place output ["+DFLT_DIR+"]\n");
	System.out.println("  -l: set event format to FORMAT_LEGACY ["+(DFLT_EVENTFORMAT==FORMAT_ABSTIME?"FORMAT_ABSTIME":"FORMAT_LEGACY")+"]\n");
	System.out.println("see Also: siam/utils/epStats\n");

    }

    public static void exitError(String msg, int exitCode){
	System.out.println(msg);
	printUsage();
	System.exit(exitCode);
    }
    public void setInputFile(String file){
	_inFile=new File(file);
	
	if(!_inFile.exists())
	    exitError("ProfileAnalyzer: File not found ("+file+")",1);
    }
    public String getPath(String name){
	return _outputDirectory+"/"+name;
    }

    public void setOutputDirectory(String dir){
	_outputDirectory=dir;
    }

    public void setEventFormat(int eventFormat){
	_eventFormat=eventFormat;
    }

    public void analyze(){
	try{
	    BufferedReader br = new BufferedReader(new FileReader(_inFile));
	    String line=null;

	    DataSet pp=new ProfileAnalyzer.PlotProfile();

	    while(br.ready()){
		line=br.readLine();
		try{
		    ProfileEvent pe = new ProfileEvent(line,_eventFormat);
		    //_log4j.debug("event: "+pe);
		    pp.notify(pe);
		}catch(Exception e){
		    _log4j.warn(e);
		}
	    }
	    pp.export();
	    Vector statSets=new Vector();
	    exportStats(((PlotProfile)pp).getSegments(),statSets);
	    exportTable(statSets,true);

	}catch(FileNotFoundException e){
	    e.printStackTrace();
	    return;
	}catch(IOException i){
	    i.printStackTrace();
	    return;
	}
    }

    class ProfileEvent{
	
	String _stime=null;
	String _name=null;
	String _type=null;
	int _status=Profiler.NO_STATUS;
	long _id=Profiler.NO_ID;
	String _prefix="profile";
	String _delimiter=";";
	String _message=null;
	long _itime=-1L;
	int _eventFormat=FORMAT_ABSTIME;

	long _deviceID=-1L;
	PacketStats _stats=null;

	public ProfileEvent(){}

	public ProfileEvent(String line,int eventFormat){
	    _eventFormat=eventFormat;
	    parse(line);
	}
    
	public boolean parse(String sEvent){
	    switch(_eventFormat){
	    case FORMAT_LEGACY:
		return parseLegacy(sEvent);
	    case FORMAT_ABSTIME:
		return parseAbsTime(sEvent);
	    default:
		_log4j.error("Invalid ProfileEvent format: "+_eventFormat);
	    }
	    return false;
	}
	/** The AbsTime format has a consistent structure
	    prefix,time,type,name,message
	    where time is an absolute time (ms since epoch).
	*/
	public boolean parseAbsTime(String sEvent){

	    StringTokenizer st = new StringTokenizer(sEvent,this._delimiter);
	    //_log4j.debug("event "+sEvent);
	    try{
		_prefix  = st.nextToken().trim();
		_stime   = st.nextToken().trim();
		_itime   = Long.parseLong(_stime);
		_type    = st.nextToken().trim();
		_name    = st.nextToken();
		String sStatus  = st.nextToken();
		_status = Integer.parseInt(sStatus);
		String sID      = st.nextToken();
		_deviceID = Long.parseLong(sID);
		_message = st.nextToken();
		if(_type.equals(Profiler._info)){
		    //_log4j.debug("info event msg="+_message);
		    if(_message.equals("stats")){
			String stats="";
			while(st.hasMoreTokens()){
			    stats+=st.nextToken().trim();
			    if(st.hasMoreTokens())
				stats+=this._delimiter;
			}
			_stats=new PacketStats(stats,this._delimiter);
		    }
		}
		return true;
	    }catch(Exception e){
		//e.printStackTrace();
		_log4j.warn("parseAbsTime: parsing error "+e);
	    }
	    return false;
	}

	/** The Legacy format has a format that varies with
	    type:
	    time,prefix,type,name,message (start,end packets)
	    time,prefix,type,message (info packets)
	    where time is an relative time (ms since start of application)
	*/
	public boolean parseLegacy(String sEvent){

	    StringTokenizer st = new StringTokenizer(sEvent,this._delimiter);
	    try{
		_stime  = st.nextToken().trim();
		_itime = Long.parseLong(_stime);
		_prefix = st.nextToken().trim();
		_type   = st.nextToken().trim();
		if(_type.equals(Profiler._info)){
		    _message = st.nextToken();
		    if(_message.equals("stats")){
			String stats="";
			while(st.hasMoreTokens()){
			    stats+=st.nextToken().trim();
			    if(st.hasMoreTokens())
				stats+=this._delimiter;
			}
			_stats=new PacketStats(stats,this._delimiter);
		    }
		    return true;
		}
		_name   = st.nextToken();
		_message = st.nextToken();
		return true;
	    }catch(Exception e){
		e.printStackTrace();
	    }
	    _log4j.error("parseLegacy: parsing error");

	    return false;
	}

	public boolean isStart(){
	    if(_type.equals(Profiler._start))
		return true;
	    return false;
	}
	public boolean isEnd(){
	    if(_type.equals(Profiler._end))
		return true;
	    return false;
	}
	public boolean isInfo(){
	    if(_type.equals(Profiler._info))
		return true;
	    return false;
	}

	public void setDelimiter(String delimiter){
	    this._delimiter=delimiter;
	}
	public String getDelimiter(){
	    return this._delimiter;
	}

	public void setName(String name){
	    _name=name;
	}
	public String getName(){
	    return _name;
	}
	public String getType(){
	    return _type;
	}
    
	public void setPrefix(String prefix){
	    _prefix=prefix;
	}
	public String getPrefix(){
	    return _prefix;
	}

	public long getTime(){
	    return _itime;
	}
 	public String getStime(){
	    return _stime;
	}
	public void setMessage(String message){
	    _message=message;
	}
	public String getMessage(){
	    return _message;
	}
	public PacketStats getStats(){
	    return _stats;
	}
	public long getSourceID(){
	    return _deviceID;
	}
	public int getStatus(){
	    return _status;
	}
	public String toString(){
	    String retval="";
	    retval=_stime+this._delimiter+
		_prefix+this._delimiter+
		_type+this._delimiter+
		_name+this._delimiter+
		_message+this._delimiter+
		_deviceID+this._delimiter+
		_stats;

	    return retval;
	}

    }

    class EventSegment{
	long _start=-1L;
	long _end=-1L;
	String _name=null;
	boolean _open=false;
	int _value=-1;
	int _index=-1;
	Vector _stats=new Vector();
	long _deviceID=-1L;
	long _totalPacketBytes=0L;
	long _totalDataBytes=0L;
	long _transferPackets=0L;
	int _ostatus=Profiler.NO_STATUS;
	int _cstatus=Profiler.NO_STATUS;
	String _omessage=null;
	String _cmessage=null;

	public final int START=0x0;
	public final int END=0x1;
	public final int DURATION=0x2;
	public final int NAME=0x4;
	public final int VALUE=0x8;
	public final int INDEX=0x10;
	public final int BLANK=0x20;
	public final int NEWLINE=0x40;
	public final int DEVICEID=0x80;
	public final int TRANSFER_BYTES=0x100;
	public final int EFFECTIVE_BITRATE=0x200;
	public final int PERCENT_NOMINAL_BITRATE=0x400;
	public final int TRANSFER_PACKETS=0x800;
	public final int OSTATUS=0x1000;
	public final int CSTATUS=0x2000;
	public final int OMESSAGE=0x4000;
	public final int CMESSAGE=0x8000;
	public final int NOOP=0x10000;

	public final int BITS_PER_BYTE=10;
	//	public final int NOMINAL_BITRATE=7200;

	public EventSegment(){}
	public EventSegment(String name,ProfileEvent pe){
	    this();
	    open(name,pe);
	}

	public void open(String name,ProfileEvent pe){
	    _start=pe.getTime();
	    _ostatus=pe.getStatus();
	    _omessage=pe.getMessage();
	    _name=name;
	    if(_deviceID==(-1L) && pe.getSourceID()>0)
		_deviceID=pe.getSourceID();
	    _open=true;
	}
	public void setIndex(int index){
	    _index=index;
	}
	public int getIndex(){
	    return _index;
	}
	public void setValue(int value){
	    _value=value;
	}
	public int getValue(){
	    return _value;
	}
	public long getTotalPacketBytes(){
	    return _totalPacketBytes;
	}
	public long getTotalDataBytes(){
	    return _totalDataBytes;
	}
	public String getName(){
	    return _name;
	}
	public long getStart(){return _start;}
	public long getEnd(){return _end;}
	public long getDuration(){return (_end-_start);}

	public void close(ProfileEvent pe){
	    _end=pe.getTime();
	    _cstatus=pe.getStatus();
	    _open=false;
	    _cmessage=pe.getMessage();
	}
	public void close(long time){
	    _end=time;
	    _open=false;
	}
	public void addStats(PacketStats stats ){
	    _totalPacketBytes+=stats.getExportSize();
	    if(_deviceID==(-1L))
		_deviceID=stats.getSourceID();
	    _stats.add(stats);
	    _transferPackets++;
	}
	public Vector getStats(){
	    return _stats;
	}
	public long getSourceID(){
	    return _deviceID;	

	}

	public String getStartDate(){
	    return DateFormat.getDateTimeInstance().format(new Date(_start));
	}
	public String getEndDate(){
	    return DateFormat.getDateTimeInstance().format(new Date(_end));
	}

	public boolean isOpen(){
	    return _open;
	}
	public long duration(){
	    if(isOpen())
		return -1;
	    else
		return (_end-_start);
	}

	public String export(int[] components, String delimiter){
	    String retval="";
	    
	    for(int i=0;i<components.length;i++){
		if(i!=0 && components[i]!=NEWLINE && components[i-1]!=NEWLINE && delimiter!=null)
		    retval+=delimiter;
		SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		switch(components[i]){
		case START:
		    retval+=sdf.format(new Date(_start));
		    break;
		case END:
		    retval+=sdf.format(new Date(_end));
		    break;
		case DURATION:
		    retval+=(_end-_start);
		    break;
		case NAME:
		    retval+=_name;
		    break;
		case DEVICEID:
		    retval+=_deviceID;
		    break;
		case TRANSFER_BYTES:
		    retval+=_totalPacketBytes;
		    break;
		case CSTATUS:
		    retval+=_cstatus;
		    break;
		case OSTATUS:
		    retval+=_ostatus;
		    break;
		case CMESSAGE:
		    retval+=_cmessage;
		    break;
		case OMESSAGE:
		    retval+=_omessage;
		    break;
		case EFFECTIVE_BITRATE:
		    double ebr=(double )(_totalPacketBytes*BITS_PER_BYTE);
		    ebr=ebr/((double)((_end-_start)/1000.0));
		    retval+=(long)ebr;
		    break;
		case PERCENT_NOMINAL_BITRATE:
		    double pnb=(double )(_totalPacketBytes*BITS_PER_BYTE);
		    pnb=pnb/((double)((_end-_start)/1000.0));
		    pnb=pnb/(double)NOMINAL_BITRATE;
		    NumberFormat nf=NumberFormat.getInstance();
		    nf.setMaximumFractionDigits(4);
		    retval+=nf.format(pnb);
		    break;
		case TRANSFER_PACKETS:
		    retval+=_transferPackets;
		    break;
		case VALUE:
		    retval+=_value;
		    break;
		case INDEX:
		    retval+=_index;
		    break;
		case BLANK:
		    // do nothing
		    break;
		case NEWLINE:
		    retval+='\n';
		    break;
		default:
		    break;
		}

	    }
	    return (retval);
	}

    }

    class DataSet{
	public DataSet(){
	}

	public DataSet(String outFile){
	    this();
	}

	public void openSegment(String name,Vector segments, ProfileEvent pe){
	    EventSegment es=null;
	    try{
		for(int i=segments.size()-1;i>=0;i--){
		    // make sure that the last matching segment is closed
		    es=(EventSegment)segments.elementAt(i);
		    if(es.getName().equals(name))
			if(es.isOpen()){
			    es.close(pe.getTime());
			    break;
			}
		    
		}
	    }catch(NoSuchElementException e){
		_log4j.info("no segment; initializing");
	    }
	    // open a new segment
	    es=new EventSegment(name,pe);
	    segments.add(es);
	}
	public void closeSegment(String name,Vector segments,ProfileEvent pe){
	    EventSegment es=null;
	    try{
		for(int i=segments.size()-1;i>=0;i--){
		    // Should find an open segment
		    es=(EventSegment)segments.elementAt(i);
		    if(es.getName().equals(name))
			if(es.isOpen()){
			    es.close(pe.getTime());
			    break;
			}
		}
	    }catch(NoSuchElementException e){
		_log4j.error("segment received w/o start");
	    }
	}

	/** Override BufferedWriter's newLine() method, which
	    varies the newline character(s) according to 
	    platform. ConfigFiles should always use UNIX (\n)
	    newline.
	*/
	public void newLine(BufferedWriter bw)throws IOException{
	    //bw.newLine();
	    
	    bw.write('\n');
	}

	public void notify(ProfileAnalyzer.ProfileEvent pe){
	    // default do nothing
	    //_log4j.debug("DataSet.notify");
	}
	public void export(){
	    // default do nothing
	    //_log4j.debug("DataSet.export");
	}

    }
/*
==== event profiling ====
event                       startMsg                           EndMsg
===============================================================================================
connected                   start."nodeLinkConnect"            end."nodeLinkDisconnect"
resetWDT                    start."reset WDT"                  end."reset WDT"
nodeInfo                    start."getID"                      end."getID"
retrieveAndDistributeData   start."retrieveAndDistributeData"  end."retrieveAndDistributeData"
getPacketSet                start."getPacketSet"               end."getPacketSet"
downloadPacketSet           start."downloadPacketSet"          end."downloadPacketSet"
processPacketSet            end."processPacketSet"             end."processPacketSet"

*/

    class PlotProfile extends DataSet{
	Vector _connSegments=new Vector();
	Vector _rwdtSegments=new Vector();
	Vector _ninfoSegments=new Vector();
	Vector _rddataSegments=new Vector();
	Vector _gpacketsetSegments=new Vector();
	Vector _dpacketsetSegments=new Vector();
	Vector _ppacketsetSegments=new Vector();
	Vector _statSegments=new Vector();
	Vector _sessionSegments=new Vector();
	Vector _allSegments=new Vector();
	
	public final String CONN_START="nodeLinkConnect";
	public final String CONN_END="nodeLinkDisconnect";
	public final String CONN_NAME="connected";
	public final String RWDT_START="reset WDT";
	public final String RWDT_END="reset WDT";
	public final String RWDT_NAME="resetWDT";
	public final String NINFO_START="nodeInfo";
	public final String NINFO_END="nodeInfo";
	public final String NINFO_NAME="nodeInfo";
	public final String RDDATA_START="retrieveAndDistributeData";
	public final String RDDATA_END="retrieveAndDistributeData";
	public final String RDDATA_NAME="retrieveDistributeData";
	public final String GPACKETSET_START="getPacketSet";
	public final String GPACKETSET_END="getPacketSet";
	public final String GPACKETSET_NAME="getPacketSet";
	public final String DPACKETSET_START="downloadPacketSet";
	public final String DPACKETSET_END="downloadPacketSet";
	public final String DPACKETSET_NAME="downloadPacketSet";
	public final String PPACKETSET_START="processPacketSet";
	public final String PPACKETSET_END="processPacketSet";
	public final String PPACKETSET_NAME="processPacketSet";
	public final String SESSION_START="startSession";
	public final String SESSION_END="endSession";
	public final String SESSION_NAME="session";

	public PlotProfile(){
	    super();
	    _allSegments.add(_sessionSegments);
	    _allSegments.add(_rwdtSegments);
	    _allSegments.add(_ninfoSegments);
	    _allSegments.add(_rddataSegments);
	    _allSegments.add(_gpacketsetSegments);
	    _allSegments.add(_dpacketsetSegments);
	    _allSegments.add(_ppacketsetSegments);
	    _allSegments.add(_connSegments);
	    //_allSegments.add(_statSegments);
	}

	public PlotProfile(String outFile){
	    super(outFile);
	    _allSegments.add(_sessionSegments);
	    _allSegments.add(_rwdtSegments);
	    _allSegments.add(_ninfoSegments);
	    _allSegments.add(_rddataSegments);
	    _allSegments.add(_gpacketsetSegments);
	    _allSegments.add(_dpacketsetSegments);
	    _allSegments.add(_ppacketsetSegments);
	    _allSegments.add(_connSegments);
	    //_allSegments.add(_statSegments);
	}
	public Vector getSegments(){return _allSegments;}
	public void notify(ProfileAnalyzer.ProfileEvent pe){
	    //_log4j.debug("PlotProfile.notify");

	    // process event start messages
	    // note that it's possible for event
	    // segments to close on receipt of a
	    // start message.
	    if(pe.getType().equals(Profiler._start)){
		if(pe.getName().indexOf(CONN_START)>=0){
		    openSegment(CONN_NAME,_connSegments,pe);
		}
		if(pe.getName().indexOf(RWDT_START)>=0){
		    openSegment(RWDT_NAME,_rwdtSegments,pe);
		}
		if(pe.getName().indexOf(NINFO_START)>=0){
		    openSegment(NINFO_NAME,_ninfoSegments,pe);
		}
		if(pe.getName().indexOf(RDDATA_START)>=0){
		    openSegment(RDDATA_NAME,_rddataSegments,pe);
		}
		if(pe.getName().indexOf(GPACKETSET_START)>=0){
		    openSegment(GPACKETSET_NAME,_gpacketsetSegments,pe);
		    openSegment(GPACKETSET_NAME,_statSegments,pe);
		}
		if(pe.getName().indexOf(DPACKETSET_START)>=0){
		    openSegment(DPACKETSET_NAME,_dpacketsetSegments,pe);
		}
		if(pe.getName().indexOf(PPACKETSET_START)>=0){
		    openSegment(PPACKETSET_NAME,_ppacketsetSegments,pe);
		}

	    }

	    // process event end messages
	    // note that it's possible for event
	    // segments to open on receipt of a
	    // end message.
	    if(pe.getType().equals(Profiler._end)){
		if(pe.getName().indexOf(CONN_END)>=0){
		    closeSegment(CONN_NAME,_connSegments,pe);
		}
		if(pe.getName().indexOf(RWDT_END)>=0){
		    closeSegment(RWDT_NAME,_rwdtSegments,pe);
		}
		if(pe.getName().indexOf(RDDATA_END)>=0){
		    closeSegment(RDDATA_NAME,_rddataSegments,pe);
		}
		if(pe.getName().indexOf(NINFO_END)>=0){
		    closeSegment(NINFO_NAME,_ninfoSegments,pe);
		}
		if(pe.getName().indexOf(GPACKETSET_END)>=0){
		    closeSegment(GPACKETSET_NAME,_gpacketsetSegments,pe);
		    closeSegment(GPACKETSET_NAME,_statSegments,pe);
		}
		if(pe.getName().indexOf(DPACKETSET_END)>=0){
		    closeSegment(DPACKETSET_NAME,_dpacketsetSegments,pe);
		}
		if(pe.getName().indexOf(PPACKETSET_END)>=0){
		    closeSegment(PPACKETSET_NAME,_ppacketsetSegments,pe);
		}
	    }

	    // process info messages
	    // packet transfer statistics use info 
	    // packets. Info messages have a single
	    // time associated with them (start=end)
	    if(pe.getType().equals(Profiler._info)){
		//_log4j.debug("got info: message="+pe.getMessage()+" name="+pe.getName()+" deviceID="+pe.getSourceID());
		if(pe.getMessage().equals("stats")){
		    EventSegment es=(EventSegment)_statSegments.lastElement();
		    es.addStats(pe.getStats());
		    //_log4j.debug("added stats:"+pe.getStats()+"  transferBytes:"+es.getTotalPacketBytes());
		}
		if(pe.getName().equals(SESSION_START)){
		    //_log4j.debug("open session segment "+(SESSION_NAME+pe.getSourceID()));
		    openSegment((SESSION_NAME+pe.getSourceID()),_sessionSegments,pe);
		}
		if(pe.getName().equals(SESSION_END)){
		    //_log4j.debug("close session segment "+pe.getTime()+" "+(SESSION_NAME+pe.getSourceID()));
		    closeSegment((SESSION_NAME+pe.getSourceID()),_sessionSegments,pe);
		}
	    }
	}

	public void writeRecord(BufferedWriter bw,String x){
	    try{
		bw.write(x);
		newLine(bw);
		bw.flush();
		return;
	    }catch(IOException i){
		i.printStackTrace();
	    }

	}

	public void exportCSV(){
	    BufferedWriter bwPlotProfile= null;
	    try{
		bwPlotProfile = new BufferedWriter(new FileWriter(getPath("pp.csv")));

		String header="start,end,duration,event,value,index";
		//writeRecord(bwPlotProfile,header);

		int value=0;
		int[] components={0,0,0,0,0,0};

		for(Enumeration e=_allSegments.elements();e.hasMoreElements();){
		    Vector v=(Vector)e.nextElement();
		    value++;
		    int index=0;
		    
		    for(Enumeration ve=v.elements();ve.hasMoreElements();){
			EventSegment es=(EventSegment)ve.nextElement();

			es.setValue(value);
			es.setIndex(index++);

			components[0]=es.START;
			components[1]=es.END;
			components[2]=es.DURATION;
			components[3]=es.NAME;
			components[4]=es.VALUE;
			components[5]=es.INDEX;
			writeRecord(bwPlotProfile,es.export(components,","));

		    }
		}
		
		bwPlotProfile.close();
	    }catch(IOException i){
		i.printStackTrace();
	    }
	    
	    BufferedWriter bwTransferStats=null;

	    try{
		bwTransferStats = new BufferedWriter(new FileWriter(getPath("ts.csv")));

		String header="deviceID,transferBytes,%nominalBitRate,packetBytes";
		//writeRecord(bwTransferStats,header);{

		int value=0;
		//for(Enumeration e=_statSegments.elements();e.hasMoreElements();){
		// Vector v=(Vector)e.nextElement();
		Vector v=_statSegments;
		    value++;
		    int index=0;
		    int[] components={0,0,0,0};
		    for(Enumeration ve=v.elements();ve.hasMoreElements();){
			EventSegment es=(EventSegment)ve.nextElement();

			if(es.getSourceID()<=0)
			    continue;
			es.setIndex(index++);
			es.setValue(value);

			components[0]=es.DEVICEID;
			components[1]=es.TRANSFER_BYTES;
			components[2]=es.PERCENT_NOMINAL_BITRATE;
			components[3]=es.EFFECTIVE_BITRATE;
			writeRecord(bwTransferStats,es.export(components,","));

			// PacketBytes are multi-valued, so we'll get the
			// PacketStats from the EventSegment and build the records
			// instead of using EventSegment.export()
			Vector stats=es.getStats();
			for(Enumeration se=stats.elements();se.hasMoreElements();){
			    PacketStats ps=(PacketStats)se.nextElement();
			    writeRecord(bwTransferStats,(ps.getSourceID()+",,,,"+ps.getExportSize()));
			}
		    }
		    //}
		    bwTransferStats.close();
	    }catch(IOException i){
		i.printStackTrace();
	    }
	}

	public void export(){
	    exportCSV();
	}

    }
    class EventStatSet extends StatSet{
	String _name="";
	long _firstTime=Long.MAX_VALUE;
	long _lastTime=Long.MIN_VALUE;
	String _firstDate="";
	String _lastDate="";
	long _eventType=-1L;
	long _sourceID=-1L;
	long _parentID=-1L;
	//Hashset _cStatus=new Hashset();

	long _totalDurationMsec=0L;
	double _averageDurationMsec=0.0;

	public EventStatSet(){
	    super();
	}

	public EventStatSet(String outFile){
	    super(outFile);
	}

	public void process(Object value){
	    EventSegment es=(EventSegment)value;

	    //_log4j.debug("EventStatSet.process");

	    if(_name.equals(""))
		_name=es.getName();
	    if(_sourceID<0)
		_sourceID=es.getSourceID();
	    if(_eventType<0)
		_eventType=es.getValue();


	    long t=es.getStart();
	    if(t<_firstTime){
		_firstTime=t;
		_firstDate=es.getStartDate();
	    }else
	    if(t>_lastTime){
		_lastTime=t;
		_lastDate=es.getStartDate();
	    }

	    long duration=es.getDuration();

	    if(duration>_max)
		_max=duration;
	    if(duration<_min)
		_min=duration;
	    
	    _totalDurationMsec+=duration;
	    _averageDurationMsec=_totalDurationMsec/(++_count);
	    _average=_averageDurationMsec;
	    _sum+=_totalDurationMsec;

	}
	public String getName(){return _name;}
	public void setName(String name){_name=name;}
	public long eventType(){return _eventType;}
	public long sourceID(){return _sourceID;}
	public long parentID(){return _parentID;}
	public long durationTotal(){return _totalDurationMsec;}
	public double durationAverage(){return _averageDurationMsec;}

	public String header(){
	    String s="name"+this._delimiter+
		"sourceID"+this._delimiter+
		"eventType"+this._delimiter+
		"firstDate"+this._delimiter+
		"lastDate"+this._delimiter+
		"count"+this._delimiter+
		"min"+this._delimiter+
		"max"+this._delimiter+
		"averageDuration"+this._delimiter+
		"totalDuration";
	    return s;
	}

	public String toString(){
	    String s=_name+this._delimiter+
		_sourceID+this._delimiter+
		_eventType+this._delimiter+
		_firstDate+this._delimiter+
		_lastDate+this._delimiter+
		_count+this._delimiter+
		_count+this._delimiter+
		_min+this._delimiter+
		_max+this._delimiter+
		_averageDurationMsec+this._delimiter+
		_totalDurationMsec;
	    return s;
	}
    }

    // call export() first
    public void exportStats(Vector segments, Vector statSets){
	for(Enumeration e=segments.elements();e.hasMoreElements();){
	    Vector v=(Vector)e.nextElement();
	    for(Enumeration ve=v.elements();ve.hasMoreElements();){
		EventSegment es=(EventSegment)ve.nextElement();
		EventStatSet ss =null;
		//_log4j.debug("looking for match "+es.getName()+" ("+es.getValue()+")");
		for(Enumeration s=statSets.elements();s.hasMoreElements();){
		    EventStatSet x = (EventStatSet)s.nextElement();
		    if(x.getName()==es.getName()){
			//_log4j.debug("match FOUND "+es.getName()+" ("+es.getValue()+")");
			x.process(es);
			ss=x;
			break;
		    }
		}

		if(ss==null){
		    //_log4j.debug("match NOT FOUND "+es.getName()+" ("+es.getValue()+")");
		    EventStatSet y=new EventStatSet();
		    y.process(es);
		    statSets.add(y);
		    _eventTypeSet.add(new Long(es.getValue()));
		    _eventNameSet.add(es.getName());
		}
		
	    }
	}

    }

    public String getTableHeader(String[] eventNames){
	String header="";
	for(int i=0;i<Arrays.asList(eventNames).size();i++){
	    // add delimiter to all (even 0) so that cols
	    // match up
	    header+=this._delimiter+eventNames[i];
	}    
	return header;
    }

    public String getTableTotals(String[] totals){
	String retval="";
	for(int i=0;i<Arrays.asList(totals).size();i++){
	    // add delimiter to all (even 0) so that cols
	    // match up
	    retval+=this._delimiter+totals[i];
	}    
	return retval;
    }
    public String getTableTotals(Long[] totals){
	String retval="";
	for(int i=0;i<Arrays.asList(totals).size();i++){
	    // add delimiter to all (even 0) so that cols
	    // match up
	    retval+=this._delimiter+totals[i].longValue();
	}    
	return retval;
    }
    public String getTableTotals(Double[] totals){
	String retval="";
	for(int i=0;i<Arrays.asList(totals).size();i++){
	    // add delimiter to all (even 0) so that cols
	    // match up
	    retval+=this._delimiter+totals[i].doubleValue();
	}    
	return retval;
    }

    public void exportTable(Vector statSets,boolean showHeader){
	
	String[] eventNames=new String[_eventNameSet.size()];
	int idx=0;
	for(Iterator i=_eventNameSet.iterator();i.hasNext();){
	    eventNames[idx++]=(String)i.next();

	}
	/*
	Long[] eventTypes=new Long[_eventTypeSet.size()];
	idx=0;
	for(Iterator i=_eventTypeSet.iterator();i.hasNext();){
	    eventTypes[idx++]=(Long)i.next();
	}
	*/
	String[] statNames={"count","min","max","mean","sum"};

	Arrays.sort(eventNames);
	//Arrays.sort(eventTypes);
	int rows=Array.getLength(statNames);
	int cols=Array.getLength(eventNames);

	Long[][] cellsTotal=new Long[rows][cols];
	Double[][] cellsAverage=new Double[rows][cols];
	//Long[] totalSum=new Long[cols];
	//Double[] averageSum=new Double[cols];

	for(int i=0;i<rows;i++){
	    for(int j=0;j<cols;j++){
		//_log4j.debug("id "+id+" type "+type);
		String name=eventNames[j];
		//long type=eventTypes[i].longValue();
		for(Enumeration e=statSets.elements();e.hasMoreElements();){
		    EventStatSet ss = (EventStatSet)e.nextElement();
		    if(ss.getName().equals(name)){
			switch(i){
			case 0:
			    cellsAverage[i][j]=new Double(ss.count());
			    break;
			case 1:
			    cellsAverage[i][j]=new Double(ss.min());
			    break;
			case 2:
			    cellsAverage[i][j]=new Double(ss.max());
			    break;
			case 3:
			    cellsAverage[i][j]=new Double(ss.average());
			    break;
			case 4:
			    cellsAverage[i][j]=new Double(ss.sum());
			    break;
			default:
			    break;
			}
			//cellsTotal[i][j]=new Long(ss.durationTotal());
			//cellsAverage[i][j]=new Double(ss.durationAverage());
			/*
			if(totalSum[j]==null)
			    totalSum[j]=new Long(cellsTotal[i][j].longValue());
			else
			    totalSum[j]=new Long(totalSum[j].longValue()+cellsTotal[i][j].longValue());

			if(averageSum[j]==null)
			    averageSum[j]=new Double(cellsAverage[i][j].doubleValue());
			else
			    averageSum[j]=new Double(averageSum[j].doubleValue()+cellsAverage[i][j].doubleValue());
			*/
		    }
		}
	    }
	}

	System.out.println("\nTotal Duration by eventType");
	System.out.println(getTableHeader(eventNames));
	for(int i=0;i<rows;i++){
	    System.out.print(statNames[i]);
	    for(int j=0;j<cols;j++){
		System.out.print(this._delimiter);
		if(cellsTotal[i][j]!=null)
		System.out.print(cellsTotal[i][j]);
	    }
	    System.out.print("\n");
	}
	//System.out.println(getTableTotals(totalSum));

	System.out.println("\nAverage Duration by eventType");
	System.out.println(getTableHeader(eventNames));
	for(int i=0;i<rows;i++){
	    System.out.print(statNames[i]);
	    for(int j=0;j<cols;j++){
		System.out.print(this._delimiter);
		if(cellsAverage[i][j]!=null)
		System.out.print(cellsAverage[i][j]);
	    }
	    System.out.print("\n");
	}
	//System.out.println(getTableTotals(averageSum));
    }

    public static void processArgs(ProfileAnalyzer pa, String args[]){
	for(int i=0;i<args.length;i++){
	    if(args[i].equals("-l")){
		pa.setEventFormat(ProfileAnalyzer.FORMAT_LEGACY);
	    }else
	    if(args[i].equals("-d")){
		pa.setOutputDirectory(args[i+1]);
		i++;
	    }else{
		pa.setInputFile(args[i]);
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
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);
	/*
	System.out.println(args.length+" args:");
	for(int i=0;i<args.length;i++)
	    System.out.println(args[i]);
	*/
	if(args.length<1){
	    exitError("Missing file name",1);
	}
	ProfileAnalyzer pe=new ProfileAnalyzer();
	ProfileAnalyzer.processArgs(pe,args);
	pe.analyze();
	
    }

}

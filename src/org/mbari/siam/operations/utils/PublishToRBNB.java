// Copyright MBARI 2011
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DateFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.lang.SecurityException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.core.SiamProperties;
import org.mbari.siam.utils.TimeUtils;
import com.rbnb.sapi.*;

import org.mbari.siam.core.NodeService;
import org.mbari.siam.utils.PrintfFormat;

public class PublishToRBNB extends NodeUtility
{
    protected static Logger _log4j = Logger.getLogger(PublishToRBNB.class);
    SimpleDateFormat	_dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    protected Calendar	_calendar = Calendar.getInstance();

    protected Node	_node;
    protected SiamProperties _props;
    protected PubProperties _globalProps = new PubProperties();
    protected long	_startTime=0, _runDuration=0, _pubSize=3600000;
    protected String	_rbnbServer="localhost", _propertyFile, _home = "/home/ops/siam";
    protected long[]	_deviceIDs;
    protected PacketFilter[] filter = new PacketFilter[1];
    protected long[]     _excludeIDs;
    protected Vector	_instruments = new Vector();
    protected Timer	_timer;

    /** Constructor */
    public PublishToRBNB() {
	filter[0] = new PacketSubsampler(0, DevicePacket.SENSORDATA_FLAG);
    }

    /** Process the data from the SIAM Node, publishing to the RBNB Host	*/
    public void processNode(Node node) throws Exception
    {
	Iterator	it;
	long		now = System.currentTimeMillis();
	InstrumentDesc	idesc;

	_node = node;
	getInstruments(node);

	if (_startTime > 0) {
	    for (it = _instruments.iterator(); it.hasNext(); ) {
		((InstrumentDesc)(it.next())).setNextSampleTime(_startTime);
	    }
	}
	else {
	    Sink timeSink = new Sink();
	    long lastTime;

	    try {
		timeSink.OpenRBNBConnection(_rbnbServer, "TimeGetter");
	    } catch (Exception e) {
		_log4j.error("Can't connect to RBNB server at " + _rbnbServer);
		throw e;
	    }
	    for (it = _instruments.iterator(); it.hasNext(); )	{
		idesc = (InstrumentDesc)(it.next());
		lastTime = idesc.getLastRBNBTime(timeSink);
		idesc.setNextSampleTime((lastTime==0) ? now : lastTime);
	    }

	    timeSink.CloseRBNBConnection();
	}

	if (_runDuration > 0) {
	    //Run it once, for _runDuration milliseconds
	    updateInstruments(_runDuration);
	    detachInstruments();
	}
	else {
	    //Update each instrument to present time
	    for (it = _instruments.iterator(); it.hasNext(); ) {
		idesc = (InstrumentDesc)(it.next());
		idesc.update(now - idesc.getNextSampleTime());
	    }
	    _timer = new Timer();
	    Runtime.getRuntime().addShutdownHook(new ShutdownThread());
	    _timer.scheduleAtFixedRate(new UpdateTask(), 0, _pubSize);
	}
    }


    /** Get the SIAM Instruments to process */
    public void getInstruments(Node node) throws Exception
    {
	InstrumentDesc idesc;

	if ((_deviceIDs == null) || (_deviceIDs.length == 0)) {
	    Device[] devices = node.getDevices();
	    for (int i = 0; i < devices.length; i++) {
		if (devices[i] instanceof Instrument){
		    try {
			idesc = new InstrumentDesc((Instrument)(devices[i]));
			if (!isExcluded(idesc._isiID)) {
			    _instruments.add(idesc);
			}
		    } catch (SAPIException se) {
			_log4j.error("SAPIException - No RBNB Server? " + se);
			throw se;
		    } catch (Exception e) {
			_log4j.error("Can't process device: " + e);
		    }
		}
		else {
		    _log4j.info("Skipping device " + devices[i].getId() +
				" -- not an Instrument");
		}
	    }
	}
	else {
	    for (int i = 0; i < _deviceIDs.length; i++) {
		try {
		    _instruments.add(new InstrumentDesc(_node.getDevice(_deviceIDs[i])));
		} catch (Exception e) {
		    _log4j.error("Can't process device " + _deviceIDs[i] + ": " + e);
		}
		
	    }
	}
    }

    /** Call detach() for each InstrumentDesc */
    protected void detachInstruments()
    {
	for (Iterator it = _instruments.iterator(); it.hasNext(); ) {
	    ((InstrumentDesc)(it.next())).detach();
	}
    }

    /** Call update() for each InstrumentDesc */
    protected void updateInstruments(long duration) throws Exception
    {
	for (Iterator it = _instruments.iterator(); it.hasNext(); ) {
	    ((InstrumentDesc)(it.next())).update(duration);
	}
    }

    /** Return true if id is in _excludeIDs[] */
    protected boolean isExcluded(long id)
    {
	if (_excludeIDs != null) {
	    for (int i = 0; i < _excludeIDs.length; i++) {
		if (id == _excludeIDs[i]) {
		    return(true);
		}
	    }
	}

	return(false);
    }

    /** Find, open, and read the Properties file */
    public SiamProperties openPropertiesFile(String[] args)
	throws FileNotFoundException, IOException, SecurityException
    {
	try {
	    _home = System.getenv("SIAM_HOME");
	} catch (Exception e) {
	    _log4j.info("Can't find environ variable SIAM_HOME");
	}
	_propertyFile = _home + "/properties/PublishToRBNB.properties";

	for (int i = 1; i < args.length; i++)
	{
	    if (args[i].equals("-propertyFile")) {
		i++;
		_propertyFile = args[i];
	    }
	}

        FileInputStream in = 
            new FileInputStream(_propertyFile);

	_props = new SiamProperties();
	_props.load(in);
	in.close();
	return(_props);
    }

    /** Read the properties from the Properties file into the _globalProps object */
    public void readGlobalProperties()
    {
	_globalProps.readProperties(_props, 0);
    }

    /** Kludge that calls _props.getIntegerArrayProperty, and copies to long[] array.
	@return null if property not found
    */
    protected long[] getLongArrayProperty(SiamProperties props, String name)
    {
	try {
	    int[] itmp = props.getIntegerArrayProperty(name);
	    long[] lrtn = new long[itmp.length];
	    for (int i = 0; i < itmp.length; i++) {
		lrtn[i] = itmp[i];
	    }
	    return(lrtn);
	} catch(Exception e) {
	    return(null);
	}
    }

    /** Process the command-line arguments */
    public boolean processArgs(String[] args)
    {
	long	deviceID;
	int	index;

	if ((args.length < 1) || args[0].startsWith("-")) {
	    return(false);
	}

	// Get the node URL string representation
	_nodeURL = getNodeURL(args[0]);

	try {
	    _home = System.getenv("SIAM_HOME");
	} catch (Exception e) {
	    _log4j.info("Can't find environ variable SIAM_HOME");
	}
	_propertyFile = _home + "/properties/PublishToRBNB.properties";

	for (int i = 1; i < args.length; i++)
	{
	    if ((args[i].equals("-start")) || (args[i].equals("-startl"))) {
		// Parse lower end of time window
		try {
		    i++;
		    _startTime = TimeUtils.parseDateTime(args[i]);
		    if (args[i-1].equals("-start")) {
			_startTime += utcOffset(_calendar);
		    }
		} catch (ParseException e) {
		    _log4j.error("Invalid timestring: " + args[i]);
		    return(false);
		}
	    }
	    else if (args[i].equals("-rbnbServer")) {
		i++;
		_rbnbServer = args[i];
	    }
	    else if (args[i].equals("-propertyFile")) {
		i++;
		_propertyFile = args[i];
	    }
	    else if (args[i].equals("-openMode")) {
		i++;
		_globalProps._openMode = args[i];
	    }
	    else if (args[i].equals("-duration")) {
		try {
		    i++;
		    _runDuration = getDuration(args[i]);
		} catch (Exception e) {
		    _log4j.error("Invalid -duration: " + args[i]);
		    return(false);
		}
	    }
	    else if (args[i].equals("-pubSize")) {
		try {
		    i++;
		    _pubSize = getDuration(args[i]);
		} catch (Exception e) {
		    _log4j.error("Invalid -pubSize: " + args[i]);
		    return(false);
		}
	    }
	    else if (args[i].equals("-cacheSize")) {
		try {
		    i++;
		    _globalProps._cacheSize = Integer.parseInt(args[i]);
		} catch (Exception e) {
		    _log4j.error("Invalid cacheSize: " + args[i]);
		    return(false);
		}
	    }
	    else if (args[i].equals("-archiveSize")) {
		try {
		    i++;
		    _globalProps._archiveSize = Integer.parseInt(args[i]);
		} catch (Exception e) {
		    _log4j.error("Invalid archiveSize: " + args[i]);
		    return(false);
		}
	    }
	    else if (args[i].equals("-excludeRecordType")) {
		try {
		    i++;
		    _globalProps._excludeTypes = addOneLong(_globalProps._excludeTypes, args[i]);
		} catch (Exception e) {
		    _log4j.error("Invalid -excludeRecordType: " + args[i]);
		    return(false);
		}
	    }
	    else if (args[i].equals("-excludeID")) {
		try {
		    i++;
		    _excludeIDs = addOneLong(_excludeIDs, args[i]);
		} catch (Exception e) {
		    _log4j.error("Invalid -excludeID: " + args[i]);
		    return(false);
		}
	    }
	    else if (args[i].charAt(0) == '-') {
		_log4j.error("Unknown option:  " + args[i]);
	    }
	    else {
		//Remainder of args assumed to be deviceIDs
		_deviceIDs = addOneLong(_deviceIDs, args[i]);
	    }
	}

	return(true);
    }

    /** Routine to add one long value to a long[] array */
    protected long[] addOneLong(long[] array, String newStr)
	throws NumberFormatException
    {
	int len = (array == null) ? 0 : array.length;
	long val = Long.parseLong(newStr);
	long[] tmpArray = new long[len+1];

	if (array != null) {
	    //If duplicate, don't add, just return
	    for (int i = 0; i < len; i++) {
		if (array[i] == val) {
		    return(array);
		}
	    }
	    //Otherwise, copy into the bigger array
	    System.arraycopy(array, 0, tmpArray, 0, len);
	}

	tmpArray[len] = val;
	return(tmpArray);
    }

    /** Get command-line arg expressed a duration, as nnn[s|m|h|d]
     *  [p] '<nnn>' or '<nnn>s' means seconds
     *  [p] '<nnn>m' means minutes
     *  [p] '<nnn>h' means hours
     *  [p] '<nnn>d' means days
     *  @return milliseconds
     */
    protected long getDuration(String s)
	throws ParseException, NumberFormatException
    {
	String	numStr = s;
	int	lastIndex = s.length()-1;
	long	val;
	char	endChar = s.charAt(lastIndex);

	if (Character.isLetter(endChar)) {
	    numStr = s.substring(0, lastIndex);
	}
	else
	    endChar = 's';

	val = Long.parseLong(numStr);
	switch (endChar) {
	  case 's':
	      return(val*1000);

	  case 'm':
	      return(val*60000);

	  case 'h':
	      return(val*3600000);

	  case 'd':
	      return(val*86400000);
	}

	throw new ParseException("Invalid duration", lastIndex);
    }

    /** Print operational parameters to System.out */
    public void printParms()
    {
	System.out.println("_nodeURL = " + _nodeURL);
	System.out.println("_rbnbServer = " + _rbnbServer);
	System.out.println("_startTime = " +
			   _dateFormatter.format(new Date(_startTime)) + " UTC");
	System.out.println("_runDuration = " + _runDuration/1000 + " seconds");
	System.out.println("_pubSize = " + _pubSize/1000 + " seconds");
	System.out.println("_propertyFile = " + _propertyFile);

	if (_deviceIDs == null) {
	    System.out.println("No deviceIDs specified");
	}
	else {
	    System.out.print("Device IDs: ");
	    for (int i = 0; i < _deviceIDs.length; i++) {
		System.out.print(" " + _deviceIDs[i]);
	    }
	    System.out.println("");
	}

	_globalProps.print();
    }

    /** Print Usage message */
    public void printUsage() {
	System.err.println("");
	System.err.println("usage: publishToRBNB nodeURL [options] [deviceID ... deviceID]");
	System.err.println("       if no deviceID's specified, publish all Devices on nodeURL");
	System.err.println("");
	System.err.println("Options:");
	System.err.println(" -h                  Print this help message");
	System.err.println(" -start <time>       Start at specified time, expressed in UTC");
	System.err.println(" -startl <time>      Same as -start, but time expressed in local time");
	System.err.println(" 			 If no -start{l} specified, start from end of RBNB");
	System.err.println(" -rbnbServer <host>  Publish to specified RBNB host");
	System.err.println(" -duration <n>       Run once to publish <n> secs/mins/hrs/days (s/m/h/d)");
	System.err.println(" 			 If -duration not specified, app runs forever");
	System.err.println(" -pubSize <amount>   Frequency to publish, secs/mins/hrs/days (s/m/h/d)");
	System.err.println(" -openMode <mode>    Open mode for RBNB (\"append\" or \"create\", default append");
	System.err.println(" -cacheSize <n>      RBNB cacheSize (default 20)");
	System.err.println(" -archiveSize <n>    RBNB archiveSize (default 20000)");
	System.err.println(" -excludeID <ID>     Instrument ID to exclude for publishing. Can have multiple");
	System.err.println(" -excludeRecordType <n> RecordType to exclude for publishing. Can have multiple");
	System.err.println(" -propertyFile <name> Name of property file");
	System.err.println("       Note most of the above can be set in the property file, either globally,"); 
	System.err.println("       or for specific device by appending '.<deviceID>' to the property name");
	System.err.println("");
    }

    /** Compute offset of local timezone from UTC */
    static int utcOffset(Calendar calendar) {
	return calendar.get(Calendar.ZONE_OFFSET) + 
	    calendar.get(Calendar.DST_OFFSET);
    }

    /** Main() routine of application */
    public static void main(String[] args)
    {
	PublishToRBNB pub = new PublishToRBNB();

	try {
	    pub.openPropertiesFile(args);
	} catch (Exception e) {
	    _log4j.warn("Could not read properties file: " + e);
	}

	pub.readGlobalProperties();

	if (!pub.processArgs(args)) {
	    pub.printUsage();
	    return;
	}

	if (_log4j.isDebugEnabled())
	    pub.printParms();

	try {
	    pub.run();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }


    /** Class to encapsulate properties, either globally or particular to one device */
    protected class PubProperties
    {
	protected int	_cacheSize=20, _archiveSize=20000;
	protected String _openMode="append";
	protected long[] _excludeTypes;

	protected void copy(PubProperties src)
	{
	    _cacheSize = src._cacheSize;
	    _archiveSize = src._archiveSize;
	    _openMode = src._openMode;
	    if (_excludeTypes != null) {
		_excludeTypes = new long[src._excludeTypes.length];
		for (int i = 0; i < src._excludeTypes.length; i++)
		    _excludeTypes[i] = src._excludeTypes[i];
	    }
	}

	protected void readProperties(SiamProperties props, long isiID)
	{
	    String suffix = (isiID > 0) ? ("." + Long.toString(isiID)) : "";

	    _cacheSize = props.getIntegerProperty("cacheSize" + suffix, _cacheSize);
	    _archiveSize = props.getIntegerProperty("archiveSize" + suffix, _archiveSize);
	    _openMode = props.getProperty("openMode" + suffix, _openMode);
	    _excludeTypes = getLongArrayProperty(props, "exclude" + suffix);
	}

	protected void print()
	{
	    System.out.println("_cacheSize = " + _cacheSize);
	    System.out.println("_archiveSize = " + _archiveSize);
	    System.out.println("_openMode = " + _openMode);

	    if (_excludeTypes == null) {
		System.out.println("No exclude specified");
	    }
	    else {
		System.out.print("Excluded record types: ");
		for (int i = 0; i < _excludeTypes.length; i++) {
		    System.out.print(" " + _excludeTypes[i]);
		}
		System.out.println("");
	    }
	}
    }

    /** TimerTask to update all instruments */
    protected class UpdateTask extends TimerTask
    {
	public void run()
	{
	    if (_log4j.isDebugEnabled())
		_log4j.debug("Running UpdateTask");

	    try {
		updateInstruments(4*_pubSize);
	    } catch (Exception e) {
	    }
	}
    }

    /** Thread passed to addShutdownHook to shutdown the application */
    protected class ShutdownThread extends Thread
    {
	public void run()
	{
	    _log4j.info("Running ShutdownThread");
	    _timer.cancel();
	    detachInstruments();
	}
    }

    /** Class to encapsulate one Instrument */
    protected class InstrumentDesc
    {
	long		_isiID;
	Instrument	_instrument;
	PubProperties	_pubProps;
	String		_srcName;
	Source		_dtSource;
	Vector		_rcdDescs;
	PacketParser	_parser;
	long		_nextSampleTime = 0;

	/** Constructor for SIAM Instrument */
	protected InstrumentDesc(Device device)
	    throws NotSupportedException, RemoteException, SAPIException
	{
	    if (!(device instanceof Instrument))
		throw new NotSupportedException("Not an Instrument");

	    _instrument = (Instrument)device;
	    _isiID = _instrument.getId();
	    _pubProps = new PubProperties();
	    _pubProps.copy(_globalProps);
	    _pubProps.readProperties(_props, _isiID);
	    _rcdDescs = new Vector();

	    if ((_parser = _instrument.getParser()) == null)
		throw new NotSupportedException("No packet parser for device " + _isiID);

	    try {
		_instrument.setProperty("packetSetSize=1000".getBytes(), null);
	    } catch (Exception e) {
	    }

	    _dtSource = new Source(_pubProps._cacheSize, _pubProps._openMode,
				   _pubProps._archiveSize);
	    _dtSource.OpenRBNBConnection(_rbnbServer, getSourceName());
	}

	/** Detach from RBNB server */
	protected void detach()
	{
	    _dtSource.Detach();
	}

	/** Set the time for the next sample to be fetched */
	protected void setNextSampleTime(long sampleTime)
	{
	    _nextSampleTime = sampleTime;
	}

	/** Get the time for the next sample to be fetched */
	protected long getNextSampleTime()
	{
	    return(_nextSampleTime);
	}

	/** Update RBNB server with Instrument data, starting at _nextTime, for duration milliseconds */
	protected void update(long duration) throws Exception
	{
	    long	last, endTime, now = System.currentTimeMillis();
	    Vector	pkts;
	    DevicePacketSet pktSet;
	    DevicePacket pkt;
	    RecordDesc	rcdDesc;
	    Iterator	it;
	    PacketParser.Field[] fields;
	    int		numPublished = 0;
	    long	firstTime=-1, lastTime=-1;

	    _log4j.info("Processing device " + _isiID + " " + getSourceName());
	    
	    endTime = _nextSampleTime + duration;
	    if (endTime > now)
		endTime = now;

	    while(_nextSampleTime < endTime)
	    {
		last = _nextSampleTime + _pubSize - 1;

		// If we're within 1/2 pubSize of the end time, just publish all
		if (last > (endTime - _pubSize/2)) {
		    last = endTime;
		}

		if (_log4j.isDebugEnabled()) {
		    _log4j.debug("Requesting data for ID " + _isiID + " from " +
				 _dateFormatter.format(new Date(_nextSampleTime)) + " to " +
				 _dateFormatter.format(new Date(last)));
		}

		for (boolean complete=false; !complete; )
		{
		    try {
			pktSet = _instrument.getPackets(_nextSampleTime, last, filter, false);
			pkts = pktSet._packets;
			complete = pktSet.complete();
			_log4j.debug("Got " + pkts.size() + " packets");

			for (it = pkts.iterator(); it.hasNext(); ) {
			    pkt = (DevicePacket)(it.next());
			    if (pkt.systemTime() >= _nextSampleTime) {
				_nextSampleTime = pkt.systemTime() + 1;
			    }
			    if (pkt instanceof SensorDataPacket) {
				rcdDesc = getRcdDesc(pkt, _rcdDescs);
				if (rcdDesc != null) {
				    rcdDesc.putPacket(pkt);
				    lastTime = pkt.systemTime();
				    if (firstTime < 0) {
					firstTime = lastTime;
				    }
				}
			    }
			}
		    } catch (NoDataException e) {
			_log4j.debug("No packets");
			complete = true;
			_nextSampleTime = last + 1;
		    }
		}

		try { 
		    for (it = _rcdDescs.iterator(); it.hasNext(); ) {
			rcdDesc = (RecordDesc)(it.next());
			numPublished += rcdDesc.publish(_dtSource, _parser);
			rcdDesc.clear();
		    }
		} catch (Exception e) {
		    _log4j.error("Could not publish to RBNB server: " + e);
		}
	    }

	    if (numPublished == 0) {
		_log4j.info("No packets for ID " + _isiID);
	    }
	    else {
		_log4j.info("Published " + numPublished + " packets for ID " + _isiID +
			    " from " + _dateFormatter.format(new Date(firstTime)) + " to "
			    + _dateFormatter.format(new Date(lastTime)) );
	    }
	}

	protected String getSourceName() throws RemoteException
	{
	    if (_srcName == null) {
		_srcName = (new String(_instrument.getName())).replace(' ', '_') + "-" + _isiID;
	    }
	    return(_srcName);
	}

	protected RecordDesc getRcdDesc(DevicePacket pkt, Vector rcdDescs)
	{
	    RecordDesc rcdDesc;
	    long       rcdType = pkt.getRecordType();
	
	    if (_pubProps._excludeTypes != null) {
		for (int i = 0; i < _pubProps._excludeTypes.length; i++) {
		    if (rcdType == _pubProps._excludeTypes[i]) {
			return(null);
		    }
		}
	    }

	    for (Iterator it = rcdDescs.iterator(); it.hasNext(); ) {
		rcdDesc = (RecordDesc)(it.next());
		if (rcdDesc.equals(rcdType)) {
		    return(rcdDesc);
		}
	    }

	    // No RecordDesc for this recordType yet.  Build a new one
	    rcdDesc = new RecordDesc(_isiID, rcdType);
	    try {
		ChannelMap chanMap = rcdDesc.createChannelMap(_parser.parseFields(pkt));

		if (chanMap.NumberOfChannels() == 0) {
		    return(null);
		}

		_dtSource.Register(chanMap);

	    } catch (Exception e) {
		_log4j.warn("Exception creating ChannelMap for ID " + _isiID + ": " + e);
		return(null);
	    }

	    rcdDescs.add(rcdDesc);
	    return(rcdDesc);
	}

	/** Get the time of the last data point in the RBNB buffer */
	protected long getLastRBNBTime(Sink sink)
	{
	    ChannelMap	reqMap = new ChannelMap();
	    double maxTime = 0.0;

	    try {
		reqMap.Add(getSourceName() + "/*");
		sink.Request(reqMap, 0.0, 0, "newest");
		ChannelMap resultMap = sink.Fetch(10000);

		for (int i = 0; i < resultMap.NumberOfChannels(); i++)
		{
		    double[] times = resultMap.GetTimes(i);
		    if (_log4j.isDebugEnabled()) {
			System.out.println("Channel " + i + " name " + resultMap.GetName(i) +
					   " sample time " + 
					   _dateFormatter.format(new Date((long)(times[0]*1000.))) + " UTC");
		    }
		    if (!resultMap.GetName(i).endsWith("metadata"))
			if (times[0] > maxTime)
			    maxTime = times[0];
		}
	    } catch (Exception e) {
		_log4j.error("Error getting last sample time: " + e);
		e.printStackTrace();
		return(0);
	    }
	    return((long)(maxTime*1000.));
	}
    } /* class InstrumentDesc */

    /** Class to encapsulate one RecordType from one Instrument */
    protected class RecordDesc
    {
	long		_recordType, _isiID;
	Vector		_pkts = new Vector();
	int[]		_chanIndex;
	int		_numFields;
	ChannelMap	_chanMap;

	protected RecordDesc(long isiID, long recordType) {
	    _isiID = isiID;
	    _recordType = recordType;
	}

	protected void putPacket(DevicePacket pkt) {
	    _pkts.add(pkt);
	}

	protected void clear() {
	    _pkts.clear();
	}

	protected boolean equals(long rcdType) {
	    return(rcdType == _recordType);
	}

	/** Parse all packets in the _pkts Vector, and publish them to RBNB server */
	protected int publish(Source dtSource, PacketParser parser)
	    throws NotSupportedException, ParseException, SAPIException
	{
	    _log4j.debug("Publishing " + _pkts.size() + " packets for ID " + _isiID + 
			 " recordType " + _recordType);

	    if (_pkts.size() == 0) {
		return(0);
	    }

	    double[][]	values = new double[_numFields][_pkts.size()];
	    double[]	times = new double[_pkts.size()];
	    int		i, j;
	    PacketParser.Field[] fields = null;
	    DevicePacket pkt;
	    Object	obj;

	    for (i = 0; i < _pkts.size(); i++) {
		pkt = (DevicePacket)(_pkts.elementAt(i));
		times[i] = (double)(pkt.systemTime())/1000.;
		fields = parser.parseFields(pkt);

		for (j = 0; j < fields.length; j++) {
		    obj = fields[j].getValue();
		    if (obj instanceof Number) {
			values[j][i] = ((Number)obj).doubleValue();
		    }
		}
	    }

	    /* Write the data times */
	    _chanMap.PutTimes(times);

	    /* Now write the data	*/
	    for (i = 0; i < _numFields; i++) {
		if ((fields != null) && (fields[i].getValue() instanceof Number)) {
		    _log4j.debug("Putting data for channel " + i);
		    _chanMap.PutDataAsFloat64(_chanIndex[i], values[i]);
		}
	    }

	    _log4j.debug("Flushing ChannelMap");
	    dtSource.Flush(_chanMap, true);
	    _log4j.debug("Published " + _pkts.size() + " packets for ID " + _isiID);
	    return(_pkts.size());
	}

	/** Create a new ChannelMap, populate it with Channels from the parsed packet
	 */
	ChannelMap createChannelMap(PacketParser.Field[] fields) throws SAPIException
	{
	    _chanMap = new ChannelMap();
	    _numFields = fields.length;
	    _chanIndex = new int[_numFields];

	    for (int i = 0; i < fields.length; i++)
	    {
		Object obj = fields[i].getValue();

		// Check that we can handle value type
		if (obj instanceof Number)
		{
		    String channelName = fields[i].getName();

		    int chan = _chanMap.Add(getLegalChannelName(channelName));
		    _chanIndex[i] = chan;

		    String units = fields[i].getUnits();
		    if (units != null && units.length() > 0) {
			_chanMap.PutUserInfo(chan, "units=" + units);
		    }
		    else {
			_chanMap.PutUserInfo(chan, "no units on this channel");
		    }

		    _chanMap.PutMime(chan, "application/octet-stream");
		}
		else {
		    _chanIndex[i] = 0;
		}
	    }

	    return(_chanMap);
	}

	/** Return a legal DataTurbine channel name, formed by removing any
	    invalid characters from input name */
	String getLegalChannelName(String inputName)
	{
	    int index = inputName.lastIndexOf("/");
	    if (index >= 0) {
		inputName = inputName.substring(index+1);
	    }
	    return inputName.replace(' ', '_');
	}

    } /* class RecordDesc */

} /* PublishToRBNB */

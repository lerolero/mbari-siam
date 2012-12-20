package org.mbari.siam.dataTurbine;

/**
 * @Title Class to Import SIAM Data into DataTurbine
 * @author Tom O'Reilly
 * @version 1.1
 * @date 30 June 2009
 *
 * Modified for multiple recordTypes by Bob Herlien, 22 Feb 2010
 *
 * Copyright MBARI 2009
 */


import java.util.HashMap;
import java.util.Iterator;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.rbnb.sapi.*;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.registry.RegistryEntry;
import org.mbari.siam.registry.InstrumentDataListener;
import org.apache.log4j.Logger;


/**
   Turbinator parses instrument DevicePackets and writes them to DataTurbine
   channels.
*/
public class Turbinator implements InstrumentDataListener
{
    static private Logger _log4j = Logger.getLogger(Turbinator.class);

    /** Parser parses raw data into numeric-valued fields */
    PacketParser _parser;

    /** Contains Channel names, units, recordTypes */
    protected HashMap _channels = new HashMap();

    /** Specifies mime type for specific channels */
    protected HashMap _mimeTypes = new HashMap();

    /** Contains the ChannelMaps by recordType */
    protected HashMap _dtChannelMaps = new HashMap();

    /** DataTurbine "source" (Turbinator writes to this, clients read from it) */
    protected Source _dtSource;

    /** Unique instrument instance name */
    protected String _instrumentName;

    /** DataTurbine ring buffer's host name */
    protected String _dtHostName;

    /** DataTurbine ring buffer's host port */
    protected int _dtHostPort = 3333;

    protected ChannelDesc _metadataChannel = null;

    protected int _cacheSize;

    protected int _archiveSize;

    /** DNSAdvertiser that will advertise service on ZeroConf	*/
    // protected DNSAdvertiser _advertiser = null;

    public static final String METADATA_CHANNEL_NAME = "metadata";

    private int _testCounter = 0;
    private int ERROR_TEST_INTERVAL = 5;

    private SimpleDateFormat _formatter = 
	new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");


    /** Create the Turbinator */
    public Turbinator(PacketParser parser, 
		      String instrumentName, 
		      String dtHostName, 
		      String sourceLocationName,
		      String instrumentMnemonic,
		      boolean advertiseService,
		      int cacheSize, int archiveSize)
	throws Exception
    {
	_parser = parser;
	_instrumentName = new String(instrumentName);
	_dtHostName = new String(dtHostName);

	if (advertiseService) {
	    // _advertiser = new DNSAdvertiser(instrumentName, sourceLocationName, 
	    //				    instrumentMnemonic);
	}
	int colonIndex = _dtHostName.indexOf(':');
	if (colonIndex > 0)
	    try
		{
		    _dtHostPort = Integer.parseInt(_dtHostName.substring(colonIndex+1));
		} catch (Exception e) {
		_log4j.warn("No port number found after \":\" - " + _dtHostName);
	    }

	// Create the DataTurbine "source" (we'll write to this)
	_cacheSize = cacheSize;
	_archiveSize= archiveSize;
	_dtSource = new Source(_cacheSize, "append", _archiveSize);

	_dtSource.OpenRBNBConnection(_dtHostName, _instrumentName);

	if (_log4j.isDebugEnabled()) {
	    _log4j.debug("Constructed Turbinator for " + _instrumentName +
			 " on host " + _dtHostName + ", cacheSize=" +
			 cacheSize + " archiveSize=" + archiveSize);
	}
    }


    public Turbinator(PacketParser parser, 
		      String instrumentName, 
		      String dtHostName, 
		      String sourceLocationName,
		      String instrumentMnemonic,
		      boolean advertiseService)
	throws Exception
    {
	this(parser, instrumentName, dtHostName, sourceLocationName,
	     instrumentMnemonic, advertiseService, 20, 102400);
    }


    /** Ignore serviceRegisteredCallback() */
    public void serviceRegisteredCallback(RegistryEntry entry) {
    }


    /** Write data to DataTurbine */
    public void dataCallback(DevicePacket sensorData, PacketParser.Field fields[])
    {
	try {
	    write(sensorData, fields);
	} catch (Exception e) {
	    _log4j.error("dataCallback(): " + e);
	    e.printStackTrace();
	}
    }

    /** Parse SIAM device packet and write data to DataTurbine */
    public void write(DevicePacket packet) throws Exception
    {
	write(packet, _parser.parseFields(packet));
    }

    /** Write data to DataTurbine from parsed packet */
    public void write(DevicePacket packet, PacketParser.Field[] fields) throws Exception

    {
	if (_log4j.isDebugEnabled()) {
	    _log4j.debug(_instrumentName + " pkt: " + packet.toString());
	}

	ChannelDesc chanDesc = null;
	Long recordType = new Long(packet.getRecordType());
	ChannelMap channelMap = (ChannelMap)_dtChannelMaps.get(recordType);

	// If _dtChannelMaps contains an explicit null, we're excluding this recordType
	if ((channelMap == null) && (_dtChannelMaps.containsKey(recordType))) {
	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("Skipping record type " + recordType.longValue() + 
			     " for " + _instrumentName);
	    }
	    return;
	}
    
	double[][] values = new double[fields.length][1];

	// If channelMap is null here, we just haven't created it yet
	if (channelMap == null) {
	    if ((channelMap = createChannelMap(recordType, fields)) == null) {
		return;
	    }
	}

	// We'll timestamp all channels with current time-of-day 
	// (maybe instead use packet timestamp)
	channelMap.PutTime((double)(packet.systemTime())/1000., 0.001);
//	channelMap.PutTimeAuto("timeofday");

	for (int i = 0; i < fields.length; i++) {

	    // Get the value of the field
	    Object obj = fields[i].getValue();

	    chanDesc = (ChannelDesc)_channels.get(fields[i].getName());

	    if (chanDesc == null)
	    {
		if (_log4j.isDebugEnabled() &&
		    ((obj instanceof Number) ||
		     (obj.getClass().isArray() && 
		      obj.getClass().getComponentType().equals(Byte.TYPE))))
		{
		    _log4j.debug(" Unregistered channel in write():  "
				 + fields[i].getName() +
				 " (instrument " + _instrumentName + ")");
		}
		continue;
	    }

	    if (chanDesc.recordType() != recordType.longValue()) {
		_log4j.warn("Found " + fields[i].getName() + 
			    " as recordType " +
			    chanDesc.recordType() + ", expected " + 
			    recordType);
		continue;
	    }

	    if (obj instanceof Number) {

		// Convert value to double and write it

		values[i][0] = ((Number)obj).doubleValue();

		try {
		    // Now write the data to the appropriate field
		    channelMap.PutDataAsFloat64(chanDesc.channel(), 
						values[i]);
		}
		catch (Exception e) {
		    _log4j.error("write() exception at " + 
				 _formatter.format(new Date(System.currentTimeMillis()))
				 + ": " + e);
		}
	    }
	    else if (obj.getClass().isArray() &&
		     obj.getClass().getComponentType().equals(Byte.TYPE)) {

		// Write byte array
		try {
		    channelMap.PutMime(chanDesc.channel(), "image/jpeg");
		    channelMap.PutDataAsByteArray(chanDesc.channel(), 
						  (byte[] )obj);
		}
		catch (Exception e) {
		    _log4j.error("write() exception at " + 
				 _formatter.format(new Date(System.currentTimeMillis()))
				 + ": " + e);
		}
	    }

	}

	// Flush channel maps
	try {
	    if (_log4j.isDebugEnabled())
		_log4j.debug("Flush() channelMap for " + _instrumentName);

	    _dtSource.Flush(channelMap, false);

	} catch (Exception e) {

	    _log4j.error("Flush() exception at " + 
			 _formatter.format(new Date(System.currentTimeMillis()))
			 + ": " + e);

	    // Re-open source and re-register channels
	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("Flush(): detach source");
	    }
	    _dtSource.Detach();
	    _dtSource = null;

	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("Flush(): create new source");
	    }
	    _dtSource = new Source(_cacheSize, "append", _archiveSize);

	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("Flush(): remove channel map");
	    }

	    // Re-register channels next time we get a packet
	    // First remove the channel names
	    for (int i = 0; i < fields.length; i++) {
		_channels.remove(fields[i].getName());
	    }

	    // Now remove the ChannelMap for this recordType
	    // This is what forces a new ChannelMap on the next data record
	    _dtChannelMaps.remove(recordType);

	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("Flush(): open rbnb connection at " + 
			     _dtHostName + ", name=" + _instrumentName);
	    }	
	    _dtSource.OpenRBNBConnection(_dtHostName, _instrumentName);

	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("Flush(): OpenRBNBConnection success");
	    }	
	    // We can't process more channels yet until they get
	    // re-registered when next packet comes in, so 
	    // re-throw exception.
	    throw e;
	}
    }


    /** Exclude the given recordType from being written to DataTurbine
     * @return Previous ChannelMap if it had already been assigned
     */
    public ChannelMap excludeRecordType(long rcdType)
    {
	Long recordType = new Long(rcdType);
	ChannelMap oldChannelMap = (ChannelMap)_dtChannelMaps.remove(recordType);

	_dtChannelMaps.put(recordType, null);

	return(oldChannelMap);
    }


    /** Create a new ChannelMap and insert it by the (Long) recordType.
     * If creation fails, throw Exception.
     */
    ChannelMap createChannelMap(Long recordType, PacketParser.Field[] fields) 
	throws Exception
    {
	long rcdType = recordType.longValue();
	if (_log4j.isDebugEnabled()) {
	    _log4j.debug("createChannelMap() - Register new recordType " + rcdType
			 + ", " + fields.length + " fields");
	}

	ChannelMap channelMap = new ChannelMap();
	_dtChannelMaps.put(recordType, channelMap);
	int numChans = 0;

	for (int i = 0; i < fields.length; i++) {
	    
	    Object obj = fields[i].getValue();

	    // Check that we can handle value type
	    if (!(obj instanceof Number) &&
		!(obj.getClass().isArray() && 
		  obj.getClass().getComponentType().equals(Byte.TYPE))) {

		// Can't handle this value type
		continue;
	    }

	    String channelName = fields[i].getName();

	    // Check to see if channel is already registered
	    if (_channels.containsKey(channelName)) {
		// Already registered
		continue;
	    }

	    // This is a new channel
	    int chan = 
		channelMap.Add(Turbinator.getLegalChannelName(channelName));

	    numChans++;
	    String units = fields[i].getUnits();
	    if (units != null && units.length() > 0) {
		channelMap.PutUserInfo(chan, "units=" + units);
	    }
	    else {
		channelMap.PutUserInfo(chan, "no units on this channel" + 
				       units);
	    }

	    // Default mime type
	    String mimeType = "application/octet-stream";
	    if (_mimeTypes.containsKey(channelName)) {
		// Use specified mime type for this channel
		mimeType = (String )_mimeTypes.get(channelName);
	    }

	    channelMap.PutMime(chan, mimeType);

	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("createChannelMap() - added new channel, #" + chan + 
			     " recordType " + rcdType + ", named " + channelName + 
			     ", units=" + units + ", mimeType=" + mimeType);
	    }

	    _channels.put(channelName, new ChannelDesc(rcdType, chan, units));

	}

	if (numChans == 0) {
	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("createChannelMap() - numChans = 0, excluding");
	    }
	    excludeRecordType(rcdType);
	    return(null);
	}

	// If we have a image/jpeg channel, then adding this additional 
	// metadata channel can cause StringIndexOutOfBoundsException when
	// Source.Register() is called!!!
	if (_metadataChannel == null) {
	    // Add channel for service properties
	    int metadataChanNum = channelMap.Add(METADATA_CHANNEL_NAME);
	    channelMap.PutMime(metadataChanNum, "text/plain");
	    _metadataChannel = new ChannelDesc(rcdType, metadataChanNum, 
					       "text");
	    _channels.put(METADATA_CHANNEL_NAME, _metadataChannel);
	}


	if (_log4j.isDebugEnabled()) {
	    _log4j.debug("register with source");
		_log4j.debug("cmap channels["+channelMap.NumberOfChannels()+"]");
		if(channelMap.NumberOfChannels()>0){
		String[] list=channelMap.GetChannelList();
		for(int i=0;i<list.length;i++){
			_log4j.debug("channels["+i+"]="+list[i]);		
		}
		}
	}
	_dtSource.Register(channelMap);
	if (_log4j.isDebugEnabled()) {
	    _log4j.debug("registered");
	}

	// Write property values 
	if ((_metadataChannel != null) && 
	    (rcdType == _metadataChannel.recordType())) {
	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("put string into metadata channel");
	    }
	    channelMap.PutDataAsString(_metadataChannel.channel(), 
				       "SIAM metadata goes here");
	}

	_dtSource.Flush(channelMap, false);


	
	// if (_advertiser != null) {
	//    _advertiser.advertiseService(_dtHostName, _dtHostPort,
	//				 rcdType, _channels);
	// }

	return(channelMap);
    }


    /** Close the DataTurbine source and associated ZeroConf service */
    public void close() {
	if (_log4j.isDebugEnabled()) {
	    _log4j.debug("Turbinator.close()");
	}

	// if (_advertiser != null) {
	//    _advertiser.close();
	//    _advertiser = null;
	// }

	// _log4j.debug("CloseRBNBConnection()");
	// _dtSource.CloseRBNBConnection();
	_dtSource.Detach();
	_dtSource = null;
    }


    /** Set mime type for specified channel 
	(default is "application/octet-stream");
	this method MUST be called prior to parsing first record. */
    public void setMimeType(String channelName, String mimeType) {
	_mimeTypes.put(channelName, mimeType);
    }

    /** Class that goes into _channels HashMap */
    protected class ChannelDesc {
	protected long	_recordType;
	protected int	_channel;
	protected String _units;

	protected ChannelDesc(long recordType, int channel, String units) {
	    _recordType = recordType;
	    _channel = channel;
	    _units = units;
	}

	protected long recordType() {
	    return(_recordType);
	}

	protected int channel() {
	    return(_channel);
	}

	protected String units() {
	    return(_units);
	}
    }


    /** Return a legal DataTurbine channel name, formed by removing any
	invalid characters from input name */
    static String getLegalChannelName(String inputName) {
	// No '/' allowed
	int index = inputName.lastIndexOf("/");
	if (index >= 0) {
	    inputName = inputName.substring(index+1);
	}
	return inputName.replace(' ', '_');
    }
}

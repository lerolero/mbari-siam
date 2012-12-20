// Copyright MBARI 2003
package org.mbari.siam.foce.deployed;

import org.mbari.siam.core.*;
import org.mbari.siam.distributed.devices.AnalogBoard;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;

/** FOCESensorayBoard encapsulates one Sensoray 518 acquisition board */
public class FOCESensorayBoard implements AnalogBoard
{
    static Logger _log4j = Logger.getLogger(FOCESensorayBoard.class);

    protected static final int	NUMBER_OF_CHANNELS = 8;
    protected static final int	DEFAULT_BOARD_ADDR = 0x330;
    protected static final int	UNITS_RAW = 0;
    protected static final int	UNITS_SCALED = 1;
    protected static final int	UNITS_DEGREES_F = 0;
    protected static final int	UNITS_DEGREES_C = 1;
    protected static final int	UNITS_DEGREES_K = 2;

    protected static final String ANALOG_INIT="envInit ";
    protected static final String AD_SETUP = " ";
    protected static final String AD_SAMPLE = "envReadChannel ";
    protected static final String AD_SCAN = "envReadChannels ";
    protected static final String AD_BOARD_TEMP = "envReadBoardTemp ";
    protected static final String AD_PRODUCT_ID = "envProductId ";
    protected static final String AD_READ_CONFIG = "envReadConfig ";
    protected static final String AD_BOARD_ADDRESS = "envBoardAddress ";
    protected static final String AD_FIRMWARE_VERSION = "envFirmwareVersion ";
    protected static final String AD_CONFIGURE_CHANNEL = "envConfigureChannel ";
    protected static final String AD_SET_FILTER_CONSTANT = "envSetFilterConstant ";

    protected static final byte SENSOR_TYPE_THERMISTOR=26;//0x1A
    protected static final byte SENSOR_TYPE_VOLTAGE_5V=21;//0x15

    protected int	_boardAddress = DEFAULT_BOARD_ADDR;
    protected int	_boardIndex;
    protected IOMapper	_ioMapper;

    protected ChannelConfig _boardConfig[]= new ChannelConfig[NUMBER_OF_CHANNELS];

    /** Constructor
     *  @param address Address in I/O space for this board.
     */
    public FOCESensorayBoard(int address) throws IOException
    {
	_boardAddress = address;
	_ioMapper = IOMapper.getInstance();
	_ioMapper.transact(ANALOG_INIT + "\n");
	//defaultConfig();
    }

    /** Creates FOCESensorayBoard at default address and interrupt vector.
     */
    public FOCESensorayBoard() throws IOException
    {
	this(DEFAULT_BOARD_ADDR);
    }

    /** Set up one or more analog channels */
    public void analogSetup(int chan, int range, int polarity, int gain)
	throws IOException
    {
	// does nothing for FOCESensorayBoard
    }

    /** Convert one A/D channel to voltage */
    public double analogSample(int chan)
	throws IOException, NumberFormatException
    {
	if (chan >= NUMBER_OF_CHANNELS || chan<0)
	    throw new IOException("A/D Channel number out of range!");

	return(Double.parseDouble(_ioMapper.transact(AD_SAMPLE + 
						     chan + "\n")));
    }

    /** Convert multiple A/D channels to voltages */
    public double[] analogScan(int chan, int nchans)
	throws IOException, NumberFormatException
    {
	if(chan<0 || chan>NUMBER_OF_CHANNELS || 
	   nchans<=0 || nchans>NUMBER_OF_CHANNELS ||
	   ((chan+nchans)>NUMBER_OF_CHANNELS)){
	    throw new IOException("Invalid A/D Channel  range ["+chan+".."+nchans+"]");
	}
	String result = _ioMapper.transact(AD_SCAN +
					   chan + " " + 
					   nchans + "\n");

	double[] rtnval = new double[nchans];
	StringTokenizer st = new StringTokenizer(result, " ,\t\n\r");

	for (int i = 0; i < nchans; i++)
	{
	    try {
		rtnval[i] = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		_log4j.error("Cannot parse result of analogScan: " + result);
		throw new IOException("Cannot parse result of analogScan: " + result);
	    }
	}

	return(rtnval);
    }


    /** Close this board.  This closes the underlying IOMapper */
    public void close() throws IOException
    {	
	_ioMapper.close();
	_ioMapper = null;
    }

    /** Return number of channels per board */
    public int numChans()
    {
	return(NUMBER_OF_CHANNELS);
    }
    
    /** Return name. */
    public String getName()
    {
	return("FOCESensorayBoard at 0x" + Integer.toHexString(_boardAddress));
    }

    /** Return board address. */
    public int getBoardAddress()
	throws IOException
    {
	return Integer.parseInt(_ioMapper.transact(AD_BOARD_ADDRESS+"\n"));
    }
    /** Return board temperature. */
    public double getBoardTemp()
	throws IOException
    {
	_log4j.debug(">>>>>>>>>requesting BoardTemp...\n");
	String sval=_ioMapper.transact(AD_BOARD_TEMP+UNITS_DEGREES_C+"\n");
	_log4j.debug(">>>>>>>>>requesting BoardTemp="+sval+"...\n");
	return Double.parseDouble(sval);
    }
    /** Return firmware version. */
    public double getFirmwareVersion()
	throws IOException
    {
	_log4j.debug(">>>>>>>>>requesting FirmwareVersion...\n");
	String sval=_ioMapper.transact(AD_FIRMWARE_VERSION+"\n");
	_log4j.debug(">>>>>>>>>requesting FirmwareVersion="+sval+"...\n");
	return Double.parseDouble(sval);
    }
    /** Return board address. */
    public int getProductID()
	throws IOException
    {
	_log4j.debug(">>>>>>>>>requesting ProductID...\n");
	String sval=_ioMapper.transact(AD_PRODUCT_ID+"\n");
	_log4j.debug(">>>>>>>>>requesting ProductID="+sval+"...\n");
	return Integer.parseInt(sval);
    }

    /** Return board address. */
    public String readConfig()
	throws IOException
    {
	_log4j.debug(">>>>>>>>>requesting Channel Configuration...\n");
	String sval=_ioMapper.transact(AD_READ_CONFIG+";\n");
	_log4j.debug(">>>>>>>>>requesting Channel Configuration="+sval+"...\n");
	StringBuffer sb=new StringBuffer();
	StringTokenizer st=new StringTokenizer(sval,";");
	while(st.hasMoreTokens())
	    sb.append(st.nextToken()+"\n");
	return sb.toString();
    }

    /** Set the filter constant for one Sensoray channel.
	Enables filtering configuration via service
    */
    public void setFilterConstant(byte channel, byte filterConstant)
	throws IOException
    {
	_ioMapper.transact(AD_SET_FILTER_CONSTANT+channel+" "+filterConstant+"\n");
    }

    /** Set one Sensoray channel (all parameters).
	Hook to enable full hardware configuration via service, but not currently used
     */
    public void configureChannel(ChannelConfig config)
	throws IOException
    {
	_ioMapper.transact(AD_CONFIGURE_CHANNEL+
			   config._channel+" "+
			   config._sensorType+" "+
			   config._scalar+" "+
			   config._offset+" "+
			   config._filterPercent+" "+
			   config._filterConstant+" "+
			   config._name+" "+
			   config._units+"\n");
    }

    /** Set all Sensoray channel parameters
	Hook to enable full hardware configuration via service, but not currently used
     */
    public void configureChannels(ChannelConfig config[])
	throws IOException
    {
	for(int i=0;i<config.length && i<NUMBER_OF_CHANNELS;i++){
	    configureChannel(config[i]);
	}
	    
    }

    /** Set default copy of channel config.
	Hook to enable full hardware configuration via service, but not currently used
     */
    public void setConfig(ChannelConfig config[]){
	_boardConfig=config;
    }

    /** Set default copy of channel config.
	Hook to enable full hardware configuration via service, but not currently used.
	The foceio server already configures the channels, not much thought has been
	given to if/how the service should do the configuration and keep in sync w/ foceio end.
	It may only make sense to be able to adjust filterConstants, since everything else 
	can't really be changed	on a running system.
     */
    public void defaultConfig(){
	for(int i=0;i<_boardConfig.length;i++){
	    _boardConfig[i]=null;
	}
	_boardConfig[0]=new ChannelConfig((byte)0, SENSOR_TYPE_THERMISTOR, 0.0100, 0.0, (short)10, (byte)25,"Therm-HE104","UNKNOWN");
	_boardConfig[1]=new ChannelConfig((byte)1, SENSOR_TYPE_THERMISTOR, 0.0100, 0.0, (short)10, (byte)25,"Therm-36V","UNKNOWN");
	_boardConfig[2]=new ChannelConfig((byte)2, SENSOR_TYPE_THERMISTOR, 0.0100, 0.0, (short)10, (byte)25,"Therm-24V","UNKNOWN");
	_boardConfig[3]=new ChannelConfig((byte)3, SENSOR_TYPE_THERMISTOR, 0.0100, 0.0, (short)10, (byte)25,"Therm-12V","UNKNOWN");
	_boardConfig[4]=new ChannelConfig((byte)4, SENSOR_TYPE_VOLTAGE_5V, 0.0002, 0.0, (short)15, (byte)38,"Humidity","UNKNOWN");
	_boardConfig[5]=new ChannelConfig((byte)5, SENSOR_TYPE_THERMISTOR, 0.0100, 0.0, (short)10, (byte)25,"Temp","UNKNOWN");
	_boardConfig[6]=new ChannelConfig((byte)6, SENSOR_TYPE_THERMISTOR, 0.0100, 0.0, (short)10, (byte)25,"Therm-Heatsink","UNKNOWN");
	_boardConfig[7]=new ChannelConfig((byte)7, SENSOR_TYPE_VOLTAGE_5V, 0.0002, 0.0, (short)15, (byte)38,"Test2","UNKNOWN");
    }

    /** ChannelConfig describes Sensoray sensor channels */
    public class ChannelConfig{
	byte _channel;
	byte _sensorType;
	double _scalar;
	double _offset;
	short _filterPercent;
	byte _filterConstant;
	String _name="UNKNOWN";
	String _units="UNKNOWN";
	
	/** No arg constructor */
	public ChannelConfig(){
	}

	/** Initializing constructor */
	public ChannelConfig(byte channel,
			   byte sensorType,
			   double scalar,
			   double offset,
			   short filterPercent,
			   byte filterConstant,
			   String name,
			   String units){
	    _channel=channel;
	    _sensorType=sensorType;
	    _scalar=scalar;
	    _offset=offset;
	    _filterPercent=filterPercent;
	    _filterConstant=filterConstant;
	    _name=name;
	    _units=units;
	}

	/** Initializing constructor */
	public ChannelConfig(String info, String delimiter)
	    throws NumberFormatException
	{
	    parseInfo(info,delimiter);
	}

	public String toString(){
	    StringBuffer sb=new StringBuffer();
	    sb.append(_channel+" ");
	    sb.append(_sensorType+" ");
	    sb.append(_scalar+" ");
	    sb.append(_filterPercent+" ");
	    sb.append(_filterConstant+" ");
	    sb.append(_name+" ");
	    sb.append(_units+" ");
	    return sb.toString();
	}

	/** Set values using */
	public void parseInfo( String infoString,String delimiter)
	    throws NumberFormatException
	{
	    StringTokenizer st=new StringTokenizer(infoString,delimiter);
	    int i=0;
	    while(st.hasMoreTokens() && i<7){
		String tok=st.nextToken().trim();
		switch(i++){
		case 0:
		    if(tok!=null)
			_channel=Byte.parseByte(tok);
		    break; 
		case 1:
		    if(tok!=null)
			_sensorType=Byte.parseByte(tok);
		    break; 
		case 2:
		    if(tok!=null)
			_scalar=Double.parseDouble(tok);
		    break; 
		case 3:
		    if(tok!=null)
			_filterPercent=Short.parseShort(tok);
		    break; 
		case 4:
		    if(tok!=null)
			_filterConstant=Byte.parseByte(tok);
		    break; 
		case 5:
		    if(tok!=null)
			_name=tok;
		    break; 
		case 6:
		    if(tok!=null)
			_units=tok;
		    break; 
		default:
		    break; 
		}
	    }
	}
    }
}

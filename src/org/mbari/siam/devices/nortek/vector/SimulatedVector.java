package org.mbari.siam.devices.nortek.vector;

/**
 * @Title Simulated Nortek Vector Velocity Meter Instrument Driver
 * @author Bob Herlien
 * @version 1.0
 * @date 19 Feb 2010
 *
 * Copyright MBARI 2010
 */


/**
 * This driver simulates a Nortek Vector.  It requires a DeviceLog with data
 * from an actual Nortek Vector.  It then streams out that data as if it came
 * from an actual Nortek Vector.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.io.FileNotFoundException;
import java.util.Vector;
import java.text.ParseException;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.StreamingInstrumentService;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.TimeUtils;


/** Instrument service for Simlulated Nortek Vector velocimeter */
public class SimulatedVector extends org.mbari.siam.devices.nortek.vector.Vector
    implements AquadoppIF, Safeable
{
    /** log4j Logger */
    static private Logger _log4j = Logger.getLogger(SimulatedVector.class);
    
    /**
     * Parser used to convert SensorDataPackets into SiamRecords that can be
     * consumed by the SummaryBlock
     */
    /** Configurable Aquadopp attributes */
    public SimulatedVectorAttributes _attributes = new SimulatedVectorAttributes(this);

    protected DeviceLog _deviceLog = null;
    protected long	_curKey = 0, _maxKey = 0;

    public SimulatedVector() throws RemoteException
    {
	super();
    }
    
    
    /** Initialize the Instrument
     *  This means to set all initial settings - This section for one
     * time only instrument settings */
    protected void initializeInstrument() throws InitializeException, Exception
    {
	/* Set up DeviceLog */

	try {
	    _deviceLog = new DeviceLog(getId(), _attributes.deviceDirectory);
	} catch (Exception e) {
	    throw new InitializeException("Nested exception: " + e);
	}

	_curKey = _deviceLog.getMinTimestamp();
	_maxKey = _deviceLog.getMaxTimestamp();
        _log4j.debug("Done with initializeInstrument().");
    }
    
    
    /** Get Instrument State Metadata. TRUE STATE OF INSTRUMENT. */
    protected byte[] getInstrumentStateMetadata()
	throws Exception
    {
	return(("Echoing data from directory: " + _attributes.deviceDirectory).getBytes());
    }
    
    
    /** Service attributes. */
    public class SimulatedVectorAttributes extends org.mbari.siam.devices.nortek.vector.Vector.VectorAttributes
    {
        public SimulatedVectorAttributes(StreamingInstrumentService service)
	{
            super(service);
        }
        
	/** DeviceLog file to use for simulated data. */
	public String deviceDirectory = null;

        /** Time in log file from which to start simulating */
	public String keyTime = null;

        /** Called when an attribute is found */
        protected void setAttributeCallback(String attributeName, String valueString)
	    throws InvalidPropertyException
	{
	    super.setAttributeCallback(attributeName, valueString);
            if (attributeName.equals("keyTime"))
	    {
		try {
		    long newKey = TimeUtils.parseDateTime(valueString);
		    _curKey = newKey;
		    _log4j.info("New keyTime: " + keyTime);
		} catch (ParseException e) {
		    _log4j.warn("Non-parseable keyTime found: " + keyTime);
		}
            }
        }
    }
    
    
    /** Read a Nortek binary sample; overrides
     * BaseInstrumentService.readSample().
     */
    protected int readVectorSample(byte[] sample) throws TimeoutException, IOException, Exception
    {
	DevicePacketSet	pktSet = null;
	DevicePacket	pkt = null;
	SensorDataPacket spkt = null;
	Vector		pktVec = null;
	long		stTime;

	stTime = System.currentTimeMillis();
        
        // Get data from device file
	for (int i = 0; (spkt == null) && (i < 20); i++)
	{
	    try {
		pktSet = _deviceLog.getPackets(_curKey, _maxKey, 1);
		pktVec = pktSet._packets;
		pkt = (DevicePacket)(pktVec.firstElement());
	    } catch (Exception e) {
		_curKey = _deviceLog.getMinTimestamp();
	    }
	    
	    _curKey = pkt.systemTime() + 1;

	    if (pkt instanceof SensorDataPacket) {
		spkt = (SensorDataPacket)pkt;
	    }
	}

	if (spkt == null)
	    throw new TimeoutException("No data in 20 tries");

        int nBytes = spkt.dataBuffer().length;

	if (nBytes > sample.length) {
	    throw new InvalidDataException("Packet data too long");
	}

	System.arraycopy(spkt.dataBuffer(), 0, sample, 0, nBytes);

	if (DataStructure.isVectorVelocityData(sample)) {
	    try {
		Thread.sleep((1000/_attributes.sampleFrequency) - (System.currentTimeMillis() - stTime));
	    } catch (Exception e) {
	    }
	}

	return(nBytes);
    }

    /** Method overridden because we have no actual instrument */
    void getInstrumentAttention() throws Exception
    {
    }

    /** Stop streaming.  Overridden because we have no actual instrument */
    protected void stopStreaming() throws Exception
    {
    }

    /** Put instrument into streaming mode.  Overridden
     * because we have no actual instrument */
    protected void startStreaming() throws Exception
    {
    }

} // End of Vector class

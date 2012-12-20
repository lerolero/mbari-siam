/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.util.Properties;

import org.mbari.siam.utils.ByteUtility;

import org.doomdark.uuid.UUID;
import org.mbari.siam.distributed.InstrumentServiceAttributes;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/** Definition of puck header @author Mike Risi */
public class PuckHeader
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PuckHeader.class);

    private byte[] _headerBytes = null;
    
    // scratch buffer at least large enough to holde the service name
    private byte[] _tempBuffer = new byte[_SERVICE_NAME_SIZE];

    //MBARI is one I guess, who manages these
    /** header organization ID */
    public static final int _ORG_ID = 1;

    /** header type/version ID */
    public static final int _TYPE_ID = 1;

    //What follows are all of the byte sizes and offsets for the puck header
    //this list may be appended for future versions of the puck but properties
    //can never be inserted or backwards compatibility will be lost.
    private static final int _ORG_SIZE = 4;
    private static final int _ORG_OFFSET = 0;
    
    private static final int _TYPE_SIZE = 4;
    private static final int _TYPE_OFFSET = _ORG_SIZE + _ORG_OFFSET;

    private static final int _UUID_SIZE = 16;
    private static final int _UUID_OFFSET = _TYPE_SIZE + _TYPE_OFFSET;

    private static final int _PAYLOAD_START_SIZE = 4;
    private static final int _PAYLOAD_START_OFFSET = _UUID_SIZE + 
                                                     _UUID_OFFSET;

    private static final int _PAYLOAD_END_SIZE = 4;
    private static final int _PAYLOAD_END_OFFSET = _PAYLOAD_START_SIZE + 
                                                   _PAYLOAD_START_OFFSET;

    private static final int _PAYLOAD_CHECKSUM_SIZE = 4;
    private static final int _PAYLOAD_CHECKSUM_OFFSET = _PAYLOAD_END_SIZE + 
                                                        _PAYLOAD_END_OFFSET;
    
    //********* electronic datasheet properties start here *********
    private static final int _SERVICE_NAME_SIZE = 256;
    private static final int _SERVICE_NAME_OFFSET = _PAYLOAD_CHECKSUM_SIZE + 
                                                    _PAYLOAD_CHECKSUM_OFFSET;

    private static final int _ISI_ID_SIZE = 8;
    private static final int _ISI_ID_OFFSET = _SERVICE_NAME_SIZE + 
                                              _SERVICE_NAME_OFFSET;

    private static final int _CURRENT_LIMIT_SIZE = 4;
    private static final int _CURRENT_LIMIT_OFFSET = _ISI_ID_SIZE + 
                                                     _ISI_ID_OFFSET;

    /** header size in bytes */
    public static final int _SIZE = _ORG_SIZE +
                                    _TYPE_SIZE +
                                    _UUID_SIZE +
                                    _PAYLOAD_START_SIZE +
                                    _PAYLOAD_END_SIZE +
                                    _PAYLOAD_CHECKSUM_SIZE +
                                    _SERVICE_NAME_SIZE +
                                    _ISI_ID_SIZE +
                                    _CURRENT_LIMIT_SIZE;
    
    /** create a puck header using a byte array */
    public PuckHeader(byte[] headerBytes)
    {
        _headerBytes = new byte[_SIZE];

        //set _headerByte to zero
        for (int i = 0; i < _SIZE ; i++)
            _headerBytes[i] = 0;
        
        //copy over the header bytes you were given
        for (int i = 0; i < headerBytes.length; i++)
            _headerBytes[i] = headerBytes[i];
        
    }


    /** create an empty puck header */
    public PuckHeader()
    {
        _headerBytes = new byte[_SIZE];
        setOrgId(_ORG_ID);
        setTypeId(_TYPE_ID);
        setPayloadStart(_SIZE);
    }

    /** get the puck header bytes */
    public byte[] getBytes()
    {
        return _headerBytes;
    }

    /** get the organization ID */
    public int getOrgId()
    {
        try
        {
            return ByteUtility.bytesToInt(_headerBytes, _ORG_OFFSET);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }

    /** get the header type/version ID */
    public int getTypeId()
    {
        try
        {
            return ByteUtility.bytesToInt(_headerBytes, _TYPE_OFFSET);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
        
    }

    /** get the UUID for the puck */
    public UUID getUUID()
    {
        return new UUID(_headerBytes, _UUID_OFFSET);
    }

    /** get the start address of the puck payload in bytes */
    public int getPayloadStart()
    {
        try
        {
            return ByteUtility.bytesToInt(_headerBytes, 
                                          _PAYLOAD_START_OFFSET);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }

    /** get the end address of the puck payload in bytes */
    public int getPayloadEnd()
    {
        try
        {
            return ByteUtility.bytesToInt(_headerBytes, _PAYLOAD_END_OFFSET);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }

    /** get the size of the of the payload in bytes */
    public int getPayloadSize()
    {
        return (getPayloadEnd() - getPayloadStart());
    }

    /** get the checksum for the puck payload */
    public long getChecksum()
    {
        byte[] b = new byte[8];
        
        // zero out MSBs
        for (int i = 0; i < 4; ++i)
            b[i] = 0;

        // get checksum bytes
        for (int i = 0; i < 4; ++i)
            b[i + 4] = _headerBytes[i + _PAYLOAD_CHECKSUM_OFFSET];
        
        try
        {
            return ByteUtility.bytesToLong(b);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }

    /** get the driver name for the device */
    public String getServiceName()
    {
        String serviceName = new String(_headerBytes, 
                                        _SERVICE_NAME_OFFSET, 
                                        _SERVICE_NAME_SIZE);  
        
        return serviceName.trim();

    }

    /** get the device ISI ID */
    public long getIsiId()
    {
        try
        {
            return ByteUtility.bytesToLong(_headerBytes, _ISI_ID_OFFSET);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }

    /** get the current limit of device attached to the puck */
    public int getCurrentLimit()
    {
        try
        {
            return ByteUtility.bytesToInt(_headerBytes, 
                                          _CURRENT_LIMIT_OFFSET);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }

    // the organization code i.e. MBARI, WHOI, Exxon, US Navy, etc.
    /** set the organization ID */
    private void setOrgId(int id)
    {
        byte[] b = ByteUtility.intToBytes(id);
        for (int i = 0; i < _ORG_SIZE; ++i)
            _headerBytes[i + _ORG_OFFSET] = b[i];
    }

    /** set the header type/version ID */
    private void setTypeId(int id)
    {
        byte[] b = ByteUtility.intToBytes(id);
        for (int i = 0; i < _TYPE_SIZE; ++i)
            _headerBytes[i + _TYPE_OFFSET] = b[i];
    }

    /** set the UUID for this puck wite */
    public void setUUID(UUID uuid)
    {
        byte[] b = uuid.toByteArray();
        for (int i = 0; i < _UUID_SIZE; ++i)
            _headerBytes[i + _UUID_OFFSET] = b[i];
    }

    /** set payload size for the device service code */
    private void setPayloadStart(int start)
    {
        byte[] b = ByteUtility.intToBytes(start);
        for (int i = 0; i < _PAYLOAD_START_SIZE; ++i)
            _headerBytes[i + _PAYLOAD_START_OFFSET] = b[i];
    }

    /** set payload size for the device service code */
    public void setPayloadSize(int size)
    {
        int end = getPayloadStart() + size;
        byte[] b = ByteUtility.intToBytes(end);
        for (int i = 0; i < _PAYLOAD_END_SIZE; ++i)
            _headerBytes[i + _PAYLOAD_END_OFFSET] = b[i];
    }
    
    /** set the checksum for the puck payload */
    public void setChecksum(long checkSum)
    {
        byte[] b = ByteUtility.longToBytes(checkSum);
        //in this case we only want the last 4 bytes
        //the first four are always zero
        for (int i = 0; i < _PAYLOAD_CHECKSUM_SIZE; ++i)
            _headerBytes[i + _PAYLOAD_CHECKSUM_OFFSET] = b[i + 4];
    }

    /** set the driver name for the device */
    public void setServiceName(String name)
    {
        byte[] b = name.getBytes();
        
        for (int i = 0; i < _SERVICE_NAME_SIZE; ++i)
        {
            if ( i < b.length)
                _headerBytes[i + _SERVICE_NAME_OFFSET] = b[i];
            else
                _headerBytes[i + _SERVICE_NAME_OFFSET] = 0;

        }
    }

    /** set the ISI ID for the device*/
    public void setIsiId(long id)
    {
        byte[] b = ByteUtility.longToBytes(id);
        for (int i = 0; i < _ISI_ID_SIZE; ++i)
            _headerBytes[i + _ISI_ID_OFFSET] = b[i];
    }

    /** set the devices curretn limit */
    public void setCurrentLimit(int limit)
    {
        byte[] b = ByteUtility.intToBytes(limit);
        for (int i = 0; i < _CURRENT_LIMIT_SIZE; ++i)
            _headerBytes[i + _CURRENT_LIMIT_OFFSET] = b[i];
    }

    /** Attempts to set the puck header isiId, serviceName, and currentLimit 
    from a ServiceProperties object.  If a property can not be found an error
    value is used.  For the ISI ID and the current limit the error value
    is -1 */
    public void setServiceProperties(Properties properties) {

	InstrumentServiceAttributes attributes = 
	    InstrumentServiceAttributes.getAttributes();

	try {
	    attributes.fromProperties(properties, false);
	}
	catch (Exception e) {
	    // Maybe invalid or missing properties
	}

        //set the service name from the properties
        String serviceName = new String(attributes.serviceName);

        if ( serviceName == null )
            serviceName = "*** UNKNOWN ***";

        setServiceName(serviceName);

        //set the isiID
        long isiID = attributes.isiID;

	if (isiID <= 0) {
            _log4j.error("ISI ID invalid in properties file");
            _log4j.error("Setting ISI ID to -1.");
            isiID = -1;
        }

        
        setIsiId(isiID);

        //set the current limit from the properties
        int currentLimit = attributes.currentLimitMa;
	if (currentLimit <= 0) {
            _log4j.error("Current limit invalid in properties file");
            _log4j.error("Setting current limit to -1.");
            currentLimit = -1;
        }
        
        setCurrentLimit(currentLimit);
    }
}

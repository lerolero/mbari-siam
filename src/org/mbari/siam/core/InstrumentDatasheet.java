/****************************************************************************/
/* Copyright 2005 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.io.PrintStream;
import java.lang.Long;

import org.mbari.siam.utils.ByteUtility;

import org.doomdark.uuid.UUID;

/** Definition of Instrument Datasheet @author Mike Risi */
public class InstrumentDatasheet
{
    private byte[] _dataSheetBytes = null;
    
    public static final int _FORMAT_VERSION = 1;

    //What follows are all of the byte sizes and offsets for the instrument
    //datasheet this list may be appended for future versions of the datashee 
    //but datasheet fields can never be inserted or backwards compatibility 
    //will be lost.
    private static final int _UUID_LEN = 16;
    private static final int _UUID_OFFSET = 0;
    
    private static final int _DS_VERS_LEN = 2;
    private static final int _DS_VERS_OFFSET = _UUID_LEN + _UUID_OFFSET;

    private static final int _DS_SIZE_LEN = 2;
    private static final int _DS_SIZE_OFFSET = _DS_VERS_LEN + _DS_VERS_OFFSET;

    private static final int _MAN_ID_LEN = 4;
    private static final int _MAN_ID_OFFSET = _DS_SIZE_LEN + _DS_SIZE_OFFSET;
    
    private static final int _MAN_MODEL_LEN = 2;
    private static final int _MAN_MODEL_OFFSET = _MAN_ID_LEN + _MAN_ID_OFFSET;
    
    private static final int _MAN_VERS_LEN = 2;
    private static final int _MAN_VERS_OFFSET = _MAN_MODEL_LEN + 
                                                _MAN_MODEL_OFFSET;
    
    private static final int _SER_NUM_LEN = 4;
    private static final int _SER_NUM_OFFSET = _MAN_VERS_LEN + _MAN_VERS_OFFSET;
    
    private static final int _NAME_LEN = 64;
    private static final int _NAME_OFFSET = _SER_NUM_LEN + _SER_NUM_OFFSET;

    /** instrument datasheet size in bytes */
    public static final int _SIZE = _UUID_LEN +
                                    _DS_VERS_LEN +
                                    _DS_SIZE_LEN +
                                    _MAN_ID_LEN + 
                                    _MAN_MODEL_LEN + 
                                    _MAN_VERS_LEN + 
                                    _SER_NUM_LEN + 
                                    _NAME_LEN;

    /** create an instrument datasheet using a byte array */
    public InstrumentDatasheet(byte[] datasheet_bytes)
    {
        _dataSheetBytes = new byte[_SIZE];

        //copy over the header bytes you were given
        for (int i = 0; i < _dataSheetBytes.length; i++)
            if ( i < datasheet_bytes.length )
                _dataSheetBytes[i] = datasheet_bytes[i];
    }


    /** create an uninitialize instrument datasheet */
    public InstrumentDatasheet()
    {
        _dataSheetBytes = new byte[_SIZE];
        setIntParam(_SIZE, _DS_SIZE_OFFSET);
        setIntParam(_FORMAT_VERSION, _DS_VERS_OFFSET);
    }

    public void list(PrintStream out)
    {
        out.println("UUID        : " + getUUID());
        out.println("ver         : " + getVersion());
        out.println("size        : " + getDsSize());
        out.println("man id      : " + getManId());
        out.println("man model   : " + getManModel());
        out.println("man version : " + getManVer());
        out.println("man serial  : " + getManSer());
        out.println("name        : " + getName());
    }
    
    
    /** return the instrument datasheet bytes */
    public byte[] getDatasheetBytes()
    {
        byte[] datasheet = new byte[_SIZE];
        System.arraycopy(_dataSheetBytes, 0, datasheet, 0, _SIZE);
        return datasheet;
    }

    
    /** get the instrument datasheet UUID */
    public UUID getUUID()
    {
        return new UUID(_dataSheetBytes, _UUID_OFFSET);
    }

    public int getVersion()
    {
        return getIntParam(_DS_VERS_OFFSET, _DS_VERS_LEN);
    }

    public int getDsSize()
    {
        return getIntParam(_DS_SIZE_OFFSET, _DS_SIZE_LEN);
    }
    
    public long getManId()
    {
        return getLongParam(_MAN_ID_OFFSET, _MAN_ID_LEN);
    }

    public int getManModel()
    {
        return getIntParam(_MAN_MODEL_OFFSET, _MAN_MODEL_LEN);
    }

    public int getManVer()
    {
        return getIntParam(_MAN_VERS_OFFSET, _MAN_VERS_LEN);
    }

    public long getManSer()
    {
        return getLongParam(_SER_NUM_OFFSET, _SER_NUM_LEN);
    }

    public String getName()
    {
        return new String(getNameBytes()).trim();
    }

    public byte[] getNameBytes()
    {
        byte[] name = new byte[_NAME_LEN];
        System.arraycopy(_dataSheetBytes, _NAME_OFFSET, name, 0, _NAME_LEN);
        return name;
    }

    public void setUUID(UUID id)
    {
        byte[] id_bytes = id.toByteArray();

        System.arraycopy(id_bytes, 0, _dataSheetBytes, _UUID_OFFSET, _UUID_LEN);
    }

    public void setVersion(int version)
    {
        setIntParam(version, _DS_VERS_OFFSET);
    }

    public void setManId(long man_id)
    {
        setLongParam(man_id, _MAN_ID_OFFSET);
    }
    
    public void setManModel(int man_model)
    {
        setIntParam(man_model, _MAN_MODEL_OFFSET);
    }

    public void setManVer(int man_vers)
    {
        setIntParam(man_vers, _MAN_VERS_OFFSET);
    }
    
    public void setManSer(long man_ser)
    {
        setLongParam(man_ser, _SER_NUM_OFFSET);
    }

    public void setName(String name)
    {
        setNameBytes(name.getBytes());
    }

    public void setNameBytes(byte[] name_bytes)
    {
        //copy to the datasheet, zero out remaining bytes
        //if they exist
        for (int i = 0; i < _NAME_LEN; ++i)
        {
            if ( name_bytes.length > i)
                _dataSheetBytes[i + _NAME_OFFSET] = name_bytes[i];
            else
                _dataSheetBytes[i + _NAME_OFFSET] = 0;
        }
    }

    private int getIntParam(int offset, int length)
    {
        byte[] val_bytes = new byte[4];
        
        //copy bytes to val_bytes buffer
        System.arraycopy(_dataSheetBytes, offset, val_bytes, 2, length);

        //covnert the bytes to an int and return the value
        try
        {
            return ByteUtility.bytesToInt(val_bytes);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }


    private long getLongParam(int offset, int length)
    {
        byte[] val_bytes = new byte[8];
        
        //copy bytes to val_bytes buffer
        System.arraycopy(_dataSheetBytes, offset, val_bytes, 4, length);

        //covnert the bytes to a long and return the value
        try
        {
            return ByteUtility.bytesToLong(val_bytes);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            //should never get here
            return -1;
        }
    }

    private void setIntParam(int param, int offset)
    {
        //turn the int to bytes
         byte[] param_bytes = ByteUtility.intToBytes(param);

         //copy bytes to _dataSheetBytes buffer
         System.arraycopy(param_bytes, 2, _dataSheetBytes, offset, 2);
    }
    
    private void setLongParam(long param, int offset)
    {
        //turn the long to bytes
        byte[] param_bytes = ByteUtility.longToBytes(param);

        //copy bytes to _dataSheetBytes buffer
        System.arraycopy(param_bytes, 4, _dataSheetBytes, offset, 4);
    }
}

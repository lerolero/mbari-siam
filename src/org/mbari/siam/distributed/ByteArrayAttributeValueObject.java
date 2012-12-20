// Copyright 2002 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Externalizable;
import java.io.DataOutput;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.io.OptionalDataException;
import java.io. IOException;
import java.lang.ClassNotFoundException;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.mbari.siam.distributed.Exportable;

/**
   ByteArrayAttributeValueObject - The value for state attributes which have
   byte array (byte[]) values.
  
   @author Kent Headley
*/

public class ByteArrayAttributeValueObject extends AttributeValueObject{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(ByteArrayAttributeValueObject.class);

    /** Serial version ID */
    private static final long serialVersionUID=0x0L;

    public ByteArrayAttributeValueObject(){}

    /** Contructor 
	@param s constructor calls parse(s) to obtain _value member
     */
    public ByteArrayAttributeValueObject(String s) throws Exception{
	_value = parse(s);
    }
    public ByteArrayAttributeValueObject(byte[] s) throws Exception{
	_value = s;
    }

    /** Return String representation. */
    public String toString(){
	return (new String((byte[])_value));
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(String s) throws Exception{	
	_value = s.getBytes();
	return _value;
    }

    public void writeExternal(ObjectOutput out) 
	throws IOException{

	//_logger.debug("Externalizing ByteArrayAttributeValueObject ***************************");

	out.writeInt(((byte[])_value).length);
	out.write((byte[])_value);
    }

    public void readExternal(ObjectInput in) 
	throws IOException,ClassNotFoundException{

	//_logger.debug("Un-Externalizing ByteArrayAttributeValueObject ***************************");
	int valueLen = in.readInt();
	_value = new byte[valueLen];
	in.readFully((byte[])_value,0,valueLen);
    }

    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	out.writeShort(Exportable.EX_BYTEARRAYOBJATT);	
	out.writeLong(serialVersionUID);
	out.writeInt(((byte[])_value).length);
	out.write((byte[])_value);
    }
}


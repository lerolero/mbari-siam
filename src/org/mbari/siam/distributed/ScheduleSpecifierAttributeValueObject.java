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
import java.io.IOException;
import java.lang.ClassNotFoundException;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.mbari.siam.distributed.Exportable;

/**
   ScheduleSpecifierAttributeValueObject - The value for state attributes which have
   ScheduleSpecifier values.
  
   @author Kent Headley
*/

public class ScheduleSpecifierAttributeValueObject extends AttributeValueObject{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(StateAttribute.class);

    /** Serial version ID */
    private static final long serialVersionUID=0x0L;

    public ScheduleSpecifierAttributeValueObject(){}

    /** Contructor 
	@param s constructor calls parse(s) to obtain _value member
     */
    public ScheduleSpecifierAttributeValueObject(String s) throws Exception{
	_value = parse(s);
    }

    public ScheduleSpecifierAttributeValueObject(long s) throws Exception{
	Long i = new Long(s);
	_value = parse(i.toString());
    }

    public ScheduleSpecifierAttributeValueObject(ScheduleSpecifier s) throws Exception{
	// The SechedulerSpecifier is validated on createion
	_value = s;
    }

    /** Return String representation. */
    public String toString(){
	return ((ScheduleSpecifier)_value).toString();
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(String s) throws Exception{	
	_value = new ScheduleSpecifier(s);
	return _value;
    }

    public void writeExternal(ObjectOutput out) 
	throws IOException{

	//_log4j.debug("Externalizing ScheduleSpecifierAttributeValueObject ***************************");
	String value = ((ScheduleSpecifier)_value).toString();
	out.writeInt(value.getBytes().length);
	out.write(value.getBytes());
    }

    public void readExternal(ObjectInput in) 
	throws IOException,ClassNotFoundException{

	//_log4j.debug("Un-Externalizing ScheduleSpecifierAttributeValueObject ***************************");

	int valueLen = in.readInt();
	byte[] value = new byte[valueLen];
	in.readFully(value,0,valueLen);
	try{
	    _value = new ScheduleSpecifier(new String(value));
	}catch(ScheduleParseException e){
	    _log4j.error(e);
	    e.printStackTrace();
	}
    }

    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	out.writeShort(Exportable.EX_SCHEDULESPECIFIEROBJATT);	
	out.writeLong(serialVersionUID);

	String value = ((ScheduleSpecifier)_value).toString();
	out.writeInt(value.getBytes().length);
	out.write(value.getBytes());	
    }
}






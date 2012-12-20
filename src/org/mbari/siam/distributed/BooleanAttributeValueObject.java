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
   BooleanAttributeValueObject - The value for state attributes which have
   Boolean values.
  
   @author Kent Headley
*/

public class BooleanAttributeValueObject extends AttributeValueObject{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(StateAttribute.class);

    /** Serial version ID */
    private static final long serialVersionUID=0x0L;



    public BooleanAttributeValueObject(){}

    /** Contructor 
	@param s constructor calls parse(s) to obtain _value member
     */
    public BooleanAttributeValueObject(String s) throws Exception{
	_value = parse(s);
    }

    public BooleanAttributeValueObject(boolean i) throws Exception{
	_value = new Boolean(i);
    }

    /** Return String representation. */
    public String toString(){
	return ((Boolean)_value).toString();
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(String s) throws Exception{
	_value = new Boolean(s);
	return _value;
    }

    public void writeExternal(ObjectOutput out) 
	throws IOException{
	//_logger.debug("Externalizing BooleanAttributeValueObject ***************************");
	out.writeBoolean(((Boolean)_value).booleanValue());

    }

    public void readExternal(ObjectInput in) 
	throws IOException,ClassNotFoundException{

	//_logger.debug("Un-Externalizing BooleanAttributeValueObject ***************************");
	_value = new Boolean(in.readBoolean());
    }

    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	out.writeShort(EX_BOOLEANOBJATT);
	out.writeLong(serialVersionUID);
	out.writeBoolean(((Boolean)_value).booleanValue());
    }
}



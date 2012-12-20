// Copyright 2002 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.io.Externalizable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.DataOutput;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.io.OptionalDataException;
import java.lang.ClassNotFoundException;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.mbari.siam.distributed.Exportable;

/**
   StateAttribute is a class from which
   all StateAttributes inherit.
   @author Kent Headley
*/

public class StateAttribute implements  Externalizable, Exportable {
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(StateAttribute.class);

    /** Serialization version ID */
    private static final long serialVersionUID=0x0L;
    public static final int STATEATTRIBUTE_SERIAL_TAG = 1;

    /** Name of this attribute */
    protected String _name;

    /** Value of this attribute */
    protected AttributeValueObject _value;

    /** Value may be changed or deleted 
	Super classes can create StateAttributes with accessible set
	to false, and provide accessor methods which check its value
	to provide access control. Default value is false.
     */
    private boolean _accessible=false;

    public StateAttribute(){
    }

    /** Create a new attribute from an object */
    public StateAttribute(String name, AttributeValueObject value, boolean accessible){
	this();
	_accessible = accessible;
    }

    public StateAttribute(String name, AttributeValueObject value){
	_name = name;
	_value = value;
    }

    /** Return value of accessible member variable */
    public boolean isAccessible(){
	return _accessible;
    }

    /** Return the name of this attribute */
    public String getName(){
	return _name;
    }

    /** Return _value member variable. */
    public AttributeValueObject getValue(){
	return _value;
    }

    /** Return Object representation of value. Some attributes may, for example,
	store _value as a String, but value() may return an Integer, ScheduleSpecifier, or 
	other Object.
    */
    public Object value(){
	return _value.value();
    }

    /** Return String representation. */
    public String toString(){
	return(_name+":"+_value);
    }

    /** Return serialization version of this instance */
    public long getVersion(){
	return serialVersionUID;
    }

    /** Write a state attribute object to an output stream. Adds a state object
        to a metadata packet payload, which is a stream of serialized
	State and StateAttributes.
    */
    public static void writeStateAttribute(ObjectOutput out, StateAttribute attribute)
    throws IOException{
	out.writeShort(Exportable.EX_STATEATTRIBUTE);
	out.writeObject(attribute);
	out.flush();
    }

    public void writeExternal(ObjectOutput out)
	throws IOException{
	//_logger.debug("Externalizing StateAttribute ***************************");

 	out.writeInt(_name.getBytes().length);
	out.writeBytes(_name);

	out.writeObject(_value);
   }


    public void readExternal(ObjectInput in)
	throws IOException,ClassNotFoundException{
	//_logger.debug("Un-Externalizing StateAttribute ***************************");

	int nameLen = in.readInt();
	byte name[]=new byte[nameLen];
	in.readFully(name,0,nameLen);
	_name = new String(name);

	_value = (AttributeValueObject)in.readObject();
    }
    
    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	out.writeInt(Exportable.EX_STATEATTRIBUTE);
	out.writeLong(serialVersionUID);

 	out.writeInt(_name.getBytes().length);
	out.writeBytes(_name);
	_value.export(out);
    }
}




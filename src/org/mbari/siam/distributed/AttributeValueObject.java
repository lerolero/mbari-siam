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
import org.mbari.siam.distributed.ExportException;

/**
   AttributeValueObject
   @author Kent Headley
*/

public abstract class AttributeValueObject implements Externalizable, Exportable{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(AttributeValueObject.class);

    /** Serial version ID */
    private static final long serialVersionUID=0x0L;

    /** The value */
    protected Object _value;

    /** Empty CTOR for serialization */
    public AttributeValueObject(){}

    /** Return String representation. */
    abstract public String toString();

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    abstract public Object parse(String s) throws Exception;

    /** Return serialization version of this instance */
    public long getVersion(){
	return serialVersionUID;
    }

    /** Return byte array representation. */
    public Object value(){
	return _value;
    }

    // Subclasses MUST also implement and document the following methods,
    // with the following signatures:

    public void writeExternal(ObjectOutput out) throws IOException{
 	_logger.debug("Externalizing AttributeValueObject ***************************");
   }

    public void readExternal(ObjectInput in) throws IOException,ClassNotFoundException{
 	_logger.debug("Un-Externalizing AttributeValueObject ***************************");
    }

    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	throw(new ExportException("Export Not Implemented"));
    }
}



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
   LongAttributeValueObject - The value for state attributes which have
   Integer values.
  
   @author Kent Headley
*/

public class LongAttributeValueObject extends AttributeValueObject{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(StateAttribute.class);

    /** Serial version ID */
    private static final long serialVersionUID=0x0L;

    /** if _rangeMin<_rangeMax, _rangeMin<=_value<=_rangeMax, 
	if _rangeMin>=_rangeMax, _rangeMin>=_value || _value>=_rangeMax
	if _rangeMin==_rangeMax, _value=_rangeMin=_rangeMax
    */
    private long _rangeMax=Long.MAX_VALUE;

    /** if _rangeMin<_rangeMax, _rangeMin<=_value<=_rangeMax, 
	if _rangeMin>=_rangeMax, _rangeMin>=_value || _value>=_rangeMax
	if _rangeMin==_rangeMax, _value=_rangeMin=_rangeMax
    */
    private long _rangeMin=Long.MIN_VALUE;

    public LongAttributeValueObject(){}

    /** Contructor 
	@param s constructor calls parse(s) to obtain _value member
	@param rangeMin minimum value 
	@param rangeMax maximum value
     */
    public LongAttributeValueObject(String s, long rangeMin, long rangeMax) throws Exception{
	_rangeMax = rangeMax;
	_rangeMin = rangeMin;
	_value = parse(s);
    }

    public LongAttributeValueObject(String s) throws Exception{
	_value = parse(s);
    }

    public LongAttributeValueObject(long i, long rangeMin, long rangeMax) throws Exception{
	_rangeMax = rangeMax;
	_rangeMin = rangeMin;
	
	_value = new Long(i);
	if(!validate((Long)_value))
	    throw(new Exception("Invalid Long Value"));
    }

    public LongAttributeValueObject(long i) throws Exception{
	_value = new Long(i);
	if(!validate((Long)_value))
	    throw(new Exception("Invalid Long Value"));
    }

    /** Return String representation. */
    public String toString(){
	return ((Long)_value).toString();
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(String s) throws Exception{
	Long i = new Long(s);
	if(!validate(i))
	    throw(new Exception("LongAttributeValueObject: value out of range "+i+" ("+_rangeMin+"-"+_rangeMax+")"));
	_value = i;
	return _value;
    }

    public boolean validate(Long i) throws Exception{
	try{
	    boolean outOfRange = false;
	    if(_rangeMin<_rangeMax){
		// check if value is BETWEEN rangeMin and rangeMax (inclusive)
		if(i.longValue()<_rangeMin || i.longValue()>_rangeMax)
		   outOfRange = true;
	    }else if(_rangeMin>_rangeMax){
		// check if value is OUTSIDE rangeMin to rangeMax (inclusive)
		if(i.longValue()>_rangeMax && i.longValue()<_rangeMin)
		    outOfRange = true;
	    }else{
		// i must equal range
		if(i.longValue()!=_rangeMax)
		    outOfRange = true;		
	    }
	    if(outOfRange)
		return false;
	    return true;
	}catch(NumberFormatException e){
	    throw(e);
	}
    }

    public void writeExternal(ObjectOutput out) 
	throws IOException{

	//_logger.debug("Externalizing LongAttributeValueObject ***************************");
	out.writeLong(((Long)_value).longValue());
	out.writeLong(_rangeMin);
	out.writeLong(_rangeMax);
    }

    public void readExternal(ObjectInput in) 
	throws IOException,ClassNotFoundException{

	//_logger.debug("Un-Externalizing LongAttributeValueObject ***************************");
	_value = new Long(in.readLong());
	_rangeMin = in.readLong();
	_rangeMax = in.readLong();
    }

    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{

	out.writeShort(Exportable.EX_LONGOBJATT);	
	out.writeLong(serialVersionUID);

	out.writeLong(((Long)_value).longValue());
	out.writeLong(_rangeMin);
	out.writeLong(_rangeMax);
    }
}



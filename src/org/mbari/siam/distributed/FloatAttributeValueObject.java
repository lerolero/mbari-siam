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
   FloatAttributeValueObject - The value for state attributes which have
   Integer values.
  
   @author Kent Headley
*/

public class FloatAttributeValueObject extends AttributeValueObject{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(StateAttribute.class);

    /** Serial version ID */
    private static final long serialVersionUID=0L;

    /** if _rangeMin<_rangeMax, _rangeMin<=_value<=_rangeMax, 
	if _rangeMin>=_rangeMax, _rangeMin>=_value || _value>=_rangeMax
	if _rangeMin==_rangeMax, _value=_rangeMin=_rangeMax
    */
    private float _rangeMax=Float.POSITIVE_INFINITY;

    /** if _rangeMin<_rangeMax, _rangeMin<=_value<=_rangeMax, 
	if _rangeMin>=_rangeMax, _rangeMin>=_value || _value>=_rangeMax
	if _rangeMin==_rangeMax, _value=_rangeMin=_rangeMax
    */
    private float _rangeMin=Float.NEGATIVE_INFINITY;

    public FloatAttributeValueObject(){}

    /** Contructor 
	@param s constructor calls parse(s) to obtain _value member
	@param rangeMin minimum value 
	@param rangeMax maximum value
     */
    public FloatAttributeValueObject(String s, float rangeMin, float rangeMax) throws Exception{
	_rangeMax = rangeMax;
	_rangeMin = rangeMin;
	_value = parse(s);
    }

    public FloatAttributeValueObject(String s) throws Exception{
	_value = parse(s);
    }

    public FloatAttributeValueObject(float i, float rangeMin, float rangeMax) throws Exception{
	_rangeMax = rangeMax;
	_rangeMin = rangeMin;
	
	_value = new Float(i);
	if(!validate((Float)_value))
	    throw(new Exception("Invalid Float Value"));
    }

    public FloatAttributeValueObject(float i) throws Exception{
	_value = new Float(i);
	if(!validate((Float)_value))
	    throw(new Exception("Invalid Float Value"));
    }

    /** Return String representation. */
    public String toString(){
	return ((Float)_value).toString();
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(String s) throws Exception{
	Float i = new Float(s);
	if(!validate(i))
	    throw(new Exception("FloatAttributeValueObject: value out of range "+i+" ("+_rangeMin+"-"+_rangeMax+")"));
	_value = i;
	return _value;
    }

    public boolean validate(Float i) throws Exception{
	try{
	    boolean outOfRange = false;
	    if(_rangeMin<_rangeMax){
		// check if value is BETWEEN rangeMin and rangeMax (inclusive)
		if(i.floatValue()<_rangeMin || i.floatValue()>_rangeMax)
		   outOfRange = true;
	    }else if(_rangeMin>_rangeMax){
		// check if value is OUTSIDE rangeMin to rangeMax (inclusive)
		if(i.floatValue()>_rangeMax && i.floatValue()<_rangeMin)
		    outOfRange = true;
	    }else{
		// i must equal range
		if(i.floatValue()!=_rangeMax)
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

	//_logger.debug("Externalizing FloatAttributeValueObject ***************************");
	out.writeFloat(((Float)_value).floatValue());
	out.writeFloat(_rangeMin);
	out.writeFloat(_rangeMax);

    }

    public void readExternal(ObjectInput in) 
	throws IOException,ClassNotFoundException{

	//_logger.debug("Un-Externalizing FloatAttributeValueObject ***************************");
	_value = new Float(in.readFloat());
	_rangeMin = in.readFloat();
	_rangeMax = in.readFloat();
    }

    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	out.writeShort(Exportable.EX_INTEGEROBJATT);	
	out.writeLong(serialVersionUID);

	out.writeFloat(((Float)_value).floatValue());
	out.writeFloat(_rangeMin);
	out.writeFloat(_rangeMax);
     }
}



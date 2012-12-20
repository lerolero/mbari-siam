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
   IntegerAttributeValueObject - The value for state attributes which have
   Integer values.
  
   @author Kent Headley
*/

public class IntegerAttributeValueObject extends AttributeValueObject{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(StateAttribute.class);

    /** Serial version ID */
    private static final long serialVersionUID=0x0L;

    /** if _rangeMin<_rangeMax, _rangeMin<=_value<=_rangeMax, 
	if _rangeMin>=_rangeMax, _rangeMin>=_value || _value>=_rangeMax
	if _rangeMin==_rangeMax, _value=_rangeMin=_rangeMax
    */
    private int _rangeMax=Integer.MAX_VALUE;

    /** if _rangeMin<_rangeMax, _rangeMin<=_value<=_rangeMax, 
	if _rangeMin>=_rangeMax, _rangeMin>=_value || _value>=_rangeMax
	if _rangeMin==_rangeMax, _value=_rangeMin=_rangeMax
    */
    private int _rangeMin=Integer.MIN_VALUE;

    public IntegerAttributeValueObject(){}

    /** Contructor 
	@param s constructor calls parse(s) to obtain _value member
	@param rangeMin minimum value 
	@param rangeMax maximum value
     */
    public IntegerAttributeValueObject(String s, int rangeMin, int rangeMax) throws Exception{
	_rangeMax = rangeMax;
	_rangeMin = rangeMin;
	_value = parse(s);
    }

    public IntegerAttributeValueObject(String s) throws Exception{
	_value = parse(s);
    }

    public IntegerAttributeValueObject(int i, int rangeMin, int rangeMax) throws Exception{
	_rangeMax = rangeMax;
	_rangeMin = rangeMin;	
	_value = new Integer(i);
	if(!validate((Integer)_value))
	    throw(new Exception("Invalid Integer Value"));
    }

    public IntegerAttributeValueObject(int i) throws Exception{
	_value = new Integer(i);
	if(!validate((Integer)_value))
	    throw(new Exception("Invalid Integer Value"));
    }

    /** Return String representation. */
    public String toString(){
	return ((Integer)_value).toString();
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(String s) throws Exception{	
	Integer i = new Integer(s);
	if(!validate(i))
	    throw(new Exception("IntegerAttributeValueObject: value out of range "+i+" ("+_rangeMin+"-"+_rangeMax+")"));
	_value = i;
	return _value;
    }

    private boolean validate(Integer i) throws Exception{
	try{
	    boolean outOfRange = false;
	    if(_rangeMin<_rangeMax){
		// check if value is BETWEEN rangeMin and rangeMax (inclusive)
		if(i.intValue()<_rangeMin || i.intValue()>_rangeMax)
		   outOfRange = true;
	    }else if(_rangeMin>_rangeMax){
		// check if value is OUTSIDE rangeMin to rangeMax (inclusive)
		if(i.intValue()>_rangeMax && i.intValue()<_rangeMin)
		    outOfRange = true;
	    }else{
		// i must equal range
		if(i.intValue()!=_rangeMax)
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

	//_logger.debug("Externalizing IntegerAttributeValueObject ***************************");
	out.writeInt(((Integer)_value).intValue());
	out.writeInt(_rangeMin);
	out.writeInt(_rangeMax);

    }

    public void readExternal(ObjectInput in) 
	throws IOException,ClassNotFoundException{

	//_logger.debug("Un-Externalizing IntegerAttributeValueObject ***************************");
	_value = new Integer(in.readInt());
	_rangeMin = in.readInt();
	_rangeMax = in.readInt();
    }

    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	out.writeShort(Exportable.EX_INTEGEROBJATT);	
	out.writeLong(serialVersionUID);

	out.writeInt(((Integer)_value).intValue());
	out.writeInt(_rangeMin);
	out.writeInt(_rangeMax);
     }
}



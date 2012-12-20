/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Arrays;

import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.RangeException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
   SiamProperties extends java.util.Properties, with more
   robust parsing of keys and values.
 */
public class SiamProperties extends Properties {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(SiamProperties.class);

    /** Searches for the property with specified key. Trailing
	whitespace is trimmed from returned value. */
    public String getProperty(String key) {
	String value = super.getProperty(key);
	if (value != null)
	    value = value.trim();

	return value;
    }

    /** Searches for the property with specified key. If the property
	key is not found, then defaultValue is returned. Trailing
	whitespace is trimmed from returned value. */
    public String getProperty(String key, String defaultValue) {
	String value = super.getProperty(key, defaultValue);
	if (value != null) 
	    value = value.trim();

	return value;
    }


    /** Get value of specified property; throw MissingPropertyException 
	if not found. */
    public String getRequiredProperty(String key) 
	throws MissingPropertyException {
	String value = getProperty(key);
	if (value == null) {
	    throw new MissingPropertyException("Missing property " + key);
	}
	return value;
    }

    /** Performs an Integer.parseInt(), but allows hexadecimal
	beginning with "0x"				 */
    protected int parseInteger(String s) throws NumberFormatException
    {
	try {
	    return(Integer.parseInt(s));
	} catch (NumberFormatException e) {
	    if (s.startsWith("0x"))
		return(Integer.parseInt(s.substring(2), 16));
	    throw e;
	}
    }

    /** Searches for the integer property with specified key. Throws
	InvalidPropertyException if property is not an integer value.
	Throws MissingPropertyException if property not found	*/
    public int getIntegerProperty(String key)
	throws MissingPropertyException, InvalidPropertyException
    {
	String strValue = getRequiredProperty(key);
	int    value;

	try
	{
	  value = Integer.parseInt(strValue);
	} catch(NumberFormatException e) {
	  throw new InvalidPropertyException("Invalid integer property "+key);
	}
	return value;
    }

    /** Searches for the integer property with specified key. If the
	property key is not found or is not a valid integer, then defaultValue
	is returned. */
    public int getIntegerProperty(String key, int defaultValue)
    {
        int value;
        String strValue = super.getProperty(key);

	if (strValue == null) 
	    return(defaultValue);

	try {
	  strValue = strValue.trim();
	  value = Integer.parseInt(strValue);
	} catch(NumberFormatException e) {
	  _log4j.error("Invalid integer property "+key+" = " + strValue +
			     "  Using default "+defaultValue);
	  return(defaultValue);
	}
	return value;
    }

    /** Searches for the integer range property with specified key.  
	Integer ranges are sets of (possibly) non-contiguous integer
	values, expressed using a comma and dash syntax, e.g.:
	2-5,11,13-15 (==2,3,4,5,11,13,14,15)
    */
    public ChannelRange[] getChannelRangeProperty(String key)
	throws MissingPropertyException, InvalidPropertyException
    {
	String rangeList=super.getProperty(key);
	if(rangeList==null){
	    throw new MissingPropertyException("Missing integer range property: ["+rangeList+"]");
	}
	return parseChannelRangeProperty(key);
    }
    public ChannelRange[] parseChannelRangeProperty(String rangeStr)
	throws MissingPropertyException, InvalidPropertyException
    {
	Vector values=new Vector();

	StringTokenizer stPri=new StringTokenizer(rangeStr,",");
	StringTokenizer stSec;
	_log4j.debug("parseIntegerRangeProperty found "+stPri.countTokens()+" toks in "+rangeStr);
	while(stPri.hasMoreTokens()){
	    String subrange=stPri.nextToken();
	    if(subrange.indexOf("-")>=0){
		stSec=new StringTokenizer(subrange,"-");
		_log4j.debug("parseIntegerRangeProperty found "+stSec.countTokens()+" toks in "+subrange);
		if(stSec.countTokens()!=2){
		    throw new InvalidPropertyException("Invalid sub-range: ["+subrange+"]");
		}
		int min=new Integer(stSec.nextToken()).intValue();
		int max=new Integer(stSec.nextToken()).intValue();
		if(min>max){
		    int foo=min;
		    min=max;
		    max=foo;
		}
		_log4j.debug("adding to range (mul)");
		try{
		    values.add(new ChannelRange(min,max));
		}catch(RangeException e){
		    throw new InvalidPropertyException("Invalid range value: ["+subrange+"]");
		}
	    
	    }else{
		try{
		    _log4j.debug("adding "+subrange+" to range (sgl)");
		    int rangeval=new Integer(subrange).intValue();
		    values.add(new ChannelRange(rangeval,rangeval));
		}catch(NumberFormatException e){
		    throw new InvalidPropertyException("Invalid number format: ["+subrange+"]");
		}catch(RangeException e){
		    throw new InvalidPropertyException("Invalid range value: ["+subrange+"]");
		}
	    }
	}
	
	//_log4j.debug("parseChannelRange found "+values.size()+" ranges");
	ChannelRange[] retval=new ChannelRange[values.size()];
	
	_log4j.debug("parseChannelRange returning:");
	for(int x=0;x<retval.length;x++){
	    retval[x]=(ChannelRange)values.get(x);
	    //_log4j.debug(retval[x]);
	}
	return (retval);
    }

    public int getNonNegativeIntegerProperty(String key) 
	throws MissingPropertyException, InvalidPropertyException {

	int value = getIntegerProperty(key);

	if (value < 0) {
	    throw new InvalidPropertyException("Non-negative required");
	}

	return value;
    }


    /** Searches for the long integer property with specified key. Throws
	InvalidPropertyException if property is not an integer value.
	Throws MissingPropertyException if property not found	*/
    public long getLongProperty(String key)
	throws MissingPropertyException, InvalidPropertyException
    {
	String strValue = getProperty(key);
	long    value;

	try
	{
	  value = Long.parseLong(strValue);
	} catch(NumberFormatException e) {
	  throw new InvalidPropertyException("Invalid integer property "+key);
	}
	return value;
    }

    /** Searches for the long integer property with specified key. If the
	property key is not found or is not a valid integer, then defaultValue
	is returned. */
    public long getLongProperty(String key, long defaultValue)
    {
        long value;
        String strValue = super.getProperty(key);

	if (strValue == null) 
	    return(defaultValue);

	try
	{
	  strValue = strValue.trim();
	  value = Long.parseLong(strValue);
	} catch(NumberFormatException e) {
	  _log4j.error("Invalid integer property "+key+" = " + strValue +
			     "  Using default "+defaultValue);
	  return(defaultValue);
	}
	return value;
    }

    /** Searches for the property with specified key, where the value
	contains an array of integers.  Throws InvalidPropertyException
	if any value is not an integer.	 Throws MissingPropertyException
	if property not found or is null.
	<p>
	For example: analogPort12 = 0 0 4 100 10 #returns 5 integers
    */
    public int[] getIntegerArrayProperty(String key)
	throws MissingPropertyException, InvalidPropertyException
    {
	StringTokenizer tok;

	try {
	    tok = new StringTokenizer(getRequiredProperty(key));
	}
	catch (NullPointerException e) {
	    throw new MissingPropertyException("Can't find " + key + " or has null value.");
	}

	int numValues = tok.countTokens();
	int[] values = new int[numValues];

	for (int i = 0; i < numValues; i++)
	{
	    try {
		values[i] = parseInteger(tok.nextToken());
	    }
	    catch(NumberFormatException e) {
		throw new InvalidPropertyException("Invalid integer array property "
						   +key);
	    }
	}

	return(values);
    }

    /** Searches for the property with specified key, where the value
	contains an array of Strings.  Throws InvalidPropertyException
	if any value is not a String the Vector validValues; if validValues
	is null, any values are OK.	 
	Throws MissingPropertyException if property not found or is null.
	<p>
	For example: platformAnalogTypes = foo bar baz # returns 3 Strings
    */
    public String[] getStringArrayProperty(String key, String[] validValues, String delimiter)
	throws MissingPropertyException, InvalidPropertyException
    {
	_log4j.debug("getStringArrayProperty looking for "+key);
	String fields=getRequiredProperty(key);
	_log4j.debug("getStringArrayProperty got "+key+"="+fields);
	return parseStringArrayProperty( fields,  validValues,  delimiter);
    }
    public String[] parseStringArrayProperty(String inStr, String[] validValues, String delimiter)
	throws MissingPropertyException, InvalidPropertyException
    {
	StringTokenizer tok;
	_log4j.debug("parseStringArrayProperty got "+inStr);
	try {
	    tok = new StringTokenizer(inStr,delimiter);
	}
	catch (NullPointerException e) {
	    throw new MissingPropertyException("Can't find " + inStr + " or has null value.");
	}

	int numValues = tok.countTokens();
	String[] values = new String[numValues];
	boolean gotMatch=false;
	for (int i = 0; i < numValues; i++){
	    String s=tok.nextToken().trim();
	    if(validValues!=null){
		for(int j=0;j<validValues.length;j++){
		    if(s.equalsIgnoreCase(validValues[i])){
			values[i]=s;
			gotMatch=true;
			break;
		    }
		    if(!gotMatch){
			throw new InvalidPropertyException("String property not valid ["+s+"]");
		    }else{
			values[i]=s;
		    }
		}
	    }else{
		values[i]=s;
	    }
	}
	return(values);
    }

} /* SiamProperties */


/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.text.NumberFormat;

import org.mbari.siam.distributed.InvalidPropertyException;

import org.apache.log4j.Logger;

/** This filter evaluates the value of its (single) input against a specified range of values.
	The output is updated if the value is within the specified range. If the input value is
	outside the valid range, there are several options, set by a member variable:
	- REJECT         : reject the value; do not change output
	- USE_LAST_VALID : output last valid input value
    - USE_AVERAGE    : average up to _average_n previous valid values
    - USE_CONSTANT   : use the same constant value
 */

public class RangeFilter extends Filter{

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(RangeFilter.class);
	
	protected RangeValidator _validator;
	protected StringBuffer _stringBuffer;
	
    public RangeFilter()
	{
		super();
    }
	

    public RangeFilter(String name, int id, FilterInput input, RangeValidator validator ) 
	throws Exception{
		super(name,id);
		_validator=validator;

		if(input!=null){
			addInput(input);
		}
		_dfmt=NumberFormat.getInstance();
		_dfmt.setMinimumFractionDigits(0);
		_dfmt.setMaximumFractionDigits(2);
		
    }
	
	public void setBounds(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper)
	throws Exception{
		_validator.setBounds(region,lowerBound,upperBound,includeLower,includeUpper);
	}
	
	public void setConstant(double constant,boolean allowOutsideRange )throws Exception{
		_validator.setConstant(constant,allowOutsideRange);
	}
	
	public void setRejectPolicy(int policy)
	throws Exception{
		_validator.setRejectPolicy(policy);
	}
	
	private void setAverageN(int averageN)
	throws Exception{
		_validator.setAverageN(averageN);
	}
		
    /** Perform the filter function with the new input value.
	Sets member variable _filterValue, which will be passed
	to the output by triggerOut() if the output gating
	conditions are met.
	Overrides base class default.
    */
    protected int doFilterAction(double value){
		
		double test=_validator.validate(value);
		
		if(Double.isNaN(test)){
			_log4j.debug("["+value+"] output ["+test+"] - rejected");
			return ACTION_CANCEL;
		}else{
			_log4j.debug("["+value+"] output ["+test+"] - valid");
			_filterValue=test;
		}
		return ACTION_OK;
		
    }

	public String toString(){
		if(_stringBuffer==null){
			_stringBuffer=new StringBuffer();
		}
		_stringBuffer.setLength(0);
		_stringBuffer.insert(0,super.toString());
		_stringBuffer.append(_validator.toString());
		return _stringBuffer.toString();
	}

}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import org.mbari.siam.distributed.InvalidPropertyException;

import org.apache.log4j.Logger;

/** This filter produces the resulting heading from its two (XY) inputs.
	The output is recalculated whenever any of the inputs changes.
	
 */

public class HeadingFilter extends Filter{
	
	public static final int OUTPUT_RADIANS=0;
	public static final int OUTPUT_DEGREES=1;
	protected int _output=OUTPUT_DEGREES;
	public final double PI=Math.toRadians(180.0);
	
    public HeadingFilter(){
		super();
    }
	
    public HeadingFilter(String name, int id){
		super(name,id);
    }
	 
    public HeadingFilter(FilterInput xInput, FilterInput yInput, int mode)
	throws InvalidPropertyException{
		this(DEFAULT_NAME,DEFAULT_ID,xInput,yInput,mode);
    }
	
    public HeadingFilter(String name, FilterInput xInput, FilterInput yInput, int mode) 
	throws InvalidPropertyException{
		this(name,DEFAULT_ID,xInput,yInput,mode);
    }
	
    public HeadingFilter(int id, FilterInput xInput, FilterInput yInput, int mode) 
	throws InvalidPropertyException{
		this(DEFAULT_NAME,id,xInput,yInput,mode);
    }
    public HeadingFilter(String name,int id, FilterInput xInput, FilterInput yInput, int mode) 
	throws InvalidPropertyException{
		this(name,id);
		setInputs(xInput,yInput);
		setMode(mode);
    }
	
	public void setInputs(FilterInput xInput, FilterInput yInput)
	throws InvalidPropertyException{
		_inputs.clear();
		addInput(xInput);
		addInput(yInput);
	}
	
	public void setMode(int mode)
	throws InvalidPropertyException{
		switch (mode) {
			case OUTPUT_DEGREES:
				_output=OUTPUT_DEGREES;
				break;
			case OUTPUT_RADIANS:
				_output=OUTPUT_RADIANS;
				break;
			default:
				throw new InvalidPropertyException("Invalid output mode ["+mode+"]");
		}
	}
	
    /** Perform the filter function with the new input value.
	Sets member variable _filterValue, which will be passed
	to the output by triggerOut() if the output gating
	conditions are met.
	Overrides base class default.
    */
    protected int doFilterAction(double value){
		double xMag=((FilterInput)_inputs.get(0)).doubleValue();
		double yMag=((FilterInput)_inputs.get(1)).doubleValue();
		double heading=Math.atan2(xMag,yMag);
		if(_output==OUTPUT_DEGREES){
			heading=Math.toDegrees(heading);
		}
		_filterValue=heading;
		//_log4j.debug(" xMag:"+xMag+" yMag:"+yMag+" val:"+_filterValue);
		return ACTION_OK;
    }


}

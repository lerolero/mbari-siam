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

/** This filter produces the square root of the sum of the squares (magnitude) of its inputs.
	The output is recalculated whenever any of the inputs changes.
	
 */

public class MagnitudeFilter extends Filter{
	
    public MagnitudeFilter(){
		super();
    }
	
    public MagnitudeFilter(String name,int id){
		super(name,id);
    }
	
    public MagnitudeFilter(Vector inputs){
		this();
		_inputs=inputs;
    }
	
    public MagnitudeFilter(String name, Vector inputs) 
	throws InvalidPropertyException{
		super(name,DEFAULT_ID,inputs);
    }
	
    public MagnitudeFilter(int id, Vector inputs) 
	throws InvalidPropertyException{
		super(DEFAULT_NAME,id,inputs);
    }
    
	public MagnitudeFilter(String name, int id, Vector inputs) 
	throws InvalidPropertyException{
		super(name,id,inputs);
    }
	
    /** Perform the filter function with the new input value.
	Sets member variable _filterValue, which will be passed
	to the output by triggerOut() if the output gating
	conditions are met.
	Overrides base class default.
    */
    protected int doFilterAction(double value){
		double sum=0.0;	
		for( Enumeration e=_inputs.elements();e.hasMoreElements();){
			FilterInput input=(FilterInput)e.nextElement();
			if(input.getInhibit()==false){
				sum+=input.doubleValue()*input.doubleValue();
			}
		}
		_filterValue=Math.sqrt(sum);
		//_log4j.debug(" sum:"+sum+" val:"+_filterValue);
		return ACTION_OK;
    }


}

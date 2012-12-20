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

/** This filter produces an instantaneous (no memory) weighted average of its inputs.
	The output is recalculated whenever any of the inputs changes.
	
 */

public class WeightedAverageFilter extends Filter{
	/** Average divisor.
	    If divisor == 0, 
		the average is computed using the sum of the uninhibited inputs
		divided by the total number of uninhibited inputs.
		If divisor != 0, 
	    the average is computed using the sum of the uninhibited inputs
		divided by the divisor.
	 
		By default, divisor is set to zero.
	 */
	int _divisor=0;
	
    public WeightedAverageFilter()
	{
		super();
    }
	
    public WeightedAverageFilter(String name,int id)
	{
		super(name,id);
    }
	
    public WeightedAverageFilter(Vector inputs) 
	{
		this();
		_inputs=inputs;
    }
    public WeightedAverageFilter(String name, Vector inputs) 
	throws InvalidPropertyException{
		this(name,DEFAULT_ID, inputs,0);
    }
    public WeightedAverageFilter(int id, Vector inputs) 
	throws InvalidPropertyException{
		this(DEFAULT_NAME,id,inputs,0);
    }
    public WeightedAverageFilter(String name, int id, Vector inputs) 
	throws InvalidPropertyException{
		this(name,id,inputs,0);
    }
	
    public WeightedAverageFilter(String name, int id, Vector inputs, int divisor) 
	throws InvalidPropertyException{
		super(name,id,inputs);
		setDivisor(divisor);
    }
	
	/** set divisor value */
	public void setDivisor(int divisor){
		_divisor=divisor;
	}
	
	/** get divisor value */
	public int getDivisor(){
		return _divisor;
	}
	
    /** Perform the filter function with the new input value.
	Sets member variable _filterValue, which will be passed
	to the output by triggerOut() if the output gating
	conditions are met.
	Overrides base class default.
    */
    protected int doFilterAction(double value){
		double sum=0.0;	
		int n=0;
		for( Enumeration e=_inputs.elements();e.hasMoreElements();){
			FilterInput input=(FilterInput)e.nextElement();
			if(input.getInhibit()==false){
				sum+=input.doubleValue();
				n++;
			}
		}

		int div=( (_divisor==0) ? n : _divisor);
		
		if(div==0){
			_log4j.error("Divisor is zero in doFilterAction [filter id="+_id+" name:"+_name+"]");
			return ACTION_CANCEL;
		}
		
		_filterValue=sum/div;
		/*
		if(_log4j.isDebugEnabled()){
			_log4j.debug("name:"+_name+" id:"+_id+" DIV:"+_divisor+" in:"+inputCount()+" sum:"+sum+" n:"+n+" div:"+div+" val:"+_filterValue);
			_log4j.debug("DUMP["+this+"]");
		}
		 */
		return ACTION_OK;
    }


}

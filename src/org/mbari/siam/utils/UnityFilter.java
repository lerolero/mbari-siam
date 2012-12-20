/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.util.Enumeration;

import org.mbari.siam.distributed.InvalidPropertyException;

import org.apache.log4j.Logger;

public class UnityFilter extends Filter{

    public UnityFilter(){
		super();
    }
	
    public UnityFilter(String name,int id){
		super(name,id);
    }
	 
	
    /** Perform the filter function with the new input value.
	Sets member variable _filterValue, which will be passed
	to the output by triggerOut() if the output gating
	conditions are met.
	Overrides base class default.
    */
    protected int doFilterAction(double value){
		_filterValue=value;
		return ACTION_OK;
    }

}

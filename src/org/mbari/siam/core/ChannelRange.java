/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.RangeException;

public class ChannelRange{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(ChannelRange.class);

    protected int _first=9999;
    protected int _last=-1;
    public int length;

    public ChannelRange(int firstChannel, int lastChannel)
	throws RangeException
    {
	if(firstChannel<0 || lastChannel<0){
	    throw new RangeException("Channels must be >= 0");
	}
	if(firstChannel>lastChannel){
	    _last=firstChannel;
	    _first=lastChannel;
	}else{
	    _last=lastChannel;
	    _first=firstChannel;
	}
	length=_last-_first+1;
    }

    public int first(){
	return _first;
    }
    public int last(){
	return _last;
    }
    public String toString(){
	return ("["+_first+","+_last+","+length+"]");
    }
}

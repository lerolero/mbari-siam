/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;


import org.apache.log4j.Logger;
import org.mbari.siam.distributed.RangeException;

public class ChannelParameters{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(ChannelParameters.class);
	    
    protected int _board;
    protected ChannelRange _channels[];
    protected int _numberOfChannels=0;
    protected int _min=9999;
    protected int _max=-1;
    public ChannelParameters(int board, ChannelRange channels[])
	throws RangeException
    {
	if(board<0){
	    throw new RangeException("invalid board number must be > 0 ["+board+"]");
	}
	_board=board;
	_channels=channels;
	for(int i=0;i<_channels.length;i++){
	    _numberOfChannels+=_channels[i].length;
	    if(_channels[i].first()<_min){
		_min=_channels[i].first();
	    }
	    if(_channels[i].first()>_max){
		_max=_channels[i].first();
	    }
	}
    }
    public ChannelRange[] getChannels(){
	return _channels;
    }
    public int boardNumber(){
	return _board;
    }
    public int ranges(){
	return _channels.length;
    }
    public int numberOfchannels(){
	return _numberOfChannels;
    }
    public int min(){return _min;}
    public int max(){return _max;}
}

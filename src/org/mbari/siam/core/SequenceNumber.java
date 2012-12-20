//Copyright 2003 MBARI  (where 2003 is date processed or created)
//Monterey Bay Aquarium Research Institute Proprietary Information.
//All rights reserved.

package org.mbari.siam.core;

import org.mbari.siam.distributed.SequenceGenerator;
import org.mbari.siam.distributed.RangeException;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;


/** SequenceNumber is an implementation of the SequenceGenerator 
    interface used for generating packet sequence numbers.

    The packet sequence numbers should start at zero within a 
    log file and increase by one each time a packet is logged;
    also, they should persist across power cycles. Thus, if a log
    file exists, when a power cycle occurs, the packet numbers
    should pick up where they left off in the log.

    The user is responsible for determining the initial values.

    IMPORTANT NOTES:
    This implementation assumes that sequence numbers will not
    be less than zero (uses only 0-Long.MAX_VALUE), and therefore
    doesn't suffer from overflow problems. This class will give 
    incorrect results if you use it in a way that exceeds the 
    range of a long.

   @author Kent Headley
*/

public class SequenceNumber implements SequenceGenerator {
    /** log4j logger */
     static Logger _log4j = Logger.getLogger(SequenceNumber.class);

    long _current;
    long _greatest;
    long _least;

    /** Constructor for creating and (explicitly) initializing a SequenceNumber */
    public SequenceNumber(long least,long greatest,long start) throws RangeException{
	initialize(least,greatest,start);
    }


    /** Get next number in sequence */
    public synchronized long getNext(){

	long retval=_current;

	// detect rollover and adjust _current
	if( _current == _greatest){
	    _current = _least;
	    // should we do something (e.g., trigger a metadata packet)
	    // on rollover?
	}else
	{    
	    _current++;
	}
	return retval;
    }

    /** Get minimum value in sequence */
    public synchronized long getLeast(){
	return _least;
    }

    /** Get maximum value in sequence */
    public synchronized long getGreatest(){
	return _greatest;
    }

    /** Set sequence range */
    public synchronized void initialize(long least,long greatest,long current)
	throws RangeException{
	if(least > greatest){
	    _least = greatest;
	    _greatest = least;
	}else{
	    _least = least;
	    _greatest = greatest;
	}
	if(current>_greatest || current<least)
	    throw new RangeException("setSequenceRange: Invalid current value: "+current);
	_current=current;
    }

    /** Restart sequence from minimum value */
    public synchronized void reset(){
	_current = _least;
    }

    /** Look at nth number in sequence (relative to the current number)
	without actually changing anything. 
	The offset may be positive, negative or zero.
	Using an offset of 0 returns the most recent number returned.
	Using an offset beyond the sequence bounds optionally wraps around.
    */
    public synchronized long peek(long offset,boolean wrap){
	long retval=0L;
	long span;

	// if the offset wraps more than once,
	// and you care about wrapping, cut it down to size
	span = (_greatest-_least);
	if(span<0L){
	    _log4j.error("peek: detected overflow(g-l): span="+span);
	    //throw new Exception();
	}
	if(wrap && (offset > span) ){
	   offset%=span;
	}


	if(offset == 0L){
	    // current value
	    retval = _current;
	}else{
	if(offset > 0L){
	    span = (_greatest-_current);
	    if(span<0L){
		_log4j.error("peek: detected overflow(g-c): span="+span);
		//throw new Exception();
	    }
	    if( span >= offset ){
		// positive, no wrap
		retval = _current + offset;
	    }else{
		// positive wrap around
		if(wrap){
		    retval = _least + (offset - span);
		}else{
		    retval = _greatest;
		}
	    }
	}else{

	    span = _current-_least;

	    // detect overflow
	    if(span<0L){
		_log4j.error("peek: detected overflow(c-l): span="+span);
		//throw new Exception();
	    }

	    if( span >= -offset ){
		// negative, no wrap
		retval = _current + offset;
	    }else{
		// negative wrap around
		if(wrap){
		    retval = _greatest + (offset + span);
		}else{
		    retval = _least;
		}
	    }
	}
	}
	return retval;
    }

    /** Eine Kleine test code */
    public static void main( String args[]){
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	SequenceNumber sn;
	try{
	    long init;
	    if(args.length>0)
		init=Long.parseLong(args[0]);
	    else
		init=0L;

	    long min=0L;
	    long max=Long.MAX_VALUE;

	    _log4j.debug("init("+min+","+max+","+init+")");
	    sn = new SequenceNumber(min,max,init);
	    _log4j.debug("current:"+sn.peek(0L,false));
	    sn.getNext();
	    _log4j.debug("getNext()");	
	    _log4j.debug("current:"+sn.peek(0L,false));	
	    sn.getNext();
	    _log4j.debug("getNext()");	
	    _log4j.debug("current:"+sn.peek(0L,false));	

	    _log4j.debug("peek(-3,false):"+sn.peek(-3,false));	
	    _log4j.debug("peek(-2,false):"+sn.peek(-2,false));	
	    _log4j.debug("peek(-1,false):"+sn.peek(-1,false));	
	    _log4j.debug("peek(0,false):"+sn.peek(0,false));	
	    _log4j.debug("peek(1,false):"+sn.peek(1,false));	
	    _log4j.debug("peek(2,false):"+sn.peek(2,false));	
	    _log4j.debug("peek(3,false):"+sn.peek(3,false));
	    sn.initialize(min,max,5);
	    _log4j.debug("init("+min+","+max+",5)");	
	    _log4j.debug("current:"+sn.peek(0L,false));	
	    _log4j.debug("peek(3,true):"+sn.peek(3,true));	
	    _log4j.debug("peek(-1,true):"+sn.peek(-1,true));	
	    _log4j.debug("peek(-9,true):"+sn.peek(-9,true));	
	    _log4j.debug("peek(MAXLONG,true):"+sn.peek(Long.MAX_VALUE,true));	
	    init=max-2;
	    sn.initialize(min,max,init);
	    _log4j.debug("init("+min+","+max+","+init+")");	
	    _log4j.debug("current:"+sn.peek(0L,false));	
	    _log4j.debug("peek(3,true):"+sn.peek(3,true));	
	    _log4j.debug("peek(-1,true):"+sn.peek(-1,true));	
	    _log4j.debug("peek(-9,true):"+sn.peek(-9,true));	
	    _log4j.debug("peek(MAXLONG,true):"+sn.peek(Long.MAX_VALUE,true));	
	    sn.reset();
	    _log4j.debug("reset()");
	    _log4j.debug("current:"+sn.peek(0L,false));	
	    try{
		init=12;
		min=3;
		max=8;
		_log4j.debug("init("+min+","+max+","+init+")");	
		sn.initialize(min,max,init);
		_log4j.error("Range Exception Test - FAILED");
	    }catch(RangeException e){
		_log4j.error("Range Exception Test - PASS");
	    }
	    _log4j.debug("");
	    _log4j.debug("MAXLONG="+(Long.MAX_VALUE));
	    _log4j.debug("MINLONG="+(Long.MIN_VALUE));
	    _log4j.debug("MAXLONG+1="+(Long.MAX_VALUE+1));
	    _log4j.debug("MAXLONG+2="+(Long.MAX_VALUE+2));
	    _log4j.debug("MAXLONG+MAXLONG="+(Long.MAX_VALUE+Long.MAX_VALUE));
	    _log4j.debug("MINLONG+MINLONG="+(Long.MIN_VALUE+Long.MIN_VALUE));
	    _log4j.debug("MINLONG-1="+(Long.MIN_VALUE-1));
	    _log4j.debug("MINLONG-2="+(Long.MIN_VALUE-2));
	    _log4j.debug("MINLONG-MINLONG="+(Long.MIN_VALUE-Long.MIN_VALUE));
	    _log4j.debug("MAXLONG+MINLONG="+(Long.MAX_VALUE+Long.MIN_VALUE));
	    _log4j.debug("MAXLONG-MINLONG="+(Long.MAX_VALUE-Long.MIN_VALUE));
	    _log4j.debug("MINLONG-MAXLONG="+(Long.MIN_VALUE-Long.MAX_VALUE));

	}catch(RangeException e){
	    _log4j.error(e);
	    System.exit(1);
	}catch(NumberFormatException e){
	    _log4j.error(e);
	    System.exit(1);
	}

    }
}// end class SequenceNumber

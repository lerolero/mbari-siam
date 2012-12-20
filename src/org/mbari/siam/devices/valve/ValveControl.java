/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.valve;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.devices.bbElec.BB232SDD16;
import org.mbari.siam.distributed.devices.ValveIF;
import org.mbari.siam.distributed.devices.Valve2WayIF;

import org.apache.log4j.Logger;

/** Implementation of ValveIF for one Hanbay MDx-xxxDT valve connected to a
 * B&B Electronics 232SDD16 Digital I/O controller
 */
/*
 $Id: ValveControl.java,v 1.7 2012/12/17 21:34:43 oreilly Exp $
 $Name: HEAD $
 $Revision: 1.7 $
 */

public class ValveControl implements Valve2WayIF, Remote
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(ValveControl.class);
    protected static int[] positionMap = 
    {Valve2WayIF.OPEN_FUNC, Valve2WayIF.OPEN_FUNC, 
     Valve2WayIF.CLOSE_FUNC, Valve2WayIF.CLOSE_FUNC};

    protected BB232SDD16 _digitalIO;
    protected ValveAttributes _attributes;
    protected int	_valveNum;
    protected int	_cmd1Bit, _cmd2Bit, _feedback1Vector, _feedback2Vector;
    protected int[]	_funcMap = new int[2];

    public ValveControl(BB232SDD16 digIO, ValveAttributes attributes, int valveNum)
    {	
	_digitalIO = digIO;
	_attributes = attributes;
	_cmd1Bit = attributes.cmd1Bits[valveNum];
	_cmd2Bit = attributes.cmd2Bits[valveNum];
	_feedback1Vector = (1 << attributes.feedback1Bits[valveNum]);
	_feedback2Vector = (1 << attributes.feedback2Bits[valveNum]);
	_funcMap[Valve2WayIF.OPEN_FUNC] = attributes._openpos[valveNum];
	_funcMap[Valve2WayIF.CLOSE_FUNC] = attributes._closedpos[valveNum];
    }

    /** Open the valve.  This sets the valve to the position indicated by 
	getFunctionMap(OPEN_FUNC) */
    public void open() throws IOException
    {
	try {
	    setPosition(_funcMap[OPEN_FUNC]);
	} catch (RangeException e) {
	    _log4j.error("Bad position value in _funcMap for open(), value = " +
			 _funcMap[OPEN_FUNC] + " : " + e);
	    throw new IOException("Internal error in ValveControl");
	}
    }

    /** Close the valve.  This sets the valve to the position indicated by
	getFunctionMap(CLOSE_FUNC) */
    public void close() throws IOException
    {
	try {
	    setPosition(_funcMap[CLOSE_FUNC]);
	} catch (RangeException e) {
	    _log4j.error("Bad position value in _funcMap for close(), value = " +
			 _funcMap[CLOSE_FUNC] + " : " + e);
	    throw new IOException("Internal error in ValveControl");
	}
    }

    /** Get current position of valve.  This MAY perform I/O to the valve
	to query the position */
    public int  getPosition() throws IOException
    {
	int inputs = _digitalIO.readInputs();
	int position;

	if ((inputs & _feedback1Vector) != 0)
	    position = ((inputs & _feedback2Vector) != 0) ? POSITION_CENTER : POSITION_RIGHT;
	else
	    position = ((inputs & _feedback2Vector) != 0) ? POSITION_LEFT : POSITION_MOVING;
	if (_log4j.isDebugEnabled())
	    _log4j.debug("getPosition returning " + position);
	return(position);
    }

    /** Determine whether valve is in an open position.
     * Both POSITION_LEFT and POSITION_RIGHT are considered open.
     */
    public boolean isOpen() throws IOException
    {
	int position = getPosition();
	return((position == POSITION_LEFT) || (position == POSITION_RIGHT));
    }

    /** Determine whether valve is in a closed position.
     * Both POSITION_CENTER and POSITION_BACK are considered closed.
     */
    public boolean isClosed() throws IOException
    {
	int position = getPosition();
	return((position == POSITION_CENTER) || (position == POSITION_BACK));
    }

    /** Internal routine to wait for valve to reach position */
    protected boolean waitForPosition(int position, long timeout)
    {
	long	startTime = System.currentTimeMillis();
	int	curPos;

	do {
	    try {
		Thread.sleep(500);

		curPos = getPosition();

		if (_log4j.isDebugEnabled())
		    _log4j.debug("Waiting for position " + position +
				 ", got " + curPos);

		if (curPos == position)
		    return(true);

	    } catch (Exception e) {
		_log4j.warn("Exception in waitForPosition(): " + e);
	    }
	
	} while (System.currentTimeMillis() < (startTime + timeout));

	return(false);
    }


    /** Explicitly set valve position	*/
    public void setPosition(int position) throws RangeException, IOException
    {
	int	outBits;
	int	bitMask = ((1 << _cmd1Bit) | (1 << _cmd2Bit));

	switch(position)
	{
	  case POSITION_LEFT:
	      outBits = (1 << _cmd1Bit);
	      break;

	  case POSITION_RIGHT:
	      outBits = (1 << _cmd2Bit);
	      break;

	  case POSITION_CENTER:
	      outBits = ((1 << _cmd1Bit) | (1 << _cmd2Bit));
	      break;

	  default:
	      throw new RangeException("Position=" + position + " not supported");
	}

	for (int i = 0; i < _attributes.retries; i++)
	{
	    /* Although each operation is synchronized, we need to grab	*/
	    /* the lock for this entire operation, so someone doesn't	*/
	    /* read the position while we're moving the valve.		*/
	    synchronized(_digitalIO)
	    {
		/* Command valve to new position */
		_digitalIO.setOutputBits(outBits, bitMask);

		if (waitForPosition(position, _attributes.timeoutMS))
		    return;

		/* Toggle the bits for 10 ms, per the Hanbay doc 		*/
		/* Note - did this cause the capacitor to fry?  Deleting for now */
//		_digitalIO.setOutputBits(outBits ^ _cmd1Bit, bitMask);
//	        try {
//		    Thread.sleep(10);
//	        } catch (Exception e) {
//	        }
//	        _digitalIO.setOutputBits(outBits, bitMask);
	    }
	}

	throw new IOException("Timeout waiting for valve to reach " + 
			      _attributes.valvePositions[position] + " position");
    }

    /** Set the meaning of a function (open() or close(), in terms of valve position
	@param function OPEN_FUNC or CLOSE_FUNC, function to map
	@param position Position this function maps to
	Note that isOpen() and isClosed() are hard wired, and ignore the positions
	mapped by this function.
     */
    public void setFunctionMap(int function, int position)
	throws RangeException, IOException
    {
	if ((function < Valve2WayIF.OPEN_FUNC) || (function > Valve2WayIF.CLOSE_FUNC) ||
	    (position < ValveIF.POSITION_LEFT) || (position >= ValveIF.POSITION_BACK))
	    throw new RangeException("function or position out of range");

	_funcMap[function] = position;
    }

    /** Get the meaning of a function (open() or close(), in terms of valve position */
    public int  getFunctionMap(int function) throws RangeException, IOException
    {
	if ((function < Valve2WayIF.OPEN_FUNC) || (function > Valve2WayIF.CLOSE_FUNC))
	    throw new RangeException("function out of range");

	return(_funcMap[function]);
    }

    /** Get the String name for the given valve position */
    public String getPositionName(int position) throws RemoteException
    {
	if ((position >= 0) && (position < _attributes.valvePositions.length))
	    return(_attributes.valvePositions[position]);

	return("Unknown");
    }

    /** Parse the given String name into a valve position */
    public int parsePositionName(String name) 
	throws IllegalArgumentException, RemoteException
    {
	try {
	    int pos = Integer.parseInt(name);
	    if ((pos < POSITION_LEFT) || (pos > POSITION_BACK))
		throw new IllegalArgumentException("Position number out of range");
	    return(pos);
	} catch (NumberFormatException e) {
	}

	for (int i = 0; i < _attributes.valvePositions.length; i++)
	    if (name.equalsIgnoreCase(_attributes.valvePositions[i]))
		return(i);

	if (name.equalsIgnoreCase("OPEN"))
	    return(_attributes._openpos[_valveNum]);

	if (name.equalsIgnoreCase("CLOSED") || name.equalsIgnoreCase("CLOSE"))
	    return(_attributes._closedpos[_valveNum]);

	throw new IllegalArgumentException("Not a valid valve position: " + name);
    }

} /* class ValveControl */

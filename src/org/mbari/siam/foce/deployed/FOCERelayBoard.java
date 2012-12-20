// Copyright MBARI 2003
package org.mbari.siam.foce.deployed;

import org.mbari.siam.core.*;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;


/** FOCERelayBoard encapsulates one Real-Time Devices DM6952HR PC/104 Relay Board */
public class FOCERelayBoard
{
    static Logger _log4j = Logger.getLogger(FOCERelayBoard.class);

    protected static final int	RELAYS_PER_BOARD = 16;
    protected static final int	DEFAULT_BOARD_ADDR = 0x310;
    protected static final String RELAY_INIT="relayInit ";
    protected static final String RELAY_ON="relayOn ";
    protected static final String RELAY_OFF="relayOff ";
    protected static final String RELAY_STATE="relayState ";

    protected int	_boardAddress = DEFAULT_BOARD_ADDR;
    protected int	_boardIndex;
    protected IOMapper	_ioMapper;

    /** Constructor
     *  @param address Address in I/O space for this board.
     */
    public FOCERelayBoard(int address) throws IOException
    {
	_boardAddress = address;
	_ioMapper = IOMapper.getInstance();
	String rtn =_ioMapper.transact(RELAY_INIT + 
				       Integer.toHexString(address) + "\n");
	try {
	    _boardIndex = Integer.parseInt(rtn);
	} catch (NumberFormatException ne) {
	    throw new IOException("Unexpected return from relayInit: " + rtn);
	}
    }

    /** Creates FOCERelayBoard at default address
     */
    public FOCERelayBoard()
	throws IOException
    {
	this(DEFAULT_BOARD_ADDR);
    }

    /** Power on a single relay */
    public void powerOnBit(int bit) throws IOException
    {
	_ioMapper.transact(RELAY_ON + _boardIndex + " " +
			   Integer.toHexString(1 << bit) + "\n");
    }

    /** Power off a single relay */
    public void powerOffBit(int bit) throws IOException
    {
	_ioMapper.transact(RELAY_OFF + _boardIndex + " " +
			   Integer.toHexString(1 << bit) + "\n");
    }

    /** Power off all relays */
    public void powerOnAll() throws IOException
    {
	_ioMapper.transact(RELAY_ON + _boardIndex + " " +
			   Integer.toHexString((1 << RELAYS_PER_BOARD) - 1)
			   + "\n");
    }

    /** Power off all relays */
    public void powerOffAll() throws IOException
    {
	_ioMapper.transact(RELAY_OFF + _boardIndex + " " +
			   Integer.toHexString((1 << RELAYS_PER_BOARD) - 1)
			   + "\n");
    }

    /** Power on a multiple relays */
    public void powerOnBits(int bit, int numBits)
	throws IOException
    {
	_ioMapper.transact(RELAY_ON + _boardIndex + " " +
			   Integer.toHexString(((1 << numBits) - 1) << bit)
			   + "\n");
    }	

    /** Power off a multiple relays */
    public void powerOffBits(int bit, int numBits)
	throws IOException
    {
	_ioMapper.transact(RELAY_OFF + _boardIndex + " " +
			   Integer.toHexString(((1 << numBits) - 1) << bit)
			   + "\n");
    }

    /** Return state of relays in relay board */
    public int relayState() throws IOException, NumberFormatException
    {
	return(Integer.parseInt(_ioMapper.transact(RELAY_STATE + _boardIndex + "\n")));
    }

    /** Close this board.  This closes the underlying IOMapper */
    public void close() throws IOException
    {	
	_ioMapper.close();
	_ioMapper = null;
    }

    /** Power off a multiple relays */
    public int numRelays()
    {
	return(RELAYS_PER_BOARD);
    }
    
    /** Return name. */
    public String getName()
    {
	return("FOCERelayBoard at 0x" + Integer.toHexString(_boardAddress));
    }
}

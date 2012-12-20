// Copyright MBARI 2003
package org.mbari.siam.foce.deployed;

import org.mbari.siam.core.*;
import org.mbari.siam.distributed.devices.AnalogBoard;
import org.mbari.siam.distributed.devices.DigitalInputBoard;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;


/** FOCEAnalogBoard encapsulates one Diamond Systems DMM-32X-AT Data acquisition board */
public class FOCEAnalogBoard implements AnalogBoard, DigitalInputBoard
{
    static Logger _log4j = Logger.getLogger(FOCEAnalogBoard.class);

    protected static final int	CHANS_PER_BOARD = 32;
    protected static final int	DEFAULT_BOARD_ADDR = 0x300;
    protected static final int	DEFAULT_BOARD_INT = 7;
    protected static final String ANALOG_INIT="analogInit ";
    protected static final String AD_SETUP = "analogSetup ";
    protected static final String AD_SAMPLE = "analogSample ";
    protected static final String AD_SCAN = "analogScan ";
    protected static final String READ_DIO = "readDIOByte ";

    protected int	_boardAddress = DEFAULT_BOARD_ADDR;
    protected int	_boardIndex;
    protected IOMapper	_ioMapper;

    /** Constructor
     *  @param address Address in I/O space for this board.
     *  @param intVector Interrupt vector number for this board (default 7).
     */
    public FOCEAnalogBoard(int address, int intVector) throws IOException
    {
	_boardAddress = address;
	_ioMapper = IOMapper.getInstance();
	String rtn =_ioMapper.transact(ANALOG_INIT + 
				       Integer.toHexString(address) + " "
				       + intVector + "\n");
	try {
	    _boardIndex = Integer.parseInt(rtn);
	} catch (NumberFormatException ne) {
	    throw new IOException("Unexpected return from analogInit: " + rtn);
	}
    }

    /** Creates FOCEAnalogBoard with default interrupt vector.
     *  @param address Address in I/O space for this board.
     */
    public FOCEAnalogBoard(int address)
	throws IOException
    {
	this(address, DEFAULT_BOARD_INT);
    }

    /** Creates FOCEAnalogBoard at default address and interrupt vector.
     */
    public FOCEAnalogBoard()
	throws IOException
    {
	this(DEFAULT_BOARD_ADDR, DEFAULT_BOARD_INT);
    }

    /** Set up one or more analog channels */
    public void analogSetup(int chan, int range, int polarity, int gain)
	throws IOException
    {
	if (chan >= CHANS_PER_BOARD)
	    throw new IOException("A/D Channel number out of range!");

	if ((range != 5) && (range != 10))
	    throw new IOException("Range must be 5 or 10 volts!");

	if ((gain != 1) && (gain != 2) && (gain != 4) && (gain != 8))
	    throw new IOException("Gain must be one of 1, 2, 4, or 8!");

	_ioMapper.transact(AD_SETUP + _boardIndex + " " + chan + " " +
			   range + " " + polarity + " " + gain + "\n");
    }

    /** Convert one A/D channel to voltage */
    public double analogSample(int chan)
	throws IOException, NumberFormatException
    {
	if (chan >= CHANS_PER_BOARD)
	    throw new IOException("A/D Channel number out of range!");

	return(Double.parseDouble(_ioMapper.transact(AD_SAMPLE + _boardIndex + 
						     " " + chan + "\n")));
    }


    /** Convert multiple A/D channels to voltages */
    public double[] analogScan(int chan, int nchans)
	throws IOException, NumberFormatException
    {
	if (chan + nchans > CHANS_PER_BOARD)
	    throw new IOException("A/D Channel number out of range!");

	String result = _ioMapper.transact(AD_SCAN + _boardIndex + 
					   " " + chan + " " + nchans + "\n");

	double[] rtnval = new double[nchans];
	StringTokenizer st = new StringTokenizer(result, " ,\t\n\r");

	for (int i = 0; i < nchans; i++)
	{
	    try {
		rtnval[i] = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		_log4j.error("Cannot parse result of analogScan: " + result);
		throw new IOException("Cannot parse result of analogScan: " + result);
	    }
	}

	return(rtnval);
    }


    public int readDIO(int port) throws IOException, NumberFormatException
    {
	String result = _ioMapper.transact(READ_DIO + _boardIndex + " " + port + "\n");

	return(Integer.parseInt(result.trim()));
    }


    /** Close this board.  This closes the underlying IOMapper */
    public void close() throws IOException
    {	
	_ioMapper.close();
	_ioMapper = null;
    }

    /** Return number of channels per board */
    public int numChans()
    {
	return(CHANS_PER_BOARD);
    }
    
    /** Return name. */
    public String getName()
    {
	return("FOCEAnalogBoard at 0x" + Integer.toHexString(_boardAddress));
    }
}

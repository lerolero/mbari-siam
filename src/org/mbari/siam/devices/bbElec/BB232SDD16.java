/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.bbElec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import gnu.io.SerialPort;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.utils.StreamUtils;

/** Class to encapsulate operations to 232SDD16 RS-232 Digital I/O module
    from B&B Electronics
 */
/*
 $Id: BB232SDD16.java,v 1.4 2012/12/17 21:33:25 oreilly Exp $
 $Name: HEAD $
 $Revision: 1.4 $
 */

public class BB232SDD16 extends BBElec
{
    // CVS revision
    private static String _versionID = "$Revision: 1.4 $";

    protected static byte[] SET_OUTPUT = "!0SO".getBytes();
    protected static byte[] READ_INPUT = "!0RD".getBytes();
    protected static byte[] READ_CONFIG = "!0RC".getBytes();
    protected static byte[] DEFINE_IO = "!0SD".getBytes();
    protected static byte[] SET_PWRUP = "!0SS".getBytes();

    protected int _outputState = 0;

    /** Constructor
	@param serport Serial port to use for device
	@param tmout Read timeout in ms
     */	
    public BB232SDD16(SerialPort serport, long tmout) throws IOException
    {
	super(serport, tmout);
    }

    /** Constructor
	@param serport Serial port to use for device
     */	
    public BB232SDD16(SerialPort serport) throws IOException
    {
	super(serport, DFLT_TMOUT);
    }

    /** Define I/O lines
	@param outputs Bit vector, '1' defines output bit, '0' defines input bit
    */
    public void defineIO(int outputs) throws IOException
    {
	send16bitCmd(DEFINE_IO, outputs);
    }

    /** Set Output lines
	@param newState State of output lines to set
    */
    public void setOutput(int newState) throws IOException
    {
	send16bitCmd(SET_OUTPUT, newState);
	_outputState = newState;
    }

    /** Set a subset of output bits
	@param newBits  State of output lines to set
	@param bitMask Mask for which bits to set
    */
    public void setOutputBits(int newBits, int bitMask) throws IOException
    {
	setOutput((_outputState & ~bitMask) | (newBits & bitMask));
    }

    /** Set Power-up state
	@param pwrupState State of output lines when device powers up
    */
    public void setPwrupState(int pwrupState) throws IOException
    {
	send16bitCmd(SET_PWRUP, pwrupState);
    }

    /** Read Input Bits
    */
    public int readInputs() throws IOException
    {
	return(cmdWith16bitReply(READ_INPUT));
    }

    /** Read Configuration
	@return I/O Definition in bits 31:16, Powerup state in bits 15:0
    */
    public int readConfiguration() throws IOException
    {
	return(cmdWith32bitReply(READ_CONFIG));
    }

} /* class BB232SDD16 */

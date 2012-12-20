/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.elmo.base;

import java.util.Vector;
import java.util.Iterator;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.distributed.TimeoutException;

/** Elmo Solo Motor controller hardware abstraction.
    Used by SIAM instrument services to access Elmo
    motor controller
 *  
 */
/*
  $Id: ElmoSolo.java,v 1.13 2010/08/04 05:32:03 headley Exp $
  $Name:  $
  $Revision: 1.13 $
*/

public class ElmoSolo extends Elmo{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(ElmoSolo.class);

    /** Constructor */
    public ElmoSolo(SerialPort port) throws IOException 
    {
		super(port);
    }

    /** Initialize motor controller. 
	 - set serial mode
	 - disables echo
  	 - disable hex mode
	 - stops motor
	 - sets unit mode=MODE_SPEED
	 - uses reference speed limits set in flash
	  (for FOCE: VL[2]=-340 VH[2]=340 LL[2]=-1080 HL[2]=1080)
	 - flushes the serial port input buffer
	 @param serialMode serial comms mode (rs232 or RFC1722)
	 @param unitMode elmo unit mode
	 */
    public void initializeController(int serialMode,int unitMode ) 
	throws TimeoutException, IOException,Exception
    {
		// set serial mode remote/local
		setSerialMode(serialMode);
		// EO=0
		setEchoMode(ECHO_DISABLED);
		// HX=0
		setHexMode(false);
		// ST
		stopMotor();
		// MO=0
		//disableMotor();
		setEnable(false,Elmo.TM_CMD_MSEC);
		// set UM=2
		setUnitMode(unitMode);//MODE_SPEED
		// read all characters in serial port queue
		emptyInput(_serialRx,Elmo.TM_EMPTY_INPUT_MSEC);
	}

} // end of class

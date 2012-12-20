/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.analog;

import java.util.Vector;
import java.util.Iterator;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.AnalogInstrumentPort;
import org.mbari.siam.core.ServiceSandBox;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.foce.deployed.FOCEAnalogBoard;
import org.mbari.siam.foce.deployed.FOCESensorayBoard;

import org.mbari.siam.distributed.devices.AnalogBoard;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;

/**
 * Analog instrument using the Sensoray 518 data acquisition board.
 * For FOCE, it is used to monitor enclosure temperature and humidity.
 */
/*
  $Id: Sensoray518.java,v 1.9 2012/12/17 21:36:31 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.9 $
*/

//public class Sensoray518 extends AnalogInstrument implements Instrument
public class Sensoray518 extends ScaledAnalog implements Instrument
{
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(Sensoray518.class);

    public Sensoray518() throws RemoteException {
	super();

	/* instantiate base class _attributes member here rather
	   than declaring
	    SensorayAttributes _attributes = new SensorayAttributes(this);
	   since this creates a separate instance of 
	   _attributes of a different type.
	   Whenever it used by this class, it needs to be 
	   cast as type SensorayAttributes to access
	   SensorayAttribute members.
	 */
	_attributes = new SensorayAttributes(this);
    }


    /** Return metadata. */
    protected byte[] getInstrumentStateMetadata()
    {
	//byte[] base=super.getInstrumentStateMetadata();
	
	StringBuffer sb = new StringBuffer();
	byte[] baseMD=super.getInstrumentStateMetadata();
	
	try{
	    sb.append(new String(baseMD));
	    sb.append("Name:");
	    sb.append(_analogBoard.getName()+"\n");
	    sb.append("Firmware Version: ");
	    sb.append(((FOCESensorayBoard)_analogBoard).getFirmwareVersion()+"\n");
	    sb.append("Product ID: ");
	    sb.append(((FOCESensorayBoard)_analogBoard).getProductID()+"\n");
	    sb.append("Board Temperature: ");
	    sb.append(((FOCESensorayBoard)_analogBoard).getBoardTemp()+"\n");
	    sb.append("Channel Configuration\n");
	    sb.append(((FOCESensorayBoard)_analogBoard).readConfig()+"\n");
	}catch(IOException e){
	    e.printStackTrace();
	}
	return(sb.toString().getBytes());
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	// Sample every 30 min by default
	return new ScheduleSpecifier(1800000);
    }

    /** Attributes for general Sensoray Analog Instrument
     * @author Kent Headley
     */
    class SensorayAttributes extends ScaledAnalog.ScaledAnalogAttributes
    {
	SensorayAttributes(DeviceServiceIF service) {
	    super(service);
	}

	public String registryName = "Enclosure";

        /**
         * Throw InvalidPropertyException if any invalid attribute values found
         */
        public void checkValues() throws InvalidPropertyException {
	    super.checkValues();
	}
    }

} // end of class

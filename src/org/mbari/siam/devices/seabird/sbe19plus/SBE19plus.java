/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.sbe19plus;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.devices.seabird.base.Seabird;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.devices.seabird.eventDetector.TurbidityEventDetector;
import org.mbari.siam.devices.seabird.sbe16plus.SBE16plus;
import org.mbari.siam.distributed.TimeoutException;

import java.io.IOException;
import java.rmi.RemoteException;

public class SBE19plus extends SBE16plus
        implements Instrument, Safeable
{
    static private Logger _log4j = Logger.getLogger(SBE19plus.class);
    protected byte[] cmdbuf = new byte[1024];

    /**
     * Constructor; can throw RemoteException.
     */
    public SBE19plus() throws RemoteException {
        super();
    }

   protected void initializeInstrument() throws Exception
    {
	super.initializeInstrument();
	_fromDevice.flush();
	getPrompt(3);

	sendCommandConfirm("MM\r");

	sendCommand("MooredPumpMode=" + ((Attributes)_attributes).pumpMode + "\r");

	sendCommand("DelayBeforeSampling=" + ((Attributes )_attributes).delayBeforeSamplingSec + "\r");
    }

    /** Method to send a command for which the SBE-19 requires confirmation.  Unfortunately, there appears to
     * be two different modes of confirmation, depending on the firmware in the SBE-19.  You either need to
     * send the command a second time, or you need to confirm with a 'Y'.  This method tries to parse the
     * prompt and do the right thing.
     */
    public void sendCommandConfirm(String cmd)
	throws TimeoutException, NullPointerException, IOException, Exception 
    {
        _log4j.debug("Sending cmd: " + cmd);
        _toDevice.write(cmd.getBytes());
	_toDevice.flush();
	Thread.sleep(1000);

	int nbytes = _fromDevice.available();
	if (nbytes > cmdbuf.length)
	    nbytes = cmdbuf.length;
	_fromDevice.read(cmdbuf, 0, nbytes);
	String response = new String(cmdbuf, 0, nbytes);
	_fromDevice.flush();
	_log4j.debug("Response: " + response);

	if (response.indexOf("Y/N") >= 0)
	{
	    _log4j.debug("Writing \"Y\"");
	    _toDevice.write("Y\r".getBytes());
	    _toDevice.flush();
	    StreamUtils.skipUntil(_fromDevice, getPromptString(), 5000);
	}
	else if (response.indexOf("repeat") >= 0)
	{
	    _log4j.debug("Repeating: " + cmd);
	    _toDevice.write(cmd.getBytes());
	    _toDevice.flush();
	    StreamUtils.skipUntil(_fromDevice, getPromptString(), 5000);
	}
	else
	{
	    _log4j.warn("Did not receive expected confirmation prompt from command: " + cmd);
	    getPrompt(3);
	}
	
    }

} // end of class

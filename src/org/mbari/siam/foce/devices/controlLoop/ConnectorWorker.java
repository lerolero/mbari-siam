/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import com.rbnb.sapi.*;

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.foce.devices.controlLoop.*;
import org.mbari.siam.distributed.devices.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/* InputConnector worker thread base class
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public abstract class ConnectorWorker extends WorkerThread  {
	
    static protected Logger _log4j = Logger.getLogger(ConnectorWorker.class);  
	
	InputConnector _connector;	
	
	public ConnectorWorker(InputConnector connector, long updatePeriodMillisec){
		super(updatePeriodMillisec);
		_connector=connector;
	}
	
	
}
/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.osdt;

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
import org.mbari.siam.tests.utils.osdt.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/* OSDT Test Worker thread creates and runs and OSDT data channel
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public class OSDTTestWorker extends WorkerThread  {
	
    static protected Logger _log4j = Logger.getLogger(OSDTTestWorker.class);  
	OSDTTestServer.ChannelSpec _channel;
	Source _source;
	ChannelMap _channelMap;

	public OSDTTestWorker( Source source, OSDTTestServer.ChannelSpec channel)
	throws SAPIException{
		super(channel._periodMillisec);
		_source=source;
		_channel=channel;
		_channelMap=new ChannelMap();
		_channelMap.Add(_channel._name);
	}

	public void doWorkerAction(){
		try{
			//_log4j.debug("channel "+_channel._name+" doing action");
			//_channelMap=new ChannelMap();
			//_channelMap.Add(_channel._name);
			_channelMap.PutTimeAuto("timeofday");
			switch(_channel._typeID){
				case ChannelMap.TYPE_FLOAT64:
					double[] doubleData={_channel.doubleValue()};
					_channelMap.PutDataAsFloat64(0,doubleData);
					break;
				case ChannelMap.TYPE_FLOAT32:
					float[] floatData={_channel.floatValue()};
					_channelMap.PutDataAsFloat32(0,floatData);
					break;
				case ChannelMap.TYPE_INT64:
					long[] longData={_channel.longValue()};
					_channelMap.PutDataAsInt64(0,longData);
					break;
				case ChannelMap.TYPE_INT32:
					int[] intData={_channel.intValue()};
					_channelMap.PutDataAsInt32(0,intData);
					break;
				case ChannelMap.TYPE_INT16:
					short[] shortData={_channel.shortValue()};
					_channelMap.PutDataAsInt16(0,shortData);
					break;
				case ChannelMap.TYPE_INT8:
					byte[] byteData={_channel.byteValue()};
					_channelMap.PutDataAsInt8(0,byteData);
					break;
				case ChannelMap.TYPE_STRING:
					String stringData=_channel.stringValue();
					_channelMap.PutDataAsString(0,stringData);
					break;
				default:
					break;
					
			}
			_log4j.debug("flushing value ["+_channel+"]");
			_source.Flush(_channelMap);
		}catch (Exception e) {
			_log4j.error(e);
		}				
		return;
	}
}
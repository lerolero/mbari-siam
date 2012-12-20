// Copyright 2002 MBARI
package org.mbari.siam.tests.serialization;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Enumeration;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.operations.utils.ExportablePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.DevicePacketOutputStream;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SensorDataPacket;
//import org.mbari.isi.interfaces.ExternalizableSensorDataPacket;
//import org.mbari.siam.distributed.SensorDataPacket2;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SummaryPacket;

import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.NoDataException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;

public class TestLogger{


    public static void main(String args[]) {

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

	long aaaa=0xaaaaaaaaaaaaaaaaL;
	long bbbb=0xbbbbbbbbbbbbbbbbL;
	long cccc=0xccccccccccccccccL;
	long dddd=0xddddddddddddddddL;
	long eeee=0xeeeeeeeeeeeeeeeeL;
	long parentID=44L;
	long recordType=0L;

	try{
	    long dfltID=1000L;
	    long exID=1750L;

	    DeviceLog dlSDP=new DeviceLog(dfltID,"./");


	    for(int i=0;i<20;i++){
		if(i==0 || i==11 || i==17){
		    System.out.println("Preparing MetadataPacket id= "+dfltID);
		    String cause=("cause_"+i);
		    String payload=("Metadata_"+i);
		    MetadataPacket mdp=new MetadataPacket(dfltID,cause.getBytes(),payload.getBytes());
		    mdp.setSystemTime(System.currentTimeMillis()+(i*1000*600));
		    mdp.setSequenceNo(0L+(long)i);
		    mdp.setMetadataRef(0L);
		    mdp.setParentId(parentID);
		    mdp.setRecordType(recordType);

		    System.out.println("Appending to log "+dfltID+" using Serializable interface");
		    dlSDP.appendPacket(mdp);

		}else{
		    System.out.println("Preparing SensorDataPacket id= "+dfltID+" i="+i);
		    SensorDataPacket sdp=new SensorDataPacket(dfltID,64);
		    sdp.setSystemTime(System.currentTimeMillis()+(i*1000*600));
		    sdp.setSequenceNo(0L+(long)i);
		    if(i%3==0)
			sdp.setParentId(parentID+i);
		    else
			sdp.setParentId(parentID);
		    sdp.setRecordType(recordType);
		    String foo=("SensorData_"+i);
		    sdp.setDataBuffer(foo.getBytes());

		    System.out.println("Appending to log "+dfltID+" using Serializable interface");
		    dlSDP.appendPacket(sdp);
		}
	    }

	}catch(FileNotFoundException e){
	    e.printStackTrace();
	}catch(IOException e){
	    e.printStackTrace();
	}
    }
}

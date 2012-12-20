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
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
// packets with alternative serialization using
// Externalizeable
/*
import org.mbari.siam.core.DeviceLog2;
import org.mbari.siam.distributed.DevicePacket2;
import org.mbari.siam.distributed.DeviceMessagePacket2;
import org.mbari.siam.distributed.SensorDataPacket2;
import org.mbari.siam.distributed.MetadataPacket2;
*/

import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.DevicePacketOutputStream;
import org.mbari.siam.distributed.SummaryPacket;

import org.mbari.siam.distributed.NoDataException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;

public class PacketSerializationTest2{


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

	try{
	    long dfltID=1000L;
	    long exID=1750L;

	    System.out.println("Preparing SensorDataPacket id= "+dfltID);
	    SensorDataPacket sdp=new SensorDataPacket(dfltID,64);
	    sdp.setSystemTime(System.currentTimeMillis());
	    sdp.setSequenceNo(0L);
	    sdp.setMetadataRef(cccc);
	    sdp.setParentId(dddd);
	    sdp.setRecordType(eeee);
	    sdp.setDataBuffer("SensorData_0123".getBytes());

	    System.out.println("Appending to log "+dfltID+" using Serializable interface");
	    DeviceLog dlSDP=new DeviceLog(dfltID,"./");
	    dlSDP.appendPacket(sdp);

	    System.out.println("Serializing single packet to file exportSDP using ExportablePacket");
	    FileOutputStream fos=new FileOutputStream("./exportSDP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(sdp);
	    ep.export(dos);
	    dos.close();
	    fos.close();

	    System.out.println("Serializing single packet to file exportSDP using default serialization ");
	    fos=new FileOutputStream("./exportJSDP");
	    ObjectOutputStream oos=new ObjectOutputStream(fos);
	    oos.writeObject(sdp);
	    oos.close();
	    fos.close();
/*
	    System.out.println("Preparing ExternalizableSensorDataPacket id= "+exID);
	    SensorDataPacket2 esdp=new SensorDataPacket2(exID,64);
	    esdp.setSystemTime(System.currentTimeMillis());
	    esdp.setSequenceNo(0L);
	    esdp.setMetadataRef(cccc);
	    esdp.setParentId(dddd);
	    esdp.setRecordType(eeee);
	    esdp.setDataBuffer("ExSensorData_01".getBytes());
	    DeviceLog2 dlESDP=new DeviceLog2(exID,"./");

	    System.out.println("Appending to log "+exID+" using Externalizable interface");
	    dlESDP.appendPacket(esdp);

	    System.out.println("Serializing single packet to file exportESDP using Externalizable");
	    fos=new FileOutputStream("./exportESDP");
	    oos=new ObjectOutputStream(fos);
	    oos.writeObject(esdp);
	    oos.close();
	    fos.close();
*/
	    System.out.println("retrieving serialized packets: \n");
	    DevicePacketSet packetSet1=dlSDP.getPackets(0L,System.currentTimeMillis(),10);
	    int i=0;
	    for(Enumeration e=packetSet1._packets.elements();e.hasMoreElements();)
		System.out.println("packet "+ (i++) +":"+e.nextElement());
/*		
	    System.out.println("retrieving externalized packets: \n");
	    DevicePacketSet packetSet2=dlESDP.getPackets(0L,System.currentTimeMillis(),10);
	    i=0;
	    for(Enumeration e=packetSet2._packets.elements();e.hasMoreElements();)
		System.out.println("packet "+ (i++) +":"+(SensorDataPacket2)e.nextElement());
*/

	    /*
	    DeviceLog dlSDP2=new DeviceLog(1500L,"./");
	    dlSDP2.appendPacket(sdp2,false,false);
	    DeviceLog dlDMP=new DeviceLog(2000L,"./");
	    dlDMP.appendPacket(dmp,false,false);
	    DeviceLog dlMDP=new DeviceLog(3000L,"./");
	    dlMDP.appendPacket(mdp,false,false);
	    DeviceLog dlSP=new DeviceLog(4000L,"./");
	    dlSP.appendPacket(sp,false,false);
	    */
	}catch(FileNotFoundException e){
	    e.printStackTrace();
	}catch(IOException e){
	    e.printStackTrace();
	}catch(NoDataException e){
	    e.printStackTrace();
	}

	/*
	try{
	    FileOutputStream fos=new FileOutputStream("./exportSDP2");
	    ObjectOutputStream oos=new ObjectOutputStream(fos);
	    oos.writeObject(sdp2);
	}catch(IOException e){e.printStackTrace();}
	try{
	    FileOutputStream fos=new FileOutputStream("./exportMDP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(mdp);
	    ep.export(dos);
	}catch(IOException e){e.printStackTrace();}
	try{
	    FileOutputStream fos=new FileOutputStream("./exportDMP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(dmp);
	    ep.export(dos);
	}catch(IOException e){e.printStackTrace();}
	try{
	    FileOutputStream fos=new FileOutputStream("./exportSP");
	    DataOutputStream dos=new DataOutputStream(fos);
	    ExportablePacket ep=new ExportablePacket();
	    ep.wrapPacket(sp);
	    ep.export(dos);
	}catch(IOException e){e.printStackTrace();}
	
	Vector packetSet=new Vector();
	// write an empty packet set
	try{
	    DevicePacketSet ps=new DevicePacketSet(packetSet,true);
	    FileOutputStream fos=new FileOutputStream("./exportSDPacketSet_0");
	    ObjectOutputStream oos=new ObjectOutputStream(fos);
	    oos.writeObject(ps);
	}catch(IOException e){e.printStackTrace();}

	// write a packet set of 10 packets
	for(int i=0;i<10;i++){
	    sdp=new SensorDataPacket(1000,64);
	    now=System.currentTimeMillis();
	    sdp.setSystemTime(now);
	    sdp.setSequenceNo((100L+i));
	    sdp.setMetadataRef(0L);
	    sdp.setParentId(parentID);
	    sdp.setRecordType(1L);
	    sdp.setDataBuffer(("SensorData_01234").getBytes());
	    packetSet.add(sdp);
	}
	try{
	    DevicePacketSet ps=new DevicePacketSet(packetSet,true);
	    FileOutputStream fos=new FileOutputStream("./exportSDPacketSet_10");
	    ObjectOutputStream oos=new ObjectOutputStream(fos);
	    oos.writeObject(ps);
	}catch(IOException e){e.printStackTrace();}

	// write a packet set of 100 packets
	for(int i=0;i<100;i++){
	    sdp=new SensorDataPacket(1000,64);
	    now=System.currentTimeMillis();
	    sdp.setSystemTime(now);
	    sdp.setSequenceNo((100L+i));
	    sdp.setMetadataRef(0L);
	    sdp.setParentId(parentID);
	    sdp.setRecordType(1L);
	    sdp.setDataBuffer(("SensorData_01234").getBytes());
	    packetSet.add(sdp);
	}
	try{
	    DevicePacketSet ps=new DevicePacketSet(packetSet,true);
	    FileOutputStream fos=new FileOutputStream("./exportSDPacketSet_100");
	    ObjectOutputStream oos=new ObjectOutputStream(fos);
	    oos.writeObject(ps);
	}catch(IOException e){e.printStackTrace();}
	*/
    }
}

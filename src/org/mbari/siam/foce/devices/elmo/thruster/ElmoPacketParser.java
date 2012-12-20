/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.elmo.thruster;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;

import org.mbari.siam.foce.devices.elmo.base.*;

/** Parses SIAM data packets */
/*
  $Id: ElmoPacketParser.java,v 1.9 2010/08/05 01:49:48 headley Exp $
  $Name:  $
  $Revision: 1.9 $
*/

public class ElmoPacketParser extends PacketParser {

    static private Logger _log4j = Logger.getLogger(ElmoPacketParser.class);

    private static final long serialVersionUID=1L;
	public ElmoPacketParser(){
		super();
	}
    public ElmoPacketParser(String registryName){
	super(registryName);
    }

    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException {

	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	String dataString = new String(((SensorDataPacket)packet).dataBuffer());

        StringTokenizer st = 
            new StringTokenizer(dataString,",");

	PacketParser.Field[] fields = new PacketParser.Field[10];
	try{
	    fields[0]=new PacketParser.Field("name",this._registryName,"string");
	    // data format: status,velocity,faults
	    fields[1] = new PacketParser.Field("encoderVelocity",new Integer(st.nextToken()),"counts/sec");
	    fields[2] = new PacketParser.Field("joggingVelocity",new Integer(st.nextToken()),"counts/sec");
	    Integer status=new Integer(Integer.parseInt(st.nextToken(),16));
	    fields[3] = new PacketParser.Field("statusRegister",status,"bit field (integer)");
	    Long fault=new Long(Long.parseLong(st.nextToken(),16));
	    fields[4] = new PacketParser.Field("faultRegister",fault,"bit field (long)");
		Integer tsState=new Integer(st.nextToken());
	    fields[5] = new PacketParser.Field("turnSensorState",ElmoImpl.getTSStateMnemonic(tsState.intValue()),"state");
	    fields[6] = new PacketParser.Field("turnSensorCount",new Long(st.nextToken()),"triggers");
	    fields[7] = new PacketParser.Field("turnSensorTime",new Long(st.nextToken()),"ms");
	    fields[8] = new PacketParser.Field("srString","\n"+ElmoPacketParser.getStatusString(status),"string");
	    fields[9] = new PacketParser.Field("mfString","\n"+ElmoPacketParser.getFailureString(fault),"string");
	}catch (Exception e){
	    _log4j.error("Can't parse Elmo record: " + dataString);
	    throw new ParseException(e.toString(),0);
	}

	return fields;
    }

    /** Format the Elmo status register bit field as a (human readable) String */
    public static String getStatusString(Integer status){
	return getStatusString(status.intValue());
    }

    public static String getStatusString(int status){
	StringBuffer sb=new StringBuffer();
	int SR_DRIVE_READ            = 0x00000001;
	int SR_STATUS                = 0x0000000E;
	int SR_MOTOR_ON              = 0x00000010;
	int SR_REFERENCE_MODE        = 0x00000020;
	int SR_MOTOR_FAILURE_LATCHED = 0x00000040;
	int SR_UNIT_MODE             = 0x00000380;
	int SR_GAIN_SCHEDULING_ON    = 0x00000400;
	int SR_HOMING_PROCESSING     = 0x00000800;
	int SR_PROGRAM_RUNNING       = 0x00001000;
	int SR_CURRENT_LIMIT_ON      = 0x00002000;
	int SR_MOTION_STATUS         = 0x0000C000;
	int SR_RECORDER_STATUS       = 0x00030000;
	int SR_DIGITAL_HALLS         = 0x07000000;
	int SR_CPU_STATUS            = 0x08000000;
	int SR_LIMIT_STOP            = 0x10000000;
	int SR_USER_PROGRAM_ERROR    = 0x20000000;

	int SR_OFFSET_DRIVE_READ            = 0;
	int SR_OFFSET_STATUS                = 1;
	int SR_OFFSET_MOTOR_ON              = 4;
	int SR_OFFSET_REFERENCE_MODE        = 5;
	int SR_OFFSET_MOTOR_FAILURE_LATCHED = 6;
	int SR_OFFSET_UNIT_MODE             = 7;
	int SR_OFFSET_GAIN_SCHEDULING_ON    = 10;
	int SR_OFFSET_HOMING_PROCESSING     = 11;
	int SR_OFFSET_PROGRAM_RUNNING       = 12;
	int SR_OFFSET_CURRENT_LIMIT_ON      = 13;
	int SR_OFFSET_MOTION_STATUS         = 14;
	int SR_OFFSET_RECORDER_STATUS       = 16;
	int SR_OFFSET_DIGITAL_HALLS         = 24;
	int SR_OFFSET_CPU_STATUS            = 27;
	int SR_OFFSET_LIMIT_STOP            = 28;
	int SR_OFFSET_USER_PROGRAM_ERROR    = 29;
	int field=0;
	int unit_mode=0;
	field=(status & SR_DRIVE_READ);
	field=field>>SR_OFFSET_DRIVE_READ;
	if(field==0){
	    sb.append("Drive Read: ok\n");
	}else{
	    sb.append("Drive Read: error\n");	    
	}
	field=(status & SR_STATUS);
	field=field>>SR_OFFSET_STATUS;
	switch(field){
	case 0:
	    sb.append("Drive Status: ok\n");	    
	    break;
	case 1:
	    sb.append("Drive Status: under voltage\n");	    
	    break;
	case 2:
	    sb.append("Drive Status: over voltage\n");	    
	    break;
	case 5:
	    sb.append("Drive Status: short circuit\n");	    
	    break;
	case 6:
	    sb.append("Drive Status: temperature\n");	    
	    break;
	default:
	    sb.append("Drive Status: undefined error ["+field+"]\n");	    
	    break;
	}
	field=(status & SR_MOTOR_ON);
	field=field>>SR_OFFSET_MOTOR_ON;
	if(field==0){
	    sb.append("Motor ON (MO): false\n");
	}else{
	    sb.append("Motor ON (MO): true\n");	    
	}
	field=(status & SR_REFERENCE_MODE);
	field=field>>SR_OFFSET_REFERENCE_MODE;
	if(field==0){
	    sb.append("Reference Mode (RM): interpreter/user program\n");
	}else{
	    sb.append("Reference Mode (RM): software/aux reference\n");	    
	}
	field=(status & SR_MOTOR_FAILURE_LATCHED);
	field=field>>SR_OFFSET_MOTOR_FAILURE_LATCHED;
	if(field==0){
	    sb.append("Motor Failure Latched: false\n");
	}else{
	    sb.append("Motor Failure Latched: true\n");	    
	}
	field=(status & SR_UNIT_MODE);
	field=field>>SR_OFFSET_UNIT_MODE;
	unit_mode=field;
	switch(field){
	case 1:
	    sb.append("Unit Mode: torque control\n");
	    break;
	case 2:
	    sb.append("Unit Mode: speed control\n");
	    break;
	case 3:
	    sb.append("Unit Mode: micro-stepper\n");
	    break;
	case 4:
	    sb.append("Unit Mode: dual feedback position control\n");
	    break;
	case 5:
	    sb.append("Unit Mode: single loop position\n");
	    break;
	default:
	    sb.append("Unit Mode: undefined ["+field+"]\n");
	    break;
	}
	field=(status & SR_GAIN_SCHEDULING_ON);
	field=field>>SR_OFFSET_GAIN_SCHEDULING_ON;
	if(field==0){
	    sb.append("Gain Scheduling ON: false\n");
	}else{
	    sb.append("Gain Scheduling ON: true\n");	    
	}
	field=(status & SR_HOMING_PROCESSING);
	field=field>>SR_OFFSET_HOMING_PROCESSING;
	if(field==0){
	    sb.append("Homing Processing: false\n");
	}else{
	    sb.append("Homing Processing: true\n");	    
	}
	field=(status & SR_PROGRAM_RUNNING);
	field=field>>SR_OFFSET_PROGRAM_RUNNING;
	if(field==0){
	    sb.append("Program Running: false\n");
	}else{
	    sb.append("Program Running: true\n");	    
	}
	field=(status & SR_CURRENT_LIMIT_ON);
	field=field>>SR_OFFSET_CURRENT_LIMIT_ON;
	if(field==0){
	    sb.append("Current Limit ON: false\n");
	}else{
	    sb.append("Current Limit ON: true\n");	    
	}
	field=(status & SR_MOTION_STATUS);
	field=field>>SR_OFFSET_MOTION_STATUS;
	switch(unit_mode){
	case 3:
	case 4:
	case 5:
	    switch(field){
	    case 0:
		sb.append("Motion Status (MS): position stabilized\n");
		break;
	    case 1:
		sb.append("Motion Status (MS): reference stationary/motor off\n");
		break;
	    case 2:
		sb.append("Motion Status (MS): dynamically controlled via PTP/Jog/PT/PVT\n");
		break;
	    default:
		sb.append("Motion Status (MS): N/A\n");
		break;
	    }
	    break;
	case 2:
	    switch(field){
	    case 1:
		sb.append("Motion Status (MS): reference == speed target\n");
		break;
	    case 2:
		sb.append("Motion Status (MS):reference != speed target\n");
		break;
	    default:
		sb.append("Motion Status (MS): N/A\n");
		break;
	    }
	    break;
	default:
	    sb.append("Motion Status (MS): N/A\n");
	    break;
	}
	field=(status & SR_RECORDER_STATUS);
	field=field>>SR_OFFSET_RECORDER_STATUS;
	switch(field){
	case 0:
	    sb.append("Recorder Status: inactive\n");
	    break;
	case 1:
	    sb.append("Recorder Status: waiting for trigger event\n");
	    break;
	case 2:
	    sb.append("Recorder Status: data ready\n");
	    break;
	case 3:
	    sb.append("Recorder Status: recording\n");
	    break;
	default:
	    sb.append("Recorder Status: unknown ["+field+"]\n");
	    break;
	}
	field=(status & SR_DIGITAL_HALLS);
	field=field>>SR_OFFSET_DIGITAL_HALLS;
	    sb.append("Digital Halls (ABC): ["+field+"]\n");
	field=(status & SR_CPU_STATUS);
	field=field>>SR_OFFSET_CPU_STATUS;
	if(field==0){
	    sb.append("CPU Status: ok\n");
	}else{
	    sb.append("CPU Status: overflow/exception\n");	    
	}
	field=(status & SR_OFFSET_LIMIT_STOP);
	field=field>>SR_OFFSET_LIMIT_STOP;
	if(field==0){
	    sb.append("Limit Stop: false\n");
	}else{
	    sb.append("Limit Stop: true\n");	    
	}
	field=(status & SR_USER_PROGRAM_ERROR);
	field=field>>SR_OFFSET_USER_PROGRAM_ERROR;
	if(field==0){
	    sb.append("User Program Error: false\n");
	}else{
	    sb.append("User Program Error: true\n");	    
	}

	return sb.toString();
    }
    /** Format the Elmo status register bit field as a (human readable) String */
    public static String getFailureString(Long failure){
	return getFailureString(failure.intValue());
    }

    public static String getFailureString(long failure){
	StringBuffer sb=new StringBuffer();
	final int MF_OK                = 0x0;
	final int MF_ANALOG_FEEBACK    = 0x1;
	final int MF_FEEDBACK_LOSS     = 0x4;
	final int MF_PEAK_CURRENT      = 0x8;
	final int MF_INHIBIT           = 0x10;
	final int MF_MULT_HALL         = 0x40;
	final int MF_SPEED_TRACKING    = 0x80;
	final int MF_POSITION_TRACKING = 0x100;
	final int MF_DATABASE          = 0x200;
	final int MF_ECAM              = 0x400;
	final int MF_HEARTBEAT         = 0x800;
	final int MF_SERVO             = 0x1000;
	final int MF_UNDER_VOLTAGE     = 0x3000;
	final int MF_OVER_VOLTAGE      = 0x5000;
	final int MF_SHORT_CIRCUIT     = 0xB000;
	final int MF_TEMPERATURE       = 0xF000;
	final int MF_ZERO_NOT_FOUND    = 0x10000;
	final int MF_SPEED             = 0x20000;
	final int MF_STACK_OVERFLOW    = 0x40000;
	final int MF_CPU               = 0x80000;
	final int MF_STUCK             = 0x200000;
	final int MF_POSITION          = 0x400000;
	final int MF_NO_START          = 0x20000000;

	if( failure == MF_OK ){
	    sb.append("failure: ok/on/normal shutdown\n");
	}else if( failure == MF_ANALOG_FEEBACK ){
	    sb.append("failure: Resolver or Analog Halls not ready\n");
	}else if( failure == MF_FEEDBACK_LOSS ){
	    sb.append("failure: Feedback loss\n");
	}else if( failure == MF_PEAK_CURRENT ){
	    sb.append("failure: peak current exceeded (drive malfunction or bad tuning)\n");
	}else if( failure == MF_INHIBIT ){
	    sb.append("failure: inhibit\n");
	}else if( failure == MF_MULT_HALL ){
	    sb.append("failure: multiple hall sensors changed simultaneously \n");
	}else if( failure == MF_SPEED_TRACKING ){
	    sb.append("failure: speed tracking\n");
	}else if( failure == MF_POSITION_TRACKING ){
	    sb.append("failure: position tracking\n");
	}else if( failure == MF_DATABASE ){
	    sb.append("failure: inconsistent database\n");
	}else if( failure == MF_ECAM ){
	    sb.append("failure: ECAM table difference too large\n");
	}else if( failure == MF_HEARTBEAT ){
	    sb.append("failure: heartbeat failure on CAN bus\n");
	}else if( failure == MF_SERVO ){
	    sb.append("failure: servo drive fault\n");
	}else if( failure == MF_UNDER_VOLTAGE ){
	    sb.append("failure: under voltage\n");
	}else if( failure == MF_OVER_VOLTAGE ){
	    sb.append("failure: over voltage\n");
	}else if( failure == MF_SHORT_CIRCUIT ){
	    sb.append("failure: short circuit\n");
	}else if( failure == MF_TEMPERATURE ){
	    sb.append("failure: drive overheating\n");
	}else if( failure == MF_ZERO_NOT_FOUND ){
	    sb.append("failure: Failed to find the electrical zero (possibly insufficient current applied)\n");
	}else if( failure == MF_SPEED ){
	    sb.append("failure: speed limit exceeded\n");
	}else if( failure == MF_STACK_OVERFLOW ){
	    sb.append("failure: stack overflow (internal firmware bug)\n");
	}else if( failure == MF_CPU ){
	    sb.append("failure: CPU exception (internal firmware error)\n");
	}else if( failure == MF_STUCK ){
	    sb.append("failure: motor stuck\n");
	}else if( failure == MF_POSITION ){
	    sb.append("failure: position limit exceeded\n");
	}else if( failure == MF_NO_START ){
	    sb.append("failure: cannot start motor\n");
	}else{
	    sb.append("failure: unknown code: ["+failure+"]\n");
	}
	return sb.toString();
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Vector;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.utils.DelimitedStringParser;


public class ControlLoopParser extends DelimitedStringParser{
	
	static private Logger _log4j = Logger.getLogger(ControlLoopParser.class);
	
	public static final int INDEX_BASE          =0;
	public static final int PH_PROCESS_VALUE_INDEX =INDEX_BASE+0;
	public static final int PH_VEL_CMODE_INDEX     =INDEX_BASE+1;
	public static final int PH_PH_CMODE_INDEX      =INDEX_BASE+2;
	public static final int PH_ESWV_AMODE_INDEX    =INDEX_BASE+3;
	public static final int PH_ESWP_AMODE_INDEX    =INDEX_BASE+4;
	public static final int PH_THRU_AMODE_INDEX    =INDEX_BASE+5;
	public static final int PH_OFFSET_INDEX        =INDEX_BASE+6;
	public static final int PH_SETPOINT_INDEX      =INDEX_BASE+7;
	public static final int PH_ERROR_INDEX         =INDEX_BASE+8;
	public static final int PH_CORRECTION_INDEX    =INDEX_BASE+9;
	public static final int PH_RAW_CORR_INDEX      =INDEX_BASE+10;
	public static final int PH_KP_INDEX            =INDEX_BASE+11;
	public static final int PH_KI_INDEX            =INDEX_BASE+12;
	public static final int PH_KD_INDEX            =INDEX_BASE+13;
	public static final int PH_SCALE_INDEX         =INDEX_BASE+14;
	public static final int PH_PTERM_INDEX         =INDEX_BASE+15;
	public static final int PH_ITERM_INDEX         =INDEX_BASE+16;
	public static final int PH_DTERM_INDEX         =INDEX_BASE+17;

	public static final int PH_PH_EXT_INDEX        =INDEX_BASE+18;
	public static final int PH_PUMP_CMD_INDEX      =INDEX_BASE+19;
	public static final int PH_FWD_VALVE_CMD_INDEX =INDEX_BASE+20;
	public static final int PH_AFT_VALVE_CMD_INDEX =INDEX_BASE+21;
	public static final int PH_VX_INT_INDEX        =INDEX_BASE+22;
	public static final int PH_VY_INT_INDEX        =INDEX_BASE+23;
	public static final int PH_VX_EXT_INDEX        =INDEX_BASE+24;
	public static final int PH_VY_EXT_INDEX        =INDEX_BASE+25;
	public static final int PH_VMAG_INT_INDEX      =INDEX_BASE+26;
	public static final int PH_VHDG_INT_INDEX      =INDEX_BASE+27;
	public static final int PH_VMAG_EXT_INDEX      =INDEX_BASE+28;
	public static final int PH_VHDG_EXT_INDEX      =INDEX_BASE+29;
	
	public static final int VEL_PROCESS_VALUE_INDEX =INDEX_BASE+30;
	public static final int VEL_VEL_CMODE_INDEX     =INDEX_BASE+31;
	public static final int VEL_PH_CMODE_INDEX      =INDEX_BASE+32;
	public static final int VEL_ESWV_AMODE_INDEX    =INDEX_BASE+33;
	public static final int VEL_ESWP_AMODE_INDEX    =INDEX_BASE+34;
	public static final int VEL_THRU_AMODE_INDEX    =INDEX_BASE+35;
	public static final int VEL_OFFSET_INDEX        =INDEX_BASE+36;
	public static final int VEL_SETPOINT_INDEX      =INDEX_BASE+37;
	public static final int VEL_ERROR_INDEX         =INDEX_BASE+38;
	public static final int VEL_CORRECTION_INDEX    =INDEX_BASE+39;
	public static final int VEL_RAW_CORR_INDEX      =INDEX_BASE+40;
	public static final int VEL_KP_INDEX            =INDEX_BASE+41;
	public static final int VEL_KI_INDEX            =INDEX_BASE+42;
	public static final int VEL_KD_INDEX            =INDEX_BASE+43;
	public static final int VEL_SCALE_INDEX         =INDEX_BASE+44;
	public static final int VEL_PTERM_INDEX         =INDEX_BASE+45;
	public static final int VEL_ITERM_INDEX         =INDEX_BASE+46;
	public static final int VEL_DTERM_INDEX         =INDEX_BASE+47;

	public static final int VEL_FWD_THRUSTER_CMD_INDEX =INDEX_BASE+48;
	public static final int VEL_AFT_THRUSTER_CMD_INDEX =INDEX_BASE+49;
	public static final int VEL_VX_INT_INDEX           =INDEX_BASE+50;
	public static final int VEL_VY_INT_INDEX           =INDEX_BASE+51;
	public static final int VEL_VX_EXT_INDEX           =INDEX_BASE+52;
	public static final int VEL_VY_EXT_INDEX           =INDEX_BASE+53;
	public static final int VEL_VMAG_INT_INDEX         =INDEX_BASE+54;
	public static final int VEL_VHDG_INT_INDEX         =INDEX_BASE+55;
	public static final int VEL_VMAG_EXT_INDEX         =INDEX_BASE+56;
	public static final int VEL_VHDG_EXT_INDEX         =INDEX_BASE+57;
	
	public Vector fieldNames=new Vector();
	public Vector fieldUnits=new Vector();
	
	
	private static final long serialVersionUID=1L;
	
	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public ControlLoopParser(){
		super();
		this.initialize();
	}
	
	public ControlLoopParser(String registryName, String delimiters){
		super(registryName,delimiters);
		this.initialize();
	}

	protected void initialize(){
		fieldNames.add(PH_PROCESS_VALUE_INDEX,"pH process value");
		fieldNames.add(PH_VEL_CMODE_INDEX,"pH vel_cmode");
		fieldNames.add(PH_PH_CMODE_INDEX,"pH ph_cmode");
		fieldNames.add(PH_ESWV_AMODE_INDEX,"pH eswv_amode");
		fieldNames.add(PH_ESWP_AMODE_INDEX,"pH eswp_amode");
		fieldNames.add(PH_THRU_AMODE_INDEX,"pH thru_amode");
		fieldNames.add(PH_OFFSET_INDEX,"pH offset");
		fieldNames.add(PH_SETPOINT_INDEX,"pH setpoint");
		fieldNames.add(PH_ERROR_INDEX,"pH err");
		fieldNames.add(PH_CORRECTION_INDEX,"pH corr");
		fieldNames.add(PH_RAW_CORR_INDEX,"pH rawCorr");
		fieldNames.add(PH_KP_INDEX,"pH Kp");
		fieldNames.add(PH_KI_INDEX,"pH Ki");
		fieldNames.add(PH_KD_INDEX,"pH Kd");
		fieldNames.add(PH_SCALE_INDEX,"pH scale");
		fieldNames.add(PH_PTERM_INDEX,"pH pterm");
		fieldNames.add(PH_ITERM_INDEX,"pH iterm");
		fieldNames.add(PH_DTERM_INDEX,"pH dterm");
		
		fieldUnits.add(PH_PROCESS_VALUE_INDEX,"double");
		fieldUnits.add(PH_VEL_CMODE_INDEX,"int");
		fieldUnits.add(PH_PH_CMODE_INDEX,"int");
		fieldUnits.add(PH_ESWV_AMODE_INDEX,"int");
		fieldUnits.add(PH_ESWP_AMODE_INDEX,"int");
		fieldUnits.add(PH_THRU_AMODE_INDEX,"int");
		fieldUnits.add(PH_OFFSET_INDEX,"double");
		fieldUnits.add(PH_SETPOINT_INDEX,"double");
		fieldUnits.add(PH_ERROR_INDEX,"double");
		fieldUnits.add(PH_CORRECTION_INDEX,"double");
		fieldUnits.add(PH_RAW_CORR_INDEX,"double");
		fieldUnits.add(PH_KP_INDEX,"short");
		fieldUnits.add(PH_KI_INDEX,"short");
		fieldUnits.add(PH_KD_INDEX,"short");
		fieldUnits.add(PH_SCALE_INDEX,"short");
		fieldUnits.add(PH_PTERM_INDEX,"float");
		fieldUnits.add(PH_ITERM_INDEX,"float");
		fieldUnits.add(PH_DTERM_INDEX,"float");

		fieldNames.add(PH_PH_EXT_INDEX       ,"pH extPH");
		fieldNames.add(PH_PUMP_CMD_INDEX     ,"pH pumpCmd");
		fieldNames.add(PH_FWD_VALVE_CMD_INDEX,"pH vfwdCmd");
		fieldNames.add(PH_AFT_VALVE_CMD_INDEX,"pH vaftCmd");
		fieldNames.add(PH_VX_INT_INDEX       ,"pH vxInt");
		fieldNames.add(PH_VY_INT_INDEX       ,"pH vyInt");
		fieldNames.add(PH_VX_EXT_INDEX       ,"pH vxExt");
		fieldNames.add(PH_VY_EXT_INDEX       ,"pH vyExt");
		fieldNames.add(PH_VMAG_INT_INDEX     ,"pH vmagInt");
		fieldNames.add(PH_VHDG_INT_INDEX     ,"pH vhdgInt");
		fieldNames.add(PH_VMAG_EXT_INDEX     ,"pH vmagExt");
		fieldNames.add(PH_VHDG_EXT_INDEX     ,"pH vhdgExt");
		
		fieldUnits.add(PH_PH_EXT_INDEX       ,"double");
		fieldUnits.add(PH_PUMP_CMD_INDEX     ,"double");
		fieldUnits.add(PH_FWD_VALVE_CMD_INDEX,"int");
		fieldUnits.add(PH_AFT_VALVE_CMD_INDEX,"int");
		fieldUnits.add(PH_VX_INT_INDEX       ,"double");
		fieldUnits.add(PH_VY_INT_INDEX       ,"double");
		fieldUnits.add(PH_VX_EXT_INDEX       ,"double");
		fieldUnits.add(PH_VY_EXT_INDEX       ,"double");
		fieldUnits.add(PH_VMAG_INT_INDEX     ,"double");
		fieldUnits.add(PH_VHDG_INT_INDEX     ,"double");
		fieldUnits.add(PH_VMAG_EXT_INDEX     ,"double");
		fieldUnits.add(PH_VHDG_EXT_INDEX     ,"double");
		
		fieldNames.add(VEL_PROCESS_VALUE_INDEX,"velocity process value");
		fieldNames.add(VEL_VEL_CMODE_INDEX,"velocity vel_cmode");
		fieldNames.add(VEL_PH_CMODE_INDEX,"velocity ph_cmode");
		fieldNames.add(VEL_ESWV_AMODE_INDEX,"velocity eswv_amode");
		fieldNames.add(VEL_ESWP_AMODE_INDEX,"velocity eswp_amode");
		fieldNames.add(VEL_THRU_AMODE_INDEX,"velocity thru_amode");
		fieldNames.add(VEL_OFFSET_INDEX,"velocity offset");
		fieldNames.add(VEL_SETPOINT_INDEX,"velocity setpoint");
		fieldNames.add(VEL_ERROR_INDEX,"velocity err");
		fieldNames.add(VEL_CORRECTION_INDEX,"velocity corr");
		fieldNames.add(VEL_RAW_CORR_INDEX,"velocity rawCorr");
		fieldNames.add(VEL_KP_INDEX,"velocity Kp");
		fieldNames.add(VEL_KI_INDEX,"velocity Ki");
		fieldNames.add(VEL_KD_INDEX,"velocity Kd");
		fieldNames.add(VEL_SCALE_INDEX,"velocity scale");
		fieldNames.add(VEL_PTERM_INDEX,"velocity pterm");
		fieldNames.add(VEL_ITERM_INDEX,"velocity iterm");
		fieldNames.add(VEL_DTERM_INDEX,"velocity dterm");
		
		fieldUnits.add(VEL_PROCESS_VALUE_INDEX,"double");
		fieldUnits.add(VEL_VEL_CMODE_INDEX,"int");
		fieldUnits.add(VEL_PH_CMODE_INDEX,"int");
		fieldUnits.add(VEL_ESWV_AMODE_INDEX,"int");
		fieldUnits.add(VEL_ESWP_AMODE_INDEX,"int");
		fieldUnits.add(VEL_THRU_AMODE_INDEX,"int");
		fieldUnits.add(VEL_OFFSET_INDEX,"double");
		fieldUnits.add(VEL_SETPOINT_INDEX,"double");
		fieldUnits.add(VEL_ERROR_INDEX,"double");
		fieldUnits.add(VEL_CORRECTION_INDEX,"double");
		fieldUnits.add(VEL_RAW_CORR_INDEX,"double");
		fieldUnits.add(VEL_KP_INDEX,"short");
		fieldUnits.add(VEL_KI_INDEX,"short");
		fieldUnits.add(VEL_KD_INDEX,"short");
		fieldUnits.add(VEL_SCALE_INDEX,"short");
		fieldUnits.add(VEL_PTERM_INDEX,"float");
		fieldUnits.add(VEL_ITERM_INDEX,"float");
		fieldUnits.add(VEL_DTERM_INDEX,"float");
		
		fieldNames.add(VEL_FWD_THRUSTER_CMD_INDEX,"velocity tfwdCmd");
		fieldNames.add(VEL_AFT_THRUSTER_CMD_INDEX,"velocity taftCmd");
		fieldNames.add(VEL_VX_INT_INDEX          ,"velocity vxInt");
		fieldNames.add(VEL_VY_INT_INDEX          ,"velocity vyInt");
		fieldNames.add(VEL_VX_EXT_INDEX          ,"velocity vxExt");
		fieldNames.add(VEL_VY_EXT_INDEX          ,"velocity vyExt");
		fieldNames.add(VEL_VMAG_INT_INDEX        ,"velocity vmagInt");
		fieldNames.add(VEL_VHDG_INT_INDEX        ,"velocity vhdgInt");
		fieldNames.add(VEL_VMAG_EXT_INDEX        ,"velocity vmagExt");
		fieldNames.add(VEL_VHDG_EXT_INDEX        ,"velocity vhdgExt");
		
		fieldUnits.add(VEL_FWD_THRUSTER_CMD_INDEX,"double");
		fieldUnits.add(VEL_FWD_THRUSTER_CMD_INDEX,"double");
		fieldUnits.add(VEL_VX_INT_INDEX          ,"double");
		fieldUnits.add(VEL_VY_INT_INDEX          ,"double");
		fieldUnits.add(VEL_VX_EXT_INDEX          ,"double");
		fieldUnits.add(VEL_VY_EXT_INDEX          ,"double");
		fieldUnits.add(VEL_VMAG_INT_INDEX        ,"double");
		fieldUnits.add(VEL_VHDG_INT_INDEX        ,"double");
		fieldUnits.add(VEL_VMAG_EXT_INDEX        ,"double");
		fieldUnits.add(VEL_VHDG_EXT_INDEX        ,"double");
	}
	
	
	/** Process the token, whose position in string is nToken. If
	 token corresponds to a Field, create and return the field. 
	 Otherwise return null. */
    protected PacketParser.Field processToken(int nToken,String token)
	throws ParseException{
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("parsing token ["+token+"/"+nToken+"]");
		//}
		switch (nToken) {
			case PH_ERROR_INDEX:
			case PH_OFFSET_INDEX:
			case PH_SETPOINT_INDEX:
			case PH_PROCESS_VALUE_INDEX:
			case PH_CORRECTION_INDEX:
			case PH_RAW_CORR_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			case PH_KP_INDEX:
			case PH_KI_INDEX:
			case PH_KD_INDEX:
			case PH_SCALE_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Short(token),(String)fieldUnits.get(nToken));
			case PH_PTERM_INDEX:
			case PH_ITERM_INDEX:
			case PH_DTERM_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Float(token),(String)fieldUnits.get(nToken));
			case PH_VEL_CMODE_INDEX:
			case PH_PH_CMODE_INDEX:
			case PH_ESWV_AMODE_INDEX:
			case PH_ESWP_AMODE_INDEX:
			case PH_THRU_AMODE_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Integer(token),(String)fieldUnits.get(nToken));

			case PH_PUMP_CMD_INDEX:
			case PH_PH_EXT_INDEX:
			case PH_VX_INT_INDEX:
			case PH_VY_INT_INDEX:
			case PH_VX_EXT_INDEX:
			case PH_VY_EXT_INDEX:
			case PH_VMAG_INT_INDEX:
			case PH_VHDG_INT_INDEX:
			case PH_VMAG_EXT_INDEX:
			case PH_VHDG_EXT_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			case PH_FWD_VALVE_CMD_INDEX:
			case PH_AFT_VALVE_CMD_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Integer(token),(String)fieldUnits.get(nToken));
				
			case VEL_ERROR_INDEX:
			case VEL_OFFSET_INDEX:
			case VEL_SETPOINT_INDEX:
			case VEL_PROCESS_VALUE_INDEX:
			case VEL_CORRECTION_INDEX:
			case VEL_RAW_CORR_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			case VEL_KP_INDEX:
			case VEL_KI_INDEX:
			case VEL_KD_INDEX:
			case VEL_SCALE_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Short(token),(String)fieldUnits.get(nToken));
			case VEL_PTERM_INDEX:
			case VEL_ITERM_INDEX:
			case VEL_DTERM_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Float(token),(String)fieldUnits.get(nToken));
			case VEL_VEL_CMODE_INDEX:
			case VEL_PH_CMODE_INDEX:
			case VEL_ESWV_AMODE_INDEX:
			case VEL_ESWP_AMODE_INDEX:
			case VEL_THRU_AMODE_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Integer(token),(String)fieldUnits.get(nToken));

			case VEL_FWD_THRUSTER_CMD_INDEX:
			case VEL_AFT_THRUSTER_CMD_INDEX:
			case VEL_VX_INT_INDEX:
			case VEL_VY_INT_INDEX:
			case VEL_VX_EXT_INDEX:
			case VEL_VY_EXT_INDEX:
			case VEL_VMAG_INT_INDEX:
			case VEL_VHDG_INT_INDEX:
			case VEL_VMAG_EXT_INDEX:
			case VEL_VHDG_EXT_INDEX:
				return new PacketParser.Field((String)fieldNames.get(nToken),new Double(token),(String)fieldUnits.get(nToken));
			default:
				throw new ParseException("invalid field index ["+nToken+"] parsing PID Response packet",nToken);
		}
	}	
}
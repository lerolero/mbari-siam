/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.devices;

import java.util.HashMap;
import java.util.Map;

public interface ProcessParameterIF{
	
	/** map parameter mnemonics to attribute names */
	public static  Map param_pname2aname=DummyParamNames.getMap();
	class DummyParamNames{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put("PARAM_PH_SETPOINT","ph_setpoint");
			map.put("PARAM_PH_OFFSET","ph_offset");
			map.put("PARAM_MAX_FLOW_CHANGE_PERCENT","max_flow_change_percent");
			map.put("PARAM_PH_ABS_MIN","ph_abs_min");
			map.put("PARAM_PH_ABS_MAX","ph_abs_max");
			map.put("PARAM_PH_DEADBAND_LO","ph_deadband_lo");
			map.put("PARAM_PH_DEADBAND_HI","ph_deadband_hi");
			map.put("PARAM_PH_MAX_CORRECTION","ph_max_correction");
			map.put("PARAM_EXP_A","exp_a");
			map.put("PARAM_EXP_B","exp_b");
			map.put("PARAM_EXP_H","exp_k");
			map.put("PARAM_EXP_K","exp_h");
			map.put("PARAM_PH_PID_KP","ph_pid_Kp");
			map.put("PARAM_PH_PID_KI","ph_pid_Ki");
			map.put("PARAM_PH_PID_KD","ph_pid_Kd");
			map.put("PARAM_PH_PID_MAX_KI","ph_pid_max_ki");
			map.put("PARAM_PH_PID_SCALE_FACTOR","ph_pid_scale_factor");
			
			map.put("PARAM_PH_LIN_SLOPE","ph_lin_slope");
			map.put("PARAM_PH_LIN_OFFSET","ph_lin_offset");
			map.put("PARAM_PH_LIN_FILTER_DEPTH","ph_lin_filter_depth");
			map.put("PARAM_PH_LIN_ERROR_LIMIT","ph_lin_error_limit");

			map.put("PARAM_PH_CMODE","ph_control_mode");
			map.put("PARAM_PH_RMODE","ph_response_mode");

			map.put("PARAM_CO2_CONCENTRATION","CO2_CONCENTRATION_MMOL_PER_L");
			map.put("PARAM_FLUME_AREA","FLUME_AREA_M2");
			map.put("PARAM_DENSITY_SW","SW_DENSITY_KG_PER_M3");
			
			map.put("PARAM_VELOCITY_SETPOINT","velocity_setpoint");
			map.put("PARAM_VELOCITY_OFFSET","velocity_offset");
			map.put("PARAM_VELOCITY_MAX_CORRECTION","velocity_max_correction");
			map.put("PARAM_VELOCITY_DEADBAND_LO","velocity_deadband_lo");
			map.put("PARAM_VELOCITY_DEADBAND_HI","velocity_deadband_hi");
			map.put("PARAM_VELOCITY_PID_KP","velocity_pid_Kp");
			map.put("PARAM_VELOCITY_PID_KI","velocity_pid_Ki");
			map.put("PARAM_VELOCITY_PID_KD","velocity_pid_Kd");
			map.put("PARAM_VELOCITY_PID_MAX_KI","velocity_pid_max_ki");
			map.put("PARAM_VELOCITY_PID_SCALE_FACTOR","velocity_pid_scale_factor");
			map.put("PARAM_VELOCITY_MIN_RPM","velocity_min_rpm");
			map.put("PARAM_VELOCITY_MAX_RPM","velocity_max_rpm");
			map.put("PARAM_VELOCITY_CMODE","velocity_control_mode");
			map.put("PARAM_VELOCITY_RMODE","velocity_response_mode");

			map.put("PARAM_VELOCITY_CAL_A","velocity_cal_a");
			map.put("PARAM_VELOCITY_CAL_B","velocity_cal_b");
			map.put("PARAM_VELOCITY_CAL_C","velocity_cal_c");
			
			map.put("PARAM_ESW_VALVE_AMODE","esw_valve_actuation");
			map.put("PARAM_ESW_PUMP_AMODE","esw_pump_actuation");
			map.put("PARAM_THRUSTER_AMODE","thruster_actuation");

			return map;
		}
	}
	/** map parameter ID to parameter mnemonics */
	public static  Map param_id2pname=DummyIDMap.getMap();
	class DummyIDMap{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put(new Integer(PARAM_PH_SETPOINT),"PARAM_PH_SETPOINT");
			map.put(new Integer(PARAM_PH_OFFSET),"PARAM_PH_OFFSET");
			map.put(new Integer(PARAM_VELOCITY_DEADBAND_LO),"PARAM_VELOCITY_DEADBAND_LO");
			map.put(new Integer(PARAM_VELOCITY_DEADBAND_HI),"PARAM_VELOCITY_DEADBAND_HI");
			map.put(new Integer(PARAM_MAX_FLOW_CHANGE_PERCENT),"PARAM_MAX_FLOW_CHANGE_PERCENT");
			map.put(new Integer(PARAM_PH_ABS_MIN),"PARAM_PH_ABS_MIN");
			map.put(new Integer(PARAM_PH_ABS_MAX),"PARAM_PH_ABS_MAX");
			map.put(new Integer(PARAM_PH_DEADBAND_LO),"PARAM_PH_DEADBAND_LO");
			map.put(new Integer(PARAM_PH_DEADBAND_HI),"PARAM_PH_DEADBAND_HI");
			map.put(new Integer(PARAM_PH_MAX_CORRECTION),"PARAM_PH_MAX_CORRECTION");
			map.put(new Integer(PARAM_PH_CMODE),"PARAM_PH_CMODE");
			map.put(new Integer(PARAM_PH_RMODE),"PARAM_PH_RMODE");

			map.put(new Integer(PARAM_EXP_A),"PARAM_EXP_A");
			map.put(new Integer(PARAM_EXP_B),"PARAM_EXP_B");
			map.put(new Integer(PARAM_EXP_H),"PARAM_EXP_H");
			map.put(new Integer(PARAM_EXP_K),"PARAM_EXP_K");
			map.put(new Integer(PARAM_PH_PID_KP),"PARAM_PH_PID_KP");
			map.put(new Integer(PARAM_PH_PID_KI),"PARAM_PH_PID_KI");
			map.put(new Integer(PARAM_PH_PID_KD),"PARAM_PH_PID_KD");
			map.put(new Integer(PARAM_PH_PID_MAX_KI),"PARAM_PH_PID_MAX_KI");
			map.put(new Integer(PARAM_PH_PID_SCALE_FACTOR),"PARAM_PH_PID_SCALE_FACTOR");
			map.put(new Integer(PARAM_CO2_CONCENTRATION),"CO2_CONCENTRATION");
			map.put(new Integer(PARAM_FLUME_AREA),"FLUME_AREA");
			map.put(new Integer(PARAM_DENSITY_SW),"DENSITY_SW");

			map.put(new Integer(PARAM_VELOCITY_SETPOINT),"PARAM_VELOCITY_SETPOINT");
			map.put(new Integer(PARAM_VELOCITY_OFFSET),"PARAM_VELOCITY_OFFSET");
			map.put(new Integer(PARAM_VELOCITY_MAX_CORRECTION),"PARAM_VELOCITY_MAX_CORRECTION");
			map.put(new Integer(PARAM_VELOCITY_DEADBAND_LO),"PARAM_VELOCITY_DEADBAND_LO");
			map.put(new Integer(PARAM_VELOCITY_DEADBAND_HI),"PARAM_VELOCITY_DEADBAND_HI");
			map.put(new Integer(PARAM_VELOCITY_PID_KP),"PARAM_VELOCITY_PID_KP");
			map.put(new Integer(PARAM_VELOCITY_PID_KI),"PARAM_VELOCITY_PID_KI");
			map.put(new Integer(PARAM_VELOCITY_PID_KD),"PARAM_VELOCITY_PID_KD");
			map.put(new Integer(PARAM_VELOCITY_PID_MAX_KI),"PARAM_VELOCITY_PID_MAX_KI");
			map.put(new Integer(PARAM_VELOCITY_PID_SCALE_FACTOR),"PARAM_VELOCITY_PID_SCALE_FACTOR");
			map.put(new Integer(PARAM_VELOCITY_MIN_RPM),"PARAM_VELOCITY_MIN_RPM");
			map.put(new Integer(PARAM_VELOCITY_MAX_RPM),"PARAM_VELOCITY_MAX_RPM");
			map.put(new Integer(PARAM_VELOCITY_CMODE),"PARAM_VELOCITY_CMODE");
			map.put(new Integer(PARAM_VELOCITY_RMODE),"PARAM_VELOCITY_RMODE");

			map.put(new Integer(PARAM_VELOCITY_CAL_A),"PARAM_VELOCITY_CAL_A");
			map.put(new Integer(PARAM_VELOCITY_CAL_B),"PARAM_VELOCITY_CAL_B");
			map.put(new Integer(PARAM_VELOCITY_CAL_C),"PARAM_VELOCITY_CAL_C");

			map.put(new Integer(PARAM_PH_LIN_SLOPE),"PARAM_PH_LIN_SLOPE");
			map.put(new Integer(PARAM_PH_LIN_OFFSET),"PARAM_PH_LIN_OFFSET");
			map.put(new Integer(PARAM_PH_LIN_FILTER_DEPTH),"PARAM_PH_LIN_FILTER_DEPTH");
			map.put(new Integer(PARAM_PH_LIN_ERROR_LIMIT),"PARAM_PH_LIN_ERROR_LIMIT");
			
			map.put(new Integer(PARAM_ESW_VALVE_AMODE),"esw_valve_actuation");
			map.put(new Integer(PARAM_ESW_PUMP_AMODE),"esw_pump_actuation");
			map.put(new Integer(PARAM_THRUSTER_AMODE),"thruster_actuation");
			
			return map;
		}
	}

	/** map signal ID to signal mnemonics */
	public static  Map signal_id2sname=DummySIDMap.getMap();
	class DummySIDMap{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put(new Integer(SIG_PH_INT_FWD_L),"SIG_PH_INT_FWD_L");
			map.put(new Integer(SIG_PH_INT_FWD_R),"SIG_PH_INT_FWD_R");
			
			map.put(new Integer(SIG_PH_INT_AFT_L),"SIG_PH_INT_AFT_L");
			map.put(new Integer(SIG_PH_INT_AFT_R),"SIG_PH_INT_AFT_R");

			map.put(new Integer(SIG_PH_INT_FILT),"SIG_PH_INT_FILT");
			
			map.put(new Integer(SIG_PH_INT_FWD_FILT),"SIG_PH_INT_FWD_FILT");
			map.put(new Integer(SIG_PH_INT_AFT_FILT),"SIG_PH_INT_AFT_FILT");
			
			map.put(new Integer(SIG_PH_EXT_MID_L),"SIG_PH_EXT_MID_L");
			map.put(new Integer(SIG_PH_EXT_MID_R),"SIG_PH_EXT_MID_R");
			
			map.put(new Integer(SIG_PH_EXT_FILT),"SIG_PH_EXT_FILT");
			
			map.put(new Integer(SIG_PH_GRAD),"SIG_PH_GRAD");
			map.put(new Integer(SIG_PH_ERR),"SIG_PH_ERR");
			
			map.put(new Integer(SIG_PH_ESW),"SIG_PH_ESW");
			map.put(new Integer(SIG_PH_ESW_FILT),"SIG_PH_ESW_FILT");
			
			map.put(new Integer(SIG_VH2O_INT_X_RAW),"SIG_VH2O_INT_X_RAW");
			map.put(new Integer(SIG_VH2O_INT_X_FILT),"SIG_VH2O_INT_X_FILT");
			map.put(new Integer(SIG_VH2O_INT_Y_RAW),"SIG_VH2O_INT_Y_RAW");
			map.put(new Integer(SIG_VH2O_INT_Y_FILT),"SIG_VH2O_INT_Y_FILT");
			map.put(new Integer(SIG_VH2O_INT_MAG_FILT),"SIG_VH2O_INT_MAG_FILT");
			map.put(new Integer(SIG_VH2O_INT_DIR_FILT),"SIG_VH2O_INT_DIR_FILT");
			map.put(new Integer(SIG_VH2O_EXT_X_RAW),"SIG_VH2O_EXT_X_RAW");
			map.put(new Integer(SIG_VH2O_EXT_X_FILT),"SIG_VH2O_EXT_X_FILT");
			map.put(new Integer(SIG_VH2O_EXT_Y_RAW),"SIG_VH2O_EXT_Y_RAW");
			map.put(new Integer(SIG_VH2O_EXT_Y_FILT),"SIG_VH2O_EXT_Y_FILT");
			map.put(new Integer(SIG_VH2O_EXT_MAG_FILT),"SIG_VH2O_EXT_MAG_FILT");
			map.put(new Integer(SIG_VH2O_EXT_DIR_FILT),"SIG_VH2O_EXT_DIR_FILT");
			map.put(new Integer(SIG_VH2O_INT_ERR),"SIG_VH2O_INT_ERR");
			map.put(new Integer(SIG_ESW_INJ_VOL),"SIG_ESW_INJ_VOL");
			map.put(new Integer(SIG_ESW_PUMP_CMD_RAW),"SIG_ESW_PUMP_CMD_RAW");
			map.put(new Integer(SIG_ESW_PUMP_CMD_CHK),"SIG_ESW_PUMP_CMD_CHK");
			map.put(new Integer(SIG_ESW_PUMP_VEL),"SIG_ESW_PUMP_VEL");
			map.put(new Integer(SIG_ESW_FWD_VALVE_CMD),"SIG_ESW_FWD_VALVE_CMD");
			map.put(new Integer(SIG_ESW_FWD_VALVE_STATE),"SIG_ESW_FWD_VALVE_STATE");
			map.put(new Integer(SIG_ESW_AFT_VALVE_CMD),"SIG_ESW_AFT_VALVE_CMD");
			map.put(new Integer(SIG_ESW_AFT_VALVE_STATE),"SIG_ESW_AFT_VALVE_STATE");
			map.put(new Integer(SIG_FWD_THRUSTER_VEL_CMD_RAW),"SIG_FWD_THRUSTER_VEL_CMD_RAW");
			map.put(new Integer(SIG_FWD_THRUSTER_VEL_CMD_CHK),"SIG_FWD_THRUSTER_VEL_CMD_CHK");
			map.put(new Integer(SIG_FWD_THRUSTER_VEL),"SIG_FWD_THRUSTER_VEL");
			map.put(new Integer(SIG_AFT_THRUSTER_VEL_CMD_RAW),"SIG_AFT_THRUSTER_VEL_CMD_RAW");
			map.put(new Integer(SIG_AFT_THRUSTER_VEL_CMD_CHK),"SIG_AFT_THRUSTER_VEL_CMD_CHK");
			map.put(new Integer(SIG_AFT_THRUSTER_VEL),"SIG_AFT_THRUSTER_VEL");
			return map;
		}
	}
	
	/** map signal ID to signal mnemonics */
	public static  Map filter_id2fname=DummyFIDMap.getMap();
	class DummyFIDMap{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put(new Integer(BX_PH_INT_FWD_L),"BX_PH_INT_FWD_L");
			map.put(new Integer(BX_PH_INT_FWD_R),"BX_PH_INT_FWD_R");
			map.put(new Integer(BX_PH_INT_AFT_L),"BX_PH_INT_AFT_L");
			map.put(new Integer(BX_PH_INT_AFT_R),"BX_PH_INT_AFT_R");
			map.put(new Integer(BX_PH_EXT_MID_L),"BX_PH_EXT_MID_L");
			map.put(new Integer(BX_PH_EXT_MID_R),"BX_PH_EXT_MID_R");
			map.put(new Integer(FC_PH_INT_FWD),"FC_PH_INT_FWD");
			map.put(new Integer(AC_PH_INT_AFT),"AC_PH_INT_AFT");
			map.put(new Integer(EC_PH_EXT),"EC_PH_EXT");
			map.put(new Integer(IC_PH_INT),"IC_PH_INT");
			map.put(new Integer(BX_PH_ESW),"BX_PH_ESW");
			map.put(new Integer(BX_VH2O_INT_X),"BX_VH2O_INT_X");
			map.put(new Integer(BX_VH2O_INT_Y),"BX_VH2O_INT_Y");
			map.put(new Integer(BX_VH2O_EXT_X),"BX_VH2O_EXT_X");
			map.put(new Integer(BX_VH2O_EXT_Y),"BX_VH2O_EXT_Y");
			map.put(new Integer(VC_VH2O_INT_X),"VC_VH2O_INT_X");
			map.put(new Integer(VC_VH2O_INT_Y),"VC_VH2O_INT_Y");
			map.put(new Integer(VC_VH2O_EXT_X),"VC_VH2O_EXT_X");
			map.put(new Integer(VC_VH2O_EXT_Y),"VC_VH2O_EXT_Y");
			map.put(new Integer(MA_MAG_INT),"MA_MAG_INT");
			map.put(new Integer(HD_HDG_INT),"HD_HDG_INT");
			map.put(new Integer(MA_MAG_EXT),"MA_MAG_EXT");
			map.put(new Integer(HD_HDG_EXT),"HD_HDG_EXT");
			map.put(new Integer(BX_VTHR_FWD),"BX_VTHR_FWD");
			map.put(new Integer(BX_VTHR_AFT),"BX_VTHR_AFT");
			map.put(new Integer(VS_ESWV_FWD),"VS_ESWV_FWD");
			map.put(new Integer(VS_ESWV_AFT),"VS_ESWV_AFT");
			return map;
		}
	}
	
	/** map signal ID to signal mnemonics */
	public static  Map input_id2iname=DummyIIDMap.getMap();
	class DummyIIDMap{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put(new Integer(IBX_PH_INT_FWD_L),"IBX_PH_INT_FWD_L");
			map.put(new Integer(IBX_PH_INT_FWD_R),"IBX_PH_INT_FWD_R");
			map.put(new Integer(IBX_PH_INT_AFT_L),"IBX_PH_INT_AFT_L");
			map.put(new Integer(IBX_PH_INT_AFT_R),"IBX_PH_INT_AFT_R");
			map.put(new Integer(IBX_PH_EXT_MID_L),"IBX_PH_EXT_MID_L");
			map.put(new Integer(IBX_PH_EXT_MID_R),"IBX_PH_EXT_MID_R");
			map.put(new Integer(IFC_PH_INT_FWD_L),"IFC_PH_INT_FWD_L");
			map.put(new Integer(IFC_PH_INT_FWD_R),"IFC_PH_INT_FWD_R");
			map.put(new Integer(IAC_PH_INT_AFT_L),"IAC_PH_INT_AFT_L");
			map.put(new Integer(IAC_PH_INT_AFT_R),"IAC_PH_INT_AFT_R");
			map.put(new Integer(IEC_PH_EXT_MID_L),"IEC_PH_EXT_MID_L");
			map.put(new Integer(IEC_PH_EXT_MID_R),"IEC_PH_EXT_MID_R");
			map.put(new Integer(IIC_PH_INT_FWD),"IIC_PH_INT_FWD");
			map.put(new Integer(IIC_PH_INT_AFT),"IIC_PH_INT_AFT");
			map.put(new Integer(IBX_PH_ESW),"IBX_PH_ESW");
			map.put(new Integer(IBX_VH2O_INT_X),"IBX_VH2O_INT_X");
			map.put(new Integer(IBX_VH2O_INT_Y),"IBX_VH2O_INT_Y");
			map.put(new Integer(IBX_VH2O_EXT_X),"IBX_VH2O_EXT_X");
			map.put(new Integer(IBX_VH2O_EXT_Y),"IBX_VH2O_EXT_Y");
			map.put(new Integer(IVC_VH2O_INT_X),"IVC_VH2O_INT_X");
			map.put(new Integer(IVC_VH2O_INT_Y),"IVC_VH2O_INT_Y");
			map.put(new Integer(IVC_VH2O_EXT_X),"IVC_VH2O_EXT_X");
			map.put(new Integer(IVC_VH2O_EXT_Y),"IVC_VH2O_EXT_Y");
			map.put(new Integer(IMA_MAG_INT_X),"IMA_MAG_INT_X");
			map.put(new Integer(IMA_MAG_INT_Y),"IMA_MAG_INT_Y");
			map.put(new Integer(IHD_HDG_INT_X),"IHD_HDG_INT_X");
			map.put(new Integer(IHD_HDG_INT_Y),"IHD_HDG_INT_Y");
			map.put(new Integer(IMA_MAG_EXT_X),"IMA_MAG_EXT_X");
			map.put(new Integer(IMA_MAG_EXT_Y),"IMA_MAG_EXT_Y");
			map.put(new Integer(IHD_HDG_EXT_X),"IHD_HDG_EXT_X");
			map.put(new Integer(IHD_HDG_EXT_Y),"IHD_HDG_EXT_Y");
			map.put(new Integer(IBX_VTHR_FWD),"IBX_VTHR_FWD");
			map.put(new Integer(IBX_VTHR_AFT),"IBX_VTHR_AFT");
			map.put(new Integer(IVS_ESWV_FWD),"IVS_ESWV_FWD");
			map.put(new Integer(IVS_ESWV_AFT),"IVS_ESWV_AFT");
			return map;
		}
	}
	
	/** map output ID to output mnemonics */
	public static  Map output_id2name=DummyOIDMap.getMap();
	class DummyOIDMap{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put(new Integer(OUTPUT_ESW_PUMP_VELOCITY),"ROLE_ESW_PUMP");
			map.put(new Integer(OUTPUT_FWD_THRUSTER_VELOCITY),"ROLE_FWD_THRUSTER");
			map.put(new Integer(OUTPUT_AFT_THRUSTER_VELOCITY),"ROLE_AFT_THRUSTER");
			map.put(new Integer(OUTPUT_FWD_ESW_VALVE),"ROLE_FWD_ESW_VALVE");
			map.put(new Integer(OUTPUT_AFT_ESW_VALVE),"ROLE_AFT_ESW_VALVE");
			return map;
		}
	}
	
	/** map role ID to role mnemonics */
	public static  Map role_id2name=DummyRIDMap.getMap();
	class DummyRIDMap{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put(new Integer(ROLE_INT_FWD_L_PH),"ROLE_INT_FWD_L_PH");
			map.put(new Integer(ROLE_INT_FWD_R_PH),"ROLE_INT_FWD_R_PH");
			map.put(new Integer(ROLE_INT_AFT_L_PH),"ROLE_INT_AFT_L_PH");
			map.put(new Integer(ROLE_INT_AFT_R_PH),"ROLE_INT_AFT_R_PH");
			map.put(new Integer(ROLE_EXT_MID_L_PH),"ROLE_EXT_MID_L_PH");
			map.put(new Integer(ROLE_EXT_MID_R_PH),"ROLE_EXT_MID_R_PH");
			map.put(new Integer(ROLE_ESW_PH),"ROLE_ESW_PH");
			map.put(new Integer(ROLE_FWD_ESW_VALVE),"ROLE_FWD_ESW_VALVE");
			map.put(new Integer(ROLE_AFT_ESW_VALVE),"ROLE_AFT_ESW_VALVE");
			map.put(new Integer(ROLE_ESW_PUMP),"ROLE_ESW_PUMP");
			map.put(new Integer(ROLE_INT_X_VELOCITY),"ROLE_INT_X_VELOCITY");
			map.put(new Integer(ROLE_INT_Y_VELOCITY),"ROLE_INT_Y_VELOCITY");
			map.put(new Integer(ROLE_EXT_X_VELOCITY),"ROLE_EXT_X_VELOCITY");
			map.put(new Integer(ROLE_EXT_Y_VELOCITY),"ROLE_EXT_Y_VELOCITY");
			map.put(new Integer(ROLE_FWD_THRUSTER),"ROLE_FWD_THRUSTER");
			map.put(new Integer(ROLE_AFT_THRUSTER),"ROLE_AFT_THRUSTER");
			return map;
		}
	}
	
	/** map mode ID to parameter mnemonics */
	public static  Map mode_id2pname=DummyModeNames.getMap();
	class DummyModeNames{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put(new Integer(CONTROL_MODE_OFFSET),"CMODE_OFFSET");
			map.put(new Integer(CONTROL_MODE_CONSTANT),"CMODE_CONSTANT");
			map.put(new Integer(CONTROL_MODE_MANUAL),"CMODE_MANUAL");
			map.put(new Integer(CONTROL_MODE_DEADBAND),"CMODE_DEADBAND");
			map.put(new Integer(CONTROL_MODE_PANIC),"CMODE_PANIC");
			map.put(new Integer(RESPONSE_MODE_EXP),"RMODE_EXP");
			map.put(new Integer(RESPONSE_MODE_PID),"RMODE_PID");
			map.put(new Integer(RESPONSE_MODE_LIN),"RMODE_LIN");
			map.put(new Integer(ACTUATION_ENABLED),"AMODE_ENABLED");
			map.put(new Integer(ACTUATION_DISABLED),"AMODE_DISABLED");
			return map;
		}
	}
	/** map mode ID to parameter mnemonics */
	public static  Map const_mnem2value=DummyConstValues.getMap();
	class DummyConstValues{
		public static Map getMap(){
			HashMap map=new HashMap();
			map.put("CMODE_OFFSET",new Integer(CONTROL_MODE_OFFSET));
			map.put("CMODE_CONSTANT",new Integer(CONTROL_MODE_CONSTANT));
			map.put("CMODE_MANUAL",new Integer(CONTROL_MODE_MANUAL));
			map.put("CMODE_DEADBAND",new Integer(CONTROL_MODE_DEADBAND));
			map.put("CMODE_PANIC",new Integer(CONTROL_MODE_PANIC));
			map.put("RMODE_EXP",new Integer(RESPONSE_MODE_EXP));
			map.put("RMODE_PID",new Integer(RESPONSE_MODE_PID));
			map.put("RMODE_LIN",new Integer(RESPONSE_MODE_LIN));
			map.put("AMODE_ENABLED",new Integer(ACTUATION_ENABLED));
			map.put("AMODE_DISABLED",new Integer(ACTUATION_DISABLED));
			return map;
		}
	}
	
	/** Exponential model response parameter A*/
	public static final int PARAM_EXP_A=0;
	/** Exponential model response parameter B*/
	public static final int PARAM_EXP_B=1;
	/** Exponential model response parameter K*/
	public static final int PARAM_EXP_K=2;
	/** Exponential model response parameter H*/
	public static final int PARAM_EXP_H=3;
	
	/** PID model response parameter KP (proportional gain) */
	public static final int PARAM_PH_PID_KP=4;
	/** PID model response parameter KI (integral gain) */
	public static final int PARAM_PH_PID_KI=5;
	/** PID model response parameter KD (derivative gain) */
	public static final int PARAM_PH_PID_KD=6;
	/** maximum PID_KI value */
	public static final int PARAM_PH_PID_MAX_KI=7;
	/** PID gain scale factor (divisor) */
	public static final int PARAM_PH_PID_SCALE_FACTOR=8;
	/** pH setpoint (desired pH value in constant mode) */
	public static final int PARAM_PH_SETPOINT             =9;	
	/** pH offset (relative pH value in offset mode) */
	public static final int PARAM_PH_OFFSET               =10;
	
	/** (unused) */
	public static final int PARAM_PH_ABS_MIN              =11;	
	/** (unused) */
	public static final int PARAM_PH_ABS_MAX              =12;	
	/** (unused)*/
	public static final int PARAM_PH_DEADBAND_LO          =13;
	/** (unused)*/
	public static final int PARAM_PH_DEADBAND_HI          =14;
	/** max pH control correction limit */
	public static final int PARAM_PH_MAX_CORRECTION       =15;
	/** constant: CO2 concentration (mmol/liter) */
	public static final int PARAM_CO2_CONCENTRATION       =16;
	/** constant: FOCE flume cross sectional area (m^2) */
	public static final int PARAM_FLUME_AREA              =17;
	/** constant: seawater density (kg/m^3) */
	public static final int PARAM_DENSITY_SW              =18;
	/** (unused)*/
	public static final int PARAM_MAX_FLOW_CHANGE_PERCENT =19;

	/** PID model response parameter KP (proportional gain) */
	public static final int PARAM_VELOCITY_PID_KP=20;
	/** PID model response parameter KI (integral gain) */
	public static final int PARAM_VELOCITY_PID_KI=21;
	/** PID model response parameter KD (derivative gain) */
	public static final int PARAM_VELOCITY_PID_KD=22;
	/** maximum PID_KI value */
	public static final int PARAM_VELOCITY_PID_MAX_KI=23;
	/** PID gain scale factor (divisor) */
	public static final int PARAM_VELOCITY_PID_SCALE_FACTOR=24;
	/** water velocity setpoint (absolute). Used to calculate process error
	 signal in CONSTANT control mode
	 */
	public static final int PARAM_VELOCITY_SETPOINT       =25;
	/** velocity offset relative to external. Used to calculate process error
	 signal in OFFSET control mode
	 */	
	public static final int PARAM_VELOCITY_OFFSET         =26;
	/** thruster min speed (motor rpm) */
	public static final int PARAM_VELOCITY_MIN_RPM        =27;
	/** thruster max speed (motor rpm) */
	public static final int PARAM_VELOCITY_MAX_RPM        =28;
	/** velocity deadband min. Used to calculate process error
	 signal in DEADBAND control mode
	 */	
	public static final int PARAM_VELOCITY_DEADBAND_LO    =29;
	/** velocity deadband max. Used to calculate process error
	 signal in DEADBAND control mode
	 */	
	public static final int PARAM_VELOCITY_DEADBAND_HI    =30;
	/** max change limit for thruster motor speed (motor rpm) */
	public static final int PARAM_VELOCITY_MAX_CORRECTION =31;
	/** velocity calibration constant (VH20= aS^2+bS+c),
	 where S is motor speed (motor rpm)
	 */
	public static final int PARAM_VELOCITY_CAL_A          =32;
	/** velocity calibration constant (VH20= aS^2+bS+c),
	 where S is motor speed (motor rpm)
	 */
	public static final int PARAM_VELOCITY_CAL_B          =33;
	/** velocity calibration constant (VH20= aS^2+bS+c),
	 where S is motor speed (motor rpm)
	 */
	public static final int PARAM_VELOCITY_CAL_C          =34;

	
	/** LIN model response parameter slope */
	public static final int PARAM_PH_LIN_SLOPE            =35;
	/** LIN model response parameter offset */
	public static final int PARAM_PH_LIN_OFFSET           =36;
	/** LIN model response parameter slope */
	public static final int PARAM_PH_LIN_FILTER_DEPTH     =37;
	/** LIN model response parameter slope */
	public static final int PARAM_PH_LIN_ERROR_LIMIT      =38;
	
	//////////////////////////////
	/* High Level Configuration */
	//////////////////////////////
	/** pH control mode.
	 Valid values:
	 CONTROL_MODE_MANUAL   : set manually (no closed loop)
	 CONTROL_MODE_CONSTANT : maintain setpoint
	 CONTROL_MODE_OFFSET   : follow specified offset from reference
	 */
	public static final int PARAM_PH_CMODE       =600;
	/** Response mode 
	 valid values:
	 RMODE_EXP : exponential (not implemented)
	 RMODE_PID : PID response
	 */
	public static final int PARAM_PH_RMODE       =605;

	/** velocity control mode.
	 Valid values:
	 CONTROL_MODE_MANUAL   : set manually (no closed loop)
	 CONTROL_MODE_CONSTANT : maintain setpoint
	 CONTROL_MODE_OFFSET   : follow specified offset from reference
	 CONTROL_MODE_DEADBAND : maintain outside of deadband
	 */
	public static final int PARAM_VELOCITY_CMODE =620;
	/** Response mode 
	 valid values:
	 RMODE_EXP : exponential (not implemented)
	 RMODE_PID : PID response
	 */
	public static final int PARAM_VELOCITY_RMODE =625;

	/** Actuation mode 
	 valid values:
	 ACTUATION_ENABLED  : enable control loops to do actuation 
	 ACTUATION_DISABLED : calculate commands but do not perform actuation 
	 */	
	public static final int PARAM_ESW_VALVE_AMODE =640;
	/** Actuation mode 
	 valid values:
	 ACTUATION_ENABLED  : enable control loops to do actuation 
	 ACTUATION_DISABLED : calculate commands but do not perform actuation 
	 */	
	public static final int PARAM_ESW_PUMP_AMODE  =645;
	/** Actuation mode 
	 valid values:
	 ACTUATION_ENABLED  : enable control loops to do actuation 
	 ACTUATION_DISABLED : calculate commands but do not perform actuation 
	 */	
	public static final int PARAM_THRUSTER_AMODE  =650;
	
	/** InputConnector type 
		- SIAM registry: SIAM via SIAM registry
		- SIAM OSDT : SIAM via Open Source DataTurbine
		- EXT OSDT (for test)
		- RAW (not used)
	 */
	public static final int CONNECTOR_TYPE_SIAM_REG  =0;
	/** InputConnector type 
	 - SIAM registry: SIAM via SIAM registry
	 - SIAM OSDT : SIAM via Open Source DataTurbine
	 - EXT OSDT (for test)
	 - RAW (not used)
	 */
	public static final int CONNECTOR_TYPE_SIAM_OSDT =1;
	/** InputConnector type 
	 - SIAM registry: SIAM via SIAM registry
	 - SIAM OSDT : SIAM via Open Source DataTurbine
	 - EXT OSDT (for test)
	 - RAW (not used)
	 */
	public static final int CONNECTOR_TYPE_EXT_OSDT  =2;
	/** InputConnector type 
	 - SIAM registry: SIAM via SIAM registry
	 - SIAM OSDT : SIAM via Open Source DataTurbine
	 - EXT OSDT (for test)
	 - RAW (not used)
	 */
	public static final int CONNECTOR_TYPE_RAW       =3;
	/** Output type (unused) */
	public static final int OUTPUT_TYPE_SIAM         =4;
	/** Output type (unused) */
	public static final int OUTPUT_TYPE_EXT          =5;	
	
	
	//////////////////////////////
	/* Control signals */
	//////////////////////////////
	/** pH internal forward left */
	public static final int SIG_PH_INT_FWD_L          =100;
	/** pH internal forward right */
	public static final int SIG_PH_INT_FWD_R          =105;
	/** pH internal aft left */
	public static final int SIG_PH_INT_AFT_L          =110;
	/** pH internal aft right */
	public static final int SIG_PH_INT_AFT_R          =115;
	/** pH internal (combined average - this is the value used for control) */
	public static final int SIG_PH_INT_FILT           =120;
	/** pH internal forward combined */
	public static final int SIG_PH_INT_FWD_FILT       =121;
	/** pH internal aft combined */
	public static final int SIG_PH_INT_AFT_FILT       =122;
	
	/** pH external mid left */
	public static final int SIG_PH_EXT_MID_L          =130;
	/** pH external mid right */
	public static final int SIG_PH_EXT_MID_R          =135;
	/** pH external (combined average - this is the value used for control) */
	public static final int SIG_PH_EXT_FILT           =140;
	/** pH difference between internal fwd and aft */
	public static final int SIG_PH_GRAD               =150;
	/** pH difference between desired and actual value */
	public static final int SIG_PH_ERR                =160;
	/** pH ESW */
	public static final int SIG_PH_ESW                =170;
	/** pH ESW (filtered) */
	public static final int SIG_PH_ESW_FILT           =180;
	
	/** water velocity internal X direction (raw) */
	public static final int SIG_VH2O_INT_X_RAW        =200;
	/** water velocity internal X direction (filtered) */
	public static final int SIG_VH2O_INT_X_FILT       =205;
	/** water velocity internal Y direction (raw) */
	public static final int SIG_VH2O_INT_Y_RAW        =210;
	/** water velocity internal Y direction (filtered) */
	public static final int SIG_VH2O_INT_Y_FILT       =215;
	/** water velocity external X direction (raw) */
	public static final int SIG_VH2O_EXT_X_RAW        =220;
	/** water velocity external X direction (filtered) */
	public static final int SIG_VH2O_EXT_X_FILT       =225;
	/** water velocity external Y direction (raw) */
	public static final int SIG_VH2O_EXT_Y_RAW        =230;
	/** water velocity external Y direction (filtered) */
	public static final int SIG_VH2O_EXT_Y_FILT       =235;
	/** water velocity internal magnitude of filtered X,Y */
	public static final int SIG_VH2O_INT_MAG_FILT     =240;
	/** water velocity internal direction of filtered X,Y */
	public static final int SIG_VH2O_INT_DIR_FILT     =245;
	/** water velocity external magnitude of filtered X,Y */
	public static final int SIG_VH2O_EXT_MAG_FILT     =250;
	/** water velocity external direction of filtered X,Y */
	public static final int SIG_VH2O_EXT_DIR_FILT     =255;
	/** velocity difference between desired and actual value */
	public static final int SIG_VH2O_INT_ERR          =260;
	/**(not used)*/
	public static final int SIG_ESW_INJ_VOL           =300;
	/**(not used)*/
	public static final int SIG_ESW_PUMP_CMD_RAW      =301;
	/**(not used)*/
	public static final int SIG_ESW_PUMP_CMD_CHK      =302;
	/** ESW pump velocity (input) */
	public static final int SIG_ESW_PUMP_VEL          =303;
	/** ESW valve command (input) */
	public static final int SIG_ESW_FWD_VALVE_CMD     =310;
	/** ESW valve state (input) */
	public static final int SIG_ESW_FWD_VALVE_STATE   =311;
	/** ESW pump velocity (input) */
	public static final int SIG_ESW_AFT_VALVE_CMD     =312;
	/** ESW valve state (input) */
	public static final int SIG_ESW_AFT_VALVE_STATE   =313;
	
	/** fwd thruster velocity raw (input) */
	public static final int SIG_FWD_THRUSTER_VEL_CMD_RAW =400;
	/**(not used)*/
	public static final int SIG_FWD_THRUSTER_VEL_CMD_CHK =401;
	/** fwd thruster velocity filtered (input) */
	public static final int SIG_FWD_THRUSTER_VEL         =402;
	/** aft thruster velocity raw (input) */
	public static final int SIG_AFT_THRUSTER_VEL_CMD_RAW =403;
	/**(not used)*/
	public static final int SIG_AFT_THRUSTER_VEL_CMD_CHK =404;
	/** aft thruster velocity filtered (input) */
	public static final int SIG_AFT_THRUSTER_VEL         =405;
	
	//////////////////////////////
	/* Filter Inputs        */
	//////////////////////////////
	/** boxcar filter input, pH internal fwd left */
	public static final int IBX_PH_INT_FWD_L           =500;
	/** boxcar filter input, pH internal fwd right */
	public static final int IBX_PH_INT_FWD_R           =510;
	/** boxcar filter input, pH internal aft left */
	public static final int IBX_PH_INT_AFT_L           =515;
	/** boxcar filter input, pH internal aft right */
	public static final int IBX_PH_INT_AFT_R           =520;
	/** boxcar filter input, pH external mid left */
	public static final int IBX_PH_EXT_MID_L           =525;
	/** boxcar filter input, pH external mid right */
	public static final int IBX_PH_EXT_MID_R           =530;
	/** filter input, internal forward pH combiner, fwd left */
	public static final int IFC_PH_INT_FWD_L           =535;
	/** filter input, internal forward pH combiner, fwd right */
	public static final int IFC_PH_INT_FWD_R           =540;
	
	/** filter input, internal aft pH combiner, aft left */
	public static final int IAC_PH_INT_AFT_L           =545;
	/** filter input, internal aft pH combiner, aft right */
	public static final int IAC_PH_INT_AFT_R           =550;
	
	/** filter input, external mid pH combiner, mid left */
	public static final int IEC_PH_EXT_MID_L           =560;
	/** filter input, external mid pH combiner, mid right */
	public static final int IEC_PH_EXT_MID_R           =565;
	
	/** filter input, internal pH combiner, fwd */
	public static final int IIC_PH_INT_FWD             =570;
	/** filter input, internal pH combiner, aft */
	public static final int IIC_PH_INT_AFT             =571;
	
	/** boxcar filter input, pH ESW */
	public static final int IBX_PH_ESW                 =575;
	/** boxcar filter input, internal H2O X velocity */
	public static final int IBX_VH2O_INT_X             =580;
	/** boxcar filter input, internal H2O Y velocity */
	public static final int IBX_VH2O_INT_Y             =585;
	/** boxcar filter input, external H2O X velocity */
	public static final int IBX_VH2O_EXT_X             =590;
	/** boxcar filter input, external H2O Y velocity */
	public static final int IBX_VH2O_EXT_Y             =595;
	
	/** filter input, internal H2O X velocity combiner */
	public static final int IVC_VH2O_INT_X             =600;
	/** filter input, internal H2O Y velocity combiner */
	public static final int IVC_VH2O_INT_Y             =605;
	/** filter input, external H2O X velocity combiner */
	public static final int IVC_VH2O_EXT_X             =615;
	/** filter input, external H2O Y velocity combiner */
	public static final int IVC_VH2O_EXT_Y             =620;
	
	/** filter input, internal H2O magnitude filter X */
	public static final int IMA_MAG_INT_X             =630;
	/** filter input, internal H2O magnitude filter Y */
	public static final int IMA_MAG_INT_Y             =635;
	/** filter input, internal H2O direction (heading) filter X */
	public static final int IHD_HDG_INT_X             =640;
	/** filter input, internal H2O direction (heading) filter Y */
	public static final int IHD_HDG_INT_Y             =645;
	/** filter input, external H2O magnitude filter X */
	public static final int IMA_MAG_EXT_X             =650;
	/** filter input, external H2O magnitude filter Y */
	public static final int IMA_MAG_EXT_Y             =655;
	/** filter input, external H2O direction (heading) filter X */
	public static final int IHD_HDG_EXT_X             =660;
	/** filter input, external H2O direction (heading) filter Y */
	public static final int IHD_HDG_EXT_Y             =665;

	/** boxcar filter input, fwd thruster velocity */
	public static final int IBX_VTHR_FWD              =670;
	/** boxcar filter input, aft thruster velocity */
	public static final int IBX_VTHR_AFT              =675;
	/** filter input, fwd ESW valve state */
	public static final int IVS_ESWV_FWD              =680;
	/** filter input, aft ESW valve state */
	public static final int IVS_ESWV_AFT              =685;
	/** filter input, aft ESW pump velocity */
	public static final int IBX_VESWP                 =690;
	
	
	//////////////////////////////
	/* Outputs       */
	//////////////////////////////
	/** ESW pump output */
	public static final int OUTPUT_ESW_PUMP_VELOCITY     =700;
	/** fwd thruster output */
	public static final int OUTPUT_FWD_THRUSTER_VELOCITY =710;
	/** aft thruster output */
	public static final int OUTPUT_AFT_THRUSTER_VELOCITY =720;
	/** fwd ESW valve output */
	public static final int OUTPUT_FWD_ESW_VALVE         =730;
	/** aft ESW valve output */
	public static final int OUTPUT_AFT_ESW_VALVE         =740;
	
	//////////////////////////////
	/* Filters                  */
	//////////////////////////////
	/** boxcar filter, pH internal fwd left */
	public static final int BX_PH_INT_FWD_L           =800;
	/** boxcar filter, pH internal fwd right */
	public static final int BX_PH_INT_FWD_R           =810;
	/** boxcar filter, pH internal aft left */
	public static final int BX_PH_INT_AFT_L           =815;
	/** boxcar filter, pH internal aft right */
	public static final int BX_PH_INT_AFT_R           =820;
	/** boxcar filter, pH external aft left */
	public static final int BX_PH_EXT_MID_L           =825;
	/** boxcar filter, pH external aft right */
	public static final int BX_PH_EXT_MID_R           =830;
	/** fwd pH combiner filter, pH internal */
	public static final int FC_PH_INT_FWD             =835;
	/** aft combiner filter, pH internal */
	public static final int AC_PH_INT_AFT             =845;
	/** external  combiner filter, pH */
	public static final int EC_PH_EXT                 =860;
	/** internal  combiner filter, pH */
	public static final int IC_PH_INT                 =870;

	/** boxcar filter, pH ESW */
	public static final int BX_PH_ESW                 =875;
	/** boxcar filter, H2O velocity, internal X */
	public static final int BX_VH2O_INT_X             =880;
	/** boxcar filter, H2O velocity, internal Y */
	public static final int BX_VH2O_INT_Y             =885;
	/** boxcar filter, H2O velocity, external X */
	public static final int BX_VH2O_EXT_X             =890;
	/** boxcar filter, H2O velocity, external Y */
	public static final int BX_VH2O_EXT_Y             =895;
	/** combiner filter, H2O velocity, internal X */
	public static final int VC_VH2O_INT_X             =900;
	/** combiner filter, H2O velocity, internal Y */
	public static final int VC_VH2O_INT_Y             =905;
	/** combiner filter, H2O velocity, external X */
	public static final int VC_VH2O_EXT_X             =910;
	/** combiner filter, H2O velocity, external Y */
	public static final int VC_VH2O_EXT_Y             =915;
	/** magnitude filter, H2O velocity, internal XY */
	public static final int MA_MAG_INT                =920;
	/** heading filter, H2O velocity, internal XY */
	public static final int HD_HDG_INT                =925;
	/** magnitude filter, H2O velocity, external XY */
	public static final int MA_MAG_EXT                =930;
	/** heading filter, H2O velocity, external XY */
	public static final int HD_HDG_EXT                =935;
	/** boxcar filter, fwd thruster (input) */
	public static final int BX_VTHR_FWD               =940;
	/** boxcar filter, aft thruster (input) */
	public static final int BX_VTHR_AFT               =945;
	/** boxcar filter, fwd ESW valve state (input) */
	public static final int VS_ESWV_FWD               =950;
	/** boxcar filter, aft ESW valve state (input) */
	public static final int VS_ESWV_AFT               =955;
	/** boxcar filter, ESW pump velocity (input) */
	public static final int BX_VESWP                  =960;
	
	
	//////////////////////////////
	/* Attributes */
	//////////////////////////////
	/** role mask: location internal */
	public static final int LOC_INT        =0x1; //2 bits
	/** role mask: location external */
	public static final int LOC_EXT        =0x2;
	/** role mask: location ESW */
	public static final int LOC_ESW        =0x3;
	
	/** role mask: position fwd */
	public static final int POS_FWD        =0x4; //2 bits
	/** role mask: position mid */
	public static final int POS_MID        =0x8;
	/** role mask: position aft */
	public static final int POS_AFT        =0xC;
	/** role mask: position left */
	public static final int POS_L          =0x100; // 2bits
	/** role mask: position right */
	public static final int POS_R          =0x200;
	
	/** role mask: direction X */
	public static final int DIR_X          =0x400; // 2bits
	/** role mask: direction Y */
	public static final int DIR_Y          =0x800;
	/** role mask: direction Z */
	public static final int DIR_Z          =0xC00;
	
	/** role mask: type pH */
	public static final int TYPE_PH        =0x1000; //3bits
	/** role mask: type ESW */
	public static final int TYPE_ESW       =0x2000;
	/** role mask: type velocity */
	public static final int TYPE_VELOCITY  =0x3000;
	/** role mask: type motor */
	public static final int TYPE_MOTOR     =0x4000;
	/** role mask: type valve */
	public static final int TYPE_VALVE     =0x5000;
	/** role mask: type valve */
	public static final int TYPE_ALL       =0x7000;

	/** role mask: mask of role bits */
	public static final int ROLE_MASK      =0x7FFF;
	/** role mask: mask of type bits */
	public static final int TYPE_MASK      =0x7000;

	/**filtering include (unused)*/
	public static final int PROC_INCL      =0x1; //2 bits
	/**filtering exclude (unused)*/
	public static final int PROC_EXCL      =0x2;
	
	/** connection role */
	public static final int ROLE_INT_FWD_L_PH  = LOC_INT|POS_FWD|POS_L|TYPE_PH;
	/** connection role */
	public static final int ROLE_INT_FWD_R_PH  = LOC_INT|POS_FWD|POS_R|TYPE_PH;
	/** connection role */
	public static final int ROLE_INT_AFT_L_PH  = LOC_INT|POS_AFT|POS_L|TYPE_PH;
	/** connection role */
	public static final int ROLE_INT_AFT_R_PH  = LOC_INT|POS_AFT|POS_R|TYPE_PH;
	/** connection role */
	public static final int ROLE_EXT_MID_L_PH  = LOC_EXT|POS_MID|POS_L|TYPE_PH;
	/** connection role */
	public static final int ROLE_EXT_MID_R_PH  = LOC_EXT|POS_MID|POS_R|TYPE_PH;
	
	/** connection role */
	public static final int ROLE_ESW_PH        = LOC_ESW|TYPE_PH;
	/** connection role */
	public static final int ROLE_FWD_ESW_VALVE = LOC_ESW|POS_FWD|TYPE_VALVE;
	/** connection role */
	public static final int ROLE_AFT_ESW_VALVE = LOC_ESW|POS_AFT|TYPE_VALVE;
	/** connection role */
	public static final int ROLE_ESW_PUMP      = LOC_ESW|TYPE_MOTOR;
	
	/** connection role */
	public static final int ROLE_INT_X_VELOCITY  = LOC_INT|POS_MID|DIR_X|TYPE_VELOCITY;
	/** connection role */
	public static final int ROLE_INT_Y_VELOCITY  = LOC_INT|POS_MID|DIR_Y|TYPE_VELOCITY;
	/** connection role */
	public static final int ROLE_EXT_X_VELOCITY  = LOC_EXT|POS_MID|DIR_X|TYPE_VELOCITY;
	/** connection role */
	public static final int ROLE_EXT_Y_VELOCITY  = LOC_EXT|POS_MID|DIR_Y|TYPE_VELOCITY;
	
	/** connection role */
	public static final int ROLE_FWD_THRUSTER  = LOC_INT|POS_FWD|TYPE_MOTOR;
	/** connection role */
	public static final int ROLE_AFT_THRUSTER  = LOC_INT|POS_AFT|TYPE_MOTOR;
	
	/** control mode: manual */
	public static final int CONTROL_MODE_MANUAL       =0;
	/** control mode: constant */
	public static final int CONTROL_MODE_CONSTANT     =1;
	/** control mode: deadband */
	public static final int CONTROL_MODE_DEADBAND     =2;
	/** control mode: offset */
	public static final int CONTROL_MODE_OFFSET       =3;
	/** control mode: panic */
	public static final int CONTROL_MODE_PANIC        =4;
	/** control mode min value (for validation) */
	public static final int CONTROL_MODE_MIN          =CONTROL_MODE_MANUAL;
	/** control mode max value (for validation) */
	public static final int CONTROL_MODE_MAX          =CONTROL_MODE_PANIC;
	
	/** actuation: disabled */
	public static final int ACTUATION_DISABLED        =4;
	/** actuation: ensabled */
	public static final int ACTUATION_ENABLED         =5;
	/** actuation mode min value (for validation) */
	public static final int ACTUATION_MIN          =ACTUATION_DISABLED;
	/** actuation mode max value (for validation) */
	public static final int ACTUATION_MAX          =ACTUATION_ENABLED;

	/** response mode: exponention (not used) */
	public static final int RESPONSE_MODE_EXP         =6;
	/** response mode: PID */
	public static final int RESPONSE_MODE_PID         =7;
	/** response mode: LINEAR */
	public static final int RESPONSE_MODE_LIN         =8;
	/** response mode min value (for validation) */
	public static final int RESPONSE_MODE_MIN          =RESPONSE_MODE_EXP;
	/** response mode max value (for validation) */
	public static final int RESPONSE_MODE_MAX          =RESPONSE_MODE_LIN;

}
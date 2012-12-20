/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.util.Vector;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.distributed.devices.ProcessParameterIF;

public class ControlLoopAttributes extends InstrumentServiceAttributes implements ProcessParameterIF{

	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(ControlLoopAttributes.class);

	////////////////////////////////////////
	// Geometric/Physical Parameters
	////////////////////////////////////////
	
	/** constant: seawater density (kg/m^3) */
	public double SW_DENSITY_KG_PER_M3=1031.4;//1022.8;
	/** constant: CO2 concentration (mmol/liter) */
	public double CO2_CONCENTRATION_MMOL_PER_L=500.0;
	/** constant: FOCE flume cross sectional area (m^2) */
	public double FLUME_AREA_M2=0.25;//0.5;
	/** constant: ESW pump displacment (ml/revolution) */
	public double ESW_PUMP_DISPLACEMENT_ML_PER_REV=1.6;
	/** constant: pH sensitivity (pH units/mmol/kg) */
	public double DELTA_PH_PER_MMOL_PER_KG=2.55;//3.8257;// empirical value, orig 1.756;
	
	////////////////////////////////////////
	// Input/Output Connector Definitions
	////////////////////////////////////////
	/** Convenience constant (connector type) */
	private int OSDT_EXT=FOCEProcess.CONNECTOR_TYPE_EXT_OSDT;
	/** Convenience constant (connector type) */
	private int SIAM_REG=FOCEProcess.CONNECTOR_TYPE_SIAM_REG;
	/** Convenience constant (connector type) */
	private int SIAM_OSDT=FOCEProcess.CONNECTOR_TYPE_SIAM_OSDT;
	/** update timeout convenience constant for connector definitions */
	private long UPDDATE_TIMEOUT=30000L;
	/** update period convenience constant for (OSDT) connector definitions */
	private long UPDDATE_PERIOD=10000L;
	
	/** default boxcar filter depth */
	public int default_filter_depth=3;//10
	
	/** OSDT server name for test connectors */
	public String osdt_reg="OSDTTestServer";
	
	/* SIAM registry names
	 used for SIAM reg and SIAM OSDT 
	 data connectors
	 */
	public String ph1_reg="pH1";
	public String ph2_reg="pH2";
	public String ph3_reg="pH3";
	public String ph4_reg="pH4";
	public String ph5_reg="pH5";
	public String ph6_reg="pH6";
	public String ph7_reg="pH7";
	public String vxint_reg="Velocity";
	public String vyint_reg="Velocity";
	public String vxext_reg="ADCP";
	public String vyext_reg="ADCP";
	public String tfwd_reg="motor2";
	public String taft_reg="motor1";
	public String eswp_reg="eswPump";
	public String vfwd_reg="ValveService";
	public String vaft_reg="ValveService";

	/* SIAM registry channels */
	public String ph1_channel="pH";
	public String ph2_channel="pH";	
	public String ph4_channel="pH";
	public String ph3_channel="pH";
	public String ph5_channel="pH";
	public String ph6_channel="pH";
	public String ph7_channel="pH";
	public String vxint_channel="avgVelocityX";
	public String vyint_channel="avgVelocityY";
	public String vxext_channel="VelocityX";
	public String vyext_channel="VelocityY";
	public String tfwd_channel="encoderVelocity";//"joggingVelocity"	
	public String taft_channel="encoderVelocity";//"joggingVelocity"
	public String eswp_channel="encoderVelocity";//"joggingVelocity"
	public String vfwd_channel="position0";
	public String vaft_channel="position1";
	
	/* OSDT Test channels */
	public String ph1_ochannel="pH1/pH";
	public String ph2_ochannel="pH2/pH";	
	public String ph3_ochannel="pH3/pH";
	public String ph4_ochannel="pH4/pH";
	public String ph5_ochannel="pH5/pH";
	public String ph6_ochannel="pH6/pH";
	public String ph7_ochannel="pH7/pH";
	public String vxint_ochannel="Velocity/avgVelocityX";
	public String vyint_ochannel="Velocity/avgVelocityY";
	public String vxext_ochannel="ADCP/VelocityX";
	public String vyext_ochannel="ADCP/VelocityX";
	public String tfwd_ochannel="encoderVelocity";//"joggingVelocity"	
	public String taft_ochannel="encoderVelocity";//"joggingVelocity"
	public String eswp_ochannel="encoderVelocity";//"joggingVelocity"
	public String vfwd_ochannel="position0";
	public String vaft_ochannel="position1";
		
	/** OSDT input connector definitions (for offline testing) */
	private ConnectorSpec testConnectors[]={
	new ConnectorSpec("ph1.ifl"       ,OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_FWD_L_PH  ,PROC_INCL,osdt_reg,ph1_ochannel  ,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("ph2.ifr"       ,OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_FWD_R_PH  ,PROC_INCL,osdt_reg,ph2_ochannel  ,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("ph3.ial"       ,OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_AFT_L_PH  ,PROC_INCL,osdt_reg,ph3_ochannel  ,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("ph4.iar"       ,OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_AFT_R_PH  ,PROC_INCL,osdt_reg,ph4_ochannel  ,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("ph5.eml"       ,OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_MID_L_PH  ,PROC_INCL,osdt_reg,ph5_ochannel  ,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("ph6.emr"       ,OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_MID_R_PH  ,PROC_INCL,osdt_reg,ph6_ochannel  ,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("ph7.co2"       ,OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_ESW_PH        ,PROC_INCL,osdt_reg,ph7_ochannel  ,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("velocity.x.int",OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_X_VELOCITY,PROC_INCL,osdt_reg,vxint_ochannel,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("velocity.y.int",OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_Y_VELOCITY,PROC_INCL,osdt_reg,vyint_ochannel,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("velocity.x.ext",OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_X_VELOCITY,PROC_INCL,osdt_reg,vxext_ochannel,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("velocity.y.ext",OSDT_EXT ,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_Y_VELOCITY,PROC_INCL,osdt_reg,vyext_ochannel,UPDDATE_TIMEOUT,UPDDATE_PERIOD),
	new ConnectorSpec("thruster.fwd"  ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_FWD_THRUSTER  ,PROC_INCL,tfwd_reg,tfwd_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("thruster.aft"  ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_AFT_THRUSTER  ,PROC_INCL,taft_reg,taft_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("pump.esw"      ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_ESW_PUMP      ,PROC_INCL,eswp_reg,eswp_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("valve.esw.fwd" ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_BOOLEAN,default_filter_depth,ROLE_FWD_ESW_VALVE ,PROC_INCL,vfwd_reg,vfwd_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("valve.esw.aft" ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_BOOLEAN,default_filter_depth,ROLE_AFT_ESW_VALVE ,PROC_INCL,vaft_reg,vaft_channel  ,UPDDATE_TIMEOUT)
	};
	
	/** SIAM OSDT input connector definitions (for deployment) */
	private ConnectorSpec siamOSDTInputConnectors[]={
	new ConnectorSpec("ph1.ifl",       SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_FWD_L_PH  ,PROC_INCL,ph1_reg  ,ph1_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("ph2.ifr",       SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_FWD_R_PH  ,PROC_INCL,ph2_reg  ,ph2_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("ph3.ial",       SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_AFT_L_PH  ,PROC_INCL,ph3_reg  ,ph3_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("ph4.iar",       SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_AFT_R_PH  ,PROC_INCL,ph4_reg  ,ph4_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("ph5.eml",       SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_MID_L_PH  ,PROC_INCL,ph5_reg  ,ph5_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("ph6.emr",       SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_MID_R_PH  ,PROC_INCL,ph6_reg  ,ph6_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("ph7.co2",       SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_ESW_PH        ,PROC_INCL,ph7_reg  ,ph7_channel  ,UPDDATE_TIMEOUT),
	new ConnectorSpec("velocity.x.int",SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_X_VELOCITY,PROC_INCL,vxint_reg,vxint_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("velocity.y.int",SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_Y_VELOCITY,PROC_INCL,vyint_reg,vyint_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("velocity.x.ext",SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_X_VELOCITY,PROC_INCL,vxext_reg,vxext_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("velocity.y.ext",SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_Y_VELOCITY,PROC_INCL,vyext_reg,vyext_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("thruster.fwd",  SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_FWD_THRUSTER  ,PROC_INCL,tfwd_reg ,tfwd_channel ,UPDDATE_TIMEOUT),
	new ConnectorSpec("thruster.aft",  SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_AFT_THRUSTER  ,PROC_INCL,taft_reg ,taft_channel ,UPDDATE_TIMEOUT),
	new ConnectorSpec("pump.esw"    ,  SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_ESW_PUMP      ,PROC_INCL,eswp_reg ,eswp_channel ,UPDDATE_TIMEOUT),
	new ConnectorSpec("valve.esw.fwd" ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_BOOLEAN,default_filter_depth,ROLE_FWD_ESW_VALVE ,PROC_INCL,vfwd_reg ,vfwd_channel ,UPDDATE_TIMEOUT),
	new ConnectorSpec("valve.esw.aft" ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_BOOLEAN,default_filter_depth,ROLE_AFT_ESW_VALVE ,PROC_INCL,vaft_reg ,vaft_channel ,UPDDATE_TIMEOUT)
	};
	
	/* SIAM registry input connector definitions */
	private ConnectorSpec siamRegistryConnectors[]={
	 new ConnectorSpec("ph1.ifl",       SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_FWD_L_PH  ,PROC_INCL,ph1_reg  ,ph1_channel  ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("ph2.ifr",       SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_FWD_R_PH  ,PROC_INCL,ph2_reg  ,ph2_channel  ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("ph3.ial",       SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_AFT_L_PH  ,PROC_INCL,ph3_reg  ,ph3_channel  ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("ph4.iar",       SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_AFT_R_PH  ,PROC_INCL,ph4_reg  ,ph4_channel  ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("ph5.eml",       SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_MID_L_PH  ,PROC_INCL,ph5_reg  ,ph5_channel  ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("ph6.emr",       SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_MID_R_PH  ,PROC_INCL,ph6_reg  ,ph6_channel  ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("ph7.co2",       SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_ESW_PH        ,PROC_INCL,ph7_reg  ,ph7_channel  ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("velocity.x.int",SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_X_VELOCITY,PROC_INCL,vxint_reg,vxint_channel,UPDDATE_TIMEOUT),
	 new ConnectorSpec("velocity.y.int",SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_INT_Y_VELOCITY,PROC_INCL,vyint_reg,vyint_channel,UPDDATE_TIMEOUT),
	 new ConnectorSpec("velocity.x.ext",SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_X_VELOCITY,PROC_INCL,vxext_reg,vxext_channel,UPDDATE_TIMEOUT),
	 new ConnectorSpec("velocity.y.ext",SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_EXT_Y_VELOCITY,PROC_INCL,vyext_reg,vyext_channel,UPDDATE_TIMEOUT),
	 new ConnectorSpec("thruster.fwd",  SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_FWD_THRUSTER  ,PROC_INCL,tfwd_reg ,tfwd_channel ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("thruster.aft",  SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_AFT_THRUSTER  ,PROC_INCL,taft_reg ,taft_channel ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("pump.esw"    ,  SIAM_REG,ControlInputIF.FIELD_TYPE_DOUBLE, default_filter_depth,ROLE_ESW_PUMP      ,PROC_INCL,eswp_reg ,eswp_channel ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("valve.esw.fwd" ,SIAM_REG,ControlInputIF.FIELD_TYPE_BOOLEAN,default_filter_depth,ROLE_FWD_ESW_VALVE ,PROC_INCL,vfwd_reg ,vfwd_channel ,UPDDATE_TIMEOUT),
	 new ConnectorSpec("valve.esw.aft" ,SIAM_REG,ControlInputIF.FIELD_TYPE_BOOLEAN,default_filter_depth,ROLE_AFT_ESW_VALVE ,PROC_INCL,vaft_reg ,vaft_channel ,UPDDATE_TIMEOUT)
	 };
	
	/** Output connector definitions */
	private ConnectorSpec siamOSDTOutputConnectors[]={
	new ConnectorSpec("thruster.fwd.cmd" ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE ,0,ROLE_FWD_THRUSTER ,PROC_INCL,tfwd_reg,tfwd_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("thruster.aft.cmd" ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE ,0,ROLE_AFT_THRUSTER ,PROC_INCL,taft_reg,taft_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("pump.esw.cmd"     ,SIAM_OSDT,ControlInputIF.FIELD_TYPE_DOUBLE ,0,ROLE_ESW_PUMP     ,PROC_INCL,eswp_reg,eswp_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("valve.esw.fwd.cmd",SIAM_OSDT,ControlInputIF.FIELD_TYPE_BOOLEAN,0,ROLE_FWD_ESW_VALVE,PROC_INCL,vfwd_reg,vfwd_channel,UPDDATE_TIMEOUT),
	new ConnectorSpec("valve.esw.aft.cmd",SIAM_OSDT,ControlInputIF.FIELD_TYPE_BOOLEAN,0,ROLE_AFT_ESW_VALVE,PROC_INCL,vaft_reg,vaft_channel,UPDDATE_TIMEOUT)
	};
	
	/** Select input connectors
		Valid values are
		"TEST" - OSDT connectors for the test harness
		"SIAM_OSDT" - SIAM services connect via OSDT
		"SIAM_REG"  - SIAM service connect via SIAM registry
	 */
	public String input_connector_select="SIAM_OSDT";
	
	/** OutputConnector definitions 
	 references a privately defined array
	 */
	public String output_connector_select="SIAM_OSDT";

	/** Ensures that connectors are only selected once at initialization. 
	 */
	private boolean input_connector_select_lock=false;
	
	/** Ensures that connectors are only selected once at initialization. 
	 */
	private boolean output_connector_select_lock=false;
	
	
	////////////////////////////////////////
	// pH Control Loop Parameters
	////////////////////////////////////////
	
	/** pH setpoint (absolute). Used to calculate process error
	 signal in CONSTANT control mode
	 */
	public double ph_setpoint       =7.2;//7.625
	/** pH offset relative to external. Used to calculate process error
		signal in OFFSET control mode
	 */
	public double ph_offset         =-0.4;//0.0
	/** (unused)*/
	public double ph_deadband_lo    =3.5;
	/** (unused)*/
	public double ph_deadband_hi    =4.0;
	/** lower pH valid reading (used by input connector range validators */
	public double ph_valid_lo       = 5.5;
	/** upper pH valid reading (used by input connector range validators */
	public double ph_valid_hi       = 8.0;
	/** max change limit for ESW pump (motor rpm) */
	public double ph_eswpump_max_change = 50.0;
	/** max pH control correction limit */
	public double ph_max_correction =1.5;//0.3;//1.5;
	
	/** pH PID proportional gain */
	public float ph_pid_Kp=25.0f;
	/** pH PID integral gain */
	public float ph_pid_Ki=0.0f;
	/** pH PID derivative gain */
	public float ph_pid_Kd=100.0f;
	/** pH PID max Ki (to prevent integral wind-up) */
	public float ph_pid_max_ki=Long.MAX_VALUE/2.0f;
	/** pH PID divisor (divides raw correction sum: correction=(Pterm+Iterm+Dterm)/scaleFactor) */
	public float ph_pid_scale_factor=256.0f;//128
	/** enable OSDT publishing for testing (in addition to control loop OSDT publishing) */
	boolean ph_pid_enable_turbinator=false;

	/** pH Linear Response filter depth */
	public int ph_lin_filter_depth=1;
	/** pH Linear Response slope */
	public double ph_lin_slope=0.0;
	/** pH Linear Response offset */
	public double ph_lin_offset=0.0;
	/** pH Linear Response error limit
		(the error value below which the
		error term is applied)
	 */
	public double ph_lin_error_limit=0.0;
	
	/** ESW pump min speed (motor rpm)*/
	double esw_pump_min_rpm=50.0; 
	/** ESW pump max speed (motor rpm)*/
	double esw_pump_max_rpm=5000.0;
	
	/** (unused) */
	public double ph_abs_max        =7.0;
	/** (unused) */
	public double ph_abs_min        =2.5;
	
	/** (unused)*/
	public double exp_a=0.025;
	/** (unused)*/
	public double exp_b=2.35;
	/** (unused)*/
	public double exp_h=1.0;
	/** (unused)*/
	public double exp_k=0.0;
	
	
	////////////////////////////////////////
	// Velocity Control Loop Parameters
	////////////////////////////////////////
	
	/** water velocity setpoint (absolute). Used to calculate process error
	 signal in CONSTANT control mode
	 */
	double velocity_setpoint       =0.0;
	/** velocity offset relative to external. Used to calculate process error
	 signal in OFFSET control mode
	 */	
	double velocity_offset         =-3.0;
	/** velocity deadband min. Used to calculate process error
	 signal in DEADBAND control mode
	 */	
	double velocity_deadband_lo    =-1.0;
	/** velocity deadband max. Used to calculate process error
	 signal in DEADBAND control mode
	 */	
	double velocity_deadband_hi    =1.0;
	/** velocity calibration constant (MotorRPM= aV^2+bV+c),
	 where V is water velocity (cm/sec)
	 */
	double velocity_cal_a          =0.0;
	/** velocity calibration constant (MotorRPM= aV^2+bV+c),
	 where V is water velocity (cm/sec)
	 */
	double velocity_cal_b          =50.0;
	/** velocity calibration constant (MotorRPM= aV^2+bV+c),
	 where V is water velocity (cm/sec)
	 */
	double velocity_cal_c          =0.0;
	/** max change limit for thruster motor speed (motor rpm) */
	double velocity_thruster_max_change = 50.0;
	
	/** velocity PID proportional gain */
	public float velocity_pid_Kp=25.0f;
	/** velocity PID integral gain */
	public float velocity_pid_Ki=0.3f;
	/** velocity PID derivative gain */
	public float velocity_pid_Kd=250.0f;
	/** velocity PID max Ki (to prevent integral wind-up) */
	public float velocity_pid_max_ki=Long.MAX_VALUE/2.0f;
	/** velocity PID divisor (divides raw correction sum: correction=(Pterm+Iterm+Dterm)/scaleFactor) */
	public float velocity_pid_scale_factor=256.0f;
	/** (unused)*/
	double max_flow_change_percent=1.0;
	/** enable OSDT publishing for testing (in addition to control loop OSDT publishing) */
	boolean velocity_pid_enable_turbinator=false;
	
	/** thruster min speed (motor rpm) */
	double velocity_min_rpm        =1.0;
	/** thruster max speed (motor rpm) */
	double velocity_max_rpm        =300.0;
	/** max change limit for thruster PID */
	double velocity_max_correction =50.0;

	////////////////////////////////////////
	// Control Loop Configuration
	////////////////////////////////////////
	
	/** Control loop update period */
	public long loop_period_msec=10000;
	
	/** I/O monitor update period */
	public long monitor_period_msec=10000;
	
	/** SIAM host (for registry input connectors) */
	public String siamHost="localhost";//"focetest3";//"localhost";//"focetest3";
	
	/** OSDT host (for OSDT input connectors) */
	public String osdtHost="localhost:3333";
	
	/** enable/disable input state logging */
	public boolean logInputState=true;
	
	/** enable/disable output state logging */
	public boolean logOutputState=true;
	
	////////////////////////////////////////
	// Control Loop State: Mode Settings
	////////////////////////////////////////
	
	/** pH control mode.
	 Valid values:
	 CONTROL_MODE_MANUAL   : set manually (no closed loop)
	 CONTROL_MODE_CONSTANT : maintain setpoint
	 CONTROL_MODE_OFFSET   : follow specified offset from reference
	 */
	int ph_control_mode=CONTROL_MODE_MANUAL;
	
	/** velocity control mode.
	 Valid values:
	 CONTROL_MODE_MANUAL   : set manually (no closed loop)
	 CONTROL_MODE_CONSTANT : maintain setpoint
	 CONTROL_MODE_OFFSET   : follow specified offset from reference
	 CONTROL_MODE_DEADBAND : maintain outside of deadband
	 */
	int velocity_control_mode=CONTROL_MODE_MANUAL;
	
	/** Response mode 
	 valid values:
	 RESPONSE_MODE_EXP : exponential (not implemented)
	 RESPONSE_MODE_PID : PID response
	 RESPONSE_MODE_LIN : Linear response
	 */
	int ph_response_mode=RESPONSE_MODE_LIN;

	/** Response mode 
	 valid values:
	 RMODE_EXP : exponential (not implemented)
	 RMODE_PID : PID response
	 */
	int velocity_response_mode=RESPONSE_MODE_LIN;
	
	/** Actuation mode 
	 valid values:
	  ACTUATION_ENABLED  : enable control loops to do actuation 
	  ACTUATION_DISABLED : calculate commands but do not perform actuation 
	 */	
	int esw_pump_actuation=ACTUATION_DISABLED;

	/** Actuation mode 
	 valid values:
	 ACTUATION_ENABLED  : enable control loops to do actuation 
	 ACTUATION_DISABLED : calculate commands but do not perform actuation 
	 */	
	int esw_valve_actuation=ACTUATION_DISABLED;

	/** Actuation mode 
	 valid values:
	 ACTUATION_ENABLED  : enable control loops to do actuation 
	 ACTUATION_DISABLED : calculate commands but do not perform actuation 
	 */	
	int thruster_actuation=ACTUATION_DISABLED;
	
	
	////////////////////////////////////////
	// Constructors
	////////////////////////////////////////
	
	/** Standard Constructor */
	public ControlLoopAttributes(DeviceServiceIF service) {
		super(service);
	}
	
	/** Standard Constructor (with dummy arg to differentiate signatures) */
	public ControlLoopAttributes(DeviceServiceIF service,boolean dummyArg) {
		this(service);
		isiID=9999;
		locationName="right here";
		rbnbServer=osdtHost;
		serviceName="ControlLoop".getBytes();
		advertiseService=true;
		registryName="regControlLoop";
		//rbnbCacheFrames=1024;
		//rbnbArchiveFrames=1024;
	}
	
	////////////////////////////////////////
	// Validation Methods
	////////////////////////////////////////

	/** validate whether numeric attribute (as String) value falls inside/outside specified range. 
	 @param valueString number to validate (String)
	 @param min range minimum
	 @param max range maximum
	 @param inside if true, compare to inside of range; otherwise compare to outside
	 @param includeEnds include endpoints (min/max) in validation
	 
	 @throws InvalidPropertyException if number is part of the specified range
	 
	 */
	public void validateDouble(String valueString, double min, double max, boolean inside, boolean includeEnds) 
	throws InvalidPropertyException{
		double value;
		try{
			value=Double.parseDouble(valueString);
		}catch (Exception e) {
			throw new InvalidPropertyException("parse error for "+valueString);
		}
		if(inside){
			if(includeEnds){
				if((value<min) || (value>max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value<=min) || (value>=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}else{
			if(includeEnds){
				if((value>min) || (value<max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value>=min) || (value<=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}
	}
	
	/** validate whether numeric attribute (as String) value falls inside/outside specified range. 
	 @param valueString number to validate (String)
	 @param min range minimum
	 @param max range maximum
	 @param inside if true, compare to inside of range; otherwise compare to outside
	 @param includeEnds include endpoints (min/max) in validation
	 
	 @throws InvalidPropertyException if number is part of the specified range
	 
	 */
	public void validateFloat(String valueString, float min, float max, boolean inside, boolean includeEnds) 
	throws InvalidPropertyException{
		float value;
		try{
			value=Float.parseFloat(valueString);
		}catch (Exception e) {
			throw new InvalidPropertyException("parse error for "+valueString);
		}
		if(inside){
			if(includeEnds){
				if((value<min) || (value>max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value<=min) || (value>=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}else{
			if(includeEnds){
				if((value>min) || (value<max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value>=min) || (value<=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}
	}
	
	/** validate whether numeric attribute (as String) value falls inside/outside specified range. 
	 @param valueString number to validate (String)
	 @param min range minimum
	 @param max range maximum
	 @param inside if true, compare to inside of range; otherwise compare to outside
	 @param includeEnds include endpoints (min/max) in validation
	 
	 @throws InvalidPropertyException if number is part of the specified range
	 
	 */
	public void validateInt(String valueString, int min, int max, boolean inside, boolean includeEnds) 
	throws InvalidPropertyException{
		int value;
		try{
			value=Integer.parseInt(valueString);
		}catch (Exception e) {
			throw new InvalidPropertyException("parse error for "+valueString);
		}
		if(inside){
			if(includeEnds){
				if((value<min) || (value>max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value<=min) || (value>=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}else{
			if(includeEnds){
				if((value>min) || (value<max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value>=min) || (value<=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}
	}
	
	/** validate whether numeric attribute (as String) value falls inside/outside specified range. 
	 @param valueString number to validate (String)
	 @param min range minimum
	 @param max range maximum
	 @param inside if true, compare to inside of range; otherwise compare to outside
	 @param includeEnds include endpoints (min/max) in validation
	 	 
	 */
	public void validateLong(String valueString, long min, long max, boolean inside, boolean includeEnds) 
	throws InvalidPropertyException{
		long value;
		try{
			value=Long.parseLong(valueString);
		}catch (Exception e) {
			throw new InvalidPropertyException("parse error for "+valueString);
		}
		if(inside){
			if(includeEnds){
				if((value<min) || (value>max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value<=min) || (value>=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}else{
			if(includeEnds){
				if((value>min) || (value<max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value>=min) || (value<=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
			
		}
	}
	
	/** validate whether numeric attribute (as String) value falls inside/outside specified range. 
	 @param valueString number to validate (String)
	 @param min range minimum
	 @param max range maximum
	 @param inside if true, compare to inside of range; otherwise compare to outside
	 @param includeEnds include endpoints (min/max) in validation
	 
	 @throws InvalidPropertyException if number is part of the specified range
	 
	 */
	public void validateShort(String valueString, short min, short max, boolean inside, boolean includeEnds) 
	throws InvalidPropertyException{
		short value;
		try{
			value=Short.parseShort(valueString);
		}catch (Exception e) {
			throw new InvalidPropertyException("parse error for "+valueString);
		}
		if(inside){
			if(includeEnds){
				if((value<min) || (value>max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value<=min) || (value>=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}else{
			if(includeEnds){
				if((value>min) || (value<max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value>=min) || (value<=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
			
		}
	}
	
	/**
     * Called when specified attribute has been found. Throw
     * InvalidPropertyException if specified attribute has invalid value.
     * Note that the ServiceAttributes base class automatically validates
     * the value type before setAttributeCallback() is invoked; so this
     * method needs only to validate the value.
     * 
     * @param attributeName  name of parsed attribute
     * @param valueString  value of parsed attribute (String)
     */
    protected void setAttributeCallback(String attributeName, String valueString)
	throws InvalidPropertyException {
		if(attributeName.equals("exp_a")){
			validateDouble(valueString,0.0,100.0,true,true);
		}else if(attributeName.equals("exp_b")){
			validateDouble(valueString,0.0,100.0,true,true);
		}else if(attributeName.equals("exp_h")){
			validateDouble(valueString,0.0,100.0,true,true);
		}else if(attributeName.equals("exp_k")){
			validateDouble(valueString,0.0,100.0,true,true);
		}else if(attributeName.equals("ph_pid_Kp")){
			validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,true);
		}else if(attributeName.equals("ph_pid_Ki")){
			validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,true);
		}else if(attributeName.equals("ph_pid_Kd")){
			validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,true);
		}else if(attributeName.equals("ph_pid_scale_factor")){
			validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,false);
		}else if(attributeName.equals("ph_lin_filter_depth")){
			validateInt(valueString,1,(int)Integer.MAX_VALUE,true,true);
		}else if(attributeName.equals("ph_lin_slope")){
			validateDouble(valueString,-(double)Double.MAX_VALUE,(double)Double.MAX_VALUE,true,false);
		}else if(attributeName.equals("ph_lin_offset")){
			validateDouble(valueString,-(double)Double.MAX_VALUE,(double)Double.MAX_VALUE,true,false);
		}else if(attributeName.equals("ph_lin_error_limit")){
			validateDouble(valueString,-(double)Double.MAX_VALUE,(double)Double.MAX_VALUE,true,false);
		}else if(attributeName.equals("ph_setpoint")){
			validateDouble(valueString,5.0,8.0,true,true);
		}else if(attributeName.equals("ph_offset")){
			validateDouble(valueString,-1.0,1.0,true,true);
		}else if(attributeName.equals("max_flow_change_percent")){
			validateDouble(valueString,-2.0,2.0,true,true);
		}else if(attributeName.equals("ph_abs_min")){
			validateDouble(valueString,2.5,7.0,true,true);
		}else if(attributeName.equals("ph_abs_max")){
			validateDouble(valueString,2.5,7.0,true,true);
		}else if(attributeName.equals("ph_deadband_lo")){
			validateDouble(valueString,3.0,4.0,true,true);
		}else if(attributeName.equals("ph_deadband_hi")){
			validateDouble(valueString,3.0,4.0,true,true);
		}else if(attributeName.equals("ph_max_correction")){
			validateDouble(valueString,0.0001,5.0,true,true);
		}else if(attributeName.equals("ph_control_mode")){
			validateInt(valueString,CONTROL_MODE_MIN,CONTROL_MODE_MAX,true,true);
		}else if(attributeName.equals("ph_response_mode")){
			validateInt(valueString,RESPONSE_MODE_MIN,RESPONSE_MODE_MAX,true,true);
		}else if(attributeName.equals("ph_actuation")){
			validateInt(valueString,ACTUATION_MIN,ACTUATION_MAX,true,true);
		}else if(attributeName.equals("velocity_pid_Kp")){
			 validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,true);
		 }else if(attributeName.equals("velocity_pid_Ki")){
			 validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,true);
		 }else if(attributeName.equals("velocity_pid_Kd")){
			 validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,true);
		 }else if(attributeName.equals("velocity_pid_scale_factor")){
			 validateFloat(valueString,0.0f,(float)Short.MAX_VALUE,true,false);
		 }else if(attributeName.equals("velocity_setpoint")){
			validateDouble(valueString,-10.0,10.0,true,true);
		}else if(attributeName.equals("velocity_offset")){
			validateDouble(valueString,-5.0,5.0,true,true);
		}else if(attributeName.equals("velocity_max_correction")){
			validateDouble(valueString,0.01,1000.0,true,true);
		}else if(attributeName.equals("velocity_min_rpm")){
			validateDouble(valueString,-500.0,500.0,true,true);
		}else if(attributeName.equals("velocity_max_rpm")){
			validateDouble(valueString,-600,600,true,true);
		}else if(attributeName.equals("velocity_deadband_lo")){
			validateDouble(valueString,-3.0,3.0,true,true);
		}else if(attributeName.equals("velocity_deadband_hi")){
			validateDouble(valueString,-10.0,10.0,true,true);
			// must be gt deadband lo
		}else if(attributeName.equals("velocity_control_mode")){
			validateInt(valueString,CONTROL_MODE_MIN,CONTROL_MODE_MAX,true,true);
		}else if(attributeName.equals("velocity_response_mode")){
			validateInt(valueString,RESPONSE_MODE_MIN,RESPONSE_MODE_MAX,true,true);
		}else if(attributeName.equals("velocity_actuation")){
			validateInt(valueString,ACTUATION_DISABLED,ACTUATION_ENABLED,true,true);
		}else if(attributeName.equals("loop_period_msec")){
			validateLong(valueString,2500L,120000L,true,true);
		}else if(attributeName.equals("monitor_period_msec")){
			validateLong(valueString,10000L,600000L,true,true);
		}else if(attributeName.equals("esw_pump_min_rpm")){
			validateDouble(valueString,50.0,5000.0,true,true);
		}else if(attributeName.equals("esw_pump_max_rpm")){
			validateDouble(valueString,50.0,5000.0,true,true);
		}else if(attributeName.equals("input_connector_select")){
			if(input_connector_select_lock){
				//do nothing
				//throw new InvalidPropertyException("input_connector_select may only be set at service initialization");
			}else if ( !valueString.equalsIgnoreCase("TEST") &&
				!valueString.equalsIgnoreCase("SIAM_OSDT") &&
				!valueString.equalsIgnoreCase("SIAM_REG")) {
				throw new InvalidPropertyException("Invalid input connector set ["+valueString+"] - Valid values are \"TEST\",\"SIAM_OSDT\", \"SIAM_REG\"");
			}else{
				input_connector_select=valueString;
				input_connector_select_lock=true;
			}
		}
		else if(attributeName.equals("output_connector_select")){
			if(output_connector_select_lock){
				//do nothing
				//throw new InvalidPropertyException("output_connector_select may only be set at service initialization");
			}else if (!valueString.equalsIgnoreCase("SIAM_OSDT")) {
				throw new InvalidPropertyException("Invalid input connector set ["+valueString+"] - Valid values are \"SIAM_OSDT\"");
			}else{
				output_connector_select=valueString;
				output_connector_select_lock=true;
			}
		}
		/* if new connector parameters are set, 
			try to re-start connection
		if(attributeName.equals("")){
		}
		*/
    }
	
	////////////////////////////////////////
	// Other Methods
	////////////////////////////////////////
	
	/** Return (some) attributes as a String */
	public String toString(){
		StringBuffer buffer=new StringBuffer();
		for(Iterator i=attributes();i.hasNext();){
			NumberAttribute nextAttribute=(NumberAttribute)i.next();
			buffer.append(nextAttribute.name()+":"+nextAttribute+"\n");
		}
		// could add String, boolean attributes too...
		return buffer.toString();
	}
	/**
	 use registryKey to get OSDT name from ConnectorSpec in _attributes
	 if OSDT source name is not explicitly defined in ConnectorSpec, the registryKey is
	 used as the OSDT source name
	 */
	public String registry2osdt(String registryName){
		ConnectorSpec[] inputConnectors=getInputConnectors(this.input_connector_select);
		for(int i=0;i<inputConnectors.length;i++){
			if(inputConnectors[i].registry_key.equals(registryName)){
				if(inputConnectors[i].osdt_key!=null){
					// osdt name is defined, use it
					return inputConnectors[i].osdt_key;
				}else{
					// osdt name is null, use the registry key
					return inputConnectors[i].registry_key;
				}
			}
		}
		// not found
		return null;
	}
	
	/** Get mnemonic for mode with specified ID */
	public static String modeName(int mode){
		return (String)mode_id2pname.get(new Integer(mode));
	}
	
	/** Get mnemonic for functional role with specified ID */	
	public static String roleName(int role) {
		return (String)role_id2name.get(new Integer(role));
	}
	
	/** InputConnector definitions 
	 Selects a defined set of input connector definitions:
	 - one for test (using OSDT test server for sensor inputs)
	 - two for deployment (using SIAM OSDT or registry for sensor inputs)
	 */
	public ConnectorSpec[] getInputConnectors(String id){
		if (id.equalsIgnoreCase("TEST")) {
			return this.testConnectors;
		}else if(id.equalsIgnoreCase("SIAM_OSDT")) {
			return this.siamOSDTInputConnectors;
		}else if(id.equalsIgnoreCase("SIAM_REG")) {
			return this.siamRegistryConnectors;
		}
		return null;
	}
	
	/** OutputConnector definitions 
	 Selects a defined set of input connector definitions:
	 - one for test (using OSDT test server for sensor inputs)
	 - two for deployment (using SIAM OSDT or registry for sensor inputs)
	 */
	public ConnectorSpec[] getOutputConnectors(String id){
		// for now, there's only one set, so you can't go wrong
		return siamOSDTOutputConnectors;
	}
	
	/** return Iterator of input ConnectorSpecs */
	public ConnectorSpec connectorSpec(int role){
		Vector v=new Vector();
		ConnectorSpec[] inputConnectors=getInputConnectors(this.input_connector_select);

		for(int i=0;i<inputConnectors.length;i++){
			if(inputConnectors[i].role()==role){
				return inputConnectors[i];
			}
		}
		return null;
	}
	
	
	/** return Iterator of input ConnectorSpecs */
	public Iterator inputConnectors(int type){
		Vector v=new Vector();
		ConnectorSpec[] inputConnectors=getInputConnectors(this.input_connector_select);

		for(int i=0;i<inputConnectors.length;i++){
			if(inputConnectors[i].type()==type || type==TYPE_ALL){
				v.add(inputConnectors[i]);
			}
		}
		return v.iterator();
	}
	
	/** return Iterator of output ConnectorSpecs */
	public Iterator outputConnectors(int type){
		Vector v=new Vector();
		ConnectorSpec[] outputConnectors=getOutputConnectors(this.output_connector_select);
		for(int i=0;i<outputConnectors.length;i++){
			if(outputConnectors[i].type()==type || type==TYPE_ALL){
				v.add(outputConnectors[i]);
			}
		}
		return v.iterator();
	}
	
	public int filterDepth(int filterID)
	throws Exception{		
		switch(filterID){
				// internal pH boxcar
			case BX_PH_INT_FWD_L:
			case BX_PH_INT_FWD_R:
			case BX_PH_INT_AFT_L:
			case BX_PH_INT_AFT_R:
				// external pH boxcar
			case BX_PH_EXT_MID_L:
			case BX_PH_EXT_MID_R:
				return 2;

				// ESW boxcar
			case BX_PH_ESW:
				// internal/external H20 velocity boxcars
			case BX_VH2O_INT_X:
			case BX_VH2O_INT_Y:
			case BX_VH2O_EXT_X:
			case BX_VH2O_EXT_Y:
				// forward/aft thruster velocity boxcars
			case BX_VTHR_FWD:				
			case BX_VTHR_AFT:
				return default_filter_depth;
			default:
				throw new Exception("Unsupported filter ID ["+filterID+"]");
		}		
	}
	
	/** Return iterator of numeric attributes 
	 @see Attribute
	 */
	public Iterator attributes(){
		
		NumberAttribute[] attributes={
			new NumberAttribute(Attribute.TYPE_DOUBLE,"exp_a",new Double(exp_a)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"exp_b",new Double(exp_b)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"exp_h",new Double(exp_h)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"exp_k",new Double(exp_k)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_pid_Kp",new Double(ph_pid_Kp)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_pid_Ki",new Double(ph_pid_Ki)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_pid_Kd",new Double(ph_pid_Kd)),
			new NumberAttribute(Attribute.TYPE_INT,"ph_lin_filter_depth",new Integer(ph_lin_filter_depth)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_lin_offset",new Double(ph_lin_offset)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_lin_slope",new Double(ph_lin_slope)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_lin_error_limit",new Double(ph_lin_error_limit)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_abs_max",new Double(ph_abs_max)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_abs_min",new Double(ph_abs_min)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_max_correction",new Double(ph_max_correction)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_setpoint",new Double(ph_setpoint)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_offset",new Double(ph_offset)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_deadband_lo",new Double(ph_deadband_lo)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"ph_deadband_hi",new Double(ph_deadband_hi)),
			new NumberAttribute(Attribute.TYPE_INT   ,"ph_control_mode",new Integer(ph_control_mode),modeName(ph_control_mode)),
			new NumberAttribute(Attribute.TYPE_INT   ,"ph_response_mode",new Integer(ph_response_mode),modeName(ph_response_mode)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_pid_Kp",new Double(velocity_pid_Kp)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_pid_Ki",new Double(velocity_pid_Ki)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_pid_Kd",new Double(velocity_pid_Kd)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_min_rpm",new Double(velocity_min_rpm)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_max_rpm",new Double(velocity_max_rpm)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_max_correction",new Double(velocity_max_correction)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_setpoint",new Double(velocity_setpoint)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_offset",new Double(velocity_offset)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_deadband_lo",new Double(velocity_deadband_lo)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_deadband_hi",new Double(velocity_deadband_hi)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_cal_a",new Double(velocity_cal_a)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_cal_b",new Double(velocity_cal_b)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"velocity_cal_c",new Double(velocity_cal_c)),
			new NumberAttribute(Attribute.TYPE_INT   ,"velocity_control_mode",new Integer(velocity_control_mode),modeName(velocity_control_mode)),
			new NumberAttribute(Attribute.TYPE_INT   ,"velocity_response_mode",new Integer(velocity_response_mode),modeName(velocity_response_mode)),
			new NumberAttribute(Attribute.TYPE_INT   ,"esw_pump_actuation",new Integer(esw_pump_actuation),modeName(esw_pump_actuation)),
			new NumberAttribute(Attribute.TYPE_INT   ,"esw_valve_actuation",new Integer(esw_valve_actuation),modeName(esw_valve_actuation)),
			new NumberAttribute(Attribute.TYPE_INT   ,"thruster_actuation",new Integer(thruster_actuation),modeName(thruster_actuation)),
			new NumberAttribute(Attribute.TYPE_DOUBLE,"max_flow_change_percent",new Double(max_flow_change_percent)),
			new NumberAttribute(Attribute.TYPE_LONG,"loop_period_msec",new Long(loop_period_msec)),			
			new NumberAttribute(Attribute.TYPE_LONG,"monitor_period_msec",new Long(monitor_period_msec))			
		};
		return Arrays.asList(attributes).iterator();
	}
	
	////////////////////////////////////////
	// Internal Classes
	////////////////////////////////////////
	
	/** ConnectorSpec: encapsulates input/output connection definition */
	public class ConnectorSpec{
		public String signal_name;
		public int connector_type;
		public int field_type;
		public int filter_depth;
		public int property_flags;
		public int process_flags;
		public String registry_key;
		public String signal_key;
		public String osdt_key=null;
		public long update_period_ms=-1L;
		public long update_timeout_ms=-1L;
		
		/** SIAM registry ConnectorSpec (no worker) */
		public ConnectorSpec(String SignalName,
							 int TypeID,
							 int FieldType,
							 int FilterDepth,
							 int PropertyFlags,
							 int ProcessFlags,
							 String RegistryKey,
							 String SignalKey,
							 long UpdateTimeout,
							 long UpdatePeriod){
			signal_name=SignalName;
			connector_type=TypeID;
			field_type=FieldType;
			filter_depth=FilterDepth;
			property_flags=PropertyFlags;
			process_flags=ProcessFlags;
			registry_key=RegistryKey;
			signal_key=SignalKey;
			update_timeout_ms=UpdateTimeout;
			update_period_ms=UpdatePeriod;
		}
		
		/** SIAM registry ConnectorSpec (no worker) */
		public ConnectorSpec(String SignalName,
							 int TypeID,
							 int FieldType,
							 int FilterDepth,
							 int PropertyFlags,
							 int ProcessFlags,
							 String RegistryKey,
							 String SignalKey,
							 long UpdateTimeout){
			this(SignalName,TypeID,FieldType,FilterDepth,PropertyFlags,ProcessFlags,RegistryKey,SignalKey,UpdateTimeout,OSDTInputConnector.DEFAULT_UPDATE_PERIOD);
		}
		
		/** OSDT ConnectorSpec (default worker period) */
		public ConnectorSpec(String SignalName,
							 int TypeID,
							 int FieldType,
							 int FilterDepth,
							 int PropertyFlags,
							 int ProcessFlags,
							 String RegistryKey,
							 String SignalKey,
							 String OSDTKey,
							long UpdateTimeout){
			this(SignalName,TypeID,FieldType,FilterDepth,PropertyFlags,ProcessFlags,RegistryKey,SignalKey,OSDTKey,UpdateTimeout,OSDTInputConnector.DEFAULT_UPDATE_PERIOD);
		}
		/** OSDT ConnectorSpec (explicit worker period and timeout) */
		public ConnectorSpec(String SignalName,
							 int TypeID,
							 int FieldType,
							 int FilterDepth,
							 int PropertyFlags,
							 int ProcessFlags,
							 String RegistryKey,
							 String SignalKey,
							 String OSDTKey,
							 long UpdateTimeout,
							 long UpdatePeriod){
			this(SignalName,TypeID,FieldType,FilterDepth,PropertyFlags,ProcessFlags,RegistryKey,SignalKey,UpdateTimeout,UpdatePeriod);
			osdt_key=OSDTKey;
		}
		
		public int role(){
			return (property_flags & ControlLoopAttributes.ROLE_MASK);
		}
		public int type(){
			return (property_flags & ControlLoopAttributes.TYPE_MASK);
		}
	}
	
	
	/** Encapsulates an (primative) attribute, it's type, value and (mnemonic) name.
		Simplifies reporting/printing of attributes, and grouping by type;
		used by toString() to print name and mnemonic.
	 */
	public abstract class Attribute{
		public final static int TYPE_INT     =10;
		public final static int TYPE_LONG    =20;
		public final static int TYPE_FLOAT   =30;
		public final static int TYPE_DOUBLE  =40;
		public final static int TYPE_HEX     =50;
		public final static int TYPE_BOOLEAN =100;
		public final static int TYPE_STRING  =200;
		int _type;
		String _name=null;
		String _mnemonic=null;
		public abstract String toString();
		public String name(){
			return _name;
		}
	}
	
	/** Numeric Attribute.
		@see Attribute
	 */
	public class NumberAttribute extends Attribute{
		Number _numberValue=null;
		public NumberAttribute(int type, String name, Number numberValue){
			this(type,name,numberValue,null);
		}
		public NumberAttribute(int type, String name, Number numberValue, String mnemonic){
			_name=name;
			_type=type;
			_numberValue=numberValue;
			_mnemonic=mnemonic;
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("mnemonic:"+_mnemonic);
			//}
		}
		public int intValue(){return _numberValue.intValue();}
		public long longValue(){return _numberValue.longValue();}
		public short shortValue(){return _numberValue.shortValue();}
		public float floatValue(){return _numberValue.floatValue();}
		public double doubleValue(){return _numberValue.doubleValue();}
		public String hexValue(){
			return Long.toHexString(_numberValue.longValue());
		}
		public String toString(){
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("mnemonic:"+_mnemonic);
			//}
			if(_mnemonic==null){
				return _numberValue.toString();
			}
			else{
				return _numberValue.toString()+" ["+_mnemonic+"]";
			}
		}
	}
	
	/** String Attribute.
	 @see Attribute
	 */
	public class StringAttribute extends Attribute{
		String _stringValue=null;
		public StringAttribute(String name, String stringValue){
			this(name,stringValue,null);
		}
		public StringAttribute(String name, String stringValue, String mnemonic){
			_name=name;
			_type=TYPE_STRING;
			_stringValue=stringValue;
			_mnemonic=mnemonic;
		}
		public String value(){
			return _stringValue;
		}
		public String toString(){
			if(_mnemonic==null){
				return _stringValue.toString();
			}
			else{
				return _stringValue.toString()+" ["+_mnemonic+"]";
			}
		}
	}
	/** Boolean Attribute.
	 @see Attribute
	 */
	public class BooleanAttribute extends Attribute{
		Boolean _booleanValue=null;
		public BooleanAttribute(String name, boolean booleanValue){
			this(name,booleanValue,null);
		}
		public BooleanAttribute(String name, boolean booleanValue, String mnemonic){
			_name=name;
			_type=TYPE_BOOLEAN;
			_booleanValue=new Boolean(booleanValue);
			_mnemonic=mnemonic;
		}
		public boolean value(){
			return _booleanValue.booleanValue();
		}
		public String toString(){
			if(_mnemonic==null){
				return _booleanValue.toString();
			}
			else{
				return _booleanValue.toString()+" ["+_mnemonic+"]";
			}
		}
	}
	 
	
}
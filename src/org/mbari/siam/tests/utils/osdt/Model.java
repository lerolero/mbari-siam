/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.osdt;

import java.util.Properties;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Vector;
import java.net.*;
import java.io.*;
import java.text.NumberFormat;

import com.rbnb.sapi.*;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;


import org.mbari.siam.foce.devices.controlLoop.OSDTConnectorWorker;
import org.mbari.siam.foce.devices.controlLoop.OSDTInputConnector;
import org.mbari.siam.foce.devices.controlLoop.InputConnector;
import org.mbari.siam.foce.devices.controlLoop.WorkerThread;
import org.mbari.siam.foce.utils.CLOTHProtocol;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.utils.TCPProtocol;
import org.mbari.siam.utils.TCPClient;
import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.BoxcarFilter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.tests.utils.osdt.OSDTTestProtocol;

public class Model extends WorkerThread{

	public static final int ID_PHCORR=0;
	public static final int ID_PHPUMPCMD=1;
	public static final int ID_VELCORR=2;
	public static final int ID_VELXINT=3;
	public static final int ID_TFWDCMD=4;
	public static final int ID_TAFTCMD=5;
	public static final int ID_PSCALE=6;
	public static final int ID_ISCALE=7;
	public static final int ID_TSHIFT=8;
	public static final int ID_USEVCONST=9;
	public static final int ID_VXCONST=10;
	public static final int ID_VYCONST=11;
	public static final int ID_VELA=12;
	public static final int ID_VELB=13;
	public static final int ID_VELC=14;
	public static final int ID_PHBOXCARDEPTH=15;
	public static final int ID_PH1=16;
	public static final int ID_PH2=17;
	public static final int ID_PH3=18;
	public static final int ID_PH4=19;
	public static final int ID_PH5=20;
	public static final int ID_PH6=21;

	double phCorr;
	double _lastPHCORR;
	double phPumpCmd;
	double velCorr;
	double ph1;
	double ph2;
	double ph3;
	double ph4;
	double ph5;
	double ph6;
	double velXInt;
	double xInt,yInt;
	double xExt,yExt;
	double tfwdCmd;
	double taftCmd;

	double pscale=1.0;
	double iscale=0.2;
	long tshift=0L;
	int useVconst=0;
	double vxconst=0.0;
	double vyconst=0.0;
	double vel_a=0.0;
	double vel_b=50.0;
	double vel_c=0.0;
	int phBoxcarDepth=10;
	
	NumberFormat nf=null;
	NumberFormat inf=null;
	BoxcarFilter phBoxcar;
	FilterInput  phBoxcarIn;

	Vector paramNames=null;
	Vector channels=null;
	
	static protected Logger _log4j = Logger.getLogger(Model.class);  

	String _osdtHost="localhost";
	int _osdtPort=3333;
	String _testServerHost="localhost";
	int _testServerPort=4445;
	String _phSource="PID_PH";
	String _vxSource="PID_VELOCITY";
	String _testSource="OSDTTestServer";
	String _useTestPump="FALSE";
	String _useTestThrusters="FALSE";
	
	OSDTTestClient _testClient;
	OSDTTestProtocol _protocol;
	long _startTime;

	double phNominal=7.625;
	double phMin=4.0;
	double phMax=8.5;

	long _phDriftStartTime,_phDriftTime=180000L;
	double _phDriftRate=0.3/120000;// pH units/msec
	final static int DRIFT_STATE_DRIVEN=0;
	final static int DRIFT_STATE_DRIFTING=1;
	int _phDriftState=DRIFT_STATE_DRIFTING;
	long _lastUpdateTime;
	
	double _currentPeriodScale=1.0;
	
	public Model(){
		super();
		nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(5);
		nf.setMinimumFractionDigits(3);
		nf.setMinimumIntegerDigits(1);
		nf.setGroupingUsed(false);		
		inf=NumberFormat.getInstance();
		inf.setMaximumFractionDigits(0);
		inf.setMinimumFractionDigits(0);
		inf.setMinimumIntegerDigits(1);
		inf.setGroupingUsed(false);		
		_protocol=new OSDTTestProtocol();
		channels=new Vector();
	}
	public Model(long delayMillis){
		super(delayMillis);
		nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(5);
		nf.setMinimumFractionDigits(3);
		nf.setMinimumIntegerDigits(1);
		nf.setGroupingUsed(false);		
		inf=NumberFormat.getInstance();
		inf.setMaximumFractionDigits(0);
		inf.setMinimumFractionDigits(0);
		inf.setMinimumIntegerDigits(1);
		inf.setGroupingUsed(false);		
		_protocol=new OSDTTestProtocol();
	}
	public Model(String osdtHost, int osdtPort, String testHost, int testPort, long delayMillis){
		this(delayMillis);
		_osdtHost=osdtHost;
		_osdtPort=osdtPort;
		_testServerHost=testHost;
		_testServerPort=testPort;
	}
	
	public void setOSDTHost(String host){
		_osdtHost=host;
	}
	public void setOSDTPort(int port){
		_osdtPort=port;
	}
	public void setTestHost(String host){
		_testServerHost=host;
	}
	public void setTestPort(int port){
		_testServerPort=port;
	}

	public void initialize() 
	throws Exception{
		_startTime=System.currentTimeMillis();
		
		ph1=phNominal;
		ph2=phNominal;
		ph3=phNominal;
		ph4=phNominal;
		if(channels==null){
			channels=new Vector();
		}
		channels.clear();
		
		connect(_testServerHost,_testServerPort,_osdtHost,_osdtPort);
		
		phBoxcarIn=new FilterInput("ph_bci");
		phBoxcar=new BoxcarFilter("ph_bcf",Filter.DEFAULT_ID,phBoxcarDepth);
		try{
			phBoxcar.addInput(phBoxcarIn);
		}catch (Exception e) {
			_log4j.error("Model failed to add input in initialize");
		}
		phBoxcarIn.put(phNominal);

		_lastUpdateTime=System.currentTimeMillis();
	}
	
	public void connect(String testHost, int testPort, String osdtHost, int osdtPort)
	throws Exception{
		
		ControlInputIF _phCorr;
		ControlInputIF _phPumpCmd;
		ControlInputIF _velCorr;
		ControlInputIF _velXInt;
		ControlInputIF _tfwdCmd;
		ControlInputIF _taftCmd;
		
		ControlInputIF _pscale;	
		ControlInputIF _iscale;
		ControlInputIF _tshift;
		ControlInputIF _useVconst;
		ControlInputIF _vxconst;
		ControlInputIF _vyconst;
		ControlInputIF _phBoxcarDepth;
		ControlInputIF _vel_a;
		ControlInputIF _vel_b;
		ControlInputIF _vel_c;
		
		ControlInputIF _ph1;
		ControlInputIF _ph2;
		ControlInputIF _ph3;
		ControlInputIF _ph4;
		ControlInputIF _ph5;
		ControlInputIF _ph6;
		
		if(_testServerHost!=testHost){
			setTestHost(testHost);
		}
		if (_testServerPort!=testPort) {
			setTestPort(testPort);
		}
		if(_osdtHost!=osdtHost){
			setOSDTHost(osdtHost);
		}
		if (_osdtPort!=osdtPort) {
			setOSDTPort(osdtPort);
		}
		
		_log4j.debug("connecting to test server on "+_testServerHost+":"+_testServerPort);
		_testClient=new OSDTTestClient(_testServerHost,_testServerPort,_protocol);
		_testClient.connect();
		
		_log4j.debug("connecting to OSDT server");
		
		_phCorr=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_phSource,"ph_corr",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("phCorr"));	
		_phCorr.initialize();
		_phCorr.connect();
		
		if (_useTestPump.equalsIgnoreCase("FALSE")) {
			_phPumpCmd=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_phSource,"ph_pumpCmd",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("phPumpCmd"));	
		}else {
			_phPumpCmd=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"esw/ph_pumpCmd",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("phPumpCmd"));	
		}
		_phPumpCmd.initialize();
		_phPumpCmd.connect();

		
		_velCorr=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_vxSource,"vx_corr",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("velCorr"));	
		_velCorr.initialize();
		_velCorr.connect();
		
		_velXInt=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_vxSource,"vx_vxInt",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vx_vxInt"));	
		_velXInt.initialize();
		_velXInt.connect();
		
		if (_useTestThrusters.equalsIgnoreCase("FALSE")) {
			_tfwdCmd=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_vxSource,"vx_tfwdCmd",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vx_tfwdCmd"));	
			_taftCmd=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_vxSource,"vx_taftCmd",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vx_taftCmd"));	
		}else {
			_tfwdCmd=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"thrusters/vx_tfwdCmd",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vx_tfwdCmd"));	
			_taftCmd=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"thrusters/vx_taftCmd",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vx_taftCmd"));	
		}
		_tfwdCmd.initialize();
		_tfwdCmd.connect();
		_taftCmd.initialize();
		_taftCmd.connect();
	
		_ph1=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"pH1/pH",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("ph1"));	
		_ph1.initialize();
		_ph1.connect();
		_ph2=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"pH2/pH",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("ph2"));	
		_ph2.initialize();
		_ph2.connect();
		_ph3=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"pH3/pH",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("ph3"));	
		_ph3.initialize();
		_ph3.connect();
		_ph4=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"pH4/pH",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("ph4"));	
		_ph4.initialize();
		_ph4.connect();
		_ph5=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"pH5/pH",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("ph5"));	
		_ph5.initialize();
		_ph5.connect();
		_ph6=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"pH6/pH",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("ph6"));	
		_ph6.initialize();
		_ph6.connect();

		_pscale=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/pscale",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("pscale"));	
		_pscale.initialize();
		_pscale.connect();
		_testClient.sendCommand("setv,modelConfig/pscale,"+nf.format(pscale));
		
		_iscale=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/iscale",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("iscale"));	
		_iscale.initialize();
		_iscale.connect();
		_testClient.sendCommand("setv,modelConfig/iscale,"+nf.format(iscale));
		
		_tshift=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/tshift",ControlInputIF.FIELD_TYPE_LONG,new FilterInput("tshift"));	
		_tshift.initialize();
		_tshift.connect();
		_testClient.sendCommand("setv,modelConfig/tshift,"+inf.format(tshift));
		
		_useVconst=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/useVconst",ControlInputIF.FIELD_TYPE_INT,new FilterInput("useVconst"));	
		_useVconst.initialize();
		_useVconst.connect();
		_testClient.sendCommand("setv,modelConfig/useVconst,"+inf.format(useVconst));
		
		_vxconst=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/vxconst",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vxconst"));	
		_vxconst.initialize();
		_vxconst.connect();
		_testClient.sendCommand("setv,modelConfig/vxconst,"+nf.format(vxconst));
		
		_vyconst=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/vyconst",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vyconst"));	
		_vyconst.initialize();
		_vyconst.connect();
		_testClient.sendCommand("setv,modelConfig/vyconst,"+nf.format(vyconst));
		
		_vel_a=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/vel_a",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vel_a"));	
		_vel_a.initialize();
		_vel_a.connect();
		_testClient.sendCommand("setv,modelConfig/vel_a,"+nf.format(vel_a));
		
		_vel_b=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/vel_b",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vel_b"));	
		_vel_b.initialize();
		_vel_b.connect();
		_testClient.sendCommand("setv,modelConfig/vel_b,"+nf.format(vel_b));
		
		_vel_c=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/vel_c",ControlInputIF.FIELD_TYPE_DOUBLE,new FilterInput("vel_c"));	
		_vel_c.initialize();
		_vel_c.connect();
		_testClient.sendCommand("setv,modelConfig/vel_c,"+nf.format(vel_c));
		
		_phBoxcarDepth=new OSDTInputConnector( (_osdtHost+":"+_osdtPort),_testSource,"modelConfig/phBoxcarDepth",ControlInputIF.FIELD_TYPE_INT,new FilterInput("phBoxcarDepth"));	
		_phBoxcarDepth.initialize();
		_phBoxcarDepth.connect();
		_testClient.sendCommand("setv,modelConfig/phBoxcarDepth,"+inf.format(phBoxcarDepth));
		
		
		channels.add(new ChannelDefinition(_phCorr,((OSDTInputConnector)_phCorr).channel(),ID_PHCORR));
		channels.add(new ChannelDefinition(_phPumpCmd,((OSDTInputConnector)_phPumpCmd).channel(),ID_PHPUMPCMD));
		channels.add(new ChannelDefinition(_velCorr,((OSDTInputConnector)_velCorr).channel(),ID_VELCORR));
		channels.add(new ChannelDefinition(_velXInt,((OSDTInputConnector)_velXInt).channel(),ID_VELXINT));
		channels.add(new ChannelDefinition(_tfwdCmd,((OSDTInputConnector)_tfwdCmd).channel(),ID_TFWDCMD));
		channels.add(new ChannelDefinition(_taftCmd,((OSDTInputConnector)_taftCmd).channel(),ID_TAFTCMD));
		channels.add(new ChannelDefinition(_pscale,((OSDTInputConnector)_pscale).channel(),ID_PSCALE));
		channels.add(new ChannelDefinition(_iscale,((OSDTInputConnector)_iscale).channel(),ID_ISCALE));
		channels.add(new ChannelDefinition(_tshift,((OSDTInputConnector)_tshift).channel(),ID_TSHIFT));
		channels.add(new ChannelDefinition(_useVconst,((OSDTInputConnector)_useVconst).channel(),ID_USEVCONST));
		channels.add(new ChannelDefinition(_vxconst,((OSDTInputConnector)_vxconst).channel(),ID_VXCONST));
		channels.add(new ChannelDefinition(_vyconst,((OSDTInputConnector)_vyconst).channel(),ID_VYCONST));
		channels.add(new ChannelDefinition(_vel_a,((OSDTInputConnector)_vel_a).channel(),ID_VELA));
		channels.add(new ChannelDefinition(_vel_b,((OSDTInputConnector)_vel_b).channel(),ID_VELB));
		channels.add(new ChannelDefinition(_vel_c,((OSDTInputConnector)_vel_c).channel(),ID_VELC));
		channels.add(new ChannelDefinition(_phBoxcarDepth,((OSDTInputConnector)_phBoxcarDepth).channel(),ID_PHBOXCARDEPTH));
		channels.add(new ChannelDefinition(_ph1,((OSDTInputConnector)_ph1).channel(),ID_PH1));
		channels.add(new ChannelDefinition(_ph2,((OSDTInputConnector)_ph2).channel(),ID_PH2));
		channels.add(new ChannelDefinition(_ph3,((OSDTInputConnector)_ph3).channel(),ID_PH3));
		channels.add(new ChannelDefinition(_ph4,((OSDTInputConnector)_ph4).channel(),ID_PH4));
		channels.add(new ChannelDefinition(_ph5,((OSDTInputConnector)_ph5).channel(),ID_PH5));
		channels.add(new ChannelDefinition(_ph6,((OSDTInputConnector)_ph6).channel(),ID_PH6));
		
		if( paramNames==null){
			paramNames=new Vector();
		}
		paramNames.clear();
		if (channels!=null) {
			_log4j.debug("connect - found ["+channels.size()+"] channels");
			for(int i=0;i<channels.size();i++){
				ChannelDefinition channel=(ChannelDefinition)channels.get(i);
				String cname=channel.name();
				_log4j.debug("connect - adding: "+cname);
				//if (cname.indexOf("modelConfig")>=0) {
					paramNames.add(cname);
				//}
			}			
		}
		getChannel(ID_PSCALE).input.getFilterInput().put(pscale);
		getChannel(ID_ISCALE).input.getFilterInput().put(iscale);
		getChannel(ID_TSHIFT).input.getFilterInput().put(tshift);
		getChannel(ID_USEVCONST).input.getFilterInput().put(useVconst);
		getChannel(ID_VXCONST).input.getFilterInput().put(vxconst);
		getChannel(ID_VYCONST).input.getFilterInput().put(vyconst);
		getChannel(ID_VELA).input.getFilterInput().put(vel_a);
		getChannel(ID_VELB).input.getFilterInput().put(vel_b);
		getChannel(ID_VELC).input.getFilterInput().put(vel_c);
	}
		
	/** compute new pH from pump command */
	protected double pumpCmd2PH(double backgroundPH,double pumpSpeedRPM){
		////////////////////////////////////////
		// Geometric/Physical Parameters
		////////////////////////////////////////
		
		/** constant: seawater density (kg/m^3) */
		double SW_DENSITY_KG_PER_M3=1031.4;// orig 1022.8
		/** constant: CO2 concentration (mmol/liter) */
		double CO2_CONCENTRATION_MMOL_PER_L=500.0;
		/** constant: FOCE flume cross sectional area (m^2) */
		double FLUME_AREA_M2=0.25;//orig 0.5;
		/** constant: ESW pump displacment (ml/revolution) */
		double ESW_PUMP_DISPLACEMENT_ML_PER_REV=1.6;
		/** constant: pH sensitivity (pH units/mmol/kg) */
		double DELTA_PH_PER_MMOL_PER_KG=3.8257;//(2.1786*1.756);// orig 1.756;

		double offset=0.0;
		double newPH=0.0;
		//double pumpSpeedRPM=0.0;
		
		// get pumpSpeed RPM
		// (counts/sec)*(60 sec/min)/(6 counts/rev)/(67.5 rev/rev)
		//pumpSpeedRPM=pumpSpeedCounts*60.0/6.0/67.5;
		
		// get SWE volume rate (l/min)
		// (rev/min)*(ml/rev)*(l/ml)*(0.001 l/ml) => l/min
		double displacement_L_PER_REV=ESW_PUMP_DISPLACEMENT_ML_PER_REV*0.001;
		double sweRate=pumpSpeedRPM*displacement_L_PER_REV;
		
		// compute *OFFSET* relative to background
		// (cm/s) * (0.01 m/cm) => m/sec
		double seawaterVelocity_M_PER_SEC=xInt*0.01;		
		// (m/s)*(m^2)*(kg/m3)*(s/min) =>kg/min
		double seawaterRate=Math.abs(seawaterVelocity_M_PER_SEC)*FLUME_AREA_M2*SW_DENSITY_KG_PER_M3*60.0;
		// (l/min)*(phUnits/mMol/kg)*(mMol/l) / (kg/min) => phUnits
		double phOffset=sweRate*DELTA_PH_PER_MMOL_PER_KG*CO2_CONCENTRATION_MMOL_PER_L/seawaterRate;
		
		// compute pH value
		newPH=backgroundPH-Math.abs(phOffset);

		if(_log4j.isDebugEnabled()){
			_log4j.debug("\n pumpSpeed (counts):"+nf.format(pumpSpeedRPM*6.75)+
						 "\n pumpSpeed (rpm):"   +nf.format(pumpSpeedRPM)+
						 "\n flumeArea (m^2):"   +nf.format(FLUME_AREA_M2)+
						 "\n seVelocity (m/s):"  +nf.format(seawaterVelocity_M_PER_SEC)+
						 "\n seDensity (kg/m^3):"+nf.format(SW_DENSITY_KG_PER_M3)+
						 "\n seRate (kg/min):"   +nf.format(seawaterRate)+
						 "\n sweRate (l/min):"   +nf.format(sweRate)+
						 "\n phOffset (phUnits):"+nf.format(phOffset)+
						 "\n newPH (phUnits):"   +nf.format(newPH));
		}			
				
		return newPH;
	}
	
	/** Update model pH inputs and outputs */
	protected void updatePH(){

		
		long now=System.currentTimeMillis();
		long elapsedDriftTime=now-_lastUpdateTime;
		double newPH1=0;
		double newPH2=0;
		double newPH3=0;
		double newPH4=0;
		double newPH5=0;
		double newPH6=0;
		
		double newPH=0.0;
		
		try{
												
			// set new internal pH values using corrections
			double attenuation=1.0;
			
			// compute newPH from pump command
			newPH=pumpCmd2PH(ph6,phPumpCmd);
			
			_log4j.debug("\n phcorr:"+nf.format(phCorr)+" atn:"+attenuation+" phmin:"+phMin+" lastPHCORR:"+nf.format(_lastPHCORR)+
						 "\n phPumpCmd:"+nf.format(phPumpCmd)+" drift state:"+_phDriftState+" ["+(_phDriftState==0?"DRIVEN":"DRIFTING")+"]"+
						 "\n ph1:"+nf.format(ph1)+" ph2:"+nf.format(ph2)+
						 "\n ph3:"+nf.format(ph3)+" ph4:"+nf.format(ph4)+
						 "\n ph5:"+nf.format(ph5)+" ph6:"+nf.format(ph6)+
						 "\n newPH:"+nf.format(newPH)+" elapsed time:"+elapsedDriftTime+" msec");
			
			switch (_phDriftState) {
				case DRIFT_STATE_DRIVEN:
					// pumping ESW
					// put the new pH value into the filter
					phBoxcarIn.put(newPH);
					newPH1=newPH2=newPH3=newPH4=phBoxcar.doubleValue();
					break;
				case DRIFT_STATE_DRIFTING:
					// not pumping ESW (drifting towards external pH)
					// put the external pH into the filter
					phBoxcarIn.put(ph6);
					newPH1=newPH2=newPH3=newPH4=phBoxcar.doubleValue();
					
					break;
				default:
					break;
			}
			
			// limit pH values
			if (ph1>phMax) {
				ph1=phMax;
			}
			if (ph1<phMin) {
				ph1=phMin;
			}
			if (ph2>phMax) {
				ph2=phMax;
			}
			if (ph2<phMin) {
				ph2=phMin;
			}
			if (ph3>phMax) {
				ph3=phMax;
			}
			if (ph3<phMin) {
				ph3=phMin;
			}
			if (ph4>phMax) {
				ph4=phMax;
			}
			if (ph4<phMin) {
				ph4=phMin;
			}
			// update internal pH sensor readings
			_testClient.sendCommand("setv,pH1/pH,"+nf.format(newPH1));
			_testClient.sendCommand("setv,pH2/pH,"+nf.format(newPH2));
			_testClient.sendCommand("setv,pH3/pH,"+nf.format(newPH3));
			_testClient.sendCommand("setv,pH4/pH,"+nf.format(newPH4));
			
			// set External pH values (time varying)
			long time=(System.currentTimeMillis()-_startTime)/1000L;
			newPH5=(phNominal+0.025*Math.sin(time/86400.0));
			_testClient.sendCommand("setv,pH5/pH,"+nf.format(newPH5));
			
			newPH6=(phNominal+0.025*Math.sin(time/86400.0));			
			_testClient.sendCommand("setv,pH6/pH,"+nf.format(newPH6));
		}catch (Exception e) {
			e.printStackTrace();
		}	
		_log4j.debug("newPH1:"+newPH1);
		_log4j.debug("newPH2:"+newPH2);
		_log4j.debug("newPH3:"+newPH3);
		_log4j.debug("newPH4:"+newPH4);
		_log4j.debug("newPH5:"+newPH5);
		_log4j.debug("newPH6:"+newPH6);
		
		_lastUpdateTime=now;
		_lastPHCORR=phCorr;
	}
	
	
	/** compute new water velocity (vx) from thruster command */
	protected double rpm2VEL(double thrusterRPM){
		
		double vx_cm_per_sec=0.0;
		
		// force all math into first quadrant
		double arpm=Math.abs(thrusterRPM);
		double a=Math.abs(vel_a);
		double b=Math.abs(vel_b);
		double c=Math.abs(vel_c);
		
		if(vel_a==0.0){
			// quadratic term is zero
			// solve as linear
			vx_cm_per_sec=(arpm-c)/b;
		}else {
			vx_cm_per_sec= (-b+Math.sqrt(b*b-4.0*a*(c-arpm)))/(2.0*a);
		}

		if(thrusterRPM<0.0){
			vx_cm_per_sec *= (-1.0);
		}
		return vx_cm_per_sec;
	}
	
	/** Update model water velocity (current) inputs and outputs */
	protected void updateCurrent(){
		try{

			//readVELConnectors();
			
			long time=((System.currentTimeMillis()-_startTime)/1000L)+tshift;
			double PI=Math.toRadians(180.0);
			// max X velocity (external)
			double a=12.5;
			// max Y velocity (external)
			double b=1.25;
			// primary period (24 hour)
			double T1=pscale*86400;
			// secondary period (12 hour)
			double T2=pscale*T1/2.0;
			
			// parametric equations theta(t)=wt=2*PI*t/T
			double alpha=2*PI*time/T1;
			double beta=2*PI*time/T2;
			// driven by two combined ellipses w/ different periods
			double r1=Math.sqrt( (a*a*b*b) / ((a*a*Math.sin(alpha)*Math.sin(alpha))+(b*b*Math.cos(alpha)*Math.cos(alpha))));
			double r2=Math.sqrt( (a*a*b*b) / ((a*a*Math.sin(beta)*Math.sin(beta))+(b*b*Math.cos(beta)*Math.cos(beta))));
			double xExt=r1*Math.cos(alpha)+r2*Math.cos(beta);
			double yExt=r1*Math.sin(alpha)+r2*Math.sin(beta);
			
			// internal currents 1/5 internal currents (plus velocity correction from thrusters)
			xInt=iscale*xExt+rpm2VEL(tfwdCmd+taftCmd);//velCorr;
			yInt=iscale*yExt;
			
			// override to use constant velocity if configured to do so
			if(useVconst!=0){
				xInt=iscale*vxconst+velCorr;
				yInt=iscale*vyconst;
			}
			
			_log4j.debug("\n velCorr:"+nf.format(velCorr)+
						 "\n xExt:"+nf.format(xExt)+" velXint:"+nf.format(velXInt)+" xInt:"+nf.format(xInt)+
						 "\n alpha(deg):"+nf.format(Math.toDegrees(alpha))+
						 "\n pscale:"+nf.format(pscale)+
						 "\n tshift:"+nf.format(tshift)+
						 "\n iscale:"+nf.format(iscale)+
						 "\n useTestThrusters:"+_useTestThrusters+
						 "\n tfwdCmd:"+nf.format(tfwdCmd)+
						 "\n taftCmd:"+nf.format(taftCmd)+
						 "\n useVconst:"+useVconst+
						 "\n vxconst:"+nf.format(vxconst)+
						 "\n vyconst:"+nf.format(vyconst));
			
			_testClient.sendCommand("setv,Velocity/avgVelocityX,"+nf.format(xInt));
			_testClient.sendCommand("setv,Velocity/avgVelocityY,"+nf.format(yInt));
			_testClient.sendCommand("setv,ADCP/VelocityX,"+nf.format(xExt));
			_testClient.sendCommand("setv,ADCP/VelocityY,"+nf.format(yExt));
			
		}catch (Exception e) {
			e.printStackTrace();
		}				
	}
	
	public void updateConfig(){
		/*
		try{
		_testClient.sendCommand("setv,modelConfig/pscale,"+nf.format(pscale));
		_testClient.sendCommand("setv,modelConfig/iscale,"+nf.format(iscale));
		_testClient.sendCommand("setv,modelConfig/tshift,"+inf.format(tshift));
		_testClient.sendCommand("setv,modelConfig/useVconst,"+inf.format(useVconst));
		_testClient.sendCommand("setv,modelConfig/vxconst,"+nf.format(vxconst));
		_testClient.sendCommand("setv,modelConfig/vyconst,"+nf.format(vyconst));
		_testClient.sendCommand("setv,modelConfig/vel_a,"+nf.format(vel_a));
		_testClient.sendCommand("setv,modelConfig/vel_b,"+nf.format(vel_b));
		_testClient.sendCommand("setv,modelConfig/vel_c,"+nf.format(vel_c));
		_testClient.sendCommand("setv,modelConfig/phBoxcarDepth,"+inf.format(phBoxcarDepth));
		}catch (Exception e) {
			e.printStackTrace();
		}
		 */
	}
	
	public void readConnectors(){

		
		double dtest=0.0;
		int itest=0;
		long ltest=0L;
		
		for(int i=0;i<channels.size();i++){
			ChannelDefinition channel=(ChannelDefinition)channels.get(i);
			try{
				channel.update();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		try{
			ChannelDefinition channel=getChannel(ID_PHBOXCARDEPTH);
			if(channel.previousNumberValue().intValue()>0 &&
			   channel.numberValue().intValue() > 0 &&  
			   (channel.numberValue()!=channel.previousNumberValue())){
				phBoxcarDepth=channel.numberValue().intValue();
				phBoxcar.setDepth(phBoxcarDepth);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		try{
			ControlInputIF pumpCmdInput=getChannel(ID_PHPUMPCMD).input;
			if(pumpCmdInput.getFilterInput().isUpdate()){
				double test=pumpCmdInput.getInputValue().doubleValue();
				
				//if(_phPumpCmd.getFilterInput().isUpdate()){
				//double test=_phPumpCmd.getInputValue().doubleValue();
				
				if(phPumpCmd!=0.0){
					// if the pump was running...
					if(test == 0.0){
						// and now it's off...
						_phDriftState=DRIFT_STATE_DRIFTING;
						_phDriftStartTime=System.currentTimeMillis();
					}else{
						// and is still running
						_phDriftState=DRIFT_STATE_DRIVEN;
					}
				}else{
					// if the pump was off...
					if(test == 0.0){
						// and now it's off...
						// keep drifting
						_phDriftState=DRIFT_STATE_DRIFTING;
					}else{
						// if it's on now
						// resume driven mode
						_phDriftState=DRIFT_STATE_DRIVEN;
					}
				}
				phPumpCmd=test;
			}else{
				_log4j.debug("phPumpCmd - no update");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		phCorr=getChannelValue(ID_PHCORR).doubleValue();
		
		// phPumpCmd set above
		//phPumpCmd=getChannelValue(ID_PHPUMPCMD).doubleValue();
		ph1=getChannelValue(ID_PH1).doubleValue();
		ph2=getChannelValue(ID_PH2).doubleValue();
		ph3=getChannelValue(ID_PH3).doubleValue();
		ph4=getChannelValue(ID_PH4).doubleValue();
		ph5=getChannelValue(ID_PH5).doubleValue();
		ph6=getChannelValue(ID_PH6).doubleValue();
		
		tshift=getChannelValue(ID_TSHIFT).longValue();
		dtest=getChannelValue(ID_PSCALE).doubleValue();
		if(dtest>0){
			pscale=dtest;
		}
		dtest=getChannelValue(ID_ISCALE).doubleValue();
		if(dtest>0){
			iscale=dtest;
		}
		
		tfwdCmd=getChannelValue(ID_TFWDCMD).doubleValue();
		taftCmd=getChannelValue(ID_TAFTCMD).doubleValue();
		vxconst=getChannelValue(ID_VXCONST).doubleValue();
		vyconst=getChannelValue(ID_VYCONST).doubleValue();
		velCorr=getChannelValue(ID_VELCORR).doubleValue();
		useVconst=getChannelValue(ID_USEVCONST).intValue();
		
	}
	
	public void doWorkerAction(){
		synchronized (this) {
			readConnectors();
			updateConfig();
			updatePH();
			updateCurrent();
		}
	}
	
	public void shutdown(){	
		for(int i=0;i<channels.size();i++){
			ChannelDefinition channel=(ChannelDefinition)channels.get(i);
			try{
				((OSDTInputConnector)channel.input).disconnect();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
		
	public void printHelp(){
		StringBuffer sb=new StringBuffer();
		sb.append("\n");
		sb.append("OSDT Test Model - Creates FOCE model for Control Loop testing\n");
		sb.append("\n");
		sb.append("runModel [-oh <host> -op <port> -th <host> -tp <port> -p <delayMillisec> -s <currentPeriodScale> -f <file> -help]\n");
		sb.append("\n");
		sb.append("-f <file>    : configuration file path\n");
		sb.append("\n");
		sb.append("A configuration file (specified with the -f option) may be used to set all options\n");
		sb.append("e.g. runModel -f /path/to/configFile:\n");
		sb.append("\n");
		sb.append("       osdtHost=localhost\n");
		sb.append("       osdtPort=3333\n");
		sb.append("       testHost=localhost\n");
		sb.append("       testPort=4445\n");
		sb.append("       delayMillis=5000L\n");
		sb.append("       useTestPump=TRUE\n");
		sb.append("       useTestThrusters=TRUE\n");
		sb.append("       phBoxcarDepth=10\n");
		sb.append("       useVconst=1\n");
		sb.append("       vxconst=20.0\n");
		sb.append("       vyconst=0.0\n");
		sb.append("       vel_a=0.0\n");
		sb.append("       vel_b=50.0\n");
		sb.append("       vel_c=0.0\n");
		sb.append("       pscale=1.0\n");
		sb.append("       iscale=0.2\n");
		sb.append("       tshift=0\n");
		sb.append("       \n");
		sb.append("       \n");
		sb.append("       \n");
		sb.append("\n");
		sb.append("       phSource=pH_Control_Service-1787\n");
		sb.append("       vxSource=pH_Control_Service-1787\n");
		sb.append("       testSource=OSDTTestServer\n");
		sb.append("\n");
		System.out.println(sb.toString());
	}
	
	public ChannelDefinition getChannel(int channelID){
		for(int i=0;i<channels.size();i++){
			ChannelDefinition channel=(ChannelDefinition)channels.get(i);
			if (channel.id==channelID) {
				return channel;
			}
		}
		return null;
	}
	
	public Vector getParameterNames(){
		return paramNames;
	}

	public Number getChannelValue(int channelID){
		for(int i=0;i<channels.size();i++){
			ChannelDefinition channel=(ChannelDefinition)channels.get(i);
			if (channel.id==channelID) {
				_log4j.debug("getChannelValue(id) - returning "+channel.name()+": ["+channel.stringValue()+"/"+((OSDTInputConnector)channel.input).typeName()+"]");
				return channel.numberValue();
			}
		}
		return null;
	}
	
	
	public String getChannelValue(String channelName){
		for(int i=0;i<channels.size();i++){
			ChannelDefinition channel=(ChannelDefinition)channels.get(i);
			String cname=channel.name();
			//_log4j.debug("getChannelValue - looking for "+channelName+" found "+cname);
			if (cname.equalsIgnoreCase(channelName)) {
				_log4j.debug("getChannelValue(name) - returning "+channel.name()+": ["+channel.stringValue()+"/"+((OSDTInputConnector)channel.input).typeName()+"]");
				String value=cname+":"+channel.stringValue();
				return value;
			}
		}
		return null;
	}
	
	public void configure(String[] args) throws Exception{
		if(args.length<=0){
			return;
		}
		
		// to hold next arg for processing
		String arg=null;
		
		// process properties file, if any
		for(int i=0;i<args.length;i++){
			arg=args[i];
			if(arg.equals("-help") || arg.equals("--help")){
				printHelp();
				System.exit(1);
			}
			if(arg.equals("-f")){
				// read configuration from a properties file
				FileInputStream fis=new FileInputStream(args[i+1]);
				Properties properties=new Properties();
				properties.load(fis);
				fis.close();
				
				for(Enumeration e=properties.propertyNames();e.hasMoreElements();){
					String propertyName=(String)e.nextElement();
					propertyName.trim();
					String propertyString=properties.getProperty(propertyName);
					if(propertyName.equalsIgnoreCase("osdtHost")){
						_osdtHost=propertyString;
					}
					if(propertyName.equalsIgnoreCase("osdtPort")){
						_osdtPort=Integer.parseInt(propertyString);
					}
					if(propertyName.equalsIgnoreCase("testHost")){
						_testServerHost=propertyString;
					}
					if(propertyName.equalsIgnoreCase("testPort")){
						_testServerPort=Integer.parseInt(propertyString);
					}
					if(propertyName.equalsIgnoreCase("delayMillis")){
						setUpdatePeriod(Long.parseLong(propertyString));
					}
					if(propertyName.equalsIgnoreCase("phSource")){
						_phSource=propertyString;
					}
					if(propertyName.equalsIgnoreCase("vxSource")){
						_vxSource=propertyString;
					}
					if(propertyName.equalsIgnoreCase("testSource")){
						_testSource=propertyString;
					}
					if(propertyName.equalsIgnoreCase("useTestPump")){
						_useTestPump=propertyString;
					}
					if(propertyName.equalsIgnoreCase("useVconst")){
						useVconst=Integer.parseInt(propertyString);
					}
					if(propertyName.equalsIgnoreCase("useTestThrusters")){
						_useTestThrusters=propertyString;
					}
					if(propertyName.equalsIgnoreCase("phBoxcarDepth")){
						phBoxcarDepth=Integer.parseInt(propertyString);
					}
					if(propertyName.equalsIgnoreCase("vxconst")){
						vxconst=Double.parseDouble(propertyString);
					}
					if(propertyName.equalsIgnoreCase("vyconst")){
						vyconst=Double.parseDouble(propertyString);
					}
					if(propertyName.equalsIgnoreCase("vel_a")){
						vel_a=Double.parseDouble(propertyString);
					}
					if(propertyName.equalsIgnoreCase("vel_b")){
						vel_b=Double.parseDouble(propertyString);
					}
					if(propertyName.equalsIgnoreCase("vel_c")){
						vel_c=Double.parseDouble(propertyString);
					}
					if(propertyName.equalsIgnoreCase("iscale")){
						iscale=Double.parseDouble(propertyString);
					}
					if(propertyName.equalsIgnoreCase("pscale")){
						pscale=Double.parseDouble(propertyString);
					}
					if(propertyName.equalsIgnoreCase("tshift")){
						tshift=Long.parseLong(propertyString);
					}
				}
				i++;
			}
		}
		
		// process command line args, if any
		// (should override properties file settings)
		for(int i=0;i<args.length;i++){
			arg=args[i];
			
			if(arg.equals("-help") || arg.equals("--help")){
				printHelp();
				System.exit(1);
			}
			if(args[i].equals("-s")){
				_currentPeriodScale=Double.parseDouble(args[i+1]);
				i++;
			}
			if(args[i].equals("-oh")){
				_osdtHost=args[i+1];
				i++;
			}
			if(args[i].equals("-op")){
				_osdtPort=Integer.parseInt(args[i+1]);
				i++;
			}
			if(args[i].equals("-th")){
				_testServerHost=args[i+1];
				i++;
			}
			if(args[i].equals("-tp")){
				_testServerPort=Integer.parseInt(args[i+1]);
				i++;
			}
			if(args[i].equals("-p")){
				setUpdatePeriod(Long.parseLong(args[i+1]));
				i++;
			}
		}
	}
	
	public class ChannelDefinition{
		public String name;
		public ControlInputIF input;
		public int id;
		
		String _stringValue;
		Number _previousNumberValue;
		Number _numberValue;
		NumberFormat nf=NumberFormat.getInstance();

		public ChannelDefinition(){
			nf=NumberFormat.getInstance();
			nf.setMaximumFractionDigits(5);
			nf.setMinimumFractionDigits(3);
			nf.setMinimumIntegerDigits(1);
			nf.setGroupingUsed(false);
		}
		
		public ChannelDefinition(ControlInputIF input,String name,int id){
			this();
			this.name=name;
			this.id=id;
			this.input=input;
			//this.connector=(OSDTInputConnector)input;
			this._numberValue=((OSDTInputConnector)input).getInputValue();
			this._previousNumberValue=this._numberValue;
		}
		
		public String name(){
			return this.name;
		}
		
		public String stringValue(){
			switch (((OSDTInputConnector)this.input).dataType()) {
				case ControlInputIF.FIELD_TYPE_DOUBLE:
				case ControlInputIF.FIELD_TYPE_FLOAT:
					return stringValue(1,3,5);
				case ControlInputIF.FIELD_TYPE_LONG:
				case ControlInputIF.FIELD_TYPE_INT:
				case ControlInputIF.FIELD_TYPE_SHORT:
				case ControlInputIF.FIELD_TYPE_BYTE:
					return stringValue(1,0,0);
				case ControlInputIF.FIELD_TYPE_BOOLEAN:
					return "Boolean type not supported";
				case ChannelMap.TYPE_STRING:
					return "String type not supported";
				default:
					break;
			}
			return "Invalid connector data type";
		}
		protected String stringValue(int intDigits, int minFracDigits, int maxFracDigits){
				nf.setMaximumFractionDigits(maxFracDigits);
				nf.setMinimumFractionDigits(minFracDigits);
				nf.setMinimumIntegerDigits(intDigits);
				return nf.format(doubleValue());
		}
		public Number numberValue(){return _numberValue;}
		public Number previousNumberValue(){return _previousNumberValue;}
		public double doubleValue(){return _numberValue.doubleValue();}
		public float floatValue(){return _numberValue.floatValue();}
		public long longValue(){return _numberValue.longValue();}
		public int intValue(){return _numberValue.intValue();}
		public short shortValue(){return _numberValue.shortValue();}
		public byte byteValue(){return _numberValue.byteValue();}
		
		public void update() throws Exception{
			
			OSDTInputConnector oic=(OSDTInputConnector)this.input;
			_log4j.debug("Model.update - checking channel ["+this.name+" ("+oic.typeName()+")]");
			
			if(oic.getFilterInput().isUpdate()){
				
				_log4j.debug("Model.update - updating channel ["+this.name+"]");

				// use test in case exception is thrown
				Number test=oic.getInputValue();
				this._previousNumberValue=this._numberValue;
				this._numberValue=test;
				_log4j.debug("Model.update - "+this.name+" new:"+this._numberValue.doubleValue()+" prev:"+this._previousNumberValue.doubleValue());
			}else{
				_log4j.debug("Model.update - "+this.name+" no update: ["+this._numberValue.doubleValue()+"/"+this._previousNumberValue.doubleValue()+"]");
			}		
		}
		
		public String toString(){
			return (name+","+((OSDTInputConnector)this.input).typeName()+","+stringValue());
		}
		
		
	}
	
	public static void main(String[] args){
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
		
		try {
			
			Model model=new Model("localhost",3333,"localhost",4445,5000L);
			model.configure(args);
			model.start();
			model.join();
			/*
			System.out.println("**************** waiting for the end of the world...(should be here in about 10s)");
			Thread.sleep(10000L);
			System.out.println("**************** time's up, pencils down folks");
			model.terminate();
			 */
			System.exit(0);
			 
		} catch (Exception se) { 
			se.printStackTrace(); 
		}
		System.exit(1);		
    }
	
}
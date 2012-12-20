/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;
import java.text.NumberFormat;
import java.lang.reflect.*;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.util.Properties;

import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.operations.utils.NodeUtility;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;
import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.BoxcarFilter;
import org.mbari.siam.utils.WeightedAverageFilter;
import org.mbari.siam.utils.HeadingFilter;
import org.mbari.siam.utils.MagnitudeFilter;
import org.mbari.siam.utils.RangeFilter;
import org.mbari.siam.utils.UnityFilter;
import org.mbari.siam.utils.RangeValidator;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;

import org.mbari.siam.core.DeviceService;
import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ValveIF;
import org.mbari.siam.distributed.devices.Valve2WayIF;
import org.mbari.siam.distributed.devices.ValveServiceIF;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.distributed.devices.ControlOutputIF;
import org.mbari.siam.distributed.devices.ProcessStateIF;
import org.mbari.siam.distributed.devices.ProcessConfigIF;
import org.mbari.siam.distributed.devices.ControlProcessIF;
import org.mbari.siam.distributed.devices.ProcessParameterIF;

/** FOCEProcess encapsulates the FOCE control process (or "plant", in the control systems domain). 
	It is responsible for managing connections to inputs (pH and velocity sensors) and outputs (motors and valves).
	It is one of the major components of the FOCE control system software; A ControlWorkerThread periodically causes
	a ControlResponseIF (e.g. PHPIDResponse, PHEXPResponse) to update using a FOCEPRocess instance to provide current
	values for system signals and parameters. The ControlResponseIF then updates system outputs via the FOCEProcess.
 
	FOCEProcess is configured via ControlLoopAttributes. 

	@see org.mbari.siam.distributed.devices.ProcessParameterIF
 */
public class FOCEProcess implements ControlProcessIF, ProcessParameterIF{
	
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(FOCEProcess.class);
	
	/** SIAM node reference */
	Node _node=null;
	
	/** Instrument Registry intance */
	InstrumentRegistry _registry=null;

	/** holds all signal filters, indexed by signal IDs (SIG_)*/
	Vector _signalFilters;
	
	/** holds all filter inputs, indexed by input IDs (INPUT_)*/
	Vector _filterInputs;
	
	/** Input connectors */
	Vector _connectors;
		
	/** Output controls */
	Vector _outputs;

	/** pH Range Validator.*/
	RangeValidator _phRangeValidator=null;
	
	/** NumberFormat, for debug output */
	NumberFormat _decFormat;
	/** Service attributes (configuration values)
	 These may change on SetProperties.
	 */
	ControlLoopAttributes _attributes;
	
	/** control process state */
	int _controlProcessState=CPIF_STATE_UNKNOWN;
	
	/** Constructor for FOCEProcess (ControlProcessIF) 
		@param attributes  ControlLoopAttributes shared by ControlLoopService, PID loops etc.
	 */
	public FOCEProcess(ControlLoopAttributes attributes){
		
		// set configuration attributes passed in from ControlLoop service
		_attributes=attributes;

		// initialize signal filter vector
		_signalFilters=new Vector();
		
		// input connector vector (victor :o)
		_connectors=new Vector();
	
		// filter input vector
		_filterInputs=new Vector();
		
		
		// output
		_outputs=new Vector();
		_decFormat=NumberFormat.getInstance();
		_decFormat.setMaximumFractionDigits(5);
		_decFormat.setMinimumFractionDigits(3);
		_decFormat.setMinimumIntegerDigits(1);
		_decFormat.setGroupingUsed(false);		
		
	}
	
	public ControlOutputIF createOutput(ControlLoopAttributes.ConnectorSpec connectorSpec)
	throws Exception, RemoteException{
		
		ControlOutputIF newOutput=null;
		if(_log4j.isDebugEnabled()){
		_log4j.debug("creating output type ["+connectorSpec.connector_type+"]");
		}
		switch (connectorSpec.connector_type) {
			case CONNECTOR_TYPE_SIAM_REG:
			case CONNECTOR_TYPE_SIAM_OSDT:
				// lookup SIAM service using registry name
				Instrument service=null;
				try{
					service=lookupSIAMService(_attributes.siamHost,connectorSpec.registry_key);
				}catch (Exception e) {
					e.printStackTrace();
				}
				if(service == null){
					_log4j.error("createOutput - service ["+connectorSpec.registry_key+"] is null");
					//throw new Exception("createOutput - service is null");
				}
				// cast to proper type (ElmoIF, ValveIF, etc)
				ElmoIF motor=null;
				ValveIF valve=null;
				switch(connectorSpec.type()){
					case TYPE_MOTOR:
						if(service==null){
							newOutput=new MotorVelocityOutput(null);
						}else if(service instanceof ElmoIF){
							newOutput=new MotorVelocityOutput((ElmoIF)service);
						}else{
							throw new Exception("Service specified is does not implement ElmoIF ["+connectorSpec.registry_key+"]");
						}
						break;
					case TYPE_VALVE:
						if(service==null){
							newOutput=new ValveOutput((Valve2WayIF)valve);
						}else if(service instanceof ValveIF){
							valve=(ValveIF)service;
						}else if(service instanceof ValveServiceIF){
							int role=connectorSpec.role();
							switch(role){
								case ROLE_FWD_ESW_VALVE:
									valve=((ValveServiceIF)service).getValve(ValveServiceIF.VALVE_FWD);
									break;
								case ROLE_AFT_ESW_VALVE:
									valve=((ValveServiceIF)service).getValve(ValveServiceIF.VALVE_AFT);
									break;
								default:
									throw new Exception("invalid valve role ["+role+"]");
							}
						}else{
							throw new Exception("Service specified is does not implement ValveIF or ValveServiceIF ["+connectorSpec.registry_key+"]");
						}
						if( valve==null ){
							throw new Exception("valve is null");
						}
						if( !(valve instanceof Valve2WayIF) ){
							throw new Exception("valve is not a 2 way valve");
						}
						newOutput=new ValveOutput((Valve2WayIF)valve);
						break;
					default:
						throw new Exception("Invalid connector type ["+connectorSpec.type()+"]");
				}
				// assign to newOutput
				break;
			case CONNECTOR_TYPE_EXT_OSDT:
				// get parameters (serial port, etc.)
				// create control object (ValveIF, ElmoIF, etc)
				// create ControlOutputIF 
				// assign to new output
				break;
			case CONNECTOR_TYPE_RAW:
			default:
				_log4j.warn("unsupported output type ["+connectorSpec.connector_type+"]");
				break;
		}
		return newOutput;
		
	}
	
	public ControlInputIF createConnector(ControlLoopAttributes.ConnectorSpec connectorSpec, FilterInput input)
	throws InvalidPropertyException{
		ControlInputIF newConnector=null;
		if(_log4j.isDebugEnabled()){
		_log4j.debug("creating connector type ["+connectorSpec.connector_type+"]");
		}
		switch (connectorSpec.connector_type) {
			case CONNECTOR_TYPE_SIAM_REG:
				newConnector=new RegistryInputConnector(connectorSpec.registry_key,
														connectorSpec.signal_key,
														connectorSpec.field_type,
														input,
														connectorSpec.update_timeout_ms);
				try{
					newConnector.connect();
					newConnector.setService(((RegistryInputConnector)newConnector).findService());
				}catch (Exception e) {
					_log4j.error("createConnector error [ siamHost "+_attributes.siamHost+" registry_key "+connectorSpec.registry_key+"]");
					e.printStackTrace();
				}
				break;
			case CONNECTOR_TYPE_SIAM_OSDT:
				try{
					// use registryKey to get OSDT name from SIAM node
					String osdtSourceName=registry2osdt(_attributes.siamHost, connectorSpec.registry_key);
					newConnector= new OSDTInputConnector(_attributes.osdtHost,
														 osdtSourceName,
														 connectorSpec.signal_key,
														 connectorSpec.field_type,
														 input,
														 connectorSpec.update_timeout_ms,
														 connectorSpec.update_period_ms);
					Node node=getNodeReference(_attributes.siamHost);
					Instrument service=node.lookupService(connectorSpec.registry_key);
					newConnector.setService(service);
				}catch (Exception e) {
					_log4j.error("createConnector error [ siamHost "+_attributes.siamHost+" registry_key "+connectorSpec.registry_key+"]");
					e.printStackTrace();
				}
				break;
			case CONNECTOR_TYPE_EXT_OSDT:
				// use registryKey to get OSDT name from ConnectorSpec in _attributes
				// if OSDT source name is not explicitly defined in ConnectorSpec, the registryKey is
				// used as the OSDT source name
				String osdtSourceName=_attributes.registry2osdt(connectorSpec.registry_key);
				newConnector= new OSDTInputConnector(_attributes.osdtHost,
													 osdtSourceName,
													 connectorSpec.signal_key,
													 connectorSpec.field_type,
													 input,
													 connectorSpec.update_timeout_ms,
													 connectorSpec.update_period_ms);
				break;
			case CONNECTOR_TYPE_RAW:
				newConnector=new RawInputConnector(connectorSpec.registry_key,connectorSpec.signal_key,connectorSpec.field_type,input);
				break;
			default:
				_log4j.error("Invalid connector type ["+connectorSpec.connector_type+"]");
				break;
		}
		return newConnector;
	}
	
	/** Create signal processing filters for the 
		control inputs and intermediate processing
		(like pH averaging).
	 */
	public void createFilters()
	throws Exception{

		_signalFilters.add(new WeightedAverageFilter(filterName(FC_PH_INT_FWD),FC_PH_INT_FWD));
		_signalFilters.add(new WeightedAverageFilter(filterName(AC_PH_INT_AFT),AC_PH_INT_AFT));
		_signalFilters.add(new WeightedAverageFilter(filterName(EC_PH_EXT),EC_PH_EXT));
		_signalFilters.add(new WeightedAverageFilter(filterName(IC_PH_INT),IC_PH_INT));
		
		_signalFilters.add(new WeightedAverageFilter(filterName(VC_VH2O_INT_X),VC_VH2O_INT_X));
		_signalFilters.add(new WeightedAverageFilter(filterName(VC_VH2O_INT_Y),VC_VH2O_INT_Y));
		_signalFilters.add(new WeightedAverageFilter(filterName(VC_VH2O_EXT_X),VC_VH2O_EXT_X));
		_signalFilters.add(new WeightedAverageFilter(filterName(VC_VH2O_EXT_Y),VC_VH2O_EXT_Y));
		_signalFilters.add(new MagnitudeFilter(filterName(MA_MAG_INT),MA_MAG_INT));
		_signalFilters.add(new HeadingFilter(filterName(HD_HDG_INT),HD_HDG_INT));
		_signalFilters.add(new MagnitudeFilter(filterName(MA_MAG_EXT),MA_MAG_EXT));
		_signalFilters.add(new HeadingFilter(filterName(HD_HDG_EXT),HD_HDG_EXT));
		
		_signalFilters.add(new BoxcarFilter(filterName(BX_PH_INT_FWD_L),BX_PH_INT_FWD_L));
		_signalFilters.add(new BoxcarFilter(filterName(BX_PH_INT_FWD_R),BX_PH_INT_FWD_R));
		_signalFilters.add(new BoxcarFilter(filterName(BX_PH_INT_AFT_L),BX_PH_INT_AFT_L));
		_signalFilters.add(new BoxcarFilter(filterName(BX_PH_INT_AFT_R),BX_PH_INT_AFT_R));
		_signalFilters.add(new BoxcarFilter(filterName(BX_PH_EXT_MID_L),BX_PH_EXT_MID_L));
		_signalFilters.add(new BoxcarFilter(filterName(BX_PH_EXT_MID_R),BX_PH_EXT_MID_R));
		_signalFilters.add(new BoxcarFilter(filterName(BX_PH_ESW),BX_PH_ESW));
		
		_signalFilters.add(new BoxcarFilter(filterName(BX_VH2O_INT_X),BX_VH2O_INT_X));
		_signalFilters.add(new BoxcarFilter(filterName(BX_VH2O_INT_Y),BX_VH2O_INT_Y));
		_signalFilters.add(new BoxcarFilter(filterName(BX_VH2O_EXT_X),BX_VH2O_EXT_X));
		_signalFilters.add(new BoxcarFilter(filterName(BX_VH2O_EXT_Y),BX_VH2O_EXT_Y));
		
		_signalFilters.add(new BoxcarFilter(filterName(BX_VTHR_FWD),BX_VTHR_FWD));
		_signalFilters.add(new BoxcarFilter(filterName(BX_VTHR_AFT),BX_VTHR_AFT));
		
		_signalFilters.add(new UnityFilter(filterName(VS_ESWV_FWD),VS_ESWV_FWD));
		_signalFilters.add(new UnityFilter(filterName(VS_ESWV_AFT),VS_ESWV_AFT));
	}
					
	/** Perform per-filter configuration for all filters.
		@see #configureFilter(Filter filter)
	 */
	public void configureAllFilters(){
		Filter filter;
		for(Iterator i=_signalFilters.iterator();i.hasNext();){
			filter=(Filter)i.next();
			try{
				configureFilter(filter);
			}catch(Exception e){
				e.printStackTrace();
				_log4j.error(e.getMessage());
			}
		}
	}			
		
	
	/** Perform per-filter configuration for specified Filter.
	 @see #configureAllFilters()
	 */
	public void configureFilter(Filter filter)
	throws Exception{
		int id=filter.getID();

		switch(id){
				// internal pH boxcar
			case BX_PH_INT_FWD_L:
			case BX_PH_INT_FWD_R:
			case BX_PH_INT_AFT_L:
			case BX_PH_INT_AFT_R:
				// external pH boxcar
			case BX_PH_EXT_MID_L:
			case BX_PH_EXT_MID_R:
				// ESW pH boxcar
			case BX_PH_ESW:
				// internal/external H20 velocity boxcars
			case BX_VH2O_INT_X:
			case BX_VH2O_INT_Y:
			case BX_VH2O_EXT_X:
			case BX_VH2O_EXT_Y:
				// forward/aft thruster velocity boxcars
			case BX_VTHR_FWD:				
			case BX_VTHR_AFT:
				((BoxcarFilter)filter).setDepth(_attributes.filterDepth(id));
				break;
				
			case FC_PH_INT_FWD:
			case AC_PH_INT_AFT:
			case EC_PH_EXT:
			case IC_PH_INT:
			case VC_VH2O_INT_X:
			case VC_VH2O_INT_Y:
			case VC_VH2O_EXT_X:
			case VC_VH2O_EXT_Y:
			case MA_MAG_INT:
			case HD_HDG_INT:
			case MA_MAG_EXT:
			case HD_HDG_EXT:
			case VS_ESWV_FWD:
			case VS_ESWV_AFT:
			case BX_VESWP:
				// nothing to do for these (yet)
				break;
			default:
				throw new Exception("Unsupported filter ID ["+id+"]");
		}
	}
	
	public void createFilterInputs()
	throws Exception{

		_filterInputs.add(new FilterInput(filterInputName(IBX_PH_INT_FWD_L),IBX_PH_INT_FWD_L,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_PH_INT_FWD_R),IBX_PH_INT_FWD_R,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_PH_INT_AFT_L),IBX_PH_INT_AFT_L,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_PH_INT_AFT_R),IBX_PH_INT_AFT_R,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IBX_PH_EXT_MID_L),IBX_PH_EXT_MID_L,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_PH_EXT_MID_R),IBX_PH_EXT_MID_R,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IFC_PH_INT_FWD_L),IFC_PH_INT_FWD_L,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IFC_PH_INT_FWD_R),IFC_PH_INT_FWD_R,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IAC_PH_INT_AFT_L),IAC_PH_INT_AFT_L,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IAC_PH_INT_AFT_R),IAC_PH_INT_AFT_R,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IEC_PH_EXT_MID_L),IEC_PH_EXT_MID_L,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IEC_PH_EXT_MID_R),IEC_PH_EXT_MID_R,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IIC_PH_INT_FWD),IIC_PH_INT_FWD,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IIC_PH_INT_AFT),IIC_PH_INT_AFT,FilterInput.TRIGGER_ON_UPDATE));
				
		_filterInputs.add(new FilterInput(filterInputName(IBX_PH_ESW),IBX_PH_ESW,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IBX_VH2O_INT_X),IBX_VH2O_INT_X,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_VH2O_INT_Y),IBX_VH2O_INT_Y,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_VH2O_EXT_X),IBX_VH2O_EXT_X,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_VH2O_EXT_Y),IBX_VH2O_EXT_Y,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IVC_VH2O_INT_X),IVC_VH2O_INT_X,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IVC_VH2O_INT_Y),IVC_VH2O_INT_Y,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IVC_VH2O_EXT_X),IVC_VH2O_EXT_X,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IVC_VH2O_EXT_Y),IVC_VH2O_EXT_Y,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IMA_MAG_INT_X),IMA_MAG_INT_X,FilterInput.TRIGGER_ON_UPDATE));		
		_filterInputs.add(new FilterInput(filterInputName(IMA_MAG_INT_Y),IMA_MAG_INT_Y,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IMA_MAG_EXT_X),IMA_MAG_EXT_X,FilterInput.TRIGGER_ON_UPDATE));		
		_filterInputs.add(new FilterInput(filterInputName(IMA_MAG_EXT_Y),IMA_MAG_EXT_Y,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IHD_HDG_INT_X),IHD_HDG_INT_X,FilterInput.TRIGGER_ON_UPDATE));		
		_filterInputs.add(new FilterInput(filterInputName(IHD_HDG_INT_Y),IHD_HDG_INT_Y,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IHD_HDG_EXT_X),IHD_HDG_EXT_X,FilterInput.TRIGGER_ON_UPDATE));		
		_filterInputs.add(new FilterInput(filterInputName(IHD_HDG_EXT_Y),IHD_HDG_EXT_Y,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IBX_VTHR_FWD),IBX_VTHR_FWD,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IBX_VTHR_AFT),IBX_VTHR_AFT,FilterInput.TRIGGER_ON_UPDATE));
		
		_filterInputs.add(new FilterInput(filterInputName(IVS_ESWV_FWD),IVS_ESWV_FWD,FilterInput.TRIGGER_ON_UPDATE));
		_filterInputs.add(new FilterInput(filterInputName(IVS_ESWV_AFT),IVS_ESWV_AFT,FilterInput.TRIGGER_ON_UPDATE));
	}
	
	/** Make connections between all the filters in the filtering
		network.
		@see #connectFilter(Filter filter)
	 */
	public void connectFilterNetwork(){
		Filter filter;
		
		for(Iterator i=_signalFilters.iterator();i.hasNext();){
			filter=(Filter)i.next();
			try{
			connectFilter(filter);
			}catch(Exception e){
				e.printStackTrace();
				_log4j.error(e.getMessage());
			}
		}
	}	
	
	/** make connections to the filtering network for the specified filter */
	public void connectFilter(Filter filter)
	throws Exception{
		
		switch(filter.getID()){
				// internal pH boxcar
			case BX_PH_INT_FWD_L:
				filter.addInput(getFilterInput(IBX_PH_INT_FWD_L));
				filter.attach(getFilterInput(IFC_PH_INT_FWD_L));					
				break;
			case BX_PH_INT_FWD_R:
				filter.addInput(getFilterInput(IBX_PH_INT_FWD_R));
				filter.attach(getFilterInput(IFC_PH_INT_FWD_R));					
				break;
			case BX_PH_INT_AFT_L:
				filter.addInput(getFilterInput(IBX_PH_INT_AFT_L));
				filter.attach(getFilterInput(IAC_PH_INT_AFT_L));					
				break;
			case BX_PH_INT_AFT_R:
				filter.addInput(getFilterInput(IBX_PH_INT_AFT_R));
				filter.attach(getFilterInput(IAC_PH_INT_AFT_R));					
				break;
				
				// external pH boxcar
			case BX_PH_EXT_MID_L:
				filter.addInput(getFilterInput(IBX_PH_EXT_MID_L));
				filter.attach(getFilterInput(IEC_PH_EXT_MID_L));					
				break;
			case BX_PH_EXT_MID_R:
				filter.addInput(getFilterInput(IBX_PH_EXT_MID_R));
				filter.attach(getFilterInput(IEC_PH_EXT_MID_R));					
				break;
				
				// fwd/aft pH combiners	
			case FC_PH_INT_FWD:
				filter.addInput(getFilterInput(IFC_PH_INT_FWD_L));
				filter.addInput(getFilterInput(IFC_PH_INT_FWD_R));
				filter.attach(getFilterInput(IIC_PH_INT_FWD));
				break;
			case AC_PH_INT_AFT:
				filter.addInput(getFilterInput(IAC_PH_INT_AFT_L));
				filter.addInput(getFilterInput(IAC_PH_INT_AFT_R));
				filter.attach(getFilterInput(IIC_PH_INT_AFT));
				break;
				
				// internal/external pH combiners	
			case IC_PH_INT:
				filter.addInput(getFilterInput(IIC_PH_INT_FWD));
				filter.addInput(getFilterInput(IIC_PH_INT_AFT));
				break;
			case EC_PH_EXT:
				filter.addInput(getFilterInput(IEC_PH_EXT_MID_L));
				filter.addInput(getFilterInput(IEC_PH_EXT_MID_R));
				break;
				
				// ESW pH boxcar
			case BX_PH_ESW:
				filter.addInput(getFilterInput(IBX_PH_ESW));
				break;
				
				// internal/external H20 velocity boxcars
			case BX_VH2O_INT_X:
				filter.addInput(getFilterInput(IBX_VH2O_INT_X));
				filter.attach(getFilterInput(IVC_VH2O_INT_X));					
				break;
			case BX_VH2O_INT_Y:
				filter.addInput(getFilterInput(IBX_VH2O_INT_Y));
				filter.attach(getFilterInput(IVC_VH2O_INT_Y));					
				break;
			case BX_VH2O_EXT_X:
				filter.addInput(getFilterInput(IBX_VH2O_EXT_X));
				filter.attach(getFilterInput(IVC_VH2O_EXT_X));					
				break;
			case BX_VH2O_EXT_Y:
				filter.addInput(getFilterInput(IBX_VH2O_EXT_Y));
				filter.attach(getFilterInput(IVC_VH2O_EXT_Y));					
				break;
				
				// internal/external H20 velocity combiners
			case VC_VH2O_INT_X:
				filter.addInput(getFilterInput(IVC_VH2O_INT_X));
				filter.attach(getFilterInput(IMA_MAG_INT_X));					
				filter.attach(getFilterInput(IHD_HDG_INT_X));
				break;
			case VC_VH2O_INT_Y:
				filter.addInput(getFilterInput(IVC_VH2O_INT_Y));
				filter.attach(getFilterInput(IMA_MAG_INT_Y));					
				filter.attach(getFilterInput(IHD_HDG_INT_Y));					
				break;
			case VC_VH2O_EXT_X:
				filter.addInput(getFilterInput(IVC_VH2O_EXT_X));
				filter.attach(getFilterInput(IMA_MAG_EXT_X));					
				filter.attach(getFilterInput(IHD_HDG_EXT_X));
				break;
			case VC_VH2O_EXT_Y:
				filter.addInput(getFilterInput(IVC_VH2O_EXT_Y));
				filter.attach(getFilterInput(IMA_MAG_EXT_Y));					
				filter.attach(getFilterInput(IHD_HDG_EXT_Y));					
				break;
				
				// internal/external H2O heading and magnitude
			case MA_MAG_INT:
				filter.addInput(getFilterInput(IMA_MAG_INT_X));
				filter.addInput(getFilterInput(IMA_MAG_INT_Y));
				break;
			case HD_HDG_INT:
				filter.addInput(getFilterInput(IHD_HDG_INT_X));
				filter.addInput(getFilterInput(IHD_HDG_INT_Y));
				break;
			case MA_MAG_EXT:
				filter.addInput(getFilterInput(IMA_MAG_EXT_X));
				filter.addInput(getFilterInput(IMA_MAG_EXT_Y));
				break;
			case HD_HDG_EXT:
				filter.addInput(getFilterInput(IHD_HDG_EXT_X));
				filter.addInput(getFilterInput(IHD_HDG_EXT_Y));
				break;
				
				// forward/aft thruster velocity boxcars
			case BX_VTHR_FWD:
				filter.addInput(getFilterInput(IBX_VTHR_FWD));
				break;
			case BX_VTHR_AFT:
				filter.addInput(getFilterInput(IBX_VTHR_AFT));
				break;
				
				// forward/aft ESW valve (unity filter)
			case VS_ESWV_FWD:
				filter.addInput(getFilterInput(IVS_ESWV_FWD));
				break;
			case VS_ESWV_AFT:
				filter.addInput(getFilterInput(IVS_ESWV_AFT));
				break;
				
			default:
				throw new Exception("Unsupported filter ID ["+filter.getID()+"]");
		}
	}
	
	/** Cause a new pH Range Validator instance to be 
		created (with latest configuration from ControlLoopAttributes)
	 */
	public void initPHRangeValidatorInstance(){
		_phRangeValidator=null;
		getPHRangeValidatorInstance();
	}
	/** initialize an input validator that will be used by shared
	// by all of the pH input connectors.
	// Uses simple range (including endpoints), and inserts the last 
	// valid value if an input is invalid.
	// May need one per channel, for example if the validator should 
	// return an average of previous values when an invalid input is encountered.
	// Could also implement as a filter, but for now a single validator may be used,
	// reducing the object count. 
	 */
	public RangeValidator getPHRangeValidatorInstance(){
		if(_phRangeValidator==null){
			try{
				_phRangeValidator= RangeValidator.getLastValidValidator(RangeValidator.RANGE_INSIDE,
																		_attributes.ph_valid_lo,
																		_attributes.ph_valid_hi,
																		true,
																		true);
			}catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return _phRangeValidator;
	}	
	
	/** Create and configure the input connectors for the data stream inputs.
		The configuration information is specified in ControlLoopAttributes
		in an array of ControlLoopAttributes.ConnectorSpec objects
	 
	 @see ControlLoopAttributes
	 @see ControlLoopAttributes.ConnectorSpec
	 */
	public void initializeInputConnectors()
	throws InvalidPropertyException,Exception{

		for(Iterator i=_attributes.inputConnectors(TYPE_ALL);i.hasNext();){
			try{
				// get input ConnectorSpec
				ControlLoopAttributes.ConnectorSpec connectorSpec = (ControlLoopAttributes.ConnectorSpec)i.next();
				configureInputConnector(connectorSpec);
			}catch (Exception e) {
				_log4j.error(e);
				//e.printStackTrace();
			}
		}
	}

	public void configureInputConnector(ControlLoopAttributes.ConnectorSpec connectorSpec)
	throws InvalidPropertyException,Exception{

		int role=connectorSpec.role();
		RangeValidator validator=null;
		FilterInput filterInput=null;

		switch (role) {
			case ROLE_INT_FWD_L_PH:
				filterInput=getFilterInput(IBX_PH_INT_FWD_L);
				validator=getPHRangeValidatorInstance();
				break;
			case ROLE_INT_FWD_R_PH:
				filterInput=getFilterInput(IBX_PH_INT_FWD_R);
				validator=getPHRangeValidatorInstance();
				break;
			case ROLE_INT_AFT_L_PH:
				filterInput=getFilterInput(IBX_PH_INT_AFT_L);
				validator=getPHRangeValidatorInstance();
				break;
			case ROLE_INT_AFT_R_PH:
				filterInput=getFilterInput(IBX_PH_INT_AFT_R);
				validator=getPHRangeValidatorInstance();
				break;
			case ROLE_EXT_MID_L_PH:
				filterInput=getFilterInput(IBX_PH_EXT_MID_L);
				validator=getPHRangeValidatorInstance();
				break;
			case ROLE_EXT_MID_R_PH:
				filterInput=getFilterInput(IBX_PH_EXT_MID_R);
				validator=getPHRangeValidatorInstance();
				break;
			case ROLE_ESW_PH:
				filterInput=getFilterInput(IBX_PH_ESW);
				validator=getPHRangeValidatorInstance();
				break;
			case ROLE_FWD_ESW_VALVE:
				filterInput=getFilterInput(IVS_ESWV_FWD);
				break;
			case ROLE_AFT_ESW_VALVE:
				filterInput=getFilterInput(IVS_ESWV_AFT);
				break;
			case ROLE_ESW_PUMP:
				//filterInput=getFilterInput();
				//break;
				return;
			case ROLE_INT_X_VELOCITY:
				filterInput=getFilterInput(IBX_VH2O_INT_X);
				break;
			case ROLE_INT_Y_VELOCITY:
				filterInput=getFilterInput(IBX_VH2O_INT_Y);
				break;
			case ROLE_EXT_X_VELOCITY:
				filterInput=getFilterInput(IBX_VH2O_EXT_X);
				break;
			case ROLE_EXT_Y_VELOCITY:
				filterInput=getFilterInput(IBX_VH2O_EXT_Y);
				break;
			case ROLE_FWD_THRUSTER:
				filterInput=getFilterInput(IBX_VTHR_FWD);
				break;
			case ROLE_AFT_THRUSTER:
				filterInput=getFilterInput(IBX_VTHR_AFT);
				break;
			default:
				throw new Exception("Invalid input connector role ["+role+"/"+_attributes.roleName(role)+"]");
		}
		
				
		// make an input connector to connect the filter to a data source,
		// as defined by the connectorSpec
		if(_log4j.isDebugEnabled()){
		_log4j.debug("FOCEProcess - creating input connector for "+connectorSpec.registry_key+"/"+connectorSpec.signal_key);
		}
		ControlInputIF connector=createConnector(connectorSpec,filterInput);
		
		if(connector==null){
			throw new Exception("Could not create connector (createConnector returned null)");
		}
		
		if(validator!=null){
			// set input validator for the connector
			connector.setValidator(validator);
		}
		
		// set the ID for the *input connector* to the role of the signal
		connector.setInputID(role);
	
		// if the connector exists, remove it
		ControlInputIF existingInput=getInput(role);
		if(existingInput!=null){
			_connectors.remove(existingInput);
		}
		
		// add the connector to the set of all connectors
		_connectors.add(connector);
	
	}
	
	public void initializeOutputConnectors()
	throws InvalidPropertyException,Exception{
	
		for(Iterator i=_attributes.outputConnectors(TYPE_ALL);i.hasNext();){
			try{
				// get input ConnectorSpec
				ControlLoopAttributes.ConnectorSpec connectorSpec = (ControlLoopAttributes.ConnectorSpec)i.next();
				configureOutputConnector(connectorSpec);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void configureOutputConnector(ControlLoopAttributes.ConnectorSpec connectorSpec)
	throws InvalidPropertyException,Exception{
		
		int role=connectorSpec.role();
		int outputID=-1;
		switch(role){
			case ROLE_ESW_PUMP:
				outputID=OUTPUT_ESW_PUMP_VELOCITY;
				break;
			case ROLE_FWD_THRUSTER:
				outputID=OUTPUT_FWD_THRUSTER_VELOCITY;
				break;
			case ROLE_AFT_THRUSTER:
				outputID=OUTPUT_AFT_THRUSTER_VELOCITY;
				break;
			case ROLE_FWD_ESW_VALVE:
				outputID=OUTPUT_FWD_ESW_VALVE;
				break;
			case ROLE_AFT_ESW_VALVE:
				outputID=OUTPUT_AFT_ESW_VALVE;
				break;
			default:
				throw new Exception("Invalid output connector role ["+role+"/"+_attributes.roleName(role)+"]");
		}
		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("FOCEProcess - creating output connector for "+connectorSpec.registry_key+"/"+connectorSpec.signal_key);
		}
		// make an output connector manage the device,
		// as defined by the connectorSpec
		ControlOutputIF output=createOutput(connectorSpec);
		// set the output ID
		output.setOutputID(outputID);
		// set the output name
		output.setName(outputName(outputID));

		// if the connector exists, remove it
		ControlOutputIF existingOutput=getOutput(role);
		if(existingOutput!=null){
			_outputs.remove(existingOutput);
		}
		
		// add the output
		_outputs.add(output);
	}
	
	/** set ControlLoopAttributes instance */
	public void setAttributes(ControlLoopAttributes attributes){
		
		_attributes=attributes;
		// logic to update things here
	}
	
	/** get ControlLoopAttributes instance */
	protected ControlLoopAttributes getAttributes(){
		
		return _attributes;
	}
	
	/** get mnemonic associated with specified mode ID */	
	public String modeName(int mode){
		return (String)mode_id2pname.get(new Integer(mode));
	}
	
		
	//////////////////////////
	// ProcessStateIF methods
	//////////////////////////

	/** Get signal value by ID.
	 Signals represent intermediate calculated values in the control loop processing chain,
	 i.e., the "lines" in control loop block diagram that connect functional blocks.
	 */
	public Number getSignal(int signalID) throws Exception{
		double signalValue=0.0;
		switch (signalID) {
			case SIG_PH_INT_FWD_L:
				return getInput(ROLE_INT_FWD_L_PH).getInputValue();
			case SIG_PH_INT_FWD_R:
				return getInput(ROLE_INT_FWD_R_PH).getInputValue();
				
			case SIG_PH_INT_AFT_L:
				return getInput(ROLE_INT_AFT_L_PH).getInputValue();
			case SIG_PH_INT_AFT_R:
				return getInput(ROLE_INT_AFT_R_PH).getInputValue();

			case SIG_PH_INT_FILT:
				return new Double(getFilter(IC_PH_INT).doubleValue());
	
			case SIG_PH_EXT_MID_L:
				return getInput(ROLE_EXT_MID_L_PH).getInputValue();
			case SIG_PH_EXT_MID_R:
				return getInput(ROLE_EXT_MID_R_PH).getInputValue();

			case SIG_PH_EXT_FILT:
				return new Double(getFilter(EC_PH_EXT).doubleValue());

			case SIG_PH_GRAD:
				double fwdCombiner=getFilter(FC_PH_INT_FWD).doubleValue();
				double aftCombiner=getFilter(AC_PH_INT_AFT).doubleValue();
				double gradient=fwdCombiner-aftCombiner;
				return new Double(gradient);
			
			case SIG_PH_ERR:
				double error;
				int mode=getParameter(PARAM_PH_CMODE).intValue();
				if(mode==CONTROL_MODE_OFFSET){
					double phInternal=getFilter(IC_PH_INT).doubleValue();
					double phExternal=getFilter(EC_PH_EXT).doubleValue();
					double offset=getParameter(PARAM_PH_OFFSET).doubleValue();
					error=(phExternal+offset)-phInternal;
				}else{
					double phInternal=getFilter(IC_PH_INT).doubleValue();
					double phSetpoint=getParameter(PARAM_PH_SETPOINT).doubleValue();
					error=phSetpoint-phInternal;
				}
				
				return new Double(error);
			case SIG_PH_ESW:
				return getInput(ROLE_ESW_PH).getInputValue();
			case SIG_PH_ESW_FILT:
				return new Double(getFilter(BX_PH_ESW).doubleValue());
				
			case SIG_VH2O_INT_X_RAW:				
				return getInput(ROLE_INT_X_VELOCITY).getInputValue();
			case SIG_VH2O_INT_X_FILT:				
				return new Double(getFilter(VC_VH2O_INT_X).doubleValue());
			
			case SIG_VH2O_INT_Y_RAW:				
				return getInput(ROLE_INT_Y_VELOCITY).getInputValue();
			case SIG_VH2O_INT_Y_FILT:				
				return new Double(getFilter(VC_VH2O_INT_Y).doubleValue());
			
			case SIG_VH2O_INT_MAG_FILT:				
				return new Double(getFilter(MA_MAG_INT).doubleValue());
			case SIG_VH2O_INT_DIR_FILT:				
				return new Double(getFilter(HD_HDG_INT).doubleValue());
			
			case SIG_VH2O_EXT_X_RAW:				
				return getInput(ROLE_EXT_X_VELOCITY).getInputValue();
			case SIG_VH2O_EXT_X_FILT:				
				return new Double(getFilter(VC_VH2O_EXT_X).doubleValue());
			
			case SIG_VH2O_EXT_Y_RAW:				
				return getInput(ROLE_EXT_Y_VELOCITY).getInputValue();
			case SIG_VH2O_EXT_Y_FILT:				
				return new Double(getFilter(VC_VH2O_EXT_Y).doubleValue());
			
			case SIG_VH2O_EXT_MAG_FILT:				
				return new Double(getFilter(MA_MAG_EXT).doubleValue());
			case SIG_VH2O_EXT_DIR_FILT:				
				return new Double(getFilter(HD_HDG_EXT).doubleValue());
			
			case SIG_VH2O_INT_ERR:	
				int cmode=getParameter(PARAM_VELOCITY_CMODE).intValue();
				if(cmode==CONTROL_MODE_MANUAL){
					return new Double(0.0);
				}
				double verror;
				if(cmode==CONTROL_MODE_OFFSET){
					double velocityInternal=getFilter(VC_VH2O_INT_X).doubleValue();
					double velocityExternal=getFilter(VC_VH2O_EXT_X).doubleValue();
					double offset=getParameter(PARAM_VELOCITY_OFFSET).doubleValue();
					verror=(velocityExternal+offset)-velocityInternal;
				}else{
					double velocityInternal=getFilter(VC_VH2O_INT_X).doubleValue();
					double velocitySetpoint=getParameter(PARAM_VELOCITY_SETPOINT).doubleValue();
					verror=velocitySetpoint-velocityInternal;
				}
				return new Double(verror);
			case SIG_ESW_INJ_VOL:
				double velocity=getInput(ROLE_ESW_PUMP).getInputValue().doubleValue();
				
				return new Double(velocity*_attributes.ESW_PUMP_DISPLACEMENT_ML_PER_REV/1000.0);
			case SIG_ESW_PUMP_CMD_RAW:
				break;
			case SIG_ESW_PUMP_CMD_CHK:				
				break;
			case SIG_ESW_PUMP_VEL:				
				ControlInputIF eswpInput= getInput(ROLE_ESW_PUMP);
				if(eswpInput==null){
					return null;
				}else{
					return eswpInput.getInputValue();
				}
			case SIG_ESW_FWD_VALVE_CMD:
				break;
			case SIG_ESW_FWD_VALVE_STATE:
				ControlInputIF fvInput= getInput(ROLE_FWD_ESW_VALVE);
				if(fvInput==null){
					return null;
				}else{
					return fvInput.getInputValue();
				}
			case SIG_ESW_AFT_VALVE_CMD:				
				break;
			case SIG_ESW_AFT_VALVE_STATE:				
				ControlInputIF avInput= getInput(ROLE_AFT_ESW_VALVE);
				if(avInput==null){
					return null;
				}else{
					return avInput.getInputValue();
				}
			case SIG_FWD_THRUSTER_VEL_CMD_RAW:				
				break;
			case SIG_FWD_THRUSTER_VEL_CMD_CHK:				
				break;
			case SIG_FWD_THRUSTER_VEL:				
				ControlInputIF fthInput= getInput(ROLE_FWD_THRUSTER);
				if(fthInput==null){
					return null;
				}else{
					return fthInput.getInputValue();
				}
			case SIG_AFT_THRUSTER_VEL_CMD_RAW:				
				break;
			case SIG_AFT_THRUSTER_VEL_CMD_CHK:				
				break;
			case SIG_AFT_THRUSTER_VEL:				
				ControlInputIF athInput= getInput(ROLE_AFT_THRUSTER);
				if(athInput==null){
					return null;
				}else{
					return athInput.getInputValue();
				}
			default:
				throw new Exception("Unknown signal value ["+signalID+"]");
		}
		return new Double(signalValue);
	}
		
	/** get current value of a control system parameter by ID */
	public Number getParameter(int paramID) throws Exception{
		Number paramValue=new Integer(-1);
		switch (paramID) {
			case PARAM_PH_SETPOINT:
				return new Double(_attributes.ph_setpoint);
			case PARAM_PH_OFFSET:
				return new Double(_attributes.ph_offset);
			case PARAM_MAX_FLOW_CHANGE_PERCENT:
				return new Double(_attributes.max_flow_change_percent);
			case PARAM_PH_ABS_MIN:
				return new Double(_attributes.ph_abs_min);
			case PARAM_PH_ABS_MAX:
				return new Double(_attributes.ph_abs_max);
			case PARAM_PH_DEADBAND_LO:
				return new Double(_attributes.ph_deadband_lo);
			case PARAM_PH_DEADBAND_HI:
				return new Double(_attributes.ph_deadband_hi);
			case PARAM_PH_MAX_CORRECTION:
				return new Double(_attributes.ph_max_correction);
			case PARAM_PH_CMODE:
				return new Integer(_attributes.ph_control_mode);
			case PARAM_PH_RMODE:
				return new Integer(_attributes.ph_response_mode);

			case PARAM_EXP_A:
				return new Double(_attributes.exp_a);
			case PARAM_EXP_B:
				return new Double(_attributes.exp_b);
			case PARAM_EXP_H:
				return new Double(_attributes.exp_h);
			case PARAM_EXP_K:
				return new Double(_attributes.exp_k);
			case PARAM_PH_LIN_SLOPE:
				return new Double(_attributes.ph_lin_slope);
			case PARAM_PH_LIN_OFFSET:
				return new Double(_attributes.ph_lin_offset);
			case PARAM_PH_LIN_FILTER_DEPTH:
				return new Double(_attributes.ph_lin_filter_depth);
			case PARAM_PH_LIN_ERROR_LIMIT:
				return new Double(_attributes.ph_lin_error_limit);
			case PARAM_PH_PID_KP:
				return new Float(_attributes.ph_pid_Kp);
			case PARAM_PH_PID_KI:
				return new Float(_attributes.ph_pid_Ki);
			case PARAM_PH_PID_KD:
				return new Float(_attributes.ph_pid_Kd);
			case PARAM_PH_PID_MAX_KI:
				return new Float(_attributes.ph_pid_max_ki);
			case PARAM_PH_PID_SCALE_FACTOR:
				return new Float(_attributes.ph_pid_scale_factor);
			case PARAM_CO2_CONCENTRATION:
				return new Double(_attributes.CO2_CONCENTRATION_MMOL_PER_L);
			case PARAM_FLUME_AREA:
				return new Double(_attributes.FLUME_AREA_M2);
			case PARAM_DENSITY_SW:
				return new Double(_attributes.SW_DENSITY_KG_PER_M3);
			case PARAM_VELOCITY_PID_KP:
				return new Float(_attributes.velocity_pid_Kp);
			case PARAM_VELOCITY_PID_KI:
				return new Float(_attributes.velocity_pid_Ki);
			case PARAM_VELOCITY_PID_KD:
				return new Float(_attributes.velocity_pid_Kd);
			case PARAM_VELOCITY_PID_MAX_KI:
				return new Float(_attributes.velocity_pid_max_ki);
				
			case PARAM_VELOCITY_SETPOINT:
				return new Double(_attributes.velocity_setpoint);
			case PARAM_VELOCITY_OFFSET:
				return new Double(_attributes.velocity_offset);
			case PARAM_VELOCITY_MIN_RPM:
				return new Double(_attributes.velocity_min_rpm);
			case PARAM_VELOCITY_MAX_RPM:
				return new Double(_attributes.velocity_max_rpm);
			case PARAM_VELOCITY_DEADBAND_LO:
				return new Double(_attributes.velocity_deadband_lo);
			case PARAM_VELOCITY_DEADBAND_HI:
				return new Double(_attributes.velocity_deadband_hi);
			case PARAM_VELOCITY_MAX_CORRECTION:
				return new Double(_attributes.velocity_max_correction);
			case PARAM_VELOCITY_CMODE:
				return new Integer(_attributes.velocity_control_mode);
			case PARAM_VELOCITY_RMODE:
				return new Integer(_attributes.velocity_response_mode);

			case PARAM_VELOCITY_CAL_A:
				return new Double(_attributes.velocity_cal_a);
			case PARAM_VELOCITY_CAL_B:
				return new Double(_attributes.velocity_cal_b);
			case PARAM_VELOCITY_CAL_C:
				return new Double(_attributes.velocity_cal_c);

			case PARAM_ESW_VALVE_AMODE:
				return new Double(_attributes.esw_valve_actuation);
			case PARAM_ESW_PUMP_AMODE:
				return new Double(_attributes.esw_pump_actuation);
			case PARAM_THRUSTER_AMODE:
				return new Double(_attributes.thruster_actuation);
				
			default:
				throw new Exception("Invalid parameter ["+paramID+"]");
		}
	}
	
	/** get the parameter ID by name */
	public int parameterID(String parameterName)
	throws Exception{
		Class c=ProcessParameterIF.class;
		Field signalField=c.getField(parameterName);
		return signalField.getInt(this);
	}
	
	/** get the parameter name by ID */
	public String parameterName(int parameterID)
	throws Exception{
		return (String)param_id2pname.get(new Integer(parameterID));
	}
	
	/** get the signal ID by name */
	public int signalID(String signalName)
	throws Exception{
		Class c=ProcessParameterIF.class;
		Field signalField=c.getField(signalName);
		return signalField.getInt(this);
	}
	/** return set of parameter names that may be used to get parameter values
	 KeySet/Iterator are not exportable, so we deal in String arrays */
	public String[] parameterNames(){
		String[] names=new String[param_pname2aname.size()];
		Iterator i=param_pname2aname.keySet().iterator();
		for(int j=0;i.hasNext();j++){
			names[j]=(String)i.next();
		}
		return names;
	}
	/** return set of signal names that may be used to get signal values
	 KeySet/Iterator are not exportable, so we deal in String arrays */
	public String[] signalNames(){
		String[] names=new String[signal_id2sname.size()];

		Iterator i=signal_id2sname.values().iterator();
		for(int j=0;i.hasNext();j++){
			names[j]=(String)i.next();
		}
		return names;
	}
	
	/** return a signal name for the given signal ID */
	public String signalName(int signalID){
		return (String)signal_id2sname.get(new Integer(signalID));
	}
	/** return a filter name for the given filter ID */
	public String filterName(int filterID){
		return (String)filter_id2fname.get(new Integer(filterID));
	}
	/** return a filter input name for the given signal ID */
	public String filterInputName(int inputID){
		return (String)input_id2iname.get(new Integer(inputID));
	}
	
	/** return an output name for the given output ID */
	public String outputName(int outputID){
		return (String)output_id2name.get(new Integer(outputID));
	}
	
	
	//////////////////////////
	// ProcessConfigIF methods
	//////////////////////////

	/** set a control system parameter */
	public void setParameter(int paramID, Number paramValue) throws Exception{
		setParameter((String)param_id2pname.get(new Integer(paramID)),paramValue.toString());
	}
	
	/** Set parameter using the underlying ControlLoopAttributes class.
		This ensures that proper validation is done when assigning the new value.
	 */
	public void setParameter(String parameterName, String parameterValue) throws Exception{
		
		Properties aprops=_attributes.toConfigurableProperties();
		String aname=null;
		try{
			// validate property
			aname=(String)param_pname2aname.get(parameterName);
			if(_log4j.isDebugEnabled()){
			_log4j.debug(parameterName+" maps to "+aname);
			}
			// if the parameter is a mnemonic, get String representation of numeric value
			Number test=(Number)const_mnem2value.get(parameterValue);
			if(_log4j.isDebugEnabled()){
			_log4j.debug("******** testing param value ["+parameterValue+"]  as mnemonic");
			}
			if(test!=null){
				if(_log4j.isDebugEnabled()){
				_log4j.debug("******* param value ["+parameterValue+"]  is a mnemonic, value:["+test+"]");
				}
				parameterValue=test.toString();
			}// else not a mnemonic const
			
			if(_log4j.isDebugEnabled()){
			_log4j.debug("******* setting param name ["+aname+"] to param value ["+parameterValue+"]");
			}			
			_attributes.setAttributeCallback(aname,parameterValue);
			aprops.setProperty(aname,parameterValue);
		}catch (InvalidPropertyException e) {
			throw new Exception("invalid property exception setting ["+parameterName+"/"+aname+": value:"+parameterValue+"] "+e);
		}
		_attributes.fromProperties(aprops,true);

	}

	//////////////////////////
	// ControlProcessIF methods
	//////////////////////////
	/** shutdown process */
	public void stopProcess() 
	throws Exception{
		for(Iterator i=_connectors.iterator();i.hasNext();){
			ControlInputIF connector = (ControlInputIF)i.next();
			try{
				if(_log4j.isDebugEnabled()){
				_log4j.debug("disconnecting "+connector.getInputID());
				}
				connector.disconnect();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		setState(CPIF_STATE_STOPPED);
	}
	
	public void simpleStart()
	throws Exception{
		
		try{
			_node=getNodeReference(_attributes.siamHost);
			createFilters();
			createFilterInputs();
			configureAllFilters();
			connectFilterNetwork();
			initializeInputConnectors();
			initializeOutputConnectors();
		}catch(Exception e){
			_log4j.error("error initializing inputs:");
			e.printStackTrace();
		}
		
		// connect to data streams
		for(Enumeration e=_connectors.elements();e.hasMoreElements();){
			ControlInputIF connector=(ControlInputIF)e.nextElement();
			try{
				// do any initialization for the connector
				connector.initialize();
				// connect() causes the connection to start receiving data
				// e.g. by starting worker threads (OSDTConnectionWorker)
				// or registering dataCallbacks (RegistryConnectionWorker)
				connector.connect();
			}catch (Exception ce) {
				_log4j.error("data stream connection error:"+ce);
			}
		}
		setState(CPIF_STATE_RUNNING);
		
	}
	
	public void originalStart()
	throws Exception{
		throw new Exception("Method originalStart() is deprecated - use simpleStart");
		/*
		try{
			_node=getNodeReference(_attributes.siamHost);
			//createFilters();
			initializePhInputs();
			initializeVelocityInputs();
			initializeMotorInputs();
		}catch(Exception e){
			_log4j.error("error initializing inputs:");
			e.printStackTrace();
		}
		
		try{
			initializeMotorOutputs();
			initializeValveOutputs();
		}catch(Exception e){
			_log4j.error("error initializing outputs:");
			e.printStackTrace();
		}
		
		try{
			// connect intermediate filters
			
			FilterInput internalCombinerFwd=new FilterInput(filterInputName(IIC_PH_INT_FWD),IIC_PH_INT_FWD);
			FilterInput internalCombinerAft=new FilterInput(filterInputName(IIC_PH_INT_AFT),IIC_PH_INT_AFT);
			getFilter(IC_PH_INT).addInput(internalCombinerFwd);
			getFilter(IC_PH_INT).addInput(internalCombinerAft);			
			getFilter(FC_PH_INT_FWD).attach(internalCombinerFwd);
			getFilter(AC_PH_INT_AFT).attach(internalCombinerAft);
			_filterInputs.add(internalCombinerFwd);
			_filterInputs.add(internalCombinerAft);
			
			FilterInput xIntInput=new FilterInput(filterInputName(IMA_MAG_INT_X),IMA_MAG_INT_X);
			FilterInput yIntInput=new FilterInput(filterInputName(IMA_MAG_INT_Y),IMA_MAG_INT_Y);
			getFilter(MA_MAG_INT).addInput(xIntInput);
			getFilter(MA_MAG_INT).addInput(yIntInput);
			getFilter(HD_HDG_INT).addInput(xIntInput);
			getFilter(HD_HDG_INT).addInput(yIntInput);
			getFilter(VC_VH2O_INT_X).attach(xIntInput);
			getFilter(VC_VH2O_INT_Y).attach(yIntInput);
			_filterInputs.add(xIntInput);
			_filterInputs.add(yIntInput);
			
			FilterInput xExtInput=new FilterInput(filterInputName(IMA_MAG_EXT_X),IMA_MAG_EXT_X);
			FilterInput yExtInput=new FilterInput(filterInputName(IMA_MAG_EXT_Y),IMA_MAG_EXT_Y);
			getFilter(MA_MAG_EXT).addInput(xExtInput);
			getFilter(MA_MAG_EXT).addInput(yExtInput);
			getFilter(HD_HDG_EXT).addInput(xExtInput);
			getFilter(HD_HDG_EXT).addInput(yExtInput);
			getFilter(VC_VH2O_EXT_X).attach(xExtInput);
			getFilter(VC_VH2O_EXT_Y).attach(yExtInput);
			_filterInputs.add(xExtInput);
			_filterInputs.add(yExtInput);
		}catch(Exception e){
			_log4j.error("error initializing filters:");
			e.printStackTrace();
		}
		
		// connect to data streams
		for(Enumeration e=_connectors.elements();e.hasMoreElements();){
			ControlInputIF connector=(ControlInputIF)e.nextElement();
			try{
				// do any initialization for the connector
				connector.initialize();
				// connect() causes the connection to start receiving data
				// e.g. by starting worker threads (OSDTConnectionWorker)
				// or registering dataCallbacks (RegistryConnectionWorker)
				connector.connect();
			}catch (Exception ce) {
				_log4j.error("data stream connection error:"+ce);
			}
		}
		setState(CPIF_STATE_RUNNING);
		 */
	}
	
	/** initialize process */
	public void startProcess() 
	throws Exception{
		
		simpleStart();
		// originalStart();
		 
	}
	
	public void setOutput(int roleID, ControlOutputIF output) throws Exception{
		for(Iterator i=_outputs.iterator();i.hasNext();){
			ControlOutputIF nextOutput=(ControlOutputIF)i.next();
			// remove current output with this ID if it exists
			if(nextOutput.getOutputID()==roleID){
				i.remove();
				break;
			}
		}
		// add the output
		_outputs.add(output);
	}
	
	public ControlOutputIF getOutput(int outputID) throws RemoteException{
		for(Iterator i=_outputs.iterator();i.hasNext();){
			ControlOutputIF nextOutput=(ControlOutputIF)i.next();
			if(nextOutput.getOutputID()==outputID){
				return nextOutput;
			}
		}
		return null;
	}
	
	public ControlOutputIF[] getOutputs() 
	throws RemoteException{
		Object[] oOutputs=_outputs.toArray();
		ControlOutputIF[] outputs=new ControlOutputIF[oOutputs.length];
		for (int i=0;i<oOutputs.length;i++ ) {
			outputs[i]=(ControlOutputIF)oOutputs[i];
		}
		return outputs;
	}
	
	public ControlInputIF getInput(int inputID) throws RemoteException{
		for(Enumeration e=_connectors.elements();e.hasMoreElements();){
			ControlInputIF input = (ControlInputIF)e.nextElement();
			if(input.getInputID()==inputID){
				return input;
			}
		}
		
		return null;
	}
	
	public ControlInputIF[] getInputs() 
	throws RemoteException{
		Object[] oConnectors=_connectors.toArray();
		ControlInputIF[] connectors=new ControlInputIF[oConnectors.length];
		for (int i=0;i<oConnectors.length;i++ ) {
			connectors[i]=(ControlInputIF)oConnectors[i];
		}
		return connectors;
	}
	
	public int getState() throws RemoteException{
		return _controlProcessState;
	}
	
	/** set ControlProcessIF state (but not part of the ControlProcessIF interface) */
	public void setState(int state) throws Exception{
		switch (state) {
			case CPIF_STATE_UNKNOWN:
			case CPIF_STATE_INSTANTIATED:
			case CPIF_STATE_STOPPED:
			case CPIF_STATE_RUNNING:
				_controlProcessState=state;
				break;
			default:
				throw new Exception("invalid state ["+state+"] in setState");
		}
	}

	//////////////////////////
	// intermediate calculation methods
	//////////////////////////
	
	/** return water volume flow m^3/min */
	public double volumeH2O(double area, double velocity){
		return 60.0*area*velocity;//60 sec/min * m^2 * m/sec => m^3/min
	}
	
	/** return water mass flow kg/min */
	public double massflowH2O(double densityCO2, double volumeH2O){
		return densityCO2*volumeH2O; // kg/m^3 * m^3/min => kg/min
	}
	
	/** return CO2 flow mmol/min */
	public double flowCO2(double massCO2, double massH2O){
		return massCO2*massH2O*0.001; //umol/kg * kg/min * 0.001 mmol/umol=> mmol/min
	}
	/** return CO2 volume flow liters/min */
	public double volumeCO2(double flowCO2, double concentrationCO2){
		return flowCO2/concentrationCO2; // mmol/min / mmol/l => l/min
	}
	/** return required pumped fluid volume rate (liters/min),
		given a desired change in pH.
	 */
	public double dph2rate(double deltaPH){
		/*
		if(_log4j.isDebugEnabled()){
		_log4j.debug("dph2rate - deltaPH:"+_decFormat.format(deltaPH));
		}
		 */
		try{
			
			double flumeArea=_attributes.FLUME_AREA_M2;
			
			//double seawaterVelocity=getSignal(SIG_VH2O_INT_X_FILT).doubleValue()*0.01;// (cm/s) * (0.01 m/cm) => m/sec
			double seawaterVelocity=getSignal(SIG_VH2O_INT_MAG_FILT).doubleValue()*0.01;// (cm/s) * (0.01 m/cm) => m/sec
			
			double seawaterDensity=_attributes.SW_DENSITY_KG_PER_M3;//1031.4 1022.8 kg/m^3
			
			double seawaterRate=Math.abs(seawaterVelocity)*flumeArea*seawaterDensity*60.0;// (m/s)*(m^2)*(kg/m3)*(s/min) =>kg/min

			double phSensitivity=_attributes.DELTA_PH_PER_MMOL_PER_KG; // (deltaPH/mmol/kg)
			
			//  [ (kg/min)*(ph)/(ph kg/mmol) ] / (mmol/l) => (l/min)
			double sweRate=(seawaterRate*Math.abs(deltaPH)/phSensitivity)/_attributes.CO2_CONCENTRATION_MMOL_PER_L;

			if(_log4j.isDebugEnabled()){
				
				_log4j.debug("\n  phOffset   (pH)    :"+_decFormat.format(deltaPH)+
							 "\n  flumeArea  (m^2)   :"+_decFormat.format(flumeArea)+
							 "\n  seVelocity (m/s)   :"+_decFormat.format(seawaterVelocity)+
							 "\n  seDensity  (kg/m^3):"+_decFormat.format(seawaterDensity)+
							 "\n  seRate     (kg/min):"+_decFormat.format(seawaterRate)+
							 "\n  phSen (pH/mmol/kg) :"+_decFormat.format(phSensitivity)+
							 "\n  sweRate    (l/min) :"+_decFormat.format(sweRate));
				
			}			
			
			return sweRate;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 0.0;
	}
	
	/** compute pump speed (rpm) from CO2 flow rate (l/min)  */
	public double volume2rpm(double volRate){
		// ml/rev / 1000 ml/l => l/rev
		double displacementL_PER_REV=_attributes.ESW_PUMP_DISPLACEMENT_ML_PER_REV/1000.0;
		
		// (liters/min) / (liters/revolution) => revolutions/min
		double rpm=volRate/displacementL_PER_REV;
		if(_log4j.isDebugEnabled()){
		_log4j.debug("\n  volume rate (l/min):"+_decFormat.format(volRate)+
					 "\n  displacement (l/rev):"+_decFormat.format(displacementL_PER_REV)+
					 "\n  pump command (rpm):"+_decFormat.format(rpm));
		}		
		return (rpm);
	}
	
	/**  computer thruster speed (rpm) from water velocity (cm/sec) 
		MotorRPM= aV^2+bV+c
		where V is water velocity (cm/sec)
	 */
	public double vel2rpm(double velocity){
		double speedRPM=(_attributes.velocity_cal_a*velocity*velocity)+(_attributes.velocity_cal_b*velocity)+(_attributes.velocity_cal_c);
		return speedRPM;
	}
	
	//////////////////////////
	// utilities
	//////////////////////////
	
	public Filter getFilter(int filterID){
		for(Iterator i=_signalFilters.iterator();i.hasNext();){
			Filter filter=(Filter)i.next();
			if(filter.getID()==filterID){
				return filter;
			}
		}
		return null;
	}
	
	public FilterInput getFilterInput(int inputID){
		for(Iterator i=_filterInputs.iterator();i.hasNext();){
			FilterInput input=(FilterInput)i.next();
			if(input.id()==inputID){
				return input;
			}
		}
		return null;
	}
	
	public void setFilterInputInhibit(int filterID, String inputName,boolean inhibitValue)
	throws Exception{
		Filter filter=getFilter(filterID);
		if(filter==null){
			throw new Exception("null filter in inhibitFilterInput");
		}
		FilterInput input=filter.getInput(inputName);
		if(input==null){
			throw new Exception("null input in inhibitFilterInput");
		}
		input.setInhibit(inhibitValue);
	}
	
	public Node getNodeReference(String siamHost) throws Exception{
		
		if(siamHost==null){
			return null;
		}
		
		// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new SecurityManager());
		}
		
		if(_node==null){
			String _nodeURL=NodeUtility.getNodeURL(siamHost);
			if(_log4j.isDebugEnabled()){
			_log4j.debug("registry2osdt using nodeURL "+_nodeURL);
			}
			
			// Create socket factory; overcomes problems with RMI 'hostname'
			// property.
			try {
				String host = NodeUtility.getHostName(_nodeURL);
				if(_log4j.isDebugEnabled()){
				_log4j.debug("getNodeReference setting SocketFactory using host "+host);
				}
				RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
			}
			catch (MalformedURLException e) {
				_log4j.error("Malformed URL \"" + _nodeURL + "\": " + 
							 e.getMessage());
			}
			catch(SocketException se){
				_log4j.info("SocketException:"+se.getMessage());
				//se.printStackTrace();
			}
			catch (IOException e) {
				_log4j.error("RMISocketFactory.setSocketFactory() failed");
				throw e;
			}
			
			// Get the node's proxy
			try {
				if(_log4j.isDebugEnabled()){
				_log4j.debug("Get node proxy from " + _nodeURL);
				}
				_node = (Node)Naming.lookup(_nodeURL.toString());
			}
			catch (Exception e) {
				_log4j.error("Couldn't get node proxy at " + _nodeURL + ":");
				throw e;
			}
		}
		return _node;
	}
	
	public Instrument lookupSIAMService(String siamHost, String registryName)
	throws Exception{
		
		_node=getNodeReference(siamHost);
		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("Have Node Proxy, requesting service");
		}
		Instrument service=_node.lookupService(registryName);
		return service;
	}
	
	public String registry2osdt(String siamHost, String registryName)
	throws Exception{
				
		Instrument service=lookupSIAMService(siamHost,registryName);
		String osdtName = null;

		if(service==null){
			throw new Exception("registry2osdt - could not find service ["+registryName+" on node "+siamHost+"]");
		}else{
			String serviceName=new String(service.getName());
			if(_log4j.isDebugEnabled()){
			_log4j.debug("serviceName - "+serviceName);
			}			
			if(serviceName.length() > 0){
				// Use service mnemonic to build turbine name
				osdtName = (serviceName.replace(' ', '_') + "-" + service.getId());
				if(_log4j.isDebugEnabled()){
				_log4j.debug("osdtName.mod - "+osdtName);
				}
			}else{
				
				// Use class name to build turbine name
				osdtName = service.getClass().getName();
				int index = osdtName.lastIndexOf(".");
				if (index >= 0) {
					osdtName = osdtName.substring(index+1);
				}
				osdtName = osdtName + "-" + service.getId();
				if(_log4j.isDebugEnabled()){
				_log4j.debug("osdtName.build - "+osdtName);
				}
			}
		}
		return osdtName;
	}

	//////////////////////////
	// string me
	//////////////////////////
	public String toString(){
		StringBuffer buffer=new StringBuffer();
		buffer.append("FOCEProcess state\n");
		if(_attributes!=null){
		buffer.append(_attributes.toString()+"\n");
		}
		if(_signalFilters!=null){
		for(Iterator i=_signalFilters.iterator();i.hasNext();){
			Filter filter=(Filter)i.next();
			if(filter!=null){
				buffer.append(filter.name()+": output="+_decFormat.format(filter.doubleValue())+"\n");
			}
		}
		}
		return buffer.toString();
	}
	
		
}
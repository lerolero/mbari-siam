// Copyright MBARI 2003
package org.mbari.siam.foce.utils;

import java.rmi.RemoteException;
import java.rmi.Naming;

import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.operations.utils.PortUtility;
import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ElmoLouverIF;
import org.mbari.siam.distributed.devices.ElmoThrusterIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.foce.devices.elmo.base.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class ElmoClientExample extends PortUtility{
	
    static private Logger _log4j = Logger.getLogger(ElmoClientExample.class);
	
	private ElmoThrusterIF _elmoThruster;
	private Instrument _instrument;

	private double _thrusterVelocityRPM=20;
	
	public static void main(String[] args) {
		// Configure log4j
		PropertyConfigurator.configure(System.getProperties());
		BasicConfigurator.configure();
		
		ElmoClientExample app=new ElmoClientExample();
		app.multiPortsAllowed(false);
		app.processArguments(args,3);
		app.run();
	}
	
	/** No custom options for this application. */
	public void processCustomOption(String[] args, int index) 
	throws InvalidOption 
	{
		System.err.println("processCustomOption");
		System.err.println("args len:"+args.length);
		
		_thrusterVelocityRPM=Double.parseDouble(args[2]);
		
		// No custom options for this application... so throw 
		// invalid option exception if this method is called.
		//throw new InvalidOption("unknown option: " + args[index]);
	}
	
	/** Print application-specific usage message to stdout. 
	 */
	public  void printUsage(){
		System.out.println("ElmoClientExample <host> <port> [thrusterRPM]");
	}
	
	/** get remote service stub and cast it to various interfaces
	 */
	public void getServiceStub(Node node, String portName){
		try{
			// get a Device service from the node
			Device device = node.getDevice(portName.getBytes());
			
			// if it implements the Instrument interface...
			if (device instanceof Instrument){
				// cast the device as an Instrument
				_instrument = (Instrument)device;
				
				// see if it implements the ElmoThrusterIF interface
				if(_instrument instanceof ElmoThrusterIF){
					// cast the instrument as an ElmoThruster and ElmoService
					_elmoThruster = (ElmoThrusterIF)_instrument;
				}else{
					_log4j.error("Device on port " + portName + 
								 " is not an ElmoThruster");
				}
			}else{
				_log4j.error("Device on port " + portName + 
							 " is not an Instrument");
			}
		}
		catch (PortNotFound e){
			_log4j.error("Port " + portName + " not found");
		}
		catch (DeviceNotFound e){
			_log4j.error("Device not found on port " + portName);
		}catch (Exception e){
			_log4j.error("exception " + e);
		}
	}
	
	/** Use the ElmoThrusterIF interface to write and read motor velocity */
	public int writeReadVelocity(double thrusterVelocityRPM) throws Exception{
		try{
			// calculate the motor speed
			// using the ElmoThruster interface
			int velocityCounts = _elmoThruster.rpm2counts(thrusterVelocityRPM);
			
			// set the jogging velocity (enables and begins motion)
			// using the ElmoThruster interface
			System.out.println("writing jogging velocity: "+velocityCounts+" counts/sec");
			_elmoThruster.setJoggingVelocity(velocityCounts);
			
			// read and return the jogging velocity
			// using the ElmoThruster interface
			int joggingVelocity=_elmoThruster.getJoggingVelocity();
			System.out.println("read jogging velocity: "+joggingVelocity);
			return joggingVelocity;
		}
		catch (Exception e){
			_log4j.error(e);
			throw e;
		}
	}
	
	public void parsePacket(){
		try{
			// get data packet
			// using the Instrument interface
			System.out.println("requesting data packet...");

			SensorDataPacket packet = _instrument.getLastSample();	
			// parse data packet
			// using the Instrument interface
			PacketParser parser = _instrument.getParser();

			System.out.println("parsing data packet...");
			PacketParser.Field[] packetFields = parser.parseFields(packet);

			// do stuff with the parsed packet...
			System.out.println("data packet contents:\n"+packetFields);
			for(int i=0;i<packetFields.length;i++)
				System.out.println(packetFields[i].getName()+","+packetFields[i].getValue()+","+packetFields[i].getUnits());
		}
		catch (Exception e){
			_log4j.error(e);
		}
		
	}
	
	/** main PortUtility action */
	public void processPort(Node node, String portName)
	throws RemoteException{
		try{
		// get the service stub
		getServiceStub(node,portName);
		
		// do something (set and read back the velocity)
		// using ElmoThrusterIF interface
		writeReadVelocity(this._thrusterVelocityRPM);
			
		// get and parse a packet
		// using Instrument interface
		parsePacket();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
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
import org.mbari.siam.foce.devices.elmo.base.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
 Acquire and print data sample from specified instrument.
 */
public class ElmoRMI extends PortUtility 
	{
		static private Logger _log4j = Logger.getLogger(ElmoRMI.class);
		String _commands[]=null;
		ElmoCI _elmoCI=new ElmoCI();
		
		public static void main(String[] args) 
		{
			// Configure log4j
			PropertyConfigurator.configure(System.getProperties());
			BasicConfigurator.configure();
			
			ElmoRMI app = new ElmoRMI();
			_log4j.debug("args.length:"+args.length);
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
			_commands=new String[args.length-index];
			for(int i=index;i<args.length;i++){
				System.err.println("args["+i+"]:"+args[i]);
				_commands[i-index]=args[i];
			}

			// No custom options for this application... so throw 
			// invalid option exception if this method is called.
			//throw new InvalidOption("unknown option: " + args[index]);
		}
		
		/** Print application-specific usage message to stdout. 
		 */
		public  void printUsage(){
			_elmoCI.printUsage();
		}
		
		/** Sample specified port. */
		public void processPort(Node node, String portName) 
		throws RemoteException 
		{
			try 
			{
				// get a Device from the node
				Device device = node.getDevice(portName.getBytes());
				
				// if it implements the Instrument interface...
				if (device instanceof Instrument) 
				{
					Instrument instrument = (Instrument)device;

					// see if it implements one of the Elmo interfaces
					if(instrument instanceof ElmoLouverIF){
						ElmoLouverIF _elmo= (ElmoLouverIF)instrument;
						// pass the command interface an ElmoLouver
						_elmoCI.setElmo(_elmo);
					}else if(instrument instanceof ElmoThrusterIF){
						// pass the command interface an ElmoThruster
						ElmoThrusterIF _elmo= (ElmoThrusterIF)instrument;
						_elmoCI.setElmo(_elmo);
					}else if(instrument instanceof ElmoIF){
						// pass the command interface an ElmoIF (basic motor)
						ElmoIF _elmo= (ElmoIF)instrument;
						_elmoCI.setElmo(_elmo);
					}else{
						_log4j.error("Device on port " + portName + 
									  " is not an ElmoIF");
					}
					_elmoCI.run(_commands);
				}
				else 
				{
					_log4j.error("Device on port " + portName + 
								  " is not an Instrument");
				}
			}
			catch (PortNotFound e) 
			{
				_log4j.error("Port " + portName + " not found");
			}
			catch (DeviceNotFound e) 
			{
				_log4j.error("Device not found on port " + portName);
			}
		}
		
	}

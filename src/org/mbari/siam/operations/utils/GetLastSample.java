// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.text.ParseException;

import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NotSupportedException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/**
   Acquire and print data sample from specified instrument.
 */
public class GetLastSample extends PortUtility 
{
    static private Logger _logger = Logger.getLogger(GetLastSample.class);
    boolean _doParse=false;

    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	GetLastSample sampler = new GetLastSample();
	sampler.processArguments(args);
	sampler.run();
    }
    
    
    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption 
    {

	for(int i=0;i<args.length;i++){
	    if(args[i].equals("-p")){
		_doParse=true;
	    }
	}
	// No custom options for this application... so throw 
	// invalid option exception if this method is called.
	//throw new InvalidOption("unknown option: " + args[index]);
    }

    /** Print usage message. */
    public void printUsage() 
    {
	System.err.println("GetLastSample nodeURL port [-p]");
	System.err.println("\n -p : parse packet if supported");
    }

    /** Sample specified port. */
    public void processPort(Node node, String portName) 
	throws RemoteException 
    {

        try 
        {
            Device device = node.getDevice(portName.getBytes());
            
            if (device instanceof Instrument) 
            {
        	Instrument instrument = (Instrument )device;
        	SensorDataPacket packet = instrument.getLastSample();
        	System.out.println(packet.toString());
		if(_doParse==true){
		    try {
			PacketParser parser = instrument.getParser();
			PacketParser.Field[] fields = 
			    parser.parseFields(packet);
			System.out.println("");
			for (int j = 0; j < fields.length; j++) {
			    if (fields[j] == null) {
				continue;
			    }
			    System.out.println(fields[j].getName() + 
					       ": " +
					       fields[j].getValue() +
					       " " + 
					       fields[j].getUnits());
			}
		    }catch (NotSupportedException e) {
			System.err.println("Parser not implemented: " + e);
		    }catch (ParseException pe) {
			System.err.println("Parsing error:"+pe);
		    }
		}
            }
            else 
            {
        	_logger.error("Device on port " + portName + 
                              " is not an Instrument");
            }
        }
        catch (PortNotFound e) 
        {
            _logger.error("Port " + portName + " not found");
        }
        catch (DeviceNotFound e) 
        {
            _logger.error("Device not found on port " + portName);
        }
        catch (NoDataException e) 
        {
            _logger.error("No data from instrument on port " + portName);
        }
    }
}

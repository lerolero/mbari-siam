// Copyright MBARI 2008
package org.mbari.siam.operations.utils;

import java.util.Vector;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;

import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/**
 *  Monitor one or more Instrument data streams.
 *  Based on GetLastSample
 *  @author Bob Herlien
 */
public class MonitorStreams extends NodeUtility 
{
    static private Logger _log4j = Logger.getLogger(MonitorStreams.class);
    
    protected int _delayMs = 1000;
    protected Vector _instruments = new Vector();
    protected SimpleDateFormat _dateFormatter = new SimpleDateFormat("H:mm:ss");
    protected boolean _keepGoing=false;

    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	MonitorStreams monitor = new MonitorStreams();

	monitor.getNode(args);
	monitor.processArguments(args);

	// Now do application-specific processing
	try {
            monitor.processNode();
	} catch (Exception e) {
	    System.err.println("Caught exception: "+e);
	    e.printStackTrace();
	    return;
	}
    }
    
    
    /** Get node proxy */
    public void getNode (String[] args)
    {
	if (args.length < 1 ) {
	    System.err.println("Invalid usage:");
	    printUsage();
	    System.exit(1);
	}

	_nodeURL = getNodeURL(args[0]);

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = getHostName(_nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    System.err.println("Malformed URL \"" + _nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	}

	// Get the node's proxy
	try {
	    _log4j.debug("Get node proxy from " + _nodeURL);
	    _node = (Node)Naming.lookup(_nodeURL.toString());
	}
	catch (Exception e) {
	    System.err.println("Couldn't get node proxy at " + _nodeURL + ":");
	    System.err.println(e);
	    System.exit(1);
	}
    }


    /** Process command line options. The first command line argument is always the 
     * node server's URL or hostname.
    */
    public void processArguments(String[] args)
    {
	for (int i = 1; i < args.length; i++)
	{
	    if (args[i].equalsIgnoreCase("-delay"))
	    {
		try {
		    i++;
		    double delayTime = Double.parseDouble(args[i]);
		    if (delayTime >= 0.001)
			_delayMs = (int)(delayTime * 1000);
		    else
			_log4j.warn("Specified delay too short:  " + delayTime);
		} catch (NumberFormatException e) {
		    _log4j.warn(args[i] + " is not a number.  Using default delay of 1 second.");
		}
	    }else
	    if (args[i].equalsIgnoreCase("-k"))
	    {
		_keepGoing=true;
	    }
	    else
	    {
		Device device = null;
		boolean gotIt = false;

		try  {
		    device = _node.getDevice(args[i].getBytes());
		    gotIt = true;
		} catch (Exception e) {
		}

		if (!gotIt) {
		    try  {
			device = _node.getDevice(Long.parseLong(args[i]));
			gotIt = true;
		    } catch (Exception e) {
		    }
		}

		if (gotIt)
	        {
		    if (device instanceof Instrument) 
		    {
			try {
			    _instruments.add(new InstrumentParms((Instrument)device));
			} catch (Exception e) {
			    _log4j.warn("Exception in creating InstrumentParm: " + e);
			}
		    }
		    else 
			_log4j.warn(args[i]+ " is not an Instrument");
		}
		else
		    _log4j.warn("Device " + args[i] + " not found.");
	    }
	}

	
	if (_instruments.size() <= 0)
	{
	    _log4j.error("No instruments specified.  Exiting.");
	    System.exit(1);
	}

	System.out.print("Monitoring ");
	Iterator it = _instruments.iterator();
	while (it.hasNext())
	    System.out.print(" " + ((InstrumentParms)it.next())._id);

	System.out.println();
    }


    /** Print usage message. */
    public void printUsage() 
    {
	System.err.println("MonitorStreams nodeURL [-delay secs] [-k] port [port ...]");
	System.err.println("  -k: Keep going on error");
    }


    /** Run forever, displaying data from specified port(s) */
    public void processNode() throws Exception
    {
	Iterator it;
	InstrumentParms iparms=null;
	SensorDataPacket packet;
	long		pktTime;
	byte[]		buf;

	while(true)
	{
	    it = _instruments.iterator();

	    while(it.hasNext())
	    {
		try{
		    iparms = (InstrumentParms)it.next();
		    if (iparms instanceof InstrumentParms)
			{
			    packet = iparms._instrument.getLastSample();
			    pktTime = packet.systemTime();
			    buf = packet.dataBuffer();

			    if (pktTime > iparms._lastDataTime)
				{
				    System.out.println(_dateFormatter.format(new Date(pktTime)) +
						       " " + iparms._name + ": " +
						       new String(buf, 0, buf.length));
				    iparms._lastDataTime = pktTime;
				}
			}
		}catch(Exception e){
		    if(iparms!=null){
			System.err.println("ERROR processing ID "+iparms._id+": "+e.getMessage());
		    }else{
			System.err.println("ERROR (iparms is NULL):"+e.getMessage());
		    }
		    if(_keepGoing==false){
			System.exit(-1);
		    }
		}
	    }

	    StopWatch.delay(_delayMs);
	}
    }

    public void processNode(Node node) throws Exception
    {
	processNode();
    }

    protected class InstrumentParms
    {
	protected long	_id;
	protected String _name;
	protected Instrument _instrument;
	protected long _lastDataTime;

	InstrumentParms(Instrument instrument) throws RemoteException
	{
	    _instrument = instrument;
	    _id = _instrument.getId();
	    _name = new String(_instrument.getCommPortName());
	    _lastDataTime = 0;
	}
    }

}

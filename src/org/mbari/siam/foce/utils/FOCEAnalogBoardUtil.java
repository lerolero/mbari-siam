// Copyright MBARI 2003
package org.mbari.siam.foce.utils;

import org.mbari.siam.foce.deployed.FOCEAnalogBoard;
import org.mbari.siam.core.NodeProperties;

import java.io.IOException;
import java.util.Properties;

import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.MissingPropertyException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;



/** User utility to turn on/off a relay on a FOCEAnalogBoard */
public class FOCEAnalogBoardUtil
{
    static Logger _log4j = Logger.getLogger(FOCEAnalogBoardUtil.class);

    FOCEAnalogBoard _analogBoard;

    public FOCEAnalogBoardUtil(int boardAddress) throws IOException
    {
	_analogBoard = new FOCEAnalogBoard(boardAddress);
    }

    /** Creates board at default address.
     */
    public FOCEAnalogBoardUtil() throws IOException
    {
	_analogBoard = new FOCEAnalogBoard();
    }

    public void setup(int chan, int range, int polarity, int gain)
	throws IOException
    {
	_log4j.debug("setup()");

	_analogBoard.analogSetup(chan, range, polarity, gain);
    }

    public double sample(int chan) throws IOException
    {
	return(_analogBoard.analogSample(chan));
    }

    public double[] scan(int chan, int nchans) throws IOException
    {
	return(_analogBoard.analogScan(chan, nchans));
    }

    public void close() throws IOException
    {
	_analogBoard.close();
    }

    static void usage()
    {
	System.out.println("Usage: analog [-address addr] <channel> [numChannels [range [polarity [gain]]]]");
	System.out.println("    -address is board address.  If none, use default of 0x300.");
	System.out.println("    <channel> - Analog channel");
	System.out.println("    [numChannels] - Number of channels to convert.  Default 1.");
	System.out.println("    [range] - A/D conversion range, 5 or 10 (volts). Default 10");
	System.out.println("    [polarity] - 0 for unipolar, otherwise bipolar.  Default unipolar");
	System.out.println("    [gain] - 1, 2, 4, or 8, for pre-amplifier gain.  Default 2.");
	System.out.println("    Note 5V unipolar is invalid combination.  Use 10V with gain of 2");
    }

    /** Main method is for command-line testing */
    public static void main(String[] args)
    {
	int		chan = 0;
	int		numChans = 1;
	int		address = 0x300;
	int		range = 10;
	int		polarity = 0;
	int		gain = 2;
	int		index = 0;
	boolean		gotAddr = false;

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

	_log4j.info("FOCE A/D Board Utility");

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	/* Look to see if user passed an address */
	for (index = 0; index < args.length; )
	{
	    if (args[index].equalsIgnoreCase("-address"))
	    {
		index++;
		try {
		    address = Integer.parseInt(args[index++]);
		    gotAddr = true;
		} catch (NumberFormatException e) {
		}
	    }
	    else
		break;
	    index++;
	}

	/* Check to make sure we still have channel number */
	if (args.length < index + 1)
	{
	    usage();
	    return;
	}

	_log4j.debug("index = " + index + ", args.length = " + args.length);

	/* Get the channel number */
	try {
	    chan = Integer.parseInt(args[index]);
	    index++;
	} catch (NumberFormatException e) {
	    _log4j.error("Exception parsing channel number: " + e);
	    usage();
	    return;
	}

	/* Get remaining parameters */
	try {
	    if (args.length > index)
		numChans = Integer.parseInt(args[index++]);
	    if (args.length > index)
		range = Integer.parseInt(args[index++]);
	    if (args.length > index)
		polarity = Integer.parseInt(args[index++]);
	    if (args.length > index)
		gain = Integer.parseInt(args[index++]);
	} catch (NumberFormatException e) {
	    _log4j.debug("Exception parsing " + args[index] + ": " + e);
	}

	try {
	    FOCEAnalogBoardUtil util = gotAddr ?
		new FOCEAnalogBoardUtil(address) :
		new FOCEAnalogBoardUtil();

	    util.setup(chan, range, polarity, gain);

	    if (numChans <= 1)
	    {
		System.out.println("Channel " + chan + " is " +
				   util.sample(chan) + " volts");

		System.out.println("Channel " + chan + " is " +
				   util.sample(chan) + " volts");
	    }
	    else
	    {
		double[] result = util.scan(chan, numChans);
		System.out.println("Results for channels " + chan + "-" + 
				   Integer.toString(chan+numChans-1) + ":");
		for (int i = 0; i < numChans; i++)
		    System.out.print(result[i] + " ");
		System.out.println();
	    }

	    util.close();
	}
	catch (Exception e) {
	    System.out.println("Exception in running FOCEAnalogBoardUtil:  " + e);
	    e.printStackTrace();
	}
	
    }

}

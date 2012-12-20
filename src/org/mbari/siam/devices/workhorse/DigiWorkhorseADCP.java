/**
 * @Title RD Instruments Workhorse ADCP instrument driver as deployed on DigiConnect
 * @author Bob Herlien
 * @version 1.0
 * @date 11/28/2007
 *
 * Copyright MBARI 2007
 * @author MBARI
 * @revision $Id: DigiWorkhorseADCP.java,v 1.1 2008/11/04 22:17:58 bobh Exp $
 *
 */

package org.mbari.siam.devices.workhorse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.rmi.RemoteException;
import java.net.URL;
import java.net.URLConnection;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;

import org.apache.log4j.Logger;

/**
 * The <code>DigiWorkhorseADCP</code> class represents the
 * <code>InstrumentServices</code> driver for controlling the RDI Workhorse
 * ADCP as deployed on a DigiConnect virtual serial port.
 * DigiWorkhorseADCP is sub-classed from <CODE>WorkhorseADCP</CODE>,
 * and simply overrides the sendBreak() method.
 */

public class DigiWorkhorseADCP extends WorkhorseADCP
        implements Instrument, Safeable
{

    DigiWorkhorseAttributes _attributes = new DigiWorkhorseAttributes(this);

   // log4j Logger
    static private Logger _log4j = Logger.getLogger(DigiWorkhorseADCP.class);

    /**
     * Allocates a new <code>DigiWorkhorseADCP</code>
     *
     * @throws RemoteException .
     */
    public DigiWorkhorseADCP() throws RemoteException
    {
	super();
    }


    /**
     * Called by the framework to initialize the instrument prior to sampling.
     * Normally this method will use properties determined by the setProperties
     * method.
     *
     *<p>Override base class initializeInstrument, because base class has a bug
     * -- it sends some commands with _toDevice.write() rather than sendCommand(),
     * and doesn't wait for a reply.  This was causing exceptions in the UW deployment.
     *
     * @throws InitializeException
     * @throws Exception
     */
    protected void initializeInstrument() throws InitializeException, Exception
    {
	if (_attributes.authName != null)
	    Authenticator.setDefault(new DigiAuthenticator());

	super.initializeInstrument();
    }


    /**
     * Send a String to a OutputStream, get reply from InputStream
     */
    protected String urlTransaction(URL url, String cmd)
	throws IOException
    {
	int ch;
	StringBuffer replyBuf = new StringBuffer(256);
	String reply = null;

	_log4j.debug("Sending \"" + cmd + "\"");

	URLConnection conn = url.openConnection();

	conn.setDoOutput(true);

	OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

	wr.write(cmd);
	wr.flush();
 
	InputStreamReader in = new InputStreamReader(conn.getInputStream());
	
	while ((ch = in.read()) != -1)
	    replyBuf.append((char)ch);
	    
	reply = replyBuf.toString();

	_log4j.debug("Reply from URL was \"" + reply + "\"");

	wr.close();
	in.close();

	return(reply);
    }


    /**
     * sends a break longer than 350mS required by the Workhorse
     */
    protected void sendBreak()
    {
	_log4j.debug("Sending break via URL " + _attributes.digiURL);

	try
	{
	    URL url = new URL(_attributes.digiURL);

	    urlTransaction(url, _attributes.breakOnString);

	    Thread.sleep(_attributes.breakMs);

	    urlTransaction(url, _attributes.breakOffString);

	} catch (Exception e) {
	    _log4j.error("Exception in sending Break via URL \"" + _attributes.digiURL
			 + "\": " + e);
	    e.printStackTrace();
	}

    }


    /**
     * Attributes for DigiWorkhorseADCP
     * @author bobh
     *
     */
    class DigiWorkhorseAttributes extends WorkhorseADCP.Attributes
    {
	DigiWorkhorseAttributes(DeviceServiceIF service)
	{
	    super(service);
	}

	/** URL to connect to */
	String digiURL = "http://75.160.105.185/UE/rci";

	/** Number of milliseconds to assert BREAK */
	int breakMs = 400;
	
	/** String to assert break */
	String breakOnString = 
	    "<rci_request version=\"1.1\"><set_state><gpio><pin4>unasserted" +
	    "</pin4></gpio></set_state></rci_request>";
	
	/** String to de-assert break */
	String breakOffString = 
	    "<rci_request version=\"1.1\"><set_state><gpio><pin4>asserted" +
	    "</pin4></gpio></set_state></rci_request>";

	/** Authentication username.  null to skip authentication */
	String authName = null;

	/** Authentication password */
	String authPassword = null;

    }

    class DigiAuthenticator extends Authenticator
    {
        // This method is called when a password-protected URL is accessed
        protected PasswordAuthentication getPasswordAuthentication()
	{
            return(new PasswordAuthentication(_attributes.authName, 
					      _attributes.authPassword.toCharArray()));
        }
    }

} /* class DigiWorkhorseADCP */

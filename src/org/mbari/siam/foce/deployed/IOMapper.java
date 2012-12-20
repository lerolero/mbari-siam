// Copyright MBARI 2008
package org.mbari.siam.foce.deployed;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.mbari.siam.utils.SyncProcessRunner;

import org.apache.log4j.Logger;
import org.mbari.siam.core.NodeManager;
import org.mbari.siam.distributed.NodeConfigurator;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;

/** IOMapper opens a socket to a native daemon, written in 'C', that
 *  does port-level I/O on behalf of the Java applications.
 *  @author Bob Herlien
*/
public class IOMapper
{
    static private Logger _log4j = Logger.getLogger(IOMapper.class);

    protected static final String IOPORT_EXEC = "/native/foce/foceio";
    protected static final int EXEC_TMOUT = 3000;
    protected static final int IOPORT_PORT = 7933;

    protected static IOMapper _theIOMapper = null;

    protected OutputStream _out;
    BufferedReader _reader;


    /** Method to get singleton instance. 			*/
    public synchronized static IOMapper getInstance()
	throws IOException, UnknownHostException
    {	
	if (_theIOMapper == null)
	    _theIOMapper = new IOMapper();

	return(_theIOMapper);
    }

    private IOMapper() throws IOException, UnknownHostException
    {
	Socket sock = null;
	boolean gotIt = false;

	/* Try to connect to the ioPort server		*/
	try {
	    sock = new Socket("localhost", IOPORT_PORT);
	    gotIt = true;
	} catch (IOException e) {
	    if (e instanceof UnknownHostException)
		throw e;
	    _log4j.info("Exception in 1st try at connecting to ioport server: " + e);
	}

	/* If didn't connect, try to exec() it		*/
	if (!gotIt) {
	    String siamHome = null;
	    try {
		siamHome = NodeManager.getInstance().getNodeConfigurator().getSiamHome();
	    } catch (MissingPropertyException e) {
		_log4j.warn(e);
		siamHome = "/home/ops/siam";
	    }

	    SyncProcessRunner runner = new SyncProcessRunner();
	    String execCmd = siamHome + IOPORT_EXEC + " -p " + IOPORT_PORT;

	    try {
		runner.exec(execCmd);
		runner.waitFor(EXEC_TMOUT);
	    } catch (IllegalThreadStateException e) {
		throw new IOException("IllegalThreadStateException in trying to exec "
				      + execCmd);
	    } catch (InterruptedException ie) {
		_log4j.error("InterruptedException in SyncProcessRunner.exec(): " + ie);
	    }

	    /* OK, try to connect to the ioPort server one more time */
	    /* Let IOException bubble through if it still doesn't work */
	    sock = new Socket("localhost", IOPORT_PORT);
	}

	_reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	_out = sock.getOutputStream();
    }

    /** Perform one I/O transaction with the I/O server, return String result.
     *  Check to make sure return string starts with "OK".  If so, return remainder
     *  of string.  Else, throw IOException with return string as message.
     */
    public synchronized String transact(String cmd) throws IOException
    {
//	_log4j.debug("IOMapper writing: " + cmd);
	_out.write(cmd.getBytes());
	String rtn = _reader.readLine();
//	_log4j.debug("IOMapper read: " + rtn);

	if (rtn.startsWith("OK")) {
	    if (rtn.length() > 3)
		return(rtn.substring(3));
	    return(rtn);
	}

	throw new IOException("Exception in transact(): " + rtn);
    }

    public void close() throws IOException
    {
	_log4j.info("IOMapper.close()");
	_out.write("bye\n".getBytes());
    }
}

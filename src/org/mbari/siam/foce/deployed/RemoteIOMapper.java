// Copyright MBARI 2010
package org.mbari.siam.foce.deployed;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;


/** RemoteIOMapper opens a socket to a native daemon, written in 'C', that
 *  does port-level I/O on behalf of the Java applications.  The 'Remote' version
 *  can talk to a remote host, but cannot try to exec the foceio daemon as the
 *  local version can.
 *  @author Bob Herlien
*/
public class RemoteIOMapper implements Serializable
{
    protected static final int IOPORT_PORT = 7933;

    protected OutputStream _out;
    BufferedReader _reader;

    public RemoteIOMapper(String hostName) throws IOException, UnknownHostException
    {
	Socket sock = null;

	/* Try to connect to the ioPort server		*/
	sock = new Socket(hostName, IOPORT_PORT);

	_reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	_out = sock.getOutputStream();
    }

    /** Perform one I/O transaction with the I/O server, return String result.
     *  Check to make sure return string starts with "OK".  If so, return remainder
     *  of string.  Else, throw IOException with return string as message.
     */
    public synchronized String transact(String cmd) throws IOException
    {
	_out.write(cmd.getBytes());
	String rtn = _reader.readLine();

	if (rtn.startsWith("OK")) {
	    if (rtn.length() > 3)
		return(rtn.substring(3));
	    return(rtn);
	}

	throw new IOException("Exception in transact(): " + rtn);
    }

    public void close() throws IOException
    {
	_out.write("bye\n".getBytes());
	
    }
}

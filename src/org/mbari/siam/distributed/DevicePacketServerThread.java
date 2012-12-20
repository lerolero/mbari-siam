// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.net.ServerSocket;
import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
 * Base class for thread which accepts DevicePacketStream client connections.
 * 
 * @author Tom O'Reilly
 */
public class DevicePacketServerThread extends Thread {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(DevicePacketServerThread.class);

	ServerSocket _socket;

	/** Vector of DevicePacketOutputStream objects. */
	public Vector _clients = new Vector();

	/**
	 * Construct DevicePacketServerThread; create encapsulated server socket.
	 */
	public DevicePacketServerThread(int port) throws IOException {
		_socket = new ServerSocket(port);
	}

	/**
	 * Wait for and accept new client connection; add new client to list of
	 * clients.
	 */
	public void run() {

		while (true) {

			try {
				DevicePacketOutputStream outputStream = new DevicePacketOutputStream(
						_socket.accept());

				_log4j.info("Accepted DevicePacketStream connection");

				// Add to list of connections
				addStream(outputStream);
			} catch (IOException e) {
				_log4j.error("ServerSocket.accept() failed: "
						+ e.getMessage());
			}
		}
	}

	/**
	 * Add new connection to list of clients.
	 */
	synchronized void addStream(DevicePacketOutputStream stream) {
		_clients.add(stream);
	}
}

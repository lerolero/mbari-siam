// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.net.Socket;
import java.net.InetAddress;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
 * DevicePacketStream is a source of DevicePacket objects.
 * 
 * @author Tom O'Reilly
 */
public class DevicePacketStream implements Serializable {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(DevicePacketStream.class);


	transient ObjectInputStream _inputStream;

	boolean _connected = false;

	InetAddress _serverHost;

	int _serverPort;

	/**
	 * Creates a DevicePacketStream, using specified host and port as the
	 * source.
	 */
	public DevicePacketStream(InetAddress serverHost, int serverPort) {
		_serverHost = serverHost;
		_serverPort = serverPort;
	}

	/** Read next DevicePacket. */
	public DevicePacket read() throws IOException, ClassNotFoundException {

		// Create input stream if doesn't exist yet.
		if (!_connected) {
			_inputStream = createInputStream();
			_log4j.debug("DevicePacket.read() - connected");
			_connected = true;
		}

		// DEBUG DEBUG DEBUG
		// _log4j.debug("#bytes avail: " + _inputStream.available());
		return (DevicePacket) _inputStream.readObject();
	}

	protected ObjectInputStream createInputStream() throws IOException {

		_log4j.debug("Creating input stream...");
		_log4j.debug("Server host: " + _serverHost.getHostAddress());
		_log4j.debug("Server port: " + _serverPort);
		Socket socket = new Socket(_serverHost, _serverPort);
		_log4j.debug("Created socket to server...");
		return new ObjectInputStream(socket.getInputStream());
	}
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * RemoteSerialPort provides a network communication channel to a
 * DeviceService's serial port. RemoteSerialPort is instantiated by a
 * DeviceService, and returned to a client over the network (hence
 * RemoteSerialPort implements java.io.Serializable).
 *  
 */
public class RemoteSerialPort implements Serializable {

	boolean _connected = false;

	int _serverPort;

	InetAddress _serverAddress;

	transient Socket _socket;

	transient BufferedReader _in;

	transient PrintWriter _out;

	/** Create RemoteSerialPort, using specified server host and port. */
	public RemoteSerialPort(InetAddress serverAddress, int serverPort) {
		_serverAddress = serverAddress;
		_serverPort = serverPort;
	}

	/** Prepare RemoteSerialPort for use; must be called on client side. */
	public void connect() throws UnknownHostException, IOException {

		if (_connected)
			return;

		_socket = new Socket(_serverAddress, _serverPort);

		_in = new BufferedReader(
				new InputStreamReader(_socket.getInputStream()));

		_out = new PrintWriter(_socket.getOutputStream(), true);

		_connected = true;
	}

	public void write(char output) throws IOException {

		// Just in case we're not connected yet...
		connect();

		_out.print(output);
		_out.flush();
	}

	public int read() throws IOException {

		// Just in case we're not connected yet...
		connect();

		return _in.read();
	}

	public int getServerPort() {
		return _serverPort;
	}

	public InetAddress getServerInetAddress() {
		return _serverAddress;
	}

}
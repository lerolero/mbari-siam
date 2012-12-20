// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.net.Socket;
import java.net.InetAddress;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * DevicePacketOutputStream serializes DevicePacket objects and writes them to a
 * Socket.
 * 
 * @author Tom O'Reilly
 */
public class DevicePacketOutputStream {

	ObjectOutputStream _outputStream;

	DevicePacketOutputStream(Socket clientSocket) throws IOException {
		_outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
	}

	/** Write DevicePacket. */
	public void write(DevicePacket packet) throws IOException {
		_outputStream.writeObject(packet);
		_outputStream.flush();
	}

	/** Close the stream; call only when stream no longer needed. */
	public void close() throws IOException {

		_outputStream.close();
	}
}
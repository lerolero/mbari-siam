// Copyright 2002 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * DevicePacketStreamFactory creates DevicePacketStream objects.
 * DevicePacketStreamFactory is Serializable, and so instances of it can be
 * passed via RMI. We use the factory approach since DevicePacketStream is NOT
 * Serializable, as it encapsulates a Socket which must be created on the client
 * host.
 * 
 * @author Tom O'Reilly
 */
public class DevicePacketStreamFactory implements Serializable {

	int _serverPort;

	InetAddress _serverHost;

	/**
	 * Create a DevicePacketStreamFactory, which will manufacture
	 * DevicePacketStreams using the specified server and port.
	 */
	DevicePacketStreamFactory(InetAddress serverHost, int serverPort) {
		_serverHost = serverHost;
		_serverPort = serverPort;
	}

	/** Create a DevicePacketStream. */
	DevicePacketStream getStream() {
		return new DevicePacketStream(_serverHost, _serverPort);
	}
}


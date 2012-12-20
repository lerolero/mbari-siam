/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

import org.apache.log4j.Logger;
//import org.mbari.siam.operations.utils.NodeUtility;
//import org.mbari.siam.utils.SiamSocketFactory;

/**
 * Intends to circumvent the limitation in NodeUtility.run in terms of allowing
 * multiple execution of the utilities within the same JVM session.
 * 
 * <p>
 * Note that {@link NodeUtility#run()} calls <a href="http://download.oracle.com/javase/1.3/docs/api/java/rmi/server/RMISocketFactory.html#setSocketFactory%28java.rmi.server.RMISocketFactory%29"
 * >RMISocketFactory.setSocketFactory</a>, however,
 * "The RMI socket factory can only be set once," so, it is not suited to
 * support multiple execution of the utilities. The behavior is that only the
 * first call to that method will be effective, and subsequent calls will fail,
 * so, for example, a different serverHost is not possible.
 * 
 * <p>
 * The implementation here is adapted from {@link SiamSocketFactory} (the one
 * used in {@link NodeUtility#run()}), but with the following changes:
 * 
 * <ul>
 * <li>The constructor here is no-arg and private.
 * <li>{@link #setUp()} is to be called before any command execution to set a
 * (singleton) instance of this class as the rmi socket factory.
 * <li>The serverHost can be set (any number of times) via the static method
 * {@link #setServerHost(String)}.
 * <li>{@link #remove()} can be called when iSiam is about to exit.
 * </ul>
 * 
 * Note: it is assumed (and this is the case currently) that the call in
 * {@link NodeUtility#run()} to set the rmi registry will simply fail (because
 * we set that first!) but that it will continue with the remaining of the
 * command execution.
 * 
 * @author carueda
 */
public class SiamShellSocketFactory extends RMISocketFactory {

	private static final Logger _log = Logger.getLogger(SiamShellSocketFactory.class);

	/**
	 * To save any preexisting socket factory
	 */
	private static RMISocketFactory _defaultFactory;

	/**
	 * Our singleton instance.
	 */
	private static SiamShellSocketFactory _instance;

	/**
	 * The serverHost, which can be updated before the execution of any of the
	 * NodeUtility commands.
	 */
	private static String _serverHost = null;

	/**
	 * ISiam calls this when setting the JVM environment.
	 * 
	 * @return null if everythong ok, otherwise a message that can be logged.
	 */
	static String setUp() {
		if (_instance == null) {
			_defaultFactory = RMISocketFactory.getSocketFactory();
			_instance = new SiamShellSocketFactory();
			try {
				RMISocketFactory.setSocketFactory(_instance);
			}
			catch (IOException e) {
				return "Error while setting rmi socket factory: "
						+ e.getMessage();
			}
		}
		else {
			return "SiamShellSocketFactory.setUp called again!";
		}
		return null;
	}

	/**
	 * ISiam calls to update the serverHost property that should be used in
	 * subsequent calls to {@link #createSocket(String, int)}.
	 * 
	 * @return null if everythong ok, otherwise a message that can be logged.
	 */
	static void setServerHost(String serverHost) {
		_serverHost = serverHost;
	}

	/**
	 * ISiam calls this when about to exit.
	 * 
	 * @return null if everything ok, otherwise a message that can be logged.
	 */
	static String remove() {
		if (_instance != null) {
			try {
				RMISocketFactory.setSocketFactory(_defaultFactory);
			}
			catch (IOException e) {
				return "Error while setting rmi socket factory to the default: "
						+ e.getMessage();
			}
		}
		else {
			return "SiamShellSocketFactory.setUp has not been run";
		}
		return null;
	}

	private SiamShellSocketFactory() {
		super();
	}

	public ServerSocket createServerSocket(int port) throws IOException {
		if (_log.isDebugEnabled()) {
			_log.debug("SiamShellSocketFactory.createServerSocket(" + port
					+ ") called.  _serverHost=" + _serverHost);
		}

		return new ServerSocket(port);
	}

	public Socket createSocket(String host, int port) throws IOException {
		if (_log.isDebugEnabled()) {
			_log.debug("SiamShellSocketFactory.createSocket(" + host + ", "
					+ port + ") called.  _serverHost=" + _serverHost);
		}

		Socket socket = new Socket(_serverHost, port);

		return socket;
	}
}

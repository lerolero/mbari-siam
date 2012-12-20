/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMIClientSocketFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class SiamClientSocketFactory 
    implements RMIClientSocketFactory, Serializable {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(SiamClientSocketFactory.class);

    private int _hashCode = "SiamClientSocketFactory".hashCode();

    public SiamClientSocketFactory() {
	super();
	try {
	    _log4j.debug("SiamClientSocketFactory: local host = " +
			       InetAddress.getLocalHost());
	}
	catch (UnknownHostException e) {
	    _log4j.error("UnknownHostException: " + e);
	}

    }



    public Socket createSocket(String host, int port)
	throws IOException {

	_log4j.debug("SiamClientSocketFactory.createPort() - host=" + 
			   host + ", port=" + port);

	return new Socket(host, port);
    }


    public boolean equals(Object object) {
	if (object instanceof SiamClientSocketFactory) {
	    return true;
	}

	return false;
    }


    public int hashCode() {
	return _hashCode;
    }
}

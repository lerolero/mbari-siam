/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class SiamServerSocketFactory 
    implements RMIServerSocketFactory, Serializable {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(SiamServerSocketFactory.class);

    private int _hashCode = "SiamServerSocketFactory".hashCode();

    public SiamServerSocketFactory() {
	super();
    }

    public ServerSocket createServerSocket(int port)
	throws IOException {

	_log4j.info("SiamServerSocketFactory.createPort() - port=" + 
			   port);

	return new ServerSocket(port);
    }


    public boolean equals(Object object) {
	if (object instanceof SiamServerSocketFactory) {
	    return true;
	}

	return false;
    }


    public int hashCode() {
	return _hashCode;
    }
}

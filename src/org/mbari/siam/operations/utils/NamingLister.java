/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.rmi.Naming;

/**
List contents of RMI Naming Registry on specified host.
*/
public class NamingLister {

    public static void main(String[] args) {

	if (args.length != 1) {
	    System.err.println("usage: host");
	    return;
	}

	String host = args[0];

	String[] services = null;
	try {
	    services = Naming.list(host);
	}
	catch (Exception e) {
	    System.err.println("Exception from Naming.list(): " + e);
	    return;
	}

	for (int i = 0; i < services.length; i++) {
	    System.out.println(services[i]);
	}
    }

}

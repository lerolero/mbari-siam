/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmarks.client;

import java.rmi.Naming;

public class RmiList {


    public static void main(String[] args) {
	String url = args[0];


	try {
	    String[] services = Naming.list(url);

	    System.out.println("Found " + services.length + " bound objects");
	    for (int i = 0; i < services.length; i++) {
		System.out.println(services[i]);
	    }
	}
	catch (Exception e) {
	    System.err.println("Caught Exception: " + e);
	}
    }

}



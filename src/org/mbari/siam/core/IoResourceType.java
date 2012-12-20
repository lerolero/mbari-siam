/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;


public class IoResourceType {
    private String _name;
    private IoResourceType(String aString) {
	_name = aString;
    }
    public static final IoResourceType DPA_BOARD = new IoResourceType("DPA_CHANNEL");
    public static final IoResourceType INSTRUMENT = new IoResourceType("INSTRUMENT");
}

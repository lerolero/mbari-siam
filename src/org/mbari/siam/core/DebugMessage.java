/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class DebugMessage {
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(DebugMessage.class);

    private static long _debugBits = 0;

    public static void setLevel(long level)
    {
        _debugBits = level;
    }
    
    public static long getLevel()
    {
        return _debugBits;
    }

    public static void println(String s) {
	if (_debugBits > 0)
            System.out.println(s);
    }

    public static void println(Exception e) {
	if (_debugBits > 0)
	    System.out.println(e);
    }

    public static void print(String s){
	if (_debugBits > 0)
	    System.out.print(s);
    }

    public static void print(Exception e){
	if (_debugBits > 0)
    	    System.out.print(e);
    }

}// end class DebugMessage

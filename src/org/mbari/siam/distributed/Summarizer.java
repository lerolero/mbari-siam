/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
   This interface should be implemented by instrument services that can
   produce summary packets.
 */
public interface Summarizer {

    public void turnOn();

    public void turnOff();

    public boolean on();

}

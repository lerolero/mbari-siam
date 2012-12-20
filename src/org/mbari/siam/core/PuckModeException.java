// Copyright MBARI 2002
package org.mbari.siam.core;


/** PuckModeException indicates that the PuckSerialInstrumentPort
was not in puck mode */
public class PuckModeException extends Exception 
{
    public PuckModeException() {super();}
    public PuckModeException(String s) {super(s);}
}

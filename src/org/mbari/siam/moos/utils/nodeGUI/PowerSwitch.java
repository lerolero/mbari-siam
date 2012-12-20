/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import javax.swing.JButton;


public class PowerSwitch extends JButton {

    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int ERROR = -1;

    /** Construct the switch */
    public PowerSwitch(String name) {
	super(name);
    }
}

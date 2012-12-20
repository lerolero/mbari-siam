/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;


import java.awt.Dimension;
import javax.swing.JLabel;


/**
   Label to be used with ComponentManager.
 */
public class UnmanagedLabel extends JLabel {
    ComponentManager _componentManager;
    int _x;
    int _y;
    Dimension _latestSize = new Dimension(1, 1);

    public UnmanagedLabel(String name, int x, int y,
			  Dimension fullImageSize) {
	super(name);
	_componentManager = new ComponentManager(x, y, fullImageSize);
	_latestSize = fullImageSize;
    }

    /** Layout position */
    public void layout(Dimension imageSize) {
	_componentManager.layout(this, imageSize);
	_latestSize = imageSize;
    }

    public void setSizedText(String text) {
	super.setText(text);
	_componentManager.layout(this, _latestSize);
    }
}

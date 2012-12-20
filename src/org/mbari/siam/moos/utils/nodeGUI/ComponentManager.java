/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import java.awt.Component;
import java.awt.Dimension;

/**
   ComponentManager manages the position and size of a Component in the absence
   of a LayoutManager.
 */
public class ComponentManager {

    double _x;
    double _y;

    /**
       Constructor. Parameters specify coordinates and canvas size 
       in full-sized reference frame. 
     */
    public ComponentManager(int x, int y, Dimension size) {
	_x = ((double )x) / size.width;
	_y = ((double )y) / size.height;
    }

    
    /**
       Layout specified component in panel of specified size
     */
    void layout(Component component, Dimension panelSize) {

	Dimension componentSize = component.getPreferredSize();

	component.setBounds((int )(_x * panelSize.width),
			    (int )(_y * panelSize.height),
			    componentSize.width, componentSize.height);
    }
}


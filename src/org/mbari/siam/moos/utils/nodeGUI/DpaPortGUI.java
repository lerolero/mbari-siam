/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import java.io.IOException;
import java.rmi.server.RMISocketFactory;
import java.awt.Dimension;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.utils.ClientSocketFactory;
import org.mbari.siam.operations.utils.NodeUtility;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;

class DpaPortGUI extends JPanel {

    static private Logger _logger = Logger.getLogger(DpaPortGUI.class);

    public static final Color PWR_ON_COLOR = Color.green;
    public static final Color OVERCURRENT_COLOR = Color.red;

    String _portName;
    DataValue _voltage;
    DataValue _current;
    JLabel _overCurrent;
    JLabel _power;
    // JLabel _serviceMnem;

    //    JLabel _common;
    
    ComponentManager _componentManager;
    
    DpaPortGUI(String portName, int x, int y, Dimension fullImageSize) {

	_componentManager = new ComponentManager(x, y, fullImageSize);

	_portName = portName;
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	_voltage = new DataValue("volts", portName + " voltage", true);
	_voltage.displayName(true);
	add(_voltage);
	_current = new DataValue("curr", portName + " current", true);
	_current.displayName(true);
	add(_current);

	_power = new JLabel("power");
	_power.setToolTipText("Instrument power");
	add(_power);

	//	_common = new JLabel("common");
	//	_common.setToolTipText("Comms connected to ground");
	//	add(_common);
	_overCurrent = new JLabel("overCurr");
	_overCurrent.setToolTipText("Overcurrent status");
	add(_overCurrent);
	setVisible(false);
    }

    /** Update displayed values. */
    public void update(long timestamp, DpaPortStatus status) {
	_logger.debug("update()");
	_voltage.update(timestamp, new Float(status.getVoltage()));
	_current.update(timestamp, new Float(status.getCurrentMA()));

	if (status.instrumentPowerOn()) {
	    _power.setText("PWR ON");
	    _power.setBackground(PWR_ON_COLOR);
	    _power.setOpaque(true);
	}
	else {
	    _power.setText("PWR OFF");
	    _power.setBackground(Color.black);
	    _power.setOpaque(false);
	}

	if (status.overcurrentTripped()) {
	    _overCurrent.setText("OVERCURR");
	    _overCurrent.setBackground(OVERCURRENT_COLOR);
	    _overCurrent.setOpaque(true);
	}
	else {
	    _overCurrent.setText("OverCurr OK");
	    _overCurrent.setOpaque(false);
	}
	setVisible(true);
    }

    /** Layout position. */
    public void layout(Dimension imageSize) {
	_componentManager.layout(this, imageSize);
    }

}

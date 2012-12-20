/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import java.util.Date;
import java.text.DecimalFormat;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Frame;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JLabel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.chart.ChartFrame;
import org.apache.log4j.Logger;

/**
 * DataValue displays a numeric data value in a label. A time-series chart of 
 * sensor data values can be displayed by right-mouse-click on the data value.
 * @author oreilly
 *
 */
class DataValue
    extends JLabel {

    static protected Logger _logger = Logger.getLogger(DataValue.class);
    public static final Color OK_DATA_COLOR = Color.blue;
    public static final Color STALE_DATA_COLOR = Color.black;
    public static final Color NO_DATA_COLOR = Color.red;

    /** Name of this sensor. */
    String _name;

    String _mnemonic;

    boolean _displayMnemonic = false;
    boolean _displayName = false;

    double _xPos = 0.;
    double _yPos = 0;
    long _nSamples = 0;
    JFreeChart _chart = null;
    ChartFrame _chartFrame;
    TimeSeries _timeSeries = null;
    MouseListener _mouseListener = new MouseListener();
    WindowListener _windowListener = new WindowListener();
    ComponentManager _componentManager = null;

    float _dataValue;  // Current value of data

    DecimalFormat _dataFormatter = null;

    /** Create data value. */
    public DataValue(String name, String mnemonic) {

	super("No Data");

	initialize(name, mnemonic, false);
    }

    /** Create data value, use internal component manager. */
    public DataValue(String name, String mnemonic, int x, int y, 
		     Dimension canvasSize) {

	this(name, mnemonic, false);
	_componentManager = new ComponentManager(x, y, canvasSize);
    }


    /** Create data value. */
    DataValue(String name, String mnemonic, boolean displayName) {

	super("No Data");

	initialize(name, mnemonic, displayName);
    }


    /** Set the output format of the value. */
    public void setFormat(String format) {
	if (_dataFormatter == null) {
	    _dataFormatter = new DecimalFormat();
	}
	_dataFormatter.applyPattern(format);
    }

    protected void initialize(String name, String mnemonic, 
			      boolean displayName) {

	displayName(displayName);
	_mnemonic = mnemonic;
	_name = name;
	setOpaque(true);

	setBackground(NO_DATA_COLOR);
	setForeground(Color.white);
	if (_displayName) {
	    setText(_name + ": NO DATA");
	}
	else {
	    setText("NO DATA");
	}
	setToolTipText(name + ": " + mnemonic);
	addMouseListener(_mouseListener);
    }

    /** Turn display of mnemonic on/off. */
    public void displayName(boolean display) {
	_displayName = display;
    }

    /** Update displayed value. */
    void update(long timeStampMsec, Number value) {
	_nSamples++;

	String valueString = null;
	if (_dataFormatter == null) {
	    valueString = value.toString();
	}
	else {
	    // Format the output 
	    valueString = _dataFormatter.format(value.doubleValue());
	}


	if (_displayName) {
	    setText(_name + ": " + valueString);
	}
	else {
	    setText(valueString);
	}
	setBackground(OK_DATA_COLOR);
	Rectangle r = getBounds();
	Dimension size = getPreferredSize();
	setBounds(r.x, r.y, size.width, size.height);
	if (_timeSeries != null) {
	    try {
		_logger.debug("update() - add to time series...");
		_timeSeries.add(new Second(new Date(timeStampMsec)), 
				value);
		_logger.debug("update() - done adding to time series.");
	    }
	    catch (Throwable e) {
		_logger.error("Caught exception when adding to time series",
			      e);
	    }
	}
    }


    /** Set 'stale data' or 'no data' indication. */
    public void setNotFound() {
	if (_nSamples > 0) 
	    setBackground(STALE_DATA_COLOR);
	else
	    setBackground(NO_DATA_COLOR);
    }


    /** Return sensor name. */
    public String getName() {
	return _name;
    }


    /** Layout position */
    public void layout(Dimension imageSize) {
	if (_componentManager == null) {
	    _logger.error("layout() - No component manager!");
	    return;
	}
	_logger.debug("Layout this data value  - " + getText());
	_componentManager.layout(this, imageSize);
    }


    /** Create and display the strip chart for this sensor. */
    void createChart() {
	_logger.debug("Create chart");

	_timeSeries = new TimeSeries(_name, Second.class);

	_timeSeries.setHistoryCount(24 * 3600);

	_chart = 
	    ChartFactory.createTimeSeriesChart(_mnemonic + " (" + _name + ")",
					       "time", _mnemonic,
					       new TimeSeriesCollection(_timeSeries),
					       false, false, false);

	// Try to overcome "bug" in JDK1.4...
	_chart.setAntiAlias(false);
	ChartPanel panel = new ChartPanel(_chart, false);
	panel.setPreferredSize(new Dimension(500, 270));
	panel.setMouseZoomable(true, false);
	_chartFrame = new ChartFrame(_mnemonic, _chart);
	_chartFrame.addWindowListener(_windowListener);
	_chartFrame.setContentPane(panel);
	_chartFrame.pack();
	_chartFrame.setVisible(true);
    }



    /** Listen for relevant mouse events. */
    class MouseListener extends MouseAdapter {

	public void mouseClicked(MouseEvent event) {
	    if ((event.getModifiers() & InputEvent.BUTTON3_MASK) ==
		InputEvent.BUTTON3_MASK) {
		if (_chartFrame != null) {
		    // Bring existing chart to top
		    _logger.debug("Bring chart to front");
		    if (_chartFrame.getState() == Frame.ICONIFIED) {
			_chartFrame.setState(Frame.NORMAL);
		    }
		    _chartFrame.toFront();
		}
		else {
		    _logger.debug("Create new chart");
		    createChart();
		}
	    }
	}
    }


    /** Listen for strip chart window closing. */
    class WindowListener extends WindowAdapter {
	public void windowClosed(WindowEvent e) {
	    _logger.debug("Window closed");
	    _chartFrame = null;
	}
    }

}


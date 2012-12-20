/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;
import java.util.Date;
import java.text.DateFormat;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.BoxLayout;
import org.apache.log4j.Logger;

/** CommunicationPanel displays items related to the shore-to-node 
    communication link. */
public class CommunicationPanel 
    extends JPanel implements ActionListener {

    static protected Logger _logger = 
	Logger.getLogger(CommunicationPanel.class);

    protected JComboBox _sampleIntervals;
    protected JLabel _sampleIndicator;
    protected JCheckBox _stayConnected;
    protected InstrumentSampler _sampler = null;
    protected int _sampleIntervalMsec;
    protected Application _application;

    public CommunicationPanel(Application application) {

	_application = application;

	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	JPanel subPanel = new JPanel();
	subPanel.setLayout(new GridLayout(1, 0));

	JPanel subPanel2 = new JPanel();
	JLabel label = new JLabel("Sample interval:");
	subPanel2.add(label);
	String[] intervals = {"10 sec", "30 sec", "60 sec", "120 sec"};
	_sampleIntervals = new JComboBox(intervals);
	_sampleIntervals.addActionListener(this);
	_sampleIntervals.setSelectedIndex(0);
	subPanel2.add(_sampleIntervals);
	subPanel.add(subPanel2);

	subPanel2 = new JPanel();
	_sampleIndicator = 
	    new JLabel("Acquired 0 samples                        ");

	subPanel2.add(_sampleIndicator);
	subPanel.add(subPanel2);
	add(subPanel);

	subPanel = new JPanel();
	subPanel.setLayout(new GridLayout(1, 0));

	_stayConnected = 
	    new JCheckBox("Stay connected (portal disabled while checked)");

	subPanel.add(_stayConnected);

	subPanel2 = new JPanel();
	label = new JLabel("Data legend:  ");
	subPanel2.add(label);
	label = new JLabel("New data");
	label.setOpaque(true);
	label.setForeground(Color.white);
	label.setBackground(DataValue.OK_DATA_COLOR);
	label.setToolTipText("Data received on latest sample attempt");
	subPanel2.add(label);

	label = new JLabel("Old data");
	label.setOpaque(true);
	label.setForeground(Color.white);
	label.setBackground(DataValue.STALE_DATA_COLOR);
	label.setToolTipText("No new data received on latest sample attempt");
	subPanel2.add(label);

	label = new JLabel("No Data");
	label.setOpaque(true);
	label.setForeground(Color.white);
	label.setBackground(DataValue.NO_DATA_COLOR);
	label.setToolTipText("Data never received");
	subPanel2.add(label);

	subPanel.add(subPanel2);

	add(subPanel);
    }	


    /** Indicate whether "stay connected" has been selected. */
    public boolean stayConnected() {
	return _stayConnected.isSelected();
    }

    /** Return sampling interval in millisec. */
    public int sampleIntervalMsec() {
	return _sampleIntervalMsec;
    }


    /** Set "now sampling" indicator. */
    public void setSamplingIndicator() {
	_sampleIndicator.setText("Sampling node...");
	_sampleIndicator.setOpaque(true);
	_sampleIndicator.setForeground(Color.white);
	_sampleIndicator.setBackground(Color.green);
    }


    /** Set indicator to show successful acquisition. */
    public void setSampledIndicator(long nSamples, long lastSampledTime) {
	String text = 
	    "Acquired " + nSamples + " samples (Last sampled at " +
	    DateFormat.getDateTimeInstance().format(new Date(lastSampledTime))
	    + ")";

	_sampleIndicator.setText(text);
	_sampleIndicator.setOpaque(false);
	_sampleIndicator.setForeground(Color.black);
    }

    /** Indicate unsuccesful acquisition attempt. */
    public void setErrorIndicator(long nSamples, long lastSampledTime) {

	String text;
	if (nSamples > 0) {
	    text = 
		"Error while sampling! Last sampled at " +
		DateFormat.getDateTimeInstance().format(new Date(lastSampledTime))
	    + ")";
	}
	else {
	    text = "Error while sampling! (0 samples)";
	}

	_sampleIndicator.setText(text);

	_sampleIndicator.setOpaque(true);
	_sampleIndicator.setForeground(Color.white);
	_sampleIndicator.setBackground(Color.red);
    }


    /** Respond to user actions. */
    public void actionPerformed(ActionEvent event) {
	Object object = event.getSource();
	if (object == _sampleIntervals) {
	    String string = (String )_sampleIntervals.getSelectedItem();
	    String subString = string.substring(0, string.indexOf(" "));
	    _logger.info("Selected interval: " + subString);
	    // Terminate existing sampler and start a new one
	    try {
		_sampleIntervalMsec = Integer.parseInt(subString) * 1000;
		_application.startSampling(_sampleIntervalMsec);
	    }
	    catch (Exception e) {
		_logger.error("actionPerformed(): ", e);
	    }
	}
    }
}

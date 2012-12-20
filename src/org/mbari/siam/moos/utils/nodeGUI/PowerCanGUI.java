/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;
import java.net.InetAddress;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BoxLayout;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.chart.ChartFrame;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.devices.serialadc.SerialAdcPacketParser;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.devices.Power;
import org.mbari.siam.operations.utils.NodeUtility;
import org.mbari.siam.utils.ClientSocketFactory;

public class PowerCanGUI extends JPanel 
    implements Application {

    static protected Logger _logger = Logger.getLogger(PowerCanGUI.class);

    Image _fullImage;
    Dimension _fullImageDimension;
    DrawingPanel _drawingPanel;
    MediaTracker _mediaTracker;
    float _imageAspect = 0.f;
    Sensor[] _sensors;
    long _lastSampledTime = 0;
    HiVoltageSwitch _hiVoltage;
    Power _powerService = null;
    Node _nodeService = null;
    long _nSamples = 0;
    SerialAdcPacketParser _packetParser = new SerialAdcPacketParser();
    byte[] _leaseNote = "UNKNOWN_HOST".getBytes();
    InstrumentSampler _sampler = null;
    CommunicationPanel _communicationPanel = null;

    /** Lease ID for communication link to node. */
    int _leaseID = -1;

    public PowerCanGUI(Node node, String imagefileName) throws Exception {

	File imagefile = new File(imagefileName);
	if (!imagefile.exists()) {
	    throw new Exception("image file " + imagefileName + " not found");
	}

	_nodeService = node;
	_powerService = getPowerService(_nodeService);

	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	_communicationPanel = new CommunicationPanel(this);
	add(_communicationPanel);

	_mediaTracker = new MediaTracker(this);
	_fullImage = Toolkit.getDefaultToolkit().createImage(imagefileName);
	_mediaTracker.addImage(_fullImage, 0);
	_mediaTracker.waitForID(0);
	_fullImageDimension = new Dimension(_fullImage.getWidth(this),
					    _fullImage.getHeight(this));

	_imageAspect = _fullImageDimension.width / _fullImageDimension.height;
	
	System.out.println("image width: " + _fullImageDimension.width +
			   ", image height: " + _fullImageDimension.height);

	_drawingPanel = new DrawingPanel(_fullImage);
	_drawingPanel.setLayout(null);
	createSensors();

	_hiVoltage = new HiVoltageSwitch(1800, 934, _fullImageDimension);
	_drawingPanel.add(_hiVoltage);

	// Scale image display to size of display screen. 
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	screenSize.width -= 50;
	scaleImage(screenSize);

	add(_drawingPanel);

	try {
	    InetAddress address = InetAddress.getLocalHost();
	    String hostname = address.getHostName();
	    _leaseNote = ("PowerCanGUI@" + hostname).getBytes();
	}
	catch (Exception e) {
	    _logger.error("Got exception while getting local host: " + e);
	}


    }


    /** Scale image to input panel size, and set it on the drawing panel. */
    void scaleImage(Dimension panelSize) {
	if (panelSize.width <= 0 || panelSize.height <= 0) {
	    System.err.println("scaleImage() - *** Invalid dimensions!!!");
	    return;
	}

	float panelAspect = panelSize.width / panelSize.height;
	int width = 0;
	int height = 0;
	if (panelAspect > _imageAspect) {
	    width = -1;
	    height = panelSize.height;
	}
	else {
	    width = panelSize.width;
	    height = -1;
	}

        Image image = 
	    _fullImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

	try {
	    _mediaTracker.addImage(image, 0);
	    System.out.println("Wait for scaled image to load...");
	    _mediaTracker.waitForID(0);
	    System.out.println("Scaled image loaded.");
	}
	catch (InterruptedException e) {
	    System.err.println("mediaTracker interrupted");
	}

	_drawingPanel.setImage(image);

	Dimension imageSize = 
	    new Dimension(image.getWidth(this), image.getHeight(this));

	_drawingPanel.setPreferredSize(imageSize);

	for (int i = 0; i < _sensors.length; i++) {
	    _sensors[i].layout(imageSize);
	}

	_hiVoltage.layout(imageSize);
    }


    /** Create and position the sensor DataValues */
    void createSensors() {
		
	Dimension imageSize = _fullImageDimension;
		
	_sensors = new Sensor[43];

	// Create sensors 
	_sensors[0] = new Sensor("ADC00 avg", "N solar curr", 20, 237,
				 imageSize);

	_sensors[1] = new Sensor("ADC01 avg", "E solar curr", 199, 236,
				 imageSize);

	_sensors[2] = new Sensor("ADC02 avg", "N solpar volt", 20, 350,
				 imageSize);

	_sensors[3] = new Sensor("ADC03 avg", "E solar volt", 199, 350,
				 imageSize);

	_sensors[4] = new Sensor("ADC04 avg", "N batt curr", 20, 711,
				 imageSize);

	_sensors[5] = new Sensor("ADC05 avg", "E batt curr", 221, 711,
				 imageSize);
		

	_sensors[6] = new Sensor("ADC14 avg", "S batt curr", 423, 711,
				 imageSize);

	_sensors[7] = new Sensor("ADC15 avg", "W batt curr", 621, 711,
				 imageSize);

	_sensors[8] = new Sensor("ADC10 avg", "S solar curr", 421, 236,
				 imageSize);

	_sensors[9] = new Sensor("ADC11 avg", "W solar curr", 600, 236,
				 imageSize);

	_sensors[10] = new Sensor("ADC12 avg", "S solar volt", 421, 350,
				 imageSize);

	_sensors[11] = new Sensor("ADC13 avg", "W solar volt", 600, 350,
				 imageSize);

	_sensors[12] = new Sensor("ADC20 avg", "Wind curr", 788, 420,
				 imageSize);

	_sensors[13] = new Sensor("ADC25 avg", "Aux input curr", 969, 420,
				 imageSize);

	_sensors[14] = new Sensor("ADC22 avg", "Batt bus volt", 1160, 385,
				 imageSize);

	_sensors[15] = new Sensor("ADC27 avg", "Batt bus volt", 1255, 385,
				 imageSize);

	_sensors[16] = new Sensor("ADC24 avg", "Div reg N temp", 918, 734,
				 imageSize);

	_sensors[17] = new Sensor("ADC23 avg", "Loadsink N temp", 927, 1082,
				 imageSize);

	_sensors[18] = new Sensor("ADC29 avg", "Div reg S temp", 1287, 734,
				 imageSize);

	_sensors[19] = new Sensor("ADC28 avg", "Loadsink S temp", 1287, 1082,
				 imageSize);

	_sensors[20] = new Sensor("GFZ00 avg", "400V bus GFZ ", 1937, 777,
				 imageSize);

	_sensors[21] = new Sensor("GFL00 avg", "400V bus GFL ", 1937, 800,
				 imageSize);

	_sensors[22] = new Sensor("GFH00 avg", "400V bus GFH ", 1937, 823,
				 imageSize);

	_sensors[23] = new Sensor("GFT00 avg", "400V bus GFT ", 1937, 846,
				 imageSize);


	_sensors[24] = new Sensor("GFZ01 avg", "48V bus GFZ ", 1389, 314, 
				 imageSize);

	_sensors[25] = new Sensor("GFL01 avg", "48V bus GFL ", 1389, 337,
				 imageSize);

	_sensors[26] = new Sensor("GFH01 avg", "48V bus GFH ", 1389, 360,
				 imageSize);

	_sensors[27] = new Sensor("GFT01 avg", "48V bus GFT ", 1389, 383,
				 imageSize);

	_sensors[28] = new Sensor("ADC30 avg", "48-400V #0 curr", 1559, 691,
				 imageSize);

	_sensors[29] = new Sensor("ADC31 avg", "48-400V #1 curr", 1559, 889,
				 imageSize);

	_sensors[30] = new Sensor("ADC32 avg", "48-400V volt", 1801, 796,
				 imageSize);

	_sensors[31] = new Sensor("ADC35 avg", "48-12V #0 curr", 1559, 249,
				 imageSize);

	_sensors[32] = new Sensor("ADC36 avg", "48-12V #1 curr", 1559, 416, 
				 imageSize);

	_sensors[33] = new Sensor("ADC37 avg", "48-12V volt", 1825, 349,
				 imageSize);

	_sensors[34] = new Sensor("ADC21 avg", "Loadsink N curr", 918, 585,
				 imageSize);

	_sensors[35] = new Sensor("ADC26 avg", "Loadsink S curr", 1287, 585,
				 imageSize);

	_sensors[36] = new Sensor("ADC40 avg", "Bridle load cell", 1477, 1014,
				 imageSize);

	_sensors[37] = new Sensor("ADC41 avg", "Anchor load cell", 1477, 1074,
				 imageSize);

	_sensors[38] = new Sensor("ADC42 avg", "Spare", 1477, 1129,
				 imageSize);

	_sensors[39] = new Sensor("ADC43 avg", "Spare", 1477, 1181,
				 imageSize);

	_sensors[40] = new Sensor("PRES avg", "Spare", 1902, 1001,
				 imageSize);

	_sensors[41] = new Sensor("TMP1 avg", "Spare", 1902, 1065,
				 imageSize);

	_sensors[42] = new Sensor("HUMI avg", "Spare", 1902, 1139,
				 imageSize);


	// Add sensors to the diagram
	for (int i = 0; i < _sensors.length; i++) {
	    _drawingPanel.add(_sensors[i]);
	}

    }


    /** Set "sample starting" indicator. */
    public void sampleStartCallback() {
	_communicationPanel.setSamplingIndicator();
    }

    /** Set "sample ended" indicator. */
    public void sampleEndCallback() {
	_nSamples++;
	_lastSampledTime = System.currentTimeMillis();

	_communicationPanel.setSampledIndicator(_nSamples, _lastSampledTime);
    }


    /** Set "sample error" indicator. */
    public void sampleErrorCallback(Exception e) {
	_communicationPanel.setErrorIndicator(_nSamples, _lastSampledTime);
	_logger.error(e);
    }

	
    /** Process sample that was read from power can service. */
    public synchronized void processSample(DevicePacket packet) {
	_logger.debug("processSample()");

	try {
	    PacketParser.Field fields[] = _packetParser.parseFields(packet);

	    for (int i = 0; i < _sensors.length; i++) {
		boolean found = false;
		for (int j = 0; j < fields.length; j++) {
		    if (_sensors[i].getName().equals(fields[j].getName())) {
			Number value = (Number )fields[j].getValue();
			_sensors[i].update(packet.systemTime(), value);
			found = true;
			break;
		    }
		}

		if (!found) {
		    _logger.debug(_sensors[i].getName() + 
				  ": sample not found");

		    _sensors[i].setNotFound();
		}
	    }
	}
	catch (Exception e) {
	    _logger.error("Exception in processSample(): ", e);
	}

	if (_communicationPanel.stayConnected()) {
	    // Try to establish/renew lease if user wants to stay connected
	    _logger.debug("Try to establish/renew lease");

	    //	    int leasePeriod = 4 * _sampleIntervalMsec;
	    int leasePeriod = 4 * _communicationPanel.sampleIntervalMsec();

	    boolean tryEstablish = false;
	    if (_leaseID == -1) {
		tryEstablish = true;
	    }
	    else {
		// Try to renew existing lease
		try {
		    _nodeService.renewLease(_leaseID, leasePeriod);
		}
		catch (LeaseRefused e) {
		    // Lease must have expired; try to get a new one
		    tryEstablish = true;
		}
		catch (Exception e) {
		    _logger.info("Exception trying to renew lease: ", e);
		}
	    }
	
	    if (tryEstablish) {
		try {
		    _leaseID = _nodeService.establishLease(leasePeriod,
							   _leaseNote);
		}
		catch (Throwable e) {
		    _logger.info("Exception trying to establish lease: ", e);
		}
	    }
	    _logger.debug("Done with lease management.");
	}
	else {
	    _logger.debug("Don't worry about maintaining lease.");
	}
    }


    /** Return node's Power service proxy. */
    Power getPowerService(Node nodeService) 
	throws Exception, DeviceNotFound {
	Device devices[] = nodeService.getDevices();
	for (int i = 0; i < devices.length; i++) {
	    if (devices[i] instanceof Power) {
		return (Power )devices[i];
	    }
	}
	throw new DeviceNotFound("Power service not found on node");
    }


    /** Start sampling at specified interval. */
    public void startSampling(int millisec) {
	if (_sampler != null)
	    _sampler.terminate();

	_sampler = 
	    new InstrumentSampler(_powerService, millisec, this);

	Thread thread = new Thread(_sampler);
	_logger.info("Start sampler thread");
	thread.start();
    }



    static public void main(String[] args) {

	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	if (args.length != 2) {
	    System.err.println("usage: imageFile nodeName");
	    return;
	}

	String imageName = args[0];
	String nodeName = args[1];

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new SecurityManager());
	}

	PowerCanGUI gui = null;

	try {
	    // Set socket factory, for shorter timeouts
	    RMISocketFactory.setSocketFactory(new ClientSocketFactory(10000,
								      10000));
	}
	catch (IOException e) {
	    _logger.error("Exception while trying to get socket factory: ", e);
	    return;
	}

	Node node = null;
	String nodeURL = NodeUtility.getNodeURL(nodeName);

	// Try to get node proxy
	while (node == null) {
	    try {
		_logger.info("Try to get node proxy");
		node = (Node )Naming.lookup(nodeURL);
		break;
	    }
	    catch (Exception e) {
		_logger.info("Failed to get node proxy: trying again...");
		try {
		    Thread.sleep(3000);
		}
		catch (Exception e2) {
		}
	    }
	}


	try {
	    _logger.info("Create GUI");
	    gui = new PowerCanGUI(node, imageName);
	}
	catch (Exception e) {
	    System.err.println(e);
	    return;
	}
	
	JFrame frame = new JFrame(nodeName + ": power subsystem");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().add(gui);

	frame.pack();
	frame.setVisible(true);
    }
}


/** Sensor is a DataValue that manages its own position. */
class Sensor extends DataValue {

    ComponentManager _componentManager;

    Sensor(String name, String mnemonic, int x, int y, 
	   Dimension fullImageSize) {
	super(name, mnemonic);

	_componentManager = new ComponentManager(x, y, fullImageSize);
    }


    /** Layout position */
    public void layout(Dimension imageSize) {
	_componentManager.layout(this, imageSize);
    }

}


/** Display and control state of power can's high voltage switch. */
class HiVoltageSwitch extends PowerSwitch {
    ComponentManager _componentManager;

    HiVoltageSwitch(int x, int y, Dimension fullPanelSize) {
	super("Hi voltage");
	_componentManager = new ComponentManager(x, y, fullPanelSize);
    }

    void layout(Dimension panelSize) {
	_componentManager.layout(this, panelSize);
    }
}

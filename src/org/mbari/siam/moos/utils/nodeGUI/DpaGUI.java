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
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.devices.Environmental;
import org.mbari.siam.operations.utils.NodeUtility;
import org.mbari.siam.utils.ClientSocketFactory;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;
import org.mbari.siam.devices.msp430.MSP430PacketParser;

public class DpaGUI extends JPanel
    implements Application {

    static protected Logger _logger = Logger.getLogger(DpaGUI.class);

    Image _fullImage;
    Dimension _fullImageDimension;
    DrawingPanel _drawingPanel;
    MediaTracker _mediaTracker;
    float _imageAspect = 0.f;
    long _lastSampledTime = 0;
    MOOSNode _nodeService = null;
    SerialAdcPacketParser _packetParser = new SerialAdcPacketParser();
    byte[] _leaseNote = "UNKNOWN_HOST".getBytes();
    InstrumentSampler _sampler = null;
    ComponentManager _componentManager = null;
    CommunicationPanel _communicationPanel = null;
    DpaPortGUI[] _dpaPorts;
    DataValue[] _dpaTemprts;
    UnmanagedLabel[] _services;
    Environmental _environmental = null;
    int _nSamples = 0;
    MSP430PacketParser _msp430Parser = new MSP430PacketParser();
    DataValue _gfLow;
    DataValue _gfHigh;
    long _lastTimestamp = 0;

    /** Lease ID for communication link to node. */
    int _leaseID = -1;

    public DpaGUI(MOOSNode node, String imagefileName) 
	throws Exception {

	File imagefile = new File(imagefileName);
	if (!imagefile.exists()) {
	    throw new Exception("image file " + imagefileName + " not found");
	}

	_nodeService = node;

	try {
	    _environmental = getEnvironmentalService(node);
	}
	catch (DeviceNotFound e) {
	    _logger.info("MSP430 service not found on node");
	}

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


	final int nDpaPorts = 12;

	_dpaPorts = new DpaPortGUI[nDpaPorts];

	int x = 35;
	int y = 24;
	int xOffset = 526;
	int yOffset = 90;

	for (int i = 0; i < nDpaPorts; i += 2) {
	    _dpaPorts[i] = 
		new DpaPortGUI("port-" + i, x, y, _fullImageDimension);

	    _drawingPanel.add(_dpaPorts[i]);

	    _dpaPorts[i+1] = 
		new DpaPortGUI("port-" + (i+1), x + xOffset, y, 
			       _fullImageDimension);

	    _drawingPanel.add(_dpaPorts[i+1]);

	    y += yOffset;
	}

	_services = new UnmanagedLabel[nDpaPorts];
	x = 156;
	y = 60;
	xOffset = 288;
	yOffset = 88;
	for (int i = 0; i < nDpaPorts; i += 2) {
	    _services[i] = 
		new UnmanagedLabel("NO SRVC", x, y, _fullImageDimension);

	    _drawingPanel.add(_services[i]);

	    _services[i+1] = 
		new UnmanagedLabel("NO SRVC", x + xOffset, y, 
			       _fullImageDimension);

	    _drawingPanel.add(_services[i+1]);

	    y += yOffset;
	}

	int nDpaBoards = nDpaPorts / 2;
	_dpaTemprts = new DataValue[nDpaBoards];
	x = 312;
	y = 58;
	yOffset = 88;
	for (int i = 0; i < nDpaBoards; i++) {
	    _logger.debug("Create dpaTemprts #" + i);
	    _dpaTemprts[i] = 
		new DataValue("tmprt", "DPA " + i + " Temperature",
			      x, y, _fullImageDimension);

	    _dpaTemprts[i].displayName(true);
	    _dpaTemprts[i].setFormat("#.##");

	    // Don't show until we update it. 
	    _dpaTemprts[i].setVisible(false);
	    _drawingPanel.add(_dpaTemprts[i]);
	    y += yOffset;
	}


	// Scale image display to size of display screen. 
	// Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	// screenSize.width -= 50;
	// scaleImage(screenSize);
	scaleImage(_fullImageDimension);

	JPanel subPanel = new JPanel();

	subPanel.add(_drawingPanel);

	JPanel subPanel2 = new JPanel();

	subPanel2.setLayout(new BoxLayout(subPanel2, BoxLayout.Y_AXIS));
	JPanel subPanel3 = new JPanel();
	subPanel3.add(new JLabel("GF-LOW"));
	_gfLow = new DataValue("gfLow", "MSP430 Groundfault-Low");
	subPanel3.add(_gfLow);
	subPanel2.add(subPanel3);
	subPanel3 = new JPanel();
	subPanel3.add(new JLabel("GF-HIGH"));
	_gfHigh = new DataValue("gfHigh", "MSP430 Groundfault-High");
	subPanel3.add(_gfHigh);
	subPanel2.add(subPanel3);

	subPanel.add(subPanel2);
	add(subPanel);
		     
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

	for (int i = 0; i < _dpaPorts.length; i++) {
	    _dpaPorts[i].layout(imageSize);
	    _services[i].layout(imageSize);
	}

	for (int i = 0; i < _dpaTemprts.length; i++) {
	    _dpaTemprts[i].layout(imageSize);
	    _logger.debug("layout dpaTemprt #" + i);
	}
    }


    /** Process the input packet. */
    public void processSample(DevicePacket packet) {

	_lastTimestamp = packet.systemTime();

	_logger.debug("processSample(): " + packet.toString());

	try {
	    PacketParser.Field[] fields = _msp430Parser.parseFields(packet);

	    // Update Groundfault-low display
	    PacketParser.Field field = 
		PacketParser.getField(fields, 
				      MSP430PacketParser.GRND_FAULT_LO_MNEM);

	    _gfLow.update(packet.systemTime(), (Number )field.getValue());

	    // Update Groundfault-high display
	    field = 
		PacketParser.getField(fields, 
				      MSP430PacketParser.GRND_FAULT_HI_MNEM);

	    _gfHigh.update(packet.systemTime(), (Number )field.getValue());
	}
	catch (Exception e) {
	    _logger.error("processSample(): ", e);
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

    /** Sampling about to start. */
    public void sampleStartCallback() {
	_logger.debug("startSampleCallback()");
	_communicationPanel.setSamplingIndicator();

	if (_environmental == null) {
	    // Try to find environmental service
	    try {
		_environmental = getEnvironmentalService(_nodeService);
		_sampler.addInstrument(_environmental);
	    }
	    catch (DeviceNotFound e) {
		_logger.info("MSP430 service not found on node");
	    }
	    catch (Exception e) {
		_logger.error(e);
	    }
	}
    }


    /** Finished sampling instruments - now get some port information. */
    public void sampleEndCallback() {
	_logger.debug("sampleEndCallback()");

	long timestamp;

	if (_lastTimestamp > 0) {
	    timestamp = _lastTimestamp;
	}
	else {
	    timestamp = System.currentTimeMillis();
	}

	// Get Dpa port information from node
	try {
	    DpaPortStatus[] dpaStatus = _nodeService.getDpaPortStatus();

	    _logger.debug("Got status from " + dpaStatus.length + " ports");
	    for (int i = 0; i < dpaStatus.length; i++) {
		_logger.debug("sampleEndCallback() - status #" + i);
		_logger.debug("temp:" + dpaStatus[i].getTemperature());
	    }

	    for (int i = 0; i < dpaStatus.length; i++) {
		int index = dpaStatus[i].getPortNumber();
		_dpaPorts[index].update(timestamp, dpaStatus[i]);

		_services[index].setSizedText(dpaStatus[i].getServiceMnemonic());
		if ((index % 2) == 0) {
		    // Update DPA board temperature
		    _dpaTemprts[index/2].update(timestamp,
						new Float(dpaStatus[i].getTemperature()));

		    _dpaTemprts[index/2].setVisible(true);
		}
	    }
	    _nSamples++;

	    _communicationPanel.setSampledIndicator(_nSamples, 
						    timestamp);
	}
	catch (IOException e) {
	    _logger.error("sampleEndCallback()", e);
	}
    }


    /** Error when sampling */
    public void sampleErrorCallback(Exception e) {
	_logger.debug("sampleErrorCallback()");
	_communicationPanel.setErrorIndicator(_nSamples, _lastSampledTime);
	_logger.error(e);
    }

    /** Start sampling at specified interval */
    public void startSampling(int millisec) {
	_logger.debug("startSampling()");

	if (_sampler != null)
	    _sampler.terminate();

	_sampler = 
	    new InstrumentSampler(_environmental, millisec, this);

	Thread thread = new Thread(_sampler);
	_logger.info("Start sampler thread");
	thread.start();

    }


    /** Return node's Power service proxy. */
    Environmental getEnvironmentalService(MOOSNode nodeService) 
	throws Exception, DeviceNotFound {
	Device devices[] = nodeService.getDevices();
	for (int i = 0; i < devices.length; i++) {
	    if (devices[i] instanceof Environmental) {
		return (Environmental )devices[i];
	    }
	}
	throw new DeviceNotFound("Environmental service not found on node");
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

	DpaGUI gui = null;

	try {
	    // Set socket factory, for shorter timeouts
	    RMISocketFactory.setSocketFactory(new ClientSocketFactory(10000,
								      10000));
	}
	catch (IOException e) {
	    _logger.error("Exception while trying to get socket factory: ", e);
	    return;
	}

	MOOSNode node = null;
	String nodeURL = NodeUtility.getNodeURL(nodeName);

	// Try to get node proxy
	while (node == null) {
	    try {
		_logger.info("Try to get node proxy");
		node = (MOOSNode )Naming.lookup(nodeURL);
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
	    gui = new DpaGUI(node, imageName);
	}
	catch (Exception e) {
	    _logger.error("Got exception from DpaGUI constructor", e);
	    return;
	}
	
	JFrame frame = new JFrame(nodeName + ": power subsystem");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().add(gui);

	frame.pack();
	frame.setVisible(true);
    }
}


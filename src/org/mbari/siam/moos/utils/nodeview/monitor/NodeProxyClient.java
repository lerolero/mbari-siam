/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.URL;
import java.rmi.*;

import java.util.*;
import javax.swing.*;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.JButton;

import java.lang.Math;

import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.operations.utils.ListNodePorts;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;


/**
   @author Kent Headley
   This is a prototype app/applet that polls a given node and
   displays its status. It also allows the user to scan, shutdown
   suspend or resume one or more ports on a given node.

*/
/*
   To Do:
   - refactor to remove cut and paste code and do general cleanup
   - stop that annoying flicker
   - do not display unoccupied ports
   - include a message box that displays command output
   - do conversions to allow to run as an applet
   - add operations (show schedule, DPA, ...)
*/ 

// This applet tag is here so that appletviewer can load this source file directly
/*
<applet code="moos.operations.utils.nodeview.monitor.NodeProxyClient" width=425 height=525 codebase=../../../../../../classes>
<param name=server
       value="krill">
<param name=source
       value=node>
<param name=period
       value=3000>
<param name=data
       value=status>
<param name=title
       value="Port Status">
<param name=node
       value="sidearm5">
</applet>

*/

/**************************************************************************
**
**    This is a simple applet that periodically displays node status
**
*************************************************************************/

public class NodeProxyClient extends JApplet implements Runnable,ActionListener {
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(NodeProxyClient.class);

    static final int MAX_PORTS=16;

    JTabbedPane graphPane=new JTabbedPane(JTabbedPane.TOP);
    JPanel graphPanel = new JPanel(new BorderLayout());
    JPanel configPanel = new JPanel(new BorderLayout());

    JPanel statusPanel=new JPanel(new GridLayout(0,4));
    JPanel newStatusPanel=new JPanel(new GridLayout(0,4));
    JPanel controlPanel;
    JPanel titlePanel = new JPanel(new GridLayout(0,1));
    JComboBox nodeComboBox;
    JComboBox statusNodeComboBox;
    JList portList;
    JScrollPane portListPane;
    JButton scanButton;
    JButton shutdownButton;
    JButton suspendButton;
    JButton resumeButton;
    JLabel title;
    JLabel lastSample;
    JLabel statusPortLabels[][]=new JLabel[MAX_PORTS][4];
    JPanel headerPanel = new JPanel(new GridLayout(0,4));
    JLabel headerPortName;
    JLabel headerService;
    JLabel headerID;
    JLabel headerStatus;
    String nodeNames[]=new String[14];
    String portNames[]=new String[MAX_PORTS];
    Font titleFont=new Font("TimesRoman",Font.PLAIN|Font.BOLD,20);
    Font updateFont=new Font("TimesRoman",Font.PLAIN|Font.BOLD,15);
    Font headerFont=new Font("TimesRoman",Font.PLAIN|Font.BOLD,14);
    Font nodeFont=new Font("TimesRoman",Font.PLAIN|Font.ITALIC,14);
    Font statusFont=new Font("TimesRoman",Font.PLAIN,16);

    Port ports[]=new Port[MAX_PORTS];
    String portStatusStrings[][]=new String[MAX_PORTS][4];

    String nodeStatus=null;
    Thread runner = null;

    // a NodeProxy object
    NodeProxy sampler = null;

    int count = 0;
    String _args[]=null;

    /* milliseconds between updates */
    int period      = 5000;
    String _periodString=null;
    String _server = null;
    String _node = null;
    String _dataItem = null;
    String _source="PORTAL";
    boolean _isApplet=true;
    String _title=null;

    public void initParams(){
	// Get the passed parameters
	
	if(_isApplet)
	    _log4j.debug("I am an applet");
	else{
	    _log4j.debug("I am NOT an applet my args are:");
	    for(int j=0;j<_args.length;j++)
		System.out.print( _args[j]+" ");
	    System.out.println("");
	}
	
	if(_isApplet){
	    // These come from the <parameter= value=> tags in the applet
	    _server=getParameter("SERVER");
	    _source=getParameter("SOURCE");
	    _dataItem = getParameter("DATA");
	    _periodString=getParameter("PERIOD");
	    _title = getParameter("TITLE");
	    _node=getParameter("NODE");
	}else
	    for(int i=0;i<_args.length;i++){
		// These are passed in from the command line (non-applet)
		if(_args[i].trim().toUpperCase().indexOf("S=")>=0)
		    _server=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("SRC=")>=0)
		    _source=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("DATA=")>=0)
		    _dataItem=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("P=")>=0)
		    _periodString=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("TI=")>=0)
		    _title=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("NODE=")>=0)
		    _node=_args[i].substring(_args[i].indexOf("=")+1);
	    }

	_log4j.debug("PERIOD: "+_periodString+"\nSERVER: "+_server+"\nTITLE:"+_title+"\nP:"+_periodString+"\nNODE:"+_node+"\nDATA:"+_dataItem+"\nSRC:"+_source);

	// Validate and convert non-string parameters
        try {
	    period   = Integer.parseInt(_periodString);
	}
        catch (Exception e) {
	    _log4j.error("Frequency parameter not an Integer!");
	}

        try {
	    if(!_source.toUpperCase().equals("NODE"))
		if(!_source.toUpperCase().equals("PORTAL"))
		    if(!_source.toUpperCase().equals("TEST")){
		    _log4j.error("Invalid SRC: "+_source);
		    System.exit(0);    
		}
	}
        catch (Exception e) {
	    _log4j.error("Frequency parameter not an Integer!");
	}
    }


    /*
    **    Initialize the applet. The Parameters passed are the title of the plot
    **    the marker file to use and the update period in milliseconds
    */
    public void init() {
        int i;
        int j;

	initParams();
	Container contentPane=getContentPane();

	for(i=0;i<nodeNames.length;i++){
	    nodeNames[i]=new String("sidearm"+(i+1));
	}

	//Create the Title
        title = new JLabel(_title, JLabel.CENTER);
        title.setFont(titleFont);
        lastSample = new JLabel("Last Update: ",JLabel.CENTER);
	lastSample.setFont(updateFont);
        JLabel nodeLabel = new JLabel(("Node: "),JLabel.CENTER);
	nodeLabel.setFont(nodeFont);

	Box statusBox = Box.createVerticalBox();
        Box nodeSelectBox = Box.createHorizontalBox();
	statusNodeComboBox=new JComboBox();
	statusNodeComboBox.setEditable(false);

	for(i=0;i<nodeNames.length;i++){
	    statusNodeComboBox.addItem(nodeNames[i]);
	}

	if(_node!=null)
	    statusNodeComboBox.setSelectedItem(_node);


	title.setMaximumSize(title.getPreferredSize());

	nodeLabel.setMaximumSize(nodeLabel.getPreferredSize());
	statusNodeComboBox.setMaximumSize(statusNodeComboBox.getPreferredSize());
	nodeSelectBox.add(nodeLabel);
	nodeSelectBox.add(statusNodeComboBox);
	statusBox.add(nodeSelectBox);

	titlePanel.add(title);
	titlePanel.add(lastSample);
	//titlePanel.add(statusNodeComboBox);
	titlePanel.add(statusBox);

        // make container for node status info
	//statusPanel=new JPanel(new GridLayout(0,4));

	// make header labels
	headerPortName=new JLabel("Port Name",JLabel.CENTER);
	headerService=new JLabel("Service",JLabel.CENTER);
	headerID=new JLabel("ISI-ID",JLabel.CENTER);
	headerStatus=new JLabel("Status",JLabel.CENTER);

	headerPortName.setFont(headerFont);
	headerService.setFont(headerFont);
	headerID.setFont(headerFont);
	headerStatus.setFont(headerFont);


	headerPortName.setMaximumSize(headerPortName.getPreferredSize());
	headerService.setMaximumSize(headerService.getPreferredSize());
	headerID.setMaximumSize(headerID.getPreferredSize());
	headerStatus.setMaximumSize(headerStatus.getPreferredSize());

	headerPanel.add(headerPortName);
	headerPanel.add(headerService);
	headerPanel.add(headerID);
	headerPanel.add(headerStatus);

	titlePanel.add(headerPanel);

	// Do node operations stuff
	JPanel operationsPanel = new JPanel(new GridLayout(0,1));
	Box operationsBox=Box.createVerticalBox();
	Box comboBox = Box.createHorizontalBox();
	Box buttonBox=Box.createHorizontalBox();
	Box buttonBox2=Box.createHorizontalBox();

	JLabel nodeComboBoxLabel=new JLabel("Node:",JLabel.CENTER);
	nodeComboBox=new JComboBox();
	nodeComboBox.setEditable(false);

	for(i=0;i<nodeNames.length;i++){
	    nodeComboBox.addItem(nodeNames[i]);
	}
	if(_node!=null)
	    nodeComboBox.setSelectedItem(_node);


	for(i=0;i<portNames.length;i++)
	    portNames[i]=new String("/dev/ttySX"+(i));

	JLabel portListLabel=new JLabel("Ports:",JLabel.CENTER);
	portList=new JList(portNames);
        portList.setSelectedIndex(-1);
        //portList.addListSelectionListener(this);
        portList.setVisibleRowCount(5);

	portListPane=new JScrollPane(portList);
	scanButton=new JButton("Scan Selected");
	scanButton.addActionListener(this);
	scanButton.setActionCommand("SCAN");
	shutdownButton=new JButton("Shutdown Selected");
	shutdownButton.addActionListener(this);
	shutdownButton.setActionCommand("SHUTDOWN");
	suspendButton=new JButton("Suspend Selected");
	suspendButton.addActionListener(this);
	suspendButton.setActionCommand("SUSPEND");
	resumeButton=new JButton("Resume Selected");
	resumeButton.addActionListener(this);
	resumeButton.setActionCommand("RESUME");

	scanButton.setMaximumSize(scanButton.getPreferredSize());
	shutdownButton.setMaximumSize(shutdownButton.getPreferredSize());
	suspendButton.setMaximumSize(suspendButton.getPreferredSize());
	resumeButton.setMaximumSize(resumeButton.getPreferredSize());
	nodeComboBox.setMaximumSize(nodeComboBox.getPreferredSize());
	nodeComboBoxLabel.setMaximumSize(nodeComboBoxLabel.getPreferredSize());
	portListPane.setMaximumSize(portListPane.getPreferredSize());

	comboBox.add(nodeComboBoxLabel);
	comboBox.add(nodeComboBox);
        comboBox.add(Box.createHorizontalStrut(10));
	comboBox.add(portListLabel);
	comboBox.add(portListPane);

	buttonBox.add(scanButton);
	buttonBox.add(Box.createHorizontalStrut(25));
	buttonBox.add(shutdownButton);

	buttonBox2.add(suspendButton);
	buttonBox2.add(Box.createHorizontalStrut(25));
	buttonBox2.add(resumeButton);

	operationsBox.add(Box.createVerticalStrut(20));
	operationsBox.add(comboBox);
	operationsBox.add(Box.createVerticalStrut(50));
	operationsBox.add(buttonBox);
	operationsBox.add(buttonBox2);

	operationsPanel.add(operationsBox);


	/*
	Panel buttonPanel = new Panel(new GridLayout(3,3));
	Button _yMaxUp=new Button("+");
	_yMaxUp.addActionListener(this);
	_yMaxUp.setActionCommand("ZOOMIN);
	Button _yMaxDn=new Button("-");
	_yMaxDn.addActionListener(this);
	_yMaxDn.setActionCommand("ZOOMOUT");
	Button _yMinUp=new Button("Up");
	_yMinUp.addActionListener(this);
	_yMinUp.setActionCommand("SCROLLUP");
	Button _yMinDn=new Button("Down");
	_yMinDn.addActionListener(this);
	_yMinDn.setActionCommand("SCROLLDN");
	Button _xWidthDn=new Button("-");
	_xWidthDn.addActionListener(this);
	_xWidthDn.setActionCommand("XWIDTHDN");
	Button _xWidthUp=new Button("+");
	_xWidthUp.addActionListener(this);
	_xWidthUp.setActionCommand("XWIDTHUP");
	
	Label ymaxLabel = new Label("Y Zoom",JLabel.CENTER);
	Label yminLabel = new Label("Y Scroll",JLabel.CENTER);
	Label xwidthLabel = new Label("X Width",JLabel.CENTER);
	ymaxLabel.setFont(new Font("TimesRoman",Font.PLAIN,15));
	yminLabel.setFont(new Font("TimesRoman",Font.PLAIN,15));
	xwidthLabel.setFont(new Font("TimesRoman",Font.PLAIN,15));
	buttonPanel.add(ymaxLabel);
	buttonPanel.add(_yMaxDn);
	buttonPanel.add(_yMaxUp);
	buttonPanel.add(yminLabel);
	buttonPanel.add(_yMinDn);
	buttonPanel.add(_yMinUp);
	buttonPanel.add(xwidthLabel);
	buttonPanel.add(_xWidthDn);
	buttonPanel.add(_xWidthUp);
	*/

        graphPanel.add(titlePanel,BorderLayout.NORTH);
	graphPanel.add(statusPanel,BorderLayout.CENTER);
	//graphPanel.add(buttonPanel,BorderLayout.SOUTH);

	graphPane.addTab("Node Status",graphPanel);
	graphPane.addTab("Operations",operationsPanel);
	graphPane.addTab("Options",configPanel);

	// Add the whole tabbed pane thing to the applet
        contentPane.setLayout( new BorderLayout() );
	contentPane.add(graphPane,BorderLayout.CENTER);

    }

    public void updateStatusPanel(){
	int i=0;
	int j=0;
	int nPorts=0;
	JLabel[][] newStatusPortLabels;

	newStatusPanel=new JPanel(new GridLayout(0,4));

        lastSample.setText("Last Update: "+new Date(System.currentTimeMillis()));
	if(portStatusStrings!=null)
	    _log4j.debug("updateStatusPanel: portStatusStrings.length="+portStatusStrings.length);

	if( (portStatusStrings!=null) && (portStatusStrings.length>0) ){
	    _log4j.debug("updateStatusPanel: PortStatusStrings is VALID...");
	    // make container for node status info

	    newStatusPortLabels=new JLabel[portStatusStrings.length][4]; 

	    for(i=0;i<portStatusStrings.length;i++)
		for( j=0;j<4;j++){
		    _log4j.debug("portStatusStrings["+i+"]["+j+"]="+portStatusStrings[i][j]);
		    newStatusPortLabels[i][j]=new JLabel(portStatusStrings[i][j],JLabel.CENTER);
		    newStatusPortLabels[i][j].setFont(statusFont);
		    newStatusPortLabels[i][j].setMaximumSize(newStatusPortLabels[i][j].getPreferredSize());
		    newStatusPanel.add(newStatusPortLabels[i][j]);
		}
	    nPorts=portStatusStrings.length;
	}else{
	    _log4j.debug("updateStatusPanel: portStatusString is NULL...");
	    // make container for node status info
	    nPorts=1;
	    newStatusPortLabels=new JLabel[nPorts][4]; 
	    
	    for(i=0;i<nPorts;i++)
		for( j=0;j<4;j++){
		    newStatusPortLabels[i][j]=new JLabel(" ",JLabel.CENTER);
		    newStatusPortLabels[i][j].setFont(statusFont);
		    newStatusPortLabels[i][j].setMaximumSize(newStatusPortLabels[i][j].getPreferredSize());
		    newStatusPanel.add(newStatusPortLabels[i][j]);
		}
	}
	_log4j.debug("updateStatusPanel: swapping statusPanel...");
	graphPanel.remove(statusPanel);
	statusPanel = newStatusPanel;
	graphPanel.add(statusPanel,BorderLayout.CENTER);

	// Redraw everything
	redrawJComponent(graphPanel);
	redrawJComponent(statusPanel);
	    for(i=0;i<nPorts;i++)
		for( j=0;j<4;j++){
		    redrawJComponent(newStatusPortLabels[i][j]);
		}
	//newStatusPanel=null;
    }

    public void start() {
	if(runner == null) {
	    runner = new Thread(this);
	    runner.start();
        }
    }

    public void run() {
	int i =0;
	String nodeStatus;
	String samplerURL;
	String nodeURL="rmi://"+_node+"/node";
	if(_isApplet){		 
	    _log4j.debug("Applet Setting URL...");
	    // If no server specified, use same server that
	    // applet was served from (security may force you
	    // to use the same server, applet viewer may allow
	    // you to specify another server)
	    if(_server==null)
		samplerURL = "rmi://"+getCodeBase().getHost()+"/sampler";
	    else
		samplerURL = "rmi://"+_server+"/sampler";
		      
	}else{
	    _log4j.debug("Applet Setting URL & SecMgr...");
	    System.setSecurityManager(new RMISecurityManager());
	    samplerURL = "rmi://"+_server+"/sampler";
	}

	// Get remote object NodeProxy, which can talk
	// to a portal, get and format data, etc.
	if( !_source.toUpperCase().equals("TEST")){
	    try{
		sampler = (NodeProxy)Naming.lookup(samplerURL);
	    }catch(Exception e){
		_log4j.error("NodeProxyClient config error: looking up "+samplerURL+" "+e);
		System.exit(1);
	    }
	
	nodeStatus = "";
	while(true) {

	    try{
		if(_source.toUpperCase().equals("NODE")){
		    nodeURL="//"+nodeNames[statusNodeComboBox.getSelectedIndex()]+"/node";
		    //portStatusStrings=null;
		    portStatusStrings=sampler.getPortStatusStrings(nodeURL);
		    //fillFakeDisplay();
		}else
  	        if(_source.toUpperCase().equals("TEST")){
		    // Some fake data options
		    //data[1] = sampler.pickANumber();
		    //data[1] = Math.sin((2.0*random.nextDouble()-1.0)*50);
		    nodeStatus="test";
		}else{
		 _log4j.error("Bad SRC option: "+_source);
  		 System.exit(0);
		}
	    }catch(Exception e){
		_log4j.error("NodeProxyClient: data error "+e);
	    }
	    _log4j.debug("client: updating graph.....");
	    updateStatusPanel();

	    try {  Thread.sleep(period); }
	    catch(Exception e) { }

	}

    }
    }

    /** This fulfills the actionListener interface 
	for handling GUI controls
     */
    public void actionPerformed(ActionEvent e){
	_log4j.debug("Got a button press: "+e.getActionCommand());

	if(e.getActionCommand().equals("ZOOMIN")){
	}
	if(e.getActionCommand().equals("ZOOMOUT")){
	}
	if(e.getActionCommand().equals("SCROLLUP")){
	}
	if(e.getActionCommand().equals("SCROLLDN")){
	}
	if(e.getActionCommand().equals("XWIDTHDN")){
	}
	if(e.getActionCommand().equals("XWIDTHUP")){
	}
	if(e.getActionCommand().equals("SCAN")){

	    int selectedNode=nodeComboBox.getSelectedIndex();
	    int selectedPorts[]=portList.getSelectedIndices();

	    String scanNode="//"+nodeNames[selectedNode]+"/node";
	    String[] scanPorts=new String[selectedPorts.length];

	    for(int port=0;port<selectedPorts.length;port++){
		scanPorts[port]=portNames[selectedPorts[port]];
	    }
	    _log4j.debug("SCANNING PORTS: "+scanNode+" "+scanPorts);
	    try{
		sampler.doPortOperation(e,scanNode,scanPorts);
	    }catch(RemoteException r1){_log4j.error("RemoteException on SCAN: "+r1);}

	}
	if(e.getActionCommand().equals("SHUTDOWN")){
	    int selectedNode=nodeComboBox.getSelectedIndex();
	    int selectedPorts[]=portList.getSelectedIndices();

	    String scanNode="//"+nodeNames[selectedNode]+"/node";
	    String[] scanPorts=new String[selectedPorts.length];

	    for(int port=0;port<selectedPorts.length;port++){
		scanPorts[port]=portNames[selectedPorts[port]];
	    }
	    _log4j.debug("SHUTDOWN PORTS: "+scanNode+" "+scanPorts);
	    try{
		sampler.doPortOperation(e,scanNode,scanPorts);
	    }catch(RemoteException r2){_log4j.error("RemoteException on SHUTDOWN: "+r2);}

	}
	if(e.getActionCommand().equals("SUSPEND")){

	    int selectedNode=nodeComboBox.getSelectedIndex();
	    int selectedPorts[]=portList.getSelectedIndices();

	    String scanNode="//"+nodeNames[selectedNode]+"/node";
	    String[] scanPorts=new String[selectedPorts.length];

	    for(int port=0;port<selectedPorts.length;port++){
		scanPorts[port]=portNames[selectedPorts[port]];
	    }
	    _log4j.debug("SUSPEND PORTS: "+scanNode+" "+scanPorts);
	    try{
		sampler.doPortOperation(e,scanNode,scanPorts);
	    }catch(RemoteException r3){_log4j.error("RemoteException on SUSPEND: "+r3);}

	}
	if(e.getActionCommand().equals("RESUME")){
	    int selectedNode=nodeComboBox.getSelectedIndex();
	    int selectedPorts[]=portList.getSelectedIndices();

	    String scanNode="//"+nodeNames[selectedNode]+"/node";
	    String[] scanPorts=new String[selectedPorts.length];

	    for(int port=0;port<selectedPorts.length;port++){
		scanPorts[port]=portNames[selectedPorts[port]];
	    }
	    _log4j.debug("RESUME PORTS: "+scanNode+" "+scanPorts);
	    try{
		sampler.doPortOperation(e,scanNode,scanPorts);
	    }catch(RemoteException r4){_log4j.error("RemoteException on RESUME: "+r4);}

	}
    }
    /** Displays fixed fake data ( nice when posing for a screen dump ) */
    protected void fillFakeDisplay(){
	portStatusStrings[0][0]="/dev/ttySX0";
	portStatusStrings[0][1]="";
	portStatusStrings[0][2]="";
	portStatusStrings[0][3]="";
	portStatusStrings[1][0]="/dev/ttySX1";
	portStatusStrings[1][1]="KVH 100C";
	portStatusStrings[1][2]="1694";
	portStatusStrings[1][3]="OK";
	portStatusStrings[2][0]="/dev/ttySX3";
	portStatusStrings[2][1]="Seabird SBE37";
	portStatusStrings[2][2]="1103";
	portStatusStrings[2][3]="OK";
	portStatusStrings[3][0]="/dev/ttySX4";
	portStatusStrings[3][1]="SmartStar ";
	portStatusStrings[3][2]="1066";
	portStatusStrings[3][3]="SAMPLING";
	portStatusStrings[4][0]="/dev/ttySX5";
	portStatusStrings[4][1]="ASIMET PRR";
	portStatusStrings[4][2]="1127";
	portStatusStrings[4][3]="SAMPLING";
	portStatusStrings[5][0]="/dev/ttySX6";
	portStatusStrings[5][1]="Garmin 25HVS";
	portStatusStrings[5][2]="1324";
	portStatusStrings[5][3]="OK";
	portStatusStrings[6][0]="/dev/ttySX7";
	portStatusStrings[6][1]="";
	portStatusStrings[6][2]="";
	portStatusStrings[6][3]="";
	portStatusStrings[7][0]="/dev/ttySX15";
	portStatusStrings[7][1]="Environmental";
	portStatusStrings[7][2]="1201";
	portStatusStrings[7][3]="OK";
    }

    /** Redraw a JComponent (immediately) */
    public static void redrawJComponent(JComponent jc) {
        jc.invalidate();
        jc.validate();
        jc.repaint();
        jc.doLayout();
    }

    public static void main(String[] args) {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);

	NodeProxyClient ss = new NodeProxyClient();
	ss._args=args;
	ss._isApplet=false;
	AppletFrame frame= new AppletFrame(ss);
	frame.setTitle("Node Control");
	frame.setSize(425,525);//width,height
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.show();
    }

}



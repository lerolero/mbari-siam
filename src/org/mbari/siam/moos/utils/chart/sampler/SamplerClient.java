package org.mbari.siam.moos.utils.chart.sampler;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.URL;
import java.rmi.*;

import java.util.*;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JList;
import javax.swing.JScrollPane;

import java.lang.Math;
import org.mbari.siam.moos.utils.chart.graph.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/*
<applet code=sampler.SamplerClient width=400 height=300 codebase=../../classes>
<param name=title
       value="Sidearm5 Temperature (C)">
<param name=markers
       value="http://krill/graph/marker.txt">
<param name=ymin
       value=25>
<param name=ymax
       value=35>
<param name=node
       value=sidearm5>
<param name=port
       value=/dev/ttySX15>
<param name=portal
       value=abalone>
<param name=data
       value=temperature>
<param name=id
       value=1217>
<param name=poll
       value=FALSE>
</applet>

*/

/*
java -cp all.jar -Djava.security.policy=$SIAM_HOME/properties/policy sampler.SamplerClient ti=foo m=marker.txt s=http://localhost/graph/src/sampler p=3000 min=-2 max=2 host=//sidearm5/node port=/dev/ttySX15 data=temperature id=999

*/

/*************************************************************************
**
**    Applet SamplerClient
**                                              Version 1.0   August 1996
**
**************************************************************************
**    Copyright (C) 1996 Leigh Brookshaw
**
**    This program is free software; you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation; either version 2 of the License, or
**    (at your option) any later version.
**
**    This program is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with this program; if not, write to the Free Software
**    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
**************************************************************************
**
**    This is a simple applet that creates a Scroll Chart using the 
**    Graph2D class library and double buffering
**
*************************************************************************/

public class SamplerClient extends Applet implements Runnable,ActionListener {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(SamplerClient.class);

    Graph2D graph;
    Label title;
    Label lastSample;
    DataSet data1 = new DataSet();
    double data[] = new double[2];
    
    Axis    yaxis_right;

    Image    osi = null;
    Graphics osg = null;
    int iwidth  = 0;
    int iheight = 0;

    Thread runner = null;
    Random random = new Random();
    int count = 0;
    String _args[]=null;


    /*
    **    In milliseconds how often do we want to add a new data point.
    */
    int period      = 500;
    String _periodString=null;
    String _yminString=null;
    String _ymaxString=null;
    double ymin=0;
    double ymax=1;
    String _server = null;
    String _node = null;
    String _port = null;
    String _portal = null;
    String _dataItem = null;
    String _devIDString=null;
    String _source="PORTAL";
    long _devID=0;
    /*
    **    Maximum number of points to display before scrolling the data
    */
    int maximum        = 25;
    URL markersURL;
    boolean _isApplet=true;
    String _title=null;
    String _markers=null;

    public void initParams(){
	//Get the passed parameters
	
	if(_isApplet)
	    _log4j.debug("I am an applet");
	else{
	    _log4j.debug("I am NOT an applet my args are:");
	    for(int j=0;j<_args.length;j++)
		_log4j.debug( _args[j]+" ");
	    _log4j.debug("");
	}
	
	if(_isApplet){
	    // These come from the <parameter= value=> tags in the applet
	    _title = getParameter("TITLE");
	    _markers=getParameter("MARKERS");
	    _yminString = getParameter("YMIN");
	    _ymaxString = getParameter("YMAX");
	    _portal = getParameter("PORTAL");
	    _devIDString = getParameter("ID");
	    _dataItem = getParameter("DATA");
	    _node=getParameter("NODE");
	    _periodString=getParameter("PERIOD");
	    _server=getParameter("SERVER");
	    _source=getParameter("SOURCE");
	}else
	    for(int i=0;i<_args.length;i++){
		// These are passed in from the command line (non-applet)
		if(_args[i].trim().toUpperCase().indexOf("P=")>=0)
		    _periodString=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("S=")>=0)
		    _server=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("TI=")>=0)
		    _title=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("M=")>=0)
		    _markers=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("P=")>=0)
		    _periodString=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("MIN=")>=0)
		    _yminString=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("MAX=")>=0)
		    _ymaxString=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("NODE=")>=0)
		    _node=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("PORT=")>=0)
		    _port=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("PORTAL=")>=0)
		    _portal=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("DATA=")>=0)
		    _dataItem=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("ID=")>=0)
		    _devIDString=_args[i].substring(_args[i].indexOf("=")+1);
		if(_args[i].trim().toUpperCase().indexOf("SRC=")>=0)
		    _source=_args[i].substring(_args[i].indexOf("=")+1);
	    }

	_log4j.debug("PERIOD: "+_periodString+"\nSERVER: "+_server+"\nTITLE:"+_title+"\nM:"+_markers+"\nP:"+_periodString+"\nNODE:"+_node+"\nMIN:"+_yminString+"\nMAX:"+_ymaxString+"\nPORT:"+_port+"\nPORTAL:"+_portal+"\nDATA:"+_dataItem+"\nID:"+_devIDString+"\nSRC:"+_source);

	// Validate and convert non-string parameters
        try {
	    period   = Integer.parseInt(_periodString);
	}
        catch (Exception e) {
	    _log4j.error("Frequency parameter not an Integer!");
	}
        try {
	    _devID   = Long.parseLong(_devIDString);
	}
        catch (Exception e) {
	    _log4j.error("DeviceID is not a Long");
	}
        try {
	    ymin   = Double.parseDouble(_yminString);
	}
        catch (Exception e) {
	    _log4j.error("Invalid ymin");
	}
        try {
	    ymax   = Double.parseDouble(_ymaxString);
	}
        catch (Exception e) {
	    _log4j.error("Invalid ymax");
	}
	if(ymin>ymax){
	    double temp=ymin;
	    ymin=ymax;
	    ymax=temp;
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

	JTabbedPane graphPane=new JTabbedPane(JTabbedPane.TOP);
	Panel graphPanel = new Panel(new BorderLayout());
	Panel configPanel = new Panel(new BorderLayout());
	//Create the Graph instance and modify the default behaviour

        graph = new Graph2D();
        graph.zerocolor = new Color(0,255,0);
        graph.borderTop    = 50;
        graph.borderBottom = 50;
        graph.setDataBackground(Color.black);

	//Create the Title
        Panel titlePanel = new Panel(new GridLayout(3,1));
        title = new Label(_title, Label.CENTER);
        title.setFont(new Font("TimesRoman",Font.PLAIN,25));
        lastSample = new Label("Last Sample: ",Label.CENTER);
	lastSample.setFont(new Font("TimesRoman",Font.PLAIN,15));
        Label nodeLabel = new Label(("Node: "+_node),Label.CENTER);
	nodeLabel.setFont(new Font("TimesRoman",Font.PLAIN,15));
	titlePanel.add(title);
	titlePanel.add(lastSample);
	titlePanel.add(nodeLabel);

	Panel buttonPanel = new Panel(new GridLayout(3,3));
	Button _yMaxUp=new Button("+");
	_yMaxUp.addActionListener(this);
	_yMaxUp.setActionCommand("ZOOMIN");
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
	
	Label ymaxLabel = new Label("Y Zoom",Label.CENTER);
	Label yminLabel = new Label("Y Scroll",Label.CENTER);
	Label xwidthLabel = new Label("X Width",Label.CENTER);
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

        graphPanel.add("North",  titlePanel);
        graphPanel.add("Center", graph);
	graphPanel.add("South",buttonPanel);
	graphPane.addTab("Strip Chart",graphPanel);

	//add(configPane);
	Panel foo = new Panel();
	String listContents[]={"one","two","three","four"};
	JList listBox=new JList(listContents);
	listBox.setVisibleRowCount(1);
	JScrollPane scrollPane=new JScrollPane(listBox);
	foo.add(scrollPane);
	configPanel.add("Center",foo);
	graphPane.addTab("Options",configPanel);

	// Add the whole tabbed pane thing to the applet
        setLayout( new BorderLayout() );
	add("Center",graphPane);

	//Load a file containing Marker definitions
	try {
	    markersURL = new URL(_markers);
		
	    _log4j.debug("markers@"+markersURL);
	    graph.setMarkers(new Markers(markersURL));

	} catch(Exception e) {
	    _log4j.error("Failed to create Marker URL: "+e);
	}
 
	// Modify the default Data behaviour
        data1.linecolor   = new Color(255,0,0);
        data1.marker      = 1;
        data1.markercolor = new Color(100,100,255);

	// Setup the Axis. Attach it to the Graph2D instance,
	// and attach the data to it.

        //yaxis_right = graph.createAxis(Axis.RIGHT);
        //yaxis_right.attachDataSet(data1);
        //yaxis_right.setLabelFont(new Font("Helvetica",Font.PLAIN,20));

        yaxis_right = graph.createAxis(Axis.LEFT);
        yaxis_right.attachDataSet(data1);
        yaxis_right.setLabelFont(new Font("Helvetica",Font.PLAIN,20));


        graph.attachDataSet(data1);

	try{
	    updateGraph();
	}catch(Exception e){
	    _log4j.error("Don't worry, be happy");
	}

    }

    public void updateGraph(){

        lastSample.setText("Last Sample: "+data[1]+" "+new Date(System.currentTimeMillis()));
	Graphics g = graph.getGraphics();

	data1.yaxis.maximum =  ymax;
	data1.yaxis.minimum =  ymin;

	if( osi == null || iwidth != graph.getSize().width
	    || iheight != graph.getSize().height  ) {
	    iwidth = graph.getSize().width;
	    iheight = graph.getSize().height;
	    osi = graph.createImage(iwidth,iheight);
	    osg = osi.getGraphics();
	}

	osg.setColor(this.getBackground());
	osg.fillRect(0,0,iwidth,iheight);
	osg.setColor(g.getColor());
	
	osg.clipRect(0,0,iwidth,iheight);

	graph.update(osg);

	g.drawImage(osi,0,0,graph);
	
    }

    public void start() {
	if(runner == null) {
	    runner = new Thread(this);
	    runner.start();
        }
    }

    public void run() {
	int i =0;

	String samplerURL;
	String portalURL="rmi://"+_portal+"/portal/"+_node;
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

	// Get remote object Sampler, which can talk
	// to a portal, get and format data, etc.
	Sampler sampler = null;
	if( !_source.toUpperCase().equals("TEST")){
	    try{
		sampler = (Sampler)Naming.lookup(samplerURL);
		sampler.setApplet(_isApplet);
		sampler.setPortal(portalURL);
		sampler.setNode(nodeURL);
	    }catch(Exception e){
		_log4j.error("SamplerClient config error: looking up "+samplerURL+" "+e);
		System.exit(1);
	    }
	}

	while(true) {

	    count++;

	    if(count >= maximum) data1.delete(0,0);

	    try{
		if(_source.toUpperCase().equals("NODE")){
		    data[1]=sampler.getNodeSample(_devID,_dataItem);
		}else
  	        if(_source.toUpperCase().equals("PORTAL")){
		    data[1]=sampler.getPortalSample(_devID,_dataItem);
		}else
  	        if(_source.toUpperCase().equals("TEST")){
		    // Some fake data options
		    //data[1] = sampler.pickANumber();
		    data[1] = Math.sin((2.0*random.nextDouble()-1.0)*50);
		}else{
		 _log4j.error("Bad SRC option: "+_source);
		System.exit(0);
		}
	    }catch(Exception e){
		_log4j.error("SamplerClient: data error "+e);
		//System.exit(0);
	    }
	    
	    data[0] = count;
	    try {
		data1.append(data,1);
	    }
	    catch (Exception e) {
		_log4j.error("Error appending Data!");
	    }

	    updateGraph();

	    try {  Thread.sleep(period); }
	    catch(Exception e) { }

	}

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

	SamplerClient ss = new SamplerClient();
	ss._args=args;
	ss._isApplet=false;
	AppletFrame frame= new AppletFrame(ss);
	frame.setTitle("Sampler Client");
	frame.setSize(400,300);
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.show();
    }

    /** This fulfills the actionListener interface 
	for handling GUI controls
     */
    public void actionPerformed(ActionEvent e){
	//_log4j.debug("Got a button press: "+e);
	double zinc = ((ymax-ymin)/10.0);
	if(e.getActionCommand().equals("ZOOMIN")){
	    ymax-=zinc;
	    ymin+=zinc;
	}
	if(e.getActionCommand().equals("ZOOMOUT")){
	    ymax*=1.1;
	    ymin*=0.9;
	}
	if(e.getActionCommand().equals("SCROLLUP")){
	    ymax+=zinc;
	    ymin+=zinc;
	}
	if(e.getActionCommand().equals("SCROLLDN")){
	    ymax-=zinc;
	    ymin-=zinc;
	}
	if(e.getActionCommand().equals("XWIDTHDN")){
	    maximum--;
	    data1.delete(0,0);
	}
	if(e.getActionCommand().equals("XWIDTHUP")){
	    maximum++;
	    try{
		data1.append(data,1);
	    }catch(Exception ex){_log4j.error("Doh! "+ex);}
	}
	updateGraph();
    }
}



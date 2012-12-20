/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.awt.*;
import java.applet.*;
import javax.swing.JFrame;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.io.InputStream;

public class AppletFrame extends JFrame
    implements AppletStub,AppletContext{

    public Applet applet =null;
    public AppletFrame(Applet anApplet){
	applet = anApplet;
	Container contentPane = getContentPane();
	contentPane.add(applet);
	applet.setStub(this);
    }
    public void show(){
	applet.init();
	super.show();
	applet.start();
    }

    // AppletStub methods
    public boolean isActive(){return true;}
    public URL getDocumentBase(){return null;}
    public URL getCodeBase(){return null;}
    public String getParameter(String name){return "";}
    public AppletContext getAppletContext(){return this;}
    public void appletResize(int width, int height){}

    // AppletContextMethods
    public AudioClip getAudioClip(URL url){return null;}
    public Image getImage(URL url){return null;}
    public Applet getApplet(String name){return null;}
    public Enumeration getApplets(){return null;}
    public void showDocument(URL url){}
    public void showDocument(URL url, String target){}
    public void showStatus(String status){}
    public void setStream(String s,InputStream i){}
    public InputStream getStream(String s){return null;}
    public Iterator getStreamKeys(){return null;}
}

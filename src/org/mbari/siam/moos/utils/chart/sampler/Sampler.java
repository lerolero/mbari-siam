/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.chart.sampler;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.URL;
import java.net.UnknownHostException;

/** 
    Interface to a Sampler, which acts as a bridge between 
    an applet and a portal. The SampleServer provides a stream
    of formatted records, delimited by an arbitrary specified
    string. The parameters are specified by means of an array
    of strings naming the parameters. The names are well known
    to a parser (provided to the SampleServer by the instrument
    service or portal), which parses data packets and creates
    the returned records.

    @author Kent Headley 
*/
public interface Sampler extends Remote {

    /** test code */
    public double pickANumber()	throws RemoteException;

    public void setNode(String node) throws RemoteException;
    public void setApplet(boolean isApplet) throws RemoteException;
    public void setPort(String port)throws RemoteException;
    public void setPortal(String port)throws RemoteException;
    public void setDeviceID(long id) throws RemoteException;
    public void setDataItem(String dataItem) throws RemoteException;
    public double getNodeSample(long isiID,String dataItem) throws RemoteException;
    public double getPortalSample(long isiID,String dataItem) throws RemoteException;
    public Vector getParsedPacket(long isiID) throws RemoteException;
}




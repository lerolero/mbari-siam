/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.awt.event.ActionEvent;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.URL;
import java.net.UnknownHostException;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
/** 
    Interface to a NodeProxy, which acts as a bridge between 
    an applet and a portal or node.
    @author Kent Headley 
*/
public interface NodeProxy extends Remote {

    /** test code */
    public double pickANumber()	throws RemoteException;

    public double getNodeSample(String nodeURL,long isiID,String dataItem) throws RemoteException;
    public double getPortalSample(String portalURL,long isiID,String dataItem) throws RemoteException;
    public Vector getParsedPacket(String portalURL,long isiID) throws RemoteException;
    public Port[] getPorts(String nodeURL) throws RemoteException;
    public Node getNodeService(String nodeURL) throws RemoteException;
    public String [][] getPortStatusStrings(String NodeURL) throws RemoteException;
    public void doPortOperation(ActionEvent e, String node, String selectedPorts[]) throws RemoteException;

}




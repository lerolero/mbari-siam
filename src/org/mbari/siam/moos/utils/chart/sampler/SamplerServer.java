// Copyright MBARI 2003
package org.mbari.siam.moos.utils.chart.sampler;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;

import java.io.OutputStream;
import java.io.IOException;

import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NoDataException;

public class SamplerServer  {

    public static void main(String[] args) 
	throws RemoteException{

	try{
	    System.out.println("SamplerServer creating implementations...");

	    SamplerImpl s = new SamplerImpl();
	    System.out.println("SamplerServer binding implementations to RMI registry...");
	    Naming.rebind("sampler",s);

	    System.out.println("SamplerServer waiting for customers...");
	}catch(Exception e){
	    System.err.println("SamplerServer error:"+e);
	}
    }

}




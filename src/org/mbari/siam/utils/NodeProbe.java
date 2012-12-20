/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.lang.Runtime;

import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.utils.SyncProcessRunner;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;


import org.mbari.siam.operations.portal.Profiler;

/**
  NodeProbe tests for network connectivity to a specified IP address and
  port number.
 */
public class NodeProbe{

    /** log4j logger */
    static private Logger _log4j = Logger.getLogger(NodeProbe.class);

    /** return status
	true if available (socket attempt successful)
	false otherwise
    */
    public boolean _status=false;

    /**
       ProbeWorker tries to open a socket to the specified host
       on the specified port.
       If successful, it sets the _status variable of the specified
       NodeProbe=true; otherwise, it sets _status=false;
    */
    class ProbeWorker extends Thread{
	InetAddress _addr;
	int _port;
	NodeProbe _parent;

	public ProbeWorker(InetAddress addr,int port, NodeProbe parent){
	    _log4j.debug("Creating new ProbeWorker");
	    _addr=addr;
	    _port=port;
	    _parent=parent;
	    
	    // do this so that the JVM will exit 
	    // when the thread is done
	    this.setDaemon(true);
	    this.start();
	}

	public void run(){
	    try{
		Socket _socket = new Socket(_addr,_port);
		_socket.close();
		_parent._status=true;
		return;
	    }catch(Exception e){
			_log4j.debug(e);
	    }
	    _parent._status=false;
	    return;
	}

      }
    
    /**
       The probe method creates a ProbeWorker to contact the specified host.
       The socket call can take several minutes (~3) to time out, so probe waits 
       until the specified timeout has expired abd then interrupts the thread and exits.
    */
    //public boolean probe(InetAddress addr,long timeoutMillis) throws Exception{}
    public boolean probe(InetAddress addr,int port,long timeoutMillis) throws Exception{

	ProbeWorker p=new ProbeWorker(addr,port,this);

	  long start=System.currentTimeMillis();

	  // wait until the specified timeout expires 
	  // or until the ProbeWorker is successful
	  while(((System.currentTimeMillis()-start)<timeoutMillis)){
	      try{
		  Thread.sleep(500);
	      }catch(InterruptedException e){
		  _log4j.debug("interrupted - exiting loop");
		  break;
	      }
	      if(_status==true || !p.isAlive()){
		  _log4j.debug("got status - exiting loop");
		  break;
	      }
	      
	  }

	  // stop the ProbeWorker thread
	  if(p!=null)
	      p.interrupt();

	  _log4j.debug("host "+(_status?"available":"not available")+" ("+(System.currentTimeMillis()-start)+" ms)");
	      
	  return this._status;
      }


    

    public static void main(String args[]){
	
	/* Stolen from NodeTest.java				*/
	/* Set up a simple configuration that logs on the console.
	   Note that simply using PropertyConfigurator doesn't work
	   unless JavaBeans classes are available on target. 
	   For now, we configure a PropertyConfigurator, using properties
	   passed in from the command line, followed by BasicConfigurator
	   which sets default console appender, etc.
	*/
	    PropertyConfigurator.configure(System.getProperties());
	    PatternLayout layout = 
		new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	    BasicConfigurator.configure(new ConsoleAppender(layout));

	try{

	    // defaults
	    long timeout=5000;
	    String host="localhost";
	    int port=Portals.nodeProbePort();


	    for(int i=0;i<args.length;i++){
		if(args[i].equalsIgnoreCase("--help")){
		    System.err.println("\nUsage: NodeProbe [-h <host>] [-p <port>] [-t <timeoutSeconds>]\n");
		    System.exit(0);
		}
		if(args[i].equals("-h") && (args.length-1)>i){
		    host=args[i+1];
		    i++;
		}else
		if(args[i].equals("-p") && (args.length-1)>i){
		    try{
			port=Integer.parseInt(args[i+1]);
			i++;
		    }catch(Exception e){
			System.err.println("NodeProbe: invalid port -- using default ("+port+")");
		    }
		}else
  	        if(args[i].equals("-t") && (args.length-1)>i){
		    try{
			long t=Long.parseLong(args[1])*1000L;
			if(t>0)
			    timeout=t;
			else
			    System.err.println("NodeProbe: invalid timeout -- using default ("+(timeout/1000L)+" s)");
			i++;
		    }catch(Exception e){
			System.err.println("NodeProbe: invalid timeout -- using default ("+timeout/1000L+")");
		    }
		}
	    }

	    // create an InetAddress
	    InetAddress hostAddr=InetAddress.getByName(host);

	    System.out.println("probing "+host+" on port "+ port+" w/ timeout "+(timeout/1000L)+" s");

	    // Here's how it's used
	    NodeProbe pt=new NodeProbe();
	    boolean ret=pt.probe(hostAddr, port, timeout);

	    System.out.println("ping("+host+","+(timeout/1000L)+" s) returned "+ret);

	}catch(Exception e){
	    e.printStackTrace();
	}
	
    }
}

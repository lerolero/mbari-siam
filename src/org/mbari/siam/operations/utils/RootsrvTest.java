/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RootsrvTest {

    private static final int ROOTSRV_PORT = 7932;

    public static void main(String[] args) 
	throws IOException {

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String	  s;
      InetAddress inetaddr = InetAddress.getLocalHost();
      int port = ROOTSRV_PORT;

      if (args.length > 0) {
	try {
	  inetaddr = InetAddress.getByName(args[0]);
	} catch(UnknownHostException e) {
	  System.err.println("Bad command line value for inet addr" + e);
	}
      }

      if (args.length > 1) {
	try {
	  port = Integer.parseInt(args[1]);
	} catch(NumberFormatException e) {
	  System.err.println("Bad command line value for port number" + e);
	}
      }

      Socket sock = new Socket(inetaddr, port);
      sock.setKeepAlive(false);
      sock.setSoLinger(false, 0);
      sock.setTcpNoDelay(true);
      OutputStream sockOut = sock.getOutputStream();

      while (((s = in.readLine()) != null) && (s.length() != 0))
      {
	sockOut.write(s.getBytes());
	sockOut.write("\n".getBytes());
      }
    }
}


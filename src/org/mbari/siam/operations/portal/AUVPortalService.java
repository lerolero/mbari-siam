/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
AUVPortalService provides a "bridge" between shore/shipboard networks
and low-bandwidth links to AUVs.
@author Tom O'Reilly
 */
public class AUVPortalService {

    protected ServerSocket _serverSocket;

    public AUVPortalService(int port) 
	throws IOException {
	_serverSocket = new ServerSocket(port);
    }


    /** Wait for and process new client connections. */
    public void run() {
	Socket clientSocket = null;

	while (true) {
	    try {
		System.out.println("Wait for client...");
		clientSocket = _serverSocket.accept();
		System.out.println("accepted connection");
		processConnection(clientSocket);
	    }
	    catch (IOException e) {
		System.err.println("IOException: " + e.getMessage());
	    }
	}
    }


    /** Read and parse message from AUV, then retrieve files from AUV
     via ftp. */
    protected void processConnection(Socket clientSocket) 
	throws IOException {
	
	byte buf[] = new byte[1024];

	InputStream in = clientSocket.getInputStream();
	int nBytes = 0;
	// Read message from client
	while (true) {
	    int c = in.read();
	    if (c == -1) {
		System.err.println("processConnection() - end-of-stream");
		break;
	    }
	    buf[nBytes++] = (byte )c;
	}

	String message = new String(buf);

	WorkThread workThread = new WorkThread(message);
	workThread.start();

    }


    /** Responsible for processing contact from AUV. Retrieves data
     from AUV, processes metadata, and notifies SSDS. */
    class WorkThread extends Thread {

	/** Message received from AUV. */
	protected String _message;

	public WorkThread(String message) {
	    _message = message;
	}

	/** Process message received from AUV. */
	public void run() {

	    System.out.println("processConnection() got message:\n" + 
			       _message);

	    // Parse message
	    StringTokenizer tokenizer = new StringTokenizer(_message, ";");
	    if (tokenizer.countTokens() < 4) {
		System.err.println("Not enough tokens in message:\n" + 
				   _message);
		return;
	    }

	    String auvHost = null;
	    String login = null;
	    String passwd = null;
	    String file = null;

	    int nToken = 0;
	    while (tokenizer.hasMoreTokens()) {
		String token = tokenizer.nextToken();
	    
		switch (nToken++) {
		case 0:
		    auvHost = token;
		    break;

		case 1:
		    login = token;
		    break;

		case 2:
		    passwd = token;
		    break;

		default:
		    file = token;
		    try {
			retrieveAndProcess(auvHost, login, passwd, file);
		    }
		    catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		    }
		}
	    }
	}


	/** Retrieve specified file from specified AUV host. */
	void retrieveAndProcess(String auvHost, String login, String passwd,
				String fileName) 
	    throws IOException {

	    // Retrieve file via ftp...
	    System.out.println("Retrieve mission files from AUV host:");
	    System.out.println(auvHost + ": " + fileName);

	    // Retrieve to current directory for now
	    String destination = "./myjunk";

	    try {
		File file = new File(fileName);
		FTPClient ftpClient = new FTPClient(auvHost);
		ftpClient.user(login);
		ftpClient.password(passwd);
		ftpClient.setType(FTPTransferType.BINARY);

		System.out.println("Retrieving " + fileName);
		ftpClient.get(file.getName(), fileName.trim());

		// All done
		System.out.println("All done");
		ftpClient.quit();
	    }
	    catch (IOException e) {
		System.err.println("IOException: " + e.getMessage());
	    }
	    catch (FTPException e) {
		System.err.println("FTPException: " + e.getMessage() + "\n" + 
				   "reply code: " + e.getReplyCode());
	    }
	}
    }


    public static void main(String[] args) {
	try {
	    AUVPortalService service = new AUVPortalService(4444);
	    service.run();
	}
	catch (IOException e) {
	    System.err.println("Caught IOException: " + e.getMessage());
	}
    }

}



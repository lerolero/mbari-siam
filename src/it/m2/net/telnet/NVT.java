package it.m2.net.telnet;

import java.io.*;
import java.net.*;

/**
 * Class to handle a BASIC RFC854 Network Virtual server
 *
 * @author Mario Viara
 * @version $Id: NVT.java,v 1.1 2009/07/15 20:40:51 bobh Exp $
 */
public class NVT extends Telnet
{
	private boolean telnetSupported = false;
	private NVTEventManager rcvdManager	 = new NVTEventManager();
	private Thread thread = null;
	
	public NVT() throws IOException
	{
	}
	
	public NVT(Socket socket) throws IOException
	{
		super(socket);
	}

	public NVT(String name,int port) throws IOException
	{
		this(new Socket(name,port));


	}

	public synchronized boolean isRunning()
	{
		if (thread == null)
			return false;

		return thread.isAlive();
	}
	
	public synchronized void addReceivedListener(NVTListener listener)
	{
		rcvdManager.addListener(listener);

		if (thread == null)
		{
			thread = new Thread(new Runnable()
			{
				public void run()
				{
					byte buffer[] = new byte[1024];
					int count;
					boolean running;
					
					for (;;)
					{
						try
						{
							count = read(buffer);
							if (count > 0)
							{
								NVTEventReceived event = new NVTEventReceived(NVT.this,buffer,count);
								rcvdManager.fireEvent(event);
							}
						}
						catch (Exception ex)
						{
							System.out.println(ex);
						}
					}
				}
				
			});
			
			thread.start();
		}
	}
	
	protected void setOptions() throws IOException
	{
		super.setOptions();
		SampleOptionHandler handler = new SampleOptionHandler();
		
		// Add Local options
		addOption(TRASMIT_BINARY,handler);
		addOption(ECHO,handler);
		addOption(SUPPRESS_GO_AHEAD,handler);

	}

	protected void setDefaults() throws IOException
	{
		super.setOptions();
		setWillOption(SUPPRESS_GO_AHEAD,true);
		setWillOption(TRASMIT_BINARY,true);
		setDoOption(SUPPRESS_GO_AHEAD,true);
		setDoOption(TRASMIT_BINARY,true);
	}
	

}

   
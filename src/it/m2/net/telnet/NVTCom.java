package it.m2.net.telnet;

import java.io.*;
import java.net.*;

/**
 * Network Virtual Telnet Client Base on RFC2217
 *
 * Permit to use a Serial Line connect a one terminal server
 * compliant with RFC2217.
 * 
 * - Tested with sredird 2.1.1
 *
 * @version $Id: NVTCom.java,v 1.2 2009/07/16 22:04:51 headley Exp $
 * @author Mario Viara
 *
 * Modified by Bob Herlien, MBARI, to add capability to specify log file
 * on command line for main()
 */
public class NVTCom extends NVT 
{

	/// Timeout for received one answer after sub option negotiation
	/// request
	private final int SB_TIMEOUT	= 10000;
	
	private boolean sbReceived = false;
	private int sbLengthReceived;
	private byte sbBufferReceived[] = new byte[1024];
	private int modemState;
	
	public static final int FLOW_QUERY		= -1;
	public static final int FLOW_NONE		= 0;
	public static final int FLOW_XONXOFF	= 1;
	public static final int FLOW_CTSRTS		= 2;

	private NVTEventManager modemManager ;
	
	/**
	 * Standard constructor
	 *
	 * @param host RFC2217 host
	 * @param port RFC2217 port
	 */
	public NVTCom(String host,int port) throws IOException
	{
		super();
		
		//setLogLevel(7);
		//addLogger(System.out);
		
		modemManager = new NVTEventManager();
		setSocket(host,port);
	}

	public void addModemChangeListener(NVTListener listener)
	{
		modemManager.addListener(listener);
	}
	
	public void setDefaults() throws IOException
	{
		super.setDefaults();
		setWillOption(COM_PORT_OPTION,true);
		setDoOption(COM_PORT_OPTION,true);

	}
	
	protected void setOptions() throws IOException
	{
		super.setOptions();
		addOption(COM_PORT_OPTION,
				  new SampleOptionHandler()
					   {
						  public void	sb(Telnet telnet,int option,int sbLength,byte sbBuffer[]) throws IOException
						  {
							  NVTCom.this.sb(telnet,option,sbLength,sbBuffer);
						  }
					   });

		
		modemState = 0;
	}

	

	/**
	 * Invoke when telnet sub negotiation is necessary
	 */
	private void		sb(Telnet telnet,int option,int sbLength,byte sbBuffer[]) throws IOException
	{
		
		// Gestione invii asincroni
		if (sbLength == 2)
			if (sbBuffer[0] == (NOTIFY_MODEMSTATE+SERVER_PREFIX))
			{
				modemState = sbBuffer[1] & 0xff;
/*				debug("Modem State"+modemState);

				if ((modemState & MODEM_DCD) != 0)
					debug(" DCD");
				if ((modemState & MODEM_RI) != 0)
					debug(" RI");
				if ((modemState & MODEM_DSR) != 0)
					debug(" DSR");
				if ((modemState & MODEM_CTS) != 0)
					debug(" CTS");

				if ((modemState & MODEM_DDCD) != 0)
					debug(" DDCD");
				if ((modemState & MODEM_DRI) != 0)
					debug(" DRI");
				if ((modemState & MODEM_DDSR) != 0)
					debug(" DDSR");
				if ((modemState & MODEM_DCTS) != 0)
					debug(" DCTS");

				debugln();
*/

				NVTEvent event = new NVTEvent(this);
				modemManager.fireEvent(event);
				return;
			}
			
		this.sbLengthReceived	= sbLength;

		for (int i = 0 ; i < sbLength ; i++)
			sbBufferReceived[i] = sbBuffer[i];

		sbReceived = true;
	}
	

	/**
	 * Wait one response after sending one SB option
	 *
	 * @return true response ok
	 */
	private boolean waitResponse(int code) throws IOException
	{
		long timeout = System.currentTimeMillis() +  SB_TIMEOUT;
		boolean async = isRunning();

		
		
		while (sbReceived == false)
		{
			
			if (async == false)
				negotiate();
			else
				releaseCpu();
			releaseCpu();
			
			if (System.currentTimeMillis() > timeout)
			{
				log(4,"SB Timeout "+code);
				//System.out.println("SB_TIMEOUT "+code);
				//debugln("RFC2217 ** Timeout expired "+SB_TIMEOUT+" ms**");
				return false;
			}

			//int c = read();

		}



		sbReceived = false;
		
		if (sbBufferReceived[0] != (code + SERVER_PREFIX) && code != 0)
		{
			System.out.println("Bad response waiting "+code+" received "+sbBufferReceived[0]);
			//debugln("Bad response waiting "+code+"
			//received "+sbBuffer[0]);
			System.exit(1);
			return false;
		}
		
		return true;
	}

	/**
	 * Return the port signature or null if the option is not received
	 */
	public String getSignature() throws IOException
	{
		sbReceived = false;
		sendSB(COM_PORT_OPTION,SIGNATURE);
		
		if (waitResponse(SIGNATURE) == false)
			return null;
		
		return new String(sbBufferReceived,1,sbLengthReceived-1);
	}

	/**
	 * Return the current baud rate
	 */
	public int getBaud() throws IOException
	{
		return setBaud(0);
		
	}

	/**
	 * Return the current modem state
	 */
	public int getModemState()
	{
		return modemState;
	}
	
	public boolean setModemMask(int mask) throws IOException
	{
		//debugln("set modem mask "+mask);
		sendSB(COM_PORT_OPTION,SET_MODEMSTATE_MASK,mask);

		
		return waitResponse(SET_MODEMSTATE_MASK);
	}
	
	public int setBaud(int baud) throws IOException
	{
		byte buffer[] = new byte[4];
		int i;

		//debugln("Set baud "+baud);
		
		for (i = 0 ; i < 4 ; i ++)
			buffer[i] = (byte)((baud >> (24 - (i * 8))) & 0xff);

		sbReceived = false;
		sendSB(COM_PORT_OPTION,SET_BAUDRATE,buffer);

		if (waitResponse(SET_BAUDRATE) == false)
			return 0;

		baud = 0;
		for (i = 0 ; i < 4 ; i++)
			baud |= ((sbBufferReceived[i+1] & 0xff)<< (24 -(i*8))) ;

		//debugln("get baud "+baud);
		
		return baud;
	}

	public int getDataSize() throws IOException
	{
		return setDataSize(0);
	}
	
	public int setDataSize(int data) throws IOException
	{
		//debugln("Set data size "+data);

		sendSB(COM_PORT_OPTION,SET_DATASIZE,data);

		if (waitResponse(SET_DATASIZE) == false)
			return 0;

		data =  sbBufferReceived[1];

		//debugln("get data size "+data);

		return data;
		
	}

	public int getStopSize() throws IOException
	{
		return setStopSize(0);
	}

	public int setStopSize(int stop) throws IOException
	{
		//debugln("Set stop size "+stop);

		sendSB(COM_PORT_OPTION,SET_STOPSIZE,stop);

		if (waitResponse(SET_STOPSIZE) == false)
			return 0;

		stop =  sbBufferReceived[1];

		//debugln("get stop size "+stop);

		return stop;

	}

	
	public String getParity() throws IOException
	{
		return setParity(null);
	}

	/**
	 * Set parity
	 *
	 * @param s Parity (None,Even,Odd,Mark,Space) onlu the first
	 * char is checked and it is case insensitive.
	 */
	public String setParity(String s) throws IOException
	{
		int code = 0;
		
		if (s != null)
			if (s.length() > 0)
			{
				switch (s.charAt(0))
				{
					case	'n':
					case	'N':
							code = PARITY_NONE;
							break;
					case	'o':
					case	'O':
							code = PARITY_ODD;
							break;
					case	'e':
					case	'E':
							code = PARITY_EVEN;
							break;
					case	's':
					case	'S':
							code = PARITY_SPACE;
							break;
					case	'm':
					case	'M':
							code = PARITY_MARK;
							break;
				}
			}


		// Send set parity option
		sendSB(COM_PORT_OPTION,SET_PARITY,code);

		s = "Unknown";

		// Wait one resposne
		if (waitResponse(SET_PARITY) == true)
		{
			switch (sbBufferReceived[1])
			{
				case	PARITY_NONE:
						s = "NONE";
						break;
						
				case	PARITY_ODD:
						s = "ODD";
						break;
						
				case	PARITY_EVEN:
						s = "EVEN";
						break;
						
				case	PARITY_MARK:
						s = "MARK";
						break;
						
				case	PARITY_SPACE:
						s = "SPACE";
						break;
				
			}
		}
		
		return s;

	}

	private int sendControl(int code) throws IOException
	{
		//debugln("send Control "+code);
		
		sendSB(COM_PORT_OPTION,SET_CONTROL,code);

		if (waitResponse(SET_CONTROL) == false)
			return -1;

		//debugln("control result "+sbBuffer[1]);
		return sbBufferReceived[1] & 0xff;
	}

	public int getFlow() throws IOException
	{
		return setFlow(FLOW_QUERY);
	}

	public int setFlow(int flow) throws IOException
	{
		int code = 0;
		
		switch (flow)
		{
			case	FLOW_QUERY:
					code = 0;
					break;
					
			case	FLOW_NONE:
					code = 1;
					break;
			case	FLOW_XONXOFF:
					code = 2;
					break;
			case	FLOW_CTSRTS:
					code = 3;
					break;
		}


		flow = -1;

		switch (sendControl(code))
		{
			case	1:
					flow = FLOW_NONE;
					break;
			case	2:
					flow = FLOW_XONXOFF;
					break;
			case	3:
					flow = FLOW_CTSRTS;
					break;
		}

		return flow;
	}

	private boolean sendControlCheck(int code) throws IOException
	{
		if (sendControl(code) == code)
			return true;
		else
			return false;
	}

	public boolean getDSR() throws IOException
	{
		return (modemState & MODEM_DSR) != 0 ? true : false;
	}

	public boolean getDCD() throws IOException
	{
		return (modemState & MODEM_DCD) != 0 ? true : false;
	}

	public boolean getCTS() throws IOException
	{
		return (modemState & MODEM_CTS) != 0 ? true : false;
	}

	public boolean getRI() throws IOException
	{
		return (modemState & MODEM_RI) != 0 ? true : false;
	}

	public boolean getDTR() throws IOException
	{
		int code = sendControl(7);

		if (code == 8)
			return true;
		else
			return false;
	}

	public boolean setDTR(boolean mode) throws IOException
	{
		return sendControlCheck(mode ? 8 : 9);
	}


	public boolean getRTS() throws IOException
	{
		int code = sendControl(10);

		if (code == 11)
			return true;
		else
			return false;
	}

	public boolean setRTS(boolean mode) throws IOException
	{
		return sendControlCheck(mode ? 11 : 12);
	}


	public boolean getBREAK() throws IOException
	{
		int code = sendControl(4);

		if (code == 5)
			return true;
		else
			return false;
	}
	
	public boolean setBREAK(boolean mode) throws IOException
	{
		return sendControlCheck(mode ? 5 : 6);
	}

	public String getPortConfig() throws IOException
	{
		String s = getSignature()+":"+getBaud()+","+getParity()+","+
				   getDataSize()+","+getStopSize();

		switch (getFlow())
		{
			case	FLOW_XONXOFF:
					s = s + " XON/XOFF";
					break;
			case	FLOW_CTSRTS:
					s = s + " CTS/RTS";
					break;
		}

		return s;
	}

	public boolean isComSupported()
	{
		
		//if (getRemoteOption(COM_PORT_OPTION) == false)
		//	return false;
		if (getLocalOption(COM_PORT_OPTION) == false)
			return false;

		return true;
		
	}

	public boolean sendInfo() throws IOException
	{
		return sendControlCheck(126);

	}

	public boolean sendTest() throws IOException
	{
		return sendControlCheck(125);

	}

	void testPort() throws IOException
	{
		int baudrates[] = {115200,57600,38400,19200,9600,4800,2400,1200,600,300};
		String parity[] = {"NONE","EVEN","ODD","SPACE","MARK"};
		String flow[] = {"None","XON/XOFF","CTS/RTS"};
		int i;
		String s;

		System.out.println("$Id: NVTCom.java,v 1.2 2009/07/16 22:04:51 headley Exp $ Test port "+getRemoteHost()+":"+getRemotePort());

		
		if (isComSupported() == false)
		{
			System.out.println("NOT a RFC2217 compliant server");
			socket.close();
			System.exit(1);
			return;
		}
				
		
		System.out.println("Port signature       : " + getSignature());
		System.out.println("Current baud rate    : " + getBaud());
		
		System.out.print(  "Supported baud rates : ");
		
		for (i = 0 ; i < baudrates.length ; i++)
			if (setBaud(baudrates[i]) == baudrates[i])
				System.out.print(baudrates[i]+" ");
		System.out.println();

		System.out.println("Current data size    : " + getDataSize());

		System.out.print(  "Supported data sizes : ");

		for (i = 0 ; i < 4 ; i++)
			if (setDataSize(i+5) == (i+5))
				System.out.print((i+5)+" ");
		
		System.out.println();

		System.out.println("Current parity       : " + getParity());

		System.out.print(  "Supported parity     : ");

		for (i = 0 ; i < parity.length ; i++)
		{
			
			if (setParity(parity[i]).equalsIgnoreCase(parity[i]))
				System.out.print(parity[i]+" ");
		}
		
		System.out.println();

		System.out.println("Current stop size    : " + getStopSize());

		System.out.print(  "Supported stop sizes : ");

		for (i = 0 ; i < 3 ; i++)
			if (setStopSize(i+1) == (i+1))
				System.out.print((i+1)+" ");

		System.out.println();

		System.out.print("Stop 1.5 with 5 bit  : ");
		setDataSize(5);
		
		if (setStopSize(3) != 3)
			System.out.print("Not ");
		
		System.out.println("Supported");

		setStopSize(1);
		
		i = getFlow();

		System.out.print("Current Flow         : ");
		
		if (i == -1)
			System.out.println("Not supported");
		else
		{
			System.out.println(flow[i]);
			System.out.print("Supported Flow       : ");
			for (i = 0 ; i < flow.length ; i++)
				if (setFlow(i) == i)
					System.out.print(flow[i]+" ");
			System.out.println();
		}

		setFlow(FLOW_NONE);
		
		System.out.print( "BREAK Support        : ");

		s= "Not ";
		
		if (setBREAK(false) == true)
			if (getBREAK() == false)
				if (setBREAK(true) == true)
					if (getBREAK() == true)
						s = "";

		setBREAK(false);
		
		System.out.println(s+"Supported");
		
		System.out.print( "DTR Support          : ");

		s= "Not ";

		if (setDTR(false) == true)
			if (getDTR() == false)
				if (setDTR(true) == true)
					if (getDTR() == true)
						s = "";

		setDTR(false);
		
		System.out.println(s+"Supported");

		
		System.out.print( "RTS Support          : ");

		s= "Not ";

		if (setRTS(false) == true)
			if (getRTS() == false)
				if (setRTS(true) == true)
					if (getRTS() == true)
						s = "";

		setRTS(false);

		System.out.println(s+"Supported");

		System.out.print("MODEM state change   : ");

		if (setModemMask(0xff) == false)
		{
			System.out.println("Not supported");
		}
		else
		{
			System.out.println("Supported");
		}
		
	}
	
	static public void main(String args[])
	{
		if (args.length < 2)
		{
			System.out.println("Usage : NVTCom address port");
			System.exit(1);
		}
		
		try
		{
			System.out.println("NVT Test on $Revision: 1.2 $ "+args[0]+":"+args[1]);
			int port = Integer.parseInt(args[1]);
			//NVTCom com = new NVTCom("172.29.2.11",7000);
			//NVTCom com = new NVTCom("172.29.4.252",7001);
			//NVTCom com = new NVTCom("172.29.2.220",1000);
			NVTCom com = new NVTCom(args[0],port);
			if (args.length > 2)
			    com.addLogger(args[2]);
			if (args.length > 3)
			    com.setLogLevel(Integer.parseInt(args[3]));
			com.testPort();
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.out.println(ex);
			System.exit(1);
		}
	}
}

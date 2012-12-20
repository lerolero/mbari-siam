package it.m2.net.telnet;

import java.io.*;
import java.net.*;
import java.util.Vector;

/**
 * Class to handle telnet protocol
 *
 * @author Mario Viara
 * @version $Id: Telnet.java,v 1.1 2009/07/15 20:40:51 bobh Exp $
 *
 * Modified by Bob Herlien, MBARI.  Added available(), flush(), and
 * read(byte[],int,int) to support InputStream/OutputStream
 */
public class Telnet implements TelnetOptions
{
	
	
	// State to handle telnet data
	private static final int IS_NORMAL		= 0;
	private static final int IS_IAC			= 1;
	private static final int IS_IACDO		= 2;
	private static final int IS_IACWILL		= 3;
	private static final int IS_IACDONT		= 4;
	private static final int IS_IACWONT		= 5;
	private static final int IS_IACSB		= 6;
	private static final int IS_IACSBDATA	= 7;
	private static final int IS_IACSBIAC	= 8;
	private static final int IS_CR			= 9;
	

	// Log level
	private static int		logLevel = 5;
	
	// Output state
	private int				oState;

	// Input state
	private	int				iState;

			
	protected Socket			socket;
	private OutputStream	os;
	private InputStream		is;
	private RingBuffer		ib;

	// Timeout negoziazioni telnet
	private int TIMEOUT	= 1000;
	
	// Current sub negotiation option
	private int				sbOption;

	// Lenght op sub negotiation buffer
	private int				sbLength;

	// Sub negotiation buffer
	private byte			sbBuffer[];

	// Local option status
	private	TelnetOptionStatus	options[];

	private long sentBytes;
	private long rcvdBytes;

	private int		receivedCount;
	private int		receivedNum;
	private byte	receivedBuffer[];
	static private Vector	loggers = new Vector();
	
	public Telnet() throws IOException
	{
		this(null);
	}

	public Telnet(String host,int port) throws IOException
	{
		this(new Socket(host,port));
	}
	
	
	public Telnet(Socket socket) throws IOException
	{
		setSocket(socket);



	}

	public static void addLogger(OutputStream os)
	{
		addLogger(new TelnetLoggerFile(os));
	}
	
	public static void addLogger(String filename)
	{
		try
		{
			addLogger(new TelnetLoggerFile(filename));
		}
		catch (Exception ex)
		{
		}
	}
	

	public synchronized static void addLogger(TelnetLogger logger)
	{
		loggers.add(logger);
	}

	public boolean isLog(int level)
	{
		return logLevel >= level ? true : false;
	}

	public void log(int lvl,String title,byte buffer[],int length)
	{
		if (isLog(lvl) == false)
			return;

		StringBuffer sb = new StringBuffer();
		sb.append(title+": '");
		
		for (int i = 0 ; i < length ; i++)
		{
			switch (buffer[i])
			{
				default:
						if (buffer[i] < 32)
						{
							String s = Integer.toHexString(buffer[i] & 0xff);
							if (s.length() < 2)
								s = "0"+s;
							s = s.toUpperCase();
							sb.append("<"+s+">");
						}
						else
						{
							sb.append(new String(buffer,i,1));
						}
						break;
				case	(byte)TelnetOptions.IAC:
						sb.append("<IAC>");
						break;
				case	(byte)TelnetOptions.DONT:
						sb.append("<DONT>");
						break;
				case	(byte)TelnetOptions.DO:
						sb.append("<DO>");
						break;
				case	(byte)TelnetOptions.WONT:
						sb.append("<WONT>");
						break;
				case	(byte)TelnetOptions.WILL:
						sb.append("<WILL>");
						break;
				case	(byte)TelnetOptions.SB:
						sb.append("<SB>");
						break;
				case	(byte)TelnetOptions.SE:
						sb.append("<SE>");
						break;
				case	13:
						sb.append("<CR>");
						break;
				case	10:
						sb.append("<LF>");
						break;
				case	27:
						sb.append("<ESC>");
						break;
						
			}
		}

		sb.append("'");
		log(lvl,sb.toString());
	}
	
	public synchronized void log(int level,String txt)
	{
		
		if (isLog(level))
		{
			java.util.Date date = new java.util.Date();
			
			for (int i = 0 ; i < loggers.size() ; i++)
			{
				TelnetLogger logger = (TelnetLogger)loggers.elementAt(i);
				logger.log(this,level,date,txt);
			}
		}
	}

	public final void setSocket(String host,int port) throws IOException
	{
		setSocket(new Socket(host,port));
	}
	
	public final void setSocket(Socket socket) throws IOException
	{
		receivedCount = 0;
		receivedNum = 0;
		receivedBuffer= new byte[1024];
		sentBytes = 0;
		rcvdBytes = 0;
		
		this.socket = socket;

		if (socket == null)
			return;

		
		is = socket.getInputStream();

		os = socket.getOutputStream();
		
		iState = IS_NORMAL;
		oState = IS_NORMAL;

		ib = new RingBuffer();

		sbBuffer = new byte[1024];

		options		= new TelnetOptionStatus[256];

		for (int i = 0 ; i < 256 ; i++)
		{
			options[i] = null;
		}

		setOptions();
		setDefaults();
	}

	protected void setOptions() throws IOException
	{
	}

	protected void setDefaults() throws IOException
	{
	}

        /** Return number of bytes that can be read from RingBuffer.
	 * Added to support NVTInputStream.available().
	 */
        public int available()
        {
	    if (ib.isEmpty())
		try {
		    negotiate();
		} catch (Exception e) {
		}
		
	    return(ib.available());
	}

        /** Flush the OutputStream
	 * Added to support NVTOutputStream.flush()
	 */
        public void flush() throws IOException
        {
	    os.flush();
	}

	public void releaseCpu()
	{
		try
		{
			Thread.sleep(100);
		}
		catch (Exception ex)
		{
		}

	}

	public boolean isDo(int option)
	{
		TelnetOptionStatus o = options[option];

		if (o == null)
			return false;

		return o.isDo();
	}


	public boolean isWill(int option)
	{
		TelnetOptionStatus o = options[option];

		if (o == null)
			return false;

		return o.isWill();
	}
	
	public boolean setWillOption(TelnetOptionStatus option,boolean mode) throws IOException
	{
		
		// Check if the option is supported
		if (option == null)
			return false;

		log(5,"WILL "+option+" = "+mode);

		long timeout = System.currentTimeMillis() + TIMEOUT;

		if (mode == true)
		{
			sendIAC(WILL,option.getOption());
			
			option.setWillSent(true);
			
			while (option.isWillSent())
			{
				negotiate();
				releaseCpu();
				
				if ( System.currentTimeMillis() > timeout)
				{
					log(5,"Will Timeout");
					option.setWillSent(false);
					return false;
				}
			}
		}
		else
		{
			sendIAC(WONT,option.getOption());
			option.setWontSent(true);
			while (option.isWontSent())
			{
				negotiate();
				releaseCpu();

				if ( System.currentTimeMillis() > timeout)
				{
					log(5,"Want Timeout");
					option.setWontSent(false);
					return false;
				}
			}

		}

		return true;
	}

	public boolean setWillOption(int option,boolean mode) throws IOException
	{
		return setWillOption(options[option],mode);
	}
	

	public boolean getLocalOption(int option)
	{
		return options[option].isWill();
	}

	public boolean getRemoteOption(int option)
	{
		return options[option].isDo();
	}

	public void addOption(int option,TelnetOptionHandler handler)
	{
		options[option] = new TelnetOptionStatus(option);
		options[option].setHandler(handler);

		log(5,"Add option "+options[option]);
	}


	
	boolean  setDoOption(TelnetOptionStatus option,boolean mode) throws IOException
	{
		// Check if the option is supported
		if (option == null)
			return false;

		log(5,"DO "+option+" = "+mode);

		long timeout = System.currentTimeMillis() + TIMEOUT;

		if (mode == true)
		{
			sendIAC(DO,option.getOption());
			option.setDoSent(true);
			
			while (option.isDoSent())
			{
				negotiate();
				releaseCpu();
				if ( System.currentTimeMillis() > timeout)
				{
					log(5,"DO Timeout");
					option.setDoSent(false);
					return false;
				}
			}
		}
		else
		{
			sendIAC(DONT,option.getOption());
			option.setDontSent(true);
			while (option.isDontSent())
			{
				negotiate();
				releaseCpu();

				if ( System.currentTimeMillis() > timeout)
				{
					log(5,"DONT Timeout");
					option.setDontSent(false);
					return false;
				}
			}
		}

		return true;
	}

	public boolean setDoOption(int option,boolean mode) throws IOException
	{
		return setDoOption(options[option],mode);
	}


	private void receivedCr(int c)
	{
		iState = IS_NORMAL;
		
		if (c == 0)
			return;
		ib.put(c);
	}
	
	private void receivedNormal(int c)
	{
		switch (c)
		{
			case	IAC:
					iState = IS_IAC;
					break;
			case	CR:
					ib.put(c);
					if (isDo(TRASMIT_BINARY) == false)
						iState = IS_CR;
					break;
			default:
					ib.put(c);
					break;
		}
	}
	
	private void receivedIAC(int c)
	{
		switch (c)
		{
			case	NOP:
					log(7,"RCVD IAC NOP");
					iState = IS_NORMAL;
					break;
			case	IAC:
					ib.put(c);
					iState = IS_NORMAL;
					break;
			case	DO:
					iState = IS_IACDO;
					break;
			case	WILL:
					iState = IS_IACWILL;
					break;
			case	WONT:
					iState = IS_IACWONT;
					break;
			case	DONT:
					iState = IS_IACDONT;
					break;
			case	SB:
					iState = IS_IACSB;
					break;
			default:
					ib.put(IAC);
					ib.put(c);
					iState = IS_NORMAL;
					log(4,"Warning received IAC "+c);
					break;
		}
	}


	public void sendIACNOP() throws Exception
	{
		log(7,"SENT IAC NOP");
		sendIAC(NOP);
	}
	
	public void sendSB(int cmd,int opt,byte buffer[]) throws IOException
	{
		byte tmpBuffer[] = new byte[buffer.length*2];
		int i;
		int len;
		
		//FIXME Controllare
/*		debug("SEND IAC SB "+cmd+" "+opt+" len = "+buffer.length+"(");
		for (i = 0 ; i < buffer.length ; i++)
			debug((buffer[i] & 0xff)+" ");
		debugln(")");
*/
		for (i = 0 , len = 0 ; i < buffer.length ; i++)
		{
			if ((buffer[i] & 0xff) == IAC)
			{
				tmpBuffer[len++] = (byte)IAC;
				tmpBuffer[len++] = (byte)IAC;
			}
			else
				tmpBuffer[len++] = buffer[i];
		}


		log(4,"Send SB cmd "+cmd+" opt "+opt+" ("+buffer.length+")");
		
		os.write(IAC);
		os.write(SB);
		os.write(cmd);
		os.write(opt);
		os.write(tmpBuffer,0,len);
		os.write(IAC);
		os.write(SE);

		updateSentByteCount(len+6);
	}
	
	public void sendSB(int cmd,int opt,String buffer) throws IOException
	{
		sendSB(cmd,opt,buffer.getBytes());
	}

	public void sendSB(int cmd,int opt,int value) throws IOException
	{
		byte buffer[] = new byte[1];
		buffer[0] = (byte)value;
		
		sendSB(cmd,opt,buffer);
	}
	
	public void sendSB(int cmd,int opt) throws IOException
	{
		sendSB(cmd,opt,"");
	}

	public void sendIAC(int cmd) throws IOException
	{
		log(7,"SEND IAC "+cmd);
		byte buffer[] = new byte[2];

		buffer[0] = (byte)IAC;
		buffer[1] = (byte)cmd;
		os.write(buffer);
		updateSentByteCount(2);
	}
	
	public void sendIAC(int cmd,int opt) throws IOException
	{
		byte buffer[] = new byte[3];

		buffer[0] = (byte)IAC;
		buffer[1] = (byte)cmd;
		buffer[2] = (byte)opt;
		os.write(buffer);
		updateSentByteCount(3);

		String name = "UNKNOWN ("+cmd+")";
		
		switch (cmd)
		{
			case	NOP:
					name = "NOP";
					break;
			case	DO:
					name = "DO";
					break;
			case	DONT:
					name = "DONT";
					break;
			case	WILL:
					name = "WILL";
					break;
			case	WONT:
					name = "WONT";
					break;
		}
		
		log(7,"SENT IAC "+name+" "+opt);

		
	}
	
	private void receivedIACDO(int c) throws IOException
	{
		log(7,"RCVD IAC DO "+c);
		TelnetOptionStatus o;
		TelnetOptionHandler h;
		
		iState = IS_NORMAL;

		o = options[c];

		if (o != null)
		{
			h = o.getHandler();
			
			if (o.isWillSent())
			{
				log(4,"ISWILL Accepted "+c);
				o.setWill(this,true);
				o.setWillSent(false);
				return;
			}

			
			if (h != null)
			{
				if (h.acceptDo(c))
				{
					if (o.isDoProcessed() == false)
					{
						log(4,"DO Accepted "+c);
						o.setDo(this,true);
						sendIAC(WILL,c);
						o.setDoProcessed(true);
					}
					else
						log(4,"DO ALREADY accepted "+c);
					return;
				}
			}
		}

		log(4,"DO reject "+c);
		sendIAC(WONT,c);
		
	}
	
	private void receivedIACWILL(int c) throws IOException
	{
		log(7,"RCVD IAC WILL "+c);
		TelnetOptionStatus o;
		TelnetOptionHandler h;

		iState = IS_NORMAL;

		o = options[c];

		if (o != null)
		{
			h = o.getHandler();
			
			if (o.isDoSent())
			{
				log(4,"ISDO Accepted "+c);
				o.setDo(this,true);
				o.setDoSent(false);
				return;
			}

			if (h != null)
			{
				if (h.acceptWill(c))
				{
					if (o.isWillProcessed() == false)
					{
						log(4,"WILL Accepted "+c);
						o.setWill(this,true);
						sendIAC(DO,c);
						o.setWillProcessed(true);
					}
					else
						log(4,"WILL ALREADY Accepted "+c);
					return;
				}
			}
		}
		
		log(5,"WILL REJECT "+c);
		sendIAC(DONT,c);
		
	}
	
	private void receivedIACDONT(int c) throws IOException
	{
		log(7,"RCVD IAC DONT "+c);
		TelnetOptionStatus o;
		TelnetOptionHandler h;

		iState = IS_NORMAL;

		o = options[c];

		if (o != null)
		{
			h = o.getHandler();

			if (o.isWillSent())
			{
				o.setWill(this,false);
				o.setWillSent(false);
				return;
			}


			if (h != null)
			{

				if (h.acceptDont(c))
				{
					if (o.isDontProcessed() == false)
					{
						log(4,"DONT Accepted "+c);
						o.setDo(this,false);
						sendIAC(WONT,c);
						o.setDontProcessed(true);
					}
					else
						log(4,"DONT ALREADY accepted "+c);
					return;
				}
			}
		}

		sendIAC(WILL,c); // FIXME
	}
	
	private void receivedIACWONT(int c) throws IOException
	{
		log(7,"RCVD IAC WONT "+c);
		TelnetOptionStatus o;
		TelnetOptionHandler h;

		iState = IS_NORMAL;

		o = options[c];

		if (o != null)
		{
			h = o.getHandler();
			
			if (o.isDoSent())
			{
				o.setDo(this,false);
				o.setDoSent(false);
				return;
			}

			if (h != null)
			{
				
				if (h.acceptWont(c))
				{
					if (o.isWontProcessed() == false)
					{
						log(4,"WONT Accepted "+c);
						o.setWill(this,false);
						sendIAC(DONT,c);
						o.setWontProcessed(true);
					}
					else
						log(4,"WONT ALREADY accepted "+c);
					return;
				}
			}
		}


		sendIAC(DO,c);
	}
	
	private void receivedIACSB(int c)
	{
		sbOption = c;
		sbLength = 0;
		iState = IS_IACSBDATA;
	}
	
	private void receivedIACSBDATA(int c)
	{
		if (c == IAC)
			iState = IS_IACSBIAC;
		else
			sbBuffer[sbLength++] = (byte)c;
	}
	
	private void receivedIACSBIAC(int c) throws IOException
	{
		if (c == SE)
		{
/*			debug("RCVD IAC SB "+sbOption+" length "+sbLength+"(");

			for (int i = 0 ; i < sbLength;i++)
				debug(sbBuffer[i]+" ");
			debugln(")");
	*/		
			iState = IS_NORMAL;
			
			TelnetOptionStatus o = options[sbOption];

			if (o != null)
				o.sb(this,sbLength,sbBuffer);
				
		
		}
		else
		{
			sbBuffer[sbLength++] = (byte)IAC;
			
			if (c != IAC)
			{
				log(1,"ERROR IAC SBDATA IAC "+c);
				sbBuffer[sbLength++] = (byte)c;
			}
			
			iState = IS_IACSBDATA;
		}
	}

	public synchronized boolean negotiate() throws IOException
	{
		int count = 0;

		for (;;)
		{
			int n;
			int c;

			if (receivedCount >= receivedNum)
			{
				if (is.available() > 0)
				{
					receivedCount = 0;
					receivedNum = is.read(receivedBuffer);
					//log(4,"rcvd",receivedBuffer,receivedNum);
					updateReceivedByteCount(receivedNum);
				}
				else
					break;
			}

			count++;
			c = receivedBuffer[receivedCount++] & 0xff;
			

			//System.out.print(" "+c);

			//System.out.println("Negotiate "+c+" state "+iState);

			switch (iState)
			{
				default:
						System.out.println("Unknown istate "+iState);
						System.exit(1);
						break;

				case	IS_CR:
						receivedCr(c);
						break;

				case	IS_NORMAL:
						receivedNormal(c);
						break;

				case	IS_IAC:
						receivedIAC(c);
						break;

				case	IS_IACDO:
						receivedIACDO(c);
						break;

				case	IS_IACWILL:
						receivedIACWILL(c);
						break;

				case	IS_IACDONT:
						receivedIACDONT(c);
						break;

				case	IS_IACWONT:
						receivedIACWONT(c);
						break;

				case	IS_IACSB:
						receivedIACSB(c);
						break;

				case	IS_IACSBDATA:
						receivedIACSBDATA(c);
						break;

				case	IS_IACSBIAC:
						receivedIACSBIAC(c);
						break;

			}
		}

		return count == 0 ? false : true;
	}


        /* Added 9July2009 Bob Herlien */
        public int read(byte buffer[], int off, int len) throws IOException
	{
	    int count = 0, pos = off;

	    if ((off < 0) || (len < 0) || (off+len > buffer.length))
		throw new IndexOutOfBoundsException();

	    if (ib.isEmpty())
		negotiate();
		
	    while ((ib.isEmpty() == false) && (count < len) && (pos < buffer.length))
	    {
		buffer[pos] = (byte)ib.get();
		pos++;
		count++;
	    }

	    return count;
	}


        /* Re-implemented using read(byte[],int,int) 9July2009 Bob Herlien */
        public int read(byte buffer[]) throws IOException
        {
	    return(read(buffer, 0, buffer.length));
	}


	public int read() throws IOException
	{
		if (ib.isEmpty())
		{
			negotiate();
			if (ib.isEmpty())
				return -1;
		}

		return ib.get();
	}

	
	public void write(int c) throws IOException
	{
		switch (oState)
		{
			case	IS_NORMAL:
					os.write(c);
					updateSentByteCount(1);

					switch (c)
					{
						case	CR:
								if (isWill(TRASMIT_BINARY) == false)
									oState = IS_CR;
								break;
						case	IAC:
								os.write(IAC);
								updateSentByteCount(1);
								
								break;
					}
					break;
					
			case	IS_CR:
					if (c != LF)
					{
						os.write(0);
						updateSentByteCount(1);
					}
					os.write(c);
					updateSentByteCount(1);
					oState = IS_NORMAL;
					break;
		}
	}


	public void write(String s) throws IOException
	{
		write(s.getBytes());
	}

	public void write(byte buffer[]) throws IOException
	{
		write(buffer,0,buffer.length);
	}

	public void write(byte buffer[],int from,int len) throws IOException
	{
		byte tmpBuffer[] = new byte[buffer.length*2];
		int i;
		int l;
		
		for (i = 0,l = 0; i < len ; i++)
			switch (oState)
			{
				case	IS_NORMAL:
						
						switch (buffer[from+i])
						{
							default:
									tmpBuffer[l++] = buffer[from+i];
									break;
							case	(byte)IAC:
									tmpBuffer[l++] = (byte)IAC;
									tmpBuffer[l++] = (byte)IAC;
									break;
							case	CR:
									tmpBuffer[l++] = CR;
									if (isWill(TRASMIT_BINARY) == false)
										oState = IS_CR;				
									break;
						}
						break;
						
				case	IS_CR:
						if (buffer[i] != LF)
							tmpBuffer[l++] = 0;
						tmpBuffer[l++] = buffer[from+i];
						oState = IS_NORMAL;
						break;
			}
		
		os.write(tmpBuffer,0,l);
		updateSentByteCount(l);
		
	}
	
	static public void main(String args[])
	{
		System.out.println("Telnet test");
		final RingBuffer bc = new RingBuffer();
		
		Thread tr = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
				for (;;)
				{
				
					int c = System.in.read();
					if (c == -1)
					{
						System.exit(1);
					}
					bc.put(c);
				}
				}
				catch (Exception ex)
				{
					System.out.println(ex);
				}
			}
		});

		tr.start();
		
		try
		{
			int c;
			byte b[] = new byte[1];
			//Telnet t = new Telnet(new Socket("172.29.1.21",23));
			Telnet t = new Telnet(new Socket("172.29.10.2",23));
			RFC1091 rfc1091 = new RFC1091();
			
			t.addOption(Telnet.TERMINAL_TYPE,rfc1091);
			
			for (;;)
			{
				c = t.read();
				
				if (c != -1)
				{
					b[0] = (byte)c;
					System.out.print(new String(b));
				}

				c = bc.get();

				if (c != -1)
				{
					t.write(c);
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println(ex);
			System.exit(1);
		}
					
	}

	protected synchronized void  updateReceivedByteCount(int value)
	{
		rcvdBytes += value;
	}
	
	protected synchronized void  updateSentByteCount(int value)
	{
		sentBytes += value;
	}


	public synchronized long getReceivedByteCount()
	{
		return rcvdBytes;
	}

	public synchronized long getSentByteCount()
	{
		return sentBytes;
	}

	public static void setLogLevel(int level)
	{
		logLevel = level;
	}

	public int getLogLevel()
	{
		return logLevel;
	}
	
	public void close() throws IOException
	{
		if (socket != null)
		{
			socket.close();
			socket = null;
		}
		
	}

	public InetAddress getInetAddress()
	{
		return socket.getInetAddress();
	}

	public String getRemoteHost()
	{
		InetAddress add = getInetAddress();

		return add.getHostAddress();

	}

	public int getRemotePort()
	{
		return socket.getPort();
	}
}

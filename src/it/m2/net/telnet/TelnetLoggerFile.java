package it.m2.net.telnet;

import java.io.*;
import java.net.*;

public class TelnetLoggerFile implements TelnetLogger
{
	private PrintStream ps;
	
	TelnetLoggerFile(String name) throws Exception
	{
		this(new FileOutputStream(name));
	}

	TelnetLoggerFile(OutputStream os)
	{
		ps = new PrintStream(os);
	}
	
	public void log(Telnet telnet,int level,java.util.Date date,String txt)
	{
		ps.println("<"+level+"> "+date+" "+txt);
	}
}
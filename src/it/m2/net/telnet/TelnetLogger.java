package it.m2.net.telnet;

import java.io.*;
import java.net.*;

public interface TelnetLogger
{
	public void log(Telnet telnet,int level,java.util.Date date,String txt);
}
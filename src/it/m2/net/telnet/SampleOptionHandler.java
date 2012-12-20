package it.m2.net.telnet;
import java.io.IOException;

public class SampleOptionHandler implements TelnetOptionHandler
{
	public void setDo(Telnet telnet,int option,boolean oldState,boolean newState)
	{
	}
	

	public void setWill(Telnet telnet,int option,boolean oldState,boolean newState)
	{
	}

	public void		sb(Telnet telnet,int option,int sbLength,byte sbBuffer[]) throws IOException
	{
	}

	public boolean	acceptDo(int option)
	{
		return true;
	}
	
	public boolean  acceptWill(int option)
	{
		return true;
	}
	public boolean  acceptWont(int option)
	{
		return true;
	}
	
	public boolean  acceptDont(int option)
	{
		return true;
	}

}

package it.m2.net.telnet;

public class NVTEvent 
{
	private NVT nvt;
	
	NVTEvent(NVT nvt)
	{
		this.nvt = nvt;
	}

	NVT getSource()
	{
		return nvt;
	}
}
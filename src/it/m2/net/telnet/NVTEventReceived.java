package it.m2.net.telnet;

public class NVTEventReceived extends NVTEvent
{
	byte buffer[];
	int len;
	
	NVTEventReceived(NVT nvt,byte buffer[],int len)
	{
		super(nvt);
		this.buffer = buffer;
		this.len = len;
	}

	public byte[] getBuffer()
	{
		return buffer;
	}

	public int getLength()
	{
		return len;
	}
}


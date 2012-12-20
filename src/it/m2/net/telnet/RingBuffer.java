package it.m2.net.telnet;

/**
 * Sample utility class to implement a ring buffer
 *
 * @author Mario Viara
 * @version $Id: RingBuffer.java,v 1.1 2009/07/15 20:40:51 bobh Exp $
 *
 * Modified by Bob Herlien, MBARI.  Added available()
 */
class RingBuffer
{
	/// Buffer to store data
	private byte buffer[];

	// Read and write pointer
	private int ps,pl;

	/// Default constructor
	RingBuffer()
	{
		this(1024);
	}

	RingBuffer(int size)
	{
		buffer = new byte[size];
		pl = ps = 0;
	}

	public boolean isEmpty()
	{
		return pl == ps ? true : false;
	}

	public boolean isFull()
	{
		int newPs = ps + 1;

		if (newPs >= buffer.length)
			newPs = 0;

		return newPs == pl ? true : false;
	}

        public int available()
        {
	    int n = ps - pl;
	    if (n < 0)
		n += buffer.length;
	    return(n);
	}

	public int get(byte dest[])
	{
		int i;
		int c;

		for (i = 0 ; i < dest.length ; i++)
		{
			c = get();

			if (c == -1)
				break;
			dest[i] = (byte)(c & 0xff);
		}

		return i;
	}

	public int get()
	{
		byte b;

		if (isEmpty())
			return -1;

		b = buffer[pl];

		if (++pl >= buffer.length)
			pl = 0;

		return b & 0xff;

	}

	public boolean put(int c)
	{
		if (isFull())
		{
			return false;
		}

		buffer[ps++] = (byte)(c & 0xff);

		if (ps >= buffer.length)
			ps = 0;

		return true;
	}

	public int put(byte buffer[])
	{
		int i;

		for (i = 0 ; i < buffer.length ; i++)
			if (put(buffer[i]) == false)
				break;

		return i;
	}
}

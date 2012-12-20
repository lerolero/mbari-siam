package it.m2.net.telnet;

import java.io.IOException;

/***
 *
 * Sample implementation ot terminel option TERMINAL-TYPE 24
 * described in RFC1091.
 * If no terminal are added the default is UNKNOWN
 *
 * This version do not notify terminal change.
 *
 * @author Mario Viara
 * @version $Id: RFC1091.java,v 1.1 2009/07/15 20:40:51 bobh Exp $
 */
class RFC1091 extends SampleOptionHandler
{
	/// Vector to store all supported terminal
	private java.util.Vector	terminals;

	private int	terminalIndex;


	RFC1091()
	{
		terminalIndex = 0;
		terminals = new java.util.Vector();
	}

	public void setDo(Telnet telnet,int option, boolean oldState,boolean newState)
	{
		terminalIndex = 0;
	}


	public void sb(Telnet telnet,int option,int length,byte buffer[]) throws IOException
	{
		if (buffer[0] == Telnet.TERMINAL_TYPE_SEND)
		{
			String name = "UNKNOWN";


			if (terminalIndex < terminals.size())
				name = (String)terminals.elementAt(terminalIndex++);

			telnet.sendSB(Telnet.TERMINAL_TYPE,Telnet.TERMINAL_TYPE_IS,name);

		}
	}

	/**
	 * Add a new supported terminal name
	 */
	public void addTerminal(String name)
	{
		terminals.add(name);
	}

}


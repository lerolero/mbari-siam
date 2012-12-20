package it.m2.net.telnet;
import java.io.IOException;

/**
 * Interface to handle telnet option
 *
 * @author Mario Viara
 * @version $Id: TelnetOptionHandler.java,v 1.2 2009/07/16 22:04:51 headley Exp $
 */
interface TelnetOptionHandler
{
	/**
	 * Invoked when the option chane state
	 *
	 * @param telnet Telnet ownner
	 * @param option Telnet option number
	 * @param oldState Old state of the option
	 * @param newState New state of the option
	 */
	public void setDo(Telnet telnet,int option,boolean oldState,boolean newState);

	public void setWill(Telnet telnet,int option,boolean oldState,boolean newState);

	/**
	 * Invoke when telnet sub negotiation is necessary
	 *
	 * @param telnet Telnet ownner
	 * @param option Telnet option number
	 * @param sbLength lenght of the sub option received
	 * @param sbBuffer buffer with the sub option received.
	 */
	public void		sb(Telnet telnet,int option,int sbLength,byte sbBuffer[]) throws IOException;

	public boolean	acceptDo(int option);
	public boolean  acceptWill(int option);
	public boolean  acceptWont(int option);
	public boolean  acceptDont(int option);
	
}

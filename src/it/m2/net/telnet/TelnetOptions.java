package it.m2.net.telnet;

public interface TelnetOptions
{
	// RFC856 Options
	public static final int TRASMIT_BINARY		= 0;	

	// RFC857 oprions
	public static final int ECHO				= 1;	

	// RFC1091 options
	public static final int TERMINAL_TYPE		= 24;	
	public static final int TERMINAL_TYPE_SEND	= 1;
	public static final int TERMINAL_TYPE_IS	= 0;

	// RFC858 options
	public static final int SUPPRESS_GO_AHEAD	= 3;	

	// RFC 2217 option
	public final static int COM_PORT_OPTION	= 44;
	public final static int SIGNATURE           = 0;
	public final static int SET_BAUDRATE        = 1;
	public final static int SET_DATASIZE        = 2;
	public final static int SET_PARITY          = 3;
	public final static int SET_STOPSIZE        = 4;
	public final static int SET_CONTROL         = 5;

	public final static int NOTIFY_LINESTATE    = 6;
	public final static int NOTIFY_MODEMSTATE   = 7;
	public final static int FLOWCONTROL_SUSPEND = 8;
	public final static int FLOWCONTROL_RESUME  = 9;
	public final static int SET_LINESTATE_MASK  = 10;
	public final static int SET_MODEMSTATE_MASK = 11;
	public final static int PURGE_DATA          = 12;
	public final static int SERVER_PREFIX       = 100;

	public final static int MODEM_DCD			= 0x80;
	public final static int MODEM_RI			= 0x40;
	public final static int MODEM_DSR			= 0x20;
	public final static int MODEM_CTS			= 0x10;
	public final static int MODEM_DDCD			= 0x08;
	public final static int MODEM_DRI			= 0x04;
	public final static int MODEM_DDSR			= 0x02;
	public final static int MODEM_DCTS			= 0x01;
	public final static int DATASIZE5      = 5;
	public final static int DATASIZE6      = 6;
	public final static int DATASIZE7      = 7;
	public final static int DATASIZE8      = 8;

	public final static int PARITY_NONE    = 1;
	public final static int PARITY_ODD     = 2;
	public final static int PARITY_EVEN    = 3;
	public final static int PARITY_MARK    = 4;
	public final static int PARITY_SPACE   = 5;

	public final static int STOP_1         = 1;
	public final static int STOP_2         = 2;
	public final static int STOP_15        = 3;

	/** request status of RTS */
	//public final  static int REQUEST_RTS           = 10;
	/** set RTS to on */
	//public final  static int SET_RTS               = 11;
	/** set RTS to off */
	//public final  static int CLEAR_RTS             = 12;
	/** get inbound flow control */
	public final  static int REQUEST_INBOUND       = 13;
	/** use no flow control (inbound) */
	public final  static int FLOW_CTRL_IN_NONE     = 14;
	/** use XON/XOFF flow control (inbound) */
	public final  static int FLOW_CTRL_IN_XONXOFF  = 15;
	/** use hardware flow control (inbound) */
	public final  static int FLOW_CTRL_IN_HARDWARE = 16;
	/** use DCD flow control (outbound/both) */
	public final  static int FLOW_CTRL_DCD         = 17;
	/** use DTR flow control (inbound) */
	public final  static int FLOW_CTRL_DTR         = 18;
	/** use DSR flow control (outbound/both) */
	public final  static int FLOW_CTRL_DSR         = 19;

	// Telnet special characters
	public static final int SE		= 240;
	public static final int NOP		= 241;
	public static final int SB		= 250;
	public static final int WILL	= 251;
	public static final int WONT	= 252;
	public static final int DO		= 253;
	public static final int DONT	= 254;
	public static final int	IAC		= 255;
	public static final int CR		= 13;
	public static final int LF		= 10;

}

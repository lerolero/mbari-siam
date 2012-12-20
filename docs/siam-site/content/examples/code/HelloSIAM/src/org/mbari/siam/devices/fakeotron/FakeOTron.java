package org.mbari.siam.devices.fakeotron;

import java.util.Vector;
import java.util.Iterator;
import java.util.StringTokenizer;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.jserial.JSerial;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;


/**
 Fake instrument for HelloSIAM example.
 Writes text data message to a specified serial port, that may be connected to a second serial port
 to be read by the HelloSIAM service.
 */
public class FakeOTron {
	
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(FakeOTron.class);
	
	public static final int MODE_POLLED=0;
	public static final int MODE_STREAM=1;
	public static final String CMD_GET_DATA="getData";
	public static final String CMD_SET_INC="setInc";
	public static final String CMD_GET_INC="getInc";
	public static final String PROMPT="F>";

	JSerial _serialPort=null;
	String _portName=null;
	String _dataText="This is Fake-O-Tron data";
	long _periodMsec=10000L;
	int _mode=MODE_POLLED;
	int _dataIncrement=1;
	int _dataCounter=0;
	
    public FakeOTron(JSerial port) {
		_serialPort=port;
    }
	
	protected void messageResponse(String message)throws Exception{
		if(message!=null){
			sendData(message);
		}
		_serialPort.write(PROMPT.getBytes());
	}
	protected void sendData(String message)throws Exception{
		if(message!=null){
			_serialPort.write(message.getBytes());
		}
	}

	
	public void goFakeOTronic(){
		_log4j.debug("Going Fake-O-Tronic!");
		try{
			_serialPort.open();
		}catch (Exception e) {
			_log4j.error("bummer:"+e);
		}
		boolean doLoop=true;
		int _dataCounter=0;
		String returnString=null;
		String line=null;
		while(doLoop==true){
		
			if(_mode==MODE_POLLED){
				// read a line
				_log4j.debug("waiting for command...");
				try{
					line=_serialPort.readLine();
				}catch (IOException ioe) {
					ioe.printStackTrace();
				}
				_log4j.debug("read line ["+line+"]");
				
				// adjust for backspace and other editing characters
				// (create new "cooked" version of line
				byte[] lineRaw=line.getBytes();
				byte[] lineCooked=new byte[lineRaw.length];
				int cookedPtr=0;
				for(int i=0;i<lineRaw.length;i++){
					int c=(int)lineRaw[i];
					if( (c==8 || c==127) && cookedPtr>0 ){
						cookedPtr--;
					}else{
						lineCooked[cookedPtr]=lineRaw[i];
						cookedPtr++;
					}
				}
				String cookedLine=new String(lineCooked).trim();

				// parse input line
				// each case sets returnString
				if(cookedLine.equals(CMD_GET_DATA)){
					// process getData command
					_log4j.debug("getData command received");
					_log4j.debug("writing data...");
					returnString=(_dataText+" "+(_dataCounter)+"\r\n");
					_dataCounter+=_dataIncrement;
				}else if(cookedLine.equals(CMD_GET_INC)){
					// process getData command
					_log4j.debug("getInc command received");
					_log4j.debug("writing inc...");
					returnString=(_dataIncrement+"\r\n");
				}else if(cookedLine.indexOf(CMD_SET_INC)>=0){
					// process setInc command 
					// (format setInc <integer>)
					_log4j.debug("setInc command received");
					StringTokenizer st=new StringTokenizer(cookedLine,", =");
					String tok=null;
					if(st.hasMoreTokens()){
						// throw away first token (command)
						st.nextToken();
					}
					if(st.hasMoreTokens()){
					// second token should be new increment value (an int)
						tok=st.nextToken();
					}
					try{
						// parse the new value
						int test=Integer.parseInt(tok);
						_dataIncrement=test;
					}catch (NumberFormatException e) {
						e.printStackTrace();
					}
					_log4j.debug("increment set to "+_dataIncrement);
				}else if(cookedLine.equals("\n") || cookedLine.equals("\r")||cookedLine.equals("\r\n")||cookedLine.equals("")){
					// do nothing (returnString==null will just return prompt)
				}else{
					// command not recognized
					// print error message
					_log4j.debug("command not recognized");
					_log4j.debug("writing error...");
					returnString=("error: invalid command ["+line+"]\r\n");

					// some debugging info
					for(int i=0;i<lineRaw.length;i++){
						_log4j.debug("raw["+i+"]:"+(int)lineRaw[i]+" ["+(char)lineRaw[i]+"]");
					}
					for(int i=0;i<lineCooked.length;i++){
						_log4j.debug("cooked["+i+"]:"+(int)lineCooked[i]+" ["+(char)lineCooked[i]+"]");
					}
				}
				
				try{
					// send response
					_log4j.debug("return response ["+returnString+"]");
					messageResponse(returnString);
				}catch (Exception e) {
					// if that fails, quit
					doLoop=false;
				}
				// reset returnString
				returnString=null;
			}else{	
				// in non-polled mode, just send data periodically
				try{
					sendData((_dataText+"\r\n"));
					Thread.sleep(_periodMsec);
				}catch (Exception e) {
					// quit on failure
					doLoop=false;
				}
			}
		}
	}
	/** Process command line options (for main method) */
    public boolean processArgs(String[] args){
		
		//System.out.println("args: "+args.length);
		if(args.length<=0){
			// help
			printUsage();
			return false;
		}
		
		// set parameters
		String dataText=null;
		long periodMsec=0L;
		
		for(int i=0;i<args.length;i++){
			//System.out.println("arg["+i+"]:"+args[i]);
			if(args[i].equals("-m")){
				i++;
				dataText=args[i];
			}else if(args[i].equals("-t")){
				i++;
				periodMsec=1000L*Long.parseLong(args[i]);
			}
		}
		if(dataText!=null){
			_dataText=dataText;
		}
		if(periodMsec>0L){
			_periodMsec=periodMsec;
		}
		_log4j.debug("data text     : "+_dataText);
		_log4j.debug("update period : "+_periodMsec);

		return true;
	}
	public void printUsage(){
		System.err.println("");
		System.err.println("Usage: Fake-O-Tron [options] [device]");
		System.err.println("");
		System.err.println("Options:");
		System.err.println(" -b <baud>    : bit rate");
		System.err.println(" -d <dataBits>: number of data bits");
		System.err.println(" -s <stopBits>: number of stop bits");
		System.err.println(" -p <parity>  : parity                        (E=even,O=odd,N=none)");
		System.err.println(" -f <flow>    : input and output flow control (N=none,R=rts/cts,X=xon/xoff)");
		System.err.println(" -i <iflow>   : input flow control            (N=none,R=rts/cts,X=xon/xoff)");
		System.err.println(" -o <oflow>   : output flow control           (N=none,R=rts/cts,X=xon/xoff)");
		System.err.println(" -e <echo>    : disable echo");
		System.err.println(" -m <message> : data message");
		System.err.println(" -t <period>  : update period (sec)");
		//System.err.println(" -B <buffer>  : input buffer size in KBytes");
		System.err.println(" -H <host>  : host (for remote serial port)");
		System.err.println(" -P <port>  : port (for remote serial port)");
		System.err.println("");
		System.err.println(" device       : serial port device name       (COM1, /dev/ttyS0, etc.)");
		System.err.println("");
		System.err.println(" Defaults: 9600,N,8,1,Flow:none,Echo:enabled");
		System.err.println("");
    }
	
    /** main: demo and excercise JSerial class */
    public  static void main(String[] args) {
		/* Set up a simple configuration that logs on the console.
		 Note that simply using PropertyConfigurator doesn't work
		 unless JavaBeans classes are available on target. 
		 For now, we configure a PropertyConfigurator, using properties
		 passed in from the command line, followed by BasicConfigurator
		 which sets default console appender, etc.
		 */
		
		PatternLayout layout = 
		new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		
		BasicConfigurator.configure(new ConsoleAppender(layout));
		
		// get and initialize a JSerial port
		JSerial port = new JSerial();
		if(!port.processArgs(args)){
			System.exit(0);
		}
		
		FakeOTron app=new FakeOTron(port);
		app.processArgs(args);
		app.goFakeOTronic();
		System.exit(0);
    }
	
} // end of class

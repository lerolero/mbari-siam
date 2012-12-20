package org.mbari.siam.foce.utils;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

import org.mbari.siam.foce.deployed.*;
import org.mbari.siam.distributed.NodeConfigurator;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
/*
  $Id: FOCEElmoUtil.java,v 1.5 2010/08/04 05:51:19 headley Exp $
  $Name: HEAD $
  $Revision: 1.5 $
*/

/** FOCEElmoUtil - command line utilty to switch power (via foceio daemon) to Elmo 
 controllers.
 */
public class FOCEElmoUtil{
    static private Logger _log4j = Logger.getLogger(FOCEElmoUtil.class);
    protected static final int EXEC_TMOUT = 3000;
    protected static final int IOPORT_PORT = 7933;
    protected Socket _sock;
    protected String _host="localhost";
    protected int _port=IOPORT_PORT;
    protected OutputStream _out;
    BufferedReader _reader;


    public FOCEElmoUtil()
    {
    }
    public FOCEElmoUtil(String host, String port)
    throws Exception{
	if(host!=null){
	    _host=host;
	}
	if(port!=null){
	    _port=Integer.parseInt(port);
	}
	_log4j.debug("connecting to "+_host+":"+_port);
	_sock=new Socket(_host,_port);
	_reader = new BufferedReader(new InputStreamReader(_sock.getInputStream()));
	_out = _sock.getOutputStream();

    }
    public String writeRead(String cmd) throws IOException{
	String foo=(cmd+"\n");
	_log4j.debug("writing "+foo);
	_out.write((cmd+"\n").getBytes());
	String rtn = _reader.readLine();

	if (rtn.startsWith("OK")) {
	    if (rtn.length() > 3)
		return(rtn.substring(3));
	    return(rtn);
	}

	throw new IOException("Exception in transact(): " + rtn);
    }
    public void close() throws IOException
    {
	_out.write("bye\n".getBytes());
	_sock.close();
    }

    public void printUsage(){
	StringBuffer sb=new StringBuffer();
	String cmdName=System.getProperty("EXEC_NAME","FOCEElmoUtil");
	sb.append("\n");
	sb.append("  Usage: "+cmdName+" [-h <host>] [-p <port>] [<value>]\n");
	sb.append("\n");
	sb.append("   Options:\n");
	sb.append("     -h <host>   : host name or IP address of node ["+_host+"]\n");
	sb.append("     -p <port>   : TCP/IP port used by foceio      ["+_port+"]\n");
	sb.append("     --help      : print this help message \n");	
	sb.append("     <value>     : hex byte value (00-ff) to write (w/o leading 0x)\n");
	sb.append("\n");
	sb.append("   Examples:\n");
	sb.append("\n");
	sb.append("     Write 0x80 to digital IO port on this node (foceio using default port)\n");
	sb.append("       "+cmdName+" 80\n");
	sb.append("\n");
	sb.append("     Write 0xC0 to digital IO port on node foce4 using port 7934\n");
	sb.append("       "+cmdName+" -h foce4 -p 7934 80\n");
	sb.append("\n");
	sb.append("     Read current value of digital IO port on this node (foceio using default port)\n");
	sb.append("       "+cmdName+"\n");
	sb.append("\n");
	System.out.println(sb.toString());

    }
    public static void errorOut(String msg){

	if(msg!=null){
	    System.err.println("");
	    System.err.println(msg);
	}
	System.err.println("");
	new FOCEElmoUtil().printUsage();
	System.exit(0);
    }

    public static void main(String[] args)
    {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	// pull the log4j debug level from the java environment (use info if unset)
	Logger.getRootLogger().setLevel(Level.toLevel(System.getProperty("LOG4J_THRESHOLD","INFO")));

	for(int i=0;i<args.length;i++)
	    _log4j.debug("args["+i+"]:"+args[i]);

	String host=null;
	String port=null;
	String value=null;
	boolean readOnly=true;
	// parse command line
	for(int i=0;i<args.length;i++){
	    if(args[i].equals("-h")){
		host=args[i+1];
		i++;
	    }else if(args[i].equals("--help")){
		errorOut(null);
	    }else if(args[i].equals("-p")){
		port=args[i+1];
		i++;
	    }else{
		value=args[i];
		_log4j.debug("value set:"+value);
	    }		    
	}
	// validate command line args...
	if(value!=null){
	    int test=Integer.parseInt(value,16);
	    if( test>255 || test<0 ){
		errorOut("  ERROR: value out of range, expecting 00<value<ff");
	    }
	}

	// do (optional) write and read back
	try{
	    FOCEElmoUtil util=new FOCEElmoUtil(host,port);
	    String result;
	    if( value!=null){
		result=util.writeRead("switchElmo "+value);
		_log4j.debug("write returned:"+result);
	    }
	    result=util.writeRead("readElmo");
	    System.out.println("Digital IO port set to:"+result);
	    util.close();
	}catch(Exception e){
	    e.printStackTrace();
	}
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.mclane;

import gnu.io.SerialPort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.util.Vector;
import java.lang.Boolean;
import java.security.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.StopWatch;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.NoDataException;

import Serialio.xmodem.XModemListener;
import Serialio.xmodem.XModemPort;

public class McLaneProfiler extends PolledInstrumentService 
    implements Instrument {

	
	
    //Default sample schedule.
    private static final long DEFAULT_SAMPLE_INTERVAL = 60*60*1000;  //1 hour;  
    
    private long TOTAL_BYTES_RECEIVED = 0;
	
    //Information about the files coming off the mclane profiler
    private static final String SCIENCE_FILENAME_PREFIX = "SH__";

    private static final String ENGINEERING_FILENAME_PREFIX = "CAST";

    private static final String SCIENCE_FILENAME_EXTENSION = ".HEX";

    private static final String ENGINEERING_FILENAME_EXTENSION = ".ENG";

    //the delimiter used between file names
    private static final String FILE_DELIMITER = ",";

    //Modem commands
    private static final String LINE_TERMINATOR = "\r";

    private static final String SEND_HELLO = LINE_TERMINATOR;
	
    private static final String SEND_SLEEP = "slp" + LINE_TERMINATOR;

    private static final String RECEIVE_HELLO = "T>";

    private static final String REQUEST_RECENT_FILE = "fn" + LINE_TERMINATOR;

    private static final String REQUEST_MD5 = "md5 " ;
    
    private static final String REQUEST_DEL = "del " ;
    
    private static final String Y_TRANSFER = "yxfer ";

    //private static final int MAX_RESPONSE_BYTES = 100000;
    private static final int MAX_RESPONSE_BYTES = 500000; // 10/2009 BCK - Change to avoid having MAX_RESPONSE_BYTES be less than maxBytesTransfer

    //the length of the response, in bytes, returnd from a REQUEST_RECENT_FILE
    // command
    private static final int RECENT_FILES_RETURN_LENGTH = 256;

    private boolean handShakeSuccess = false;

    // Log number for xmodem library internal logging mechanism
    // used in transferFile(); appended to log file name (filename.lognum).
    // Incremented each time transferFile is called.
    private int lognum = 1;

 
    //the maximum time to wait for a file to dowload, in
    // milliseconds
    private static final int MAX_FILE_DOWNLOAD_TIME = 30*60*1000;  // 30 minutes
	
    /** Name of file containing list of downloaded files */
    private static String FILE_LOG_NAME="list.dat";

    /** Name of directory (relative to SIAM log dir) for mclane files */
    private static String MCLANE_FILE_DIR="mclane";

    private static File _mclaneDirectory=null;
    private static File _fileList=null;

    /** Maximum number of mclane directories */
    private static int MAX_FILE_DIRECTORIES=10;

    private List filesDownloadedList = new ArrayList();
	

  // Configurable McLane attributes
	Attributes _attributes = new Attributes(this); 

    // log4j Logger
    private Logger _logger = Logger.getLogger(McLaneProfiler.class);

    public McLaneProfiler() throws RemoteException {
	super();
	_instrumentAttributes.sampleTimeoutMsec = 6000;

	//Set up to only try once.
	try {
	    setMaxSampleTries(1);
	}
	catch (RangeException e) {
	    _logger.error(e);
	}
    }



    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Specify startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return 0;
    }

    /** Specify current limit in increments of 120 mA upto 11880 mA. */
    protected int initCurrentLimit() {
	return 1000;
    } 


    protected byte[] initSampleTerminator() {
	//TODO what is a sample terminator?
	//I don't think there is a sample termiator for the mclane
	return new byte[0];
    }

    protected byte[] initPromptString() {
	return RECEIVE_HELLO.getBytes();
    }

    protected int initMaxSampleBytes() {
	return MAX_RESPONSE_BYTES;
    }

    protected synchronized void requestSample() throws TimeoutException,
						       Exception {
	    //Got Connection
					
	    //complete Modem Handshake, then close up shop so SIAM can request data.
	    completeModemHandshake();

    }


    /**  Method to establish handshake with Telemetry Can. Sends "T>" and looks for "B>"  
	 This routine tries 10 times to establish handshaking, with a 3 second delay between attempts.  
	 If a successful handshake is not established after this effort, the routine throws a TimeoutException.
	 @throws: TimeoutException Indicates that handshake was not established.
	 @throws: IOException in case of trouble reading/writing serial port.
    */
    private void completeModemHandshake() 
	throws NullPointerException,
	       IOException, TimeoutException, Exception {

	int maxTries = 10;
	for (int tries = 0; tries < maxTries; tries++) {
	    _logger.info("Beginning handshake");
	    try {
		_fromDevice.flush();
		_toDevice.write(SEND_HELLO.getBytes());
		_toDevice.flush();
		StreamUtils.skipUntil(_fromDevice, RECEIVE_HELLO
				      .getBytes(), 5000);
		//there may, or may not be a return line character
		//following the hello response, so flush the buffer in
		//case there is a return line in there
		_fromDevice.flush();
		return;   //Got response, so return.

	    } catch (TimeoutException te) {
		synchronized (this) {
		    try {
			wait(3000);
		    }
		    catch (InterruptedException e) {
			_logger.error("InterruptedException while in wait()");
		    }
		}
		_fromDevice.flush();
		continue;
	    }
	}
	_logger.error(maxTries+" attempts exceeded"); 
	throw new TimeoutException("completeModemHandshake(): exceeded " + 
				   maxTries + " attempts");
    }



    protected synchronized int readSample(byte[] sample)
	throws TimeoutException, IOException, Exception {
	int bytesRead = 0;
	
	
	try {
    // If the attribute is set, clean up any potential files on the node
    if(!_attributes.persistFilesNode) {	
      File [] filesToCleanup = _mclaneDirectory.listFiles();
      if (filesToCleanup != null ) {
        for (int i=0; i<filesToCleanup.length; i++) {
          if ( (filesToCleanup[i].getName().startsWith("E")) || (filesToCleanup[i].getName().startsWith("C")) || (filesToCleanup[i].getName().startsWith("A")) ) {
          	_logger.error("Cleaning up file " + filesToCleanup[i].getName() + "\n");
          	filesToCleanup[i].delete();
          } 	
        }
      }
    }
    
	 //Read in FilesdownloadedList
	  FileInputStream f_in = 
		new FileInputStream(getFileListPath());

	    ObjectInputStream obj_in = new ObjectInputStream(f_in);
	    filesDownloadedList = (List)obj_in.readObject();
	    //_logger.debug("DownloadedFiles List has " + 
	    //		   filesDownloadedList.size() + 
	    //		   " files names in it");
	}
	catch(Exception e){
	    _logger.error("List not found\n");
	    filesDownloadedList.clear(); // Clear list to start fresh since there has been data loss
	}
		
	String[] fileNames = getFileNames();

	String[] missingFiles = findMissingFiles(fileNames);
	_logger.info("number of files returned by findMissingFiles = "+missingFiles.length+"\n");

	if(missingFiles.length == 0) { // If no files need to be downloaded, return.
		_logger.debug("Nothing to download.\n");		
    setRecordType(_recordType);
	  sample="Nothing to download".getBytes();
	  return sample.length;
	}


	for (int i = 0; i < missingFiles.length; i++) {
	    _logger.debug("File names returned from missingFiles: " + missingFiles[i] + "\n");
	}
	Vector missingFilesVector = new Vector();
	
	try{
	   missingFilesVector = transferFiles(missingFiles);
	}
	catch (Exception e)  {
	    _logger.error("transferFiles() failed in readSample");
	    e.printStackTrace();
	    throw(e);
	}
	
	//Concatenate list of all files successfully uploaded from MMP controller.
	File[] allFiles = new File[missingFilesVector.size()];
	for (int i = 0; i < missingFilesVector.size(); i++) {
	  allFiles[i] = (File)missingFilesVector.elementAt(i);
  }
	
	for (int i = 0; i < allFiles.length; i++) {
	    _logger.debug("File names in allFiles: " + allFiles[i] + "\n");
	}
		
			
	//Zip up all appropriate files and dump to buffer.
	if(allFiles.length > 0) {
	  // Create zip file
	  File zipFile = zipFiles(allFiles);
	    
	  // Verify the zip file and add files to list.dat
	  if ( validateZipFile(zipFile, allFiles) ) {
	    _logger.debug("Zip file VALID. Adding files to list.dat\n");

	    for (int i = 0; i < allFiles.length; i++)  {
	      filesDownloadedList.add(allFiles[i].getName());
			}
			  
			// Get number of bytes
   	  bytesRead += dumpFileToBuffer(zipFile, sample);

			//Store List of uploaded files.
			try {
			  FileOutputStream f_out = new FileOutputStream(getFileListPath());
			  ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
			  obj_out.writeObject(filesDownloadedList);
		  }
			catch(Exception e){
			  _logger.error("List not written\n");
			}
	  }
      
	  else {
	    _logger.debug("Zip file NOT VALID\n");
	  }
	  
	  // Delete files may delete from the node and/or Can  
	  deleteFiles(allFiles);
	    
	}
	else {
    bytesRead = 0;
	}
	
	// Put telemetry can back to sleep...
	_toDevice.write(SEND_SLEEP.getBytes());
	_toDevice.flush();
	
	// ...and return
	return bytesRead;
  }


    private String[] getFileNames() {
	String fileNamesString = "";
	while (fileNamesString.length() < 5) {
	    try {
		_fromDevice.flush();
		try {
		    //let the last request for files come through and flush out
		    //before trying again.
		    synchronized (this) {
			wait(3000);
		    }
		    _fromDevice.flush();
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	    } catch (IOException e1) {
		//do nothing
		e1.printStackTrace();
	    }
	    try {
		_toDevice.write(REQUEST_RECENT_FILE.getBytes());
		_toDevice.flush();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    byte[] fileNamesBuffer = new byte[1024];
	    try {
		StreamUtils.readUntil(_fromDevice, fileNamesBuffer, "\r"
				      .getBytes(), 10000);
		String echoCheck = new String(fileNamesBuffer);
		_logger.debug("Echo check: " + echoCheck);
		if (echoCheck.startsWith("fn")) {
		    fileNamesBuffer = new byte[1024];
		    StreamUtils.readUntil(_fromDevice, fileNamesBuffer, "\r"
					  .getBytes(), 10000);
		}

	    } catch (Exception e) {
		e.printStackTrace();
		_logger.error(new String(fileNamesBuffer));
		try {
		    //let the last request files come through and flush out
		    //before trying again.
		    synchronized (this) {
			wait(3000);
		    }
		    _fromDevice.flush();
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
		continue;
	    }
	    fileNamesString = new String(fileNamesBuffer);
	    _logger.debug("fileNamesString = "+fileNamesString+"\n");
	    fileNamesString = fileNamesString.trim();
	    //The 'fn' command is included in the return string along with the
	    // filenames.
	    if (fileNamesString.startsWith("FN ") || fileNamesString.startsWith("fn ")) {
		fileNamesString = fileNamesString.substring(3);
	    }
	    _logger.debug("File names returned from fn: "
			       + fileNamesString);
	}
	return split(fileNamesString, FILE_DELIMITER);
    }


    /**
     * @param files
     * @return zip archive
     * @throws FileNotFoundException
     * @throws IOException
     */
    private File zipFiles(File[] files) throws FileNotFoundException, IOException {
	File zipFile = new File(getLocalStorageDirectory(), "temp.zip");
	ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile));
	for(int i = 0; i < files.length; i++){
	    try{
		zipStream.putNextEntry(new ZipEntry(files[i].getName()));
		InputStream fileIn = new FileInputStream(files[i]);
		int nextByte = 0;
		while((nextByte = fileIn.read()) != -1){
		    zipStream.write(nextByte);
		}
	    }catch(NullPointerException e){
		_logger.error("Caught NullPointerException (file "+i+")");
	    }
	    zipStream.closeEntry();
	}
	zipStream.close();
	return zipFile;
    }

  // Checks elements in zip file to make sure they are correct prior
  // to sending packets off
  private boolean validateZipFile(File zipToCheck, File[] files) {
  	boolean zipValid = false;
  	try {
      if (zipToCheck.exists()) {
        _logger.debug("Checking zip file\n");
        ZipInputStream z = new ZipInputStream(new FileInputStream(zipToCheck));

        ZipEntry ze ;
        int i = 0;
        while ( (null != (ze = z.getNextEntry())) && (i < files.length) ) {
          _logger.debug("Checking Zip entry: " + ze.getName() + "\n");
          if ( (files[i].getName()).equals(ze.getName()) ) {
            zipValid = true;
            i++;
          	continue;
          }
          else {
          	zipValid = false;
            _logger.error("ZIP File INVALID. Expecting: " + files[i].getName() + " Received: "  + ze.getName() + "\n");
            break;
          }  
        }
        
        // Check to see that all files were validated
        if ( (zipValid != true) && (i != files.length) ) {
        	zipValid = false;
        }      
      }
  	}
  	catch(Exception e) {
         e.printStackTrace();
    }

    return zipValid;
  }


    private void deleteFiles(File[] files)
  throws TimeoutException, IOException, Exception {
	if(!_attributes.persistFilesNode) {	
	  for (int i = 0; i < files.length; i++) {
	    files[i].delete();
	  }
  }
    
	if(!_attributes.persistFilesCan) {	
	  for (int i = 0; i < files.length; i++) {    
	    try {
        completeModemHandshake();
        _logger.debug("Got Prompt\n"); //*** DEBUG
	    }
	    catch (Exception e) {
	    	_logger.debug("NO PROMPT. Throwing exception in delete files\n"); //*** debug
		    e.printStackTrace();
		    throw (e);  
		}
	  try {
	  	_logger.debug("Requesting DEL with:" + REQUEST_DEL + files[i].getName() + LINE_TERMINATOR + "END\n");
		  _toDevice.write((REQUEST_DEL + files[i].getName() + LINE_TERMINATOR).getBytes());
		  _toDevice.flush();
	  } catch (Exception e) {
		  e.printStackTrace();
	  }

    } // end of for loop
  } // end of attribute check
    } // end of delete

    private int dumpFileToBuffer(File file, byte[] byteBuffer)
	throws IOException {
	int bytesRead;
	InputStream fileStream = new FileInputStream(file);
	int nextByte;
	for (bytesRead = 0; bytesRead < byteBuffer.length; bytesRead++) {
	    nextByte = fileStream.read();
	    if (nextByte == -1) {
		break;
	    }
	    byteBuffer[bytesRead] = (byte) nextByte;
	}
	return bytesRead;
    }

    /*
     * Run backward from files reported in most recent fn command (passed in fileNames argument) to file specified in 
     * _attributes.missingFileCount.  If _attributes.missingFileCount is negative, this indicates how 
     * far back to look, if it's positive, then it's interpreted as an absolute file number to look back to.
     * If file isn't in the filesDownloadedList, add it to the missingFiles report.
     */
    private String[] findMissingFiles(String[] fileNames) {
	List missingFiles = new ArrayList();
		
	_logger.debug("In findMissingFiles: fileNames.length = " + fileNames.length +"\n");
	_logger.debug("filesDownloadedList.size() = "+filesDownloadedList.size()+"\n");
	//		for (Iterator iter = filesDownloadedList.iterator(); iter.hasNext();) {
	//	String fileName = (String) iter.next();
	//	_logger.debug("Filenames = "+fileName+"\n");
	//}
		
		
	// For each file in fileNames, search filesDownloadedList backward
	for (int i = 0; i < fileNames.length; i++) {
 	    _logger.debug("fileNames[" +i+"] = "+fileNames[i]+"\n");
	    //Extract Prefix and Suffix  //TODO:  Make these extractions more generic
	    String Prefix = new String();
	    Prefix = fileNames[i].substring(0,1);
	    String Suffix = new String();
	    Suffix = fileNames[i].substring(8);
	 


	    // Extract filenumber 
	    String CheckFileName = new String();
	    int CheckFileNumber = getSequenceNumberFromFileName(fileNames[i]);
	    int LastFileNumberToCheck = CheckFileNumber+_attributes.missingFileCount;
	    
	    if(LastFileNumberToCheck < 0)
                {
		LastFileNumberToCheck = 0;
	        }

	    /* configure a NumberFormat object to generate the
	       formatted (padded 7 digit integer) number part of a file name
	    */
	    NumberFormat nf=NumberFormat.getInstance();
	    nf.setMaximumFractionDigits(0);
	    nf.setMaximumIntegerDigits(7);
	    nf.setMinimumIntegerDigits(7);
	    nf.setParseIntegerOnly(true);
	    nf.setGroupingUsed(false);

	    while(CheckFileNumber >= LastFileNumberToCheck)
		{
		CheckFileName = new String(Prefix + nf.format(CheckFileNumber) + Suffix);
		// _logger.debug("Check Filename = "+CheckFileName+"\n");
		if(!filesDownloadedList.contains(CheckFileName))
		    {
		    //_logger.debug("Adding "+CheckFileName+" to missingFiles\n");
		    missingFiles.add(CheckFileName);
		    }
		CheckFileNumber--;
		} 
	}		
		
		
	return (String[]) missingFiles.toArray(new String[0]);
    }

    /*
     * Extract the sequence number from a file name. 
     * All filenames have only one continous string of numbers, so iterate
     * through the characters in the filename until a number is hit, then group
     * all subsequent numbers, then stop iterating when a letter is hit.
     */
    private int getSequenceNumberFromFileName(String fileName) {
	StringBuffer number = new StringBuffer();
	boolean numberFound = false;
	for (int i = 0; i < fileName.length(); i++) {
	    if (Character.isDigit(fileName.charAt(i))) {
		numberFound = true;
		number.append(fileName.charAt(i));
	    } else if (numberFound) {
		break;
	    }
	}
	return Integer.parseInt(number.toString());
    }

    /**
     * @param byteBuffer
     * @param fileNames
     * @return (Vector)
     * @throws Exception
     */
    private Vector transferFiles(String[] fileNames)
	throws Exception 
    {
	Vector filesTransfered = new Vector();
	TOTAL_BYTES_RECEIVED = 0; 
	
	for (int i = 0; i < fileNames.length; i++) {
	    /*
	     * The xmodem transferFile method launches a separate thread to
	     * perform the transfer, but we want to wait until the download is
	     * finished before doing anything else, so I'm going to use "this"
	     * act as the notifier for waiting for completion.
	     */
	    _logger.debug("transferring " + fileNames[i]); 		
		
	  try {
		  File localFile = transferFile(fileNames[i]);  
				
		  if (!localFile.exists()) {
		    throw new Exception("File transfer failed for "
					+ fileNames[i]);
		  }
		  else {
		    // File exists so verify
		  
		    // Check file integrity
		  	boolean checksumMatches = false;
		  	try {
		    	checksumMatches = checkFileIntegrity(fileNames[i]);
			
	    		if (checksumMatches) {
		  	    // Add the file to the files transferred list
		        filesTransfered.add(localFile); 
		        TOTAL_BYTES_RECEIVED += localFile.length();
		        _logger.debug(fileNames[i] + " size is " + (int)localFile.length() + " bytes."); 	
		        if (TOTAL_BYTES_RECEIVED >= _attributes.maxBytesTransfer) {
		        	_logger.debug("Reached " + (int)TOTAL_BYTES_RECEIVED + " bytes. Maximum is: " + _attributes.maxBytesTransfer + " bytes.");
		        	break;
		        }
          }					 
	      }
	      catch(Exception e){
			    _logger.error("checkFileIntegrity threw exception");
			     e.printStackTrace();
			}	
	    }
	  }
	  catch (Exception e) {
		  _logger.error("transferFile threw exception \n" + e + "\n");
		  e.printStackTrace();
		}	
	}	 		
	   		
        	/* Be sure we're at a B> prompt to avoid possible confusion */
	try{completeModemHandshake();}
	catch (Exception ee) {
	  throw (ee);  
  }

	
	return filesTransfered;
  }

    protected byte[] getInstrumentStateMetadata() {
	String mdString = new String(getName())
	    + " does not provide instrument state information\n";

	return mdString.getBytes();
    }


    private boolean checkFileIntegrity(String fileName)
	throws TimeoutException, IOException, Exception {
        _logger.debug("Checking file integrity for " + fileName + "\n");
        String md5SumString = "";

       	/* Be sure we're at a B> prompt to avoid possible confusion */
	    try {
        completeModemHandshake();
        _logger.debug("Got Prompt\n"); //*** DEBUG
	    }
	    catch (Exception e) {
	    	_logger.debug("NO PROMPT. Throwing exception in check file integrity\n"); //*** debug
		    e.printStackTrace();
		    throw (e);  
		}

	
    // _logger.debug("Got Prompt\n"); //*** DEBUG
	
	  try {
	  	_logger.debug("Requesting MD5 with:" + REQUEST_MD5+fileName+LINE_TERMINATOR + "END\n");
		  _toDevice.write((REQUEST_MD5+fileName+LINE_TERMINATOR).getBytes());
		  _toDevice.flush();
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
	  
	  byte[] md5SumBuffer = new byte[1024];
	  
	  try {
      StreamUtils.skipUntil(_fromDevice, REQUEST_MD5
               .getBytes(), 5000);
		  
		  StreamUtils.readUntil(_fromDevice, md5SumBuffer, "\r" 
				       .getBytes(), 10000);
	
		  String echoCheck = new String(md5SumBuffer);
		  _logger.debug("Echo check: " + echoCheck);
	  	if (echoCheck.startsWith(fileName)) {
		    md5SumBuffer = new byte[1024];
		    StreamUtils.readUntil(_fromDevice, md5SumBuffer, "\r"
					  .getBytes(), 10000);
		  }

	  } catch (Exception e) {
		  e.printStackTrace();
	  	_logger.error(new String(md5SumBuffer));
		try {
		  //let the last request files come through and flush out
		  //before trying again.
		  synchronized (this) {
			  wait(3000);
		  }
		  _fromDevice.flush();
		} catch (Exception ex) {
		  ex.printStackTrace();
		}
	    }
	    md5SumString = new String(md5SumBuffer);
	    md5SumString = md5SumString.trim();
      _logger.debug("md5 Sum from MMP = " + md5SumString);


//	File dir = new File(getLocalStorageDirectory());
	FileInputStream fis = new FileInputStream(getLocalStorageDirectory()+"/"+fileName);
	BufferedInputStream bis = new BufferedInputStream(fis);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	int ch;
	while((ch = bis.read()) != -1)
          {
	  baos.write(ch);
	  }
	byte[] buffer = baos.toByteArray();
	MessageDigest algorithm = MessageDigest.getInstance("MD5");
	algorithm.reset();
	algorithm.update(buffer);
  	
	byte[] digest = algorithm.digest();
	
	StringBuffer hexString = new StringBuffer();
	for (int i=0;i<digest.length;i++)
	  {
	  hexString.append(Integer.toHexString(0xff&digest[i] | 0x100).substring(1));
	  }
	_logger.debug("md5 digest = "+hexString.toString());

  if(md5SumString.equals(((hexString.toString()).trim()))) {
	  _logger.debug("Checksum Match.");
	  return true;
  } 
  else {
  	_logger.debug("Checksum FAILS to Match!");
  	return false;
  }
	
	
	}


    /**
     * This method will download the file specified by the given
     * <code>fileName</code> from <code>xmodem</code> specified. Because of
     * the XModem implementation, the download will execute asynchronously. To
     * allow for synchronous interaction with this method, a call to
     * <code>this.notifyAll</code> is made when the download is complete.
     * Therefore, you can make a call to <code>wait()</code> after calling
     * <code>transferFile</code> and when the download is complete, the
     * <code>wait</code> ing thread will be notified.
     * 
     * @param fileName
     * @throws IOException
     */
    public File transferFile(final String fileName) throws Exception, IOException {
	final XModemPort xmodem = new XModemPort(_toDevice, _fromDevice);
	xmodem.setYmodemBat(true);
	xmodem.setYmodemG(false);
	xmodem.setTransferMode('b');
	xmodem.setModem7Bat(false);
	xmodem.setLongPack(false);
	xmodem.setMCRCMode(true);
	//xmodem.setXModemFileName(fileName); 
	
	//xmodem.openLogFile(fileName+"."+lognum);
	lognum++;
	
	_toDevice.write(new String(Y_TRANSFER + fileName + "\r")
			.getBytes());
  _toDevice.flush();

	xmodem.addXModemListener(new XModemListener() {  

		public void transferBegin(String fname, long fileSize) { 
		    _logger.debug("Beginning transfer of: " + fname + " "
				       + fileSize + " bytes");
		}

		public void transferData(long currentBytes) {
		    _logger.debug(currentBytes + " transferred");
		}

		public void transferDone() {
		    xmodem.removeXModemListener(this);
		    Exception ex = xmodem.getTransferException();
		    if (ex != null) {
			_logger.error("Xmodem ex != null inside transferDone");
			ex.printStackTrace();
			//TODO the partially downloaded file should be deleted.
		    } else {
			_logger.info("transferDone without error (at least getTransferException returned null");

		    // Check file integrity
//		  	boolean checksumMatches = false;
//		  	try {
//		    	checksumMatches = checkFileIntegrity(fileNames[i]);
//			
//	    		if (checksumMatches) {
//		  	    // Add the file to the files transferred list
//		        filesTransfered.add(localFile);  	  
//          }					 
//	    }
//	      catch(Exception e){
//			    _logger.error("checkFileIntegrity threw exception");
//			     e.printStackTrace();
//			}	
			
			
                       }
		   
				
		    synchronized (McLaneProfiler.this) {
			McLaneProfiler.this.notifyAll();
		    }
		}
	    });

	File dir = new File(getLocalStorageDirectory());
	
	try{
	    _logger.info("About to try xmodem.receive");
	    xmodem.receive(dir);
	} catch (Exception e){
	    _logger.error("XModem.recieve threw: "+e);
	    //xmodem.closeLogFile();
	    xmodem.cancel();
	  _logger.debug("Cacel called during " + fileName + "\n");
	    
	    throw (e);
	}
	//TODO if this wait times out instead of being awakened by a
	// "notify" call
	//we should probably do some sort of error recovery because the
	// modem could
	//still be trying to send the file
	synchronized (this) {
	    try {
		wait(MAX_FILE_DOWNLOAD_TIME);
	    } catch (InterruptedException e) {
		_logger.error("InterruptedException ended timeout");
	    }
	}	
	// *** Temporarily commented out 
	//xmodem.cancel();  //TODO:  Eliminate need for this line, which sends cancel to account for funny case where XYModem reports successful download but MMP controller is still sending.  The CAN-CAN-CAN does no harm when download is truly successful.  TODO
	//_logger.debug("Cacel called during " + fileName + "\n");
	
	
	
	//xmodem.closeLogFile();
	return new File(dir, fileName);
    }

    /**
     * how on earth did java make it to 1.3 without a split function? this one
     * is probably pretty inneficient, but it should get the job done
     * 
     * @param stringToSplit
     * @param stringToSplitOn
     * @return resulting strings 
     */
    public static String[] split(String stringToSplit, String stringToSplitOn) {
	if (stringToSplit == null) {
	    throw new NullPointerException();
	}
	List wordList = new ArrayList();
	int splitPosition = 0;
	int splitStringLength = stringToSplitOn.length();
	while (splitPosition < stringToSplit.length()) {
	    int nextSplitPosition = stringToSplit.indexOf(stringToSplitOn,
							  splitPosition);
	    if (nextSplitPosition == -1) {
		nextSplitPosition = stringToSplit.length();
	    }
	    wordList.add(stringToSplit.substring(splitPosition,
						 nextSplitPosition));
	    splitPosition = nextSplitPosition + splitStringLength;
	}

	return (String[]) wordList.toArray(new String[0]);
    }

    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	return new ScheduleSpecifier(DEFAULT_SAMPLE_INTERVAL);

    }

    public void setClock(long epochMsecs) throws NotSupportedException {
	//TODO can this be done? it will have to happen while the
	//RF connection is active, so my guess is no.
    }

    public SerialPortParameters getSerialPortParameters() {
	try {
	    return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					    SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	} catch (gnu.io.UnsupportedCommOperationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }

    // override base class method
    public void initializeInstrument(){
	
	// create storage directory and file list, if necessary
	if(initializeLocalStorageDirectory()==null){
	    _logger.error("Could not create mclane directory");
	}else{
	    initializeFileList();
	}
    }

    public int test() throws RemoteException {
	//TODO not yet implemented. what should be done here?
	return 0;
    }

    /** Initialize file list to persist an index of downloaded files
	Creates the file if it does not already exist.
     */
    public String initializeFileList(){

	_logger.debug("enter initializeFileList: "+_fileList);

	_fileList=null;
	_fileList = new File((getLocalStorageDirectory()+
			      File.separator+FILE_LOG_NAME));

	_logger.debug("initializing fileList "+_fileList.getPath());

	try{
	    // if it exists, use it
	    if(_fileList.exists() && !_fileList.isDirectory() && _fileList.isFile()){
		_logger.debug("fileList exists: "+_fileList.getPath());
		return _fileList.getPath();
	    }
	    // if not, try to create it
	    if(!_fileList.createNewFile()){
		    _logger.error("fileList could not be created: "+_fileList.getPath());
	    }
	    else {
	      _logger.debug("fileList created: "+_fileList.getPath());
	    }
	    return _fileList.getPath();
	}catch(IOException e){
	    _logger.error("IOException in initializeFileList: "+e);
	}

	try{
	    _fileList=null;
	    _fileList=new File(_nodeProperties.getDeviceLogDirectory()+File.separator+FILE_LOG_NAME);
	    _logger.error("attempting to create fileList in default directory: "+_fileList.getPath());
	    _fileList.createNewFile();
	    _logger.debug("fileList created in dflt dir: "+_fileList.getPath());
	    return _fileList.getPath();
	}catch(IOException e){
	    _logger.error("IOException in initializeFileList: "+e);
	}catch(MissingPropertyException m){
	    _logger.error("MissingPropertyException in initializeFileList: "+m);
	}catch(InvalidPropertyException i){
	    _logger.error("MissingPropertyException in initializeFileList: "+i);
	}

	return null;
    }

    /** create mclane directory on local file system, as necessary */
    public String initializeLocalStorageDirectory(){
	try {
	    _logger.debug("enter initializeLocalStorageDir: "+_mclaneDirectory);
	    // use existing directory, if possible
	    if(_mclaneDirectory!=null)
		if(_mclaneDirectory.exists() && _mclaneDirectory.isDirectory()){
		    _logger.debug("mclane Dir exists returning "+_mclaneDirectory.getPath());
		    return _mclaneDirectory.getPath();
		}
	    _logger.debug("mclaneDir doesn't exist of is not dir");
	    String dfltDir=_nodeProperties.getDeviceLogDirectory();
	    String base=dfltDir+File.separator+MCLANE_FILE_DIR;
	    String suffix="";
	    String p = base+suffix;
	    _mclaneDirectory=null;
	    _mclaneDirectory = new File(p);

	    // If it doesn't exist, try to create it.
	    try{
		_logger.debug("trying to create mclaneDir: "+_mclaneDirectory.getPath());
		if(!_mclaneDirectory.mkdir()){
		    _logger.debug("could not create mclaneDir");
		    if(_mclaneDirectory.exists() && !_mclaneDirectory.isDirectory()){
			_logger.debug("mclaneDir is not a directory...creating alt");
		    
			// if there is already a file with the same name as the mclane directory,
			// make a new directory with an alternative name (using "_n", where n is a
			//  a number). If numbered directory exists, use it. If other files are
			// in the way, keep bumping up the number until one is found that doesn't exist.

			for(int dirCount=0;dirCount<MAX_FILE_DIRECTORIES;dirCount++){
			    suffix=("_"+dirCount);
			    p=base+suffix;
			    _mclaneDirectory=null;
			    _mclaneDirectory=new File(p);
			    _logger.debug("trying mclaneDir alt: "+_mclaneDirectory.getPath());
			    // if it exists or can be created, we're golden
			    if(_mclaneDirectory.exists() && _mclaneDirectory.isDirectory()){
				_logger.debug("mclane directory exists and set to "+_mclaneDirectory.getPath());
				return _mclaneDirectory.getPath();
			    }
			    // if it doesn't exist or isn't a directory, try to make it
			    if(_mclaneDirectory.mkdir()){
				_logger.debug("mclane directory created and set to "+_mclaneDirectory.getPath());
				return _mclaneDirectory.getPath();
			    }
			    _logger.debug("could not set mclane directory to "+_mclaneDirectory.getPath());
			}

			// if too many directories already exist,
			// just dump the files into the log directory
			_logger.error("Could not create mclane directory: too many existing directories");
			_mclaneDirectory=null;
			_mclaneDirectory=new File(dfltDir);

			_logger.debug("mclane directory set to dflt dir "+_mclaneDirectory.getPath());
			
			return (_mclaneDirectory.getPath());
		    }
		} // else successfully created

		_logger.debug("mclane directory set: "+_mclaneDirectory.getPath());

	    }catch(SecurityException e){
		// if it can't be created
		// just dump the files into the log directory
		_mclaneDirectory=new File(dfltDir);
		_logger.error("Could not create mclane directory: IOException; set to "+_mclaneDirectory.getPath());
		return (dfltDir);
	    }


	    _logger.debug("mclane directory set to "+_mclaneDirectory.getPath());
	    // if successfully created, return path
	    return (_mclaneDirectory.getPath());
	}
	catch (MissingPropertyException e) {
	    _logger.error("getLocalStorageDirectory(): " + e);
	    return null;
	}
	catch (InvalidPropertyException e) {
	    _logger.error("getLocalStorageDirectory(): " + e);
	    return null;
	}
    }

    /**
     * @return Returns the localStorageDirectory.
     */
    public String getLocalStorageDirectory() {
	_logger.debug("checking mclaneDir "+_mclaneDirectory);

	if(_mclaneDirectory==null || !_mclaneDirectory.exists() || !_mclaneDirectory.isDirectory()){
	    _logger.debug("initializing mclaneDir ");
	    initializeLocalStorageDirectory();
	}

	_logger.debug( "mclaneDirectory set to "+_mclaneDirectory.getPath());	

	return (_mclaneDirectory.getPath());
    }

    /**
     * @return Returns the path to the fileList
     */
    public String getFileListPath() {
	_logger.debug("checking fileListPath "+_fileList);
	if(_fileList==null || !_fileList.exists() || !_fileList.isFile()){
	    _logger.debug("initializing fileList");
	    initializeFileList();
	}
	_logger.debug("fileListPath set to "+_fileList.getPath());	
	return (_fileList.getPath());
    }

	
    private void writeObject(ObjectOutputStream out) throws IOException{
	out.writeObject(filesDownloadedList);
    }
	
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
	filesDownloadedList = (List)in.readObject();
	_logger.debug("List read in has " + filesDownloadedList.size() + " files names in it");
    }

    /** 
     * Configurable instrument-specific attributes.
     * @author oreilly
     *
     */	
    class Attributes extends InstrumentServiceAttributes {
		
	Attributes(DeviceServiceIF service) {
	    super(service);
	}
	
		
	/** Keep intermediate files (Annnnnnn.DAT, Cnnnnnnn.DAT, and Ennnnnnn.DAT)
	   Default=FALSE
	*/
	
	// Keep downloaded files on the Node?
	private boolean persistFilesNode=true;
	
	// Keep downloaded files on the Telemetry CAN?
	private boolean persistFilesCan=true;
	
	/** Number of missing files to retrieve. If missingFileCount< 0, 
	    look back missingFileCount files from latest (relative), otherwise, 
	    go back to missingFileCount (absolute). It should be capped (-120?), 
	    so as not to go back too far.
	*/
	private int missingFileCount=-1;
	
  private long maxBytesTransfer = 50000; // 300000 bytes / 6 samples per day

	/** Called just after each attribute is found and set.
	    Can do validation here.
	*/
	protected void setAttributeCallback(String name, String valueString)throws InvalidPropertyException {
	    if(name.compareToIgnoreCase("persistFilesNode")==0){
		if(valueString.compareToIgnoreCase("TRUE")==0){
		    persistFilesNode=true;
		}
		if(valueString.compareToIgnoreCase("FALSE")==0){
		    persistFilesNode=false;
		}		
	    }	    

	    if(name.compareToIgnoreCase("persistFilesCan")==0){
		if(valueString.compareToIgnoreCase("TRUE")==0){
		    persistFilesCan=true;
		}
		if(valueString.compareToIgnoreCase("FALSE")==0){
		    persistFilesCan=false;
		}		
	    }	

	}
	/**
	 * Throw InvalidPropertyException if any invalid attribute values found
	 */
	public void checkValues() throws InvalidPropertyException {

	    // check individual ranges here
	 
	    if(missingFileCount<-120){
		throw new InvalidPropertyException("Invalid missingFileCount ("+
						   missingFileCount+
						   ") must be > -120");
	    }

	    // check for interdependencies
	    
	}

	/**
	 * Called when specified attribute was not found. Throw
	 * MissingPropertyException if specified attribute is mandatory.
	 * 
	 * @param attributeName
	 *            name of missing attribute
	 */
	protected void missingAttributeCallback(String attributeName)
	    throws MissingPropertyException {

	}

    }

}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seahorse;

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
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.DeviceServiceIF;
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

public class Seahorse extends PolledInstrumentService implements Instrument {

	
	
    //Default sample schedule, Default is just longer than the Carrier Detect timeout,
    //instrument service will adjust schedule as it learns this profilers connection habits.

    private static final long DEFAULT_SAMPLE_INTERVAL = 60*60*1000;  //60*60*1000;  
    private long TOTAL_BYTES_RECEIVED = 0;
	
    //Information about the files coming off the seahorse
    private static final String SCIENCE_FILENAME_PREFIX = "SH__";

    private static final String ENGINEERING_FILENAME_PREFIX = "CAST";

    private static final String SCIENCE_FILENAME_EXTENSION = ".HEX";

    private static final String ENGINEERING_FILENAME_EXTENSION = ".ENG";

    //the delimiter used between file names
    private static final String FILE_DELIMITER = ",";

    //Modem commands
    private static final String LINE_TERMINATOR = "\r";

    private static final String SEND_HELLO = "T>" + LINE_TERMINATOR;
	
    private static final String SEND_SLEEP = "slp" + LINE_TERMINATOR;

    private static final String RECEIVE_HELLO = "B>";

    private static final String REQUEST_RECENT_FILE = "fn" + LINE_TERMINATOR;

    private static final String REQUEST_MD5 = "md5 " ;
    private static final String REQUEST_DEL = "del " ;
    private static final String Y_TRANSFER = "yxfer";

    private static final int MAX_RESPONSE_BYTES = 100000;

    //the length of the response, in bytes, returnd from a REQUEST_RECENT_FILE
    // command
    private static final int RECENT_FILES_RETURN_LENGTH = 256;

    private boolean handShakeSuccess = false;

    // Log number for xmodem library internal logging mechanism
    // used in transferFile(); appended to log file name (filename.lognum).
    // Incremented each time transferFile is called.
    private int lognum = 1;

 
    //the maximum time to wait for a file to dowload across the freewave, in
    // milliseconds
    private static final int MAX_FILE_DOWNLOAD_TIME = 600000;
	
    /** Name of file containing list of downloaded files */
    private static String FILE_LOG_NAME="list.dat";

    /** Name of directory (relative to SIAM log dir) for seahorse files */
    private static String SEAHORSE_FILE_DIR="seahorse";

    private static File _seahorseDirectory=null;
    private static File _fileList=null;

    /** Maximum number of seahorse directories */
    private static int MAX_FILE_DIRECTORIES=10;

    private List filesDownloadedList = new ArrayList();
	
    private static List Connections = new ArrayList();

    // Configurable Seahorse attributes
    Attributes _attributes = new Attributes(this);

    /** This is the actual period at which the service will attempt to 
	run; it's sampleSchedule is ignored.

	This value is set dynamically,and depends on whether 
	or not the service has synchronized with the profiler buoy radio. 

	When trying to sync with the profiler radio, this value is set to
	(radioOnTimeSecs-radioConnectTimeoutSecs/2)

	When sync has been established, this value is set to
	(profilerSamplePeriodSecs-radioConnectTimeoutSecs/2)
	
	and is adjusted each time 
	Note that radioConnectTimeoutSecs also varies dynamically (see below)

     */
    private long _samplePeriodSecs = 0L;

    /** This is the length of time that the service will wait
	for the radio connection to be established.

	This value is set dynamically,and depends on whether 
	or not the service has synchronized with the profiler buoy radio. 

	When trying to sync with the profiler radio, this value is set to
	(syncMarginSecs)

	When sync has been established, this value is set to
	(sampleMarginSecs)

	(Could consider doing this as a percentage of the period length
	instead of an absolute value)
	
     */
    private long _radioConnectTimeoutSecs = 0L;

    /** Number of attempts to connect to profiler radio
	used together with syncRetryPeriods attribute to
	determine when to throw and exception if sync
	can not be established.
     */
    private int _syncRetryCounts=0;


    // log4j Logger
    private Logger _logger = Logger.getLogger(Seahorse.class);
 

    public Seahorse() throws RemoteException {
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
	return PowerPolicy.NEVER;
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
	//TODO this was copied from the WorkhorseADCP driver, which also
	//had a todo in it.
	return 1000;
    } 


    protected byte[] initSampleTerminator() {
	//TODO what is a sample terminator?
	//I don't think there is a sample termiator for the seahorse
	return new byte[0];
    }

    protected byte[] initPromptString() {
	return RECEIVE_HELLO.getBytes();
    }

    protected int initMaxSampleBytes() {
	return MAX_RESPONSE_BYTES;
    }

    /**
     * Acquire data sample from instrument, process it, log it, and return it to
     * caller.
     */
    /** Overriding acquire() is uncommon; Seahorse does so because it can potentially
	spend a long time synchronizing with the profiler radio. It would not be 
	acceptable for it to leave it's radio on all the time (act as a streaming
	instrument), so it needs to adjust it's schedule to poll for the radio link.
	It needs to do this frequently, and will fail most of the time, which would 
	generate a lot of exceptions. Overriding acquire() allows the polling logic
	to be moved to an appropriate place, in order to avoid using too much
	power or disk space. (k headley 11/2005)

     */
    public synchronized SensorDataPacket acquire(boolean logPacket) 
	throws NoDataException {
		
		assertSamplingState();
		
	if(connectRadio(_attributes.signalLine,_radioConnectTimeoutSecs,5)){
	    _logger.info("Radio connection successful");

	    // set next contact time now; 
	    // connectRadio sets up the args for call to resyncService
	    resyncService(_samplePeriodSecs,_radioConnectTimeoutSecs);
	    
	    // sample as usual
	    return super.acquire(logPacket);

	}else {
	    _logger.info("Radio connection failed; adjusting schedule to establish sync");

	    long p1=_attributes.profilerSamplePeriodSecs-_attributes.sampleMarginSecs;
	    long p2=_attributes.radioOnTimeSecs-_attributes.syncMarginSecs;
	    long maxRetries=_attributes.syncRetryPeriods*p1/p2;

	    // If we have exceeded retry count (determined by period length, specified number of
	    // periods to retry, and radioOnTime), throw exception and log error packet
	    if(_syncRetryCounts>maxRetries){
		
		// put service in ERROR state and increment error count
		setStatusError();

		// Otherwise, try to establish sync
		// connectRadio sets up the args for call to resyncService
		resyncService(_samplePeriodSecs,_radioConnectTimeoutSecs);

		incRetryCount();

		// do managePowerOff and others
		cleanupServiceState();

		// post an error packet
		StringBuffer errMsg= new StringBuffer("");
		errMsg.append(getName()+":getData(): exceeded max retries ("+maxRetries+")");
		annotate(errMsg.toString().getBytes());

		// throw exception
		throw new NoDataException(errMsg.toString());
	    }

	    // Otherwise, try to establish sync
	    // connectRadio sets up the args for call to resyncService
	    resyncService(_samplePeriodSecs,_radioConnectTimeoutSecs);

	    incRetryCount();

	    // must set correct device status for stats
	    // reporting to work; can't use setStatusOk(),
	    // since that increments _samplingCount, which 
	    // isn't appropriate here
	    _attributes.serviceStatus = Device.OK;

	    cleanupServiceState();

	    return null;
	}
    }


    protected void resyncService(long periodSecs,long marginSecs){

	// Wake up in (periodSecs-marginSecs/2)*1000 milliseconds
	// Note: be careful to avoid zero result for marginSecs<2
	long periodMillis=(periodSecs*1000-marginSecs*500);
	sync( periodMillis );

	// if we don't replace the ScheduleSpecifier, the period will be
	// wrong, causing the timeRemaining to return the wrong value
	_logger.debug("resyncService: reset scheduleSpecifier="
		      + periodMillis);
        try{
	    getDefaultSampleSchedule().setSpecifier(new ScheduleSpecifier(periodMillis));
	}catch(ScheduleParseException e){
	    _logger.error(e);
	}
	
    }

    /** Establish sync with profiler buoy radio.
	Test for Carrier Detect on signalLine retries times during timeout seconds,
	return true if CD found, false otherwise
     */
    protected boolean connectRadio(String signalLine, long timeoutSecs, int retries){

	long now=System.currentTimeMillis();
	boolean gotCD=false;
	int sleepTime=1;
	if(retries>0)
	    sleepTime=(int)(_radioConnectTimeoutSecs*1000/retries);

	_logger.debug("connectRadio: sig: "+signalLine+" tmout: "+timeoutSecs+" ret: "+ retries+" sleep: "+sleepTime);

	while( (System.currentTimeMillis()-now)<timeoutSecs*1000 ){
	    if(signalLine.equals("CD")){
		_logger.debug("connectRadio looking for carrier on CD");
		if(((SerialInstrumentPort)_instrumentPort).getCarrierDetectStatus()){
		    gotCD=true;
		    _logger.debug("connectRadio set gotCD=true");
		}
	    }
	    if(signalLine.equals("CTS")){
		_logger.debug("connectRadio looking for carrier on CTS");
		//if(((SerialInstrumentPort)_instrumentPort).getClearToSendStatus())
		if(((SerialInstrumentPort)_instrumentPort).isCTS()){
		    gotCD=true;
		    _logger.debug("connectRadio set gotCD=true");
		}
	    }

	    if(gotCD==true){
		_logger.info("connectRadio detected carrier on "+signalLine);
		_samplePeriodSecs=_attributes.profilerSamplePeriodSecs;
		_radioConnectTimeoutSecs=_attributes.sampleMarginSecs;
		_syncRetryCounts=0;
		return true;
	    }

	    // sleep between retries
	    _logger.debug("connectRadio sleeping: "+sleepTime);
	    StopWatch.delay(sleepTime);
	}

	_logger.info("connectRadio unable to detect carrier on "+signalLine);
	_syncRetryCounts++;
	_samplePeriodSecs=_attributes.radioOnTimeSecs;
	_radioConnectTimeoutSecs=_attributes.syncMarginSecs;
	return false;
    }

    protected synchronized void requestSample() throws TimeoutException,
						       Exception {
	    //Got Connection
	    Connections.add(new Connection());
					
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
	    fileNamesString = fileNamesString.trim();
	    //The 'fn' command is included in the return string along with the
	    // filenames.
	    if (fileNamesString.startsWith("FN ")) {
		fileNamesString = fileNamesString.substring(3);
	    }
	    _logger.debug("File names returned from fn: "
			       + fileNamesString);
	}
	return split(fileNamesString, FILE_DELIMITER);
    }

    protected synchronized int readSample(byte[] sample)
	throws TimeoutException, IOException, Exception {
	int bytesRead = 0;
	
		
	//Read in FilesdownloadedList
	try {
    // If the attribute is set, clean up any potential files on the node
    if(!_attributes.persistFilesNode) {	
      File [] filesToCleanup = _seahorseDirectory.listFiles();
      if (filesToCleanup != null ) {
        for (int i=0; i<filesToCleanup.length; i++) {
          if ( (filesToCleanup[i].getName().startsWith("Cast")) || (filesToCleanup[i].getName().startsWith("Sh_")) ) {
          	_logger.error("Cleaning up file " + filesToCleanup[i].getName() + "\n");
          	filesToCleanup[i].delete();
          } 	
        }
      }
    }
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
	for (int i = 0; i < fileNames.length; i++) {
	    if (!(fileNames[i].startsWith(ENGINEERING_FILENAME_PREFIX) || fileNames[i]
		  .startsWith(SCIENCE_FILENAME_PREFIX))) {
		throw new Exception("filename not recognized: " + 
				    fileNames[i]);
	    }

	}
		
	String[] missingFiles = findMissingFiles(fileNames);
	_logger.info("number of files returned by findMissingFiles = "+missingFiles.length+"\n");
	if(missingFiles.length == 0) { // If no files need to be downloaded, return.
		_logger.debug("Nothing to download.\n");		
    setRecordType(_recordType);
	  sample="Nothing to download".getBytes();
	  return sample.length;
	}
	for (int i = 0; i < missingFiles.length; i++) {
	    _logger.debug("File names returned from findMissingFiles: " + missingFiles[i] + "\n");
	}
	Vector missingFilesVector = new Vector();
	try{
	   missingFilesVector = transferFiles(missingFiles);
	}
	catch (Exception e)  {
	    _logger.error("transferFiles() failed in readSample");
	    e.printStackTrace();
	    // should we delete files here? close connection?
	    throw(e);
	}

	//if (missingFiles.length > 0) {
	//	_logger.debug("Transfering files in findMissingFiles: \n ");
	//	missingFilesArray = transferFiles(missingFiles);
	//}
		
		
	//Concatenate list of all files successfully uploaded from buoy.
	File[] allFiles = new File[missingFilesVector.size()];
	for (int i = 0; i < missingFilesVector.size(); i++) {
	  allFiles[i] = (File)missingFilesVector.elementAt(i);
	}
	for (int i = 0; i < allFiles.length; i++) {
	    _logger.debug("File names in allFiles: " + allFiles[i] + "\n");
	}
		
		
	ListIterator it = Connections.listIterator(Connections.size());

	Connection conn = (Connection) it.previous();  //Get data from most recent connection.
	conn.CloseConnection();
		
	_logger.info("Connection Time = "+conn.time_established);
	_logger.info("Conn Duration = "+conn.duration);
	_logger.info("Expected Next Connection = "+conn.expected_next_connection);
	//Based on success, set up next wake-time.
		
	//Store List of uploaded files.
	//try{
	//	FileOutputStream f_out = new FileOutputStream(getFileListPath());
	//	ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
	//	obj_out.writeObject(filesDownloadedList);
	//	}
	//	catch(Exception e){
	//		_logger.error("List not written\n");
	//	}
		
	//Zip up all appropriate files and dump to buffer.
	if(allFiles.length > 0){
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

    /**
     * @param files
     * @return zip archive of files
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
		
	_logger.debug("In findMissingFiles\n");
	_logger.debug("filesDownloadedList.size() = "+filesDownloadedList.size()+"\n");
	//		for (Iterator iter = filesDownloadedList.iterator(); iter.hasNext();) {
	//	String fileName = (String) iter.next();
	//	_logger.debug("Filenames = "+fileName+"\n");
	//}
		
		
	// For each file in fileNames, search filesDownloadedList backward
	for (int i = 0; i < fileNames.length; i++) {
	    //Set up Prefix and Suffix
	    String CheckFileName = new String();
	    int CheckFileNumber = getSequenceNumberFromFileName(fileNames[i]);
	    int LastFileNumberToCheck = CheckFileNumber+_attributes.missingFileCount;
	    if(LastFileNumberToCheck < 0){
		LastFileNumberToCheck = 0;
	    }

	    /* configure a NumberFormat object to generate the
	       formatted (padded 4 digit integer) number part of a file name
	    */
	    NumberFormat nf=NumberFormat.getInstance();
	    nf.setMaximumFractionDigits(0);
	    nf.setMaximumIntegerDigits(4);
	    nf.setMinimumIntegerDigits(4);
	    nf.setParseIntegerOnly(true);
	    nf.setGroupingUsed(false);

	    while(CheckFileNumber > LastFileNumberToCheck){
		/* 
		   //Construct CheckFileName, there HAS to be a better way!
		String Num = new String("9999");
					
		if(CheckFileNumber<10){
		    Num = ("000"+CheckFileNumber);
		} else if(CheckFileNumber<100){
		    Num = ("00"+CheckFileNumber);
		} else if(CheckFileNumber<1000){
		    Num = ("0"+CheckFileNumber);
		} else
		    Num = (""+CheckFileNumber);
		if (fileNames[i].startsWith(SCIENCE_FILENAME_PREFIX)) {
		    CheckFileName = new String(SCIENCE_FILENAME_PREFIX + Num + SCIENCE_FILENAME_EXTENSION);
		} else if (fileNames[i].startsWith(ENGINEERING_FILENAME_PREFIX)) {
		    CheckFileName = new String(ENGINEERING_FILENAME_PREFIX + Num + ENGINEERING_FILENAME_EXTENSION);
		} else {
		    throw new IllegalArgumentException(
						       "Filenames must either have the engineering or science prefix, "
						       + fileNames[i] + " had neither");
		}

		*/

		/* There IS a better way; here it is (use NumberFormat, configured above): */

		if (fileNames[i].startsWith(SCIENCE_FILENAME_PREFIX)) {
		    CheckFileName = new String(SCIENCE_FILENAME_PREFIX + nf.format(CheckFileNumber) + SCIENCE_FILENAME_EXTENSION);
		} else if (fileNames[i].startsWith(ENGINEERING_FILENAME_PREFIX)) {
		    CheckFileName = new String(ENGINEERING_FILENAME_PREFIX + nf.format(CheckFileNumber) + ENGINEERING_FILENAME_EXTENSION);
		} else {
		    throw new IllegalArgumentException(
						       "Filenames must either have the engineering or science prefix, "
						       + fileNames[i] + " had neither");
		}
					
		CheckFileNumber--;
		// _logger.debug("Check Filename = "+CheckFileName+"\n");

		if(!filesDownloadedList.contains(CheckFileName)){
		    // _logger.debug("Adding "+CheckFileName+"to missingFiles\n");
		    missingFiles.add(CheckFileName);
		}
					
	    } 
	}		
		
		
	return (String[]) missingFiles.toArray(new String[0]);
    }

    /*
     * Extract the sequence number from a file name. Both the CASTNNNN.HEX and
     * SH__NNNN.ENG have only one continous string of numbers, so iterate
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

	  try{completeModemHandshake();}  //Get prompt before next transfer
		  catch (Exception ee) {
		  	_logger.error("Complete Modem Handshake threw exception");
		    return filesTransfered;  
		}	     
	     
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
	//xmodem.openLogFile(fileName+"."+lognum);
	lognum++;
	_toDevice.write(new String(Y_TRANSFER + " " + fileName + "\r")
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
		    }
				
		    synchronized (Seahorse.this) {
			Seahorse.this.notifyAll();
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
	//xmodem.cancel();  //TODO:  Eliminate need for this line, which sends cancel to account for funny case where XYModem reports successful download but buoy is still sending.  The CAN-CAN-CAN does no harm when download is truly successful.  TODO
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
	_syncRetryCounts=0;
	_samplePeriodSecs=_attributes.radioOnTimeSecs;
	_radioConnectTimeoutSecs=_attributes.syncMarginSecs;	

	// create storage directory and file list, if necessary
	if(initializeLocalStorageDirectory()==null){
	    _logger.error("Could not create seahorse directory");
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

    /** create seahorse directory on local file system, as necessary */
    public String initializeLocalStorageDirectory(){
	try {
	    _logger.debug("enter initializeLocalStorageDir: "+_seahorseDirectory);
	    // use existing directory, if possible
	    if(_seahorseDirectory!=null)
		if(_seahorseDirectory.exists() && _seahorseDirectory.isDirectory()){
		    _logger.debug("seahorseDir exists returning "+_seahorseDirectory.getPath());
		    return _seahorseDirectory.getPath();
		}
	    _logger.debug("seahorseDir doesn't exist of is not dir");
	    String dfltDir=_nodeProperties.getDeviceLogDirectory();
	    String base=dfltDir+File.separator+SEAHORSE_FILE_DIR;
	    String suffix="";
	    String p = base+suffix;
	    _seahorseDirectory=null;
	    _seahorseDirectory = new File(p);

	    // If it doesn't exist, try to create it.
	    try{
		_logger.debug("trying to create seahorseDir: "+_seahorseDirectory.getPath());
		if(!_seahorseDirectory.mkdir()){
		    _logger.debug("could not create seahorseDir");
		    if(_seahorseDirectory.exists() && !_seahorseDirectory.isDirectory()){
			_logger.debug("seahorseDir is not a directory...creating alt");
		    
			// if there is already a file with the same name as the seahorse directory,
			// make a new directory with an alternative name (using "_n", where n is a
			//  a number). If numbered directory exists, use it. If other files are
			// in the way, keep bumping up the number until one is found that doesn't exist.

			for(int dirCount=0;dirCount<MAX_FILE_DIRECTORIES;dirCount++){
			    suffix=("_"+dirCount);
			    p=base+suffix;
			    _seahorseDirectory=null;
			    _seahorseDirectory=new File(p);
			    _logger.debug("trying seahorseDir alt: "+_seahorseDirectory.getPath());
			    // if it exists or can be created, we're golden
			    if(_seahorseDirectory.exists() && _seahorseDirectory.isDirectory()){
				_logger.debug("seahorse directory exists and set to "+_seahorseDirectory.getPath());
				return _seahorseDirectory.getPath();
			    }
			    // if it doesn't exist or isn't a directory, try to make it
			    if(_seahorseDirectory.mkdir()){
				_logger.debug("seahorse directory created and set to "+_seahorseDirectory.getPath());
				return _seahorseDirectory.getPath();
			    }
			    _logger.debug("could not set seahorse directory to "+_seahorseDirectory.getPath());
			}

			// if too many directories already exist,
			// just dump the files into the log directory
			_logger.error("Could not create seahorse directory: too many existing directories");
			_seahorseDirectory=null;
			_seahorseDirectory=new File(dfltDir);

			_logger.debug("seahorse directory set to dflt dir "+_seahorseDirectory.getPath());
			
			return (_seahorseDirectory.getPath());
		    }
		} // else successfully created

		_logger.debug("seahorse directory set: "+_seahorseDirectory.getPath());

	    }catch(SecurityException e){
		// if it can't be created
		// just dump the files into the log directory
		_seahorseDirectory=new File(dfltDir);
		_logger.error("Could not create seahorse directory: IOException; set to "+_seahorseDirectory.getPath());
		return (dfltDir);
	    }


	    _logger.debug("seahorse directory set to "+_seahorseDirectory.getPath());
	    // if successfully created, return path
	    return (_seahorseDirectory.getPath());
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
	_logger.debug("checking seahorseDir "+_seahorseDirectory);

	if(_seahorseDirectory==null || !_seahorseDirectory.exists() || !_seahorseDirectory.isDirectory()){
	    _logger.debug("initializing seahorseDir ");
	    initializeLocalStorageDirectory();
	}

	_logger.debug( "seahorseDirectory set to "+_seahorseDirectory.getPath());	

	return (_seahorseDirectory.getPath());
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
	
	/** Which line to use for signaling connection from seahorse buoy
	    Default is Carrier Detect (CD), but DPAs don't support this, so
	    the radio CD line must be wired to CTS.
	*/
	String signalLine = "CD";
		
	/** Number of seconds to wait before checking for connection
	    with profiler buoy radio.
	    The default is 2 min, which is how long the buoy radio is
	    powered on each cycle. 
	*/
	long radioOnTimeSecs = 120;
	
	/** Nominal profiler sample period */
	long profilerSamplePeriodSecs = 3600;
	
	/** Number of seconds in advance to wake up synchronizing
	    with the profiler buoy radio
	*/
	long syncMarginSecs = 10;
	
	/** Number of seconds to wake up in advance of a sample  */
	long sampleMarginSecs = 15;

	/** Number of sample periods to try to connect with radio before
	    throwing an exception.
	*/
	private int syncRetryPeriods=1;
	
	/** Keep intermediate files (SH__nnn.HEX, CASTnnn.ENG)
	   Default=FALSE
	*/
	private boolean persistFilesNode=true;
	
	// Keep downloaded files on the Telemetry CAN?
	private boolean persistFilesCan=true;
	
	/** Number of missing files to retrieve. If missingFileCount< 0, 
	    look back missingFileCount files from latest (relative), otherwise, 
	    go back to missingFileCount (absolute). It should be capped (-120?), 
	    so as not to go back too far.
	*/
	private int missingFileCount=-24;
  private long maxBytesTransfer = 50000; // 300000 bytes / 6 samples per day

	/** Called just after each attribute is found and set.
	    Can do validation here.
	*/
	protected void setAttributeCallback(String name, String valueString)throws InvalidPropertyException {
	    if(name.compareToIgnoreCase("signalLine")==0){
		if(valueString.compareToIgnoreCase("CTS")==0){
		    signalLine="CTS";
		}
		if(valueString.compareToIgnoreCase("CD")==0)
		    signalLine="CD";
	    }
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
	    if( !(signalLine.compareToIgnoreCase("CD")==0) &&
		!(signalLine.compareToIgnoreCase("CTS")==0)){
		throw new InvalidPropertyException("Invalid signalLine ("+
						   signalLine+
						   ") Valid values are CD and CTS");
	    }
	    if(radioOnTimeSecs<=0){
		throw new InvalidPropertyException("Invalid radioOnTimeSecs ("+
						   radioOnTimeSecs+
						   ") must be >0");
	    }
	    if(profilerSamplePeriodSecs<=0){
		throw new InvalidPropertyException("Invalid profilerSamplePeriodSecs ("+
						   profilerSamplePeriodSecs +
						   ") must be >0");
	    }
	    if(syncMarginSecs<=0){
		throw new InvalidPropertyException("Invalid syncMarginSecs ("+
						   syncMarginSecs+
						   ") must be >0");
	    }
	    if(sampleMarginSecs<=0){
		throw new InvalidPropertyException("Invalid sampleMarginSecs ("+
						   sampleMarginSecs+
						   ") must be >0");
	    }
	    if(syncRetryPeriods<=0){
		throw new InvalidPropertyException("Invalid syncRetryPeriods ("+
						   syncRetryPeriods+
						   ") must be >0");
	    }
	    if(missingFileCount<-120){
		throw new InvalidPropertyException("Invalid missingFileCount ("+
						   missingFileCount+
						   ") must be > -120");
	    }

	    // check for interdependencies
	    if(sampleMarginSecs>=profilerSamplePeriodSecs)
		throw new InvalidPropertyException("sampleMarginSecs>=profilerSamplePeriodSecs");
	    if(syncMarginSecs>=radioOnTimeSecs)
		throw new InvalidPropertyException("syncMarginSecs>=radioOnTimeSecs");
	    if(radioOnTimeSecs>=profilerSamplePeriodSecs)
		throw new InvalidPropertyException("radioOnTimeSecs>=profilerSamplePeriodSecs");
	    if((_attributes.radioOnTimeSecs-_attributes.syncMarginSecs)>
	    (_attributes.profilerSamplePeriodSecs-_attributes.sampleMarginSecs))
		throw new InvalidPropertyException("(_attributes.radioOnTimeSecs-_attributes.syncMarginSecs)>(_attributes.profilerSamplePeriodSecs-_attributes.sampleMarginSecs)");
	    /*
	    if()
		throw new InvalidPropertyException("");
	    if()
		throw new InvalidPropertyException("");
	    if()
		throw new InvalidPropertyException("");
	    if()
		throw new InvalidPropertyException("");
	    */
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
	    if(attributeName.compareToIgnoreCase("profilerSamplePeriodSecs")==0)
		throw new MissingPropertyException("profilerSamplePeriodSecs must be set");
	    if(attributeName.compareToIgnoreCase("radioOnTimeSecs")==0)
		throw new MissingPropertyException("radioOnTimeSecs must be set");

	}

    }

}

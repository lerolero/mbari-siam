/****************************************************************************/
/* Copyright 2002 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;

import org.mbari.siam.core.Scheduler.ScheduleKey;
import org.mbari.siam.distributed.jddac.InstrumentBlock;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NoChildrenException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NoParentException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.UnknownLocationException;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.RangeException;

/**
 * @deprecated Replaced by BaseInstrumentService, PolledInstrumentService, and
 * StreamingInstrumentService.
 * InstrumentService is the base class of an "application framework" for
 * instruments which communicate through a serial port
 * 
 * <p>
 * The application framework is a semi-complete application, providing data and
 * behavior which are common to all instrument services. To complete the implementation of a
 * specific instrument service, the programmer extends the InstrumentService
 * class, and implements or overrides several InstrumentService methods.
 * <P>
 * InstrumentService provides the following as built-in features:
 * <ul>
 * <li>Generic service and instrument initialization method
 * <li>Main control loop which invokes methods to a) request data sample from
 * the instrument, b) read the sample from the instrument, c) process the
 * sample, and d) log the sample. The subclass must implement the sample 
 * request method, but in many cases the sample retrieval and processing methods 
 * can be used "as is".
 * 
 * <li>Automatic power management of the instrument
 * </ul>
 * 
 * <p>
 * Access to instrument serial port: InstrumentService contains a
 * java.io.OutputStream for output to the instrument's CommPort, and a
 * java.io.InputStream for input from the instrument's CommPort. Subclasses can
 * use these stream objects to communicate with the instrument. Note that these
 * objects should NOT be accessed from your subclass constructor, as they
 * haven't been created yet at construction time.
 * <p>
 * Power management: Your subclass must implement methods
 * initCommunicationPowerPolicy(), and initInstrumentPowerPolicy(), which should
 * return one of the following values defined in interface
 * org.mbari.isi.interfaces.Instrument: PowerPolicy.NEVER (for self-powered
 * instruments), PowerPolicy.ALWAYS, or PowerPolicy.WHEN_SAMPLING. The subclass
 * needn't do any further power management, as the base automatically manages
 * instrument power through its DPA port based on the "power policy" specified
 * by your subclass.
 * <p>
 * The base class provides default values for a number of attributes (member
 * variables); see comments on the DEFAULT_ fields (below). These default values
 * can be overridden using the "set" methods provided for each attribute (e.g.
 * setMaxSampleTries()). For some attributes, no reasonable default value is
 * known, and must be provided by your subclass. These values are specified by
 * implementing the abstract "init" member functions defined by the base class
 * (e.g. initCommunicationPowerPolicy()).
 * <p>
 * To implement a specific instrument service, complete the following steps:
 * 
 * <p>
 * 1. Extend InstrumentService to create your service's subclass
 * <p>
 * 2. Implement any abstract InstrumentService methods (see documentation below 
 * for specific methods)
 * <p>
 * 3. If necessary, override base class initializeInstrument() method, where
 * service- and instrument-specific initialization should be done. If necessary,
 * change values of base class attributes here, using "set" methods provided.
 * <p>
 * 4. If necessary, override other base class methods
 * <p>
 * 5. If necessary, add any additional attributes/methods needed to implement
 * your instrument's service.
 * 
 * @author Tom O'Reilly, Kent Headley, Bill Baugh
 */
public abstract class InstrumentService extends DeviceService 
    implements Instrument, DeviceServiceIF, ScheduleOwner {

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(InstrumentService.class);


    // "Scratch" properties object used to hold new key/value pairs as they
    // are set by clients.
    Properties _scratchProperties = new Properties();

    /**
     * Default configuration line terminator = "\r". Change using
     * setConfigEol().
     */
    static final byte[] DEFAULT_CONFIG_EOL = "\r".getBytes();

    /**
     * Specifies the maximum number of packets that can be returned from packet
     * log in one method call. Necessary in cases where bandwidth to client is
     * limited.
     */
    static final int MAX_RETRIEVED_PACKETS = 10;

    /** Set to false until run() is successful. */
    private boolean _running = false;

    /** Indicates if run() has been successful. */
    public boolean running() {
	return _running;
    }

    /**
     * Directory where log files are written. Default is current working
     * directory.
     */
    private String _logDirectory = ".";

    /** DeviceMessagePacket. */
    protected DeviceMessagePacket _messagePacket = null;


    /////////////////////////////////////////////////////////////////////////
    // Subclasses MAY wish to override these defaults

    /** Maximum number of bytes in a data sample read from instrument. */
    private int _maxSampleBytes = 0;

    /** Instrument's "prompt" character(s). */
    private byte[] _promptString;

    /** Sample terminator */
    private byte[] _sampleTerminator;

    /** Instrument start delay. */
    //    private int _instrumentStartDelay;
    /** Buffer for raw samples. */
    private byte[] _sampleBuf;

    /** SensorDataPacket to be returned to clients. */
    protected SensorDataPacket _sensorDataPacket = null;

    /** Most recent SensorDataPacket sent */
    protected SensorDataPacket _lastSensorDataPacket = null;

    /** Most recent DataPacket sent (of any kind) */
    protected DevicePacket _lastPacket = null;

    /** Sensor log for persistent packet storage. */
    protected FilteredDeviceLog _packetLog = null;

    /** Vector of threads that are sampling the instrument. */
    private Vector _samplingThreads = new Vector();

    /** Semaphore used to synchronize threads */
    private Object _semaphore = new Object();

    /** Default record type */
    protected static final long RECORDTYPE_UNDEFINED = -1;

    protected static final long RECORDTYPE_METADATA = 0;

    protected static final long RECORDTYPE_DEFAULT = 1;

    protected long _recordType = RECORDTYPE_DEFAULT;

    private LogSampleServiceEvent _loggingEvent;
    
    private InstrumentBlock instrumentBlock;

    /**
     * Boolean flag which indicates whether to run diagnostics at sampling time.
     */
    private boolean _runSamplingDiagnostics = true;

    /** Enable sampling diagnostics. */
    public void enableSamplingDiagnostics() {
	_runSamplingDiagnostics = true;
    }

    /** Disable sampling diagnostics. */
    public void disableSamplingDiagnostics() {
	_runSamplingDiagnostics = false;
    }

    ////////////////////////////////////////////////////////////////////////
    // These methods MUST be implemented by subclasses

    // The initWhatever generate critical default values
    // which may be overridden by values specified in
    // service.properties

    /** Return initial value of instrument power policy. */
    protected abstract PowerPolicy initInstrumentPowerPolicy();

    /** Return initial value of instrument communication power policy. */
    protected abstract PowerPolicy initCommunicationPowerPolicy();

    /** Return initial value of instrument startup time in millisec. */
    protected abstract int initInstrumentStartDelay();

    /** Return initial value of DPA current limit (milliamp). */
    protected abstract int initCurrentLimit();

    /** Return initial value for instrument's sample terminator */
    protected abstract byte[] initSampleTerminator();

    /** Return initial value for instrument's "prompt" character. */
    protected abstract byte[] initPromptString();

    /**
     * Return initial value for maximum number of bytes in a instrument data
     * sample.
     */
    protected abstract int initMaxSampleBytes();

    /** Number of times power has been applied to instrument. */
    private int _nPowerRequests = 0;

    ///////////////////////////////////////////
    // Members related to ScheduleTasks
    /** ScheduleTask container (schedules) */
    protected Vector _schedules = new Vector();

    /** Name of default sample schedule */
    protected String _defaultSampleScheduleName 
	= ScheduleSpecifier.DEFAULT_SCHEDULE_NAME;

    /** Unique key used to access Scheduler entries */
    private ScheduleKey _scheduleKey = null;

    /** Basic instrument service attributes. */
    protected InstrumentServiceAttributes _instrumentAttributes = new InstrumentServiceAttributes(
												  this);

    /** Constructor. */
    public InstrumentService() throws RemoteException {
	super();
	super.setAttributes(_instrumentAttributes);
	_loggingEvent = new LogSampleServiceEvent(this, LogSampleServiceEvent.SAMPLE_LOGGED,
						  (int )getId(), null);
    }

    /** Get instrument's "prompt" character(s). */
    protected final byte[] getPromptString() {
	return _promptString;
    }

    /**
     * Set instrument's "prompt" character(s).
     * 
     * @param promptString
     *            instrument's prompt string
     */
    protected final void setPromptString(byte[] promptString) {
	_promptString = new byte[promptString.length];
	System
	    .arraycopy(promptString, 0, _promptString, 0,
		       promptString.length);
    }

    /** Get sample terminator */
    protected final byte[] getSampleTerminator() {
	return _sampleTerminator;
    }

    /**
     * Set sample terminator.
     * 
     * @param sampleTerminator
     *            instrument sample termination string
     */
    protected final void setSampleTerminator(byte[] sampleTerminator) {
	_sampleTerminator = new byte[sampleTerminator.length];
	System.arraycopy(sampleTerminator, 0, _sampleTerminator, 0,
			 sampleTerminator.length);
    }

    /** Get sampling timeout (millisec) */
    protected final long getSampleTimeout() {
	return _instrumentAttributes.sampleTimeoutMsec;
    }

    /**
     * Set sampling timeout (millisec).
     * 
     * @param timeout
     *            sampling timeout in millisec
     */
    protected final void setSampleTimeout(long timeout) throws RangeException {
	if (timeout < 0)
	    throw new RangeException("argument must be non-negative");

	_instrumentAttributes.sampleTimeoutMsec = timeout;
    }

    /**
     * Get maximum number of tries when retrieving sample from instrument.
     */
    protected final int getMaxSampleTries() {
	return _instrumentAttributes.maxSampleTries;
    }

    /**
     * Set maximum number of tries when retrieving sample from instrument.
     * 
     * @param maxTries
     *            maximum number of times to attempt sample retrieval; must be
     *            greater than 0
     */
    protected final void setMaxSampleTries(int maxTries) throws RangeException {
	if (maxTries <= 0)
	    throw new RangeException("argument must be positive");

	_instrumentAttributes.maxSampleTries = maxTries;
    }

    /** Get maximum number of bytes in a data sample read from instrument. */
    protected final int getMaxSampleBytes() {
	return _maxSampleBytes;
    }

    /**
     * Set maximum number of bytes in a raw data sample read from instrument.
     * Allocates sample buffer (retrieved by getSampleBuf()).
     * 
     * @param maxBytes
     *            maximum number of bytes in a raw sample
     */
    protected final void setMaxSampleBytes(int maxBytes) throws RangeException {
	// NOTE: We currently do not enforce a maximum size of this buffer.
	if (maxBytes <= 0)
	    throw new RangeException("argument must be positive");

	_maxSampleBytes = maxBytes;
	// This for the GC, if _sampleBuf already exists
	_sampleBuf = null;
	_sampleBuf = new byte[_maxSampleBytes];
	/*
	 * This should be independend of setting MaxSampleBytes Property...
	 * Having it here causes initialization ordering problems (sensor ID is
	 * not set when this is called, causing data packets to retain id=0.
	 * Moving to run() _sensorDataPacket = new SensorDataPacket(getId(),
	 * _maxSampleBytes);
	 */
    }

    /** Get raw sample buffer. */
    protected final byte[] getSampleBuf() {
	return _sampleBuf;
    }

    /** Get last sample packet. */
    public final SensorDataPacket getLastSample() throws NoDataException {
	if (_lastSensorDataPacket == null)
	    throw new NoDataException("getLastSample: Last Sample is NULL");
	return _lastSensorDataPacket;
    }

    /** Get power channel's current limit. */
    protected final int getCurrentLimit() {
	return _instrumentAttributes.currentLimitMa;
    }

    /**
     * Set power channel's current limit. Breaker will trip if limit is
     * exceeded.
     * 
     * @param milliamps
     *            current limit in milliamps
     */
    protected final void setCurrentLimit(int milliamps) throws RangeException {
	if (milliamps <= 0)
	    throw new RangeException("argument must be positive");

	_instrumentAttributes.currentLimitMa = milliamps;
    }

    /** Get instrument power policy. */
    protected final PowerPolicy getInstrumentPowerPolicy() {
	return _instrumentAttributes.powerPolicy;
    }
    
    public InstrumentBlock getInstrumentBlock() {
        return instrumentBlock;
    }
    
    /*
     * Sets the JDDAC Function block used for post-processing samples. (For 
     * exmple, for event-resoponse or Summaryizing)
     *
     *@param instrumentBlock THe instrument block used for processing
     */
    public void setInstrumentBlock(InstrumentBlock instrumentBlock) {
        this.instrumentBlock = instrumentBlock;
    }

    /**
     * Set instrument power policy.
     */
    protected final void setInstrumentPowerPolicy(PowerPolicy policy) {
	_instrumentAttributes.powerPolicy = policy;
    }

    /** Get instrument communication power policy. */
    protected final PowerPolicy getCommunicationPowerPolicy() {
	return _instrumentAttributes.commPowerPolicy;
    }

    /**
     * Set instrument communication power policy.
     */
    protected final void setCommunicationPowerPolicy(PowerPolicy policy) {
	_instrumentAttributes.commPowerPolicy = policy;
    }

    /** Get instrument startup time */
    protected final int getInstrumentStartDelay() {
	return _instrumentAttributes.startDelayMsec;
    }

    /**
     * Set instrument startup time
     * 
     * @param millisec
     *            instrument startup delay in milliseconds
     */
    protected final void setInstrumentStartDelay(int millisec)
	throws RangeException {
	if (millisec < 0)
	    throw new RangeException("argument must be non-negative");

	_instrumentAttributes.startDelayMsec = millisec;
    }

    /**
     * Load defaults for member variables using abstract methods. Values
     * specified in service.properties may override some of these.
     */
    public void initializeDriverDefaults() throws InitializeException {

	// Set subclass-specified values of uninitialized member variables
	try {
	    // values in service.properties override these
	    setPromptString(initPromptString());
	    setSampleTerminator(initSampleTerminator());
	    setMaxSampleBytes(initMaxSampleBytes());
	    setCurrentLimit(initCurrentLimit());
	    setInstrumentPowerPolicy(initInstrumentPowerPolicy());
	    setCommunicationPowerPolicy(initCommunicationPowerPolicy());
	    setInstrumentStartDelay(initInstrumentStartDelay());
	} catch (RangeException e) {
	    throw new InitializeException("RangeException: " + e.getMessage());
	}

    }

    /**
     * Initialize service and instrument. This method must be invoked by outside
     * entity before data acquisition starts. This method is declared 'final' to
     * guarantee basic initialization
     */
    public synchronized final void prepareToRun() 
	throws InitializeException, InterruptedException {

	super.prepareToRun();

	_messagePacket = 
	    new DeviceMessagePacket(_instrumentAttributes.isiID);

	setStatusInitial();

	// Create (reusable ) data packet
	_sensorDataPacket = new SensorDataPacket(getId(), _maxSampleBytes);

	// Set subclass-specified values of uninitialized member variables
	try {

	    _log4j.debug("Service " + new String(getName())
			  + " initializing port");

	    _instrumentPort.setCurrentLimit(getCurrentLimit());

	} catch (RangeException e) {
	    throw new InitializeException("RangeException: " + e.getMessage());
	} catch (NotSupportedException e) {
	    _log4j.debug("setCurrentLimit() not supported for this port");
	}

	_log4j.debug("run(): Done with port initialization");

	// Do subclass-specific initialization
	try {
	    // Turn on port power/comms; may be needed for initialization.
	    managePowerWake();

	    _log4j.info("initializeInstrument() for " + new String(getName()));

	    initializeInstrument();

	    _log4j.info("done with initializeInstrument() for "
			 + new String(getName()));
	} catch (Exception e) {
	    throw new InitializeException("initializeInstrument() failed: "
					  + e.getMessage());
	}

	// Set subclass-specified values of uninitialized member variables

	// old sample schedule...
	try {
	    _log4j.debug("run() - initialize schedules");
	    // get unique id to reference schedules
	    _scheduleKey = Scheduler.getScheduleKey(this);

	    // Create the default sampling schedule if it doesn't exist
	    // (i.e. hasn't been specified by a property)
	    if (_instrumentAttributes.sampleSchedule == null)
		_instrumentAttributes.sampleSchedule = 
		    createDefaultSampleSchedule();

	    ScheduleTask st = createTask(_defaultSampleScheduleName,
					 _instrumentAttributes.sampleSchedule, 
					 this);

	    _schedules.removeAllElements();
	    _schedules.add(st);

	    // Register the schedules with the scheduler;
	    // does not start them yet
	    Scheduler.getInstance().setSchedules(_schedules);

	} catch (ScheduleParseException e) {
	    throw new InitializeException("ScheduleParseException: "
					  + e.getMessage());
	}

	_log4j.debug("creating metadataPacket...");

	try {
	    // generate and log complete metadata state packet
	    getMetadata("Service Init".getBytes(), MDATA_ALL, true);
	} catch (Exception e) {
	    _log4j.warn("problem creating metadataPacket...that's OK " + e);
	}
	_log4j.debug("done creating metadataPacket...");

	// Turn off port power/comms (subclass might have turned 'em on)
	managePowerSleep();

	setStatusOk();
	_running = true;

	// Now reset the schedule tasks 
	// DO THIS ONLY after setting _running to true, to prevent race
	_log4j.debug("InstrumentService.run() - reset schedule tasks");
	Iterator iterator = getAllSchedules().iterator();
	while (iterator.hasNext()) {
	    ScheduleTask task = (ScheduleTask) iterator.next();

	    _log4j.debug("InstrumentService: reset timer for task "
			  + task.getOwnerName() + "#"
			  + task.getOwner().getScheduleKey().value() + ":"
				  + task.getName());

	    task.resetTimer();

	    _log4j.debug("InstrumentService: task timer has been reset");
	}

	
	return;
    }

    /**
     * Prepare the device for sampling; called before requestSample(). By default
     this method does nothing, and may be overridden in the subclass.
     */
    protected void prepareToSample() throws Exception {
	//does nothing here
    }

    /**
     * Called after sample has been acquired, processed and logged. By default 
     this method does nothing, an may be overridden in the subclass.
     */
    protected void postSample() {
	//does nothing here
    }

    /**
     * Acquire data sample from instrument, process it, log it, and return it to
     * caller.
     */
    public synchronized SensorDataPacket acquireSample(boolean logPacket) 
	throws NoDataException {

	Thread thisThread = Thread.currentThread();

	// Get the semaphore, which shutdown thread will also contend for
	synchronized (_semaphore) {

	    // Service is shutting down - throw exception
	    if (getStatus() == Device.SHUTDOWN) {
		throw new NoDataException("Service in shutdown state");
	    }
	    // Add this current thread to list of sampling threads
	    _log4j.warn("TEST - add to samplingThreads vector");
	    _samplingThreads.add(thisThread);
	    _log4j.debug("samplingThread count = " + _samplingThreads.size());
	    _log4j.debug("totalmem:" + Runtime.getRuntime().totalMemory() +
			 ", freemem=" + Runtime.getRuntime().freeMemory());

	}
	
	SensorDataPacket packet = null;

	try {
	    packet =  acquire(logPacket);
	}
	catch (NoDataException e) {
	    throw e;
	}
	finally {
	    // Done - remove from list of sampling threads
	    synchronized (_semaphore) {
		if (!_samplingThreads.remove(thisThread)) {
		    _log4j.error("Didn't remove sampling thread");
		}
	    }
	}

	return packet;
    }


    /** Do overhead tasks needed to prepare for sample cycle. 
	It is broken out into a separate method to allow other
	methods (e.g., when a service wants to override acquire())
	to use the same code without cutting and pasting.
     */
    public synchronized void initServiceState()
	throws NoDataException {

	if (getStatus() == Device.SUSPEND) {
	    throw new NoDataException("service is suspended");
	}


	// Instrument isn't been initialized yet
	if (!_running) {
	    try {
		prepareToRun();
	    } catch (Exception e) {
		annotate("ERR: initialize error".getBytes());
		setStatusError();
		throw new NoDataException("run() failed: " + e.getMessage());
	    }
	}

	// put service into SAMPLING state
	setStatusSampling();

	// turn on communications power
	managePowerWake();

	return;
    }

    /** Do overhead tasks needed to clean up after sample cycle.
	It is broken out into a separate method to allow other
	methods (e.g., when a service wants to override acquire())
	to use the same code without cutting and pasting.
     */
    public synchronized void cleanupServiceState(){

	// turn off communications power
	managePowerSleep();
	
	return;
    }

    /**
     * Acquire data sample from instrument, process it, and put data into output
     * queue. Call sequence is requestSample(), readSample(), and
     * processSample(). If these are successful, output is put into outbound
     * message queue.
     * 
     * @param logSample
     *            log data if true
     */
    public synchronized SensorDataPacket acquire(boolean logSample)
	throws NoDataException {

	initServiceState();

	String name = new String(getName());

	//prepare device for sampling
	try {
	    prepareToSample();
	} catch (Exception e) {
	    setStatusError();
	    managePowerSleep();
	    _log4j.error(name + e.getMessage());
	    annotate("ERR: while preparing to sample".getBytes());
	    throw new NoDataException(e.getMessage());
	}

	int tries;
	StringBuffer errMsg = new StringBuffer("");

	for (tries = 0; tries < _instrumentAttributes.maxSampleTries; tries++) {

	    try {
		_log4j.debug(name + ": calling requestSample()\n");
		// Send sample request to instrument
		requestSample();

		// Read raw sample from instrument
		int nBytes;

		_log4j.debug(name + ": calling readSample()\n");
		if ((nBytes = readSample(_sampleBuf)) <= 0) {
		    _log4j.warn(name + ": readSample() failed");
		    errMsg.append("readSample returned 0 bytes;");
		    incRetryCount();
		    continue;
		}

		validateSample(_sampleBuf, nBytes);

		// Process the sample, generate TimeStampedData object
		// (By default, base class' processSample() just
		// puts raw data in output object)
		_log4j.debug(name + ": calling processSample() \n");
		SensorDataPacket dataPacket = processSample(_sampleBuf, nBytes);

		// Allow sub-class to deal w/instrument after sampling
		// complete
		postSample();

		// turn off power
		managePowerSleep();

		if (logSample) {
		    logPacket(dataPacket);
		}
                
        /*
         * Execute any JDDAC FunctionBlocks associated with this Service
         */
        InstrumentBlock block = getInstrumentBlock();
        if (block != null) {
            block.processDevicePacket(dataPacket);
        }

		setStatusOk();

		// Don't really need to send every packet
		// to the console, except for debugging.
		// Some services implement printData 
		// using System.x.print methods, rather
		// than log4j.
		//printData(dataPacket.dataBuffer());

		return dataPacket;
	    } catch (InvalidDataException e) {
		retryPuck();
		incRetryCount();
		_log4j.warn(name + 
			     ".acquireSample() InvalidDataException "
			     + e);
		errMsg.append("InvalidData;");
				
	    } catch (TimeoutException e) {
		retryPuck();
		incRetryCount();
		_log4j.warn(name + ".acquireSample(): TimeoutException");
		errMsg.append("Timeout;");
	    } catch (IOException e) {
		retryPuck();
		incRetryCount();
		_log4j.warn(name + ".acquireSample(): IOException", e);
		errMsg.append("IOException;");
	    }

	    catch (Exception e) {
		retryPuck();
		incRetryCount();
		_log4j.warn(name + ".acquireSample(): Exception", e);
		errMsg.append("Exception;");
	    }

	    // other Exceptions fly past into the caller
	    Thread.yield();
	}

	// Failed to acquire and process sample.
	setStatusError();

	// Allow sub-class to deal w/ instrument after sampling complete
	postSample();

	// turn off power
	managePowerSleep();

	// Throw NoDataException
	_log4j.error(name + ": Retry limit exceeded (retries=" + 
		      tries + ")");

	errMsg.append("retry limit exceeded");
	errMsg.insert(0, "ERR: ");
	annotate(errMsg.toString().getBytes());

	String retryMsg = name + ": Retry limit exceeded (retries=" + tries
	    + ")";

	throw new NoDataException(retryMsg);
    }


    /**
     * Prepare instrument for sampling; connect comms and turn on power. Uses
     * _powerPolicy to determine appropriate action. Instrument and comms power
     * are lumped together (no immediate need to have separate power policy for
     * comms); may need to change.
     */
    protected void managePowerWake() {

	if (_instrumentAttributes.powerPolicy == PowerPolicy.NEVER) {
	    _instrumentPort.disconnectPower();
	} 
	else {
	    _log4j.debug("managePowerWake() - connect power");
	    if (powerOn() == Device.ERROR) {
		return;
	    }
	    _nPowerRequests++;
	}

	if (_instrumentAttributes.commPowerPolicy == PowerPolicy.NEVER) {
	    _instrumentPort.disableCommunications();
	} else {
	    _log4j.debug("managePowerWake() - enable comms");
	    _instrumentPort.enableCommunications();
	}

	if (_instrumentAttributes.powerPolicy == PowerPolicy.WHEN_SAMPLING
	    || 
	    (_nPowerRequests == 1 && 
	     _instrumentAttributes.powerPolicy == PowerPolicy.ALWAYS)) {

	    _log4j.debug("managePowerWake() - powerOnCallback");
	    powerOnCallback();
	}

	/** Get diagnostic information before sampling. */
	if (_runSamplingDiagnostics) {
	    // Get instrument port diagnostics
	    _log4j.debug("managePowerWake() - getStatusMessage()");
	    _instrumentPort.getStatusMessage();

	    try {
		// Run parent diagnostics
		_log4j.debug("managePowerWake() - runDiagnostics()");
		_parentNode.runDiagnostics(new String("dev " + getId() + " powerOn"));
	    } catch (Exception e) {
		_log4j.info("Exception from Parent.runDiagnostics(): "
			     + e.getMessage());
	    }
	    _log4j.debug("managePowerWake() - done.");
	}
    }

    /** Turn on instrument power */
    public int powerOn() {
	if (_parentNode.powerAvailable(_instrumentAttributes.nominalCurrentMa)) {
	    _instrumentPort.connectPower();
	    return Device.OK;
	}
	else {
	    _log4j.error("powerOn() - power not available from parent");
	    return Device.ERROR;
	}
    }

    /**
     * Return instrument to lowest possible power state between samples
     */
    protected void managePowerSleep() {

	if (_instrumentAttributes.powerPolicy == PowerPolicy.WHEN_SAMPLING
	    || _instrumentAttributes.powerPolicy == PowerPolicy.NEVER) {
	    _instrumentPort.disconnectPower();
	} else {
	    // Always on
	    _instrumentPort.connectPower();
	}

	if (_instrumentAttributes.commPowerPolicy == PowerPolicy.WHEN_SAMPLING
	    || _instrumentAttributes.commPowerPolicy == PowerPolicy.NEVER) {
	    try {
		_toDevice.flush();
	    } catch (Exception e) {
		_log4j.error("Error flushing output");
	    }
	    _instrumentPort.disableCommunications();
	} else {
	    // Always on
	    _instrumentPort.enableCommunications();
	}

	powerOffCallback();
    }

    /** Turn off instrument power. */
    public int powerOff() {
	_instrumentPort.disconnectPower();
	return Device.OK;
    }

    /** Request a data sample from instrument. */
    protected abstract void requestSample() throws TimeoutException, Exception;


    /**
     * Read raw sample bytes from serial port into buffer, return number of
     * bytes read. Reads characters from serial port until sample terminator
     * string is encountered.
     * 
     * @param sample
     *            output buffer
     */
    protected int readSample(byte[] sample) throws TimeoutException,
						   IOException, Exception {

	// In the default case, this will be RECORDTYPE_DEFAULT
	setRecordType(_recordType);

	return StreamUtils.readUntil(_fromDevice, sample, _sampleTerminator,
				     _instrumentAttributes.sampleTimeoutMsec);
    }

    /**
     * This method can optionally be overriden so the sub-class can determines
     * the validity of the sampled bytes. If an error is detected an
     * InvalidDataException is thrown.
     * 
     * @param _sampleBuf
     *            sample buffer containing data to validate
     * @param nBytes
     *            Number of bytes of data to validate
     * 
     * @exception InvalidDataException
     *                thrown if data is found to be invalid
     */
    protected void validateSample(byte[] _sampleBuf, int nBytes)
	throws InvalidDataException {

    }

    /**
     * Process raw sample bytes, return TimeStampedData object. By default, raw
     * sample is encapsulated in TimeStampedData object, and no additional
     * processing is done.
     * 
     * @param sample
     *            raw sample
     * @param nBytes
     *            number of bytes in raw sample
     */
    protected SensorDataPacket processSample(byte[] sample, int nBytes)
	throws Exception {

	// Set timestamp
	_sensorDataPacket.setSystemTime(System.currentTimeMillis());

	// Set record type; subclasses must set _recordType when they
	// submit data other than the default record format
	_sensorDataPacket.setRecordType(_recordType);

	byte[] buf;
	// Make buffer that is exactly the right size
	if (nBytes != sample.length) {
	    buf = new byte[nBytes];
	    System.arraycopy(sample, 0, buf, 0, nBytes);
	} else
	    buf = sample;

	_sensorDataPacket.setDataBuffer(buf);

	return _sensorDataPacket;
    }

    /** Parse the SensorDataPacket, returning an Object of a type that can (presumably)
	be interpreted by the invoker.  This is intended to be used with the
	InstrumentRegistry.  The consumer of information initially invokes
	InstrumentRegistry.registerDataCallback().  It then receives a
	SensorDataPacket every time this service generates one.  It can then
	use pass this SensorDataPacket to the parseDataPacket() method, receiving
	back an Object that it knows how to use.  This default implementation
	just returns a null Object.
    */
    public Object parseDataPacket(SensorDataPacket packet)
	throws InvalidDataException, RemoteException
    {
	return(null);
    }

    /**
     * Get requested metadata state components in a byte array containing
     * serialized State and StateAttribute objects.
     */
    public byte[] getMetadataPayload(int components, String[] attributeList)
	throws RemoteException, InvalidPropertyException {

	StringBuffer output = new StringBuffer(10000);

	if ((components & INSTRUMENT_STATE) > 0) {
	    output.append(MetadataPacket.DEVICE_INFO_TAG + "\n");
	    // Apply power policy to instrument
	    managePowerWake();
	    // Synchronize to prevent serial port contention 
	    synchronized (this) {
		output.append(new String(getInstrumentStateMetadata()) + "\n"
			      + MetadataPacket.DEVICE_INFO_CLOSE_TAG + "\n");
	    }
	    managePowerSleep();
	}

	if ((components & SERVICE_ATTRIBUTES) > 0) {
	    output.append(MetadataPacket.SERVICE_ATTR_TAG
			  + "\n"
			  + ServiceAttributes.toPropertyStrings(_instrumentAttributes
								.toProperties(attributeList))
			  + MetadataPacket.SERVICE_ATTR_CLOSE_TAG + "\n");
	}

	if ((components & SERVICE_PROPERTIES) > 0) {
	    String sp=new String(getServicePropertiesBytes());
	    output.append(MetadataPacket.SERVICE_PROP_TAG
			  + "\n"
			  + sp
			  + MetadataPacket.SERVICE_PROP_CLOSE_TAG + "\n");
	}

	if ((components & SERVICE_XML) > 0) {
	    output.append(MetadataPacket.INSTRUMENT_DOC_TAG + "\n");

	    try {
		BufferedReader xmlReader = new BufferedReader(new FileReader(
									     _serviceXMLPath));

		String line = null;
		while ((line = xmlReader.readLine()) != null) {
		    output.append(line + "\n");
		}
		xmlReader.close();

	    } catch (IOException e) {
		_log4j.info("getMetadataPayload(): unable to access XML file");
		output.append("unable to access XML file");
	    }
	    output
		.append("\n" + MetadataPacket.INSTRUMENT_DOC_CLOSE_TAG
			+ "\n");

	}
	return new String(output).getBytes();
    }

    /**
     * Create, return and optionally log a metadata packet
     */
    public MetadataPacket getMetadata(byte[] cause, int components,
				      boolean logPacket) throws RemoteException {

	MetadataPacket packet = null;
	try {
	    packet = getMetadata(cause, components, null, logPacket);
	} catch (InvalidPropertyException e) {
	    _log4j.error("getMetadata(): " + e);
	}
	return packet;
    }

    /**
     * Create, return, and optionally log metadata packet; if service attributes
     * are requested, then include only specified attributes.
     * 
     * @param cause
     * @param components
     * @param attributeNames
     * @param logPacket
     * @return one or more types of metadata (MetadataPacket)
     */
    protected MetadataPacket getMetadata(byte[] cause, int components,
					 String[] attributeNames, boolean logPacket) throws RemoteException,
											    InvalidPropertyException {

	MetadataPacket packet = new MetadataPacket(getId(), cause,
						   getMetadataPayload(components, attributeNames));

	packet.setSystemTime(System.currentTimeMillis());

	packet.setParentId(_parentNode.getParentId());

	if (logPacket) {
	    logPacket(packet);
	}
	return packet;
    }

    /** Get the contents of a file and return them as a byte array */
    public byte[] getFileBytes(String file) throws FileNotFoundException,
						   IOException, NullPointerException {

	byte retval[];
	FileInputStream fis;
	ByteArrayOutputStream bos;
	fis = new FileInputStream(file);
	bos = new ByteArrayOutputStream();

	while (fis.available() > 0) {
	    bos.write(fis.read());
	}

	fis.close();
	bos.close();

	retval = bos.toByteArray();
	return retval;
    }

    /** Get Service XML file, returned as a byte array */
    public byte[] getServiceXMLBytes() {
	byte retval[];
	try {
	    retval = getFileBytes(new String(_serviceXMLPath));
	} catch (FileNotFoundException e) {
	    _log4j.debug("getServiceXMLBytes: File not found: "
			  + new String(_serviceXMLPath) + "\n" + e);
	    retval = ("getServiceXMLBytes: File not found: "
		      + new String(_serviceXMLPath) + "\n").getBytes();
	} catch (IOException e) {
	    _log4j.debug("getServiceXMLBytes: IOException reading "
			  + new String(_serviceXMLPath) + "\n" + e);
	    retval = ("getServiceXMLBytes: IOException reading "
		      + new String(_serviceXMLPath) + "\n").getBytes();
	} catch (NullPointerException e) {
	    _log4j.debug("getServiceXMLBytes: Null Pointer Exception: "
			  + "serviceXMLPath" + "\n" + e);
	    retval = ("getServiceXMLBytes: Null Pointer Exception:"
		      + "ServiceXMLPath\n").getBytes();
	} catch (Exception e) {
	    _log4j.debug("getServiceXMLBytes: Exception:\n" + e);
	    retval = ("getServiceXMLBytes: Exception:\n" + e + "\n").getBytes();
	}
	return retval;
    }

    /** Get Service Properties file, returned as a byte array */
    public byte[] getServicePropertiesBytes() {
	byte retval[];
	try {
	    retval = getFileBytes(new String(_servicePropertiesPath));
	} catch (FileNotFoundException e) {
	    _log4j.debug("getServicePropertiesBytes: File not found: "
			  + new String(_servicePropertiesPath));
	    retval = ("getServicePropertiesBytes: File not found: "
		      + new String(_servicePropertiesPath) + "\n").getBytes();
	} catch (IOException e) {
	    _log4j.debug("getServicePropertiesBytes: IOException reading "
			  + new String(_servicePropertiesPath) + "\n" + e);
	    retval = ("getServicePropertiesBytes: IOException reading "
		      + new String(_servicePropertiesPath) + "\n").getBytes();
	} catch (NullPointerException e) {
	    _log4j.debug("getServicePropertiesBytes: Null Pointer Exception: "
			  + "servicePropertiesPath");
	    retval = ("getServicePropertiesBytes: Null Pointer Exception:"
		      + "ServicePropertiesPath\n").getBytes();
	} catch (Exception e) {
	    _log4j.debug("getServicePropertiesBytes: Exception: " + e);
	    retval = ("getServicePropertiesBytes: Exception:\n" + e).getBytes();
	}

	return retval;
    }

    /** Get Service Cache file, returned as a byte array */
    public byte[] getServiceCacheBytes() {
	byte retval[];
	try {
	    retval = getFileBytes(new String(_serviceCachePath));
	} catch (FileNotFoundException e) {
	    _log4j.debug("getServiceCacheBytes: File not found: "
			  + new String(_serviceCachePath));
	    retval = ("getServiceCacheBytes: File not found: "
		      + new String(_serviceCachePath) + "\n").getBytes();
	} catch (IOException e) {
	    _log4j.debug("getServiceCacheBytes: IOException reading "
			  + new String(_serviceCachePath) + "\n" + e);
	    retval = ("getServiceCacheBytes: IOException reading "
		      + new String(_serviceCachePath) + "\n").getBytes();
	} catch (NullPointerException e) {
	    _log4j.debug("getServiceCacheBytes: Null Pointer Exception: "
			  + "serviceCachePath");
	    retval = ("getServiceCacheBytes: Null Pointer Exception:"
		      + "ServiceCachePath\n").getBytes();
	}

	return retval;
    }


    /**
     * Get instrument- and service-specific metadata. By default, this method
     * does nothing, and can be overridden. This method should return any available
     * configuration information that is available from the instrument device.
     * Note that this method should NOT power-manage the device, as that is the
     * responsibility of the caller.
     */
    protected byte[] getInstrumentStateMetadata() {
	// Do nothing. Leave it to subclasses.
	// Subclasses should, to whatever degree possible, provide
	// all state information by directly reading them from the instrument,
	// i.e., current values of metadata items
	// represented in service.properties or the service's XML, or other
	// information relevant to the sampling context that can be obtained
	// from the instrument.
	// The idea here is to directly read the instrument, which may not agree
	// with other versions of the instrument state variables.

	String mdString = new String(getName())
	    + " does not provide instrument state information\n";

	return mdString.getBytes();
    }

    /**
     * Do instrument- and service-specific initialization. By default, this method
     * does nothing, and may be overridden.
     */
    protected void initializeInstrument() throws InitializeException, Exception {

	// Do nothing. Leave it to subclasses.
    }

    /** Return Location of device. NOT YET IMPLEMENTED. */
    public Location getLocation() throws UnknownLocationException {
	throw new UnknownLocationException("Not implemented");
    }

    /**
     * Put annotation into device data stream. Annotation will be automatically
     * time-tagged by host.
     */
    public synchronized void annotate(byte[] annotation) {

	//create wrapper for message packet
	String s = new String(new String(annotation));

	_log4j.debug("Annotation: " + s);

	_messagePacket.setMessage(System.currentTimeMillis(), s.getBytes());
	logPacket(_messagePacket);
    }

    /** Return byte-string representation of instrument sampling schedule. */
    public final byte[] getSampleSchedule() {

	if (_instrumentAttributes.sampleSchedule.getScheduleTime() != null)
	    return _instrumentAttributes.sampleSchedule.getScheduleTime()
		.getBytes();
	else
	    return "".getBytes();
    }

    /** Return parent Device. */
    public Device getParent() throws NoParentException {
	throw new NoParentException("Not implemented");
    }

    /** Return child Devices. */
    public Device[] getChildren() throws NoChildrenException {
	throw new NoChildrenException("Not implemented");
    }

    /**
     * Initialize service parameters. Called immediately after service is
     * instantiated.
     */
    public void initialize(NodeProperties nodeProperties, Parent parent,
			   InstrumentPort port, ServiceSandBox sandBox,
			   String serviceXMLPath, String servicePropertiesPath,
			   String cachedServicePath)

	throws MissingPropertyException, InvalidPropertyException,
	       PropertyException, InitializeException, IOException,
	       UnsupportedCommOperationException {

	super.initialize(nodeProperties, parent, port, sandBox,
			 serviceXMLPath, servicePropertiesPath, cachedServicePath);

	// Initialize parameters to default values
	_log4j.debug("Initialize service defaults");
	initializeDriverDefaults();

	// Get attribute values from configuration properties;
	// these override defaults
	_log4j.debug("Get attribute values from properties");
	_instrumentAttributes.fromProperties(sandBox.getServiceProperties(), 
					     true);

	// Create device log
	_log4j.debug("Create the packet log");
	createPacketLog();

    }

    /** Create device packet log. */
    private void createPacketLog() throws MissingPropertyException,
					  InvalidPropertyException, FileNotFoundException, IOException {

	// Get name of log directory
	Properties systemProperties = System.getProperties();
	String siamHome = systemProperties.getProperty("siam_home").trim();
	String logDirectory = _nodeProperties.getDeviceLogDirectory();

	_logDirectory = siamHome + File.separator + logDirectory;
	PacketFilter[] filters = new PacketFilter[1];
	filters[0] = new PacketSubsampler(0, DevicePacket.ALL_TYPES);
	_packetLog = new FilteredDeviceLog(getId(), _logDirectory, filters);
    }

    /**
     * Called after power is applied to instrument; return when instrument is
     * ready for use. By default, sleeps for 'instrumentStartDelay'
     * milliseconds.
     */
    protected void powerOnCallback() {

	try {
	    _log4j.debug(new String(getName())
			  + ": default waitForInstrumentStart(); " + "sleep for "
			  + getInstrumentStartDelay() + " ms");
	    Thread.sleep(getInstrumentStartDelay());
	} catch (Exception e) {
	}
    }

    /**
     * Called after power is removed from instrument. By default this method
     does nothing, and may be overridden.
     */
    protected void powerOffCallback() {
	return;
    }

    /**
     * Put service in SUSPEND state. Release resources (e.g. serial port) for
     * use by other applications.
     */
    public synchronized void suspend() {


	interruptSampling();

	super.suspend();

	// Turn on instrument power (unless its policy is NEVER).
	if (_instrumentAttributes.powerPolicy != PowerPolicy.NEVER) {
	    _instrumentPort.connectPower();
	}
    }

    /**
     * Return all logged data packets having creation time within specified time
     * window.
     */
    public DevicePacketSet getPackets(long startTime, long stopTime)
	throws NoDataException {

	return _packetLog.getPackets(startTime, stopTime,
				     MAX_RETRIEVED_PACKETS);
    }


    /**
     * Return all logged data packets having creation time within specified
     * time window, that pass specified packet filters.
     */
    public DevicePacketSet getPackets(long startTime, long stopTime,
				      PacketFilter[] filters, 
				      boolean excludeStale)
	throws NoDataException {

	return _packetLog.getPackets(startTime, stopTime,
				     MAX_RETRIEVED_PACKETS, filters,
				     excludeStale);
    }



    /** Clear default packet filters for data retrieval. */
    public void clearDefaultPacketFilters() {
	annotate("clear pckt filters".getBytes());
	_packetLog.clearDefaultFilters();
    }

    /** Add default packet filters for data retrieval. */
    public void addDefaultPacketFilters(PacketFilter[] filters) {
	annotate("add pckt filters".getBytes());
	_packetLog.addDefaultFilters(filters);
    }


    /** Get default packet filters for data retrieval. */
    public PacketFilter[] getDefaultPacketFilters() {
	return _packetLog.getDefaultFilters();
    }



    /** Get value of specified service property. */
    public byte[] getProperty(byte[] key) throws MissingPropertyException {

	Properties properties = _instrumentAttributes.toProperties();

	String value = properties.getProperty(new String(key));

	if (value == null) {
	    throw new MissingPropertyException(new String(key));
	}

	return value.getBytes();
    }

    /** Set value of specified instrument service properties. Input consists 
	of one or more 'key=value' pairs. Each key=value pair is separated from
	the next pair by newline ('\n'). */
    final public void setProperty(byte[] propertyStrings, byte[] unused) 
	throws RemoteException, InvalidPropertyException {

	Properties backupProperties = 
	    _instrumentAttributes.toConfigurableProperties();

	_scratchProperties.clear();

	ByteArrayInputStream inputStream = 
	    new ByteArrayInputStream(propertyStrings);

	try {
	    // Add specified passed-in property settings
	    _scratchProperties.load(inputStream);
	}
	catch (IOException e) {
	    throw new InvalidPropertyException(e.getMessage());
	}	

	_log4j.debug("setProperty: Input properties strings\n" + 
		      new String(propertyStrings));

	_log4j.debug("setProperty: Input properties\n" + _scratchProperties);

	// This implementation does not allow certain properties to be set
	// on-the-fly, since those properties interact with service state 
	// in complicated ways. The following method throws a 
	// PropertyException if any of those non-settable properties 
	// have been specified.
	checkInputProperties(_scratchProperties);

	// If we get here, then no non-settable properties have been 
	// specified. Now load scratch properties with current attribute values
	_scratchProperties = _instrumentAttributes.toConfigurableProperties();
	// Reload the input stream
	inputStream = new ByteArrayInputStream(propertyStrings);

	try {
	    // Add specified passed-in property settings
	    _scratchProperties.load(inputStream);
	}
	catch (IOException e) {
	    throw new InvalidPropertyException(e.getMessage());
	}


	// Set attribute values based on properties. If there are problems
	// then a PropertyException will be thrown here.
	try {
	    _instrumentAttributes.fromProperties(_scratchProperties, true);
	}
	catch (PropertyException e) {

	    try {
		// If exception was thrown, restore previous attribute values
		_instrumentAttributes.fromProperties(backupProperties, true);
	    }
	    catch (Exception e2) {
		_log4j.error("Got exception while restoring attributes: " + 
			      e2);
	    }

	    // Now throw the exception back to the client
	    throw new InvalidPropertyException(e.getMessage());
	}

	// Log the updated attributes in metadata packet
	getMetadata("setProperty()".getBytes(), SERVICE_ATTRIBUTES, true);
    }


    /** Cache service properties on the node, such that current property
     values will be restored next time service is created on this node. */
    public void cacheProperties(byte[] note) throws Exception {
	_sandBox.saveServiceProperties(_instrumentAttributes.toConfigurableProperties());
	annotate(note);
    }

    /** Clear properties cache. */
    public void clearPropertiesCache(byte[] note) 
	throws RemoteException, Exception {
	_sandBox.deleteAllFiles();
	annotate(note);
    }


    /** Check that all specified properties are allowed to be set 
	on-the-fly. Throw PropertyException if any disallowed properties
	have been specified.
    */
    protected void checkInputProperties(Properties properties) 
	throws InvalidPropertyException {

	boolean error = false;
	StringBuffer errBuf = new StringBuffer("Can't modify property: ");

	String verbotenProperty[] = {"UUID", "isiID", "sampleSchedule"};

	for (int i = 0; i < verbotenProperty.length; i++) {
	    if (properties.getProperty(verbotenProperty[i]) != null) {
		error = true;
		errBuf.append(" " + verbotenProperty[i]);
	    }
	}

	if (error) {
	    throw new InvalidPropertyException(new String(errBuf));
	}
    }


    /**
     * Get Vector of instrument properties; each Vector element consists of byte
     * array with form "key=value".
     */
    public Vector getProperties() {

	Vector propList = new Vector();
	Properties properties = _instrumentAttributes.toProperties();

	for (Enumeration keys = properties.propertyNames(); keys
		 .hasMoreElements();) {
	    String key = (String) keys.nextElement();
	    String entry = key + "=" + properties.getProperty(key);
	    propList.addElement(entry.getBytes());
	}

	return propList;
    }

    /** Try putting puck into 'sensor' mode again. */
    private void retryPuck() {
	if (_instrumentPort instanceof PuckSerialInstrumentPort) {
	    _log4j.warn("retryPuck(): retrying puck setSensorMode()");

	    PuckSerialInstrumentPort pip = (PuckSerialInstrumentPort) _instrumentPort;

	    try {
		pip.setSensorMode(true);
	    } catch (IOException e) {
		_log4j.warn("retryPuck(): " + e);
	    }

	} else {
	    _log4j.warn("retryPuck(): _instrumentPort not "
			 + "PuckSerialInstrumentPort, do nothing");
	}
    }

    /** Return specifier for default sampling schedule. */
    protected abstract ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException;

    /** Sync default sample schedule */
    protected synchronized int sync(long delayMillis) {
	return syncSchedule(_defaultSampleScheduleName, delayMillis);
    }

    /** Print instrument data contained in input buffer. By default does nothing, and
     may be overridden. */
    protected void printData(byte[] buf) {
	// Do nothing - subclass can override this and optionally
	// print contents
    }

    /** Get diagnostics message from device's port and optionally log it. */
    public byte[] getPortDiagnostics(boolean logPacket) throws RemoteException {

	String statusMsg = _instrumentPort.getStatusMessage();

	if (logPacket) {
	    _messagePacket.setMessage(System.currentTimeMillis(), statusMsg
				      .getBytes());

	    logPacket(_messagePacket);
	}

	return statusMsg.getBytes();
    }

    /** Set _recordType field */
    protected void setRecordType(long type) {
	_recordType = type;
    }

    /** Centralized packet logging logic */
    public void logPacket(DevicePacket devicePacket) {

	// set the parentID field
	try {
	    devicePacket.setParentId(_parentNode.getParentId());
	} catch (RemoteException e) {
	    _log4j.error(e);
	}

	if (devicePacket instanceof SensorDataPacket) {

	    _log4j.debug("sendPacket: got SensorDataPacket _recordType = "
			  + _recordType);

	    SensorDataPacket packet = (SensorDataPacket) devicePacket;
	    packet.setRecordType(_recordType);

	    // don't move this down; it needs to be sent before setting
	    // _lastSensorDataPacket
	    _packetLog.appendPacket(packet, true, true);
	    _lastSensorDataPacket = packet;
	    _lastPacket = packet;
	    _parentNode.publish(_loggingEvent);
	} else {
	    // MessagePackets and others processed here
	    _packetLog.appendPacket(devicePacket, true, true);
	    _lastPacket = devicePacket;
	}
    }
    
    
    /**
     * NOTE: THis method should be overridden by classes who need a DevicePacketParser
     * @return The DevicePacketParser for this instrument. Will return <b>null</b>
     *  if none has been defined
     */
    public DevicePacketParser getDevicePacketParser() {
        return null;
    }

    /**
     * Get diagnostics summary message from device's port and optionally log it.
     */
    public byte[] getPortDiagnosticsSummary(boolean logPacket)
	throws RemoteException {

	String statusMsg = _instrumentPort.getStatusSummaryMessage();

	if (logPacket) {
	    _messagePacket.setMessage(System.currentTimeMillis(), statusMsg
				      .getBytes());
	    logPacket(_messagePacket);
	}

	return statusMsg.getBytes();
    }

    /** Reset port diagnostics. */
    public void resetPortDiagnostics() {
	_instrumentPort.resetStatus();
    }

    /** Get instrument's parser. Not supported, by default. */
    public PacketParser getParser() throws NotSupportedException {
	throw new NotSupportedException("Parser not defined for instrument");
    }

    /** Set device's clock to current time; can throw NotSupportedException. */
    public void setClock() 
	throws NotSupportedException {
	throw 
	    new NotSupportedException("service does not implement setClock()");
    }

    /** Set device's clock; can throw NotSupportedException. */
    public abstract void setClock(long epochMsecs) throws NotSupportedException;

    ///////////////////////////////////////////////////////
    // The following methods are for manipulating schedules
    ///////////////////////////////////////////////////////
    /** Return the schedule key, as part of ScheduleOwner implementation. */
    public final ScheduleKey getScheduleKey() {
	return _scheduleKey;
    }

    /** Execute the scheduled task, as part of ScheduleOwner implementation. */
    public final void doScheduledTask(ScheduleTask task) {
	try {
	    acquireSample(true);
	} catch (NoDataException e) {
	    _log4j.error("doScheduledTask: caught NoDataException e" + e, e);
	}
    }

    /**
     * Return a vector of ScheduleTask objects Part of Instrument interface.
     */
    public final Vector getSchedules() throws RemoteException {
	return _schedules;
    }

    /**
     * Return a vector of ScheduleTask objects Part of ScheduleOwner interface.
     */
    public final Collection getAllSchedules() {
	return _schedules;
    }

    /** Get the default schedule */
    public ScheduleTask getDefaultSampleSchedule() {
	return getSchedule(_defaultSampleScheduleName);
    }

    /** Get a schedule by name */
    public ScheduleTask getSchedule(String name) {

	for (Enumeration e = _schedules.elements(); e.hasMoreElements();) {
	    ScheduleTask st = (ScheduleTask) e.nextElement();
	    if (st.getName().equals(name)) {
		return st;
	    }
	}
	return null;
    }

    /** Get default sample schedule name */
    public String getDefaultSampleScheduleName() {
	return _defaultSampleScheduleName;
    }

    /**
     * Add/replace instrument schedule
     * 
     * @param name
     *            Schedule name
     * @param schedule
     *            Representation of schedule
     * @param overwrite
     *            If true, replace existing schedule
     * @return Scheduler.UNDEFINED,Scheduler.NOT_FOUND
     */
    public int addSchedule(String name, String schedule, boolean overwrite) {
	int retval = Scheduler.UNDEFINED;
	try {
	    ScheduleSpecifier specifier = new ScheduleSpecifier(schedule);

	    ScheduleTask newTask = createTask(name, specifier, this);

	    if (newTask == null) {
		return Scheduler.NOT_FOUND;
	    }

	    retval = addSchedule(newTask, overwrite);
	    if (name.equalsIgnoreCase(_defaultSampleScheduleName)) {
		_instrumentAttributes.sampleSchedule = specifier;
	    }

	} catch (ScheduleParseException e) {
	    _log4j.error("addSchedule " + e, e);
	    retval = Scheduler.INVALID_SCHEDULE;
	}
	return retval;
    }

    /** Add the specified schedule task. */
    protected int addSchedule(ScheduleTask schedule, boolean overwrite) {
	Scheduler s = Scheduler.getInstance();
	ScheduleTask st = getSchedule(schedule.getName());

	if (st != null && overwrite == false) {
	    return Scheduler.ALREADY_EXISTS;
	}

	// doesn't exist or is OK to overwrite
	int i = Scheduler.UNDEFINED;
	if (st != null) {
	    s.removeSchedule(st);
	    _schedules.remove(st);
	}
	if (schedule != null) {
	    _schedules.add(schedule);
	    i = s.addSchedule(schedule, overwrite);
	}
	return i;
    }

    /** Remove all schedules. Return integer code defined by Scheduler. */
    public int removeAllSchedules(){
	// notify Scheduler to remove all of this
	// ScheduleOwner's schedules
	int retval=Scheduler.getInstance().removeSchedules(_schedules);
	// Clear this ScheduleOwner's Collection (Vector) of 
	// schedules
	_schedules.removeAllElements();
	return retval;
    }

    /** Remove the specified schedule. */
    public int removeSchedule(String taskName) {
	Scheduler scheduler = Scheduler.getInstance();
	ScheduleTask task = getSchedule(taskName);
	int i = Scheduler.OK;
	if (task != null) {
	    i = scheduler.removeSchedule(task);
	    _schedules.remove(task);
	} else
	    i = Scheduler.NOT_FOUND;
	return i;
    }

    /** Suspend the specified schedule. */
    public int suspendSchedule(String schedule) {
	ScheduleTask st = getSchedule(schedule);
	int i = Scheduler.OK;
	if (st != null)
	    i = st.suspend();
	else
	    i = Scheduler.NOT_FOUND;
	return i;
    }

    public int resumeSchedule(String schedule) {
	ScheduleTask st = getSchedule(schedule);
	int i = Scheduler.OK;
	if (st != null)
	    i = st.resume();
	else
	    i = Scheduler.NOT_FOUND;
	return i;
    }

    public int syncSchedule(String schedule, long delayMillis) {
	ScheduleTask st = getSchedule(schedule);
	int i = Scheduler.OK;
	if (st != null)
	    i = st.sync(delayMillis);
	else
	    i = Scheduler.NOT_FOUND;
	return i;
    }

    /** Set default sample schedule name */
    public void setDefaultSampleScheduleName(String name) {
	_defaultSampleScheduleName = name;
    }

    /**
     * createTask is a factory method to make new schedule tasks by name.
     * Subclasses may add new types of schedules by overriding createTask,
     * calling super, and checking the return for null (return value if not
     * null) before checking the name argument for one of the new added types.
     */
    public ScheduleTask createTask(String name, ScheduleSpecifier schedule,
				   ScheduleOwner owner) {
	if (name.equals(((InstrumentService) owner)
			.getDefaultSampleScheduleName()))
	    try {
		ScheduleTaskImpl newTask = new ScheduleTaskImpl(name, schedule,
								owner);

		newTask
		    .setOwnerName((this.getClass().getName() + ":" + getId()));
		return newTask;
	    } catch (ScheduleParseException e) {
		_log4j.debug("createTask:", e);
	    }
	return null;
    }

    /** Set the ServiceAttributes object for this service. */
    public final void setAttributes(ServiceAttributes attributes) {
	super.setAttributes(attributes);
	if (attributes instanceof InstrumentServiceAttributes) {
	    // Only set if an InstrumentServiceAttributes object is coming in
	    _instrumentAttributes = (InstrumentServiceAttributes) attributes;
	}
    }

    /** Stop the service. Note that this method should NOT be synchronized,
     since must be able to shut down a service that is currently being
    sampled. */
    public byte[] shutdown() {

	interruptSampling();

	_log4j.debug("shutdown() - cancel all schedules");
	Iterator iterator = getAllSchedules().iterator();
	while (iterator.hasNext()) {
	    ScheduleTask task = (ScheduleTask) iterator.next();

	    _log4j.debug("InstrumentService: Removing schedule "
			  + task.getOwnerName() + "#"
			  + task.getOwner().getScheduleKey().value() + ":"
				  + task.getName());

	    Scheduler.getInstance().removeSchedule(task);
	    iterator.remove();

	    _log4j.debug("schedule removed");
	}


	String msg = null;

	try {
	    msg = shutdownInstrument();
	}
	catch (Exception e) {

	    msg = "Exception while shutting down instrument" + 
		getName();

	    _log4j.error(msg, e);
	}

	// Power off ports 'n stuff
	super.shutdown();

	return msg.getBytes();
    }


    /** Interrupt any sampling threads. */
    protected void interruptSampling() {

	synchronized (_semaphore) {

	    setStatusShutdown();

	    // Interrupt all sampling threads
	    Iterator iterator = _samplingThreads.iterator();
	    while (iterator.hasNext()) {
		Thread thread = (Thread )iterator.next();
		if (thread.isAlive()) {
		    // Interrupt the thread
		    _log4j.debug("Interrupt sampling thread" + 
				 thread.getName());
		    thread.interrupt();
		    _log4j.debug("done with thread.interrupt()");
		}
		else {
		    _log4j.debug("Thread " + thread.getName() + 
				 " is not alive");
		}
		 
		// Remove the thread
		_log4j.debug("remove thread " + thread.getName() + 
			     " from list");
		iterator.remove();

		_log4j.debug("done with thread.remove()");
	    }
	}
    }


    /** If execution thread of specified task is sleeping, return time
	at which it will resume; otherwise return 0. */
    public long sleepingUntil(ScheduleTask task) {
	_log4j.warn("sleepUntil() not yet implemented");
	return 0;
    }


    /** Perform any instrument-specific shutdown actions; subclasses can
	override; by default does nothing. */
    protected String shutdownInstrument() 
	throws Exception {
	return "OK";
    }

    /** Enable data summary generation. */
    public void enableSummary() {
	// Does nothing - not implemented yet. 
    }

    /** Disable data summary generation. */
    public void disableSummary() {
	// Does nothing - not implemented yet. 
    }

    /** Return true if summary generation is enabled. */
    public boolean summaryEnabled() {
	// Does nothing - not implemented yet. 
	return false;
    }

}

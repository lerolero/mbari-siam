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
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.mbari.siam.core.Scheduler.ScheduleKey;
import org.mbari.siam.distributed.jddac.DummyBlock;
import org.mbari.siam.distributed.jddac.InstrumentServiceBlock;
import org.mbari.siam.distributed.jddac.SummaryBlock;
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
import org.mbari.siam.distributed.MeasurementPacket;
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
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.dataTurbine.Turbinator;
import org.mbari.jddac.NewArgArrayEvent;
import org.mbari.jddac.NewArgArrayListener;
import org.mbari.jddac.StatsBlock;
import net.java.jddac.jmdi.fblock.FunctionBlock;

/**
 * BaseInstrumentService is the base class of an "application framework" for
 * instruments which communicate through a serial port
 *
 * <p>
 * The application framework is a semi-complete application, providing data and
 * behavior which are common to all instrument services. To complete the
 * implementation of a specific instrument service, the programmer extends
 * the base BaseInstrumentService class, and implements or overrides several
 * base class methods.
 * <P>
 * BaseInstrumentService provides the following as built-in features:
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
 * Access to instrument serial port: BaseInstrumentService contains a
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
 * 1. Extend StreamingInstrumentService or PolledInstrumentService to create your service's subclass
 * <p>
 * 2. Implement any abstract methods (see documentation below
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
 * NOTE: Service implementations should AVOID catching InterruptedException, 
 * since infrastructure relies on thread interruption to quickly shutdown
 * a service.
 *
 * @author Tom O'Reilly, Kent Headley, Bill Baugh
 */
public abstract class BaseInstrumentService
    extends DeviceService
    implements Instrument, DeviceServiceIF, ScheduleOwner {
		
		private static String _versionID = "$Revision: 1.28 $";
		
		/** Log4j logger */
		protected static Logger _log4j =
		Logger.getLogger(BaseInstrumentService.class);
		
		
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
		protected boolean _running = false;
		
		/** Indicates if run() has been successful. */
		public boolean running() {
			return _running;
		}
		
		/** Error message cache */
		protected MessageCache _errorCache = null;
		
		/** Default sampler task */
		protected ScheduleTask _defaultSamplerTask = null;
		
		/**
		 * Directory where log files are written. Default is current working
		 * directory.
		 */
		private String _logDirectory = ".";
		
		/** DeviceMessagePacket. */
		protected DeviceMessagePacket _messagePacket = null;
		
		/** Vector of threads that are accessing the instrument. */
		private Vector _deviceAccessThreads = new Vector();
		
		/** Semaphore used to synchronize threads */
		private Object _deviceAccessSemaphore = new Object();
		
		// Sampling task might want to sleep - this is the time at which it
		// wants to wake up
		protected long _samplerWakeupTime = 0;
		
		// This object locks a thread until "wakeup" task unlocks it
		private Object _sleepLock = new Object();
		
		/////////////////////////////////////////////////////////////////////////
		// Subclasses MAY wish to override these defaults
		
		/** Maximum number of bytes in a data sample read from instrument. */
		protected int _maxSampleBytes = 0;
		
		/** Instrument's "prompt" character(s). */
		private byte[] _promptString;
		
		/** Sample terminator */
		private byte[] _sampleTerminator;
		
		/** Big buffer for raw samples. */
		private byte[] _bigSampleBuf;
		
		/** SensorDataPacket to be returned to clients. */
		protected SensorDataPacket _sensorDataPacket = null;
		
		/** Buffer for sensor data packet's payload */
		private byte[] _sensorDataBuf = null;
		
		/** Most recent SensorDataPacket sent */
		protected SensorDataPacket _lastSensorDataPacket = null;
		
		/** Most recent DataPacket sent (of any kind) */
		protected DevicePacket _lastPacket = null;
		
		/** Sensor log for persistent packet storage. */
		protected FilteredDeviceLog _packetLog = null;
		
		/** Default record type */
		protected static final long RECORDTYPE_UNDEFINED = -1;
		
		protected static final long RECORDTYPE_METADATA = 0;
		
		protected static final long RECORDTYPE_DEFAULT = 1;
		protected static final long MAX_BASE_RECORDTYPE = RECORDTYPE_DEFAULT;
		
		protected long _recordType = RECORDTYPE_DEFAULT;
		
		protected Turbinator _turbinator = null;
		
		private ServiceEvent _loggingEvent = null;
		
		/**
		 * This a a JDDAC FunctionBlock that handles aquired samples. Typical uses
		 * of this might be logging of Statistics or Event Response.
		 */
		private InstrumentServiceBlock _instrumentServiceBlock;
		
		/** Number of times power has been applied to instrument. */
		protected int _nPowerRequests = 0;
		
		/** Number of times instrument wakeup has been requested */
		protected int _nWakeRequests = 0;
		
		///////////////////////////////////////////
		// Members related to ScheduleTasks
		/** ScheduleTask container (schedules) */
		protected Vector _schedules = new Vector();
		
		/** Name of default sample schedule */
		protected String _defaultSampleScheduleName
		= ScheduleSpecifier.DEFAULT_SCHEDULE_NAME;
		
		/** Unique key used to access Scheduler entries */
		protected ScheduleKey _scheduleKey = null;
		
		/** Basic instrument service attributes. */
		protected InstrumentServiceAttributes _instrumentAttributes =
		new InstrumentServiceAttributes(this);
		
		/** Constructor. */
		public BaseInstrumentService() throws RemoteException {
			super();
			super.setAttributes(_instrumentAttributes);
			_errorCache = new MessageCache(this, _instrumentAttributes.errCacheLimit);
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
		protected final void setMaxSampleTries(int maxTries)
		throws RangeException {
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
		protected final void setMaxSampleBytes(int maxBytes)
		throws RangeException {
			// NOTE: We currently do not enforce a maximum size of this buffer.
			if (maxBytes <= 0)
				throw new RangeException("argument must be positive");
			
			_maxSampleBytes = maxBytes;
			// This for the GC, if _bigSampleBuf already exists
			_bigSampleBuf = null;
			_bigSampleBuf = new byte[_maxSampleBytes];
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
			return _bigSampleBuf;
		}
		
		/** Get last sample packet. */
		public final SensorDataPacket getLastSample()
		throws NoDataException {
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
		
		/** initialize a set of reusable data packet objects 
			Base class creates only one.
		    subclasses may override to support larger pool for different types
		 */
		protected void initializePooledResources(long id, int len){
			// Create (reusable ) data packet
			_sensorDataPacket = new SensorDataPacket(id,len);
		}
		
		/**
		 * Initialize service and instrument, sampling schedule.
		 * This method must be invoked by
		 * outside entity before data acquisition starts.
		 */
		public synchronized void prepareToRun() 
		throws InitializeException, InterruptedException {
			
			if (_packetLog == null) {
				// No packet log (may have been destroyed in shutdown()) -
				// create it.
				try {
					createPacketLog();
				}
				catch (Exception e) {
					throw new InitializeException(e.getMessage());
				}
			}
			
			// Create sampling schedule
			_log4j.debug("run() - create default sampling schedule");
			try {
				// get unique id to reference schedules
				// This should return the same number if we've
				// called it before.
				_scheduleKey = Scheduler.getScheduleKey(this);
				
				_log4j.debug("_scheduleKey=" + _scheduleKey);
				
				// Create the default sampling schedule if it doesn't exist
				// (i.e. hasn't been specified by a property)
				if (_instrumentAttributes.sampleSchedule == null) {
					_instrumentAttributes.sampleSchedule =
					createDefaultSampleSchedule();
				}
				
				// Remove all schedules, notifying Scheduler
				removeAllSchedules();
				
				/*
				 if (_defaultSamplerTask != null) {
				 // Remove default sampler task if it was created by
				 // previous attempted invocation to run()
				 removeSchedule(getDefaultSampleScheduleName());
				 _defaultSamplerTask = null;
				 }
				 */
				
				_defaultSamplerTask =
				createTask(_defaultSampleScheduleName,
						   _instrumentAttributes.sampleSchedule,
						   this);
				
				// add default sample schedule, notifying Scheduler
				// overwrite it if it exists
				addSchedule(_defaultSamplerTask,true);
				
			} catch (ScheduleParseException e) {
				throw new InitializeException("ScheduleParseException: "
											  + e.getMessage());
			}
			
			
			super.prepareToRun();
			
			// Keep track of device device-access thread
			try {
				startDeviceAccess();
			}
			catch (Exception e) {
				throw new InitializeException("run() - caught exception from " +
											  "startDeviceAccess(): " +
											  e.getMessage());
			}
			
			setStatusInitial();
			
			// Create (reusable ) data packet
			initializePooledResources(getId(), _maxSampleBytes);
			//_sensorDataPacket = new SensorDataPacket(getId(), _maxSampleBytes);
			
			// (Re-)Create error message cache, using specified errCacheLimit 
			// attribute
			_errorCache = 
			new MessageCache(this, _instrumentAttributes.errCacheLimit);
			
			// Set subclass-specified values of uninitialized member variables
			try {
				
				_log4j.debug("Service " + _serviceName
							 + " initializing port");
				
				_instrumentPort.setCurrentLimit(getCurrentLimit());
				
			} catch (RangeException e) {
				throw new InitializeException("RangeException: " + e.getMessage());
			} catch (NotSupportedException e) {
				_log4j.debug("setCurrentLimit() not supported for this port");
			}
			
			_log4j.debug("run(): Done with port initialization");
			
			if (_nPowerRequests == 0) {
				// Wait for specified delay before first power-on.
				_log4j.debug("snoozing for powerOnDelay ("+
							 _instrumentAttributes.powerOnDelaySec+" sec)");
				snooze(_instrumentAttributes.powerOnDelaySec);
				_log4j.debug("done with powerOnDelay");
			}
			
			// Do subclass-specific initialization
			try {
				
				// Turn on port power/comms; may be needed for initialization.
				managePowerWake();
				
				_log4j.info("initializeInstrument() for " +
							_serviceName);
				
				initializeInstrument();
				
				_log4j.info("done with initializeInstrument() for "
							+ _serviceName);
			} 
			catch (Exception e) {
				
				// If thread was interrupted, propagate up call stack
				if (e instanceof InterruptedException) {
					_log4j.info("run() - got InterruptedException() from initializeInstrument()");
					throw (InterruptedException )e;
				}
				
				throw new InitializeException("initializeInstrument() failed: "
											  + e.getMessage());
			}
			
			
			// Set subclass-specified values of uninitialized member variables
			try {
				// generate and log complete metadata state packet
				getMetadata("Service Init".getBytes(), MDATA_ALL, true);
			} catch (Exception e) {
				// If thread was interrupted, propagate up call stack
				if (e instanceof InterruptedException) {
					_log4j.info("run() - got InterruptedExcecption() from getMetadata()");
					throw (InterruptedException )e;
				}
				
				throw new InitializeException(e.getMessage());
			}
			_log4j.debug("done creating metadataPacket...");
			
			// No longer accessing instrument device
			endDeviceAccess();
			
			// Turn off port power/comms (subclass might have turned 'em on)
			managePowerSleep();
			
                       // Register with InstrumentRegistry AFTER InitializeInstrument(), to give 	 
			// the subclass a chance to change its registryName 	 
			// But first, if there's a _turbinator but no registryName attribute, 	 
			// create registryName from getName(), so we get a RegistryEntry 	 
			// for the turbinator 	 
	  	 
			if ((_instrumentAttributes.rbnbServer != null) &&
			    (_instrumentAttributes.rbnbServer.length() > 0)  &&
			    (registryName() == null)) {
			    _instrumentAttributes.registryName = new String(getName()); 	 
			}


			// Now create the RegistryEntry
			createRegistryEntry();

			if (_regEntry != null) {
			    try {
				_regEntry.setPacketParser(getParser());
			    } catch (NotSupportedException e) {
			    }
			}

			// Create the Turbinator(s)
			createTurbinators();

			setStatusOk();
			
			_running = true;
			
			// Reset any scheduled tasks
			// DO THIS ONLY after setting _running to true, to prevent race
			_log4j.debug("BaseInstrumentService.run() - reset schedule tasks");
			Iterator iterator = getAllSchedules().iterator();
			while (iterator.hasNext()) {
				ScheduleTask task = (ScheduleTask) iterator.next();
				
				
				_log4j.debug("task.getOwner()=" +
							 task.getOwner());
				
				
				_log4j.debug("task.getOwner().getScheduleKey()=" +
							 task.getOwner().getScheduleKey());
				
				_log4j.debug("BaseInstrumentService: reset timer for task "
							 + task.getOwnerName() + "#"
							 + task.getOwner().getScheduleKey().value() + ":"
							 + task.getName());
				
				task.resetTimer();
				
				_log4j.debug("BaseInstrumentService: task timer has been reset");
			}
			
			return;
		}
		
		/** Helper function to create the Turbinators */
    		protected void createTurbinators()
    		{
		    if ((_instrumentAttributes.rbnbServer != null) &&
			(_instrumentAttributes.rbnbServer.length() > 0))
		    {
			// First close Turbinator if it already exists.
			if (_turbinator != null) {
			    removeDataListener(_turbinator);
			    _turbinator.close();
			    _turbinator = null;
			}
				
			// Form legal, unique DataTurbine source name from instrument 
			// mnemonic and ISI ID. If mnemonic is null/blank, use instrument
			// class name
			String turbineName = "";
			if (new String(getName()).length() > 0) {
			    // Use service mnemonic to build turbine name
			    turbineName = 
				(new String(getName())).replace(' ', '_') + "-" + getId();
			}
			else {
			    // Use class name to build turbine name
			    turbineName = getClass().getName();
			    int index = turbineName.lastIndexOf(".");
			    if (index >= 0) {
				turbineName = turbineName.substring(index+1);
			    }
			    turbineName = turbineName + "-" + getId();
			}
				
			try {
			    _turbinator = new Turbinator(getParser(),
							 turbineName,
							 _instrumentAttributes.rbnbServer,
							 _instrumentAttributes.locationName,
							 new String(_instrumentAttributes.serviceName),
							 _instrumentAttributes.rbnbAdvertiseService,
							 _instrumentAttributes.rbnbCacheFrames,
							 _instrumentAttributes.rbnbArchiveFrames);
					
			    if ((_instrumentAttributes.rbnbExcludeRecordTypes != null) &&
				(_instrumentAttributes.rbnbExcludeRecordTypes.length > 0)) {
				for (int i = 0; i < _instrumentAttributes.rbnbExcludeRecordTypes.length; i++) {
				    _turbinator.excludeRecordType(_instrumentAttributes.rbnbExcludeRecordTypes[i]);
				}
			    }

			    //Register the Turbinator as an InstrumentDataListener
			    addDataListener(_turbinator);
					
			}
			catch (Exception e) {
			    _log4j.error("Caught exception constructing Turbinator for " +
					 getClass().getName() + ": " + e, e);
			}
		    }

		} /* createTurbinators() */

		/**
		 * Prepare the device for sampling; called before requestSample().
		 * By default this method does nothing, and may be overridden in the
		 * subclass.
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
		 * Acquire data sample from instrument, process it, log it, and return it
		 * to caller.
		 */
		public final synchronized SensorDataPacket acquireSample(boolean logPacket)
		throws NoDataException {
			
			try {
				// About to access instrument device - keep track of this thread
				startDeviceAccess();
			}
			catch (Exception e) {
				throw new NoDataException(e.getMessage());
			}
			
			
			SensorDataPacket packet = null;
			try {
				packet = acquire(logPacket);
				
			}
			catch (NoDataException e) {
				throw e;
			}
			catch (Exception e) {
				throw new NoDataException(e.getMessage());
			}
			finally {
				// Done with device access
				endDeviceAccess();
			}
			
			/*
			 * Attempt to process a packet in the InstrumentServiceBlock
			 */
			try {
				if (packet != null && getInstrumentServiceBlock() != null) {
					_log4j.debug("Adding packet to InstrumentServiceBlock");
					getInstrumentServiceBlock().processDevicePacket(packet);
				}
			}
			catch (Exception e) {
				_log4j.error("An error occured when adding a packet to the InstrumentServiceBlock", e);
			}
			
			return packet;
		}
		
		/** Do overhead tasks needed to prepare for sample cycle.
		 It is broken out into a separate method to allow other
		 methods (e.g., when a service wants to override acquire())
		 to use the same code without cutting and pasting.
		 */
		public synchronized void assertSamplingState()
		throws NoDataException {
			
			if (getStatus() == Device.SUSPEND || getStatus() == Device.SAFE) {
				throw new NoDataException("service is suspended/safed");
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
				try {
					powerOnCallback();
				}
				catch (Exception e) {
					_log4j.error("managePowerWake() - powerOnCallback(): " + e);
				}
			}
			
			/** Get diagnostic information before sampling. */
			if ((_instrumentAttributes.diagnosticSampleInterval > 0) &&
				(++_nWakeRequests >= _instrumentAttributes.diagnosticSampleInterval)) {
				
				_nWakeRequests = 0;
				
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
		
		/** Turn on instrument power. If override is true, don't check with
		 parent first. */
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
			
			if (_instrumentAttributes.powerPolicy == PowerPolicy.WHEN_SAMPLING
				||
				(_nPowerRequests == 1 &&
				 _instrumentAttributes.powerPolicy == PowerPolicy.NEVER)) {
				
				_log4j.debug("managePowerSleep() - powerOffCallback");
				powerOffCallback();
			}
			
			if  ((_instrumentAttributes.commPowerPolicy == PowerPolicy.WHEN_SAMPLING
				  || _instrumentAttributes.commPowerPolicy == PowerPolicy.NEVER) &&
				 (_toDevice != null))
			{
				try {
					_toDevice.flush();
				} catch (Exception e) {
					_log4j.error("Error flushing output: " + e.getMessage());
				}
				_instrumentPort.disableCommunications();
			} else {
				// Always on
				_instrumentPort.enableCommunications();
			}
			
			
		}
		
		/** Turn off instrument power. */
		public int powerOff() {
			_instrumentPort.disconnectPower();
			return Device.OK;
		}
		
		
		/** Return number of power-on requests issued so far */
		public int nPowerRequests() {
			return _nPowerRequests;
		}
		
		
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
			
			if (_instrumentPort instanceof HttpInstrumentPort) {
				
				HttpInstrumentPort httpPort = (HttpInstrumentPort )_instrumentPort;
				
				// Need to continually read HTTP port's input stream until 
				// no more is available.
				boolean done = false;
				int totalBytesRead = 0;
				
				while (!done) {
					
					int offset = totalBytesRead;
					int len = getSampleBuf().length - totalBytesRead;
					
					// Expect InputStream.read() to block until end-of-file. 
					// But for some reason it returns even though the entire
					// HTTP page content has not yet been returned...
					totalBytesRead += httpPort.getInputStream().read(sample, 
																	 offset, len);
					
					// Wait a bit before checking for more available bytes.
					// This is arbitrarily set to 1000 msec...
					Thread.sleep(1000);
					
					int nAvailable = httpPort.getInputStream().available();
					_log4j.info("AxisCamera.readSample(): " + nAvailable +
								" bytes still available");
					
					if (nAvailable == 0) {
						done = true;
					}
				}
				
				return totalBytesRead;
			}
			
			// Read from serial port until sample terminator encountered 
			return StreamUtils.readUntil(_fromDevice, sample, _sampleTerminator,
										 _instrumentAttributes.sampleTimeoutMsec);
		}
		
		/**
		 * This method can optionally be overriden so the sub-class can determines
		 * the validity of the sampled bytes. If an error is detected an
		 * InvalidDataException is thrown.
		 *
		 * @param sampleBuf
		 *            sample buffer containing data to validate
		 * @param nBytes
		 *            Number of bytes of data to validate
		 *
		 * @exception InvalidDataException
		 *                thrown if data is found to be invalid
		 */
		protected void validateSample(byte[] sampleBuf, int nBytes)
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
		throws Exception  {
			// Set timestamp
			_sensorDataPacket.setSystemTime(System.currentTimeMillis());
			
			// Set record type; subclasses must set _recordType when they
			// submit data other than the default record format
			_sensorDataPacket.setRecordType(_recordType);
			
			// Make sure that sensor data packet buffer is exactly the right
			// size
			if (_sensorDataBuf == null || nBytes != _sensorDataBuf.length) {
				// (Re-)allocate sensor data buffer to be exactly the 
				// right size for incoming sample. 
				_sensorDataBuf = new byte[nBytes];
				
				// Set sensor data packet's buffer reference
				_sensorDataPacket.setDataBuffer(_sensorDataBuf);
			}
			// Copy data from big sample buffer to correctly-sized sensor
			// data buffer
			System.arraycopy(sample, 0, _sensorDataBuf, 0, nBytes);

			// Call the DataListeners that have registered for dataCallbacks
			callDataListeners(_sensorDataPacket);
			
			return _sensorDataPacket;
		}
		

		/** Call any DataListeners that have registered for dataCallbacks,
		 * for the given SensorDataPacket */
		public void callDataListeners(SensorDataPacket packet)
    		{
		    if ((_regEntry != null) && (_regEntry.numDataListeners() > 0)) {
			_regEntry.callDataListeners(packet);
		    }
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
		
		/**
		 * Get requested metadata state components in a byte array containing
		 * serialized State and StateAttribute objects.
		 */
		public byte[] getMetadataPayload(int components, String[] attributeList)
		throws RemoteException, Exception {
			
			StringBuffer output = new StringBuffer(10000);
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
			
			if ((components & INSTRUMENT_STATE) > 0) {
				
				// Synchronize retrieval of device metadata, since we can't
				// access device is another thread is sampling or vice versa
				synchronized (this) {
					startDeviceAccess();
					output.append(MetadataPacket.DEVICE_INFO_TAG + "\n");
					// Apply power policy to instrument
					managePowerWake();
					
					try {
						output.append(new String(getInstrumentStateMetadata()) + "\n"
									  + MetadataPacket.DEVICE_INFO_CLOSE_TAG + "\n");
					}
					catch (Exception e) {
						output.append(e.getMessage() + "\n"
									  + MetadataPacket.DEVICE_INFO_CLOSE_TAG + "\n");
					}
					
					endDeviceAccess();
					
					managePowerSleep();
				}
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
										  boolean logPacket)
		throws RemoteException {
			
			MetadataPacket packet = null;
			try {
				packet = getMetadata(cause, components, null, logPacket);
			} catch (Exception e) {
				_log4j.error("getMetadata(): " + e);
			}
			return packet;
		}
		
		/**
		 * Create, return, and optionally log metadata packet; if service
		 * attributes are requested, then include only specified attributes.
		 *
		 * @param cause
		 * @param components
		 * @param attributeNames
		 * @param logPacket
		 * @return metadata (MetadataPacket)
		 */
		protected MetadataPacket getMetadata(byte[] cause, int components,
											 String[] attributeNames,
											 boolean logPacket)
		throws RemoteException, Exception {
			
			MetadataPacket packet =
			new MetadataPacket(getId(), cause,
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
		IOException,
		NullPointerException {
			
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
		
		
		/**
		 * Get instrument 'metadata' from device itself. By default, this method
		 * does nothing, and can be overridden. This method should return any
		 * available configuration information that is available from the
		 * instrument device. Note that this method should NOT power-manage
		 * the device, as that is the responsibility of the caller.
		 */
		protected byte[] getInstrumentStateMetadata()
		throws Exception {
			// Do nothing. Leave it to subclasses.
			// Subclasses should, to whatever degree possible, provide
			// all state information by directly reading them from the instrument,
			// i.e., current values of metadata items
			// represented in service.properties or the service's XML, or other
			// information relevant to the sampling context that can be obtained
			// from the instrument.
			// The idea here is to directly read the instrument, which may not
			// agree with other versions of the instrument state variables.
			
			String mdString = _serviceName
			+ " does not provide instrument state information\n";
			
			return mdString.getBytes();
		}
		
		/**
		 * Do instrument- and service-specific initialization. By default, this
		 * method does nothing, and may be overridden.
		 */
		protected void initializeInstrument()
		throws InitializeException, Exception {
			// Do nothing. Leave it to subclasses.
		}
		
		/**
		 * Put annotation into device data stream. Annotation will be automatically
		 * time-tagged by host.
		 */
		public synchronized void annotate(byte[] annotation) {
			
			//create wrapper for message packet
			_log4j.debug("Annotation: " + new String(annotation));
			
			if (_messagePacket != null) {
				_messagePacket.setMessage(System.currentTimeMillis(), annotation);
				logPacket(_messagePacket);
			}
			else {
				_log4j.error("annotate() - message packet not created yet, would write: " + 
							 new String(annotation));
			}
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
							 serviceXMLPath, servicePropertiesPath,
							 cachedServicePath);
			
			
			// Initialize parameters to default values
			_log4j.debug("Initialize service defaults");
			initializeDriverDefaults();
			
			// Get attribute values from configuration properties;
			// these override defaults
			_log4j.debug("Get attribute values from properties");
			_instrumentAttributes.fromProperties(sandBox.getServiceProperties(), 
												 true);
			
			_messagePacket =
			new DeviceMessagePacket(_instrumentAttributes.isiID);
			
			// Create device log
			_log4j.debug("Create the packet log");
			createPacketLog();
			
		}
		
		/** Create device packet log. */
		protected void createPacketLog() throws MissingPropertyException,
		InvalidPropertyException,
		FileNotFoundException, IOException {
			
			// Get name of log directory
			Properties systemProperties = System.getProperties();
			String siamHome = systemProperties.getProperty("siam_home").trim();
			String logDirectory = _nodeProperties.getDeviceLogDirectory();
			
			_logDirectory = siamHome + File.separator + logDirectory;
			PacketFilter[] filters = new PacketFilter[2];
			filters[0] = 
			new PacketSubsampler(_instrumentAttributes.defaultSkipInterval, 
								 DevicePacket.SENSORDATA_FLAG);
			
			filters[1] = new PacketSubsampler(0, ~DevicePacket.SENSORDATA_FLAG);
			
			long shelfLifeMsec = 
			(long )(_instrumentAttributes.dataShelfLifeHours * 3600000.);
			
			_packetLog = new FilteredDeviceLog(getId(), _logDirectory, filters,
											   shelfLifeMsec);
		}
		
		/**
		 * Called after power is applied to instrument; return when instrument is
		 * ready for use. By default, sleeps for 'instrumentStartDelay'
		 * milliseconds.
		 */
		protected void powerOnCallback() throws Exception {
			
			try {
				_log4j.debug(_serviceName
							 + ": default waitForInstrumentStart(); " +
							 "sleep for "
							 + getInstrumentStartDelay() + " ms");
				Thread.sleep(getInstrumentStartDelay());
			} catch (Exception e) {
				_log4j.error("Failed to execute powerOnCallback() call", e);
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
		 * use by other applications. Note that this method should NOT be 
		 synchronized, since must be able to suspend a service that is currently
		 sampling.
		 */
		public void suspend() {
			
			super.suspend();
			
			interruptDeviceAccess(5000);
			
			// Turn on instrument power (unless its policy is NEVER).
			if (_instrumentAttributes.powerPolicy != PowerPolicy.NEVER) {
				_instrumentPort.connectPower();
			}
		}
		
		/*
		 * Put service in OK state. Re-acquire resources (e.g. serial port).
		 * Re-instantiate device log (in case it was moved away while 
		 * service was suspended.)
		 */
		public synchronized void resume() {
			// Re-instantiate packet log
			try {
				if (_packetLog == null) {
					createPacketLog();
				}
			}
			catch (Exception e) {
				String msg = 
				"resume() caught exception while re-instantiating log";
				
				_log4j.error(msg);
			}
			
			super.resume();
		}
		
		
		/**
		 * Return all logged data packets having creation time within specified
		 * time window.
		 */
		public DevicePacketSet getPackets(long startTime, long stopTime)
		throws NoDataException {
			
			return _packetLog.getPackets(startTime, stopTime,
										 _instrumentAttributes.packetSetSize);
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
										 _instrumentAttributes.packetSetSize,
										 filters, excludeStale);
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
			finally {
				try {
					inputStream.close();
				}
				catch (IOException e) {
				}
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
			finally {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					
				}
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
					_log4j.debug("Exception thrown while restoring attributes", e);
				}
				
				// Now throw the exception back to the client
				throw new InvalidPropertyException(e.getMessage());
			}
			
			
			// Log the updated attributes in metadata packet
			getMetadata("setProperty()".getBytes(), SERVICE_ATTRIBUTES, true);
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
		 * Get Vector of instrument properties; each Vector element consists of
		 * byte array with form "key=value".
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
		
		
		/** Cache service properties on the "instrument host" node, 
		 such that current service property values will be restored next time 
		 service is created on current port of current node. */
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
		
		
		/** Sync default sample schedule */
		protected synchronized int sync(long delayMillis) {
			return syncSchedule(_defaultSampleScheduleName, delayMillis);
		}
		
		/** Print instrument data contained in input buffer. By default does
		 nothing, and
		 may be overridden. */
		protected void printData(byte[] buf) {
			// Do nothing - subclass can override this and optionally
			// print contents
		}
		
		/** Get diagnostics message from device's port and optionally log it. */
		public byte[] getPortDiagnostics(boolean logPacket)
		throws RemoteException {
			
			String statusMsg = _instrumentPort.getStatusMessage();
			
			if (logPacket) {
				annotate(statusMsg.getBytes());
			}
			
			return statusMsg.getBytes();
		}
		
		/** Set _recordType field */
		protected void setRecordType(long type) {
			_recordType = type;
		}
		
		/** Centralized packet logging logic */
		public synchronized void logPacket(DevicePacket devicePacket) {
			
			if (devicePacket instanceof MeasurementPacket) {
				_log4j.debug("logPacket() - measurement=" +
							 devicePacket.toString());
			}
			
			// set the parentID field
			try {
				devicePacket.setParentId(_parentNode.getParentId());
			} catch (RemoteException e) {
				_log4j.error(e);
				_log4j.debug("Failed to set devicePacket parentId", e);
			}
			
			if (devicePacket instanceof SensorDataPacket) {
				
				_log4j.debug("sendPacket: got SensorDataPacket _recordType = "
							 + ((SensorDataPacket)devicePacket).getRecordType());
				
				_lastSensorDataPacket = (SensorDataPacket)devicePacket;
				
			}
			// MessagePackets and others pass through to here
			
			_packetLog.appendPacket(devicePacket, true, true);
			_lastPacket = devicePacket;
			_log4j.debug("Publishing SampleLogged event for Id " + getId());
			
			_parentNode.publish(new LogSampleServiceEvent(this, LogSampleServiceEvent.SAMPLE_LOGGED,
														  (int)getId(), devicePacket));
		}
		
		
		/**
		 * Get diagnostics summary message from device's port and optionally
		 * log it.
		 */
		public byte[] getPortDiagnosticsSummary(boolean logPacket)
		throws RemoteException {
			
			String statusMsg = _instrumentPort.getStatusSummaryMessage();
			
			if (logPacket) {
				annotate(statusMsg.getBytes());
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
		
		/**
		 * Get instruments' DevicePacketParser, which returns JDDAC records.
		 * Not supported by default
		 */
		public DevicePacketParser getDevicePacketParser() throws NotSupportedException {
			throw new NotSupportedException("DevicePacketParser not defined for instrument");
		}
		
		
		///////////////////////////////////////////////////////
		// The following methods are for manipulating schedules
		///////////////////////////////////////////////////////
		/** Return the schedule key, as part of ScheduleOwner implementation. */
		public final ScheduleKey getScheduleKey() {
			return _scheduleKey;
		}
		
		/** Execute the scheduled task, as part of ScheduleOwner implementation.
		 Subclass will actually implement */
		abstract public void doScheduledTask(ScheduleTask task);
		
		
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
		 * @return Scheduler.UNDEFINED, Scheduler.INVALID_SCHEDULE
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
				
				// Log the updated attributes in metadata packet
				getMetadata("addSchedule()".getBytes(), SERVICE_ATTRIBUTES, 
							true);
				
				
			} catch (ScheduleParseException e) {
				_log4j.error("addSchedule " + e, e);
				retval = Scheduler.INVALID_SCHEDULE;
			}
			catch (Exception e) {
				_log4j.error("addSchedule(): Exception from getMetdata(): " + e);
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
				// notify Scheduler to remove existing Schedule
				s.removeSchedule(st);
				// now remove our own reference
				_schedules.remove(st);
			}
			if (schedule != null) {
				// add the new ScheduleTask to
				// our Vector
				_schedules.add(schedule);
				// register it with the Scheduler
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
		
		/** Remove specified schedule. Return integer code defined by Scheduler. */
		public int removeSchedule(String name) {
			
			ScheduleTask task = getSchedule(name);
			int i = Scheduler.OK;
			if (task != null) {
				i = Scheduler.getInstance().removeSchedule(task);
				_schedules.remove(task);
			} else
				i = Scheduler.NOT_FOUND;
			return i;
		}
		
		/** Suspend specified schedule.Return integer code defined by Scheduler. */
		public int suspendSchedule(String schedule) {
			ScheduleTask st = getSchedule(schedule);
			int i = Scheduler.OK;
			if (st != null){
				i = st.suspend();
			}else{
				i = Scheduler.NOT_FOUND;
			}
			return i;
		}
		
		/** Resume specified schedule. Return integer code defined by Scheduler. */
		public int resumeSchedule(String schedule) {
			ScheduleTask st = getSchedule(schedule);
			int i = Scheduler.OK;
			if (st != null){
				i = st.resume();
			}else{
				i = Scheduler.NOT_FOUND;
			}
			return i;
		}
		
		/** "Synchronize" specified schedule. Return integer code defined by
		 Scheduler.*/
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
			
			if (name.equals(getDefaultSampleScheduleName())) {
				try {
					ScheduleTaskImpl newTask = new ScheduleTaskImpl(name, schedule,
																	owner);
					
					newTask.setOwnerName((this.getClass().getName() + ":" + getId()));
					return newTask;
				} catch (ScheduleParseException e) {
					_log4j.error("createTask parse exception parsing schedule ["+name+"]:", e);
				}
			}
			return null;
		}
		
		/** Set the ServiceAttributes object for this service. */
		public void setAttributes(ServiceAttributes attributes) {
			super.setAttributes(attributes);
			if (attributes instanceof InstrumentServiceAttributes) {
				// Only set if an InstrumentServiceAttributes object is coming in
				_instrumentAttributes = (InstrumentServiceAttributes) attributes;
			}
		}
		
		
		/** Enable data summary generation. */
		public void enableSummary() {
			// Does nothing - not implemented yet.
			_log4j.error("enableSummary() - not implemented");
		}
		
		/** Disable data summary generation. */
		public void disableSummary() {
			// Does nothing - not implemented yet.
			_log4j.error("disableSummary() - not implemented");
		}
		
		/** Return true if summary generation is enabled. */
		public boolean summaryEnabled() {
			if (!canSummarize()) {
				// Not capable of summarizing
				return false;
			}
			if (_instrumentAttributes.maxSummarySamples > 0 ||
				_instrumentAttributes.summaryTriggerCount > 0) {
				// At least one summarization property has been set to 
				// positive value.
				return true;
			}
			else {
				return false;
			}
		}
		
		
		/** Stop the service. Note that this method should NOT be synchronized,
		 since must be able to shut down a service that is currently being
		 sampled. */
		public byte[] shutdown() {
			
			_log4j.debug("shutdown()");
			
			setStatusShutdown();
			
			// Interrupting sampling thread(s)
			interruptDeviceAccess(5000);
			
			if (getAllSchedules() != null) {
				_log4j.debug("shutdown() - cancel all schedules");
				Iterator iterator = getAllSchedules().iterator();
				_log4j.debug("shutdown() - iterator=" + iterator);
				while (iterator.hasNext()) {
					
					ScheduleTask task = (ScheduleTask) iterator.next();
					
					_log4j.debug("shutdown() - task=" + task);
					_log4j.debug("shutdown() - removing schedule "
								 + task.getOwnerName() + "#"
								 + task.getOwner().getScheduleKey().value() + ":"
								 + task.getName());
					
					Scheduler.getInstance().removeSchedule(task);
					
					iterator.remove();
					
					_log4j.debug("shutdown() - schedule removed");
				}
			}
			else {
				_log4j.debug("shutdown() - no schedules to remove");
			}
			
			String msg = null;
			
			try {
				msg = shutdownInstrument();
			}
			catch (Exception e) {
				msg = "Exception while shutting down instrument " +
				_serviceName;
				
				_log4j.error(msg, e);
			}
			
			if (_turbinator != null) {
				// Close the DataTurbine ring buffer
			    removeDataListener(_turbinator);
			    _turbinator.close();
			    _turbinator = null;
			}
			
			// Flush any error messages to log
			_errorCache.flush();
			
			// Power off ports 'n stuff
			super.shutdown();
			annotate("shutdown".getBytes());
			
			// Close packet log and remove its object
			try {
				if (_packetLog != null) {
					_packetLog.close();
				}
				_packetLog = null;
			}
			catch (IOException e) {
				msg = "Exception while closing packet log for " +
				_serviceName;
			}
			
			_log4j.debug("shutdown() - done");
			
			return msg.getBytes();
		}
		
		
		/** Add to list of threads that are accessing instrument device.
		 This thread list will be accessed by shutdown() and suspend() in
		 order to quickly terminate threads that are interacting with the
		 device.
		 */
		protected void startDeviceAccess() throws Exception {
			
			Thread thisThread = Thread.currentThread();
			
			synchronized (_deviceAccessSemaphore) {
				int status = getStatus();
				if (status == Device.SHUTDOWN ||
					status == Device.SUSPEND ||
					status == Device.SAFE) {
					throw new Exception("Sevice in shutdown, suspend, or safe  state");
				}
				
				// Add this current thread to list of device access threads
				_log4j.debug("TEST - add to samplingThread vector");
				_deviceAccessThreads.add(thisThread);
				_log4j.debug(_serviceName + " samplingThread count = " +
							 _deviceAccessThreads.size());
				
				_log4j.debug("totalmem:" + Runtime.getRuntime().totalMemory() +
							 ", freemem=" + Runtime.getRuntime().freeMemory());
			}
		}
		
		/** Remove from list of threads that are accessing instrument device. */
		protected void endDeviceAccess() {
			Thread thisThread = Thread.currentThread();
			synchronized (_deviceAccessSemaphore) {
				if (!_deviceAccessThreads.remove(thisThread)) {
					_log4j.info("Didn't remove " + 
								_serviceName + 
								" sampling thread (total of " + 
								_deviceAccessThreads.size() + " threads)");
				}
			}
		}
		
		/** Interrupt any threads that are accessing the device, optionally
		 wait specified time for thread to die after interrupt. 
		 * caller is responsible for setting service state. */
		protected void interruptDeviceAccess(int waitMsec) {
			
			_log4j.debug("interruptDeviceAccess() - get access sem");
			synchronized (_deviceAccessSemaphore) {
				_log4j.debug("interruptDeviceAccess() - GOT access sem");
				
				// Interrupt all sampling threads
				Iterator iterator = _deviceAccessThreads.iterator();
				while (iterator.hasNext()) {
					Thread thread = (Thread )iterator.next();
					if (thread.isAlive()) {
						// Interrupt the thread
						_log4j.debug("Interrupt sampling thread" +
									 thread.getName());
						thread.interrupt();
						
						if (waitMsec > 0) {
							// Wait for device access thread to complete
							try {
								_log4j.debug("interruptDeviceAccess() - wait " + 
											 waitMsec + 
											 " msec for thread to complete");
								
								thread.join(waitMsec);
							}
							catch (InterruptedException e) {
								_log4j.error("interruptDeviceAccess() - join interrupted!");
							}
						}
						
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
		
		
		/** Perform any instrument-specific shutdown actions and optionally
		 return a human-readable message (e.g. noting caveats, status, etc);
		 the returned message is purely for human operator.
		 */
		protected String shutdownInstrument()
		throws Exception {
			return "OK";
		}
		
		
		/** If execution thread of specified task is sleeping, return time
		 at which it will resume; otherwise return 0. */
		public final long sleepingUntil(ScheduleTask task) {
			if (task == _defaultSamplerTask) {
				// Return wakeup time set by snooze()
				return _samplerWakeupTime;
			}
			else {
				// Some other task... don't know how to handle yet
				return 0;
			}
		}
		
		
		/** Put thread to sleep for specified seconds, in such a way that
		 cpu is guaranteed to be awake when the thread is done sleeping.
		 Throw InterruptedException if the thread is interrupted during
		 Thread.sleep(). */
		protected final void snooze(int seconds)
		throws InterruptedException {
			
			if (seconds <= 0) {
				// Don't bother to create timer and worker thread
				return;
			}
			
			// Save current status
			int status = getStatus();
			
			// Set status to SLEEPING (so SleepManager knows we can sleep for
			// at least a while)
			setStatus(Device.SLEEPING);
			
			// Schedule wakeup time; this is the time that is returned by
			// the ScheduleOwner.sleepingUntil() method, which is used by
			// SleepManager when determining if the cpu can sleep even though
			// a scheduled task is executing.
			_samplerWakeupTime = System.currentTimeMillis() + seconds * 1000;
			
			// Note that Thread.sleep() does not reliably return at the
			// correct time when the MMC node is sleep-managed. Instead we
			// utilize the SiamTimer and SiamTimerTask classes, which are
			// properly managed by SleepManager.
			// Create a SiamTimer and a SiamTimerTask that will notify this
			// thread when sleep time's up. SleepManager will ensure that
			// cpu is awake when it's time to execute the wakeup SiamTimerTask.
			
			SiamTimer timer = new SiamTimer();
			timer.schedule(new WakeupTask(), seconds * 1000);
			
			_log4j.debug("snooze() - sleep for " + seconds*1000 + " msec");
			
			// Wait for WakeupTask.run() notification to the _sleepLock object
			_log4j.debug("snooze() - get _sleepLock");
			synchronized (_sleepLock) {
				_log4j.debug("snooze() - call _sleepLock.wait()");
				_sleepLock.wait();
				_log4j.debug("snooze() - done with _sleepLock.wait(); cancel timer");
				timer.cancel();
			}
			
			_log4j.debug("snooze() - done with sleep");
			
			_samplerWakeupTime = 0;
			
			// Restore status as it was prior to sleeping
			setStatus(status);
		}
		
		class WakeupTask extends SiamTimerTask {
			public void run() {
				_log4j.debug("WakeupTask.run()");
				
				synchronized (_sleepLock) {
					_log4j.debug("WakeupTask.run() - notifyAll()");
					_sleepLock.notifyAll();
					_log4j.debug("WakeupTask.run() - done");
				}
			}
		}
		
		
		
		/** Set device's clock to specified time.
		 DEPRECATED - use setClock() with no arguments.
		 */
		public void setClock(long t)
		throws NotSupportedException {
			_log4j.error("setClock(long t) - OBSOLETE");
			throw new NotSupportedException("setClock(long t) is obsolete");
		}
		
		
		/** Set device's clock to current time; can throw NotSupportedException. */
		public void setClock()
		throws NotSupportedException {
			throw new NotSupportedException("service does not implement setClock()");
		}
		
		
		
		/* *** Abstract methods *** */
		
		/**
		 * Acquire data sample from instrument, process it, and put data into
		 * output.
		 */
		protected abstract SensorDataPacket acquire(boolean logSample)
		throws NoDataException;
		
		/** Return initial value of instrument power policy. */
		protected abstract PowerPolicy initInstrumentPowerPolicy();
		
		/** Return initial value of instrument communication power policy. */
		protected abstract PowerPolicy initCommunicationPowerPolicy();
		
		/** Return instrument startup time in millisec. */
		protected abstract int initInstrumentStartDelay();
		
		/** Return DPA current limit (milliamp). */
		protected abstract int initCurrentLimit();
		
		/** Return instrument's sample terminator characters. */
		protected abstract byte[] initSampleTerminator();
		
		/** Return instrument's "prompt" characters. */
		protected abstract byte[] initPromptString();
		
		/** Return maximum number of bytes in a instrument data sample. */
		protected abstract int initMaxSampleBytes();
		
		/** Return default sampling schedule. */
		protected abstract ScheduleSpecifier createDefaultSampleSchedule()
		throws ScheduleParseException;
		
		/**
		 * <p>Retrive the {@link net.java.jddac.jmdi.fblock.FunctionBlock} used for specialized processing of acquired
		 * samples. By default it will return a SummaryBlock if the following
		 * conditions are met:
		 * <ul>
		 *  <li>The getInstrumentService has not been overridden</li>
		 *  <li>A DevicePacketParser is returned from getDevicePacketParser()</li>
		 *  <li>getAttributes() returns an instance of InstrumentServiceAttributes. Most
		 *   Services will do this by default.</li>
		 *  <li>attributes.maxSummarySamples has been set to greater than 0</li>
		 *  <li>attributes.summaryTriggerCount has been set to greater than 0</li>
		 *  <li>attributes.summaryVars has been set with a valid String[] </li>
		 * </ul>
		 *
		 * </p>
		 *
		 *
		 * @return The JDDAC FunctionBlock used for specialized
		 * processing of aquired samples.
		 */
		public InstrumentServiceBlock getInstrumentServiceBlock() {
			
			
			if (_instrumentServiceBlock == null) {
				
				_log4j.debug("An InstrumentServiceBlock has not been defined for" +
							 this.toString() + ". Creating one");
				
				boolean isParserSet = false;
				
				DevicePacketParser parser = null;
				try{
					parser = getDevicePacketParser();
				}
				catch (Exception e) {
					_log4j.debug("Failed to retrieve the DevicePacketParser");
				}
				
				if ((parser != null) &&
                    (getAttributes() instanceof InstrumentServiceAttributes)) {
					
					_log4j.debug("Found a DevicePacketParser and InstrumentServiceAttributes. Checking that the Attributes are valid");
					
					final InstrumentServiceAttributes attributes = (InstrumentServiceAttributes) getAttributes();
					
					if (attributes.summaryVars != null &&
                        attributes.maxSummarySamples > 0 &&
                        attributes.summaryTriggerCount > 0) {
						
						_log4j.debug("Initializing SummaryBlock for " + 
									 this.toString());
						
						SummaryBlock fblock = new SummaryBlock();
						
						/*
						 * If there is an error in configuration this will set the paramters
						 * so that a summary will always occur
						 */
						final int summaryTriggerCount = Math.min(attributes.maxSummarySamples, attributes.summaryTriggerCount);
						fblock.setSampleCount(attributes.maxSummarySamples);
						
						/*
						 * Set the summary block to produce summaries for each variable in
						 * the Attributes object
						 */
						for (int i = 0; i < attributes.summaryVars.length; i++) {
							if (!"".equals(attributes.summaryVars[i])) {
								fblock.addVariableName(attributes.summaryVars[i]);
								_log4j.debug("SummaryBlock will generate statistics for " +
											 attributes.summaryVars[i]);
							}
						}
						
						final StatsBlock statsBlock = fblock.getStatsBlock();
						statsBlock.addNewArgArrayListener(new NewArgArrayListener() {
														  
														  private int count = 0;
														  public void processEvent(NewArgArrayEvent event) {
														  count++;
														  _log4j.debug("processEvent() for " + 
																	   _serviceName + 
																	   ": count=" + count);
														  if (count == summaryTriggerCount) {
														  _log4j.debug("processEvent() for " + 
																	   _serviceName + 
																	   ": doStats()");
														  
														  try {
														  statsBlock.doStats();
														  
														  _log4j.debug("processEvent() for " + 
																	   _serviceName + 
																	   ": done with doStats()");
														  }
														  catch (Throwable e) {
														  _log4j.error("Caught throwable from " + 
																	   "statsBlock.doStats(): ",
																	   e);
														  }
														  
														  
														  /*
						 * Optimization for cases where summaryTriggerCount ==
						 * maxSummarySamples. In this case we can clear samples
						 * out of the memory after statistics have been
						 * completed.
						 */
														  if (count >= attributes.maxSummarySamples) {
														  statsBlock.clear();
														  }
														  
														  count = 0;
														  }
														  }
														  });
						isParserSet = true;
						setInstrumentServiceBlock(fblock);
					}
					
				}
				
				/*
				 * Add a DummyBlock if no SummaryBlock can be set to bypass all the creation code on subsequent calls
				 */
				if (!isParserSet) {
					_log4j.debug("Summary statistics are not enabled for " + 
								 this.toString());
					
					setInstrumentServiceBlock(new DummyBlock());
				}
				
			}
			return _instrumentServiceBlock;
		}
		
		/**
		 * @param instrumentServiceBlock The JDDAC FunctionBlock used for specialized
		 * processing of aquired samples.
		 */
		public void setInstrumentServiceBlock(InstrumentServiceBlock instrumentServiceBlock) {
			this._instrumentServiceBlock = instrumentServiceBlock;
			_instrumentServiceBlock.setInstrumentService(this);
		}
		
		/**
		 * A check method to indecate if this instrument service can produce summary packets. This is a bit of hack but
		 * should work for the MTM4 deployment.
		 *
		 * @return true if the {@link BaseInstrumentService} can produce 
		 {@link org.mbari.siam.distributed.SummaryPacket}s.
		 *  false otherwise.
		 */
		public boolean canSummarize() {
			FunctionBlock fblock = getInstrumentServiceBlock();
			return fblock != null && fblock instanceof SummaryBlock;
		}
		
	}

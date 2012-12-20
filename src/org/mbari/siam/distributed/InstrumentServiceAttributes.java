/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import org.mbari.siam.core.InstrumentPort;
import java.io.Serializable;

/**
 * InstrumentServiceAttributes and its base class ServiceAttributes provide a
 * framework for representation of instrument service state, which is an
 * important component of service metadata. Each member variable of
 * InstrumentServiceAttributes whose name does not being with "_" is treated as
 * an "attribute", and each instrument service contains an instance of
 * InstrumentServiceAttributes (or subclass). As the service runs, it sets the
 * attribute values to reflect the service state. When generating a metadata
 * packet, the InstrumentService framework automatically generates a
 * representation of service state based on the ServiceAttribute values.
 * <p>
 * Moreover some attributes are "configurable", in the sense that the
 * attribute's initial value can be specified at service runtime, in a "service
 * properties" file. InstrumentServiceAttributes supports runtime configuration
 * by providing methods that convert from attribute values to Java property
 * strings, and vice versa. Each configurable attribute will have a property,
 * the property name being the name of the attribute's member variable. E.g. an
 * attribute declared as
 * 
 * <pre>
 * long isiID;
 * </pre>
 * 
 * will be represented as a property named "isiID" and a property value string
 * that can be converted to a "long" value. An attribute is configurable only if
 * the requiredIsConfigurable() or isConfigurable() method returns 'true'.
 * <p>
 * When a service is started, the InstrumentService framework reads the service
 * property file and invokes ServiceAttributes.fromProperties(). During this
 * process, ServiceAttributes will verify that the property values are
 * consistent with the attribute types, and will detect property names that do
 * not correspond to any attribute name. Moreover, several override-able
 * ServiceAttributes callback methods are invoked:
 * 
 * <ul>
 * <li>isConfigurable(); returns true if value of specified attribute can be set through a property
 * <li>setAttributeCallback(); allows attribute value to be checked as its
 * property is parsed
 * <li>missingAttributeCallback(); called when an input property is not found
 * for an attribute
 * <li>checkValues(); called after all properties have been parsed
 * </ul>
 * 
 * <h2>Extending the class</h2>
 * By default, the framework instantiates an InstrumentServiceAttributes object
 * for every instrument service; this object is referenced by the
 * InstrumentService._instrumentAttributes member. However, you can extend
 * InstrumentServiceAttributes by adding additional attribute members, and
 * overriding the various callback methods, adding addtional methods, etc. Any
 * member variable that begins with underscore ("_") will NOT be treated as an
 * attribute. Attribute members must be of one of the following:
 * 
 * <ul>
 * 
 * <li>primitive
 * <li>String
 * <li>any object that implements org.mbari.siam.distributed.Mnemonic (e.g.
 * PowerPolicy, ScheduleSpecifier). Note that this object *MUST* have a public
 * no-argument constructor.
 * </ul>
 * 
 * Your subclass can also include arrays of the above types. Your subclass must
 * define a constructor that takes a DeviceServiceIF object as an argument, and
 * invokes super() with the DeviceServiceIF argument. By default, an attribute
 * is configurable. To define attributes that are NOT configurable, override
 * isConfigurable(), and return 'false' if the input field name matches the name
 * of any of your non-configurable attributes. To define mandatory configurable
 * attributes, override missingAttributeCallback(), and throw
 * MissingPropertyException if the input name matches that of any of your
 * mandatory configurable attributes. To verify that all attributes have been
 * assigned valid values, override checkValues(), and throw
 * InvalidPropertyException if any attributes have invalid values. Note that when
 * overriding methods with attribute name string arguments, you must take care to
 * specify strings that correspond to member names.
 * 
 * <p>
 * Finally, instantiate your attributes subclass within your instrument service,
 * specifying a reference to the instrument service as a constructor argument:
 * 
 * <pre>
 * MyAttributes _attributes = new MyAttributes(this);
 * </pre>
 * 
 * This will replace the framework's InstrumentServiceAttributes object with
 * your attributes subclass object. (Note that if you do not instantiate a new
 * attributes object in this manner, the framework will continue to use the
 * default InstrumentServiceAttributes object referenced by the
 * InstrumentService._instrumentAttributes member.)
 * <p>*
 * 
 * @see ServiceAttributes
 * @see DeviceServiceIF
 * @see Mnemonic
 * @author Tom O'Reilly
 * 
 *  
 */
public class InstrumentServiceAttributes extends ServiceAttributes implements
								       Serializable {

    /**
     * Default number of times to try sample acquisition. Change using
     * setMaxSampleTries().
     */
    static final int _DEFAULT_MAX_SAMPLE_TRIES = 3;

    /**
     * Default timeout milliseconds for sample acquisition. Change using
     * setSampleTimeout().
     */
    static final long _DEFAULT_SAMPLE_TIMEOUT = 1000;

    /** Current limit (milliamp) */
    public int currentLimitMa = 12000;

    /** Nominal current draw (milliamp) */
    public int nominalCurrentMa = 1000;

    /** Peak current draw (milliamp); this value is the peak current
	spike or individual spikes lasting longer than 50 microseconds 
	drawn by the instrument over one sample period. */
    public int peakCurrentMa = 5000;


    /** Maximum sampling tries */
    public int maxSampleTries = _DEFAULT_MAX_SAMPLE_TRIES;

    /* Instrument startup delay (msec) */
    public int startDelayMsec = 0;

    /** Sample timeout (msec) */
    public long sampleTimeoutMsec = _DEFAULT_SAMPLE_TIMEOUT;

    /** Sampling schedule. */
    public ScheduleSpecifier sampleSchedule = null;

    /** Instrument power policy. */
    public PowerPolicy powerPolicy = PowerPolicy.NEVER;

    /** Instrument communication policy. */
    public PowerPolicy commPowerPolicy = PowerPolicy.WHEN_SAMPLING;

    /**
     * Time-synchronization flag; if true then synchronize instrument's clock to
     * host clock.
     */
    public boolean timeSynch = false;

    /* Delay before first powerup of instrument */
    public int powerOnDelaySec = 0;


    /** SensorDataPackets skipped by default when subsampling */
    public int defaultSkipInterval = 0;

    /** Max number of packets in each retrieved packet set. */
    public int packetSetSize = 10;

    /** 
     * Sets the maximum number of samples that will be stored in the
     *  {@link org.mbari.siam.distributed.jddac.SummaryBlock}. This will be the maximum number of samples used
     *  to calculate a summary.
     */
    public int maxSummarySamples = 0;

    /**
     * Sample interval between summaries
     */
    public int summaryTriggerCount = 0;

    /**
     * The names of the variables that will be summaryized. These should match
     * the names that are put out by the {@link DevicePacketParser} if you want
     * any actual summaries to occur.
     */
    public String[] summaryVars = null;
    
    /**
     * Number of error messages to cache before logging to MessagePacket in telemetry stream
     */
    public int errCacheLimit = 5;

    /**
       Interval (in samples) at which to also sample port/node diagnostics.
       A value of '0' means never sample.
     */
    public int diagnosticSampleInterval = 1;

    /** 
	By default don't telemeter packets older than dataShelfLifeHours.
	Negative value indicates 'infinite' shelf life. 
     */
    public float dataShelfLifeHours = -1.f;

    /**
       Make data available to subscribers as it is acquired. Zero-length or
       null value means do not publish.
     */
    public String rbnbServer = "";

    /** Number of frames of memory cache for RBNB DataTurbine */
    public int rbnbCacheFrames = 20;

    /** Number of frames of disk archive for RBNB DataTurbine */
    public int rbnbArchiveFrames = 10000;

    /**  Advertise RBNB service on network (e.g. with ZeroConf) */
    public boolean rbnbAdvertiseService = false;

    /** RecordTypes to exclude from the Turbinator */
    public long[] rbnbExcludeRecordTypes = null;

    /**
     * Construct instrument service attributes, and associate it with specified
     * service. As this is the only public constructor, it ensures that
     * InstrumentServiceAttributes subclass instances will always be associated
     * with a service.
     */
    public InstrumentServiceAttributes(DeviceServiceIF service) {
	super(service);
    }

    /**
     * Create instrument service attributes. This constructor takes no
     * arguments, since applications occassionally need an instance which is not
     * associated with any particular service. But applications can't call this
     * constructor directly; instead they use the static getAttributes() method.
     *  
     */
    private InstrumentServiceAttributes() {
    }

    final protected void setAttributeRequiredCallback(String fieldName)
	throws InvalidPropertyException {
	super.setAttributeRequiredCallback(fieldName);
    }

    /**
     * Throw MissingPropertyException if specified attribute is mandatory.
     */
    final protected void missingAttributeRequiredCallback(String fieldName)
	throws MissingPropertyException {

	super.missingAttributeRequiredCallback(fieldName);
    }

    /** Throw InvalidPropertyException if any invalid values */
    final protected void requiredCheckValues() throws InvalidPropertyException {

	super.requiredCheckValues();
    }

    /**
     * Return true if specified field is "configurable", i.e. should be
     * converted to a property. This method can not be overrided outside of the
     * framework, i.e. it is declared "final" within the framework.
     * 
     * @param fieldName
     * @return true if field configurable
     */
    final protected boolean requiredIsConfigurable(String fieldName) {

	return super.requiredIsConfigurable(fieldName);
    }

    /**
     * Return an InstrumentServiceAttributes object which is not associated with
     * any particular service.
     * 
     * @return instrument service attributes
     */
    static public InstrumentServiceAttributes getAttributes() {
	return new InstrumentServiceAttributes();
    }

}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/

package org.mbari.siam.distributed;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * The ServiceAttributes class provides a framework for representation of
 * service state, which is an important component of service metadata. Each
 * member variable whose name does not begin with underscore ("_") is treated as
 * an "attribute". NOTE: any private member names MUST be preceded by 
 * underscore. 
 * Each instrument service contains an instance of
 * ServiceAttributes (or subclass). As the service runs, the instrument service
 * sets the attribute values to reflect the service state. When generating a
 * metadata packet, the InstrumentService framework automatically generates a
 * representation of service state based on the ServiceAttribute values.
 * <p>
 * Moreover some attributes are "configurable", in the sense that the
 * attribute's initial value can be specified at service runtime, in a "service
 * properties" file. ServiceAttributes supports runtime configuration by
 * providing methods that convert from attribute values to Java property
 * strings, and vice versa. Each configurable attribute will have a property,
 * the property name being the name of the attribute's member variable. E.g. an
 * attribute declared as
 * 
 * <pre>
 * 
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
 * <ul>
 * <li>isConfigurable(); returns true if specified property can be specified as
 * a property
 * <li>setAttributeCallback(); allows attribute value to be checked as its
 * property is parsed (Note that the framework autmoatically verifies 
 * appropriate value type before setAttributeCallback() is invoked).)
 * <li>missingAttributeCallback(); called when an input property is not found
 * for an attribute
 * <li>checkValues(); called after all properties have been parsed
 * </ul>
 * The ServiceAttributes class can be extended by adding additional attribute
 * members, and overriding the various callback methods. Attribute members must
 * be declared non-static, and of one of the following types:
 * <ul>
 * <li>primitive
 * <li>String
 * <li>any object that implements org.mbari.siam.distrbuted.Mnemonic (e.g.
 * PowerPolicy, ScheduleSpecifier). Note that this object *MUST* have a public
 * no-argument constructor.
 * </ul>
 * Attributes can also be arrays of the above types.
 * 
 * @see InstrumentServiceAttributes
 * @see Mnemonic Importable
 * @author Tom O'Reilly
 */
public class ServiceAttributes implements Serializable, Cloneable {

    /** Log4j logger */
    private static Logger _log4j = Logger.getLogger(ServiceAttributes.class);

    /** Deal with configurable attributes only. */
    protected static final boolean _CONFIGURABLE_FILTER_ON = true;

    /** Deal with all properties, configurable and non-configurable. */
    protected static final boolean _CONFIGURABLE_FILTER_OFF = false;

    /**
     * Service mnemonic name. Define as a byte array rather than String, since
     * ServiceAttributes object might need to be passed over low-bandwidth link.
     */
    public byte[] serviceName = "".getBytes();

    /** Unique ISI device ID. */
    public long isiID = 0;

    /** Parent's ISI device ID. */
    public long parentID = 0;

    /** Instrument UUID */
    public String UUID = "00000000-0000-0000-0000-000000000000";

    /** Instrument Name */
    public String instrumentName = "UNKNOWN";

    /** SIAM framework software version */
    public byte[] frameworkVersion = "".getBytes();

    /** Application software version */
    public byte[] extendedVersion = "".getBytes();

    /** Instrument service status. */
    public int serviceStatus = Device.INITIAL;

    /** ServiceAttributes subclass name. */
    public byte[] className = "".getBytes();

    /** Name for lookups in the Registry */
    public String registryName = null;

    /**  Advertise service on network (e.g. with ZeroConf) */
    public boolean advertiseService = false;

    /** Device location name */
    public String locationName = "";

    /** Create attributes. */
    public ServiceAttributes() {}

    /** Create attributes object and associate it with specified service. */
    public ServiceAttributes(DeviceServiceIF service) {
        service.setAttributes(this);
        serviceName = service.getName();

        className = this.getClass().getName().getBytes();
        extendedVersion = getExtendedVersion().getBytes();
    }

    /**
     * Convert to string representation, all attributes (configurable and
     * non-configurable).
     */
    public String toString() {
        Properties properties = toProperties();
        return ServiceAttributes.toPropertyStrings(properties);
    }

    /**
     * Show valid property names and types.
     */
    public String getHelp() {

        Class c = this.getClass();
        String string = "Valid properties:\n";
        while (c != null && !c.getName().equals(Object.class.getName())) {

            string += getHelp(c);
            c = c.getSuperclass();
        }
        return string;
    }

    public Field[] getConfigurableFields(Class c) {
        List fieldList = new ArrayList();
        Field fields[] = c.getDeclaredFields();

        // Get access to all fields
        try {
            AccessibleObject.setAccessible(fields, true);
        } catch (SecurityException e) {
            _log4j.error(e);
        }

        for (int i = 0; i < fields.length; i++) {

            if (!isAttribute(fields[i])
                || !requiredIsConfigurable(fields[i].getName())
                || !isConfigurable(fields[i].getName())) {
                continue;
            }
            fieldList.add(fields[i]);
        }

        return (Field[]) fieldList.toArray(new Field[0]);
    }

    public String getTypeName(Field field) {
        Class fieldType = field.getType();
        String typeName = fieldType.getName();
        boolean isMnemonic = isMnemonic(fieldType);
        boolean isImportable = isImportable(fieldType);
        if (isMnemonic) {
            typeName = "Mnemonic";
        }else
         if (isImportable) {
            typeName = "Importable";
        }
        if (fieldType.isArray()) {

            typeName = fieldType.getComponentType().getName();

            if (typeName.equals("byte")) {
                typeName = "String";
            } else {
                typeName = typeName + " array";
            }
        }
        return typeName;
    }

    /**
     * Show valid property names and types
     */
    private String getHelp(Class c) {

        String string = "";

        // Retrieving the configurable fields this way may be a little slower
        // since it introduces another loop, but I think the improvement in
        // readability makes it worth it. acc 20050527
        Field fields[] = getConfigurableFields(c);

        // Get access to all fields
        try {
            AccessibleObject.setAccessible(fields, true);
        } catch (SecurityException e) {
            _log4j.error(e);
        }

        for (int i = 0; i < fields.length; i++) {
            string += "  " + fields[i].getName() + " - ";
            String typeName = getTypeName(fields[i]);
            string += typeName;
            if (isMnemonic(fields[i].getType())) {
                // List valid mnemonic strings
                string += ": ";
                String[] validValues = getMnemonicValues(fields[i].getType());
                for (int j = 0; j < validValues.length; j++) {
                    string += "\"" + validValues[j] + "\"";
                    if (j < (validValues.length - 1)) {
                        string += ", ";
                    }
                }
            }
            string += "\n";
        }

        return string;
    }

    public String[] getMnemonicValues(Class fieldType) {
        if (!isMnemonic(fieldType)) {
            return new String[0];
        }
        Object object = null;
        try {
            object = fieldType.newInstance();
        } catch (Exception e) {
            return new String[]{"[values unavailable]"};
        }
        String[] validValues = ((Mnemonic) object).validValues();
        return validValues;
    }

    /** Convert to property string representation. */
    public static String toPropertyStrings(Properties properties) {

        String output = "";
        Enumeration keys = properties.propertyNames();

        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = properties.getProperty(key);
            if (value != null) {
                output += (key + "=" + value + "\n");
            }
        }

        return output;
    }

    /**
     * Convert attributes to Java properties, for an object instance in the
     * ServiceAttributes hierarchy. The resulting properties are named after
     * members of the ServiceAttributes class (or subclass). E.g.
     * InstrumentServiceAttributes.toProperties() results in properties named
     * mnemonicName, deviceID, timeSynch, etc.
     */
    public Properties toProperties() {
        return toProperties(ServiceAttributes._CONFIGURABLE_FILTER_OFF);
    }

    /**
     * Convert all configurable attributes to Java properties, for an object
     * instance in the ServiceAttributes hierarchy. The resulting properties are
     * named after members of the ServiceAttributes class (or subclass). E.g.
     * InstrumentServiceAttributes.toProperties() results in properties named
     * mnemonicName, deviceID, timeSynch, etc.
     */
    public Properties toConfigurableProperties() {
        return toProperties(ServiceAttributes._CONFIGURABLE_FILTER_ON);
    }

    /**
     * Convert specified attributes to Java properties. Throws
     * InvalidPropertyException if any input names do not correspond to an
     * attribute.
     * 
     * @param attributeNames
     *            Names of attributes to be converted
     * @return Attributes in 'properties' form
     */
    public Properties toProperties(String[] attributeNames)
        throws InvalidPropertyException {

        Properties properties = null;

        // Make sure that input list of attribute names are valid; construct
        // temporary property list and pass to checkPropertyNames()
        if (attributeNames != null && attributeNames.length > 0) {
            properties = new Properties();
            for (int i = 0; i < attributeNames.length; i++) {
                properties.setProperty(attributeNames[i], "");
            }
            checkPropertyNames(properties, _CONFIGURABLE_FILTER_OFF);
        }

        properties = new Properties();

        Class c = this.getClass();

        boolean extended = false;

        // Determine if we are in an extension of the framework attributes
        if (c == ServiceAttributes.class
            || c == InstrumentServiceAttributes.class) {
            extended = true;
        }

        while (c != null && !c.getName().equals(Object.class.getName())) {

            toProperties(c, this, properties, _CONFIGURABLE_FILTER_OFF,
                attributeNames);
            c = c.getSuperclass();
        }

        return properties;
    }

    /**
     * Convert attributes to Java properties, for an object instance in the
     * ServiceAttributes hierarchy. The resulting properties are named after
     * members of the ServiceAttributes class (or subclass). E.g.
     * InstrumentServiceAttributes.toProperties() results in properties named
     * mnemonicName, deviceID, timeSynch, etc.
     */
    private Properties toProperties(boolean configurableOnly) {

        Properties properties = new Properties();

        Class c = this.getClass();

        boolean extended = false;

        // Determine if we are in an extension of the framework attributes
        if (c == ServiceAttributes.class
            || c == InstrumentServiceAttributes.class) {
            extended = true;
        }

        while (c != null && !c.getName().equals(Object.class.getName())) {

            toProperties(c, this, properties, configurableOnly, null);
            c = c.getSuperclass();
        }

        // If framework has not been extended, then remove
        // extension-related properties (to save bandwidth)
        if (extended) {
            properties.remove("className");
            properties.remove("extendedVersion");
        }
        return properties;
    }

    /**
     * Convert attributes to properties for specified class.
     */
    private void toProperties(Class c, Object instance, Properties properties,
        boolean getConfigurableOnly, String[] attributeList) {

        Field fields[] = c.getDeclaredFields();

        // Get accessibility to all fields
        try {
            AccessibleObject.setAccessible(fields, true);
        } catch (SecurityException e) {
            _log4j.error(e);
        }

        for (int i = 0; i < fields.length; i++) {

            if (!isAttribute(fields[i])) {
                continue;
            }

            String keyName = fields[i].getName();
            String keyValue = "";

            if (attributeList != null) {
                // Specific attributes have been requested
                boolean match = false;
                for (int j = 0; j < attributeList.length; j++) {
                    if (attributeList[j].equals(keyName)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    // This field doesn't match any of the requested attributes;
                    // go to next one
                    continue;
                }
            }

            // Check whether this field is to be converted to a property,
            // i.e. whether it is "configurable".
            if (getConfigurableOnly
                && (!requiredIsConfigurable(keyName) || !isConfigurable(keyName))) {
                continue;
            }

            // If field value is null, don't convert to property
            try {
                Object value = fields[i].get(instance);
                if (value == null) {
                    continue;
                }

                if (value.getClass().isArray()) {

                    int arrayLength = Array.getLength(value);
                    String typeName = value.getClass().getComponentType()
                        .getName();

                    // If byte array, print out as a string
                    if (typeName.equals("byte")) {
                        byte[] buf = new byte[arrayLength];
                        for (int j = 0; j < arrayLength; j++) {
                            buf[j] = Array.getByte(value, j);
                        }

                        keyValue = new String(buf, 0, arrayLength);
                    } else {
                        for (int j = 0; j < arrayLength; j++) {
                            keyValue += (Array.get(value, j) + " ");
                        }
                    }
                } else {
                    keyValue = value.toString();
                }

                properties.setProperty(keyName, keyValue);

            } catch (IllegalAccessException e) {
                _log4j.error("toProperties() - # VALUE NOT ACCESSIBLE");
            }
        }
    }

    /**
     * Set values in an object instance of a class from the ServiceAttributes
     * hierarchy, according to input property string values. Input properties
     * have names that map to object instance variables. E.g.
     * InstrumentServiceAttributes.fromProperties() parses properties named
     * mnemonicName, deviceID, timeSynch, etc.
     * 
     * @param properties
     *            Input properties to be parsed
     * @param unknownPropertyCheck
     *            set to true if method should check for unknown property names
     */
    final public void fromProperties(Properties properties,
        boolean unknownPropertyCheck) throws PropertyException {

        boolean error = false;
        String errorMsg = "\n";

        Class c = this.getClass();
        while (c != null && !c.getName().equals(Object.class.getName())) {

            try {
                fromProperties(c, this, properties);
            } catch (PropertyException e) {
                error = true;
                errorMsg += e.getMessage() + "fp failed\n";
            }

            // Now do for parent class, until we get to Object
            c = c.getSuperclass();
        }

        // Done parsing; check for invalid values.
        try {
            requiredCheckValues();
        } catch (InvalidPropertyException e) {
            error = true;
            errorMsg += e.getMessage();
	    _log4j.error(e.getMessage() + "; requiredCheckValues() failed");
        }

        try {
            checkValues();
        } catch (InvalidPropertyException e) {
            error = true;
            errorMsg += e.getMessage();
	    _log4j.error(e.getMessage() + "; checkValues() failed");
        }

        if (unknownPropertyCheck) {
            try {
                // Verify that each property corresponds to an attribute
                checkPropertyNames(properties, _CONFIGURABLE_FILTER_ON);
            } catch (InvalidPropertyException e) {
                error = true;
                errorMsg += e.getMessage();
		_log4j.error(e.getMessage() + "; checkPropertyNames() failed");
            }
        }
        // If any errors, throw an exception
        if (error) {
            throw new PropertyException(errorMsg);
        }

    }

    /**
     * Set values in ServiceAttributes object according to input property
     * values.
     */
    private void fromProperties(Class c, Object instance, Properties properties)
        throws PropertyException {

        boolean error = false;
        String errorMsg = "\n";

        Field fields[] = c.getDeclaredFields();

        // Get accessibility to all fields
        try {
            AccessibleObject.setAccessible(fields, true);
        } catch (SecurityException e) {
            _log4j.error(e);
        }

        for (int i = 0; i < fields.length; i++) {

            if (!isAttribute(fields[i])) {
                continue;
            }

            if (!requiredIsConfigurable(fields[i].getName())
                || !isConfigurable(fields[i].getName())) {
                // Only read in configurable attributes
                continue;
            }

            String valueString = properties.getProperty(fields[i].getName());

            if (valueString == null) {

                try {
                    // Required callback
                    missingAttributeRequiredCallback(fields[i].getName());
                } catch (MissingPropertyException e) {
                    error = true;
                    errorMsg += "Missing required property: " + e.getMessage()
                        + "\n";
                }

                try {
                    missingAttributeCallback(fields[i].getName());
                } catch (MissingPropertyException e) {
                    error = true;
                    errorMsg += "Missing required property: " + e.getMessage()
                        + "\n";
                }
                // OK, check next attribute
                continue;
            }

            // Parse string value into appropriate value
            try {
                setField(valueString, fields[i], instance);
            } catch (InvalidPropertyException e) {
                error = true;
                _log4j.error("Invalid property for field "
                    + fields[i].getName() + ": " + e.getMessage());
				errorMsg+="ipe "+e.getMessage()+" ["+fields[i].getName()+"]\n";

            } catch (InstantiationException e) {
                error = true;
                _log4j.error(e);
				errorMsg+="ie "+e.getMessage()+"\n";
            }

            catch (IllegalAccessException e) {
                error = true;
                _log4j.error("VALUE NOT ACCESSIBLE");
				errorMsg+="iae "+e.getMessage()+"\n";
            }
        }

        if (error) {
            throw new PropertyException(errorMsg);
        }

    }

    /**
     * Set field of object according to property string value.
     */
    private void setField(String stringValue, Field field, Object thisObject)
        throws InvalidPropertyException, InstantiationException,
        IllegalAccessException {

        Class typeClass = field.getType();

        _log4j.debug("setField(): " + field.getName() + ": value="
            + stringValue + ", type=" + typeClass.getName());

        boolean isImportable = isImportable(typeClass);

        if (typeClass.isArray()) {

            // Replace any commas with space
            String tmp = stringValue.replace(',', ' ');
            stringValue = tmp;

            if (typeClass.getComponentType().getName().equals("byte")) {
                // We always represent a byte[] array attribute as a string
                setByteArrayField(thisObject, field, stringValue);
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(stringValue, " \t");

            // Determine how many tokens we've got in the string
            int nTokens = tokenizer.countTokens();
            if (nTokens <= 0) {
                throw new InvalidPropertyException("no property values");
            }

            _log4j
                .debug("Array.newArrayInstance() - " + nTokens + " elements");

            Object newArray = Array.newInstance(typeClass.getComponentType(),
                nTokens);

            // Parse the tokens and put them into the array
            String token = null;
            int index = 0;
            while (tokenizer.hasMoreTokens()) {

                token = tokenizer.nextToken();
                if (token == null)
                    break;

                // Set the value of each element in array
                Object value = getValue(typeClass.getComponentType(),
                    isImportable, token);

                Array.set(newArray, index, value);
                index++;
            }

            // Set the array field in the Attributes object
            field.set(thisObject, newArray);
            return;
        } else {

            // Set the field value
            field.set(thisObject, getValue(typeClass, isImportable, stringValue));
        }
        // Invoke required set field callback
        setAttributeRequiredCallback(field.getName());

        // Invoke set-field callback
        setAttributeCallback(field.getName(), stringValue);

    }

    /**
     * Return value object corresponding to this class and string
     * representation.
     */
    private Object getValue(Class typeClass, boolean isImportable, String token)
        throws InvalidPropertyException, InstantiationException,
        IllegalAccessException {

        String typeName = typeClass.getName();

        Object value = null;

        try {
            if (typeName.equals("boolean")) {
                // Boolean.valueOf() does not explicitly detect errors in
                // spelling!
                if (!token.equalsIgnoreCase("true")
                    && !token.equalsIgnoreCase("false")) {
                    throw new NumberFormatException();
                }
                value = Boolean.valueOf(token);
            } else if (typeName.equals("byte")) {
                value = Byte.valueOf(token);
            } else if (typeName.equals("short")) {
                value = Short.valueOf(token);
            } else if (typeName.equals("int")) {
                value = Integer.valueOf(token);
            } else if (typeName.equals("long")) {
                value = Long.valueOf(token);
            } else if (typeName.equals("float")) {
                value = Float.valueOf(token);
            } else if (typeName.equals("double")) {
                value = Double.valueOf(token);
            }

            else if (typeName.equals(String.class.getName())) {
                value = token;
            } else if (isImportable) {
                // Implements Importable
                Importable importable = (Importable) (typeClass.newInstance());
                value = importable.fromString(token);
            } else {
                throw new InvalidPropertyException("Can't handle type \""
                    + typeName + "\"");
            }
        } catch (NumberFormatException e) {
            throw new InvalidPropertyException(token + ": invalid value. "
                + "Must be of type " + typeName);
        }

        if (value == null) {
            _log4j.error("setField() - null value");
            throw new InvalidPropertyException("null value");
        }
        return value;
    }

    /** Set byte[] array field to input string value. */
    private void setByteArrayField(Object thisObject, Field field, String string)
        throws IllegalAccessException {
        byte[] bytes = string.getBytes();

        Object newArray = Array.newInstance(field.getType().getComponentType(),
            string.length());

        for (int i = 0; i < string.length(); i++) {
            Array.set(newArray, i, new Byte(bytes[i]));
        }
        field.set(thisObject, newArray);
    }

    /** Check if this field is to be handled as an attribute. */
    private boolean isAttribute(Field field) {

        if (field.getName().startsWith("_")) {
            return false;
        }

        Class typeClass = field.getType();

        if (typeClass.isArray()) {
            typeClass = typeClass.getComponentType();
        }

        if (typeClass.isPrimitive()
            || typeClass.getName().equals(String.class.getName())) {

            return true;
        }

        if (isImportable(typeClass)) {
            return true;
        }

        return false;
    }

    /**
     * Throw MissingPropertyException if specified attribute is mandatory; at
     * least one subclass must implement this method.
     */
    protected void missingAttributeRequiredCallback(String fieldName)
        throws MissingPropertyException {

        if (fieldName.equals("isiID")) {
            throw new MissingPropertyException(fieldName);
        }
    }

    /**
     * Throw InvalidPropertyException if specified field has invalid value.
     * 
     * @param fieldName
     * @throws InvalidPropertyException
     */
    protected void setAttributeRequiredCallback(String fieldName)
        throws InvalidPropertyException {
        if (fieldName.equals("frameworkVersion")) {
            _log4j.warn("Ignoring input \"frameworkVersion\" property");
        }
    }

    /**
     * Throw InvalidPropertyException if any invalid values; at least one
     * subclass must implement this method.
     */
    protected void requiredCheckValues() throws InvalidPropertyException {

        if (isiID <= 0) {
            throw new InvalidPropertyException("Invalid isiID: " + isiID);
        }
    }

    /**
     * Return true if specified field is "configurable", i.e. should be
     * converted to a property. This method can not be overrided outside of the
     * framework, i.e. it is declared "final" within the framework.
     * 
     * @param fieldName
     * @return true if configurable
     */
    protected boolean requiredIsConfigurable(String fieldName) {
        if (fieldName.equals("parentID")
            || fieldName.equals("frameworkVersion")
            || fieldName.equals("extendedVersion")
            || fieldName.equals("className")
            || fieldName.equals("serviceStatus")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Return true if specified attribute is "configurable", i.e. can be
     * initialized at startup by a property. Subclasses can override this
     * method.
     * 
     * @param attributeName name of attribute to test
     * @return true if configurable
     */
    protected boolean isConfigurable(String attributeName) {
        return true;
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

    /**
     * Called when specified attribute has been found. Throw
     * InvalidPropertyException if specified attribute has invalid value.
     * Note that the ServiceAttributes base class automatically validates
     * the value type before setAttributeCallback() is invoked; so this
     * method needs only to validate the value.
     * 
     * @param attributeName
     *            name of parsed attribute
     */
    protected void setAttributeCallback(String attributeName, String valueString)
        throws InvalidPropertyException {

    }

    /**
     * Return true if input class implements Mnemonic.
     * 
     * @param c
     *            Class to be examined
     * @return true if class implements Mnemonic
     */
    private boolean isMnemonic(Class c) {
        // Get list of interfaces implemented by this class
	while (c != null) {
	    Class interfaces[] = c.getInterfaces();
	    for (int i = 0; i < interfaces.length; i++) {
		if (interfaces[i].getName().equals(Mnemonic.class.getName())) {
		    return true;
		}
		if (isMnemonic(interfaces[i])) {
		    return true;
		}
	    }
	    c = c.getSuperclass();
	}

        return false;
    }

    /**
     * Return true if input class implements Importable
     * 
     * @param c
     *            Class to be examined
     * @return true if implements Importable
     */
    private boolean isImportable(Class c) {
        // Get list of interfaces implemented by this class
	while (c != null) {
	    Class interfaces[] = c.getInterfaces();
	    for (int i = 0; i < interfaces.length; i++) {
		if (interfaces[i].getName().equals(Importable.class.getName())) {
		    return true;
		}
		if (isImportable(interfaces[i])) {
		    return true;
		}
	    }
	    c = c.getSuperclass();
	}
        return false;
    }

    /**
     * Called when all attributes have been parsed. Throw
     * InvalidPropertyException if any invalid attribute values found
     */
    public void checkValues() throws InvalidPropertyException {}

    /**
     * Return software version identifier of extensions to the framework, i.e.
     * of the instrument service subclass code. Extensions can (and generally
     * should) override this.
     * 
     * @return "UNK" by defaule
     */
    protected String getExtendedVersion() {
        return "UNK";
    }

    /**
     * Throw InvalidPropetyException if any property name does not correspond to
     * anattribute.
     * 
     * @param properties
     *            Properties object
     * @param configurableOnly
     *            Property names must match configurable attribute names
     * @throws InvalidPropertyException
     */
    final public void checkPropertyNames(Properties properties,
        boolean configurableOnly) throws InvalidPropertyException {
        boolean error = false;
        String errorMsg = "";

        Enumeration names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String propertyName = (String) names.nextElement();

            boolean foundAttribute = false;
            Class c = this.getClass();
            while (!foundAttribute && c != null
                && !c.getName().equals(Object.class.getName())) {

                Field fields[] = c.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].getName().equals(propertyName)) {
                        if (!isAttribute(fields[i])) {
                            error = true;
                            errorMsg += "Can't handle field: " + propertyName
                                + "\n";
                        }
                        if (configurableOnly
                            && (!requiredIsConfigurable(fields[i].getName()) || !isConfigurable(fields[i]
                                .getName()))) {
                            error = true;
                            errorMsg += "Unconfigurable field: " + propertyName
                                + "\n";
                        }
                        foundAttribute = true;
                        break;
                    }
                }
                // Go to next class in hierarchy, until we get to Object
                c = c.getSuperclass();
            }
            if (!foundAttribute) {
                error = true;
                errorMsg += "Unknown property: " + propertyName + "\n";
            }
        }
        if (error) {
            throw new InvalidPropertyException(errorMsg);
        }
    }

    
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.util.Hashtable;

/**
ZeroConf defines naming conventions for SIAM ZeroConf interface
*/
public class ZeroConf {

    public static final String SIAM_NODE_TYPE = "_siamNode._tcp";
    public static final String SIAM_DEVICE_TYPE = "_siamDevice._tcp";
    public static final String SIAM_INSTRUMENT_TYPE = "_siamInstrument._tcp";
    public static final String RBNB_TYPE = "_dataTurbine._tcp";

    public static final String RMI_URL_KEY = "rmiURL";
    public static final String DEVICE_ID_KEY = "deviceID";
    public static final String SERVICE_MNEMONIC_KEY = "serviceName";
    public static final String RBNB_CHANNELNAME_PREFIX = "channelName-";
    public static final String RBNB_RECORDTYPE_PREFIX = "recordType-";
    public static final String RBNB_CHANNELUNITS_PREFIX = "channelUnits-";
    public static final String LOCATION_NAME_KEY = "locationName";

}

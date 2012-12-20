// Copyright 2002 MBARI
package org.mbari.siam.moos.distributed.mooring;

import java.io.Serializable;


/**
Associates device ID with channel.
*/
public class ChannelConfiguration implements Serializable {

    /** Channel */
    public int _channel;

    /** Device ID */
    public long _deviceID;
}

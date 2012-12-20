// Copyright 2001 MBARI
package org.mbari.siam.distributed.portal;

import org.mbari.siam.distributed.Node;

/**
"Utility" class containing static methods relevant to Portals.
@author Tom O'Reilly
*/

public class Portals {
    static final int PORTAL_TCP_PORT = 5510;
    static final int NODE_TCP_PORT = 5511;

    /** use HTTP port to determine if node is awake*/
    static final int NODE_PROBE_PORT = 80; 

    /** Default socket timeout for portal-node RMI */
    public static final int DEFAULT_WIRELESS_SOCKET_TIMEOUT = 60000;

    /** Return URL string for mooring LeaseManager server. */
    public static String mooringURL(String mooringHost) {
	return "//" + mooringHost + "/" + Node.SERVER_NAME;
    }

    /** Return URL string for portal LeaseManager server. */
    public static String portalURL(String portalHost) {
        return "//" + portalHost + "/portal";
    }	

    /** Return TCP Port for portal notification */
    public static int portalTCPPort() {
        return PORTAL_TCP_PORT;
    }	

    /** Return TCP Port for node socket server */
    public static int nodeTCPPort() {
        return NODE_TCP_PORT;
    }	

    /** Return TCP Port for node probe client */
    public static int nodeProbePort() {
        return NODE_PROBE_PORT;
    }	
}

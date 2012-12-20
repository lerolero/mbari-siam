// Copyright 2003 MBARI
package org.mbari.siam.foce.deployed;

import org.mbari.siam.core.PortManager;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;


/**
 * FOCEPortManager implements PortManager for FOCE hardware 
 * (Lippert PC/104, RTD relay board, etc)
 * 
 * @author Bob Herlien
 */
public class FOCEPortManager extends PortManager
{
    private static Logger _log4j = Logger.getLogger(FOCEPortManager.class);

    FOCENodeProperties _nodeProps;

    /** Create FOCEPortManager object. */
    FOCEPortManager(String siamHome, FOCENodeProperties nodeProps)
	throws MissingPropertyException, InvalidPropertyException
    {
	super(siamHome, nodeProps);
	_nodeProps = nodeProps;
    }


    /** Get port configuration from properties file and store in vector. */
    public void initPortVector() throws IOException, MissingPropertyException,
					InvalidPropertyException
    {
	IOException ie = null;

	try {
	    _nodeProps.getPowerBoards();
	    _nodeProps.getAnalogBoards();
	} catch (IOException e) {
	    ie = e;
	}

	super.initPortVector();

	if (ie != null)
	    throw ie;
    }
}

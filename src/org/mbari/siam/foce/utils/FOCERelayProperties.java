// Copyright MBARI 2010
package org.mbari.siam.foce.utils;

import java.lang.NumberFormatException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;

import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;


/** Representation of relay.properties file */
public class FOCERelayProperties extends Properties
{
    public static final String POWER_PREFIX = "powerRelay";
    protected static final int MAX_PROPS = 100;

    Vector _allProps = null;

    public FOCERelayProperties()
    {
	super();
    }

    public FOCERelayProperties(String urlString)
	throws MalformedURLException, IOException
    {
	super();
	URL url = new URL(urlString);
	load(url.openStream());
    }

    public FOCERelayProperties(InputStream istream) throws IOException
    {
	super();
	load(istream);
    }

    public RelayProperty getRelayProperty(String key)
	throws InvalidPropertyException
    {
	String property = getProperty(key);
	return((property == null) ? null : new RelayProperty(property));
    }


    public Collection getAllProperties()
    {
	if (_allProps != null) {
	    return(_allProps);
	}

	_allProps = new Vector();

	for (int i = 0; i < MAX_PROPS; i++) {
	    try {
		RelayProperty prop = getRelayProperty(POWER_PREFIX + i);
		if (prop != null) {
		    _allProps.add(prop);
		}
	    } catch (Exception e) {
	    }
	}

	return(_allProps);
    }

    public RelayProperty findPropertyByDescriptor(String key)
    {
	for (Iterator it = getAllProperties().iterator(); it.hasNext(); )
	{
	    RelayProperty prop = (RelayProperty)(it.next());

	    if (prop.getDescriptor().equalsIgnoreCase(key)) {
		return(prop);
	    }
	}

	return(null);
    }

    public RelayProperty findPropertyByRelay(int board, int relay)
    {
	for (Iterator it = getAllProperties().iterator(); it.hasNext(); )
	{
	    RelayProperty prop = (RelayProperty)(it.next());

	    if ((prop.getRelay() == relay) && (prop.getBoard() == board)) {
		return(prop);
	    }
	}

	return(null);
    }

    public class RelayProperty
    {
	String _property;
	int    _board, _relayNum;
	String _descriptor = null;

	public RelayProperty(String property) throws InvalidPropertyException
	{
	    _property = property;

	    try {
		StringTokenizer st = new StringTokenizer(property);
		_board = Integer.parseInt(st.nextToken());
		_relayNum = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens()) {
		    _descriptor = st.nextToken();
		}

	    } catch (Exception e) {
		throw new InvalidPropertyException("Unparseable relay property: " + property +
						   " - " + e);
	    }
	}

	public int getBoard()
	{
	    return(_board);
	}

	public int getRelay()
	{
	    return(_relayNum);
	}

	public String getDescriptor()
	{
	    return(_descriptor);
	}
    }
}

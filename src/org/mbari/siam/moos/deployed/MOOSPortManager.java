// Copyright 2003 MBARI
package org.mbari.siam.moos.deployed;

import gnu.io.CommPortIdentifier;
import gnu.io.CommPortOwnershipListener;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.mbari.siam.utils.FileUtils;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.ByteUtility;
import org.mbari.siam.utils.PuckUtils;

import org.mbari.siam.operations.utils.ServiceJarUtils;

import org.apache.log4j.Logger;
import org.doomdark.uuid.UUID;

import org.mbari.siam.core.DevicePort;
import org.mbari.siam.core.PortManager;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.NullPowerPort;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.puck.Puck;
import org.mbari.puck.Puck_1_3;

/**
 * MOOSPortManager implements PortManager for MOOS hardware 
 * (MOOS Mooring Controller, Dual Power Adapters, etc)
 * 
 * @author Bob Herlien
 */
public class MOOSPortManager extends PortManager
{
    private static Logger _log4j = Logger.getLogger(MOOSPortManager.class);

    //dpa specific variables
    private static final int TOTAL_DPA_CHANS = 12;

    private static final int TOTAL_DPA_BOARDS = 6;

    private static DpaBoard.DpaChannel[] _dpaChannels = 
        new DpaBoard.DpaChannel[TOTAL_DPA_CHANS];

    //spi specific variables
    private static int[] _spiSlaveIndex = { SpiMaster.SPI_SLAVE_SELECT_0,
                                            SpiMaster.SPI_SLAVE_SELECT_1, 
                                            SpiMaster.SPI_SLAVE_SELECT_2,
                                            SpiMaster.SPI_SLAVE_SELECT_3, 
                                            SpiMaster.SPI_SLAVE_SELECT_4,
                                            SpiMaster.SPI_SLAVE_SELECT_5};

    private static SpiMaster _spi = SpiMaster.getInstance();

    /** Create MOOSPortManager object. */
    MOOSPortManager(String siamHome, NodeProperties nodeProps)
	throws MissingPropertyException, InvalidPropertyException
    {
	super(siamHome, nodeProps);
    }


    /** Get port configuration from properties file and store in vector. */
    public void initPortVector() throws IOException, MissingPropertyException,
				 InvalidPropertyException
    {
	super.initPortVector();

	//scan backplane for DPA boards if DPA is in siamPorts.cfg
	//this is a cheezy hack for platform portablity, need to do this
	// right!!!
        if ( dpasInConfig() )
	    {
                //initialize the SPI bus
		synchronized (_spi) {
		    _spi.setClkDivider(_spi.SPI_CLOCK_DIVIDER_64);
		}

		scanDpas();
	    }
        else
	    {
		_log4j.info("PortManager: no DPAs set in siamPorts.cfg");
	    }
    }


    /**
     * Check the siamPorts.cfg to see if any ports are configured for DPAs.
     */
    boolean dpasInConfig()
    {
        for ( int i = 0; i < getPorts().size(); i++ )
	{
	    DevicePort port = (DevicePort)getPorts().elementAt(i);
	    if ( port.hasPowerPort() )
		return true;
	}
        // No DPAs found in configuration
        return false;
    }


    /** Scan the backplane for DPA power channels */
    void scanDpas()
    {
	DevicePort port = null;
	for ( int i = 0; i < TOTAL_DPA_BOARDS; i++ )
	{
	    int channelIndex = 2 * i;

	    if ( DpaBoard.checkForDpaHardware(_spiSlaveIndex[i]) )
	    {
		_log4j.info("Found DPA at slot: " + i);
		//if there is a DPA create a board and get it's channels
		DpaBoard dpa = new DpaBoard(_spiSlaveIndex[i]);

		//left channel
		_dpaChannels[channelIndex] = dpa.getLeftChannel();
		_dpaChannels[channelIndex].initializeChannel();
		setSidearmPowerPort(channelIndex);

		//right channel
		_dpaChannels[++channelIndex] = dpa.getRightChannel();
		_dpaChannels[channelIndex].initializeChannel();
		setSidearmPowerPort(channelIndex);

	    }
	    else
	    {
		_log4j.info("No DPA found at slot: " + i);

		//left channel
		_dpaChannels[channelIndex] = null;
		setNullPowerPort(channelIndex);
		//right channel
		_dpaChannels[++channelIndex] = null;
		setNullPowerPort(channelIndex);
	    }
	}
    }


    /** Create and associate a SidearmPowerPort for the specified port */
    void setSidearmPowerPort(int index)
    {
	_log4j.debug("Create SidearmPowerPort for port " + index);

	PowerPort powerPort = new SidearmPowerPort("DPA chan " + index, 
						   _dpaChannels[index]);

	DevicePort port = (DevicePort)getPorts().elementAt(index);
	port.setPowerPort(powerPort);
	_log4j.debug("PortManager.setSidearmPort: setting port " +port+
		     " commsMode=" + port.getCommsMode());
	port.setCommsMode(port.getCommsMode());
    }


    /** Create and associate a NullPowerPort for the specified port */
    void setNullPowerPort(int index)
    {
	_log4j.debug("Create NullPowerPort for port " + index);

	DevicePort port = (DevicePort)getPorts().elementAt(index);
	port.setPowerPort(new NullPowerPort());
    }
}

// Copyright MBARI 2004
package org.mbari.siam.distributed.devices;

import java.rmi.RemoteException;
import org.mbari.siam.distributed.Instrument;

/** 
    Interface to extend services of mooring power service
    @author Mike Risi, Kent Headley
*/


public interface Power extends Instrument 
{

    public final static int POWER_SWITCH = 0;
    public final static int POWER_SWITCH_ON = 1;
    public final static int POWER_SWITCH_OFF = 2;
    public final static int POWER_SWITCH_QUERY = 3; 
    public final static int BIN_SWITCH = 4;
    public final static int BIN_SWITCH_QUERY = 5; 
    public final static int BIN_SWITCH_ON=6;
    public final static int BIN_SWITCH_OFF=7;
    public final static int BIN_SWITCH_ALLOFF=8;
    public final static int BIN_SWITCH_ALLON=9;
    public final static String[] SWITCH_STATES ={"POWER_SWITCH","POWER_SWITCH_ON","POWER_SWITCH_OFF",
						 "POWER_SWITCH_QUERY","BIN_SWITCH","BIN_SWITCH_QUERY",
						 "BIN_SWITCH_ON","BIN_SWITCH_OFF","BIN_SWITCH_ALLOFF",
						 "BIN_SWITCH_ALLON"};

    public final static byte[] CMD_HIGH_VOLTAGE_ON="HIGH VOLTAGE ON\r".getBytes();
    public final static byte[] CMD_HIGH_VOLTAGE_OFF="HIGH VOLTAGE OFF\r".getBytes();
    public final static byte[] CMD_HIGH_VOLTAGE_STATUS="HIGH VOLTAGE\r".getBytes();

    public final static String CMD_BIN_400VSW_ON="ON";
    public final static String CMD_BIN_400VSW_OFF="OFF";
    public final static String CMD_BIN_400VSW_ALLON="ALLON";
    public final static String CMD_BIN_400VSW_ALLOFF="ALLOFF";
    public final static String CMD_BIN_400VSW_STATUS="BIN 400VSW";

    public final static int BIN_BACKUP = 0;
    public final static int BIN_BACKUP_EN = 1;
    public final static int BIN_BACKUP_DI = 2;
    public final static int BIN_BACKUP_ALLEN = 3;
    public final static int BIN_BACKUP_ALLDI = 4;
    public final static int BIN_BACKUP_QUERY = 5; 
    public final static String[] BACKUP_STATES ={"BIN_BACKUP","BIN_BACKUP_EN",
						 "BIN_BACKUP_DI","BIN_BACKUP_ALLEN",
						 "BIN_BACKUP_ALLDI","BIN_BACKUP_QUERY"};

    public final static String CMD_BIN_BACKUP_EN="EN";
    public final static String CMD_BIN_BACKUP_DI="DI";
    public final static String CMD_BIN_BACKUP_ALLEN="ALLEN";
    public final static String CMD_BIN_BACKUP_ALLDI="ALLDI";
    public final static String CMD_BIN_BACKUP_STATUS="BIN BACKUP";

    /** Enable high voltage power to sub sea nodes. */
    public void enableHiVoltage()
	throws RemoteException, Exception;


    /** Disable high voltage power to sub sea nodes. */
    public void disableHiVoltage()
	throws RemoteException, Exception;

    /** Check state of high voltage switch on sub sea nodes */
    public boolean isHighVoltageEnabled() 
	throws RemoteException, Exception;

    /** Query BIN 400V switch status
     */
    public byte[] queryBIN400V() throws RemoteException, Exception;

    /** Switch specified BIN 400V power channel(s) 
	returns switch status of all BIN 400v power channels
     */
    public byte[] switchBIN400V(int[] switchStates)
	throws RemoteException, Exception;

    /** Disable/Enable backup batteries and/or holdup capacitors
	returns switch status of backups
     */
    public byte[] binBackups(int enableBatteries, int enableCapacitors)
	throws RemoteException, Exception;
}

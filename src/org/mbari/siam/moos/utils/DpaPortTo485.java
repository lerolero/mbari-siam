/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.dpa;

import org.mbari.siam.moos.deployed.SpiMaster;
import org.mbari.siam.moos.deployed.DpaBoard;
import java.lang.Integer;
import gnu.getopt.Getopt;
import org.mbari.siam.utils.StopWatch;

public class DpaPortTo485
{
    //dpa specific variables
    private static final int TOTAL_DPA_CHANS = 12;
    private static final int TOTAL_DPA_BOARDS = 6;

    private static DpaBoard.DpaChannel[] _dpaChannels = 
        new DpaBoard.DpaChannel[TOTAL_DPA_CHANS];
    
    public static void main(String[] args) 
    {
        //use args from main to set com port
        DpaPortTo485 app = new DpaPortTo485();

        app.scanDpas();

        app.execute(args);
        System.exit(0);
    }
    
    //spi specific variables
    private static int[] _spiSlaveIndex = 
    {
        SpiMaster.SPI_SLAVE_SELECT_0,
        SpiMaster.SPI_SLAVE_SELECT_1,
        SpiMaster.SPI_SLAVE_SELECT_2,
        SpiMaster.SPI_SLAVE_SELECT_3,
        SpiMaster.SPI_SLAVE_SELECT_4,
        SpiMaster.SPI_SLAVE_SELECT_5
    };

    private static SpiMaster _spi = SpiMaster.getInstance();

    public void execute(String[] args)
    {
        Getopt g = new Getopt(this.getClass().getName(), args, "c:s:h");
        int c;
        String slot = null;
        String chan = null;

        while ((c = g.getopt()) != -1)
        {
            switch (c)
            {
                case 'c': chan = g.getOptarg(); break;
                case 's': slot = g.getOptarg(); break;
                
                case 'h': 
                case '?': // this is a problem
                default:
                    showHelp(); 
                    return; 
                    
            }
        }

        //check the slot argument
        if ( !checkSlot(slot) )
        {
            System.out.println("Slot '" + slot + "' not valid");
            showHelp();
            return;
        }

        //check the channel argument
        if ( !checkChannel(chan) )
        {
            System.out.println("Channel '" + chan + "' not valid");
            showHelp();
            return;
        }
       
        int chan_int = Integer.parseInt(chan.trim());
        int slot_int = Integer.parseInt(slot.trim());

        System.out.println("Setting DPA slot '" + slot_int + 
                           "' channel '" + chan_int + "' to RS-485");
        
        setPort485(slot_int, chan_int);

    }
    
    void showHelp()
    {
        System.out.println("usage: java -cp . moos.utils.SetDpaPort " +
                           "-c channel -s slot -l loop_mode");
        return;
    }

    void setPort485(int slot, int chan)
    {
        int channelIndex = (2 * slot) + chan;
        
        if (_dpaChannels[channelIndex] != null)
        {
            _dpaChannels[channelIndex]._relayReg.connect485Terminators();
            _dpaChannels[channelIndex]._relayReg.connectCommunicationsGround();
            _dpaChannels[channelIndex]._relayReg.isolateInstrumentPower();
            _dpaChannels[channelIndex]._relayReg.write();
            
            _dpaChannels[channelIndex]._channelCtrlReg.setCommModeRs485();
            _dpaChannels[channelIndex]._channelCtrlReg.setCommFullDuplex();
            _dpaChannels[channelIndex]._channelCtrlReg.setCommPowerOn();
            _dpaChannels[channelIndex]._channelCtrlReg.setSlewRateNotLimited();
            _dpaChannels[channelIndex]._channelCtrlReg.setTxHiPower();
            _dpaChannels[channelIndex]._channelCtrlReg.write();            
        }
        else
        {
            System.err.println("DPA channel at slot '" + slot + 
                               "' channel '" + chan + "' is not available");
        }
    
    }

    /** Scan the backplane for DPA power channels */
    void scanDpas()
    {
        for (int i = 0; i < TOTAL_DPA_BOARDS; i++) 
        {
            int channelIndex = 2 * i;
            
            if ( DpaBoard.checkForDpaHardware(_spiSlaveIndex[i]) ) 
            {
                System.err.println("Found DPA at slot: " + i );
                    
                //if there is a DPA create a board and get it's channels
                DpaBoard dpa = new DpaBoard(_spiSlaveIndex[i]);
                
                //left channel
                _dpaChannels[channelIndex] = dpa.getLeftChannel();
                _dpaChannels[channelIndex].initializeChannel();
                
                //right channel
                _dpaChannels[++channelIndex] = dpa.getRightChannel();
                _dpaChannels[channelIndex].initializeChannel();
            } 
            else 
            {
                System.err.println("No DPA Found at slot: " + i );
                
                //left channel
                _dpaChannels[channelIndex] = null;
                //right channel
                _dpaChannels[++channelIndex] = null;
            }
        }
    }

    boolean checkSlot(String slot)
    {
        if ( slot == null)
            return false;
        
        int i = Integer.parseInt(slot.trim());

        if ( (i < TOTAL_DPA_BOARDS) && (i > -1 ) )
            return true;

        return false;
    }

    boolean checkChannel(String chan)
    {
        if ( chan == null)
            return false;
        
        int i = Integer.parseInt(chan.trim());

        if ( (i == 0 ) || (i == 1 ) )
            return true;

        return false;
    }

    boolean checkLoop(String loop)
    {
        if ( loop == null)
            return false;
        
        int i = Integer.parseInt(loop.trim());

        if ( (i == 0 ) || (i == 1 ) )
            return true;

        return false;
    }
}


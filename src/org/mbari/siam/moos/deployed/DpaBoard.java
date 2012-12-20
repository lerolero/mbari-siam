// Copyright MBARI 2003
package org.mbari.siam.moos.deployed;

import gnu.io.SerialPort;
import org.apache.log4j.Logger;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.moos.distributed.dpa.Dpa;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.IoResource;
import org.mbari.siam.core.IoResourceType;
import org.mbari.siam.core.DebugMessage;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.RangeException;

/**
This version of DpaBoard was submitted by B. Baugh on 6/27/02
and first integrated on 7/8/02 by K. Headley. Bill changed the way 
that ADC is instantiated to fix a problem in which only the last
DPA channel instantiated would have a working ADC. Headley 
folded in changes made earlier to SPI (write one byte to sync up)
and changes to initializeDPAChannel (to put the channel into a 
 specific state).

<pre>
 * Major things not yet tested:
 *   - taking a reading from the ADC
 *   - taking all readings from the ADC
 *   - turning each relay on, power cycle, first write is turn it off
 *   - turning each relay off, power cycle, first write is turn it on
 *   - turning each relay of, power cycle, first write is turn it off
 *   - turning each relay on, power cycle, first write is turn it on
 *   - I presume that interactions between relays are not significant
 *   - getting an enabled interrupt
 *   - getting an enabled interrupt and disabling it
 *   - getting a disabled interrupt
 *   - tripping the digital circuit breaker
 *   - tripping the digital circuit breaker and resetting it
 *   - repeatedly trip/reset the digital circuit breaker, deal with short
 *   - various combinations of drive modes (unipolar/bipolar, RS-485, etc.)
 *

  - DPA
  - Notes:  In the listing below, single-bit register fields are only
  listed by their values for a '1' in that position.  If a '0' in
  that position means something other than the simple negation of
  a '1', then the meaning of '0' is listed in the same line.
  There are only four items that are not single-bit fields:  (1) the
  command (in all cases), (2) the power-down (or simply 'power') mode for
  an ADC Register Write, (3) the channel value for an ADC Register
  Write, and (4) the 'count', or number of clicks to move the
  potentiometer for a Dpot
  (Current Limit) Register Write.  There are individual names for
  each possible value for the cmnd field and for the ADC power
  mode.  The ADC Channel and the Dpot Count field are simply
  unsigned numbers, so only a mask value is specified for those.
  - Defaults:  Most of the values do not have meaningful defaults.
  Those that do have a '*' or a '!' before the value.  An '*'
  means that the asterisked value is the default.  A '!' means
  that a 0 in that bit position is the default.
  - Summary of registers
  - ADC
  - Interrupt
  - Relays
  - Dpot
  - Channel Control
  - DPA board-level commands (cmnd mask 0xf000, values:)
  - ADC
  - Notes: Programmer writes a command to the ADC and waits for
  the DPA's status to become not busy, which indicates
  that the command has been accepted and the conversion
  has been completed.  After the DPA status becomes
  nonbusy, the programmer can read the ADC.  If the power
  mode is power on, the command is also a conversion
  request; all bits are always meaningful, so every
  conversion request must include power mode, acquisition
  mode and drive types.
  - 0x4000 ADC Write (aka take reading)
  - Power Mode (mask 0x00c0, values:)
  -  0x0000: full power down [default when not sampling]
  - *0x0040: standby
  -  0x0080: power on / use internal clock [default when sampling]
  -  0x00c0: power on / use external clock
  - Acquisition Mode
  - !0x0020: External (0 means Internal)
  - SGL/DIF (drive type)
  - *0x0010: Single-ended (0 means Differential)
  - Uni/Bi (drive polarity)
  - *0x0008: Unipolar (0 means Bipolar)
  - Channels (mask 0x0007, values:)
  - 0x0007: Battery Supply Voltage (Vbat = 5.8*Vin)
  - 0x0006: Right Channel voltage sense (Vinstr=6*Vin)
  - 0x0005: Right Channel trip level sense
  - 0x0004: Right Channel current sense
  - 0x0003: Heat sink temperature
  - 0x0002: Left Channel voltage sense (Vinstr=6*Vin)
  - 0x0001: Left Channel trip level sense
  - 0x0000: Left Channel current sense
  - 0xb000 ADC Read (mask for values: 0x0fff)
  - Interrupt Register
  - Notes: The DPA is capable of generating an interrupt to the
  SideKick processor ONLY on overcurrent conditions.  If a
  channel has an overcurrent condition (is drawing more
  current than allowed by the SetCurrentLimit cmnd), the
  electronic circuit breaker will trip.  An interrupt will
  be asserted iff the electronic circuit breaker for that
  channel has tripped (and has not been reset), AND the
  interrupt enable for that channel is set, AND the global
  interrupt enable has been set.  The assertion of an
  interrupt will cause three things to be true: the
  interrupt flag for that channel will be set, the global
  interrupt flag will be set, and the DPA board will
  assert an interrupt to the SideKick processor.  The
  overcurrent condition itself is asserted by the digital
  circuit breaker, and that breaker is cleared by turning
  it off.  (Reading the interrupt register has no effect
  on the interrupt flags.)
  - Commands
  - 0x5000 Write interrupt reg
  - 0xc000 Read interrupt reg
  - Register bits (mask 0x003f, RO=WriteOnly, RW=Read/Write)
  - 0x0020: (RW)Left Channel overcurrent intr en
  - 0x0010: (RW)Right Channel overcurrent intr en
  - 0x0008: (RW)Global intr en
  - 0x0004: (RO)Left Channel overcurrent intr flag
  - 0x0002: (RO)Right Channel overcurrent intr flag
  - 0x0001: (RO)Global intr flag
  - DPA Channel-level commands (cmnd mask 0xf000, values:)
  - Relays
  - Commands
  - 0x2000: Write left channel register
  - 0x3000: Write right channel register
  - 0xa000: Read left channel register
  - 0x9000: Read right channel register
  - Register bits
  - 0x0004: Connect 485 Terminators
  - 0x0002: Isolate communications ground
  - 0x0001: Isolate instrument power
  - Dpot (Digital Circuit Breaker Current Limit)
  - Commands
  - 0x0000: Write left channel
  - 0x1000: Write right channel
  - Register bits and fields
  - 0x0100: Save position
  - 0x0080: Up (move potentiometer UP)
  - 0x007f: mask for counter value (0 through 127)
  - Channel Control
  - Commands
  - 0x6000: Write left channel
  - 0x7000: Write right channel
  - 0xd000: Read left channel
  - 0xe000: Read right channel
  - Register bits
  - 0x0020: Serial tx power on
  - 0x0010: Comm fast (0 means Slew Rate Controlled)
  - 0x0008: Comm Mode RS-485 (0 means RS-232)
  - 0x0004: Comm Half Duplex (0 means full duplex)
  - 0x0002: Comm Power On
  - 0x0001: Instrument Power On (this is the Digital Circuit Breaker)

</pre>

Implementation notes:  when a board is created (ctor) it should
instantiate all the left and right components, because it should
initialize them.  From there on out, when a user of a channel
manipulates any of its components, including components that are
specialized to the left or right channel, they will do the right
thing, since (a) they have access to the DPA board resources (shared
between the channels) and (b) have their own control when that's
appropriate.  Since the operation will be specified in the (abstract)
superclass while the implementation lives in the subclass, the
user can safely call superclass operations and they will either
do the right thing because they are identical for both channels, or
they will do the right thing because the implementation that gets
invoked has been correctly specialized to the correct channel.
Probably the only thing that really needs to handle the left and
right channels as such is the board itself, and it can do so.
E.g., if it needs to initialize the current limit by setting both
channels to zero, it can do { leftChannel.adc.setCurrentLimit(0);
rightChannel.adc.setCurrentLimit(0); }.

*/

public class DpaBoard {

    private static Logger _logger = Logger.getLogger(DpaBoard.class);

    private DpaChannel _dpaLeftChannel; // auto-initialized to null
    private boolean _dpaAvailChecked;    // have we looked for a DPA?
    private boolean _dpaHardwareIsPresent; // is it there?

    private DpaChannel dpaRightChannel; // auto-initialized to null
    private int[] _adcReg = new int[1];
    private int[] _leftRelayReg = new int[1];
    private int[] _rightRelayReg = new int[1];
    private int[] _leftChannelCtrlReg = new int[1];
    private int[] _rightChannelCtrlReg = new int[1];
    private int[] _interruptReg = new int[1];
    {
        _adcReg[0] = 0;
        _leftRelayReg[0] = 0;
        _rightRelayReg[0] = 0;
        _leftChannelCtrlReg[0] = 0;
        _rightChannelCtrlReg[0] = 0;
        _interruptReg[0] = 0;
    }

    abstract public class HdwrReg {
        protected int[] _regVal;
        protected boolean _changed = true; // power-up value uncertain

        HdwrReg() {
            _logger.debug("HdwrReg(void) ctor invoked");
        }
        HdwrReg(int[] regVal) {
            _logger.debug("HdwrReg(int[]) ctor invoked");
            _regVal = regVal;
        }
        protected void set(int mask, int val) {
            if ((_regVal[0] & mask) == val)
                return;
            _regVal[0] &= ~mask;
            _regVal[0] |= val;
            _changed = true;
        }

        protected void set(int bitmask) {
            set (bitmask, bitmask);
        }

        protected void clear(int mask) {
            set(mask, 0);
        }
    };

    class Adc extends HdwrReg {

        private Adc(int[] regVal) {
            super(regVal);

            setSglDrive();
            setUnipolar();
        }
        public void setPowerOff() {
            set(Dpa.ADC_POWER_MASK, Dpa.ADC_POWER_OFF);
        }
        public void setPowerStby() {
            set(Dpa.ADC_POWER_MASK, Dpa.ADC_POWER_STBY);
        }
        public void setPowerOnIntClk() {
            set(Dpa.ADC_POWER_MASK, Dpa.ADC_POWER_ON_INT);
        }
        public void setAcqExternal() {
            set (Dpa.ADC_ACQMOD_EXT, Dpa.ADC_ACQMOD_EXT);
        }
        public void setAcqInternal() {
            set (Dpa.ADC_ACQMOD_EXT, 0);
        }
        public void setSglDrive() {
            set (Dpa.ADC_INMOD_SGL, Dpa.ADC_INMOD_SGL);
        }
        public void setDifferentialDrive() {
            set (Dpa.ADC_INMOD_SGL, 0);
        }
        public void setUnipolar() {
            set (Dpa.ADC_REF_UNIPOLAR, Dpa.ADC_REF_UNIPOLAR);
        }
        public void setBipolar() {
            set (Dpa.ADC_REF_UNIPOLAR, 0);
        }

        void initialize() {
            setAcqInternal();
            setPowerStby();
            writeReadSpi(Dpa.ADC_START | _regVal[0] | 0); // channel is don't-care here
            stallOnBusyBit();
        }

        /** To take a reading, put the ADC into the on/ext state,
	    Wait 100msec,
	    Put it into the stby state,
	    Wait for the board to become nonbusy,
	    Read the value.
	*/
        protected int getReading(int adcChannel) {
            setAcqExternal();
            setPowerOnIntClk();
            writeReadSpi(Dpa.ADC_START | _regVal[0] | adcChannel);
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e) {
                _logger.debug("ADC sleep got interrupted: " + e);
            }
            stallOnBusyBit();

            setAcqInternal();
            setPowerStby();
            writeReadSpi(Dpa.ADC_START | _regVal[0] | adcChannel);
            stallOnBusyBit();

            int retval = writeReadSpi(Dpa.ADC_R); // other 12 bits are don't-cares
            stallOnBusyBit(); // read cannot possibly make it busy, though
            return retval;
        }

    };

    private Adc _adc = new Adc(_adcReg);


    public class BoardAdc {

	/** Return supply voltage (in counts). */
        public int getBatterySupplyVoltage() {
            return _adc.getReading(Dpa.ADC_VBAT_ADDR);
        }

	/** Return heat sink temperature (in counts). */
        public int getHeatSinkTemp() {
            return _adc.getReading(Dpa.ADC_HEATSINK_TEMP_ADDR);
        }
    };

    public BoardAdc _boardAdc = new BoardAdc();

    public abstract class ChannelAdc {
        abstract public int getVoltage();
        abstract public int getTripLevel();
        abstract public int getCurrent();
    };

    public class LeftChannelAdc extends ChannelAdc {
        public int getVoltage() {
            return _adc.getReading(Dpa.ADC_VSENSE_CH0_ADDR);
        }
        public int getTripLevel() {
            return _adc.getReading(Dpa.ADC_VTRIP_CH0_ADDR);
        }
        public int getCurrent() {
            return _adc.getReading(Dpa.ADC_ISENSE_CH0_ADDR);
        }
    }

    public class RightChannelAdc extends ChannelAdc {
        public int getVoltage() {
            return _adc.getReading(Dpa.ADC_VSENSE_CH1_ADDR);
        }
        public int getTripLevel() {
            return _adc.getReading(Dpa.ADC_VTRIP_CH1_ADDR);
        }
        public int getCurrent() {
            return _adc.getReading(Dpa.ADC_ISENSE_CH1_ADDR);
        }
    }

    // To read the intr register, first do a read (otherwise you may get
    // a stale value)
    //

    abstract public class RelayReg extends HdwrReg {
        RelayReg(int[] regVal) {
            super(regVal);
            _logger.debug("RelayReg ctor invoked");
        }
        public void connect485Terminators() {
            set(Dpa.RELAY_485_CON);
        }
        public void disconnect485Terminators() {
            clear(Dpa.RELAY_485_CON);
        }
        public void isolateCommunicationsGround() {
            set(Dpa.RELAY_COMM_ISO);
        }
        public void connectCommunicationsGround() {
            clear(Dpa.RELAY_COMM_ISO);
        }
        public void isolateInstrumentPower() {
            set(Dpa.RELAY_IPOWER_ISO);
        }
        public void connectInstrumentPower() {
            clear(Dpa.RELAY_IPOWER_ISO);
        }
        abstract public void write(); 
        abstract public int read();
   };


    private class LeftRelayReg extends RelayReg {
        LeftRelayReg(int[] regVal) {
            super(regVal);
            _logger.debug("LeftRelayReg ctor invoked");
        }
        public void write() {
            writeReadSpi(Dpa.RELAY_W_CH0 | _regVal[0]);
            stallOnBusyBit();
        }
        public int read() {
            //_logger.debug("doing a LeftRelayReg read()");
            int i=writeReadSpi(Dpa.RELAY_R_CH0);
            stallOnBusyBit();
	    return i;
        }
    };

    // Comedy of errors: 
    // - DPA doc mistakenly uses same command (0xB) for both ADC read and for ch2 relay reg read
    // - Channel 1 and channel 2 (left and right) are swapped here
    // - was left=ch1=0xA; right=ch2=0xB
    // - changed to left=ch1=0x9 right=ch2=0xA
    // - k headley 10/13/2005

    private class RightRelayReg extends RelayReg {
        RightRelayReg(int[] regVal) {
            super(regVal);
            _logger.debug("RightRelayReg ctor invoked");
        }
        public void write() {
            writeReadSpi(Dpa.RELAY_W_CH1 | _regVal[0]);
            stallOnBusyBit();
        }
        public int read() {
            //_logger.debug("doing a RightRelayReg read()");
            int i=writeReadSpi(Dpa.RELAY_R_CH1);
            stallOnBusyBit();
	    return i;
        }
    };

    abstract public class ChannelCtrlReg extends HdwrReg {
        ChannelCtrlReg(int[] regVal) {
            super(regVal);
        }
        public void setTxHiPower() {
            set(Dpa.CONTROL_TXPOWER_HI);
        }
        public void setTxLoPower() {
            clear(Dpa.CONTROL_TXPOWER_HI);
        }
        public void setSlewRateNotLimited() {
            set(Dpa.CONTROL_SLEW_UNLIMITED);
        }
        public void setSlewRateLimited() {
            clear(Dpa.CONTROL_SLEW_UNLIMITED);
        }
        public void setCommModeRs485() {
            set(Dpa.CONTROL_MODE_485);
        }
        public void setCommModeRs232() {
            clear(Dpa.CONTROL_MODE_485);
        }
        public void setCommHalfDuplex() {
            set(Dpa.CONTROL_DUP_HALF);
        }
        public void setCommFullDuplex() {
            clear(Dpa.CONTROL_DUP_HALF);
        }
        public void setCommPowerOn() {
            set(Dpa.CONTROL_CPOWER_ON);
        }
        public void setCommPowerOff() {
            clear(Dpa.CONTROL_CPOWER_ON);
        }
        public void setInstrumentPowerOn() {
            set(Dpa.CONTROL_IPOWER_ON);
        }
        public void setInstrumentPowerOff() {
            clear(Dpa.CONTROL_IPOWER_ON);
        }
        abstract public void write();
        abstract public int read();
    };
    private class LeftChannelCtrlReg extends ChannelCtrlReg {
        private LeftChannelCtrlReg(int[] regVal) {
            super(regVal);
            _logger.debug("LeftChannelCtrlReg ctor invoked");
        }
        public void write() {
            //_logger.debug("doing a LeftChannelCtrlReg write()");
            writeReadSpi(Dpa.CONTROL_W_CH0 | _regVal[0]);
            stallOnBusyBit();
        }
        public int read() {
            //_logger.debug("doing a LeftChannelCtrlReg read()");
            int i=writeReadSpi(Dpa.CONTROL_R_CH0);
            stallOnBusyBit();
	    return i;
        }
    };

    public class RightChannelCtrlReg extends ChannelCtrlReg {
        private RightChannelCtrlReg(int[] regVal) {
            super(regVal);
            _logger.debug("RightChannelCtrlReg ctor invoked");
        }
        public void write() {
            //_logger.debug("doing a RightChannelCtrlReg write()");
            writeReadSpi(Dpa.CONTROL_W_CH1 | _regVal[0]);
            stallOnBusyBit();
        }
        public int read() {
            //_logger.debug("doing a RightChannelCtrlReg read()");
            int i=writeReadSpi(Dpa.CONTROL_R_CH1);
            stallOnBusyBit();
	    return i;
        }
    };

    abstract public class InterruptReg extends HdwrReg {

        public InterruptReg(int[] regVal) {
            super(regVal);
        }

        public void setGlobalOvercurrentEn() {
            set(Dpa.INTERRUPT_OCE_GLOBAL);
        }
        public void clearGlobalOvercurrentEn() {
            clear(Dpa.INTERRUPT_OCE_GLOBAL);
        }
        public boolean isGlobalOvercurrentIntrFlagSet() {
            return ((_regVal[0] & Dpa.INTERRUPT_OCF_GLOBAL) != 0);
        }
        abstract public void setOvercurrentIntrEn();
        abstract public void clearOvercurrentIntrEn();
        abstract public boolean isOvercurrentFlagSet();
        public void write() {
            int retval = writeReadSpi(Dpa.INTERRUPT_W | _regVal[0]);
            _regVal[0] = ((_regVal[0] & Dpa.INTERRUPT_OCE_MASK) | (retval & Dpa.INTERRUPT_OCF_MASK));
            stallOnBusyBit();
        }

        public int read() 
        {
            int i=writeReadSpi(Dpa.INTERRUPT_R);
            stallOnBusyBit();
            return i;
        }
    };
    private class LeftInterruptReg extends InterruptReg {
        private LeftInterruptReg(int[] regVal) {
            super(regVal);
        }
        public void setOvercurrentIntrEn() {
            set(Dpa.INTERRUPT_OCE_CH0);
        }
        public void clearOvercurrentIntrEn() {
            clear(Dpa.INTERRUPT_OCE_CH0);
        }
        public boolean isOvercurrentFlagSet() {
            return ((_regVal[0] & Dpa.INTERRUPT_OCF_CH0) != 0);
        }
    };
    private class RightInterruptReg extends InterruptReg {
        private RightInterruptReg(int[] regVal) {
            super(regVal);
        }
        public void setOvercurrentIntrEn() {
            set(Dpa.INTERRUPT_OCE_CH1);
        }
        public void clearOvercurrentIntrEn() {
            clear(Dpa.INTERRUPT_OCE_CH1);
        }
        public boolean isOvercurrentFlagSet() {
            return ((_regVal[0] & Dpa.INTERRUPT_OCF_CH1) != 0);
        }
    };

    protected boolean doDebug = false;

    protected int _spiSlaveSelectValue;
    protected static SpiMaster _spi = SpiMaster.getInstance();
    protected boolean _boardInitialized = false;

    public DpaBoard (int spiSlaveSelectValue) {
        _logger.debug("DpaBoard ctor invoked");
        _spiSlaveSelectValue = spiSlaveSelectValue;
    }

    // NOTE:  The create-if-null strategy will NOT work in the presence
    // of contention by multiple threads
    //
    public DpaChannel getLeftChannel() {
        if (_dpaLeftChannel == null)
            _dpaLeftChannel = this.new DpaLeftChannel(this);

        return _dpaLeftChannel;
    }

    public DpaChannel getRightChannel() {
        if (dpaRightChannel == null)
            dpaRightChannel = this.new DpaRightChannel(this);

        return dpaRightChannel;
    }

    static int writeReadSpi(int slaveSelect, int dataOut) {
        int dataIn;
        synchronized (_spi) {
	    /* For Ajile
	    // Write one byte to sync up the SPI
	    dataIn = spi.writeReadData(0xff & (dataOut >> 8));

            spi.setSlaveSelect(slaveSelect);
            dataIn = spi.writeReadData(0xff & (dataOut >> 8));
            dataIn <<= 8;
            dataIn |= (0xff & spi.writeReadData(0xff & dataOut));
            spi.setSlaveSelect(spi.SPI_SLAVE_SELECT_NONE);
	    */
	    // For MMC, use SpiMaster
            _spi.setSlaveSelect(slaveSelect);
            dataIn = _spi.writeReadData(dataOut );
            _spi.setSlaveSelect(_spi.SPI_SLAVE_SELECT_NONE);
        }

        // if (dataIn == 0xffff) or
        //  (dataIn & 0x7fff) == 0x7fff) throw exception
        return dataIn;
    }


    /** Detect DPA hardware. Returns true if the hardware is present. */
    public static boolean checkForDpaHardware(int slaveSelectVal) {
	/* //for Ajile...the reasoning is not that reliable
	   int dpaRetVal = writeReadSpi(slaveSelectVal, 0xf << 12);

	   // 0x7fff is the busy indication.  If we get that on
	   // the first read, we know that the real hardware,
	   // which would definitely not be busy, is not there
	   // and the circuitry that is there will assert 'busy'
	   // forever.  Note that sometimes no hardware will
	   // return 0xffff, hence the mask.

	   return ((dpaRetVal & 0x7fff) != 0x7fff);
	*/

	/* For Sidearm: To reliably figure out if there is a 
	   dpa present, we will set and read back the overcurrent
	   interrupt enable bits to two diffent values 
	*/
	int dataIn;

	// write and read 10 pattern to Interrupt reg overcurrent enable bits
	writeReadSpi(slaveSelectVal,(Dpa.INTERRUPT_W|Dpa.INTERRUPT_OCE_CH1));
	dataIn=writeReadSpi(slaveSelectVal,Dpa.INTERRUPT_R);
	if( (dataIn & (Dpa.INTERRUPT_OCE_CH0|Dpa.INTERRUPT_OCE_CH1)) != Dpa.INTERRUPT_OCE_CH1)
	    return false;

	// write and read 01 pattern to Interrupt reg overcurrent enable bits
	writeReadSpi(slaveSelectVal,(Dpa.INTERRUPT_W|Dpa.INTERRUPT_OCE_CH0));
	dataIn=writeReadSpi(slaveSelectVal,Dpa.INTERRUPT_R);
	if( (dataIn & (Dpa.INTERRUPT_OCE_CH0|Dpa.INTERRUPT_OCE_CH1)) != Dpa.INTERRUPT_OCE_CH0)
	    return false;	

	return true;
    }

    protected int writeReadSpi(int dataOut) {
        return writeReadSpi(_spiSlaveSelectValue, dataOut);
    }

    public synchronized void stallOnBusyBit() {
        /**/
        int regVal;
        if (_dpaHardwareIsPresent) {
            do {
                regVal = writeReadSpi(Dpa.DPA_COMMAND_BUSY);
            } while ((regVal & Dpa.DPA_BUSY_MASK) == Dpa.DPA_BUSY);
        }
        /**/
        /**
	   _logger.debug("Spi bus cycle complete");
	   /**/
    }

    /**
     The DpaChannel contains everything associated with the
     channel, and also with its DPA board.
     */
    public abstract class DpaChannel implements IoResource {

        protected boolean _channelInitialized = false;
        protected boolean _currentCtrlInitialized = false;
        protected Instrument _instrument;
        protected gnu.io.SerialPort _instrumentPort;
        protected int _presentPosition;
        //protected static final int MOVE_UP = 1;
        //protected static final int MOVE_DOWN = 0;
        public DpaBoard _parent = null;

        //protected ChannelAdc _channelAdc;
        //protected RelayReg _relayReg;
        //protected ChannelCtrlReg _channelCtrlReg;
        
        public ChannelAdc _channelAdc;
        public RelayReg _relayReg;
        public ChannelCtrlReg _channelCtrlReg;
        
        protected InterruptReg _interruptCtrlReg;
        protected int _defaultCurrentLimit = Dpa.DEFAULT_CURRENT_LIMIT_MA;

        public DpaChannel(DpaBoard parent) {
            _logger.debug("DpaChannel ctor invoked");
	    _parent = parent;
        }


	/** Initialize the port. */
	public void initialize() {
	    _logger.debug("DpaChannel.initialize() - not implemented");
	}

	/** Set current limit on port. */
	public void setCurrentLimit(long currentLimit) {
	    _logger.debug("DpaChannel.setCurrentLimit() - " + 
				 "not implemented");
	}

	/** Enable communications */
	public void enableCommunications() {
	    _logger.debug("DpaChannel.enableCommunications() - " + 
				 "not implemented");
	}

	/** Disable communications */
	public void disableCommunications() {
	    _logger.debug("DpaChannel.disableCommunications() - " + 
				 "not implemented");
	}

	/** Connect instrument to power. */
	public void connectPower() {
	    _logger.debug("DpaChannel.connectPower() - " + 
				 "not implemented"); 
	}

	/** Disconnect instrument from power. */
	public void disconnectPower() {
	    _logger.debug("DpaChannel.disconnectPower() - " + 
				 "not implemented");
	}


        public void initializeChannel() {
	    _logger.debug("Initializing DpaChannel "+this);
            if (! _channelInitialized) {
                initializeBoard(); // no-op if board already initialized

                //     1)  Initialize the ADC.  Done in initializeBoard();

                //     2)  Initialize the relays
		_logger.debug("Disconnecting RS485 terminators");
		_logger.debug("Isolating communications ground");
		_logger.debug("Isolating Instrument power");
                
                _relayReg.disconnect485Terminators();
                _relayReg.isolateCommunicationsGround();
                _relayReg.isolateInstrumentPower();
                _relayReg.write();

                //     3)  Set Current Limit -- this is done by the
                //         SideArm (or some other configurator) later.
		_logger.debug("Setting Current Limit to " +
				     _defaultCurrentLimit);
                setCurrentLimit(_defaultCurrentLimit); // sets trip value for
		// digital circuit breaker
		// 1400 is 11 steps (rounded down)

                //     4)  Configure the comm channel -- this is also done
                //         by the configurator.
                //relayReg.connectInstrumentPower();
                //relayReg.write();
                _logger.debug("initializing channelCtrlReg");
                //channelCtrlReg.setTxHiPower(); // driver should set Hi before using
                _channelCtrlReg.setTxLoPower();
                _channelCtrlReg.setSlewRateNotLimited();
                _channelCtrlReg.setCommModeRs232();
                _channelCtrlReg.setCommFullDuplex();
                //channelCtrlReg.setCommPowerOn();
                //channelCtrlReg.setInstrumentPowerOn();
                _channelCtrlReg.write();

		// Explicitly set Power OFF to clear overcurrent		
		_logger.debug("Setting COMM and INSTR power OFF ");		
		_channelCtrlReg.setCommPowerOff();//set off, do in drvr
		_channelCtrlReg.setInstrumentPowerOff();//set off, do in drvr
		_channelCtrlReg.write();

		// should remember global setting and other channel's setting
		_interruptCtrlReg.setOvercurrentIntrEn();
		_interruptCtrlReg.write();

		_channelInitialized = true;
            }
        }


        protected void checkOnceForDpaHardware() {
            if (! _dpaAvailChecked) {
                // this is just like stallOnBusyBit()
                int spiVal;
                spiVal = writeReadSpi(Dpa.DPA_COMMAND_BUSY);
                _dpaAvailChecked = true;

                // 0x7fff is the busy indication.  If we get that on
                // the first read, we know that the real hardware,
                // which would definitely not be busy, is not there
                // and the circuitry that is there will assert 'busy'
                // forever.  Note that sometimes no hardware will
                // return 0xffff, hence the mask.

                _dpaHardwareIsPresent = ((spiVal & Dpa.DPA_BUSY_MASK) != Dpa.DPA_BUSY);
                DebugMessage.print("checkOnceForDpaHardware() called...");
                if (_dpaHardwareIsPresent) {
                    _logger.debug("DPA is available");
                } else {
                    _logger.debug("found no DPA hardware.");
                }
            }
        }

        protected void initializeBoard() {
            _logger.debug("first line of initializeBoard()");
            if (! _boardInitialized) {
                _logger.debug("Doing DpaChannel.initializeBoard()");
                _spi.setClkDivider(_spi.SPI_CLOCK_DIVIDER_64);

                _spi.setSlaveSelect(_spi.SPI_SLAVE_SELECT_NONE);
                _spi.writeReadData(0); // Do a write with no chip select.
                                      // This forces the DPA into a known
                                      // state (just in case it had seen
                                      // spurious spi clock edges with a
                                      // spurious CS).


                // Check for DPA hardware before any normal SPI cycles.
                //
		checkOnceForDpaHardware();

                _adc.initialize();// this was commented out; it is mostly done in
                // adc.getReading(), but still we should initialize here so that
                // the DPA adc is in STBY at start up.		
                _interruptCtrlReg.setGlobalOvercurrentEn();
                _interruptCtrlReg.write();
                _boardInitialized = true;
            }
        }

        public void setSerialPort(SerialPort aPort) {
            // if aPort is null or instrumentPort isn't, throw exception...
            _instrumentPort = aPort;
        }

        public Object getResourceType(IoResourceType aType) {
            if (aType == IoResourceType.DPA_BOARD)
                return this;
            else if (aType == IoResourceType.INSTRUMENT)
                return _instrument;
            else
                return null;
        }

        private void initializeCurrentCtrl() {
            _logger.debug("in InitializeCurrentCtrl()");

            movePot(Dpa.DPOT_DIR_DOWN, 99);
            stallOnBusyBit();
            _presentPosition = 0;
            _currentCtrlInitialized = true;
        }

	/** Set the current limit. */
        public void setCurrentLimit(int milliamps) {
            _logger.debug("In setCurrentLimit(int milliamps) " +
				 milliamps);
            _logger.debug("On Entry Present Position = " +
				 _presentPosition);
            if (! _currentCtrlInitialized) {
                _logger.debug("  about to initializeCurrentCtrl()...");
                initializeCurrentCtrl();
                _logger.debug("  initializeCurrentCtrl() done.");
            }
	    _logger.debug("After Init Present Position = " +
				 _presentPosition);

            // if milliamps < 0 or > ?? throw exception

            // For safety, we always round DOWN (i.e., std integer division)
            // The device can be set in increments of 120 milliamps,
            // from 0*120 to 99*120 = 11880 (i.e., from 0 to 11.88 Amps)
            int newPosition = milliamps / Dpa.DPOT_MILLIAMPS_PER_COUNT;
            int diff = newPosition - _presentPosition;
            if (diff > 0)
                movePot(Dpa.DPOT_DIR_UP, diff );
            else if (diff < 0)
                movePot(Dpa.DPOT_DIR_DOWN, -diff );

            stallOnBusyBit();
	    _logger.debug("Present " + _presentPosition);
	    _logger.debug("New " + newPosition);
	    _logger.debug("diff " + diff);

            _presentPosition = newPosition;

            //wait for voltage on the DPA's MAX919 comparator to settle before
            //allowing any other operations (e.g. turning on power).  The cap
            //on the DPA 101312 REV 3.1 schematic is 4.7uF which will take 
            //a bit of time to charge.
	    _logger.debug("setCurrentLimit(...) settling");
            StopWatch.delay(600);

            /*END*/
        }

        int getCurrentLimitCtrlValue() {
            return _presentPosition * Dpa.DPOT_MILLIAMPS_PER_COUNT;
        }

        protected abstract void movePot(int direction, int nStepsUp );

        public void setInstrument( InstrumentService anInstrument ) {
            _instrument = anInstrument;
        }
    };

    public class DpaLeftChannel extends DpaChannel {
        DpaLeftChannel(DpaBoard parent) {
	    super(parent);
            _logger.debug("DpaLeftChannel ctor invoked");
            _channelAdc = new LeftChannelAdc();
            _relayReg = new LeftRelayReg(_leftRelayReg); // but not relay reg
            _channelCtrlReg = new LeftChannelCtrlReg(_leftChannelCtrlReg);
            _interruptCtrlReg = new LeftInterruptReg(_interruptReg);
        }

        protected void movePot(int direction, int nStepsUp ) {
            _logger.debug("DpaLeftChannel movePot(dir="
			  + (direction==Dpa.DPOT_DIR_UP?"UP":"DOWN")
			  + "; steps=" + nStepsUp );
            // CMND = 0 is left channel Dpot cmnd
            // up is 1 in bit 8, down is 0
            // do not save position to flash
            // the count is in the low order 7 bits
                     
            int regValue =
                Dpa.DPOT_W_CH0                      // CMND = 0 is left channel Dpot cmnd
                | direction                         // up is 1 in bit 8, down is 0
                | Dpa.DPOT_NOSAVE_POS               // do not save position to flash
                | (nStepsUp & Dpa.DPOT_COUNT_MASK); // the count is in the low order 7 bits

            writeReadSpi(regValue);
        }

    };

    public class DpaRightChannel extends DpaChannel {
        DpaRightChannel(DpaBoard parent) {
	    super(parent);
            _logger.debug("DpaRightChannel ctor invoked");
            _channelAdc = new RightChannelAdc();
            _relayReg = new RightRelayReg(_rightRelayReg); // but not relay reg
            _channelCtrlReg = new RightChannelCtrlReg(_rightChannelCtrlReg);
            _interruptCtrlReg = new RightInterruptReg(_interruptReg);
        }

        protected void movePot(int direction, int nStepsUp ) {
            _logger.debug("DpaRightChannel movePot(dir="
			  + (direction==Dpa.DPOT_DIR_UP?"UP":"DOWN")
			  + "; steps=" + nStepsUp );
            int regValue =
                Dpa.DPOT_W_CH1                      // CMND = 0 is left channel Dpot cmnd
                | direction                         // up is 1 in bit 8, down is 0
                | Dpa.DPOT_NOSAVE_POS               // do not save position to flash
                | (nStepsUp & Dpa.DPOT_COUNT_MASK); // the count is in the low order 7 bits

            writeReadSpi(regValue);
        }
    };
}

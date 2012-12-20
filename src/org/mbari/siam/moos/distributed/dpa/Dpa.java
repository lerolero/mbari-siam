/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.distributed.dpa;

/**
This utility class defines DPA-related constants and flags.
It has been created as a separate class so that shore-side 
code that uses DPA interface information  will not require 
all of the hardware-specific code/classes to be included. 
*/
public class Dpa {

    /** Default Current Limit */
    public static final int DEFAULT_CURRENT_LIMIT_MA=1000;

    /** DPA command: check if busy */
    public static final int DPA_COMMAND_BUSY=0xF000;

    /** DPA return value when busy */
    public static final int DPA_BUSY=0x7FFF;

    /** DPA Mask: mask of bits returned when busy */
    public static final int DPA_BUSY_MASK=0x7FFF;

    /** Relay Register */
    /** Mask of valid relay register bits*/
    public static final int RELAY_MASK = 0x7;
    /** Relay Register ch 0 write */
    public static final int RELAY_W_CH0=0x2000;
    /** Relay Register ch 0 read */
    public static final int RELAY_R_CH0=0x9000;
    /** Relay Register ch 1 write */
    public static final int RELAY_W_CH1=0x3000;
    /** Relay Register ch 1 read */
    public static final int RELAY_R_CH1=0xA000;
    /** Relay Register Mask: RS485 Terminations Connected */
    public static final int RELAY_485_CON=0x0004;
    /** Relay Register Mask: Communications Isolated */
    public static final int RELAY_COMM_ISO=0x0002;
    /**Relay Register Mask: Power Isolated */
    public static final int RELAY_IPOWER_ISO=0x0001;

    /** Channel Control Register */
    /** Mask of valid control register bits*/
    public static final int CONTROL_MASK = 0xFF;
    /** Control Register ch 0 write */
    public static final int CONTROL_W_CH0=0x6000;
    /** Control Register ch 0 read */
    public static final int CONTROL_R_CH0=0xD000;
    /** Control Register ch 1 write */
    public static final int CONTROL_W_CH1=0x7000;
    /** Control Register ch 1 read */
    public static final int CONTROL_R_CH1=0xE000;
    /** Control Register Mask: Status FAULT */
    public static final int CONTROL_STATUS_FAULT=0x0080;
    /** Control Register Mask: Serial Direction ON (actually Tx Power hi/lo) */
    public static final int CONTROL_TXPOWER_HI=0x0020;
    /** Control Register Mask: Duplex HALF */
    public static final int CONTROL_DUP_HALF=0x0010;
    /** Control Register Mask: Mode RS485*/
    public static final int CONTROL_MODE_485=0x0008;
    /** Control Register Mask: Slew Rate UNLIMITED */
    public static final int CONTROL_SLEW_UNLIMITED=0x0004;
    /** Control Register Mask: Communications Power ON*/
    public static final int CONTROL_CPOWER_ON=0x0002;
    /** Control Register Mask: Instrument Power ON */
    public static final int CONTROL_IPOWER_ON=0x0001;

    /** Interrupt Register */
    /** Mask of valid interrupt register bits */
    public static final int INTERRUPT_MASK = 0x3F;
    /** Interrupt Register Command: write */
    public static final int INTERRUPT_W=0x5000;
    /** Interrupt Register Command: read */
    public static final int INTERRUPT_R=0xC000;
    /** Interrupt Register Mask: overcurrent enable mask */
    public static final int INTERRUPT_OCE_MASK=0x0038;
    /** Interrupt Register Mask: ch 0 overcurrent enable */
    public static final int INTERRUPT_OCE_CH0=0x0020;
    /** Interrupt Register Mask: ch 1 overcurrent enable */
    public static final int INTERRUPT_OCE_CH1=0x0010;
    /** Interrupt Register Mask: global overcurrent enable */
    public static final int INTERRUPT_OCE_GLOBAL=0x0008;
    /** Interrupt Register Mask: overcurrent flag mask */
    public static final int INTERRUPT_OCF_MASK=0x0007;
    /** Interrupt Register Mask: ch 0 overcurrent interrupt flag */
    public static final int INTERRUPT_OCF_CH0=0x0002;
    /** Interrupt Register Mask: ch 1 overcurrent interrupt flag */
    public static final int INTERRUPT_OCF_CH1=0x0004;
    /** Interrupt Register Mask: global overcurrent interrupt flag */
    public static final int INTERRUPT_OCF_GLOBAL=0x0001;

    /** A/D Converter (ADC) */
    /** ADC Command: Write */
    public static final int ADC_START=0x4000;
    /** ADC Command: Read */
    public static final int ADC_R=0xB000;
    /** ADC Mask: power bits */
    public static final int ADC_POWER_MASK=0x00C0;
    /** ADC Mask: power off */
    public static final int ADC_POWER_OFF=0x0000;
    /** ADC Mask: power standby */
    public static final int ADC_POWER_STBY=0x0040;
    /** ADC Mask: power on, internal clock */
    public static final int ADC_POWER_ON_INT=0x0080;
    /** ADC Mask: aquisition mode external */
    public static final int ADC_ACQMOD_EXT=0x0020;
    /** ADC Mask: input mode single ended */
    public static final int ADC_INMOD_SGL=0x0010;
    /** ADC Mask:  unipolar reference*/
    public static final int ADC_REF_UNIPOLAR=0x0008;
    /** ADC Channel Address:  battery supply voltage */
    public static final int ADC_VBAT_ADDR=0x0007;
    /** ADC Channel Address:  ch 1  voltage sense */
    public static final int ADC_VSENSE_CH1_ADDR=0x0006;
    /** ADC Channel Address:  ch 1  trip level sense */
    public static final int ADC_VTRIP_CH1_ADDR=0x0005;
    /** ADC Channel Address:  ch 1  current sense */
    public static final int ADC_ISENSE_CH1_ADDR=0x0004;
    /** ADC Channel Address: heat sink temperature  */
    public static final int ADC_HEATSINK_TEMP_ADDR=0x0003;
    /** ADC Channel Address:  ch 0  voltage sense */
    public static final int ADC_VSENSE_CH0_ADDR=0x0002;
    /** ADC Channel Address:  ch 0  trip level sense */
    public static final int ADC_VTRIP_CH0_ADDR=0x0001;
    /** ADC Channel Address:  ch 0  current sense */
    public static final int ADC_ISENSE_CH0_ADDR=0x0000;

    /** Digital Potentiometer (DPOT, sets current limit) */
    /** DPOT Command : channel 0 write */
    public static final int DPOT_W_CH0 = 0x0000;
    /** DPOT Command : channel 1 write */
    public static final int DPOT_W_CH1 = 0x1000;
    /** DPOT Command Mask: Direction UP  */
    public static final int DPOT_DIR_UP = 0x0100;
    /** DPOT Command Mask: Direction DOWN  */
    public static final int DPOT_DIR_DOWN = 0x0000;
    /** DPOT Command Mask: save position  */
    public static final int DPOT_SAVE_POS = 0x0080;
    /** DPOT Command Mask: do not save position  */
    public static final int DPOT_NOSAVE_POS = 0x0000;
    /** DPOT Command Mask: count bits */
    public static final int DPOT_COUNT_MASK= 0x007F;
    /** DPOT Constant: milliamps per count */
    public static final int DPOT_MILLIAMPS_PER_COUNT=120;

}

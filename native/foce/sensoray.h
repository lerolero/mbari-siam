/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/********************************************************************************/
/* Summary  : Test Routine for Sensoray 518 environmental sensor board		*/	
/* Filename : testSensoray518.c								*/
/* Author   : Kent Headley (klh), shamelessly stolen from Diamond's test code	*/
/* Project  : FOCE								*/
/* Revision : 1.0								*/
/* Created  : 9 mar 2009         						*/
/*										*/
/* MBARI provides this documentation and code "as is", with no warranty,	*/
/* express or implied, of its quality or consistency. It is provided without	*/
/* support and without obligation on the part of the Monterey Bay Aquarium	*/
/* Research Institute to assist in its use, correction, modification, or	*/
/* enhancement. This information should not be published or distributed to	*/
/* third parties without specific written permission from MBARI.		*/
/*										*/
/********************************************************************************/
/* Modification History:							*/
/* $Name: HEAD $
   $Id: sensoray.h,v 1.11 2012/12/18 00:00:30 oreilly Exp $
   $Log: sensoray.h,v $
   Revision 1.11  2012/12/18 00:00:30  oreilly
   added copyright header

   Revision 1.10  2009/05/13 20:04:19  headley
   added offset in addition to scalar for Sensoray data channels. This enables a complete linear scaling for voltages (previously had scalar only)

   Revision 1.9  2009/03/30 04:00:33  headley
   - added interface for Sensoray 518 A/D board (environmental sensors)
   - Sensoray driver code defined in sensoray.h and sensoray.c
   - Sensoray test module in testSensoray.c

   Revision 1.8  2009/03/24 01:09:33  headley
   pulled out main to testSensoray.c; now just has implementation of sensoray functions.

   Revision 1.7  2009/03/21 01:56:45  headley
   fixed timing values (timing was being affected by debug statements...this should work w/ or w/o debug)

   Revision 1.6  2009/03/21 01:50:51  headley
   redefined default value for OPEN_SENSOR_VALUE_MASK (0x00 all full scale negative)

   Revision 1.5  2009/03/21 01:49:35  headley
   disable DEBUG_TESTSENSORAY by default
   renamed timing constants
   POSTW_USEC 70
    PRER_USEC 15
   STATUS_REG_CHANGE_DELAY 20

   added Bit mask (bit0:channel 0, etc.) defining data value to return when sensor is open OPEN_SENSOR_VALUE_MASK
   define  CMD_SET_OPEN_SENSOR_VALUES
   changed filterConstant data type
   defined new methods:
    int isSet(short unsigned statusMask);
    void waitDAV();
    void waitCRMT();
    void setOpenSensorValues(Byte mask);
    void showChannelConfig();
    const char* getSensorTypeName(int typeCode);

   Revision 1.4  2009/03/19 06:00:25  headley
   renamed several methods to avoid name collisions when used with focio

   Revision 1.3  2009/03/14 05:42:32  headley
   added timing to fix temp channel read cycle.
   added debug statements and formatting

   Revision 1.1  2009/03/10 20:20:08  headley
   C driver code for Sensoray 518 PC104 environmental sensor board

   
 */
/********************************************************************************/

#ifndef _SENSORAY_H_
#define _SENSORAY_H_
/*
The 518 may be mapped to any four-byte address block within the range 000 to 3FF
hex. Although the board occupies a four-byte block of I/O space, it uses only the
first two address locations in the block.
To avoid addressing conflicts, you must not map the 518 into any address range
occupied by other devices. Similar to many other I/O cards, the 518 does not
decode the full PC104 16-bit I/O address -- only the low ten address bits are
decoded. As a consequence, "images" of the 518 will appear throughout the 16-bit
address range at intervals of 400H bytes. You should ensure that these images do
not conflict with other devices.
Option shunts E1 through E8 are used to select the 518 base address. Shunts are
factory set to locate the board at base address 2B0H. This address should not
conflict with any standard I/O address assignments. If you require a different base
address, use the following table to determine the correct shunt programming for
your target base address.

*/

//#define DEBUG_TESTSENSORAY		/* Turn on to enable debug output messages */

/* Following constants to conform to jumpers on board		*/

#define ADDR_BASE	0x330 //default 0x2B0
#define PORT_BYTES      4
#define IOPERM_ALLOW    1

#define DATA_REG        (ADDR_BASE+0) //read only
#define COMMAND_REG     (ADDR_BASE+0) //write only
#define STATUS_REG      (ADDR_BASE+1) //read only
#define CONTROL_REG     (ADDR_BASE+1) //write only


#define NUMBER_OF_CHANNELS 8

#define POSTW_USEC 150
#define PRER_USEC 80
#define STATUS_REG_CHANGE_DELAY 20

#define CH1_SENSOR_TYPE SENSOR_TYPE_THERMISTOR
#define CH1_SENSOR_NAME "Therm_HE104"
#define CH2_SENSOR_TYPE SENSOR_TYPE_THERMISTOR
#define CH2_SENSOR_NAME "Therm_36V"
#define CH3_SENSOR_TYPE SENSOR_TYPE_THERMISTOR
#define CH3_SENSOR_NAME "Therm_24V"
#define CH4_SENSOR_TYPE SENSOR_TYPE_THERMISTOR
#define CH4_SENSOR_NAME "Therm_12V"
#define CH5_SENSOR_TYPE SENSOR_TYPE_THERMISTOR
#define CH5_SENSOR_NAME "Therm_12V"

/** Status Register 
Before writing a byte to the command register the host must test the CRMT bit. When CRMT
contains a logic 1, the command register is ready to accept a new command byte. The host
should write to the command register only when CRMT contains a logic 1. Similarly, the DAV
status bit must be tested before reading a byte from the data register.
When DAV contains a logic 1, a new byte is available in the data register for reading by the host.
Although these handshake rules are simple, failure to observe them will almost certainly result in
communication errors.
IMPORTANT NOTE: The CRMT, DAV, and ALARM status bits are not valid when the
FAULT bit is active. After resetting the 518 board (by means of either soft or hard reset), the
host processor should not attempt to handshake to or from the 518 until the FAULT bit changes
to the inactive (logic 0) state.
*/
/* Command Register eMpTy; 518 ready to accept command byte 
*/
#define STA_CRMT_MASK        0x80 

/* Data Available
*/
#define STA_DAV_MASK         0x40 

/* Alarm 
*/
#define STA_ALARM_MASK       0x20 

/* Board reset in progress or board fault detected 
*/
#define STA_FAULT_MASK       0x10

/** Specifies the control function to be performed. When set to logic zero, the
518 board is reset and all other control register bits are ignored. When set
to logic one, the other control register bits behave as described below.
*/
#define CTL_SET_CLR 0x80
/**  Specifies whether selected interrupts are to be enabled or disabled. When
set to logic one, all selected interrupts are enabled. When set to logic zero,
selected interrupts are disabled.
*/
#define CTL_INT_RST 0x10
/**  Selects CRMT interrupt. When set to logic one, the CRMT interrupt is
enabled or disabled as determined by the state of the SET/CLR bit. When
set to logic zero, the CRMT interrupt enable is unchanged. While
enabled, the host will be interrupted whenever the status register CRMT
bit is asserted.
*/
#define CTL_ICMD 0x40
/** Selects DAV interrupt. When set to logic one, the DAV interrupt is
enabled or disabled as determined by the state of the SET/CLR bit. When
set to logic zero, the DAV interrupt enable is unchanged. While enabled,
the host will be interrupted whenever the status register DAV bit is
asserted.
 */
#define CTL_IDAT 0x20
/** Selects ALARM interrupt. When set to logic one, the ALARM interrupt
is enabled or disabled as determined by the state of the SET/CLR bit.
When set to logic zero, the ALARM interrupt enable is unchanged. While
enabled, the host will be interrupted whenever the status register ALARM
bit is asserted.
 */
#define CTL_IALARM 0x01

/* Bit mask (bit0:channel 0, etc.) defining 
   data value to return when sensor is open.
   0: 32768 (full scale -)
   1: 32767 (full scale +)
*/
#define OPEN_SENSOR_VALUE_MASK 0xFF

/** Command opcodes, masks etc.*/
#define CMD_OPCODE_MASK             0xF0
#define CMD_CHANNEL_MASK            0x0F
#define CMD_SET_OPEN_SENSOR_VALUES  80
#define CMD_DEFINE_SENSOR_CHANNEL   16
#define CMD_SET_FILTER_TC           96
#define CMD_READ_BOARD_TEMP         64
#define CMD_READ_ALL_CHANNEL_DATA   88    
#define CMD_HIGH_SPEED_MODE_0       240
#define CMD_HIGH_SPEED_MODE_1       8
#define CMD_HIGH_SPEED_MODE_2       2
#define CMD_READ_FIRMWARE_VERSION_0 240
#define CMD_READ_FIRMWARE_VERSION_1 5
#define CMD_READ_FIRMWARE_VERSION_2 0
#define CMD_READ_PRODUCT_ID_0       240
#define CMD_READ_PRODUCT_ID_1       4
#define CMD_READ_PRODUCT_ID_2       0

/** Sensor Type Definitions*/
#define SENSOR_TYPE_THERMOCOUPLE_K     0x1C
#define SENSOR_TYPE_THERMOCOUPLE_B     0x24
#define SENSOR_TYPE_THERMOCOUPLE_C     0x23
#define SENSOR_TYPE_THERMOCOUPLE_E     0x01
#define SENSOR_TYPE_THERMOCOUPLE_J     0x1B
#define SENSOR_TYPE_THERMOCOUPLE_K     0x1C
#define SENSOR_TYPE_THERMOCOUPLE_N     0x22
#define SENSOR_TYPE_THERMOCOUPLE_T     0x1D
#define SENSOR_TYPE_THERMOCOUPLE_S     0x1E
#define SENSOR_TYPE_THERMOCOUPLE_R     0x1F
#define SENSOR_TYPE_RTD_10C_1C         0x2C
#define SENSOR_TYPE_RTD_100P385_05C    0x18
#define SENSOR_TYPE_RTD_100P392_05C    0x19
#define SENSOR_TYPE_RTD_100P385_0125C  0x2A
#define SENSOR_TYPE_RTD_100P392_0125C  0x2B
#define SENSOR_TYPE_RTD_NTR2_1C        0x21
#define SENSOR_TYPE_THERMISTOR         0x1A
#define SENSOR_TYPE_CURRENTLOOP        0x11
#define SENSOR_TYPE_PRESSURE           0x0F
#define SENSOR_TYPE_DISABLED           0x13
#define SENSOR_TYPE_RESISTANCE_400     0x0A
#define SENSOR_TYPE_RESISTANCE_3K      0x14
#define SENSOR_TYPE_RESISTANCE_600K    0x20
#define SENSOR_TYPE_RESISTANCE_CUSTOM  0x0C
#define SENSOR_TYPE_VOLTAGE_100MV      0x17
#define SENSOR_TYPE_VOLTAGE_500MV      0x16
#define SENSOR_TYPE_VOLTAGE_5V         0x15

typedef enum UNITS {DEGREES_F=0,DEGREES_C,DEGREES_K,RAW=0,SCALED}Units;

/** Global Variables */
/** FOCE Default Channel Definitions */
typedef struct {
  Byte channel;
  Byte sensorType;
  Flt64 scalar;
  Flt64 offset; // added for +/- 5V scale, e.g.: volts=scalar*counts+offset
  Int16 filterPercent;
  Byte filterConstant;
  char name[32];
  char units[32];
}ChannelInfo;

void resetSensoray();
void sensoraySendByte (unsigned int addr, Byte cmd_byte );
Byte sensorayReadByte (unsigned int addr);
void sensoraySendWord (unsigned int addr, Word cmd_word );
Word sensorayReadWord (unsigned int addr);
void defineChannelSensor(Byte channel, Byte type);
void initializeSensoray( ChannelInfo channelInfo[],int channels);
void readAllChannelData( double dest[], ChannelInfo channelInfo[],Units scaling);
double readChannelData( ChannelInfo *channelInfo,Units scaling);
double readBoardTemperature(Units units);
void setFilterTimeConstant(Byte channel, Byte filterConstant);
Word readProductId();
double readFirmwareVersion();
void highSpeedMode();
int isSet(short unsigned statusMask);
void waitDAV();
void waitCRMT();
void setOpenSensorValues(Byte mask);
void showChannelConfig(ChannelInfo channelInfo[]);
char *getChannelConfig(ChannelInfo channelInfo[], char buf[],int len,char *delimiter );
const char* getSensorTypeName(int typeCode);

#endif /* _SENSORAY_H_*/


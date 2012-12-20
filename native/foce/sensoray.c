/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/********************************************************************************/
/* Summary  : Test Routine for Sensoray 518 environmental sensor board		*/	
/* Filename : sensoray.c							*/
/* Author   : Kent Headley (klh)                                            	*/
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
   $Id: sensoray.c,v 1.8 2012/12/17 21:43:34 oreilly Exp $
   $Log: sensoray.c,v $
   Revision 1.8  2012/12/17 21:43:34  oreilly
   added copyright header

   Revision 1.7  2009/05/22 22:26:14  headley
   fixed bug in scaled output calculation for readAllDataChannels and readDataChannel

   Revision 1.6  2009/05/13 20:04:19  headley
   added offset in addition to scalar for Sensoray data channels. This enables a complete linear scaling for voltages (previously had scalar only)

   Revision 1.5  2009/03/30 04:00:33  headley
   - added interface for Sensoray 518 A/D board (environmental sensors)
   - Sensoray driver code defined in sensoray.h and sensoray.c
   - Sensoray test module in testSensoray.c

   Revision 1.4  2009/03/24 01:09:33  headley
   pulled out main to testSensoray.c; now just has implementation of sensoray functions.

   Revision 1.2  2009/03/21 01:51:57  headley
   made many changes to timing, output, etc.
   added new methods
   This version is appears to be working correctly

   Revision 1.1  2009/03/19 05:56:06  headley
   renamed testSensoray.c to sensoray.c
   renamed several methods to avoid name collisions when used with focio

 */
/********************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <asm/io.h> // for ioperm

#include "mbariTypes.h"
#include "mbariConst.h"
#include "sensoray.h"
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

// defined in sensoray.h
#ifdef DEBUG_TESTSENSORAY
#define dprintf printf
#else
#define dprintf
#endif

int getIOPerm(int addr, int bytes){
  int i=0;
  int err=0;
  /* get permission for port IO */
  dprintf("getting IO perms at 0x%03X...\n",addr);
  i=ioperm(addr,bytes,IOPERM_ALLOW);
  if( i!=0){
    err=errno;
    printf("ioperm failed; errno=%d ",err);
    switch(err){
    case EINVAL:
      printf("EINVAL\n");
      break;
    case EIO:
      printf("EIO\n");
      break;
    case EPERM:
      printf("EPERM\n");
      break;
    default:
      printf("unknown error");
      break;
    }
  }else{
      err=i;
  }

  dprintf("getIOPerm returned %d\n",i);

  return err;
}

void resetSensoray()
{
  Byte status,reset;
  int i=-1;
  
  /* get permission to use port */
  getIOPerm(ADDR_BASE,PORT_BYTES);

  /* Assert reset bit of control reg and clear interrupt enable bits */
  dprintf("asserting reset...\n");
  sensoraySendByte(CONTROL_REG,0x00);

  /* wait for fault bit to clear */
  dprintf("waiting for fault bit to clear...\n");
  while( isSet(STA_FAULT_MASK) > 0 ){
    usleep(STATUS_REG_CHANGE_DELAY);
    if(i++ > 10){
      printf("WARNING: Fault bit still set\n");
      break;
    }
  }

  /* no need to deassert reset, the fault bit clears
     when the reset is complete
  */
}

/** Initialize the Sensoray Board.
    Define channel sensor types, set filter constants.

    @param channelInfo struct ChannelInfo array of channel definition structures
    @param channels int number of channels
    @return none
 */

void initializeSensoray( ChannelInfo channelInfo[],int channels)
{
  int i=0;
  ChannelInfo *ci;

  dprintf("setting high speed mode...\n");
  highSpeedMode();

  dprintf("defining sensor channels...\n");

  /* Initialize sensor channel definitions */
  for(i=0;i<channels;i++){
    
    ci=&channelInfo[i];
    /* define channel sensor types */
    dprintf("channel %d %s 0x%02X\n",ci->channel,ci->name,ci->sensorType);
    defineChannelSensor(ci->channel,ci->sensorType);
    /* Compute and set filter time constant...*/
    printf("channel %d filter const=%d\n",ci->channel,ci->filterConstant);
    setFilterTimeConstant(ci->channel,ci->filterConstant);
  }
  
  dprintf("setting open sensor values using mask 0x%02X\n",OPEN_SENSOR_VALUE_MASK);
  setOpenSensorValues(OPEN_SENSOR_VALUE_MASK);
  dprintf("initialization complete\n");
}

int isSet(short unsigned statusMask)
{
  Byte status;
  status=inb( STATUS_REG );
  dprintf("isSet 0x%02X 0x%02X 0x%02X \n",status, statusMask,(status&statusMask));
  if( (status & statusMask) > 0){
    return 1;
  }
  return 0;
}


void waitDAV()
{
  int i=0;
  dprintf("waiting for DAV...%d\n",i++); /* wait for DAV */
  while ( isSet(STA_DAV_MASK)==0 ) {
    dprintf("waiting for DAV...%d\n",i++); /* wait for DAV */
    usleep(STATUS_REG_CHANGE_DELAY);
  }
}
void waitCRMT()
{
  int i=0;
  dprintf("waiting for CRMT...%d\n",i++); /* wait for CRMT */
  while ( isSet(STA_CRMT_MASK)==0 ) {
    dprintf("waiting for CRMT...%d\n",i++); /* wait for CRMT */
    usleep(STATUS_REG_CHANGE_DELAY);
  }
}


/* sensoraySendByte handshakes a command byte to the 518 command register */
void sensoraySendByte (unsigned int addr,Byte cmd_byte)
{
  int i=0;
  int fault=0;

  /* wait for CRMT (command reg empty bit) to set */
  waitCRMT();

  dprintf("sending byte 0x%02X:0x%02X (%d)\n",addr,cmd_byte,cmd_byte);
  outb ( cmd_byte,addr) ;/* send the byte */
  usleep(POSTW_USEC);

}

/* sensorayReadByte handshakes a data byte from the 518 data register */
Byte sensorayReadByte (unsigned int addr)
{
  Byte data=0;

  dprintf("readByte waiting\n");

  usleep(PRER_USEC);
  
  /* wait for DAV (data available bit) to set */
  waitDAV();

  data=inb ( addr );
  dprintf("readByte data: 0x%02X\n",data);

  return (data ) ; /* read the byte */
}

/* sensoraySendWord handshakes a 16-bit value to the 518 */
void sensoraySendWord (unsigned int addr, Word cmd_word )
{
  sensoraySendByte (addr,  cmd_word >> 8 ) ; /* send high byte */
  sensoraySendByte (addr,  cmd_word ) ; /* send low byte */
}

/* ADREADWORD handshakes a data byte from the 518 */
Word sensorayReadWord (unsigned int addr)
{
  Byte hiByte;
  Byte loByte;
  Word result;

  hiByte = (sensorayReadByte(addr) ); /* read high byte */
  dprintf("readWord hi byte: 0x%02X\n",hiByte);

  loByte=sensorayReadByte(addr);
  dprintf("readWord lo byte: 0x%02X\n",loByte);

  result=(hiByte<<8);
  result|=loByte;
  dprintf("readWord returning: 0x%02X\n",result);
  return result ; /* read & concatenate low byte */
}

/** Define channel sensor type. 
    @param channel Byte sensor channel
    @param type Byte sensor type code
    @return none
 */
void defineChannelSensor(Byte channel, Byte type)
{
  dprintf("defineSensorChannel: sending command\n");
  sensoraySendByte(COMMAND_REG,(CMD_DEFINE_SENSOR_CHANNEL+channel));
  dprintf("defineSensorChannel: sending type def\n");
  sensoraySendByte(COMMAND_REG,type);
}


/** Read data from all channels, returned as array of double values.

    @param dest pointer to array of double values (must hold NUMBER_OF_CHANNELS values) 
    @param channelInfo struct ChannelInfo array of channel definition structures
    @param scaling Units apply units scaling if scaling=SCALED
    @return void
*/
void readAllChannelData( double dest[],ChannelInfo channelInfo[],Units scaling)
{
  int i=0;
  ChannelInfo *ci;
  Int16 value;

  sensoraySendByte(COMMAND_REG,CMD_READ_ALL_CHANNEL_DATA);

  for(i=0;i<NUMBER_OF_CHANNELS;i++){
    ci=&channelInfo[i];
    value=sensorayReadWord(DATA_REG);
    dprintf("readAllChannelData: chan[%d] counts=%d (0x%04X)\n",i,value,value);
    dest[i]=value;

    if(scaling==SCALED){
      dest[i]*=ci->scalar;
      dest[i]+=ci->offset;
    }
  }
  return;
}

/** Read data from all channels, returned as array of double values.

    @param dest pointer to array of double values (must hold NUMBER_OF_CHANNELS values) 
    @param channelInfo struct ChannelInfo array of channel definition structures
    @param scaling Units apply units scaling if scaling=SCALED
    @return double channel data
*/
double readChannelData(ChannelInfo *channelInfo,Units scaling)
{
  int i=0;
  Int16 wvalue;
  double dvalue;
  /* the opcode to read a data channel is just the data channel number */
  sensoraySendByte(COMMAND_REG,channelInfo->channel);

  wvalue=sensorayReadWord(DATA_REG);
  dvalue=(double)wvalue;
  if(scaling==SCALED){
    dvalue*=channelInfo->scalar;
    dvalue+=channelInfo->offset;
  }

  return dvalue;
}

/** Return board temperature.

    @param units Units indicate temperature units (DEGREES_C, DEGREES_F, DEGREES_K)
    @return double board temperature
 */
double readBoardTemperature(Units units)
{
  Word cTemp;
  double value;
  sensoraySendByte(COMMAND_REG,CMD_READ_BOARD_TEMP);

  // read temp (0.1 deg C/bit)
  cTemp=(sensorayReadWord(DATA_REG));
  dprintf("read temp word 0x%04X\n",cTemp);
  value=cTemp/10.0;
  // do requested conversion
  switch(units){
  case DEGREES_F:
    return ((1.8*(double)value)+32.0);
  case DEGREES_K:
    return (double)value+273.15;
  }
  return (double)value;
}

/** Set filter time constant.

    @param Byte channel time constant to set
    @param Byte filter constant value
    @return none
 */
void setFilterTimeConstant(Byte channel, Byte filterConstant)
{
  sensoraySendByte(COMMAND_REG,CMD_SET_FILTER_TC+channel);
  sensoraySendByte(COMMAND_REG,filterConstant);
  return;
}

/** Set open sensor value.

    @param Byte channel time constant to set
    @param Byte open sensor flag mask (0:-32768 1:+32767)
    @return none
 */
void setOpenSensorValues(Byte mask)
{
  sensoraySendByte(COMMAND_REG,CMD_SET_OPEN_SENSOR_VALUES);
  sensoraySendByte(COMMAND_REG,mask);
  return;
}

/** Read sensoray product ID 

    @return Word product ID code
 */
Word readProductId()
{
  sensoraySendByte(COMMAND_REG,CMD_READ_PRODUCT_ID_0);
  sensoraySendByte(COMMAND_REG,CMD_READ_PRODUCT_ID_1);
  sensoraySendByte(COMMAND_REG,CMD_READ_PRODUCT_ID_2);
  return sensorayReadWord(DATA_REG);
}

/** Read sensoray firmware version

    @return double firmware version
 */
double readFirmwareVersion()
{
  sensoraySendByte(COMMAND_REG,CMD_READ_FIRMWARE_VERSION_0);
  sensoraySendByte(COMMAND_REG,CMD_READ_FIRMWARE_VERSION_1);
  sensoraySendByte(COMMAND_REG,CMD_READ_FIRMWARE_VERSION_2);

  return (sensorayReadWord(DATA_REG)/100.0);
}

/** Enable high speed mode.

    @return none
 */
void highSpeedMode()
{
  sensoraySendByte(COMMAND_REG,CMD_HIGH_SPEED_MODE_0);
  sensoraySendByte(COMMAND_REG,CMD_HIGH_SPEED_MODE_1);
  sensoraySendByte(COMMAND_REG,CMD_HIGH_SPEED_MODE_2);
  return;
}

const char* getSensorTypeName(int typeCode){
  switch(typeCode){
  case SENSOR_TYPE_THERMOCOUPLE_K:
    return "SENSOR_TYPE_THERMOCOUPLE_K";
    break;
  case SENSOR_TYPE_THERMISTOR:
    return "SENSOR_TYPE_THERMISTOR";
    break;
  case SENSOR_TYPE_VOLTAGE_5V:
    return "SENSOR_TYPE_VOLTAGE_5V";
    break;
  default :
    break;
  }
  return "UNKNOWN";
}

void showChannelConfig(ChannelInfo channelInfo[]){
  char buf[512];
  printf("%s",getChannelConfig(channelInfo,buf,512,"\n"));
}

char *getChannelConfig(ChannelInfo channelInfo[], char buf[],int len,char *delimiter ){
  int i;
  ChannelInfo *ci;
  char *cp;
  char tmpBuf[512];

  for(i=0;i<(len<512?len:512);i++){
    tmpBuf[i]='\0';
    buf[i]='\0';
  }

  cp=tmpBuf;
  sprintf(cp,"[channel,sensorName,typeCode,scalar,offset,filterPercent,filterConstant]%s",delimiter);
  cp=tmpBuf+strlen(tmpBuf);

  for(i=0;i<NUMBER_OF_CHANNELS;i++){
    ci=&channelInfo[i];
    sprintf(cp,"%1d %13s 0x%02X %6.4f %6.4f %2d %2d%s",ci->channel,
	    getSensorTypeName(ci->sensorType),
	    ci->sensorType,
	    ci->scalar,
	    ci->offset,
	    ci->filterPercent,
	    ci->filterConstant,
	    delimiter);
    cp=tmpBuf+strlen(tmpBuf);
  }
  if(strlen(tmpBuf)<(len-1)){
    memcpy(buf,tmpBuf,strlen(tmpBuf));
  }else{
    memcpy(buf,tmpBuf,len-1);
  }
  return buf;
}


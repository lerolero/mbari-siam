/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/********************************************************************************/
/* Summary  : Test application for Sensoray 518 environmental sensor board	*/	
/* Filename : testSensoray.c							*/
/* Author   : Kent Headley (klh)                                         	*/
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
   $Id: testSensoray.c,v 1.10 2012/12/17 21:43:36 oreilly Exp $
   $Log: testSensoray.c,v $
   Revision 1.10  2012/12/17 21:43:36  oreilly
   added copyright header

   Revision 1.9  2010/04/05 17:30:49  headley
   Added test examples

   Revision 1.8  2009/05/22 22:29:32  headley
   set channel 4 (humidity)  ch6 (heatsink therm) and ch 7 (spare) scale and offset to orignal value from sensoray data sheet table

   changed ch6 and 7 from +/- 5V to THERMISTOR

   Revision 1.7  2009/05/13 20:04:19  headley
   added offset in addition to scalar for Sensoray data channels. This enables a complete linear scaling for voltages (previously had scalar only)

   Revision 1.6  2009/03/30 04:00:33  headley
   - added interface for Sensoray 518 A/D board (environmental sensors)
   - Sensoray driver code defined in sensoray.h and sensoray.c
   - Sensoray test module in testSensoray.c

   Revision 1.5  2009/03/24 01:08:47  headley
   test program for Sensoray 581 environmental sensor board (implemented in sensoray.c,h)

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
   Test Application for Sensoray 518 Environmental Sensor Board
   (built on sensoray.c)
*/
/*
  {0,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-HE104","deg C"},
  {1,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-36V"  ,"deg C"},
  {2,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-24V"  ,"deg C"},
  {3,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-12V"  ,"deg C"},
  {4,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"Humidity"   ,"Volts"},
  {5,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Temp"       ,"deg C"},
  {6,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm","deg C"},
  {7,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm","deg C"},

  {0,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-0"   ,"Volts"},
  {1,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-1"   ,"Volts"},
  {2,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-2"   ,"Volts"},
  {3,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-3"   ,"Volts"},
  {4,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-4"   ,"Volts"},
  {5,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-5"   ,"Volts"},
  {6,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-6"   ,"Volts"},
  {7,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-7"   ,"Volts"},
*/
ChannelInfo channelData[8] = {
  {0,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-HE104","deg C"},
  {1,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-36V"  ,"deg C"},
  {2,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-24V"  ,"deg C"},
  {3,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-12V"  ,"deg C"},
  {4,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"Humidity"   ,"Volts"},
  {5,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Temp"       ,"deg C"},
  {6,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm","deg C"},
  {7,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"test-7"   ,"Volts"}
};

/************************************************************************/
/* Function    : main							*/
/* Purpose     : Main Routine for this test program			*/
/* Inputs      : None 							*/
/* Outputs     : Exit code						*/
/************************************************************************/
int main( int argc, char *argv[])
{
  Word productId=0;
  double firmwareVersion=-1,boardTemp=-1;
  ChannelInfo *ci=&channelData[0];
  double data[NUMBER_OF_CHANNELS];
  double idata[NUMBER_OF_CHANNELS];
  double test;
  int i=0;
  int doReset=1;
  int doInit=1;
  int doProductId=1;
  int doBoardTemp=1;
  int doFirmwareVersion=1;

  printf("\n");
  ioperm(ADDR_BASE,4,1);
  for(i=1;i<argc;i++){
    if(strstr(argv[i],"r")!=NULL)
       doReset=0;
    if(strstr(argv[i],"i")!=NULL)
       doInit=0;
    if(strstr(argv[i],"p")!=NULL)
       doProductId=0;
    if(strstr(argv[i],"b")!=NULL)
       doBoardTemp=0;
    if(strstr(argv[i],"f")!=NULL)
       doFirmwareVersion=0;
  }

  printf("Reset           : %s\n",doReset==1?"YES":"NO");
  printf("Init            : %s\n",doInit==1?"YES":"NO"); 
  printf("ProductId       : %s\n",doProductId==1?"YES":"NO");
  printf("BoardTemp       : %s\n",doBoardTemp==1?"YES":"NO");
  printf("FirmwareVersion : %s\n",doFirmwareVersion==1?"YES":"NO");

  printf("\n");

  if(doReset){
    printf("resetting...\n");
    resetSensoray();
  }
  if(doInit){
    printf("initializing...\n");
    initializeSensoray(ci,NUMBER_OF_CHANNELS);
  }

  if(doProductId){
    printf("reading product ID...\n");
    productId=readProductId();
  }
  if(doFirmwareVersion){
    printf("reading firmware version...\n");
    firmwareVersion=readFirmwareVersion();
  }
  if(doBoardTemp){
    printf("reading board temperature...\n");
    boardTemp=readBoardTemperature(DEGREES_C);
  }

  printf("reading all data channels...\n");
  readAllChannelData(&data[0],ci,RAW);

  printf("reading individual data channels ...\n");
  for(i=0;i<NUMBER_OF_CHANNELS;i++){
    ci=&channelData[i];
    idata[i]=readChannelData(ci,RAW);
  }

  printf("\n");
  showChannelConfig(&channelData[0]);
  printf("\n");
  printf("status register : 0x%02X\n",inb(STATUS_REG));
  printf("product ID      : %d\n",productId);
  printf("firmwareVersion : %4.2f\n",firmwareVersion);
  printf("board temp      : %4.2f deg C\n",boardTemp);
  printf("\n");  

  printf("Channel Data (readAllChannels)\n");
  printf("[channel,name,channel data,(scaled),(raw hex)]\n");
  for(i=0;i<NUMBER_OF_CHANNELS;i++){
    ci=&channelData[i];
    printf(" %d %13s 0x%02X : %6.0f counts (%7.2f) (%04X)\n",ci->channel,ci->name,ci->sensorType,data[i],data[i]*ci->scalar,(Word)data[i]);
  }
  printf("\n");  

  printf("Channel Data (readDataChannel)\n"); 
  printf("[channel,name,channel data,(scaled),(raw hex)]\n");
  for(i=0;i<NUMBER_OF_CHANNELS;i++){
    ci=&channelData[i];
    printf(" %d %13s 0x%02X : %6.0f counts (%7.2f) (%04X)\n",ci->channel,ci->name,ci->sensorType,idata[i],idata[i]*ci->scalar,(Word)idata[i]);
  }

  printf("\n");
}

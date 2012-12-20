/************************************************************************/
/* Copyright 2008 MBARI							*/
/************************************************************************/
/* Summary  : foceIO - Server to do I/O for FOCE at root privs		*/
/* Filename : foceIO.c							*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : FOCE							*/
/* Version  : 1.0							*/
/* Created  : 15 April 2008						*/
/************************************************************************/
/* Modification History:						*/
/* 15apr2008, rah - created						*/
/* 16may2008, rah, changed to do explicit relay and analog I/O		*/
/* $Log: foceio.c,v $
/* Revision 1.10  2010/03/27 22:25:01  bobh
/* Added CPUtemp
/*
/* Revision 1.8.2.3  2010/02/25 18:16:13  bobh
/* Added relayAddr to get address for relay board N, ERROR if not there
/*
/* Revision 1.8.2.2  2009/09/01 21:18:10  salamy
/* Added more debugging
/*
/* Revision 1.8.2.1  2009/08/19 22:59:03  bobh
/* Support digital inputs
/*
/* Revision 1.8  2009/06/05 01:23:29  headley
/* added on/off function to switchElmoBit
/*
 */
/************************************************************************/

#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/io.h>
#include <ctype.h>
#include <string.h>
#include <signal.h>
#include <pthread.h>
#include <errno.h>
#include "mbariTypes.h"
#include "mbariConst.h"
#include "dscud.h"
#include "sensoray.h"

//#define DEBUG		/* Turn on to (1) not make it a daemon, and	*/
			/*  (2) cause debug output messages		*/
#ifdef DEBUG
#define dprintf printf
#else
#define dprintf
#endif
	
#define IOSRV_PORT	7933		/* Arbitrary, hopefully unused port*/

#define MAX_RELAY_BOARDS	8
#define MAX_ANALOG_BOARDS	2
#define AD_INT_LEVEL		7
#define AD_CHANS_PER_BOARD	32
#define DIO_PORT_A              0x0
#define DIO_PORT_B              0x1
#define DIO_PORT_C              0x2
#define FOCE_DIO_PORT_CONFIG    0x8B /* DIO Port A:output B:input C:input */
#define ELMO_A0_ON              0x80
#define ELMO_A1_ON              0x40
#define ELMO_ALL_ON             0xC0
#define ELMO_ALL_OFF            0x00
#define ELMO_ADDR               0x300
#define ELMO_BOARD              0
#define ELMO_PORT               DIO_PORT_A

#define LPC_INDEX 0x295		/* For CPU temperature		*/
#define LPC_DATA  0x296


// Set LISTEN_IP to INADDR_ANY to listen to anyone on network
// What do we need to listen to local node???  INADDR_LOOPBACK doesn't work
#define LISTEN_IP	INADDR_ANY

#define deblank(p)	while (isspace(*p) && *p) p++;


/********************************/
/*	External Data		*/
/********************************/

extern char	*optarg;		/* From getopt() lib	    */
extern int	errno;


/********************************/
/*	Module Local Data	*/
/********************************/

MLocal int	relayBoards[MAX_RELAY_BOARDS];
MLocal int	relayStates[MAX_RELAY_BOARDS];
MLocal int	adBoards[MAX_ANALOG_BOARDS];
MLocal DSCB	adHandles[MAX_ANALOG_BOARDS];
MLocal DSCADSETTINGS adSettings[MAX_ANALOG_BOARDS][AD_CHANS_PER_BOARD];
MLocal int	lastBoard = -1, lastChan = -1;
MLocal MBool	adInited = FALSE;
pthread_mutex_t iomutex = PTHREAD_MUTEX_INITIALIZER;

ChannelInfo channelData[8] = {
  {0,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-HE104","deg C"},
  {1,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-36V"  ,"deg C"},
  {2,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-24V"  ,"deg C"},
  {3,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-12V"  ,"deg C"},
  {4,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"Humidity"   ,"Volts"},
  {5,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Temp"       ,"deg C"},
  {6,SENSOR_TYPE_THERMISTOR,0.01  ,0.0,10,25 ,"Therm-Heatsink","deg C"},
  {7,SENSOR_TYPE_VOLTAGE_5V,0.0002,0.0,15,38 ,"reserved"   ,"Volts"},
};

/************************************************************************/
/* Function    : usage							*/
/* Purpose     : Print usage string					*/
/* Inputs      : None							*/
/* Outputs     : None							*/
/************************************************************************/
static void usage(char *cmdName)
{
    printf ("usage: %s [-p port]\n\[-p port] TCP port number to use\n", 
	    cmdName);
}       


/************************************************************************/
/* Function    : writeIntReturn						*/
/* Purpose     : Send integer return value on the socket.  Preceed with */
/*		 "OK" if >= 0, else "ERROR"				*/
/* Inputs      : Socket, integer					*/
/* Outputs     : rtn							*/
/************************************************************************/
int writeIntReturn(FILE *fsock, int rtn)
{
    if (rtn < 0)
    {
	fprintf(fsock, "ERROR %d\n", rtn);
	dprintf("ERROR %d\n", rtn);
    }
    else
    {
	fprintf(fsock, "OK %d\n", rtn);
	dprintf("OK %d\n", rtn);
    }

    return(rtn);
}


/************************************************************************/
/* Function    : writeOKReturn						*/
/* Purpose     : Send "OK" on the socket				*/
/* Inputs      : Socket							*/
/* Outputs     : OK							*/
/************************************************************************/
int writeOKReturn(FILE *fsock)
{
    fprintf(fsock, "OK\n");
    dprintf("OK\n");
    return(OK);
}


/************************************************************************/
/* Function    : writeErrorReturn					*/
/* Purpose     : Send "ERROR" on the socket				*/
/* Inputs      : Socket							*/
/* Outputs     : ERROR							*/
/************************************************************************/
int writeErrorReturn(FILE *fsock)
{
    fprintf(fsock, "ERROR\n");
    dprintf("ERROR\n");
    return(ERROR);
}


/************************************************************************/
/* Function    : getRelayBoard						*/
/* Purpose     : Get or allocate a Relay board matching address addr	*/
/* Inputs      : Address						*/
/* Outputs     : Index of board, -1 for error				*/
/************************************************************************/
int getRelayBoard(int addr)
{
    int i;

    for (i = 0; i < MAX_RELAY_BOARDS; i++)
	if (relayBoards[i] == addr)
	    return(i);

    for (i = 0; i < MAX_RELAY_BOARDS; i++)
	if (relayBoards[i] == -1)
	{
	    relayBoards[i] = addr;
	    return(i);
	}

    return(-1);

} /* getRelayBoard() */


/************************************************************************/
/* Functiion   : initADBoard						*/
/* Purpose     : Get or allocate an A/D board matching address addr	*/
/* Inputs      : Socket to write return msg to, address			*/
/* Outputs     : Index of board, -1 for error				*/
/************************************************************************/
int initADBoard(FILE *fsock, int addr, int intLevel)
{
    int		i;
    DSCCB	dsccb;		/* structure containing board settings	*/
    ERRPARAMS	errorParams;
    DSCB	dscb;
    Byte	rtn;

    for (i = 0; i < MAX_ANALOG_BOARDS; i++)
	if ((adBoards[i] == addr) || (adBoards[i] == -1))
	{
	    adBoards[i] = addr;
	    if (!adInited)
	    {
		if( dscInit( DSC_VERSION ) != DE_NONE )
		{
		  
		    dscGetLastError(&errorParams);
		    if(fsock!=NULL)
		      {
			fprintf( fsock, "ERROR in dscInit: %s %s\n",
				 dscGetErrorString(errorParams.ErrCode),
				 errorParams.errstring );
		      }else{
			fprintf( stderr, "ERROR in dscInit: %s %s\n",
				 dscGetErrorString(errorParams.ErrCode),
				 errorParams.errstring );
		      }
		    return(-1);
		}
	    }

	    memset(&dsccb, 0, sizeof(DSCCB));
	    dsccb.base_address = addr;
	    dsccb.int_level = intLevel;

	    if((rtn = dscInitBoard(DSC_DMM32X, &dsccb, &dscb)) !=  DE_NONE)
	    {
		dscGetLastError(&errorParams);
		    if(fsock!=NULL)
		      {
			fprintf( fsock, "ERROR in dscInitBoard: %s %s\n",
				 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
		      }else{
			fprintf( stderr, "ERROR in dscInitBoard: %s %s\n",
				 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
		      }
		    return(-1);
	    }

	    lastBoard = -1;			/* Force reload of DSCADSETTINGS	*/
	    adHandles[i] = dscb;
	    if(fsock!=NULL)
	      {
		writeIntReturn(fsock, i);
	      }
	    return(i);
	}

    if(fsock!=NULL)
    {
      return(writeErrorReturn(fsock));
    }
      return(ERROR);
      
} /* initADBoard() */


/************************************************************************/
/* Function    : verifyADChan						*/
/* Purpose     : Set up one A/D port					*/
/* Inputs      : Socket, board, channel, setup parameters		*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int verifyADChan(int board, int chan, int nchans, int checkSetup)
{
    if (board >= MAX_ANALOG_BOARDS)
	return(ERROR);
    if (adBoards[board] < 0)
	return(ERROR);
    if (chan + nchans > AD_CHANS_PER_BOARD)
	return(ERROR);

    /* Note that current_channel is unsigned BYTE, so -1 comes out as 255 */
    if (checkSetup && (adSettings[board][chan].current_channel > AD_CHANS_PER_BOARD))
	return(ERROR);

    return(OK);

} /* verifyADChan() */

/************************************************************************/
/* Function    : adDIOConfig						*/
/* Purpose     : Set up digital (DIO) IO port				*/
/* Inputs      : board address, config byte  		                */
/* Configuration Byte
   Hex Decimal Port A Port B Port C (both halves)
   9B 155 Input Input Input
   92 146 Input Input Output
   99 153 Input Output Input
   90 144 Input Output Output
   8B 139 Output Input Input
   82 130 Output Input Output
   89 137 Output Output Input
   80 128 Output Output Output
*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int adDIOConfig(int addr, Byte config_byte) 
{

  ERRPARAMS errorParams; // structure for returning error code and error string
  int board;
  Byte result;
  board=initADBoard(NULL,addr,AD_INT_LEVEL);
  dprintf("DIO init initADBoard returned %d\n",board);
  if( (result = dscDIOSetConfig(adHandles[board], &config_byte)) != DE_NONE)
    {
      dscGetLastError(&errorParams);
      fprintf( stderr, "dscDIOSetConfig error: %s %s\n", dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
      return ERROR;
    }
  dprintf("DIO init OK; returned %x\n",result);
  return OK;
}

/************************************************************************/
/* Function    : adDIOOutputByte					*/
/* Purpose     : Output a byte to a digital (DIO) port   		*/
/* Inputs      : board, port, byte                      		*/
/* Outputs     : 0 or Error code					*/
/************************************************************************/
Byte adDIOOutputByte(int board, Byte port, Byte ioByte){
  return dscDIOOutputByte(adHandles[board],port,ioByte);
}

/************************************************************************/
/* Function    : adDIOInputByte			        		*/
/* Purpose     : Input a byte to a digital (DIO) port   		*/
/* Inputs      : board, port                               		*/
/* Outputs     : 0 or Error code					*/
/************************************************************************/
Byte adDIOInputByte(int board, Byte port, Byte *value){
  return dscDIOInputByte(adHandles[board],port,value);
}

/************************************************************************/
/* Function    : configElmo      					*/
/* Purpose     : configure digital IO (DIO) port as Elmo power enable	*/
/* Inputs      : none                                   		*/
/* Outputs     : OK or ERROR     					*/
/************************************************************************/
int configElmo(){
  return adDIOConfig(ELMO_ADDR,FOCE_DIO_PORT_CONFIG);
}

/************************************************************************/
/* Function    : switchElmo                                        	*/
/* Purpose     : Set Elmo power enable bit(s) (via digital IO port)     */
/* Inputs      : control byte                                  		*/
/* Outputs     : 0 or Error code     					*/
/************************************************************************/
Byte switchElmo(Byte select){
  return adDIOOutputByte(ELMO_BOARD, ELMO_PORT,select);
}

/************************************************************************/
/* Function    : readElmo                                        	*/
/* Purpose     : Read Elmo power enable bit(s) (via digital IO port)    */
/* Inputs      : none                                   		*/
/* Outputs     : 0 or Error code     					*/
/************************************************************************/
Byte readElmo(Byte *value){
  return adDIOInputByte(ELMO_BOARD, ELMO_PORT,value);
}

/************************************************************************/
/* Function    : adSetup						*/
/* Purpose     : Set up one A/D port					*/
/* Inputs      : Socket, board, channel, setup parameters		*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int adSetup(FILE *fsock, int board, int chan, int range, int polarity, int gain)
{
    DSCADSETTINGS *setp;

    if (verifyADChan(board, chan, 1, FALSE) != OK)
	return(writeErrorReturn(fsock));

    setp = &adSettings[board][chan];
    setp->current_channel = chan;

    if ((range < 10) && (polarity == 0))
	return(writeErrorReturn(fsock));

    if (range == 10)
	setp->range = RANGE_10;
    else if (range == 5)
	setp->range = RANGE_5;
    else
	return(writeErrorReturn(fsock));

    setp->polarity = (polarity == 0) ? UNIPOLAR : BIPOLAR;

    switch(gain)
    {
      case 1:
	  setp->gain = GAIN_1;
	  break;

      case 2:
	  setp->gain = GAIN_2;
	  break;

      case 4:
	  setp->gain = GAIN_4;
	  break;

      case 8:
	  setp->gain = GAIN_8;
	  break;

      default:
	return(writeErrorReturn(fsock));
    }

    lastChan = -1;			/* Force reload of settings	*/
    return(writeOKReturn(fsock));

} /* adSetup() */


/************************************************************************/
/* Function    : adSample						*/
/* Purpose     : Sample one A/D port					*/
/* Inputs      : Socket, board, channel					*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int adSample(FILE *fsock, int board, int chan)
{
    DSCSAMPLE	sample;
    DFLOAT	voltage;
    ERRPARAMS	errorParams;	/* structure for returning error code and error string*/
    DSCB	dscb;
    Byte	result;

    if (verifyADChan(board, chan, 1, TRUE) != OK)
	return(writeErrorReturn(fsock));

    dscb = adHandles[board];

    if ((board != lastBoard) || (chan != lastChan))
    {
	if( (result = dscADSetSettings(dscb, &adSettings[board][chan])) != DE_NONE )
	{
	    dscGetLastError(&errorParams);
	    fprintf( fsock, "ERROR in dscADSetSettings: %s %s\n",
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return(ERROR);
	}
	lastBoard = board;
	lastChan = chan;
    } 

    if ( (result = dscADSample(dscb, &sample)) != DE_NONE )
    {
	dscGetLastError(&errorParams);
	fprintf( fsock, "ERROR in dscADSample: %s %s\n",
		 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	dprintf( "ERROR in dscADSample: %s %s\n",
		 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	return(ERROR);
    }

    if( dscADCodeToVoltage(dscb, adSettings[board][chan], sample, &voltage) != DE_NONE)
    {
	dscGetLastError(&errorParams);
	fprintf( fsock, "dscADCodeToVoltage error: %s %s\n", 
		 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	dprintf( "dscADCodeToVoltage error: %s %s\n", 
		 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	return(ERROR);
    }

    fprintf(fsock, "OK %6.4lf\n", voltage);
    dprintf("OK %6.4lf\n", voltage);
    return(OK);

} /* adSample() */


/************************************************************************/
/* Function    : adScan							*/
/* Purpose     : Sample multiple contiguous A/D ports			*/
/* Inputs      : Socket, board, channel, number of channels		*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int adScan(FILE *fsock, int board, int chan, int nchans)
{
    DSCSAMPLE	samples[AD_CHANS_PER_BOARD];
    DFLOAT	voltages[AD_CHANS_PER_BOARD];
    ERRPARAMS	errorParams;	/* structure for returning error code and error string*/
    DSCB	dscb;
    DSCADSCAN	scan;
    DSCADSETTINGS *setp;
    int		i;
    Byte	result;

    if (verifyADChan(board, chan, nchans, TRUE) != OK)
	return(writeErrorReturn(fsock));

    dscb = adHandles[board];
    setp = &adSettings[board][chan];

    scan.low_channel = chan;
    scan.high_channel = chan + nchans - 1;
    scan.gain = setp->gain;
    scan.sample_values = samples;

    if ((board != lastBoard) || (chan != lastChan))
    {
	if( (result = dscADSetSettings(dscb, setp)) != DE_NONE )
	{
	    dscGetLastError(&errorParams);
	    fprintf( fsock, "ERROR in dscADSetSettings: %s %s\n",
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return(ERROR);
	}
	lastBoard = board;
	lastChan = chan;
    } 

    if ( (result = dscADScan(dscb, &scan, samples)) != DE_NONE )
    {
	dscGetLastError(&errorParams);
	fprintf( fsock, "ERROR in dscADSample: %s %s\n",
		 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	return(ERROR);
    }

    for (i = 0; i < nchans; i++)
    {
	if( dscADCodeToVoltage(dscb, adSettings[board][chan], samples[i], &voltages[i]) != DE_NONE)
	{
	    dscGetLastError(&errorParams);
	    fprintf( fsock, "ERROR in dscADCodeToVoltage: %s %s\n", 
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return(ERROR);
	}
    }

    fprintf(fsock, "OK");
    dprintf("OK");

    for (i = 0; i < nchans; i++)
    {
	fprintf(fsock, " %6.4lf", voltages[i]);
	dprintf(" %6.4lf", voltages[i]);
    }

    fprintf(fsock, "\n");
    printf("\n");

    return(OK);

} /* adScan() */


/************************************************************************/
/* Function    : envReadData						*/
/* Purpose     : Sample one or more env board (sensoray) channels	*/
/* Inputs      : Socket, channel, number of channels                    */
/*               scaling (RAW|SCALED), ChannelInfo	        	*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int envReadData(FILE *fsock, int chan, int nchans, int scaling, ChannelInfo channelData[])
{

  double envData[NUMBER_OF_CHANNELS];
  int i;
  ChannelInfo *cip;

  if(chan<0 || chan>NUMBER_OF_CHANNELS 
     || nchans<=0 || nchans>NUMBER_OF_CHANNELS ||
     ((chan+nchans)>NUMBER_OF_CHANNELS)
     ){
    fprintf(fsock, "ERROR - invalid range [%d..%d]\n",chan,nchans);
    return(ERROR);
  }
  if(scaling!=RAW && scaling!=SCALED){
    fprintf(fsock, "ERROR - invalid scaling parameter [%d]\n",scaling);	    
    return(ERROR);
  }

  // a little inefficient to read them all
  // fix it if it becomes a problem...
  readAllChannelData(envData,channelData,scaling);
  fprintf(fsock, "OK ");
  for(i=chan;i<(chan+nchans);i++){
    cip=&channelData[i];
    if(scaling==SCALED){
      fprintf(fsock, "%9.4f",envData[i]);
    }else{
      fprintf(fsock, "%6.0f",envData[i]);
    }
    if(i!=(NUMBER_OF_CHANNELS-1)){
      fprintf(fsock, ",");
    }
  }	    
  fprintf(fsock, "\n");
  return(OK);
}


/************************************************************************/
/* Function    : envConfigureChannel					*/
/* Purpose     : Configure one env board (sensoray) channel     	*/
/* Inputs      : Socket, channel, sensorType, filterConstant            */
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int envConfigureChannel(FILE *fsock, ChannelInfo *channelInfo)
{
  int channel=channelInfo->channel;
  channelData[channel].sensorType    =channelInfo->sensorType;
  channelData[channel].scalar        =channelInfo->scalar;
  channelData[channel].offset        =channelInfo->offset;
  channelData[channel].filterPercent =channelInfo->filterPercent;
  channelData[channel].filterConstant=channelInfo->filterConstant;
  strcpy(channelData[channel].name,channelInfo->name);
  strcpy(channelData[channel].units,channelInfo->units);
  defineChannelSensor(channelInfo->channel,channelInfo->sensorType);
  setFilterTimeConstant(channelInfo->channel,channelInfo->filterConstant);
}


/************************************************************************/
/* Function    : envSetFilterConstant					*/
/* Purpose     : set filter constant for one Sensoray channel     	*/
/* Inputs      : Socket, channel, filterConstant                        */
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int envSetFilterConstant(FILE *fsock, ChannelInfo *channelInfo)
{
  int channel=channelInfo->channel;
  channelData[channel].filterConstant=channelInfo->filterConstant;
  setFilterTimeConstant(channelInfo->channel,channelInfo->filterConstant);
  return(OK);
}


/************************************************************************/
/* Function    : readBoardTemp   					*/
/* Purpose     : read Sensoray board temp                        	*/
/* Inputs      : Socket, units (DEGREES_C, DEGREES_K, DEGREES_F)        */
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int envReadBoardTemp(FILE *fsock, int units)
{
  double temp;
  switch(units){
  case DEGREES_F:
  case DEGREES_C:
  case DEGREES_K:
    temp=readBoardTemperature(units);
    fprintf(fsock, "OK %4.2f\n",temp);
    break;
  default:
    fprintf(fsock, "ERROR - invalid units parameter [%d]\n",units);	    
    break;
  }

  return(OK);
}


/************************************************************************/
/* Function    : readConfig      					*/
/* Purpose     : read Sensoray channel configuration, return as string	*/
/* Inputs      : Socket                                                 */
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int envReadConfig(FILE *fsock,char *delimiter)
{
  int i;
  char buf[512];
  fprintf(fsock,"OK %s\n",getChannelConfig(channelData,buf,512,delimiter));
  return(OK);
}

/************************************************************************/
/* Function    : initCPUtemp						*/
/* Purpose     : Initialize registers for reading CPU temperature	*/
/* Inputs      : None                                                   */
/* Outputs     : OK							*/
/************************************************************************/
int initCPUTemp()
{
  ioperm(LPC_INDEX, 2, 1);

  outb(0x51, LPC_INDEX);		//thermal diode mode
  outb(0x03, LPC_DATA);
  outb(0x5c, LPC_INDEX);		//unlock offset regs
  outb(0x80, LPC_DATA);
  outb(0x56, LPC_INDEX);		//offset adjustment CPU
  outb(0x70, LPC_DATA);
  outb(0x57, LPC_INDEX);		//offset adjustment ambient
  outb(0x3c, LPC_DATA);
  outb(0x5c, LPC_INDEX);		//lock offset regs
  outb(0x00, LPC_DATA);
  return(OK);
}


/************************************************************************/
/* Function    : ioSrv							*/
/* Purpose     : The I/O port server					*/
/* Inputs      : Socket for commands					*/
/* Outputs     : None							*/
/************************************************************************/
void *ioSrv(void *psock)
{
    int		i, rtn, on, sock;
    int		parm1, parm2, parm3, parm4, parm5;
    struct linger lingeropt;
    FILE	*fsock;
    char	*p,cp[2];
    char	inbuf[256];
    double      envData[NUMBER_OF_CHANNELS];
    Word        envWData;
    double      envDData;
    int         envi;
    Byte        byteData,byteParm;
    Byte        elmoByte1,elmoByte2,elmoByte3,elmoMask;
    Byte	cputemp, ambtemp;
    int         elmoParm,elmoState;
    ChannelInfo *cip, channelInfo;

    sock = (int)psock;
    on = 0;			/* Turn off Keepalives		    */
    setsockopt(sock, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
    on = 1;			/* Set RESUSEADDR		    */
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));
    lingeropt.l_onoff = 1;
    lingeropt.l_linger = 0;
    setsockopt(sock, SOL_SOCKET, SO_LINGER, &lingeropt, sizeof(lingeropt) );

    if ((fsock = fdopen(sock, "r+")) == NULL)
    {
	dprintf("fdopen failed, errno = %d\n", errno);
	close(sock);
	return(NULL);
    }

    initCPUTemp();
    dprintf("foceio waiting for input\n");

    while (fgets(inbuf, sizeof(inbuf), fsock) != NULL)
    {
	dprintf("foceio got line:  %s", inbuf);

	p = inbuf;
	deblank(p);
	pthread_mutex_lock(&iomutex);

	if (sscanf(p, "analogSample %d %d", &parm1, &parm2) >= 2)
	{
	    adSample(fsock, parm1, parm2);
	}
	else if (sscanf(p, "analogScan %d %d %d", &parm1, &parm2, &parm3) >= 3)
	{
	    adScan(fsock, parm1, parm2, parm3);
	}
	else if (sscanf(p, "analogSetup %d %d %d %d %d",
			&parm1, &parm2, &parm3, &parm4, &parm5) >= 5)
	{
	    adSetup(fsock, parm1, parm2, parm3, parm4, parm5);
	}
	else if (sscanf(p, "relayOn %d %x", &parm1, &parm2) >= 2)
	{
	    relayStates[parm1] |= parm2;
	    outw(relayStates[parm1], relayBoards[parm1]);
	    writeOKReturn(fsock);
	}
	else if (sscanf(p, "relayOff %d %x", &parm1, &parm2) >= 2)
	{
	    relayStates[parm1] &= ~parm2;
	    outw(relayStates[parm1], relayBoards[parm1]);
	    writeOKReturn(fsock);
	}
	else if (sscanf(p, "relayState %x", &parm1) >= 1)
	{
	    writeIntReturn(fsock, (parm1<MAX_RELAY_BOARDS) ? 
			   relayStates[parm1] : -1);
	}
	else if ((i = sscanf(p, "analogInit %x %d", &parm1, &parm2)) == 1)
	{
	    initADBoard(fsock, parm1, AD_INT_LEVEL);
	}
	else if (i >= 2)
	{
	    initADBoard(fsock, parm1, parm2);
	}
        else if (sscanf(p, "relayInit %x", &parm1) >= 1)
	{
	    if ((i = getRelayBoard(parm1)) >= 0)
	    {
		dprintf("Addr %x allocated to index %d\n", parm1, i);
		rtn = ioperm(parm1, 2, -1);
		writeIntReturn(fsock, i);
	    }
	    else
		fprintf(fsock, "ERROR - exceeds number of boards\n");
	}
        else if (sscanf(p, "relayAddr %d", &parm1) >= 1)
	{
	    writeIntReturn(fsock, (parm1<MAX_RELAY_BOARDS) ? 
			   relayBoards[parm1] : -1);
	}
	else if (sscanf(p, "readDIOByte %d %d", &parm1, &parm2) >= 2)
	{
	    if (adDIOInputByte(parm1, parm2, &byteData) != DE_NONE)
		writeErrorReturn(fsock);
	    else
		writeIntReturn(fsock, (int)byteData & 0xff);
	}
	else if (sscanf(p, "switchElmoBit %d %d", &elmoParm,&elmoState) >= 2)
	{
	  dprintf("got switchElmoBit %d %d (dec)\n",(Byte)elmoParm,elmoState);

	  /* check the range */
	  if(elmoParm<0 || elmoParm>7){
		fprintf(fsock, "ERROR - Elmo power bit %02x out of range 0-7`\n",elmoParm);
	  }else{
	    /* read current power byte */
	    elmoByte2=0xFF;
	    readElmo(&elmoByte2);
	    dprintf("currentPowerByte %02X\n",elmoByte2);
	    /* mask off the specified bit */
	    elmoMask=(1<<elmoParm);
	    dprintf("elmoMask %02X\n",elmoMask);
	    if(elmoState==0){
	      elmoMask= ~elmoMask;
	      elmoByte3= (elmoByte2 & elmoMask);
	    }else{
	      elmoByte3= (elmoByte2 | elmoMask);
	    }
	    dprintf("switchElmoBit switching %02X %02X\n",elmoByte3,elmoMask);
	    /* write the new power byte */
	    byteData=switchElmo(elmoByte3);
	    dprintf("switchElmoBit returning %02x\n",byteData);
	    fprintf(fsock, "OK %0x\n",byteData);
	  }
	}
	else if (sscanf(p, "switchElmo %x", &parm1) >= 1)
	{
	  dprintf("got switchElmo %02x\n",(Byte)parm1);
	  byteData=switchElmo((Byte)parm1);
	  dprintf("switchElmo returning %02x\n",byteData);
	  fprintf(fsock, "OK %0x\n",byteData);
	}
	else if (strncasecmp(p, "readElmo", strlen("readElmo")) == 0)
	{
	  byteData=0xFF;
	  readElmo(&byteData);
	  fprintf(fsock, "OK 0x%0x\n",byteData);
	}
	else if (strncasecmp(p, "CPUtemp", strlen("CPUtemp")) == 0)
	{
	    outb(0x29, LPC_INDEX);		//read CPU temp
	    cputemp = inb(LPC_DATA);
	    outb(0x2a, LPC_INDEX);		//read ambient temp
	    ambtemp = inb(LPC_DATA);
	    fprintf(fsock, "OK %d %d\n", cputemp, ambtemp);
	}
	else if (strncasecmp(p, "envInit", strlen("envInit")) == 0)
	{
	  initializeSensoray(&channelData[0],NUMBER_OF_CHANNELS);
	  writeOKReturn(fsock);
	}
	else if (strncasecmp(p, "envProductId", strlen("envProductId")) == 0)
	{
	  envWData=readProductId();
	  fprintf(fsock, "OK %d\n",envWData);
	}
	else if (strncasecmp(p, "envFirmwareVersion", strlen("envFirmwareVersion")) == 0)
	{
	  envDData=readFirmwareVersion();
	  fprintf(fsock, "OK %4.2f\n",envDData);
	}
	else if (sscanf(p, "envReadChannel %d", &parm1) >= 1)
	{
	  envReadData(fsock, parm1, 1,SCALED, &channelData[0]);
	}
	else if (sscanf(p, "envReadChannels %d %d", &parm1, &parm2) >= 2)
	{
	  envReadData(fsock, parm1, parm2,SCALED, &channelData[0]);
	}
	else if (sscanf(p, "envConfigureChannel %c %c %lf %lf %h %c %s %s", &channelInfo.channel,
			&channelInfo.sensorType,&channelInfo.scalar,&channelInfo.offset,
			&channelInfo.filterPercent,&channelInfo.filterConstant,
			channelInfo.name,channelInfo.units) >= 7)
	{
	  envConfigureChannel(fsock,&channelInfo);
	}
	else if (sscanf(p, "envSetFilterConstant %c %h", &channelInfo.channel,
			&channelInfo.filterConstant) >= 7)
	{
	  envSetFilterConstant(fsock,&channelInfo);
	}
	else if (sscanf(p, "envReadBoardTemp %d", &parm1) >= 1)
	{
	  envReadBoardTemp(fsock, parm1);
	}
	else if (sscanf(p, "envReadConfig %c", &cp) >= 1)
	{
	  cp[1]='\0';
	  envReadConfig(fsock,&cp[0]);
	}
	else if (strncasecmp(p, "envBoardAddr", strlen("envBoardAddr")) == 0)
	{
	  fprintf(fsock,"OK 0x%02X\n",ADDR_BASE);
	}
	else if (strncasecmp(p, "bye", 3) == 0)
	{
	    pthread_mutex_unlock(&iomutex);
	    dprintf("foceio returning at client request\n");

	    fclose(fsock);
	    close(sock);
	    return;
	}
	else
	  writeErrorReturn(fsock);

	pthread_mutex_unlock(&iomutex);
    }

    dprintf("foceio returning due to NULL return\n");
    fclose(fsock);
    close(sock);			/* recv() failed, close and exit */

} /* ioSrv() */


/************************************************************************/
/* Function    : iop_error						*/
/* Purpose     : Print an error string and exit				*/
/* Inputs      : Error string						*/
/* Outputs     : None (never returns)					*/
/************************************************************************/
void iop_error(char *errStr)
{
    perror(errStr);
    exit(-1);
}


/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for ioport server			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
    int			c, listenSock, connectSock, on, i, j;
    socklen_t 		addrlen;
    struct sockaddr_in	addr;		/* Socket address structure	*/
    pthread_t		thread;

    signal(SIGCHLD, SIG_IGN);		/* Ignore child signals		*/

    for (i = 0; i < MAX_RELAY_BOARDS; i++)
    {					/* Init board states		*/
	relayBoards[i] = -1;
	relayStates[i] = 0;
    }

    for (i = 0; i < MAX_ANALOG_BOARDS; i++)
	adBoards[i] = -1;
    
    configElmo();
    switchElmo(ELMO_ALL_ON);

    memset(adSettings, -1, sizeof(adSettings));

    memset((char *)&addr, 0, sizeof(addr));
    addr.sin_family = PF_INET;
    addr.sin_addr.s_addr = LISTEN_IP;
    addr.sin_port = htons(IOSRV_PORT);

    while ((c = getopt(argc, argv, "p:")) != -1)
	switch(c)
	{
	  case 'p':
	      addr.sin_port = htons(atoi(optarg));
	      break;

	  default:
	      usage(argv[0]);
	      return(1);
	}

    if ((listenSock = socket(PF_INET, SOCK_STREAM, 0)) < 0)
	iop_error("foceio error getting listen socket:");

    dprintf("ioport got listen socket %d\n", listenSock);

    on = 0;			/* Turn off Keepalives			*/
    setsockopt(listenSock, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
    on = 1;			/* Set RESUSEADDR			*/
    setsockopt(listenSock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));

    /* Bind TCP socket address*/
    if ( bind(listenSock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
    {
	close(listenSock);
	iop_error("foceio error binding socket");
    }

    dprintf("foceio bound listen socket to port %d\n", ntohs(addr.sin_port));

    if ( listen(listenSock, 1) < 0 )
	iop_error("foceio error on listen");

#ifndef DEBUG
    daemon(0,0);
#endif

    while(1)	/* Main loop.  Listen forever to get connections	*/
    {
	dprintf("foceio trying accept()\n");
	memset( (char *)&addr, 0, sizeof(addr) );
	addrlen = sizeof(addr);
	connectSock = accept(listenSock, (struct sockaddr *)&addr, &addrlen);
	dprintf("foceio return from accept, socket = %d\n", connectSock);

	if (connectSock > 0)	/* Successful accept, got a connected socket*/
	{
	    if (pthread_create(&thread, NULL, ioSrv, (void *)connectSock) != 0)
		dprintf("Error in pthread_create()!\n");
	}
	else
	    iop_error("foceio failed accept()");
    }

    return(0);

} /* main() */

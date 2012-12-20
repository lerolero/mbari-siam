/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/********************************************************************************/
/* Summary  : Test Routine for Diamond DMM-32X-AT board				*/	
/* Filename : adtest.c								*/
/* Author   : Robert Herlien (rah), shamelessly stolen from Diamond's test code	*/
/* Project  : FOCE								*/
/* Revision : 1.0								*/
/* Created  : 1apr2008 (April Fools!)						*/
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
/* 1apr2008 rah - created from DSCUDADScan.c					*/
/* $Log: adtest.c,v $
/* Revision 1.2  2012/12/17 21:43:33  oreilly
/* added copyright header
/*
/* Revision 1.1  2008/11/06 00:29:31  bobh
/* Moved src/org/mbari/siam/foce/native to native/foce.
/*
 */
/********************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>
#include <string.h>
#include "dscud.h"
#include "mbariTypes.h"
#include "mbariConst.h"


/* Following constants to conform to jumpers on board		*/

#define BASE_ADDR	0x300
#define INT_LEVEL	7

MLocal char *ranges[] = 
  {"+/-5V Bipolar", "Unsupported", "+/-10V Bipolar", "0-10V Unipolar"};
//    {"Unsupported", "+/-5V Bipolar", "0-10V Unipolar", "+/-10V Bipolar"};


/************************************************************************/
/* Function    : adBoardInit						*/
/* Purpose     : Initialize A/D Board 					*/
/* Inputs      : Base I/O Address, Interrupt level			*/
/* Outputs     : Error code						*/
/************************************************************************/
int adBoardInit(WORD baseAddr, BYTE intLevel, DSCB *dscbp)
{
    DSCCB	dsccb;		/* structure containing board settings	*/
    ERRPARAMS	errorParams;	/* structure for returning error code and error string*/
    Byte	rtn;

    memset(&dsccb, 0, sizeof(DSCCB));
    dsccb.base_address = BASE_ADDR;
    dsccb.int_level = INT_LEVEL;

    if((rtn = dscInitBoard(DSC_DMM32X, &dsccb, dscbp)) !=  DE_NONE)
    {
	dscGetLastError(&errorParams);
	fprintf( stderr, "dscInitBoard error: %s %s\n",
		 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	return((int)rtn);
    }

    return(OK);

} /* adBoardInit() */


/************************************************************************/
/* Function    : adSetSettings						*/
/* Purpose     : Set A/D board for new settings				*/
/* Inputs      : Board ID, settings struct				*/
/* Outputs     : Error code						*/
/************************************************************************/
int adSetSettings(DSCB board, DSCADSETTINGS *settings)
{
    DSCCB	dsccb;		/* structure containing board settings	*/
    ERRPARAMS	errorParams;	/* structure for returning error code and error string*/
    Byte	result;

    if( (result = dscADSetSettings(board, settings)) != DE_NONE )
    {
	dscGetLastError(&errorParams);
	fprintf( stderr, "dscADSetSettings error: %s %s\n",
		 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	return((int)result);
    }

    return(OK);

} /* adSetSettings() */


/************************************************************************/
/* Function    : validateChannel					*/
/* Purpose     : Validate channel number and number of channels		*/
/* Inputs      : Channel number, number of channels			*/
/* Outputs     : Error code						*/
/************************************************************************/
int validateChannel(int chan, int numChans)
{
//    printf("validateChannel(%d, %d)\n", chan, numChans);
    if ((numChans < 1) || (numChans > 8))
	return(ERROR);
    if ((chan < 0) || (chan > 31))
	return(ERROR);
    if ((chan >= 16) && (chan < 24))
	return(ERROR);
    if (numChans > (8 - (chan % 8)))
	return(ERROR);
    return(OK);
}
    

/************************************************************************/
/* Function    : adSample						*/
/* Purpose     : Sample A/D channel, with or without averaging		*/
/* Inputs      : Board ID, scan struct, num avgs			*/
/* Outputs     : Error code						*/
/* Comments    : Uses DSCADSETTINGS set up by menu system		*/
/************************************************************************/
int adSample(DSCB dscb, DSCADSETTINGS settings, int numAvgs)
{
    Byte	result;
    DSCSAMPLE	sample;
    DFLOAT	voltage;
    ERRPARAMS	errorParams;	/* structure for returning error code and error string*/

    if (numAvgs <= 1)
    {
	if ( (result = dscADSample(dscb, &sample)) != DE_NONE )
	{
	    dscGetLastError(&errorParams);
	    fprintf( stderr, "dscADSample error: %s %s\n",
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return((int)result);
	}

	if( dscADCodeToVoltage(dscb, settings, sample, &voltage) != DE_NONE)
	{
	    dscGetLastError(&errorParams);
	    fprintf( stderr, "dscADCodeToVoltage error: %s %s\n", 
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return(ERROR);
	}

	printf("Sample readout: %hd, Actual voltage: %5.3lfV\n", sample, voltage);  
    }
    else
    {
	if ( (result = dscADSampleAvg(dscb, &voltage, numAvgs)) != DE_NONE )
	{
	    dscGetLastError(&errorParams);
	    fprintf( stderr, "dscADSampleAvg error: %s %s\n",
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return((int)result);
	}

	printf("Actual voltage: %5.3lfV (%hd)\n", sample, voltage, voltage);  
/*
      if ( (result = dscADSampleAvg(dscb, &sample, numAvgs)) != DE_NONE )
	{
	    dscGetLastError(&errorParams);
	    fprintf( stderr, "dscADSample error: %s %s\n",
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return((int)result);
	}

	if( dscADCodeToVoltage(dscb, settings, sample, &voltage) != DE_NONE)
	{
	    dscGetLastError(&errorParams);
	    fprintf( stderr, "dscADCodeToVoltage error: %s %s\n", 
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return(ERROR);
	}

	printf("Sample readout: %hd, Actual voltage: %5.3lfV\n", sample, voltage);  
*/
    }

    return(OK);
}


/************************************************************************/
/* Function    : adScan							*/
/* Purpose     : Sample Multiple A/D channels, with or without averaging*/
/* Inputs      : Board ID, scan struct, num avgs			*/
/* Outputs     : Error code						*/
/************************************************************************/
int adScan(DSCB dscb, DSCADSCAN *scanp, DSCADSETTINGS settings, int numAvgs)
{
    Byte	result;
    DSCSAMPLE	sample;
    DSCSAMPLE	samples[8];
    DFLOAT	voltage;
    DFLOAT	voltages[8]; 
    int		i;
    ERRPARAMS	errorParams;	/* structure for returning error code and error string*/

    scanp->gain = settings.gain;
    scanp->sample_values = samples;

    if (numAvgs <= 1)
    {
	if ( (result = dscADScan(dscb, scanp, samples)) != DE_NONE )
	{
	    dscGetLastError(&errorParams);
	    fprintf( stderr, "dscADSample error: %s %s\n",
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return((int)result);
	}

	for (i = 0; i < scanp->high_channel - scanp->low_channel + 1; i++)
	{
	    if( dscADCodeToVoltage(dscb, settings, samples[i], &voltage) != DE_NONE)
	    {
		dscGetLastError(&errorParams);
		fprintf( stderr, "dscADCodeToVoltage error: %s %s\n", 
			 dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    }

	    printf("Chan: %d  Sample readout: %hd, Actual voltage: %5.3lfV\n", 
		   i + scanp->low_channel, samples[i], voltage);  
	}
    }
    else
    {
	if ( (result = dscADScanAvg(dscb, scanp, voltages, numAvgs)) != DE_NONE )
	{
	    dscGetLastError(&errorParams);
	    fprintf( stderr, "dscADSampleAvg error: %s %s\n",
		     dscGetErrorString(errorParams.ErrCode), errorParams.errstring );
	    return((int)result);
	}

	for (i = 0; i < scanp->high_channel - scanp->low_channel + 1; i++)
	{
	    printf("Chan: %d  Sample readout: %hd, Actual voltage: %5.3lfV\n", 
		   i + scanp->low_channel, samples[i], voltages[i]);  
	}
    }

    return(OK);
}


/************************************************************************/
/* Function    : main							*/
/* Purpose     : Main Routine for this test program			*/
/* Inputs      : None 							*/
/* Outputs     : Exit code						*/
/************************************************************************/
int main( void )
{
    Byte	result;		/* returned error code			*/
    DSCB	dscb;		/* handle used to refer to the board 	*/
    ERRPARAMS	errorParams;	/* structure for returning error code and error string*/
    DSCADSCAN	dscadscan;	/* structure containing A/D scan settings*/
    int		c, val;		/* input character for loop, scratch var*/
    int		numChans = 1;	/* Number of channels to convert	*/
    int		numAvgs = 1;	/* Number of conversions to average	*/
    char	instr[256];

    DSCADSETTINGS dscadsettings =
	{0, GAIN_1, RANGE_10, UNIPOLAR, TRUE, 0};

    /************************************/
    /* Initialize DSCUD Library		*/
    /************************************/

    if( dscInit( DSC_VERSION ) != DE_NONE )
    {
        dscGetLastError(&errorParams);
	fprintf( stderr, "dscInit error: %s %s\n",
		 dscGetErrorString(errorParams.ErrCode),
		 errorParams.errstring );
	return(1);
    }

    /************************************/
    /* Initialize DMM-32X-AT Board	*/
    /************************************/

    printf( "\nDMM32X BOARD INITIALIZATION:\n" );

    if (adBoardInit(BASE_ADDR, INT_LEVEL, &dscb) != OK)
	return(2);

    dscadsettings.range = RANGE_10;
    dscadsettings.polarity = UNIPOLAR;
    dscadsettings.gain = GAIN_1;
    dscadsettings.load_cal = (BYTE)TRUE;
    dscadsettings.current_channel = 0;
    dscadsettings.scan_interval = SCAN_INTERVAL_20;

    if( adSetSettings( dscb, &dscadsettings ) != OK)
	return(3);

    dscadscan.low_channel = 0;
    dscadscan.high_channel = 0;
    dscadscan.gain = GAIN_1;

    while (TRUE)
    {
	fflush(stdin);
	printf("\n\nChoose a function by number:\n\n");

	printf("1: Change A/D channel (%d)\n", dscadscan.low_channel);
	printf("2: Change number of channels to convert for multi-channel scan (%d)\n",
	       numChans);
	printf("3: Change Input range (%s)\n", 
	       ranges[((dscadsettings.range & 1) << 1) | (dscadsettings.polarity & 1)]);
	printf("4: Change gain of amplifier (%d)\n", 
	       1 << (dscadsettings.gain & 3));
	printf("5: Change number of conversions to average (%d)\n", numAvgs);
	printf("6: Start conversion\n");
	printf("0: Exit this program\n");

	fflush(stdin);
	fgets(instr, sizeof(instr), stdin);
	c = instr[0] - '0';
	if ((c < 0) || (c > 6))
	    printf("Input not understood\n\n");
	else
	    switch(c)
	    {
	      case 0:
		  dscFree();
		  printf("Good bye\n");
		  return(0);

	      case 1:
		  printf("Channel number (or start of range of channels) to convert:  ");
		  scanf("%d", &val);
		  if (validateChannel(val, numChans) != OK)
		      printf("Invalid channel or channel/numChannels goes past 8 channel boundary\n");
		  else
		  {
		      dscadsettings.current_channel = (Byte)val;
		      dscadscan.low_channel = (Byte)val;
		      dscadscan.high_channel = (Byte)(val + numChans - 1);
		      adSetSettings(dscb, &dscadsettings);
		  }
		  break;

	      case 2:
		  printf("Number of channels to convert (1-8):  ");
		  scanf("%d", &val);
		  if (validateChannel(dscadscan.low_channel, val) != OK)
		      printf("ERROR - goes past 8 channel boundary\n");
		  else
		  {
		      numChans = val;
		      dscadscan.high_channel = (Byte)(dscadscan.low_channel + numChans - 1);
		  }
		  break;

	      case 3:
		  printf("Input range (after the gain=1-8 amplifier)\n");
		  printf("0: +/-5V  Bipolar\n");
		  printf("1: Invalid\n");
		  printf("2: +/-10V Bipolar\n");
		  printf("3: 0-10V  Unipolar\n");
		  printf("Note that 0-5V Unipolar is invalid\n");
		  printf("Range [0-3]:  ");
		  fflush(stdin);
		  fgets(instr, sizeof(instr), stdin);
		  val = (int)(instr[0] - '0');
		  if ((val < 0) || (val > 3))
		      printf("Invalid input range\n");
		  dscadsettings.range = ((val>>1) & 1);
		  dscadsettings.polarity = (val & 1);
		  adSetSettings(dscb, &dscadsettings);
		  break;

	      case 4:
		  printf("Amplifier gain [1,2,4,8]:  ");
		  fflush(stdin);
		  fgets(instr, sizeof(instr), stdin);
		  switch(instr[0])
		  {
		    case '1':
			dscadsettings.gain = dscadscan.gain = GAIN_1;
			adSetSettings(dscb, &dscadsettings);
			break;
		    case '2':
			dscadsettings.gain = dscadscan.gain = GAIN_2;
			adSetSettings(dscb, &dscadsettings);
			break;
		    case '4':
			dscadsettings.gain = dscadscan.gain = GAIN_4;
			adSetSettings(dscb, &dscadsettings);
			break;
		    case '8':
			dscadsettings.gain = dscadscan.gain = GAIN_8;
			adSetSettings(dscb, &dscadsettings);
			break;
		    default:
			printf("Invalid gain\n");
			break;
		  }
		  break;

	      case 5:
		  printf("Number of conversions to average (1-32767):  ");
		  scanf("%d", &val);
		  if ((val < 1) || (val > 32767))
		      printf("Invalid number\n");
		  else
		      numAvgs = val;
		  break;

	      case 6:
		  if (numChans == 1)
		      adSample(dscb, dscadsettings, numAvgs);
		  else
		      adScan(dscb, &dscadscan, dscadsettings, numAvgs);
		  break;

	    }

    }

    return 0;

} /* main() */

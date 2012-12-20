/************************************************************************/
/* Copyright 2004 MBARI							*/
/************************************************************************/
/* Summary  : Application to read/write/test Ricoh RTC on Sidearm4	*/
/* Filename : ricohRTC.c						*/
/* Author   : Robert Herlien (rah)					*/
/* Project  : SIAM							*/
/* Revision : 1.0							*/
/* Created  : 02/03/2004						*/
/*									*/
/* MBARI provides this documentation and code "as is", with no warranty,*/
/* express or implied, of its quality or consistency. It is provided	*/
/* without support and without obligation on the part of the Monterey	*/
/* Bay Aquarium Research Institute to assist in its use, correction,	*/
/* modification, or enhancement. This information should not be		*/
/* published or distributed to third parties without specific written	*/
/* permission from MBARI.						*/
/************************************************************************/
/* Modification History:						*/
/* 03feb2004 rah - created						*/
/************************************************************************/

#include <stdio.h>
#include <mbariTypes.h>			/* MBARI type definitions	*/
#include <ricohRTC.h>			/* Definitions for the chip	*/
#include <stdlib.h>
#include <time.h>
#include <sys/time.h>
#include <sys/types.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>


#ifndef OK
#define OK		0
#endif
#ifndef ERROR
#define ERROR		(-1)
#endif

//#define DEBUG
//#define TIMING_ON

#ifdef DEBUG
#define dbgPrintReg()	system("cat /proc/cpu/registers/PPSR");
#else
#define dbgPrintReg()	/* */
#endif

#ifndef NumberOf
#define NumberOf(arr)	((sizeof(arr) / sizeof(arr[0])))
#endif


/****************************************/
/*	External Data (from getopt)	*/
/****************************************/

extern char	*optarg;
extern int	optind;


/****************************************/
/*	Module Local Data		*/
/****************************************/
const char *revision="$Name: HEAD $ $Id: ricohRTC.c,v 1.1 2008/11/04 22:11:30 bobh Exp $";
int		lddioFd;
const int	mdays[12] =
{ 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

const char	*regNames[] = 
  {"Second Counter", "Minute Counter", "Hour Counter", "Day of Week Counter",
   "Day of Month Counter", "Month Counter and Century Bit", "Year Counter",
   "Oscillation Adjustment Reg", "Alarm_W Minute", "Alarm_W Hour",
   "Alarm_W Day of Week", "Alarm_D Minute", "Alarm_D Hour", "Unused Reg",
   "Control Reg 1", "Control Reg 2"
  };

#ifdef TIMING_ON
clock_t rtcSelectTicks;
clock_t rtcUnselectTicks;
double rtcEnabledSeconds;
#endif

/********************************/
/*	Forward Declarations	*/
/********************************/

int	printRTCtime(Void);
int	setupRTC(Void);
int	readRTC(struct tm *tmPtr);
int	setRTC(struct tm *tmPtr);
int	setRTCfromSystem(Void);
int	setSystemFromRTC(Void);
Void	readRTCReg(Void);
Void	writeRTCReg(Void);
Void	setHourMode(int mode);
int	getHourMode(Void);
int	suspendForMinutes(int minutes);
int	rtcReadBytes(int, Byte *, size_t);
Void	rtcWriteBytes(int, Byte *, size_t);


/************************************************************************/
/* Function    : printMenu						*/
/* Purpose     : Print the menu prompt					*/
/* Inputs      : None							*/
/* Outputs     : None							*/
/************************************************************************/
void printMenu(void)
{
  printf("\n");
  printf("1) Read RTC and system time\n");
  printf("2) Set system time from RTC time\n");
  printf("3) Set RTC time from system time\n");
  printf("4) Set RTC time from prompt\n");
  printf("5) Get/set 12/24 hour format\n");
  printf("6) Read RTC Register\n");
  printf("7) Write RTC Register\n");
  //  printf("8) Sleep for number of minutes (CAUTION - not functional)\n");
  printf("0) Exit this program\n");

} /* printMenu() */


/************************************************************************/
/* Function    : interative						*/
/* Purpose     : Menu-driven interactive routine for ricohRTC		*/
/* Inputs      : None							*/
/* Outputs     : Error return number					*/
/************************************************************************/
int interactive(Void)
{
  int		i, j;
  time_t	now;
  struct tm	tmnow;
  char		inbuf[256];

  printf("\n\t\t\tRicoh RTC Test Program for Sidearm 4.\n\n");

  while(1)
  {
    printMenu();
    inbuf[0] = '\0';
    fgets(inbuf, 256, stdin);

    if (sscanf(inbuf, " %d", &i) < 1)
      printf("Unrecognized input\n");
    else switch(i)
    {
      case 0:
	/* Exit		*/
	return(0);

      case 1:
	/* Read the system and RTC date/time */
	time(&now);
	printf("\nSystem time:  %s", asctime(gmtime(&now)));
	printRTCtime();
	break;

      case 2:
	/* Set System date/time	from RTC */
	setSystemFromRTC();
	break;

      case 3:
	/* Set RTC date/time from system */
	setRTCfromSystem();
	break;

      case 4:
	printf("\nEnter time/date as yyyy/mm/dd hh:mm:ss\n");
	fgets(inbuf, 256, stdin);
	if (sscanf(inbuf, " %d/%d/%d %d:%d:%d", &tmnow.tm_year,
		   &tmnow.tm_mon, &tmnow.tm_mday, &tmnow.tm_hour,
		   &tmnow.tm_min, &tmnow.tm_sec) < 6)
	  printf("\nUnrecognized input\n");
	else
	{
	  tmnow.tm_mon--;
	  tmnow.tm_year -= 1900;
	  if (setRTC(&tmnow) == -1)
	    printf("Error in setting Ricoh RTC\n");
	}
	break;

      case 5:
	printf("\n0 - Set 12 hour format (AM/PM)\n");
	printf("1 - Set 24 hour format\n");
	printf("<return> to print current format\n");
	fgets(inbuf, 256, stdin);
	if (sscanf(inbuf, " %d", &j) < 1)
	  printf("%d hour mode active\n", getHourMode() ? 24 : 12);
	else if ((j != 0) && (j != 1))
	  printf("Bad input\n");
	else
	{
	  setHourMode(j);
	  printf("Now you must set the time for the RTC registers to be properly formatted\n");
	}
	break;

      case 6:
	readRTCReg();
	break;

      case 7:
	writeRTCReg();
	break;

      case 8:
	break;
	printf("\nSuspend for how many minutes? (1-1440)  ");
	fflush(stdout);
	fgets(inbuf, 256, stdin);
	if (sscanf(inbuf, " %d", &j) < 1)
	  printf("\nUnrecognized input\n");
	else if (j > 1440)
	  printf("\nMust be less than 1440 minutes\n");
	else
	{
	  printf("Sleeping for %d minutes\n", j);
	  fflush(stdout);

	  if (suspendForMinutes(j) == ERROR)
	    printf("Error in trying to suspend\n");
	  else
	    printf("\nAwake!\n");
	}
	break;

      default:
	printf("Unrecognized input\n");
    }
  }
  
} /* interactive() */


/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for rtctest program			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
  int		c, i;
  int		rtn = 0;
  int		goInteractive = 0, gotCmdLineArgs = 0;
  double	oscadj;
  signed char	adjVal;
  Byte		modeByte;
  time_t	now;
  struct tm	tmnow;

  if ((lddioFd = open (LDDIO_DEV, O_RDWR)) == -1)
  {
    perror(LDDIO_DEV);
    exit(errno);
  }

  if (setupRTC() != OK)
    exit(1);

  rtcReadBytes(CTRL_REG2, &modeByte, 1);
  modeByte &= ~XSTP_BIT;			/* Reset the XSTP bit	*/
  rtcWriteBytes(CTRL_REG2, &modeByte, 1);

  while ((c = getopt(argc, argv, "a:hik:m:st")) != -1)
    switch(c)
    {
      case 'i':
	goInteractive = gotCmdLineArgs = 1;
	break;

      case 's':
	gotCmdLineArgs = 1;
	rtn = setSystemFromRTC();
	break;

      case 't':
	gotCmdLineArgs = 1;
	rtn = setRTCfromSystem();
	break;

      case 'a':
	gotCmdLineArgs = 1;
	oscadj = atof(optarg);
	if (oscadj > 195.)
	{
	  printf("Adjustment too high, setting to max\n");
	  adjVal = 63;
	  rtn = -1;
	}
	else if (oscadj < -195.)
	{
	  printf("Adjustment too low, setting to min\n");
	  adjVal = -63;
	  rtn = -1;
	}
	else
	  adjVal  = ((signed char)(oscadj/3.05) & 0x7f);
	rtcWriteBytes(OSC_ADJ_REG, &adjVal, 1);
	break;

      case 'k':
	gotCmdLineArgs = 1;
	i = atoi(optarg);
	rtcReadBytes(CTRL_REG1, &modeByte, 1);
	if (i)
	  modeByte |= KHZ32_BIT;
	else
	  modeByte &= ~KHZ32_BIT;
	rtcWriteBytes(CTRL_REG1, &modeByte, 1);
	break;

      case 'm':
	gotCmdLineArgs = 1;
	i = atoi(optarg);
	if ((rtn = readRTC(&tmnow)) != OK)
	{
	  printf("Error reading RTC\n");
	  break;
	}
	rtcReadBytes(CTRL_REG1, &modeByte, 1);
	if (i == 12)
	  modeByte &= ~AMPM_BIT;
	else
	  modeByte |= AMPM_BIT;
	rtcWriteBytes(CTRL_REG1, &modeByte, 1);

	/* MUST set time after changing AM/PM bit */
	rtn = setRTC(&tmnow);
	break;

      case 'h':
      default:
	gotCmdLineArgs = 1;
	printf("Usage: %s [-i] [-s] [-a oscadj] [-k {0|1}] [-m{12|24}] [-h]\n", argv[0]);
	printf("\n-i\t\tInteractive mode - menu-driven interactive program that\n");
	printf("\t\tallows user to set & display registers, time,etc\n");
	printf("-s\t\tSet system time from RTC time; you must have root privs\n");
	printf("-t\t\tSet RTC time from system time\n");
	printf("-a {oscadj}\tSet oscillator adjustment register.  'oscadj' is\n");
	printf("\t\tvalue, in ppm, that clock runs fast or slow.  If clock runs\n");
	printf("\t\tfast, ppm should be positive; if slow, negative.  Adjustment\n");
	printf("\t\ttakes place in steps of ~3 ppm, and has a range of +-192 ppm\n");
	printf("-k {0|1}\tDisable or enable 32 KHz output\n");
	printf("-m {12|24}\tSet 12 hour or 24 hour mode of operation of RTC\n");
	printf("\t\tNote 24 hour mode conforms better to Unix timekeeping\n");
	printf("-h\t\tPrint this help message\n");
	break;
    }

  if (goInteractive)
    return(interactive());

  if (gotCmdLineArgs == 0)
  {
    time(&now);
    printf("\nSystem time:  %s", asctime(gmtime(&now)));
    printRTCtime();
  }

  return(rtn);

} /* main() */


/************************************************************************/
/* Function    : printRTCtime						*/
/* Purpose     : Print time from Ricoh RTC				*/
/* Inputs      : None							*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int printRTCtime(Void)
{
  int		rtn;
  struct tm	tmnow;

  memset(&tmnow, 0, sizeof(tmnow));
  if ((rtn = readRTC(&tmnow)) != OK)
    printf("Error reading Ricoh RTC chip, printing time anyway\n");

  printf("\nRTC Time:  %02d/%02d/%d  ",
	 tmnow.tm_mday, tmnow.tm_mon + 1, tmnow.tm_year + 1900);

  if (getHourMode())
    printf("%02d:%02d:%02d\n", tmnow.tm_hour, tmnow.tm_min, tmnow.tm_sec);
  else 
    printf("%02d:%02d:%02d %s\n", 
	   (tmnow.tm_hour > 12) ? tmnow.tm_hour - 12 : tmnow.tm_hour,
	   tmnow.tm_min, tmnow.tm_sec, (tmnow.tm_hour >= 12) ? "PM" : "AM");

  return(rtn);

} /* printRTCtime() */


/************************************************************************/
/* Routines to manipulate data in RTC registers				*/
/************************************************************************/

/************************************************************************/
/* Function    : toBcd							*/
/* Purpose     : Convert to BCD						*/
/* Input       : Byte to convert					*/
/* Outputs     : BCD value						*/
/************************************************************************/
int toBcd(int val)
{
  int b0 = val % 10;
  int b1 = val / 10;
  return(((b1<<4) & 0xf0) | b0);

} /* toBcd() */

  
/************************************************************************/
/* Function    : fromBcd						*/
/* Purpose     : Convert from BCD					*/
/* Input       : Byte to convert					*/
/* Outputs     : BCD value						*/
/************************************************************************/
int fromBcd(int bcd)
{
  return((bcd & 0xf) + ((bcd>>4)&0xf)*10);

} /* fromBcd() */


/************************************************************************/
/* Function    : readRTC						*/
/* Purpose     : Read the Ricoh RTC, return time in struct tm		*/
/* Input       : Struct tm to fill in					*/
/* Outputs     : OK or ERROR						*/
/* Side Effect : Fills in *tmPtr					*/
/************************************************************************/
int readRTC(struct tm *tmPtr)
{
  int		rtn, hour, month, year;
  RtcTime	rtctm;
  Byte		ctrl1 = 0;

  if ((rtn = rtcReadBytes(SECOND_REG, (Byte *)&rtctm, RTC_TIME_SIZE)) != OK)
    return(rtn);

#if 0
  printf("Raw RTC output in hex: %x/%x/%x  %x:%x:%x weekday %x\n",
	 rtctm.year, rtctm.monthAndCenturyBit, rtctm.dayOfMonth, 
	 rtctm.hour, rtctm.minute, rtctm.second, rtctm.dayOfWeek);
#endif

  if ((tmPtr->tm_sec = fromBcd(rtctm.second)) > 60)
    rtn = ERROR;
  if ((tmPtr->tm_min = fromBcd(rtctm.minute)) >= 60)
    rtn = ERROR;

  if ((rtn = rtcReadBytes(CTRL_REG1, &ctrl1, 1)) != OK)
    rtn = ERROR;

  if (ctrl1 & AMPM_BIT)
    hour = fromBcd(rtctm.hour);
  else
  {
    hour = fromBcd(rtctm.hour & 0x1f);
    if (hour == 12)
      hour = 0;
    if (rtctm.hour & 0x20)
      hour += 12;
  }

  if (hour > 23)
    rtn = ERROR;
  tmPtr->tm_hour = hour;
  
  month = fromBcd(rtctm.monthAndCenturyBit & 0x1f) - 1;
  if (month > 11)
    rtn = ERROR;
  tmPtr->tm_mon = month;

  if ((tmPtr->tm_mday = fromBcd(rtctm.dayOfMonth)) > mdays[month])
    rtn = ERROR;

  if ((year = fromBcd(rtctm.year)) > 99)
    rtn = ERROR;

  if (rtctm.monthAndCenturyBit & CENTURY_BIT)
    year += 100;
  tmPtr->tm_year = year;

  if ((tmPtr->tm_wday = rtctm.dayOfWeek & 7) > 6)
    rtn = ERROR;

  tmPtr->tm_yday = 0;		/* We don't know/care about julian day	*/
  tmPtr->tm_isdst = 0;		/* Not using daylight savings time	*/
  return(rtn);

} /* readRTC() */


/************************************************************************/
/* Function    : setRTC							*/
/* Purpose     : Set the Ricoh RTC with values from struct tm		*/
/* Input       : Struct tm						*/
/* Outputs     : OK or ERROR						*/
/* Side Effect : Sets the RTC Time					*/
/************************************************************************/
int setRTC(struct tm *tmPtr)
{
  int		rtn, year;
  RtcTime	rtctm;
  Byte		ctrl1;

  memset(&rtctm, 0, sizeof(RtcTime));

  rtctm.second = toBcd(tmPtr->tm_sec);
  rtctm.minute = toBcd(tmPtr->tm_min);

  if ((rtn = rtcReadBytes(CTRL_REG1, &ctrl1, 1)) != OK)
    return(rtn);

  if (ctrl1 & AMPM_BIT)
    rtctm.hour = toBcd(tmPtr->tm_hour);
  else
  {
    if (tmPtr->tm_hour == 0)
      rtctm.hour = 0x12;
    else if (tmPtr->tm_hour == 12)
      rtctm.hour = 0x32;
    else if (tmPtr->tm_hour > 12)
      rtctm.hour = 0x20 + toBcd(tmPtr->tm_hour-12);
    else
      rtctm.hour = toBcd(tmPtr->tm_hour);
  }

  rtctm.dayOfMonth = toBcd(tmPtr->tm_mday);
  rtctm.dayOfWeek = tmPtr->tm_wday;

  rtctm.monthAndCenturyBit = toBcd(tmPtr->tm_mon + 1);

  if ((year = tmPtr->tm_year) > 99)
  {
    year -= 100;
    rtctm.monthAndCenturyBit += 0x80;
  }

  rtctm.year = toBcd(year);
  rtcWriteBytes(SECOND_REG, (Byte *)&rtctm, RTC_TIME_SIZE);
  return(OK);

} /* setRTC() */


/************************************************************************/
/* Function    : setRTCfromSystem					*/
/* Purpose     : Set the Ricoh RTC from system time			*/
/* Input       : None							*/
/* Outputs     : OK or ERROR						*/
/* Side Effect : Sets the RTC Time					*/
/************************************************************************/
int setRTCfromSystem(Void)
{
  int		rtn, oldrtn;
  time_t	now, oldtime;
  struct tm	*tmPtr, oldtm;

  time(&now);
  if ((tmPtr = gmtime(&now)) == NULL)
  {
    perror("gmtime");
    rtn = ERROR;
  }
  else
  {
    oldrtn = readRTC(&oldtm);

    printf("\nSetting RTC date/time to %d/%02d/%d, %02d:%02d:%02d\n",
	   tmPtr->tm_mday, tmPtr->tm_mon + 1, tmPtr->tm_year + 1900,
	   tmPtr->tm_hour, tmPtr->tm_min, tmPtr->tm_sec);
    if ((rtn = setRTC(tmPtr)) == ERROR)
      printf("Error in setting Ricoh RTC\n");

    if ((oldrtn == OK) && ((oldtime = mktime(&oldtm)) != (time_t)(-1)))
      printf("RicohRTC offset from system time was %d seconds\n",
	     (int)(oldtime - now));
  }

  return(rtn);

} /* setRTCfromSystem() */


/************************************************************************/
/* Function    : setSystemFromRTC					*/
/* Purpose     : Set the system time from Ricoh RTC time		*/
/* Input       : None							*/
/* Outputs     : OK or ERROR						*/
/* Side Effect : Sets the system Time					*/
/************************************************************************/
int setSystemFromRTC(Void)
{
  time_t	now;
  struct tm	tmnow;
  struct timeval tvnow;

  if (readRTC(&tmnow) != OK)
    printf("Error reading Ricoh RTC chip\n");
  else if ((now = mktime(&tmnow)) == (time_t)(-1))
    printf("\nmktime can't parse data from RTC chip\n");
  else
  {
    printf("\nSetting system date/time to %d/%02d/%d, %02d:%02d:%02d.\n",
	   tmnow.tm_mday, tmnow.tm_mon + 1, tmnow.tm_year + 1900,
	   tmnow.tm_hour, tmnow.tm_min, tmnow.tm_sec);

    tvnow.tv_sec = now;
    tvnow.tv_usec = 500000;	/* On average, we should be 1/2 way into sec*/
    if (settimeofday(&tvnow, NULL) == -1)
      perror("settimeofday");
    else
      return(OK);
  }
  return(ERROR);

} /* setSystemFromRTC() */


/************************************************************************/
/* Function    : readRTCReg						*/
/* Purpose     : Read a register from the Ricoh RTC			*/
/* Input       : None							*/
/* Outputs     : None							*/
/* Side Effect : Prompts user for register, prints value		*/
/************************************************************************/
Void	readRTCReg(Void)
{
  int	i;
  Byte	val;
  Byte	inbuf[256];

  printf("\nRead which RTC register? (0-15)\n\n");
  for (i = 0; i < NumberOf(regNames); i++)
    printf("  %d\t%s\n", i, regNames[i]);
  
  inbuf[0] = '\0';
  fgets(inbuf, 256, stdin);

  if (sscanf(inbuf, " %d", &i) < 1)
    printf("\nUnrecognized input\n");
  else if (i > NumberOf(regNames))
    printf("\nBad input\n");
  else if (rtcReadBytes(i, &val, 1) != OK)
    printf("\nError reading RTC register %d\n", i);
  else
    printf("%s  =  %#x", regNames[i], val);

} /* readRTCReg() */


/************************************************************************/
/* Function    : writeRTCReg						*/
/* Purpose     : Write a register from the Ricoh RTC			*/
/* Input       : None							*/
/* Outputs     : None							*/
/* Side Effect : Prompts user for register and value, sets it		*/
/************************************************************************/
Void	writeRTCReg(Void)
{
  int	i, val;
  Byte	bval;
  Byte	inbuf[256];

  printf("\nWrite which RTC register? (0-15)\n\n");
  for (i = 0; i < NumberOf(regNames); i++)
    printf("  %d\t%s\n", i, regNames[i]);
  
  inbuf[0] = '\0';
  fgets(inbuf, 256, stdin);

  if (sscanf(inbuf, " %d", &i) < 1)
    printf("\nUnrecognized input\n");
  else if (i > NumberOf(regNames))
    printf("\nBad input\n");
  else
  {
    printf("\n\nNew value (in hex) for %s  ", regNames[i]);
    fflush(stdout);
    inbuf[0] = '\0';
    fgets(inbuf, 256, stdin);
    if (sscanf(inbuf, " %x", &val) < 1)
      printf("\nUnrecognized input\n");
    else if (val > 0xff)
      printf("\nBad input - registers are one byte wide!\n");
    else
    {	
      bval = val;
      rtcWriteBytes(i, &bval, 1);
      printf("\nNew value for %s  =  %#x", regNames[i], val);
    }
  }

} /* writeRTCReg() */


/************************************************************************/
/* Function    : setHourMode						*/
/* Purpose     : Set 12 or 24 hour mode					*/
/* Input       : 0 for 12 hour mode, 1 for 24 hour mode			*/
/* Outputs     : None							*/
/************************************************************************/
Void	setHourMode(int mode)
{
  Byte	modeByte;

  modeByte = mode ? AMPM_BIT : 0;
  rtcWriteBytes(CTRL_REG1, &modeByte, 1);

} /* setHourMode() */


/************************************************************************/
/* Function    : getHourMode						*/
/* Purpose     : Get 12 or 24 hour mode					*/
/* Input       : None							*/
/* Outputs     : 0 for 12 hour mode, 1 for 24 hour mode			*/
/************************************************************************/
int	getHourMode(Void)
{
  Byte	modeByte;

  rtcReadBytes(CTRL_REG1, &modeByte, 1);
  return((modeByte & AMPM_BIT) ? 1 : 0);

} /* getHourMode() */


/************************************************************************/
/* Function    : suspendForMinutes					*/
/* Purpose     : Suspend processor for given number of minutes		*/
/* Inputs      : None							*/
/* Outputs     : 0 for success, -1 for failure				*/
/************************************************************************/
int suspendForMinutes(int minutes)
{
  int		suspendfd, rtn;
  struct tm	tmnow;
  struct rtcTime alarmTime;
  char		c;
  Byte		modeByte;

  if ((suspendfd = open ("/proc/sys/pm/suspend", O_RDONLY)) == -1)
  {
    perror("/proc/sys/pm/suspend");
    return(ERROR);
  }

  if ((rtn = readRTC(&tmnow)) != OK)
    return(rtn);

  if ((rtn = rtcReadBytes(CTRL_REG1, &modeByte, 1)) != OK)
    return(rtn);

  printf("\nCurrent time is %d/%02d/%d, %02d:%02d:%02d.\n",
	 tmnow.tm_mday, tmnow.tm_mon + 1, tmnow.tm_year + 1900,
	 tmnow.tm_hour, tmnow.tm_min, tmnow.tm_sec);

  tmnow.tm_min += minutes;
  tmnow.tm_hour += tmnow.tm_min/60;
  tmnow.tm_min %= 60;
  tmnow.tm_hour %= 24;

  memset(&alarmTime, 0, sizeof(alarmTime));
  alarmTime.minute = toBcd(tmnow.tm_min);

  if (modeByte & AMPM_BIT)
    alarmTime.hour = toBcd(tmnow.tm_hour);
  else
  {
    if (tmnow.tm_hour == 0)
      alarmTime.hour = 0x12;
    else if (tmnow.tm_hour == 12)
      alarmTime.hour = 0x32;
    else if (tmnow.tm_hour > 12)
      alarmTime.hour = 0x20 + toBcd(tmnow.tm_hour-12);
    else
      alarmTime.hour = toBcd(tmnow.tm_hour);
  }

  printf("Setting alarm for %02x:%02x (BCD)\n",
	 alarmTime.hour, alarmTime.minute);
  fflush(stdout);

  rtcWriteBytes(ALARMD_MINUTE_REG, &alarmTime.minute, 2);
  modeByte |= ENBL_ALARMD_BIT;
  rtcWriteBytes(CTRL_REG1, &modeByte, 1);

#if 1	
  printf("Interrupt Mask Register ICMR = ");
  fflush(stdout);
  system("cat /proc/cpu/registers/ICMR");
  printf("\n");
  fflush(stdout);
  sleep(1);
#endif

  read(suspendfd, &c, 1);
  printf("Return from suspend\n");

  close(suspendfd);
  modeByte &= ~ENBL_ALARMD_BIT;
  rtcWriteBytes(CTRL_REG1, &modeByte, 1);

  return(0);

  } /* suspendForMinutes() */
	  

/************************************************************************/
/* Following routines do low-level I/O to RTC chip			*/
/************************************************************************/

/************************************************************************/
/* Function    : setupRTC						*/
/* Purpose     : Setup the LDDIO bits for talking to RTC		*/
/* Input       : None							*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int	setupRTC(Void)
{
  if (write(lddioFd, LDDIO_SETUP_STR, strlen(LDDIO_SETUP_STR)) <
      strlen(LDDIO_SETUP_STR))
  {
    printf("\nFailure in configuring /dev/lddio for RTC\n\n");
    return(ERROR);
  }
#ifdef DEBUG
  printf("PPDR = ");
  fflush(stdout);
  system("cat /proc/cpu/registers/PPDR");
#endif
  return(OK);

} /* setupRTC() */


/************************************************************************/
/* Function    : rtcSelect						*/
/* Purpose     : Select RTC chip					*/
/* Input       : None							*/
/* Outputs     : None							*/
/************************************************************************/
Void rtcSelect(Void)
{
#ifdef TIMING_ON
  rtcSelectTicks=clock();
  printf("\nrtcSelectTicks: %ld\n",rtcSelectTicks);
  fflush(stdout);
#endif
  write(lddioFd, SELECT_STR, strlen(SELECT_STR));
  dbgPrintReg();
  usleep(100);
    
} /* rtcSelect() */


/************************************************************************/
/* Function    : rtcUnselect						*/
/* Purpose     : Deselect RTC chip					*/
/* Input       : None							*/
/* Outputs     : None							*/
/************************************************************************/
Void rtcUnselect(Void)
{
#ifdef TIMING_ON
  rtcUnselectTicks=clock();
  rtcEnabledSeconds=((double)(rtcUnselectTicks-rtcSelectTicks))/CLOCKS_PER_SEC;
  printf("\nrtcUnselectTicks: %ld\n",rtcUnselectTicks);
  printf("\nrtcEnabledSeconds: %03.3f (%ld CLOCKS_PER_SEC)\n",rtcEnabledSeconds,CLOCKS_PER_SEC);
  fflush(stdout);
#endif
  write(lddioFd, DESELECT_STR, strlen(DESELECT_STR));
  dbgPrintReg();
  usleep(100);
    
} /* rtcUnselect() */


/************************************************************************/
/* Function    : rtcSendByte						*/
/* Purpose     : Send one byte to RTC chip (low-level I/O		*/
/* Input       : Byte to send						*/
/* Outputs     : None							*/
/* Comments    : Assumes select line already active			*/
/************************************************************************/
Void rtcSendByte(Byte byteToSend)
{
  Reg int	bit;

#ifdef DEBUG
  printf("Sending byte %x to RTC\n", byteToSend);
#endif  
  for(bit = 7; bit >= 0; bit--)
  {
    if (byteToSend & (1<<bit))
      write(lddioFd, "30+", 3);
    else
      write(lddioFd, "20+10-", 6);
    dbgPrintReg();

    write(lddioFd, "20-", 3);
    dbgPrintReg();
  }

} /* rtcSendByte() */


/************************************************************************/
/* Function    : rtcReadBytes						*/
/* Purpose     : Read Bytes from RTC Chip				*/
/* Input       : Register nmbr to read, ptr to buffer, buffer length	*/
/* Outputs     : OK or ERROR						*/
/************************************************************************/
int rtcReadBytes(int regNumber, Byte *buf, size_t buflen)
{
  Byte		fmtByte;
  Reg int	i, byt, bit;
  unsigned long	val;
  char		lddBuf[16];

  fmtByte = (regNumber & 0x0f) << 4;
  fmtByte |= ((buflen == 1) ? 0x0c : 0x04);

  dbgPrintReg();
  rtcSelect();
  rtcSendByte(fmtByte);

  for (i = 0; i < buflen; i++)
  {
    byt = 0;
    for(bit = 7; bit >= 0; bit--)
    {
      write(lddioFd, "20+", 3);
      dbgPrintReg();
      write(lddioFd, "20-", 3);
      dbgPrintReg();
      if (read(lddioFd, lddBuf, 8) <= 0)
      {
	fprintf(stderr, "Error reading /dev/lddio\n");
	return(ERROR);
      }
#ifdef DEBUG
      lddBuf[8] = '\0';
      printf("LDDIO returns %s\n", lddBuf);
      dbgPrintReg();
#endif
      if (sscanf(lddBuf, " %lx", &val) < 1)
      {
	fprintf(stderr, "Bad value from /dev/lddio\n");
	return(ERROR);
      }

      if (val & RTC_MISO)
	byt |= (1 << bit);
    }
    buf[i] = byt;
  }

  rtcUnselect();
  dbgPrintReg();
  return(OK);

} /* rtcReadBytes() */


/************************************************************************/
/* Function    : rtcWriteBytes						*/
/* Purpose     : Write Bytes to RTC Chip				*/
/* Input       : Reg nmbr, ptr to buffer to write, number bytes to write*/
/* Outputs     : None							*/
/************************************************************************/
Void rtcWriteBytes(int regNumber, Byte *buf, size_t len)
{
  Byte		fmtByte;
  int		i;

  fmtByte = (regNumber & 0x0f) << 4;
  if (len == 1)
    fmtByte |= 0x08;

  dbgPrintReg();
  rtcSelect();
  rtcSendByte(fmtByte);

  for(i = 0; i < len; i++)
    rtcSendByte(buf[i]);

  rtcUnselect();
  dbgPrintReg();

} /* rtcWriteBytes() */

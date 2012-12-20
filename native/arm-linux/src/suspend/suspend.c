/************************************************************************/
/* Copyright 2004 MBARI							*/
/************************************************************************/
/* Summary  : Suspend - put Processor to sleep				*/
/* Filename : suspend.c							*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : SIAM							*/
/* Version  : 1.0							*/
/* Created  : 04/08/2004						*/
/* Note     : Must be owned by root, and have the setuid bit set!	*/
/************************************************************************/
/* Modification History:						*/
/* 04/08/2004 rah created						*/
/************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <linux/rtc.h>
#include <sys/ioctl.h>
#include <time.h>

#define SUSPEND_FILE	"/proc/sys/pm/suspend"
#define RTC_FILE	"/dev/rtc"
#define DEFAULT_SLEEP_SECONDS	60
#define MIN_SLEEP_SECONDS	3
#define MAX_SLEEP_SECONDS	3600
#define FALSE			0
#define TRUE			~FALSE


/****************************************/
/*	External Data (from getopt)	*/
/****************************************/

extern char	*optarg;
extern int	optind;


/****************************************/
/*	Global Data 			*/
/****************************************/

int	notPastMidnight = FALSE;
int	verbose = FALSE;


/************************************************************************/
/* Function    : sus_error						*/
/* Purpose     : Print an error string and exit				*/
/* Inputs      : Error string						*/
/* Outputs     : None (never returns)					*/
/************************************************************************/
void sus_error(char *errStr)
{
  perror(errStr);
  exit(-1);
}


/************************************************************************/
/* Function    : suspendForSeconds					*/
/* Purpose     : Suspend processor for given number of seconds		*/
/* Inputs      : Number of seconds to suspend, open fds for suspend, rtc*/
/* Outputs     : 0 for success, -1 for failure				*/
/************************************************************************/
int suspendForSeconds(int seconds, int suspendFd, int rtcFd)
{
  time_t	  now;
  struct rtc_time rtc_tm;
  struct tm	  *tmPtr;
  unsigned long	  dummy;
  char		  c;
  int		  nowDay;

  time(&now);
  if ((tmPtr = gmtime(&now)) == NULL)
      sus_error("Error calling gmtime(): ");

  nowDay = tmPtr->tm_mday;

  if (ioctl(rtcFd, RTC_SET_TIME, tmPtr) == -1)
    sus_error("Can't set RTC time: ");

  if (ioctl(rtcFd, RTC_RD_TIME, &rtc_tm) == -1)
    sus_error("Can't read RTC time: ");

  if (verbose)
  {
    printf("Current time is %d/%02d/%d, %02d:%02d:%02d.\n",
	   tmPtr->tm_mday, tmPtr->tm_mon + 1, tmPtr->tm_year + 1900,
	   tmPtr->tm_hour, tmPtr->tm_min, tmPtr->tm_sec);
    fflush(stdout);
  }

  now += seconds;
  if ((tmPtr = gmtime(&now)) == NULL)
    sus_error("Can't read RTC time: ");

  if (notPastMidnight && (tmPtr->tm_mday != nowDay))
  {
    if (verbose)
      printf("Sleep would go past midnight - just returning\n");
    return(-1);
  }

  if (verbose)
  {
    printf("Setting alarm for %d/%02d/%d, %02d:%02d:%02d.\n",
	   tmPtr->tm_mday, tmPtr->tm_mon + 1, tmPtr->tm_year + 1900,
	   tmPtr->tm_hour, tmPtr->tm_min, tmPtr->tm_sec);
  }
  
  if (ioctl(rtcFd, RTC_ALM_SET, (struct rtc_time *)tmPtr) == -1)
    sus_error("Can't set RTC alarm time: ");

  if (ioctl(rtcFd, RTC_AIE_ON, 0) == -1)
    sus_error("Can't set RTC interrupt enable: ");

  if (verbose)
  {
    system("cat /proc/driver/rtc");
    fflush(stdout);
    sleep(1);
  }

  read(suspendFd, &c, 1);

  if (verbose)
    printf("Return from suspend\n");

  /* Read the RTC to discard any pending interrupts	*/
  if (read(rtcFd, &dummy, sizeof(unsigned long)) == -1)
    sus_error("Can't read RTC: ");

  if (ioctl(rtcFd, RTC_AIE_OFF, 0) == -1)
    sus_error("Can't clear RTC interrupt enable: ");

  return(0);

  } /* suspendForSeconds() */
	  

/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for application			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
  int	c, seconds, suspendFd, rtcFd;

  while ((c = getopt(argc, argv, "nv")) != -1)
    switch(c)
    {
      case 'n':
	notPastMidnight = TRUE;
	break;

      case 'v':
	verbose = TRUE;
	break;

      default:
	printf("Usage: %s [-n] [-v] [seconds]>\n", argv[0]);
	printf("\t-n: Don't sleep past midnight\n");
	printf("\t-v: Verbose - print debug messages\n");
	printf("\tseconds: Number of seconds to sleep (default %d)\n", 
	       DEFAULT_SLEEP_SECONDS);
	exit(0);
    }

  seconds = DEFAULT_SLEEP_SECONDS;
  if (argc > optind)
    seconds = atoi(argv[optind]);
  if (seconds < MIN_SLEEP_SECONDS)
    exit(0);
  if (seconds > MAX_SLEEP_SECONDS)
    seconds = MAX_SLEEP_SECONDS;

  if (verbose)
    printf("%s for %d seconds\n", argv[0], seconds);

  if ((suspendFd = open(SUSPEND_FILE,  O_RDONLY)) < 0)
    sus_error("Error opening " SUSPEND_FILE);

  if ((rtcFd = open(RTC_FILE,  O_RDWR)) < 0)
    sus_error("Error opening " RTC_FILE);

  suspendForSeconds(seconds, suspendFd, rtcFd);

  close(suspendFd);
  close(rtcFd);

  return 0;
}


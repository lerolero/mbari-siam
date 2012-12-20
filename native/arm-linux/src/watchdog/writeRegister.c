/************************************************************************/
/* Copyright 2008 MBARI						                                    	*/
/************************************************************************/
/* Summary  : writeRegister - Writes a value to /proc/cpu/register/...  */
/* Filename : setPWER.c							                                    */
/* Author   : B. Kieft - modifications to original code by Bob Herlien  */
/* Project  : SIAM							                                        */
/* Version  : 1.0						                                           	*/
/* Created  : 12/02/2008						                                    */
/* Note     : Must be owned by root, and have the setuid bit set!	      */
/************************************************************************/
/* Modification History:						                                    */
/* 12/02/2008 initial creation                              						*/
/************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <fcntl.h>

const char *revision="$Name: $ $Id: $";

char regString[64];

/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for application			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
  int	val, fd;
  char	buf[64];
  char *regName;

  if ( (argc < 2) || (sscanf(argv[1], " %s", &regName) < 1) || (sscanf(argv[2], " %x", &val) < 1) )
  {
    printf("Usage: %s <name> <value>", argv[0]);
    printf("    where <name> is register name (e.g. OWER) and <value> is hex value for PWER register\n");
    exit(1);
  }

  sprintf(regString, "/proc/cpu/registers/%s",&regName);
  if ((fd = open(regString, O_WRONLY)) == -1)
  {
    system("insmod --noksymoops registers");
    if ((fd = open(regString, O_WRONLY)) == -1)
    	printf("Failed to open register file descriptor\n");
      exit(1);
  }

  sprintf(buf, "0x%x", val);
  write(fd, buf, strlen(buf));
  close(fd);
  return 0;
}


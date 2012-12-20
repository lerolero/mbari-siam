/************************************************************************/
/* Copyright 2004 MBARI							*/
/************************************************************************/
/* Summary  : setPWER - Set the PWER register				*/
/* Filename : setPWER.c							*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : SIAM							*/
/* Version  : 1.0							*/
/* Created  : 04/08/2004						*/
/* Note     : Must be owned by root, and have the setuid bit set!	*/
/************************************************************************/
/* Modification History:						*/
/* 04/22/2004 rah created						*/
/************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <fcntl.h>


static char	*pwerReg = "/proc/cpu/registers/PWER";


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

  if ((argc < 2) || (sscanf(argv[1], " %x", &val) < 1))
  {
    printf("Usage: %s <value>", argv[0]);
    printf("    where <value> is hex value for PWER register");
    exit(1);
  }

  if ((fd = open(pwerReg, O_WRONLY)) == -1)
  {
    system("insmod --noksymoops registers");
    if ((fd = open(pwerReg, O_WRONLY)) == -1)
      exit(1);
  }

  sprintf(buf, "0x%x", val);
  write(fd, buf, strlen(buf));
  close(fd);
  return 0;
}


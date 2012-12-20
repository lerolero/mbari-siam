/************************************************************************/
/* Copyright 2008 MBARI							*/
/************************************************************************/
/* Summary  : power - Utility to turn on/off power relays		*/
/* Filename : power.c							*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : FOCE							*/
/* Version  : 1.0							*/
/* Created  : 21 April 2008						*/
/************************************************************************/
/* Modification History:						*/
/* 21apr2008, rah - created						*/
/* $Log: rtest.c,v $
/* Revision 1.1  2008/11/06 00:29:31  bobh
/* Moved src/org/mbari/siam/foce/native to native/foce.
/*
 */
/************************************************************************/

#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/io.h>
#include <ctype.h>
#include <string.h>


/************************************************************************/
/* Function    : usage							*/
/* Purpose     : Print usage string					*/
/* Inputs      : None							*/
/* Outputs     : None							*/
/************************************************************************/
static void usage(char *cmdName)
{
    printf("usage: %s <addr> <bits> <bitMask>\n", cmdName);
    printf("  addr - I/O address of relay board\n");
    printf("  bits - New state of relay bits\n");
    printf("  bitMask - Mask for which bits to change\n");
}       


/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for ioport server			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
    int		addr, bits, bitMask;
    int	       	curState = 0;
    char	*envP;
    char	buffer[128], buffer2[128];

    if (argc < 4)
    {
	usage(argv[0]);
	return(1);
    }

//    addr = atoi(argv[1]);
//    bits = atoi(argv[2]);
//    bitMask = atoi(argv[3]);
    sscanf(argv[1], "%x", &addr);
    sscanf(argv[2], "%x", &bits);
    sscanf(argv[3], "%x", &bitMask);

    printf("%s:  %x %x %x\n", argv[0], addr, bits, bitMask);

    /* Try to find environment variable to tell us current state of bits*/
    sprintf(buffer, "RELAY%x", addr);
    if ((envP=getenv(buffer)) != NULL)
    {
	if (sscanf(envP, " %x", &curState) < 1)
	    curState = 0;
	else
	  printf("Got environ var, current state is %x\n", curState);
    }

    /* Calculate new relay bits		*/
    curState &= ~bitMask;
    curState |= bits & bitMask;


    /* Save relay state in environment variable */
    memset(buffer, 0, sizeof(buffer));
    sprintf(buffer, "declare -x RELAY%x", addr);
    sprintf(buffer+strlen(buffer), "=%x", curState);
    printf("Executing %s\n", buffer);
    system(buffer);
    //    sprintf(buffer, "RELAY%x", addr);
    //    sprintf(buffer2, "%x", curState);
    //    printf("setenv: %s %s\n", buffer, buffer2);
    //    setenv(buffer, buffer2, -1);

    /* Print it back */
    sprintf(buffer, "RELAY%x", addr);
    envP = getenv(buffer);
    if (envP == NULL)
      printf("%s not found\n", buffer);
    else
      printf("%s = %s\n", buffer, envP);

    printf("New state of relays:  0x%x\n", curState);
    return(0);

} /* main() */

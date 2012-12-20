/************************************************************************/
/* Copyright 2008 MBARI							*/
/************************************************************************/
/* Summary  : relay - Utility to turn on/off power relays		*/
/* Filename : relay.c							*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : FOCE							*/
/* Version  : 1.0							*/
/* Created  : 21 April 2008						*/
/************************************************************************/
/* Modification History:						*/
/* 21apr2008, rah - created						*/
/* $Log: relay.c,v $
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

#define RELAY_FILE "/etc/siam/relays.state"

/* Uncomment one of these two lines	*/
#define dprintf
//#define dprintf printf


/************************************************************************/
/* Function    : usage							*/
/* Purpose     : Print usage string					*/
/* Inputs      : None							*/
/* Outputs     : None							*/
/************************************************************************/
static void usage(char *cmdName)
{
    printf("usage: %s <addr> <bitMask> <newState>\n", cmdName);
    printf("  addr - I/O address of relay board\n");
    printf("  bitMask - Mask for which bits to change\n");
    printf("  newState - New state of relay bits\n");
}       


/************************************************************************/
/* Function    : getRelayState						*/
/* Purpose     : Read the state of the relays from the persistence file */
/* Inputs      : FILE ptr to state file, address of relay board		*/
/* Outputs     : Value of state, or 0 if not found			*/
/* Side Effect : Positions file at line where we found state		*/
/* Comment     : If file or line not there, we want to default to 0	*/
/************************************************************************/
int getRelayState(FILE *fp, int addr)
{
    int		rtn, foundAddr, foundVal;
    long	fpos;
    char	buffer[128];

    rewind(fp);

    for (fpos = 0; fgets(buffer,sizeof(buffer),fp) != NULL; fpos = ftell(fp))
    {
        if (sscanf(buffer, " %x = %x", &foundAddr, &foundVal) >= 2)
	{
	    if (foundAddr == addr)
	    {
	        dprintf("Found %x = %x\n", foundAddr, foundVal);
	        fseek(fp, fpos, SEEK_SET);
	        return(foundVal);
	    }
	}
    }

    return(0);
}


/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for ioport server			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
    int		addr, bits, bitMask, rtn;
    int	       	curState;
    FILE	*fp;

    if (argc < 4)
    {
	usage(argv[0]);
	return(1);
    }

    sscanf(argv[1], "%x", &addr);
    sscanf(argv[2], "%x", &bitMask);
    sscanf(argv[3], "%x", &bits);

    /* Open the relay state file to find the existing state	*/
    if ((fp = fopen(RELAY_FILE, "r+")) == NULL)
    {
        dprintf("%s not found.  Open for write\n", RELAY_FILE);
        fp = fopen(RELAY_FILE, "w");
	curState = 0;
    }
    else
        curState = getRelayState(fp, addr);

    /* Calculate new relay bits		*/
    curState &= ~bitMask;
    curState |= bits & bitMask;

    /* Output new relay bits		*/
    ioperm(addr, 2, -1);
    outw(curState, addr);

    /* Save relay state in relay file			*/
    if (fp != NULL)
    {
	rtn = fprintf(fp, "%6x = %6x\n", addr, curState);
	fclose(fp);
        dprintf("fprintf returns %d\n", rtn);
    }


    dprintf("New state of relays:  0x%x\n", curState);
    return(0);

} /* main() */

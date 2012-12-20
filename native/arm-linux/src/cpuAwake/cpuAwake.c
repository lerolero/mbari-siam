/************************************************************************/
/* Copyright 2004 MBARI							*/
/************************************************************************/
/* Summary  : cpuAwake - Send message to SIAM to keep CPU awake		*/
/* Filename : cpuAwake.c						*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : SIAM							*/
/* Version  : 1.0							*/
/* Created  : 05/10/2004						*/
/************************************************************************/
/* Modification History:						*/
/* 05/10/2004 rah created						*/
/************************************************************************/
/* Utility that the user can invoke to keep the CPU awake for a duration,
    or to cause it to wake up at a future time.  The usage is:
    cpuAwake <when> <howLong> [-r rqstID] [-n] [hostname]
    <when> - seconds in future that you'll need CPU awake
    <howLong> - how many seconds that you'll need CPU awake
    [-r rqstID] - requestor ID.  Defaults to 1, the user ID
    [-n] - don't retry if fail to find <hostname>.  Useful for
    local scripts.
    [hostname] - node you want awake.  Defaults to "localhost"
*/

#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <ctype.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

typedef int boolean;

#define RQST_USER	1
#define RQST_SHORTHAUL	2
#define DFLT_DURATION	600
#define CPULEASE_TCP_PORT 5505
#define FALSE		0
#define TRUE		~FALSE


/****************************************/
/*	External Data (from getopt)	*/
/****************************************/

extern char	*optarg;
extern int	optind;


/************************************************************************/
/* Function    : c_error						*/
/* Purpose     : Print an error string and exit				*/
/* Inputs      : Error string						*/
/* Outputs     : None (never returns)					*/
/************************************************************************/
void c_error(char *errStr)
{
  perror(errStr);
  exit(-1);
}


/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for application			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
    int		i, sock, c, on;
    struct sockaddr_in	addr;		/* Socket address structure	*/
    struct hostent *hostp;		/* Host entry structure		*/
    int		rqstID = RQST_USER;
    long	when = 0;
    long	howLong = DFLT_DURATION;
    boolean 	doRetry = TRUE;
    boolean 	verbose = TRUE;
    long	parms[2] = {0, 0};
    int		nparms = 0;
    char	*hostname = "localhost";
    char	*p, *q;
    char	rqst[128];

    while ((c = getopt(argc, argv, "nqr:")) != -1)
      switch(c)
      {
        case 'n':
	    doRetry = FALSE;
	    break;

        case 'q':
	    verbose = FALSE;
	    break;

        case 'r':
	    rqstID = atoi(optarg);
	    break;

        default:
	printf("Usage:\n");
	printf("cpuAwake <when> <howLong> [-q] [-r rqstID] [hostname]\n");
	printf("\t<when> - seconds in future that you'll need CPU awake\n");
	printf("\t<howLong> - how many seconds that you'll need CPU awake\n");
	printf("\t[-q] - quiet mode -- don't print to stdout\n");
	printf("\t[-r rqstID] - requestor ID.  Defaults to 1, the user ID\n");
	printf("\t[hostname] - node you want awake.  Defaults to \"localhost\"\n");
	exit(0);
      }

    for (i = optind; i < argc; i++)
    {
        if (isalpha(*argv[i]) || (*argv[i] == '/'))
	    hostname = argv[i];
	else if (nparms < 2)
	    parms[nparms++] = atoi(argv[i]);
    }

    if (nparms >= 2)
    {
        when = parms[0];
	howLong = parms[1];
    }
    else if (nparms == 1)
        howLong = parms[0];

    if (verbose)
      printf("Looking for node at \"%s\"\n",  hostname);

    if ((hostp = gethostbyname(hostname)) == NULL)
    {
      printf("Can't find host \"%s\"", hostname);
      exit(1);
    }

    if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0)
        c_error("Error creating socket:");

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    memcpy(&addr.sin_addr, hostp->h_addr, sizeof(addr.sin_addr));
    addr.sin_port = htons(CPULEASE_TCP_PORT);

    on = 1;
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));

    //    printf("Connecting to Inet address %x port %d\n", 
    //	   addr.sin_addr.s_addr, addr.sin_port);

    if (connect(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
      c_error("Cannot connect:");

    if (verbose)
      printf("Connected to %s\n", hostp->h_name);

    sprintf(rqst, "awake %u %lu %lu\n", rqstID, 1000*when, 1000*howLong);

    write(sock, rqst, strlen(rqst));

    if (verbose)
      printf("Requested CPU lease for %d seconds starting in %d seconds\n\n",
	     howLong, when);

} /* main() */


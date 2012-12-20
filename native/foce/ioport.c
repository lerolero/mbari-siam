/************************************************************************/
/* Copyright 2008 MBARI							*/
/************************************************************************/
/* Summary  : ioport - Server to read/write I/O ports at root priv	*/
/* Filename : ioport.c							*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : FOCE							*/
/* Version  : 1.0							*/
/* Created  : 15 April 2008						*/
/************************************************************************/
/* Modification History:						*/
/* 15apr2008, rah - created						*/
/* $Log: ioport.c,v $
/* Revision 1.1  2008/11/06 00:29:31  bobh
/* Moved src/org/mbari/siam/foce/native to native/foce.
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

//#define DEBUG		/* Turn on to (1) not make it a daemon, and	*/
			/*  (2) cause debug output messages		*/
#ifdef DEBUG
#define dprintf printf
#else
#define dprintf
#endif
	
#define IOSRV_PORT	7933		/* Arbitrary, hopefully unused port*/

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
/*	Local Data		*/
/********************************/

static char	inbuf[1024];		/* Input buffer			*/
static char	*okMsg = "OK\n";


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
/* Function    : readLine						*/
/* Purpose     : Read a line from the socket until '\n', return # bytes	*/
/* Inputs      : Socket, char buffer					*/
/* Outputs     : Number of bytes read, or -1 if error			*/
/************************************************************************/
int readLine(int sock, char *buf, int bufLen)
{
    char	*p;
    int		len, rtn;

    bzero(buf, bufLen);

    for (p = buf, len = 0; len < bufLen-1; p++, len++)
    {
	if ((rtn = read(sock, p, 1)) <= 0)
	    return(rtn);

	if ((*p == '\n') || (*p == '\r') || (*p == '\0'))
	{
	    *p = '\0';
	    return(len);
	}
    }

    *p = '\0';
    return(len);
}


/************************************************************************/
/* Function    : writeReturn						*/
/* Purpose     : Send integer return value on the socket		*/
/* Inputs      : Socket, integer					*/
/* Outputs     : None							*/
/************************************************************************/
void writeReturn(int sock, int rtn)
{
    char	buf[256];

    sprintf(buf, "%d\n", rtn);
    write(sock, buf, strlen(buf));
    dprintf("ioSrv returning: %s\n", buf);
}


/************************************************************************/
/* Function    : ioSrv							*/
/* Purpose     : The I/O port server					*/
/* Inputs      : Socket for commands					*/
/* Outputs     : None							*/
/************************************************************************/
void ioSrv(int sock)
{
    int		addr, val, rtn, on;
    struct linger lingeropt;
    char	*p;

    on = 0;			/* Turn off Keepalives		    */
    setsockopt(sock, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
    on = 1;			/* Set RESUSEADDR		    */
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));
    lingeropt.l_onoff = 1;
    lingeropt.l_linger = 0;
    setsockopt(sock, SOL_SOCKET, SO_LINGER, &lingeropt, sizeof(lingeropt) );

    dprintf("ioSrv waiting for input\n");

    while ((rtn = readLine(sock, inbuf, sizeof(inbuf))) > 0)
    {
	dprintf("ioSrv got line:  %s\n", inbuf);

	p = inbuf;
	deblank(p);

	if (sscanf(p, "map %u %u", &addr, &val) >= 2)
	{
	    rtn = ioperm(addr, val, -1);
	    writeReturn(sock, rtn);	    
	}
	else if (sscanf(p, "outw %u %u", &addr, &val) >= 2)
	{
	    outw(val, addr);
	    writeReturn(sock, 0);
	}
	else if (sscanf(p, "outb %u %u", &addr, &val) >= 2)
	{
	    outb(val, addr);
	    writeReturn(sock, 0);
	}
	else if (sscanf(p, "inw %u %u", &addr, &val) >= 2)
	{
	    val = inw(addr);
	    writeReturn(sock, val);
	}
	else if (sscanf(p, "inb %u %u", &addr, &val) >= 2)
	{
	    val = inb(addr);
	    writeReturn(sock, val);
	}
	else if (strncasecmp(p, "bye", 3) == 0)
	{
	    dprintf("ioSrv returning at client request\n");
	    close(sock);
	    return;
	}
    }

    dprintf("ioSrv returning due to return value of %d\n", rtn);
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
    int			c, listenSock, connectSock, on;
    socklen_t 		addrlen;
    struct sockaddr_in	addr;		/* Socket address structure	*/

    signal(SIGCHLD, SIG_IGN);		/* Ignore child signals		*/

    bzero((char *)&addr, sizeof(addr));
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
	iop_error("ioSrv error getting listen socket:");

    dprintf("ioport got listen socket %d\n", listenSock);

    on = 0;			/* Turn off Keepalives			*/
    setsockopt(listenSock, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
    on = 1;			/* Set RESUSEADDR			*/
    setsockopt(listenSock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));

    /* Bind TCP socket address*/
    if ( bind(listenSock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
    {
	close(listenSock);
	iop_error("ioSrv error binding socket");
    }

    dprintf("ioSrv bound listen socket to port %d\n", ntohs(addr.sin_port));

    if ( listen(listenSock, 1) < 0 )
	iop_error("ioSrv error on listen");

#ifndef DEBUG
    daemon(0,0);
#endif

    while(1)	/* Main loop.  Listen forever to get connections	*/
    {
	dprintf("ioSrv trying accept()\n");
	bzero( (char *)&addr, sizeof(addr) );
	addrlen = sizeof(addr);
	connectSock = accept(listenSock, (struct sockaddr *)&addr, &addrlen);
	dprintf("ioSrv return from accept, socket = %d\n", connectSock);

	if (connectSock > 0)	/* Successful accept, got a connected socket*/
	{
	    switch(fork())	/* Spawn the server			*/
	    {
	      case 0:		/* This is the child process		*/
		  ioSrv(connectSock);  	/* Do ioport service as long as */
		  			/*  client is there		*/
		  return(0);
		  break;

	      case -1:		/* Error				*/
		  dprintf("fork() error!\n");
		  break;

	      default:
		  dprintf("Successful fork()\n");
		  break;
	    }
	}
	else
	    iop_error("ioSrv failed accept()");
    }

    return(0);

} /* main() */

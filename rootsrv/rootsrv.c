/************************************************************************/
/* Copyright 2003 MBARI							*/
/************************************************************************/
/* Summary  : Root server - Server to execute commands at root priv	*/
/* Filename : rootsrv.c							*/
/* Author   : Bob Herlien (rah)						*/
/* Project  : SIAM							*/
/* Version  : 1.0							*/
/* Created  : 06/10/2003						*/
/************************************************************************/
/* Modification History:						*/
/* 06/10/2003 rah created						*/
/************************************************************************/

#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <ctype.h>
#include <string.h>

//#define DEBUG		/* Turn on to (1) not make it a daemon, and	*/
			/*  (2) cause debug output messages		*/
		/* 6/23/03 DEBUG no longer works after the daemon() call*/
#define ROOTSRV_PORT	7932		/* Arbitrary, hopefully unused port*/
#define SUSPEND_FILE	"/proc/sys/pm/suspend"
#define ROOTSRV_DIR	"/root/rootsrv"

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

static int	bufChars = 0;
static char	inbuf[1024];		/* Input buffer			*/
static char	cmdbuf[1024];		/* Command buffer		*/
static char	*doneMsg = "rootsrv: done\n";


/************************************************************************/
/* Function    : usage							*/
/* Purpose     : Print usage string					*/
/* Inputs      : None							*/
/* Outputs     : None							*/
/************************************************************************/
static void usage (void)
{
  printf ("usage: rootsrv [-p port]\n\
	[-p port] TCP port number to use\n");
}       


/************************************************************************/
/* Function    : rootSrv						*/
/* Purpose     : The Root Priv server					*/
/* Inputs      : Socket for commands, fd of suspend file		*/
/* Outputs     : None							*/
/************************************************************************/
void rootSrv(int sock, int suspendFd)
{
  char	*p, c;
  int	fd;

  bufChars = 0;				/* Init character counter	*/
  bzero(inbuf, sizeof(inbuf));
  dup2(sock, 0);

  while(fgets(inbuf, sizeof(inbuf), stdin) != NULL)
  {
    if ((p = strchr(inbuf, '\n')) != NULL)
      *p = '\0';			/* Strip end of line char	*/
    p = inbuf;
    deblank(p);

    if (strncasecmp(p, "sleep", strlen("sleep")) == 0)
    {
      read(suspendFd, &c, 1);
//      sleep(1);
      write(sock, doneMsg, strlen(doneMsg));
    }
    else if (strncasecmp(p, "rootcmd", strlen("rootcmd")) == 0)
    {
      p += strlen("rootcmd");
      deblank(p);
      snprintf(cmdbuf, sizeof(cmdbuf), "%s/%s >tmp.out\n", ROOTSRV_DIR, p);
      system(cmdbuf);

      if ((fd = open("tmp.out", O_RDONLY)) > 0)
      {
	while(read(fd, &c, 1) > 0)
	  write(sock, &c, 1);
	close(fd);
      }
      write(sock, doneMsg, strlen(doneMsg));
    }
    else if (strncasecmp(p, "bye", strlen("bye")) == 0)
    {
      close(sock);
      return;
    }
    bufChars = 0;
    bzero(inbuf, sizeof(inbuf));
  }

  close(sock);				/* recv() failed, close and exit */

} /* rootSrv() */


/************************************************************************/
/* Function    : rs_error						*/
/* Purpose     : Print an error string and exit				*/
/* Inputs      : Error string						*/
/* Outputs     : None (never returns)					*/
/************************************************************************/
void rs_error(char *errStr)
{
  perror(errStr);
  exit(-1);
}


/************************************************************************/
/* Function    : main							*/
/* Purpose     : main entry point for root server			*/
/* Inputs      : argc, argv						*/
/* Outputs     : Error return number					*/
/************************************************************************/
int main (int argc, char **argv)
{
  int		c, listenSock, connectSock, suspendFd, on;
  socklen_t 		addrlen;
  struct sockaddr_in	addr;		/* Socket address structure	*/
  struct linger		lingeropt;

  bzero((char *)&addr, sizeof(addr));
  addr.sin_family = PF_INET;
  addr.sin_addr.s_addr = LISTEN_IP;
  addr.sin_port = htons(ROOTSRV_PORT);

  while ((c = getopt(argc, argv, "p:")) != -1)
    switch(c)
    {
      case 'p':
	addr.sin_port = htons(atoi(optarg));
	break;

      default:
	usage();
	return(1);
    }

  if ((listenSock = socket(PF_INET, SOCK_STREAM, 0)) < 0)
    rs_error("rootSrv error getting listen socket:");

#ifdef DEBUG
  printf("rootSrv got listen socket %d\n", listenSock);
#endif
  on = 0;			/* Turn off Keepalives			*/
  setsockopt(listenSock, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
  on = 1;			/* Set RESUSEADDR			*/
  setsockopt(listenSock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));
  fcntl(listenSock, F_SETFD, 1L);

  /* Bind TCP socket address*/
  if ( bind(listenSock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
  {
    close(listenSock);
    rs_error("rootSrv error binding socket");
  }

#ifdef DEBUG
  printf("rootSrv bound listen socket to port %d\n", addr.sin_port);
#endif

  if ((suspendFd = open(SUSPEND_FILE,  O_RDONLY)) < 0)
    rs_error("rootSrv error opening suspend file");

  fcntl(suspendFd, F_SETFD, 1L);

  if ( listen(listenSock, 1) < 0 )
    rs_error("rootSrv error on listen");

#ifndef DEBUG
  daemon(0,0);
#endif

  while(1)	/* Main loop.  Listen forever to get connections	*/
  {
#ifdef DEBUG
    printf("rootSrv trying accept()\n");
#endif
    bzero( (char *)&addr, sizeof(addr) );
    addrlen = sizeof(addr);
    connectSock = accept(listenSock, (struct sockaddr *)&addr, &addrlen);
#ifdef DEBUG  
    printf("rootSrv return from accept, socket = %d\n", connectSock);
#endif

    if (connectSock > 0)	/* Successful accept, got a connected socket*/
    {
      on = 0;			/* Turn off Keepalives			*/
      setsockopt(connectSock, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
      on = 1;			/* Set RESUSEADDR			*/
      setsockopt(connectSock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));
      lingeropt.l_onoff = 1;
      lingeropt.l_linger = 0;
      setsockopt(connectSock, SOL_SOCKET, SO_LINGER, 
		 &lingeropt, sizeof(lingeropt) );
      fcntl(connectSock, F_SETFD, 1L);

      rootSrv(connectSock, suspendFd);
				/* Do root service as long as client is there*/    }
    else
      rs_error("rootSrv failed accept()");
  }

  return(0);

} /* main() */

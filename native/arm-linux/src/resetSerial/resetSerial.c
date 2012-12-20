/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#include <ctype.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <termios.h>
#include <unistd.h>
#include <sys/ioctl.h>

/* baudrate settings are defined in <asm/termbits.h>, which is
   included by <termios.h> */
#define BAUDRATE B38400            

/* change this definition for the correct port */
#define MODEMDEVICE "/dev/ttyS1"
#define _POSIX_SOURCE 1 /* POSIX compliant source */

#define FALSE 0
#define TRUE 1

volatile int STOP=FALSE; 

int main(int argc, char **argv)
{
  int fd;
  struct termios oldtio,newtio;
  int baud=38400;
  char PORT[255];
  speed_t SPEED=BAUDRATE;
  
  int x;
  sprintf(PORT,"%s",MODEMDEVICE);
  
  if (argc<2){
    fprintf(stderr,"\nUsage: resetSerial <-p port> <-b baudRate>\n");
    exit(2);
  }

  while ((x = getopt (argc, argv, "p:b:")) != -1)
    switch (x) {
    case 'b':
      sscanf(optarg,"%d",&baud);
      switch(baud){
      case 50:
	SPEED=B50;
	break;
      case 75:
	SPEED=B75;
	break;
      case 110:
	SPEED=B110;
	break;
      case 134:
	SPEED=B134;
	break;
      case 150:
	SPEED=B150;
	break;
      case 200:
	SPEED=B200;
	break;
      case 300:
	SPEED=B300;
	break;
      case 600:
	SPEED=B600;
	break;
      case 1200:
	SPEED=B1200;
	break;
      case 1800:
	SPEED=B1800;
	break;
      case 2400:
	SPEED=B2400;
	break;
      case 4800:
	SPEED=B4800;
	break;
      case 9600:
	SPEED=B9600;
	break;
      case 19200:
	SPEED=B19200;
	break;
      case 38400:
	SPEED=B38400;
	break;
      default:
	fprintf(stderr,"unsupported baud rate %d\n",baud);
	break;
      }
      break;
    case 'p':
      sprintf(PORT,"%s",optarg);
      break;
    case '?':
      if (isprint (optopt))
	fprintf (stderr, "Unknown option `-%c'.\n", optopt);
      else
	fprintf (stderr,
		 "Unknown option character `\\x%x'.\n",
		 optopt);
      return 1;
    default:
      abort ();
    }
  
  fprintf(stdout,"speed=%d port=%s\n",SPEED,PORT);

  /* 
     Open modem device for reading and writing and not as controlling tty
     because we don't want to get killed if linenoise sends CTRL-C.
  */
  fd = open(PORT, O_RDWR | O_NONBLOCK ); 

  if (fd <0) {
    perror(PORT); 
    exit(-1); 
  }
  
  tcgetattr(fd,&oldtio); /* save current serial port settings */
  bzero(&newtio, sizeof(newtio)); /* clear struct for new port settings */
        
  /* 
     BAUDRATE: Set bps rate. You could also use cfsetispeed and cfsetospeed.
     CRTSCTS : output hardware flow control (only used if the cable has
     all necessary lines. See sect. 7 of Serial-HOWTO)
     CS8     : 8n1 (8bit,no parity,1 stopbit)
     CLOCAL  : local connection, no modem contol
     CREAD   : enable receiving characters
     newtio.c_cflag = SPEED | CRTSCTS | CS8 | CLOCAL | CREAD;
  */
  newtio.c_cflag = (oldtio.c_cflag | SPEED | CREAD | CLOCAL | CREAD) & ~HUPCL ;
  newtio.c_lflag = (oldtio.c_lflag & ~ECHO);
        
  /* 
     now clean the modem line and activate the settings for the port
  */
  tcflush(fd, TCIFLUSH);
  tcsetattr(fd,TCSANOW,&newtio);
  ioctl(fd,TIOCMBIC,(TIOCM_DTR|TIOCM_DSR));
  close(fd);
  exit(0);

}


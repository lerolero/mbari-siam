/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/* Sample program to read CPU chip temperature and ambient board temperature */
/* Modifed from Lippert Cool RoadRunner-LX800 Technical Manual		*/
/* 29mar2008, rah							*/

#include <stdio.h>
#include <unistd.h>
#include <sys/io.h>

typedef unsigned char uint8;

#define LPC_INDEX 0x295
#define LPC_DATA  0x296

int main(void)
{
  int i;
  uint8 cputemp = 0;
  uint8 ambtemp = 0;

  ioperm(LPC_INDEX, 2, 1);

  outb(0x51, LPC_INDEX);		//thermal diode mode
  outb(0x03, LPC_DATA);
  outb(0x5c, LPC_INDEX);		//unlock offset regs
  outb(0x80, LPC_DATA);
  outb(0x56, LPC_INDEX);		//offset adjustment CPU
  outb(0x70, LPC_DATA);
  outb(0x57, LPC_INDEX);		//offset adjustment ambient
  outb(0x3c, LPC_DATA);
  outb(0x5c, LPC_INDEX);		//lock offset regs
  outb(0x00, LPC_DATA);

  printf("Press Ctrl-C to cancel!\n");
  printf("Sample CPU AMBIENT\n");

  for (i=0; 1; i++)
  {
    outb(0x29, LPC_INDEX);		//read out CPU temp
    cputemp = inb(LPC_DATA);

    outb(0x2a, LPC_INDEX);		//read out CPU temp
    ambtemp = inb(LPC_DATA);
    printf("%6d %3d  %3d\r", i, cputemp, ambtemp);
    fflush(stdout);
    sleep(1);
  }

  return(0);
}

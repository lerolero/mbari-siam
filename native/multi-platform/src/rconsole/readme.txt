The rconsole utility for SIAM can be built for multiple platforms.  It has 
currently been built and tested for x86-Linux, x86-Win32 using Cygwin, and 
arm-Linux.  To build an rconsole target for a specific platform just set CC in
the Makefile to the correct compiler for that platform.  For example

CC = gcc            #for x86-Linux and Cygwin
CC = arm-linux-gcc  #for arm-Linux

Currently seamonkey.shore.mbari.org is setup with the StrongARM cross 
compiler.  Just add 

/usr/local/arm/2.95.3/bin

to your PATH.

07/31/2003
Mike Risi

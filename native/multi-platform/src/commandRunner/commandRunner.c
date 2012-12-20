
/************************************************************************/
/* Copyright 2005 MBARI							*/
/************************************************************************/
/* Summary  : Application to run (priviledged) shell scripts from Java	*/
/* Filename : commandRunner.c						*/
/* Author   : Kent Headley (klh)					*/
/* Project  : SIAM							*/
/* Revision : 1.0							*/
/* Created  : 06/17/2005						*/
/*									*/
/* MBARI provides this documentation and code "as is", with no warranty,*/
/* express or implied, of its quality or consistency. It is provided	*/
/* without support and without obligation on the part of the Monterey	*/
/* Bay Aquarium Research Institute to assist in its use, correction,	*/
/* modification, or enhancement. This information should not be		*/
/* published or distributed to third parties without specific written	*/
/* permission from MBARI.						*/
/************************************************************************/
/************************************************************************/ 

/************************************************************************/  
/*
    commandRunner- Execute set of commands from command line

   Java Runtime.exec() does not work with shell scripts, only with 
   native executables. commandRunner can be exec'd to successfully
   run (priviledged) shell scripts from within java as a kind of lazy 
   man's JNI.

   usage notes:
   - Compile using 'gcc -ansi -pedantic -W -Wall -O2'
   - Requires that cygwin1.dll in cygwin be in the directory with 
     commandRunner under Win32/Cygwin.
   - Set the environment variable ACTIONCOMMAND=<commandToRun [args...]>
   - The default PATH environment variable may not be the same as
     the one used by the calling process; set the PATH environment
     variable before calling commandRunner.
*/
/************************************************************************/  


/**************/
/* Imports    */
/**************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/**************/
/* Local Data */
/**************/
  const char *revision="$Name: HEAD $ $Id: commandRunner.c,v 1.2 2008/12/03 19:54:51 headley Exp $";

/************************/
/* Function Definitions */
/************************/
void printUsage();

void printUsage(){
  fprintf(stderr,"\n\ncommandRunner version %s\n",revision);
  fprintf(stderr,"Usage: commandRunner [-v][-h]\n\n");
  fprintf(stderr,"  -v: verbose output\n");
  fprintf(stderr,"  -h: print help message\n\n");
  fprintf(stderr,"  commandRunner gets its arguments via environment variables.\n\n");
  fprintf(stderr,"  Set the following environment variables before invoking commandRunner:\n");
  fprintf(stderr,"  ACTIONCOMMAND: command to be executed\n");
  fprintf(stderr,"           PATH: path to used (does not inherit environment of calling process)\n");
  fprintf(stderr,"\n\n");
  fflush(stderr);
}

/*********************************************************/
/* Function : main                                       */
/* Purpose  : main entry point for commandRunner program */
/* Inputs   : argc, argv                                 */
/* Outputs  : Error return code (0 on success)           */
/*********************************************************/
  int main(int argc, char *argv[]){

  int err=-1;
  char *ACTIONCOMMAND=getenv("ACTIONCOMMAND");

  int verbose=0;
  int i=0;

  if(argc>1){
    for(i=1;i<argc;i++){
      if(strcmp(argv[i],"-v")==0){
	verbose=1;
      }
      if(strcmp(argv[i],"-h")==0){
	printUsage();
	return err;
      }
    }
  }

  /* Command must be specified */
  if(ACTIONCOMMAND<=0){
    fprintf(stderr,"Error: no action command specified\n");
    fflush(stderr);
    printUsage();
    return err;
  }

  /*
    Get the path from the environment
    Note that default path inheirited may be 
    different on different platforms, so we
    set it explicitly.
  */
  if(verbose!=0){
    fprintf(stderr,"revision:%s\n",revision);
    fflush(stderr);
    fprintf(stderr,"PATH:%s\n",getenv("PATH"));
    fflush(stderr);
    fprintf(stderr,"ACTIONCOMMAND:%s\n",ACTIONCOMMAND);
    fflush(stderr);
  }
  /* Run the command */
  err=system(ACTIONCOMMAND);
  
  if(verbose!=0){
    fprintf(stderr,"returning: %d\n",err);
    fflush(stderr);
  }

  return err;

}

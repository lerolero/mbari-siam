/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define __USE_XOPEN  // glibc2 needs this
#include <time.h>

#define true 1
#define false 0

typedef int boolean;

/*
Convert timestring to ephemeris time (seconds since epoch).
Input format is exemplified by "9/10/2002T01:07:33".
*/

int main(int argc, char **argv) {

  boolean error = false;

  // Default timestring format
  char *format = "%m/%d/%YT%H:%M:%S";

  // Timestring is last argument
  for (int i = 1; i < argc-1; i++) {
    if (!strcmp(argv[i], "-format") && i < argc-2) {
      format = argv[++i];
    }
    else {
      error = true;
    }
  }

  if (argc < 2) error = true;

  if (error) {
    fprintf(stderr, 
	    "usage: %s [-format \"time format\"] \"timestring\"\n", argv[0]);
    fprintf(stderr, 
	    "default format is %%m/%%d/%%YT%%H:%%M:%%S\n");
    fprintf(stderr, 
	    "(see UNIX man page for strptime() for format definitions)\n");
    return 1;
  }

  // Timestring is always last argument
  char *timeString = argv[argc-1];

  struct tm timeStruct;

  // Initialize time structure
  memset((void *)&timeStruct, 0, sizeof(struct tm));

  // Parse the timestring into time structure
  char *remainder = strptime(timeString, format, &timeStruct);

  // Workaround for bug in strptime(); let mktime() figure out if
  // DST is in effect...
  timeStruct.tm_isdst = -1;

  // Convert to seconds since epoch
  time_t lt = mktime(&timeStruct);
  long utoffset = mktime(gmtime(&lt)) - lt;

  // Check for daylight savings time
  if (timeStruct.tm_isdst) {
    // printf("Correcting for DST...\n");
    utoffset -= 3600;
  }

  printf("%d\n", lt);


  return 0;
}

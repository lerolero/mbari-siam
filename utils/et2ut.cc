/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
/*
Convert ephemeris time (seconds since epoch) to UTC timestring.
*/

int main(int argc, char **argv) {
  if (argc != 2) {
    fprintf(stderr, "usage: %s seconds\n", argv[0]);
    return 1;
  }

  time_t secs = atoi(argv[1]);

  //  printf("%s\n", ctime(&secs));
  struct tm timeStruct;
  gmtime_r(&secs, &timeStruct);
//  printf("isdst=%d\n", timeStruct.tm_isdst);
  char timeString[255];
  strftime(timeString, sizeof(timeString), "%d %b %Y, %H:%M:%S %Z", &timeStruct);
  printf("%s\n", timeString);
  return 0;
}

#include <stdio.h>
#include <string.h>

main()
{
  FILE *fin;
  int ch;

  fin=fopen("/proc/sys/pm/suspend","r");

  if(!fin)
    {
       printf("error opening file for reading\n");
       return -1;
    }
  
  ch =getc(fin);

  fclose(fin);
  return 0;
}


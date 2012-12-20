/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <net/if.h>
#include <sys/socket.h>
#include <sys/ioctl.h>

int main(int argc, char **argv)
{
	int i, sock_desc;
    int radix = 0;
	struct ifreq ifreq_buff;

    /* process cli switches */
    if ( argc > 1)
    {
        if ( !strcmp(argv[1], "-d") )
            radix = 10;
        
        if ( !strcmp(argv[1], "-h") )
        {
            printf("usage: ethaddr [-d]\n");
            printf(" -d shows address in base 10 format\n");
            return 0;
        }
    }
    
    sock_desc = socket(PF_INET, SOCK_DGRAM, 0);

    /* if you can't get a socket return a bogus address */
    if ( sock_desc < 0 ) 
    {
        printf("00:00:00:00:00:00\n");
        return 0;
    }

	memset(&ifreq_buff, 0, sizeof(ifreq_buff));
	sprintf(ifreq_buff.ifr_name, "eth0");

    /* if the ioctl fails return a bogus address */
	if ( ioctl(sock_desc, SIOCGIFHWADDR, &ifreq_buff) < 0 )
    {
        close(sock_desc);
        printf("00:00:00:00:00:00\n");
        return 0;
    }
    
    close(sock_desc);
	
	for(i = 0; i < 6; i++ )
	{
		if  ( radix == 10 )
            printf("%d", (unsigned char)ifreq_buff.ifr_hwaddr.sa_data[i]);
        else
            printf("%.2X", (unsigned char)ifreq_buff.ifr_hwaddr.sa_data[i]);
        if ( i < 5) 
            printf(":");
	}

	printf("\n");

	return 0;
}


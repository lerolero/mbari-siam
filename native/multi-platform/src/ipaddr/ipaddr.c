/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#include <stdio.h> 
#include <string.h> 
#include <sys/ioctl.h> 
#include <sys/socket.h> 
#include <sys/types.h> 
#include <net/if.h> 
#include <netdb.h> 
#include <netinet/in.h> 
#include <arpa/inet.h> 

extern int close(int);

/* Get the IP address associated with interface eth0

*/
int main(int argc, char **argv) 
{ 
    struct ifreq ifr; 
    struct sockaddr_in *sin = (struct sockaddr_in *)&ifr.ifr_addr; 
    int sockfd; 

    /* zero out ifr struct */
    bzero(&ifr, sizeof(ifr));

    /* set interface name */ 
    strcpy(ifr.ifr_name, "eth0"); 

    /* set sin_family (in some versions of linux kernel, 
       sin_family==0 would cause the ioctl call to return
       part of the MAC address on the first call.)
    */
    sin->sin_family = AF_INET; 

    /* create a socket */
    if((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) 
    { 
        perror("socket()"); 
        return(-1); 
    } 

    /* get the IP address for interface eth0 */
    if(ioctl(sockfd, SIOCGIFADDR, &ifr) == 0) 
    { 
      /*printf("%s:[%s]\n", ifr.ifr_name, inet_ntoa(sin->sin_addr));*/ 
        printf("%s\n", inet_ntoa(sin->sin_addr)); 
    } 

    /* close the socket */
    close(sockfd);

    return(0); 



} 



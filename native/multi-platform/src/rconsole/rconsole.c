/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/

#include <stdio.h>
#include <termios.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>

#include <unistd.h>
#include <fcntl.h>
#include <signal.h>

#include <sys/time.h>
#include <sys/types.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>


#define VERSION             "1.1"
#define READ_BUFFER_SIZE    32
#define KEYBOARD            0

#ifdef __CYGWIN__
void cfmakeraw(struct termios *termios_p) 
{
    termios_p->c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL|IXON);
    termios_p->c_oflag &= ~OPOST;
    termios_p->c_lflag &= ~(ECHO|ECHONL|ICANON|ISIG|IEXTEN);
    termios_p->c_cflag &= ~(CSIZE|PARENB);
    termios_p->c_cflag |= CS8;
}
#endif

static struct termios old_kb_state;
static int reset_kb_state = 0;

/* variables for terminal setup state */
static int local_echo = 0;
static int append_nl_out = 0;
static int append_nl_in = 0;


static void killhandler(int sig_num)
{
    if ( reset_kb_state )
        tcsetattr(KEYBOARD, TCSANOW, &old_kb_state);
}

void set_keyboard_raw()
{
    struct termios newstate;
    
    /* save the old keyboard settings */
    tcgetattr(KEYBOARD, &old_kb_state);
    /* get the keyboard settings */
    tcgetattr(KEYBOARD, &newstate);
    /* make the keyboard settings raw */
    cfmakeraw(&newstate);
    /* set them  */
    tcsetattr(KEYBOARD, TCSANOW, &newstate);
    
    signal(SIGTERM, killhandler);
    reset_kb_state = 1;
    return;
}

#define BLOCKING 0
#define NON_BLOCKING 1
static int mode = BLOCKING;

int ttypoll() 
{
    int count;
    char c;

    /* if the mode is BLOCKING switch it to NON_BLOCKING */
    if ( mode == BLOCKING)
    {
        int flag;

        flag = fcntl(KEYBOARD, F_GETFL, 0);
        fcntl(KEYBOARD, F_SETFL, flag | O_NDELAY);
        mode = NON_BLOCKING;
    }
    
    /* get the key */
    count = read(KEYBOARD, &c, 1);
    
    if (count <= 0 ) 
        return -1;
    
    return (int)c;
}

int ttygetc() 
{
    int count;
    unsigned char c;
    int flag;

    /* if the mode is BLOCKING switch it to NON_BLOCKING */
    if ( mode == NON_BLOCKING )
    {
        flag = fcntl(KEYBOARD, F_GETFL, 0);
        fcntl(KEYBOARD, F_SETFL, flag & ~O_NDELAY);
        mode = BLOCKING;
    }
    
    do
    {
        count = read(KEYBOARD, &c, 1);
    } while (count<=0 || c==0xFF);

    putchar(c&0x7F); 
    fflush(stdout);

    return(int) (c&0x7F);
}

void ttygets(char *buf, int len) 
{
    int i = 0;

    do
    {
        buf[i] = ttygetc();

        if ( buf[i] == 0x0D || buf[i] == 0x0A )
        {
            buf[i]='\0';
            break;
        }
        
        i++;
        buf[i] = '\0';

    } while (i<len);
}

int wait_for_char(char c, int timeout)
{
    double timeout_dbl;
    double total_time = 0;
    struct timeval tp;

    gettimeofday(&tp, NULL);
    timeout_dbl = (double)tp.tv_sec + (1.e-6) * tp.tv_usec;
    timeout_dbl += ((double)timeout) / 1000.0;

    while ( c != ttypoll() )
    {
        gettimeofday(&tp, NULL);
        if ( ((double)tp.tv_sec + (1.e-6) * tp.tv_usec) > timeout_dbl )
            return 0;
    }

    /* return the total time if it's greater than a millisecond */
    total_time = timeout_dbl - ((double)tp.tv_sec + (1.e-6) * tp.tv_usec);
    if ( total_time > 1.0 )
        return (int)(total_time * 1000.0);

    return 1;
}

void show_setup()
{
    
    /* display current rconsole setup state */
    printf("ECHO CHARACTERS LOCALLY     : ");
    if ( local_echo )
        printf("ON\r\n");
    else
        printf("OFF\r\n");

    printf("APPEND <NL> TO <CR> OUTGOING: "); 
    if ( append_nl_out )
        printf("ON\r\n");
    else
        printf("OFF\r\n");
    
    printf("APPEND <NL> TO <CR> INCOMING: "); 
    if ( append_nl_in )
        printf("ON\r\n");
    else
        printf("OFF\r\n");
    
    fflush(stdout);

}


void show_options()
{
    /* show rconsole options settings */
    printf("E - toggle echo mode\r\n");
    printf("O - toggle <NL> outgoing mode\r\n");
    printf("I - toggle <NL> incoming mode\r\n");
    printf("X - to exit options setup menu\r\n"); 
    fflush(stdout);
}

/* allow user to set rconsole echo and new line options
  return 0 if they opt out return 1 if they enter the menu*/
int set_options()
{
    int response;
    
    printf("\nHit 'a' to enter options menu or SPACE to continue: "); 
    fflush(stdout);
                
    response = ttygetc();
    printf("\n\r"); 
    fflush(stdout);

    if ((response != 'a') && (response != 'A'))
        return 0;

    printf("\n\r"); 
    
    /* show the current console setup */
    show_setup();

    /* give toggle options */
    printf("\n\r"); 
    show_options();
    printf("------------------------------\r\n"); 
    printf("enter choice: "); 
    fflush(stdout);

    response = ttygetc();
    printf("\n\r"); 
    fflush(stdout);

    while ((response != 'x') && (response != 'X'))
    {
        switch ( response ) 
        {
            case 'e':
            case 'E':
                local_echo = ! local_echo;
                show_setup();
                break;
            case 'i':
            case 'I':
                append_nl_in = ! append_nl_in;
                show_setup();
                break;
            case 'o':
            case 'O':
                append_nl_out = ! append_nl_out;
                show_setup();
                break;
            default: 
                printf("I don't understand choice '%c', ", (char)response);
                printf("valid choices are.\r\n");
                show_options();
                printf("------------------------------\r\n"); 
        }
        
        printf("enter choice: "); 
        fflush(stdout);
        response = ttygetc();
        printf("\n\r"); 
        fflush(stdout);
    }
    
    return 1;
}

void check_char(int* c)
{
    int response;
    
    switch (*c) 
    {
        case 0x01: /* CTRL-A */
            if ( set_options() )
                *c = -1;
            break;
        
        case 0x18: /* CTRL-X */
            printf("\nHit X to exit or SPACE to continue: "); 
            fflush(stdout);
                
            response = ttygetc();
            printf("\n\r"); 
            fflush(stdout);

            if (response=='x' || response=='X')
            {
                killhandler(1);
                exit(0);
            }
            break;

        default: /* do nothing */
    }
}

int main(int argc, char *argv[]) 
{
    int i;
    int ret;
    struct timeval tv;

    /* vars for chars */
    char cc;
    int c;
    char read_buffer[READ_BUFFER_SIZE];

    /* vars for the socket */
    char address_name[128] ="";
    int port_number = 0;
    int socket_desc;
    fd_set socket_set;
    struct sockaddr_in serv_addr;
    struct hostent *h;

    
    printf("rconsole version " VERSION ", built on " \
           __DATE__  " at " __TIME__ "\n");
    
    /* only accept two args server and port number */
    if ( argc != 2 )
    {
        printf("usage: rconsole <address:port>\n");
        exit(0);
    }
    
    /* get the socket address name and port number */
    strcpy(address_name, strtok(argv[1], ":")); 
    port_number = atoi(strtok(NULL, ":"));

/*    printf("got serial server address : %s\n", address_name); */
/*    printf("got serial server port    : %d\n", port_number); */
    
    /* get the hostname of the serial port server */
    h = gethostbyname(address_name);
    
    if( h == NULL ) 
    {
        printf("error: host '%s' unknown\n", address_name);
        exit(1);
    }

    serv_addr.sin_family = h->h_addrtype;
    memcpy((char *)&serv_addr.sin_addr.s_addr, h->h_addr_list[0], h->h_length);
    serv_addr.sin_port = htons(port_number);

    /* create socket */
    socket_desc = socket(AF_INET, SOCK_STREAM, 0);
    
    if ( socket_desc < 0 ) 
    {
        perror("error: cannot open socket\n");
        exit(1);
    }

    /* connect to server */
    ret = connect(socket_desc, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
    
    if ( ret < 0 ) 
    {
        perror("error: cannot connect\n");
        exit(1);
    }

    /* switch keyboard to raw state */
    set_keyboard_raw();

    tv.tv_usec = 0;
    for (;;)
    {
        /* check the keyboard for a keypress */
        c = ttypoll();

        /* if it's a special char do a special thing */
        check_char(&c);    

        /* if you get a char process it */
        if ( c >= 0 )        
        {
            cc = (char)c;

            /* write the chars */
            if (write(socket_desc, &cc, 1) < 0)
            {
                printf("error: failed to write to socket\n");
                close(socket_desc);
                killhandler(1);
                exit(1);
            }
            
            /* echo chars locally if necessary */
            if ( local_echo )
            {
                putchar(cc);
                if ( append_nl_out )
                    if ( cc == '\r' ) 
                        putchar('\n');
            }

            fflush(stdout);

            /* send the <NL> char if rconsole is setup to do so */
            if ( append_nl_out )
            {
                if ( cc == '\r' ) 
                {
                    cc = '\n';

                    if (write(socket_desc, &cc, 1) < 0)
                    {
                        printf("error: failed to write to socket\n");
                        close(socket_desc);
                        killhandler(1);
                        exit(1);
                    }
                }
            }
        }
        
        for(;;)
        {
            FD_ZERO(&socket_set);
            FD_SET(socket_desc, &socket_set);

            tv.tv_sec = 0;
            /* check for waiting chars */
            ret = select(socket_desc + 1, &socket_set, NULL, NULL, &tv); 
            /* 0 if no char avail */
            if ( ret )
            {
                ret = read(socket_desc, read_buffer, READ_BUFFER_SIZE);
                if ( ret <= 0 )
                {  
                    printf("error: error reading from socket\n");
                    killhandler(1);
                    exit(1);
                }
                
                /* if you got'em show'em */
                for (i = 0; i < ret; i++) 
                {
                    putchar(read_buffer[i]);
                    if ( append_nl_in ) 
                        if ( read_buffer[i] == '\r' )
                            putchar('\n');
                }
                
                fflush(stdout);
                break;
            }
            else
            {
                break; /* no more chars get out of here */
            }
        }
    }
}

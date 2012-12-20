/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/* 
    console.c
    
    Revision: 3/20/2003  Mike Risi -- added user timed breaks
    Revision: 3/19/2003  Mike Risi -- added break capibility   
    Initial Release: 3/18/2003  Mike Risi -- pulled out minicom specific code
   
    This simple serial console program is based on picom by 
    'Brent Roman of MBARI'
    which was based on microcom by 
    '2002 by Gregor Air, Inc., Milpitas, California'
*/

#include <sgtty.h>      /* TIOCSBRK                 */ 
#include <time.h>       /* for struct timeval       */
#include <sys/time.h>   /* for struct timeval       */
#include <stdio.h>      /* printf, open, etc.       */
#include <termios.h>    /* cfsetispeed, cfsetospeed */
#include <stdlib.h>     /* exit                     */
#include <asm/ioctls.h> /* TCSETS                   */
#include <unistd.h>     /* STDIN_FILENO             */
#include <fcntl.h>      /* O_RDWR                   */
#include <signal.h>     /* signal                   */

#define VERSION     "1.0"
#define FIFO_LIMIT  32 /* size other side's input FIFO */
#define KEYBOARD    0

#define MIN_BREAK_TIME 1
#define MAX_BREAK_TIME 10000


static struct termios oldstate;
static int iOldstateSet = 0;

static void killhandler(int arg)
{
    if (iOldstateSet)
        ioctl(KEYBOARD, TCSETS, &oldstate);
    putchar('\r');
    exit(0);
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

int main(int argc, char *argv[]) 
{
    /* return code var */
    int ret;
    int break_time = -1;

    /* timing vars */
    struct timeval tv;
    time_t time_now;

    /* vars for serial port manip */
    struct termios tattr;
    fd_set set;

    /* vars for chars */
    int c;
    int c2;
    char cc;
    char tmp_buff[256] = "";

    unsigned short queued4output = 0;
    unsigned short user_stty;

    /* file and device names */
    char* ser_port_name;
    char* in_log_name;
    char* out_log_name;

    /* file pointers and device handles */
    int ser_port_dev;
    FILE *in_log_ptr = NULL;
    FILE *out_log_ptr = NULL;

    printf("console version " VERSION " , built on " \
           __DATE__  " at " __TIME__ "\n");

    if ( argc <= 1 )
    {
        usage:
        printf("Usage: console [-u] SERIALPORT [user out log] [session log]\n"
               "Optional -u switch leaves most stdin & serial stty attributes unchanged\n"
               "Note:  console does not change SERIALPORT's baud rate\n\n"
               "Example: console /dev/ttyS0 logfile.txt user_out.txt\n");
        exit(0);
    }

    user_stty = strcmp (argv[1], "-u") ? 0 : 1;

    ser_port_name = argv[user_stty + 1];
    if (!ser_port_name)
        goto usage;

    ser_port_dev = open(ser_port_name, O_RDWR);
    if (ser_port_dev == -1)
    {
        printf("Port \"%s\" could not be opened - aborting.\n", ser_port_name);
        exit(4);
    }

    /* switch keyboard to "RAW" */
    ioctl(KEYBOARD, TCGETS, &oldstate);
    {
        struct termios newstate;

        ioctl(KEYBOARD, TCGETS, &newstate);
        newstate.c_lflag &= ~(ISIG|ICANON);

        if (!user_stty)
            cfmakeraw(&newstate);

#ifdef VMIN
        newstate.c_cc[VMIN] = 0;
        newstate.c_cc[VTIME] = 1;
#endif
        ioctl(KEYBOARD, TCSETS, &newstate);
        /* this is a nasty little race condition here */
        signal(SIGTERM, killhandler);
        iOldstateSet = 1;
    }

    /* if requested open the user out log file with "w" for this port */
    out_log_name = argv[user_stty + 2];
    if ( out_log_name )
    {
        out_log_ptr = fopen(out_log_name, "w");
        if (!out_log_ptr)
        {
            fprintf(stderr, "USER OUT LOGFILE: "); 
            perror (out_log_name); 
            putc('\r', stderr);
        }
    }
    
    /* if requested open the log file with "a+w" for this port */
    in_log_name = argv[user_stty + 3];
    if ( in_log_name )
    {
        in_log_ptr = fopen(in_log_name, "a+w");
        if (!in_log_ptr)
        {
            fprintf(stderr, "LOGFILE: "); 
            perror (in_log_name); 
            putc('\r', stderr);
        } 
        else
        {

            time(&time_now);     
            /* get the time and lop <LF> */
            strcpy(tmp_buff, (char*)asctime(localtime(&time_now)));

            if (strlen(tmp_buff) > 0)
                tmp_buff[strlen(tmp_buff) - 1] = '\0';
            
            /* if the file opened sucessfully append the log with the date */
            fprintf(in_log_ptr, 
                    "\n<**** new log entry for port %s on %s ****>\n", 
                    ser_port_name, tmp_buff);
        }
    }
    
    tcgetattr(ser_port_dev, &tattr);

    if ( user_stty )
        cfmakeraw(&tattr);
    else  /*do just the bare minimum on port attributes*/
        tattr.c_lflag &= ~(ICANON|ISIG|IEXTEN);

    tcsetattr(ser_port_dev, 0, &tattr);

    tv.tv_usec = 0;
    for (;;)
    {
        /* check the keyboard for a keypress */
        c = ttypoll();

        /* if you get a char process it */
        if ( c >= 0 )
        {
            /* if it's a CTRL-B go through the break routine */
            if (c == 0x02)
            {
                printf("\n\rEnter break time (1 to 10,000 ms) or " \
                       "SPACE to continue: "); 
                fflush(stdout);
                
                c2 = ttygetc();

                /* set break_time to -1 so you know to log a ^B in 
                the out file if the user aborts */
                break_time = -1;
                
                /* if the user did'nt hit the SPACE bar give'm a break */
                if (c2 != 0x20)
                {
                    tmp_buff[0] = c2;
                    ttygets((tmp_buff+1), sizeof(tmp_buff)-1);

                    break_time = atoi(tmp_buff); 
                    
                    /* make sure the requested break time is in range */
                    if ( (break_time < MIN_BREAK_TIME) || 
                         (break_time > MAX_BREAK_TIME) )
                    {
                        printf("\nRequested break of %d ms is out of range.\n", 
                               break_time);
                        break_time = -1;
                    }
                    else
                    {
                        printf("\nBreaking for %d ms (CTRL-C to abort) ... ",
                               break_time);
                        fflush(stdout);
                        
                        /* start the break */
                        ioctl(ser_port_dev, TIOCSBRK, 0);

                        /* wait for a time out or a CTRL-C, which ever 
                        comes first */
                        ret = wait_for_char(0x3, atoi(tmp_buff));

                        /* end the break */
                        ioctl(ser_port_dev, TIOCCBRK, 0);

                        /* if the user hit CTRL-C, tell'm */
                        if ( ret )
                        {
                            printf("user halted break!\n");
                            break_time = ret;
                        }
                        else
                        {
                            printf("break finished.\n");
                        }
                    }
                    fflush(stdout);
                }
                putchar('\n'); 
                fflush(stdout);
            } /* CTRL-B */

            /* if it's a CTRL-G let the user go if they want to */
            if (c == 0x07)
            {
                printf("\nHit X to exit or SPACE to continue: "); 
                fflush(stdout);
                
                c2 = ttygetc();
                printf("\n\r"); fflush(stdout);

                if (c2=='x' || c2=='X')
                {
                    close(ser_port_dev);
                    fflush(in_log_ptr);
                    fflush(out_log_ptr);

                    killhandler(1);
                    exit(0);
                }
            } /* CTRL-G */
            
            /* log the character to the out_log if it's there */
            if ( out_log_ptr )
            {
                switch ( c )
                {
                    case '\r':
                        /* convert <CR> to <LF> */
                        fputc('\n', out_log_ptr);
                        break;
                    case 0x02: /* CTRL-B */ 
                        if ( break_time > 0 )
                        {
                            fprintf(out_log_ptr, "\n<BREAK %d ms>\n", 
                                    break_time);
                            /* don't send ^B out the serial port if you 
                            issued a break */
                            c = -1;
                        }
                        else
                        {
                            fputc(c, out_log_ptr);
                        }
                        break;
                    default:
                        fputc(c, out_log_ptr);
                }
            }
        }

        /* if you got a char write it to the serial port */
        if ( c >= 0 )
        {
            cc = (char)c;
            while (1)
            {
                int result = write(ser_port_dev, &cc, 1);

                if (result > 0)
                {
                    if (++queued4output >= FIFO_LIMIT)
                    {
                        queued4output = 0;
                        tcdrain (ser_port_dev);
                    }
                    break;
                }

                if (result < 0)
                {
                    perror(ser_port_name);
                    close(ser_port_dev);
                    killhandler(1);
                    exit(1);
                }
            }
        }
        
        /* grab chars from the serial port if they are available */
        for(;;)
        {
            FD_ZERO(&set);
            FD_SET(ser_port_dev, &set);

            /* We rely on linux select behavior, tv_usecs is assigned 
            time remaining. */ 
            tv.tv_sec = 0;  
            ret = select(ser_port_dev + 1, &set, NULL, NULL, &tv); 
            /* 0 if no char avail */
            if ( ret )
            {
                ret = read(ser_port_dev, &cc, 1);
                if ( ret <= 0 )
                {  /* read error or EOF */
                    if ( ret < 0 )
                        perror(argv[0]);

                    killhandler(1);
                    exit (2);
                }

                putchar(cc); 
                fflush(stdout);

                /* log the chars coming from the serial port */
                if (in_log_ptr)
                    fputc(cc, in_log_ptr);

            }
            else
            {
                break; /* no more chars get out of here */
            }
        }
    }
}

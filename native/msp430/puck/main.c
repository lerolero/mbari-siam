/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : main.c                                                        */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/14/2003                                                    */
/****************************************************************************/
#include  <msp430x14x.h>
#include  <stdio.h>
#include  <string.h>
#include "errors.h"
#include "defs.h"
#include "commands.h"
#include "parser.h"
#include "serial.h"
#include "flash_mem.h"
#include "spi.h"
#include "timer.h"

extern CmdStruct puckCmds[];

/****************************************************************************/
/* TODO                                                                     */
/****************************************************************************/
/*
    -need to add io.c to delay with I/O ports, see initModules

*/


/****************************************************************************/
/*                          global scratch vars                             */
/****************************************************************************/
char msgBuffer[80];
int echoMode = 0;

/****************************************************************************/
/*                          function prototypes                             */
/****************************************************************************/
int getCommand(char* cmd);
void initModules(void);

main(void)
{
    int ret_val;
#define MAX_CHARS   80
    char cmd[MAX_CHARS];
    char* cmd_ptr;

    /* initialize all modules */
    initModules();

    for(;;)
    {
        if ( !getCommand(cmd) )
        {
            ret_val = ERR_NO_LINE;
        }
        else
        {
            /* Skip white space here */
            cmd_ptr = skip(cmd, " \t");

            /* If the cmd from getCommand was empty then return here. */
            if (!strlen(cmd_ptr))
                ret_val = ERR_EMPTY_LINE;
            else
                ret_val = parse(cmd_ptr, puckCmds, ERR_NO_COMMAND);
        }

        /* display ERR code or RDY prompt */
        if ( ret_val != ERR_NO_LINE)
        {
            if ((ret_val != ERR_SUCCESS) && (ret_val != ERR_EMPTY_LINE))
            {
                sprintf(msgBuffer,"ERR %04d\r", ret_val);
                sendMsg(msgBuffer);
                if (echoMode)
                    sendChar('\n');

            }
            
            sendMsg("RDY\r");
            if (echoMode)
                sendChar('\n');
        }
    }
}


/****************************************************************************/
/*                  get command from serial input funtion                   */
/****************************************************************************/
int getCommand(char* cmd)
{
    static int char_count = 0;
    static char line[MAX_CHARS] = "";
    const char terminator = '\r';
    int got_line;
    char c;
    int i;
    
    got_line = FALSE;

    if (  getChar(&c) )
    {
        if ( echoMode )
        {
            if (c == '\r')
            {
                sendChar('\r');
                sendChar('\n');
            }
            else
            {
                sendChar(c);
            }
        }

        if ( c != terminator )
        {
            if ( (c != '\b') && (char_count < MAX_CHARS) )
                line[char_count++] = c;
            else if ( char_count > 0 )
                --char_count;

            cmd[0] = '\0';
        } 
        else
        {
            got_line = TRUE;
            line[char_count] = '\0';
            for (i = 0; i <= char_count; i++)
                cmd[i] = line[i];
            char_count = 0;
        }
    }

    return got_line;
}

void initModules(void)
{
    /* initialize the processor */
    int i;

    WDTCTL = WDTPW + WDTHOLD;             // Stop WDT

  //  ACLK = MCLK = LFXT1 = 7.3728 MHz
  // An external 7.3728 XTAL on XIN XOUT is required for ACLK
    BCSCTL1 |= XTS;                       // ACLK = LFXT1 = HF XTAL

    do 
    {
      IFG1 &= ~OFIFG;                       // Clear OSCFault flag
      for (i = 0xFF; i > 0; i--)
        ;// Time for flag to set
    }
    while ((IFG1 & OFIFG) == OFIFG);      // OSCFault flag still set?                

    BCSCTL2 |= SELM1+SELM0;               // MCLK = LFXT1 (safe)

    /* setup the IO */
    P1DIR |= 0x10;                        // Set P1.4 to output direction

    /* PORT 4 is all output */
    P4DIR |= 0xFF;                        // Set P4 to output

    /* Port 4 settings for different modes
    -----------------------------------------------------------------
    P4.0 DE485 RS-285 xmitter enable  [0 = off         | 1 = on     ]
    P4.1 RS-485 termination resistor  [ 0 = off        | 1 = on     ]
    P4.2 Puck/instrument mode relay   [ 0 = instrument | 1 = puck   ]
    P4.3 XCVR RS-232/RS-485 selection [ 0 = RS-232     | 1 = RS-485 ]
    P4.4 XCVR slew rate control       [ 0 = slow       | 1 = fast   ]
    P4.5 Relays for RS-485 and RS-232 [ 0 = RS-485     | 1 = RS-232 ]
    P4.6 XCVR sleep mode              [ 0 = shutdown   | 1 = active ]
    P4.7 UNUSED
    -----------------------------------------------------------------
    
    sensor mode                 : 0x18
    puck mode RS-232            : 0x74
    puck mode RS-485 no term.   : 0x5C (idle) | 0x5D (xmitting)
    puck mode RS-485 term.      : 0x5E (idle) | 0x5F (xmitting) 
    */
         
    /* setup in puck mode */
    P4OUT = 0x74;
    /* init the serial module */
    initSerial();
    /* init the spi module */
    initSpi();
    /* init the DataFlash module */
    initFlash();
    /* init Timer module */
    initTimer();
    
    _EINT();                              // Enable interrupts
}


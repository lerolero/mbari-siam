/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : actions.c                                                     */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/14/2003                                                    */
/****************************************************************************/

#include <msp430x14x.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "errors.h"
#include "version.h"
#include "flash_mem.h"
#include "spi.h"
#include "timer.h"
#include "serial.h"
#include "parser.h"
#include "defs.h"

extern int echoMode;
char tempBuff[64];

void adjustAddressPtr(void)
{
    if (getAddressPtr() >= getMemSize())
      setAddressPtr(getAddressPtr() % getMemSize());
}


#define MAX_READ_BYTES 1024
#define MIN_READ_BYTES 0

int funcReadMemCmd(char* s)
{
    char data[32];
    int i;
    int j;

    int bytes = (int)strtol(s, NULL, 10);

    if ( (bytes > MAX_READ_BYTES) || (bytes < MIN_READ_BYTES) )
        return ERR_BYTES_OUT_OF_RANGE;
    
    sendMsg("[");

    i = 0;
    while ( i < bytes )
    {
        /* read max 32 bytes at a time */
        if ((bytes - i) > 32)
        {
            readFlash(data, 32);
            
            /* send out the data */
            for (j = 0; j < 32; j++)
                sendChar(data[j]);
            
            /* increment i by the number of bytes read */
            i += 32;
        }
        else
        {
            if ((bytes - i) > 0)
            {
                readFlash(data, (bytes - i));
                
                /* send out the data */
                for (j = 0; j < (bytes - i); j++)
                    sendChar(data[j]);
                
                /* increment i by the number of bytes read */
                i += (bytes - i);
            }
        }
    
    }
    
    sendMsg("]");
    adjustAddressPtr();
    return ERR_SUCCESS;
}


#define MAX_WRITE_BYTES 32
#define MIN_WRITE_BYTES 0

int funcWriteMemCmd(char* s)
{
  int i, rtn;
    unsigned char c;
    unsigned char data[32];

    /* capture the next [0-32] bytes */
    int bytes = (int)strtol(s, NULL, 10);

    if ( (bytes > MAX_WRITE_BYTES) || (bytes < MIN_WRITE_BYTES) )
        return ERR_BYTES_OUT_OF_RANGE;

    if (bytes + getAddressPtr() > getMemSize())
        return ERR_ADDRESS_RANGE;
    
    for (i = 0; i < bytes; i++)
    {
        
        while ( !getChar(&c) )
            ; /* wait for the char to come in */

        data[i] = c;
    }
    
    rtn = writeFlash(data, bytes);
    adjustAddressPtr();
    return rtn;
}

int funcGetAddressCmd(char* s)
{
    sprintf(tempBuff,"%ld\r\n", getAddressPtr());
    sendMsg(tempBuff);
    
    return ERR_SUCCESS;
}

int funcSetAddressCmd(char* s)
{
    unsigned long newAddressPtr =  strtol(s, NULL, 10);

    if (newAddressPtr >= getMemSize())
        return ERR_ADDRESS_RANGE;

    return setAddressPtr(strtol(s, NULL, 10));
}

int funcFlushMemCmd(char* s)
{
    return closeFlash();
}

int funcSensorModeCmd(char* s)
{
    /*need seperate relays.c module to deal with this
    should be accessing P4OUT directly in actions*/
    P4OUT  = 0x00;

    /* make sure the flash chip is in a low power state */
    sleepFlash();

    /* put the micro into the lowest power mode CPU OFF */
    LPM4;
    
    return ERR_SUCCESS;
}

int funcClearMemCmd(char* s)
{
    return eraseFlash();
}

int funcMemSizeCmd(char* s)
{
    sprintf(tempBuff,"%lu\r\n", getMemSize());
    sendMsg(tempBuff);

    return ERR_SUCCESS;
}

int funcGetBaudCmd(char* s)
{
    sprintf(tempBuff,"BAUDRATE %lu\r\n", getBaud());
    sendMsg(tempBuff);

    return ERR_SUCCESS;
}

int funcGetTypeCmd(char* s)
{
    /*
        0001    Read only datasheet memory
        0002    PUCK hardware is external to the instrument
    */    
    
    sendMsg("0002\r\n");
    return ERR_SUCCESS;
}

int funcSetBaudCmd(char* s)
{
    unsigned long baud = strtol(s, NULL, 10);
    return setBaud(baud);
}

int funcIsBaudValidCmd(char* s)
{
    if ( isBaudValid(strtol(s, NULL, 10)) )
        sendMsg("YES\r\n");
    else
        sendMsg("NO\r\n");

    return ERR_SUCCESS;
}

int funcVersionCmd(char* s)
{
    sendMsg(getVersion());
    sendMsg("\r\n");
    
    return ERR_SUCCESS;
}

int funcFlashStatusCmd(char* s)
{
    unsigned char status = 0;

    getFlashStatus(&status);

    sprintf(tempBuff,"FLASH STATUS = 0x%02X\r\n", status);
    sendMsg(tempBuff);

    return ERR_SUCCESS;
}

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

#define RS485_TERMINATION   0x02
#define RS485_XMITTER       0x01
/* RS-485 helper functions */
int isRs485TermOn(void)
{
    if ( P4OUT & RS485_TERMINATION )
        return TRUE;
    else
        return FALSE;
}

void enableRs485Term(void)
{
    /* enable the RS-485 termination resistor */
    P4OUT |= RS485_TERMINATION;
}

void disableRs485Term(void)
{
    /* disable the RS-485 termination resistor */
    P4OUT &= ~RS485_TERMINATION;
}

void enableRs485Transmit(void)
{
    /* enable the RS-485 xmitter */
    P4OUT |= RS485_XMITTER;

}

void disableRs485Transmit(void)
{
    /* disable the RS-485 xmitter */
    P4OUT &= ~RS485_XMITTER;
}

int funcRs485ConsoleCmd(char* s)
{
    int quitting_time = FALSE;
    char cmd;
    
    /* issue RS485 mode warning */
    sendMsg("************** entering RS-485 console mode **************\r\n");
    sendMsg("h - say hello\r\n");
    sendMsg("t - toggle termination resistor\r\n");
    sendMsg("q - query termination resistor state\r\n");
    sendMsg("x - exit RS-485 test\r\n");
    sendMsg("************** entering RS-485 console mode **************\r\n");
    delayTimerTicks(5 * TICKS_PER_SEC);

    /* setup RS485 mode */
    if ( name_match(s, "TERM") )
        P4OUT = 0x5E;
    else
        P4OUT = 0x5C;
    
    /* enter RS-485 loop */
    while ( !quitting_time )
    {
        /* get simple cmd */
        while ( !getChar(&cmd) )
            ;/* wait for char cmd from host */

        /* wait a couple of ticks msec */
delayTimerTicks(2 * TICKS_PER_SEC);
        //delayTimerTicks(2);
        
        /* enable xcvr */
        enableRs485Transmit();

        /* send response */
        switch ( cmd )
        {
            case 'h':   sendMsg("Hi!\r\n"); break;
            case 't':   if ( isRs485TermOn() )
                            disableRs485Term();
                        else
                            enableRs485Term();
                        break;
            case 'q':   if ( isRs485TermOn() )
                            sendMsg("TERM ENABLED\r\n");
                        else
                            sendMsg("TERM DISABLED\r\n");
                        break;
            case 'x': quitting_time = TRUE; break;
            default:
                sprintf(tempBuff,"ERR - '%c' command unknown\r\n", cmd);
                sendMsg(tempBuff);
        }
        
        /* disable xcver */
        disableRs485Transmit();

        /* wait for last char to get sent.  won't need this when
        charsInTxBuffer() is iplemented */
        delayTimerTicks(2);
        
        /* eat looped back chars */
        while ( getChar(&cmd) /* ||  charsInTxBuffer() */ )
            ;/* wait for char cmd from host */
    }

    /* issue RS485 mode warning */
    sendMsg("************** exiting RS-485 console mode **************\r\n");
    delayTimerTicks(5 * TICKS_PER_SEC);

    /* put back into puck mode */
    P4OUT = 0x74;
    
    return ERR_SUCCESS;
}

int funcToggleEchoCmd(char* s)
{
    echoMode = !echoMode;
    return ERR_SUCCESS;

}

int funcShowHelpCmd(char* s)
{
    sendMsg("required puck protocol commands\r\n");
    sendMsg("-------------------------------\r\n");
    sendMsg("PUCKRM -- read memory\r\n");
    sendMsg("PUCKWM -- write memory\r\n");
    sendMsg("PUCKFM -- flush write buffer\r\n");
    sendMsg("PUCKEM -- erase memory\r\n");
    sendMsg("PUCKGA -- get address\r\n");
    sendMsg("PUCKIM -- sensor mode\r\n");
    sendMsg("PUCKSA -- set address\r\n");
    sendMsg("PUCKSZ -- memory size\r\n");
    sendMsg("PUCKSB -- set baud\r\n");
    sendMsg("PUCKTY -- get type flags\r\n");
    sendMsg("PUCKVB -- verify baud\r\n");
    sendMsg("PUCKVR -- get version\r\n");
    sendMsg("\r\n");
    sendMsg("additional user commands\r\n");
    sendMsg("-------------------------------\r\n");
    sendMsg("ECHO -- toggle echo mode\r\n");
    sendMsg("HELP -- display this help\r\n");
    return ERR_SUCCESS;
}

int funcHelloKittyCmd(char* s)
{
    sendMsg("funcHelloKittyCmd(");
    sendMsg(s);
    sendMsg(")\r\n");

    return ERR_SUCCESS;
}

#if 0

int debugReadMemCmd(char* s)
{
    char data[32];
    int i;
    int j;
    unsigned long temp_addr = getAddressPtr();

    int bytes = (int)strtol(s, NULL, 10);

    sendMsg("[");

    i = 0;
    while ( i < bytes )
    {
        /* read max 32 bytes at a time */
        if ((bytes - i) > 32)
        {
sendMsg("32 BYTES\r\n");
            readFlash(data, 32);
            /* send out the data */
            for (j = 0; j < 32; j++)
            {
                //sendChar(data[j]);
sprintf(tempBuff, "%04ld '0x%02X' '%c'\r\n", temp_addr++, (int)data[j], data[j]);
sendMsg(tempBuff);
            }
            /* increment i by the number of bytes read */
            i += 32;
        }
        else
        {
            if ((bytes - i) > 0)
            {
sprintf(tempBuff, "%d BYTES\r\n", (bytes - i));
sendMsg(tempBuff);
                readFlash(data, (bytes - i));
                /* send out the data */
                for (j = 0; j < (bytes - i); j++)
                {
                    //sendChar(data[j]);
sprintf(tempBuff, "%04ld '0x%02X' '%c'\r\n", temp_addr++, (int)data[j], data[j]);
sendMsg(tempBuff);
                }
                /* increment i by the number of bytes read */
                i += (bytes - i);
            }
        }
    
    }
    
    sendMsg("]");
    return ERR_SUCCESS;
}

int funcTimerTest(char* s)
{
    int i = 0;
    TimerStruct timer;

    createTimer(&timer);
    startTimer(&timer);

    while (i < 10)
    {
        delayTimerTicks(TICKS_PER_SEC);
        ++i;
        sprintf(tempBuff,"TIMER VAL %ld\r\n", readTimer(&timer));
        sendMsg(tempBuff);
    }


    return ERR_SUCCESS;
}

#endif

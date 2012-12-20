/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : serial.c                                                      */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/27/2003                                                    */
/****************************************************************************/
#include  <msp430x14x.h>

#include <stdio.h>
#include <stdlib.h>

#include "serial.h"
#include "buffers.h"
#include "errors.h"
#include "defs.h"

#include "timer.h"

/* The receive and transmit buffers must be a power of 2 in size for the    */
/* buffer wrap around to work correctly.                                    */
#define RX_BUFFER_SIZE  32
static BufferStruct rxByteBuffer;
static unsigned char rxBytes[RX_BUFFER_SIZE];

#define TX_BUFFER_SIZE  32
static BufferStruct txByteBuffer;
static unsigned char txBytes[TX_BUFFER_SIZE];

#define B_9600      9600
#define B_19200     19200
#define B_38400     38400
#define B_57600     57600
#define B_115200    115200

void initSerial(void)
{
    /* setup the UART hardware */
    UCTL1 = CHAR;                         // 8-bit character
    UTCTL1 = SSEL0;                       // UCLK = ACLK
                                          
                                          // 7.3728Mhz/19200 = 0x180  
    UBR01 = 0x00;                         // 7.3728Mhz/9600 = 0x300
    UBR11 = 0x03;                         //
//    UBR01 = 0x40;   //115200 baud
//    UBR11 = 0x00;
    
    UMCTL1 = 0x00;                        // no modulation
    ME2 |= UTXE1 + URXE1;                 // Enable USART1 TXD/RXD
    IE2 |= URXIE1;                        // Enable USART1 RX interrupt
    
    P3SEL |= 0xC0;                        // P3.6,7 = USART1 TXD/RXD
    P3DIR |= 0x40;                        // P3.6 output direction

    /* initialize the serial buffers */
    rxByteBuffer.data = rxBytes;
    rxByteBuffer.head = 0;
    rxByteBuffer.tail = 0;
    rxByteBuffer.ptr_mask = RX_BUFFER_SIZE - 1;

    txByteBuffer.data = txBytes;
    txByteBuffer.head = 0;
    txByteBuffer.tail = 0;
    txByteBuffer.ptr_mask = TX_BUFFER_SIZE - 1;
}

int getChar(char* c)
{
    return getByte(c, &rxByteBuffer);
}

void sendChar(char c)
{
    while ((IFG2 & UTXIFG1) == 0)
        ;// USART1 TX buffer ready?

    TXBUF1 = c;
}

/****************************************************************************/
/*                      send message out serial port                        */
/****************************************************************************/
void sendMsg(char* msg)
{
    int i = 0;    
    
    while ((*msg != '\0') && (i < 100))
    {
        sendChar(*(msg++));
        i++;
    }
}

int isBaudValid(unsigned long baud)
{
    switch ( baud )
    {
        case B_9600:    break;
        case B_19200:   break;
        case B_38400:   break;
        case B_57600:   break;
        case B_115200:  break;
        default: return FALSE;
    }
        
    return TRUE;
}

unsigned long getBaud(void)
{
    unsigned long baud_rate = UBR11;

    baud_rate = baud_rate << 8;
    baud_rate |= UBR01;
    return (unsigned long)(SYSTEM_CLOCK / baud_rate);
}

int setBaud(unsigned long baud)
{
    unsigned int reg_val;
    
    if ( !isBaudValid(baud) )
        return ERR_SERIAL_INVALID_BAUD;
    
    reg_val = (unsigned int)(SYSTEM_CLOCK / baud);
    UBR11 = 0x00FF & (reg_val >> 8);
    UBR01 = 0x00FF & reg_val;
    
    return ERR_SUCCESS;
}

interrupt[UART1RX_VECTOR] void usart1_rx(void)
{
    putByte((unsigned char)RXBUF1, &rxByteBuffer);
}

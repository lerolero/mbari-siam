/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : spi.c                                                         */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/28/2003                                                    */
/****************************************************************************/
#include <msp430x14x.h>
#include "defs.h"
#include "buffers.h"
#include "spi.h"

void initSpi(void)
{
    int i;
 
    ME1 |= USPIE0;                      // ME1.6 (USPIE0=URXE0) enable tx/rx in SPI mode
    UTCTL0 = CKPL+SSEL1+SSEL0+STC;            // (reads 0x86) (SPI Mode 3???)
    UCTL0 = CHAR+SYNC+MM+SWRST;				// 8bit SPI Master **SWRST**
//    UCTL0 = CHAR+SYNC+MM+LISTEN;				// LOOPBACK TEST: 8bit SPI Master **SWRST** LOOPBACK

    UBR00 = 0x02;						// UCLK/2 (note: UBR00 = 0x02 is as fast as you can go)
    UBR10 = 0x00;						// 0
    UMCTL0 = 0x00;						// no modulation
    
    P3SEL |= 0x0E;						// P3.1,2,3 SPI option select P3.1=SIMO, P3.2=SOMI, P3.3=UCLK0
    P3DIR |= 0x0B;						// P3.0(unused)=1, P3.1(SIMO)=1, P3.2(SOMI)=0, P3.3(UCLK)=1

    for(i = 0; i < 8; i++)
        ; /* delay */
    
    UCTL0 &= 0xfe;                      // reset bit.0=SWRST

    /* disable the interrupts */
    IE1 &= ~URXIE0;
}

unsigned char spiReadByte(void)
{
    /* clear any pending interrupts */
    IFG1 &= ~URXIFG0;
    
    /* write the dummy byte */
    spiWriteByte(0xFF);

    while ((IFG1 & URXIFG0) == 0)
        ;// wait for the byte to come into USART0 RX buffer
    
    return (unsigned char)RXBUF0;
}

void spiWriteByte(unsigned char c)
{
    /* make sure the write buffer is ready */
    while ((IFG1 & UTXIFG0) == 0)
        ;// USART0 TX buffer ready?
    
    /* write the byte */
    TXBUF0 = c;
    
    /* wait for the transmission to complete before returning */
    while ( (UTCTL0 & TXEPT) == 0 )
        ;/* wait for xmission to complete */
}

/* old interrupt stuff */
#if 0

/* initialize the serial buffers */
spiRxByteBuffer.data = spiRxBytes;
spiRxByteBuffer.head = 0;
spiRxByteBuffer.tail = 0;
spiRxByteBuffer.ptr_mask = RX_BUFFER_SIZE - 1;

void spiIntEnable(void)
{
    /* clear any pending interrupts */
    IFG1 &= ~URXIFG0;
    /* enable the interrupts and wait for the bit storm */
    IE1 |= URXIE0;
}

void spiIntDisable(void)
{
    /* disable the interrupts */
    IE1 &= ~URXIE0;
}

interrupt[UART0RX_VECTOR] void usart0_rx (void)
{
    putByte((unsigned char)RXBUF0, &spiRxByteBuffer);
}

#endif

/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : flash_mem.c                                                   */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/28/2003                                                    */
/****************************************************************************/
#include <msp430x14x.h>
#include <stdio.h>
#include <stdlib.h>

#include "timer.h"
#include "serial.h"
#include "spi.h"
#include "defs.h"
#include "errors.h"

TimerStruct flashTimer;

static unsigned long flashMemPtr = 0;
static int writeMode = FALSE;

int isFlashBusy(void);
void strobeFlashCS(void);

#define MEM_ARCH_SHIFT_RIGHT_0  7
#define MEM_ARCH_SHIFT_LEFT_1   1
#define MEM_ARCH_SHIFT_RIGHT_1  8

#define BYTES_PER_BLOCK         2112

#define FLASH_128K 0

#if FLASH_128K
#define ADDRESS_RESERVED_BITS   0xFC

#define MEM_ARCH_PAGE_SZ        0x01FF

#define BLK_ERASE_TOTAL_BLOCKS  64
#define BLK_ERASE_SHIFT_RIGHT_0 5
#define BLK_ERASE_SHIFT_LEFT_1  3
#define BLK_ERASE_MASK_BITS_1   0x07
#else
#define ADDRESS_RESERVED_BITS   0xE0

#define MEM_ARCH_PAGE_SZ        0x0FFF

#define BLK_ERASE_TOTAL_BLOCKS  512
#define BLK_ERASE_SHIFT_RIGHT_0 4
#define BLK_ERASE_SHIFT_LEFT_1  4
#define BLK_ERASE_MASK_BITS_1   0x0F
#endif


int memArchToBytes(unsigned int page_addr, 
                   unsigned int byte_addr, 
                   unsigned char* addr_bytes);

int addressToMemArch(unsigned long addr, 
                     unsigned int* page_addr, 
                     unsigned int* byte_addr);

int addressToBytes(unsigned long addr_long, unsigned char* addr_bytes);


void initFlash(void)
{
    int i;
    
    /* setup port5 all outputs except bit 0*/
    P5SEL = 0x00; /*port function for entire port */
    P5DIR = 0xFE; /* all outputs except bit 0 */

    /*
        P5.0 : RDY/#BUSY input
        P5.1 : #WP
        P5.2 : #CS
        P5.3 : #RESET
    */
    
    /* setup flash part with default state */
    P5OUT = 0x0E;

    /* reset flash part */
    P5OUT &= 0xF7;
    
    /* assert rest for a bit */
    for (i = 0; i < 50; i++)
        ;/* wait here */

    /* clear reset flash part */
    P5OUT |= 0x08;
}

unsigned long getMemSize(void)
{
    unsigned long mem_size = BYTES_PER_BLOCK;
    return (mem_size * BLK_ERASE_TOTAL_BLOCKS);
}


int getFlashStatus(unsigned char* status)
{
    /* strobe the CS for this command */
    strobeFlashCS();
    
    /* send the cmd */
#define FLASH_STATUS    0x57
    spiWriteByte(FLASH_STATUS);
    
    *status = spiReadByte();
    
    return ERR_SUCCESS;
}

int readFlash(unsigned char* data, int bytes)
{
    int i = 0;
    unsigned char address[3];

    strobeFlashCS();
#define CONTINUOUS_FLASH_READ   0x68
    /* send continuous read command */
    spiWriteByte(CONTINUOUS_FLASH_READ);
    
    /* write 24 address bits bits */
    addressToBytes(flashMemPtr, address);
    spiWriteByte(address[0]);
    spiWriteByte(address[1]);
    spiWriteByte(address[2]);
    
    /* write 32 don't care bits */
    spiWriteByte(0xFF);
    spiWriteByte(0xFF);
    spiWriteByte(0xFF);
    spiWriteByte(0xFF);

    /* read bytes from memory */
    for (i = 0; i < bytes; i++)
    {
        data[i] = spiReadByte();
        flashMemPtr++;

    }

    return TRUE;
}

#define PAGE_SIZE       264
#define BUFFER_WRITE    0x84
int writeFlash(unsigned char* data, int bytes)
{
    int i = 0;
    
    unsigned int page_addr;
    unsigned int byte_addr;
    unsigned char address[3];
    
    addressToMemArch(flashMemPtr, &page_addr, &byte_addr);
    
    /* strobe the flash to prepare for the buffer write */
    strobeFlashCS();
    
    /* send buffer write command */
    spiWriteByte(BUFFER_WRITE);

    /* 8 don't care bits */
    spiWriteByte(0xFF);

    /* 7 don't care bits and first address bit */
    address[0] = 0x01 & (byte_addr >> 8);
    spiWriteByte(address[0]);

    /* lower 8 bits address bit */
    address[0] = 0x00FF & byte_addr;
    spiWriteByte(address[0]);
    
    /* wrap around case */
    if ( (byte_addr + bytes) >= PAGE_SIZE )
    {
        /* write bytes into the ram buffer */
        while ( (byte_addr + i) <  PAGE_SIZE )
        {
            spiWriteByte(data[i++]);
            flashMemPtr++;
        }

        /* program the flash with the full buffer */
        /* strobe the flash to prepare for the write */
        strobeFlashCS();
#define BUFFER_TO_MAIN_MEMORY   0x88
        spiWriteByte(BUFFER_TO_MAIN_MEMORY);

        memArchToBytes(page_addr, 0x1FF, address);

        spiWriteByte(address[0]);
        spiWriteByte(address[1]);
        spiWriteByte(address[2]);

        /* strobe the flash to write */
        strobeFlashCS();

        while ( isFlashBusy() )
            ;/* wait for the write to complete */

        /* write the last bytes to the ram buffer if any */ 
        /* strobe the flash to prepare for the write */
        strobeFlashCS();

        /* send buffer write command */
        spiWriteByte(BUFFER_WRITE);

        /* don't care bits */
        spiWriteByte(0xFF);
        /* first address bit */
        spiWriteByte(0xFE);
        /* lower 8 bits address bit */
        spiWriteByte(0x00);
        
        while ( i < bytes )
        {
            spiWriteByte(data[i++]);
            flashMemPtr++;
        }
    
    }
    else /* else normal case */
    {
        while ( i < bytes )
        {
            spiWriteByte(data[i++]);
            flashMemPtr++;
        }
    }

    /* strobe the flash to end the write */
    strobeFlashCS();
    return ERR_SUCCESS;
}

int closeFlash(void)
{
    int i = 0;
    
    unsigned int page_addr;
    unsigned int byte_addr;
    unsigned char address[3];

    
    addressToMemArch(flashMemPtr, &page_addr, &byte_addr);
    
    /* strobe the flash to prepare for the write */
    strobeFlashCS();
    
    /* send buffer write command */
    spiWriteByte(BUFFER_WRITE);

    /* write 8 don't care bits */
    spiWriteByte(0xFF);

    /* write 7 don't care bits & first address bit */
    address[0] = 0x01 & (byte_addr >> 8);
    spiWriteByte(address[0]);

    /* lower 8 bits address bit */
    address[0] = 0x00FF & byte_addr;
    spiWriteByte(address[0]);
    
    /* write remaining bytes in memory buffer to ones */
    for (i = 0; (i + byte_addr) <  PAGE_SIZE; i++)
    {
        spiWriteByte(0xFF);
        flashMemPtr++;
    }

    /* program the flash with the full buffer */
    /* strobe the flash to prepare for the write */
    strobeFlashCS();
#define BUFFER_TO_MAIN_MEMORY   0x88
    spiWriteByte(BUFFER_TO_MAIN_MEMORY);

    memArchToBytes(page_addr, 0x1FF, address);

    spiWriteByte(address[0]);
    spiWriteByte(address[1]);
    spiWriteByte(address[2]);
        
    /* strobe the flash to write */
    strobeFlashCS();

    while ( isFlashBusy() )
        ;/* wait for the write to complete */

    return ERR_SUCCESS;
}

void sleepFlash(void)
{
    /* set chip select hi (clear), this puts the flash in a low power state */
    P5OUT |= 0x04;
}


#define BLOCK_ERASE     0x50

int eraseFlash(void)
{
    unsigned int i;
    unsigned char block;

    for (i = 0; i < BLK_ERASE_TOTAL_BLOCKS; i++)
    {
        /* strobe the flash to prepare for the block erase */
        strobeFlashCS();
        /* send erase command to flash */
        spiWriteByte(BLOCK_ERASE);
        
        /* first block address byte */
        block = ADDRESS_RESERVED_BITS; 
        block |= (i & 0x01FF) >> BLK_ERASE_SHIFT_RIGHT_0;
        spiWriteByte(block);
        
        /* second block address byte */
        block = (i & 0x01FF) << BLK_ERASE_SHIFT_LEFT_1;
        block |= BLK_ERASE_MASK_BITS_1;
        spiWriteByte(block);
        
        /* don't care bits */
        spiWriteByte(0xFF);
        
        /* strobe flash to start erase */
        strobeFlashCS();

        /*wait for erase to complete and get the next block */
        while ( isFlashBusy() )
            ;/* wait for block to earse */
    }

    flashMemPtr = 0;

    return ERR_SUCCESS;
}


int setAddressPtr(unsigned long ptr)
{
    flashMemPtr = ptr;

    return ERR_SUCCESS;
}

unsigned long getAddressPtr(void)
{
    return flashMemPtr;
}

void strobeFlashCS(void)
{
    int i;
    
    /* set chip select hi (clear) */
    P5OUT |= 0x04;
    
    for(i = 0; i < 10; i++)
        ;//wait
    
    /* set chip select lo (assert)*/
    P5OUT &= 0xFB;

}


#define FLASH_RDY   0x80

int isFlashBusy(void)
{
    
#if FLASH_128K
    unsigned char status;
    
    getFlashStatus(&status);
    
    if ( FLASH_RDY & status )
        return 0; /* FALSE */
    else
        return 1; /* TRUE */
#else
/*    
    //poll busy bit for 1 MegaByte Flash
    if ( P5IN & 0x01 )
        return FALSE;
    else
        return TRUE;
*/

    unsigned char status;
    
    getFlashStatus(&status);
    
    if ( FLASH_RDY & status )
        return 0; /* FALSE */
    else
        return 1; /* TRUE */
#endif

}

int memArchToBytes(unsigned int page_addr, 
                   unsigned int byte_addr, 
                   unsigned char* addr_bytes)
{
    addr_bytes[0] = ADDRESS_RESERVED_BITS;
    addr_bytes[0] |= (page_addr & MEM_ARCH_PAGE_SZ) >> MEM_ARCH_SHIFT_RIGHT_0;
    
    addr_bytes[1] = (page_addr & MEM_ARCH_PAGE_SZ) << MEM_ARCH_SHIFT_LEFT_1;
    addr_bytes[1] |= (byte_addr & 0x01FF) >> MEM_ARCH_SHIFT_RIGHT_1;
    
    addr_bytes[2] = (byte_addr & 0x00FF);
    return TRUE;
}



#define PAGE_SIZE   264

int addressToMemArch(unsigned long address, 
                     unsigned int* page_addr, 
                     unsigned int* byte_addr)
{
    *page_addr = address / PAGE_SIZE;
    *byte_addr = address % PAGE_SIZE;
    return TRUE;
}

int addressToBytes(unsigned long addr_long, unsigned char* addr_bytes)
{
    unsigned int page_addr;
    unsigned int byte_addr;

    if ( !addressToMemArch(addr_long, &page_addr, &byte_addr) )
        return FALSE;

    if ( memArchToBytes(page_addr, byte_addr, addr_bytes) )
        return FALSE;

    return TRUE;
}


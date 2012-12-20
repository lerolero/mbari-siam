/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : buffers.c                                                     */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/27/2003                                                    */
/****************************************************************************/
#include "buffers.h"
#include "defs.h"

int putByte(unsigned char c, BufferStruct* buffer)
{
    if ( isBufferFull(buffer) )
    {
        return FALSE;
    }   
    else
    {   
        buffer->data[buffer->head] = c;
        buffer->head = (buffer->head + 1) & buffer->ptr_mask;
        return TRUE;
    }
}

int getByte(unsigned char* c, BufferStruct* buffer)
{
    if ( isBufferEmpty(buffer) )
    {
        *c = '\0'; // give back the null character
        return FALSE;
    }
    else
    {
        *c = buffer->data[buffer->tail];
        buffer->tail = (buffer->tail + 1) & buffer->ptr_mask;
        return TRUE;
    }
}

void clearBuffer(BufferStruct* buffer)
{
    buffer->tail = 0;
    buffer->head = 0;
}

int isBufferFull(BufferStruct* buffer)
{
    if ( ((buffer->head + 1) & buffer->ptr_mask) == buffer->tail )
        return TRUE;
    else
        return FALSE;
}

int isBufferEmpty(BufferStruct* buffer)
{
    if (buffer->tail == buffer->head) 
        return TRUE;
    else
        return FALSE;
}

int dataAvailable(BufferStruct* buffer)
{
    return (buffer->head - buffer->tail) & buffer->ptr_mask;
}


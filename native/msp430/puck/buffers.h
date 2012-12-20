/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : buffers.h                                                     */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/27/2003                                                    */
/****************************************************************************/
#ifndef BUFFERS_H
#define BUFFERS

typedef struct
{
    unsigned char* data;
    int head;
    int tail;
    int ptr_mask;
} BufferStruct;

/* buffer access and status functions */
int putByte(unsigned char c, BufferStruct* buffer);
int getByte(unsigned char* c, BufferStruct* buffer);

void clearBuffer(BufferStruct* buffer);
int isBufferFull(BufferStruct* buffer);
int isBufferEmpty(BufferStruct* buffer);
int dataAvailable(BufferStruct* buffer);

#endif

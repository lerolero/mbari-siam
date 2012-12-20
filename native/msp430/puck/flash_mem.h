/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : flash_mem.h                                                   */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/28/2003                                                    */
/****************************************************************************/
#ifndef FLASH_MEM_H
#define FLASH_MEM_H

void initFlash(void);

int getFlashStatus(unsigned char* data);
unsigned long getMemSize(void);

int readFlash(unsigned char* data, int bytes);
int writeFlash(unsigned char* data, int bytes);
int closeFlash(void);

void sleepFlash(void);

int eraseFlash(void);
void setWriteMode(void);
void clearWriteMode(void);
int getWriteMode(void);

int setAddressPtr(unsigned long ptr);
unsigned long getAddressPtr(void);

#endif


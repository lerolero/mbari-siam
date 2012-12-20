/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : serial.h                                                      */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/27/2003                                                    */
/****************************************************************************/
#ifndef SERIAL_H
#define SERIAL_H

void initSerial(void);

int getChar(char* c);
void sendChar(char c);
void sendMsg(char* msg);

unsigned long getBaud(void);
int setBaud(unsigned long baud);
int isBaudValid(unsigned long baud);

#endif


/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : spi.h                                                         */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 04/28/2003                                                    */
/****************************************************************************/
#ifndef SPI_H
#define SPI_H

void initSpi(void);

unsigned char spiReadByte(void);
void spiWriteByte(unsigned char c);

#endif


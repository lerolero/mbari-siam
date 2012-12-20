/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : version.c                                                     */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/19/2003                                                    */
/****************************************************************************/
#include <stdio.h>

/* version info */
const int majorRev = 0;
const int minorRev = 4;
const char stepRev[] = "";
const char tmp_buff[32];

/*****************************************************************************
 * getVersion:  Returns puck version string
 *  Arguments: none
 *  Returns: pointer to null terminated string with version information.
 * 
 *****************************************************************************/
char* getVersion(void)
{
    sprintf(tmp_buff, "MBARI PUCK REV %d.%d%s", majorRev, minorRev, stepRev);
    return tmp_buff;
}

/*****************************************************************************
 * getType:  Returns puck type
 *  Arguments: none
 *  Returns: int representing puck type       
 *                  0001    Read only datasheet memory
 *                  0002    PUCK hardware is external to the instrument
 *  
 *****************************************************************************/
int getType(void)
{
return 0x0002;
}

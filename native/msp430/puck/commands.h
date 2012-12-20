/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : commands.h                                                    */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/19/2003                                                    */
/****************************************************************************/
#ifndef COMMANDS_H
#define COMMANDS_H

typedef struct
{
    char* name;
    int (*func)(char *);
} CmdStruct;

//extern CmdStruct puckCmds[];

#endif


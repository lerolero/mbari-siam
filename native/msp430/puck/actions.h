/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : actions.h                                                     */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/14/2003                                                    */
/****************************************************************************/

#ifndef ACTIONS_H
#define ACTIONS_H


/* Puck Commands */
int funcReadMemCmd(char* s);
int funcWriteMemCmd(char* s);
int funcGetAddressCmd(char* s);
int funcSetAddressCmd(char* s);
int funcFlushMemCmd(char* s);
int funcSensorModeCmd(char* s);
int funcClearMemCmd(char* s);
int funcMemSizeCmd(char* s);
int funcGetBaudCmd(char* s);
int funcGetTypeCmd(char* s);
int funcSetBaudCmd(char* s);
int funcIsBaudValidCmd(char* s);
int funcVersionCmd(char* s);

/* Debug Commands */
int funcFlashStatusCmd(char* s);
int funcRs485ConsoleCmd(char* s);

int funcToggleEchoCmd(char* s);
int funcShowHelpCmd(char* s);
int funcHelloKittyCmd(char* s);

#endif

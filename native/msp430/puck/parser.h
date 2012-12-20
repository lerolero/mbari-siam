/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : parser.h                                                      */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/14/2003                                                    */
/****************************************************************************/

#ifndef PARSER_H
#define PARSER_H

#include "commands.h"

int name_match(char* s, char* ref);
char* skip(char* s, char* ignore);
int is_integer(char* txt);
int isvalid_name_char(char c);
int parse(char* line, CmdStruct* cmds, int err_code);

#endif

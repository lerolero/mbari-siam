/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : parser.c                                                      */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/14/2003                                                    */
/****************************************************************************/

#include <ctype.h>
#include <string.h>
#include "parser.h"
#include "defs.h"

/****************************************************************************/
/* Function    : name_match                                                 */
/* Purpose     : Compare the string s to the reference string ref.          */
/* Inputs      : pointer to string and refernece string                     */
/* Outputs     : Zero if no match, number of chars matched if match.        */
/****************************************************************************/
int name_match(char* s, char* ref)
{
    int n;
    n = 0;

    if (*s == '\0')
        return 0;

    do
    {
        /* check for match */
        if (toupper(ref[n]) != toupper(s[n]))
        {    /* search for another option in ref */
            do
            {
                ++ref;
                if (*ref == '\0')    /* end of ref = NO MATCH */
                    return 0;
            }
            while (*ref != '|');

            ++ref;
            n = 0; /* look at start of string */
        } 
        else
        {
            ++n;
            if (ref[n] == '\0' || ref[n] == '|')
            {
                /* make sure only whitespcae follows */
                if ( !isvalid_name_char(s[n]) )
                    return n;  
            }
        }
    }
    while ( n < 40 );    /* 40 chars max */

    return n;
}

/****************************************************************************/
/* Function    : skip                                                       */
/* Purpose     : Skip chars in string s that occur in string ignore.        */
/* Inputs      : pointer to string s and ignore string                      */
/* Outputs     : Pointer to first char in string s that does not occur in   */
/*               string ignore.                                             */
/****************************************************************************/
char* skip(char* s, char* ignore)
{
    int n;
    n = -1;

    while (ignore[++n] != '\0')
    {
        if (*s == ignore[n])
        {
            ++s;
            n=-1;
        }
    }

    return s;
}

/****************************************************************************/
/* Function    : is_integer                                                 */
/* Purpose     : Check whether a string is in integer format                */
/* Inputs      : pointer to string txt                                      */
/* Outputs     : TRUE or FALSE depending on the string                      */
/****************************************************************************/
int is_integer(char* txt)
{
    txt = skip(txt, " \t");
    if (*txt == '-' || *txt == '+')
        ++txt;
    txt = skip(txt, " ");
    txt = skip(txt, "0123456789");

    if ( strlen(txt) )
        return FALSE;
    else
        return TRUE;
}


/****************************************************************************/
/* Function    : isvalid_name_char                                          */
/* Purpose     : Determine if char c is a valid "name" char.                */
/* Inputs      : char to check.                                             */
/* Outputs     : Zero if not valid nonzero if valid                         */
/****************************************************************************/
int isvalid_name_char(char c)
{
    return(isalnum(c) || c == '_');
}

/****************************************************************************/
/* Function    : parse                                                      */
/* Purpose     : Check first "name" in line buffer against names in command */
/*               structure until there is a match or the end of the list is */
/*               reached.  If a match is found call the function associated */
/*               with the command structure name                            */
/* Inputs      : pointer to line to parse, pointer to command structure     */
/*               to use, and error code to return if no match               */
/* Outputs     : result of called function or error code                    */
/****************************************************************************/
int parse(char* line, CmdStruct* cmds, int err_code)
{
    int chars, i;
    i = 0;

    /* skip any leading white space */
    line = skip(line, " \t");

    /* compare name against names in CmdStruct */
    while (cmds[i].name[0] != '\0')
    {
        chars = name_match(line, cmds[i].name);
        
        if (chars)
        {
            line = skip(&(line[chars]), " \t");
            return cmds[i].func(line);
        }
        
        ++i;
    }

    return err_code;
}


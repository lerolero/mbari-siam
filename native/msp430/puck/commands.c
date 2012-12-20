/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : commands.c                                                    */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 02/14/2003                                                    */
/****************************************************************************/

#include <stddef.h>
#include "actions.h"
#include "commands.h"


/****************************************************************************/
/*                      parser command structure                            */
/****************************************************************************/

/* Note: commands listed near the begining of the structure incur less      */
/* parser overhead time than commands occuring later in the structure       */

CmdStruct puckCmds[] =
{
    {"PUCKRM",   funcReadMemCmd             },
    {"PUCKWM",   funcWriteMemCmd            },
    {"PUCKFM",   funcFlushMemCmd            },
    {"PUCKEM",   funcClearMemCmd            },
    {"PUCKGA",   funcGetAddressCmd          },
    {"PUCKIM",   funcSensorModeCmd          },
    {"PUCKSA",   funcSetAddressCmd          },
    {"PUCKSZ",   funcMemSizeCmd             },
    {"PUCKGB",   funcGetBaudCmd             },  /*deprecated */
    {"PUCKSB",   funcSetBaudCmd             },
    {"PUCKTY",   funcGetTypeCmd             },  /*deprecated */
    {"PUCKVB",   funcIsBaudValidCmd         },
    {"PUCKVR",   funcVersionCmd             },
    
    /* debug command(s) below */
/*
    {"FSTAT",   funcFlashStatusCmd      },
    {"RS485",   funcRs485ConsoleCmd     },
    
    {"EAT",   funcRs485EaterCmd         },
    {"PUKE",  funcRs485PukerCmd         },
    
    {"GET P4",  funcGetPort4            },
    {"SET P4",  funcSetPort4            },
*/
    {"ECHO",    funcToggleEchoCmd       },
    {"HELP",    funcShowHelpCmd         },
    {"HELLO KITTY", funcHelloKittyCmd   },
    {"",        NULL                    }
};

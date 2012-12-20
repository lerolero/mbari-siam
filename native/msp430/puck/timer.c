/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : timer.c                                                       */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 05/11/2003                                                    */
/****************************************************************************/

#include <msp430x14x.h>
#include "timer.h"
#include "defs.h"

static long timerVal = 0;

void initTimer(void)
{
    /* setup timer B */
    TBCTL = TBSSEL0 + TBCLR;              // ACLK, clear TAR
    TBCCTL0 = CCIE;                       // CCR0 interrupt enabled
    TBCTL |= MC1;                         // Start Timer_B in continuous mode
   /* setup timer B */
}

int isRunning(TimerStruct* timer)
{
    return timer->active;
}

/* Create the timer */
void createTimer(TimerStruct* timer)
{
    stopTimer(timer);
    clearTimer(timer);
}
    
/* Clear the timer */
void clearTimer(TimerStruct* timer)
{
    timer->total_time = 0;
}

/* Read the total amount of elapsed time the timer has running */
long readTimer(TimerStruct* timer)
{
    long current_time = timerVal;

    if ( timer->active )
        timer->total_time += (current_time - timer->ref_time);

    timer->ref_time = current_time;

    return timer->total_time;
}

/* Start the timer */
long startTimer(TimerStruct* timer)
{
    readTimer(timer);
    timer->active = TRUE;
    return timer->total_time;
}

/* Stop the timer */
long stopTimer(TimerStruct* timer)
{
    readTimer(timer);
    timer->active = FALSE;
    return timer->total_time;
}

void delayTimerTicks(int ticks)
{
    long wait_ticks = timerVal + (long)ticks;

    while ( wait_ticks > timerVal )
        ;/* wait for wait ticks to expire */
}

/* Timer B0 interrupt service routine */
interrupt[TIMERB0_VECTOR] void Timer_B (void)
{
    timerVal++;
}



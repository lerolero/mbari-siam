/****************************************************************************/
/* Copyright © 2003 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
/* Summary  : Sensor Puck Software                                          */
/* Filename : timer.h                                                       */
/* Author   : Mike Risi                                                     */
/* Project  : Puck development for SIAM                                     */
/* Created  : 05/11/2003                                                    */
/****************************************************************************/
#ifndef TIMER_H
#define TIMER_H

/* 
    The ticks per second is determined by 

    TIMER_B: 16 bit rollover
    ACLK   : 7.3728 MHz
    
    (7,372,800 Hz) / (2^16) = 112.5 Hz
    
    See initTimer() for details of TIMER_B setup.
*/

#define SYSTEM_CLOCK    7372800L
#define TICKS_PER_SEC   (SYSTEM_CLOCK/65536)

typedef struct
{
    int active;
    long ref_time;
    long total_time;

} TimerStruct;


/* intialize the timer */
void initTimer(void);
/* see if the timer is running */
int isTimerRunning(TimerStruct* timer);
/* Create the timer */
void createTimer(TimerStruct* timer);
/* Clear the timer */
void clearTimer(TimerStruct* timer);
/* Read the total amount of elapsed time the timer has running */
long readTimer(TimerStruct* timer);
/* Start the timer */
long startTimer(TimerStruct* timer);
/* Stop the timer */
long stopTimer(TimerStruct* timer);

/* delay for a specified number of timer ticks */
void delayTimerTicks(int ticks);

#endif


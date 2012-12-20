/************************************************************************/
/* Copyright 2004 MBARI							*/
/************************************************************************/
/* Summary  : Definitions for Ricoh RTC chip on Sidearm4		*/
/* Filename : ricohRTC.h						*/
/* Author   : Robert Herlien (rah)					*/
/* Project  : SIAM							*/
/* Revision : 1.0							*/
/* Created  : 02/03/2004						*/
/*									*/
/* MBARI provides this documentation and code "as is", with no warranty,*/
/* express or implied, of its quality or consistency. It is provided	*/
/* without support and without obligation on the part of the Monterey	*/
/* Bay Aquarium Research Institute to assist in its use, correction,	*/
/* modification, or enhancement. This information should not be		*/
/* published or distributed to third parties without specific written	*/
/* permission from MBARI.						*/
/************************************************************************/
/* Modification History:						*/
/* 03feb2004 rah - created						*/
/************************************************************************/

#ifndef INCricohRTCh
#define INCricohRTCh	1

/* LDDIO Bits used for RTC		*/
#define RTC_MISO	0x08
#define RTC_MOSI	0x10
#define RTC_CLK		0x20
#define RTC_CE		0x40
#define RTC_BITS	(RTC_MISO | RTC_MOSI | RTC_CLK | RTC_CE)
#define RTC_OUTBITS	(RTC_MISO | RTC_CLK | RTC_CE)

#define LDDIO_DEV	"/dev/lddio"
#define LDDIO_SETUP_STR "78<70O70-8I"
#define SELECT_STR	"40+"
#define DESELECT_STR	"40-"

typedef struct rtcTime
{
  Byte	second;
  Byte  minute;
  Byte  hour;
  Byte	dayOfWeek;
  Byte	dayOfMonth;
  Byte	monthAndCenturyBit;
  Byte	year;
} RtcTime;

#define RTC_TIME_SIZE	7	/* sizeof() returns 8		*/


/* RTC Register Addresses	*/
#define SECOND_REG		0
#define MINUTE_REG		1
#define HOUR_REG		2
#define DAYOFWEEK_REG		3
#define DAYOFMONTH_REG		4
#define MONTH_REG		5
#define YEAR_REG		6
#define OSC_ADJ_REG		7
#define ALARMW_MINUTE_REG	8
#define ALARMW_HOUR_REG		9
#define ALARMW_DAY_REG		10
#define ALARMD_MINUTE_REG	11
#define ALARMD_HOUR_REG		12
#define CTRL_REG1		14
#define CTRL_REG2		15

#define KHZ32_BIT		0x10
#define	AMPM_BIT		0x20
#define CENTURY_BIT		0x80
#define ENBL_ALARMD_BIT		0x40
#define ENBL_ALARMW_BIT		0x80

#define XSTP_BIT		0x10
	
#endif /* INCricohRTCh */


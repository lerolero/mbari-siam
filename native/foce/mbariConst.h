/************************************************************************/
/* Copyright 1991 MBARI							*/
/************************************************************************/
/* Summary  : Include file of defined constants				*/
/* Filename : const.h							*/
/* Author   : Andrew Pearce, Robert Herlien				*/
/* Project  : OASIS Mooring						*/
/* $Revision: 1.1 $							*/
/* Created  : 02/06/91							*/
/*									    */
/* MBARI provides this documentation and code "as is", with no warranty,    */
/* express or implied, of its quality or consistency. It is provided without*/
/* support and without obligation on the part of the Monterey Bay Aquarium  */
/* Research Institute to assist in its use, correction, modification, or    */
/* enhancement. This information should not be published or distributed to  */
/* third parties without specific written permission from MBARI.            */
/*									    */
/************************************************************************/
/* Modification History:						*/
/* 06feb91 rah - created from Andy's const.h (rah)			*/
/************************************************************************/

#ifndef INCconsth
#define INCconsth	1

#ifndef FALSE
#define FALSE	0
#endif

#ifndef TRUE
#define	TRUE	~FALSE
#endif

#define ON	TRUE
#define	OFF	FALSE

#define OK	 0
#define SUCCESS	 0
#define ERROR	-1
#define FOREVER	while(TRUE)

#define	min(a,b)	((a) < (b) ? (a) : (b))
#define max(a,b)	((a) > (b) ? (a) : (b))

#ifndef NAT16_MAX
#define NAT16_MAX	0xffff
#endif
#ifndef INT16_MAX
#define INT16_MAX	32767
#endif
#ifndef INT16_MIN
#define INT16_MIN	(-32767)
#endif
#ifndef INT32_MAX
#define INT32_MAX	(0x7fffffff)
#endif
#ifndef NAT32_MAX
#define NAT32_MAX	(0xffffffff)
#endif

#endif /* INCconsth */

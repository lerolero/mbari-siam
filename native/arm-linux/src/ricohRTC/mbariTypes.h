/************************************************************************/
/* Copyright 1988 MBARI							*/
/************************************************************************/
/* Summary  : General Type Definitions					*/
/* Filename : TYPES.H							*/
/* Author   : Daniel Davis, Gregg Morris				*/
/* Project  : 								*/
/* $Revision: 1.1 $							*/
/* Created  : 03/05/88							*/
/* Modified : 03/27/90							*/
/* Archived :								*/
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
/* 26feb90 rah - added modification history, deleted MSDOS stuff	*/
/* 07mar90 rah - added BitVec						*/
/************************************************************************/

#ifndef MBARI_TYPES

#define MBARI_TYPES	1

#define Extern		extern
#define MLocal		/* static */
#define Global		/**/

#ifndef NULL
#define NULL	((void *)0)
#endif

#ifndef Reg
#define Reg		register
#endif


/*
*	Basic types:
*/
#define Void		void	/* No value returned type		*/

typedef short int	MBool;	/* Bool, i.e. TRUE or FALSE		*/

typedef unsigned char	Char;	/* character, whatever that is - here	*/
				/* it is 8-bit ASCII			*/

typedef unsigned char	Byte;	/* Byte - i.e. 8 bit vector, cannot add */
				/* or subtract, multiply or divide, but */
				/* can perform bit vector operations,	*/
				/* such as OR, XOR, AND, NOT, EQUAL	*/

typedef unsigned short	Word;	/* word - i.e. 16 bit vector		*/

typedef unsigned long	DWord;	/* double word - i.e. 32 bit vector	*/

typedef signed char	Int8;	/* integer, 7 bits of precision with	*/
				/* a sign, for arithmetic operations	*/

typedef short int	Int16;	/* integer, 15 bits of precision with	*/
				/* a sign, for arithmetic operations	*/

typedef long int	Int32;	/* integer, 31 bits of precision with	*/
				/* a sign, for arithmetic operations	*/

typedef int		Int;	/* integer for scratch use, don't care	*/
				/* about size				*/

typedef unsigned short	Nat16;	/* natural number, 16 bits of precision */
				/* for indexing and positive offsets	*/

typedef unsigned long	Nat32;	/* natural number, 32 bits of precision */
				/* for indexing and positive offsets	*/

typedef float		Flt32;	/* floating point number, 32 total bits	*/

typedef double		Flt64;	/* floating point number, 64 total bits	*/

typedef unsigned int	BitVec;	/* For bit vector operations		*/

#endif

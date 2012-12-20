/*********************  linux/drivers/misc/sa1100lddio.c  *********************
 *
 *  Copyright (C) 2002 MBARI
 *
 *  MBARI Proprietary Information. All rights reserved.
 *
 *  11 Feb 2002 - brent@mbari.org:  first working version
 *  12 Feb 2002 - brent:  changed default separator from \r to \n
 *  13 Feb 2002 - brent:  fixed some typos in comments
 *  12 Mar 2002 - brent:  now works when NOT a module
 *   3 Apr 2002 - brent:  GPIO_MINOR was taken, use SA1100_GPIO_MINOR
 *   5 Sep 2002 - brent:  typos in comments
 *   9 Sep 2002 - brent:  display device major and minor numbers
 *  21 Apr 2002 - headley@mbari.org: Modified Brent Roman's sa1100gpio.c
 *                to form sa1100lddio.c
 *
 *  This is a generic LDDIO driver for the Intel StrongARM SA-1100 
 *
 *  It is intended to allow complete control of the 12 bits of the
 *  LCD Controller peripheral (when it is not being used as an LCD 
 *  controller ) from any language that can do basic file I/O.  
 *  For this reason, IOCTLs have been avoided -- only read & write 
 *  operations are required. Further, all I/O is done in 7-bit ASCII. 
 *
 *  The LDDIO pins are not capable of supporting interrupts; the interrupt
 *  handling features of sa1100gpio.c are not present in this driver.
 *  Otherwise, it works in exactly the same way as sa1100gpio.
 *
 *  lddio is a miscelleous device.  See include/linux/miscdevice.h for its
 *  reserved minor device number.
 *
 *  Only bits set in the "stencil" can be modified. 
 *  When any process opens this device, the stencil is set to STENCIL.
 *  This protects LDDIO bits used by PCMCIA and other kernel
 *  functions from being corrupted by the generic LDDIO control driver.
 *
 *  Commands output to this device are of the form:
 *
 *	{mask}<operator> 
 *
 *  where the optional {mask} is a hexidecimal number,
 *  and <operator> is a single (case insensitive) ASCII character:
 *
 *	+	PPSR |= mask & stencil        (set bits)
 *
 *	-	PPSR &= ~mask & stencil       (clear bits)
 *
 *	=	PPSR = (PPSR & ~stencil)|(mask & stencil) (copy mask to output)
 *
 *	<	stencil &= mask			(further restrict stencil)
 *	\	(next byte will be used as separator for all subsequent reads)
 *
 *	I	PPDR &= ~(mask & stencil)	(configure bits as input)
 *	O	PPDR |= mask & stencil		(configure bits as output)
 *      :	PPDR = mask & stencil		(configure I/O bits)
 *	
 *  Normally, reading this lddio device returns a stream of 7 digit
 *  hexidecimal values representing the current state of the 28 LDDIO
 *  bits (i.e. PPSR)  Each 7 digit hex value returned is separated by an
 *  ASCII carridge return character (0xd).  The following special input
 *  commands can be output examine other LDDIO registers:
 *
 *	V	read direction bits
 *      L       revert to reading PPSR    (the initial state when dev is opened)
 *	S	read current stencil
 *	R	read current readmode     (outputs MY->readMode for next read only
 *                                         subsequent reads use current readMode
 *                                         to select output)
 *
 *	\t	Whitespace characters are treated as NOP operators
 *	\r	(they can be useful to define a new mask without acting on it)
 *	\n
 *	' '
 *
 *****************************************************************************/

#include <linux/config.h>
#include <linux/kernel.h>
#include <linux/miscdevice.h>
#include <linux/slab.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/poll.h>

#include <asm/hardware.h>
#include <asm/irq.h>

#define SA1100_LDDIO_NAME "SA1100 LDDIO"
#define VERSION	 "1.0"  /* driver version string */

#ifdef MODULE
MODULE_AUTHOR("Brent Roman <brent@mbari.org> Kent Headley <headley@MBARI.org>");
MODULE_DESCRIPTION(SA1100_LDDIO_NAME " v" VERSION);
MODULE_SUPPORTED_DEVICE("StrongARM SA1100");
#endif

 
#ifdef LDDIO_DEBUG
#define DEBUG(args...)  printk (KERN_INFO args)
#else
#define DEBUG(args...)
#endif

/*****************  Local Definitions ***************/

#define USERLDDIOS  12   /* only the first 12 LDDIOS may be accessed */
#define STENCIL	((1<<USERLDDIOS)-1) /* don't allow access to upper LDDIO bits */

#define FORMAT "%07X%c" /* format string for each mask value returned */
#define SHORTFORMAT "%05X%c"  /* format for event counts returned */ 
#define VALSIZE 10	/* maximum # of bytes output by sprintf (FORMAT) */

typedef enum {		/* types of data that can be read */
  levels,		/* default LDDIO inputs */
  stencil,		/* currently active stencil mask */
  direction,		/* PPDR direction register */
  inputTypes
} inputType;

typedef enum {		/* parsing states */
  notDigit,
  inDigits,		/* parsing hex digits */
  awaitingSeparator,
  
  parseStates
} parseState;


typedef struct {	/* complete state vector */
  unsigned stencil;	/* currently active stencil */
  unsigned mask;	/* mask */
  parseState parser;	/* command parser's state */
  inputType readMode;	/* type of input to read next */
  unsigned getReadMode;  /* return readmode next */
  char	separator;	/* output separator */
  unsigned char	offset;	/* index into buffer */
  char buffer[VALSIZE]; /* ASCII buffer for last binary value read */
  			/* # of edge transitions since last time we checked */
  //unsigned short count[USERLDDIOS]; 
} lddioState;

#define MY ((lddioState *) file->private_data)

static ssize_t lddio_read(struct file *file, char *buffer, 
			     size_t count, loff_t *pos);

static ssize_t lddio_write(struct file *file, const char *buffer,
			      size_t count, loff_t *ppos);

static int lddio_open(struct inode *inode, struct file *file);

static int lddio_release(struct inode *inode, struct file *file);


/*********************  Local Data  *****************/

static lddioState justOpened = {  /* initial state when opened */
  stencil:  STENCIL,
  mask:     0,
  parser:   notDigit,
  readMode: levels,
  separator:'\n',  /* default separator is the newline character */
  offset:   -1	   /* invalid offset value forces read on first access */  
};


static struct file_operations lddio_ops = {
  read:    lddio_read,
  write:   lddio_write,
  open:    lddio_open,
  release: lddio_release
};

static struct miscdevice LDDIO_misc = {
  SA1100_LDDIO_MINOR, SA1100_LDDIO_NAME, &lddio_ops
};
    
static DECLARE_WAIT_QUEUE_HEAD (lddioWaitQ);  /* blocked on a LDDIO edge event */


/*******************  Internal Functions  ************/

static int reloadMyBuffer (struct file *file)
/*
  replenish MY->buffer  (always /0 terminated!)
  returns negative or zero error code if something went wrong...
  normally returns the number of bytes written to buffer
*/
{
  const char *format = FORMAT;
  unsigned read = 0;  

  /* switch appears to be broken...
  switch (MY->readMode) {      
    case stencil:	      
      read = MY->stencil;
    case levels:
      read = PPSR& maskedStencil;
    default:
      printk (KERN_WARNING "%s: invalid readMode (%d)\n",LDDIO_misc.name, MY->readMode);
  }

  return sprintf (MY->buffer, format, read, MY->separator);
*/
  /* If getReadMode, return readMode for this read only.
     subsequent reads get the parameter selected by readMode.
   */

  if(MY->getReadMode == 1){
      read = MY->readMode;
      MY->getReadMode=0;
  }else
  if(MY->readMode == stencil){
      read = MY->stencil;
  }
  else
  if(MY->readMode == levels){
      read = PPSR;
  }
  else
  if(MY->readMode == direction){
      read = PPDR;
  }
  else
      printk (KERN_WARNING "! %s: invalid state (#%d)\n", LDDIO_misc.name, MY->readMode);
 
  return sprintf (MY->buffer, format, read, MY->separator);

}


static inline char toupper (char c)
{
  if (c >= 'a' && c <= 'z') c -= 'a'-'A';
  return c;
}

  
static void parseNextByte (struct file *file, char c)
/*
  parse the next byte of the command string associated with this
  lddio device
*/
{
  if (MY->parser == awaitingSeparator) {
    MY->separator = c;
    MY->parser = notDigit;
    return;
  }
  c = toupper (c);
  if (c >= '0' && c <= '9') {
    if (MY->parser != inDigits) MY->mask = 0;
    MY->mask <<= 4; MY->mask |= c - '0';
    MY->parser = inDigits;
  } else if (c >= 'A' && c <= 'F') {
    if (MY->parser != inDigits) MY->mask = 0;
    MY->mask <<= 4; MY->mask |= c + 10 - 'A';
    MY->parser = inDigits;
  } else {
    unsigned myStencil = MY->stencil;
    unsigned mask = MY->mask;
    unsigned maskedStencil = mask & myStencil;

    /* get the current state of PPSR; 
       note that these operations are not atomic, as they use 
       a read/mask/write sequence. It is possible for state to 
       change in the middle of the operation, particularly if
       something else is accesssing the LDDIO pins outside of this
       driver. The user must take care to ensure thread safety when
       using the lddio driver.
     */
    unsigned PPSRo = PPSR;

    MY->parser = notDigit;
    switch (c) {
      case 'V': /* read direction register */
        MY->readMode = direction;
	MY->getReadMode=0;
	break;
      case 'L': /* read levels */
        MY->readMode = levels;
	MY->getReadMode=0;
	break;
      case 'S': /* read stencil next */
        MY->readMode = stencil;
	MY->getReadMode=0;
	break;
      case 'R': /* read current readMode */
	MY->getReadMode=1;
	break;
      case '+':	/* set output bits */
    	PPSR = PPSRo | maskedStencil;
	DEBUG("set bits: PPSRo=0x%08X\nmaskedStencil=0x%08X\nPPSR=0x%08X\n\n",PPSRo,maskedStencil,(PPSRo|(maskedStencil)));
	break;
      case '-':	/* clear output bits */
    	PPSR = PPSRo & (~mask | ~myStencil);
	DEBUG("clear bits: PPSRo=0x%08X\nmask=0x%08X ~mask=0x%08X\nmyStencil=0x%08X ~myStencil=0x%08X\nPPSR=0x%08X\n\n",PPSRo,mask,~mask,myStencil,~myStencil,(PPSRo&(~mask|~myStencil)));
	break;
      case '=':	/* assign output bits */
    	PPSR = ( (PPSRo & ~myStencil) | maskedStencil );
	break;
      case '<':	/* restrict stencil */
    	MY->stencil &= mask;
	break;
      case 'I':	/* configure as inputs */
    	PPDR &= ~maskedStencil;
	break;
      case 'O':	/* configure as outputs */
    	PPDR |= maskedStencil;
	break;
      case ':':	/* configure as outputs */
    	PPDR = maskedStencil;
	break;
      case '\\':  /* define separator */
        MY->parser = awaitingSeparator;
	break;
	
      default:	/* unknown operator */
    	printk ("%s: invalid operation '%c'\n", LDDIO_misc.name, c);
      case '\n':  /* ignore whitespace */
      case '\r':
      case ' ':
      case '\t':
        return;        
    }
  }
}


/*******************  File Operations  ************/

static ssize_t lddio_read(struct file *file, char *buffer, 
			     size_t count, loff_t *pos)
{
  const char *base;
  int len;
  size_t limit;

  if (MY->offset >= sizeof (MY->buffer) || !MY->buffer[MY->offset]) {
    MY->offset = 0;
    base = MY->buffer;
    len = reloadMyBuffer (file);
    if (len <= 0) return len;  /* error return */
  } else {
    base = MY->buffer + MY->offset;
    len = strlen (base);
  }
  limit = count > len ? len : count;
  DEBUG("%s: Reading %d/%d bytes\n", LDDIO_misc.name, limit, count);
  if (copy_to_user (buffer, MY->buffer+MY->offset, limit)) return -EFAULT;
  MY->offset += limit;
  return limit;
}


static ssize_t lddio_write(struct file *file, const char *buffer,
			      size_t count, loff_t *ppos)
{ 
  char chunk[512];  /* copy from userspace in these sized chunks */
  const char *source = buffer;
  const char *srcEnd = buffer + count;
  DEBUG("%s: Writing %d bytes\n", LDDIO_misc.name, count);
  while (source < srcEnd) {
    size_t limit = count <= sizeof(chunk) ? count : sizeof(chunk);
    const char *cursor = chunk;
    const char *end = chunk + limit;
    if (copy_from_user (chunk, source, limit)) return -EFAULT;
    while (cursor < end) parseNextByte (file, *cursor++); 
    source += limit; 
  }
  return srcEnd - buffer; 
}


static int lddio_open(struct inode *inode, struct file *file)
{
  DEBUG("%s: Open\n", LDDIO_misc.name);
  MY = (lddioState *) kmalloc (sizeof(lddioState), GFP_KERNEL);
  if (!MY) return -ENOMEM;
  MOD_INC_USE_COUNT;
  *MY = justOpened;
  return 0;
}


static int lddio_release(struct inode *inode, struct file *file)
{
  DEBUG("%s: Release\n", LDDIO_misc.name);
  kfree (MY);
  MOD_DEC_USE_COUNT;
  return 0;
}


static int __init sa1100LDDIO_init(void)
{
  if(misc_register(&LDDIO_misc)<0){
    printk(KERN_ERR "%s: unable to register misc device\n",
	   LDDIO_misc.name);
    return -EIO;
  }    
  printk("%s v" VERSION " device (%d,%d): stencil=0x%07X\n", 
        LDDIO_misc.name, MISC_MAJOR, LDDIO_misc.minor, STENCIL);
  return 0;
}  


static void __exit sa1100LDDIO_exit(void)
{
  if(misc_deregister(&LDDIO_misc)<0)
    printk(KERN_ERR "%s: unable to deregister misc device\n",
	   LDDIO_misc.name);
}


module_init(sa1100LDDIO_init);
module_exit(sa1100LDDIO_exit);
EXPORT_NO_SYMBOLS;

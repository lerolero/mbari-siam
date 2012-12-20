/*********************  linux/drivers/misc/sa1100spi.c  *********************
 *
 *  Copyright (C) 2002 MBARI
 *
 *  MBARI Proprietary Information. All rights reserved.
 *
 *   6 Sep 2002 - brent@mbari.org:  first cut
 *  24 Dec 2002 - brent:  Added delay when changing SSP clock rate
 *   6 Feb 2003 - brent:  init SSCR1, support an external clock source
 *
 *  This is a generic SPI driver for the Intel StrongARM SA-1100 
 *
 *  It is intended to allow arbitrary patterns up to 32 bits in length to
 *  be clocked in and out the SPI port.  To support all languages,
 *  IOCTLs have been avoided -- only read & write operations are required.
 *  Further, all I/O is done in 7-bit ASCII. 
 *
 *  spi is a miscelleous device.  See include/linux/miscdevice.h for its
 *  reserved minor device number.
 *
 *  This driver provides very low-level access to the SSP unit of the SA-1100.
 *  User processes must arbitrate access among themselves. 
 *
 *  Commands output to this device are of the form:
 *
 *	{mask}<operator> 
 *
 *  where the optional {mask} is a hexidecimal number,
 *  and <operator> is a single (case insensitive) ASCII character:
 *
 *      S       Set clock divisor (1..256)   (Clock Rate = 1.8432MHz / divisor)
 *      0S      Select external clock       (on GPIO 19)
 *
 *      L       Loopback  (0..1)            (Select Internal Loopback)
 *      P       Phase (0..1)                (Select Alternate clock phase)
 *      I       Invert (0..1)               (Inverted clock)       
 *
 *      G       Use GPIOs (0..1)            (Reassign SSP to GPIOs 10-14)
 *        Note:  GPIO 11 is NOT reassigned to RxD4 as indicated in Intel's Docs
 *
 *      W       Word Width (8,10,12,..32)       (must be be even -- defaults to 32)
 *
 *      =       Output		                (write mask to output)
 *
 *      \       (next byte will be used as separator for all subsequent reads)
 *              (The separator defaults to \n when the device is first opened)
 *	
 *  Reading the spi device returns the most recently received
 *  word received as a result of previous writes, followed by a separator
 *  (normally \n).  Once the separator has been read, no further characters
 *  will be available for reading until after the next Output.
 *
 *	\t	Whitespace characters are treated as NOP operators
 *	\r	(they can be useful to define a new mask without acting on it)
 *	\n
 *	' '
 *  
 *****************************************************************************/

#include <linux/config.h>
#include <linux/kernel.h>
#include <linux/major.h>
#include <linux/miscdevice.h>
#include <linux/slab.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/poll.h>
#include <linux/delay.h>

#include <asm/hardware.h>
#include <asm/irq.h>

#define SA1100_SPI_NAME "SA1100 SPI"
#define VERSION	 "1.3"  /* driver version string */

#ifdef MODULE
MODULE_AUTHOR("Brent Roman <brent@MBARI.org>");
MODULE_DESCRIPTION(SA1100_SPI_NAME " v" VERSION);
MODULE_SUPPORTED_DEVICE("StrongARM SA1100");
#endif

#ifdef SPI_DEBUG
#define DEBUG(args...)  printk (KERN_INFO args)
#else
#define DEBUG(args...)
#endif

/*****************  Local Definitions ***************/

#define FORMAT "%X%c"   /* format string for each value returned */
#define VALSIZE 20	/* maximum # of bytes output by sprintf (FORMAT) */

enum {false, true};

typedef enum {		/* parsing states */
  notDigit,
  inDigits,		/* parsing hex digits */
  awaitingSeparator,
  
  parseStates
} parseState;


typedef struct {	/* complete state vector */
  unsigned mask;	/* mask */
  parseState parser;	/* command parser's state */
  char	separator;	/* output separator */
  unsigned char	offset;	/* index into buffer */
  char buffer[VALSIZE]; /* ASCII buffer for last binary value read */
  			/* # of edge transitions since last time we checked */
} spiState;

#define MY ((spiState *) file->private_data)


static ssize_t spi_read(struct file *file, char *buffer, 
			     size_t count, loff_t *pos);

static ssize_t spi_write(struct file *file, const char *buffer,
			      size_t count, loff_t *ppos);

static unsigned spi_poll(struct file *file, poll_table *wait);

static int spi_open(struct inode *inode, struct file *file);

static int spi_release(struct inode *inode, struct file *file);

/*********************  Local Data  *****************/

static spiState justOpened = {  /* initial state when opened */
  mask:     0,
  parser:   notDigit,
  separator:'\n',  /* default separator is the newline character */
  offset:   -1	   /* invalid offset value forces read on first access */  
};


static struct file_operations spi_ops = {
  read:    spi_read,
  write:   spi_write,
  open:    spi_open,
  poll:	   spi_poll,
  release: spi_release
};

static struct miscdevice spi_misc = {
  SA1100_SPI_MINOR, SA1100_SPI_NAME, &spi_ops
};
    
static DECLARE_WAIT_QUEUE_HEAD (spiWaitQ);  /* blocked on a spi read */

static int wordValid = false;  /* word is valid and hasn't yet been read */
static unsigned word;           /* word last read */


/*******************  Internal Functions  ************/

static int reloadMyBuffer (struct file *file)
/*
  replenish MY->buffer  (always /0 terminated!)
  returns negative error code if something went wrong...
  normally returns the number of bytes written to buffer
*/
{
  for (;;)
    if (wordValid) {
      wordValid = false;
      return sprintf (MY->buffer, FORMAT, word, MY->separator);  
    }else{
      if (file->f_flags & O_NONBLOCK) return -EAGAIN; 
      if (signal_pending(current)) return -ERESTARTSYS;
      interruptible_sleep_on (&spiWaitQ);
    }
}


static inline char toupper (char c)
{
  if (c >= 'a' && c <= 'z') c -= 'a'-'A';
  return c;
}


static void writeSPI (unsigned outMask)
/*
  write lower 2*width bits of mask out to SPI port
  read result and unblock any processes blocked on read
  warn if there are unread words in the receiver fifo
*/
{
  unsigned rcvCount=0, out1, out2, in1, in2, inMask;
  unsigned sscr0 = Ser4SSCR0;
  unsigned width = ((sscr0&FMsk(SSCR0_DSS))>>FShft(SSCR0_DSS))+1;
  unsigned divisor = ((sscr0&FMsk(SSCR0_SCR))>>FShft(SSCR0_SCR))+1;
  unsigned long flags;
  while (Ser4SSSR & SSSR_RNE) {Ser4SSDR; rcvCount++;}
  out1 = width >= 8 ? outMask >> (2*width-16) : outMask << (16-2*width);
  out2 = outMask << (16-width);
  save_flags (flags); cli();
  Ser4SSDR = out1;  /* an interrupt here might cause... */
  Ser4SSDR = out2;  /* the FRM signal to deassert after writing only out1 */
  restore_flags(flags);
  inMask = (1<<width)-1;
  udelay (divisor+divisor/2);  /* wait for busy to be asserted */
  while (Ser4SSSR & SSSR_BSY) ; /* wait while transceiver is busy */
  in1 = Ser4SSDR & inMask;
  in2 = Ser4SSDR & inMask;
  word = (in1 << width) | in2;
  if (rcvCount)
      printk ("%s: purged %d unread words from receiver\n", 
                spi_misc.name, rcvCount);
  DEBUG ("%s: Wrote %x with divisor=%d\n", spi_misc.name, outMask, divisor);
  wordValid = true;
  wake_up (&spiWaitQ);
}

  
static void parseNextByte (struct file *file, char c)
/*
  parse the next byte of the command string associated with this
  spi device
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
    unsigned mask = MY->mask;
    MY->parser = notDigit;
    switch (c) {
      case 'S': /* set clock divisor or select external clock */
        if (mask > 0x100)
          printk ("%s: ignored attempt to set invalid SCLK divisor %u\n", 
                  spi_misc.name, mask);
        else{
          unsigned sscr0 = Ser4SSCR0;
          unsigned oldDivisor = ((sscr0&FMsk(SSCR0_SCR))>>FShft(SSCR0_SCR))+1;
          if (!mask) {  /* if divisor == 0, select external clock on GPIO19 */
            GPDR &= ~(1<<19);   /* make GPIO19 an input */
            GAFR |= 1<<19;      /* attach it to the SSP clock */
            Ser4SSCR1 |= SSCR1_ECS;  /* SSP to use this external clock */
          }else{ /* assign clock divisor while preserving data size */
            Ser4SSCR0 = 
              ((mask-1) << FShft (SSCR0_SCR)) | SSCR0_SSE | SSCR0_Motorola | 
                        (sscr0 & FMsk (SSCR0_DSS));
              /* brent@mbari.org -- 12/24/02 */
              /* the SSP seems to need this delay before the speed change takes effect ?! */
            Ser4SSCR1 &= ~SSCR1_ECS;  /* SSP to use this internal clock */
            GAFR &= ~(1<<19);         /* revert GPIO 19 to default fn */
          }
          if (oldDivisor != mask) {
            unsigned worst = oldDivisor > mask ? oldDivisor : mask;
            udelay (worst+worst/2);
          }
        }
        break;
      case 'L': /* enable internal loopback */
        Ser4SSSR = SSSR_ROR;    /* clear the SSSRROR status bit */
        if (mask)
          Ser4SSCR1 |= SSCR1_LBM;
        else
          Ser4SSCR1 &= ~SSCR1_LBM;
	break;       
      case 'G': /* Reassign SSP to GPIOs */
        if (mask) { /* note: GPIO11 is NOT reassigned -- this appears to be an SA1110 bug */
          GPDR |= ((1<<14)-1)^((1<<10)-1) ^ (1<<11);
          GPDR &= ~(1<11) & ((1<<28)-1);
          GAFR |= ((1<<14)-1)^((1<<10)-1);
          PPAR |= PPAR_SPR;
        }else{
          Ser4MCCR0 &= MCCR0_MCE;  /* disable the MCP so it dosn't grab the pins */
          PPAR &= ~PPAR_SPR;
        }
        break;       
      case 'P': /* Phase shift SCLK */
        if (mask)
          Ser4SSCR1 |= SSCR1_SP;
        else
          Ser4SSCR1 &= ~SSCR1_SP;
	break;
      case 'I': /* Invert SCLK */
        if (mask)
          Ser4SSCR1 |= SSCR1_SPO;
        else
          Ser4SSCR1 &= ~SSCR1_SPO;
	break;
      case 'W': /* Set word width */
        if (mask & 1 || mask > 32 || mask < 8)
          printk ("%s: ignored attempt to set unsupported %u bit word width\n", 
                    spi_misc.name, mask);
        else /* assign data size while preserving clock divisor */
          Ser4SSCR0 = (Ser4SSCR0 & FMsk (SSCR0_SCR)) |
                        SSCR0_SSE | SSCR0_Motorola | SSCR0_DataSize(mask/2);   	
	break;
      case '=':	/* Write mask */
    	writeSPI (mask);
	break;
      case '\\':  /* define separator */
        MY->parser = awaitingSeparator;
	break;
	
      default:	/* unknown operator */
    	printk ("%s: invalid operation '%c'\n", spi_misc.name, c);
      case '\n':  /* ignore whitespace */
      case '\r':
      case ' ':
      case '\t':
        return;        
    }
  }
}


/*******************  File Operations  ************/

static ssize_t spi_read(struct file *file, char *buffer, 
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
  DEBUG("%s: Reading %d/%d bytes\n", spi_misc.name, limit, count);
  if (copy_to_user (buffer, MY->buffer+MY->offset, limit)) return -EFAULT;
  MY->offset += limit;
  return limit;
}


static ssize_t spi_write(struct file *file, const char *buffer,
			      size_t count, loff_t *ppos)
{ 
  char chunk[512];  /* copy from userspace in these sized chunks */
  const char *source = buffer;
  const char *srcEnd = buffer + count;
  DEBUG("%s: Writing %d bytes\n", spi_misc.name, count);
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


static unsigned spi_poll(struct file *file, poll_table *wait)
{
  DEBUG("%s: Poll\n", spi_misc.name);
  poll_wait (file, &spiWaitQ, wait);
  return wordValid ? POLLIN | POLLRDNORM : 0;

}


static int spi_open(struct inode *inode, struct file *file)
{
  DEBUG("%s: Open\n", spi_misc.name);
  MY = (spiState *) kmalloc (sizeof(spiState), GFP_KERNEL);
  if (!MY) return -ENOMEM;
  MOD_INC_USE_COUNT;
  *MY = justOpened;
  return 0;
}


static int spi_release(struct inode *inode, struct file *file)
{
  DEBUG("%s: Release\n", spi_misc.name);
  kfree (MY);
  MOD_DEC_USE_COUNT;
  return 0;
}


static int __init sa1100SPI_init(void)
{
  if(misc_register(&spi_misc)<0){
    printk(KERN_ERR "%s: unable to register misc device\n",
	   spi_misc.name);
    return -EIO;
  }    
  printk("%s v" VERSION " device (%d,%d)\n", 
            spi_misc.name, MISC_MAJOR, spi_misc.minor);
  Ser4SSCR1 = 0;  /* SSCR1 does not power up in a known state! */            
  return 0;
}  


static void __exit sa1100SPI_exit(void)
{
  if(misc_deregister(&spi_misc)<0)
    printk(KERN_ERR "%s: unable to deregister misc device\n",
	   spi_misc.name);
}


module_init(sa1100SPI_init);
module_exit(sa1100SPI_exit);
EXPORT_NO_SYMBOLS;

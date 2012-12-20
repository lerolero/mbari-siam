/*********************  linux/drivers/misc/sa1100gpio.c  *********************
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
 *
 *  This is a generic GPIO driver for the Intel StrongARM SA-1100 
 *
 *  It is intended to allow complete control of the 28 GPIO bits 
 *  from any language that can do basic file I/O.  For this reason,
 *  IOCTLs have been avoided -- only read & write operations are required.
 *  Further, all I/O is done in 7-bit ASCII. 
 *
 *  gpio is a miscelleous device.  See include/linux/miscdevice.h for its
 *  reserved minor device number.
 *
 *  Only bits set in the "stencil" can be modified. 
 *  When any process opens this device, the stencil is set to STENCIL.
 *  This protects GPIO bits used by PCMCIA and other kernel
 *  functions from being corrupted by the generic GPIO control driver.
 *
 *  Commands output to this device are of the form:
 *
 *	{mask}<operator> 
 *
 *  where the optional {mask} is a hexidecimal number,
 *  and <operator> is a single (case insensitive) ASCII character:
 *
 *	+	GPSR = mask & stencil		(set bits)
 *
 *	-	GPCR = mask & stencil		(clear bits)
 *
 *	=	GPCR = ~mask & stencil		(copy mask to output)
 *		GPSR = mask & stencil
 *
 *	<	stencil &= mask			(further restrict stencil)
 *	\	(next byte will be used as separator for all subsequent reads)
 *
 *	@	GAFR |= mask & stencil		(select alternate bit fns)
 *	$	GAFR &= ~(mask & stencil)	(select standard bit fns)
 *
 *  These 4 functions have the side effect of clearing _all_ bits in the GEDR!
 *		-- the next three functions install GPIO interrupt handlers --
 *	^			                (detect rising edges)
 *	V			                (detect falling edges)
 *      ~                                       (detect both edges)
 *	.	                        	(ignore both edges)
 *			       -- this removes specified interrupt handlers --
 *
 *	,	                        	(remove pending edge event)
 *      ;                                       (remove all pending edge events)
 *
 *	I	GPDR &= ~(mask & stencil)	(configure bits as input)
 *	O	GPDR |= mask & stencil		(configure bits as output)
 *      :	GPDR = mask & stencil		(configure I/O bits)
 *	
 *  Normally, reading this gpio device returns a stream of 7 digit
 *  hexidecimal values representing the current state of the 28 GPIO
 *  bits (i.e. GPLR)  Each 7 digit hex value returned is separated by an
 *  ASCII carridge return character (0xd).  The following special input
 *  commands can be output examine other GPIO registers:
 *
 *      L       revert to reading GPLR    (the initial state when dev is opened)
 *	Q	query mask of pending edge events (read mask of pending events)
 *	W	like 'q', but wait until at least one edge event in mask is pending 
 *	S	read current stencil
 *      P       report # of pending events 
 *		if more than one bit is set, return the sum of all masked events
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

#define SA1100_GPIO_NAME "SA1100 GPIO"
#define VERSION	 "1.3"  /* driver version string */

#ifdef MODULE
MODULE_AUTHOR("Brent Roman <brent@MBARI.org>");
MODULE_DESCRIPTION(SA1100_GPIO_NAME " v" VERSION);
MODULE_SUPPORTED_DEVICE("StrongARM SA1100");
#endif

#ifdef GPIO_DEBUG
#define DEBUG(args...)  printk (KERN_INFO args)
#else
#define DEBUG(args...)
#endif

/*****************  Local Definitions ***************/

#define USERGPIOS  16   /* only the first 16 GPIOS may be accessed */
#define STENCIL	((1<<USERGPIOS)-1) /* don't allow access to upper GPIO bits */

#define FORMAT "%07X%c" /* format string for each mask value returned */
#define SHORTFORMAT "%05X%c"  /* format for event counts returned */ 
#define VALSIZE 10	/* maximum # of bytes output by sprintf (FORMAT) */

/* return the IRQ corresponding to the given GPIO pin */
#define GPIOIRQ(pin)  ((pin)<11 ? SA1100_IRQ(pin):IRQ_GPIO_11_27(pin))

typedef enum {		/* types of data that can be read */
  levels,		/* default GPIO inputs */
  nonblockingEdges,	/* edge events (nonblocking) */
  blockingEdges,	/* edge events (blocking) */
  stencil,		/* currently active stencil mask */
  counts,               /* event count for GPIO bit given by MY->bitnumber */
  
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
  char	separator;	/* output separator */
  unsigned char	offset;	/* index into buffer */
  char buffer[VALSIZE]; /* ASCII buffer for last binary value read */
  			/* # of edge transitions since last time we checked */
  unsigned short count[USERGPIOS]; 
} gpioState;

#define MY ((gpioState *) file->private_data)


typedef struct {  /* accounting data for GPIO interrupts */
  unsigned short count;	 /* monotonically increasing count of edges detected */
  unsigned short installed; /*# of interrupt handlers for this gpio installed*/
} edgeTally;


static ssize_t gpio_read(struct file *file, char *buffer, 
			     size_t count, loff_t *pos);

static ssize_t gpio_write(struct file *file, const char *buffer,
			      size_t count, loff_t *ppos);

static unsigned gpio_poll(struct file *file, poll_table *wait);

static int gpio_open(struct inode *inode, struct file *file);

static int gpio_release(struct inode *inode, struct file *file);


/*********************  Local Data  *****************/

static gpioState justOpened = {  /* initial state when opened */
  stencil:  STENCIL,
  mask:     0,
  parser:   notDigit,
  readMode: levels,
  separator:'\n',  /* default separator is the newline character */
  offset:   -1	   /* invalid offset value forces read on first access */  
};


static struct file_operations gpio_ops = {
  read:    gpio_read,
  write:   gpio_write,
  open:    gpio_open,
  poll:	   gpio_poll,
  release: gpio_release
};

static struct miscdevice GPIO_misc = {
  SA1100_GPIO_MINOR, SA1100_GPIO_NAME, &gpio_ops
};
    
static edgeTally edgeInterrupt[USERGPIOS] = {{0}};

static DECLARE_WAIT_QUEUE_HEAD (gpioWaitQ);  /* blocked on a GPIO edge event */


/*******************  Internal Functions  ************/

static void sa1100_gpio_handler (int irq, void *thisEdge, struct pt_regs *regs)
/*
  thisEdge points to the relevant edgeInterrupt record
  bump the interrupt count and wake up any waiting tasks
*/
{
  edgeTally *edge = (edgeTally *)thisEdge;
  edge->count++;
  DEBUG ("%s: IRQ%02u (%u)\n", GPIO_misc.name, irq, edge->count);  
  wake_up_interruptible (&gpioWaitQ);
}


static void setGPIOedge( int mask, int edges )
/*
  install or remove our interrupt handler for all the bits in mask
  depending on whether edge(s) are being monitored or ignored
*/
{
  int gpioBit = 0;
  set_GPIO_IRQ_edge (mask, edges);  /* configure hardware edge detection */
  while (mask) {
    if (mask & 1) {
      int irq = GPIOIRQ (gpioBit);
      edgeTally *thisEdge = &edgeInterrupt[gpioBit]; 
      if (edges) {	
        if (!thisEdge->installed) {
	  if (request_irq (irq, sa1100_gpio_handler, 
		           SA_SHIRQ, GPIO_misc.name, thisEdge) < 0)
	    printk (KERN_ERR "%s: unable to register IRQ for GPIO %d\n",
		    GPIO_misc.name, gpioBit);
	  else {
	    thisEdge->installed = 1;
            DEBUG ("%s: requested IRQ for GPIO %d\n", GPIO_misc.name, gpioBit);
	  }
	}
      }else
        if (thisEdge->installed) {
          free_irq (irq, thisEdge);
	  thisEdge->installed = 0;
      	  DEBUG ("%s: freed IRQ for GPIO %d\n", GPIO_misc.name, gpioBit);
        }
    }
    mask >>= 1; gpioBit++;
  }
}


static unsigned pendingEvents (struct file *file, unsigned mask)
/*
  clear those bits in mask for which there are no pending events 
*/
{
  int gpioBit = 0;
  unsigned eventMask = 0;
  while (mask) {
    if (mask & 1 && MY->count[gpioBit] != edgeInterrupt[gpioBit].count)
      eventMask |= 1<<gpioBit;
    mask >>= 1; gpioBit++;
  }
  return eventMask;  
}


static unsigned eventTally (struct file *file, unsigned mask)
/*
  return the sum of number of events pending on each bit set in mask
*/
{
  int gpioBit = 0;
  unsigned tally = 0;
  while (mask) {
    if (mask & 1) tally += edgeInterrupt[gpioBit].count - MY->count[gpioBit];
    mask >>= 1; gpioBit++;
  }
  return tally;  
}


static void purgeEvents (struct file *file, unsigned mask)
/*
  "clear" edge counts by copying them from the interrupt context
*/
{
  int gpioBit = 0;
  while (mask) {
    if (mask & 1) MY->count[gpioBit] = edgeInterrupt[gpioBit].count;
    mask >>= 1; gpioBit++;
  }  
}


static void removeOneEvent (struct file *file, unsigned mask)
/*
  increment MY edge counts to catch up with those from interrupt context
*/
{
  int gpioBit = 0;
  while (mask) {  /* if there are pending events, consume one */
    if (mask & 1 && MY->count[gpioBit] != edgeInterrupt[gpioBit].count)
      MY->count[gpioBit]++;
    mask >>= 1; gpioBit++;
  }  
}


static int reloadMyBuffer (struct file *file)
/*
  replenish MY->buffer  (always /0 terminated!)
  returns negative or zero error code if something went wrong...
  normally returns the number of bytes written to buffer
*/
{
  const char *format = FORMAT;
  unsigned maskedStencil = MY->stencil & MY->mask;
  unsigned read = 0;  
  switch (MY->readMode) {
    case nonblockingEdges:    /* current mask of pending edge events */
      read = pendingEvents (file, maskedStencil);
      break;

    case blockingEdges:       /* block if no edge events are pending */
      read = pendingEvents (file, maskedStencil);
      if (read) break;  /* don't block if events are already pending */
      if (file->f_flags & O_NONBLOCK) return -EAGAIN; 
      {
	DECLARE_WAITQUEUE (self, current);
        add_wait_queue (&gpioWaitQ, &self);
        current->state = TASK_INTERRUPTIBLE;
	for (;;) {
	  read = pendingEvents (file, maskedStencil);;
          if (read) break;
          if (signal_pending (current)) {
            current->state = TASK_RUNNING;
            remove_wait_queue (&gpioWaitQ, &self);
            return -ERESTARTSYS; /* if signaled */
          }		 
          schedule();
        }
        current->state = TASK_RUNNING;
        remove_wait_queue (&gpioWaitQ, &self);
      }
      break;

    case counts:        /* return the total # of pending edges */
      read = eventTally (file, maskedStencil);
      break;
      
    default:
      printk (KERN_WARNING "%s: invalid state (#%d)\n", 
        			      GPIO_misc.name, MY->readMode);
    case levels:	      /* current input levels */
      read = GPLR & maskedStencil;
  }
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
  gpio device
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
    unsigned stencil = MY->stencil;
    unsigned mask = MY->mask;
    unsigned maskedStencil = mask & stencil;
    MY->parser = notDigit;
    switch (c) {
      case 'P': /* read event count */
        MY->readMode = counts;   /* read event counts for these bits */
        break;
      case 'Q': /* read nonblocking edge events next */
        MY->readMode = nonblockingEdges;
	break;
      case 'W': /* read blocking edge events next */
        MY->readMode = blockingEdges;
	break;
      case 'L': /* read levels */
        MY->readMode = levels;
	break;
      case 'S': /* read stencil next */
        MY->readMode = stencil;
	break;
      case '+':	/* set output bits */
    	GPSR = maskedStencil;
	break;
      case '-':	/* clear output bits */
    	GPCR = maskedStencil;
	break;
      case '=':	/* assign output bits */
    	GPCR = ~mask & stencil;
	GPSR = maskedStencil;
	break;
      case '<':	/* restrict stencil */
        /* note: doing this with installed IRQs may leave them "orphaned" 
	         the alternative is to free such IRQs but that may
		 mess up other tasks that might also be using them
		 So... users should restrict the stencil before trying
		 to detect edges! */
    	MY->stencil &= mask;
	break;
      case '@':	/* select alternate bit functions */
    	GAFR |= maskedStencil;
	break;
      case '$':	/* select standard bit functions */
    	GAFR &= ~maskedStencil;
	break;
      case '^':	/* detect rising edges */
        setGPIOedge (maskedStencil, GPIO_RISING_EDGE);
	break;
      case 'V':	/* detect falling edges */
        setGPIOedge (maskedStencil, GPIO_FALLING_EDGE);
	break;
      case '~':	/* detect either edge */
        setGPIOedge (maskedStencil, GPIO_BOTH_EDGES);
	break;
      case '.':	/* ignore edges */
        setGPIOedge (maskedStencil, 0);
	break;
      case ';':	/* purge all edge events */
    	purgeEvents (file, maskedStencil);
	break;
      case ',':	/* remove one pending edge event */
    	removeOneEvent (file, maskedStencil);
	break;
      case 'I':	/* configure as inputs */
    	GPDR &= ~maskedStencil;
	break;
      case ':':	/* configure I/O direction */
    	GPDR &= ~maskedStencil;
      case 'O':	/* configure as outputs */
    	GPDR |= maskedStencil;
	break;
      case '\\':  /* define separator */
        MY->parser = awaitingSeparator;
	break;
	
      default:	/* unknown operator */
    	printk ("%s: invalid operation '%c'\n", GPIO_misc.name, c);
      case '\n':  /* ignore whitespace */
      case '\r':
      case ' ':
      case '\t':
        return;        
    }
  }
}


/*******************  File Operations  ************/

static ssize_t gpio_read(struct file *file, char *buffer, 
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
  DEBUG("%s: Reading %d/%d bytes\n", GPIO_misc.name, limit, count);
  if (copy_to_user (buffer, MY->buffer+MY->offset, limit)) return -EFAULT;
  MY->offset += limit;
  return limit;
}


static ssize_t gpio_write(struct file *file, const char *buffer,
			      size_t count, loff_t *ppos)
{ 
  char chunk[512];  /* copy from userspace in these sized chunks */
  const char *source = buffer;
  const char *srcEnd = buffer + count;
  DEBUG("%s: Writing %d bytes\n", GPIO_misc.name, count);
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


static unsigned gpio_poll(struct file *file, poll_table *wait)
{
  DEBUG("%s: Poll\n", GPIO_misc.name);
  poll_wait (file, &gpioWaitQ, wait);
  return pendingEvents (file, MY->stencil & MY->mask) ?
  					POLLIN | POLLRDNORM : 0;

}


static int gpio_open(struct inode *inode, struct file *file)
{
  DEBUG("%s: Open\n", GPIO_misc.name);
  MY = (gpioState *) kmalloc (sizeof(gpioState), GFP_KERNEL);
  if (!MY) return -ENOMEM;
  MOD_INC_USE_COUNT;
  *MY = justOpened;
  purgeEvents (file, STENCIL);  /* purge all pending events */
  return 0;
}


static int gpio_release(struct inode *inode, struct file *file)
{
  DEBUG("%s: Release\n", GPIO_misc.name);
  kfree (MY);
  MOD_DEC_USE_COUNT;
  return 0;
}


static int __init sa1100GPIO_init(void)
{
  if(misc_register(&GPIO_misc)<0){
    printk(KERN_ERR "%s: unable to register misc device\n",
	   GPIO_misc.name);
    return -EIO;
  }    
  printk("%s v" VERSION " device (%d,%d): stencil=0x%07X\n", 
        GPIO_misc.name, MISC_MAJOR, GPIO_misc.minor, STENCIL);
  return 0;
}  


static void __exit sa1100GPIO_exit(void)
{
  if(misc_deregister(&GPIO_misc)<0)
    printk(KERN_ERR "%s: unable to deregister misc device\n",
	   GPIO_misc.name);
  setGPIOedge (STENCIL, 0);  /* free any irq handler we might have installed */
}


module_init(sa1100GPIO_init);
module_exit(sa1100GPIO_exit);
EXPORT_NO_SYMBOLS;

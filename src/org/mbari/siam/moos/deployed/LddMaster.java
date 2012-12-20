// Copyright MBARI 2003
package org.mbari.siam.moos.deployed;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;


/**
   LddMaster provides an application level interface to the StrongARM
   LDD IO pins, which are similar to GPIO pins, except that they have
   no interrupt capabilities. The LddMaster accesses these ports using
   the character special file (/dev/lddio) interface presented by the 
   sa1100lddio.o (lddio) Linux kernel module.

   It is possible to read several different driver parameters in addition
   to reading the IO port itself. The read mode of the lddio module sticky;
   when the read mode is set, subsequent reads will read the parameter
   specified. To read a different parameter, the read mode must be explicitly
   set.

   A number of convenience functions are provided to hide this behavior; the sticky
   read mode is relevant when using readLdd directly.

*/
public class LddMaster {
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(LddMaster.class);

    /** Name of device file */
    public static final String LDD_DEVICE_NAME = "/dev/lddio"; 
    /** Set bits command */
    public static final String LDD_SET_BITS = "+"; 
    /** Clear bits command */
    public static final String LDD_CLEAR_BITS = "-"; 
    /** Write mask command */
    public static final String LDD_WRITE_LEVELS = "="; 
    /** Set direction to input command */
    public static final String LDD_SET_INPUT = "I"; 
    /** Set direction to output command */
    public static final String LDD_SET_OUTPUT = "O";
    /** Set direction register */
    public static final String LDD_WRITE_DIRECTION = ":";
    /** Read levels command */
    public static final String LDD_SET_READMODE_LEVELS = "L";
    /** Read stencil command */
    public static final String LDD_SET_READMODE_STENCIL = "S";
    /** Read direction command */
    public static final String LDD_SET_READMODE_DIRECTION = "V";
    /** Read readmode command */
    public static final String LDD_SET_READMODE_READMODE = "R";
    /** LDD bits */
    public static final int LDD_BITS = 0xFFF; 
    /** Red LED bit */
    public static final int LDD_RED_LED = 0x1;
    /** Yellow LED bit */
    public static final int LDD_YELLOW_LED = 0x2;
    /** Green LED bit */
    public static final int LDD_GREEN_LED = 0x4;
    /** RS485 Rx bit */
    public static final int LDD_RS485_RCV = 0x8;
    /** RS485 Tx bit */
    public static final int LDD_RS485_DRV = 0x10;
    /** Radio 1 power bit */
    public static final int LDD_RF1_POWER = 0x20;
    /** Radio 2 power bit */
    public static final int LDD_RF2_POWER = 0x40;
    /** MSP430 attention bit */
    public static final int LDD_ATTN_MSP430 = 0x80;
    /** Unused bits */
    public static final int LDD_UNUSED = 0xF00;
    /** Mnemonic for input */
    public static final int LDD_DIRECTION_INPUT = 0x0;
    /** Mnemonic for output */
    public static final int LDD_DIRECTION_OUTPUT = 0x1;
    // don't change these: they need to jive with
    // the lddio driver
    /** Mnemonic read mode levels */
    public static final int LDD_READMODE_LEVELS = 0x0;
    /** Mnemonic read mode stencil */
    public static final int LDD_READMODE_STENCIL = 0x1;
    /** Mnemonic read mode direction */
    public static final int LDD_READMODE_DIRECTION = 0x2;
    /** Mnemonic read mode readmode */
    public static final int LDD_READMODE_READMODE = 0x3;

    /** Mask of lower 3 bits (LEDs) and upper 4 bits 
	(used by Sidearm for other nefarious purposes) 
    */
    public static final int LDD_STENCIL = (LDD_BITS & ~(LDD_UNUSED | LDD_RED_LED | LDD_YELLOW_LED | LDD_GREEN_LED ));;
    
    /** Current read mode */
    private static int _readMode = LDD_READMODE_LEVELS;
    /** THE LddMaster instance */
    private static LddMaster cInstance = new LddMaster();
    /** LDDIO special device */
    private static RandomAccessFile lddDevice;

    
    /** Get an LddMaster instance */
    public synchronized static LddMaster getInstance(){
	if( cInstance != null){
	    return cInstance;
	}else{
	    _log4j.warn("LddMaster.getInstance() no instance -- returning null");
	    return null;
	}
    }

    /** Write Ldd */
    public void writeLdd(String data){
	try{
	    lddDevice.writeBytes(data);	    
	}catch (FileNotFoundException e){
	    _log4j.error ("Cannot open device:  "+LDD_DEVICE_NAME);
	}
	catch(IOException e){
	    _log4j.error (e);
	}
    }//writeLdd()

    /** Read Ldd in current readMode */
    public int readLdd(){
	String inData;
	int result=-1;
	try{
	    inData = lddDevice.readLine();
	    result = Integer.parseInt(inData,16);
	}catch (FileNotFoundException e){
	    _log4j.error ("Cannot open device:  "+LDD_DEVICE_NAME);
	}
	catch(IOException e){
	    _log4j.error (e);
	}
	return result;
    }//readLdd()

    /** Read bits and apply specified mask */
    public int readStenciledLdd(){
	return (readLdd() & LDD_STENCIL);
    }

    /** Apply stencil to bits (protects bits that should not be touched) */
    public int applyStencil(int mask){
	return mask & LDD_STENCIL;
    }
    
    /** Set direction for bits in mask */
    public void setDirection(int mask, int direction){
	int PPDRo=getDirection();
	int maskedStencil = applyStencil(mask);
	//_log4j.debug("setDirection: PPDRo = "+Integer.toHexString(PPDRo)+
	//		   " mask = "+Integer.toHexString(mask)+
	//		   " maskedStencil = "+Integer.toHexString(maskedStencil));
	switch(direction){
	case LDD_DIRECTION_INPUT:
	    //_log4j.debug("setDirection: input string = "+
	    //Integer.toHexString(PPDRo&maskedStencil)+LDD_SET_INPUT);
	    writeLdd(Integer.toHexString(PPDRo&maskedStencil)+LDD_SET_INPUT);
	    break;
	case LDD_DIRECTION_OUTPUT:
	    //_log4j.debug("setDirection: output string = "+
	    //Integer.toHexString(PPDRo|maskedStencil)+LDD_SET_OUTPUT);
	    writeLdd(Integer.toHexString(PPDRo|maskedStencil)+LDD_SET_OUTPUT);
	    break;
	default:
	    _log4j.error("LddMaster: Invalid direction specified: "+direction);
	    break;
	}
	return;
    }

    /** Get LDD direction bits */
    public int getDirection(){
	setReadMode(LDD_READMODE_DIRECTION);
	return readLdd();
    }

    /** Get LDD direction bits */
    public int getLevels(){
	setReadMode(LDD_READMODE_LEVELS);
	return readLdd();
    }

    /** Set to read either the LDD port or the current stencil */
    public void setReadMode(int readMode){
	switch(readMode){
	case LDD_READMODE_LEVELS:
	    writeLdd(LDD_SET_READMODE_LEVELS);
	    break;
	case LDD_READMODE_STENCIL:
	    writeLdd(LDD_SET_READMODE_STENCIL);
	    break;
	case LDD_READMODE_DIRECTION:
	    writeLdd(LDD_SET_READMODE_DIRECTION);
	    break;
	case LDD_READMODE_READMODE:
	    writeLdd(LDD_SET_READMODE_READMODE);
	    break;
	default:
	    _log4j.error("Invalid readMode: ("+readMode+")");
	}
	return;
    }

    /** Get current read mode */
    public synchronized int getReadMode(){
	// To obtain the current read mode,
	// set readMode=readMode and then read
	// the device. Note that subsequent reads
	// will return the parameter selected
	// the actual read mode.
	// This operation must be atomic:
	// It would cause confusion if the readmode were set,
	// and something else reads the device, assuming the
	// readmode were something else; therefore, this method
	// is synchronized.
	setReadMode(LDD_READMODE_READMODE);
	return readLdd();
    }
    
    /** Write mask to the PPSR register*/
    public void writeLevels(int mask){
	int PPSRo = getLevels();
	int maskedStencil = applyStencil(mask);
	int writeVal = (PPSRo & ~LDD_STENCIL) | maskedStencil;
	//_log4j.debug("writeLevels: PPSRo&~stencil = "+Integer.toHexString(PPSRo & ~LDD_STENCIL)+" maskedStencil = "+Integer.toHexString(maskedStencil));
	/** the pins must be outputs to write to them */
	setDirection( LDD_STENCIL,LDD_DIRECTION_OUTPUT);
	writeLdd(Integer.toHexString(writeVal)+LDD_WRITE_LEVELS);
    }

    /** Write entire PPDR register */
    public void writeDirection(int mask){
	int PPDRo=getDirection();
	int maskedStencil = applyStencil(mask);

	/** the pins must be outputs to write to them */
	writeLdd(Integer.toHexString( (PPDRo & ~LDD_STENCIL) | maskedStencil)+LDD_WRITE_DIRECTION);
    }

    /** Write bits set in mask to the lddio pins*/
    public void setBits(int mask){
	int maskedStencil = applyStencil(mask);
	/** the pins must be outputs to write to them */
	setDirection(maskedStencil,LDD_DIRECTION_OUTPUT);
	writeLdd(Integer.toHexString(maskedStencil)+LDD_SET_BITS);
    }
    
    /** Clear bits set in mask to the lddio pins*/
    public void clearBits(int mask){
	int maskedStencil = applyStencil(mask);
	/** the pins must be outputs to write to them */
	setDirection(maskedStencil,LDD_DIRECTION_OUTPUT);
	writeLdd(Integer.toHexString(maskedStencil)+LDD_CLEAR_BITS);
    }
        
    // one-time class initialization 
    static {
	LddMaster ldd = LddMaster.getInstance();
	try {
	    // Open device file
	    lddDevice = new RandomAccessFile (LDD_DEVICE_NAME, "rw");
	    
	    // Configure LDD Driver
	    ldd.setReadMode(LDD_READMODE_LEVELS);

	}
    catch (FileNotFoundException e) {
	_log4j.error ("Cannot open device:  "+LDD_DEVICE_NAME);
    }
    catch (IOException e) {
	_log4j.error (e);
    }
    }


    /** Eine Kleine test code */
    public int test(){
	int result=0;
	int testCounter=0;

	// Save current state of PPDR,PPSR
	_log4j.debug("\ntest: Reading current state...");
	int PPDRo = getDirection();
	int PPSRo = getLevels();

	_log4j.debug("Initial State: PPSRo = "+Integer.toHexString(PPSRo) +" PPDRo = "+Integer.toHexString(PPDRo)+" stencil = "+Integer.toHexString(LDD_STENCIL)+"\n");

	// write/read a change to PPSR (should preserve stenciled bits)
	int mask = ~(getLevels());
	_log4j.debug("attempting PPSR writeLevels("+Integer.toHexString(mask)+")...");
	writeLevels(mask);
	int readback = getLevels();
	int test = (mask & LDD_STENCIL);
	boolean passed = ( (readback & LDD_STENCIL) == ( test ) );
	_log4j.debug("read back = "+Integer.toHexString(readback));
	_log4j.debug(Integer.toHexString((readback & LDD_STENCIL))+"=="+Integer.toHexString(test)+"?");
	_log4j.debug((passed?" PASS ":" FAIL ")+"\n");
	result|=((passed?0:1)<<testCounter++);

	// Test clear bits (should preserve stenciled bits)
	mask=getLevels();
	_log4j.debug("attempting PPSR clearBits("+Integer.toHexString(mask)+")...");
	clearBits(mask);
	readback = getLevels();
	test = ( mask & LDD_STENCIL ) & ~mask;// always zero
	passed = ( (readback & LDD_STENCIL) == ( test ) );
	_log4j.debug("read back = "+Integer.toHexString(readback));
	_log4j.debug(Integer.toHexString((readback & LDD_STENCIL))+"=="+Integer.toHexString(test)+"?");
	_log4j.debug((passed?" PASS ":" FAIL ")+"\n");
	result|=((passed?0:1)<<testCounter++);

	// Test set bits (should preserve stenciled bits)
	mask=~(getLevels());
	_log4j.debug("attempting PPSR setBits("+Integer.toHexString(mask)+")...");
	setBits(mask);
	readback = getLevels();
	test = ( mask & LDD_STENCIL );
	passed = ( (readback & LDD_STENCIL) == ( test ) );
	_log4j.debug("read back = "+Integer.toHexString(readback));
	_log4j.debug(Integer.toHexString((readback & LDD_STENCIL))+"=="+Integer.toHexString(test)+"?");
	_log4j.debug((passed?" PASS ":" FAIL ")+"\n");
	result|=((passed?0:1)<<testCounter++);


	// Test write direction (should preserve stenciled bits)
	mask =~(getDirection());
	_log4j.debug("attempting PPDR writeDirection("+Integer.toHexString(mask)+")...");
	writeDirection(mask);
	readback = getDirection();
	test = ( mask & LDD_STENCIL );
	passed = ( (readback & LDD_STENCIL) == ( test ) );
	_log4j.debug("read back = "+Integer.toHexString(readback));
	_log4j.debug(Integer.toHexString((readback & LDD_STENCIL))+"=="+Integer.toHexString(test)+"?");
	_log4j.debug((passed?" PASS ":" FAIL ")+"\n");
	result|=((passed?0:1)<<testCounter++);


	// Test set direction (should preserve stenciled bits)
	mask=0xFFFFFFFF;
	_log4j.debug("attempting PPDR setDirection("+Integer.toHexString(mask)+","+LDD_DIRECTION_OUTPUT+")...");
	setDirection(mask,LDD_DIRECTION_OUTPUT);
	readback = getDirection();
	test = ( mask & LDD_STENCIL );
	passed = ( (readback & LDD_STENCIL) == ( test ) );
	_log4j.debug("read back = "+Integer.toHexString(readback));
	_log4j.debug(Integer.toHexString((readback & LDD_STENCIL))+"=="+Integer.toHexString(test)+"?");
	_log4j.debug((passed?" PASS ":" FAIL ")+"\n");
	result|=((passed?0:1)<<testCounter++);


	// Test set direction (should preserve stenciled bits)
	mask=0xFFFFFFFF;
	_log4j.debug("attempting PPDR setDirection("+Integer.toHexString(mask)+","+LDD_DIRECTION_INPUT+")...");
	setDirection(mask,LDD_DIRECTION_INPUT);
	readback = getDirection();
	test = ( mask & LDD_STENCIL )&~mask;//always zero
	passed = ( (readback & LDD_STENCIL) == ( test ) );
	_log4j.debug("read back = "+Integer.toHexString(readback));
	_log4j.debug(Integer.toHexString((readback & LDD_STENCIL))+"=="+Integer.toHexString(test)+"?");
	_log4j.debug((passed?" PASS ":" FAIL ")+"\n");
	result|=((passed?0:1)<<testCounter++);

	// Test set/get readmode
	mask=LDD_READMODE_LEVELS;
	_log4j.debug("attempting setReadmode("+Integer.toHexString(mask)+") Current readmode="+getReadMode()+"...");
	setReadMode(mask);
	readback = getReadMode();
	test = mask;
	passed = ( ( readback ) == ( test ) );
	_log4j.debug("read back = "+Integer.toHexString(readback));
	_log4j.debug(Integer.toHexString((readback))+"=="+Integer.toHexString(test)+"?");
	_log4j.debug((passed?" PASS ":" FAIL ")+"\n");
	result|=((passed?0:1)<<testCounter++);
	

	// Restore LDD state
	// write levels first
	_log4j.debug("Restoring state...");
	writeLevels(PPSRo);

	// do direction...
	writeDirection(PPDRo);

	_log4j.debug("State restored: PPSR = "+Integer.toHexString(getLevels())+" PPDR = "+Integer.toHexString(getDirection()));
	_log4j.debug("Done\n");
	return result;
    }
    static public void main(String[] args) {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	LddMaster ldd = LddMaster.getInstance();
	int result= ldd.test();
	_log4j.debug("LddMaster test exited with code "+Integer.toHexString(result));
	System.exit(result);
    }
  
}// class LddMaster

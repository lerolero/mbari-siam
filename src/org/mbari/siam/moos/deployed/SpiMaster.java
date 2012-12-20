// Copyright MBARI 2003
package org.mbari.siam.moos.deployed;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.apache.log4j.Logger;

public class SpiMaster {

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(SpiMaster.class);

    public static final String SPI_DEVICE_NAME = "/dev/spi"; 
    public static final String GPIO_DEVICE_NAME = "/dev/gpio"; 
    public static final int SPI_WORD_WIDTH_8 = 8;
    public static final int SPI_WORD_WIDTH_16 = 16;
    public static final int SPI_WORD_WIDTH_32 = 32;
    public static final int SPI_CLOCK_DIVIDER_256 = 256;
    public static final int SPI_CLOCK_DIVIDER_64 = 64;
    public static final int SPI_SLAVE_SELECT_NONE = 7;
    public static final int SPI_SLAVE_SELECT_0 = 0;
    public static final int SPI_SLAVE_SELECT_1 = 1;
    public static final int SPI_SLAVE_SELECT_2 = 2;
    public static final int SPI_SLAVE_SELECT_3 = 3;
    public static final int SPI_SLAVE_SELECT_4 = 4;
    public static final int SPI_SLAVE_SELECT_5 = 5;
    /** Shift slave select values by this amount; uses GPIO bits 4-6*/
    public static final int SPI_SLAVE_SELECT_GPIO_OFFSET=8;
    /** Slave select mask (only values 0-7 can be used); apply before shift */
    public static final int SPI_SLAVE_SELECT_MASK=0x7;
    public static final int SPI_USE_PORT = 0;
    public static final int SPI_USE_GPIO = 1;
    public static final int SPI_PASSTHROUGH = 0;
    public static final int SPI_LOOPBACK = 1;
    public static final int SPI_DEFAULT_PHASE = 0;
    public static final int SPI_ALT_PHASE = 1;
    public static final int SPI_DEFAULT_CLOCK = 0;
    public static final int SPI_INVERTED_CLOCK = 1;
 
    private static SpiMaster cInstance = new SpiMaster();
    private static RandomAccessFile spiDevice;
    private static RandomAccessFile gpioDevice;

    public synchronized static SpiMaster getInstance(){
	if( cInstance != null){
	    return cInstance;
	}else{
	    _log4j.debug("SpiMaster.getInstance() no instance -- returning null");
          return null;
	}
    }

    public void setClkDivider(int d){
	writeSpi(Integer.toHexString(d)+"S");
    }//setClkDivider()

    public void setSlaveSelect(int cs){
	// The SA1100spi module that contains the Q option is 
	// deprecated. Instead, we use the SA1100gpio module.
	// The chipselect is now masked to 3 LSBs and left-shifted
	// 4 places, since GPIO bits 4:6 are used for SPI CS.

	//writeSpi(Integer.toHexString(cs)+"Q");

	cs &= SPI_SLAVE_SELECT_MASK;
	cs = cs << SPI_SLAVE_SELECT_GPIO_OFFSET;
	/*
	int clrSpi = SPI_SLAVE_SELECT_NONE << SPI_SLAVE_SELECT_GPIO_OFFSET;
	// clear SPI chip selects
	writeGpio(Integer.toHexString(clrSpi)+"-");
	*/
	// set SPI chip selects
	// can't just use "=" operator, because it blows away other GPIO
	// bits (rfpower, etc.)

	writeGpio(Integer.toHexString(cs)+"=");

    }//setSlaveSelect()
    
    public int writeReadData(int dataOut){
	String dataIn;
	if (spiDevice == null) {
	    _log4j.error("writeReadData() - NULL spiDevice");
	    return -1;
	}

	try{
	    spiDevice.writeBytes(Integer.toHexString(dataOut)+"=");
	    dataIn = spiDevice.readLine();
	    return Integer.parseInt(dataIn,16);
	}catch (FileNotFoundException f){
	    _log4j.error ("Cannot access: "+SPI_DEVICE_NAME);
	}
	catch(IOException i){
	    _log4j.error (i);
	}
	catch(NumberFormatException n){
	    _log4j.error(n);
	}
	_log4j.error("writeReadData: Unhandled Exception - exiting SIAM app");
	System.exit(-2);
	return -1;
    }//writeReadData()

    public void writeSpi(String data){
	if (spiDevice == null) {
	    _log4j.error("Null spiDevice");
	    return;
	}
	try{
	    spiDevice.writeBytes(data);	   
	    return;
	}catch (FileNotFoundException e){
	    _log4j.error ("Cannot open device:  "+SPI_DEVICE_NAME);
	}
	catch(IOException e){
	    _log4j.error (e);
	}
	_log4j.error ("writeSpi: unhandled exception");
    }//writeSpi()

    public void writeGpio(String data){
	if (gpioDevice == null) {
	    _log4j.error("writeGpio() - NULL gpioDevice");
	    return;
	}

	try{
	    gpioDevice.writeBytes(data);
	    return;
	}catch (FileNotFoundException e){
	    _log4j.error ("Cannot open device:  "+GPIO_DEVICE_NAME);
	}
	catch(IOException e){
	    _log4j.error (e);
	}
	_log4j.error ("writeGpio: unhandled exception");
    }//writeGpio()

    static {  /* one-time class initialization */
        
        /* open files one at a time so you know who fails */
        try {
	    spiDevice = new RandomAccessFile (SPI_DEVICE_NAME, "rw");
	}
	catch (FileNotFoundException e) {
	    _log4j.error ("Cannot open device:  "+SPI_DEVICE_NAME);
	}
	
        try {
	    gpioDevice = new RandomAccessFile (GPIO_DEVICE_NAME, "rw");
	}
	catch (FileNotFoundException e) {
	    _log4j.error ("Cannot open device:  "+GPIO_DEVICE_NAME);
	}
        
        try {
	    //spiDevice = new RandomAccessFile (SPI_DEVICE_NAME, "rw");
	    //gpioDevice = new RandomAccessFile (GPIO_DEVICE_NAME, "rw");
	    // Configure SPI Driver
	    String cfgString = (Integer.toHexString(SPI_WORD_WIDTH_16)+"W"+
				  Integer.toHexString(SPI_USE_PORT)+"G"+
				  Integer.toHexString(SPI_CLOCK_DIVIDER_256)+"S"+
				  Integer.toHexString(SPI_PASSTHROUGH)+"L"+
				  Integer.toHexString(SPI_DEFAULT_PHASE)+"P"+
				Integer.toHexString(SPI_DEFAULT_CLOCK)+"I"
				//Integer.toHexString(SPI_SLAVE_SELECT_NONE)+"Q"
				);

	    if (spiDevice != null && gpioDevice != null) {
		spiDevice.writeBytes (cfgString);

		// Restrict mask so that SpiMaster won't touch anyone 
		// else's bits
		String rs = 
		    Integer.toHexString((SPI_SLAVE_SELECT_MASK << SPI_SLAVE_SELECT_GPIO_OFFSET))+"<";

		gpioDevice.writeBytes(rs);

		// Deselect all SPI chip Selects
		int gpioNone = 
		    SPI_SLAVE_SELECT_NONE<<SPI_SLAVE_SELECT_GPIO_OFFSET;

		String clrString = (Integer.toHexString(gpioNone)+"=");
		gpioDevice.writeBytes(clrString);
		_log4j.debug("SPI configured with " + cfgString + 
			     " cs=" + clrString );
	    }
	    else {
		if (spiDevice == null) {
		    _log4j.error("Null spi device");
		}
		if (gpioDevice == null) {
		    _log4j.error("Null gpio device");
		}
	    }

	}
	catch (IOException e) {
	    _log4j.error(e);
	}catch(Exception x){
	    _log4j.error(x);
	}catch(Throwable t){
	    _log4j.error(t);
	}
    }

}// class SpiMaster

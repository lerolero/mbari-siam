/****************************************************************************/
/* Copyright 2004 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.utils;
import java.io.OutputStream;
import java.io.IOException;

/**
   OutputToInstrument encapsulates an OutputStream; enforces specified 
   delay between writing output bytes, since many instruments have 
   limited processors which might otherwise be overwhelmed by the 
   incoming characters.
   Note that OutputToInstrument can not directly extend OutputStream,
   since the OutputStream we have to work with is returned by

   @author Tom O'Reilly
 */
public class OutputToInstrument {

    /** Encapsulated output stream */
    OutputStream _outputStream = null;

    /** Delay between writing of adjacent bytes */
    int _interByteMsec = 0;

    public OutputToInstrument(OutputStream outputStream) { 
	_outputStream = outputStream;

	// Hardcoded for now
	_interByteMsec = 300;
    }
    

    /** Write bytes to instrument, enforcing specified delay between writing
     of each byte. */
    public void write(byte[] b, int interByteMsec) 
	throws IOException {
	for (int i = 0; i < b.length; i++) {

	    _outputStream.write(b[i]);
	    _outputStream.flush();

	    try {
		Thread.sleep(interByteMsec);
	    }
	    catch (InterruptedException e) {
	    }
	}
    }

    /** Write bytes to instrument, enforcing default delay between writing
     of each byte. */
    public void write(byte[] b) 
	throws IOException {
	write(b, _interByteMsec);
    }

    /** Write a single int to the instrument. */
    public void write(int b) 
	throws IOException {
	_outputStream.write(b);
	_outputStream.flush();
    }


    /** Flush all characters to instrument. */
    public void flush() 
    throws IOException {
	_outputStream.flush();
    }
}

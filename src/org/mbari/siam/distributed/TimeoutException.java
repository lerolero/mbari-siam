// Copyright MBARI 2002
package org.mbari.siam.distributed;

import gnu.io.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Exception;

/**
 * TimeoutException indicates when a some operation has timed out.
 */
public class TimeoutException extends Exception {
	public TimeoutException() {
		super();
	}

	public TimeoutException(String s) {
		super(s);
	}
}
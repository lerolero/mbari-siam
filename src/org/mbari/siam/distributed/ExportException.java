// Copyright 2004 MBARI
package org.mbari.siam.distributed;

import java.io.IOException;

/**
 * ExportException
 * 
 * @author Kent Headley
 */

public class ExportException extends IOException {
	public ExportException() {
		super();
	}

	public ExportException(String s) {
		super(s);
	}
}


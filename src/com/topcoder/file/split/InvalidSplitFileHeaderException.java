/*
 * InvalidSplitFileHeaderException.java
 *
 * Copyright © 2003, TopCoder, Inc. All rights reserved
 */
package com.topcoder.file.split;

/**
 * <p>Thrown when the header in a split file to be joined is not valid.</p>
 *
 * @author srowen
 * @author npe
 * @version 1.0
 */
public class InvalidSplitFileHeaderException extends Exception {

    public InvalidSplitFileHeaderException() {
    }

    public InvalidSplitFileHeaderException(final String message) {
        super(message);
    }
}
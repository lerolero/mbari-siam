/*
 * Utils.java
 *
 * Copyright © 2003, TopCoder, Inc. All rights reserved
 */
package com.topcoder.file.split;

/**
 * This is a final, package-private utilities class for the file splitter.  It
 * consists of utility methods that are shared by both the file splitter and
 * joiner components.
 *
 * @author npe
 * @version 1.0
 * @see FileSplitter, FileJoiner
 */
final class Utils {
    /**
     * Formats a split file index value by 1) converting the integer index into
     * a string; and 2) then padding the index string with zeroes so that it is
     * 6 characters long.
     *
     * @param splitFileIndex The index of the split file, e.g. '1' or '42'.
     * @return The formatted index string, padded to 6 characters.
     */
    public static String formatIndex(int splitFileIndex) {
        String indexString = "000000" + splitFileIndex;
        int length = indexString.length();
        return indexString.substring(length - 6, length);
    }

    /**
     * Don't allow this utilities class to be instantiated.
     */
    private Utils() {
    }
}
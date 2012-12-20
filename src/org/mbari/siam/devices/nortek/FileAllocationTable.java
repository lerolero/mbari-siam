/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek;
import org.apache.log4j.Logger;

/** Represents the instrument's file allocation table. */
public class FileAllocationTable {

    static private Logger _log4j = Logger.getLogger(FileAllocationTable.class);

    public final static int TABLE_BYTES = 512;
    private byte[] _data = new byte[TABLE_BYTES];
    private int _totalRecorderBytes;
    public final static int NROWS = 32;
    public final static int ROW_LENGTH = 16;

    /** Create file allocation table, and set total recorder size (bytes) */
    public FileAllocationTable(byte[] bytes, int totalRecorderBytes) {
        System.arraycopy(bytes, 0, _data, 0, TABLE_BYTES);
        _totalRecorderBytes = totalRecorderBytes;
    }

    /** Return current number of non-empty files */
    public int nFiles() {
        int nfiles = 0;
        for (int row = 0; row < NROWS; row++) {
            int rowStart = row * ROW_LENGTH;
            // Replace null bytes in file name with blanks
            for (int i = rowStart; i < rowStart+6; i++) {
                if (_data[i] != 0 && _data[i] != ' ' && usedBytes(row) > 0) {
                    nfiles++;
                    break;
                }
            }
        }
        return nfiles;
    }


    /** Return number of bytes used by all files */
    public long usedBytes() {
        long totalBytes = 0;
        for (int row = 0; row < NROWS; row++) {

            totalBytes += usedBytes(row);
        }
        return totalBytes;
    }

    /** Return number of bytes used by specified file; fileNo must be in range 0
     * through 31. */
    public long usedBytes(int fileNo) {

        if (fileNo < 0 || fileNo >= NROWS) {
            return 0;
        }

        if (empty(fileNo)) {
            return 0;
        }

        int rowStart = fileNo * ROW_LENGTH;

        byte[] intBytes = new byte[4];

        System.arraycopy(_data, rowStart+8, intBytes, 0, 4);
        int startAddr = DataStructure.getInteger(intBytes);

        System.arraycopy(_data, rowStart+12, intBytes, 0, 4);
        int endAddr = DataStructure.getInteger(intBytes);

        _log4j.debug("usedBytes() - startAddr=" + startAddr +
                ", endAddr=" + endAddr);

        return (endAddr - startAddr);
    }


    /** Return the file storage capcacity, given specified total recorder 
     size */
    int capacityBytes() {
        byte[] intBytes = new byte[4];
        System.arraycopy(_data, 8, intBytes, 0, 4);
        return _totalRecorderBytes - DataStructure.getInteger(intBytes);
    }




    /** Return true if specified row in table is occupied, else return false. 
     */
    protected boolean empty(int row) {

        int rowStart = row * ROW_LENGTH;

        // Look for non-null entry in name field
        for (int i = rowStart; i < rowStart+6; i++) {
            if (_data[i] != 0) {
                // There is an acual entry
                return false;
            }
        }
        // Name field is empty
        return true;
    }


    /** Return string representation of FAT */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int row = 0; row < NROWS; row++) {

            if (empty(row)) {
                buf.append("(Empty)\n");
                continue;
            }

            int rowStart = row * ROW_LENGTH;

            // File name with sequence appended
            buf.append(new String(_data, rowStart, 6));
            buf.append(" ,");

            if ((_data[rowStart + 7] & 1) > 0) {
                // Wrapped at least once
                buf.append("W1");
                if ((_data[rowStart + 7] & 2) > 0) {
                    // Wrapped twice
                    buf.append(",W2");
                }
                buf.append(",");
            }
            else {
                buf.append("NW,");
            }


            byte[] intBytes = new byte[4];

            System.arraycopy(_data, rowStart+8, intBytes, 0, 4);
            int addr = DataStructure.getInteger(intBytes);
            buf.append("  x" + Integer.toHexString(addr));

            System.arraycopy(_data, rowStart+12, intBytes, 0, 4);
            addr = DataStructure.getInteger(intBytes);
            buf.append("  x" + Integer.toHexString(addr));

            buf.append("\n");
        }

        // Summarize storage
        buf.append("recorder size: " + _totalRecorderBytes);
        buf.append("\ncapacity: " + capacityBytes());
        buf.append("\nused: " + usedBytes());
        buf.append("\navailable: " + (capacityBytes() - usedBytes()) + "\n");

        return new String(buf);
    }
}

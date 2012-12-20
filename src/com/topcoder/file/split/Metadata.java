/*
 * Metadata.java
 *
 * Copyright © 2003, TopCoder, Inc. All rights reserved
 */
package com.topcoder.file.split;

import java.io.*;

/**
 * This is a final class for encapsulating metadata information used by the file
 * splitter and joiner components.  It consists of data members for the
 * information stored in the header, and has methods for writing itself to an
 * output stream and initializing itself by reading metadata from an input
 * stream.
 * <p>
 * The following is an excerpt of the metadata header format:
 * <p>The first split file also contains a header with metadata:</p>
 * <ul>
 *  <li>Signature identifying this as a split file: the four character
 *   sequence "SPLT" encoded with UTF-8 encoding (four bytes)</li>
 *  <li>Major and minor format version (each one unsigned byte)</li>
 *  <li>Length of the rest of the header (unsigned big-endian four-byte value)</li>
 *  <li>Length of original file name (unsigned big-endian two-byte value)</li>
 *  <li>Original file name without path (UTF-8 encoding)</li>
 *  <li>Length of the split format string (unsigned big-endian two-byte value)</li>
 *  <li>Split format string (UTF-8 encoding)</li>
 *  <li>Number of split files (unsigned big-endian two-byte value)</li>
 * </ul>
 *
 * @author npe
 * @version 1.0
 * @see FileSplitter, FileJoiner
 */
public final class Metadata {
    static final String SIGNATURE = "SPLT";
    private static byte[] signatureBytes;

    static {
        // We can't use writeUTF() for the signature, so read it as a byte
        // array.
	signatureBytes = SIGNATURE.getBytes();
    }

    private short majorVersion;
    private short minorVersion;
    private String originalFileName;
    private String splitFileFormatString;
    private int numSplitFiles;

    /**
     * @return The metadata format major version number.
     */
    public short getMajorVersion() {
        return majorVersion;
    }

    /**
     * @param majorVersion Sets the metadata format major version number.
     */
    public void setMajorVersion(short majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * @return The metadata format minor version number.
     */
    public short getMinorVersion() {
        return minorVersion;
    }

    /**
     * @param minorVersion Sets the metadata format minor version number.
     */
    public void setMinorVersion(short minorVersion) {
        this.minorVersion = minorVersion;
    }

    /**
     * @return The name of the original file.
     */
    public String getOriginalFileName() {
        return originalFileName;
    }

    /**
     * @param originalFileName The name of the original file.
     */
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    /**
     * @return The split file format string, which is the pattern used in a
     * <code>MessageFormat</code> object.  This string must contain <code>{0}
     * </code> and <code>{1}</code> placeholders for formatting, although that
     * is not validated here.
     */
    public String getSplitFileFormatString() {
        return splitFileFormatString;
    }

    /**
     * @param splitFileFormatString The split file format string, which is the
     * pattern used in a <code>MessageFormat</code> object.  This string must
     * contain <code>{0}</code> and <code>{1}</code> placeholders for
     * formatting, although that is not validated here.
     */
    public void setSplitFileFormatString(String splitFileFormatString) {
        this.splitFileFormatString = splitFileFormatString;
    }

    /**
     * @return The number of split files.
     */
    public int getNumSplitFiles() {
        return numSplitFiles;
    }

    /**
     * @param numSplitFiles The number of split files.
     */
    public void setNumSplitFiles(int numSplitFiles) {
        this.numSplitFiles = numSplitFiles;
    }

    /**
     * @return The size of this metadata object in number of bytes.  This value
     * is the length of the signature and format version, plus the length of the
     * remaining variable length data header.
     */
    public long getSize() throws IOException {
        int length = 4; // Signature.
        length += 1; // Format major version.
        length += 1; // Format minor version.
        length += 4; // Size of variable length data.
        return length + getVariableDataSize();
    }

    /**
     * @return The size of the remaining variable length data header.  This
     * header follows the signature and format version bytes.
     */
    private long getVariableDataSize() throws IOException {
        long length = 0;

        int originalFileNameLength = 0;
        if (originalFileName != null) {
            originalFileNameLength = originalFileName.length();
        }
        length += 2; // Length of original file name.
        length += originalFileNameLength; // Original file name.

        int splitFileFormatStringLength = 0;
        if (splitFileFormatString != null) {
            splitFileFormatStringLength =
                    splitFileFormatString.length();
        }
        length += 2; // Length of split format string.
        length += splitFileFormatStringLength; // Split format string.

        length += 2; // Number of split files.

        return length;
    }

    /**
     * Writes this metadata object as a formatted series of bytes to an output
     * stream.  The format is described in the JavaDocs for this class.  This
     * is used to write a header to the first split file.
     *
     * @param os The output stream to write the metadata header to.
     * @throws IOException If an error occurs while writing the metadata header.
     * @see FileSplitter
     */
    public void write(final OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        // Write the signature.
        dos.write(signatureBytes);

        // Write the format version.
        dos.writeByte(majorVersion);
        dos.writeByte(minorVersion);

        // Write the remaining header length.
        long remainingHeaderLength = getVariableDataSize();
        dos.writeInt((int) remainingHeaderLength);

        // Write the original file name.
	dos.writeShort((short )originalFileName.length());
        dos.write(originalFileName.getBytes(), 0, originalFileName.length());

        // Write the format string.
	dos.writeShort((short )splitFileFormatString.length());
        dos.write(splitFileFormatString.getBytes(), 0, splitFileFormatString.length());

        // Write the number of split files.
        dos.writeShort(numSplitFiles);
    }

    /**
     * Initializes this metadata object by reading a series of bytes from an
     * input stream.  As the bytes are read they are validated to ensure that
     * they conform to the metadata format, which is described in the JavaDocs
     * for this class.  This method is used to read metadata information from
     * the first split file in order to join the split files together.
     *
     * @param is The input stream containing the metadata header.
     * @throws IOException If an error occurs while reading the metadata.
     * @throws InvalidSplitFileHeaderException If the metadata read doesn't
     * conform to the split file format.
     * @see FileJoiner
     */
    public void read(final InputStream is)
            throws IOException, InvalidSplitFileHeaderException {
        DataInputStream dis = new DataInputStream(is);

        // Read the header signature.
        byte[] signatureBytes = new byte[4];
        dis.read(signatureBytes);
        String signature = new String(signatureBytes);
        if (!Metadata.SIGNATURE.equals(signature)) {
            throw new InvalidSplitFileHeaderException(
                    "Invalid header signature: " + signature);
        }

        // Read the format version.
        short majorVersion = (short) dis.readUnsignedByte();
        if (majorVersion != 1) {
            throw new InvalidSplitFileHeaderException(
                    "Major version not '1': " + majorVersion);
        }
        setMajorVersion(majorVersion);
        short minorVersion = (short) dis.readUnsignedByte();
        setMinorVersion(minorVersion);

        // Read the length of the rest of the header.
        long headerLength = dis.readInt() & 0xffffffff;
        if (headerLength < 0) {
            throw new InvalidSplitFileHeaderException(
                    "Negative remaining header length: " + headerLength);
        }

        // Read the remaining header into a separate byte array.
        byte[] remainingHeaderBytes = new byte[(int) headerLength];
        dis.read(remainingHeaderBytes);
        dis = 
	    new DataInputStream(new ByteArrayInputStream(remainingHeaderBytes));
        int bytesRead = 0;

	byte[] buf = new byte[256];

        // Read the original file name.
	short len = dis.readShort();
	dis.read(buf, 0, len);
        String originalFileName = new String(buf, 0, len);
        System.out.println("len=" + len + ", originalFileName=" + originalFileName);
        bytesRead += 2 + len;
        setOriginalFileName(originalFileName);

        // Read the split format string.
	len = dis.readShort();
	dis.read(buf, 0, len);
        String splitFileFormatString = new String(buf, 0, len);
        System.out.println("splitFileFormatString=" + splitFileFormatString);
        bytesRead += 2 + len;
        setSplitFileFormatString(splitFileFormatString);

        // Read the number of split files.
        int numSplitFiles = dis.readUnsignedShort();
        System.out.println("numSplitFiles=" + numSplitFiles);
        bytesRead += 2;
        setNumSplitFiles(numSplitFiles);

        // Check for unread data that indicates a bad header.
        if (bytesRead != headerLength) {
            throw new InvalidSplitFileHeaderException(
                    "Header is too long: headerLength=" + headerLength + 
		    ", bytesRead=" +  bytesRead);
        }
    }
}

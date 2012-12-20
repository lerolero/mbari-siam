/*
 * FileSplitter.java
 *
 * Copyright © 2003, TopCoder, Inc. All rights reserved
 */
package com.topcoder.file.split;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;

/**
 * <p>This class facilitates splitting a file into multiple smaller files.
 * This may be useful when an application must store or transmit a very
 * large file, and storage or transmission of a single large file is
 * undesirable or impossible.</p>
 *
 * <p>Methods here provide a means to:</p>
 * <ul>
 *  <li>split a file into a fixed number of smaller files</li>
 *  <li>split a file into files of a given size</li>
 * </ul>
 *
 * <p>The pieces can be rejoined with a FileJoiner object. In addition,
 * one can specify the format of the filename for each of the
 * generated smaller files, and also the directoy in which they are
 * created.</p>
 *
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
 * @author srowen
 * @author npe
 * @version 1.0
 */
public class FileSplitter {
    /** A custom buffer size of 100Kb is used for splitting files. */
    private static final int BUF_SIZE = 100000;

    private byte[] buffer;
    private File originalFile;
    private File splitFileDirectory;
    private MessageFormat splitFileNameFormat;

    /** Default split file name message format: <code>{0}.{1}</code>. */
    public static final MessageFormat DEFAULT_SPLIT_FILE_NAME_FORMAT =
            new MessageFormat("{0}.{1}");

    /**
     * <p>Creates a FileSplitter for splitting the given File. Split files
     * are created in the same directory as the given file. The split files
     * are named according to the pattern
     * <code>DEFAULT_SPLIT_FILE_NAME_FORMAT</code>.
     *
     * <p>An example should clarify - this FileSplitter:</p>
     * <p><pre>
     * FileSplitter splitter = new FileSplitter(new File("bigfile"));
     * </pre></p>
     * <p>will produce files like "bigfile.000000",
     * "bigfile.000001", and so on, in the same directory as "bigfile".</p>
     *
     * @param originalFile original file to split
     * @throws IllegalArgumentException if argument is null
     */
    public FileSplitter(final File originalFile) {
        if (originalFile == null) {
            throw new IllegalArgumentException(
                    "originalFile is invalid; it must be non-null.");
        }
        this.originalFile = originalFile;
        this.splitFileDirectory = getSplitFileDirectory(originalFile);
        this.splitFileNameFormat =
                (MessageFormat) DEFAULT_SPLIT_FILE_NAME_FORMAT.clone();
    }

    /**
     * For an original split file, this method returns the parent directory, or
     * if the parent directory is null, the current execution directory as
     * returned by the value of the JVM system property "user.dir".
     *
     * @param originalFile The original split file.
     * @return The split file directory where split files will be written to.
     */
    private static File getSplitFileDirectory(File originalFile) {
        File splitFileDirectory = originalFile.getParentFile();
        if (splitFileDirectory == null) {
            splitFileDirectory = new File(System.getProperty("user.dir"));
        }
        return splitFileDirectory;
    }

    /**
     * <p>Creates a FileSplitter for splitting the given File. This
     * constructor also allows the caller to specify the directory in
     * which the split files are created.</p>
     *
     * <p>It also allows the caller to specify the format of the filename
     * using a MessageFormat object. The format's pattern <em>must</em>
     * include a "{0}" argument placeholder for the split file's base
     * name (name of the joined file), and a "{1}" argument placeholder
     * for the index of the split file. This placeholder will be filled
     * with the index of the split file, a number between 0 and one less
     * than the total number of split files, padded with zeroes so that
     * the string is six characters. That is, the index is of the form
     * "000000", "000001", etc.</p>
     *
     * <p>An example should clarify - this FileSplitter:</p>
     * <p><pre>
     * MessageFormat format = new MessageFormat("{0}.part{1}");
     * FileSplitter splitter = new FileSplitter(new File("bigfile"),
     *                                          new File("/tmp"),
     *                                          format);
     * </pre></p>
     * <p>will produce files like "/tmp/bigfile.part000000",
     * "/tmp/bigfile.part000001", and so on.</p>
     *
     * @param originalFile original file to split
     * @param splitFileDirectory directory in which to place split files
     * @param splitFileNameFormat format of split file names
     * @throws IllegalArgumentException if any argument is null, or if the
     *  "splitFileNameFormat" argument does not contain both a
     *  "{0}" and "{1}" argument placeholder.
     * @see java.text.MessageFormat
     */
    public FileSplitter(final File originalFile,
                        final File splitFileDirectory,
                        final MessageFormat splitFileNameFormat) {
        if (originalFile == null) {
            throw new IllegalArgumentException(
                    "originalFile is invalid; it must be non-null.");
        }
        this.originalFile = originalFile;

        if (splitFileDirectory == null) {
            throw new IllegalArgumentException("splitFileDirectory is null.");
        }
        this.splitFileDirectory = splitFileDirectory;

        if (!isValidSplitFileNameFormat(splitFileNameFormat)) {
            throw new IllegalArgumentException(
                    "splitFileNameFormat must be non-null and contain '{0}' and " +
                    "'{1}'.");
        }
        this.splitFileNameFormat = (MessageFormat) splitFileNameFormat.clone();
    }

    /**
     * Tests the validity of a split file <code>MessageFormat</code> formatter.
     * The pattern used by the formatter *must* contain <code>{0}</code> and
     * </code>{1}</code> placeholders.
     *
     * @param messageFormat The formatter object to check.
     * @return True if the formatter is valid, otherwise false.
     */
    private static boolean isValidSplitFileNameFormat(
            MessageFormat messageFormat) {
        if (messageFormat != null) {
            String pattern = messageFormat.toPattern();
            if (pattern.indexOf("{0}") != -1 && pattern.indexOf("{1}") != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return File object representing original file
     */
    public File getOriginalFile() {
        return originalFile;
    }

    /**
     * @return File object representing directory holding split files
     */
    public File getSplitFileDirectory() {
        return splitFileDirectory;
    }

    /**
     * @return MessageFormat encapsulating the split file name format
     */
    public MessageFormat getSplitFileNameFormat() {
        return (MessageFormat) splitFileNameFormat.clone();
    }


    /**
     * <p>Splits the joined file's data into files of the given size.</p>
     *
     * <p>The first split file has a header with metadata; see the class
     * javadoc.</p>
     *
     * <p>For example, if a 9,000 byte file is split into files of 3,000
     * bytes, then <em>four</em> files will be created. The first will
     * contain a header, and something less than the first 3,000 bytes of
     * the file, for a total of 3,000 bytes. The next two will have 3,000
     * bytes of data each, and the final file will have whatever data is
     * leftover.
     *
     * <p>If an error occurs while splitting the file, and not all split files
     * can be created successfully, this method attempts to delete all the
     * split files that were created.</p>
     *
     * @throws IllegalArgumentException if file size is nonpositive
     * @throws IOException if an exception occurs while reading/writing file
     *  data, or if the one of the split file already exists or is not
     *  writable for any reason
     */
    public File[] splitByFileSize(final long fileSize) throws IOException {
        if (fileSize < 1) {
            throw new IllegalArgumentException(
                    "Non-positive file size: " + fileSize);
        }

        // Create the split file metadata to write.
        Metadata metadata = createMetadata();

        // Open the original file and determine its size.
        RandomAccessFile raf = new RandomAccessFile(originalFile, "r");
        List splitFiles = new ArrayList();

        try {
            long originalFileSize = raf.length();

            // Determine the number of split files there will be.
            long metadataSize = metadata.getSize();
            int numSplitFiles;
            if (fileSize > metadataSize) {
                numSplitFiles = (int) Math.ceil(
                        (float) (originalFileSize + metadataSize) / fileSize);
            } else {
                // If the header is larger or equal to the max file size, then
                // write the header only and no data to the first split file.
                // This means that the first split file will be larger than the
                // given maximum. The remaining split files should respect the
                // maximum file size.
                numSplitFiles = 1 + (int) Math.ceil((
                        (float) originalFileSize) / fileSize);
            }
            metadata.setNumSplitFiles(numSplitFiles);

            // Split the files.
            for (int i = 0; i < numSplitFiles; ++i) {
                // Determine the name of the next split file and create it.
                String formattedIndex = Utils.formatIndex(i);
                Object[] arguments = new Object[]{
                    originalFile.getName(), formattedIndex};
                String fileName = splitFileNameFormat.format(arguments);
                File outputFile = new File(splitFileDirectory, fileName);
                if (outputFile.exists()) {
                    throw new IOException(
                            "Split file already exists: " + outputFile);
                }
                splitFiles.add(outputFile);

                FileOutputStream fos = new FileOutputStream(outputFile);

                try {
                    if (i == 0) {
                        // If this is the first split file, write the metadata
                        // header.  Then copy the file data after the header.
                        metadata.write(fos);

                        // Only write data to the first file if the max file
                        // size is larger than the header size.
                        if (fileSize > metadataSize) {
                            long amountToWrite =
                                    Math.min(fileSize - metadataSize,
                                             raf.length() -
                                             raf.getFilePointer());
                            writeToFile(fos, raf, amountToWrite);
                        }
                    } else {
                        // Otherwise just copy file data into the new split
                        // file.  Keep track of whether the byte count is
                        // actually less than the file size as a result of the
                        // ceiling calculation.
                        long amountToWrite =
                                Math.min(fileSize,
                                         raf.length() - raf.getFilePointer());
                        writeToFile(fos, raf, amountToWrite);
                    }
                } finally {
                    fos.close();
                }
            }
        } catch (IOException ioe) {
            delete(splitFiles);
            throw ioe;
        } catch (RuntimeException re) {
            delete(splitFiles);
            throw re;
        } finally {
            raf.close();
        }

        File[] splitFilesArray = new File[splitFiles.size()];
        return (File[]) splitFiles.toArray(splitFilesArray);
    }

    /**
     * Writes data from the original file to a split file.  This method works
     * similarly to <code>BufferedOutputStream</code> and uses a custom byte
     * buffer so that it can write to the split file in chunks,
     * improving performance.  The optimal size of this buffer, in terms of the
     * amount of time required to split the original file, has been empirically
     * determined to be around 100Kb.  It is also unsynchronized and doesn't
     * copy arrays, improving performance over <code>BufferedOutputStream</code>
     * Custom buffering can provide up to a 15% speed boost over, which is
     * especially noticeable when splitting very large files.  The buffer itself
     * is created once on-demand and reused, so as to avoid wasteful and
     * expensive object creation.
     * @param fosSplitFile The file output stream for the split file.
     * @param rafOriginalFile The random access file object for the original
     * file.
     * @param amountToWrite The amount of data to write from the original file
     * to the split file.
     * @throws IOException If an error occurs writing to the split file from the
     * original file.
     */
    private void writeToFile(final FileOutputStream fosSplitFile,
                             final RandomAccessFile rafOriginalFile,
                             long amountToWrite)
            throws IOException
    {
        if (buffer == null) {
            buffer = new byte[BUF_SIZE];
        }

        while (amountToWrite > 0) {
            int amountRead =
                    rafOriginalFile.read(buffer, 0,
                             (int) Math.min(amountToWrite, BUF_SIZE));
            fosSplitFile.write(buffer, 0, amountRead);
            amountToWrite -= amountRead;
        }
    }

    /**
     * Creates a <code>Metadata</code> object for splitting the original file.
     * This object encapsulates the header information that is included with the
     * first split file.
     *
     * @return A <code>Metadata</code> object containing the metadata for the
     * first split file.
     * @see Metadata
     */
    private Metadata createMetadata() {
        Metadata metadata = new Metadata();
        metadata.setMajorVersion((short) 1);
        metadata.setMinorVersion((short) 0);
        metadata.setOriginalFileName(originalFile.getName());
        metadata.setSplitFileFormatString(splitFileNameFormat.toPattern());
        return metadata;
    }

    /**
     * <p>Splits the file data into the given number of files.</p>
     *
     * <p>The first split file has a header with metadata; see the class
     * javadoc.</p>
     *
     * <p>Calling this method is like calling splitByFileSize with
     * fileSize = ceiling( (total file data in bytes) / numFiles).</p>
     *
     * <p>The same amount of file data is contained in each file; all but
     * the last file has ceiling( (total file data in bytes) / numFiles)
     * bytes of data, and the last has the remainder. In addition, the
     * first file will be slightly larger because of the header.</p>
     *
     * <p>For example, splitting a 10,000 byte file into three files will
     * yield three files; two will have 3,334 bytes of file data (and the
     * first will also have header data, and the last will have
     * 3,332 bytes.</p>
     *
     * <p>If an error occurs while splitting the file, and not all split files
     * can be created successfully, this method attempts to delete all the
     * split files that were created.</p>
     *
     * @throws IllegalArgumentException if number of files is nonpositive
     * @throws IOException if an exception occurs while reading/writing file
     *  data, or if the one of the split file already exists or is not
     *  writable for any reason
     */
    public File[] splitByNumFiles(final int numFiles) throws IOException {
        if (numFiles < 1) {
            throw new IllegalArgumentException(
                    "Non-positive number of files: " + numFiles);
        }

        // Create the split file metadata to write.
        Metadata metadata = createMetadata();
        metadata.setNumSplitFiles(numFiles);

        // Open the original file and determine its size.
        RandomAccessFile raf = new RandomAccessFile(originalFile, "r");
        List splitFiles = new ArrayList(numFiles);

        try {
            long originalFileSize = raf.length();

            if(numFiles > originalFileSize) {
                throw new IllegalArgumentException("Number of split files, " +
                    numFiles + ", is greater than original file size, " +
                        originalFileSize + ".");
            }

            // Determine the size of each split file.
            long fileSize =
                    (long) Math.ceil(originalFileSize / (double) numFiles);

            // Split the files.
            for (int i = 0; i < numFiles; ++i) {
                // Determine the name of the next split file and create it.
                String formattedIndex = Utils.formatIndex(i);
                Object[] arguments = new Object[]{
                    originalFile.getName(), formattedIndex};
                String fileName = splitFileNameFormat.format(arguments);
                File outputFile = new File(splitFileDirectory, fileName);
                if (outputFile.exists()) {
                    throw new IOException(
                            "Split file already exists: " + outputFile);
                }
                splitFiles.add(outputFile);

                FileOutputStream fos = new FileOutputStream(outputFile);

                try {
                    if (i == 0) {
                        // If this is the first split file, write the metadata
                        // header.  Then copy the file data after the header.
                        metadata.write(fos);
                        writeToFile(fos, raf, fileSize);
                    } else {
                        // Otherwise just copy file data into the new split
                        // file.  Keep track of whether the byte count is
                        // actually less than the file size as a result of the
                        // ceiling calculation.
                        long amountToWrite =
                                Math.min(fileSize,
                                         raf.length() - raf.getFilePointer());
                        writeToFile(fos, raf, amountToWrite);
                    }
                } finally {
                    fos.close();
                }
            }
        } catch (IOException ioe) {
            delete(splitFiles);
            throw ioe;
        } catch (RuntimeException re) {
            delete(splitFiles);
            throw re;
        } finally {
            raf.close();
        }

        File[] splitFilesArray = new File[splitFiles.size()];
        return (File[]) splitFiles.toArray(splitFilesArray);
    }

    /**
     * Deletes a list of split files.  This method is used to delete split files
     * if an exception occurs during the splitting process.
     *
     * @param splitFiles A list of <code>java.io.File</code> objects to be
     * deleted.  There should be no open streams associated with the files to
     * be deleted.
     */
    private static void delete(List splitFiles) {
        for (Iterator iter = splitFiles.iterator(); iter.hasNext();) {
            File splitFile = (File) iter.next();
            if (splitFile.exists() && !splitFile.delete()) {
                System.err.println("Unable to delete split file: " + splitFile);
            }
        }
    }
}
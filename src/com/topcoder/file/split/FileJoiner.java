/*
 * FileJoiner.java
 *
 * Copyright 2003, TopCoder, Inc. All rights reserved
 */
package com.topcoder.file.split;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * <p>This class facilitates rejoining pieces of a file (split files) together
 * to recreate the original file. Methods here provide a means to join
 * multiple files created by a FileSplitter back into one file.</p>
 *
 * @author srowen
 * @author npe
 * @version 1.0
 */
public class FileJoiner {
    /** A custom buffer size of 100Kb is used for splitting files. */
    private static final int BUF_SIZE = 100000;

    private byte[] buffer;
    private final File firstSplitFile;

    /**
     * <p>Creates a FileJoiner from the given first split file.</p>
     *
     * @throws IllegalArgumentException if the first split file argument
     *  is null
     */
    public FileJoiner(final File firstSplitFile) {
        if (firstSplitFile == null) {
            throw new IllegalArgumentException("firstSplitFile is null");
        }
        this.firstSplitFile = firstSplitFile;
    }

    /**
     * @return first split file used by this object
     */
    public File getFirstSplitFile() {
        return firstSplitFile;
    }

    /**
     * <p>This method joins smaller files into one large file.</p>
     *
     * <p>It reads metadata about the split files (name of the original
     * file, number of split files, format of their names, etc.) from the
     * first split file given to this FileJoiner object.</p>
     *
     * <p>A file is created with the same name as the original file
     * (read from the metadata); all the split files are located, and their
     * data is put into the joined file, in order.</p>
     *
     * <p>If an error occurs while joining the file, this method attempts to
     * delete the partially joined file.</p>
     *
     * @return joined file
     * @throws InvalidSplitFileHeaderException if the first split file's
     * header is not valid
     * @throws FileNotFoundException if the given first split file does
     * not exist
     * @throws IOException if an exception occurs while reading/writing file
     * data, or if the join file already exists or is not writable for any
     * reason
     */
    public File join() throws IOException, InvalidSplitFileHeaderException {
        // Read metadata from the first split file.
        Metadata metadata = readMetadata();

        // Create the join file.
	File parent = firstSplitFile.getParentFile();
	if (parent == null) {
	    parent = new File(".");
	}
        String filePath = parent.getAbsolutePath();
        File joinFile = new File(filePath, metadata.getOriginalFileName());
        if (joinFile.exists()) {
            throw new IOException("Join file already exists: " + joinFile);
        }

        if (buffer == null) {
            buffer = new byte[BUF_SIZE];
        }

        FileOutputStream fos = new FileOutputStream(joinFile);

        // Join the split files.  Use FileChannel#transferTo() to copy the data
        // from the split files into the join file because it can be very fast
        // by reading directly from the filesystem cache.
        try {
            long metadataSize = metadata.getSize();
            File[] expectedSplitFiles = getExpectedSplitFiles();
			       
            for (int i = 0; i < expectedSplitFiles.length; ++i) {
		System.out.println("Appending segment " + 
				   expectedSplitFiles[i].getName());
                RandomAccessFile raf =
                        new RandomAccessFile(expectedSplitFiles[i], "r");
                try {
                    if (i == 0) {
                        // If this is the first split file, it contains the
                        // metadata.  Start joining actual data after that.
                        raf.seek(metadataSize);
                        long amountToWrite = raf.length() - metadataSize;
                        writeToFile(fos, raf, amountToWrite);
                    } else {
                        // Otherwise keep copying data into the join file.
                        long amountToWrite = raf.length();
                        writeToFile(fos, raf, amountToWrite);
                    }
                } finally {
                    raf.close();
                }
            }
        } catch (IOException ioe) {
            cleanup(fos, joinFile);
            throw ioe;
        } catch (InvalidSplitFileHeaderException isfhe) {
            cleanup(fos, joinFile);
            throw isfhe;
        } catch (RuntimeException re) {
            cleanup(fos, joinFile);
            throw re;
        } finally {
            fos.close();
        }

        return joinFile;
    }

    /**
     * This method is called when an exception occurs during the join process.
     * It closes the output stream and deletes the join file.
     *
     * @param fos The file output stream for the join file to be closed.
     * @param joinFile The join file to be deleted.
     */
    private void cleanup(FileOutputStream fos, File joinFile) {
        try {
            fos.close();
        } catch (IOException ioe) {
            System.err.println("Couldn't close join file output stream:" +
                               ioe.getMessage());
        }
        if (!joinFile.delete()) {
            System.err.println("Unable to delete join file: " + joinFile);
        }
    }

    /**
     * <p>Returns the split files that are expected by this object, and that
     * will be joined, by parsing the header in the first split file.</p>
     *
     * @throws InvalidSplitFileHeaderException if the first split file's
     *  header is not valid
     * @throws IOException if an error occurs while trying to determine what
     *  split files are expected
     * @return the split files that are expected by this object
     */
    public File[] getExpectedSplitFiles()
            throws IOException, InvalidSplitFileHeaderException {
        // Read the metadata from the first split file.
        Metadata metadata = readMetadata();

        // Create the list of expected split files.
        MessageFormat splitFileNameFormat =
                new MessageFormat(metadata.getSplitFileFormatString());

	File parent = firstSplitFile.getParentFile();
	if (parent == null) {
	    parent = new File(".");
	}
        String filePath = parent.getAbsolutePath();
        List expectedFiles = new ArrayList();
        int numSplitFiles = metadata.getNumSplitFiles();
        for (int i = 0; i < numSplitFiles; ++i) {
            String splitFileIndex = Utils.formatIndex(i);
            Object[] arguments = new Object[]{
                metadata.getOriginalFileName(), splitFileIndex};
            String fileName = splitFileNameFormat.format(arguments);

            File file = new File(filePath, fileName);
            expectedFiles.add(file);
        }
        File[] expectedFilesArray = new File[expectedFiles.size()];
        return (File[]) expectedFiles.toArray(expectedFilesArray);
    }

    /**
     * Reads the metadata from the first split file and populates a metadata
     * object that can be used to determine how to join the split files
     * together.
     *
     * @return A <code>Metadata</code> object containing the metadata from the
     * first split file.
     * @throws IOException If the header metadata can't be read from the first
     * split file.
     * @throws InvalidSplitFileHeaderException If the split file header metadata
     * is invalid for any reason.
     * @see Metadata
     */
    private Metadata readMetadata()
            throws IOException, InvalidSplitFileHeaderException {
        // Read the metadata from the first split file.
        Metadata metadata = new Metadata();
        FileInputStream fis = new FileInputStream(firstSplitFile);
        try {
            metadata.read(fis);
        } finally {
            fis.close();
        }
        return metadata;
    }

    /**
     * Writes data from a split file to the joined file.
     * <p>
     * This method works similarly to <code>BufferedOutputStream</code> and uses
     * a custom byte buffer so that it can write to the split file in chunks,
     * improving performance.  The optimal size of this buffer, in terms of the
     * amount of time required to split the original file, has been empirically
     * determined to be around 100Kb.  It is also unsynchronized and doesn't
     * copy arrays, improving performance over <code>BufferedOutputStream</code>
     * Custom buffering can provide up to a 15% speed boost over, which is
     * especially noticeable when splitting very large files.  The buffer itself
     * is created once on-demand and reused, so as to avoid wasteful and
     * expensive object creation.
     * @param fosJoinFile The file output stream for the join file.
     * @param rafSplitFile The random access file object for the split
     * file.
     * @param amountToWrite The amount of data to write from the split file
     * to the join file.
     * @throws IOException If an error occurs writing to the join file from the
     * split file.
     */
    private void writeToFile(final FileOutputStream fosJoinFile,
                             final RandomAccessFile rafSplitFile,
                             long amountToWrite)
            throws IOException
    {
        if (buffer == null) {
            buffer = new byte[BUF_SIZE];
        }

        while (amountToWrite > 0) {
            int amountRead =
                    rafSplitFile.read(buffer, 0,
                             (int) Math.min(amountToWrite, BUF_SIZE));
            fosJoinFile.write(buffer, 0, amountRead);
            amountToWrite -= amountRead;
        }
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import gnu.io.SerialPort;
import org.apache.log4j.Logger;
import org.mbari.puck.Puck;
import org.mbari.puck.Payload;
import com.Ostermiller.util.MD5;

/**
   PuckUtils is a utility class that includes static constants and methods 
   for writing and reading SIAM payloads from PUCKs.
 */
public class PuckUtils {

    private static Logger _log4j = Logger.getLogger(PuckUtils.class);

    /** This is the name of the file that defines correspondence between
	PUCK UUID and SIAM service */
    public static final String PUCK_REGISTRY_NAME = 
        System.getProperty("siam_home") + File.separator + "properties" + 
	File.separator + "puckRegistry";


    /** Read SIAM payload from PUCK on specified serial port and write to
	specified output file. */
    public static void readSiamPayload(Puck puck,
				       String outputFileName) 
	throws IOException, Exception {

	puck.setPuckMode(5);

	int baud = puck.setFastBaud();
        _log4j.info("readSiamPayload():Puck baudrate set to " + baud + "\n");

	Payload.Descriptor payload = 
	    Payload.extract(puck, Payload.MBARI_SIAM, outputFileName);

	// Get checksum from payload tag
	byte[] md5Checksum = payload.getMd5Checksum();

	// Compute actual payload file checksum
	byte[] md5ChecksumCalc = MD5.getHash(new File(outputFileName));


	// Compare checksum from tag with actual payload file checksum
        for (int i = 0; i < md5Checksum.length; ++i) {
            if (md5Checksum[i] != md5ChecksumCalc[i]) {
		_log4j.error("Jar file MD5 mismatch");
		_log4j.error("Read MD5     : 0x"
			     + ByteUtility.bytesToHexString(md5Checksum) + 
			     "\n");
		_log4j.error("Computed MD5 : 0x"
			     + ByteUtility.bytesToHexString(md5ChecksumCalc) +
			     "\n");
		break;
	    }
	}

        _log4j.debug("readSiamPayload() completed\n");
        return;
    }


    /** Return SIAM payload parameters; return null if SIAM payload not found 
     */
    public static SiamPayloadParams readSiamPayloadParams(Puck puck) 
	throws IOException, Exception {

        // Skip the PUCK instrument datasheet
	puck.skipDatasheet();

	Payload.InputStream input = new Payload.InputStream(puck);

	Payload.Descriptor payload = null;
	boolean found = false;
	while ((payload = Payload.getNextDescriptor(input)) != null) {
	    if (payload.getType().equals(Payload.MBARI_SIAM)) {
		found = true;
		break;
	    }
	}

	input.close();

	if (!found) {
	    return null;
	}

	SiamPayloadParams params = new SiamPayloadParams();

	params._size = payload.getSize();
	params._md5Checksum = payload.getMd5Checksum();

	return params;
    }
    


    /** Return true if PUCK has payload. */
    public static boolean hasSiamPayload(Puck puck) 
	throws IOException, Exception {

	if (!puck.hasPayload()) {
	    return false;
	}

	return true;
    }


    /** SIAM payload parameters. */
    public static class SiamPayloadParams {

	/** PuckUtils is the only thing that can instantiate this. */
	protected SiamPayloadParams() {
	}

	/** Jarfile MD5 checksum */
	public byte[] _md5Checksum = new byte[16];

	/** Jarfile size (bytes) */
	public int _size = 0;
    }
}


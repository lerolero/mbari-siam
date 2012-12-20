/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import org.mbari.siam.distributed.jddac.xml.MutableIntegerArrayTypeConverter;
import org.mbari.siam.distributed.jddac.xml.MutableDoubleArrayTypeConverter;
import org.mbari.siam.distributed.jddac.xml.DoubleTypeConverter;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.jmdi.comm.CoderBuffer;
import net.java.jddac.jmdi.comm.xml.ArgXmlCoder;
import net.java.jddac.jmdi.comm.xml.DefaultXmlCoder;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Vector;


/**
 * Encodes and decodes ArgArray and Vector objects into/from XML. This class
 * encapsulates the DefaultXmlCoder into a couple static methods. It provides
 * simple methods to convert a byte array of JDDAC Arg XML into objects and to
 * convert objects into a byte array of JDDAC Arg XML.
 *
 * @see net.java.jddac.jmdi.comm.xml.DefaultXmlCoder
 * @see net.java.jddac.jmdi.comm.xml.ArgXmlCoder
 * @see net.java.jddac.jmdi.comm.xml.ArgTypeConverter
 */
public class ArgXmlConverter {

    private static final Logger log = Logger.getLogger(ArgXmlConverter.class);
    private static final ArgXmlCoder coder = new ArgXmlCoder();

    static {
        coder.addObjectHandlers(new MutableDoubleArrayTypeConverter());
        coder.addObjectHandlers(new MutableIntegerArrayTypeConverter());
        coder.addObjectHandlers(new DoubleTypeConverter());
    }

    // Don't allow instances of this type, only static method calls.
    private ArgXmlConverter() {
    }


    /**
     * Encodes an ArgArray or Vector into JDDAC Arg XML.
     *
     * @param o -
     *          a ArgArray or Vector of JDDAC Arg objects.
     * @return a byte[] containing the JDDAC Arg XML represention of
     *         <code>o</code>.
     */
    public static byte[] encodeToXml(Object o) {
        CoderBuffer cb = new CoderBuffer();
        byte[] ret;

        // load ArgArray into Vector
        cb.setMsgList(o);

        // encode ArgArray into XML
        try {
            coder.encode(cb);
        }
        catch (Exception e) {
            log.error("Exception encoding xml", e);
            return null;
        }

        if (cb.getBufferLength() != cb.getByteBuffer().length) {
            ret = new byte[cb.getBufferLength()];
            System.arraycopy(cb.getByteBuffer(), cb.getBufferStart(), ret, 0,
                    ret.length);
        }
        else {
            ret = cb.getByteBuffer();
        }

        return ret;
    }

    /**
     * Decodes JDDAC Arg XML into an ArgArray or Vector.
     *
     * @param xml  -
     *             a byte[] containing the JDDAC Arg XML.
     * @param strt -
     *             the start of the XML in <code>xml</code>.
     * @param len  -
     *             the length of the XML in <code>xml</code>.
     * @return a ArgArray or Vector of Arg objects decoded from the XML.
     */
    public static Object decodeFromXml(byte[] xml, int strt, int len) {
        // set up the decoder
        DefaultXmlCoder cbf = new DefaultXmlCoder();
        CoderBuffer cb = new CoderBuffer();

        // decode XML into Objects
        try {
            cb.setByteBuffer(xml);
            cb.setBufferStart(strt);
            cb.setBufferLength(len);
            cbf.decode(cb);
        }
        catch (Exception e) {
            log.error("Exception decoding xml", e);
            return null;
        }

        return cb.getMsgList();
    }

    /**
     * Given an InputStream, deserialize the contents into an Object.
     *
     * @param fstream -
     *                the input stream.
     * @return A newly created object.
     */
    static Object loadObject(InputStream fstream) {
        Object content = null;
        byte buffer[] = new byte[20480];
        int bufLen = 0;

        // open file and read in XML
        try {
            int readSize = 0;
            do {
                readSize = fstream.read(buffer, bufLen, buffer.length - bufLen);
                if (readSize < 0) {
                    break;
                }
                bufLen += readSize;
                if (bufLen == buffer.length) {
                    byte[] newBuf = new byte[buffer.length * 2];
                    System.arraycopy(buffer, 0, newBuf, 0, bufLen);
                    buffer = newBuf;
                }
            }
            while (true);
            fstream.close();
        }
        catch (Exception e) {
            log.error("unable to parse contents(" + fstream + ")", e);
            return null;
        }

        // extract Vector from vector
        if (bufLen > 0) {
            content = decodeFromXml(buffer, 0, bufLen);
        }
        return content;
    }

    /**
     * Given an InputStream, deserialize the contents into an Vector.
     *
     * @param fstream -
     *                the input stream.
     * @return A newly created Vector.
     */
    public static Vector loadVector(InputStream fstream) {
        return (Vector) loadObject(fstream);
    }

    /**
     * Given an InputStream, deserialize the contents into an ArgArray.
     *
     * @param fstream -
     *                the input stream.
     * @return A newly created ArgArray.
     */
    public static ArgArray loadArgArray(InputStream fstream) {
        return (ArgArray) loadObject(fstream);
    }

    /**
     * Given an String, deserialize the contents into an Vector.
     *
     * @param xml -
     *            the string containing the xml.
     * @return A newly created Vector.
     */
    public static Vector loadVector(String xml) {
        if (xml == null) {
            return null;
        }
        byte[] xmlBytes = xml.getBytes();
        return (Vector) decodeFromXml(xmlBytes, 0, xmlBytes.length);
    }

    /**
     * Given an String, deserialize the contents into an ArgArray.
     *
     * @param xml -
     *            the string containing the xml.
     * @return A newly created ArgArray.
     */
    public static ArgArray loadArgArray(String xml) {
        if (xml == null) {
            return null;
        }
        byte[] xmlBytes = xml.getBytes();
        return (ArgArray) decodeFromXml(xmlBytes, 0, xmlBytes.length);
    }

}

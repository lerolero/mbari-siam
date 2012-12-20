/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac.xml;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.meas.MeasAttr;

import java.util.Enumeration;

/**
 * Encodes an {@link ArgArray} as XML. Numbers are formatted using upto 4
 * decimal places; longer values are rounded to conserve on transmission
 * bandwidth.
 *
 * @author Brian Schlining
 * @version $Id: XmlCoder.java,v 1.2 2012/12/17 21:35:49 oreilly Exp $
 */
public class XmlCoder extends Coder {
    
    private static NumberFormat numberFormat = new DecimalFormat("0.####");

    /**
     * Encodes an ArgArray as XML
     * 
     * @param argArray The argarray to encode
     * @return The ArgArray encoded as XML
     */
    public String encode(ArgArray argArray) {

        /*
         * Start tag for an ArgArray is <data>
         */
        StringBuffer sb = new StringBuffer("<data");

        /*
         * If the ArgArry has a 'name' attribute the start tag will be
         * <data n="theName">
         */
        Object name = argArray.get(MeasAttr.NAME);
        if (name == null) {
            sb.append(">");
        }
        else {
            sb.append(" n=\"").append(name).append("\">");
        }
        
        
        /*
         * Add other nodes to the data node. Non ArgArray values are added as
         *<pre><x n="elementName">A value</x></pre>
         */
        Enumeration e = argArray.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            Object value = argArray.get(key);
            
            // ArgArray values are handled recursively
            if (value instanceof ArgArray) {
                sb.append(encode((ArgArray) value));
            }
            else {
                String v;
                
                // Numbers are formatted for brevity
                if (value instanceof Number) {
                    v = numberFormat.format(value);
                }
                else {
                    v = value.toString();
                }
                sb.append("<x n=\"").append(key).append("\">").append(v).append("</x>");
            }
        }

        sb.append("</data>");
        return sb.toString();
    }

}

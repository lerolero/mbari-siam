/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac.xml;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Enumeration;

import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;

/**
 * A really stripped down XML encoder.
 * 
 * @author brian
 * 
 */
public class MinimalXmlCoder extends Coder {

    private static NumberFormat numberFormat = new DecimalFormat("0.####");

    public String encode(ArgArray argArray) { 
        
        StringBuffer sb = new StringBuffer();
        boolean isRecord = argArray instanceof Record;
        
        
        if (isRecord) {

            /*
             * Start tag for an ArgArray is <data>
             */
            sb = new StringBuffer("<r");
    
            /*
             * If the ArgArry has a timestamp add it.
             */
            Object time = argArray.get(MeasAttr.TIMESTAMP);
            if (time == null) {
                sb.append(">");
            }
            else {
                sb.append(" utc=\"").append(time).append("\">");
            }
        }
        
        /*
         * Add any measurement values.
         */
        String name = argArray.getString(MeasAttr.NAME);
        Object value = argArray.get(MeasAttr.VALUE);
        if (name != null && value != null) {
            String v;
            
            // Numbers are formatted for brevity
            if (value instanceof Number) {
                v = numberFormat.format(value);
            }
            else {
                v = value.toString();
            }
            sb.append("<m n=\"").append(name).append("\">").append(v).append("</m>");
        }

        /*
         * Add other nodes to the data node. ArgArrays are added recursively. Al
         * other fields are ignored except for
         */
        Enumeration e = argArray.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            value = argArray.get(key);

            // ArgArray values are handled recursively
            if (value instanceof ArgArray) {
                sb.append(encode((ArgArray) value));
            }
        }

        if (isRecord) {
            sb.append("</r>");
        }
        return sb.toString();
    }

}

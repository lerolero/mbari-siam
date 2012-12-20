/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * NumberFilter.java
 *
 * Created on April 10, 2006, 10:38 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.distributed.jddac;

import java.util.Enumeration;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;
import org.mbari.jddac.IFilter;

/**
 *
 * @author brian
 */
public class NumberFilter implements IFilter {
    
    private static final Logger log = Logger.getLogger(NumberFilter.class);
    
    /** Creates a new instance of MutableIntegerArrayFilter */
    public NumberFilter() {
    }

    public boolean accept(ArgArray argArray) {
        boolean accepted = true;
               
        Enumeration keys = argArray.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object value = argArray.get(key);
            if (value instanceof Measurement) {
                Object a = ((ArgArray) value).get(MeasAttr.VALUE);
                if (a == null || !(a instanceof Number)) {
                    accepted = false;
		    log.debug("Rejected " + argArray);
                    break;
                }
            }
        }
        
        return accepted;
    }
}

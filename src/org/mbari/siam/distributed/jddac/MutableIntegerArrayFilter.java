/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * MutableIntegerArrayFilter.java
 *
 * Created on April 10, 2006, 10:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.distributed.jddac;

import java.util.Enumeration;
import java.util.Vector;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;
import org.mbari.jddac.IFilter;

/**
 * This filter refuses if any Objects that are not Measurements with a VALUE of
 * a MutableIntegerArray are in the ArgArray. If the rem
 * @author brian
 */
public class MutableIntegerArrayFilter implements IFilter {
    
    /** Creates a new instance of MutableIntegerArrayFilter */
    public MutableIntegerArrayFilter() {
    }

    public boolean accept(ArgArray argArray) {
        boolean accepted = true;
               
        Enumeration keys = argArray.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object value = argArray.get(key);
            if (value instanceof Measurement) {
                Object a = ((ArgArray) value).get(MeasAttr.VALUE);
                if (a == null || !(a instanceof MutableIntegerArray)) {
                    accepted = false;
                    break;
                }
            }
        }
        
        return accepted;
    }
    
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * SiamRecordFilter.java
 *
 * Created on March 30, 2006, 4:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.distributed.jddac;

import org.mbari.siam.distributed.jddac.SiamRecord;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;
import org.mbari.jddac.IFilter;

import java.util.Enumeration;

/**
 * This filter checks that an ArgArray is an instanceof SiamRecord and that 
 * the SiamRecord contains at least one Measurement
 *
 * @author brian
 */
public class SiamRecordFilter implements IFilter {
    
    public boolean accept(ArgArray argArray) {
        boolean accepted = (argArray instanceof SiamRecord);
        if (accepted) {
            // Make sure the record contains at least one Measurement
            Enumeration e = argArray.elements();
            while (e.hasMoreElements()) {
                Object obj = e.nextElement();
                accepted = obj instanceof Measurement;
                if (accepted) {
                    break;
                }
            }
        }
        return accepted;
    }
    
}

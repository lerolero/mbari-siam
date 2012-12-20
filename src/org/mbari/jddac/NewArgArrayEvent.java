/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * NewArgArrayEvent.java
 *
 * Created on April 8, 2006, 1:48 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import java.util.EventObject;
import net.java.jddac.common.type.ArgArray;

/**
 * This Event occurs when a ArgArray is successfully added to an 
 * AggregationBlock
 * @author brian
 */
public class NewArgArrayEvent extends EventObject {
    
    /** Creates a new instance of NewArgArrayEvent */
    public NewArgArrayEvent(ArgArray argArray) {
        super(argArray);
    }
    
}

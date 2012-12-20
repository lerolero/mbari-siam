/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * RelayFunction.java
 *
 * Created on March 27, 2006, 9:02 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;

/**
 * This function simply relays the ArgArray to the next link in the chain.
 *
 * @author brian
 */
public class RelayFunction implements IFunction {
    
    private static final Logger log = Logger.getLogger(RelayFunction.class);
    
    public ArgArray execute(ArgArray argArray) {
        if (log.isDebugEnabled()) {
            log.debug("Relaying " + argArray);
        }
        return argArray;
    }
    
}

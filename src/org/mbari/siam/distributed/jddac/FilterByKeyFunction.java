/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import java.util.Enumeration;
import java.util.Vector;
import net.java.jddac.common.type.ArgArray;
import org.mbari.jddac.IFunction;

/**
 * Removes all elements from an ArgArray whos keys are not in the AllowedKeys
 * list
 */
public class FilterByKeyFunction implements IFunction {
    
    private final Vector allowedKeys = new Vector();
    
    public ArgArray execute(ArgArray argArray) {
        
        Vector droppedKeys = new Vector();
        
        /*
         * Find any keys that are not in the allowedKeys list
         */
        Enumeration keys = argArray.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (!allowedKeys.contains(key)) {
                droppedKeys.add(key);
            }
        }
        
        /*
         * Drop all the keys not in the allowedKeys list
         */
        keys = droppedKeys.elements();
        while (keys.hasMoreElements()) {
            argArray.remove(keys.nextElement());
        }
        
        return argArray;
    }
    
    /**
     * This is the vector that contains the acceptable keys. All other
     * values in an ArgArray that do not match one of these keys will be 
     * filtered out.
     */
    public Vector getAllowedKeys() {
        return allowedKeys;
    }
    
}
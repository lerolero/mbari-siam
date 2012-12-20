/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * ScatterBlock.java
 *
 * Created on April 10, 2006, 9:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import java.util.Enumeration;
import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import org.apache.log4j.Logger;

/**
 * Pulls out each value out of an argArray and passes them separatly to any
 * child functions. Note: You can not add functions to this block
 * @author brian
 */
public class ScatterBlock extends RelayBlock {
    
    private static final Logger log = Logger.getLogger(ScatterBlock.class);
    
    /** Creates a new instance of ScatterBlock */
    public ScatterBlock() {
        super();
    }

    /**
     * Passes each ArgArray value with in the ArgArray argument to each child
     * FunctionBlock seperatly using the same server_operation_id
     * For Example:
     * <pre>
     * ArgArray {
     * "nameA": ArgArray {
     *           "name1": Measurement
     *           "name2": Measurement
     *           ...
     *         }
     *"nameB": ArgArray {
     *           "name3": Measurement
     *           "name4": Measurement
     *           ...
     *         }
     * }
     *
     * This block passes these values to each child function it to:
     * ArgArray {
     *   "name1": Measurement
     *   "name2": Measurement
     *   ...
     * }
     *
     *ArgArray {
     *   "name3": Measurement
     *   "name4": Measurement
     *   ...
     * }
     *   
     * </pre>
     */
    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments) 
            throws Exception, OpException {
        
        Enumeration keys = server_input_arguments.keys();
        FunctionBlock[] children = getChildren();
        while(keys.hasMoreElements()) {
            Object value = server_input_arguments.get((String) keys.nextElement());
            if (value instanceof ArgArray) {
                for (int i = 0; i < children.length; i++) {
                    children[i].perform(server_operation_id, (ArgArray) value);
                }
            }
        }
        
        return null;
    }

    public boolean addFunction(ArgArray argArray) {
        // Overridden. You can not add functions to this plock
        return false;
    }
    

    

    
}

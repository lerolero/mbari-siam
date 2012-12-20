/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Log4jFunction.java
 *
 * Created on March 24, 2006, 1:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;

/**
 * This function logs the argArray passed into the process method to a Logger.
 *
 * @author brian
 */
public class Log4jFunction implements IFunction {
    
    private final Logger log;
    
    /**
     * Creates a new instance of Log4jFunction
     *
     * @param loggerName The name of the Logger
     */
    public Log4jFunction(String loggerName) {
        log = Logger.getLogger(loggerName);
    }

    public ArgArray execute(ArgArray argArray) {
        if (log.isInfoEnabled()) {
            log.info(argArray.toString());
        }
        return argArray;
    }
    
}

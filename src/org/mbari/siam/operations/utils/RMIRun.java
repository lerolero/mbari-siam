/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * RMIRun.java
 *
 * Created on May 5, 2006, 9:16 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.operations.utils;

import java.lang.reflect.Method;
import org.mbari.siam.utils.CachingClassLoader;

/**
 * This is an attempt to circumvent the RMI ClassLoader in Java 1.3. You can
 * use this class to execute java classes with main methods that make RMI calls.
 * Any classes fetched from the codebase are cached locally.
 * @author brian
 */
public class RMIRun {
    
    /** Creates a new instance of RMIRun */
    public RMIRun() {
    }
    
    public static void main(String[] args) throws Exception {
        
        /*
         * Here's our custom classloader. All references will be resolved 
         * using this classloader
         */
        CachingClassLoader classLoader = new CachingClassLoader();
        
        /*
         * The name of the class with a main method to execute. Load it using
         * our custom classloader
         */
        String mainClass = args[0];
        Class clazz = classLoader.loadClass(mainClass);
        
        /*
         * Find the 'main' method that takes a String[] as an argument using
         * reflection
         */
        Class mainArgType[] = {(new String[0]).getClass()};
        Method mainMethod = clazz.getMethod("main", mainArgType);
        
        /*
         * The arguments that will be passed to the main class
         */
        String[] arguments = new String[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, arguments.length);
        Object argsArray[] = {arguments};
        
        // execute
        mainMethod.invoke(null, argsArray);
        
        
    }
            
    
}

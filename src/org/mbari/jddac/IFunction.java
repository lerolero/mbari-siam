/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * IFunction.java
 *
 * Created on March 22, 2006, 8:48 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;

/**
 * Functions can be added to a RelayBlock as follows:
 * <pre>
 * // Create a function
 * IFunction function = new IFunction() {
 *     public ArgArray execute(ArgArray argArray) {
 *          System.out.println("I'm just passing through");
 *          return argArray
 *     }
 * }
 *
 * // Create an ArgArray to hold the function and pass it into process.
 * // OpIdToExecuteFunction is the OpId that must be passed to process in order
 * // to trigger the execution of the function. OpIdPassedToChildrenWithResult
 * // is the OpId that is passed to all children of the relay block, along with 
 * // the output of the function. Essentially all children will have a call made 
 * // as: process(OpIdPassedToChildrenWithResult, argArrayOutputFromFunction)
 * // REMEMBER: OpId's are Strings!!
 * ArgArray argArray = new ArgArray();
 * argArray.put(IFunction.OPID_IN, OpIdToExecuteFunction)
 * argArray.put(IFunction.OPID_OUT, OpIdPassedToChilderenWithResult)
 * argArray.put(IFunction.KEY, function)
 *
 * RelayBlock block = new RelayBlock();
 * block.process(RelayBlock.OpIdAddFunction, argArray);
 * </pre>
 *
 *
 * @author brian
 * @see FunctionFactory
 */
public interface IFunction {
    
    String KEY = "IFunction-20060323";
    
    String OPID_IN = "OPID_IN";
    String OPID_OUT = "OPID_OUT";
    
    ArgArray execute(ArgArray argArray);
    
}

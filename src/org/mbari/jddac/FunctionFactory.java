/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Mar 27, 2006
 * Time: 4:48:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class FunctionFactory {

    /**
     * Wraps a function so that it can function as input to a RelayBlock. This method is equivalent to calling
     * <pre>
     * ArgArray argArray = new ArgArray();
     * argArray.put(IFunction.OPID_IN, opIdIn);
     * argArray.put(IFunction.OPID_OUT, opIdOut);
     * argArray.put(IFunction.KEY, function);
     * </pre>
     * @param opIdIn This is the OpID string passed into the process method of a RelayBlock that would trigger the
     *                function to execute
     * @param opIdOut This is the OpId String that will be passed on to all the child RelayBlocks along with the results
     *                of the function
     * @param function The function to be executed
     * @return A function wrapped for input into a RelayBlock
     */
    public static ArgArray createFunctionArg(String opIdIn, String opIdOut, IFunction function) {
        ArgArray argArray = new ArgArray();
        argArray.put(IFunction.OPID_IN, opIdIn);
        argArray.put(IFunction.OPID_OUT, opIdOut);
        argArray.put(IFunction.KEY, function);
        return argArray;
    }


}

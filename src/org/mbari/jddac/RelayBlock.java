/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * RelayBlock.java
 *
 * Created on March 22, 2006, 7:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Relays results programmatically. By itself a RelayBlock does nothing. However you can associate functions with it
 * that can carry ou specific operations.
 *
 * @author brian
 * @see org.mbari.jddac.ex01.JvmMemoryApp
 */
public class RelayBlock extends FunctionBlock implements Serializable {


    /**
     * The OpId used to add a function.
     */
    public static final String OpIdAddFunction = "addFunction";
    
    
    private Vector children = new Vector();
    
    /**
     * <p>
     * Functions are stored in ArgArrays as:
     * <pre>
     * KEY                   VALUE
     * IFunction.OPID_IN     OpId uses to execute a function
     * IFunction.OPID_OUT    OpId passed to child FunctionBlocks along with 
     *                       result of Function after it's executed
     * IFunction.KEY         An IFunction
     * </pre>
     * </p>
     *
     * @see IFunction
     */
    private Vector functions = new Vector();
    
    private IFunction function;
    
    /** Log4j logger */
    private static final Logger log = Logger.getLogger(RelayBlock.class);
    
    /**
     * Creates a new instance of RelayBlock
     */
    public RelayBlock() {
    }
    
    public boolean addChild(FunctionBlock child) {
        return children.add(child);
    }
    
    public boolean removeChild(FunctionBlock child) {
        return children.remove(child);
    }
    
    public FunctionBlock[] getChildren() {
        FunctionBlock[] f;
        synchronized (children) {
            f = new FunctionBlock[children.size()];
            children.toArray(f);
        }
        return f;
    }
    
    /**
     * <p>Performs the operation using  server_input_arguments as the argument
     *  to the function used for processing</p>
     * 
     * <p>You can set the function used by calling:
     * <pre>
     * relatyBlock.addFunction(FunctionFactory.createFunctionArg(OpIdIn, OpIdOut, new SomeIFunction());
     * </pre>
     * </p>
     */
    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments)
            throws Exception, OpException {
        log.debug("RelayBlock.perform() - entry");
        ArgArray argArray = null;
        boolean done = false;
        /*
         * OpIdAddFunction = a call to addFunction
         */
        log.debug("RelayBlock.perform() - calling OpIdAddFunction");
        if (OpIdAddFunction.equals(server_operation_id)) {
            addFunction(argArray);
            done = true;
        }
    
        log.debug("RelayBlock.perform() - checking done");
        if (!done) {
	    log.debug("RelayBlock.perform() - done is FALSE");
            /*
             * Check to see if the id will trigger one of the functions
             */
            Enumeration e = functions.elements();
            while (e.hasMoreElements()) {
		log.debug("RelayBlock.perform() - enumerating over elements");
                ArgArray faa = (ArgArray) e.nextElement();
                String opIdIn = (String) faa.get(IFunction.OPID_IN);
                if (opIdIn.equals(server_operation_id)) {

		    log.debug("RelayBlock.perform() - getting and executing function");
                    // If a trigger opId for a function is found execute the function
		    log.debug("RelayBlock.perform() - faa.get()");
                    IFunction function = (IFunction) faa.get(IFunction.KEY);
		    log.debug("RelayBlock.perform() - function.execute()");
                    argArray = function.execute(server_input_arguments);

                    /* 
                     * Pass the results on to all children in the chain using the
                     * approriate opId
                     */
		    log.debug("RelayBlock.perform() - faa.get(OPID_OUT)");
                    String opIdOut = (String) faa.get(IFunction.OPID_OUT);
                    Enumeration kids = children.elements();
                    while(kids.hasMoreElements()) {
			log.debug("RelayBlock.perform() - enumerating over children");

                        FunctionBlock fblock = (FunctionBlock) kids.nextElement();
                        if (log.isDebugEnabled()) {
                            log.debug("Calling '" + fblock + ".perform(" + opIdOut + ", " + argArray + ")'" );
                        }
                        fblock.perform(opIdOut, argArray);
                    }
                    
                    // Make sure we don't do any other work
                    done = true;

                    /*
                     * There's no prohibition against several functions using
                     * the same OPID_IN so we must loop through them all to 
                     * make sure we've exectued all the right ones. so don't 
                     * break out of this loop even if a function has been 
                     * executed.
                     */
                }
            }
        }
        
        /*
         * If no operations have been performed pass it on up the FunctionBlock
         * hierarchy.
         */
        if (!done) {
            try {
		log.debug("RelayBlock.perform() - calling super.perform()");
                argArray = super.perform(server_operation_id, server_input_arguments);
            } catch (Exception ex) {
                log.error("Failed to execute '" + toString() + ".perform(" + server_operation_id + ", " + server_input_arguments + ")'" , ex);
            }
            done = true;
        }

        return argArray;
    }

    /**
     * A ddan IFunction to a relay block.
     *
     * @param argArray An ArgArray containing the needed keys-value pairs
     *
     * @see IFunction
     */
    public boolean addFunction(ArgArray argArray) {
        // Make sure this is has the required fields
        boolean accepted = false;
        if ((argArray.get(IFunction.KEY) != null) && 
                (argArray.get(IFunction.OPID_IN) != null) &&
                (argArray.get(IFunction.OPID_OUT) != null)) {
            accepted = functions.add(argArray);
        }
        
        if (accepted && log.isInfoEnabled()) {
            log.info("Added function " + argArray.get(IFunction.KEY) + " [" + 
                    argArray.get(IFunction.OPID_IN) + "->" + argArray.get(IFunction.OPID_OUT) + "] to " + toString());
        }
        return accepted;
    }
    
    
    
}

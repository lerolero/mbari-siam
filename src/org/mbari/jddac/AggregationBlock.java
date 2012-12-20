/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * AggregationBlock.java
 *
 * Created on March 22, 2006, 6:42 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import java.beans.PropertyChangeSupport;
import java.util.Enumeration;
import java.util.Iterator;
import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;

import java.util.Vector;

/**
 * FunctionBlock used for aggregating measurements or records. Measurements/Records can be filtered by adding
 * implementations of IFilter. A Filter generally accepts or rejects a Measurement but can also be more complicated.
 *
 * @author brian
 * @see IFilter
 * @see SampleCountFilter
 */
public class AggregationBlock extends RelayBlock {
    
    private static final Logger log = Logger.getLogger(AggregationBlock.class);
    
    private final Vector newArgArrayListeners = new Vector();
    
    /**
     * Operation ID for adding an argument array
     */
    public static final String OpIdAddArgArray = "addArgArray";
    
    /**
     * Vector<ArgArray>: The samples to be averaged.
     */
    private Vector samples = new Vector();
    
    /*
     * Vector<IFilter>: Filters used to accept or reject samples
     */
    private Vector filters = new Vector();
    
    public AggregationBlock() {
        super();
    }
    

    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments) 
            throws Exception, OpException {
        
        if (log.isDebugEnabled()) {
            log.debug("Calling perform(" + server_operation_id + ", " + 
                    server_input_arguments + ")");
        }
        
        ArgArray argArray = null;
        if (OpIdAddArgArray.equals(server_operation_id)) {
            addArgArray(server_input_arguments);
        }
        else if (OpIdClear.equals(server_operation_id)) {
            clear();
        }
        else {
            argArray = super.perform(server_operation_id, server_input_arguments);
        }
        return argArray;
        
    }
    
    public void addNewArgArrayListener(NewArgArrayListener listener) {
        newArgArrayListeners.add(listener);
    }
    
    public void removeNewArgArrayListener(NewArgArrayListener listener) {
        newArgArrayListeners.remove(listener);
    }
    
    public NewArgArrayListener[] getNewArgArrayListeners() {
        NewArgArrayListener[] listeners;
        synchronized (newArgArrayListeners) {
            listeners = new NewArgArrayListener[newArgArrayListeners.size()];
            newArgArrayListeners.toArray(listeners);
        }
        return listeners;
    }
    

    /**
     * Add a sample to the aggregation data. To filter which samples get added
     * use <code>addFilter(IFilter filter)</code>. If an ArgArray fails any
     * of the acceptance tests then it will not be added.
     *
     * @param argArray An argArray to add
     * @return true if the argArray was successfully added. false otherwise
     */
    public boolean addArgArray(ArgArray argArray) {
        
        if (log.isDebugEnabled()) {
            log.debug("Calling addArgArray(" + argArray + ")");
        }
        
        boolean accepted = false;
        IFilter[] f = getFilters();
        for (int i = 0; i < f.length; i++) {
            accepted = f[i].accept(argArray);
            if (!accepted) {
                break;
            }
        }
        
        if (accepted) {
            accepted = samples.add(argArray);
        }
        
        /*
         * Notify any listeners
         */
        if (accepted) {
            
            if (log.isDebugEnabled()) {
                log.debug("Adding " + argArray + " to samples");
            }
            
            NewArgArrayListener[] listeners = getNewArgArrayListeners();
            if (listeners != null && listeners.length > 0) {
                NewArgArrayEvent event = new NewArgArrayEvent(argArray);
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].processEvent(event);
                }
                
            }
        }
        return accepted;
    }
    
    /**
     * Retreive all the samples that have been stored in the AggregationBlock
     *
     * @return The ArgArray objects that have been stored.
     */
    public ArgArray[] getArgArrays() {
        ArgArray[] a;
        synchronized (samples) {
            a = new ArgArray[samples.size()];
            samples.toArray(a);
        }
        return a;
    }
    
    
    /**
     * Add a sample filter. When a ArgArray (i.e. sample) is added using 
     * <code>addArgArray</code>, it must be tested by every filter registered
     * to see if it is added as to the aggregation of samples.
     *
     * @param filter The filter to add
     * @return <b>true</b> if the filter was added.
     * @see IFilter
     */
    public boolean addFilter(IFilter filter) {
        if (log.isDebugEnabled()) {
            log.debug("Adding " + filter);
        }
        return filters.add(filter);
    }
    
    /**
     * Removes a filter
     *
     * @see IFilter
     * @return <b>true</b> if the filter was removed
     * @param filter The filter to remove
     * 
     */
    public boolean removeFilter(IFilter filter) {
        if (log.isDebugEnabled()) {
            log.debug("Removing " + filter);
        }
        return filters.remove(filter);
    }
    
    /**
     * Retreive all filters registered with the AggregationBlock
     * 
     * @return An array of IFilters registered with the AggregationBlock
     */
    public IFilter[] getFilters() {
        IFilter[] f;
        synchronized (filters) {
            f = new IFilter[filters.size()];
            filters.toArray(f);
        }
        return f;
    }
    
    
    /**
     * @return the number of ArgArrays that have been stored in the aggregation
     */
    public int size() {
        return samples.size();
    }
    
    /**
     * Clear all the samples out of the AggregationBlock
     */
    public void clear() {
        if (log.isDebugEnabled()) {
            log.debug("Clearing all filters");
        }
        samples.clear();
    }
    
    /**
     * Retrieves the underlying Vector that is used to hold all the samples.
     * You should only use this if you really, really need to have direct 
     * access to underlying data. 
     */
    public Vector getSamples() {
        return samples;
    }

    
}

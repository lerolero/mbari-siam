/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * FilterBlock.java
 *
 * Created on April 10, 2006, 10:14 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import java.util.Enumeration;
import java.util.Vector;
import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.type.ArgArray;

/**
 *
 * @author brian
 */
public class FilterBlock extends RelayBlock {
    
    /** Vector<IFilter> */
    private final Vector filters = new Vector();
    
    /** Creates a new instance of FilterBlock */
    public FilterBlock() {
    }
    
    public boolean addFilter(IFilter filter) {
        return filters.add(filter);
    }
    
    public boolean removeFilter(IFilter filter) {
        return filters.remove(filter);
    }
    
    public IFilter[] getFilters() {
        IFilter[] f;
        synchronized (filters) {
            f = new IFilter[filters.size()];
            filters.toArray(f);
        }
        return f;
    }

    /**
     * Runs the server_input_arguments through the filters. If it's not accepted
     * then an Empty ArgArray is passed on. 
     */
    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments) 
            throws Exception, OpException {
        ArgArray out = null;
        
        boolean accepted = false;
        Enumeration f = filters.elements();
        while(f.hasMoreElements()) {
            IFilter filter = (IFilter) f.nextElement();
            accepted = filter.accept(server_input_arguments);
            if (!accepted) {
                break;
            }
        }
        
        if (accepted) {
            out = super.perform(server_operation_id, server_input_arguments);
        }
        else {
            out = new ArgArray();
        }
        return out;
        
    }
    

    
}

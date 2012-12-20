/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * IFilter.java
 *
 * Created on March 22, 2006, 7:17 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;

/**
 * This interface can be implemented to reject or accept <code>ArgArrays</code> 
 * that are added to <code>AggregationBlocks</code>. For example, you may want 
 * to reject <code>ArgArrays</code> that do not contain a particular key or 
 * you may want to only accept <code>Measurements</code> (a sub-class of 
 * <code>ArgArray</code>)
 *
 * @author brian
 */
public interface IFilter {
    
    String KEY = "IFilter-20060323";
    
    boolean accept(ArgArray argArray);
    
}

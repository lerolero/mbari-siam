/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * NewArgArrayListener.java
 *
 * Created on April 8, 2006, 1:51 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import java.util.EventListener;

/**
 *
 * @author brian
 */
public interface NewArgArrayListener extends EventListener {
    
    void processEvent(NewArgArrayEvent event);
    
    
}

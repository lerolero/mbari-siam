/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.util.Vector;

/** */
public interface Parser {

    public Vector parse(Object data);
    public Vector getAll();
    public Object get(int field);
    public Object get(String fieldName);
}

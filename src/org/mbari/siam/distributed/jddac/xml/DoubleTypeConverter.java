/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac.xml;

import net.java.jddac.jmdi.comm.xml.ArgTypeConverter;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Apr 19, 2006
 * Time: 1:49:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class DoubleTypeConverter implements ArgTypeConverter {

    public static final String XML_TAG = "dbl";
    private static final TypePair[] typePairs = {new TypePair(XML_TAG,  Double.class)};

    public String getTypeName(Object o) throws IllegalArgumentException {
        return XML_TAG;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getLength(Object o) throws IllegalArgumentException {
        return -1;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isComplexType(Object o) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isVectorComplexType(String typename) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getComplexValue(Object o) throws IllegalArgumentException {
        throw new IllegalArgumentException(getClass().getName() + " does not support complex types");
    }

    public String getScalarValue(Object o) throws IllegalArgumentException {
        return o.toString();
    }

    public Object getObject(String typeName, String len, String value) throws Exception {
        Double d = null;
        if (XML_TAG.equals(typeName)) {
            d = Double.valueOf(value);
        }
        return d;
    }

    public Object getObject(String typeName, Object value) throws Exception {
        return new IllegalArgumentException(getClass().getName() + " does not support complex types");  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TypePair[] getSupportedTypePairs() {
        return typePairs;
    }
}

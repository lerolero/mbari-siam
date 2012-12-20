/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac.xml;

import org.mbari.siam.distributed.jddac.MutableDoubleArray;
import net.java.jddac.jmdi.comm.xml.ArgTypeConverter;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Apr 19, 2006
 * Time: 10:58:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class MutableDoubleArrayTypeConverter implements ArgTypeConverter {

    public static final String XML_TAG = "mda";
    private static final TypePair[] typePairs = {new TypePair(XML_TAG,  MutableDoubleArray.class)};

    public MutableDoubleArrayTypeConverter() {
        super();
    }

    public String getTypeName(Object o) throws IllegalArgumentException {
        return XML_TAG;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getLength(Object o) throws IllegalArgumentException {
        return ((MutableDoubleArray) o).size();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isComplexType(Object o) {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isVectorComplexType(String typename) {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getComplexValue(Object obj) throws IllegalArgumentException {
        double[] d = ((MutableDoubleArray) obj).getValues();
        Vector v = new Vector();
        for(int i = 0; i < d.length; i++) {
            v.add(new Double(d[i]));
        }
        return v;
    }

    public String getScalarValue(Object o) throws IllegalArgumentException {
        throw new IllegalArgumentException("Argument can not be converted to a scalar value");  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getObject(String typeName, String len, String value) throws Exception {
        throw new IllegalArgumentException("Argument is not a scalar");  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getObject(String typeName, Object value) throws Exception {
        if (XML_TAG.equals(typeName) && value instanceof Vector) {
            Vector v = (Vector) value;
            MutableDoubleArray mda = new MutableDoubleArray(new double[v.size()]);
            Enumeration e = ((Vector) value).elements();
            int count = 0;
            while (e.hasMoreElements()) {
                mda.set(count, Double.parseDouble(String.valueOf(e.nextElement())));
                count++;
            }
        }
        else {
            throw new IllegalArgumentException("Unable to handle XML tag of " + typeName + " with a non-Vector value");
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TypePair[] getSupportedTypePairs() {
        return typePairs;
    }

}
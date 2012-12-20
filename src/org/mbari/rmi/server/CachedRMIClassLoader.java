/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.rmi.server;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
//import java.rmi.server.RMIClassLoaderSpi;
import java.util.HashMap;
import java.util.Map;

//public class CachedRMIClassLoader extends RMIClassLoaderSpi {

public class CachedRMIClassLoader {
    
    /**
     * Map for storing classloaders that have alreay been instantiated
     * Map<URL, ClassLoader>
     */
    private Map cacheLoaders = new HashMap();
    private final File cacheHome;
    
    public CachedRMIClassLoader() {
        
                /*
                 * Create a directory that we can right cached RMI classes into.
                 */
        cacheHome = new File(new File(System.getProperty("user.home"), ".siam"), "rmicache");
        if (!cacheHome.exists()) {
            cacheHome.mkdirs();
        }
        
    }
    
    
    public String getClassAnnotation(Class arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        
        URL cachedCodebase = (new File(cacheHome, codebase.hashCode() + "")).toURL();
        ClassLoader classLoader = (ClassLoader) cacheLoaders.get(cachedCodebase);
        if (classLoader == null) {
            classLoader = URLClassLoader.newInstance(new URL[] {cachedCodebase, new URL(codebase)});
        }
        
        
        return null;
    }
    
    public Class loadClass(String codebase, String name,
            ClassLoader defaultLoader) throws MalformedURLException,
            ClassNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Class loadProxyClass(String codebase, String[] interfaces,
            ClassLoader defaultLoader) throws MalformedURLException,
            ClassNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }
    
    private class MyClassLoader extends URLClassLoader {
        
        public MyClassLoader(URL[] urls) {
            super(urls);
            // TODO Auto-generated constructor stub
        }
        
    }
    
}

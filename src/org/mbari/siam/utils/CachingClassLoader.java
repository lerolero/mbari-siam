/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * CachingClassLoader.java
 *
 * Created on May 5, 2006, 9:22 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 *
 * @author brian
 */
public class CachingClassLoader extends ClassLoader {
    
    private static final Logger log = Logger.getLogger(CachingClassLoader.class);
    
    /**
     * These are classes that we fetched using a URLClassLoader. They will all
     * be cached on shutdown
     * Hashtable<String, Class>
     */
    private final Hashtable loadedClasses = new Hashtable();
    
    /**
     * The root directory of our cache used for storing classes fetched over RMI.
     */
    private File cacheHome;
    
    private URL remoteCodebase;
    private URL cachedCodebase;
    
    
    /** Creates a new instance of CachingClassLoader */
    public CachingClassLoader(ClassLoader parent) {
        super(parent);
        
        /*
         * See if we can reference the codebase
         */
        String cb = System.getProperty("java.rmi.server.codebase");
        try {
            remoteCodebase = new URL(cb);
            cacheHome = new File(new File(new File(System.getProperty("user.home"), ".siam"),"classcache"), remoteCodebase.hashCode() + "");
            cachedCodebase = cacheHome.toURL();
        } catch (MalformedURLException ex) {
            log.warn("Failed to create a URL reference using the property 'java.rmi.server.codebase' [" +
                    cb + "]. Caching is disabled." );
            cacheHome = null;
        }
        
       /*
        * Create a directory that we can right cached RMI classes into.
        */
        if (cacheHome != null && !cacheHome.exists()) {
            boolean canCache = cacheHome.mkdirs();
            
            // If we can't create the cache directory then we can't cache classes
            if (!canCache) {
                if (log.isDebugEnabled()) {
                    log.info("Caching code to '" + cacheHome.getAbsolutePath() + "'");
                }
            } else {
                log.warn("Unable to create '" + cacheHome.getAbsolutePath() + "'. Caching is disabled.");
                cacheHome = null;
            }
        }
        
    }
    
    public CachingClassLoader() {
        this(getSystemClassLoader());
    }
    
    public Class loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }
    
    
    
    protected Class findClass(String name) throws ClassNotFoundException {
        
        
        // Check the local in-memory cache
        if (log.isDebugEnabled()) {
            log.debug("Checking to see if '" + name + "' has been loaded");
        }
        Class clazz = (Class) loadedClasses.get(name);
        
        // Check the primoridail ClassLoader
        if (clazz == null) {
            if (log.isDebugEnabled()) {
                log.debug("Trying to load '" + name + "' using " + getParent());
            }
            clazz = getParent().loadClass(name);
        }
                
        // Check the local on-disk caches
        if (clazz == null && cachedCodebase != null) {
            if (log.isDebugEnabled()) {
                log.debug("Trying to load '" + name + "' from " + cachedCodebase);
            }
            clazz = findClass(name, cachedCodebase, false);
        }
        
        // Check the remote codebase
        if (clazz == null && remoteCodebase != null) {
            if (log.isDebugEnabled()) {
                log.debug("Trying to load '" + name + "' from " + remoteCodebase);
            }
            clazz = findClass(name, remoteCodebase, true);
        }
        
        
        return clazz;
    }
    
    /**
     * Attempte to fetch a class from a given URL
     * @param name The name of the class to fetch
     * @param codebase The URL location that is the root of the classpath to 
     *  search
     * @param cacheIt If true the classes fetched will be cached. If false then 
     *  the classes will not be cached.
     */
    protected Class findClass(String name, URL codebase, boolean cacheIt) {
        // Try to load from local cache
        Class clazz = null;
        try {
            
            String urlString = codebase.toString();
            if (!urlString.endsWith("/")) {
                urlString += "/";
            }
            urlString += name.replace('.', '/').concat(".class");
            
            URL url = new URL(urlString);
            byte[] classData = loadClassData(url);
            if (classData.length > 0) {
                clazz = defineClass(name, classData, 0, classData.length);
                loadedClasses.put(name, clazz);
                if (cacheIt) {
                    cacheClass(name, classData);
                }
            }

        } catch (Exception e) {
            log.debug("Class '" + name + "' not found in '" + codebase + "'");
        }
        return clazz;
    }
    
    /**
     * Loads the binary data from a given URL
     */
    private byte[] loadClassData(URL url) throws IOException {
        
        // Get the size
        
        DataInputStream in = new DataInputStream(new BufferedInputStream(url.openStream()));
        Vector bytes = new Vector();
        int i;
        while ((i = in.read()) > -1) {
            bytes.add(new Byte((byte) i));
        }
        in.close();
        
        byte[] classData = new byte[bytes.size()];
        for (int j = 0; j < classData.length; j++) {
            classData[j] = ((Byte) bytes.get(j)).byteValue();
        }
        
        return classData;
    }
    
    private void cacheClass(String name, byte[] data) {
        
        name = name.replace('.', '/').concat(".class");
        File dst = new File(cacheHome, name);
        
        if (log.isDebugEnabled()) {
            log.debug("Attempting to cache '" + name + "' to " + dst.getAbsolutePath());
        }
        
        // Make sure we have a directory to write to
        File parent = dst.getParentFile();
        boolean ok = true;
        if (!parent.exists()) {
            ok = parent.mkdirs();
        }
        
        if (ok && parent.canWrite()) {
            try {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));
                out.write((byte[]) data);
                out.close();
                if (log.isDebugEnabled()) {
                    log.debug("Cached '" + dst.getAbsolutePath() + "'");
                }
            } catch (IOException ex) {
                log.error("Failed to write '" + dst.getAbsolutePath() + "'");
            }
        } else {
            log.warn("Unable to write class file to '" + parent.getAbsolutePath() + "'");
        }
    }
    
    
    
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/** Test Map collection stuff... */
public class MapTest {

    public static void main(String[] args) {

	Map map = new HashMap();

	long deviceId[] = {2001, 1984, 1886, 2001, 1082, 1886, 1950, 1955};

	for (int i = 0; i < deviceId.length; i++) {

	    Long key = new Long(deviceId[i]);

	    if (map.containsKey(key)) {
		System.out.println("map already contains " + deviceId[i]);
	    }
	    else {
		System.out.println("Add " + deviceId[i] + " to map...");
	    }

	    map.put(key, "junk-" + i);
	    
	}

	printMap(map);

	// Replace values
	System.out.println("Modify values...");
	Iterator i = map.entrySet().iterator();
	while (i.hasNext()) {
	    Map.Entry entry = (Map.Entry )i.next();
	    String s = (String )entry.getValue();
	    s = s + " NEW!";
	    entry.setValue(s);
	}

	printMap(map);
    }


    public static void printMap(Map map) {

	System.out.println("map contains " + map.size() + " keys");
	Iterator iterator = map.keySet().iterator();
	while (iterator.hasNext()) {
	    Object keyObj = iterator.next();
	    System.out.println(keyObj.toString() + ": " + 
			       (map.get(keyObj)).toString());
	}
    }
}

/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Feb 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mbari.siam.devices.seahorse;

import junit.framework.TestCase;



/**
 *
 *
 * @author achase
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSeahorse extends TestCase {

    public void testSplit() {
        //an easy one to warm up
        String[] expectedResult = new String[] { "Hi","How","Are", "You", "Today"};
        String[] result = Seahorse.split("Hi,How,Are,You,Today", ",");
        assertTrue(areStringArraysEqual(expectedResult, result));
        
        //how does it handle things when the split string doesn't show up
        expectedResult = new String[] { "HiHowAreYouToday" };
        result = Seahorse.split("HiHowAreYouToday", ",");
        assertTrue(areStringArraysEqual(expectedResult, result));
        
        //test how it handles spanish (j/k)
        expectedResult = new String[] { "buenos", "dias", "senior,", "coma", "estas?"};
        result = Seahorse.split("buenos[break]dias[break]senior,[break]coma[break]estas?", "[break]");
        assertTrue(areStringArraysEqual(expectedResult, result));
    }
    
    private boolean areStringArraysEqual(String[] array1, String[] array2){
        if(array1 == null || array2 == null){
            throw new NullPointerException();
        }
        if(array1.length != array2.length){
            return false;
        }
        boolean isEqual = true;
        for(int i = 0; i < array1.length; i++){
            if(!array1[i].equals(array2[i])){
                isEqual = false;
                break;
            }
        }
        return isEqual;
    }

}

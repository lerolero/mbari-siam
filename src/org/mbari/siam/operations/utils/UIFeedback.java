/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;


public interface UIFeedback {

    public void errorMessage(String message, Exception e);
    
    public void errorMessage(String message);
    
    public void informationalMessage(String message);
}

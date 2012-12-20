// Copyright 2002 MBARI
package org.mbari.siam.distributed.portal;

public class UnknownConfiguration extends Exception {

    public UnknownConfiguration() {
	super();
    }

    public UnknownConfiguration(String message) {
	super(message);
    }
}

// Copyright MBARI 2002
package org.mbari.siam.distributed;

/** RangeException indicates that an input argument had a value outside
of the valid range. */
public class RangeException extends Exception {
  /** Constructor. */
  public RangeException() {
    super();
  }
  
  /** Constructor; specify message. */
  public RangeException(String msg) {
    super(msg);
  }
}

/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.utils;


/** If you think it's a String, but it's not, it's a ByteArray...*/
class ByteArray {
  byte[] theArray;
  public ByteArray(int len){
    if(len>0)
      theArray = new byte[len];
  }
  public ByteArray(byte[] b){
    if(b != null){
      theArray =  new byte[b.length];
      for(int i=0;i<b.length;i++)
        theArray[i] = b[i];
    }
  }
  public ByteArray(byte[] b, int len){
    if(b != null)
      if(len>0 && len<=b.length){
        theArray =  new byte[len];
        for(int i=0;i<len;i++)
          theArray[i] = b[i];
      }
  }
  public ByteArray(byte[] b, int offset,int len){
    if(b != null)
      if( (len>0) && (len<=b.length) )
        if( (offset>=0) && (offset<b.length)){
          theArray =  new byte[len];
          for(int i=0;i<len;i++)
            theArray[i] = b[offset+i];
        }
  }
  public ByteArray(String s){
    if( s != null){
      theArray = new byte[s.length()];
      for(int i=0;i<s.length();i++)
        theArray[i]=(byte)s.charAt(i) ;
    }
  }

  public int compareTo(byte[] b){return -2;}
  public void concat(byte[] b){}
  public boolean endsWith(byte[] b){return false;}
  public byte[] getBytes(){return theArray;}
  public int indexOf(int c){return -1;}
  public int indexOf(int c,int i){return -1;}
  public int indexOf(byte[] b, int i){return -1;}
  public int indexOf(byte[] b){return -1;}
  public int lastIndexOf(int c){return -1;}
  public int lastIndexOf(int c,int i){return -1;}
  public int length(){return -1;}
  public boolean regionMatches(boolean x, int z, byte[] b, int offset, int len){ return false;}
  public void replace(byte b1, byte b2 ){}
  public boolean startsWith(byte[] b1,byte[] b2){return false;}
  public boolean startsWith(byte[] b1,byte[] b2, int i){return false;}
  public byte[] subArray(int len){return null;}
  public byte[] subArray(int offset, int len){return null;}
  public String toString(){return null;}
  public void toUpperCase(){}
  public void toLowerCase(){}
  public void trim(){}

    public static boolean checkByteArrayLen(byte[] b, int len){
      // Note <= 0, not < 0, hence different from checkByteArrayOffset
      if( (len <= 0) || (len > b.length) )
        return false;

      return true;        
    }
    public static boolean checkByteArrayOffset(byte[] b, int offset){

       if( (offset < 0) || (offset > b.length) )
        return false;

       return true;        
    }
    
    public static boolean checkByteArrayRange(byte[] b, int len, int offset){
      if( !checkByteArrayLen(b,len) )
        return false;
      if( !checkByteArrayOffset(b,offset) )
        return false;
      if( ((offset+len) >= b.length) )
        return false;

      return true;        
    }

    /** Compare two byte arrays assumed to be the same length (or the secondArray longer 
        firstArray)
        returns true if all characters match, false otherwise
     */
    public static boolean byteArrayEquals(byte[] firstArray, byte[] secondArray) throws ArrayIndexOutOfBoundsException{
      if(byteArrayEquals(firstArray,0,secondArray,0,firstArray.length)==0)
        return true;
      return false;      
    }

    /** Compare first len characters of two byte arrays
        returns true if all characters match, false otherwise
     */
    public static boolean byteArrayEquals(byte[] firstArray,byte[] secondArray, int len) throws ArrayIndexOutOfBoundsException{
      if(byteArrayEquals(firstArray,0,secondArray,0,len)==0)
        return true;
      return false;      
    }

    /** Compare two byte arrays
        returns 0 if all characters match, -1 if first mismatch < ,1 if first mismatch >
     */
    public static int byteArrayEquals(byte[] firstArray,int firstOffset,byte[] secondArray,int secondOffset, int len)throws ArrayIndexOutOfBoundsException{
      if(!checkByteArrayRange(firstArray,firstOffset,len))
        throw new ArrayIndexOutOfBoundsException("invalid firstArray range");     

      if(!checkByteArrayRange(secondArray,secondOffset,len))
        throw new ArrayIndexOutOfBoundsException("invalid secondArray range");     
            
      for(int i=0;i<len;i++){
        if(firstArray[firstOffset+i] > secondArray[secondOffset+i])
          return 1;
        if(firstArray[firstOffset+i] < secondArray[secondOffset+i])
          return -1;
      }
          
      return 0;      
    }
    

    /** Fill byte array with a character */
    public static void fillByteArray(byte[] theArray, char fillChar){
      fillByteSubArray(theArray,0,theArray.length,fillChar);
      return;      
    }
        /** Fill part of byte array with a character */
    public static void fillByteSubArray(byte[] theArray, int fromIndex, int toIndex, char fillChar){
      for(int i=fromIndex; ((i<toIndex) && (i<theArray.length)) ;i++)
      	theArray[i] = (byte)fillChar;
      return;      
    }

    /** Returns byte array containing the bytes from b[offset] to b[offset+len] */
    public static byte[] getByteSubArray(byte[] theArray,int offset, int len) throws ArrayIndexOutOfBoundsException{
       byte[] retval = new byte[len];
       if( (offset < 0) || (offset > theArray.length) )
         throw new ArrayIndexOutOfBoundsException("Invalid offset");
       if( (len <= 0)  || (len > theArray.length) )
         throw new ArrayIndexOutOfBoundsException("Invalid length");
       if( (offset+len) > theArray.length)
         throw new ArrayIndexOutOfBoundsException("Exceeded Array Bounds");
       
       for(int i=0;i<len;i++)
         retval[i]=theArray[i+offset];
       
       return retval;
    }
    
    /** Append 'buf' to 'target' 
        Starts at index 0
        Results in buffer size buf.length+target.length
    */	
    public static byte[] concat(byte[] theArray, byte[] secondBuf)
    throws ArrayIndexOutOfBoundsException{
      return concat(theArray,0,theArray.length,secondBuf,0,secondBuf.length);
    }
 
     /** Append 'buf' to 'target' 
        Starts at index 0
        Results in buffer size bufLen+ targetLength
    */	
    public static byte[] concat( byte[] theArray,int firstLen, byte[] secondBuf, int secondLen)
    throws ArrayIndexOutOfBoundsException{
      return concat(theArray,0,firstLen,secondBuf,0,secondLen);    
    }

     /** Append 'buf' to 'target' 
        Starts at index iBuf of buf, iTarget of target
        Results in buffer size bufLen+ targetLength
    */	
    public static byte[] concat(byte[] firstBuf, int firstOffset, int firstLen, byte[] secondBuf, int secondOffset, int secondLen)
    throws ArrayIndexOutOfBoundsException{

      if( (firstOffset < 0) || (firstOffset > firstBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Offset (1)");
        
      if( (secondOffset < 0) || (secondOffset > secondBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Offset (2)");

      if( (firstLen <= 0) || (firstLen > firstBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Length (1)");
      if( (secondLen <= 0) || (secondLen > secondBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Length (2)");

      if( ((firstOffset+firstLen) >= firstBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Exceeded Buffer length (1)");
      if( ((secondOffset+secondLen) >= secondBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Exceeded Buffer length (2)");
        
      int L1=(firstLen-firstOffset);
      int L2=(secondLen-secondOffset);
           
      byte[] retval = new byte[L1+L2];
      int i=0;
      int j=0;
      for(i = firstOffset; i < (firstOffset+L1); i++)
        retval[i]=firstBuf[i];
        
      for(j = secondOffset; j < (secondOffset+L2) ; j++)        
        retval[j+i]=secondBuf[j];
        
      return retval;
    }
  
}

/** Utility Methods for byte arrays/strings */
public final class ByteUtility {

    private static final int _INT_BIT_MASK = 0x000000FF; 
    private static final long _LONG_BIT_MASK = 0x00000000000000FF;
    private static final String _HEX_CHARS = "0123456789abcdef";

    
    public static boolean checkByteArrayLen(byte[] buf,int len){
      // Note <= 0, not < 0, hence different from checkByteArrayOffset
      if( (len <= 0) || (len > buf.length) )
        return false;

      return true;        
    }
    public static boolean checkByteArrayOffset(byte[] buf,int offset){

       if( (offset < 0) || (offset > buf.length) )
        return false;

       return true;        
    }
    
    public static boolean checkByteArrayRange(byte[] buf,int len,int offset){
     if( !checkByteArrayLen(buf,len) )
        return false;
      if( !checkByteArrayOffset(buf,offset) )
        return false;

      if( ((offset+len) >= buf.length) )
        return false;

      return true;        
    }

    /** Compare two byte arrays assumed to be the same length (or the secondArray longer 
        firstArray)
        returns true if all characters match, false otherwise
     */
    public static boolean byteArrayEquals(byte[] firstArray, byte[] secondArray) throws ArrayIndexOutOfBoundsException{
      if(byteArrayEquals(firstArray,0,secondArray,0,firstArray.length)==0)
        return true;
      return false;      
    }

    /** Compare first len characters of two byte arrays
        returns true if all characters match, false otherwise
     */
    public static boolean byteArrayEquals(byte[] firstArray, byte[] secondArray, int len) throws ArrayIndexOutOfBoundsException{
      if(byteArrayEquals(firstArray,0,secondArray,0,len)==0)
        return true;
      return false;      
    }

    /** Compare two byte arrays
        returns 0 if all characters match, -1 if first mismatch < ,1 if first mismatch >
     */
    public static int byteArrayEquals(byte[] firstArray, int firstOffset,byte[] secondArray,int secondOffset, int len)throws ArrayIndexOutOfBoundsException{
      if(!checkByteArrayRange(firstArray,firstOffset,len))
        throw new ArrayIndexOutOfBoundsException("invalid firstArray range");     

      if(!checkByteArrayRange(secondArray,secondOffset,len))
        throw new ArrayIndexOutOfBoundsException("invalid secondArray range");     
            
      for(int i=0;i<len;i++){
        if(firstArray[firstOffset+i] > secondArray[secondOffset+i])
          return 1;
        if(firstArray[firstOffset+i] < secondArray[secondOffset+i])
          return -1;
      }
          
      return 0;      
    }
    

    /** Fill byte array with a character */
    public static void fillByteArray(byte[] array, char fillChar){
      fillByteSubArray(array,0,array.length,fillChar);
      return;      
    }
        /** Fill part of byte array with a character */
    public static void fillByteSubArray(byte[] array, int fromIndex, int toIndex, char fillChar){
      for(int i=fromIndex; ((i<toIndex) && (i<array.length)) ;i++)
      	array[i] = (byte)fillChar;
      return;      
    }

    /** Returns byte array containing the bytes from b[offset] to b[offset+len] */
    public static byte[] getByteSubArray(byte[] b,int offset, int len) throws ArrayIndexOutOfBoundsException{
       byte[] retval = new byte[len];
       if( (offset < 0) || (offset > b.length) )
         throw new ArrayIndexOutOfBoundsException("Invalid offset");
       if( (len <= 0)  || (len > b.length) )
         throw new ArrayIndexOutOfBoundsException("Invalid length");
       if( (offset+len) > b.length)
         throw new ArrayIndexOutOfBoundsException("Exceeded Array Bounds");
       
       for(int i=0;i<len;i++)
         retval[i]=b[i+offset];
       
       return retval;
    }
    
    /** Append 'buf' to 'target' 
        Starts at index 0
        Results in buffer size buf.length+target.length
    */	
    public static byte[] concatByteArrays(byte[] firstBuf, byte[] secondBuf)
    throws ArrayIndexOutOfBoundsException{
      return concatByteArrays(firstBuf,0,firstBuf.length,secondBuf,0,secondBuf.length);
    }
 
     /** Append 'buf' to 'target' 
        Starts at index 0
        Results in buffer size bufLen+ targetLength
    */	
    public static byte[] concatByteArrays(byte[] firstBuf, int firstLen, byte[] secondBuf, int secondLen)
    throws ArrayIndexOutOfBoundsException{
      return concatByteArrays(firstBuf,0,firstLen,secondBuf,0,secondLen);    
    }

     /** Append 'buf' to 'target' 
        Starts at index iBuf of buf, iTarget of target
        Results in buffer size bufLen+ targetLength
    */	
    public static byte[] concatByteArrays(byte[] firstBuf, int firstOffset, int firstLen, byte[] secondBuf, int secondOffset, int secondLen)
    throws ArrayIndexOutOfBoundsException{

      if( (firstOffset < 0) || (firstOffset > firstBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Offset (1)");
        
      if( (secondOffset < 0) || (secondOffset > secondBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Offset (2)");

      if( (firstLen <= 0) || (firstLen > firstBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Length (1)");
      if( (secondLen <= 0) || (secondLen > secondBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Invalid Length (2)");

      if( ((firstOffset+firstLen) > firstBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Exceeded Buffer length (1)");
      if( ((secondOffset+secondLen) > secondBuf.length) )
        throw new ArrayIndexOutOfBoundsException("Exceeded Buffer length (2)");
        
      int L1=(firstLen-firstOffset);
      int L2=(secondLen-secondOffset);
           
      byte[] retval = new byte[L1+L2];
      int i=0;
      int j=0;
      for(i = firstOffset; i < (firstOffset+L1); i++)
        retval[i]=firstBuf[i];
        
      for(j = secondOffset; j < (secondOffset+L2) ; j++)        
        retval[j+i]=secondBuf[j];
        
      return retval;
    }

    /** Convert an int to a byte[] in network byte order */	
    public static byte[] intToBytes(int value)
    {
        byte[] b = new byte[4];
        b[0] = (byte) ((value >> 0x18) & _INT_BIT_MASK);
        b[1] = (byte) ((value >> 0x10) & _INT_BIT_MASK);
        b[2] = (byte) ((value >> 0x08) & _INT_BIT_MASK);
        b[3] = (byte) ((value >> 0x00) & _INT_BIT_MASK);

        return b;
    }


    /** Convert a long to a byte[] in network byte order */	
    public static byte[] longToBytes(long value)
    {
        byte[] b = new byte[8];
        b[0] = (byte) ((value >> 0x38) & _LONG_BIT_MASK);
        b[1] = (byte) ((value >> 0x30) & _LONG_BIT_MASK);
        b[2] = (byte) ((value >> 0x28) & _LONG_BIT_MASK);
        b[3] = (byte) ((value >> 0x20) & _LONG_BIT_MASK);

        b[4] = (byte) ((value >> 0x18) & _LONG_BIT_MASK);
        b[5] = (byte) ((value >> 0x10) & _LONG_BIT_MASK);
        b[6] = (byte) ((value >> 0x08) & _LONG_BIT_MASK);
        b[7] = (byte) ((value >> 0x00) & _LONG_BIT_MASK);

        return b;
    }

    
    
    
    /** Convert a byte[] to an int assumming the bytes are in network byte 
     * order */
    public static int bytesToInt(byte[] bytes) 
        throws ArrayIndexOutOfBoundsException
    {
        return bytesToInt(bytes, 0);
    }

    /** Convert a byte[] to a long assumming the bytes are in network byte
     *  order */	
    public static long bytesToLong(byte[] bytes) 
        throws ArrayIndexOutOfBoundsException
    {
        return bytesToLong(bytes, 0);
    }
    
    /** Convert a byte[] to an int assumming the bytes are in network byte 
     * order */
    public static int bytesToInt(byte[] bytes, int offset)
        throws ArrayIndexOutOfBoundsException
    {
        int result = 0;
        int shifter;

        if ((bytes.length - offset) < 4)
            throw new ArrayIndexOutOfBoundsException("byte array to short for integer converison");

        for (int i = 0; i < 4; ++i)
        {
            shifter = bytes[i + offset] & _INT_BIT_MASK;
            result += (shifter << ((3 - i) * 8));
        }
        
        return result;
    }

    /** Convert a byte[] to a long assumming the bytes are in network byte
     *  order */	
    public static long bytesToLong(byte[] bytes, int offset) 
        throws ArrayIndexOutOfBoundsException
    {
        long result = 0;
        long shifter;
        
        if ((bytes.length - offset) < 8)
            throw new ArrayIndexOutOfBoundsException("byte array to short for long converison");
        
        for (int i = 0; i < 8; ++i)
        {
            shifter = bytes[i + offset] & _LONG_BIT_MASK;
            result += (shifter << ((7 - i) * 8));
        }
        
        return result;
    }

    /** return a hex ASCII String representation of the bytes in 
     * the byte array */
    public static String bytesToHexString(byte[] b)
    {
        int hex;
        StringBuffer byteString = new StringBuffer();
        
        for (int i = 0; i < b.length; ++i) 
        {
            hex = b[i] & 0xFF;
            byteString.append(_HEX_CHARS.charAt(hex >> 4));
            byteString.append(_HEX_CHARS.charAt(hex & 0x0f));
        }

        return byteString.toString();
    }
}

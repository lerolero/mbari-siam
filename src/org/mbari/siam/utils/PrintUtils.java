/** 
* @Title PrintUtils
* @author Martyn Griffiths
* @version 1.0
* @date 8/24/2003
*
* Copyright MBARI 2003
* 
* REVISION HISTORY:
* $Log: PrintUtils.java,v $
* Revision 1.2  2009/07/16 22:02:09  headley
* javadoc syntax fixes for 1.5 JDK
*
* Revision 1.1  2008/11/04 22:17:53  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:05  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.5  2003/09/26 20:07:44  oreilly
* Added getAscii() method
*
* Revision 1.4  2003/09/16 23:58:48  martyn
* updates to PrintHex - count check removed, added to loop control
*
* Revision 1.3  2003/08/26 17:34:30  martyn
* Bug fix
*
* Revision 1.2  2003/08/26 00:41:40  martyn
* Static access to printFull and printAscii
* Bug fix
*
*/
package org.mbari.siam.utils;

import java.lang.IllegalArgumentException;
import org.mbari.siam.utils.PrintfFormat;


/**
 * Print utility class to provide formatted output such
 * as binary hex dumps ( a la MSDOS debug ). For ease of use
 * this class has been designed as a singleton w/ a static public
 * interface.
 * 
 * @author Martyn Griffiths
 * @version 1.0
 */
public class PrintUtils{

    static final PrintUtils _p = new PrintUtils();
    static int _width = 16;

    private PrintUtils(){
        // create a singleton
    }

    private void printOffset(int i){
        String str = new PrintfFormat("%04i").sprintf(i);
        System.out.print(str);
    }
    
    private int printHex(byte[] buf, int offset, int count){
        String str = "";
        int i;
        for(i=offset;i<buf.length && i<offset+count;i++){
            str += (i==offset+8)? "-":" "; // 8 byte separator
            str += new PrintfFormat("%02x").sprintf(((int)buf[i])&0xff);
        }
        System.out.print(str);
        return i-offset;
    }
    
    
    private boolean isPrintable(byte ch){
        return ch>0x1f && ch<=0x7f;
    }
    
    private void printSpace(int count){
        for(int i=0;i<count;i++)
            System.out.print(" ");
    }
    
    private int min(int a,int b){
        return (a<b)?a:b;
    }

    static public void setWidth(int width){
        _width = width;
    }


    /** Return String representation of bytes in input buffer. */
    static public String getAscii(byte[] buf, int offset, int count) 
	throws IllegalArgumentException {

        if(offset+count>buf.length)
            throw new IllegalArgumentException();
        
        if(count==0) count = Integer.MAX_VALUE;

        String str = "";
        int i;
        for(i=offset;i<buf.length && i<offset+count;i++){
            str += _p.isPrintable(buf[i])? new String(buf,i,1):".";
        }

	return str;
    }

    /** Print String representation of bytes in input buffer to stdout. */
    static public synchronized int printAscii(byte[] buf,int offset,int count) throws IllegalArgumentException{

        if(offset+count>buf.length)
            throw new IllegalArgumentException();
        
        if(count==0) count = Integer.MAX_VALUE;

        String str = "";
        int i;
        for(i=offset;i<buf.length && i<offset+count;i++){
            str += _p.isPrintable(buf[i])? new String(buf,i,1):".";
        }
        System.out.print(str);
        return i-offset;
    }


    /**
     * 
     * @param buf
     * @param offset
     * @param count
     */
     static  public synchronized void printFull(byte[] buf,int offset, int count) throws IllegalArgumentException {
        
        if(count==0) count = Integer.MAX_VALUE;

        int org = offset;
        while(offset<buf.length && (offset-org)<count){
            _p.printOffset(offset);
            _p.printSpace(2);
            int printed = _p.printHex(buf,offset,_p.min(count,_width));
            _p.printSpace((_width-printed)*3+4);
            _p.printAscii(buf,offset,printed);
            System.out.println("");
            if(_width!=printed) break;
            offset += _width;
        }
    }
}

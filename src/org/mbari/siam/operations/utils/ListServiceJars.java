/****************************************************************************/
/* Copyright 2005 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;



public class ListServiceJars
{

    public static void main(String[] args) 
    {
        ListServiceJars app = new ListServiceJars();
        
        try
        {
            app.execute();
        }
        catch(Exception e)
        {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void execute() throws Exception
    {
        String[] serviceJars = ServiceJarUtils.listServiceCode();
        
        if ( serviceJars == null )
        {
            System.out.println("no service code load");
            return;
        }
        
        for (int i =0; i < serviceJars.length; ++i)
            System.out.println(serviceJars[i]);

        return;
    }
}

    
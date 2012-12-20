/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
/*
 * Created on Mar 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
   DrawingPanel displays an image.
  
   *  * @author oreilly
   */
public class DrawingPanel extends JPanel implements ImageObserver {
	
    Image _image;
    int _imageWidth = 0;
    int _imageHeight = 0;
	
    public DrawingPanel(Image image) {
	_image = image;
	_image.getWidth(this);
	_image.getHeight(this);
		
	// Put a border around the panel
	setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    }
	
    /** (Re-)Set the panel's image. */
    public void setImage(Image image) {
	_image = image;
	_imageWidth = _image.getWidth(this);
	_imageHeight = _image.getHeight(this);
    }
	
    /** Display an image. **/
    public void paintComponent (Graphics g) {


	// First paint background
	super.paintComponent (g);
		
	// Use the image width& width to find the starting point
	int img_x = getSize ().width/2  - _image.getWidth(this) / 2;
	int img_y = getSize ().height/2 - _image.getHeight(this) / 2;
	img_x = 0;
	img_y = 0;
		
	//Draw image at centered in the middle of the panel
	g.drawImage (_image, img_x, img_y, this);
    } 
	
}

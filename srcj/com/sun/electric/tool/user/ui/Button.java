/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Button.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

/**
 * Depricated 23 Mar 2004.  Replaced by ToolBarButton.
 *
 * This class implements a button.
 * @author Willy Chung
 */
public class Button extends JButton 
{

    /*
    private JPopupMenu popupMenu;
	private Insets popupMenuButton;

	private Button(ImageIcon image)
	{
		super(image);
		setSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
		setBorderPainted(false);
	}

	private Button(String str, ImageIcon image)
	{
		super(str, image);
		setSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
		setBorderPainted(false);
	}

	private Button(String str)
	{
		super(str);
		setBorderPainted(false);
	}

	public static Button newInstance(ImageIcon image)
	{
		return new Button(image);
	}

	public static Button newInstance(String str)
	{
		return new Button(str);
	}

	public static Button newInstance(String str, ImageIcon image)
	{
		return new Button(str, image);
	}

	protected void processMouseEvent(MouseEvent e)
	{
		super.processMouseEvent(e);

		//in windows look and feel, flat and mouse roll are on as default and can't change it
		//if other than windows look and feel and wants to have flat and mouse roll feature on
		//process the mouse roll
		String str = (UIManager.getLookAndFeel()).getName();

		if(e.getID()==MouseEvent.MOUSE_ENTERED)
		{
			if(!this.isBorderPainted())
				this.setBorderPainted(true);
		}
		else
		{
			if(this.isBorderPainted())
				this.setBorderPainted(false);
		}

		//for popup menu 
		//event fired when pressed
		//needed to be modified so that mouse button is held not just pressed
		if(e.getID()==MouseEvent.MOUSE_PRESSED)
		{
			if(popupMenuButton!=null)
			{
				if(e.getY()>popupMenuButton.top && e.getY()<popupMenuButton.bottom &&
					e.getX()>popupMenuButton.left && e.getX()<popupMenuButton.right)
					{
						popupMenu.show(e.getComponent(),e.getX(),e.getY());
					}
			}
		}
	}

	//add popup menu button for a button
	public void addPopupMenu(JPopupMenu popup, Insets insets)
	{
		popupMenu = popup;
		
		if((insets.right-insets.left)>this.getSize().width || (insets.bottom - insets.top)>this.getHeight())
		{
			int temp = this.getSize().width;
			System.out.println("popup menu button cannot be bigger than actual button size");
			return;
		}
		popupMenuButton = insets;
	}
     */
}

/*
 * Created on 03-nov.-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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
 * @author Willy Chung
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Button extends JButton 
{
	public static final int STYLE_FLAT = 1;
	public static final int STYLE_RAISED = 2;
	public static final int STYLE_HOVER = 3;
	
	private boolean mousehover;
	private int buttonStyle;
	private JPopupMenu popupMenu;
	private Insets popupMenuButton;
	
	private Button(ImageIcon image)
	{
		super(image);
		setSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
		mousehover = true;
		buttonStyle = STYLE_HOVER;
	}
	
	private Button(String str, ImageIcon image)
	{
		super(str, image);
		setSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
		mousehover = true;
		buttonStyle = STYLE_HOVER;
	}
	
	private Button(String str)
	{
		super(str);
		mousehover = true;
		buttonStyle = STYLE_HOVER;
	}

	public static Button CreateButton(ImageIcon image)
	{
		return new Button(image);
	}
	
	public static Button CreateButton(String str)
	{
		return new Button(str);
	}
	
	public static Button CreateButton(String str, ImageIcon image)
	{
		return new Button(str, image);
	}
	
	//set mouse roll abillity
	public void setMouseHover(boolean bool)
	{
		mousehover = bool;
	}
	
	public boolean isMouseHover()
	{
		return mousehover;
	}
	
	protected void processMouseEvent(MouseEvent e)
	{
		super.processMouseEvent(e);
		
		//in windows look and feel, flat and mouse roll are on as default and can't change it
		//if other than windows look and feel and wants to have flat and mouse roll feature on
		//process the mouse roll
		String str = (UIManager.getLookAndFeel()).getName();
		if(str!="Windows")
		{
			if((mousehover==true) && (buttonStyle==STYLE_HOVER))
			{   
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
			}
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
	
	//return current button border style
	public int getButtonStyle()
	{
		return buttonStyle;
	}
	
	//set current button border style
	//doesn't work if look and feel is set
	public void setButtonStyle(int style)
	{
		buttonStyle = style;
		
		switch(style)
		{
			case STYLE_FLAT:
			case STYLE_HOVER:
				this.setBorderPainted(false);
				break;
			case STYLE_RAISED:
				this.setBorderPainted(true);
				break;
			default:
				this.setBorderPainted(true);
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
	
			
}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UIMenu.java
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

/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UIMenu extends JMenu 
{
	private JMenuItem menuItem = null;
	
	// constructor
	private UIMenu(String s)
	{
		super(s);
	}
	
	private UIMenu(String s, char mnemonic)
	{
		super(s);
		setMnemonic(mnemonic);
	}

	// factory function
	public static UIMenu CreateUIMenu(String s)
	{
		return new UIMenu(s);
	}
	
	public static UIMenu CreateUIMenu(String s, char mnemonic)
	{
		return new UIMenu(s, mnemonic);
	}
	
	// add menu item
	public void addMenuItem(String s, ActionListener action)
	{
		menuItem = new JMenuItem(s);
		menuItem.addActionListener(action);
		this.add(menuItem);
	}
	
	public void addMenuItem(String s, KeyStroke accelerator, ActionListener action)
	{
		menuItem = new JMenuItem(s);
		menuItem.setAccelerator(accelerator);
		menuItem.addActionListener(action);
		this.add(menuItem);
	}
}

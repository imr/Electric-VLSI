/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UIEditFrame.java
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

import javax.swing.*;
import javax.swing.event.*;

import com.sun.electric.database.hierarchy.Cell;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UIEditFrame extends JInternalFrame
{
	static int windowOffset = 0;
	UIEdit wnd;

	// constructor
	private UIEditFrame(Cell cell)
	{
		super(cell.describe(), true, true, true, true);
		setSize(500, 500);
		setLocation(windowOffset, windowOffset);
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;
		setAutoscrolls(true);
		wnd = UIEdit.CreateElectricDoc(cell);
		this.getContentPane().add(wnd);
		addInternalFrameListener(
			new InternalFrameAdapter()
			{
				public void internalFrameClosing(InternalFrameEvent evt) { windowClosed(); }
			}
		);
		show();
//		this.moveToFront();
//		this.toFront();
	}

	// factory
	public static UIEditFrame CreateEditWindow(Cell cell)
	{
		UIEditFrame frame = new UIEditFrame(cell);
		JDesktopPane desktop = UITopLevel.getDesktop();
		desktop.add(frame); 
		return frame;
	}

	public void windowClosed()
	{
		System.out.println("Closed");
	}

	public void redraw()
	{
		wnd.redraw();
	}
	
}

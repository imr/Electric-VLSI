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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;

import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;


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
	UITreeView tree;
	/** the offset of each new window on the screen */		private static List windowList = new ArrayList();

	// constructor
	private UIEditFrame(Cell cell)
	{
		// initialize the frame
		super(cell.describe(), true, true, true, true);
		setSize(700, 500);
		setLocation(windowOffset, windowOffset);
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;
		setAutoscrolls(true);

		// the right half: an edit window
		wnd = UIEdit.CreateElectricDoc(cell);

		// the left half: a cell explorer tree
		tree = UITreeView.CreateTreeView(Library.getExplorerTree(), wnd);

		// put them together into the frame
		JSplitPane js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		js.setRightComponent(wnd);
		js.setLeftComponent(tree);
		this.getContentPane().add(js);
		show();
		addInternalFrameListener(
			new InternalFrameAdapter()
			{
				public void internalFrameClosing(InternalFrameEvent evt) { windowClosed(); }
			}
		);
//		moveToFront();
//		toFront();

		// accumulate a list of current windows
		windowList.add(this);
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
		windowList.remove(this);
	}

	public UIEdit getEdit() { return wnd; }

	public static void explorerTreeChanged()
	{
		for(Iterator it = getWindows(); it.hasNext(); )
		{
			UIEditFrame uif = (UIEditFrame)it.next();
			uif.tree.repaint();
		}
	}

	public static Iterator getWindows()
	{
		return windowList.iterator();

	}
	public void setTimeTracking(boolean trackTime)
	{
		wnd.setTimeTracking(trackTime);
	}

	public void redraw()
	{
		wnd.redraw();
	}
	
}

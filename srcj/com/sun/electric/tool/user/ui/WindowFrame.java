/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WindowFrame.java
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
import com.sun.electric.tool.Job;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JDesktopPane;
import javax.swing.tree.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * This class defines an edit window, with a cell explorer on the left side.
 */
public class WindowFrame extends JInternalFrame
{
	/** the edit window part */							private EditWindow wnd;
	/** the tree view part */							private TreeView tree;
    /** root of explorer tree */                        private static DefaultMutableTreeNode root = new DefaultMutableTreeNode("EXPLORER");
	/** the offset of each new windows from the last */	private static int windowOffset = 0;
	/** the list of all windows on the screen */		private static List windowList = new ArrayList();

	// constructor
	private WindowFrame(Cell cell)
	{
		// initialize the frame
		super(cell.describe(), true, true, true, true);
		setSize(800, 500);
		setLocation(windowOffset, windowOffset);
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;
		setAutoscrolls(true);

		// the right half: an edit window
		wnd = EditWindow.CreateElectricDoc(cell);

		// the left half: a cell explorer tree in a scroll pane
        // new
        root.add(Library.getExplorerTree());
        root.add(Job.getExplorerTree());
        tree = TreeView.CreateTreeView(root, wnd);
        tree.setShowsRootHandles(true);
        explorerTreeChanged();
		// old
        //tree = TreeView.CreateTreeView(Library.getExplorerTree(), wnd);
		JScrollPane scrolledTree = new JScrollPane(tree);

		// put them together into the frame
		JSplitPane js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		js.setRightComponent(wnd);
		js.setLeftComponent(scrolledTree);
		js.setDividerLocation(220);
		this.getContentPane().add(js);
		show();
		addInternalFrameListener(
			new InternalFrameAdapter()
			{
				public void internalFrameClosing(InternalFrameEvent evt) { windowClosed(); }
			}
		);

		// accumulate a list of current windows
		windowList.add(this);
	}

	// factory
	public static WindowFrame createEditWindow(Cell cell)
	{
		WindowFrame frame = new WindowFrame(cell);

		JDesktopPane desktop = TopLevel.getDesktop();
		desktop.add(frame); 
		frame.moveToFront();
		return frame;
	}

	public static WindowFrame getCurrent()
	{
		for(Iterator it = windowList.iterator(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.isSelected()) return wf;
//			Component comp = wf.getFocusOwner();
//			if (comp != null) return wf;
		}
		return null;
	}

	private void windowClosed()
	{
		windowList.remove(this);
	}

	public EditWindow getEditWindow() { return wnd; }

	public static synchronized void explorerTreeChanged()
	{
		for(Iterator it = getWindows(); it.hasNext(); )
		{
			WindowFrame uif = (WindowFrame)it.next();
			uif.tree.updateUI();
		}
	}

	public static Iterator getWindows()
	{
		return windowList.iterator();
	}
	
}

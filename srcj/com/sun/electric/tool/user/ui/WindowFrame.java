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
import com.sun.electric.tool.user.ui.Button;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JInternalFrame;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JDesktopPane;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.tree.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * This class defines an edit window, with a cell explorer on the left side.
 */
public class WindowFrame
{
	/** the edit window part */							private EditWindow wnd;
	/** the tree view part */							private TreeView tree;
	/** the offset of each new windows from the last */	private static int windowOffset = 0;
	/** the list of all windows on the screen */		private static List windowList = new ArrayList();
	/** the internal frame (if MDI). */					private JInternalFrame jif;
	/** the top-level frame (if SDI). */				private TopLevel jf;
	/** the explorer part of a frame. */				private static DefaultMutableTreeNode root = new DefaultMutableTreeNode("Explorer");

	// constructor
	private WindowFrame() {}

	/**
	 * Routine to create a new window on the screen that displays a Cell.
	 * @param cell the cell to display.
	 * @return the WindowFrame that shows the Cell.
	 */
	public static WindowFrame createEditWindow(Cell cell)
	{
		final WindowFrame frame = new WindowFrame();

		// initialize the frame
		Dimension frameSize = new Dimension(800, 500);
		if (TopLevel.isMDIMode())
		{
			frame.jif = new JInternalFrame(cell.describe(), true, true, true, true);
			frame.jif.setSize(frameSize);
			frame.jif.setLocation(windowOffset+100, windowOffset);
			frame.jif.setAutoscrolls(true);
		} else
		{
			frame.jf = new TopLevel("Electric - "+cell.describe(), frameSize);
			frame.jf.setSize(frameSize);
			frame.jf.setLocation(windowOffset+100, windowOffset);
		}
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;

		// the right half: an edit window
		frame.wnd = EditWindow.CreateElectricDoc(cell);

		// the left half: an explorer tree in a scroll pane
		root.removeAllChildren();
		root.add(Library.getExplorerTree());
		root.add(Job.getExplorerTree());
		frame.tree = TreeView.CreateTreeView(root, frame.wnd);
		explorerTreeChanged();
		JScrollPane scrolledTree = new JScrollPane(frame.tree);
		JButton explorerButton = Button.newInstance(new ImageIcon(frame.getClass().getResource("IconExplorer.gif")));

		// put them together into the frame
		JSplitPane js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		js.setRightComponent(frame.wnd);
		js.setLeftComponent(scrolledTree);
		js.setDividerLocation(220);
		if (TopLevel.isMDIMode())
		{
			frame.jif.getContentPane().add(js);
			frame.jif.show();
			frame.jif.addInternalFrameListener(
				new InternalFrameAdapter()
				{
					public void internalFrameClosing(InternalFrameEvent evt) { frame.windowClosed(); }
				}
			);
			TopLevel.addToDesktop(frame.jif);
			frame.jif.moveToFront();
		} else
		{
			frame.jf.getContentPane().add(js);
			frame.jf.show();
			frame.jf.setEditWindow(frame.wnd);
		}
//		js.requestFocusInWindow();
//		js.setCorner(JScrollPane.LOWER_LEFT_CORNER, explorerButton);

		// accumulate a list of current windows
		windowList.add(frame);

		return frame;
	}

	/**
	 * Routine to record that this WindowFrame has been closed.
	 */
	private void windowClosed() { windowList.remove(this); }

	/**
	 * Routine to return the EditWindow associated with this frame.
	 * @return the EditWindow associated with this frame.
	 */
	public EditWindow getEditWindow() { return wnd; }

	/**
	 * Routine to return the JInternalFrame associated with this WindowFrame.
	 * This only makes sense in MDI mode, because in SDI mode, the WindowFrame is a JFrame.
	 * @return the JInternalFrame associated with this WindowFrame.
	 */
	public JInternalFrame getInternalFrame() { return jif; }

	/**
	 * Routine to return an Iterator over all WindowFrames.
	 * @return an Iterator over all WindowFrames.
	 */
	public static Iterator getWindows() { return windowList.iterator(); }

	/**
	 * Routine called when the explorer information changes.
	 * It updates the display.
     */
	public static synchronized void explorerTreeChanged()
	{
		for(Iterator it = getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.tree.updateUI();
		}
	}

    /**
	 * Routine called when the explorer information changes.
	 * It updates the display for minor changes.  See JTree.treeDidChange().
     */
	public static synchronized void explorerTreeMinorlyChanged()
	{
		for(Iterator it = getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
            wf.tree.treeDidChange();
		}
	}
  
}

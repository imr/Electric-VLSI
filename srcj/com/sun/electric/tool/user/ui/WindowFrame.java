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
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.Button;
import com.sun.electric.tool.user.ui.PaletteFrame;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JInternalFrame;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JDesktopPane;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This class defines an edit window, with a cell explorer on the left side.
 */
public class WindowFrame
{
	/** the edit window part */							private EditWindow wnd;
	/** the tree view part */							private ExplorerTree tree;
	/** the offset of each new windows from the last */	private static int windowOffset = 0;
	/** the list of all windows on the screen */		private static List windowList = new ArrayList();
	/** the current windows. */							private static WindowFrame curWindowFrame = null;
	/** the internal frame (if MDI). */					private JInternalFrame jif;
	/** the top-level frame (if SDI). */				private TopLevel jf;
	/** the explorer part of a frame. */				private static DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Explorer");
	/** the explorer part of a frame. */				private static DefaultTreeModel treeModel = null;

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
        String cellDescription = (cell == null) ? "no cell" : cell.describe();
		if (TopLevel.isMDIMode())
		{
			frame.jif = new JInternalFrame(cellDescription, true, true, true, true);
			frame.jif.setSize(frameSize);
			frame.jif.setLocation(windowOffset+150, windowOffset);
			frame.jif.setAutoscrolls(true);
		} else
		{
			frame.jf = new TopLevel("Electric - " + cellDescription, frameSize);
			frame.jf.setSize(frameSize);
			frame.jf.setLocation(windowOffset+150, windowOffset);
		}
		curWindowFrame = frame;
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;

		// the right half: an edit window
		frame.wnd = EditWindow.CreateElectricDoc(cell, frame);
//		JScrollPane scrolledWindow = new JScrollPane(frame.wnd, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		JComponent rightSide = frame.wnd;

		// the left half: an explorer tree in a scroll pane
		rootNode.removeAllChildren();
		rootNode.add(ExplorerTree.getLibraryExplorerTree());
		rootNode.add(Job.getExplorerTree());
		treeModel = new DefaultTreeModel(rootNode);
		frame.tree = ExplorerTree.CreateExplorerTree(rootNode, treeModel, frame.wnd);
		ExplorerTree.explorerTreeChanged();
		JScrollPane scrolledTree = new JScrollPane(frame.tree);
		JButton explorerButton = Button.newInstance(new ImageIcon(frame.getClass().getResource("IconExplorer.gif")));

		// put them together into the frame
		JSplitPane js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		js.setRightComponent(rightSide);
		js.setLeftComponent(scrolledTree);
		js.setDividerLocation(220);
		if (TopLevel.isMDIMode())
		{
			frame.jif.getContentPane().add(js);
			frame.jif.addInternalFrameListener(new InternalWindowsEvents(frame));
			frame.jif.show();
			TopLevel.addToDesktop(frame.jif);
//			frame.jif.moveToFront();
			try
			{
				frame.jif.setSelected(true);
			} catch (java.beans.PropertyVetoException e) {}
		} else
		{
			frame.jf.getContentPane().add(js);
			frame.jf.addWindowListener(new WindowsEvents(frame));
			frame.jf.addWindowFocusListener(new WindowsEvents(frame));
			frame.jf.setEditWindow(frame.wnd);
			frame.jf.show();
		}
//		js.requestFocusInWindow();
//		js.setCorner(JScrollPane.LOWER_LEFT_CORNER, explorerButton);

		// accumulate a list of current windows
		windowList.add(frame);

		setCurrentWindowFrame(frame);

		return frame;
	}

	/**
	 * Routine to get the current WindowFrame.
	 * @return the current WindowFrame.
	 */
	public static WindowFrame getCurrentWindowFrame() { return curWindowFrame; }

	/**
	 * Routine to set the current WindowFrame.
	 * @param wf the WindowFrame to make current.
	 */
	public static void setCurrentWindowFrame(WindowFrame wf)
	{
		curWindowFrame = wf;
		if (wf != null)
		{
			if (!TopLevel.isMDIMode())
				wf.jf.show();
			EditWindow wnd = wf.getEditWindow();
			Cell cell = wnd.getCell();
			if (cell != null)
			{
				cell.getLibrary().setCurCell(cell);

				// if auto-switching technology, do it
				Technology tech = cell.getTechnology();
				if (tech != null && tech != Technology.getCurrent())
				{
					if (User.isAutoTechnologySwitch())
					{
						tech.setCurrent();
						PaletteFrame pf = TopLevel.getPaletteFrame();
						pf.loadForTechnology();
					}
				}
			}
		}
	}

	/**
	 * Routine to record that this WindowFrame has been closed.
	 * This method is called from the event handlers on the windows.
	 */
	private void windowClosed()
	{
		windowList.remove(this);
		if (curWindowFrame == this) curWindowFrame = null;
	}

	/**
	 * Routine to return the EditWindow associated with this frame.
	 * @return the EditWindow associated with this frame.
	 */
	public EditWindow getEditWindow() { return wnd; }

	/**
	 * Routine to return the ExplorerTree associated with this frame.
	 * @return the ExplorerTree associated with this frame.
	 */
	public ExplorerTree getExplorerTree() { return tree; }

	/**
	 * Routine to return the JInternalFrame associated with this WindowFrame.
	 * This only makes sense in MDI mode, because in SDI mode, the WindowFrame is a JFrame.
	 * @return the JInternalFrame associated with this WindowFrame.
	 */
	public JInternalFrame getInternalFrame() { return jif; }

	/**
	 * Routine to return the JFrame associated with this WindowFrame.
	 * This only makes sense in SDI mode, because in MDI mode, the WindowFrame is a JInternalFrame.
	 * @return the JFrame associated with this WindowFrame.
	 */
	public JFrame getFrame() { return jf; }

	/**
	 * Routine to return an Iterator over all WindowFrames.
	 * @return an Iterator over all WindowFrames.
	 */
	public static Iterator getWindows() { return windowList.iterator(); }
    
    /**
     * Routine to set the description on the window frame
     */
    public void setTitle(String title)
    {
        if (TopLevel.isMDIMode())
            jif.setTitle(title);
        else
            jf.setTitle(title);
    }

	/**
	 * This class handles activation and close events for JFrame objects (used in SDI mode).
	 */
	static class WindowsEvents extends WindowAdapter
	{
		WindowFrame wf;

		WindowsEvents(WindowFrame wf)
		{
			super();
			this.wf = wf;
		}

		public void windowActivated(WindowEvent evt) { WindowFrame.setCurrentWindowFrame(wf); }

		public void windowClosing(WindowEvent evt)
		{
			if (windowList.size() <= 1)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot close the last window");
				return;
			}
			wf.windowClosed();
			if (!TopLevel.isMDIMode())
				wf.getFrame().dispose();
		}
	}

	/**
	 * This class handles activation and close events for JInternalFrame objects (used in MDI mode).
	 */
	static class InternalWindowsEvents extends InternalFrameAdapter
	{
		WindowFrame wf;

		InternalWindowsEvents(WindowFrame wf)
		{
			super();
			this.wf = wf;
		}

		public void internalFrameClosing(InternalFrameEvent evt) { wf.windowClosed(); }

		public void internalFrameActivated(InternalFrameEvent evt) { WindowFrame.setCurrentWindowFrame(wf); }
	}

}

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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLog;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.lang.ref.WeakReference;


/**
 * This class defines an edit window, with a cell explorer on the left side.
 */
public class WindowFrame
{
	/** This frame has a circuit editing window. */		public static final int DISPWINDOW = 0;
	/** This frame has a text editing window */			public static final int TEXTWINDOW = 1;
	/** This frame has a waveform editing window */		public static final int WAVEFORMWINDOW = 2;
	/** This frame has a 3D display window */			public static final int DISP3DWINDOW = 3;

	/** the circuit edit window part */					private int contents;
	/** the circuit edit window part */					private EditWindow wnd;
	/** the circuit edit window component. */			private JPanel circuitPanel;
	/** the bottom scrollbar on the edit window. */		private JScrollBar bottomScrollBar;
	/** the right scrollbar on the edit window. */		private JScrollBar rightScrollBar;
	/** the text edit window part */					private JTextArea textWnd;
	/** the text edit window component. */				private JScrollPane textPanel;
	/** the split pane that shows explorer and edit. */	private JSplitPane js;

	/** the tree view part */							private ExplorerTree tree;
	/** the offset of each new windows from the last */	private static int windowOffset = 0;
	/** the list of all windows on the screen */		private static List windowList = new ArrayList();
	/** the current windows. */							private static WindowFrame curWindowFrame = null;
	/** the internal frame (if MDI). */					private JInternalFrame jif;
	/** the top-level frame (if SDI). */				private TopLevel jf;
	/** the explorer part of a frame. */				private static DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Explorer");
	/** the explorer part of a frame. */				private static DefaultTreeModel treeModel = null;

	private static final int SCROLLBARRESOLUTION = 200;

	// constructor
	private WindowFrame() {}

	/**
	 * Method to create a new window on the screen that displays a Cell.
	 * @param cell the cell to display.
	 * @return the WindowFrame that shows the Cell.
	 */
    public static WindowFrame createEditWindow(Cell cell)
    {
        return createEditWindow(cell, null);
    }
    
	public static WindowFrame createEditWindow(Cell cell, GraphicsConfiguration gc)
	{
		final WindowFrame frame = new WindowFrame();

		// initialize the frame
        frame.createJFrame(cell, gc);
        
		curWindowFrame = frame;
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;

		// the right half: an edit window with scroll bars
		frame.circuitPanel = new JPanel(new GridBagLayout());

		// the horizontal scroll bar in the edit window
		int thumbSize = SCROLLBARRESOLUTION / 20;
		frame.bottomScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		frame.bottomScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 4);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		frame.circuitPanel.add(frame.bottomScrollBar, gbc);
		frame.bottomScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(frame));
		frame.bottomScrollBar.setValue(frame.bottomScrollBar.getMaximum()/2);

		// the vertical scroll bar in the edit window
		frame.rightScrollBar = new JScrollBar(JScrollBar.VERTICAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		frame.rightScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 4);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.VERTICAL;
		frame.circuitPanel.add(frame.rightScrollBar, gbc);
		frame.rightScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(frame));
		frame.rightScrollBar.setValue(frame.rightScrollBar.getMaximum()/2);

//		JButton explorerButton = Button.newInstance(new ImageIcon(frame.getClass().getResource("IconExplorer.gif")));
//		js.setCorner(JScrollPane.LOWER_LEFT_CORNER, explorerButton);

		// the edit window
		frame.wnd = EditWindow.CreateElectricDoc(null, frame);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = gbc.weighty = 1;
		frame.circuitPanel.add(frame.wnd, gbc);

		// the text edit window (for textual cells)
		frame.textWnd = new JTextArea();
		frame.textWnd.setTabSize(4);
		frame.textPanel = new JScrollPane(frame.textWnd);

		// the left half: an explorer tree in a scroll pane
		rootNode.removeAllChildren();
		rootNode.add(ExplorerTree.getLibraryExplorerTree());
		rootNode.add(Job.getExplorerTree());
		rootNode.add(ErrorLog.getExplorerTree());
		treeModel = new DefaultTreeModel(rootNode);
		frame.tree = ExplorerTree.CreateExplorerTree(rootNode, treeModel);
		ExplorerTree.explorerTreeChanged();
		JScrollPane scrolledTree = new JScrollPane(frame.tree);

		// put them together into the split pane
		frame.js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		frame.js.setRightComponent(frame.circuitPanel);
		frame.js.setLeftComponent(scrolledTree);
		frame.js.setDividerLocation(0.2);

        // Put everything into the frame
        frame.populateJFrame();
//		js.requestFocusInWindow();

		frame.wnd.setCell(cell, VarContext.globalContext);

		// accumulate a list of current windows
		windowList.add(frame);

		setCurrentWindowFrame(frame);

		return frame;
	}

    
    /**
     * Create the JFrame that will hold all the Components in 
     * this WindowFrame.
     */
    private void createJFrame(Cell cell, GraphicsConfiguration gc)
    {
		Dimension scrnSize = TopLevel.getScreenSize();
		Dimension frameSize = new Dimension(scrnSize.width * 4 / 5, scrnSize.height * 6 / 8);
		String cellDescription = (cell == null) ? "no cell" : cell.describe();
		if (TopLevel.isMDIMode())
		{
			jif = new JInternalFrame(cellDescription, true, true, true, true);
			jif.setSize(frameSize);
			jif.setLocation(windowOffset+150, windowOffset);
			jif.setAutoscrolls(true);
			jif.setFrameIcon(new ImageIcon(WindowFrame.class.getResource("IconElectric.gif")));
		} else
		{
			jf = new TopLevel("Electric - " + cellDescription, new Rectangle(frameSize), this, gc);
			jf.setSize(frameSize);
			jf.setLocation(windowOffset+150, windowOffset);
		}
    }        
    
    /**
     * Populate the JFrame with the Components
     */
    private void populateJFrame()
    {
		if (TopLevel.isMDIMode())
		{
			jif.getContentPane().add(js);
			jif.addInternalFrameListener(new InternalWindowsEvents(this));
			jif.show();
			TopLevel.addToDesktop(jif);
//			frame.jif.moveToFront();
			try
			{
				jif.setSelected(true);
			} catch (java.beans.PropertyVetoException e) {}
		} else
		{
			jf.getContentPane().add(js);
			jf.addWindowListener(new WindowsEvents(this));
			jf.addWindowFocusListener(new WindowsEvents(this));
			jf.setEditWindow(wnd);
			jf.show();
		}
    }
    
	/**
	 * Method to get the current WindowFrame.
	 * @return the current WindowFrame.
	 */
	public static WindowFrame getCurrentWindowFrame() { return curWindowFrame; }

	/**
	 * Method to set the current WindowFrame.
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
				PaletteFrame.autoTechnologySwitch(cell);
			}
		}
	}

	public void setWindowSize(Rectangle frameRect)
	{
		Dimension frameSize = new Dimension(frameRect.width, frameRect.height);
		if (TopLevel.isMDIMode())
		{
			jif.setSize(frameSize);
			jif.setLocation(frameRect.x, frameRect.y);
		} else
		{
			jf.setSize(frameSize);
			jf.setLocation(frameRect.x, frameRect.y);
		}
	}

	/**
	 * Method to record that this WindowFrame has been closed.
	 * This method is called from the event handlers on the windows.
	 */
	private void finished()
	{
        //System.out.println(this.getClass()+" being disposed of");
        // remove references to this
        windowList.remove(this);
		if (curWindowFrame == this) curWindowFrame = null;

        // tell EditWindow it's finished
        wnd.finished();

        if (!TopLevel.isMDIMode()) {
            // if SDI mode, TopLevel enclosing frame is closing, dispose of it
            ((TopLevel)jf).finished();
        }
    }

    
    /*
     * Do Not use this method.  It causes bad things to happen.
     *
     * Moves this WindowFrame to another display defined by <code>gc</code>
     * @param gc the GraphicsConfiguration that specifies the screen to move to
     *
    public void moveToDisplay(GraphicsConfiguration gc)
    {
        // only valid in SDI mode
        if (TopLevel.isMDIMode()) return;
        
        // get rid of old frame
        jf.dispose();
        jf.disposeOfMenuAndToolBar();
        jf = null;
        
        // create new frame on new screen, and populate it
        createJFrame(wnd.getCell(), gc);
        populateJFrame();
    } */
    
	/**
	 * Method to return the scroll bar resolution.
	 * This is the extent of the JScrollBar.
	 * @return the scroll bar resolution.
	 */
	public static int getScrollBarResolution() { return SCROLLBARRESOLUTION; }

	/**
	 * Method to return the horizontal scroll bar at the bottom of the edit window.
	 * @return the horizontal scroll bar at the bottom of the edit window.
	 */
	public JScrollBar getBottomScrollBar() { return bottomScrollBar; }

	/**
	 * Method to return the vertical scroll bar at the right side of the edit window.
	 * @return the vertical scroll bar at the right side of the edit window.
	 */
	public JScrollBar getRightScrollBar() { return rightScrollBar; }

	/**
	 * Method to return the EditWindow associated with this frame.
	 * @return the EditWindow associated with this frame.
	 */
	public EditWindow getEditWindow() { return wnd; }

	/**
	 * Method to return the text edit window associated with this frame.
	 * @return the text edit window associated with this frame.
	 */
	public JTextArea getTextEditWindow() { return textWnd; }

	/**
	 * Method to control the contents of this WindowFrame (text editing or circuit editing).
	 * @param contents TEXTWINDOW if text editing is to be shown; DISPWINDOW for circuit editing.
	 */
	public void setContent(int contents)
	{
		this.contents = contents;
		switch (contents)
		{
			case DISPWINDOW:
				js.setRightComponent(circuitPanel);
				break;
			case TEXTWINDOW:
				js.setRightComponent(textPanel);
				break;
		}
	}

	public int getContents() { return contents; }

	/**
	 * Method to return the ExplorerTree associated with this frame.
	 * @return the ExplorerTree associated with this frame.
	 */
	public ExplorerTree getExplorerTree() { return tree; }

	/**
	 * Method to return the JInternalFrame associated with this WindowFrame.
	 * This only makes sense in MDI mode, because in SDI mode, the WindowFrame is a JFrame.
	 * @return the JInternalFrame associated with this WindowFrame.
	 */
	public JInternalFrame getInternalFrame() { return jif; }

	/**
	 * Method to return the TopLevel associated with this WindowFrame.
	 * This only makes sense in SDI mode, because in MDI mode, the WindowFrame is a JInternalFrame.
	 * @return the TopLevel associated with this WindowFrame.
	 */
	public TopLevel getFrame() { return jf; }

	/**
	 * Method to return the number of WindowFrames.
	 * @return the number of WindowFrames.
	 */
	public static int getNumWindows() { return windowList.size(); }

	/**
	 * Method to return an Iterator over all WindowFrames.
	 * @return an Iterator over all WindowFrames.
	 */
	public static Iterator getWindows() { return windowList.iterator(); }

    /**
     * Method to set the description on the window frame
     */
    public void setTitle(String title)
    {
        if (TopLevel.isMDIMode())
            jif.setTitle(title);
        else
            jf.setTitle(title);
    }

	/**
	 * This class handles changes to the edit window scroll bars.
	 */
	static class ScrollAdjustmentListener implements AdjustmentListener
	{
        /** A weak reference to the WindowFrame */
		WeakReference wf;               

		ScrollAdjustmentListener(WindowFrame wf)
		{
			super();
			this.wf = new WeakReference(wf);
		}

		public void adjustmentValueChanged(AdjustmentEvent e)
		{
            WindowFrame frame = (WindowFrame)wf.get();
			EditWindow wnd = frame.getEditWindow();
			if (wnd == null) return;
			if (e.getSource() == frame.getBottomScrollBar())
				wnd.bottomScrollChanged(e.getValue());
			if (e.getSource() == frame.getRightScrollBar())
				wnd.rightScrollChanged(e.getValue());
		}
	}

	/**
	 * This class handles activation and close events for JFrame objects (used in SDI mode).
	 */
	static class WindowsEvents extends WindowAdapter
	{
        /** A weak reference to the WindowFrame */
		WeakReference wf;               

		WindowsEvents(WindowFrame wf)
		{
			super();
			this.wf = new WeakReference(wf);
		}

		public void windowActivated(WindowEvent evt) { WindowFrame.setCurrentWindowFrame((WindowFrame)wf.get()); }

		public void windowClosing(WindowEvent evt)
		{
            System.out.println("window closing");

			if (windowList.size() <= 1)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot close the last window");
				return;
			}
			((WindowFrame)wf.get()).finished();
		}
	}

	/**
	 * This class handles activation and close events for JInternalFrame objects (used in MDI mode).
	 */
	static class InternalWindowsEvents extends InternalFrameAdapter
	{
        /** A weak reference to the WindowFrame */
		WeakReference wf;               

		InternalWindowsEvents(WindowFrame wf)
		{
			super();
			this.wf = new WeakReference(wf);
		}

		public void internalFrameClosing(InternalFrameEvent evt) { ((WindowFrame)wf.get()).finished(); }

		public void internalFrameActivated(InternalFrameEvent evt) { WindowFrame.setCurrentWindowFrame((WindowFrame)wf.get()); }
	}

}

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
    /** the internal frame (if MDI). */					private JInternalFrame jif = null;
    /** the top-level frame (if SDI). */				private TopLevel jf = null;
    /** the internalframe listener */                   private InternalWindowsEvents internalWindowsEvents;
    /** the window event listener */                    private WindowsEvents windowsEvents;

	/** the tree view part */							private ExplorerTree tree;
	/** the offset of each new windows from the last */	private static int windowOffset = 0;
	/** the list of all windows on the screen */		private static List windowList = new ArrayList();
	/** the current windows. */							private static WindowFrame curWindowFrame = null;
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
        WindowFrame frame = new WindowFrame();
        frame.createEditWindow(cell, null);
        return frame;
    }

	private void createEditWindow(Cell cell, GraphicsConfiguration gc)
	{
		// the right half: an edit window with scroll bars
		circuitPanel = new JPanel(new GridBagLayout());

		// the horizontal scroll bar in the edit window
		int thumbSize = SCROLLBARRESOLUTION / 20;
		bottomScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		bottomScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 4);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		circuitPanel.add(bottomScrollBar, gbc);
		bottomScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(this));
		bottomScrollBar.setValue(bottomScrollBar.getMaximum()/2);

		// the vertical scroll bar in the edit window
		rightScrollBar = new JScrollBar(JScrollBar.VERTICAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		rightScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 4);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.VERTICAL;
		circuitPanel.add(rightScrollBar, gbc);
		rightScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(this));
		rightScrollBar.setValue(rightScrollBar.getMaximum()/2);

//		JButton explorerButton = Button.newInstance(new ImageIcon(frame.getClass().getResource("IconExplorer.gif")));
//		js.setCorner(JScrollPane.LOWER_LEFT_CORNER, explorerButton);

		// the edit window
		wnd = EditWindow.CreateElectricDoc(null, this);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = gbc.weighty = 1;
		circuitPanel.add(wnd, gbc);

		// the text edit window (for textual cells)
		textWnd = new JTextArea();
		textWnd.setTabSize(4);
		textPanel = new JScrollPane(textWnd);

		// the left half: an explorer tree in a scroll pane
		rootNode.removeAllChildren();
		rootNode.add(ExplorerTree.getLibraryExplorerTree());
		rootNode.add(Job.getExplorerTree());
		rootNode.add(ErrorLog.getExplorerTree());
		treeModel = new DefaultTreeModel(rootNode);
		tree = ExplorerTree.CreateExplorerTree(rootNode, treeModel);
		ExplorerTree.explorerTreeChanged();
		JScrollPane scrolledTree = new JScrollPane(tree);

		// put them together into the split pane
		js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		js.setRightComponent(circuitPanel);
		js.setLeftComponent(scrolledTree);
		js.setDividerLocation(0.2);


        // initialize the frame
        String cellDescription = (cell == null) ? "no cell" : cell.describe();
        createJFrame(cellDescription, gc);
        windowOffset += 70;
        if (windowOffset > 300) windowOffset = 0;

        // Put everything into the frame
        populateJFrame();
//		js.requestFocusInWindow();

		wnd.setCell(cell, VarContext.globalContext);

		// accumulate a list of current windows
		synchronized(windowList) {
            windowList.add(this);
        }
		setCurrentWindowFrame(this);
        wnd.fillScreen();
	}

    
    /**
     * Create the JFrame that will hold all the Components in 
     * this WindowFrame.
     */
    private void createJFrame(String title, GraphicsConfiguration gc)
    {
		Dimension scrnSize = TopLevel.getScreenSize();
		Dimension frameSize = new Dimension(scrnSize.width * 4 / 5, scrnSize.height * 6 / 8);
		if (TopLevel.isMDIMode())
		{
			jif = new JInternalFrame(title, true, true, true, true);
			jif.setSize(frameSize);
			jif.setLocation(windowOffset+150, windowOffset);
			jif.setAutoscrolls(true);
			jif.setFrameIcon(new ImageIcon(WindowFrame.class.getResource("IconElectric.gif")));
		} else
		{
			jf = new TopLevel("Electric - " + title, new Rectangle(frameSize), this, gc);
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
            internalWindowsEvents = new InternalWindowsEvents(this);
			jif.addInternalFrameListener(internalWindowsEvents);
			jif.show();
			TopLevel.addToDesktop(jif);
            // add tool bar as listener so it can find out state of cell history in EditWindow
            jif.addInternalFrameListener(TopLevel.getTopLevel().getToolBar());
            wnd.addPropertyChangeListener(TopLevel.getTopLevel().getToolBar());
//			frame.jif.moveToFront();
			try
			{
				jif.setSelected(true);
			} catch (java.beans.PropertyVetoException e) {}
		} else
		{
			jf.getContentPane().add(js);
            windowsEvents = new WindowsEvents(this);
			jf.addWindowListener(windowsEvents);
			jf.addWindowFocusListener(windowsEvents);
			jf.setEditWindow(wnd);
            // add tool bar as listener so it can find out state of cell history in EditWindow
            wnd.addPropertyChangeListener(EditWindow.propGoBackEnabled, ((TopLevel)jf).getToolBar());
            wnd.addPropertyChangeListener(EditWindow.propGoForwardEnabled, ((TopLevel)jf).getToolBar());
			jf.show();
		}
    }

    /**
     * Depopulate the JFrame.  Currently this is only used in SDI mode when
     * moving a WindowFrame from one display to another.  A new JFrame on the
     * new display must be created and populated with the WindowFrame Components.
     * To do so, those components must first be removed from the old frame.
     */
    private void depopulateJFrame()
    {
        if (TopLevel.isMDIMode()) {
            jif.getContentPane().remove(js);
            jif.removeInternalFrameListener(internalWindowsEvents);
            jif.removeInternalFrameListener(TopLevel.getTopLevel().getToolBar());
            // TODO: TopLevel.removeFromDesktop(jif);
            wnd.removePropertyChangeListener(TopLevel.getTopLevel().getToolBar());
        } else {
            jf.getContentPane().remove(js);
            jf.removeWindowListener(windowsEvents);
            jf.removeWindowFocusListener(windowsEvents);
            jf.setEditWindow(null);
            wnd.removePropertyChangeListener(EditWindow.propGoBackEnabled, ((TopLevel)jf).getToolBar());
            wnd.removePropertyChangeListener(EditWindow.propGoForwardEnabled, ((TopLevel)jf).getToolBar());
        }
    }


    public void moveEditWindow(GraphicsConfiguration gc) {

        if (TopLevel.isMDIMode()) return;           // only valid in SDI mode
        jf.hide();                                  // hide old Frame
        //jf.getFocusOwner().setFocusable(false);
        //System.out.println("Set unfocasable: "+jf.getFocusOwner());
        depopulateJFrame();                         // remove all components from old Frame
        TopLevel oldFrame = jf;
        oldFrame.finished();                        // clear and garbage collect old Frame
        Cell cell = wnd.getCell();                  // get current cell
        String cellDescription = (cell == null) ? "no cell" : cell.describe();  // new title
        createJFrame(cellDescription, gc);          // create new Frame
        populateJFrame();                           // populate new Frame
        wnd.fireCellHistoryStatus();                // update tool bar history buttons
    }

	/**
	 * Method to get the current WindowFrame.
	 * @return the current WindowFrame.
	 */
	public static WindowFrame getCurrentWindowFrame() { synchronized(windowList) { return curWindowFrame; } }

	/**
	 * Method to set the current WindowFrame.
	 * @param wf the WindowFrame to make current.
	 */
	public static void setCurrentWindowFrame(WindowFrame wf)
	{
        synchronized(windowList) {
		    curWindowFrame = wf;
        }
		if (wf != null)
		{
			//if (!TopLevel.isMDIMode())
			//	wf.jf.show();  // < ---- BAD BAD BAD BAD!!!!
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
	public void finished()
	{
        //System.out.println(this.getClass()+" being disposed of");
        // remove references to this
        synchronized(windowList) {
            if (windowList.size() <= 1)
            {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
                        "Cannot close the last window");
                return;
            }
            windowList.remove(this);
		    if (curWindowFrame == this) curWindowFrame = null;
        }

        // tell EditWindow it's finished
        wnd.finished();

        if (!TopLevel.isMDIMode()) {
            // if SDI mode, TopLevel enclosing frame is closing, dispose of it
            ((TopLevel)jf).finished();
        }
    }


    
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
	public static int getNumWindows() {
        synchronized(windowList) {
            return windowList.size();
        }
    }

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
        if (TopLevel.isMDIMode()) {
            if (jif != null) jif.setTitle(title);
        } else {
            if (jf != null) jf.setTitle(title);
        }
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

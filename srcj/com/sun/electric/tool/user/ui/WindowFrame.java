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

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Cursor;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.user.ErrorLog;
import com.sun.electric.tool.user.MenuCommands;
//import com.sun.electric.tool.user.ui.j3d.View3DWindow;
import com.sun.electric.database.variable.VarContext;

/**
 * This class defines an edit window, with a cell explorer on the left side.
 */
public class WindowFrame
{
	/** the nature of the main window part (from above) */	private WindowContent content;
	/** the text edit window part */					private JTextArea textWnd;
	/** the text edit window component. */				private JScrollPane textPanel;
	/** the split pane that shows explorer and edit. */	private JSplitPane js;
    /** the internal frame (if MDI). */					private JInternalFrame jif = null;
    /** the top-level frame (if SDI). */				private TopLevel jf = null;
    /** the internalframe listener */                   private InternalWindowsEvents internalWindowsEvents;
    /** the window event listener */                    private WindowsEvents windowsEvents;
	/** the tree view part */							public ExplorerTree tree;
	/** the explorer part of a frame. */				public DefaultMutableTreeNode rootNode;
	/** the library explorer part. */					public DefaultMutableTreeNode libraryExplorerNode;
	/** the job explorer part. */						public DefaultMutableTreeNode jobExplorerNode;
	/** the error explorer part. */						public DefaultMutableTreeNode errorExplorerNode;
	/** the signal explorer part. */					public DefaultMutableTreeNode signalExplorerNode;
	/** the explorer part of a frame. */				private DefaultTreeModel treeModel;

	/** the offset of each new windows from the last */	private static int windowOffset = 0;
	/** the list of all windows on the screen */		private static List windowList = new ArrayList();
	/** the current windows. */							private static WindowFrame curWindowFrame = null;

	/** current mouse listener */						public static MouseListener curMouseListener = ClickZoomWireListener.theOne;
    /** current mouse motion listener */				public static MouseMotionListener curMouseMotionListener = ClickZoomWireListener.theOne;
    /** current mouse wheel listener */					public static MouseWheelListener curMouseWheelListener = ClickZoomWireListener.theOne;
    /** current key listener */							public static KeyListener curKeyListener = ClickZoomWireListener.theOne;

	//******************************** CONSTRUCTION ********************************

	// constructor
	private WindowFrame()
	{
	}

	/**
	 * Method to create a new circuit-editing window on the screen that displays a Cell.
	 * @param cell the cell to display.
	 * @return the WindowFrame that shows the Cell.
	 */
	public static WindowFrame createEditWindow(Cell cell)
	{
		WindowFrame frame = new WindowFrame();
		if (cell != null && cell.getView().isTextView())
		{
			TextWindow tWnd = new TextWindow(cell, frame);
			frame.buildWindowStructure(tWnd, cell, null);
			setCurrentWindowFrame(frame);
			frame.populateJFrame();
			tWnd.fillScreen();
		} else
		{
			EditWindow eWnd = EditWindow.CreateElectricDoc(cell, frame);
			frame.buildWindowStructure(eWnd, cell, null);
			setCurrentWindowFrame(frame);
			frame.populateJFrame();
			eWnd.fillScreen();
		}
		return frame;
	}

	/**
	 * Method to create a new 3D view window on the screen for the given cell
	 * @param cell the cell to display.
	 * @return the WindowFrame that shows the Cell.
	 */
	public static WindowFrame create3DViewtWindow(Cell cell)
	{
		WindowFrame frame = new WindowFrame();

		//WindowContent vWnd1 = new View3DWindow(cell, frame);

		Class view3DClass;

		try
        {
            view3DClass = Class.forName("com.sun.electric.tool.user.ui.j3d.View3DWindow");

        } catch (ClassNotFoundException e)
        {
            System.out.println("Can't find 3D View module: " + e.getMessage());
			return frame;
        }

		try
		{
			Object vWnd = view3DClass.newInstance();
			Method constructor = view3DClass.getMethod("View3DWindow", new Class[] {String.class});
			constructor.invoke(vWnd, new Object[] {cell, frame});
			frame.buildWindowStructure((WindowContent)vWnd, cell, null);

			setCurrentWindowFrame(frame);
			frame.populateJFrame();
			//vWnd.fillScreen()
		} catch (Exception e) {
            System.out.println("Can't open 3D View window: " + e.getMessage());
        }

		return frame;
	}

	/**
	 * Method to create a new waveform window on the screen given the simulation data.
	 * @param sd the simulation data to use in the waveform window.
	 * @return the WindowFrame that shows the waveforms.
	 */
	public static WindowFrame createWaveformWindow(Simulate.SimData sd)
	{
		WindowFrame frame = new WindowFrame();
		WaveformWindow wWnd = new WaveformWindow(sd, frame);
		frame.buildWindowStructure(wWnd, sd.getCell(), null);
		setCurrentWindowFrame(frame);
		frame.populateJFrame();
		wWnd.fillScreen();
		return frame;
	}

	private void buildWindowStructure(WindowContent content, Cell cell, GraphicsConfiguration gc)
	{
		this.content = content;

		// the left half: an explorer tree in a scroll pane
		rootNode = new DefaultMutableTreeNode("Explorer");
		content.loadExplorerTree(rootNode);
		treeModel = new DefaultTreeModel(rootNode);
		tree = ExplorerTree.CreateExplorerTree(rootNode, treeModel);
		wantToRedoLibraryTree();
		JScrollPane scrolledTree = new JScrollPane(tree);

		// put them together into the split pane
		js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		js.setRightComponent(content.getPanel());
		js.setLeftComponent(scrolledTree);
		js.setDividerLocation(0.2);

		// initialize the frame
		String cellDescription = (cell == null) ? "no cell" : cell.describe();
		createJFrame(cellDescription, gc);
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;

		// Put everything into the frame
//		js.requestFocusInWindow();

		// accumulate a list of current windows
		synchronized(windowList) {
			windowList.add(this);
		}
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
			jif.addInternalFrameListener(TopLevel.getCurrentJFrame().getToolBar());
			//content.getPanel().addPropertyChangeListener(TopLevel.getTopLevel().getToolBar());
			content.getPanel().addPropertyChangeListener(EditWindow.propGoBackEnabled, TopLevel.getCurrentJFrame().getToolBar());
			content.getPanel().addPropertyChangeListener(EditWindow.propGoForwardEnabled, TopLevel.getCurrentJFrame().getToolBar());
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
			jf.setWindowFrame(this);
			// add tool bar as listener so it can find out state of cell history in EditWindow
			content.getPanel().addPropertyChangeListener(EditWindow.propGoBackEnabled, ((TopLevel)jf).getToolBar());
			content.getPanel().addPropertyChangeListener(EditWindow.propGoForwardEnabled, ((TopLevel)jf).getToolBar());
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
			jif.removeInternalFrameListener(TopLevel.getCurrentJFrame().getToolBar());
			// TopLevel.removeFromDesktop(jif);
			content.getPanel().removePropertyChangeListener(TopLevel.getCurrentJFrame().getToolBar());
		} else {
			jf.getContentPane().remove(js);
			jf.removeWindowListener(windowsEvents);
			jf.removeWindowFocusListener(windowsEvents);
			jf.setWindowFrame(null);
			content.getPanel().removePropertyChangeListener(EditWindow.propGoBackEnabled, ((TopLevel)jf).getToolBar());
			content.getPanel().removePropertyChangeListener(EditWindow.propGoForwardEnabled, ((TopLevel)jf).getToolBar());
		}
	}

	//******************************** WINDOW CONTROL ********************************

	/**
	 * Method to show a cell in the right-part of this WindowFrame.
	 * Handles both circuit cells and text cells.
	 * @param cell the Cell to display.
	 */
	public void setCellWindow(Cell cell)
	{
		if (cell != null && cell.getView().isTextView())
		{
			// want a TextWindow here
			if (!(getContent() instanceof TextWindow))
			{
				getContent().finished();
				content = new TextWindow(cell, this);
				int i = js.getDividerLocation();
				js.setRightComponent(content.getPanel());
				js.setDividerLocation(i);
				content.fillScreen();
				return;
			}
		} else
		{
			// want an EditWindow here
			if (!(getContent() instanceof EditWindow))
			{
				getContent().finished();
				content = EditWindow.CreateElectricDoc(cell, this);
				int i = js.getDividerLocation();
				js.setRightComponent(content.getPanel());
				js.setDividerLocation(i);
				content.fillScreen();
				return;
			}
		}
		content.setCell(cell, VarContext.globalContext);
	}

	public void moveEditWindow(GraphicsConfiguration gc) {

		if (TopLevel.isMDIMode()) return;           // only valid in SDI mode
		jf.hide();                                  // hide old Frame
		//jf.getFocusOwner().setFocusable(false);
		//System.out.println("Set unfocasable: "+jf.getFocusOwner());
		depopulateJFrame();                         // remove all components from old Frame
		TopLevel oldFrame = jf;
		oldFrame.finished();                        // clear and garbage collect old Frame
		Cell cell = content.getCell();                  // get current cell
		String cellDescription = (cell == null) ? "no cell" : cell.describe();  // new title
		createJFrame(cellDescription, gc);          // create new Frame
		populateJFrame();                           // populate new Frame
		content.fireCellHistoryStatus();                // update tool bar history buttons
	}

	//******************************** EXPLORER PART ********************************

	private boolean wantToRedoLibraryTree = false;
	private boolean wantToRedoJobTree = false;
	private boolean wantToRedoErrorTree = false;

	public static void wantToRedoLibraryTree()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.wantToRedoLibraryTree = true;
			wf.getContent().repaint();
		}
	}

	public static void wantToRedoJobTree()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.wantToRedoJobTree = true;
			wf.getContent().repaint();
		}
	}

	public static void wantToRedoErrorTree()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.wantToRedoErrorTree = true;
			wf.getContent().repaint();
		}
	}

	public void redoExplorerTreeIfRequested()
	{
		if (!wantToRedoLibraryTree && !wantToRedoJobTree && !wantToRedoErrorTree) return;

		// remember the state of the tree
		HashMap expanded = new HashMap();
		recursivelyCache(expanded, new TreePath(rootNode), true);

		// get the new library tree part
		if (wantToRedoLibraryTree)
			libraryExplorerNode = ExplorerTree.makeLibraryTree();
		if (wantToRedoJobTree)
			jobExplorerNode = Job.getExplorerTree();
		if (wantToRedoErrorTree)
			errorExplorerNode = ErrorLog.getExplorerTree();

		// rebuild the tree
		rootNode.removeAllChildren();
		if (libraryExplorerNode != null) rootNode.add(libraryExplorerNode);
		if (signalExplorerNode != null) rootNode.add(signalExplorerNode);
		rootNode.add(jobExplorerNode);
		rootNode.add(errorExplorerNode);

		tree.treeDidChange();
		treeModel.reload();
		recursivelyCache(expanded, new TreePath(rootNode), false);
	}

	private void recursivelyCache(HashMap expanded, TreePath path, boolean cache)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		Object obj = node.getUserObject();
		int numChildren = node.getChildCount();
		if (numChildren == 0) return;

		if (cache)
		{
			if (tree.isExpanded(path)) expanded.put(obj, obj);
		} else
		{
			if (expanded.get(obj) != null) tree.expandPath(path);
		}

		// now recurse
		for(int i=0; i<numChildren; i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
			TreePath descentPath = path.pathByAddingChild(child);
			if (descentPath == null) continue;
			recursivelyCache(expanded, descentPath, cache);
		}
	}

	//******************************** INTERFACE ********************************

	/**
	 * Method to set the content of this window.
	 * The content is the object in the right side (EditWindow, WaveformWindow, etc.)
	 * @param content the new content object.
	 */
//	public void setContent(WindowContent content)
//	{
//		this.content = content;
//
//		rootNode.removeAllChildren();
//		content.loadExplorerTree(rootNode);
//		js.setRightComponent(content.getPanel());
//	}

	/**
	 * Method to get the content of this window.
	 * The content is the object in the right side (EditWindow, WaveformWindow, etc.)
	 * @return the content of this window.
	 */
	public WindowContent getContent() { return content; }

	/**
	 * Method to get the current WindowFrame.
	 * @return the current WindowFrame.
	 */
	public static WindowFrame getCurrentWindowFrame() { synchronized(windowList) { return curWindowFrame; } }

	/**
	 * Method to set the current listener that responds to clicks in any window.
	 * There is a single listener in effect everywhere, usually controlled by the toolbar.
	 * @param listener the new lister to be in effect.
	 */
	public static void setListener(EventListener listener)
	{
		curMouseListener = (MouseListener)listener;
		curMouseMotionListener = (MouseMotionListener)listener;
		curMouseWheelListener = (MouseWheelListener)listener;
		curKeyListener = (KeyListener)listener;
	}

	/**
	 * Method to get the current listener that responds to clicks in any window.
	 * There is a single listener in effect everywhere, usually controlled by the toolbar.
	 * @return the current listener.
	 */
	public static EventListener getListener() { return curMouseListener; }

	/**
	 * Method to return the current Cell.
	 */
	public static Cell getCurrentCell()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return null;
		return wf.getContent().getCell();
	}

	/**
	 * Method to insist on a current Cell.
	 * Prints an error message if there is no current Cell.
	 * @return the current Cell in the current Library.
	 * Returns NULL if there is no current Cell.
	 */
	public static Cell needCurCell()
	{
		Cell curCell = getCurrentCell();
		if (curCell == null)
		{
			System.out.println("There is no current cell for this operation");
		}
		return curCell;
	}

	/**
	 * Method to set the cursor that is displayed in the WindowFrame.
	 * @param cursor the cursor to display here.
	 */
	public void setCursor(Cursor cursor)
	{
		content.getPanel().setCursor(cursor);
	}

	public static void removeLibraryReferences(Library lib)
	{
		for (Iterator it = getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			Cell cell = content.getCell();
			if (cell != null && cell.getLibrary() == lib)
				content.setCell(null, null);
			content.fullRepaint();
		}
	}

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
			Cell cell = wf.getContent().getCell();
			if (cell != null)
			{
				cell.getLibrary().setCurCell(cell);

				// if auto-switching technology, do it
				PaletteFrame.autoTechnologySwitch(cell);
			}
		}
		wantToRedoTitleNames();
	}

	public static void wantToRedoTitleNames()
	{
		// rebuild window titles
		for (Iterator it = getWindows(); it.hasNext(); )
		{
			WindowFrame w = (WindowFrame)it.next();
			WindowContent content = w.getContent();

			if (content != null) content.setWindowTitle();
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
			jf.validate();
		}
	}

	/**
	 * Method to record that this WindowFrame has been closed.
	 * This method is called from the event handlers on the windows.
	 */
	public void finished()
	{
        // remove references to this
        synchronized(windowList) {
            if (windowList.size() <= 1 && !TopLevel.isMDIMode() &&
				TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
            {
                MenuCommands.quitCommand();
                //JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
                //    "Cannot close the last window");
                return;
            }
            windowList.remove(this);
		    if (curWindowFrame == this) curWindowFrame = null;
        }

        // tell EditWindow it's finished
        content.finished();

        if (!TopLevel.isMDIMode()) {
            // if SDI mode, TopLevel enclosing frame is closing, dispose of it
            ((TopLevel)jf).finished();
        }
    }

	/**
	 * Method to return the WaveformWindow associated with this frame.
	 * @return the WaveformWindow associated with this frame.
	 */
	public WaveformWindow getWaveformWindow() { return (WaveformWindow)content; }

	/**
	 * Method to return the text edit window associated with this frame.
	 * @return the text edit window associated with this frame.
	 */
	public JTextArea getTextEditWindow() { return textWnd; }

	/**
	 * Method to return the ExplorerTree associated with this frame.
	 * @return the ExplorerTree associated with this frame.
	 */
	public ExplorerTree getExplorerTree() { return tree; }

	/**
	 * Method to return the TopLevel associated with this WindowFrame.
     * In SDI mode this returns this WindowFrame's TopLevel Frame.
     * In MDI mode there is only one TopLevel frame, so this method will
     * return that Frame.
	 * @return the TopLevel associated with this WindowFrame.
	 */
	public TopLevel getFrame() {
        if (TopLevel.isMDIMode()) {
            return TopLevel.getCurrentJFrame();
        }
        return jf;
    }

    /**
     * Returns true if this window frame or it's components generated
     * this event.
     * @param e the event generated
     * @return true if this window frame or it's components generated this
     * event, false otherwise.
     */
    public boolean generatedEvent(java.awt.AWTEvent e) {
        if (e instanceof InternalFrameEvent) {
            JInternalFrame source = ((InternalFrameEvent)e).getInternalFrame();
            if (source == jif) return true;
            else return false;
        }
        return false;
    }

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
	 * Centralized version of naming windows! Might move it to class
	 * that would replace WindowContext
	 * @param cell
	 * @param prefix
	 */
	public String composeTitle(Cell cell, String prefix)
	{
		// StringBuffer should be more efficient
		StringBuffer title = new StringBuffer();

		if (cell != null)
		{
			title.append(prefix + cell.describe());

			if (cell.getLibrary() != Library.getCurrent())
				title.append(" - Current library: " + Library.getCurrent().getName());
		}
		else
			title.append("***NONE***");
		return (title.toString());
	}
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

	//******************************** HANDLERS FOR WINDOW EVENTS ********************************

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

		public void windowActivated(WindowEvent evt)
		{
			WindowFrame.setCurrentWindowFrame((WindowFrame)wf.get());
		}

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

		public void internalFrameClosing(InternalFrameEvent evt)
		{
			((WindowFrame)wf.get()).finished();
		}

		public void internalFrameActivated(InternalFrameEvent evt)
		{
			WindowFrame.setCurrentWindowFrame((WindowFrame)wf.get());
		}
	}

}

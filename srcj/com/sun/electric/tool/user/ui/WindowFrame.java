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

import com.sun.electric.Main;
import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.menus.FileMenu;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * This class defines an edit window, with a cell explorer on the left side.
 */
public class WindowFrame extends Observable
{
	/** the nature of the main window part (from above) */	private WindowContent content;
	/** the text edit window part */					private JTextArea textWnd;
	/** the split pane that shows explorer and edit. */	private JSplitPane js;
    /** the internal frame (if MDI). */					private JInternalFrame jif = null;
    /** the top-level frame (if SDI). */				private TopLevel jf = null;
    /** the internalframe listener */                   private InternalWindowsEvents internalWindowsEvents;
    /** the window event listener */                    private WindowsEvents windowsEvents;
	/** the explorer tab */								private ExplorerTree explorerTab;
	/** the component tab */							private PaletteFrame paletteTab;
	/** the layers tab */								private LayerTab layersTab;
	/** the explorer part of a frame. */				public DefaultMutableTreeNode rootNode;
	/** the library explorer part. */					public DefaultMutableTreeNode libraryExplorerNode;
	/** the job explorer part. */						public DefaultMutableTreeNode jobExplorerNode;
	/** the error explorer part. */						public DefaultMutableTreeNode errorExplorerNode;
	/** the signal explorer part. */					public DefaultMutableTreeNode signalExplorerNode;
	/** the signal/sweep explorer part. */				public DefaultMutableTreeNode sweepExplorerNode;
	/** the explorer part of a frame. */				private DefaultTreeModel treeModel;
    /** true if this window is finished */              private boolean finished = false;

	/** the offset of each new windows from the last */	private static int windowOffset = 0;
	/** the list of all windows on the screen */		private static List windowList = new ArrayList();
	/** the current windows. */							private static WindowFrame curWindowFrame = null;

	/** current mouse listener */						public static MouseListener curMouseListener = ClickZoomWireListener.theOne;
    /** current mouse motion listener */				public static MouseMotionListener curMouseMotionListener = ClickZoomWireListener.theOne;
    /** current mouse wheel listener */					public static MouseWheelListener curMouseWheelListener = ClickZoomWireListener.theOne;
    /** current key listener */							public static KeyListener curKeyListener = ClickZoomWireListener.theOne;

    /** library tree updater */                         private static LibraryTreeUpdater libTreeUpdater = new LibraryTreeUpdater();


	/** Main class for 3D plugin */	                    private static final Class view3DClass = Resources.get3DMainClass();
    /** Create 3D view method */                        private static Method create3DMethod = null;


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

			// make sure the edit window has the right size
			eWnd.setScreenSize(eWnd.getSize());
			eWnd.repaintContents(null);
			eWnd.fillScreen();
		}
        removeUIBinding(frame.js, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        removeUIBinding(frame.js, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
		return frame;
	}

    /*****************************************************************************
     *          3D Stuff                                                         *
     *****************************************************************************/

	/**
	 * Method to create a new 3D view window on the screen for the given cell
	 * @param cell the cell to display.
	 * @param transPerNode
     * @return the WindowFrame that shows the Cell.
	 */
	public static WindowFrame create3DViewtWindow(Cell cell, WindowContent view2D, boolean transPerNode)
	{
        // 3D view can only be triggered by EditWindow instances
        if (!(view2D instanceof EditWindow)) return null;

		if (view3DClass == null)
		{
			System.out.println("3D View plugin not available");
			return null; // error in class initialization or no plugin available
		}

		WindowFrame frame = new WindowFrame();

		try
		{
            if (create3DMethod == null) create3DMethod = view3DClass.getDeclaredMethod("create3DWindow", new Class[] {Cell.class, WindowFrame.class,
                                                                                    WindowContent.class, Boolean.class}) ;
            Object vWnd = create3DMethod.invoke(view3DClass, new Object[] {cell, frame, view2D, new Boolean(transPerNode)});

            // null if max number of nodes is exceeded.
            if (vWnd == null) return null;
			frame.buildWindowStructure((WindowContent)vWnd, cell, null);
			setCurrentWindowFrame(frame);
			frame.populateJFrame();
		} catch (Exception e) {
            System.out.println("Can't open 3D View window: " + e.getMessage());
            ActivityLogger.logException(e);
        }

		return frame;
	}

	/**
	 * Method to access 3D view and highligh elements if view is available
	 * @param view2D
	 */
	public static void show3DHighlight(WindowContent view2D)
	{
        for(Iterator it = getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
            wf.setChanged();
            wf.notifyObservers(wf.getContent());
            wf.clearChanged();
        }
	}

    /*****************************************************************************
     *          END OF 3D Stuff                                                  *
     *****************************************************************************/

	/**
	 * Method to create a new waveform window on the screen given the simulation data.
	 * @param sd the simulation data to use in the waveform window.
	 * @return the WindowFrame that shows the waveforms.
	 */
	public static WindowFrame createWaveformWindow(Stimuli sd)
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
		explorerTab = ExplorerTree.CreateExplorerTree(rootNode, treeModel);
		JScrollPane scrolledTree = new JScrollPane(explorerTab);

		// make a tabbed list of panes on the left
		JTabbedPane tp = new JTabbedPane();
        // Only Mac version will align tabs on the left. The text orientation is vertical
        // by default on Mac
        if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
            tp.setTabPlacement(JTabbedPane.LEFT);
		paletteTab = PaletteFrame.newInstance(this);
		loadComponentMenuForTechnology();
		tp.add("Components", paletteTab.getTechPalette());

		tp.add("Explorer", scrolledTree);
		layersTab = new LayerTab();
		tp.add("Layers", layersTab.getContentPane());

		tp.setSelectedIndex(User.getDefaultWindowTab());
		tp.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
            	JTabbedPane tp = (JTabbedPane)evt.getSource();
            	User.setDefaultWindowTab(tp.getSelectedIndex());
            }
        });

		// put them together into the split pane
		js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		js.setRightComponent(content.getPanel());
		js.setLeftComponent(tp);
//		js.setDividerLocation(0.2);
		js.setDividerLocation(200);

		// initialize the frame
		String cellDescription = (cell == null) ? "no cell" : cell.describe();
		createJFrame(cellDescription, gc);
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;

		// accumulate a list of current windows
		synchronized(windowList) {
			windowList.add(this);
		}
	}

	private static final int WINDOW_OFFSET = 0;		// was 150

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
			jif.setLocation(windowOffset+WINDOW_OFFSET, windowOffset);
			jif.setAutoscrolls(true);
			jif.setFrameIcon(TopLevel.getFrameIcon());
		} else
		{
			jf = new TopLevel("Electric - " + title, new Rectangle(frameSize), this, gc);
			jf.setSize(frameSize);
			jf.setLocation(windowOffset+WINDOW_OFFSET, windowOffset);
		}
	}        

    /**
     * Set the Technology Palette (if shown) to the current technology.
     */
	public void loadComponentMenuForTechnology()
	{
		Technology tech = Technology.getCurrent();
		if (content.getCell() != null)
		{
			tech = content.getCell().getTechnology();
		}

		//Technology tech = Technology.findTechnology(User.getDefaultTechnology());
		paletteTab.loadForTechnology(tech, this);
	}

	public void addJS(JComponent js, int width, int height, int lowX, int lowY)
	{
		if (TopLevel.isMDIMode())
		{
//			JInternalFrame newJIF = new JInternalFrame();
//			newJIF.setBorder(new javax.swing.border.EmptyBorder(0,0,0,0));
//			newJIF.setSize(new Dimension(width, height));
//			newJIF.setLocation(300, 300);
//			newJIF.getContentPane().add(js);
//			newJIF.show();
//			TopLevel.addToDesktop(newJIF);
//			try
//			{
//				newJIF.setSelected(true);
//			} catch (java.beans.PropertyVetoException e) {}

			js.setSize(new Dimension(width, height));
			js.setBorder(new javax.swing.border.EmptyBorder(0,0,0,0));
			js.setLocation(lowX, lowY);
			js.setVisible(true);
			js.requestFocus();
			js.requestFocusInWindow();
			TopLevel.getDesktop().add(js, 0);
//			try
//			{
//				js.setSelected(true);
//			} catch (java.beans.PropertyVetoException e) {}
		} else
		{
			jf.getContentPane().add(js);
			if (!Main.BATCHMODE) jf.setVisible(true);
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
			// add tool bar as listener so it can find out state of cell history in EditWindow
			jif.addInternalFrameListener(TopLevel.getCurrentJFrame().getToolBar());
			//content.getPanel().addPropertyChangeListener(TopLevel.getTopLevel().getToolBar());
			content.getPanel().addPropertyChangeListener(EditWindow.propGoBackEnabled, TopLevel.getCurrentJFrame().getToolBar());
			content.getPanel().addPropertyChangeListener(EditWindow.propGoForwardEnabled, TopLevel.getCurrentJFrame().getToolBar());
//			frame.jif.moveToFront();
            TopLevel.addToDesktop(jif);
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
			if (!Main.BATCHMODE) jf.setVisible(true);
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
        Job.checkSwingThread();
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
        currentCellChanged();
	}

	public void moveEditWindow(GraphicsConfiguration gc) {

		if (TopLevel.isMDIMode()) return;           // only valid in SDI mode
		jf.setVisible(false);                       // hide old Frame
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
	private boolean wantToRedoSignalTree = false;

    /**
     * Method to request that the library tree be reloaded.
     */
	public static void wantToRedoLibraryTree()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.wantToRedoLibraryTree = true;
            wf.redoExplorerTreeIfRequested();
		}
	}

	public static void wantToRedoJobTree()
	{
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { wantToRedoJobTree(); }
            });
            return;
        }
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.wantToRedoJobTree = true;
			//wf.getContent().repaint();
            wf.redoExplorerTreeIfRequested();
		}
	}

	public static void wantToRedoErrorTree()
	{
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { wantToRedoErrorTree(); }
            });
            return;
        }
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
            wf.wantToRedoErrorTree = true;
			//wf.getContent().repaint();
            wf.redoExplorerTreeIfRequested();
		}
	}

	public void wantToRedoSignalTree()
	{
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { wantToRedoSignalTree(); }
            });
            return;
        }
        wantToRedoSignalTree = true;
		content.loadExplorerTree(rootNode);
		redoExplorerTreeIfRequested();
	}

	public void redoExplorerTreeIfRequested()
	{
        Job.checkSwingThread();
		if (!wantToRedoLibraryTree && !wantToRedoJobTree && !wantToRedoErrorTree && !wantToRedoSignalTree) return;

		// remember the state of the tree
		HashSet expanded = new HashSet();
		recursivelyCache(expanded, new TreePath(rootNode), true);

		// get the new library tree part
		if (wantToRedoLibraryTree)
			libraryExplorerNode = ExplorerTree.makeLibraryTree();
		if (wantToRedoJobTree)
			jobExplorerNode = Job.getExplorerTree();
		if (wantToRedoErrorTree)
			errorExplorerNode = ErrorLogger.getExplorerTree();
		wantToRedoLibraryTree = wantToRedoJobTree = wantToRedoErrorTree = wantToRedoSignalTree = false;

		// rebuild the tree
		rootNode.removeAllChildren();
		if (libraryExplorerNode != null) rootNode.add(libraryExplorerNode);
		if (signalExplorerNode != null) rootNode.add(signalExplorerNode);
		if (sweepExplorerNode != null) rootNode.add(sweepExplorerNode);
		rootNode.add(jobExplorerNode);
		rootNode.add(errorExplorerNode);

		explorerTab.treeDidChange();
		treeModel.reload();
		recursivelyCache(expanded, new TreePath(rootNode), false);
	}

	private void recursivelyCache(HashSet expanded, TreePath path, boolean cache)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		Object obj = node.getUserObject();
		int numChildren = node.getChildCount();
		if (numChildren == 0) return;

		if (cache)
		{
			if (explorerTab.isExpanded(path)) expanded.add(obj);
		} else
		{
			if (expanded.contains(obj)) explorerTab.expandPath(path); else
			{
				if (obj instanceof Library)
				{
					Library lib = (Library)obj;
					if (lib == Library.getCurrent()) explorerTab.expandPath(path);
				} else if (obj instanceof String)
				{
					String msg = (String)obj;
					if (msg.equalsIgnoreCase("libraries") || msg.equalsIgnoreCase("signals"))
						explorerTab.expandPath(path);
				}
			}
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
	 * Method to get the content of this window.
	 * The content is the object in the right side (EditWindow, WaveformWindow, etc.)
	 * @return the content of this window.
	 */
	public WindowContent getContent() { return content; }

	/**
	 * Method to get the current WindowFrame. If there is no current
     * WindowFrame, then it will create a new EditWindow window frame.
	 * @return the current WindowFrame.
	 */
	public static WindowFrame getCurrentWindowFrame() {
        return getCurrentWindowFrame(true);
    }

    /**
     * Method to get the current WindowFrame. If 'makeNewFrame' is true,
     * then this will make a new EditWindow window frame if there is no
     * current frame. If 'makeNewFrame' is false, this will return null
     * if there is no current WindowFrame.
     * @param makeNewFrame whether or not to make a new WindowFrame if no current frame
     * @return the current WindowFrame. May return null if 'makeNewFrame' is false.
     */
    public static WindowFrame getCurrentWindowFrame(boolean makeNewFrame) {
        synchronized(windowList) {
            if ((curWindowFrame == null) && makeNewFrame) {
                for (Iterator it = windowList.iterator(); it.hasNext(); ) {
                    // get last in list
                    curWindowFrame = (WindowFrame)it.next();
                }
                if (curWindowFrame == null)
                    curWindowFrame = createEditWindow(null);
            }
            return curWindowFrame;
        }
    }

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
		if (Main.BATCHMODE)
		{
			Library lib = Library.getCurrent();
			if (lib == null) return null;
			return lib.getCurCell();
		}
		WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
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
			System.out.println("There is no current cell for this operation.  To create one, use the 'New Cell' command from the 'Cell' menu.");
		}
		return curCell;
	}

	/**
	 * Method to set the cursor that is displayed in the WindowFrame.
	 * @param cursor the cursor to display here.
	 */
	public void setCursor(Cursor cursor)
	{
        if (!TopLevel.isMDIMode()) {
            jf.setCursor(cursor);
        }
        js.setCursor(cursor);
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
     * Method to request focus on this window
     */
/*
    public void requestFocus() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { requestFocusUnsafe(); }
            });
            return;
        }
        requestFocusUnsafe();
    }

    private void requestFocusUnsafe() {
        if (jif != null) {
            jif.toFront();
            try {
                jif.setSelected(true);
            } catch (java.beans.PropertyVetoException e) {}
        }
        if (jf != null) {
            jf.toFront();
            jf.requestFocus();
        }
    }

    private boolean isFocusOwner() {
        if (jif != null) return jif.isSelected();
        if (jf != null) return jf.isFocusOwner();
        return false;
    }
*/

	/**
	 * Method to set the current WindowFrame.
	 * @param wf the WindowFrame to make current.
	 */
	private static void setCurrentWindowFrame(WindowFrame wf)
	{
        synchronized(windowList) {
            if (wf.finished) return;            // don't do anything, the window is finished
            curWindowFrame = wf;
            // set this to be last in list
            windowList.remove(wf);
            windowList.add(wf);
        }
		if (wf != null)
		{
            wf.currentCellChanged();
            if (wf.getContent() != null) {
                Highlighter highlighter = wf.getContent().getHighlighter();
                if (highlighter != null) highlighter.gainedFocus();
            }
		}
		wantToRedoTitleNames();
	}

    private void currentCellChanged() {
        if (this != getCurrentWindowFrame(false)) return;
        Cell cell = getContent().getCell();
        if (cell != null)
        {
            cell.getLibrary().setCurCell(cell);

            // if auto-switching technology, do it
            PaletteFrame.autoTechnologySwitch(cell, this);
        }
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
        // if this was called from the code, instead of an event handler,
        // make sure we're not visible anymore
        if (TopLevel.isMDIMode()) {
            jif.setVisible(false);
        }

        // remove references to this
        synchronized(windowList) {
            if (windowList.size() <= 1 && !TopLevel.isMDIMode() &&
				TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
            {
                FileMenu.quitCommand();
                //JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
                //    "Cannot close the last window");
                return;
            }
            windowList.remove(this);
            finished = true;
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

//	/**
//	 * Method to return the Explorer tab associated with this WindowFrame.
//	 * @return the Explorer tab associated with this WindowFrame.
//	 */
//	public ExplorerTree getExplorerTab() { return explorerTab; }

	/**
	 * Method to return the component palette associated with this WindowFrame.
	 * @return the component palette associated with this WindowFrame.
	 */
	public PaletteFrame getPaletteTab() { return paletteTab; }

	/**
	 * Method to return the layer visibility tab associated with this WindowFrame.
	 * @return the layer visibility tab associated with this WindowFrame.
	 */
	public LayerTab getLayersTab() { return layersTab; }

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
	public static Iterator getWindows() {
        ArrayList listCopy = new ArrayList();
        synchronized(windowList) {
            listCopy.addAll(windowList);
        }
        return listCopy.iterator();
    }

	/**
	 * Centralized version of naming windows. Might move it to class
	 * that would replace WindowContext
	 * @param cell the cell in the window.
	 * @param prefix a prefix for the title.
	 */
	public String composeTitle(Cell cell, String prefix, int pageNo)
	{
		// StringBuffer should be more efficient
		StringBuffer title = new StringBuffer();

		if (cell != null)
		{
			title.append(prefix + cell.libDescribe());

			if (cell.isMultiPage())
			{
				title.append(" - Page " + (pageNo+1));
			}
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
        String curTitle = getTitle();
        if (title.equals(curTitle)) return;

        if (TopLevel.isMDIMode()) {
            if (jif != null) jif.setTitle(title);
        } else {
            if (jf != null) jf.setTitle(title);
        }
    }

    /**
     * Method to get the description on the window frame
     */
    public String getTitle()
    {
        if (TopLevel.isMDIMode()) {
            if (jif != null) return jif.getTitle();
        } else {
            if (jf != null) return jf.getTitle();
        }
        return "";
    }

    private static void removeUIBinding(JComponent comp, KeyStroke key) {
        removeUIBinding(comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT), key);
        removeUIBinding(comp.getInputMap(JComponent.WHEN_FOCUSED), key);
        removeUIBinding(comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW), key);
    }
    private static void removeUIBinding(InputMap map, KeyStroke key) {
        if (map == null) return;
        map.remove(key);
        removeUIBinding(map.getParent(), key);
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

    /**
     * Database change listener that updates library trees when needed
     */
    private static class LibraryTreeUpdater implements DatabaseChangeListener {

        private LibraryTreeUpdater() { Undo.addDatabaseChangeListener(this); }

        private void updateLibraryTrees() {
            for (Iterator it = WindowFrame.getWindows(); it.hasNext(); ) {
                WindowFrame frame = (WindowFrame)it.next();
                frame.wantToRedoLibraryTree = true;
                frame.redoExplorerTreeIfRequested();
            }
        }

        public void databaseChanged(DatabaseChangeEvent e)
        {
            if (e.cellTreeChanged())
                updateLibraryTrees();
        }

//         public void databaseEndChangeBatch(Undo.ChangeBatch batch)
//         {
//             boolean changed = false;
//             for (Iterator it = batch.getChanges(); it.hasNext(); )
//             {
//                 Undo.Change change = (Undo.Change)it.next();
//                 if (change.getType() == Undo.Type.LIBRARYKILL ||
//                     change.getType() == Undo.Type.LIBRARYNEW ||
//                     change.getType() == Undo.Type.CELLKILL ||
//                     change.getType() == Undo.Type.CELLNEW ||
//                     change.getType() == Undo.Type.CELLGROUPMOD ||
//                     (change.getType() == Undo.Type.OBJECTRENAME && change.getObject() instanceof Cell) ||
// 					(change.getType() == Undo.Type.VARIABLENEW && change.getObject() instanceof Cell && ((Variable)change.getO1()).getKey() == Cell.MULTIPAGE_COUNT_KEY))
//                 {
//                     changed = true;
//                     break;
//                 }
//             }
//             if (changed)
//                 updateLibraryTrees();
//         }

//         public void databaseChanged(Undo.Change evt) {}
//         public boolean isGUIListener() { return true; }
    }

}

/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExplorerTree.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.cvspm.AddRemove;
import com.sun.electric.tool.cvspm.CVS;
import com.sun.electric.tool.cvspm.CVSLibrary;
import com.sun.electric.tool.cvspm.Commit;
import com.sun.electric.tool.cvspm.Edit;
import com.sun.electric.tool.cvspm.Log;
import com.sun.electric.tool.cvspm.State;
import com.sun.electric.tool.cvspm.Update;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.EpicOut.EpicAnalysis;
import com.sun.electric.tool.project.AddCellJob;
import com.sun.electric.tool.project.AddLibraryJob;
import com.sun.electric.tool.project.CancelCheckOutJob;
import com.sun.electric.tool.project.CheckInJob;
import com.sun.electric.tool.project.CheckOutJob;
import com.sun.electric.tool.project.DeleteCellJob;
import com.sun.electric.tool.project.HistoryDialog;
import com.sun.electric.tool.project.Project;
import com.sun.electric.tool.project.UpdateJob;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.CellChangeJobs;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.ChangeCellGroup;
import com.sun.electric.tool.user.dialogs.ChangeCurrentLib;
import com.sun.electric.tool.user.dialogs.CrossLibCopy;
import com.sun.electric.tool.user.dialogs.EModelessDialog;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.menus.CellMenu;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.SweepSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Class to display a cell explorer tree-view of the database.
 */
public class ExplorerTree extends JTree implements DragSourceListener // , DragGestureListener
{
	private final static TreePath[] NULL_TREE_PATH_ARRAY = {};

    private TreeHandler handler = null;
	private final String rootNode;
    private TreePath [] currentSelectedPaths = new TreePath[0];
    private static FindCellDialog theCellSearchDialog = null;

	private static class IconGroup
	{
		/** the icon for a normal cell */					private ImageIcon regular;
		/** the icon for an old version of a cell */		private ImageIcon old;
		/** the icon for a checked-in cell */				private ImageIcon available;
		/** the icon for a cell checked-out to others */	private ImageIcon locked;
		/** the icon for a cell checked-out to you */		private ImageIcon unlocked;
	}
	private static ImageIcon iconLibrary = null, iconLibraryNormal = null, iconLibraryChecked = null;
	private static ImageIcon iconGroup = null;
	private static ImageIcon iconJobs = null;
	private static ImageIcon iconLibraries = null;
	private static ImageIcon iconErrors = null;
	private static ImageIcon iconErrorMsg = null;
    private static ImageIcon iconWarnMsg = null;
	private static ImageIcon iconSignals = null;
	private static ImageIcon iconSweeps = null;
	private static ImageIcon iconMeasurements = null;
	private static ImageIcon iconViewMultiPageSchematics = null;
	private static ImageIcon iconSpiderWeb = null;
	private static ImageIcon iconLocked = null;
	private static ImageIcon iconUnlocked = null;
	private static ImageIcon iconAvailable = null;

	/**
	 * Constructor to create a new ExplorerTree.
	 * @param contentNodes the tree to display.
	 */
	ExplorerTree(List<MutableTreeNode> contentNodes)
	{
		super((TreeModel)null);
        setModel(new ExplorerTreeModel());
        rootNode = ExplorerTreeModel.rootNode;
//		ErrorLoggerTree.updateExplorerTree(model().errorExplorerNode);
        redoContentTrees(contentNodes);

		initDND();

        // Starting Job explorer tree expanded
        expandPath(new TreePath(model().jobPath));

		// arbitrary selection in the explorer
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		// do not show top-level
		setRootVisible(false);
		setShowsRootHandles(true);
		setToggleClickCount(3);

		// enable tool tips - we'll use these to display useful info
		ToolTipManager.sharedInstance().registerComponent(this);

		// register our own extended renderer for custom icons and tooltips
		setCellRenderer(new MyRenderer());
        handler = new TreeHandler(this);
		addMouseListener(handler);
		addTreeSelectionListener(handler);
	}

	/**
	 * Method to return the currently selected objects in the explorer tree.
	 * @return the currently selected objects in the explorer tree.
	 */
	public Object [] getCurrentlySelectedObject()
	{
        Object[] selectedObjects = new Object[numCurrentlySelectedObjects()];
        for (int i = 0; i < selectedObjects.length; i++)
            selectedObjects[i] = getCurrentlySelectedObject(i);
        return selectedObjects;
    }

	/**
	 * Method to return the number of currently selected objects in the explorer tree.
	 * @return the number of currently selected objects in the explorer tree.
	 */
	public int numCurrentlySelectedObjects() { return currentSelectedPaths.length; }

	/**
	 * Method to return the currently selected object in the explorer tree.
     * @param i index of currently selected object.
	 * @return the currently selected object in the explorer tree.
	 */
	public Object getCurrentlySelectedObject(int i)
	{
        if (i >= currentSelectedPaths.length) return null;
        TreePath treePath = currentSelectedPaths[i];
        return getObjectInTreePath(treePath);
    }

	/**
	 * Method to get the object stored at a point in the explorer tree.
	 * @param treePath the TreePath in the explorer.
	 * @return the object stored there.
	 */
	private Object getObjectInTreePath(TreePath treePath)
	{
        if (treePath == null) return null;
        Object obj = treePath.getLastPathComponent();
        if (obj instanceof DefaultMutableTreeNode)
            return ((DefaultMutableTreeNode)obj).getUserObject();
        if (obj instanceof EpicAnalysis.EpicTreeNode) {
            Signal sig = EpicAnalysis.getSignal(treePath);
            if (sig != null) return sig;
        }
        return obj;
	}

	/**
     * Get a list of any libraries current selected.
     * @return list of libraries, or empty list if none selected
     */
    public List<Library> getCurrentlySelectedLibraries()
    {
        List<Library> libs = new ArrayList<Library>();
        for (int i=0; i<numCurrentlySelectedObjects(); i++) {
            Object obj = getCurrentlySelectedObject(i);
            if (obj instanceof Library) libs.add((Library)obj);
        }
        return libs;
    }

    /**
     * Get a list of any cells current selected.  Any
     * cell groups selected will have their cells added.
     * @return list of cells, or empty list if none selected
     */
    public List<Cell> getCurrentlySelectedCells()
    {
        List<Cell> cells = new ArrayList<Cell>();
        for (int i=0; i<numCurrentlySelectedObjects(); i++) {
            Object obj = getCurrentlySelectedObject(i);
			if (obj instanceof ExplorerTreeModel.CellAndCount)
				obj = ((ExplorerTreeModel.CellAndCount)obj).getCell();
            if (obj instanceof Cell) cells.add((Cell)obj);
            if (obj instanceof Cell.CellGroup) {
                Cell.CellGroup group = (Cell.CellGroup)obj;
                for (Iterator<Cell> it = group.getCells(); it.hasNext(); )
                    cells.add(it.next());
            }
        }
        return cells;
    }

	/**
	 * Method to set the currently selected object in the explorer tree.
	 * param obj the currently selected object in the explorer tree.
	 */
	public void clearCurrentlySelectedObjects()
	{
		currentSelectedPaths = NULL_TREE_PATH_ARRAY;
	}

	/**
	 * Method to return the currently selected object in the explorer tree.
	 * @return the currently selected object in the explorer tree.
	 */
	public ExplorerTreeModel model() { return (ExplorerTreeModel)treeModel; }

	/**
	 * Method to force the explorer tree to show the current library or signals list.
     * @param lib library to expand
     * @param cell
     * @param openLib true to open the current library, false to open the signals list.
     */
	void openLibraryInExplorerTree(Library lib, Cell cell, boolean openLib)
	{
        int count = -1; // starts from EXPLORER node
        openLibraryInExplorerTree(lib, cell, new TreePath(rootNode), openLib, count);
    }

    /**
     * Method to count rows to given cell considering possible cell groups and versions.
     * @param cell
     * @param treeModel
     * @param path
     * @param node
     * @param count
     * @return
     */
    private int countChildrenAndExpandInPath(Cell cell, TreeModel treeModel, TreePath path, Object node, int count)
    {
        int numChildren = treeModel.getChildCount(node);
        for (int i = 0; i < numChildren; i++)
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)treeModel.getChild(node, i);
            count++;
            Object obj = child.getUserObject();

            if (obj == cell)
                return count; // found location in library

            // Obj represents the latest version of the given cell and the given cell is an older version.
            // treeModel.getChildCount(child) > 0 otherwise it will go down every single version.
            if (treeModel.getChildCount(child) > 0 && obj instanceof Cell && ((Cell)obj).getCellGroup() == cell.getCellGroup())
            {
                TreePath descentPath = path.pathByAddingChild(child);
                expandPath(descentPath);
                return countChildrenAndExpandInPath(cell, treeModel, descentPath, child, count);
            }
            // Obj represents the cell group
            if (obj == cell.getCellGroup())
            {
                TreePath descentPath = path.pathByAddingChild(child);
                expandPath(descentPath);
                return countChildrenAndExpandInPath(cell, treeModel, descentPath, child, count);
            }

        }
        return count;
    }

	/**
	 * Method to recursively scan the explorer tree and open the current library or signals list.
     * @param library the library to open
     * @param cell
     * @param path the current position in the explorer tree.
     * @param openLib true for libraries, false for waveforms
     */
	private boolean openLibraryInExplorerTree(Library library, Cell cell, TreePath path, boolean openLib, int count)
	{
        TreeModel treeModel = model();
        Object node = path.getLastPathComponent();
        Object obj = node;
        if (obj instanceof DefaultMutableTreeNode)
            obj = ((DefaultMutableTreeNode)obj).getUserObject();
		int numChildren = treeModel.getChildCount(node);
		if (numChildren == 0) return false;

		if (openLib && (obj instanceof Library))
		{
			Library lib = (Library)obj;
            // Only expands library and its node. Doesn't contineu with rest of nodes in Explorer
			if (lib == library)
            {
                expandPath(path);
                // Counting position from library to cell selected
                if (cell != null)
                {
                    count = countChildrenAndExpandInPath(cell, treeModel, path, node, count);
                }
                setSelectionRow(count);
                return true; // found location in explorer
            }
		} else if (obj instanceof String)
		{
			String msg = (String)obj;
			if ((msg.equalsIgnoreCase("libraries") && openLib) ||
				(msg.equalsIgnoreCase("signals") && !openLib))
					expandPath(path);
		}

		// now recurse
		for(int i=0; i<numChildren; i++)
		{
            Object child = treeModel.getChild(node, i);
            if (!(child instanceof DefaultMutableTreeNode)) continue;
			TreePath descentPath = path.pathByAddingChild(child);
			if (descentPath == null) continue;
            count++;
			if (openLibraryInExplorerTree(library, cell, descentPath, openLib, count))
                return true;
		}
        return false;
	}

//	private class RedoLater implements Runnable
//	{
//		private List<MutableTreeNode> contentNodes;
//
//		public RedoLater(List<MutableTreeNode> contentNodes) { this.contentNodes = contentNodes; }
//		public void run() { redoContentTrees(contentNodes); }
//	}

	/**
	 * Method to redo the explorer tree, keeping expansion.
	 * @param contentNodes nodes that changed.
	 */
	void redoContentTrees(List<MutableTreeNode> contentNodes)
	{
        assert SwingUtilities.isEventDispatchThread();
//        if (!SwingUtilities.isEventDispatchThread())
//        {
//    		SwingUtilities.invokeLater(new RedoLater(contentNodes));
//    		return;
//        }

		// remember the state of the tree
        KeepTreeExpansion kte = new KeepTreeExpansion(this, rootNode, treeModel, new TreePath(rootNode));
        model().updateContentExplorerNodes(contentNodes);
        kte.restore();
	}

	/**
	 * Class to handle the "Select Object" dialog.
	 */
	public static class FindCellDialog extends EModelessDialog
	{
		private JList list;
		private DefaultListModel model;
		private JButton done, findText, editCell, editAllCells;
		private JScrollPane objectPane;
		private JTextField searchText;

		/** Creates new form FindCellDialog */
		private FindCellDialog(Frame parent)
		{
			super(parent, false);
			GridBagConstraints gbc;
			setTitle("Find Cells");
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { closeDialog(); }
			});
			getContentPane().setLayout(new GridBagLayout());

			JLabel jLabel1 = new JLabel("Search for:");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(jLabel1, gbc);

			searchText = new JTextField();
			searchText.setColumns(8);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(searchText, gbc);

			findText = new JButton("Find");
			findText.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { findTextActionPerformed(evt); }
			});
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(findText, gbc);
			getRootPane().setDefaultButton(findText);

			objectPane = new JScrollPane();
			model = new DefaultListModel();
			list = new JList(model);
			objectPane.setViewportView(list);
			objectPane.setMinimumSize(new Dimension(200, 200));
			objectPane.setPreferredSize(new Dimension(200, 200));
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = gbc.weighty = 1.0;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(objectPane, gbc);
			list.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent evt) {
					if (evt.getClickCount() == 2) editCell(false);
				}
			});

			editCell = new JButton("Edit Selected Cell(s)");
			editCell.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { editCell(false); }
			});
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.weightx = 0.5;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(editCell, gbc);
			editCell.setEnabled(false);

			editAllCells = new JButton("Edit All Cells");
			editAllCells.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { editCell(true); }
			});
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 3;
			gbc.weightx = 0.5;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(editAllCells, gbc);
			editAllCells.setEnabled(false);

			done = new JButton("Done");
			done.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { closeDialog(); }
			});
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 4;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(done, gbc);

			pack();

			finishInitialization();
		}

		public void init()
		{
			searchText.setText("");
			model.clear();
			setVisible(true);
		}

		protected void escapePressed() { closeDialog(); }

		private void findTextActionPerformed(ActionEvent evt)
		{
			String search = searchText.getText();
			int flags = Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE;
			Pattern p = Pattern.compile(search, flags);
			model.clear();
			boolean found = false;
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					Matcher m = p.matcher(cell.getName());
					if (m.find())
					{
						model.addElement(cell.describe(false));
						found = true;
					}
				}
			}
			editCell.setEnabled(found);
			editAllCells.setEnabled(found);
		}

		private void editCell(boolean all)
		{
			if (all)
			{
				for(int i=0; i<model.getSize(); i++)
				{
					String cellName = (String)model.getElementAt(i);
					if (cellName != null) editACell(cellName);
				}
			} else
			{
				Object [] cellNames = list.getSelectedValues();
				for(int i=0; i<cellNames.length; i++)
					editACell((String)cellNames[i]);
			}
		}

		private void editACell(String cellName)
		{
			NodeProto np = Cell.findNodeProto(cellName);
			if (np == null || !(np instanceof Cell)) return;
			Cell cell = (Cell)np;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				if (wf.getContent() instanceof EditWindow)
				{
					EditWindow wnd = (EditWindow)wf.getContent();
					if (wnd.getCell() == cell)
					{
						WindowFrame.showFrame(wf);
						return;
					}
				}
				if (wf.getContent() instanceof TextWindow)
				{
					TextWindow tw = (TextWindow)wf.getContent();
					if (tw.getCell() == cell)
					{
						WindowFrame.showFrame(wf);
						return;
					}
				}
			}
			WindowFrame.createEditWindow(cell);
		}
	}

	/**
	 * Class to remember the expansion state of a JTree and restore it after a change.
	 */
	public static class KeepTreeExpansion
	{
		private List<TreePath> expanded = new ArrayList<TreePath>();
		private JTree theTree;
		private TreeModel theTreeModel;
		private Object theRootNode;

		public KeepTreeExpansion(JTree tt, Object rn, TreeModel tm, TreePath tp)
		{
			theTree = tt;
			theTreeModel = tm;
			theRootNode = rn;
			recursivelyCacheExpandedPaths(tp);
		}

	    /**
	     * Recursively save the expansion state.
	     */
	    private void recursivelyCacheExpandedPaths(TreePath path)
	    {
	        if (!theTree.isExpanded(path)) return;
	        expanded.add(path);

	        // now recurse
	        Object node = path.getLastPathComponent();
	        for(int i=0; i< theTreeModel.getChildCount(node); i++)
	        {
	            Object child = theTreeModel.getChild(node, i);
	            if (theTreeModel.isLeaf(child)) continue;
	            TreePath descentPath = path.pathByAddingChild(child);
	            if (descentPath == null) continue;
	            recursivelyCacheExpandedPaths(descentPath);
	        }
	    }

		public void restore()
		{
	        for (TreePath path: expanded) expandCachedPath(path);
	    }

	    private void expandCachedPath(TreePath oldPath)
	    {
	        Object[] path = oldPath.getPath();
	        TreePath newPath = new TreePath(theRootNode);
	        Object topNode = theRootNode;
	        for (int i = 1; i < path.length; i++)
	        {
	            Object oldChild = path[i];
	            Object newChild = null;
	            if (oldChild instanceof DefaultMutableTreeNode)
	                newChild = findChildByUserObject(topNode, ((DefaultMutableTreeNode)oldChild).getUserObject());
	            if (newChild == null)
	            {
	                int k = theTreeModel.getIndexOfChild(topNode, oldChild);
	                if (k >= 0)
	                    newChild = theTreeModel.getChild(topNode, k);
	            }
	            if (newChild == null) return;
	            topNode = newChild;
	            newPath = newPath.pathByAddingChild(topNode);
	            theTree.expandPath(newPath);
	        }
	    }

	    private Object findChildByUserObject(Object parent, Object userObject)
	    {
	        for (int i = 0, childCount = theTreeModel.getChildCount(parent); i < childCount; i++)
	        {
	            Object newChild = theTreeModel.getChild(parent, i);
	            if (!(newChild instanceof DefaultMutableTreeNode)) continue;
	            if (((DefaultMutableTreeNode)newChild).getUserObject().equals(userObject))
	                return newChild;
	        }
	        return null;
	    }
	}

	public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf,
		int row, boolean hasFocus)
	{
        if (!(value instanceof DefaultMutableTreeNode))
            return value.toString();
		Object nodeInfo = ((DefaultMutableTreeNode)value).getUserObject();
		if (nodeInfo instanceof Cell)
		{
			Cell cell = (Cell)nodeInfo;
			if (cell.isLinked() && cell.isSchematic())
			{
				Cell.CellGroup group = cell.getCellGroup();
				Cell mainSchematic = group.getMainSchematics();
				int numSchematics = 0;
				for(Iterator<Cell> gIt = group.getCells(); gIt.hasNext(); )
				{
					Cell cellInGroup = gIt.next();
					if (cellInGroup.isSchematic()) numSchematics++;
				}
				if (numSchematics > 1 && cell == mainSchematic)
					return cell.noLibDescribe() + " **";
			}
			return cell.noLibDescribe();
		}
		if (nodeInfo instanceof Library)
		{
			Library lib = (Library)nodeInfo;
			String nodeName = lib.getName();
			if (lib == Library.getCurrent() && Library.getNumLibraries() > 1)
			{
				nodeName += " [Current]";
                if (iconLibraryChecked == null)
				    iconLibraryChecked = Resources.getResource(getClass(), "IconLibraryCheck.gif");
                iconLibrary = iconLibraryChecked;
			}
			else
			{
                if (iconLibraryNormal == null)
				    iconLibraryNormal = Resources.getResource(getClass(), "IconLibrary.gif");
                iconLibrary = iconLibraryNormal;
			}
			return nodeName;
		}
		if (nodeInfo instanceof Cell.CellGroup)
		{
			Cell.CellGroup group = (Cell.CellGroup)nodeInfo;
            return group.getName();
		}
		if (nodeInfo instanceof ErrorLoggerTree.ErrorLoggerTreeNode)
		{
            ErrorLoggerTree.ErrorLoggerTreeNode node = (ErrorLoggerTree.ErrorLoggerTreeNode)nodeInfo;
			ErrorLogger el = node.getLogger();
			String s = el.getSystem();
            if (ErrorLoggerTree.currentLogger != null && node == ErrorLoggerTree.currentLogger.getUserObject())
                s += " [Current]";
            return s;
		}
        if (nodeInfo instanceof ErrorLogger.MessageLog)
        {
            ErrorLogger.MessageLog el = (ErrorLogger.MessageLog)nodeInfo;
            return el.getMessage();
        }
		if (nodeInfo instanceof Signal)
		{
			Signal sig = (Signal)nodeInfo;
			return sig.getSignalName();
		}
		if (nodeInfo == null) return "";
		return nodeInfo.toString();
	}

	// *********************************** DRAG AND DROP ***********************************

	/** Variables needed for DnD */
	private ExplorerTreeDropTarget dropTarget;
	private Point dragOffset;
	private BufferedImage dragImage;
	private boolean subCells;

	private void initDND()
	{
		setTransferHandler(new MyTransferHandler(getTransferHandler(), this));
		setDragEnabled(true);

		/*
		 * There are two ways to do dragging from the Explorer tree:
		 * (1) the old way (pre Java 1.5) required a "drag gesture recognizer"
		 *     It has the advantage that it can recognize right-click-drag
		 *     (which was used to request dragging of the cell and all subcells)
		 *     It has the disadvantage that it cannot drag multiple objects
		 * (2) the new way requires a "Transfer Handler".
		 * To use the old dragging method (1):
		 *   comment-out the previous two lines
		 *   uncomment the next two lines
		 *   comment-out the implementation of the MyTransferHandler class
		 *   uncomment the "dragGestureRecognized" method below
		 *   have this class implement "DragGestureListener"
		 */
//		dragSource = DragSource.getDefaultDragSource();
//		DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK, this);

		// a drop target for the explorer tree
		dropTarget = new ExplorerTreeDropTarget();
		new DropTarget(this, DnDConstants.ACTION_LINK, dropTarget, true);
	}

	/**
	 * This override method fixes a bug where clicking and then dragging fails
	 * if the tree node is not already selected.
	 * This should not be necessary anymore after Java 1.5v6 but somehow is.
	 */
	public void setUI(TreeUI ui)
	{
		super.setUI(ui);

		// the default dnd implementation needs to first select and then drag
		try
		{
			// this is only valid on Java 1.5...in 1.6 or later, the code is not needed
			Class<?> clazz = Class.forName("javax.swing.plaf.basic.BasicDragGestureRecognizer");
			MouseListener[] mouseListeners = getMouseListeners();
			MouseListener dragListener = null;
			for(int i = 0; i<mouseListeners.length; i++)
			{
				if (clazz.isAssignableFrom(mouseListeners[i].getClass()))
				{
					dragListener = mouseListeners[i];
					break;
				}
			}

			if (dragListener != null)
			{
				removeMouseListener(dragListener);
				removeMouseMotionListener((MouseMotionListener)dragListener);
				addMouseListener(dragListener);
				addMouseMotionListener((MouseMotionListener)dragListener);
			}
		} catch (ClassNotFoundException e)
		{
		}
	}

	private class MyTransferHandler extends TransferHandler
	{
		private TransferHandler real;
		private ExplorerTree tree;

		MyTransferHandler(TransferHandler real, ExplorerTree tree) { this.real = real;   this.tree = tree; }

		protected Transferable createTransferable(JComponent c)
		{
			if (numCurrentlySelectedObjects() == 0) return null;

			// handle signal dragging when in a WaveformWindow setting
			if (getCurrentlySelectedObject(0) instanceof Signal)
			{
				// Get the Transferable Object
				StringBuffer buf = new StringBuffer();
				for(int i=0; i<numCurrentlySelectedObjects(); i++)
				{
					Signal sSig = (Signal)getCurrentlySelectedObject(i);
					String sigName = sSig.getFullName();
					if (sSig instanceof AnalogSignal)
					{
						if (sSig.getAnalysisType() == Analysis.ANALYSIS_TRANS) sigName = "TRANS " + sigName; else
							if (sSig.getAnalysisType() == Analysis.ANALYSIS_AC) sigName = "AC " + sigName; else
								if (sSig.getAnalysisType() == Analysis.ANALYSIS_DC) sigName = "DC " + sigName; else
									if (sSig.getAnalysisType() == Analysis.ANALYSIS_MEAS) sigName = "MEASUREMENT " + sigName;
					}
					buf.append(sigName);
					buf.append("\n");
				}
				Transferable transferable = new StringSelection(buf.toString());
				return transferable;
			}

			// cells and groups must be dragged one-at-a-time
			if (numCurrentlySelectedObjects() > 1)
			{
				Job.getUserInterface().showErrorMessage("Can drag only one Cell or Group", "Too Much Selected");
				return null;
			}

			// make a Transferable object
			EditWindow.NodeProtoTransferable transferable = new EditWindow.NodeProtoTransferable(getCurrentlySelectedObject(0), tree);
			if (!transferable.isValid()) return null;

			// find out the offset of the cursor to the selected tree node
			TreePath path = currentSelectedPaths[0];
			Rectangle pathRect = getPathBounds(path);
			if (pathRect == null) return null;
			dragOffset = new Point(0, 0);

			// render the dragged stuff
			Component comp = getCellRenderer().getTreeCellRendererComponent(tree, path.getLastPathComponent(),
				false, isExpanded(path), getModel().isLeaf(path.getLastPathComponent()), 0, false);
			int wid = (int)pathRect.getWidth();
			int hei = (int)pathRect.getHeight();

			// there is no way to tell whether the context menu button was used
			subCells = false;
//			if (e.getTriggerEvent() instanceof MouseEvent)
//				subCells = ClickZoomWireListener.isRightMouse((MouseEvent)e.getTriggerEvent());
			if (subCells) hei *= 2;
			comp.setSize(wid, hei);
			dragImage = new BufferedImage(wid, hei, BufferedImage.TYPE_INT_ARGB_PRE);
			Graphics2D g2 = dragImage.createGraphics();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));
			AffineTransform saveAT = g2.getTransform();
			if (subCells) g2.translate(0, -hei/4);
			comp.paint(g2);
			g2.setTransform(saveAT);
			if (subCells)
			{
				g2.setColor(Color.BLACK);
				g2.drawLine(wid/2, hei/2, 0, hei);
				g2.drawLine(wid/2, hei/2, wid/3, hei);
				g2.drawLine(wid/2, hei/2, wid/3*2, hei);
				g2.drawLine(wid/2, hei/2, wid, hei);
			}
			g2.dispose();
			return transferable;
		}

		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) { return real.canImport(comp, transferFlavors); }
		public void exportAsDrag(JComponent comp, InputEvent e, int action)  { real.exportAsDrag(comp, e, action); }
		protected void exportDone(JComponent source, Transferable data, int action)  {  }
		public void exportToClipboard(JComponent comp, Clipboard clip, int action)  { real.exportToClipboard(comp, clip, action); }
		public int getSourceActions(JComponent c)  { return real.getSourceActions(c); }
		public Icon getVisualRepresentation(Transferable t)  { return real.getVisualRepresentation(t); }
		public boolean importData(JComponent comp, Transferable t) { return real.importData(comp, t); }
	}

//	public void dragGestureRecognized(DragGestureEvent e)
//	{
//		if (numCurrentlySelectedObjects() == 0) return;
//
//		// handle signal dragging when in a WaveformWindow setting
//		if (getCurrentlySelectedObject(0) instanceof Signal)
//		{
//			// Get the Transferable Object
//			StringBuffer buf = new StringBuffer();
//			for(int i=0; i<numCurrentlySelectedObjects(); i++)
//			{
//				Signal sSig = (Signal)getCurrentlySelectedObject(i);
//				String sigName = sSig.getFullName();
//				if (sSig instanceof AnalogSignal)
//				{
//					AnalogSignal as = (AnalogSignal)sSig;
//					if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_TRANS) sigName = "TRANS " + sigName; else
//						if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_AC) sigName = "AC " + sigName; else
//							if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_DC) sigName = "DC " + sigName; else
//								if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_MEAS) sigName = "MEASUREMENT " + sigName;
//				}
//				buf.append(sigName);
//				buf.append("\n");
//			}
//			Transferable transferable = new StringSelection(buf.toString());
//
//			// begin the drag
//			dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
//			return;
//		}
//
//		// cells and groups must be dragged one-at-a-time
//		if (numCurrentlySelectedObjects() > 1)
//		{
//			Job.getUserInterface().showErrorMessage("Can drag only one Cell or Group", "Too Much Selected");
//			return;
//		}
//
//		// make a Transferable object
//		EditWindow.NodeProtoTransferable transferable = new EditWindow.NodeProtoTransferable(getCurrentlySelectedObject(0), this);
//		if (!transferable.isValid()) return;
//
//		// find out the offset of the cursor to the selected tree node
//		Point pt = e.getDragOrigin();
//		TreePath path = getPathForLocation(pt.x, pt.y);
//		Rectangle pathRect = getPathBounds(path);
//		if (pathRect == null) return;
//		dragOffset = new Point(pt.x-pathRect.x, pt.y-pathRect.y);
//
//		// render the dragged stuff
//		Component comp = getCellRenderer().getTreeCellRendererComponent(this, path.getLastPathComponent(),
//			false, isExpanded(path), getModel().isLeaf(path.getLastPathComponent()), 0, false);
//		int wid = (int)pathRect.getWidth();
//		int hei = (int)pathRect.getHeight();
//
//		subCells = false;
//		if (e.getTriggerEvent() instanceof MouseEvent)
//			subCells = ClickZoomWireListener.isRightMouse((MouseEvent)e.getTriggerEvent());
//		if (subCells) hei *= 2;
//		comp.setSize(wid, hei);
//		dragImage = new BufferedImage(wid, hei, BufferedImage.TYPE_INT_ARGB_PRE);
//		Graphics2D g2 = dragImage.createGraphics();
//		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));
//		AffineTransform saveAT = g2.getTransform();
//		if (subCells) g2.translate(0, -hei/4);
//		comp.paint(g2);
//		g2.setTransform(saveAT);
//		if (subCells)
//		{
//			g2.setColor(Color.BLACK);
//			g2.drawLine(wid/2, hei/2, 0, hei);
//			g2.drawLine(wid/2, hei/2, wid/3, hei);
//			g2.drawLine(wid/2, hei/2, wid/3*2, hei);
//			g2.drawLine(wid/2, hei/2, wid, hei);
//		}
//		g2.dispose();
//
//		// begin the drag
//		e.startDrag(null, dragImage, new Point(0, 0), transferable, this);
//	}

	public void dragEnter(DragSourceDragEvent e) {}

	public void dragOver(DragSourceDragEvent e) {}

	public void dragExit(DragSourceEvent e) {}

	public void dragDropEnd(DragSourceDropEvent e) {}

	public void dropActionChanged (DragSourceDragEvent e) {}

	/**
	 * Class for catching drags into the explorer tree.
	 * These drags come from elsewhere in the Explorer tree (cross-library copy, etc).
	 */
	private static class ExplorerTreeDropTarget implements DropTargetListener
	{
		private Rectangle lastDrawn = null;

		public void dragEnter(DropTargetDragEvent e)
		{
			dragAction(e);
		}

		public void dragOver(DropTargetDragEvent e)
		{
			DropTarget dt = (DropTarget)e.getSource();
			ExplorerTree tree = (ExplorerTree)dt.getComponent();
			Graphics2D g2 = (Graphics2D)tree.getGraphics();

			// erase former drawing
			eraseDragImage(dt);

			// get the original cell that was dragged
			Object obj = getDraggedObject(e.getCurrentDataFlavors());
			ExplorerTree originalTree = getOriginalTree(e.getCurrentDataFlavors());
			if (originalTree == null) return;
			if (obj instanceof Cell.CellGroup) obj = ((Cell.CellGroup)obj).getCells().next();
			if (!(obj instanceof Cell)) return;
			Cell origCell = (Cell)obj;

			// draw the image of what is being dragged
			if (!DragSource.isDragImageSupported())
			{
				// Remember where you are about to draw the new ghost image
				lastDrawn = new Rectangle(e.getLocation().x - originalTree.dragOffset.x,
					e.getLocation().y - originalTree.dragOffset.y,
					originalTree.dragImage.getWidth(), originalTree.dragImage.getHeight());

				// Draw the ghost image
				g2.drawImage(originalTree.dragImage, AffineTransform.getTranslateInstance(lastDrawn.getX(), lastDrawn.getY()), null);
			}

			// see what the drop is over
			TreePath cp = tree.getPathForLocation(e.getLocation().x, e.getLocation().y);
			Library destLib = findLibrary(cp);
			if (destLib == null) return;

			// must be a cross-library drag
			if (destLib == origCell.getLibrary()) return;

			// highlight the destination
			Rectangle path = tree.getPathBounds(cp);
			if (path == null) return;
			g2.setColor(Color.RED);
			g2.drawRect(path.x, path.y, path.width-1, path.height-1);
			if (lastDrawn == null) lastDrawn = path; else
				Rectangle.union(lastDrawn, path, lastDrawn);

			dragAction(e);
		}

		public void dropActionChanged(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dragExit(DropTargetEvent e)
		{
			eraseDragImage((DropTarget)e.getSource());
		}

		public void drop(DropTargetDropEvent dtde)
		{
			dtde.acceptDrop(DnDConstants.ACTION_LINK);

			// erase former drawing
			eraseDragImage((DropTarget)dtde.getSource());

			// get the original cell that was dragged
			Object obj = getDraggedObject(dtde.getCurrentDataFlavors());
			if (obj != null)
			{
				Cell origCell = null;
				if (obj instanceof Cell) origCell = (Cell)obj; else
				if (obj instanceof Cell.CellGroup) origCell = ((Cell.CellGroup)obj).getCells().next();
				if (origCell != null)
				{
					// see what the drop is over
					DropTarget dt = (DropTarget)dtde.getSource();
					ExplorerTree tree = (ExplorerTree)dt.getComponent();
					TreePath cp = tree.getPathForLocation(dtde.getLocation().x, dtde.getLocation().y);
					Library destLib = findLibrary(cp);
					if (destLib != null)
					{
						// must be a cross-library copy
						if (origCell.getLibrary() != destLib)
						{
							ExplorerTree originalTree = getOriginalTree(dtde.getCurrentDataFlavors());
							if (originalTree != null)
							{
                                List<Cell> fromCells = new ArrayList<Cell>();
                                fromCells.add(origCell);
                                new CrossLibCopy.CrossLibraryCopyJob(fromCells, destLib, null, false,
                                    obj instanceof Cell.CellGroup, originalTree.subCells, true);
								dtde.dropComplete(true);
								return;
							}
						}
					}
				}
			}
			dtde.dropComplete(false);
		}

		private Library findLibrary(TreePath cp)
		{
			if (cp == null) return null;
			Object obj = cp.getLastPathComponent();
			if (obj instanceof DefaultMutableTreeNode)
			{
				obj = ((DefaultMutableTreeNode)obj).getUserObject();
				if (obj instanceof Library) return (Library)obj;
				if (obj instanceof Cell.CellGroup) return ((Cell.CellGroup)obj).getCells().next().getLibrary();
				if (obj instanceof Cell) return ((Cell)obj).getLibrary();
			}
			return null;
		}

		private Object getDraggedObject(DataFlavor [] flavors)
		{
			if (flavors.length > 0)
			{
				if (flavors[0] instanceof EditWindow.NodeProtoDataFlavor)
				{
					EditWindow.NodeProtoDataFlavor npdf = (EditWindow.NodeProtoDataFlavor)flavors[0];
					Object obj = npdf.getFlavorObject();
					return obj;
				}
			}
			return null;
		}

		private ExplorerTree getOriginalTree(DataFlavor [] flavors)
		{
			if (flavors.length > 0)
			{
				if (flavors[0] instanceof EditWindow.NodeProtoDataFlavor)
				{
					EditWindow.NodeProtoDataFlavor npdf = (EditWindow.NodeProtoDataFlavor)flavors[0];
					return npdf.getOriginalTree();
				}
			}
			return null;
		}

		private void dragAction(DropTargetDragEvent e)
		{
			Object obj = getDraggedObject(e.getCurrentDataFlavors());
			if (obj != null)
				e.acceptDrag(e.getDropAction());
		}

		private void eraseDragImage(DropTarget dt)
		{
			if (lastDrawn == null) return;
			ExplorerTree tree = (ExplorerTree)dt.getComponent();
			tree.paintImmediately(lastDrawn);
			lastDrawn = null;
		}
	}

	// *********************************** DISPLAY ***********************************

	private class MyRenderer extends DefaultTreeCellRenderer
	{
		private Font plainFont, boldFont;

		public MyRenderer()
		{
			plainFont = new Font("arial", Font.PLAIN, 11);
			boldFont = new Font("arial", Font.BOLD, 11);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
			boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			// setIcon(icon)
			//setToolTipText(value.toString());
			setFont(plainFont);
            if (!(value instanceof DefaultMutableTreeNode))
                return this;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof Library)
			{
				Library lib = (Library)nodeInfo;
				if (iconLibraryNormal == null)
					iconLibraryNormal = Resources.getResource(getClass(), "IconLibrary.gif");
                if (iconLibrary == null)
                    iconLibrary = iconLibraryNormal;
				if (lib.isChanged()) setFont(boldFont);
                if (CVS.isEnabled()) setForeground(CVSLibrary.getColor(lib));
				setIcon(iconLibrary);
				setText(lib.getName());
			}
			if (nodeInfo instanceof ExplorerTreeModel.CellAndCount)
			{
				ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)nodeInfo;
				nodeInfo = cc.getCell();
                if (cc.getCell().isModified()) setFont(boldFont);
			}
			if (nodeInfo instanceof Cell)
			{
				Cell cell = (Cell)nodeInfo;
                if (cell.isModified()) setFont(boldFont);
                if (CVS.isEnabled()) setForeground(CVSLibrary.getColor(cell));
				IconGroup ig;
				if (cell.isIcon()) ig = findIconGroup(View.ICON); else
					if (cell.getView() == View.LAYOUT) ig = findIconGroup(View.LAYOUT); else
						if (cell.isSchematic()) ig = findIconGroup(View.SCHEMATIC); else
							if (cell.getView().isTextView()) ig = findIconGroup(View.DOC); else
								ig = findIconGroup(View.UNKNOWN);
				if (cell.getNewestVersion() != cell) setIcon(ig.old); else
				{
					switch (Project.getCellStatus(cell))
					{
						case Project.NOTMANAGED:         setIcon(ig.regular);     break;
						case Project.CHECKEDIN:          setIcon(ig.available);   break;
						case Project.CHECKEDOUTTOOTHERS: setIcon(ig.locked);      break;
						case Project.CHECKEDOUTTOYOU:    setIcon(ig.unlocked);    break;
					}
				}
			}
			if (nodeInfo instanceof ExplorerTreeModel.MultiPageCell)
			{
				if (iconViewMultiPageSchematics == null)
					iconViewMultiPageSchematics = Resources.getResource(getClass(), "IconViewMultiPageSchematics.gif");
				setIcon(iconViewMultiPageSchematics);
			}
			if (nodeInfo instanceof Cell.CellGroup)
			{
                Cell.CellGroup cg = (Cell.CellGroup)nodeInfo;
                boolean changed = false;
                for (Iterator<Cell> it = cg.getCells(); it.hasNext();)
                {
                    Cell c = it.next();
                    if (c.isModified())
                    {
                        changed = true;
                        break;  // no need of checking the rest
                    }
                }
                if (changed) setFont(boldFont);
                if (CVS.isEnabled()) setForeground(CVSLibrary.getColor(cg));
				if (iconGroup == null)
					iconGroup = Resources.getResource(getClass(), "IconGroup.gif");
				setIcon(iconGroup);
			}
			if (nodeInfo instanceof String)
			{
				String theString = (String)nodeInfo;
				if (theString.equalsIgnoreCase("jobs"))
				{
					if (iconJobs == null)
						iconJobs = Resources.getResource(getClass(), "IconJobs.gif");
					setIcon(iconJobs);
				} else if (theString.equalsIgnoreCase("libraries"))
				{
					if (iconLibraries == null)
						iconLibraries = Resources.getResource(getClass(), "IconLibraries.gif");
					setIcon(iconLibraries);
				} else if (theString.equalsIgnoreCase("errors"))
				{
					if (iconErrors == null)
						iconErrors = Resources.getResource(getClass(), "IconErrors.gif");
					setIcon(iconErrors);
				} else if (theString.equalsIgnoreCase("signals") || theString.equalsIgnoreCase("trans signals") ||
					theString.equalsIgnoreCase("ac signals") || theString.equalsIgnoreCase("dc signals"))
				{
					if (iconSignals == null)
						iconSignals = Resources.getResource(getClass(), "IconSignals.gif");
					setIcon(iconSignals);
				} else if (theString.equalsIgnoreCase("sweeps") || theString.equalsIgnoreCase("trans sweeps") ||
					theString.equalsIgnoreCase("ac sweeps") || theString.equalsIgnoreCase("dc sweeps"))
				{
					if (iconSweeps == null)
						iconSweeps = Resources.getResource(getClass(), "IconSweeps.gif");
					setIcon(iconSweeps);
				} else if (theString.equalsIgnoreCase("measurements"))
				{
					if (iconMeasurements == null)
						iconMeasurements = Resources.getResource(getClass(), "IconMeasurement.gif");
					setIcon(iconMeasurements);
				}
			}
            if (nodeInfo instanceof ErrorLogger.MessageLog)
            {
                ErrorLogger.MessageLog theLog = (ErrorLogger.MessageLog)nodeInfo;
                // Error   WarningLog
                if (theLog instanceof ErrorLogger.WarningLog)
                {
                    if (iconWarnMsg == null)
                        iconWarnMsg = Resources.getResource(getClass(), "IconWarningLog.gif");
                    setIcon(iconWarnMsg);
                } else // warning
                {
                    if (iconErrorMsg == null)
                        iconErrorMsg = Resources.getResource(getClass(), "IconErrorLog.gif");
                    setIcon(iconErrorMsg);
                }
            }
			if (nodeInfo instanceof JobTree.JobTreeNode)
			{
				//JobTree.JobTreeNode j = (JobTree.JobTreeNode)nodeInfo;
				//setToolTipText(j.getToolTip());
				//System.out.println("set tool tip to "+j.getToolTip());
			}
			return this;
		}

		private Map<View,IconGroup> iconGroups = new HashMap<View,IconGroup>();

		private IconGroup findIconGroup(View view)
		{
			IconGroup ig = iconGroups.get(view);
			if (ig == null)
			{
				ig = new IconGroup();

				// get the appropriate background icon
				if (view == View.LAYOUT) ig.regular = Resources.getResource(getClass(), "IconViewLayout.gif"); else
				if (view == View.SCHEMATIC) ig.regular = Resources.getResource(getClass(), "IconViewSchematics.gif"); else
				if (view == View.ICON) ig.regular = Resources.getResource(getClass(), "IconViewIcon.gif"); else
				if (view == View.DOC) ig.regular = Resources.getResource(getClass(), "IconViewText.gif"); else
				ig.regular = Resources.getResource(getClass(), "IconViewMisc.gif");

				// make sure the overlay icons have been read
				if (iconSpiderWeb == null) iconSpiderWeb = Resources.getResource(getClass(), "IconSpiderWeb.gif");
				if (iconLocked == null) iconLocked = Resources.getResource(getClass(), "IconLocked.gif");
				if (iconUnlocked == null) iconUnlocked = Resources.getResource(getClass(), "IconUnlocked.gif");
				if (iconAvailable == null) iconAvailable = Resources.getResource(getClass(), "IconAvailable.gif");

				ig.old = buildIcon(iconSpiderWeb, ig.regular);
				ig.available = buildIcon(iconAvailable, ig.regular);
				ig.locked = buildIcon(iconLocked, ig.regular);
				ig.unlocked = buildIcon(iconUnlocked, ig.regular);
				iconGroups.put(view, ig);
			}
			return ig;
		}

		private ImageIcon buildIcon(ImageIcon fg, ImageIcon bg)
		{
			// overlay and create the other icons for this view
			int wid = fg.getIconWidth();
			int hei = fg.getIconHeight();
			BufferedImage bi = new BufferedImage(wid, hei, BufferedImage.TYPE_INT_RGB);

			int [] backgroundValues = new int[wid*hei];
			PixelGrabber background = new PixelGrabber(bg.getImage(), 0, 0, wid, hei, backgroundValues, 0, wid);
			int [] foregroundValues = new int[wid*hei];
			PixelGrabber foreground = new PixelGrabber(fg.getImage(), 0, 0, wid, hei, foregroundValues, 0, wid);
			try
			{
				background.grabPixels();
				foreground.grabPixels();
			} catch (InterruptedException e) {}
			for(int y=0; y<hei; y++)
			{
				for(int x=0; x<wid; x++)
				{
					int bCol = backgroundValues[y*wid+x];
					int fCol = foregroundValues[y*wid+x];
					if ((fCol&0xFFFFFF) != 0xFFFFFF) bCol = fCol;
					bi.setRGB(x, y, bCol);
				}
			}
			return new ImageIcon(bi);
		}
	}

	private class TreeHandler implements MouseListener, MouseMotionListener, TreeSelectionListener
	{
        private ExplorerTree tree;
        private boolean draggingCell;
		private MouseEvent currentMouseEvent;
		private TreePath [] currentPaths;
		private TreePath [] originalPaths;

        TreeHandler (ExplorerTree t) {tree = t;}

        public void mouseClicked(MouseEvent e) {}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mouseMoved(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			draggingCell = false;

			// popup menu event (right click)
			if (e.isPopupTrigger())
			{
				selectTreeElement(e.getX(), e.getY());
                cacheEvent(e);
				doContextMenu();
				return;
			}

            cacheEvent(e);
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();

			// double click
			if (e.getClickCount() == 2)
			{
				Object clickedObject = null;
				TreePath cp = getPathForLocation(e.getX(), e.getY());
				if (cp != null)
				{
	                Object obj = cp.getLastPathComponent();
	                if (obj instanceof DefaultMutableTreeNode)
	    				clickedObject = ((DefaultMutableTreeNode)obj).getUserObject();
				}

				// handle things that can accomodate multiple selections
				boolean didSomething = false;
				boolean clickedIsSelected = false;
				for(int i=0; i<numCurrentlySelectedObjects(); i++)
				{
					Object obj = getCurrentlySelectedObject(i);
					if (obj == clickedObject) clickedIsSelected = true;
					if (obj instanceof Signal)
					{
						Signal sig = (Signal)obj;
						if (wf.getContent() instanceof WaveformWindow)
						{
							WaveformWindow ww = (WaveformWindow)wf.getContent();
							ww.addSignal(sig);
						}
						didSomething = true;
					}

					if (obj instanceof SweepSignal)
					{
						SweepSignal ss = (SweepSignal)obj;
						if (ss == null) return;
						ss.setIncluded(!ss.isIncluded(), true);
						updateUI();
						didSomething = true;
					}
				}
				if (didSomething) return;
				if (!clickedIsSelected) return;

				// must have only 1 selection
                if (numCurrentlySelectedObjects() == 0)
                    return;
                if (numCurrentlySelectedObjects() != 1)
				{
					Job.getUserInterface().showErrorMessage("Must select just one entry in the explorer tree", "Too Much Selected");
					return;
				}
				Object nodeObj = getCurrentlySelectedObject(0);

				if (nodeObj instanceof ExplorerTreeModel.CellAndCount)
				{
					ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)nodeObj;
					wf.setCellWindow(cc.getCell(), null);
					return;
				}

				if (nodeObj instanceof Cell)
				{
					Cell cell = (Cell)nodeObj;
					wf.setCellWindow(cell, null);
					return;
				}
				if (nodeObj instanceof ExplorerTreeModel.MultiPageCell)
				{
					ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)nodeObj;
					Cell cell = mpc.getCell();
					wf.setCellWindow(cell, null);
					if (wf.getContent() instanceof EditWindow)
					{
						EditWindow wnd = (EditWindow)wf.getContent();
						wnd.setMultiPageNumber(mpc.getPageNo());
					}
					return;
				}

				if (nodeObj instanceof Library || nodeObj instanceof Cell.CellGroup ||
					nodeObj instanceof String || nodeObj instanceof ErrorLoggerTree.ErrorLoggerTreeNode)
				{
					for(int i=0; i<currentPaths.length; i++)
					{
						if (isExpanded(currentPaths[i])) collapsePath(currentPaths[i]); else
							expandPath(currentPaths[i]);
					}
					return;
				}

				if (nodeObj instanceof JobTree.JobTreeNode)
				{
					System.out.println(((JobTree.JobTreeNode)nodeObj).getInfo());
					return;
				}

				if (nodeObj instanceof ErrorLogger.MessageLog)
				{
                    ErrorLogger.MessageLog el = (ErrorLogger.MessageLog)nodeObj;
                    Object tree = cp.getParentPath().getLastPathComponent(); // flat method
                    if (!(tree instanceof ErrorLoggerTree.ErrorLoggerDefaultMutableTreeNode))
                        tree = cp.getParentPath().getParentPath().getLastPathComponent();

                    if (!(tree instanceof ErrorLoggerTree.ErrorLoggerDefaultMutableTreeNode))
                        assert(false); // should this happen?


                    ErrorLoggerTree.ErrorLoggerDefaultMutableTreeNode er = (ErrorLoggerTree.ErrorLoggerDefaultMutableTreeNode)tree;
                    Object obj = er.getUserObject();
                    ErrorLoggerTree.ErrorLoggerTreeNode node = (ErrorLoggerTree.ErrorLoggerTreeNode)obj;
                    int index = node.getLogger().getLogIndex(el);
                    node.setLogNumber(index);

					String msg = Job.getUserInterface().reportLog(el, true, User.isShowCellsInNewWindow(), -1);
					System.out.println(msg);
					return;
				}

				// dragging: remember original object
				if (nodeObj instanceof Cell)
				{
					Cell cell = (Cell)nodeObj;
					if (cell.getNewestVersion() == cell)
					{
						originalPaths = new TreePath[currentPaths.length];
						for(int i=0; i<currentPaths.length; i++)
							originalPaths[i] = new TreePath(currentPaths[i].getPath());
						draggingCell = true;
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e)
		{
            // popup menu event (right click)
            if (e.isPopupTrigger())
            {
				selectTreeElement(e.getX(), e.getY());
                cacheEvent(e);
                doContextMenu();
            }
		}

		public void mouseDragged(MouseEvent e)
		{
			if (!draggingCell) return;
			cacheEvent(e);
			clearSelection();
			for(int i=0; i<originalPaths.length; i++)
				addSelectionPath(originalPaths[i]);
			for(int i=0; i<currentPaths.length; i++)
				addSelectionPath(currentPaths[i]);
			updateUI();
		}

		public void valueChanged(TreeSelectionEvent e)
		{
			currentPaths = getSelectionPaths();
			if (currentPaths == null) currentPaths = new TreePath[0];
			if (currentPaths.length > 0)
			{
			    Rectangle bounds = getPathBounds(currentPaths[0]);
			    if (bounds != null)
			    {
					bounds.x = 0;
					bounds.y -= bounds.height;
				    bounds.width = 0;
				    bounds.height *= 3;
				    scrollRectToVisible(bounds);
			    }
			}
			currentSelectedPaths = new TreePath[currentPaths.length];
			for(int i=0; i<currentPaths.length; i++)
			{
                currentSelectedPaths[i] = currentPaths[i];
			}

			// update highlighting to match this selection
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				if (wf.getExplorerTab() == ExplorerTree.this)
				{
					// initiate crossprobing from WaveformWindow
					if (wf.getContent() instanceof WaveformWindow)
					{
						WaveformWindow ww = (WaveformWindow)wf.getContent();
						ww.crossProbeWaveformToEditWindow();
					}
				}
			}
		}

		private void selectTreeElement(int x, int y)
		{
			TreePath cp = getPathForLocation(x, y);
			if (cp != null)
			{
				Object obj = getObjectInTreePath(cp);
				boolean selected = false;
				for(int i=0; i<numCurrentlySelectedObjects(); i++)
				{
					if (getCurrentlySelectedObject(i) == obj) { selected = true;   break; }
				}
				if (!selected)
				{
					currentSelectedPaths = new TreePath[1];
					currentSelectedPaths[0] = cp;
					clearSelection();
					addSelectionPath(cp);
					updateUI();
				}
			}
		}

		private void cacheEvent(MouseEvent e)
		{
//			currentPath = getPathForLocation(e.getX(), e.getY());
//			if (currentPath == null) { currentSelectedObject = null;   return; }
//			setSelectionPath(currentPath);
//			for(int i=0; i<currentPaths.length; i++)
//			{
//				DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPaths[i].getLastPathComponent();
//				if (i == 0) selectedNode = node;
//				Object newSelection = node.getUserObject();
//				if (newSelection != currentSelectedObject)
//				{
//					currentSelectedObject = newSelection;
//					// update highlighting to match this selection
//					for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//					{
//						WindowFrame wf = it.next();
//						if (wf.getExplorerTab() == tree)
//						{
//							// initiate crossprobing from WaveformWindow
//							if (wf.getContent() instanceof WaveformWindow)
//							{
//								WaveformWindow ww = (WaveformWindow)wf.getContent();
//								ww.crossProbeWaveformToEditWindow();
//							}
//						}
//					}
//				}
//			}

			// determine the source of this event
			currentMouseEvent = e;
		}

        /**
         * Method to add the GetInfoMenu option in all ExplorerTree nodes to be consistent
         * @param origMenu
         */
        private void addGetInfoMenu(JPopupMenu origMenu)
        {
            JPopupMenu menu = origMenu;

            if (menu == null)
            {
                menu = new JPopupMenu("Get Info");
            }
            JMenuItem menuItemGetInfo = new JMenuItem("Get Info");
            // Get info menu
            if (origMenu != null)
                menu.addSeparator();
            menu.add(menuItemGetInfo);
            menuItemGetInfo.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showGetInfoGeneral(); } });

            if (origMenu == null)
                menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
        }

        /**
         * Method to add standard open/close menus in the Explorer
         * @param menu
         */
        private void addOpenCloseMenus(JPopupMenu menu)
        {
            JMenuItem menuItem = new JMenuItem("Open");
            menu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openAction(); } });

            menuItem = new JMenuItem("Open all below here");
            menu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveOpenAction(); } });

            menuItem = new JMenuItem("Close all below here");
            menu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveCloseAction(); } });

            menu.addSeparator();
        }

        private void doContextMenu()
		{
			// see what is selected
			Object selectedObject = null;
            boolean allSame = true;
            int numSelectedElems = numCurrentlySelectedObjects();
            for(int i=0; i<numSelectedElems; i++)
			{
                Object obj = getCurrentlySelectedObject(i);
                if (obj == null) continue;
				if (selectedObject == null)
                {
                    selectedObject = obj;
                } else
				{
					Class clz = selectedObject.getClass();
					if (!clz.isInstance(obj))
					{
                        allSame = false;
                    }
				}
			}

            JPopupMenu menu;
            JMenuItem menuItem;

            // handle actions that allow multiple selections
			if (allSame && selectedObject instanceof SweepSignal)
			{
				menu = new JPopupMenu("Sweep Signal");

				menuItem = new JMenuItem("Include");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setSweepAction(true); } });

				menuItem = new JMenuItem("Exclude");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setSweepAction(false); } });

				menuItem = new JMenuItem("Highlight");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { highlightSweepAction(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}

			if (allSame && selectedObject instanceof Signal)
			{
				menu = new JPopupMenu("Signals");

				menuItem = new JMenuItem("Add to current panel");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addToWaveform(false); } });

				menuItem = new JMenuItem("Add in new panel");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addToWaveform(true); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}

			// restricted set of options when multiple things are selected
			if (allSame && numCurrentlySelectedObjects() > 1)
			{
				if (selectedObject instanceof ExplorerTreeModel.CellAndCount)
				{
					ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)selectedObject;
					selectedObject = cc.getCell();
				}
				if (selectedObject instanceof Cell)
				{
					Cell cell = (Cell)selectedObject;
					menu = new JPopupMenu("Cell");

					menuItem = new JMenuItem("Delete Cell");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellAction(); } });

					if (cell.getView() == View.ICON)
					{
						menuItem = new JMenuItem("Cannot Change View of Icon Cell");
						menuItem.setEnabled(false);
						menu.add(menuItem);
					} else
					{
						JMenu subMenu = new JMenu("Change View");
						menu.add(subMenu);
						for(View view : View.getOrderedViews())
						{
							if (cell.getView() == view) continue;
							JMenuItem subMenuItem = new JMenuItem(view.getFullName());
							subMenu.add(subMenuItem);
							subMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { reViewCellAction(e); } });
						}
						JMenuItem subMenuItem = new JMenuItem("Icon (cannot change into Icon view)");
						subMenu.add(subMenuItem);
						subMenuItem.setEnabled(false);
					}

					menuItem = new JMenuItem("Change Cell Group...");
	                menu.add(menuItem);
	                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeCellGroupAction(); }});

					if (CVS.isEnabled()) {
                        JMenu cvsMenu = new JMenu("CVS");
                        menu.add(cvsMenu);
                        addCVSMenu(cvsMenu);
                    }

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
			}

			// restrict to a single selection
			if (numCurrentlySelectedObjects() > 1 && CVS.isEnabled() &&
                    (getCurrentlySelectedLibraries().size() > 0 || getCurrentlySelectedCells().size() > 0))
			{
				menu = new JPopupMenu("CVS");
                JMenu cvsMenu = new JMenu("CVS");
                menu.add(cvsMenu);
                addCVSMenu(cvsMenu);

                menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}

			// show Job menu if user clicked on a Job
			if (selectedObject instanceof JobTree.JobTreeNode)
			{
				JobTree.JobTreeNode job = (JobTree.JobTreeNode)selectedObject;
				JPopupMenu popup = JobTree.getPopupStatus(job);
				popup.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof ExplorerTreeModel.CellAndCount)
			{
				ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)selectedObject;
				selectedObject = cc.getCell();
			}
			if (selectedObject instanceof Cell)
			{
				Cell cell = (Cell)selectedObject;
				menu = new JPopupMenu("Cell");

				menuItem = new JMenuItem("Edit");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(false); } });

				menuItem = new JMenuItem("Edit in New Window");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(true); } });

				int projStatus = Project.getCellStatus(cell);
				if (projStatus != Project.OLDVERSION &&
					(projStatus != Project.NOTMANAGED || Project.isLibraryManaged(cell.getLibrary())))
				{
					menu.addSeparator();

					if (projStatus == Project.CHECKEDIN)
					{
						menuItem = new JMenuItem("Check Out");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CheckOutJob.checkOut(getCellFromExplorerObject(getCurrentlySelectedObject(0))); } });
					}
					if (projStatus == Project.CHECKEDOUTTOYOU)
					{
						menuItem = new JMenuItem("Check In...");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CheckInJob.checkIn(getCellFromExplorerObject(getCurrentlySelectedObject(0))); } });

						menuItem = new JMenuItem("Rollback and Release Check-Out");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CancelCheckOutJob.cancelCheckOut(getCellFromExplorerObject(getCurrentlySelectedObject(0))); } });
					}
					if (projStatus == Project.NOTMANAGED)
					{
						menuItem = new JMenuItem("Add To Repository");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { AddCellJob.addCell(getCellFromExplorerObject(getCurrentlySelectedObject(0))); } });
					} else
					{
						menuItem = new JMenuItem("Show History of This Cell...");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { HistoryDialog.examineHistory(getCellFromExplorerObject(getCurrentlySelectedObject(0))); } });
					}
					if (projStatus == Project.CHECKEDIN || projStatus == Project.CHECKEDOUTTOYOU)
					{
						menuItem = new JMenuItem("Remove From Repository");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { DeleteCellJob.removeCell(getCellFromExplorerObject(getCurrentlySelectedObject(0))); } });
					}
				}

				menu.addSeparator();

				menuItem = new JMenuItem("Place Instance of Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellInstanceAction(); } });

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Version of Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellVersionAction(); } });

				menuItem = new JMenuItem("Duplicate Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateCellAction(); } });

				menuItem = new JMenuItem("Delete Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellAction(); } });

                JMenu subMenu = new JMenu("Copy Cell");
				menu.add(subMenu);
                prepareForCopyAction(subMenu, selectedObject);

                menu.addSeparator();

				menuItem = new JMenuItem("Rename Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameCellAction(); } });

				if (cell.getView() == View.ICON)
				{
					menuItem = new JMenuItem("Cannot Change View of Icon Cell");
					menuItem.setEnabled(false);
					menu.add(menuItem);
				} else
				{
					subMenu = new JMenu("Change View");
					menu.add(subMenu);
					for(View view : View.getOrderedViews())
					{
						if (cell.getView() == view) continue;
						if (view == View.ICON) continue;
						JMenuItem subMenuItem = new JMenuItem(view.getFullName());
						subMenu.add(subMenuItem);
						subMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { reViewCellAction(e); } });
					}
					JMenuItem subMenuItem = new JMenuItem("Icon (cannot change into Icon view)");
					subMenu.add(subMenuItem);
					subMenuItem.setEnabled(false);
				}

                if (CVS.isEnabled()) {
                    menu.addSeparator();
                    JMenu cvsMenu = new JMenu("CVS");
                    menu.add(cvsMenu);
                    addCVSMenu(cvsMenu);
                }

                menu.addSeparator();

                menuItem = new JMenuItem("Change Cell Group...");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeCellGroupAction(); }});

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof ExplorerTreeModel.MultiPageCell)
			{
				menu = new JPopupMenu("Cell");

				menuItem = new JMenuItem("Edit");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(false); } });

				menuItem = new JMenuItem("Edit in New Window");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(true); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Make New Page");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeNewSchematicPage(); } });

				menuItem = new JMenuItem("Delete This Page");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteSchematicPage(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof Library)
			{
				Library lib = (Library)selectedObject;
				menu = new JPopupMenu("Library");

                addOpenCloseMenus(menu);

				if (lib != Library.getCurrent())
				{
                    menuItem = new JMenuItem("Make This the Current Library");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setCurLibAction(); } });
				}

				if (Project.isLibraryManaged(lib))
				{
					menuItem = new JMenuItem("Update from Repository");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { UpdateJob.updateProject(); } });
				} else
				{
					menuItem = new JMenuItem("Add to Project Management Repository");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { AddLibraryJob.addLibrary((Library)getCurrentlySelectedObject(0)); } });
				}

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Rename Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameLibraryAction(); } });

                menuItem = new JMenuItem("Save Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryAction(); } });

				menuItem = new JMenuItem("Close Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryAction(); } });

                menuItem = new JMenuItem("Reload Library");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { reloadLibraryAction(); } });

                if (CVS.isEnabled()) {
                    menu.addSeparator();
                    JMenu cvsMenu = new JMenu("CVS");
                    menu.add(cvsMenu);
                    addCVSMenu(cvsMenu);
                }

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof Cell.CellGroup)
			{
				menu = new JPopupMenu("CellGroup");

                addOpenCloseMenus(menu);

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

                menuItem = new JMenuItem("Delete Entire Group");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteGroupAction(); } });

                JMenu subMenu = new JMenu("Copy Entire Group");
				menu.add(subMenu);
                prepareForCopyAction(subMenu, selectedObject);

                menu.addSeparator();

				menuItem = new JMenuItem("Rename Cells in Group");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameGroupAction(); } });

				menuItem = new JMenuItem("Duplicate Cells in Group");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateGroupAction(); } });

                if (CVS.isEnabled()) {
                    menu.addSeparator();
                    JMenu cvsMenu = new JMenu("CVS");
                    menu.add(cvsMenu);
                    addCVSMenu(cvsMenu);
                }

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof String)
			{
				String msg = (String)selectedObject;

                if (numSelectedElems == 1 && msg.toLowerCase().endsWith("sweeps"))
				{
					menu = new JPopupMenu("All Sweeps");

					menuItem = new JMenuItem("Include All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setAllSweepsAction(true); } });

					menuItem = new JMenuItem("Exclude All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setAllSweepsAction(false); } });

					menuItem = new JMenuItem("Remove Highlighting");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { removeSweepHighlighting(); } });

                    // Get info menu
                    addGetInfoMenu(menu);

                    menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
                if (numSelectedElems == 1 && msg.equalsIgnoreCase("errors"))
				{
					menu = new JPopupMenu("Errors");

                    addOpenCloseMenus(menu);

					menuItem = new JMenuItem("Delete All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ErrorLoggerTree.deleteAllLoggers(); } });

                    menuItem = new JMenuItem("Import Logger");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ErrorLoggerTree.importLogger(); } });

                    // Get info menu
                    addGetInfoMenu(menu);

                    menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
                    return;
                }
				if (numSelectedElems == 1 && msg.equalsIgnoreCase("libraries"))
				{
					menu = new JPopupMenu("Libraries");

                    addOpenCloseMenus(menu);

                    menuItem = new JMenuItem("Create New Cell");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

					menu.addSeparator();

					ButtonGroup bg = new ButtonGroup();
					JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem("Show Cells Alphabetically", ExplorerTreeModel.isShownAlphabetically());
					menu.add(rbMenuItem);
					rbMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ExplorerTreeModel.showAlphabeticallyAction(); } });
					bg.add(rbMenuItem);

					rbMenuItem = new JRadioButtonMenuItem("Show Cells by Group", ExplorerTreeModel.isShownByGroup());
					menu.add(rbMenuItem);
					rbMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ExplorerTreeModel.showByGroupAction(); } });
					bg.add(rbMenuItem);

					rbMenuItem = new JRadioButtonMenuItem("Show Cells by Hierarchy", ExplorerTreeModel.isShownByHierarchy());
					menu.add(rbMenuItem);
					rbMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ExplorerTreeModel.showByHierarchyAction(); } });
					bg.add(rbMenuItem);

					menu.addSeparator();

					JCheckBoxMenuItem cbMenuItem = new JCheckBoxMenuItem("Evaluate Numbers when Sorting Names", !ExplorerTreeModel.getSortLexically());
					menu.add(cbMenuItem);
					cbMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ExplorerTreeModel.setSortLexically(e); } });

					menu.addSeparator();

                    menuItem = new JMenuItem("Search...");
                    menu.add(menuItem);
                    menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { searchAction(); } });

                    // Get info menu
                    addGetInfoMenu(menu);

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (numSelectedElems == 1 && msg.equalsIgnoreCase("TECHNOLOGY LAYERS"))
				{
					menu = new JPopupMenu("Technology Layers");

					menuItem = new JMenuItem("Add New Layer");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(1); } });

					menuItem = new JMenuItem("Reorder Layers");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(1); } });

                    // Get info menu
					addGetInfoMenu(menu);

                    menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (numSelectedElems == 1 && msg.equalsIgnoreCase("TECHNOLOGY ARCS"))
				{
					menu = new JPopupMenu("Technology Arcs");

					menuItem = new JMenuItem("Add New Arc");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(2); } });

					menuItem = new JMenuItem("Reorder Arcs");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(2); } });

                    // Get info menu
					addGetInfoMenu(menu);

                    menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (numSelectedElems == 1 && msg.equalsIgnoreCase("TECHNOLOGY NODES"))
				{
					menu = new JPopupMenu("Technology Nodes");

					menuItem = new JMenuItem("Add New Node");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(3); } });

					menuItem = new JMenuItem("Reorder Nodes");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(3); } });

                    // Get info menu
                    addGetInfoMenu(menu);

                    menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}

                addGetInfoMenu(null);
                return;
            }
            if (selectedObject instanceof ErrorLoggerTree.ErrorLoggerTreeNode)
            {
                final ErrorLoggerTree.ErrorLoggerTreeNode node = (ErrorLoggerTree.ErrorLoggerTreeNode)selectedObject;
                menu = new JPopupMenu("ErrorLogger");

                addOpenCloseMenus(menu);

                menuItem = new JMenuItem("Delete");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
                { ErrorLoggerTree.deleteLogger(tree);}});

                menuItem = new JMenuItem("Export");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
                { ErrorLoggerTree.exportLogger(node); } });

                menuItem = new JMenuItem("Show All");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
                { ErrorLoggerTree.showAllLogger(tree); } });

                menuItem = new JMenuItem("Set Current");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
                { ErrorLoggerTree.setCurrentLogger(node); } });

                // Get info menu
                addGetInfoMenu(menu);

                menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
                return;
            }
            if (selectedObject instanceof ErrorLoggerTree.ErrorLoggerGroupNode)
            {
                menu = new JPopupMenu("Error Group");

                menuItem = new JMenuItem("Show All");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
                { ErrorLoggerTree.showAllLogger(tree); } });

                addGetInfoMenu(menu);

                menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
                return;
            }
            if (selectedObject instanceof ErrorLogger.MessageLog)
            {
                menu = new JPopupMenu("Error Message");

                menuItem = new JMenuItem("Show All");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e)
                { ErrorLoggerTree.showAllLogger(tree); } });

                addGetInfoMenu(menu);

                menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
                return;
            }
        }

		private void openAction()
		{
			for(int i=0; i<currentPaths.length; i++)
				expandPath(currentPaths[i]);
		}

		private void recursiveOpenAction()
		{
			for(int i=0; i<currentPaths.length; i++)
				recursivelyOpen(currentPaths[i]);
		}

		private void recursivelyOpen(TreePath path)
		{
			expandPath(path);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			int numChildren = node.getChildCount();
			for(int i=0; i<numChildren; i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				TreePath descentPath = path.pathByAddingChild(child);
				recursivelyOpen(descentPath);
			}
		}

		private void recursiveCloseAction()
		{
			for(int i=0; i<currentPaths.length; i++)
				recursivelyClose(currentPaths[i]);
		}

		private void recursivelyClose(TreePath path)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			int numChildren = node.getChildCount();
			for(int i=0; i<numChildren; i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				TreePath descentPath = path.pathByAddingChild(child);
				recursivelyClose(descentPath);
			}
			collapsePath(path);
		}

		private void setCurLibAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
            User.setCurrentLibrary(lib);
            ChangeCurrentLib.repaintAllAfterCurLibAction();
		}

        private void renameLibraryAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
			CircuitChanges.renameLibrary(lib);
		}

        private void saveLibraryAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
			FileMenu.saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true, false);
		}

		private void closeLibraryAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
			FileMenu.closeLibraryCommand(lib);
		}

        private void reloadLibraryAction()
        {
            Library lib = (Library)getCurrentlySelectedObject(0);
            new CircuitChangeJobs.ReloadLibraryJob(lib);
        }

        private Cell getCellFromExplorerObject(Object obj)
        {
        	if (obj instanceof Cell) return (Cell)obj;
			if (obj instanceof ExplorerTreeModel.CellAndCount)
				return ((ExplorerTreeModel.CellAndCount)obj).getCell();
			return null;
        }

        private void renameGroupAction()
		{
			Cell.CellGroup cellGroup = (Cell.CellGroup)getCurrentlySelectedObject(0);
			String defaultName = "";
			if (cellGroup.getNumCells() > 0)
				defaultName = (cellGroup.getCells().next()).getName();
			// check first if all cells in the group have the same name otherwise
			// the new name is added as prefix
			boolean allSameName = true;
			String lastName = null;
			for(Iterator<Cell> it = cellGroup.getCells(); it.hasNext(); )
			{
				String cellName = it.next().getName();
				if (lastName != null && !lastName.equals(cellName))
				{
					allSameName = false;
					break;
				}
				lastName = cellName;
			}
			if (!allSameName)
			{
				int ret = JOptionPane.showConfirmDialog(null,
	                    "Warning: Renaming is not possible because cells in group\ndon't have same root name. "+
	                    "New name will be added as prefix."+
	                    "\nContinue Anyway?",
	                    "Group Rename Warning", JOptionPane.YES_NO_OPTION);
	            if (ret == JOptionPane.NO_OPTION) return;
			}
			String response = JOptionPane.showInputDialog(ExplorerTree.this, "New name for cells in this group", 
					defaultName);
			if (response == null) return;
			CircuitChanges.renameCellGroupInJob(cellGroup, response);
		}

        private void deleteGroupAction()
        {
            Cell.CellGroup cellGroup = (Cell.CellGroup)getCurrentlySelectedObject(0);
            new CellChangeJobs.DeleteCellGroup(cellGroup);
        }

        private void duplicateGroupAction()
		{
			Cell.CellGroup cellGroup = (Cell.CellGroup)getCurrentlySelectedObject(0);
			Cell cell = null;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				if (cellGroup.containsCell(wf.getContent().getCell()))
				{
					cell = wf.getContent().getCell();
					break;
				}
			}
			if (cell == null) cell = cellGroup.getCells().next();
			new CellMenu.NewCellName(true, cell);
		}

		private void editCellAction(boolean newWindow)
		{
			Cell cell = null;
			int pageNo = 1;
			Object curSel = getCurrentlySelectedObject(0);
			if (curSel instanceof ExplorerTreeModel.CellAndCount)
				curSel = ((ExplorerTreeModel.CellAndCount)curSel).getCell();
			if (curSel instanceof Cell)
			{
				cell = (Cell)curSel;
			} else if (curSel instanceof ExplorerTreeModel.MultiPageCell)
			{
				ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)curSel;
				cell = mpc.getCell();
				pageNo = mpc.getPageNo();
			}
			WindowFrame wf = null;
 			if (newWindow)
			{
				wf = WindowFrame.createEditWindow(cell);
			} else
			{
				wf = WindowFrame.getCurrentWindowFrame();
			}
			wf.setCellWindow(cell, null);
			if (cell.isMultiPage() && wf.getContent() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)wf.getContent();
				wnd.setMultiPageNumber(pageNo);
			}
		}

		private void newCellInstanceAction()
		{
			Cell cell = getCellFromExplorerObject(getCurrentlySelectedObject(0));
			if (cell == null) return;
			PaletteFrame.placeInstance(cell, null, false);
		}

		private void newCellAction()
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			NewCell dialog = new NewCell(jf);
			dialog.setVisible(true);
		}

		private void addToWaveform(boolean newPanel)
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (!(wf.getContent() instanceof WaveformWindow)) return;
			WaveformWindow ww = (WaveformWindow)wf.getContent();
			List<Signal> sigs = new ArrayList<Signal>();
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				Signal sig = (Signal)getCurrentlySelectedObject(i);
				sigs.add(sig);
			}
			ww.showSignals(sigs, newPanel);
		}

		private void setSweepAction(boolean include)
		{
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				SweepSignal ss = (SweepSignal)getCurrentlySelectedObject(i);
				if (ss == null) continue;
				boolean update = (i == numCurrentlySelectedObjects()-1);
				ss.setIncluded(include, update);
			}
			updateUI();
		}

		private void highlightSweepAction()
		{
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				SweepSignal ss = (SweepSignal)getCurrentlySelectedObject(i);
				if (ss == null) continue;
				ss.highlight();
			}
			updateUI();
		}

		private void setAllSweepsAction(boolean include)
		{
			// get all sweep signals below this
			if (currentPaths.length != 1) return;
			TreePath path = currentPaths[0];
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			int numChildren = node.getChildCount();
			List<SweepSignal> sweeps = new ArrayList<SweepSignal>();
			for(int i=0; i<numChildren; i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				TreePath descentPath = path.pathByAddingChild(child);
			    Object obj = descentPath.getLastPathComponent();
				SweepSignal ss = (SweepSignal)((DefaultMutableTreeNode)obj).getUserObject();
				sweeps.add(ss);
			}

			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
			if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
                ww.setIncludeInAllSweeps(sweeps, include);
			}
			updateUI();
		}

		private void removeSweepHighlighting()
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
			if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
                ww.setHighlightedSweep(-1);
        		for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
        		{
        			Panel wp = it.next();
        			wp.repaintWithRulers();
        		}
			}
			updateUI();
		}

        private void showGetInfoGeneral()
        {
            int numSelectedElems = numCurrentlySelectedObjects();

            if (numSelectedElems > 1)
               System.out.println(numSelectedElems + " nodes selected");

            for(int i=0; i<numSelectedElems; i++)
			{
                TreePath treePath = currentSelectedPaths[i];
                Object obj = treePath.getLastPathComponent();
                if (obj instanceof DefaultMutableTreeNode)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
                    int num = node.getChildCount();
                    String nodeName = node.getUserObject().toString();
                    if (num > 0)
                        System.out.println(num + " child nodes in '" + nodeName + "'");
                    if (numSelectedElems == 1 && num == 0)
                        System.out.println(numSelectedElems + " node selected");
                }
                else
                    System.out.println("Case not implemented yet");
			}
        }

        private void newCellVersionAction()
		{
			Cell cell = getCellFromExplorerObject(getCurrentlySelectedObject(0));
			CircuitChanges.newVersionOfCell(cell);
		}

		private void duplicateCellAction()
		{
			Cell cell = getCellFromExplorerObject(getCurrentlySelectedObject(0));
			new CellMenu.NewCellName(false, cell);
		}

		private void deleteCellAction()
		{
			List<Cell> cellsToDelete = new ArrayList<Cell>();
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				Cell cell = getCellFromExplorerObject(getCurrentlySelectedObject(i));
				cellsToDelete.add(cell);
			}

			// make sure the user really wants to delete the cell
			boolean confirm = true;
			if (cellsToDelete.size() > 1)
			{
				String confirmMsg = "Are you sure you want to delete cells";
				for(Cell cell : cellsToDelete) confirmMsg += " " + cell.describe(false);
				int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
					confirmMsg + "?", "Delete Cells Dialog", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) return;
				confirm = false;
			}

			for(Cell cell : cellsToDelete)
			{
				CircuitChanges.deleteCell(cell, confirm, false);
			}
		}

		private void renameCellAction()
		{
			Cell cell = getCellFromExplorerObject(getCurrentlySelectedObject(0));
			String response = JOptionPane.showInputDialog(ExplorerTree.this, "New name for " + cell, cell.getName());
			if (response == null) return;
			CircuitChanges.renameCellInJob(cell, response);
		}

        private void prepareForCopyAction(JMenu subMenu, Object selectedObject)
        {
            Cell cell = null;
            if (selectedObject instanceof ExplorerTreeModel.CellAndCount)
            	selectedObject = ((ExplorerTreeModel.CellAndCount)selectedObject).getCell();
            if (selectedObject instanceof Cell)
            {
                cell = (Cell)selectedObject;
            }
            else
            {
                cell = ((Cell.CellGroup)selectedObject).getCells().next();
            }
            for (Iterator<Library> libIter = Library.getLibraries(); libIter.hasNext();)
            {
                Library lib = libIter.next();
                if (lib == cell.getLibrary()) continue; // already in this one
                if (lib.isHidden()) continue; // Clipboard.
                JMenuItem subMenuItem = new JMenuItem(lib.getName());
                subMenu.add(subMenuItem);
                subMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { copyCellOrCellGroupAction(e); } });
            }
        }

        private void copyCellOrCellGroupAction(ActionEvent e)
        {
			JMenuItem menuItem = (JMenuItem)e.getSource();
			String libName = menuItem.getText();
            Library destLib = Library.findLibrary(libName);
            assert(destLib != null); // it should be consistent.
            List<Cell> fromCells = new ArrayList<Cell>();
            Object obj = getCurrentlySelectedObject(0);
            boolean copyRelated = (obj instanceof Cell.CellGroup);
            Cell cell = null;
            if (copyRelated)
            {
                // get first cell from the group
                cell = ((Cell.CellGroup)obj).getCells().next();
            }
            else
                cell = (Cell)obj;
            fromCells.add(cell);
            new CrossLibCopy.CrossLibraryCopyJob(fromCells, destLib, null, false, copyRelated, false, true);
        }

        private void reViewCellAction(ActionEvent e)
		{
			JMenuItem menuItem = (JMenuItem)e.getSource();
			String viewName = menuItem.getText();
			View newView = View.findView(viewName);
			if (newView != null)
			{
				List<Cell> cellsToReview = new ArrayList<Cell>();
				for(int i=0; i<numCurrentlySelectedObjects(); i++)
				{
					Cell cell = getCellFromExplorerObject(getCurrentlySelectedObject(i));
					cellsToReview.add(cell);
				}
				for(Cell cell : cellsToReview)
				{
					ViewChanges.changeCellView(cell, newView);
				}
			}
		}

        private void changeCellGroupAction() {
        	List<Cell> cellsToChange = new ArrayList<Cell>();
        	Library lib = null;
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				Cell cell = getCellFromExplorerObject(getCurrentlySelectedObject(i));
				if (lib == null) lib = cell.getLibrary(); else
				{
					if (lib != cell.getLibrary())
					{
						Job.getUserInterface().showErrorMessage("All Cells to be regrouped must be in the same library",
							"Cannot Regroup These Cells");
						return;
					}
				}
				cellsToChange.add(cell);
			}
			if (cellsToChange.size() == 0) return;
            ChangeCellGroup dialog = new ChangeCellGroup(TopLevel.getCurrentJFrame(), true, cellsToChange, lib);
            dialog.setVisible(true);
        }

        private void makeNewSchematicPage()
        {
        	ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)getCurrentlySelectedObject(0);
            Cell cell = mpc.getCell();
         	if (!cell.isMultiPage())
        	{
        		System.out.println("First turn this cell into a multi-page schematic");
        		return;
        	}
        	int numPages = cell.getNumMultiPages();
    		new CellMenu.SetMultiPageJob(cell, numPages+1);
           	EditWindow wnd = EditWindow.needCurrent();
        	if (wnd != null) wnd.setMultiPageNumber(numPages);
        }

        private void deleteSchematicPage()
        {
        	ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)getCurrentlySelectedObject(0);
            Cell cell = mpc.getCell();
         	if (!cell.isMultiPage()) return;
        	int numPages = cell.getNumMultiPages();
         	if (numPages <= 1)
        	{
        		System.out.println("Cannot delete the last page of a multi-page schematic");
        		return;
        	}
         	new CellMenu.DeleteMultiPageJob(cell, mpc.getPageNo());
        }

        private void searchAction()
        {
            if (theCellSearchDialog == null)
            {
                JFrame jf = null;
                if (TopLevel.isMDIMode()) jf = TopLevel.getCurrentJFrame();
                theCellSearchDialog = new FindCellDialog(jf);
            }
            theCellSearchDialog.init();
            theCellSearchDialog.toFront();

//            String name = JOptionPane.showInputDialog(ExplorerTree.this, "Name of cell to search","");
//			if (name == null) return;
//            System.out.println("Search for cells named '" + name + "'");
//            boolean found = false;
//            for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
//            {
//				Library lib = it.next();
//				if (lib.isHidden()) continue;
//				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
//				{
//					Cell cell = cIt.next();
//					if (cell.getName().equalsIgnoreCase(name))
//					{
//                        System.out.println("\t" + cell.noLibDescribe() + " in Library " + cell.getLibrary().getName());
//                        found = true;
//					}
//				}
//            }
//            if (!found) System.out.println("\t" + "NO CELLS MATCH IN ANY LIBRARIES");
        }

        private void addCVSMenu(JMenu cvsMenu) {
            List<Library> libs = getCurrentlySelectedLibraries();
            List<Cell> cells = getCurrentlySelectedCells();
            Set<State> states = new TreeSet<State>();
            for (Library lib : libs) {
                states.add(CVSLibrary.getState(lib));
            }
            for (Cell cell : cells) {
                states.add(CVSLibrary.getState(cell));
            }
            JMenuItem menuItem = new JMenuItem("Commit");
            cvsMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                cvsCommit(); }});
            menuItem.setEnabled(!states.contains(State.UNKNOWN));

            menuItem = new JMenuItem("Update");
            cvsMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                cvsUpdate(Update.UpdateEnum.UPDATE); }});
            menuItem.setEnabled(!states.contains(State.UNKNOWN));

            menuItem = new JMenuItem("Get Status");
            cvsMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                cvsUpdate(Update.UpdateEnum.STATUS); }});
            menuItem.setEnabled(true);

            menuItem = new JMenuItem("List Editors");
            cvsMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                cvsListEditors(); }});
            menuItem.setEnabled(!states.contains(State.UNKNOWN));

            menuItem = new JMenuItem("Show Log");
            cvsMenu.add(menuItem);
            boolean showLog = false;
            if (libs.size() == 1 && cells.size() == 0) {
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                    showLog(1); }});
                showLog = true;
            }
            if (libs.size() == 0 && cells.size() == 1) {
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                    showLog(0); }});
                showLog = true;
            }
            menuItem.setEnabled(showLog && !states.contains(State.UNKNOWN));

            cvsMenu.addSeparator();

            menuItem = new JMenuItem("Rollback");
            cvsMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                cvsUpdate(Update.UpdateEnum.ROLLBACK); }});
            menuItem.setEnabled(!states.contains(State.UNKNOWN) && !states.contains(State.NONE));

            menuItem = new JMenuItem("Add to CVS");
            cvsMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                cvsAddRemove(true); }});
            menuItem.setEnabled(states.contains(State.UNKNOWN) || states.contains(State.REMOVED));

            if (libs.size() > 0 && cells.size() == 0) {
                menuItem = new JMenuItem("Remove from CVS");
                cvsMenu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                    cvsAddRemove(false); }});
                menuItem.setEnabled(!states.contains(State.UNKNOWN));
            }

            if (true) {
                menuItem = new JMenuItem("Undo CVS Add or Remove");
                cvsMenu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                    cvsUndoAddRemove(); }});
                menuItem.setEnabled(false);
                menuItem.setEnabled((states.size() == 1 && (states.contains(State.ADDED) || states.contains(State.REMOVED))) ||
                                    (states.size() == 2 && (states.contains(State.ADDED) && states.contains(State.REMOVED))));
            }

            menuItem = new JMenuItem("Rollforward");
            cvsMenu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                cvsUpdate(Update.UpdateEnum.ROLLFORWARD); }});
            menuItem.setEnabled(false); // need more safeguards before I should enable this
            //menuItem.setEnabled(states.size() == 1 && states.contains(State.CONFLICT));
        }

        private void cvsUpdate(Update.UpdateEnum type) {
            List<Library> libs = getCurrentlySelectedLibraries();
            List<Cell> cells = getCurrentlySelectedCells();
            Update.update(libs, cells, type, false, true);
        }

        private void cvsListEditors() {
            List<Library> libs = getCurrentlySelectedLibraries();
            List<Cell> cells = getCurrentlySelectedCells();
            Edit.listEditors(libs, cells);
        }

        private void showLog(int type) {
            if (type == 1) {
                List<Library> libs = getCurrentlySelectedLibraries();
                Log.showLog(libs.get(0));
            }
            if (type == 0) {
                List<Cell> cells = getCurrentlySelectedCells();
                Log.showLog(cells.get(0));
            }
        }

        private void cvsCommit() {
            List<Library> libs = getCurrentlySelectedLibraries();
            List<Cell> cells = getCurrentlySelectedCells();
            Commit.commit(libs, cells);
        }

        private void cvsAddRemove(boolean add) {
            List<Library> libs = getCurrentlySelectedLibraries();
            List<Cell> cells = getCurrentlySelectedCells();
            AddRemove.addremove(libs, cells, add, false);
        }

        private void cvsUndoAddRemove() {
            List<Library> libs = getCurrentlySelectedLibraries();
            List<Cell> cells = getCurrentlySelectedCells();
            AddRemove.addremove(libs, cells, false, true);
        }
	}
}

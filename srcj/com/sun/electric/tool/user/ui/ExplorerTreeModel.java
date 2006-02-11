/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExplorerTreeModel.java
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.WeakReferences;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.tecEdit.ArcInfo;
import com.sun.electric.tool.user.tecEdit.LayerInfo;
import com.sun.electric.tool.user.tecEdit.NodeInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Model of a cell explorer tree-view of the database.
 */
public class ExplorerTreeModel extends DefaultTreeModel {
    public static final String rootNode = "Explorer";
    private static final WeakReferences<TreeModelListener> allListeners = new WeakReferences<TreeModelListener>();

    private final ArrayList<MutableTreeNode> contentExplorerNodes = new ArrayList<MutableTreeNode>();
    /** the job explorer part. */						final DefaultMutableTreeNode jobExplorerNode;
    /** the error explorer part. */						final DefaultMutableTreeNode errorExplorerNode;
    private final Object[] rootPath;
    final Object[] jobPath;
    final Object[] errorPath;
    
	private static final int SHOWALPHABETICALLY = 1;
	private static final int SHOWBYCELLGROUP    = 2;
	private static final int SHOWBYHIERARCHY    = 3;
	private static int howToShow = SHOWBYCELLGROUP;
    
	static class CellAndCount
	{
		private Cell cell;
		private int count;
		CellAndCount(Cell cell, int count) { this.cell = cell;   this.count = count; }
		Cell getCell() { return cell; }
		int getCount() { return count; }
        
        public String toString() { return cell.noLibDescribe() + " (" + count + ")"; }
	}

	static class MultiPageCell
	{
		private Cell cell;
		private int pageNo;
        Cell getCell() { return cell; }
        int getPageNo() { return pageNo; }
        
        public String toString() { return cell.noLibDescribe() + " Page " + (pageNo+1); }
	}

    ExplorerTreeModel() {
        super(null);
        jobExplorerNode = JobTree.getExplorerTree();
        errorExplorerNode = ErrorLoggerTree.getExplorerTree();
        rootPath = new Object[] { rootNode };
        jobPath = new Object[] { rootNode, jobExplorerNode };
        errorPath = new Object[] { rootNode, errorExplorerNode };
    }
    
    
    
    /**
     * Returns the root of the tree.  Returns <code>null</code>
     * only if the tree has no nodes.
     *
     * @return  the root of the tree
     */
    public Object getRoot() { return rootNode; }
    
    
    /**
     * Returns the child of <code>parent</code> at index <code>index</code>
     * in the parent's
     * child array.  <code>parent</code> must be a node previously obtained
     * from this data source. This should not return <code>null</code>
     * if <code>index</code>
     * is a valid index for <code>parent</code> (that is <code>index >= 0 &&
     * index < getChildCount(parent</code>)).
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the child of <code>parent</code> at index <code>index</code>
     */
    public Object getChild(Object parent, int index) {
        if (parent == rootNode) {
            if (index == contentExplorerNodes.size()) return errorExplorerNode;
            if (index == contentExplorerNodes.size() + 1) return jobExplorerNode;
            return contentExplorerNodes.get(index);
        }
        return super.getChild(parent, index);
    }
    
    
    /**
     * Returns the number of children of <code>parent</code>.
     * Returns 0 if the node
     * is a leaf or if it has no children.  <code>parent</code> must be a node
     * previously obtained from this data source.
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the number of children of the node <code>parent</code>
     */
    public int getChildCount(Object parent) {
        if (parent == rootNode) return contentExplorerNodes.size() + 2;
        return super.getChildCount(parent);
    }
    
    
    /**
     * Returns <code>true</code> if <code>node</code> is a leaf.
     * It is possible for this method to return <code>false</code>
     * even if <code>node</code> has no children.
     * A directory in a filesystem, for example,
     * may contain no files; the node representing
     * the directory is not a leaf, but it also has no children.
     *
     * @param   node  a node in the tree, obtained from this data source
     * @return  true if <code>node</code> is a leaf
     */
    public boolean isLeaf(Object node) {
        if (node == rootNode) return false;
        return super.isLeaf(node);
    }
    
    /**
     * Messaged when the user has altered the value for the item identified
     * by <code>path</code> to <code>newValue</code>.
     * If <code>newValue</code> signifies a truly new value
     * the model should post a <code>treeNodesChanged</code> event.
     *
     * @param path path to the node that the user has altered
     * @param newValue the new value from the TreeCellEditor
     */
    public void valueForPathChanged(TreePath path, Object newValue) { super.valueForPathChanged(path, newValue); }
    
    /**
     * Returns the index of child in parent.  If either <code>parent</code>
     * or <code>child</code> is <code>null</code>, returns -1.
     * If either <code>parent</code> or <code>child</code> don't
     * belong to this tree model, returns -1.
     *
     * @param parent a note in the tree, obtained from this data source
     * @param child the node we are interested in
     * @return the index of the child in the parent, or -1 if either
     *    <code>child</code> or <code>parent</code> are <code>null</code>
     *    or don't belong to this tree model
     */
    public int getIndexOfChild(Object parent, Object child) {
        if (child == null)
            throw new IllegalArgumentException("argument is null");
        if (parent == rootNode) {
            if (child == errorExplorerNode) return contentExplorerNodes.size() ;
            if (child == jobExplorerNode) return contentExplorerNodes.size() + 1;
            return contentExplorerNodes.indexOf(child);
        }
        return super.getIndexOfChild(parent, child);
    }
    
//
//  Change Events
//
    
    /**
     * Adds a listener for the <code>TreeModelEvent</code>
     * posted after the tree changes.
     *
     * @param   l       the listener to add
     * @see     #removeTreeModelListener
     */
    public void addTreeModelListener(TreeModelListener l) {
//        System.out.println("addTreeModelListener " + l);
        allListeners.add(l);
        super.addTreeModelListener(l);
    }
    
    /**
     * Removes a listener previously added with
     * <code>addTreeModelListener</code>.
     *
     * @see     #addTreeModelListener
     * @param   l       the listener to remove
     */
    public void removeTreeModelListener(TreeModelListener l) {
//        System.out.println("removeTreeModelListener " + l);
        allListeners.remove(l);
        super.removeTreeModelListener(l);
    }
    
    /**
     * Notifies all listeners that have registered interest for notification on this event type.
     * @param source source of event
     * @param path the path to the root node
     * @param childIndices the indices of the changeed elements
     * @param children the changed elements
     */
    static void fireTreeNodesChanged(Object source, TreePath treePath, int[] childIndices, Object[] children) {
        TreeModelEvent e = new TreeModelEvent(source, treePath, childIndices, children);
        for (Iterator<TreeModelListener> it = allListeners.reverseIterator(); it.hasNext(); ) {
            TreeModelListener l = it.next();
            l.treeNodesChanged(e);
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type.
     * @param source source of event
     * @param path the path to the root node
     * @param childIndices the indices of the inserted elements
     * @param children the inserted elements
     */
    static void fireTreeNodesInserted(Object source, TreePath treePath, int[] childIndices, Object[] children) {
        TreeModelEvent e = new TreeModelEvent(source, treePath, childIndices, children);
        for (Iterator<TreeModelListener> it = allListeners.reverseIterator(); it.hasNext(); ) {
            TreeModelListener l = it.next();
            l.treeNodesInserted(e);
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type.
     * @param source source of event
     * @param path the path to the root node
     * @param childIndices the indices of the removed elements
     * @param children the removed elements
     */
    static void fireTreeNodesRemoved(Object source, TreePath treePath, int[] childIndices, Object[] children) {
        TreeModelEvent e = new TreeModelEvent(source, treePath, childIndices, children);
        for (Iterator<TreeModelListener> it = allListeners.reverseIterator(); it.hasNext(); ) {
            TreeModelListener l = it.next();
            l.treeNodesRemoved(e);
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type.
     * @param source source of event
     * @param treePath tree path to root mode.
     */
    static void fireTreeStructureChanged(Object source, TreePath treePath) {
        TreeModelEvent e = new TreeModelEvent(source, treePath, null, null);
        for (Iterator<TreeModelListener> it = allListeners.reverseIterator(); it.hasNext(); ) {
            TreeModelListener l = it.next();
            l.treeStructureChanged(e);
        }
    }
    
    /**
     * Invoke this method if you've modified the TreeNodes upon which this
     * model depends.  The model will notify all of its listeners that the
     * model has changed below the node <code>node</code> (PENDING).
     */
    public void reload(Object path[]) {
//        int i = 0;
//        for (Iterator<TreeModelListener> it = treeModelListeners.reverseIterator(); it.hasNext(); ) {
//            TreeModelListener l = it.next();
//            System.out.println("Alive listener " + (i++) + " " + l);
//        }
        if (path != null)
            fireTreeStructureChanged(this, path, null, null);
    }
    
    /**
     * Invoke this method after you've inserted some TreeNodes into
     * node.  childIndices should be the index of the new elements and
     * must be sorted in ascending order.
     */
    public void nodesWereInserted(Object[] path, int[] childIndices) {
        if (listenerList != null && path != null && path.length > 0 && childIndices != null && childIndices.length > 0) {
            int cCount = childIndices.length;
            Object[] newChildren = new Object[cCount];
            
            Object parent = path[path.length - 1];
            for (int counter = 0; counter < cCount; counter++)
                newChildren[counter] = getChild(parent, childIndices[counter]);
            fireTreeNodesInserted(this, path, childIndices, newChildren);
        }
    }
    
    /**
     * Invoke this method after you've removed some TreeNodes from
     * node.  childIndices should be the index of the removed elements and
     * must be sorted in ascending order. And removedChildren should be
     * the array of the children objects that were removed.
     */
    public void nodesWereRemoved(Object path[], int[] childIndices, Object[] removedChildren) {
        if (path != null && childIndices != null)
            fireTreeNodesRemoved(this, path, childIndices, removedChildren);
    }
    
    /**
     * Invoke this method after you've changed how the children identified by
     * childIndicies are to be represented in the tree.
     */
    public void nodesChanged(Object[] path, int[] childIndices) {
        if (path == null || path.length == 0) return;
        Object node = path[path.length - 1];
        if (node != null) {
            if (childIndices != null) {
                int cCount = childIndices.length;
                
                if (cCount > 0) {
                    Object[] cChildren = new Object[cCount];
                    
                    for (int counter = 0; counter < cCount; counter++)
                        cChildren[counter] = getChild(node, childIndices[counter]);
                    fireTreeNodesChanged(this, path, childIndices, cChildren);
                }
            } else if (node == getRoot()) {
                fireTreeNodesChanged(this, path, null, null);
            }
        }
    }
    
    /**
     * Builds the parents of node up to and including the root node,
     * where the original node is the last element in the returned array.
     * The length of the returned array gives the node's depth in the
     * tree.
     *
     * @param aNode the TreeNode to get the path for
     */
    public TreeNode[] getPathToRoot(TreeNode aNode) {
        TreeNode[] path = super.getPathToRoot(aNode);
        return path;
    }
    
    /**
     * Builds the parents of node up to and including the root node,
     * where the original node is the last element in the returned array.
     * The length of the returned array gives the node's depth in the
     * tree.
     *
     * @param aNode  the TreeNode to get the path for
     * @param depth  an int giving the number of steps already taken towards
     *        the root (on recursive calls), used to size the returned array
     * @return an array of TreeNodes giving the path from the root to the
     *         specified node
     */
    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        TreeNode[]              retNodes;
        // This method recurses, traversing towards the root in order
        // size the array. On the way back, it fills in the nodes,
        // starting from the root and working back to the original node.
        
        /* Check for null, in case someone passed in a null node, or
           they passed in an element that isn't rooted at root. */
        if(aNode == null) {
            if(depth == 0)
                return null;
            else
                retNodes = new TreeNode[depth];
        } else {
            depth++;
            if(aNode == root)
                retNodes = new TreeNode[depth];
            else {
                TreeNode parent = aNode.getParent();
                if (parent == null && root.getIndex(aNode) >= 0) {
                    retNodes = new TreeNode[depth + 1];
                    retNodes[0] = root;
                } else {
                    retNodes = getPathToRoot(parent, depth);
                }
            }
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }
    
    void updateContentExplorerNodes(List<MutableTreeNode> newContentExplorerNodes) {
        Object[] children = contentExplorerNodes.toArray();
        int[] childIndices = new int[children.length];
        for (int i = 0; i < children.length; i++) childIndices[i] = i;
        contentExplorerNodes.clear();
        nodesWereRemoved(rootPath, childIndices, children);
        
        contentExplorerNodes.addAll(newContentExplorerNodes);
        childIndices = new int[contentExplorerNodes.size()];
        for (int i = 0; i < childIndices.length; i++) childIndices[i] = i;
        nodesWereInserted(rootPath, childIndices);
    }
    
    /**
	 * A static object is used so that its open/closed tree state can be maintained.
	 */
	private static String libraryNode = "LIBRARIES";

    static void showAlphabeticallyAction() {
        howToShow = SHOWALPHABETICALLY;
        WindowFrame.wantToRedoLibraryTree();
    }
    
    static void showByGroupAction() {
        howToShow = SHOWBYCELLGROUP;
        WindowFrame.wantToRedoLibraryTree();
    }
    
    static void showByHierarchyAction() {
        howToShow = SHOWBYHIERARCHY;
        WindowFrame.wantToRedoLibraryTree();
    }

	public static DefaultMutableTreeNode makeLibraryTree()
	{
		DefaultMutableTreeNode libraryExplorerTree = new DefaultMutableTreeNode(libraryNode);

		// reconstruct the tree
		switch (howToShow)
		{
			case SHOWALPHABETICALLY:
				rebuildExplorerTreeByName(libraryExplorerTree);
				break;
			case SHOWBYCELLGROUP:
				rebuildExplorerTreeByGroups(libraryExplorerTree);
				break;
			case SHOWBYHIERARCHY:
				rebuildExplorerTreeByHierarchy(libraryExplorerTree);
				break;
		}
		return libraryExplorerTree;
	}

	private static synchronized void rebuildExplorerTreeByName(DefaultMutableTreeNode libraryExplorerTree)
	{
		for(Library lib : Library.getVisibleLibraries())
		{
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			if (!addTechnologyLibraryToTree(lib, libTree))
			{
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = eit.next();
					DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cell);
					libTree.add(cellTree);
				}
			}
			libraryExplorerTree.add(libTree);
		}
	}

	private static boolean addTechnologyLibraryToTree(Library lib, DefaultMutableTreeNode libTree)
	{
		boolean techLib = false;
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			if (cell.isInTechnologyLibrary())
			{
				techLib = true;
				break;
			}
		}
		if (!techLib) return false;

		// add this library as a technology library
		DefaultMutableTreeNode layerTree = new DefaultMutableTreeNode("TECHNOLOGY LAYERS");
		DefaultMutableTreeNode arcTree = new DefaultMutableTreeNode("TECHNOLOGY ARCS");
		DefaultMutableTreeNode nodeTree = new DefaultMutableTreeNode("TECHNOLOGY NODES");
		DefaultMutableTreeNode miscTree = new DefaultMutableTreeNode("TECHNOLOGY SUPPORT");
		libTree.add(layerTree);
		libTree.add(arcTree);
		libTree.add(nodeTree);
		libTree.add(miscTree);
		HashSet<Cell> allCells = new HashSet<Cell>();
		Cell [] layerCells = LayerInfo.getLayerCells(lib);
		for(int i=0; i<layerCells.length; i++)
		{
			allCells.add(layerCells[i]);
			layerTree.add(new DefaultMutableTreeNode(layerCells[i]));
		}
		Cell [] arcCells = ArcInfo.getArcCells(lib);
		for(int i=0; i<arcCells.length; i++)
		{
			allCells.add(arcCells[i]);
			arcTree.add(new DefaultMutableTreeNode(arcCells[i]));
		}
		Cell [] nodeCells = NodeInfo.getNodeCells(lib);
		for(int i=0; i<nodeCells.length; i++)
		{
			allCells.add(nodeCells[i]);
			nodeTree.add(new DefaultMutableTreeNode(nodeCells[i]));
		}
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			if (allCells.contains(cell)) continue;
			miscTree.add(new DefaultMutableTreeNode(cell));
		}
		return true;
	}

	private static synchronized void rebuildExplorerTreeByHierarchy(DefaultMutableTreeNode libraryExplorerTree)
	{
		for(Library lib : Library.getVisibleLibraries())
		{
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			if (!addTechnologyLibraryToTree(lib, libTree))
			{
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = eit.next();
	
					// ignore icons and text views
					if (cell.isIcon()) continue;
					if (cell.getView().isTextView()) continue;
	
			        HashSet<Cell> addedCells = new HashSet<Cell>();
					for(Iterator<Cell> vIt = cell.getVersions(); vIt.hasNext(); )
					{
						Cell cellVersion = vIt.next();
						Iterator insts = cellVersion.getInstancesOf();
						if (insts.hasNext()) continue;
	
						// no children: add this as root node
	                    if (addedCells.contains(cellVersion)) continue;          // prevent duplicate entries
						DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cellVersion);
						libTree.add(cellTree);
	                    addedCells.add(cellVersion);
						createHierarchicalExplorerTree(cellVersion, cellTree);
					}
				}
			}
			libraryExplorerTree.add(libTree);
		}
	}

	/**
	 * Method to build a hierarchical explorer structure.
	 */
	private static void createHierarchicalExplorerTree(Cell cell, DefaultMutableTreeNode cellTree)
	{
		// see what is inside
		HashMap<Cell,DBMath.MutableInteger> cellCount = new HashMap<Cell,DBMath.MutableInteger>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;
			Cell subCell = (Cell)ni.getProto();
			if (subCell.isIcon())
			{
				if (ni.isIconOfParent()) continue;
				subCell = subCell.contentsView();
				if (subCell == null) continue;
			}
			DBMath.MutableInteger mi = cellCount.get(subCell);
			if (mi == null)
			{
				mi = new DBMath.MutableInteger(0);
				cellCount.put(subCell, mi);
			}
			mi.setValue(mi.intValue()+1);
		}

		// show what is there
		for(Cell subCell : cellCount.keySet())
		{
			DBMath.MutableInteger mi = cellCount.get(subCell);
			if (mi == null) continue;

			CellAndCount cc = new CellAndCount(subCell, mi.intValue());
			DefaultMutableTreeNode subCellTree = new DefaultMutableTreeNode(cc);
			cellTree.add(subCellTree);
			createHierarchicalExplorerTree(subCell, subCellTree);
		}
	}

	private static synchronized void rebuildExplorerTreeByGroups(DefaultMutableTreeNode libraryExplorerTree)
	{
		HashSet<Cell> cellsSeen = new HashSet<Cell>();
		for(Library lib : Library.getVisibleLibraries())
		{
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			if (!addTechnologyLibraryToTree(lib, libTree))
			{
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = eit.next();
					cellsSeen.remove(cell);
				}
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = eit.next();
					if (cell.getNewestVersion() != cell) continue;
					Cell.CellGroup group = cell.getCellGroup();
					int numNewCells = 0;
					for(Iterator<Cell> gIt = group.getCells(); gIt.hasNext(); )
					{
						Cell cellInGroup = gIt.next();
						if (cellInGroup.getNewestVersion() == cellInGroup) numNewCells++;
					}
					if (numNewCells == 1)
					{
						addCellAndAllVersions(cell, libTree);
						continue;
					}
	
					List<Cell> cellsInGroup = group.getCellsSortedByView();
					DefaultMutableTreeNode groupTree = null;
					for(Cell cellInGroup : cellsInGroup)
					{
	                    if ((cellInGroup.getNumVersions() > 1) && (cellInGroup.getNewestVersion() != cellInGroup)) continue;
						if (cellsSeen.contains(cellInGroup)) continue;
						if (groupTree == null)
						{
							groupTree = new DefaultMutableTreeNode(group);
						}
						cellsSeen.add(cellInGroup);
						addCellAndAllVersions(cellInGroup, groupTree);
					}
					if (groupTree != null)
						libTree.add(groupTree);
				}
			}
			libraryExplorerTree.add(libTree);
		}
	}

	private static void addCellAndAllVersions(Cell cell, DefaultMutableTreeNode libTree)
	{
		DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cell);
		libTree.add(cellTree);
		if (cell.isMultiPage())
		{
			int pageCount = cell.getNumMultiPages();
			for(int i=0; i<pageCount; i++)
			{
				MultiPageCell mpc = new MultiPageCell();
				mpc.cell = cell;
				mpc.pageNo = i;
				DefaultMutableTreeNode pageTree = new DefaultMutableTreeNode(mpc);
				cellTree.add(pageTree);
			}
		}
		if (cell.getNumVersions() > 1)
		{
			for(Iterator<Cell> vIt = cell.getVersions(); vIt.hasNext(); )
			{
				Cell oldVersion = vIt.next();
				if (oldVersion == cell) continue;
				DefaultMutableTreeNode oldCellTree = new DefaultMutableTreeNode(oldVersion);
				cellTree.add(oldCellTree);
				if (oldVersion.isMultiPage())
				{
					int pageCount = oldVersion.getNumMultiPages();
					for(int i=0; i<pageCount; i++)
					{
						MultiPageCell mpc = new MultiPageCell();
						mpc.cell = oldVersion;
						mpc.pageNo = i;
						DefaultMutableTreeNode pageTree = new DefaultMutableTreeNode(mpc);
						oldCellTree.add(pageTree);
					}
				}
			}
		}
	}

}

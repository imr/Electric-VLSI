/*
 * Created on Oct 3, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.sun.electric.tool.user.ui;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.*;

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UITreeView extends JTree 
{
	private UITreeHandler handler = null;
	
	private UITreeView()
	{
		super();
//		single selection as default
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		//show handle as default
		setShowsRootHandles(true);
		//show only root as default
		collapseRow(0);
	}
	
	private UITreeView(TreeNode str)
	{
		super(str);
		//single selection as default
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		//show handle as default
		setShowsRootHandles(true);
		//show only root as default
		collapseRow(0);
	}

	public static UITreeView CreateTreeView()
	{
		return new UITreeView();
	}
	
	public static UITreeView CreateTreeView(TreeNode str)
	{
		return new UITreeView(str);
	}

//if need custome image to show tree
	public void addTreeBranchImage(ImageIcon icon)
	{
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(icon);
		setCellRenderer(renderer);
	}
	
	public void setTreeSelectionHandler(UITreeHandler treehandler)
	{
		treehandler.setTreeView(this);
		addTreeSelectionListener(treehandler);
		handler = treehandler;
	}
	
	public UITreeHandler getTreeSelectionHandler()
	{
		return handler; 
	}
}

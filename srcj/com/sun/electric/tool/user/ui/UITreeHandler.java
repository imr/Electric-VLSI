/*
 * Created on Oct 10, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.sun.electric.tool.user.ui;

import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UITreeHandler implements TreeSelectionListener 
//,TreeExpansionListener,TreeModelListener 
{

	private UITreeView tree;
	
	public void valueChanged(TreeSelectionEvent arg0) 
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
		if(node ==null)
		{
			return;		
		}
		
		Object nodeInfo = node.getUserObject();
		
		if(node.isLeaf())
		{
			//TODO: do something when it is leaf
		}
		else
		{
			//TODO: do something when it is not leaf
		}
		
	}
	
	public UITreeView getTreeView()
	{
		return tree;
	}
	
	public void setTreeView(UITreeView uitree)
	{
		tree = uitree;
	}

}

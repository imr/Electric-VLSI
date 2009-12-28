/*
 * ChainModel.java
 *
 * Created on August 7, 2003, 8:30 PM
 */

package com.sun.electric.tool.simulation.test;

import java.util.ArrayList;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Tree data model of a scan chain, with APIs for inspection and I/O. This class
 * should only be used by ChainG, which requires a TreeModel implementation to
 * fire up the JTree. All other programs should probably be using the more
 * streamlined ChainControl instead.
 * 
 * @author Eric Kim
 * @version 1.0 9/3/03
 * @author Tom O'Neill (toneill)
 * @version 1.1 7/22/04
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */

class ChainModel extends ChainControl {

    /** Cruft necessary for TreeModel implementation, should try to remove */
    private ArrayList treeModelListeners;

    private boolean isWorker = false; // true if worker is working

    /**
     * Creates a new instance of ChainModel, with the scan chain hierarchy
     * specified in provided the XML file
     * 
     * @param fileName
     *            Name of XML file containing scan chain description
     */
    ChainModel(String fileName) {
        super(fileName);
        treeModelListeners = new ArrayList();
    }

    /**
     * Messaged when the user has altered the value for the item identified by
     * path to newValue.
     */
    public void valueForPathChanged(TreePath path, Object newValue) {
        System.out.println("*** valueForPathChanged : " + path + " --> "
                + newValue);
    }

    /** Adds a listener for the TreeModelEvent posted after the tree changes. */
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.add(l);
    }

    /** Removes a listener previously added with addTreeModelListener. */
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(l);
    }

    /** Unit test */
    public static void main(String[] args) {
        ChainModel cm = new ChainModel("heater.xml");
        MyTreeNode node = cm.findNode("heater.pScan");
        System.out.println(node);
        MyTreeNode node2 = cm.findNode("p0.scan430", node);
        System.out.println(node2);
    }
}
